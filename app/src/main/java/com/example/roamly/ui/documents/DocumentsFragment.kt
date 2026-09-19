package com.example.roamly.ui.documents

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.util.Log
import android.view.View
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.roamly.R
import com.example.roamly.data.model.TravelDocument
import com.example.roamly.data.repository.DocumentRepository
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.util.UUID

class DocumentsFragment :
    Fragment(
        R.layout.fragment_documents
    ) {

    private lateinit var documentRepository:
            DocumentRepository

    private lateinit var documentAdapter:
            DocumentAdapter

    private lateinit var documentsRecyclerView:
            RecyclerView

    private lateinit var emptyState:
            LinearLayout

    private val documentPicker =
        registerForActivityResult(
            ActivityResultContracts
                .OpenMultipleDocuments()
        ) { uris ->

            if (uris.isEmpty()) {
                return@registerForActivityResult
            }

            var addedCount =
                0

            uris.forEach { uri ->

                if (
                    saveDocument(
                        uri
                    )
                ) {

                    addedCount++
                }
            }

            loadDocuments()

            if (addedCount > 0) {

                Toast.makeText(
                    requireContext(),
                    if (addedCount == 1) {
                        getString(
                            R.string
                                .document_added
                        )
                    } else {
                        getString(
                            R.string
                                .documents_added
                        )
                    },
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {

        super.onViewCreated(
            view,
            savedInstanceState
        )

        documentRepository =
            DocumentRepository(
                requireContext()
                    .applicationContext
            )

        documentsRecyclerView =
            view.findViewById(
                R.id.documentsRecyclerView
            )

        emptyState =
            view.findViewById(
                R.id.documentsEmptyState
            )

        val uploadButton =
            view.findViewById<MaterialButton>(
                R.id.uploadDocumentButton
            )

        documentAdapter =
            DocumentAdapter(
                onDocumentClick = {
                        document ->

                    openDocument(
                        document
                    )
                },
                onDeleteClick = {
                        document ->

                    showDeleteDialog(
                        document
                    )
                }
            )

        documentsRecyclerView
            .apply {

                layoutManager =
                    LinearLayoutManager(
                        requireContext()
                    )

                adapter =
                    documentAdapter
            }

        uploadButton
            .setOnClickListener {

                selectDocuments()
            }

        loadDocuments()
    }

    override fun onResume() {

        super.onResume()

        if (
            ::documentRepository
                .isInitialized
        ) {

            loadDocuments()
        }
    }

    private fun selectDocuments() {

        documentPicker.launch(
            arrayOf(
                "application/pdf",
                "image/*",
                "application/msword",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                "application/vnd.ms-excel",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                "application/vnd.ms-powerpoint",
                "application/vnd.openxmlformats-officedocument.presentationml.presentation",
                "text/plain"
            )
        )
    }

    private fun saveDocument(
        uri: Uri
    ): Boolean {

        try {

            try {

                requireContext()
                    .contentResolver
                    .takePersistableUriPermission(
                        uri,
                        Intent
                            .FLAG_GRANT_READ_URI_PERMISSION
                    )

            } catch (
                exception:
                SecurityException
            ) {

                Log.w(
                    "RoamlyDocuments",
                    "Could not persist document permission",
                    exception
                )
            }

            val metadata =
                readDocumentMetadata(
                    uri
                )

            val mimeType =
                requireContext()
                    .contentResolver
                    .getType(
                        uri
                    )
                    ?: "application/octet-stream"

            val document =
                TravelDocument(
                    documentId =
                        UUID.randomUUID()
                            .toString(),
                    displayName =
                        metadata.first,
                    mimeType =
                        mimeType,
                    uri =
                        uri.toString(),
                    sizeBytes =
                        metadata.second,
                    addedAt =
                        System.currentTimeMillis()
                )

            documentRepository
                .addDocument(
                    document
                )

            Log.i(
                "RoamlyDocuments",
                "Document added: ${document.displayName}"
            )

            return true

        } catch (
            exception:
            Exception
        ) {

            Log.e(
                "RoamlyDocuments",
                "Failed to add document",
                exception
            )

            return false
        }
    }

    private fun readDocumentMetadata(
        uri: Uri
    ): Pair<String, Long> {

        var displayName =
            getString(
                R.string
                    .document_unknown_name
            )

        var sizeBytes =
            0L

        requireContext()
            .contentResolver
            .query(
                uri,
                null,
                null,
                null,
                null
            )
            ?.use { cursor ->

                val nameIndex =
                    cursor
                        .getColumnIndex(
                            OpenableColumns
                                .DISPLAY_NAME
                        )

                val sizeIndex =
                    cursor
                        .getColumnIndex(
                            OpenableColumns
                                .SIZE
                        )

                if (
                    cursor.moveToFirst()
                ) {

                    if (
                        nameIndex >= 0
                    ) {

                        displayName =
                            cursor
                                .getString(
                                    nameIndex
                                )
                                ?: displayName
                    }

                    if (
                        sizeIndex >= 0 &&
                        !cursor.isNull(
                            sizeIndex
                        )
                    ) {

                        sizeBytes =
                            cursor.getLong(
                                sizeIndex
                            )
                    }
                }
            }

        return Pair(
            displayName,
            sizeBytes
        )
    }

    private fun loadDocuments() {

        val documents =
            documentRepository
                .getDocuments()
                .sortedByDescending {
                    it.addedAt
                }

        documentAdapter
            .submitList(
                documents
            )

        if (
            documents.isEmpty()
        ) {

            documentsRecyclerView
                .visibility =
                View.GONE

            emptyState
                .visibility =
                View.VISIBLE

        } else {

            documentsRecyclerView
                .visibility =
                View.VISIBLE

            emptyState
                .visibility =
                View.GONE
        }
    }

    private fun openDocument(
        document:
        TravelDocument
    ) {

        val uri =
            Uri.parse(
                document.uri
            )

        val mimeType =
            document.mimeType
                .ifBlank {
                    "*/*"
                }

        val intent =
            Intent(
                Intent.ACTION_VIEW
            ).apply {

                setDataAndType(
                    uri,
                    mimeType
                )

                addFlags(
                    Intent
                        .FLAG_GRANT_READ_URI_PERMISSION
                )
            }

        try {

            Log.i(
                "RoamlyDocuments",
                "Opening document: ${document.displayName}"
            )

            startActivity(
                Intent.createChooser(
                    intent,
                    getString(
                        R.string
                            .open_document
                    )
                )
            )

        } catch (
            exception:
            ActivityNotFoundException
        ) {

            Toast.makeText(
                requireContext(),
                getString(
                    R.string
                        .document_open_error
                ),
                Toast.LENGTH_LONG
            ).show()

        } catch (
            exception:
            SecurityException
        ) {

            Log.e(
                "RoamlyDocuments",
                "Document access denied",
                exception
            )

            Toast.makeText(
                requireContext(),
                getString(
                    R.string
                        .document_access_error
                ),
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun showDeleteDialog(
        document:
        TravelDocument
    ) {

        MaterialAlertDialogBuilder(
            requireContext()
        )
            .setTitle(
                R.string
                    .document_delete_title
            )
            .setMessage(
                R.string
                    .document_delete_message
            )
            .setNegativeButton(
                R.string.cancel,
                null
            )
            .setPositiveButton(
                R.string.delete
            ) {
                    _,
                    _ ->

                deleteDocument(
                    document
                )
            }
            .show()
    }

    private fun deleteDocument(
        document:
        TravelDocument
    ) {

        documentRepository
            .deleteDocument(
                document.documentId
            )

        try {

            requireContext()
                .contentResolver
                .releasePersistableUriPermission(
                    Uri.parse(
                        document.uri
                    ),
                    Intent
                        .FLAG_GRANT_READ_URI_PERMISSION
                )

        } catch (
            exception:
            Exception
        ) {

            Log.w(
                "RoamlyDocuments",
                "Could not release document permission",
                exception
            )
        }

        Log.i(
            "RoamlyDocuments",
            "Document removed: ${document.displayName}"
        )

        Toast.makeText(
            requireContext(),
            getString(
                R.string
                    .document_deleted
            ),
            Toast.LENGTH_SHORT
        ).show()

        loadDocuments()
    }
}