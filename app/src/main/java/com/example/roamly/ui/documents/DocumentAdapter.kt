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
    private val onDeleteClick:
        (TravelDocument) -> Unit
) : RecyclerView.Adapter<DocumentAdapter.DocumentViewHolder>() {

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

        private val deleteButton =
            itemView.findViewById<MaterialButton>(
                R.id.deleteDocumentButton
            )

        fun bind(
            document:
            TravelDocument,
            onDocumentClick:
                (TravelDocument) -> Unit,
            onDeleteClick:
                (TravelDocument) -> Unit
        ) {

            typeTextView.text =
                getDocumentType(
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

            deleteButton
                .setOnClickListener {

                    onDeleteClick(
                        document
                    )
                }
        }

        private fun getDocumentType(
            document:
            TravelDocument
        ): String {

            val mimeType =
                document.mimeType
                    .lowercase(
                        Locale.getDefault()
                    )

            return when {

                mimeType ==
                        "application/pdf" -> {

                    "PDF"
                }

                mimeType
                    .startsWith(
                        "image/"
                    ) -> {

                    "IMG"
                }

                mimeType.contains(
                    "word"
                ) ||
                        mimeType.contains(
                            "wordprocessingml"
                        ) -> {

                    "DOC"
                }

                mimeType.contains(
                    "excel"
                ) ||
                        mimeType.contains(
                            "spreadsheetml"
                        ) -> {

                    "XLS"
                }

                mimeType.contains(
                    "powerpoint"
                ) ||
                        mimeType.contains(
                            "presentationml"
                        ) -> {

                    "PPT"
                }

                mimeType.startsWith(
                    "text/"
                ) -> {

                    "TXT"
                }

                else -> {

                    "FILE"
                }
            }
        }

        private fun buildDetails(
            document:
            TravelDocument
        ): String {

            val dateFormat =
                SimpleDateFormat(
                    "dd MMM yyyy",
                    Locale.getDefault()
                )

            val date =
                dateFormat.format(
                    Date(
                        document.addedAt
                    )
                )

            val size =
                formatFileSize(
                    document.sizeBytes
                )

            return if (
                size.isBlank()
            ) {

                date

            } else {

                "$size • $date"
            }
        }

        private fun formatFileSize(
            bytes: Long
        ): String {

            if (bytes <= 0L) {
                return ""
            }

            return when {

                bytes <
                        1024L -> {

                    "$bytes B"
                }

                bytes <
                        1024L * 1024L -> {

                    String.format(
                        Locale.getDefault(),
                        "%.1f KB",
                        bytes / 1024.0
                    )
                }

                else -> {

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
}