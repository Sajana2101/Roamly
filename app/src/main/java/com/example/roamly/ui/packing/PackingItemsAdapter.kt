package com.example.roamly.ui.packing

import android.graphics.Paint
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
    private var packingItems:
    List<PackingItem>,
    private val onItemChecked:
        (PackingItem, Boolean) -> Unit,
    private val onItemEdit:
        (PackingItem) -> Unit,
    private val onItemDelete:
        (PackingItem) -> Unit
) :
    RecyclerView.Adapter<
            PackingItemAdapter.PackingItemViewHolder
            >() {

    class PackingItemViewHolder(
        view: View
    ) :
        RecyclerView.ViewHolder(
            view
        ) {

        val checkPacked:
                CheckBox =
            view.findViewById(
                R.id.checkPacked
            )

        val name:
                TextView =
            view.findViewById(
                R.id.txtPackingItemName
            )

        val btnEditItem:
                ImageButton =
            view.findViewById(
                R.id.btnEditItem
            )

        val btnDeleteItem:
                ImageButton =
            view.findViewById(
                R.id.btnDeleteItem
            )
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ):
            PackingItemViewHolder {

        val view =
            LayoutInflater
                .from(
                    parent.context
                )
                .inflate(
                    R.layout.item_packing_item,
                    parent,
                    false
                )

        return PackingItemViewHolder(
            view
        )
    }

    override fun onBindViewHolder(
        holder:
        PackingItemViewHolder,
        position: Int
    ) {

        val packingItem =
            packingItems[position]

        holder.name.text =
            packingItem.name

        holder.checkPacked
            .setOnCheckedChangeListener(
                null
            )

        holder.checkPacked
            .isChecked =
            packingItem.isPacked

        if (
            packingItem.isPacked
        ) {

            holder.name.paintFlags =
                holder.name.paintFlags or
                        Paint.STRIKE_THRU_TEXT_FLAG

            holder.name.alpha =
                0.5f

        } else {

            holder.name.paintFlags =
                holder.name.paintFlags and
                        Paint
                            .STRIKE_THRU_TEXT_FLAG
                            .inv()

            holder.name.alpha =
                1.0f
        }

        holder.checkPacked
            .setOnCheckedChangeListener {
                    _,
                    isChecked ->

                if (
                    isChecked !=
                    packingItem.isPacked
                ) {

                    onItemChecked(
                        packingItem,
                        isChecked
                    )
                }
            }

        holder.btnEditItem
            .setOnClickListener {

                onItemEdit(
                    packingItem
                )
            }

        holder.btnDeleteItem
            .setOnClickListener {

                onItemDelete(
                    packingItem
                )
            }
    }

    override fun getItemCount():
            Int {

        return packingItems.size
    }

    fun updateItems(
        newItems:
        List<PackingItem>
    ) {

        packingItems =
            newItems

        notifyDataSetChanged()
    }
}