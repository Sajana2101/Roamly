
import { randomUUID } from "node:crypto";
import { Router } from "express";
import { getTravelNotesTable } from "../lib/tableStorage";
import { requireAuth } from "../middleware/requireAuth";
import { userIdOf } from "../lib/apiHelpers";

const router = Router();

router.use(requireAuth);

type NoteFields = {
    title: string;
    content: string;
    category: string;
    tagsJson: string;
    createdAt: string;
    updatedAt: string;
};

type NoteInput = {
    title: string;
    content: string;
    category: string;
    tags: string[];
};

const readText = (
    value: unknown,
    maxLength: number
): string | null => {
    if (typeof value !== "string") {
        return null;
    }

    const text = value.trim();

    if (!text || text.length > maxLength) {
        return null;
    }

    return text;
};

const validUuid = (value: unknown): value is string =>
    typeof value === "string" &&
    /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i.test(value);

const parseNoteInput = (
    value: unknown
): { data?: NoteInput; error?: string } => {
    if (
        !value ||
        typeof value !== "object" ||
        Array.isArray(value)
    ) {
        return {
            error: "A valid travel note object is required."
        };
    }

    const input = value as Record<string, unknown>;

    const title = readText(input.title, 120);
    const content = readText(input.content, 4000);

    if (!title) {
        return {
            error: "title must contain 1 to 120 characters."
        };
    }

    if (!content) {
        return {
            error: "content must contain 1 to 4000 characters."
        };
    }

    let category = "general";

    if (input.category !== undefined) {
        const parsedCategory = readText(input.category, 40);

        if (!parsedCategory) {
            return {
                error: "category must contain 1 to 40 characters."
            };
        }

        category = parsedCategory;
    }

    let tags: string[] = [];

    if (input.tags !== undefined) {
        if (
            !Array.isArray(input.tags) ||
            input.tags.length > 8
        ) {
            return {
                error: "tags must be an array of up to 8 strings."
            };
        }

        const parsedTags = input.tags.map(tag =>
            readText(tag, 32)
        );

        if (parsedTags.some(tag => tag === null)) {
            return {
                error: "Each tag must contain 1 to 32 characters."
            };
        }

        tags = parsedTags as string[];
    }

    return {
        data: {
            title,
            content,
            category,
            tags
        }
    };
};


const toNote = (
    entity: NoteFields & { rowKey?: string }
) => {
    if (!entity.rowKey) {
        throw new Error("Travel note ID is missing.");
    }

    return {
        noteId: entity.rowKey,
        title: entity.title,
        content: entity.content,
        category: entity.category,
        tags: JSON.parse(entity.tagsJson) as string[],
        createdAt: entity.createdAt,
        updatedAt: entity.updatedAt
    };
};


const errorStatus = (error: unknown): number | null => {
    if (
        error !== null &&
        typeof error === "object" &&
        "statusCode" in error &&
        typeof error.statusCode === "number"
    ) {
        return error.statusCode;
    }

    return null;
};

const storageError = (error: unknown) => {
    console.error("Travel notes storage error:", error);
};

router.post("/", async (req, res) => {
    const parsed = parseNoteInput(req.body);

    if (!parsed.data) {
        return res.status(400).json({
            message: parsed.error
        });
    }

    try {
        const userId = userIdOf(req);
        const noteId = randomUUID();
        const now = new Date().toISOString();
        const data = parsed.data;

        const entity = {
            partitionKey: userId,
            rowKey: noteId,
            title: data.title,
            content: data.content,
            category: data.category,
            tagsJson: JSON.stringify(data.tags),
            createdAt: now,
            updatedAt: now
        };

        await getTravelNotesTable().createEntity(entity);

        return res.status(201).json({
            note: toNote(entity)
        });
    } catch (error) {
        storageError(error);

        return res.status(503).json({
            message: "Travel notes storage is unavailable."
        });
    }
});

router.get("/", async (req, res) => {
    try {
        const userId = userIdOf(req);
        const client = getTravelNotesTable();
        const notes: ReturnType<typeof toNote>[] = [];

        const safeUserId = userId.replace(/'/g, "''");

        const entities = client.listEntities<NoteFields>({
            queryOptions: {
                filter: `PartitionKey eq '${safeUserId}'`
            }
        });

        for await (const entity of entities) {
            notes.push(toNote(entity));

            if (notes.length >= 100) {
                break;
            }
        }

        notes.sort((a, b) =>
            b.updatedAt.localeCompare(a.updatedAt)
        );

        return res.status(200).json({
            notes
        });
    } catch (error) {
        storageError(error);

        return res.status(503).json({
            message: "Travel notes storage is unavailable."
        });
    }
});

router.get("/:noteId", async (req, res) => {
    const noteId = req.params.noteId;

    if (!validUuid(noteId)) {
        return res.status(400).json({
            message: "Invalid travel note ID."
        });
    }

    try {
        const userId = userIdOf(req);

        const entity =
            await getTravelNotesTable().getEntity<NoteFields>(
                userId,
                noteId
            );

        return res.status(200).json({
            note: toNote(entity)
        });
    } catch (error) {
        if (errorStatus(error) === 404) {
            return res.status(404).json({
                message: "Travel note not found."
            });
        }

        storageError(error);

        return res.status(503).json({
            message: "Travel notes storage is unavailable."
        });
    }
});

router.put("/:noteId", async (req, res) => {
    const noteId = req.params.noteId;

    if (!validUuid(noteId)) {
        return res.status(400).json({
            message: "Invalid travel note ID."
        });
    }

    const parsed = parseNoteInput(req.body);

    if (!parsed.data) {
        return res.status(400).json({
            message: parsed.error
        });
    }

    try {
        const userId = userIdOf(req);
        const client = getTravelNotesTable();

        const existing =
            await client.getEntity<NoteFields>(
                userId,
                noteId
            );

        const data = parsed.data;

        const entity = {
            partitionKey: userId,
            rowKey: noteId,
            title: data.title,
            content: data.content,
            category: data.category,
            tagsJson: JSON.stringify(data.tags),
            createdAt: existing.createdAt,
            updatedAt: new Date().toISOString()
        };

        await client.updateEntity(entity, "Replace", {
            etag: existing.etag
        });

        return res.status(200).json({
            note: toNote(entity)
        });
    } catch (error) {
        if (errorStatus(error) === 404) {
            return res.status(404).json({
                message: "Travel note not found."
            });
        }

        if (errorStatus(error) === 412) {
            return res.status(409).json({
                message: "Travel note changed. Please reload and retry."
            });
        }

        storageError(error);

        return res.status(503).json({
            message: "Unable to update travel note."
        });
    }
});

router.delete("/:noteId", async (req, res) => {
    const noteId = req.params.noteId;

    if (!validUuid(noteId)) {
        return res.status(400).json({
            message: "Invalid travel note ID."
        });
    }

    try {
        const userId = userIdOf(req);
        const client = getTravelNotesTable();

        const existing = await client.getEntity<NoteFields>(
            userId,
            noteId
        );

        await client.deleteEntity(userId, noteId, {
            etag: existing.etag
        });

        return res.status(204).send();
    } catch (error) {
        if (errorStatus(error) === 404) {
            return res.status(404).json({
                message: "Travel note not found."
            });
        }

        if (errorStatus(error) === 412) {
            return res.status(409).json({
                message: "Travel note changed. Please reload and retry."
            });
        }

        storageError(error);

        return res.status(503).json({
            message: "Unable to delete travel note."
        });
    }
});

export default router;
