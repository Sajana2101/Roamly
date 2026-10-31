
import { Router } from "express";
import type { Request } from "express";
import prisma from "../lib/prisma";
import {
    requireAuth,
    type AuthenticatedRequest
} from "../middleware/requireAuth";

const router = Router();

router.use(requireAuth);

type DocumentData = {
    name?: string;
    documentType?: string;
    expiryDate?: Date | null;
    fileName?: string;
    fileUrl?: string;
    fileSizeBytes?: bigint;
    notes?: string | null;
};

const uuidPattern =
    /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;

const maxFileSize = BigInt("9223372036854775807");

const isUuid = (value: unknown): value is string =>
    typeof value === "string" && uuidPattern.test(value);

const userIdOf = (req: Request): string =>
    (req as AuthenticatedRequest).authenticatedUserId!;

const asRecord = (
    value: unknown
): Record<string, unknown> | null =>
    value !== null &&
    typeof value === "object" &&
    !Array.isArray(value)
        ? value as Record<string, unknown>
        : null;

const cleanText = (
    value: unknown,
    maxLength = 200
): string | null => {
    if (typeof value !== "string") {
        return null;
    }

    const result = value.trim();

    return result.length > 0 && result.length <= maxLength
        ? result
        : null;
};

const parseDate = (value: unknown): Date | null => {
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

const parseSize = (value: unknown): bigint | null => {
    let result: bigint;

    if (
        typeof value === "number" &&
        Number.isSafeInteger(value) &&
        value >= 0
    ) {
        result = BigInt(value);
    } else if (
        typeof value === "string" &&
        /^\d+$/.test(value)
    ) {
        result = BigInt(value);
    } else {
        return null;
    }

    return result <= maxFileSize ? result : null;
};

const parseUrl = (value: unknown): string | null => {
    if (
        typeof value !== "string" ||
        value.length > 2048
    ) {
        return null;
    }

    try {
        const url = new URL(value);

        if (
            url.protocol !== "https:" ||
            !url.hostname ||
            url.username ||
            url.password ||
            url.searchParams.has("sig")
        ) {
            return null;
        }

        return url.toString();
    } catch {
        return null;
    }
};

const parseDocument = (
    value: unknown,
    creating: boolean
): { data?: DocumentData; error?: string } => {
    const body = asRecord(value);

    if (!body) {
        return {
            error: "A valid document object is required."
        };
    }

    const data: DocumentData = {};

    for (const field of ["name", "documentType"] as const) {
        if (creating || body[field] !== undefined) {
            const text = cleanText(body[field]);

            if (!text) {
                return {
                    error: `Invalid ${field}.`
                };
            }

            data[field] = text;
        }
    }

    if (creating || body.fileName !== undefined) {
        const fileName = cleanText(body.fileName, 255);

        if (
            !fileName ||
            /[\/\\\u0000-\u001f]/.test(fileName)
        ) {
            return {
                error: "A valid fileName is required."
            };
        }

        data.fileName = fileName;
    }

    if (creating || body.fileUrl !== undefined) {
        const fileUrl = parseUrl(body.fileUrl);

        if (!fileUrl) {
            return {
                error: "fileUrl must be a valid HTTPS URL without access tokens."
            };
        }

        data.fileUrl = fileUrl;
    }

    if (creating || body.fileSizeBytes !== undefined) {
        const fileSizeBytes = parseSize(body.fileSizeBytes);

        if (fileSizeBytes === null) {
            return {
                error: "fileSizeBytes must be a non-negative integer."
            };
        }

        data.fileSizeBytes = fileSizeBytes;
    }

    if (body.expiryDate !== undefined) {
        if (body.expiryDate === null || body.expiryDate === "") {
            data.expiryDate = null;
        } else {
            const date = parseDate(body.expiryDate);

            if (!date) {
                return {
                    error: "expiryDate must use YYYY-MM-DD format."
                };
            }

            data.expiryDate = date;
        }
    }

    if (body.notes !== undefined) {
        if (body.notes === null || body.notes === "") {
            data.notes = null;
        } else {
            const notes = cleanText(body.notes, 2000);

            if (!notes) {
                return {
                    error: "Invalid document notes."
                };
            }

            data.notes = notes;
        }
    }

    if (!creating && Object.keys(data).length === 0) {
        return {
            error: "Provide at least one field to update."
        };
    }

    return { data };
};

const serializeDocument = <T extends { fileSizeBytes: bigint }>(
    document: T
) => ({
    ...document,
    fileSizeBytes: document.fileSizeBytes.toString()
});

router.get("/", async (req, res) => {
    try {
        const documents = await prisma.travelDocument.findMany({
            where: {
                userId: userIdOf(req)
            },
            orderBy: {
                uploadedAt: "desc"
            }
        });

        return res.status(200).json({
            documents: documents.map(serializeDocument)
        });
    } catch (error) {
        console.error("List documents error:", error);

        return res.status(500).json({
            message: "Unable to retrieve documents."
        });
    }
});

router.get("/:documentId", async (req, res) => {
    const documentId = req.params.documentId;

    if (!isUuid(documentId)) {
        return res.status(400).json({
            message: "Invalid document ID."
        });
    }

    try {
        const document = await prisma.travelDocument.findFirst({
            where: {
                documentId,
                userId: userIdOf(req)
            }
        });

        if (!document) {
            return res.status(404).json({
                message: "Document not found."
            });
        }

        return res.status(200).json({
            document: serializeDocument(document)
        });
    } catch (error) {
        console.error("Get document error:", error);

        return res.status(500).json({
            message: "Unable to retrieve document."
        });
    }
});

router.post("/", async (req, res) => {
    const parsed = parseDocument(req.body, true);

    if (!parsed.data) {
        return res.status(400).json({
            message: parsed.error
        });
    }

    const data = parsed.data;

    try {
        const document = await prisma.travelDocument.create({
            data: {
                userId: userIdOf(req),
                name: data.name!,
                documentType: data.documentType!,
                expiryDate: data.expiryDate ?? null,
                fileName: data.fileName!,
                fileUrl: data.fileUrl!,
                fileSizeBytes: data.fileSizeBytes!,
                notes: data.notes ?? null
            }
        });

        return res.status(201).json({
            document: serializeDocument(document)
        });
    } catch (error) {
        console.error("Create document error:", error);

        return res.status(500).json({
            message: "Unable to create document."
        });
    }
});

router.patch("/:documentId", async (req, res) => {
    const documentId = req.params.documentId;

    if (!isUuid(documentId)) {
        return res.status(400).json({
            message: "Invalid document ID."
        });
    }

    const parsed = parseDocument(req.body, false);

    if (!parsed.data) {
        return res.status(400).json({
            message: parsed.error
        });
    }

    try {
        const where = {
            documentId,
            userId: userIdOf(req)
        };

        const result = await prisma.travelDocument.updateMany({
            where,
            data: parsed.data
        });

        if (result.count === 0) {
            return res.status(404).json({
                message: "Document not found."
            });
        }

        const document = await prisma.travelDocument.findFirst({
            where
        });

        if (!document) {
            return res.status(404).json({
                message: "Document not found."
            });
        }

        return res.status(200).json({
            document: serializeDocument(document)
        });
    } catch (error) {
        console.error("Update document error:", error);

        return res.status(500).json({
            message: "Unable to update document."
        });
    }
});

router.delete("/:documentId", async (req, res) => {
    const documentId = req.params.documentId;

    if (!isUuid(documentId)) {
        return res.status(400).json({
            message: "Invalid document ID."
        });
    }

    try {
        const result = await prisma.travelDocument.deleteMany({
            where: {
                documentId,
                userId: userIdOf(req)
            }
        });

        if (result.count === 0) {
            return res.status(404).json({
                message: "Document not found."
            });
        }

        return res.status(200).json({
            message: "Document deleted successfully."
        });
    } catch (error) {
        console.error("Delete document error:", error);

        return res.status(500).json({
            message: "Unable to delete document."
        });
    }
});

export default router;
