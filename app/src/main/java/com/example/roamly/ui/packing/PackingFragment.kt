package com.example.roamly.ui.packing

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ProgressBar
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.roamly.R
import com.example.roamly.data.model.PackingListRequest
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.launch


class PackingFragment : Fragment(R.layout.fragment_packing) {

    private val viewModel: PackingViewModel by activityViewModels()
    private lateinit var adapter: PackingListAdapter


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        // Get the views from the XML file
        val recyclerView = view.findViewById<RecyclerView>(R.id.recyclerPackingLists)
        val progressBar = view.findViewById<ProgressBar>(R.id.progressBar)
        val addPackingListButton = view.findViewById<Button>(R.id.btnAddPackingList)

        // Set up RecyclerView
        recyclerView.layoutManager =  LinearLayoutManager(requireContext())

        // Set up adapter
        adapter = PackingListAdapter( emptyList(),

            // Click packing list
            { packingList ->

                val fragment = PackingItemsFragment()
                val bundle = Bundle()

                bundle.putString(
                    "packingListId",
                    packingList.packingListId
                )

                bundle.putString(
                    "packingListName",
                    packingList.name
                )

                bundle.putString(
                    "packingListDescription",
                    packingList.description ?: ""
                )

                fragment.arguments = bundle


                parentFragmentManager.beginTransaction()
                    .replace(
                        R.id.fragmentContainer,
                        fragment
                    )
                    .addToBackStack(null)
                    .commit()
            }
        )


        recyclerView.adapter = adapter

        // Add packing list button
        addPackingListButton.setOnClickListener {

            // Load the custom XML layout
            val dialogView = layoutInflater.inflate(
                R.layout.dialog_create_list,
                null
            )

            // Get the views from the XML
            val nameInput = dialogView.findViewById<EditText>(R.id.etCreateListName)

            val descriptionInput = dialogView.findViewById<EditText>(R.id.etCreateListDescription )

            val cancelButton =  dialogView.findViewById<MaterialButton>(R.id.btnCancelCreateList)

            val createButton = dialogView.findViewById<MaterialButton>( R.id.btnConfirmCreateList)

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

            // Create list
            createButton.setOnClickListener {

                val name = nameInput.text.toString().trim()

                val description = descriptionInput.text.toString().trim()

                // Make sure a list name was entered
                if (name.isEmpty()) {

                    nameInput.error = "Please enter a list name"
                    return@setOnClickListener
                }

                // Create the request
                val request = PackingListRequest(
                    name = name,
                    description = description
                )

                // Add the list
                viewModel.createPackingList(request)

                // Close dialog
                dialog.dismiss()
            }
        }



        // Observe packing lists
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(
                Lifecycle.State.STARTED
            ) {

                launch {
                    viewModel.packingLists.collect { lists ->

                        Log.d(
                            "PackingFragment",
                            "Packing lists: $lists"
                        )

                        adapter.updateLists(lists)
                    }
                }
                // Observe packing progress (progress bar updates on main page as well)
                launch {
                    viewModel.packingProgress.collect { progress ->

                        adapter.updateProgress(progress)
                    }
                }


                // Observe loading state
                launch {
                    viewModel.isLoading.collect { loading ->

                        progressBar.visibility =
                            if (loading) View.VISIBLE
                            else View.GONE
                    }
                }


                // Observe error messages
                launch {
                    viewModel.errorMessage.collect { error ->

                        if (error != null) {

                            AlertDialog.Builder(requireContext())
                                .setTitle("Error")
                                .setMessage(error)
                                .setPositiveButton("OK", null)
                                .show()

                            viewModel.clearErrorMessage()
                        }
                    }
                }
            }
        }


        // Load packing lists when the screen opens
        viewModel.getPackingLists()
    }
}