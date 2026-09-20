import { Router } from "express";
import bcrypt from "bcryptjs";
import jwt, { SignOptions } from "jsonwebtoken";
import { OAuth2Client } from "google-auth-library";
import prisma from "../lib/prisma";

const router = Router();

const googleClient = new OAuth2Client();

const emailPattern = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

const passwordPattern =
  /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d).{8,}$/;

const createToken = (
  userId: string,
  email: string
) => {
  const secret = process.env.JWT_SECRET;

  if (!secret) {
    throw new Error(
      "JWT_SECRET is not configured"
    );
  }

  const expiresIn =
    (process.env.JWT_EXPIRES_IN ||
      "7d") as SignOptions["expiresIn"];

  return jwt.sign(
    {
      userId,
      email,
    },
    secret,
    {
      expiresIn,
    }
  );
};

const toUserResponse = (user: {
  userId: string;
  name: string;
  email: string;
  ssoProvider: string | null;
  providerUserId: string | null;
  createdAt: Date;
  updatedAt: Date;
}) => {
  return {
    userId: user.userId,
    name: user.name,
    email: user.email,
    ssoProvider: user.ssoProvider,
    providerUserId: user.providerUserId,
    createdAt: user.createdAt.toISOString(),
    updatedAt: user.updatedAt.toISOString(),
  };
};

router.post(
  "/register",
  async (req, res) => {
    try {
      const {
        name,
        email,
        password,
        confirmPassword,
      } = req.body;

      if (
        !name ||
        !email ||
        !password ||
        !confirmPassword
      ) {
        return res.status(400).json({
          message:
            "All registration fields are required.",
        });
      }

      const cleanName =
        String(name).trim();

      const cleanEmail =
        String(email)
          .trim()
          .toLowerCase();

      if (
        cleanName.length < 2
      ) {
        return res.status(400).json({
          message:
            "Name must contain at least 2 characters.",
        });
      }

      if (
        !emailPattern.test(
          cleanEmail
        )
      ) {
        return res.status(400).json({
          message:
            "Enter a valid email address.",
        });
      }

      if (
        !passwordPattern.test(
          password
        )
      ) {
        return res.status(400).json({
          message:
            "Password must be at least 8 characters and contain an uppercase letter, lowercase letter and number.",
        });
      }

      if (
        password !==
        confirmPassword
      ) {
        return res.status(400).json({
          message:
            "Passwords do not match.",
        });
      }

      const existingUser =
        await prisma.user.findUnique({
          where: {
            email: cleanEmail,
          },
        });

      if (
        existingUser
      ) {
        return res.status(409).json({
          message:
            "An account with this email already exists.",
        });
      }

      const passwordHash =
        await bcrypt.hash(
          password,
          12
        );

      const user =
        await prisma.user.create({
          data: {
            name: cleanName,
            email: cleanEmail,
            passwordHash,
            ssoProvider: null,
            providerUserId: null,
          },
        });

      const accessToken =
        createToken(
          user.userId,
          user.email
        );

      return res
        .status(201)
        .json({
          accessToken,
          user:
            toUserResponse(
              user
            ),
        });
    } catch (error) {
      console.error(
        "Registration error:",
        error
      );

      return res.status(500).json({
        message:
          "An unexpected error occurred while registering.",
      });
    }
  }
);

router.post(
  "/login",
  async (req, res) => {
    try {
      const {
        email,
        password,
      } = req.body;

      if (
        !email ||
        !password
      ) {
        return res.status(400).json({
          message:
            "Email and password are required.",
        });
      }

      const cleanEmail =
        String(email)
          .trim()
          .toLowerCase();

      const user =
        await prisma.user.findUnique({
          where: {
            email:
              cleanEmail,
          },
        });

      if (
        !user ||
        !user.passwordHash
      ) {
        return res.status(401).json({
          message:
            "Invalid email or password.",
        });
      }

      const passwordMatches =
        await bcrypt.compare(
          password,
          user.passwordHash
        );

      if (
        !passwordMatches
      ) {
        return res.status(401).json({
          message:
            "Invalid email or password.",
        });
      }

      const accessToken =
        createToken(
          user.userId,
          user.email
        );

      return res
        .status(200)
        .json({
          accessToken,
          user:
            toUserResponse(
              user
            ),
        });
    } catch (error) {
      console.error(
        "Login error:",
        error
      );

      return res.status(500).json({
        message:
          "An unexpected error occurred while logging in.",
      });
    }
  }
);
// This verifies the Google ID token, finds or creates the corresponding user in Azure SQL, then issues Roamly's own JWT session token.
router.post(
  "/google",
  async (req, res) => {
    try {
      const {
        idToken,
      } = req.body;

      if (
        !idToken
      ) {
        return res.status(400).json({
          message:
            "Google ID token is required.",
        });
      }

      const googleClientId =
        process.env
          .GOOGLE_CLIENT_ID;

      if (
        !googleClientId
      ) {
        return res.status(500).json({
          message:
            "Google authentication is not configured.",
        });
      }

      const ticket =
        await googleClient
          .verifyIdToken({
            idToken,
            audience:
              googleClientId,
          });

      const payload =
        ticket.getPayload();

      if (
        !payload ||
        !payload.sub ||
        !payload.email ||
        !payload.email_verified
      ) {
        return res.status(401).json({
          message:
            "Google authentication failed.",
        });
      }

      const providerUserId =
        payload.sub;

      const email =
        payload.email
          .trim()
          .toLowerCase();

      const name =
        payload.name
          ?.trim() ||
        email.split("@")[0];

      let user =
        await prisma.user
          .findFirst({
            where: {
              ssoProvider:
                "google",
              providerUserId,
            },
          });

      if (
        !user
      ) {
        const userWithSameEmail =
          await prisma.user
            .findUnique({
              where: {
                email,
              },
            });

        if (
          userWithSameEmail
        ) {
          user =
            await prisma.user
              .update({
                where: {
                  userId:
                    userWithSameEmail
                      .userId,
                },
                data: {
                  ssoProvider:
                    "google",
                  providerUserId,
                  name:
                    userWithSameEmail
                      .name ||
                    name,
                },
              });
        } else {
          user =
            await prisma.user
              .create({
                data: {
                  name,
                  email,
                  passwordHash:
                    null,
                  ssoProvider:
                    "google",
                  providerUserId,
                },
              });
        }
      }

      const accessToken =
        createToken(
          user.userId,
          user.email
        );

      return res
        .status(200)
        .json({
          accessToken,
          user:
            toUserResponse(
              user
            ),
        });
    } catch (error) {
      console.error(
        "Google authentication error:",
        error
      );

      return res.status(401).json({
        message:
          "Google authentication failed.",
      });
    }
  }
);

export default router;