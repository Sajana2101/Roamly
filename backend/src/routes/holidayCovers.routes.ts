
import { randomUUID } from "node:crypto";
import { Router, type RequestHandler } from "express";
import multer, { MulterError } from "multer";
import { getDocumentContainer } from "../lib/blobStorage";
import prisma from "../lib/prisma";
import {
    requireAuth,
    type AuthenticatedRequest
} from "../middleware/requireAuth";

const router = Router();

router.use(requireAuth);

const uuidPattern =
    /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;

const upload = multer({
    storage: multer.memoryStorage(),
    limits: {
        fileSize: 5 * 1024 * 1024,
        files: 1
    }
});

const receiveCover: RequestHandler = (req, res, next) => {
    upload.single("file")(req, res, error => {
        if (error) {
            const large =
                error instanceof MulterError &&
                error.code === "LIMIT_FILE_SIZE";

            res.status(large ? 413 : 400).json({
                message: large
                    ? "Cover exceeds 5 MB."
                    : "Invalid cover upload."
            });
            return;
        }

        next();
    });
};

const blobPath = (
    urlString: string | null,
    userId: string,
    holidayId: string
): string | null => {
    if (!urlString) return null;

    try {
        const container = getDocumentContainer();
        const base = new URL(
            `${container.url.replace(/\/$/, "")}/`
        );
        const url = new URL(urlString);

        if (
            url.protocol !== "https:" ||
            url.origin !== base.origin ||
            url.search ||
            url.hash ||
            !url.pathname.startsWith(base.pathname)
        ) {
            return null;
        }

        const path = decodeURIComponent(
            url.pathname.slice(base.pathname.length)
        );
        const segments = path.split("/");

        if (
            segments.length !== 4 ||
            segments[0] !== "holiday-covers" ||
            segments[1] !== userId ||
            segments[2] !== holidayId ||
            !/^[0-9a-f-]{36}\.jpg$/i.test(segments[3])
        ) {
            return null;
        }

        return path;
    } catch {
        return null;
    }
};

router.post(
    "/:holidayId/cover",
    receiveCover,
    async (req, res) => {
        const holidayId = req.params.holidayId;

        if (
            typeof holidayId !== "string" ||
            !uuidPattern.test(holidayId)
        ) {
            return res.status(400).json({
                message: "Invalid holiday ID."
            });
        }

        const file = req.file;

        if (
            !file ||
            file.buffer.length < 3 ||
            file.buffer[0] !== 0xff ||
            file.buffer[1] !== 0xd8 ||
            file.buffer[2] !== 0xff
        ) {
            return res.status(400).json({
                message: "A JPEG cover is required."
            });
        }

        const userId =
            (req as AuthenticatedRequest)
                .authenticatedUserId!;

        let uploadedName: string | null = null;

        try {
            const existing =
                await prisma.holiday.findFirst({
                    where: {
                        holidayId,
                        userId
                    },
                    select: {
                        imageUrl: true
                    }
                });

            if (!existing) {
                return res.status(404).json({
                    message: "Holiday not found."
                });
            }

            const container = getDocumentContainer();

            const name =
                `holiday-covers/${userId}/${holidayId}/${randomUUID()}.jpg`;

            const blob = container.getBlockBlobClient(name);

            await blob.uploadData(file.buffer, {
                blobHTTPHeaders: {
                    blobContentType: "image/jpeg"
                }
            });

            uploadedName = name;

            const result =
                await prisma.holiday.updateMany({
                    where: {
                        holidayId,
                        userId
                    },
                    data: {
                        imageUrl: blob.url
                    }
                });

            if (result.count !== 1) {
                return res.status(404).json({
                    message: "Holiday not found."
                });
            }

            uploadedName = null;

            const previous = blobPath(
                existing.imageUrl,
                userId,
                holidayId
            );

            if (previous && previous !== name) {
                try {
                    await container
                        .getBlobClient(previous)
                        .deleteIfExists();
                } catch (error) {
                    console.error(
                        "Previous holiday cover cleanup failed:",
                        error
                    );
                }
            }

            return res.status(200).json({
                imageUrl: blob.url
            });
        } catch (error) {
            console.error(
                "Holiday cover upload failed:",
                error
            );

            return res.status(500).json({
                message: "Unable to upload holiday cover."
            });
        } finally {
            if (uploadedName) {
                try {
                    await getDocumentContainer()
                        .getBlobClient(uploadedName)
                        .deleteIfExists();
                } catch (error) {
                    console.error(
                        "Holiday cover cleanup failed:",
                        error
                    );
                }
            }
        }
    }
);

router.get("/:holidayId/cover", async (req, res) => {
    const holidayId = req.params.holidayId;

    if (
        typeof holidayId !== "string" ||
        !uuidPattern.test(holidayId)
    ) {
        return res.status(400).json({
            message: "Invalid holiday ID."
        });
    }

    const userId =
        (req as AuthenticatedRequest)
            .authenticatedUserId!;

    try {
        const holiday =
            await prisma.holiday.findFirst({
                where: {
                    holidayId,
                    userId
                },
                select: {
                    imageUrl: true
                }
            });

        if (!holiday) {
            return res.status(404).json({
                message: "Holiday not found."
            });
        }

        const path = blobPath(
            holiday.imageUrl,
            userId,
            holidayId
        );

        if (!path) {
            return res.status(404).json({
                message: "Cover not available."
            });
        }

        const download = await getDocumentContainer()
            .getBlobClient(path)
            .download();

        const stream = download.readableStreamBody;

        if (!stream) {
            return res.status(404).json({
                message: "Cover not available."
            });
        }

        res.setHeader("Content-Type", "image/jpeg");
        res.setHeader(
            "Cache-Control",
            "private, max-age=300"
        );

        stream.on("error", error => {
            console.error(
                "Holiday cover stream error:",
                error
            );
            res.destroy(error);
        });

        stream.pipe(res);
        return;
    } catch (error) {
        console.error(
            "Holiday cover download failed:",
            error
        );

        if (!res.headersSent) {
            return res.status(500).json({
                message: "Unable to load cover."
            });
        }

        res.destroy();
    }
});

export default router;
