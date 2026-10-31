
import { Router } from "express";
import type { Request } from "express";
import prisma from "../lib/prisma";
import {
    requireAuth,
    type AuthenticatedRequest
} from "../middleware/requireAuth";

const router = Router();

router.use(requireAuth);

type AccommodationData = {
    name?: string;
    type?: string;
    address?: string;
    checkInDate?: Date;
    checkInTime?: Date;
    checkOutDate?: Date;
    checkOutTime?: Date;
    bookingReference?: string | null;
    notes?: string | null;
};

const uuidPattern =
    /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;

const textFields = ["name", "type", "address"] as const;

const dateFields = ["checkInDate", "checkOutDate"] as const;

const timeFields = ["checkInTime", "checkOutTime"] as const;

const optionalFields = ["bookingReference", "notes"] as const;

const getUserId = (req: Request): string => {
    return (req as AuthenticatedRequest).authenticatedUserId!;
};

const isValidId = (value: unknown): value is string => {
    return typeof value === "string" && uuidPattern.test(value);
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

const parseTime = (value: unknown): Date | null => {
    if (typeof value !== "string") {
        return null;
    }

    const match = value.match(
        /^([01]\d|2[0-3]):([0-5]\d)(?::([0-5]\d))?$/
    );

    if (!match) {
        return null;
    }

    const hours = match[1];
    const minutes = match[2];
    const seconds = match[3] ?? "00";

    return new Date(
        `1970-01-01T${hours}:${minutes}:${seconds}.000Z`
    );
};

const isValidDateRange = (
    checkInDate: Date,
    checkInTime: Date,
    checkOutDate: Date,
    checkOutTime: Date
): boolean => {
    const startDate = checkInDate.getTime();
    const endDate = checkOutDate.getTime();

    if (endDate < startDate) {
        return false;
    }

    if (endDate === startDate) {
        return checkOutTime.getTime() > checkInTime.getTime();
    }

    return true;
};

const parseAccommodationBody = (
    body: unknown,
    partial: boolean
): { data?: AccommodationData; error?: string } => {
    if (
        body === null ||
        typeof body !== "object" ||
        Array.isArray(body)
    ) {
        return {
            error: "A valid accommodation object is required."
        };
    }

    const input = body as Record<string, unknown>;
    const data: AccommodationData = {};

    for (const field of textFields) {
        const value = input[field];

        if (!partial || value !== undefined) {
            const maxLength = field === "address" ? 500 : 200;

            if (
                typeof value !== "string" ||
                !value.trim() ||
                value.trim().length > maxLength
            ) {
                return {
                    error: `${field} must contain between 1 and ${maxLength} characters.`
                };
            }

            data[field] = value.trim();
        }
    }

    for (const field of dateFields) {
        if (!partial || input[field] !== undefined) {
            const date = parseDate(input[field]);

            if (!date) {
                return {
                    error: `${field} must use YYYY-MM-DD format.`
                };
            }

            data[field] = date;
        }
    }

    for (const field of timeFields) {
        if (!partial || input[field] !== undefined) {
            const time = parseTime(input[field]);

            if (!time) {
                return {
                    error: `${field} must use HH:mm or HH:mm:ss format.`
                };
            }

            data[field] = time;
        }
    }

    for (const field of optionalFields) {
        const value = input[field];

        if (value !== undefined) {
            if (value === null || value === "") {
                data[field] = null;
            } else if (
                typeof value === "string" &&
                value.trim().length <= 2000
            ) {
                data[field] = value.trim();
            } else {
                return {
                    error: `${field} must be text with at most 2000 characters.`
                };
            }
        }
    }

    if (partial && Object.keys(data).length === 0) {
        return {
            error: "Provide at least one accommodation field to update."
        };
    }

    if (
        data.checkInDate &&
        data.checkInTime &&
        data.checkOutDate &&
        data.checkOutTime &&
        !isValidDateRange(
            data.checkInDate,
            data.checkInTime,
            data.checkOutDate,
            data.checkOutTime
        )
    ) {
        return {
            error: "Check-out must be later than check-in."
        };
    }

    return { data };
};

router.get("/", async (req, res) => {
    try {
        const accommodations = await prisma.accommodation.findMany({
            where: {
                holiday: {
                    is: {
                        userId: getUserId(req)
                    }
                }
            },
            orderBy: {
                checkInDate: "asc"
            }
        });

        return res.status(200).json({ accommodations });
    } catch (error) {
        console.error("List accommodations error:", error);

        return res.status(500).json({
            message: "Unable to retrieve accommodations."
        });
    }
});

router.get("/holiday/:holidayId", async (req, res) => {
    const holidayId = req.params.holidayId;

    if (!isValidId(holidayId)) {
        return res.status(400).json({
            message: "Invalid holiday ID."
        });
    }

    try {
        const accommodation = await prisma.accommodation.findFirst({
            where: {
                holidayId,
                holiday: {
                    is: {
                        userId: getUserId(req)
                    }
                }
            }
        });

        if (!accommodation) {
            return res.status(404).json({
                message: "Accommodation not found."
            });
        }

        return res.status(200).json({ accommodation });
    } catch (error) {
        console.error("Get accommodation by holiday error:", error);

        return res.status(500).json({
            message: "Unable to retrieve accommodation."
        });
    }
});

router.get("/:accommodationId", async (req, res) => {
    const accommodationId = req.params.accommodationId;

    if (!isValidId(accommodationId)) {
        return res.status(400).json({
            message: "Invalid accommodation ID."
        });
    }

    try {
        const accommodation = await prisma.accommodation.findFirst({
            where: {
                accommodationId,
                holiday: {
                    is: {
                        userId: getUserId(req)
                    }
                }
            }
        });

        if (!accommodation) {
            return res.status(404).json({
                message: "Accommodation not found."
            });
        }

        return res.status(200).json({ accommodation });
    } catch (error) {
        console.error("Get accommodation error:", error);

        return res.status(500).json({
            message: "Unable to retrieve accommodation."
        });
    }
});

router.post("/", async (req, res) => {
    const parsed = parseAccommodationBody(req.body, false);

    if (parsed.error || !parsed.data) {
        return res.status(400).json({
            message: parsed.error
        });
    }

    const holidayId = req.body.holidayId;

    if (!isValidId(holidayId)) {
        return res.status(400).json({
            message: "A valid holidayId is required."
        });
    }

    try {
        const holiday = await prisma.holiday.findFirst({
            where: {
                holidayId,
                userId: getUserId(req)
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

        const existing = await prisma.accommodation.findUnique({
            where: {
                holidayId
            }
        });

        if (existing) {
            return res.status(409).json({
                message: "This holiday already has accommodation."
            });
        }

        const data = parsed.data;

        const accommodation = await prisma.accommodation.create({
            data: {
                holidayId,
                name: data.name!,
                type: data.type!,
                address: data.address!,
                checkInDate: data.checkInDate!,
                checkInTime: data.checkInTime!,
                checkOutDate: data.checkOutDate!,
                checkOutTime: data.checkOutTime!,
                bookingReference: data.bookingReference ?? null,
                notes: data.notes ?? null
            }
        });

        return res.status(201).json({ accommodation });
    } catch (error) {
        console.error("Create accommodation error:", error);

        if (
            error !== null &&
            typeof error === "object" &&
            "code" in error &&
            error.code === "P2002"
        ) {
            return res.status(409).json({
                message: "This holiday already has accommodation."
            });
        }

        return res.status(500).json({
            message: "Unable to create accommodation."
        });
    }
});

router.patch("/:accommodationId", async (req, res) => {
    const accommodationId = req.params.accommodationId;

    if (!isValidId(accommodationId)) {
        return res.status(400).json({
            message: "Invalid accommodation ID."
        });
    }

    const parsed = parseAccommodationBody(req.body, true);

    if (parsed.error || !parsed.data) {
        return res.status(400).json({
            message: parsed.error
        });
    }

    try {
        const userId = getUserId(req);

        const existing = await prisma.accommodation.findFirst({
            where: {
                accommodationId,
                holiday: {
                    is: {
                        userId
                    }
                }
            }
        });

        if (!existing) {
            return res.status(404).json({
                message: "Accommodation not found."
            });
        }

        const data = parsed.data;

        const checkInDate = data.checkInDate ?? existing.checkInDate;
        const checkInTime = data.checkInTime ?? existing.checkInTime;
        const checkOutDate = data.checkOutDate ?? existing.checkOutDate;
        const checkOutTime = data.checkOutTime ?? existing.checkOutTime;

        if (
            !isValidDateRange(
                checkInDate,
                checkInTime,
                checkOutDate,
                checkOutTime
            )
        ) {
            return res.status(400).json({
                message: "Check-out must be later than check-in."
            });
        }

        const result = await prisma.accommodation.updateMany({
            where: {
                accommodationId,
                holiday: {
                    is: {
                        userId
                    }
                }
            },
            data
        });

        if (result.count === 0) {
            return res.status(404).json({
                message: "Accommodation not found."
            });
        }

        const accommodation = await prisma.accommodation.findFirst({
            where: {
                accommodationId,
                holiday: {
                    is: {
                        userId
                    }
                }
            }
        });

        return res.status(200).json({ accommodation });
    } catch (error) {
        console.error("Update accommodation error:", error);

        return res.status(500).json({
            message: "Unable to update accommodation."
        });
    }
});

router.delete("/:accommodationId", async (req, res) => {
    const accommodationId = req.params.accommodationId;

    if (!isValidId(accommodationId)) {
        return res.status(400).json({
            message: "Invalid accommodation ID."
        });
    }

    try {
        const result = await prisma.accommodation.deleteMany({
            where: {
                accommodationId,
                holiday: {
                    is: {
                        userId: getUserId(req)
                    }
                }
            }
        });

        if (result.count === 0) {
            return res.status(404).json({
                message: "Accommodation not found."
            });
        }

        return res.status(200).json({
            message: "Accommodation deleted successfully."
        });
    } catch (error) {
        console.error("Delete accommodation error:", error);

        return res.status(500).json({
            message: "Unable to delete accommodation."
        });
    }
});

export default router;
