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

        fun bind(number: String) {
            binding.textNumber.text = number
            binding.btnDelete.setOnClickListener { onDelete(number) }
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

    companion object {
        val DIFF = object : DiffUtil.ItemCallback<String>() {
            override fun areItemsTheSame(a: String, b: String) = a == b
            override fun areContentsTheSame(a: String, b: String) = a == b
        }
    }
}
