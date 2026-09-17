package com.example.roamly.ui.packing

import androidx.appcompat.app.AlertDialog
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.roamly.R
import com.example.roamly.data.model.PackingItem
import com.example.roamly.data.model.PackingItemRequest
import com.example.roamly.data.model.PackingListRequest
import kotlinx.coroutines.launch


class PackingItemsFragment : Fragment(R.layout.fragment_packing_items) {

    private lateinit var adapter: PackingItemAdapter
    private val viewModel: PackingViewModel by activityViewModels()


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Get the views from the XML file
        val recyclerView =  view.findViewById<RecyclerView>(R.id.recyclerPackingItems)
        val title =  view.findViewById<TextView>(R.id.txtPackingListTitle)
        val description =  view.findViewById<TextView>(R.id.packingListDescription)
        val addItemButton =  view.findViewById<ImageButton>(R.id.btnAddPackingItem)
        val editPackingListButton =  view.findViewById<ImageButton>(R.id.btnEditPackingList)
        val deletePackingListButton = view.findViewById<ImageButton>(R.id.btnDeletePackingList)
        val progressBar = view.findViewById<ProgressBar>(R.id.packingProgressBar)
        val packingProgress = view.findViewById<TextView>(R.id.txtPackingProgress)

        // Get the selected packing list information
        val packingListId = arguments?.getString("packingListId") ?: ""
        val packingListName = arguments?.getString("packingListName") ?: "Packing List"
        val packingListDescription = arguments?.getString("packingListDescription") ?: " "

        // Display packing list name and description
        title.text = packingListName
        description.text = packingListDescription

        // Edit packing list button
        editPackingListButton.setOnClickListener {
            showEditPackingListDialog(packingListId,title, description )
        }


        // Delete packing list button
        deletePackingListButton.setOnClickListener {
            showDeletePackingListDialog(packingListId,packingListName)
        }

        // Set up RecyclerView
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        // Set up packing item adapter
        adapter = PackingItemAdapter( emptyList(),

            // Checkbox clicked
            { item, isChecked ->

                viewModel.updatePackingItem(
                    item.packingItemId,
                    PackingItemRequest(
                        name = item.name,
                        isPacked = isChecked
                    )
                )
            },

            // Edit item
            { item ->
                showEditItemDialog(item)
            },

            // Delete item
            { item ->
                showDeleteItemDialog(
                    item,
                    packingListId
                )
            }
        )


        recyclerView.adapter = adapter


        // Add packing item
        addItemButton.setOnClickListener {

            val itemInput = EditText(requireContext())
            itemInput.hint = "e.g. Sunglasses"

            val layout = LinearLayout(requireContext())
            layout.orientation = LinearLayout.VERTICAL
            layout.setPadding(48, 0, 48, 0)

            layout.addView(itemInput)


            // Show add item dialog
            AlertDialog.Builder(requireContext())
                .setTitle("Add Packing Item")
                .setView(layout)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("+ Add Item") { _, _ ->

                    val itemName =
                        itemInput.text.toString().trim()

                    if (itemName.isNotEmpty()) {

                        val request = PackingItemRequest(
                            name = itemName,
                            isPacked = false
                        )

                        viewModel.addPackingItem(
                            packingListId,
                            request
                        )
                    }
                }
                .show()
        }


        // Observe packing items
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(
                androidx.lifecycle.Lifecycle.State.STARTED
            ) {

                launch {
                    viewModel.packingItems.collect { items ->

                        // display items in the recycler view
                        adapter.updateItems(items)

                        // update progress bar whenever an item is ticked-off
                        val totalItems = items.size
                        val packedItems = items.count { it.isPacked }

                        val progress = if (totalItems == 0) {
                            0
                        } else {
                            (packedItems * 100) / totalItems
                        }

                        progressBar.progress = progress
                        packingProgress.text = "$packedItems of $totalItems packed"
                    }
                }
            }
        }


        // Load items for this packing list
        viewModel.getPackingItems(packingListId)
    }



    // PACKING LIST EDIT
    private fun showEditPackingListDialog(packingListId: String,title: TextView, description: TextView) {

        val nameInput = EditText(requireContext())
        nameInput.setText(title.text.toString())
        nameInput.hint = "List name"

        val descriptionInput = EditText(requireContext())
        descriptionInput.setText(description.text.toString())
        descriptionInput.hint = "Description"

        val layout = LinearLayout(requireContext())
        layout.orientation = LinearLayout.VERTICAL
        layout.setPadding(48, 0, 48, 0)

        layout.addView(nameInput)
        layout.addView(descriptionInput)


        val dialog = AlertDialog.Builder(requireContext())
            .setTitle("Edit List")
            .setView(layout)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Save Changes", null)
            .create()

        dialog.show()

        dialog.window?.setBackgroundDrawableResource(R.drawable.dialog_background)

        val saveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)

        styleDialogButton(saveButton, R.drawable.dialog_button_blue)

        saveButton.setOnClickListener {

            val newName = nameInput.text.toString().trim()
            val newDescription = descriptionInput.text.toString().trim()

            if (newName.isNotEmpty()) {

                val request = PackingListRequest(
                    name = newName,
                    description = newDescription
                )

                viewModel.updatePackingList(
                    packingListId,
                    request
                )

                title.text = newName
                description.text = newDescription

                dialog.dismiss()
            }
        }
    }


    // PACKING LIST DELETE
    private fun showDeletePackingListDialog( packingListId: String,  packingListName: String) {

        val dialog = AlertDialog.Builder(requireContext())
            .setTitle("Delete List")
            .setMessage(
                "Are you sure you want to delete \"$packingListName\"?\n\n" +
                        "This will delete the list and every item inside it."
            )
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Delete", null)
            .create()

        dialog.show()

        dialog.window?.setBackgroundDrawableResource(R.drawable.dialog_background)

        val deleteButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)

        styleDialogButton(deleteButton,R.drawable.dialog_button_red
        )

        deleteButton.setOnClickListener {

            viewModel.deletePackingList(packingListId)

            parentFragmentManager.popBackStack()

            dialog.dismiss()
        }
    }


    // PACKING ITEM DELETE
    private fun showDeleteItemDialog(item: PackingItem, packingListId: String ) {

        val dialog = AlertDialog.Builder(requireContext())
            .setTitle("Delete Item?")
            .setMessage(
                "Are you sure you want to delete \"${item.name}\"?"
            )
            .setNegativeButton("Cancel", null)
            .setPositiveButton("X Delete", null)
            .create()

        dialog.show()

        dialog.window?.setBackgroundDrawableResource(R.drawable.dialog_background)

        val deleteButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)

        styleDialogButton( deleteButton,R.drawable.dialog_button_red)

        deleteButton.setOnClickListener {

            viewModel.deletePackingItem(
                item.packingItemId,
                packingListId
            )

            dialog.dismiss()
        }
    }


    // PACKING ITEM EDIT
    // PACKING ITEM EDIT
    private fun showEditItemDialog(item: PackingItem) {

        val itemInput = EditText(requireContext())
        itemInput.setText(item.name)
        itemInput.hint = "Item name"

        val layout = LinearLayout(requireContext())
        layout.orientation = LinearLayout.VERTICAL
        layout.setPadding(48, 0, 48, 0)

        layout.addView(itemInput)

        // Create the dialog
        val dialog = AlertDialog.Builder(requireContext())
            .setTitle("Edit Item")
            .setView(layout)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Save Changes", null)
            .create()

        // Show the dialog first
        dialog.show()

        // Make the dialog rounded
        dialog.window?.setBackgroundDrawableResource( R.drawable.dialog_background
        )

        // Get the Save button
        val saveButton = dialog.getButton( AlertDialog.BUTTON_POSITIVE
        )

        // Make the Save button blue
        styleDialogButton( saveButton, R.drawable.dialog_button_blue
        )

        // Save the changes
        saveButton.setOnClickListener {

            val newName = itemInput.text.toString().trim()

            if (newName.isNotEmpty()) {

                viewModel.updatePackingItem(
                    item.packingItemId,
                    PackingItemRequest(
                        name = newName,
                        isPacked = item.isPacked
                    )
                )

                dialog.dismiss()
            }
        }
    }

    private fun styleDialogButton(button: Button, background: Int
    ) {
        button.backgroundTintList = null
        button.setBackgroundResource(background)
        button.setTextColor(android.graphics.Color.WHITE)
        button.setPadding(32, 12, 32, 12)
        button.alpha = 1f
    }



}