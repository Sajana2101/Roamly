
import { Router } from "express";
import prisma from "../lib/prisma";
import { requireAuth } from "../middleware/requireAuth";
import {
    asRecord,
    cleanText,
    isUuid,
    parseDate,
    parseTime,
    serverError,
    userIdOf
} from "../lib/apiHelpers";

const router = Router();

router.use(requireAuth);

type ReminderData = {
    title?: string;
    reminderDate?: Date;
    reminderTime?: Date;
    isEnabled?: boolean;
    holidayId?: string | null;
};

const parseReminderBody = (
    input: unknown,
    creating: boolean
): { data?: ReminderData; error?: string } => {
    const body = asRecord(input);

    if (!body) {
        return {
            error: "A valid reminder object is required."
        };
    }

    const data: ReminderData = {};

    if (creating || body.title !== undefined) {
        const title = cleanText(body.title);

        if (!title) {
            return {
                error: "A valid reminder title is required."
            };
        }

        data.title = title;
    }

    if (creating || body.reminderDate !== undefined) {
        const date = parseDate(body.reminderDate);

        if (!date) {
            return {
                error: "reminderDate must use YYYY-MM-DD format."
            };
        }

        data.reminderDate = date;
    }

    if (creating || body.reminderTime !== undefined) {
        const time = parseTime(body.reminderTime);

        if (!time) {
            return {
                error: "reminderTime must use HH:mm or HH:mm:ss format."
            };
        }

        data.reminderTime = time;
    }

    if (body.isEnabled !== undefined) {
        if (typeof body.isEnabled !== "boolean") {
            return {
                error: "isEnabled must be true or false."
            };
        }

        data.isEnabled = body.isEnabled;
    }

    if (body.holidayId !== undefined) {
        if (body.holidayId === null) {
            data.holidayId = null;
        } else if (isUuid(body.holidayId)) {
            data.holidayId = body.holidayId;
        } else {
            return {
                error: "holidayId must be a valid UUID or null."
            };
        }
    }

    if (!creating && Object.keys(data).length === 0) {
        return {
            error: "Provide at least one reminder field to update."
        };
    }

    return { data };
};

router.get("/", async (req, res) => {
    try {
        const reminders = await prisma.reminder.findMany({
            where: {
                userId: userIdOf(req)
            },
            orderBy: [
                { reminderDate: "asc" },
                { reminderTime: "asc" }
            ]
        });

        return res.status(200).json({ reminders });
    } catch (error) {
        return serverError(
            res,
            error,
            "Unable to retrieve reminders."
        );
    }
});

router.get("/holiday/:holidayId", async (req, res) => {
    const holidayId = req.params.holidayId;

    if (!isUuid(holidayId)) {
        return res.status(400).json({
            message: "Invalid holiday ID."
        });
    }

    try {
        const userId = userIdOf(req);

        const holiday = await prisma.holiday.findFirst({
            where: {
                holidayId,
                userId
            },
            select: {
                holidayId: true
            }
        });

        if (!holiday) {
            return res.status(404).json({
                message: "Holiday not found."
            });
        }

        const reminders = await prisma.reminder.findMany({
            where: {
                holidayId,
                userId
            },
            orderBy: [
                { reminderDate: "asc" },
                { reminderTime: "asc" }
            ]
        });

        return res.status(200).json({ reminders });
    } catch (error) {
        return serverError(
            res,
            error,
            "Unable to retrieve holiday reminders."
        );
    }
});

router.get("/:reminderId", async (req, res) => {
    const reminderId = req.params.reminderId;

    if (!isUuid(reminderId)) {
        return res.status(400).json({
            message: "Invalid reminder ID."
        });
    }

    try {
        const reminder = await prisma.reminder.findFirst({
            where: {
                reminderId,
                userId: userIdOf(req)
            }
        });

        if (!reminder) {
            return res.status(404).json({
                message: "Reminder not found."
            });
        }

        return res.status(200).json({ reminder });
    } catch (error) {
        return serverError(
            res,
            error,
            "Unable to retrieve reminder."
        );
    }
});

router.post("/", async (req, res) => {
    const parsed = parseReminderBody(req.body, true);

    if (!parsed.data) {
        return res.status(400).json({
            message: parsed.error
        });
    }

    try {
        const userId = userIdOf(req);
        const data = parsed.data;

        if (data.holidayId) {
            const holiday = await prisma.holiday.findFirst({
                where: {
                    holidayId: data.holidayId,
                    userId
                },
                select: {
                    holidayId: true
                }
            });

            if (!holiday) {
                return res.status(404).json({
                    message: "Holiday not found."
                });
            }
        }

        const reminder = await prisma.reminder.create({
            data: {
                userId,
                holidayId: data.holidayId ?? null,
                title: data.title!,
                reminderDate: data.reminderDate!,
                reminderTime: data.reminderTime!,
                isEnabled: data.isEnabled ?? true
            }
        });

        return res.status(201).json({ reminder });
    } catch (error) {
        return serverError(
            res,
            error,
            "Unable to create reminder."
        );
    }
});

router.patch("/:reminderId", async (req, res) => {
    const reminderId = req.params.reminderId;

    if (!isUuid(reminderId)) {
        return res.status(400).json({
            message: "Invalid reminder ID."
        });
    }

    const parsed = parseReminderBody(req.body, false);

    if (!parsed.data) {
        return res.status(400).json({
            message: parsed.error
        });
    }

    try {
        const userId = userIdOf(req);
        const data = parsed.data;

        if (data.holidayId) {
            const holiday = await prisma.holiday.findFirst({
                where: {
                    holidayId: data.holidayId,
                    userId
                },
                select: {
                    holidayId: true
                }
            });

            if (!holiday) {
                return res.status(404).json({
                    message: "Holiday not found."
                });
            }
        }

        const where = {
            reminderId,
            userId
        };

        const result = await prisma.reminder.updateMany({
            where,
            data
        });

        if (result.count === 0) {
            return res.status(404).json({
                message: "Reminder not found."
            });
        }

        const reminder = await prisma.reminder.findFirst({
            where
        });

        return res.status(200).json({ reminder });
    } catch (error) {
        return serverError(
            res,
            error,
            "Unable to update reminder."
        );
    }
});

router.delete("/:reminderId", async (req, res) => {
    const reminderId = req.params.reminderId;

    if (!isUuid(reminderId)) {
        return res.status(400).json({
            message: "Invalid reminder ID."
        });
    }

    try {
        const result = await prisma.reminder.deleteMany({
            where: {
                reminderId,
                userId: userIdOf(req)
            }
        });

        if (result.count === 0) {
            return res.status(404).json({
                message: "Reminder not found."
            });
        }

        return res.status(200).json({
            message: "Reminder deleted successfully."
        });
    } catch (error) {
        return serverError(
            res,
            error,
            "Unable to delete reminder."
        );
    }
});

export default router;
