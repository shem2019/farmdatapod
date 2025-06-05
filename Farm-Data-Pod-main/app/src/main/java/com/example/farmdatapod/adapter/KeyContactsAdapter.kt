
package com.example.farmdatapod.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.widget.addTextChangedListener
import androidx.recyclerview.widget.RecyclerView
import com.example.farmdatapod.databinding.ItemKeyContactBinding
import com.example.farmdatapod.models.KeyContact as ModelKeyContact
import com.google.android.material.textfield.TextInputEditText
import java.text.SimpleDateFormat
import java.util.*

data class KeyContact(
    var firstName: String = "",
    var lastName: String = "",
    var gender: String = "",
    var role: String = "",
    var dateOfBirth: String = "1990-01-01T00:00:00", // Default date format
    var displayDateOfBirth: String = "",
    var email: String = "",
    var phoneNumber: String = "",
    var idNumber: String = ""
)

fun KeyContact.toModelKeyContact(): ModelKeyContact {
    return ModelKeyContact(
        date_of_birth = this.dateOfBirth.ifEmpty { "1990-01-01T00:00:00" },
        email = this.email,
        gender = this.gender.trim(),
        id_number = this.idNumber.toIntOrNull() ?: 0,
        last_name = this.lastName.trim(),
        other_name = this.firstName.trim(),
        phone_number = this.phoneNumber,
        role = this.role.trim()
    )
}

class KeyContactAdapter(
    private var contacts: MutableList<KeyContact>,
    private val showDatePickerDialog: (TextInputEditText, Int) -> Unit
) : RecyclerView.Adapter<KeyContactAdapter.KeyContactViewHolder>() {

    private val apiDateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
    private val displayDateFormat = SimpleDateFormat("MMMM d, yyyy", Locale.getDefault())

    class KeyContactViewHolder(val binding: ItemKeyContactBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): KeyContactViewHolder {
        val binding = ItemKeyContactBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return KeyContactViewHolder(binding)
    }

    override fun onBindViewHolder(holder: KeyContactViewHolder, position: Int) {
        val contact = contacts[position]
        holder.binding.apply {
            etKeyContactFirstName.setText(contact.firstName)
            etKeyContactLastName.setText(contact.lastName)
            etKeyContactGender.setText(contact.gender)
            etKeyContactRole.setText(contact.role)
            etKeyContactDateOfBirth.setText(contact.displayDateOfBirth)
            etKeyContactEmail.setText(contact.email)
            etKeyContactPhoneNumber.setText(contact.phoneNumber)
            etKeyContactIdNumber.setText(contact.idNumber)

            // Set up date picker
            etKeyContactDateOfBirth.setOnClickListener {
                showDatePickerDialog(etKeyContactDateOfBirth, position)
            }

            // Set up text change listeners to update the contact object
            etKeyContactFirstName.addTextChangedListener { contact.firstName = it?.toString() ?: "" }
            etKeyContactLastName.addTextChangedListener { contact.lastName = it?.toString() ?: "" }
            etKeyContactGender.addTextChangedListener { contact.gender = it?.toString() ?: "" }
            etKeyContactRole.addTextChangedListener { contact.role = it?.toString() ?: "" }
            etKeyContactEmail.addTextChangedListener { contact.email = it?.toString() ?: "" }
            etKeyContactPhoneNumber.addTextChangedListener { contact.phoneNumber = it?.toString() ?: "" }
            etKeyContactIdNumber.addTextChangedListener { contact.idNumber = it?.toString() ?: "" }
        }
    }

    override fun getItemCount() = contacts.size

    fun addContact() {
        contacts.add(KeyContact())
        notifyItemInserted(contacts.size - 1)
    }

    fun getContacts(): List<KeyContact> = contacts

    fun getModelContacts(): List<ModelKeyContact> = contacts.map { it.toModelKeyContact() }

    fun setContacts(newContacts: MutableList<KeyContact>) {
        contacts = newContacts
        notifyDataSetChanged()
    }

    fun updateContactDate(position: Int, apiDate: String, displayDate: String) {
        contacts[position].dateOfBirth = apiDate
        contacts[position].displayDateOfBirth = displayDate
        notifyItemChanged(position)
    }
}
