
import { Router } from "express";
import prisma from "../lib/prisma";
import { requireAuth } from "../middleware/requireAuth";
import {
    asRecord,
    cleanText,
    isUuid,
    serverError,
    userIdOf
} from "../lib/apiHelpers";

const router = Router();

router.use(requireAuth);

router.get("/", async (req, res) => {
    try {
        const checklists = await prisma.checklist.findMany({
            where: {
                userId: userIdOf(req)
            },
            include: {
                items: {
                    orderBy: {
                        createdAt: "asc"
                    }
                }
            },
            orderBy: {
                createdAt: "desc"
            }
        });

        return res.json({ checklists });
    } catch (error) {
        return serverError(
            res,
            error,
            "Unable to list checklists."
        );
    }
});

router.post("/", async (req, res) => {
    const body = asRecord(req.body);
    const name = cleanText(body?.name);

    if (!name) {
        return res.status(400).json({
            message: "A valid checklist name is required."
        });
    }

    try {
        const checklist = await prisma.checklist.create({
            data: {
                userId: userIdOf(req),
                name
            },
            include: {
                items: true
            }
        });

        return res.status(201).json({ checklist });
    } catch (error) {
        return serverError(
            res,
            error,
            "Unable to create checklist."
        );
    }
});

router.get("/:checklistId", async (req, res) => {
    const checklistId = req.params.checklistId;

    if (!isUuid(checklistId)) {
        return res.status(400).json({
            message: "Invalid checklist ID."
        });
    }

    try {
        const checklist = await prisma.checklist.findFirst({
            where: {
                checklistId,
                userId: userIdOf(req)
            },
            include: {
                items: {
                    orderBy: {
                        createdAt: "asc"
                    }
                }
            }
        });

        if (!checklist) {
            return res.status(404).json({
                message: "Checklist not found."
            });
        }

        return res.json({ checklist });
    } catch (error) {
        return serverError(
            res,
            error,
            "Unable to retrieve checklist."
        );
    }
});

router.patch("/:checklistId", async (req, res) => {
    const checklistId = req.params.checklistId;
    const body = asRecord(req.body);
    const name = cleanText(body?.name);

    if (!isUuid(checklistId) || !name) {
        return res.status(400).json({
            message: "Invalid checklist update."
        });
    }

    try {
        const where = {
            checklistId,
            userId: userIdOf(req)
        };

        const result = await prisma.checklist.updateMany({
            where,
            data: {
                name
            }
        });

        if (!result.count) {
            return res.status(404).json({
                message: "Checklist not found."
            });
        }

        const checklist = await prisma.checklist.findFirst({
            where,
            include: {
                items: true
            }
        });

        return res.json({ checklist });
    } catch (error) {
        return serverError(
            res,
            error,
            "Unable to update checklist."
        );
    }
});

router.delete("/:checklistId", async (req, res) => {
    const checklistId = req.params.checklistId;

    if (!isUuid(checklistId)) {
        return res.status(400).json({
            message: "Invalid checklist ID."
        });
    }

    try {
        const result = await prisma.checklist.deleteMany({
            where: {
                checklistId,
                userId: userIdOf(req)
            }
        });

        if (!result.count) {
            return res.status(404).json({
                message: "Checklist not found."
            });
        }

        return res.json({
            message: "Checklist deleted successfully."
        });
    } catch (error) {
        return serverError(
            res,
            error,
            "Unable to delete checklist."
        );
    }
});

router.post("/:checklistId/items", async (req, res) => {
    const checklistId = req.params.checklistId;
    const body = asRecord(req.body);
    const title = cleanText(body?.title);

    if (!isUuid(checklistId) || !title) {
        return res.status(400).json({
            message: "Invalid checklist item."
        });
    }

    if (
        body?.isCompleted !== undefined &&
        typeof body.isCompleted !== "boolean"
    ) {
        return res.status(400).json({
            message: "isCompleted must be boolean."
        });
    }

    try {
        const checklist = await prisma.checklist.findFirst({
            where: {
                checklistId,
                userId: userIdOf(req)
            },
            select: {
                checklistId: true
            }
        });

        if (!checklist) {
            return res.status(404).json({
                message: "Checklist not found."
            });
        }

        const item = await prisma.checklistItem.create({
            data: {
                checklistId,
                title,
                isCompleted:
                    (body?.isCompleted as boolean | undefined) ?? false
            }
        });

        return res.status(201).json({ item });
    } catch (error) {
        return serverError(
            res,
            error,
            "Unable to create checklist item."
        );
    }
});

router.patch("/items/:itemId", async (req, res) => {
    const checklistItemId = req.params.itemId;
    const body = asRecord(req.body);

    if (!isUuid(checklistItemId) || !body) {
        return res.status(400).json({
            message: "Invalid item update."
        });
    }

    const data: {
        title?: string;
        isCompleted?: boolean;
    } = {};

    if (body.title !== undefined) {
        const title = cleanText(body.title);

        if (!title) {
            return res.status(400).json({
                message: "Invalid item title."
            });
        }

        data.title = title;
    }

    if (body.isCompleted !== undefined) {
        if (typeof body.isCompleted !== "boolean") {
            return res.status(400).json({
                message: "isCompleted must be boolean."
            });
        }

        data.isCompleted = body.isCompleted;
    }

    if (Object.keys(data).length === 0) {
        return res.status(400).json({
            message: "No fields to update."
        });
    }

    try {
        const where = {
            checklistItemId,
            checklist: {
                is: {
                    userId: userIdOf(req)
                }
            }
        };

        const result = await prisma.checklistItem.updateMany({
            where,
            data
        });

        if (!result.count) {
            return res.status(404).json({
                message: "Checklist item not found."
            });
        }

        const item = await prisma.checklistItem.findFirst({
            where
        });

        return res.json({ item });
    } catch (error) {
        return serverError(
            res,
            error,
            "Unable to update checklist item."
        );
    }
});

router.delete("/items/:itemId", async (req, res) => {
    const checklistItemId = req.params.itemId;

    if (!isUuid(checklistItemId)) {
        return res.status(400).json({
            message: "Invalid checklist item ID."
        });
    }

    try {
        const result = await prisma.checklistItem.deleteMany({
            where: {
                checklistItemId,
                checklist: {
                    is: {
                        userId: userIdOf(req)
                    }
                }
            }
        });

        if (!result.count) {
            return res.status(404).json({
                message: "Checklist item not found."
            });
        }

        return res.json({
            message: "Checklist item deleted successfully."
        });
    } catch (error) {
        return serverError(
            res,
            error,
            "Unable to delete checklist item."
        );
    }
});

export default router;
