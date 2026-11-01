
package com.example.roamly.data.repository

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.example.roamly.data.auth.SessionManager
import com.example.roamly.data.model.TravelDocument
import com.example.roamly.data.sync.DocumentSyncWorker
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class DocumentRepository(context: Context) {

    private val appContext = context.applicationContext

    private val userId =
        SessionManager(appContext).getUserId()
            ?: throw IllegalStateException(
                "Sign in to access documents."
            )

    private val preferences =
        appContext.getSharedPreferences(
            "roamly_documents_$userId",
            Context.MODE_PRIVATE
        )

    private val syncPreferences =
        appContext.getSharedPreferences(
            "roamly_document_sync_$userId",
            Context.MODE_PRIVATE
        )

    private val gson = Gson()

    init {
        DocumentSyncWorker.request(appContext)
    }

    fun getDocuments(): MutableList<TravelDocument> {
        val json = preferences.getString(
            "documents",
            null
        ) ?: return mutableListOf()

        return try {
            val type =
                object :
                    TypeToken<MutableList<TravelDocument>>() {}
                    .type

            gson.fromJson<MutableList<TravelDocument>>(
                json,
                type
            ) ?: mutableListOf()
        } catch (_: Exception) {
            mutableListOf()
        }
    }

    fun addDocument(document: TravelDocument) {
        validate(document)
        preserveUriPermission(document.uri)

        val documents = getDocuments()

        if (documents.any { it.uri == document.uri }) {
            return
        }

        documents.add(document)
        save(documents)
    }

    fun updateDocument(document: TravelDocument) {
        validate(document)
        preserveUriPermission(document.uri)

        val documents = getDocuments()
        val index = documents.indexOfFirst {
            it.documentId == document.documentId
        }

        if (index < 0) return

        documents[index] = document
        save(documents)
    }

    fun deleteDocument(documentId: String) {
        val documents = getDocuments()

        if (documents.none { it.documentId == documentId }) {
            return
        }

        val serverId =
            syncPreferences.getString(
                "mapping_$documentId",
                null
            )

        if (serverId != null) {
            check(
                syncPreferences.edit()
                    .putString(
                        "delete_$documentId",
                        serverId
                    )
                    .remove("mapping_$documentId")
                    .remove("uploaded_uri_$documentId")
                    .commit()
            )
        }

        documents.removeAll {
            it.documentId == documentId
        }

        save(documents)
    }

    private fun preserveUriPermission(uriString: String) {
        val uri = Uri.parse(uriString)

        if (
            uri.scheme == "content" &&
            uri.authority !=
            "${appContext.packageName}.cloudfiles"
        ) {
            try {
                appContext.contentResolver
                    .takePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
            } catch (_: SecurityException) {
                checkNotNull(
                    appContext.contentResolver.openInputStream(uri)
                ).close()
            }
        }
    }

    private fun validate(document: TravelDocument) {
        val name =
            document.fileName ?: document.displayName

        val extension =
            name.substringAfterLast('.', "").lowercase()

        require(
            extension in setOf(
                "pdf",
                "png",
                "jpg",
                "jpeg"
            )
        ) {
            "Only PDF, PNG and JPEG documents can be uploaded to Azure."
        }

        require(
            document.sizeBytes in
                    0..(5L * 1024L * 1024L)
        ) {
            "Documents must be 5 MB or smaller."
        }
    }

    private fun save(documents: List<TravelDocument>) {
        check(
            preferences.edit()
                .putString(
                    "documents",
                    gson.toJson(documents)
                )
                .commit()
        ) {
            "Unable to save documents locally."
        }

        DocumentSyncWorker.request(appContext)
    }
}
