
import { Router } from "express";
import prisma from "../lib/prisma";
import {
    requireAuth,
    type AuthenticatedRequest
} from "../middleware/requireAuth";

const router = Router();

router.get("/me", requireAuth, async (req, res) => {
    const userId =
        (req as AuthenticatedRequest).authenticatedUserId;

    if (!userId) {
        res.status(401).json({
            message: "Authentication required."
        });
        return;
    }

    try {
        const user = await prisma.user.findUnique({
            where: {
                userId
            },
            select: {
                userId: true,
                name: true,
                email: true,
                ssoProvider: true,
                createdAt: true,
                updatedAt: true
            }
        });

        if (!user) {
            res.status(404).json({
                message: "User not found."
            });
            return;
        }

        res.status(200).json({
            user
        });
    } catch (error) {
        console.error("User profile error:", error);

        res.status(500).json({
            message: "Unable to retrieve user profile."
        });
    }
});

export default router;
