import "dotenv/config";
import { PrismaMssql } from "@prisma/adapter-mssql";
import { PrismaClient } from "../generated/prisma/client";

const getEnv = (name: string) => {
  const value = process.env[name];

  if (!value) {
    throw new Error(`${name} is not configured`);
  }

  return value;
};

const adapter = new PrismaMssql({
  server: getEnv("DB_HOST"),
  database: getEnv("DB_NAME"),
  user: getEnv("DB_USER"),
  password: getEnv("DB_PASSWORD"),
  options: {
    encrypt: true,
    trustServerCertificate: false,
  },
});

const prisma = new PrismaClient({ adapter });

export default prisma;