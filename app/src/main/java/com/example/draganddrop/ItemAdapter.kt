package com.example.draganddrop

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView

class ItemAdapter(
    private val onItemClick: (Item) -> Unit = {},
    private val onItemLongClick: (Item) -> Unit = {}
) : ListAdapter<Item, ItemAdapter.ItemViewHolder>(ItemDiffCallback()) {

    class ItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textView: TextView = itemView.findViewById(R.id.item_text)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_layout, parent, false)
        return ItemViewHolder(view)
    }

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        val item = getItem(position)
        holder.textView.text = item.text
        holder.textView.setBackgroundColor(item.color)
        
        // Visual indication for non-draggable items
        if (!item.isDraggable) {
            holder.itemView.alpha = 0.6f
            holder.textView.text = "${item.text} (Locked)"
        } else {
            holder.itemView.alpha = 1.0f
        }
        
        holder.itemView.setOnClickListener {
            onItemClick(item)
        }
        
        // Only allow long click for draggable items
        holder.itemView.setOnLongClickListener {
            if (item.isDraggable) {
                onItemLongClick(item)
                true
            } else {
                false // Prevent long click for non-draggable items
            }
        }
    }

    /**
     * Public method to get item at specific position
     */
    fun getItemAt(position: Int): Item? {
        return if (position >= 0 && position < itemCount) {
            getItem(position)
        } else {
            null
        }
    }

    class ItemDiffCallback : DiffUtil.ItemCallback<Item>() {
        override fun areItemsTheSame(oldItem: Item, newItem: Item): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Item, newItem: Item): Boolean {
            return oldItem == newItem
        }
    }
}
