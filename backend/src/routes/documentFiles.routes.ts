
import { randomUUID } from "node:crypto";
import { Router, type RequestHandler } from "express";
import multer, { MulterError } from "multer";
import prisma from "../lib/prisma";
import { getDocumentContainer } from "../lib/blobStorage";
import { requireAuth } from "../middleware/requireAuth";
import {
    cleanText,
    parseDate,
    userIdOf
} from "../lib/apiHelpers";

const router = Router();

router.use(requireAuth);

const maxFileSize = 5 * 1024 * 1024;

const upload = multer({
    storage: multer.memoryStorage(),
    limits: {
        fileSize: maxFileSize,
        files: 1,
        fields: 4
    }
});

const receiveFile: RequestHandler = (req, res, next) => {
    upload.single("file")(req, res, error => {
        if (error) {
            const tooLarge =
                error instanceof MulterError &&
                error.code === "LIMIT_FILE_SIZE";

            res.status(tooLarge ? 413 : 400).json({
                message: tooLarge
                    ? "Document exceeds the 5 MB upload limit."
                    : "Invalid document upload."
            });

            return;
        }

        next();
    });
};

const getFileType = (
    buffer: Buffer,
    filename: string
): {
    extension: string;
    contentType: string;
} | null => {
    if (
        /\.pdf$/i.test(filename) &&
        buffer.subarray(0, 5).toString("ascii") === "%PDF-"
    ) {
        return {
            extension: ".pdf",
            contentType: "application/pdf"
        };
    }

    if (
        /\.(jpg|jpeg)$/i.test(filename) &&
        buffer.length >= 3 &&
        buffer[0] === 0xff &&
        buffer[1] === 0xd8 &&
        buffer[2] === 0xff
    ) {
        return {
            extension: ".jpg",
            contentType: "image/jpeg"
        };
    }

    if (
        /\.png$/i.test(filename) &&
        buffer.length >= 8 &&
        buffer.subarray(0, 8).equals(
            Buffer.from([
                137, 80, 78, 71, 13, 10, 26, 10
            ])
        )
    ) {
        return {
            extension: ".png",
            contentType: "image/png"
        };
    }

    return null;
};

router.post("/upload", receiveFile, async (req, res) => {
    const file = req.file;

    if (!file || file.size === 0) {
        return res.status(400).json({
            message: "A document file is required."
        });
    }

    const fileName = file.originalname;

    if (
        !fileName ||
        fileName.length > 255 ||
        /[\/\\\u0000-\u001f]/.test(fileName)
    ) {
        return res.status(400).json({
            message: "Invalid document filename."
        });
    }

    const fileType = getFileType(file.buffer, fileName);

    if (!fileType) {
        return res.status(400).json({
            message: "Only valid PDF, PNG and JPEG files are supported."
        });
    }

    const name = cleanText(req.body?.name);
    const documentType = cleanText(req.body?.documentType);

    if (!name || !documentType) {
        return res.status(400).json({
            message: "Valid name and documentType fields are required."
        });
    }

    let expiryDate: Date | null = null;

    if (req.body.expiryDate) {
        expiryDate = parseDate(req.body.expiryDate);

        if (!expiryDate) {
            return res.status(400).json({
                message: "expiryDate must use YYYY-MM-DD format."
            });
        }
    }

    let notes: string | null = null;

    if (req.body.notes) {
        notes = cleanText(req.body.notes, 2000);

        if (!notes) {
            return res.status(400).json({
                message: "Invalid document notes."
            });
        }
    }

    const userId = userIdOf(req);

    const blobName =
        `${userId}/${randomUUID()}${fileType.extension}`;

    let uploaded = false;

    try {
        const container = getDocumentContainer();

        const blobClient =
            container.getBlockBlobClient(blobName);

        await blobClient.uploadData(file.buffer, {
            blobHTTPHeaders: {
                blobContentType: fileType.contentType
            }
        });

        uploaded = true;

        try {
            const document =
                await prisma.travelDocument.create({
                    data: {
                        userId,
                        name,
                        documentType,
                        expiryDate,
                        fileName,
                        fileUrl: blobClient.url,
                        fileSizeBytes: BigInt(file.size),
                        notes
                    }
                });

            return res.status(201).json({
                document: {
                    ...document,
                    fileSizeBytes:
                        document.fileSizeBytes.toString()
                }
            });
        } catch (databaseError) {
            try {
                await blobClient.deleteIfExists();
                uploaded = false;
            } catch (cleanupError) {
                console.error(
                    "Blob cleanup error:",
                    cleanupError
                );
            }

            throw databaseError;
        }
    } catch (error) {
        console.error(
            "Document upload error:",
            error
        );

        if (uploaded) {
            console.error(
                "Document Blob may require manual cleanup:",
                blobName
            );
        }

        return res.status(500).json({
            message: "Unable to upload document."
        });
    }
});

export default router;
