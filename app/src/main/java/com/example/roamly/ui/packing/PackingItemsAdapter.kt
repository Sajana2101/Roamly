package com.example.roamly.ui.packing

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.roamly.R
import com.example.roamly.data.model.PackingItem

class PackingItemAdapter(
    private var packingItems: List<PackingItem>,
    private val onItemChecked: (PackingItem, Boolean) -> Unit,
    // when click on the image,it gives u the option to delete/edit
    private val onItemEdit: (PackingItem) -> Unit,
    private val onIteDelete: (PackingItem) -> Unit
) : RecyclerView.Adapter<PackingItemAdapter.PackingItemViewHolder>() {

    // Holds the views for one packing item
    class PackingItemViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val checkPacked: CheckBox = view.findViewById(R.id.checkPacked)
        val name: TextView = view.findViewById(R.id.txtPackingItemName)
        val btnEditItem: ImageButton = view.findViewById(R.id.btnEditItem)
        val btnDeleteItem: ImageButton = view.findViewById(R.id.btnDeleteItem)
    }

    // Creates the layout for each packing item
    override fun onCreateViewHolder( parent: ViewGroup,viewType: Int): PackingItemViewHolder {

        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_packing_item, parent, false)

        return PackingItemViewHolder(view)
    }

    // Displays the packing item data
    override fun onBindViewHolder(holder: PackingItemViewHolder, position: Int
    ) {

        val packingItem = packingItems[position]
        holder.name.text = packingItem.name

        // Set the checkbox to the item's current status
        // Cross out and grey the item when it is packed
        if (packingItem.isPacked) {
            holder.name.paintFlags = holder.name.paintFlags or android.graphics.Paint.STRIKE_THRU_TEXT_FLAG

            holder.name.alpha = 0.5f
        } else {
            holder.name.paintFlags = holder.name.paintFlags and
                        android.graphics.Paint.STRIKE_THRU_TEXT_FLAG.inv()

            holder.name.alpha = 1.0f
        }

        // Detect when the checkbox is changed
        holder.checkPacked.setOnCheckedChangeListener { _, isChecked ->
            onItemChecked(packingItem, isChecked)
        }

        // buttons to edit/delete items
        holder.btnEditItem.setOnClickListener {
            onItemEdit(packingItem)
        }

        holder.btnDeleteItem.setOnClickListener {
            onIteDelete(packingItem)
        }


    }

    // Returns the number of packing items
    override fun getItemCount(): Int {
        return packingItems.size
    }

    // Updates the adapter with new packing items
    fun updateItems(newItems: List<PackingItem>) {
        packingItems = newItems
        notifyDataSetChanged()
    }
}