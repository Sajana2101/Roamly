
package com.example.roamly.data.sync

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.core.content.FileProvider
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.roamly.data.auth.SessionManager
import com.example.roamly.data.model.TravelDocument
import com.example.roamly.data.network.ApiConfig
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.CancellationException
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.File

class DocumentSyncWorker(
    context: Context,
    parameters: WorkerParameters
) : CoroutineWorker(context, parameters) {

    private val sessions =
        SessionManager(applicationContext)

    private val gson = Gson()

    private val api = Retrofit.Builder()
        .baseUrl(ApiConfig.BASE_URL)
        .addConverterFactory(
            GsonConverterFactory.create()
        )
        .build()
        .create(DocumentSyncApi::class.java)

    override suspend fun doWork(): Result {
        val session =
            sessions.getSession() ?: return Result.success()

        val userId = session.userId
        val auth = "Bearer ${session.accessToken}"

        val preferences =
            applicationContext.getSharedPreferences(
                "roamly_documents_$userId",
                Context.MODE_PRIVATE
            )

        val sync =
            applicationContext.getSharedPreferences(
                "roamly_document_sync_$userId",
                Context.MODE_PRIVATE
            )

        return try {
            val remoteResponse = api.list(auth)

            check(remoteResponse.isSuccessful) {
                "Document list failed HTTP ${remoteResponse.code()}"
            }

            val remoteArray =
                remoteResponse.body()
                    ?.getAsJsonArray("documents")
                    ?: error("Azure document list missing")

            val remote =
                remoteArray.map { it.asJsonObject }

            val deletions = sync.all.keys.filter {
                it.startsWith("delete_")
            }

            val deletedServerIds =
                deletions.mapNotNull {
                    sync.getString(it, null)
                }.toSet()

            for (key in deletions) {
                val id =
                    sync.getString(key, null) ?: continue

                val response = api.delete(auth, id)

                check(
                    response.isSuccessful ||
                            response.code() == 404
                ) {
                    "Delete document failed HTTP ${response.code()}"
                }

                check(
                    sync.edit()
                        .remove(key)
                        .commit()
                )
            }

            val local = readLocal(preferences)

            for (document in local) {
                check(
                    sessions.getSession()?.userId == userId
                )

                val id = document.documentId

                val serverId =
                    sync.getString("mapping_$id", null)

                val uploadedUri =
                    sync.getString(
                        "uploaded_uri_$id",
                        null
                    )

                if (
                    serverId == null ||
                    document.uri != uploadedUri
                ) {
                    val uri = Uri.parse(document.uri)

                    val name =
                        document.fileName
                            ?: document.displayName

                    val extension =
                        name.substringAfterLast(
                            '.',
                            ""
                        ).lowercase()

                    require(
                        extension in setOf(
                            "pdf",
                            "png",
                            "jpg",
                            "jpeg"
                        )
                    ) {
                        "Unsupported document file type: $extension"
                    }

                    val bytes =
                        applicationContext.contentResolver
                            .openInputStream(uri)
                            ?.use { input ->
                                val content = input.readBytes()

                                require(
                                    content.size <= 5 * 1024 * 1024
                                ) {
                                    "Document exceeds Azure's 5 MB limit"
                                }

                                content
                            } ?: error(
                            "Cannot read $name. Select the document again."
                        )

                    val mime = when (extension) {
                        "pdf" -> "application/pdf"
                        "png" -> "image/png"
                        else -> "image/jpeg"
                    }

                    val fileBody = bytes.toRequestBody(
                        mime.toMediaTypeOrNull()
                    )

                    val part =
                        MultipartBody.Part.createFormData(
                            "file",
                            name,
                            fileBody
                        )

                    val textType =
                        "text/plain".toMediaTypeOrNull()

                    val result = api.upload(
                        auth,
                        part,
                        document.displayName
                            .toRequestBody(textType),
                        (document.documentType ?: "Other")
                            .toRequestBody(textType),
                        (document.notes ?: "")
                            .toRequestBody(textType)
                    )

                    check(result.isSuccessful) {
                        "Upload document failed HTTP ${result.code()}: ${
                            result.errorBody()
                                ?.string()
                                ?.take(200)
                        }"
                    }

                    val newId =
                        result.body()
                            ?.getAsJsonObject("document")
                            ?.get("documentId")
                            ?.asString
                            ?: error(
                                "Azure did not return documentId"
                            )

                    check(
                        sync.edit()
                            .putString(
                                "mapping_$id",
                                newId
                            )
                            .putString(
                                "uploaded_uri_$id",
                                document.uri
                            )
                            .putString(
                                "uploaded_details_$id",
                                details(document)
                            )
                            .commit()
                    )

                    if (
                        serverId != null &&
                        serverId != newId
                    ) {
                        val old = api.delete(
                            auth,
                            serverId
                        )

                        check(
                            old.isSuccessful ||
                                    old.code() == 404
                        )
                    }
                } else if (
                    details(document) !=
                    sync.getString(
                        "uploaded_details_$id",
                        null
                    )
                ) {
                    val patch = JsonObject().apply {
                        addProperty(
                            "name",
                            document.displayName
                        )
                        addProperty(
                            "documentType",
                            document.documentType ?: "Other"
                        )
                        addProperty(
                            "notes",
                            document.notes ?: ""
                        )
                    }

                    val updated =
                        api.update(
                            auth,
                            serverId,
                            patch
                        )

                    check(updated.isSuccessful) {
                        "Document update HTTP ${updated.code()}"
                    }

                    check(
                        sync.edit()
                            .putString(
                                "uploaded_details_$id",
                                details(document)
                            )
                            .commit()
                    )
                }
            }

            val knownServerIds =
                sync.all.filterKeys {
                    it.startsWith("mapping_")
                }.values.mapNotNull {
                    it as? String
                }.toSet()

            for (entry in remote) {
                if (
                    sessions.getSession()?.userId != userId
                ) {
                    return Result.success()
                }

                val serverId =
                    entry.get("documentId")
                        ?.asString ?: continue

                if (
                    serverId in knownServerIds ||
                    readLocal(preferences).any {
                        it.documentId == serverId
                    }
                ) {
                    continue
                }

                if (serverId in deletedServerIds) {
                    continue
                }

                val fileName =
                    entry.get("fileName")
                        ?.asString ?: continue

                val extension =
                    fileName.substringAfterLast(
                        '.',
                        ""
                    ).lowercase()

                if (
                    extension !in setOf(
                        "pdf",
                        "png",
                        "jpg",
                        "jpeg"
                    )
                ) {
                    continue
                }

                val response = api.download(
                    auth,
                    serverId
                )

                check(response.isSuccessful) {
                    "Download document HTTP ${response.code()}"
                }

                val body =
                    response.body()
                        ?: error("Document bytes missing")

                val folder = File(
                    applicationContext.filesDir,
                    "cloud_documents"
                )

                check(
                    folder.isDirectory ||
                            folder.mkdirs()
                )

                val file = File(
                    folder,
                    "$serverId.$extension"
                )

                body.use { content ->
                    content.byteStream().use { input ->
                        file.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }
                }

                val contentUri =
                    FileProvider.getUriForFile(
                        applicationContext,
                        "${applicationContext.packageName}.cloudfiles",
                        file
                    ).toString()

                val mimeType = when (extension) {
                    "pdf" -> "application/pdf"
                    "png" -> "image/png"
                    else -> "image/jpeg"
                }

                val restored = TravelDocument(
                    documentId = serverId,
                    displayName =
                        entry.get("name")?.asString
                            ?: fileName,
                    mimeType = mimeType,
                    uri = contentUri,
                    sizeBytes =
                        entry.get("fileSizeBytes")
                            ?.asString
                            ?.toLongOrNull()
                            ?: file.length(),
                    addedAt =
                        System.currentTimeMillis(),
                    documentType =
                        entry.get("documentType")
                            ?.asString ?: "Other",
                    fileName = fileName,
                    notes =
                        entry.get("notes")
                            ?.takeUnless {
                                it.isJsonNull
                            }
                            ?.asString
                )

                val current =
                    readLocal(preferences)

                check(
                    preferences.edit()
                        .putString(
                            "documents",
                            gson.toJson(
                                current + restored
                            )
                        )
                        .commit()
                )

                check(
                    sync.edit()
                        .putString(
                            "mapping_$serverId",
                            serverId
                        )
                        .putString(
                            "uploaded_uri_$serverId",
                            contentUri
                        )
                        .putString(
                            "uploaded_details_$serverId",
                            details(restored)
                        )
                        .commit()
                )
            }

            Result.success()
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            Log.e(
                "RoamlyDocumentSync",
                "Document synchronization failed",
                error
            )

            Result.retry()
        }
    }

    private fun details(
        document: TravelDocument
    ): String =
        listOf(
            document.displayName,
            document.documentType,
            document.notes
        ).joinToString("\n")

    private fun readLocal(
        preferences: android.content.SharedPreferences
    ): List<TravelDocument> {
        val json =
            preferences.getString(
                "documents",
                null
            ) ?: return emptyList()

        val type =
            object :
                TypeToken<List<TravelDocument>>() {}
                .type

        return gson.fromJson<List<TravelDocument>>(
            json,
            type
        ) ?: emptyList()
    }

    companion object {
        fun request(context: Context) {
            val constraints =
                Constraints.Builder()
                    .setRequiredNetworkType(
                        NetworkType.CONNECTED
                    )
                    .build()

            val work =
                OneTimeWorkRequestBuilder<DocumentSyncWorker>()
                    .setConstraints(constraints)
                    .build()

            WorkManager.getInstance(
                context.applicationContext
            ).enqueueUniqueWork(
                "roamly_document_sync",
                ExistingWorkPolicy.APPEND_OR_REPLACE,
                work
            )
        }
    }
}
