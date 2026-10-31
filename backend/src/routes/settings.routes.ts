
import { Router } from "express";
import prisma from "../lib/prisma";
import { requireAuth } from "../middleware/requireAuth";
import {
    asRecord,
    serverError,
    userIdOf
} from "../lib/apiHelpers";

const router = Router();

router.use(requireAuth);

const defaults = {
    homeCurrency: "ZAR",
    languageCode: "en",
    notificationsEnabled: true
};

const parseSettings = (
    input: unknown
): {
    data?: {
        homeCurrency?: string;
        languageCode?: string;
        notificationsEnabled?: boolean;
    };
    error?: string;
} => {
    const body = asRecord(input);

    if (!body) {
        return {
            error: "A valid settings object is required."
        };
    }

    const data: {
        homeCurrency?: string;
        languageCode?: string;
        notificationsEnabled?: boolean;
    } = {};

    if (body.homeCurrency !== undefined) {
        if (
            typeof body.homeCurrency !== "string" ||
            !/^[A-Za-z]{3}$/.test(body.homeCurrency)
        ) {
            return {
                error: "homeCurrency must be a three-letter currency code."
            };
        }

        data.homeCurrency = body.homeCurrency.toUpperCase();
    }

    if (body.languageCode !== undefined) {
        if (
            typeof body.languageCode !== "string" ||
            !/^[a-zA-Z]{2,3}(-[a-zA-Z0-9]{2,8})*$/.test(
                body.languageCode
            )
        ) {
            return {
                error: "Invalid language code."
            };
        }

        data.languageCode = body.languageCode;
    }

    if (body.notificationsEnabled !== undefined) {
        if (typeof body.notificationsEnabled !== "boolean") {
            return {
                error: "notificationsEnabled must be true or false."
            };
        }

        data.notificationsEnabled = body.notificationsEnabled;
    }

    if (Object.keys(data).length === 0) {
        return {
            error: "Provide at least one setting to update."
        };
    }

    return { data };
};

router.get("/", async (req, res) => {
    try {
        const userId = userIdOf(req);

        const existing = await prisma.userSettings.findUnique({
            where: {
                userId
            }
        });

        const settings = existing
            ? {
                homeCurrency: existing.homeCurrency,
                languageCode: existing.languageCode,
                notificationsEnabled: existing.notificationsEnabled,
                updatedAt: existing.updatedAt
            }
            : {
                ...defaults,
                updatedAt: null
            };

        return res.status(200).json({ settings });
    } catch (error) {
        return serverError(
            res,
            error,
            "Unable to retrieve user settings."
        );
    }
});

router.patch("/", async (req, res) => {
    const parsed = parseSettings(req.body);

    if (!parsed.data) {
        return res.status(400).json({
            message: parsed.error
        });
    }

    try {
        const userId = userIdOf(req);
        const data = parsed.data;

        const settings = await prisma.userSettings.upsert({
            where: {
                userId
            },
            create: {
                userId,
                homeCurrency:
                    data.homeCurrency ?? defaults.homeCurrency,
                languageCode:
                    data.languageCode ?? defaults.languageCode,
                notificationsEnabled:
                    data.notificationsEnabled ??
                    defaults.notificationsEnabled
            },
            update: data
        });

        return res.status(200).json({
            settings: {
                homeCurrency: settings.homeCurrency,
                languageCode: settings.languageCode,
                notificationsEnabled: settings.notificationsEnabled,
                updatedAt: settings.updatedAt
            }
        });
    } catch (error) {
        return serverError(
            res,
            error,
            "Unable to update user settings."
        );
    }
});

export default router;
