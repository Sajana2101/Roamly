
import { DefaultAzureCredential } from "@azure/identity";
import {
    BlobServiceClient,
    type ContainerClient
} from "@azure/storage-blob";

let containerClient: ContainerClient | null = null;

export const getDocumentContainer = (): ContainerClient => {
    if (containerClient) {
        return containerClient;
    }

    const containerName =
        process.env.AZURE_STORAGE_CONTAINER_NAME;

    if (!containerName) {
        throw new Error("Storage container name is missing.");
    }

    const connectionString =
        process.env.AZURE_STORAGE_CONNECTION_STRING;

    let serviceClient: BlobServiceClient;

    if (connectionString) {
        serviceClient =
            BlobServiceClient.fromConnectionString(connectionString);
    } else {
        const accountName =
            process.env.AZURE_STORAGE_ACCOUNT_NAME;

        if (!accountName) {
            throw new Error("Storage account name is missing.");
        }

        serviceClient = new BlobServiceClient(
            `https://${accountName}.blob.core.windows.net`,
            new DefaultAzureCredential()
        );
    }

    containerClient =
        serviceClient.getContainerClient(containerName);

    return containerClient;
};
