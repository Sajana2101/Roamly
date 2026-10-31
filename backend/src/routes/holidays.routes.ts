
import { Router } from "express";
import type { Request } from "express";
import prisma from "../lib/prisma";
import {
    requireAuth,
    type AuthenticatedRequest
} from "../middleware/requireAuth";

const router = Router();

router.use(requireAuth);

type HolidayData = {
    title?: string;
    tripType?: string;
    country?: string;
    city?: string;
    startDate?: Date;
    endDate?: Date;
    imageUrl?: string | null;
};

const textFields = [
    "title",
    "tripType",
    "country",
    "city"
] as const;

const uuidPattern =
    /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;

const getUserId = (req: Request): string => {
    return (req as AuthenticatedRequest).authenticatedUserId!;
};

const parseDate = (value: unknown): Date | null => {
    if (
        typeof value !== "string" ||
        !/^\d{4}-\d{2}-\d{2}$/.test(value)
    ) {
        return null;
    }

    const date = new Date(`${value}T00:00:00.000Z`);

    if (
        Number.isNaN(date.getTime()) ||
        date.toISOString().slice(0, 10) !== value
    ) {
        return null;
    }

    return date;
};

const parseHolidayBody = (
    body: unknown,
    partial: boolean
): { data?: HolidayData; error?: string } => {
    if (
        body === null ||
        typeof body !== "object" ||
        Array.isArray(body)
    ) {
        return { error: "A valid holiday object is required." };
    }

    const input = body as Record<string, unknown>;
    const data: HolidayData = {};

    for (const field of textFields) {
        const value = input[field];

        if (!partial || value !== undefined) {
            if (
                typeof value !== "string" ||
                !value.trim() ||
                value.trim().length > 200
            ) {
                return {
                    error: `${field} must contain between 1 and 200 characters.`
                };
            }

            data[field] = value.trim();
        }
    }

    for (const field of ["startDate", "endDate"] as const) {
        if (!partial || input[field] !== undefined) {
            const date = parseDate(input[field]);

            if (!date) {
                return {
                    error: `${field} must be a valid date in YYYY-MM-DD format.`
                };
            }

            data[field] = date;
        }
    }

    if (input.imageUrl !== undefined) {
        if (input.imageUrl === null || input.imageUrl === "") {
            data.imageUrl = null;
        } else {
            if (
                typeof input.imageUrl !== "string" ||
                input.imageUrl.length > 2048
            ) {
                return { error: "imageUrl must be a valid HTTPS URL." };
            }

            try {
                const url = new URL(input.imageUrl);

                if (url.protocol !== "https:") {
                    return {
                        error: "imageUrl must use HTTPS."
                    };
                }

                data.imageUrl = url.toString();
            } catch {
                return {
                    error: "imageUrl must be a valid HTTPS URL."
                };
            }
        }
    }

    if (
        data.startDate &&
        data.endDate &&
        data.endDate < data.startDate
    ) {
        return {
            error: "endDate cannot be earlier than startDate."
        };
    }

    if (partial && Object.keys(data).length === 0) {
        return {
            error: "Provide at least one holiday field to update."
        };
    }

    return { data };
};

router.get("/", async (req, res) => {
    try {
        const holidays = await prisma.holiday.findMany({
            where: {
                userId: getUserId(req)
            },
            orderBy: {
                startDate: "asc"
            }
        });

        return res.status(200).json({ holidays });
    } catch (error) {
        console.error("List holidays error:", error);

        return res.status(500).json({
            message: "Unable to retrieve holidays."
        });
    }
});

router.get("/:holidayId", async (req, res) => {
    const holidayId = req.params.holidayId;

    if (
        typeof holidayId !== "string" ||
        !uuidPattern.test(holidayId)
    ) {
        return res.status(400).json({
            message: "Invalid holiday ID."
        });
    }

    try {
        const holiday = await prisma.holiday.findFirst({
            where: {
                holidayId,
                userId: getUserId(req)
            }
        });

        if (!holiday) {
            return res.status(404).json({
                message: "Holiday not found."
            });
        }

        return res.status(200).json({ holiday });
    } catch (error) {
        console.error("Get holiday error:", error);

        return res.status(500).json({
            message: "Unable to retrieve holiday."
        });
    }
});

router.post("/", async (req, res) => {
    const parsed = parseHolidayBody(req.body, false);

    if (parsed.error || !parsed.data) {
        return res.status(400).json({
            message: parsed.error
        });
    }

    const data = parsed.data;

    try {
        const holiday = await prisma.holiday.create({
            data: {
                userId: getUserId(req),
                title: data.title!,
                tripType: data.tripType!,
                country: data.country!,
                city: data.city!,
                startDate: data.startDate!,
                endDate: data.endDate!,
                imageUrl: data.imageUrl ?? null
            }
        });

        return res.status(201).json({ holiday });
    } catch (error) {
        console.error("Create holiday error:", error);

        return res.status(500).json({
            message: "Unable to create holiday."
        });
    }
});

router.patch("/:holidayId", async (req, res) => {
    const holidayId = req.params.holidayId;

    if (
        typeof holidayId !== "string" ||
        !uuidPattern.test(holidayId)
    ) {
        return res.status(400).json({
            message: "Invalid holiday ID."
        });
    }

    const parsed = parseHolidayBody(req.body, true);

    if (parsed.error || !parsed.data) {
        return res.status(400).json({
            message: parsed.error
        });
    }

    try {
        const userId = getUserId(req);

        const existing = await prisma.holiday.findFirst({
            where: {
                holidayId,
                userId
            }
        });

        if (!existing) {
            return res.status(404).json({
                message: "Holiday not found."
            });
        }

        const data = parsed.data;

        const startDate = data.startDate ?? existing.startDate;
        const endDate = data.endDate ?? existing.endDate;

        if (endDate < startDate) {
            return res.status(400).json({
                message: "endDate cannot be earlier than startDate."
            });
        }

        const result = await prisma.holiday.updateMany({
            where: {
                holidayId,
                userId
            },
            data
        });

        if (result.count === 0) {
            return res.status(404).json({
                message: "Holiday not found."
            });
        }

        const holiday = await prisma.holiday.findFirst({
            where: {
                holidayId,
                userId
            }
        });

        return res.status(200).json({ holiday });
    } catch (error) {
        console.error("Update holiday error:", error);

        return res.status(500).json({
            message: "Unable to update holiday."
        });
    }
});

router.delete("/:holidayId", async (req, res) => {
    const holidayId = req.params.holidayId;

    if (
        typeof holidayId !== "string" ||
        !uuidPattern.test(holidayId)
    ) {
        return res.status(400).json({
            message: "Invalid holiday ID."
        });
    }

    try {
        const userId = getUserId(req);

        const existing = await prisma.holiday.findFirst({
            where: {
                holidayId,
                userId
            },
            select: {
                holidayId: true
            }
        });

        if (!existing) {
            return res.status(404).json({
                message: "Holiday not found."
            });
        }

        const [, result] = await prisma.$transaction([
            prisma.reminder.deleteMany({
                where: {
                    holidayId,
                    userId
                }
            }),
            prisma.holiday.deleteMany({
                where: {
                    holidayId,
                    userId
                }
            })
        ]);

        if (result.count === 0) {
            return res.status(404).json({
                message: "Holiday not found."
            });
        }

        return res.status(200).json({
            message: "Holiday deleted successfully."
        });
    } catch (error) {
        console.error("Delete holiday error:", error);

        return res.status(500).json({
            message: "Unable to delete holiday."
        });
    }
});

export default router;
