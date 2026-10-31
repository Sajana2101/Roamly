
import { Router } from "express";
import prisma from "../lib/prisma";
import { requireAuth } from "../middleware/requireAuth";
import {
    asRecord,
    cleanText,
    isUuid,
    nullableText,
    serverError,
    userIdOf
} from "../lib/apiHelpers";

const router = Router();

router.use(requireAuth);

router.get("/", async (req, res) => {
    try {
        const packingLists = await prisma.packingList.findMany({
            where: { userId: userIdOf(req) },
            include: {
                items: {
                    orderBy: { createdAt: "asc" }
                }
            },
            orderBy: { createdAt: "desc" }
        });

        return res.json({ packingLists });
    } catch (error) {
        return serverError(res, error, "Unable to list packing lists.");
    }
});

router.post("/", async (req, res) => {
    const body = asRecord(req.body);
    const name = cleanText(body?.name);

    if (!name) {
        return res.status(400).json({
            message: "A valid packing list name is required."
        });
    }

    const description = body?.description === undefined
        ? null
        : nullableText(body.description);

    if (body?.description !== undefined &&
        body.description !== null &&
        body.description !== "" &&
        description === null) {
        return res.status(400).json({
            message: "Invalid description."
        });
    }

    try {
        const packingList = await prisma.packingList.create({
            data: {
                userId: userIdOf(req),
                name,
                description
            },
            include: { items: true }
        });

        return res.status(201).json({ packingList });
    } catch (error) {
        return serverError(res, error, "Unable to create packing list.");
    }
});

router.get("/:listId", async (req, res) => {
    const packingListId = req.params.listId;

    if (!isUuid(packingListId)) {
        return res.status(400).json({ message: "Invalid packing list ID." });
    }

    try {
        const packingList = await prisma.packingList.findFirst({
            where: {
                packingListId,
                userId: userIdOf(req)
            },
            include: {
                items: {
                    orderBy: { createdAt: "asc" }
                }
            }
        });

        if (!packingList) {
            return res.status(404).json({ message: "Packing list not found." });
        }

        return res.json({ packingList });
    } catch (error) {
        return serverError(res, error, "Unable to retrieve packing list.");
    }
});

router.patch("/:listId", async (req, res) => {
    const packingListId = req.params.listId;
    const body = asRecord(req.body);

    if (!isUuid(packingListId)) {
        return res.status(400).json({ message: "Invalid packing list ID." });
    }

    if (!body || (body.name === undefined && body.description === undefined)) {
        return res.status(400).json({ message: "No fields to update." });
    }

    const data: {
        name?: string;
        description?: string | null;
    } = {};

    if (body.name !== undefined) {
        const name = cleanText(body.name);

        if (!name) {
            return res.status(400).json({ message: "Invalid name." });
        }

        data.name = name;
    }

    if (body.description !== undefined) {
        const description = nullableText(body.description);

        if (description === null &&
            body.description !== null &&
            body.description !== "") {
            return res.status(400).json({ message: "Invalid description." });
        }

        data.description = description;
    }

    try {
        const where = {
            packingListId,
            userId: userIdOf(req)
        };

        const result = await prisma.packingList.updateMany({
            where,
            data
        });

        if (!result.count) {
            return res.status(404).json({ message: "Packing list not found." });
        }

        const packingList = await prisma.packingList.findFirst({
            where,
            include: { items: true }
        });

        return res.json({ packingList });
    } catch (error) {
        return serverError(res, error, "Unable to update packing list.");
    }
});

router.delete("/:listId", async (req, res) => {
    const packingListId = req.params.listId;

    if (!isUuid(packingListId)) {
        return res.status(400).json({ message: "Invalid packing list ID." });
    }

    try {
        const result = await prisma.packingList.deleteMany({
            where: {
                packingListId,
                userId: userIdOf(req)
            }
        });

        if (!result.count) {
            return res.status(404).json({ message: "Packing list not found." });
        }

        return res.json({ message: "Packing list deleted successfully." });
    } catch (error) {
        return serverError(res, error, "Unable to delete packing list.");
    }
});

router.post("/:listId/items", async (req, res) => {
    const packingListId = req.params.listId;
    const body = asRecord(req.body);
    const name = cleanText(body?.name);

    if (!isUuid(packingListId) || !name) {
        return res.status(400).json({ message: "Invalid list ID or item name." });
    }

    if (body?.isPacked !== undefined &&
        typeof body.isPacked !== "boolean") {
        return res.status(400).json({ message: "isPacked must be boolean." });
    }

    try {
        const list = await prisma.packingList.findFirst({
            where: {
                packingListId,
                userId: userIdOf(req)
            },
            select: { packingListId: true }
        });

        if (!list) {
            return res.status(404).json({ message: "Packing list not found." });
        }

        const item = await prisma.packingItem.create({
            data: {
                packingListId,
                name,
                isPacked: (body?.isPacked as boolean | undefined) ?? false
            }
        });

        return res.status(201).json({ item });
    } catch (error) {
        return serverError(res, error, "Unable to create packing item.");
    }
});

router.patch("/items/:itemId", async (req, res) => {
    const packingItemId = req.params.itemId;
    const body = asRecord(req.body);

    if (!isUuid(packingItemId) || !body) {
        return res.status(400).json({ message: "Invalid item request." });
    }

    const data: {
        name?: string;
        isPacked?: boolean;
    } = {};

    if (body.name !== undefined) {
        const name = cleanText(body.name);

        if (!name) {
            return res.status(400).json({ message: "Invalid item name." });
        }

        data.name = name;
    }

    if (body.isPacked !== undefined) {
        if (typeof body.isPacked !== "boolean") {
            return res.status(400).json({ message: "isPacked must be boolean." });
        }

        data.isPacked = body.isPacked;
    }

    if (!Object.keys(data).length) {
        return res.status(400).json({ message: "No fields to update." });
    }

    try {
        const where = {
            packingItemId,
            packingList: {
                is: { userId: userIdOf(req) }
            }
        };

        const result = await prisma.packingItem.updateMany({
            where,
            data
        });

        if (!result.count) {
            return res.status(404).json({ message: "Packing item not found." });
        }

        const item = await prisma.packingItem.findFirst({ where });

        return res.json({ item });
    } catch (error) {
        return serverError(res, error, "Unable to update packing item.");
    }
});

router.delete("/items/:itemId", async (req, res) => {
    const packingItemId = req.params.itemId;

    if (!isUuid(packingItemId)) {
        return res.status(400).json({ message: "Invalid packing item ID." });
    }

    try {
        const result = await prisma.packingItem.deleteMany({
            where: {
                packingItemId,
                packingList: {
                    is: { userId: userIdOf(req) }
                }
            }
        });

        if (!result.count) {
            return res.status(404).json({ message: "Packing item not found." });
        }

        return res.json({ message: "Packing item deleted successfully." });
    } catch (error) {
        return serverError(res, error, "Unable to delete packing item.");
    }
});

export default router;
