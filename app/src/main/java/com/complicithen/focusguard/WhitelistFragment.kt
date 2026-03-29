package com.complicithen.focusguard

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.ContactsContract
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.complicithen.focusguard.databinding.FragmentWhitelistBinding
import com.google.android.material.button.MaterialButton

class WhitelistFragment : Fragment() {
    private var _binding: FragmentWhitelistBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: WhitelistAdapter
    private lateinit var whitelistManager: WhitelistManager

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentWhitelistBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        whitelistManager = WhitelistManager(requireContext())

        adapter = WhitelistAdapter { entry ->
            whitelistManager.remove(entry)
            refreshList()
        }

        binding.recyclerWhitelist.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@WhitelistFragment.adapter
        }

        binding.fabAddNumber.setOnClickListener { showAddDialog() }
        refreshList()
    }

    // ─── Contact picker ───────────────────────────────────────────────────────

    private val contactPickerLauncher = registerForActivityResult(
        ActivityResultContracts.PickContact()
    ) { uri ->
        if (uri != null) handleContactPicked(uri)
    }

    private val requestContactsPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) contactPickerLauncher.launch(null)
        else Toast.makeText(requireContext(), "Contacts permission denied", Toast.LENGTH_SHORT).show()
    }

    private fun handleContactPicked(uri: Uri) {
        val cr = requireContext().contentResolver
        var displayName = ""
        cr.query(uri, arrayOf(ContactsContract.Contacts.DISPLAY_NAME_PRIMARY), null, null, null)
            ?.use { cursor ->
                if (cursor.moveToFirst()) displayName = cursor.getString(0) ?: ""
            }

        val contactId = uri.lastPathSegment ?: return
        val phones = mutableListOf<String>()
        cr.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER),
            "${ContactsContract.CommonDataKinds.Phone.CONTACT_ID} = ?",
            arrayOf(contactId),
            null
        )?.use { cursor ->
            while (cursor.moveToNext()) {
                cursor.getString(0)?.let { phones.add(it) }
            }
        }

        if (displayName.isEmpty() && phones.isEmpty()) {
            Toast.makeText(requireContext(), "Could not read contact", Toast.LENGTH_SHORT).show()
            return
        }

        // If contact has a phone number, ask whether to add name, phone, or both.
        // If no phone number (e.g. Messenger-only contact), just add the name.
        if (phones.isEmpty()) {
            whitelistManager.add(displayName)
            refreshList()
            Toast.makeText(requireContext(), "Added: $displayName", Toast.LENGTH_SHORT).show()
            return
        }

        val phone = phones.first()
        val options = buildList {
            if (displayName.isNotEmpty()) add("Name: $displayName")
            add("Phone: $phone")
            if (displayName.isNotEmpty()) add("Both name and phone")
        }.toTypedArray()

        AlertDialog.Builder(requireContext())
            .setTitle("What to whitelist?")
            .setItems(options) { _, which ->
                when {
                    options[which].startsWith("Both") -> {
                        whitelistManager.add(displayName)
                        whitelistManager.add(phone)
                        Toast.makeText(requireContext(), "Added name + phone", Toast.LENGTH_SHORT).show()
                    }
                    options[which].startsWith("Name") -> {
                        whitelistManager.add(displayName)
                        Toast.makeText(requireContext(), "Added: $displayName", Toast.LENGTH_SHORT).show()
                    }
                    else -> {
                        whitelistManager.add(phone)
                        Toast.makeText(requireContext(), "Added: $phone", Toast.LENGTH_SHORT).show()
                    }
                }
                refreshList()
            }
            .show()
    }

    // ─── Add dialog ───────────────────────────────────────────────────────────

    private fun showAddDialog() {
        val ctx = requireContext()

        val container = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(56, 16, 56, 8)
        }

        val editText = EditText(ctx).apply {
            hint = "Phone number or contact name"
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_WORDS
        }

        val btnContacts = MaterialButton(ctx, null, com.google.android.material.R.attr.borderlessButtonStyle).apply {
            text = "Choose from contacts"
            setIconResource(R.drawable.ic_person)
            iconGravity = MaterialButton.ICON_GRAVITY_TEXT_START
        }

        container.addView(editText)
        container.addView(btnContacts)

        val dialog = AlertDialog.Builder(ctx)
            .setTitle("Add to whitelist")
            .setView(container)
            .setPositiveButton("Add") { _, _ ->
                val raw = editText.text.toString().trim()
                if (raw.isEmpty()) {
                    Toast.makeText(ctx, "Enter a number or name", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                val isPhone = whitelistManager.looksLikePhone(raw)
                val stored = if (isPhone) raw.filter { it.isDigit() || it == '+' } else raw.lowercase()
                if (whitelistManager.getAll().any { it.equals(stored, ignoreCase = true) }) {
                    Toast.makeText(ctx, "Already in whitelist", Toast.LENGTH_SHORT).show()
                } else {
                    whitelistManager.add(raw)
                    refreshList()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()

        btnContacts.setOnClickListener {
            dialog.dismiss()
            if (ContextCompat.checkSelfPermission(ctx, Manifest.permission.READ_CONTACTS)
                == PackageManager.PERMISSION_GRANTED) {
                contactPickerLauncher.launch(null)
            } else {
                requestContactsPermission.launch(Manifest.permission.READ_CONTACTS)
            }
        }
    }

    // ─── List ─────────────────────────────────────────────────────────────────

    private fun refreshList() {
        val list = whitelistManager.getAll().toList().sortedWith(
            compareBy({ whitelistManager.looksLikePhone(it) }, { it })
        )
        adapter.submitList(list)
        binding.emptyState.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
