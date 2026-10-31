
import { Router } from "express";
import type { Request } from "express";
import prisma from "../lib/prisma";
import {
    requireAuth,
    type AuthenticatedRequest
} from "../middleware/requireAuth";

const router = Router();

router.use(requireAuth);

type FlightData = {
    flightType?: string;
    airline?: string;
    flightNumber?: string;
    departureAirport?: string;
    arrivalAirport?: string;
    flightDate?: Date;
    boardingTime?: Date | null;
    departureTime?: Date;
    arrivalTime?: Date;
    terminal?: string | null;
    gate?: string | null;
};

const uuidPattern =
    /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;

const requiredTextFields = [
    "flightType",
    "airline",
    "flightNumber",
    "departureAirport",
    "arrivalAirport"
] as const;

const requiredTimeFields = [
    "departureTime",
    "arrivalTime"
] as const;

const optionalTextFields = [
    "terminal",
    "gate"
] as const;

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
    if (
        typeof value !== "string" ||
        !/^([01]\d|2[0-3]):[0-5]\d(?::[0-5]\d)?$/.test(value)
    ) {
        return null;
    }

    const normalized = value.length === 5
        ? `${value}:00`
        : value;

    return new Date(`1970-01-01T${normalized}.000Z`);
};

const parseFlightBody = (
    body: unknown,
    creating: boolean
): { data?: FlightData; error?: string } => {
    if (
        body === null ||
        typeof body !== "object" ||
        Array.isArray(body)
    ) {
        return {
            error: "A valid flight object is required."
        };
    }

    const input = body as Record<string, unknown>;
    const data: FlightData = {};

    for (const field of requiredTextFields) {
        const value = input[field];

        if (creating || value !== undefined) {
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

    if (creating || input.flightDate !== undefined) {
        const date = parseDate(input.flightDate);

        if (!date) {
            return {
                error: "flightDate must use YYYY-MM-DD format."
            };
        }

        data.flightDate = date;
    }

    for (const field of requiredTimeFields) {
        if (creating || input[field] !== undefined) {
            const time = parseTime(input[field]);

            if (!time) {
                return {
                    error: `${field} must use HH:mm or HH:mm:ss format.`
                };
            }

            data[field] = time;
        }
    }

    if (input.boardingTime !== undefined) {
        if (
            input.boardingTime === null ||
            input.boardingTime === ""
        ) {
            data.boardingTime = null;
        } else {
            const time = parseTime(input.boardingTime);

            if (!time) {
                return {
                    error: "boardingTime must use HH:mm or HH:mm:ss format."
                };
            }

            data.boardingTime = time;
        }
    }

    for (const field of optionalTextFields) {
        const value = input[field];

        if (value !== undefined) {
            if (value === null || value === "") {
                data[field] = null;
            } else if (
                typeof value === "string" &&
                value.trim().length > 0 &&
                value.trim().length <= 200
            ) {
                data[field] = value.trim();
            } else {
                return {
                    error: `${field} must be text with at most 200 characters.`
                };
            }
        }
    }

    if (!creating && Object.keys(data).length === 0) {
        return {
            error: "Provide at least one flight field to update."
        };
    }

    return { data };
};

router.get("/", async (req, res) => {
    try {
        const flights = await prisma.flight.findMany({
            where: {
                holiday: {
                    is: {
                        userId: getUserId(req)
                    }
                }
            },
            orderBy: [
                { flightDate: "asc" },
                { departureTime: "asc" }
            ]
        });

        return res.status(200).json({ flights });
    } catch (error) {
        console.error("List flights error:", error);

        return res.status(500).json({
            message: "Unable to retrieve flights."
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

        const flights = await prisma.flight.findMany({
            where: {
                holidayId
            },
            orderBy: [
                { flightDate: "asc" },
                { departureTime: "asc" }
            ]
        });

        return res.status(200).json({ flights });
    } catch (error) {
        console.error("Get holiday flights error:", error);

        return res.status(500).json({
            message: "Unable to retrieve holiday flights."
        });
    }
});

router.get("/:flightId", async (req, res) => {
    const flightId = req.params.flightId;

    if (!isValidId(flightId)) {
        return res.status(400).json({
            message: "Invalid flight ID."
        });
    }

    try {
        const flight = await prisma.flight.findFirst({
            where: {
                flightId,
                holiday: {
                    is: {
                        userId: getUserId(req)
                    }
                }
            }
        });

        if (!flight) {
            return res.status(404).json({
                message: "Flight not found."
            });
        }

        return res.status(200).json({ flight });
    } catch (error) {
        console.error("Get flight error:", error);

        return res.status(500).json({
            message: "Unable to retrieve flight."
        });
    }
});

router.post("/", async (req, res) => {
    const parsed = parseFlightBody(req.body, true);

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

        const data = parsed.data;

        const flight = await prisma.flight.create({
            data: {
                holidayId,
                flightType: data.flightType!,
                airline: data.airline!,
                flightNumber: data.flightNumber!,
                departureAirport: data.departureAirport!,
                arrivalAirport: data.arrivalAirport!,
                flightDate: data.flightDate!,
                boardingTime: data.boardingTime ?? null,
                departureTime: data.departureTime!,
                arrivalTime: data.arrivalTime!,
                terminal: data.terminal ?? null,
                gate: data.gate ?? null
            }
        });

        return res.status(201).json({ flight });
    } catch (error) {
        console.error("Create flight error:", error);

        return res.status(500).json({
            message: "Unable to create flight."
        });
    }
});

router.patch("/:flightId", async (req, res) => {
    const flightId = req.params.flightId;

    if (!isValidId(flightId)) {
        return res.status(400).json({
            message: "Invalid flight ID."
        });
    }

    const parsed = parseFlightBody(req.body, false);

    if (parsed.error || !parsed.data) {
        return res.status(400).json({
            message: parsed.error
        });
    }

    try {
        const userId = getUserId(req);

        const result = await prisma.flight.updateMany({
            where: {
                flightId,
                holiday: {
                    is: {
                        userId
                    }
                }
            },
            data: parsed.data
        });

        if (result.count === 0) {
            return res.status(404).json({
                message: "Flight not found."
            });
        }

        const flight = await prisma.flight.findFirst({
            where: {
                flightId,
                holiday: {
                    is: {
                        userId
                    }
                }
            }
        });

        return res.status(200).json({ flight });
    } catch (error) {
        console.error("Update flight error:", error);

        return res.status(500).json({
            message: "Unable to update flight."
        });
    }
});

router.delete("/:flightId", async (req, res) => {
    const flightId = req.params.flightId;

    if (!isValidId(flightId)) {
        return res.status(400).json({
            message: "Invalid flight ID."
        });
    }

    try {
        const result = await prisma.flight.deleteMany({
            where: {
                flightId,
                holiday: {
                    is: {
                        userId: getUserId(req)
                    }
                }
            }
        });

        if (result.count === 0) {
            return res.status(404).json({
                message: "Flight not found."
            });
        }

        return res.status(200).json({
            message: "Flight deleted successfully."
        });
    } catch (error) {
        console.error("Delete flight error:", error);

        return res.status(500).json({
            message: "Unable to delete flight."
        });
    }
});

export default router;
