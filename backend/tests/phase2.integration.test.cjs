
const test = require("node:test");
const assert = require("node:assert/strict");
const { randomUUID } = require("node:crypto");

const base = (process.env.ROAMLY_TEST_URL || "http://localhost:3000").replace(/\/$/, "");
const token = process.env.ROAMLY_TEST_TOKEN;
const otherToken = process.env.ROAMLY_TEST_OTHER_TOKEN;

async function api(method, path, body, bearer = token) {
    const headers = { Accept: "application/json" };

    if (bearer) {
        headers.Authorization = bearer.startsWith("Bearer ")
            ? bearer
            : `Bearer ${bearer}`;
    }

    if (body !== undefined) {
        headers["Content-Type"] = "application/json";
    }

    const response = await fetch(`${base}${path}`, {
        method,
        headers,
        body: body === undefined ? undefined : JSON.stringify(body),
        signal: AbortSignal.timeout(60000)
    });

    const content = await response.text();
    let data;

    try {
        data = JSON.parse(content);
    } catch {
        data = { message: content.slice(0, 200) };
    }

    return { status: response.status, data };
}

async function check(label, method, path, status, body, bearer = token) {
    const response = await api(method, path, body, bearer);

    assert.equal(
        response.status,
        status,
        `${label}: ${JSON.stringify(response.data)}`
    );

    console.log(`PASS: ${label}`);
    return response.data;
}

test("Roamly Phase 2 end-to-end verification", async () => {
    assert.ok(
        token,
        "Set ROAMLY_TEST_TOKEN using your existing test account."
    );

    const ids = {};
    let previousSettings;
    let settingsChanged = false;

    try {
        await check("API health", "GET", "/api/health", 200);
        await check("Azure SQL health", "GET", "/api/health/database", 200);

        const me = await check(
            "Authenticated profile",
            "GET",
            "/api/users/me",
            200
        );

        assert.ok(me.user.userId);

        for (const path of [
            "/api/users/me",
            "/api/holidays",
            "/api/accommodations",
            "/api/flights",
            "/api/itineraries",
            "/api/packing",
            "/api/checklists",
            "/api/documents",
            "/api/reminders",
            "/api/settings",
            "/api/currency/rate?from=USD&to=ZAR"
        ]) {
            await check(
                `JWT required: ${path}`,
                "GET",
                path,
                401,
                undefined,
                null
            );
        }

        await check("Holiday date validation", "POST", "/api/holidays", 400, {
            title: "Invalid Test",
            tripType: "Leisure",
            country: "South Africa",
            city: "Durban",
            startDate: "2027-07-31",
            endDate: "2027-07-01"
        });

        const holiday = await check(
            "Create holiday",
            "POST",
            "/api/holidays",
            201,
            {
                title: `Phase 2 Test ${randomUUID().slice(0, 8)}`,
                tripType: "Leisure",
                country: "South Africa",
                city: "Cape Town",
                startDate: "2027-07-01",
                endDate: "2027-07-05"
            }
        );

        ids.holiday = holiday.holiday.holidayId;
        assert.ok(ids.holiday);

        await check(
            "Read holiday",
            "GET",
            `/api/holidays/${ids.holiday}`,
            200
        );

        const accommodation = await check(
            "Create accommodation",
            "POST",
            "/api/accommodations",
            201,
            {
                holidayId: ids.holiday,
                name: "Phase 2 Test Hotel",
                type: "Hotel",
                address: "123 Test Street",
                checkInDate: "2027-07-01",
                checkInTime: "14:00",
                checkOutDate: "2027-07-05",
                checkOutTime: "10:00"
            }
        );

        ids.accommodation = accommodation.accommodation.accommodationId;

        await check(
            "Read accommodation",
            "GET",
            `/api/accommodations/${ids.accommodation}`,
            200
        );

        await check(
            "Reject duplicate accommodation",
            "POST",
            "/api/accommodations",
            409,
            {
                holidayId: ids.holiday,
                name: "Duplicate Test",
                type: "Hotel",
                address: "123 Test Street",
                checkInDate: "2027-07-01",
                checkInTime: "14:00",
                checkOutDate: "2027-07-05",
                checkOutTime: "10:00"
            }
        );

        const flight = await check(
            "Create flight",
            "POST",
            "/api/flights",
            201,
            {
                holidayId: ids.holiday,
                flightType: "Outbound",
                airline: "Test Airways",
                flightNumber: "TA123",
                departureAirport: "JNB",
                arrivalAirport: "CPT",
                flightDate: "2027-07-01",
                departureTime: "08:00",
                arrivalTime: "10:00"
            }
        );

        ids.flight = flight.flight.flightId;

        await check(
            "Read flight",
            "GET",
            `/api/flights/${ids.flight}`,
            200
        );

        const itinerary = await check(
            "Create itinerary",
            "POST",
            "/api/itineraries",
            201,
            {
                holidayId: ids.holiday,
                name: "Phase 2 Test Plan"
            }
        );

        ids.itinerary = itinerary.itinerary.itineraryId;

        const day = await check(
            "Create itinerary day",
            "POST",
            `/api/itineraries/${ids.itinerary}/days`,
            201,
            {
                dayNumber: 1,
                date: "2027-07-01"
            }
        );

        ids.day = day.day.itineraryDayId;

        const activity = await check(
            "Create itinerary activity",
            "POST",
            `/api/itineraries/days/${ids.day}/activities`,
            201,
            {
                title: "Test activity",
                startTime: "09:00",
                endTime: "10:00"
            }
        );

        ids.activity = activity.activity.activityId;

        const fullItinerary = await check(
            "Read nested itinerary",
            "GET",
            `/api/itineraries/${ids.itinerary}`,
            200
        );

        assert.equal(
            fullItinerary.itinerary.days[0].activities[0].activityId,
            ids.activity
        );

        const packing = await check(
            "Create packing list",
            "POST",
            "/api/packing",
            201,
            {
                name: "Phase 2 Test Packing"
            }
        );

        ids.packing = packing.packingList.packingListId;

        const packingItem = await check(
            "Create packing item",
            "POST",
            `/api/packing/${ids.packing}/items`,
            201,
            {
                name: "Test item"
            }
        );

        ids.packingItem = packingItem.item.packingItemId;

        const packed = await check(
            "Mark item packed",
            "PATCH",
            `/api/packing/items/${ids.packingItem}`,
            200,
            {
                isPacked: true
            }
        );

        assert.equal(packed.item.isPacked, true);

        const checklist = await check(
            "Create checklist",
            "POST",
            "/api/checklists",
            201,
            {
                name: "Phase 2 Test Checklist"
            }
        );

        ids.checklist = checklist.checklist.checklistId;

        const checklistItem = await check(
            "Create checklist item",
            "POST",
            `/api/checklists/${ids.checklist}/items`,
            201,
            {
                title: "Test task"
            }
        );

        ids.checklistItem = checklistItem.item.checklistItemId;

        const completed = await check(
            "Complete checklist item",
            "PATCH",
            `/api/checklists/items/${ids.checklistItem}`,
            200,
            {
                isCompleted: true
            }
        );

        assert.equal(completed.item.isCompleted, true);

        const document = await check(
            "Create document metadata",
            "POST",
            "/api/documents",
            201,
            {
                name: "Phase 2 Dummy PDF",
                documentType: "Other",
                fileName: "test.pdf",
                fileUrl: "https://example.com/roamly-test.pdf",
                fileSizeBytes: "1024"
            }
        );

        ids.document = document.document.documentId;

        assert.equal(document.document.fileSizeBytes, "1024");

        await check(
            "Read document",
            "GET",
            `/api/documents/${ids.document}`,
            200
        );

        const reminder = await check(
            "Create reminder",
            "POST",
            "/api/reminders",
            201,
            {
                holidayId: ids.holiday,
                title: "Phase 2 Reminder",
                reminderDate: "2027-07-02",
                reminderTime: "09:00",
                isEnabled: true
            }
        );

        ids.reminder = reminder.reminder.reminderId;

        const disabled = await check(
            "Disable reminder",
            "PATCH",
            `/api/reminders/${ids.reminder}`,
            200,
            {
                isEnabled: false
            }
        );

        assert.equal(disabled.reminder.isEnabled, false);

        const settings = await check(
            "Read settings",
            "GET",
            "/api/settings",
            200
        );

        previousSettings = settings.settings;

        const changed = await check(
            "Update settings",
            "PATCH",
            "/api/settings",
            200,
            {
                homeCurrency: "USD",
                languageCode: "en-ZA",
                notificationsEnabled: false
            }
        );

        settingsChanged = true;

        assert.equal(changed.settings.homeCurrency, "USD");

        const persisted = await check(
            "Read saved settings",
            "GET",
            "/api/settings",
            200
        );

        assert.equal(persisted.settings.homeCurrency, "USD");

        const rate = await check(
            "Get real currency rate",
            "GET",
            "/api/currency/rate?from=USD&to=ZAR",
            200
        );

        assert.ok(rate.rate > 0);

        const converted = await check(
            "Convert currency",
            "GET",
            "/api/currency/convert?from=USD&to=ZAR&amount=100",
            200
        );

        assert.ok(converted.convertedAmount > 0);

        await check(
            "Invalid currency amount",
            "GET",
            "/api/currency/convert?from=USD&to=ZAR&amount=-5",
            400
        );

        if (otherToken) {
            const other = await check(
                "Second account token",
                "GET",
                "/api/users/me",
                200,
                undefined,
                otherToken
            );

            assert.notEqual(
                other.user.userId,
                me.user.userId,
                "Second account must be a different user."
            );

            for (const [name, path] of [
                ["holiday", `/api/holidays/${ids.holiday}`],
                ["accommodation", `/api/accommodations/${ids.accommodation}`],
                ["flight", `/api/flights/${ids.flight}`],
                ["itinerary", `/api/itineraries/${ids.itinerary}`],
                ["packing list", `/api/packing/${ids.packing}`],
                ["checklist", `/api/checklists/${ids.checklist}`],
                ["document", `/api/documents/${ids.document}`],
                ["reminder", `/api/reminders/${ids.reminder}`]
            ]) {
                await check(
                    `Cross-user isolation: ${name}`,
                    "GET",
                    path,
                    404,
                    undefined,
                    otherToken
                );
            }

            await check(
                "Cross-user holiday deletion denied",
                "DELETE",
                `/api/holidays/${ids.holiday}`,
                404,
                undefined,
                otherToken
            );
        } else {
            console.log(
                "SKIP: Cross-user isolation needs ROAMLY_TEST_OTHER_TOKEN."
            );
        }

        await check(
            "Delete activity",
            "DELETE",
            `/api/itineraries/activities/${ids.activity}`,
            200
        );
        ids.activity = null;

        await check(
            "Delete day",
            "DELETE",
            `/api/itineraries/days/${ids.day}`,
            200
        );
        ids.day = null;

        await check(
            "Delete itinerary",
            "DELETE",
            `/api/itineraries/${ids.itinerary}`,
            200
        );
        ids.itinerary = null;

        await check(
            "Delete flight",
            "DELETE",
            `/api/flights/${ids.flight}`,
            200
        );
        ids.flight = null;

        await check(
            "Delete accommodation",
            "DELETE",
            `/api/accommodations/${ids.accommodation}`,
            200
        );
        ids.accommodation = null;

        await check(
            "Delete reminder",
            "DELETE",
            `/api/reminders/${ids.reminder}`,
            200
        );
        ids.reminder = null;

        await check(
            "Delete packing list",
            "DELETE",
            `/api/packing/${ids.packing}`,
            200
        );
        ids.packing = null;

        await check(
            "Delete checklist",
            "DELETE",
            `/api/checklists/${ids.checklist}`,
            200
        );
        ids.checklist = null;

        await check(
            "Delete document",
            "DELETE",
            `/api/documents/${ids.document}`,
            200
        );
        ids.document = null;

        await check(
            "Delete holiday",
            "DELETE",
            `/api/holidays/${ids.holiday}`,
            200
        );

        await check(
            "Deleted holiday returns 404",
            "GET",
            `/api/holidays/${ids.holiday}`,
            404
        );

        ids.holiday = null;

        console.log("ALL PHASE 2 INTEGRATION CHECKS PASSED");
    } finally {
        if (settingsChanged && previousSettings) {
            const restored = await api("PATCH", "/api/settings", {
                homeCurrency: previousSettings.homeCurrency,
                languageCode: previousSettings.languageCode,
                notificationsEnabled: previousSettings.notificationsEnabled
            }).catch(() => null);

            if (!restored || restored.status !== 200) {
                console.error("WARNING: Could not restore test settings.");
            }
        }

        for (const [type, path] of [
            ["reminder", "/api/reminders"],
            ["document", "/api/documents"],
            ["packing", "/api/packing"],
            ["checklist", "/api/checklists"],
            ["holiday", "/api/holidays"]
        ]) {
            if (ids[type]) {
                const result = await api(
                    "DELETE",
                    `${path}/${ids[type]}`
                ).catch(() => null);

                if (!result || ![200, 404].includes(result.status)) {
                    console.error(
                        `WARNING: Could not clean up ${type} ${ids[type]}.`
                    );
                }
            }
        }
    }
});
