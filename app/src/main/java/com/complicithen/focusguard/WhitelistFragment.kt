package com.complicithen.focusguard

import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DividerItemDecoration
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
            addItemDecoration(
                DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL)
            )
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
            hint = "+46701234567"
            inputType = InputType.TYPE_CLASS_PHONE
            setPadding(48, 32, 48, 16)
        }

        AlertDialog.Builder(requireContext())
            .setTitle("Add to whitelist")
            .setMessage("This number will always be able to reach you, even during focus mode.")
            .setView(editText)
            .setPositiveButton("Add") { _, _ ->
                val raw = editText.text.toString().trim()
                val normalized = raw.filter { it.isDigit() || it == '+' }
                when {
                    normalized.isEmpty() ->
                        Toast.makeText(requireContext(), "Enter a valid phone number", Toast.LENGTH_SHORT).show()
                    whitelistManager.getAll().contains(normalized) ->
                        Toast.makeText(requireContext(), "Already in whitelist", Toast.LENGTH_SHORT).show()
                    else -> {
                        whitelistManager.add(raw)
                        refreshList()
                    }
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
