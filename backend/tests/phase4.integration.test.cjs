
const test = require("node:test");
const assert = require("node:assert/strict");
const { randomUUID } = require("node:crypto");

const base = process.env.ROAMLY_TEST_BASE_URL?.replace(/\/+$/, "");
const token = process.env.ROAMLY_TEST_TOKEN?.trim();

if (!base || !base.startsWith("https://")) {
    throw new Error("A live HTTPS API URL is required.");
}

if (!token) {
    throw new Error("ROAMLY_TEST_TOKEN is required.");
}

if (/localhost|127\.0\.0\.1/i.test(base)) {
    throw new Error("Tests must target the live Azure API.");
}

const auth = accessToken => ({
    Authorization: `Bearer ${accessToken}`
});

const jsonHeaders = accessToken => ({
    ...auth(accessToken),
    "Content-Type": "application/json"
});

async function request(path, options = {}) {
    return fetch(`${base}${path}`, options);
}

async function expectStatus(response, expected, label) {
    if (response.status !== expected) {
        const body = (await response.text()).slice(0, 500);

        throw new Error(
            `${label}: Expected ${expected}, received ${response.status}. ${body}`
        );
    }

    console.log(`PASS: ${label}`);

    return response;
}

test("Roamly Phase 4 live NoSQL integration", {
    timeout: 120000
}, async () => {
    let noteId = null;
    let deleted = false;

    try {
        await expectStatus(
            await request("/api/travel-notes"),
            401,
            "Authentication required"
        );

        await expectStatus(
            await request("/api/travel-notes", {
                method: "POST",
                headers: jsonHeaders(token),
                body: JSON.stringify({
                    title: "",
                    content: ""
                })
            }),
            400,
            "Invalid note rejected"
        );

        const createdResponse = await expectStatus(
            await request("/api/travel-notes", {
                method: "POST",
                headers: jsonHeaders(token),
                body: JSON.stringify({
                    title: "Phase 4 Test Note",
                    content: "Planning my holiday",
                    category: "planning",
                    tags: ["holiday", "test"]
                })
            }),
            201,
            "Travel note created in Azure NoSQL"
        );

        const created = await createdResponse.json();

        noteId = created.note?.noteId;

        assert.ok(noteId, "Created note ID is missing");

        const readResponse = await expectStatus(
            await request(`/api/travel-notes/${noteId}`, {
                headers: auth(token)
            }),
            200,
            "Travel note retrieved"
        );

        const read = await readResponse.json();

        assert.equal(read.note.title, "Phase 4 Test Note");

        const listResponse = await expectStatus(
            await request("/api/travel-notes", {
                headers: auth(token)
            }),
            200,
            "Travel notes listed"
        );

        const list = await listResponse.json();

        assert.ok(
            list.notes.some(note => note.noteId === noteId),
            "Created note is missing from list"
        );

        console.log("PASS: Created note appears in user list");

        const updateResponse = await expectStatus(
            await request(`/api/travel-notes/${noteId}`, {
                method: "PUT",
                headers: jsonHeaders(token),
                body: JSON.stringify({
                    title: "Updated Phase 4 Note",
                    content: "Holiday planning updated",
                    category: "planning",
                    tags: ["food", "planning"]
                })
            }),
            200,
            "Travel note updated"
        );

        const updated = await updateResponse.json();

        assert.equal(
            updated.note.title,
            "Updated Phase 4 Note"
        );

        assert.deepEqual(
            updated.note.tags,
            ["food", "planning"]
        );

        console.log("PASS: Updated note content verified");

        const secondEmail =
            `roamly.nosql.${randomUUID()}@example.com`;

        const secondPassword =
            `RoamlyTest${randomUUID().slice(0, 12)}Aa7`;

        const secondResponse = await expectStatus(
            await request("/api/auth/register", {
                method: "POST",
                headers: {
                    "Content-Type": "application/json"
                },
                body: JSON.stringify({
                    name: "NoSQL Test User",
                    email: secondEmail,
                    password: secondPassword,
                    confirmPassword: secondPassword
                })
            }),
            201,
            "Second test user registered"
        );

        const secondAccount = await secondResponse.json();

        assert.ok(
            secondAccount.accessToken,
            "Second user token missing"
        );

        const secondToken = secondAccount.accessToken;

        await expectStatus(
            await request(`/api/travel-notes/${noteId}`, {
                headers: auth(secondToken)
            }),
            404,
            "Another user cannot read the note"
        );

        await expectStatus(
            await request(`/api/travel-notes/${noteId}`, {
                method: "DELETE",
                headers: auth(secondToken)
            }),
            404,
            "Another user cannot delete the note"
        );

        const secondListResponse = await expectStatus(
            await request("/api/travel-notes", {
                headers: auth(secondToken)
            }),
            200,
            "Second user can list their own notes"
        );

        const secondList = await secondListResponse.json();

        assert.ok(
            !secondList.notes.some(note => note.noteId === noteId),
            "Another user's note was exposed"
        );

        console.log("PASS: Notes isolated by user");

        await expectStatus(
            await request(`/api/travel-notes/${noteId}`, {
                method: "DELETE",
                headers: auth(token)
            }),
            204,
            "Owner deleted the note"
        );

        deleted = true;

        await expectStatus(
            await request(`/api/travel-notes/${noteId}`, {
                headers: auth(token)
            }),
            404,
            "Deleted note no longer accessible"
        );

        console.log("ALL PHASE 4 LIVE INTEGRATION CHECKS PASSED");
    } finally {
        if (noteId && !deleted) {
            try {
                const cleanup = await request(
                    `/api/travel-notes/${noteId}`,
                    {
                        method: "DELETE",
                        headers: auth(token)
                    }
                );

                if (![204, 404].includes(cleanup.status)) {
                    console.error(
                        `Cleanup returned HTTP ${cleanup.status}`
                    );
                }
            } catch (error) {
                console.error(
                    "Test note cleanup failed:",
                    error.message
                );
            }
        }
    }
});
