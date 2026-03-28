package com.complicithen.focusguard

import android.os.Bundle
import android.text.InputType
import android.text.InputType.TYPE_CLASS_TEXT
import android.text.InputType.TYPE_TEXT_FLAG_CAP_WORDS
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.complicithen.focusguard.databinding.FragmentWhitelistBinding

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

        adapter = WhitelistAdapter { number ->
            whitelistManager.remove(number)
            refreshList()
        }

        binding.recyclerWhitelist.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@WhitelistFragment.adapter
        }

        binding.fabAddNumber.setOnClickListener { showAddDialog() }
        refreshList()
    }

    private fun refreshList() {
        val list = whitelistManager.getAll().toList().sorted()
        adapter.submitList(list)
        binding.emptyState.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun showAddDialog() {
        val editText = EditText(requireContext()).apply {
            hint = "Phone number or contact name"
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_WORDS
            setPadding(48, 32, 48, 16)
        }

        AlertDialog.Builder(requireContext())
            .setTitle("Add to whitelist")
            .setMessage("Enter a phone number (+46...) OR a contact name as shown in Messenger, WhatsApp, etc. Names are matched against notification senders.")
            .setView(editText)
            .setPositiveButton("Add") { _, _ ->
                val raw = editText.text.toString().trim()
                if (raw.isEmpty()) {
                    Toast.makeText(requireContext(), "Enter a number or name", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                val isPhone = whitelistManager.looksLikePhone(raw)
                val stored = if (isPhone) raw.filter { it.isDigit() || it == '+' } else raw.lowercase()
                if (whitelistManager.getAll().any { it.equals(stored, ignoreCase = true) }) {
                    Toast.makeText(requireContext(), "Already in whitelist", Toast.LENGTH_SHORT).show()
                } else {
                    whitelistManager.add(raw)
                    refreshList()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
