package com.complicithen.focusguard

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.complicithen.focusguard.databinding.ItemWhitelistBinding

class WhitelistAdapter(
    private val onDelete: (String) -> Unit
) : ListAdapter<String, WhitelistAdapter.ViewHolder>(DIFF) {

    inner class ViewHolder(private val binding: ItemWhitelistBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(entry: String) {
            val isPhone = looksLikePhone(entry)
            binding.textNumber.text = if (isPhone) entry else toTitleCase(entry)
            binding.textType.text = if (isPhone) "Phone number" else "Contact name (matched in Messenger, WhatsApp, etc.)"
            binding.iconType.setImageResource(if (isPhone) R.drawable.ic_phone else R.drawable.ic_person)
            binding.btnDelete.setOnClickListener { onDelete(entry) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        ViewHolder(
            ItemWhitelistBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            )
        )

    override fun onBindViewHolder(holder: ViewHolder, position: Int) =
        holder.bind(getItem(position))

    private fun looksLikePhone(entry: String) = entry.filter { it.isDigit() }.length >= 4

    private fun toTitleCase(s: String) = s.split(" ").joinToString(" ") { word ->
        word.replaceFirstChar { it.titlecase() }
    }

    companion object {
        val DIFF = object : DiffUtil.ItemCallback<String>() {
            override fun areItemsTheSame(a: String, b: String) = a == b
            override fun areContentsTheSame(a: String, b: String) = a == b
        }
    }
}
