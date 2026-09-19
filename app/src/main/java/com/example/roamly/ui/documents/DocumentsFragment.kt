package com.example.roamly.ui.documents

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.roamly.R
import com.example.roamly.data.model.TravelDocument
import com.example.roamly.data.repository.DocumentRepository
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
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

    private var addDocumentDialog:
            AlertDialog? = null

    private var editDocumentDialog:
            AlertDialog? = null

    private var selectedAddFileUri:
            Uri? = null

    private var selectedEditFileUri:
            Uri? = null

    private var addSelectedFileText:
            TextView? = null

    private var editReplacementFileText:
            TextView? = null

    private var documentBeingEdited:
            TravelDocument? = null

    private val addFilePicker =
        registerForActivityResult(
            ActivityResultContracts
                .OpenDocument()
        ) { uri ->

            if (
                uri == null
            ) {
                return@registerForActivityResult
            }

            selectedAddFileUri =
                uri

            val metadata =
                readDocumentMetadata(
                    uri
                )

            addSelectedFileText
                ?.text =
                metadata.first
        }

    private val editFilePicker =
        registerForActivityResult(
            ActivityResultContracts
                .OpenDocument()
        ) { uri ->

            if (
                uri == null
            ) {
                return@registerForActivityResult
            }

            selectedEditFileUri =
                uri

            val metadata =
                readDocumentMetadata(
                    uri
                )

            editReplacementFileText
                ?.text =
                "New file: ${metadata.first}"
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
            view.findViewById<
                    MaterialButton
                    >(
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
                onEditClick = {
                        document ->

                    showEditDocumentDialog(
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

                showAddDocumentDialog()
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

    private fun documentTypes():
            List<String> {

        return listOf(
            "Identification",
            "Visa",
            "Flight",
            "Accommodation",
            "Insurance",
            "Booking",
            "Other"
        )
    }

    private fun supportedDocumentTypes():
            Array<String> {

        return arrayOf(
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
    }

    private fun showAddDocumentDialog() {

        selectedAddFileUri =
            null

        val dialogView =
            LayoutInflater
                .from(
                    requireContext()
                )
                .inflate(
                    R.layout.dialog_add_document,
                    null
                )

        val nameEditText =
            dialogView
                .findViewById<
                        TextInputEditText
                        >(
                    R.id.addDocumentNameEditText
                )

        val typeAutoComplete =
            dialogView
                .findViewById<
                        AutoCompleteTextView
                        >(
                    R.id.addDocumentTypeAutoComplete
                )

        val notesEditText =
            dialogView
                .findViewById<
                        TextInputEditText
                        >(
                    R.id.addDocumentNotesEditText
                )

        val chooseFileButton =
            dialogView
                .findViewById<
                        MaterialButton
                        >(
                    R.id.chooseDocumentFileButton
                )

        val cancelButton =
            dialogView
                .findViewById<
                        MaterialButton
                        >(
                    R.id.cancelAddDocumentButton
                )

        val uploadButton =
            dialogView
                .findViewById<
                        MaterialButton
                        >(
                    R.id.confirmAddDocumentButton
                )

        addSelectedFileText =
            dialogView
                .findViewById(
                    R.id.selectedDocumentFileTextView
                )

        val typeAdapter =
            ArrayAdapter(
                requireContext(),
                android.R.layout
                    .simple_dropdown_item_1line,
                documentTypes()
            )

        typeAutoComplete
            .setAdapter(
                typeAdapter
            )

        typeAutoComplete
            .setOnClickListener {

                typeAutoComplete
                    .showDropDown()
            }

        val dialog =
            MaterialAlertDialogBuilder(
                requireContext()
            )
                .setView(
                    dialogView
                )
                .create()

        addDocumentDialog =
            dialog

        chooseFileButton
            .setOnClickListener {

                addFilePicker.launch(
                    supportedDocumentTypes()
                )
            }

        cancelButton
            .setOnClickListener {

                dialog.dismiss()
            }

        uploadButton
            .setOnClickListener {

                val documentName =
                    nameEditText.text
                        ?.toString()
                        ?.trim()
                        .orEmpty()

                val documentType =
                    typeAutoComplete.text
                        ?.toString()
                        ?.trim()
                        .orEmpty()

                val notes =
                    notesEditText.text
                        ?.toString()
                        ?.trim()
                        .orEmpty()

                val fileUri =
                    selectedAddFileUri

                if (
                    documentName.isBlank()
                ) {

                    nameEditText.error =
                        "Enter a document name"

                    return@setOnClickListener
                }

                if (
                    documentType.isBlank()
                ) {

                    typeAutoComplete.error =
                        "Select a document type"

                    return@setOnClickListener
                }

                if (
                    fileUri == null
                ) {

                    Toast.makeText(
                        requireContext(),
                        "Choose a file to upload.",
                        Toast.LENGTH_SHORT
                    ).show()

                    return@setOnClickListener
                }

                if (
                    createDocument(
                        documentName =
                            documentName,
                        documentType =
                            documentType,
                        notes =
                            notes,
                        uri =
                            fileUri
                    )
                ) {

                    dialog.dismiss()

                    loadDocuments()

                    Toast.makeText(
                        requireContext(),
                        "Document uploaded.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

        dialog.setOnDismissListener {

            selectedAddFileUri =
                null

            addSelectedFileText =
                null

            addDocumentDialog =
                null
        }

        dialog.show()
    }

    private fun createDocument(
        documentName: String,
        documentType: String,
        notes: String,
        uri: Uri
    ): Boolean {

        return try {

            val duplicate =
                documentRepository
                    .getDocuments()
                    .any {
                        it.uri ==
                                uri.toString()
                    }

            if (
                duplicate
            ) {

                Toast.makeText(
                    requireContext(),
                    "This file has already been added.",
                    Toast.LENGTH_SHORT
                ).show()

                return false
            }

            persistReadPermission(
                uri
            )

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
                        documentName,
                    mimeType =
                        mimeType,
                    uri =
                        uri.toString(),
                    sizeBytes =
                        metadata.second,
                    addedAt =
                        System.currentTimeMillis(),
                    documentType =
                        documentType,
                    fileName =
                        metadata.first,
                    notes =
                        notes
                )

            documentRepository
                .addDocument(
                    document
                )

            Log.i(
                TAG,
                "Document added: ${document.displayName}"
            )

            true

        } catch (
            exception: Exception
        ) {

            Log.e(
                TAG,
                "Failed to add document",
                exception
            )

            Toast.makeText(
                requireContext(),
                "Could not upload the document.",
                Toast.LENGTH_LONG
            ).show()

            false
        }
    }

    private fun showEditDocumentDialog(
        document: TravelDocument
    ) {

        selectedEditFileUri =
            null

        documentBeingEdited =
            document

        val dialogView =
            LayoutInflater
                .from(
                    requireContext()
                )
                .inflate(
                    R.layout.dialog_edit_document,
                    null
                )

        val nameEditText =
            dialogView
                .findViewById<
                        TextInputEditText
                        >(
                    R.id.editDocumentNameEditText
                )

        val typeAutoComplete =
            dialogView
                .findViewById<
                        AutoCompleteTextView
                        >(
                    R.id.editDocumentTypeAutoComplete
                )

        val currentFileText =
            dialogView
                .findViewById<
                        TextView
                        >(
                    R.id.currentDocumentFileTextView
                )

        val notesEditText =
            dialogView
                .findViewById<
                        TextInputEditText
                        >(
                    R.id.editDocumentNotesEditText
                )

        val replaceFileButton =
            dialogView
                .findViewById<
                        MaterialButton
                        >(
                    R.id.replaceDocumentFileButton
                )

        val cancelButton =
            dialogView
                .findViewById<
                        MaterialButton
                        >(
                    R.id.cancelEditDocumentButton
                )

        val saveButton =
            dialogView
                .findViewById<
                        MaterialButton
                        >(
                    R.id.saveDocumentChangesButton
                )

        editReplacementFileText =
            dialogView
                .findViewById(
                    R.id.replacementDocumentFileTextView
                )

        nameEditText.setText(
            document.displayName
        )

        val existingType =
            document.documentType
                ?.takeIf {
                    it.isNotBlank()
                }
                ?: "Other"

        typeAutoComplete.setText(
            existingType,
            false
        )

        currentFileText.text =
            document.fileName
                ?.takeIf {
                    it.isNotBlank()
                }
                ?: document.displayName

        notesEditText.setText(
            document.notes
                .orEmpty()
        )

        val typeAdapter =
            ArrayAdapter(
                requireContext(),
                android.R.layout
                    .simple_dropdown_item_1line,
                documentTypes()
            )

        typeAutoComplete
            .setAdapter(
                typeAdapter
            )

        typeAutoComplete
            .setOnClickListener {

                typeAutoComplete
                    .showDropDown()
            }

        val dialog =
            MaterialAlertDialogBuilder(
                requireContext()
            )
                .setView(
                    dialogView
                )
                .create()

        editDocumentDialog =
            dialog

        replaceFileButton
            .setOnClickListener {

                editFilePicker.launch(
                    supportedDocumentTypes()
                )
            }

        cancelButton
            .setOnClickListener {

                dialog.dismiss()
            }

        saveButton
            .setOnClickListener {

                val documentName =
                    nameEditText.text
                        ?.toString()
                        ?.trim()
                        .orEmpty()

                val documentType =
                    typeAutoComplete.text
                        ?.toString()
                        ?.trim()
                        .orEmpty()

                val notes =
                    notesEditText.text
                        ?.toString()
                        ?.trim()
                        .orEmpty()

                if (
                    documentName.isBlank()
                ) {

                    nameEditText.error =
                        "Enter a document name"

                    return@setOnClickListener
                }

                if (
                    documentType.isBlank()
                ) {

                    typeAutoComplete.error =
                        "Select a document type"

                    return@setOnClickListener
                }

                if (
                    updateDocument(
                        originalDocument =
                            document,
                        documentName =
                            documentName,
                        documentType =
                            documentType,
                        notes =
                            notes,
                        replacementUri =
                            selectedEditFileUri
                    )
                ) {

                    dialog.dismiss()

                    loadDocuments()

                    Toast.makeText(
                        requireContext(),
                        "Document updated.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

        dialog.setOnDismissListener {

            selectedEditFileUri =
                null

            editReplacementFileText =
                null

            documentBeingEdited =
                null

            editDocumentDialog =
                null
        }

        dialog.show()
    }

    private fun updateDocument(
        originalDocument:
        TravelDocument,
        documentName: String,
        documentType: String,
        notes: String,
        replacementUri: Uri?
    ): Boolean {

        return try {

            var finalUri =
                originalDocument.uri

            var finalMimeType =
                originalDocument.mimeType

            var finalSize =
                originalDocument.sizeBytes

            var finalFileName =
                originalDocument.fileName
                    ?.takeIf {
                        it.isNotBlank()
                    }
                    ?: originalDocument
                        .displayName

            if (
                replacementUri != null
            ) {

                persistReadPermission(
                    replacementUri
                )

                val metadata =
                    readDocumentMetadata(
                        replacementUri
                    )

                finalUri =
                    replacementUri
                        .toString()

                finalMimeType =
                    requireContext()
                        .contentResolver
                        .getType(
                            replacementUri
                        )
                        ?: "application/octet-stream"

                finalSize =
                    metadata.second

                finalFileName =
                    metadata.first
            }

            val updatedDocument =
                originalDocument.copy(
                    displayName =
                        documentName,
                    documentType =
                        documentType,
                    notes =
                        notes,
                    uri =
                        finalUri,
                    mimeType =
                        finalMimeType,
                    sizeBytes =
                        finalSize,
                    fileName =
                        finalFileName
                )

            documentRepository
                .updateDocument(
                    updatedDocument
                )

            if (
                replacementUri != null &&
                replacementUri.toString() !=
                originalDocument.uri
            ) {

                releaseReadPermission(
                    Uri.parse(
                        originalDocument.uri
                    )
                )
            }

            true

        } catch (
            exception: Exception
        ) {

            Log.e(
                TAG,
                "Failed to update document",
                exception
            )

            Toast.makeText(
                requireContext(),
                "Could not update the document.",
                Toast.LENGTH_LONG
            ).show()

            false
        }
    }

    private fun persistReadPermission(
        uri: Uri
    ) {

        try {

            requireContext()
                .contentResolver
                .takePersistableUriPermission(
                    uri,
                    Intent
                        .FLAG_GRANT_READ_URI_PERMISSION
                )

        } catch (
            exception: SecurityException
        ) {

            Log.w(
                TAG,
                "Could not persist document permission",
                exception
            )
        }
    }

    private fun releaseReadPermission(
        uri: Uri
    ) {

        try {

            requireContext()
                .contentResolver
                .releasePersistableUriPermission(
                    uri,
                    Intent
                        .FLAG_GRANT_READ_URI_PERMISSION
                )

        } catch (
            exception: Exception
        ) {

            Log.w(
                TAG,
                "Could not release document permission",
                exception
            )
        }
    }

    private fun readDocumentMetadata(
        uri: Uri
    ): Pair<String, Long> {

        var displayName =
            "Travel Document"

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

            startActivity(
                Intent.createChooser(
                    intent,
                    "Open Document"
                )
            )

        } catch (
            exception:
            ActivityNotFoundException
        ) {

            Toast.makeText(
                requireContext(),
                "No compatible app was found to open this document.",
                Toast.LENGTH_LONG
            ).show()

        } catch (
            exception:
            SecurityException
        ) {

            Log.e(
                TAG,
                "Document access denied",
                exception
            )

            Toast.makeText(
                requireContext(),
                "Roamly no longer has permission to access this document.",
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
                "Delete Document"
            )
            .setMessage(
                "Are you sure you want to remove this document from Roamly?"
            )
            .setNegativeButton(
                "Cancel",
                null
            )
            .setPositiveButton(
                "Delete"
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

        releaseReadPermission(
            Uri.parse(
                document.uri
            )
        )

        Toast.makeText(
            requireContext(),
            "Document deleted.",
            Toast.LENGTH_SHORT
        ).show()

        loadDocuments()
    }

    companion object {

        private const val TAG =
            "RoamlyDocuments"
    }
}