
const test = require("node:test");
const assert = require("node:assert/strict");
const { randomUUID, createHash } = require("node:crypto");

const base = process.env.ROAMLY_TEST_BASE_URL?.replace(/\/+$/, "");
const token = process.env.ROAMLY_TEST_TOKEN?.trim();

if (!base || !/^https:\/\//i.test(base)) {
    throw new Error("ROAMLY_TEST_BASE_URL must be the live HTTPS API URL.");
}

if (!token) {
    throw new Error("ROAMLY_TEST_TOKEN is required.");
}

if (/localhost|127\.0\.0\.1/i.test(base)) {
    throw new Error("Phase 3 tests must target the live Azure API.");
}

const png = Buffer.from(
    "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+/l90AAAAASUVORK5CYII=",
    "base64"
);

const request = (path, options = {}) =>
    fetch(`${base}${path}`, options);

const authorized = (accessToken, extra = {}) => ({
    Authorization: `Bearer ${accessToken}`,
    ...extra
});

async function verify(response, expected, label) {
    if (response.status !== expected) {
        const body = (await response.text()).slice(0, 400);

        throw new Error(
            `${label}: expected HTTP ${expected}, received ${response.status}. ${body}`
        );
    }

    console.log(`PASS: ${label}`);
    return response;
}

function createForm(buffer, filename, contentType) {
    const form = new FormData();

    form.append("name", "Phase 3 Automated Test");
    form.append("documentType", "Other");
    form.append(
        "file",
        new Blob([buffer], { type: contentType }),
        filename
    );

    return form;
}

function checksum(buffer) {
    return createHash("sha256").update(buffer).digest("hex");
}

test("Roamly Phase 3 live document security and lifecycle", {
    timeout: 120000
}, async () => {
    let documentId = null;
    let deleted = false;

    try {
        await verify(
            await request("/api/documents/upload", {
                method: "POST"
            }),
            401,
            "Upload requires authentication"
        );

        await verify(
            await request("/api/documents/upload", {
                method: "POST",
                headers: authorized(token),
                body: createForm(
                    Buffer.from("invalid document"),
                    "invalid.pdf",
                    "application/pdf"
                )
            }),
            400,
            "Invalid document content rejected"
        );

        const uploadResponse = await verify(
            await request("/api/documents/upload", {
                method: "POST",
                headers: authorized(token),
                body: createForm(
                    png,
                    "phase3-test.png",
                    "image/png"
                )
            }),
            201,
            "Valid PNG uploaded to live Blob Storage"
        );

        const upload = await uploadResponse.json();

        documentId = upload.document?.documentId;

        assert.ok(documentId, "Document ID is missing");

        await verify(
            await request(`/api/documents/${documentId}`, {
                headers: authorized(token)
            }),
            200,
            "Document metadata saved in Azure SQL"
        );

        await verify(
            await request(`/api/documents/${documentId}/download`),
            401,
            "Download requires authentication"
        );

        const downloadResponse = await verify(
            await request(`/api/documents/${documentId}/download`, {
                headers: authorized(token)
            }),
            200,
            "Owner can download the document"
        );

        const downloaded = Buffer.from(
            await downloadResponse.arrayBuffer()
        );

        assert.equal(checksum(downloaded), checksum(png));

        console.log("PASS: Downloaded file matches uploaded file");

        await verify(
            await request(`/api/documents/${documentId}`, {
                method: "PATCH",
                headers: authorized(token, {
                    "Content-Type": "application/json"
                }),
                body: JSON.stringify({
                    fileUrl: "https://example.com/replaced.png"
                })
            }),
            409,
            "Uploaded file location cannot be modified"
        );

        const email =
            `roamly.phase3.${randomUUID()}@example.com`;

        const password =
            `RoamlyTest${randomUUID().slice(0, 12)}Aa7`;

        const registerResponse = await verify(
            await request("/api/auth/register", {
                method: "POST",
                headers: {
                    "Content-Type": "application/json"
                },
                body: JSON.stringify({
                    name: "Phase 3 Test User",
                    email,
                    password,
                    confirmPassword: password
                })
            }),
            201,
            "Second test account registered"
        );

        const secondAccount = await registerResponse.json();
        const secondToken = secondAccount.accessToken;

        assert.ok(secondToken, "Second account token is missing");

        await verify(
            await request(`/api/documents/${documentId}/download`, {
                headers: authorized(secondToken)
            }),
            404,
            "Another user cannot download the document"
        );

        await verify(
            await request(`/api/documents/${documentId}`, {
                method: "DELETE",
                headers: authorized(secondToken)
            }),
            404,
            "Another user cannot delete the document"
        );

        await verify(
            await request(`/api/documents/${documentId}`, {
                headers: authorized(token)
            }),
            200,
            "Document remains available to its owner"
        );

        await verify(
            await request(`/api/documents/${documentId}`, {
                method: "DELETE",
                headers: authorized(token)
            }),
            200,
            "Owner successfully deletes the document"
        );

        deleted = true;

        await verify(
            await request(`/api/documents/${documentId}`, {
                headers: authorized(token)
            }),
            404,
            "Deleted SQL metadata no longer accessible"
        );

        await verify(
            await request(`/api/documents/${documentId}/download`, {
                headers: authorized(token)
            }),
            404,
            "Deleted document can no longer be downloaded"
        );

        console.log("ALL PHASE 3 LIVE INTEGRATION CHECKS PASSED");
    } finally {
        if (documentId && !deleted) {
            try {
                const cleanup = await request(
                    `/api/documents/${documentId}`,
                    {
                        method: "DELETE",
                        headers: authorized(token)
                    }
                );

                if (![200, 204, 404].includes(cleanup.status)) {
                    console.error(
                        `Test document cleanup returned HTTP ${cleanup.status}.`
                    );
                }
            } catch (error) {
                console.error(
                    "Test document cleanup failed:",
                    error.message
                );
            }
        }
    }
});
