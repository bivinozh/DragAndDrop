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
        
        fun bind(item: Item) {
            textView.text = item.text
            if (item.isDraggable) {
                textView.setBackgroundColor(item.color)
                textView.alpha = 1.0f
            } else {
                // Make non-draggable items appear disabled
                textView.setBackgroundColor(item.color)
                textView.alpha = 0.5f
                textView.text = "${item.text} (Fixed)"
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_layout, parent, false)
        return ItemViewHolder(view)
    }

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        val item = getItem(position)
        holder.bind(item)
        
        holder.itemView.setOnClickListener {
            if (item.isDraggable) {
                onItemClick(item)
            }
        }
        
        holder.itemView.setOnLongClickListener {
            if (item.isDraggable) {
                onItemLongClick(item)
                true
            } else {
                false // Don't handle long click for non-draggable items
            }
        }
    }

    /**
     * Public method to get an item at a specific position
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

