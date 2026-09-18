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
import com.google.android.material.button.MaterialButton
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

            // Load the custom XML layout
            val dialogView = layoutInflater.inflate(
                R.layout.dialog_create_item,
                null
            )

            // Get the views from the XML
            val itemInput =  dialogView.findViewById<EditText>(R.id.etCreateItemName  )

            val cancelButton =  dialogView.findViewById<MaterialButton>( R.id.btnCancelCreateItem )

            val addButton = dialogView.findViewById<MaterialButton>(R.id.btnConfirmCreateItem )

            // Create dialog
            val dialog = AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .create()

            dialog.show()

            // Make the default dialog background transparent
            dialog.window?.setBackgroundDrawableResource( android.R.color.transparent)

            // Cancel
            cancelButton.setOnClickListener {
                dialog.dismiss()
            }

            // Add item
            addButton.setOnClickListener {

                val itemName =itemInput.text.toString().trim()

                // Make sure an item name was entered
                if (itemName.isEmpty()) {

                    itemInput.error = "Please enter an item name"
                    return@setOnClickListener
                }

                // Create the request
                val request = PackingItemRequest(
                    name = itemName,
                    isPacked = false
                )

                // Add the item
                viewModel.addPackingItem(packingListId, request  )

                // Close dialog
                dialog.dismiss()
            }
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
    private fun showEditPackingListDialog( packingListId: String, title: TextView, description: TextView ) {

        // Load the custom XML layout
        val dialogView = layoutInflater.inflate(R.layout.dialog_edit_list, null )

        // Get the views from the XML
        val nameInput =dialogView.findViewById<EditText>(R.id.etListName)
        val descriptionInput = dialogView.findViewById<EditText>(R.id.etListDescription)
        val cancelButton = dialogView.findViewById<MaterialButton>(R.id.btnCancelEditList )
        val saveButton = dialogView.findViewById<MaterialButton>(R.id.btnConfirmEditList )

        // Display current values
        nameInput.setText(title.text.toString())
        descriptionInput.setText(description.text.toString())

        // Create dialog
        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .create()

        dialog.show()

        // Make the default dialog background transparent
        dialog.window?.setBackgroundDrawableResource( android.R.color.transparent)

        // Cancel
        cancelButton.setOnClickListener {
            dialog.dismiss()
        }

        // Save changes
        saveButton.setOnClickListener {

            val newName = nameInput.text.toString().trim()

            val newDescription =  descriptionInput.text.toString().trim()

            if (newName.isEmpty()) {

                nameInput.error = "Please enter a list name"
                return@setOnClickListener
            }

            val request = PackingListRequest(
                name = newName,
                description = newDescription
            )

            viewModel.updatePackingList(
                packingListId,
                request
            )

            // Update the page immediately
            title.text = newName
            description.text = newDescription

            dialog.dismiss()
        }
    }


    // PACKING LIST DELETE
    private fun showDeletePackingListDialog(packingListId: String, packingListName: String) {

        val dialogView = layoutInflater.inflate( R.layout.dialog_delete_list, null )

        val deleteTitle = dialogView.findViewById<TextView>(R.id.tvDeleteTitle)

        val deleteMessage = dialogView.findViewById<TextView>(R.id.tvDeleteMessage)

        val cancelButton = dialogView.findViewById<MaterialButton>(R.id.btnCancelDelete )

        val confirmButton = dialogView.findViewById<MaterialButton>(R.id.btnConfirmDelete)

        deleteTitle.text = "Delete Packing List ?"

        deleteMessage.text = "Are you sure you want to delete \"$packingListName\"?\n\n" +
                    "This will delete the list and every item inside it."

        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .create()

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        cancelButton.setOnClickListener { dialog.dismiss() }

        confirmButton.setOnClickListener {

            viewModel.deletePackingList(packingListId)

            parentFragmentManager.popBackStack()

            dialog.dismiss()
        }

        dialog.show()

        dialog.window?.setBackgroundDrawableResource(
            android.R.color.transparent
        )
    }


    // PACKING ITEM DELETE
    private fun showDeleteItemDialog(item: PackingItem, packingListId: String) {

        val dialogView = layoutInflater.inflate(R.layout.dialog_delete_item, null )

        val deleteTitle =dialogView.findViewById<TextView>(R.id.tvDeleteTitle)

        val deleteMessage = dialogView.findViewById<TextView>(R.id.tvDeleteMessage)

        val cancelButton = dialogView.findViewById<MaterialButton>(R.id.btnCancelDelete )

        val confirmButton =dialogView.findViewById<MaterialButton>(R.id.btnConfirmDelete )

        deleteTitle.text = "Delete Item? "

        deleteMessage.text = "Are you sure you want to delete \"${item.name}\"?"

        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .create()

        dialog.window?.setBackgroundDrawableResource( android.R.color.transparent )

        cancelButton.setOnClickListener {
            dialog.dismiss()
        }

        confirmButton.setOnClickListener {

            viewModel.deletePackingItem(
                item.packingItemId,
                packingListId
            )

            dialog.dismiss()
        }

        dialog.show()

        dialog.window?.setBackgroundDrawableResource( android.R.color.transparent )
    }



    // PACKING ITEM EDIT
    private fun showEditItemDialog(item: PackingItem) {

        // Load the custom XML layout
        val dialogView = layoutInflater.inflate(
            R.layout.dialog_edit_item,
            null
        )

        // Get the views from the XML
        val itemInput = dialogView.findViewById<EditText>( R.id.etItemName )

        val cancelButton = dialogView.findViewById<MaterialButton>(R.id.btnCancelEditItem )

        val saveButton = dialogView.findViewById<MaterialButton>(R.id.btnConfirmEditItem)

        // Display the current item name
        itemInput.setText(item.name)

        // Create dialog
        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .create()

        dialog.show()

        // Make the default dialog background transparent
        dialog.window?.setBackgroundDrawableResource(
            android.R.color.transparent
        )

        // Cancel
        cancelButton.setOnClickListener {
            dialog.dismiss()
        }

        // Save changes
        saveButton.setOnClickListener {

            val newName = itemInput.text.toString().trim()

            if (newName.isEmpty()) {

                itemInput.error = "Please enter an item name"
                return@setOnClickListener
            }

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