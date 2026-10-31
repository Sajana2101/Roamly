
import type { Request, Response } from "express";
import type { AuthenticatedRequest } from "../middleware/requireAuth";

const uuidPattern =
    /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;

export const isUuid = (value: unknown): value is string =>
    typeof value === "string" && uuidPattern.test(value);

export const asRecord = (
    value: unknown
): Record<string, unknown> | null =>
    value !== null &&
    typeof value === "object" &&
    !Array.isArray(value)
        ? value as Record<string, unknown>
        : null;

export const userIdOf = (req: Request): string =>
    (req as AuthenticatedRequest).authenticatedUserId!;

export const cleanText = (
    value: unknown,
    maxLength = 200
): string | null => {
    if (typeof value !== "string") return null;

    const text = value.trim();

    return text.length >= 1 && text.length <= maxLength
        ? text
        : null;
};

export const nullableText = (
    value: unknown,
    maxLength = 2000
): string | null => {
    if (value === null || value === "") return null;
    return cleanText(value, maxLength);
};

export const parseDate = (value: unknown): Date | null => {
    if (
        typeof value !== "string" ||
        !/^\d{4}-\d{2}-\d{2}$/.test(value)
    ) {
        return null;
    }

    const date = new Date(`${value}T00:00:00.000Z`);

    return !Number.isNaN(date.getTime()) &&
        date.toISOString().slice(0, 10) === value
        ? date
        : null;
};

export const parseTime = (value: unknown): Date | null => {
    if (
        typeof value !== "string" ||
        !/^([01]\d|2[0-3]):[0-5]\d(?::[0-5]\d)?$/.test(value)
    ) {
        return null;
    }

    const normalized =
        value.length === 5 ? `${value}:00` : value;

    return new Date(`1970-01-01T${normalized}.000Z`);
};

export const serverError = (
    res: Response,
    error: unknown,
    message: string
) => {
    console.error(message, error);
    return res.status(500).json({ message });
};
