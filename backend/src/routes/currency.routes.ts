
import { Router } from "express";
import { requireAuth } from "../middleware/requireAuth";
import { asRecord } from "../lib/apiHelpers";

const router = Router();

router.use(requireAuth);

type ExchangeRate = {
    base: string;
    quote: string;
    date: string;
    rate: number;
    source: string;
};

type CachedRate = {
    value: ExchangeRate;
    expiresAt: number;
};

const rateCache = new Map<string, CachedRate>();
const cacheDuration = 15 * 60 * 1000;

class UnsupportedCurrencyError extends Error {}

const currencyCode = (value: unknown): string | null => {
    if (
        typeof value !== "string" ||
        !/^[A-Za-z]{3}$/.test(value)
    ) {
        return null;
    }

    return value.toUpperCase();
};

const readCurrencies = (
    query: Record<string, unknown>
): { base: string; quote: string } | null => {
    const base = currencyCode(query.from);
    const quote = currencyCode(query.to);

    if (!base || !quote) {
        return null;
    }

    return { base, quote };
};

const getExchangeRate = async (
    base: string,
    quote: string
): Promise<ExchangeRate> => {
    if (base === quote) {
        return {
            base,
            quote,
            date: new Date().toISOString().slice(0, 10),
            rate: 1,
            source: "Identity conversion"
        };
    }

    const key = `${base}:${quote}`;
    const cached = rateCache.get(key);

    if (cached && cached.expiresAt > Date.now()) {
        return cached.value;
    }

    const url =
        `https://api.frankfurter.dev/v2/rate/${base.toLowerCase()}/${quote.toLowerCase()}`;

    const response = await fetch(url, {
        headers: {
            Accept: "application/json"
        },
        signal: AbortSignal.timeout(10000)
    });

    if (
        response.status === 400 ||
        response.status === 404 ||
        response.status === 422
    ) {
        throw new UnsupportedCurrencyError(
            "Unsupported currency pair."
        );
    }

    if (!response.ok) {
        throw new Error(
            `Exchange-rate provider returned HTTP ${response.status}.`
        );
    }

    const payload: unknown = await response.json();
    const data = asRecord(payload);

    if (
        !data ||
        typeof data.rate !== "number" ||
        !Number.isFinite(data.rate) ||
        data.rate <= 0 ||
        typeof data.date !== "string" ||
        !/^\d{4}-\d{2}-\d{2}$/.test(data.date) ||
        data.base !== base ||
        data.quote !== quote
    ) {
        throw new Error("Invalid exchange-rate response.");
    }

    const value: ExchangeRate = {
        base,
        quote,
        date: data.date,
        rate: data.rate,
        source: "Frankfurter"
    };

    rateCache.set(key, {
        value,
        expiresAt: Date.now() + cacheDuration
    });

    return value;
};

const sendRateError = (
    res: import("express").Response,
    error: unknown
) => {
    if (error instanceof UnsupportedCurrencyError) {
        return res.status(400).json({
            message: error.message
        });
    }

    console.error("Currency provider error:", error);

    return res.status(503).json({
        message: "Exchange-rate service temporarily unavailable."
    });
};

router.get("/rate", async (req, res) => {
    const currencies = readCurrencies(req.query);

    if (!currencies) {
        return res.status(400).json({
            message: "Provide valid from and to currency codes."
        });
    }

    try {
        const result = await getExchangeRate(
            currencies.base,
            currencies.quote
        );

        return res.status(200).json(result);
    } catch (error) {
        return sendRateError(res, error);
    }
});

router.get("/convert", async (req, res) => {
    const currencies = readCurrencies(req.query);
    const amountText = req.query.amount;

    if (!currencies) {
        return res.status(400).json({
            message: "Provide valid from and to currency codes."
        });
    }

    if (
        typeof amountText !== "string" ||
        !/^\d{1,9}(\.\d{1,2})?$/.test(amountText)
    ) {
        return res.status(400).json({
            message: "Amount must be positive with at most two decimal places."
        });
    }

    const amount = Number(amountText);

    if (
        !Number.isFinite(amount) ||
        amount <= 0
    ) {
        return res.status(400).json({
            message: "Amount must be greater than zero."
        });
    }

    try {
        const result = await getExchangeRate(
            currencies.base,
            currencies.quote
        );

        const convertedAmount =
            Math.round(amount * result.rate * 100) / 100;

        if (!Number.isFinite(convertedAmount)) {
            return res.status(400).json({
                message: "Converted amount is too large."
            });
        }

        return res.status(200).json({
            ...result,
            amount,
            convertedAmount
        });
    } catch (error) {
        return sendRateError(res, error);
    }
});

export default router;
