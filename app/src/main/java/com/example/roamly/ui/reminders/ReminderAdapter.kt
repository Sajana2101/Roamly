
package com.example.roamly.ui.reminders

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.roamly.R
import com.example.roamly.data.model.Reminder
import com.google.android.material.button.MaterialButton
import com.google.android.material.switchmaterial.SwitchMaterial
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ReminderAdapter(
    private val onEdit: (Reminder) -> Unit,
    private val onDelete: (Reminder) -> Unit,
    private val onEnabledChange: (Reminder, Boolean) -> Unit,
    private val holidayNameForId: (Int?) -> String?
) : RecyclerView.Adapter<ReminderAdapter.ReminderViewHolder>() {

    private var reminders: List<Reminder> = emptyList()

    class ReminderViewHolder(view: View) :
        RecyclerView.ViewHolder(view) {

        val title: TextView =
            view.findViewById(R.id.reminderItemTitle)

        val date: TextView =
            view.findViewById(R.id.reminderItemDate)

        val enabledSwitch: SwitchMaterial =
            view.findViewById(R.id.reminderItemSwitch)

        val editButton: MaterialButton =
            view.findViewById(R.id.reminderItemEditButton)

        val deleteButton: MaterialButton =
            view.findViewById(R.id.reminderItemDeleteButton)
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ReminderViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_reminder, parent, false)

        return ReminderViewHolder(view)
    }

    override fun onBindViewHolder(
        holder: ReminderViewHolder,
        position: Int
    ) {
        val reminder = reminders[position]

        holder.title.text = reminder.title

        val dateFormat = SimpleDateFormat(
            "dd MMM yyyy, HH:mm",
            Locale.getDefault()
        )

        val details = mutableListOf(
            dateFormat.format(Date(reminder.scheduledAtMillis))
        )

        holidayNameForId(reminder.holidayId)?.let {
            details.add("Holiday: $it")
        }

        if (reminder.description.isNotBlank()) {
            details.add(reminder.description)
        }

        holder.date.text = details.joinToString("\n")

        holder.enabledSwitch.setOnCheckedChangeListener(null)
        holder.enabledSwitch.isChecked = reminder.isEnabled

        holder.enabledSwitch.setOnCheckedChangeListener { _, checked ->
            onEnabledChange(reminder, checked)
        }

        holder.editButton.setOnClickListener {
            onEdit(reminder)
        }

        holder.deleteButton.setOnClickListener {
            onDelete(reminder)
        }
    }

    override fun getItemCount(): Int = reminders.size

    fun updateReminders(newReminders: List<Reminder>) {
        reminders = newReminders.toList()
        notifyDataSetChanged()
    }
}
