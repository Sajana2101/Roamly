
import { Router, type Request, type Response } from "express";
import prisma from "../lib/prisma";
import {
    requireAuth,
    type AuthenticatedRequest
} from "../middleware/requireAuth";

const router = Router();

router.use(requireAuth);

const uuidPattern =
    /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;

const isUuid = (value: unknown): value is string =>
    typeof value === "string" && uuidPattern.test(value);

const userIdOf = (req: Request) =>
    (req as AuthenticatedRequest).authenticatedUserId!;

const isObject = (
    value: unknown
): value is Record<string, unknown> =>
    value !== null &&
    typeof value === "object" &&
    !Array.isArray(value);

const textValue = (
    value: unknown,
    max = 200
): string | null =>
    typeof value === "string" &&
    value.trim().length > 0 &&
    value.trim().length <= max
        ? value.trim()
        : null;

const parseDate = (value: unknown): Date | null => {
    if (
        typeof value !== "string" ||
        !/^\d{4}-\d{2}-\d{2}$/.test(value)
    ) {
        return null;
    }

    const result = new Date(`${value}T00:00:00.000Z`);

    return !Number.isNaN(result.getTime()) &&
        result.toISOString().slice(0, 10) === value
        ? result
        : null;
};

const parseTime = (value: unknown): Date | null => {
    if (
        typeof value !== "string" ||
        !/^([01]\d|2[0-3]):[0-5]\d(?::[0-5]\d)?$/.test(value)
    ) {
        return null;
    }

    const normalized =
        value.length === 5 ? `${value}:00` : value;

    return new Date(
        `1970-01-01T${normalized}.000Z`
    );
};

const optionalText = (
    value: unknown,
    max = 2000
): string | null | undefined => {
    if (value === null || value === "") {
        return null;
    }

    if (value === undefined) {
        return undefined;
    }

    return textValue(value, max) ?? undefined;
};

const itineraryFilter = (
    itineraryId: string,
    userId: string
) => ({
    itineraryId,
    holiday: {
        is: {
            userId
        }
    }
});

const dayFilter = (
    itineraryDayId: string,
    userId: string
) => ({
    itineraryDayId,
    itinerary: {
        is: {
            holiday: {
                is: {
                    userId
                }
            }
        }
    }
});

const activityFilter = (
    activityId: string,
    userId: string
) => ({
    activityId,
    itineraryDay: {
        is: {
            itinerary: {
                is: {
                    holiday: {
                        is: {
                            userId
                        }
                    }
                }
            }
        }
    }
});

const itineraryInclude = {
    holiday: {
        select: {
            holidayId: true,
            title: true,
            startDate: true,
            endDate: true
        }
    },
    days: {
        orderBy: {
            dayNumber: "asc" as const
        },
        include: {
            activities: {
                orderBy: {
                    startTime: "asc" as const
                }
            }
        }
    }
};

const handleError = (
    res: Response,
    message: string,
    error: unknown
) => {
    console.error(message, error);

    return res.status(500).json({
        message
    });
};

router.get("/", async (req, res) => {
    try {
        const itineraries =
            await prisma.itinerary.findMany({
                where: {
                    holiday: {
                        is: {
                            userId: userIdOf(req)
                        }
                    }
                },
                include: itineraryInclude,
                orderBy: {
                    createdAt: "desc"
                }
            });

        return res.json({ itineraries });
    } catch (error) {
        return handleError(
            res,
            "Unable to list itineraries.",
            error
        );
    }
});

router.post("/", async (req, res) => {
    if (
        !isObject(req.body) ||
        !isUuid(req.body.holidayId)
    ) {
        return res.status(400).json({
            message: "A valid holidayId is required."
        });
    }

    const name = textValue(req.body.name);

    if (!name) {
        return res.status(400).json({
            message: "A valid itinerary name is required."
        });
    }

    try {
        const holiday = await prisma.holiday.findFirst({
            where: {
                holidayId: req.body.holidayId,
                userId: userIdOf(req)
            }
        });

        if (!holiday) {
            return res.status(404).json({
                message: "Holiday not found."
            });
        }

        const existing = await prisma.itinerary.findUnique({
            where: {
                holidayId: holiday.holidayId
            }
        });

        if (existing) {
            return res.status(409).json({
                message: "Holiday already has an itinerary."
            });
        }

        const itinerary = await prisma.itinerary.create({
            data: {
                holidayId: holiday.holidayId,
                name
            }
        });

        return res.status(201).json({ itinerary });
    } catch (error) {
        if (
            isObject(error) &&
            error.code === "P2002"
        ) {
            return res.status(409).json({
                message: "Holiday already has an itinerary."
            });
        }

        return handleError(
            res,
            "Unable to create itinerary.",
            error
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
        const itinerary = await prisma.itinerary.findFirst({
            where: {
                holidayId,
                holiday: {
                    is: {
                        userId: userIdOf(req)
                    }
                }
            },
            include: itineraryInclude
        });

        if (!itinerary) {
            return res.status(404).json({
                message: "Itinerary not found."
            });
        }

        return res.json({ itinerary });
    } catch (error) {
        return handleError(
            res,
            "Unable to retrieve itinerary.",
            error
        );
    }
});

router.get("/:itineraryId", async (req, res) => {
    const itineraryId = req.params.itineraryId;

    if (!isUuid(itineraryId)) {
        return res.status(400).json({
            message: "Invalid itinerary ID."
        });
    }

    try {
        const itinerary = await prisma.itinerary.findFirst({
            where: itineraryFilter(
                itineraryId,
                userIdOf(req)
            ),
            include: itineraryInclude
        });

        if (!itinerary) {
            return res.status(404).json({
                message: "Itinerary not found."
            });
        }

        return res.json({ itinerary });
    } catch (error) {
        return handleError(
            res,
            "Unable to retrieve itinerary.",
            error
        );
    }
});

router.patch("/:itineraryId", async (req, res) => {
    const itineraryId = req.params.itineraryId;

    if (!isUuid(itineraryId)) {
        return res.status(400).json({
            message: "Invalid itinerary ID."
        });
    }

    const name = isObject(req.body)
        ? textValue(req.body.name)
        : null;

    if (!name) {
        return res.status(400).json({
            message: "A valid itinerary name is required."
        });
    }

    try {
        const result = await prisma.itinerary.updateMany({
            where: itineraryFilter(
                itineraryId,
                userIdOf(req)
            ),
            data: {
                name
            }
        });

        if (!result.count) {
            return res.status(404).json({
                message: "Itinerary not found."
            });
        }

        const itinerary = await prisma.itinerary.findFirst({
            where: itineraryFilter(
                itineraryId,
                userIdOf(req)
            ),
            include: itineraryInclude
        });

        return res.json({ itinerary });
    } catch (error) {
        return handleError(
            res,
            "Unable to update itinerary.",
            error
        );
    }
});

router.delete("/:itineraryId", async (req, res) => {
    const itineraryId = req.params.itineraryId;

    if (!isUuid(itineraryId)) {
        return res.status(400).json({
            message: "Invalid itinerary ID."
        });
    }

    try {
        const result = await prisma.itinerary.deleteMany({
            where: itineraryFilter(
                itineraryId,
                userIdOf(req)
            )
        });

        if (!result.count) {
            return res.status(404).json({
                message: "Itinerary not found."
            });
        }

        return res.json({
            message: "Itinerary deleted successfully."
        });
    } catch (error) {
        return handleError(
            res,
            "Unable to delete itinerary.",
            error
        );
    }
});

router.post("/:itineraryId/days", async (req, res) => {
    const itineraryId = req.params.itineraryId;

    if (!isUuid(itineraryId)) {
        return res.status(400).json({
            message: "Invalid itinerary ID."
        });
    }

    if (!isObject(req.body)) {
        return res.status(400).json({
            message: "A valid day object is required."
        });
    }

    const dayNumber = req.body.dayNumber;
    const date = parseDate(req.body.date);

    if (
        typeof dayNumber !== "number" ||
        !Number.isInteger(dayNumber) ||
        dayNumber < 1 ||
        dayNumber > 366 ||
        !date
    ) {
        return res.status(400).json({
            message: "Valid dayNumber and date are required."
        });
    }

    try {
        const itinerary = await prisma.itinerary.findFirst({
            where: itineraryFilter(
                itineraryId,
                userIdOf(req)
            ),
            include: {
                holiday: {
                    select: {
                        startDate: true,
                        endDate: true
                    }
                }
            }
        });

        if (!itinerary) {
            return res.status(404).json({
                message: "Itinerary not found."
            });
        }

        const daysFromStart =
            Math.round(
                (date.getTime() -
                    itinerary.holiday.startDate.getTime()) /
                    86400000
            ) + 1;

        if (
            date < itinerary.holiday.startDate ||
            date > itinerary.holiday.endDate ||
            dayNumber !== daysFromStart
        ) {
            return res.status(400).json({
                message: "Day number and date must match the holiday dates."
            });
        }

        const existing = await prisma.itineraryDay.findFirst({
            where: {
                itineraryId,
                OR: [
                    { dayNumber },
                    { date }
                ]
            }
        });

        if (existing) {
            return res.status(409).json({
                message: "This itinerary day already exists."
            });
        }

        const day = await prisma.itineraryDay.create({
            data: {
                itineraryId,
                dayNumber,
                date
            }
        });

        return res.status(201).json({ day });
    } catch (error) {
        return handleError(
            res,
            "Unable to create itinerary day.",
            error
        );
    }
});

router.patch("/days/:dayId", async (req, res) => {
    const dayId = req.params.dayId;

    if (!isUuid(dayId)) {
        return res.status(400).json({
            message: "Invalid day ID."
        });
    }

    if (
        !isObject(req.body) ||
        (
            req.body.date === undefined &&
            req.body.dayNumber === undefined
        )
    ) {
        return res.status(400).json({
            message: "Provide dayNumber or date."
        });
    }

    if (
        req.body.dayNumber !== undefined &&
        (
            typeof req.body.dayNumber !== "number" ||
            !Number.isInteger(req.body.dayNumber) ||
            req.body.dayNumber < 1 ||
            req.body.dayNumber > 366
        )
    ) {
        return res.status(400).json({
            message: "Invalid day number."
        });
    }

    const parsedDate = req.body.date === undefined
        ? undefined
        : parseDate(req.body.date);

    if (parsedDate === null) {
        return res.status(400).json({
            message: "Invalid date."
        });
    }

    try {
        const current = await prisma.itineraryDay.findFirst({
            where: dayFilter(
                dayId,
                userIdOf(req)
            ),
            include: {
                itinerary: {
                    include: {
                        holiday: {
                            select: {
                                startDate: true,
                                endDate: true
                            }
                        }
                    }
                }
            }
        });

        if (!current) {
            return res.status(404).json({
                message: "Itinerary day not found."
            });
        }

        const date = parsedDate ?? current.date;

        const dayNumber =
            (req.body.dayNumber as number | undefined) ??
            current.dayNumber;

        const holiday = current.itinerary.holiday;

        const daysFromStart =
            Math.round(
                (date.getTime() -
                    holiday.startDate.getTime()) /
                    86400000
            ) + 1;

        if (
            date < holiday.startDate ||
            date > holiday.endDate ||
            dayNumber !== daysFromStart
        ) {
            return res.status(400).json({
                message: "Day number and date must match the holiday dates."
            });
        }

        const conflictingDay =
            await prisma.itineraryDay.findFirst({
                where: {
                    itineraryId: current.itineraryId,
                    NOT: {
                        itineraryDayId: dayId
                    },
                    OR: [
                        { dayNumber },
                        { date }
                    ]
                }
            });

        if (conflictingDay) {
            return res.status(409).json({
                message: "This itinerary day already exists."
            });
        }

        const result = await prisma.itineraryDay.updateMany({
            where: dayFilter(
                dayId,
                userIdOf(req)
            ),
            data: {
                dayNumber,
                date
            }
        });

        if (!result.count) {
            return res.status(404).json({
                message: "Itinerary day not found."
            });
        }

        const day = await prisma.itineraryDay.findFirst({
            where: dayFilter(
                dayId,
                userIdOf(req)
            )
        });

        return res.json({ day });
    } catch (error) {
        return handleError(
            res,
            "Unable to update itinerary day.",
            error
        );
    }
});

router.delete("/days/:dayId", async (req, res) => {
    const dayId = req.params.dayId;

    if (!isUuid(dayId)) {
        return res.status(400).json({
            message: "Invalid day ID."
        });
    }

    try {
        const result = await prisma.itineraryDay.deleteMany({
            where: dayFilter(
                dayId,
                userIdOf(req)
            )
        });

        if (!result.count) {
            return res.status(404).json({
                message: "Itinerary day not found."
            });
        }

        return res.json({
            message: "Itinerary day deleted successfully."
        });
    } catch (error) {
        return handleError(
            res,
            "Unable to delete itinerary day.",
            error
        );
    }
});

type ActivityData = {
    title?: string;
    startTime?: Date;
    endTime?: Date | null;
    location?: string | null;
    notes?: string | null;
};

const parseActivity = (
    value: unknown,
    partial: boolean
): { data?: ActivityData; error?: string } => {
    if (!isObject(value)) {
        return {
            error: "A valid activity object is required."
        };
    }

    const data: ActivityData = {};

    if (!partial || value.title !== undefined) {
        const title = textValue(value.title);

        if (!title) {
            return {
                error: "A valid activity title is required."
            };
        }

        data.title = title;
    }

    if (!partial || value.startTime !== undefined) {
        const startTime = parseTime(value.startTime);

        if (!startTime) {
            return {
                error: "startTime must use HH:mm format."
            };
        }

        data.startTime = startTime;
    }

    if (value.endTime !== undefined) {
        const endTime =
            value.endTime === null || value.endTime === ""
                ? null
                : parseTime(value.endTime);

        if (
            endTime === null &&
            value.endTime !== null &&
            value.endTime !== ""
        ) {
            return {
                error: "endTime must use HH:mm format."
            };
        }

        data.endTime = endTime;
    }

    for (const field of ["location", "notes"] as const) {
        if (value[field] !== undefined) {
            const fieldValue = optionalText(
                value[field],
                field === "location" ? 500 : 2000
            );

            if (fieldValue === undefined) {
                return {
                    error: `Invalid ${field}.`
                };
            }

            data[field] = fieldValue;
        }
    }

    if (partial && !Object.keys(data).length) {
        return {
            error: "Provide at least one activity field."
        };
    }

    if (
        data.startTime &&
        data.endTime &&
        data.endTime <= data.startTime
    ) {
        return {
            error: "Activity endTime must be later than startTime."
        };
    }

    return { data };
};

router.post("/days/:dayId/activities", async (req, res) => {
    const dayId = req.params.dayId;

    if (!isUuid(dayId)) {
        return res.status(400).json({
            message: "Invalid day ID."
        });
    }

    const parsed = parseActivity(req.body, false);

    if (!parsed.data) {
        return res.status(400).json({
            message: parsed.error
        });
    }

    try {
        const day = await prisma.itineraryDay.findFirst({
            where: dayFilter(
                dayId,
                userIdOf(req)
            )
        });

        if (!day) {
            return res.status(404).json({
                message: "Itinerary day not found."
            });
        }

        const activity =
            await prisma.itineraryActivity.create({
                data: {
                    itineraryDayId: dayId,
                    title: parsed.data.title!,
                    startTime: parsed.data.startTime!,
                    endTime: parsed.data.endTime ?? null,
                    location: parsed.data.location ?? null,
                    notes: parsed.data.notes ?? null
                }
            });

        return res.status(201).json({ activity });
    } catch (error) {
        return handleError(
            res,
            "Unable to create activity.",
            error
        );
    }
});

router.patch("/activities/:activityId", async (req, res) => {
    const activityId = req.params.activityId;

    if (!isUuid(activityId)) {
        return res.status(400).json({
            message: "Invalid activity ID."
        });
    }

    const parsed = parseActivity(req.body, true);

    if (!parsed.data) {
        return res.status(400).json({
            message: parsed.error
        });
    }

    try {
        const existing =
            await prisma.itineraryActivity.findFirst({
                where: activityFilter(
                    activityId,
                    userIdOf(req)
                )
            });

        if (!existing) {
            return res.status(404).json({
                message: "Activity not found."
            });
        }

        const startTime =
            parsed.data.startTime ?? existing.startTime;

        const endTime =
            parsed.data.endTime === undefined
                ? existing.endTime
                : parsed.data.endTime;

        if (endTime && endTime <= startTime) {
            return res.status(400).json({
                message: "Activity endTime must be later than startTime."
            });
        }

        const result =
            await prisma.itineraryActivity.updateMany({
                where: activityFilter(
                    activityId,
                    userIdOf(req)
                ),
                data: parsed.data
            });

        if (!result.count) {
            return res.status(404).json({
                message: "Activity not found."
            });
        }

        const activity =
            await prisma.itineraryActivity.findFirst({
                where: activityFilter(
                    activityId,
                    userIdOf(req)
                )
            });

        return res.json({ activity });
    } catch (error) {
        return handleError(
            res,
            "Unable to update activity.",
            error
        );
    }
});

router.delete("/activities/:activityId", async (req, res) => {
    const activityId = req.params.activityId;

    if (!isUuid(activityId)) {
        return res.status(400).json({
            message: "Invalid activity ID."
        });
    }

    try {
        const result =
            await prisma.itineraryActivity.deleteMany({
                where: activityFilter(
                    activityId,
                    userIdOf(req)
                )
            });

        if (!result.count) {
            return res.status(404).json({
                message: "Activity not found."
            });
        }

        return res.json({
            message: "Activity deleted successfully."
        });
    } catch (error) {
        return handleError(
            res,
            "Unable to delete activity.",
            error
        );
    }
});

export default router;
