
import type { Request, RequestHandler } from "express";
import jwt from "jsonwebtoken";

export type AuthenticatedRequest = Request & {
    authenticatedUserId?: string;
};

export const requireAuth: RequestHandler = (req, res, next) => {
    const authorization = req.headers.authorization;

    if (!authorization?.startsWith("Bearer ")) {
        res.status(401).json({
            message: "Authentication required."
        });
        return;
    }

    const token = authorization.slice(7).trim();
    const secret = process.env.JWT_SECRET;

    if (!secret) {
        res.status(500).json({
            message: "Authentication is not configured."
        });
        return;
    }

    try {
        const payload = jwt.verify(token, secret);

        if (
            typeof payload === "string" ||
            typeof payload.userId !== "string" ||
            !payload.userId
        ) {
            res.status(401).json({
                message: "Invalid authentication token."
            });
            return;
        }

        (req as AuthenticatedRequest).authenticatedUserId =
            payload.userId;

        next();
    } catch {
        res.status(401).json({
            message: "Invalid or expired authentication token."
        });
    }
};
