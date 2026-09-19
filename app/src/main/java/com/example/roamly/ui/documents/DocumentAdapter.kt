package com.example.roamly.ui.documents

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.roamly.R
import com.example.roamly.data.model.TravelDocument
import com.google.android.material.button.MaterialButton
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DocumentAdapter(
    private val onDocumentClick:
        (TravelDocument) -> Unit,
    private val onEditClick:
        (TravelDocument) -> Unit,
    private val onDeleteClick:
        (TravelDocument) -> Unit
) :
    RecyclerView.Adapter<
            DocumentAdapter.DocumentViewHolder
            >() {

    private val documents =
        mutableListOf<TravelDocument>()

    fun submitList(
        newDocuments:
        List<TravelDocument>
    ) {

        documents.clear()

        documents.addAll(
            newDocuments
        )

        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): DocumentViewHolder {

        val view =
            LayoutInflater
                .from(
                    parent.context
                )
                .inflate(
                    R.layout.item_travel_document,
                    parent,
                    false
                )

        return DocumentViewHolder(
            view
        )
    }

    override fun onBindViewHolder(
        holder: DocumentViewHolder,
        position: Int
    ) {

        holder.bind(
            documents[position],
            onDocumentClick,
            onEditClick,
            onDeleteClick
        )
    }

    override fun getItemCount():
            Int {

        return documents.size
    }

    class DocumentViewHolder(
        itemView: View
    ) :
        RecyclerView.ViewHolder(
            itemView
        ) {

        private val typeTextView =
            itemView.findViewById<TextView>(
                R.id.documentTypeTextView
            )

        private val nameTextView =
            itemView.findViewById<TextView>(
                R.id.documentNameTextView
            )

        private val detailsTextView =
            itemView.findViewById<TextView>(
                R.id.documentDetailsTextView
            )

        private val openButton =
            itemView.findViewById<MaterialButton>(
                R.id.openDocumentButton
            )

        private val editButton =
            itemView.findViewById<MaterialButton>(
                R.id.editDocumentButton
            )

        private val deleteButton =
            itemView.findViewById<MaterialButton>(
                R.id.deleteDocumentButton
            )

        fun bind(
            document: TravelDocument,
            onDocumentClick:
                (TravelDocument) -> Unit,
            onEditClick:
                (TravelDocument) -> Unit,
            onDeleteClick:
                (TravelDocument) -> Unit
        ) {

            typeTextView.text =
                getFileType(
                    document
                )

            nameTextView.text =
                document.displayName

            detailsTextView.text =
                buildDetails(
                    document
                )

            itemView
                .setOnClickListener {

                    onDocumentClick(
                        document
                    )
                }

            openButton
                .setOnClickListener {

                    onDocumentClick(
                        document
                    )
                }

            editButton
                .setOnClickListener {

                    onEditClick(
                        document
                    )
                }

            deleteButton
                .setOnClickListener {

                    onDeleteClick(
                        document
                    )
                }
        }

        private fun buildDetails(
            document: TravelDocument
        ): String {

            val values =
                mutableListOf<String>()

            document.documentType
                ?.takeIf {
                    it.isNotBlank()
                }
                ?.let {
                    values.add(
                        it
                    )
                }

            document.fileName
                ?.takeIf {
                    it.isNotBlank()
                }
                ?.let {
                    values.add(
                        it
                    )
                }

            val size =
                formatFileSize(
                    document.sizeBytes
                )

            if (
                size.isNotBlank()
            ) {

                values.add(
                    size
                )
            }

            val date =
                SimpleDateFormat(
                    "dd MMM yyyy",
                    Locale.getDefault()
                )
                    .format(
                        Date(
                            document.addedAt
                        )
                    )

            values.add(
                date
            )

            return values.joinToString(
                " • "
            )
        }

        private fun getFileType(
            document: TravelDocument
        ): String {

            val mime =
                document.mimeType
                    .lowercase(
                        Locale.getDefault()
                    )

            return when {

                mime ==
                        "application/pdf" ->
                    "PDF"

                mime.startsWith(
                    "image/"
                ) ->
                    "IMG"

                mime.contains(
                    "word"
                ) ||
                        mime.contains(
                            "wordprocessingml"
                        ) ->
                    "DOC"

                mime.contains(
                    "excel"
                ) ||
                        mime.contains(
                            "spreadsheetml"
                        ) ->
                    "XLS"

                mime.contains(
                    "powerpoint"
                ) ||
                        mime.contains(
                            "presentationml"
                        ) ->
                    "PPT"

                mime.startsWith(
                    "text/"
                ) ->
                    "TXT"

                else ->
                    "FILE"
            }
        }

        private fun formatFileSize(
            bytes: Long
        ): String {

            if (
                bytes <= 0L
            ) {
                return ""
            }

            return when {

                bytes < 1024L ->
                    "$bytes B"

                bytes <
                        1024L * 1024L ->
                    String.format(
                        Locale.getDefault(),
                        "%.1f KB",
                        bytes / 1024.0
                    )

                else ->
                    String.format(
                        Locale.getDefault(),
                        "%.1f MB",
                        bytes /
                                (
                                        1024.0 *
                                                1024.0
                                        )
                    )
            }
        }
    }
}