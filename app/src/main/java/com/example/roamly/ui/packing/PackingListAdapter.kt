package com.example.roamly.ui.packing

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.roamly.R
import com.example.roamly.data.model.PackingList
import android.graphics.Color
import android.widget.ImageView
import com.google.android.material.card.MaterialCardView

// Holds the views for one packing list
class PackingListAdapter(
    private var packingLists: List<PackingList>,
    // when u click a card, it allows u to add items in  that packing list
    private val onListClicked: (PackingList) -> Unit
) : RecyclerView.Adapter<PackingListAdapter.PackingListViewHolder>() {

    // store the progress of each packing list
    private var progressMap = mapOf<String, Pair<Int, Int>>()

    class PackingListViewHolder(view: View) : RecyclerView.ViewHolder(view) {

        val name: TextView = view.findViewById(R.id.txtPackingListName)
        val description: TextView = view.findViewById(R.id.txtPackingListDescription)
        val progressBar: ProgressBar = view.findViewById(R.id.packingListProgressBar)
        val progressText: TextView = view.findViewById(R.id.txtPackingListProgress)

        val icon: ImageView = view.findViewById(R.id.imgPackingListIcon)
        val iconCard: MaterialCardView = view.findViewById(R.id.cardPackingListIcon)

    }

    // Creates the layout for each packing list
    override fun onCreateViewHolder( parent: ViewGroup,viewType: Int ): PackingListViewHolder {

        Log.d("PackingAdapter", "onCreateViewHolder called")

        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_packing_list, parent, false)

        return PackingListViewHolder(view)
    }

    // Displays the packing list data
    override fun onBindViewHolder(holder: PackingListViewHolder,position: Int
    ) {

        val packingList = packingLists[position]

        Log.d("PackingAdapter", "onBindViewHolder called for ${packingLists[position].name}")

        holder.name.text = packingList.name

        applyPackingListIconStyle( packingList.name, holder.icon,holder.iconCard)

        holder.description.text = packingList.description
            ?: holder.itemView.context.getString(R.string.no_description)

        // get progress for this specific packing list
        val progress =  progressMap[packingList.packingListId]

        if (progress != null) {
            val packedItems = progress.first
            val totalItems = progress.second

            val percentage = if (totalItems == 0) {
                    0
                } else {
                    (packedItems * 100) / totalItems
                }

            holder.progressBar.progress = percentage

            holder.progressText.text =
                holder.itemView.context.getString(
                    R.string.packing_progress,
                    packedItems,
                    totalItems
                )

        } else {

            holder.progressBar.progress = 0

            holder.progressText.text =
                holder.itemView.context.getString(R.string.packing_progress_empty)
        }

        // if u click on the card, takes u to the next screen --> can add items in that packing list
        holder.itemView.setOnClickListener { onListClicked(packingList)
        }
    }



    // Returns the number of packing lists
    override fun getItemCount(): Int {
        return packingLists.size
    }

    // Updates the adapter with new packing lists
    fun updateLists(newLists: List<PackingList>) {
        packingLists = newLists
        notifyDataSetChanged()
    }


    // Updates the progress for each packing list
    fun updateProgress(
        newProgressMap: Map<String, Pair<Int, Int>>
    ) {
        progressMap = newProgressMap
        notifyDataSetChanged()
    }


    // adds the icon-selection function
    private fun applyPackingListIconStyle(name: String, iconView: ImageView,iconCard: MaterialCardView
    ) {

        val (icon, tint, background) = when {
                    name.contains("clothing", ignoreCase = true) ||
                    name.contains("outfit", ignoreCase = true) ||
                    name.contains("shoes", ignoreCase = true) -> {
                Triple(R.drawable.ic_apparel, "#2F8FD8", "#E5F2FD")
            }

            name.contains("toiletries", ignoreCase = true) ||
                    name.contains("hygiene", ignoreCase = true) -> {
                Triple(R.drawable.ic_toiletries, "#3E9B7C", "#E5F5EF")
            }

            name.contains("electronics", ignoreCase = true) ||
                    name.contains("gadgets", ignoreCase = true) -> {
                Triple(R.drawable.ic_electronics, "#7867B8", "#EEEAF8")
            }

            name.contains("document", ignoreCase = true) ||
                    name.contains("passport", ignoreCase = true) ||
                    name.contains("travel essentials", ignoreCase = true) -> {
                Triple(R.drawable.ic_document, "#D99032", "#FFF2DF")
            }

            else -> {
                Triple(R.drawable.ic_baggage, "#2F8FD8", "#E5F2FD")
            }
        }

        iconView.setImageResource(icon)
        iconView.setColorFilter(Color.parseColor(tint))
        iconCard.setCardBackgroundColor(Color.parseColor(background))
    }


}