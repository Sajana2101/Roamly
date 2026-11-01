
import { TableClient } from "@azure/data-tables";

let tableClient: TableClient | null = null;

export const getTravelNotesTable = (): TableClient => {
    if (tableClient) {
        return tableClient;
    }

    const connectionString =
        process.env.AZURE_STORAGE_CONNECTION_STRING;

    const tableName =
        process.env.AZURE_TABLE_NAME;

    if (!connectionString) {
        throw new Error(
            "AZURE_STORAGE_CONNECTION_STRING is missing."
        );
    }

    if (
        !tableName ||
        !/^[A-Za-z][A-Za-z0-9]{2,62}$/.test(tableName)
    ) {
        throw new Error(
            "AZURE_TABLE_NAME is missing or invalid."
        );
    }

    tableClient = TableClient.fromConnectionString(
        connectionString,
        tableName
    );

    return tableClient;
};
