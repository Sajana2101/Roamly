package com.example.roamly.data.repository

import android.content.Context
import com.example.roamly.data.model.TravelDocument
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class DocumentRepository(
    context: Context
) {

    private val sharedPreferences =
        context.getSharedPreferences(
            "roamly_documents",
            Context.MODE_PRIVATE
        )

    private val gson =
        Gson()

    fun getDocuments():
            MutableList<TravelDocument> {

        val json =
            sharedPreferences.getString(
                DOCUMENTS_KEY,
                null
            ) ?: return mutableListOf()

        return try {

            val type =
                object :
                    TypeToken<MutableList<TravelDocument>>() {}
                    .type

            gson.fromJson(
                json,
                type
            )

        } catch (_: Exception) {

            mutableListOf()
        }
    }

    fun addDocument(
        document: TravelDocument
    ) {

        val documents =
            getDocuments()

        val alreadyExists =
            documents.any {
                it.uri == document.uri
            }

        if (!alreadyExists) {

            documents.add(
                document
            )

            saveDocuments(
                documents
            )
        }
    }

    fun deleteDocument(
        documentId: String
    ) {

        val documents =
            getDocuments()

        documents.removeAll {
            it.documentId ==
                    documentId
        }

        saveDocuments(
            documents
        )
    }

    private fun saveDocuments(
        documents:
        List<TravelDocument>
    ) {

        sharedPreferences
            .edit()
            .putString(
                DOCUMENTS_KEY,
                gson.toJson(
                    documents
                )
            )
            .apply()
    }

    companion object {

        private const val DOCUMENTS_KEY =
            "documents"
    }
}