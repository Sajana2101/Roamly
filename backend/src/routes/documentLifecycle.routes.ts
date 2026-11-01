
import { Router } from "express";
import prisma from "../lib/prisma";
import { getDocumentContainer } from "../lib/blobStorage";
import { requireAuth } from "../middleware/requireAuth";
import { isUuid, userIdOf } from "../lib/apiHelpers";

const router = Router();

router.use(requireAuth);

const maxFileSize = 5 * 1024 * 1024;

type StoredLocation =
    | { kind: "blob"; name: string }
    | { kind: "legacy" }
    | { kind: "invalid" };

const storedLocation = (
    fileUrl: string,
    userId: string
): StoredLocation => {
    const container = getDocumentContainer();

    try {
        const stored = new URL(fileUrl);
        const root = new URL(`${container.url.replace(/\/$/, "")}/`);

        if (
            stored.protocol !== "https:" ||
            stored.origin !== root.origin ||
            !stored.pathname.startsWith(root.pathname)
        ) {
            return { kind: "legacy" };
        }

        if (stored.search || stored.hash) {
            return { kind: "invalid" };
        }

        const name = decodeURIComponent(
            stored.pathname.slice(root.pathname.length)
        );

        const segments = name.split("/");

        if (
            segments.length !== 2 ||
            segments[0].toLowerCase() !== userId.toLowerCase() ||
            !/^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\.(pdf|png|jpg)$/i.test(
                segments[1]
            )
        ) {
            return { kind: "invalid" };
        }

        return {
            kind: "blob",
            name
        };
    } catch {
        return { kind: "invalid" };
    }
};

const contentTypeFor = (name: string): string => {
    if (/\.pdf$/i.test(name)) {
        return "application/pdf";
    }

    if (/\.png$/i.test(name)) {
        return "image/png";
    }

    if (/\.jpe?g$/i.test(name)) {
        return "image/jpeg";
    }

    return "application/octet-stream";
};

router.get("/:documentId/download", async (req, res) => {
    const documentId = req.params.documentId;

    if (!isUuid(documentId)) {
        return res.status(400).json({
            message: "Invalid document ID."
        });
    }

    try {
        const userId = userIdOf(req);

        const document = await prisma.travelDocument.findFirst({
            where: {
                documentId,
                userId
            }
        });

        if (!document) {
            return res.status(404).json({
                message: "Document not found."
            });
        }

        const location = storedLocation(document.fileUrl, userId);

        if (location.kind !== "blob") {
            return res.status(409).json({
                message: "This document has no accessible uploaded file."
            });
        }

        const container = getDocumentContainer();
        const blob = container.getBlobClient(location.name);
        const properties = await blob.getProperties();

        if (
            properties.contentLength === undefined ||
            properties.contentLength > maxFileSize
        ) {
            return res.status(413).json({
                message: "Document exceeds the download limit."
            });
        }

        const contents = await blob.downloadToBuffer();

        const filename =
            document.fileName
                .replace(/[^a-zA-Z0-9._-]/g, "_")
                .slice(0, 180) || "document";

        res.setHeader(
            "Content-Type",
            contentTypeFor(location.name)
        );

        res.setHeader(
            "Content-Disposition",
            `attachment; filename="${filename}"`
        );

        res.setHeader("Cache-Control", "private, no-store");
        res.setHeader("X-Content-Type-Options", "nosniff");
        res.setHeader("Content-Length", contents.length);

        return res.status(200).send(contents);
    } catch (error) {
        if (
            error !== null &&
            typeof error === "object" &&
            "statusCode" in error &&
            error.statusCode === 404
        ) {
            return res.status(404).json({
                message: "Uploaded file not found."
            });
        }

        console.error("Document download error:", error);

        return res.status(500).json({
            message: "Unable to download document."
        });
    }
});

router.patch("/:documentId", async (req, res, next) => {
    const documentId = req.params.documentId;

    if (!isUuid(documentId)) {
        return next();
    }

    const body = req.body;

    if (
        !body ||
        typeof body !== "object" ||
        Array.isArray(body)
    ) {
        return next();
    }

    const protectedFields = [
        "fileUrl",
        "fileName",
        "fileSizeBytes"
    ];

    if (
        !protectedFields.some(field =>
            Object.prototype.hasOwnProperty.call(body, field)
        )
    ) {
        return next();
    }

    try {
        const userId = userIdOf(req);

        const document = await prisma.travelDocument.findFirst({
            where: {
                documentId,
                userId
            }
        });

        if (!document) {
            return res.status(404).json({
                message: "Document not found."
            });
        }

        const location = storedLocation(document.fileUrl, userId);

        if (location.kind !== "legacy") {
            return res.status(409).json({
                message: "Uploaded file properties cannot be changed."
            });
        }

        return next();
    } catch (error) {
        console.error("Document update check error:", error);

        return res.status(500).json({
            message: "Unable to validate document update."
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
        const userId = userIdOf(req);

        const document = await prisma.travelDocument.findFirst({
            where: {
                documentId,
                userId
            }
        });

        if (!document) {
            return res.status(404).json({
                message: "Document not found."
            });
        }

        const location = storedLocation(document.fileUrl, userId);

        if (location.kind === "invalid") {
            return res.status(409).json({
                message: "Document file location requires review."
            });
        }

        if (location.kind === "blob") {
            const container = getDocumentContainer();

            const blob = container.getBlobClient(location.name);

            await blob.deleteIfExists({
                deleteSnapshots: "include"
            });
        }

        const result = await prisma.travelDocument.deleteMany({
            where: {
                documentId,
                userId
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
        console.error("Document deletion error:", error);

        return res.status(500).json({
            message: "Unable to delete document."
        });
    }
});

export default router;
