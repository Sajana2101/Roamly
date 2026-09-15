import cors from "cors";
import dotenv from "dotenv";
import express from "express";
import prisma from "./lib/prisma";
import accommodationsRouter from "./routes/accommodations.routes";
import authRouter from "./routes/auth.routes";
import checklistsRouter from "./routes/checklists.routes";
import currencyRouter from "./routes/currency.routes";
import documentsRouter from "./routes/documents.routes";
import flightsRouter from "./routes/flights.routes";
import holidaysRouter from "./routes/holidays.routes";
import itinerariesRouter from "./routes/itineraries.routes";
import packingRouter from "./routes/packing.routes";
import remindersRouter from "./routes/reminders.routes";
import settingsRouter from "./routes/settings.routes";
import usersRouter from "./routes/users.routes";

dotenv.config();

const app = express();

app.use(cors());
app.use(express.json());
app.use("/api/auth", authRouter);
app.use("/api/users", usersRouter);
app.use("/api/settings", settingsRouter);
app.use("/api/holidays", holidaysRouter);
app.use("/api/accommodations", accommodationsRouter);
app.use("/api/flights", flightsRouter);
app.use("/api/itineraries", itinerariesRouter);
app.use("/api/packing", packingRouter);
app.use("/api/checklists", checklistsRouter);
app.use("/api/documents", documentsRouter);
app.use("/api/reminders", remindersRouter);
app.use("/api/currency", currencyRouter);

app.get("/api/health", (_req, res) => {
  res.status(200).json({ status: "ok" });
});

app.get("/api/health/database", async (_req, res) => {
  try {
    await prisma.$queryRaw`SELECT 1`;
    res.status(200).json({
      status: "ok",
      database: "connected",
    });
  } catch (error) {
    console.error(error);

    res.status(500).json({
      status: "error",
      database: "disconnected",
    });
  }
});

export default app;