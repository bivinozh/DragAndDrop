package com.example.draganddrop

import androidx.recyclerview.widget.RecyclerView

class UnifiedDataManager {
    
    // Single unified data source - all items in one list
    private val allItems: MutableList<Item> = Item.createSampleItems().toMutableList()
    
    // Adapters
    private var leftAdapter: ItemAdapter? = null
    private var rightAdapter: ItemAdapter? = null
    
    fun setAdapters(leftAdapter: ItemAdapter, rightAdapter: ItemAdapter) {
        this.leftAdapter = leftAdapter
        this.rightAdapter = rightAdapter
        updateAdapters()
    }
    
    fun getLeftItems(): List<Item> = allItems.take(10)
    fun getRightItems(): List<Item> = allItems.drop(10)
    
    fun moveItem(fromRecyclerView: RecyclerView, fromPosition: Int, toRecyclerView: RecyclerView, toPosition: Int) {
        val isFromLeft = fromRecyclerView.id == R.id.left_recycler_view
        val isToLeft = toRecyclerView.id == R.id.left_recycler_view
        
        // Calculate global positions in the unified list
        val globalFromPosition = if (isFromLeft) fromPosition else 10 + fromPosition
        val globalToPosition = if (isToLeft) toPosition else 10 + toPosition
        
        // Perform the move in the unified list
        if (globalFromPosition != globalToPosition && globalFromPosition < allItems.size) {
            val item = allItems.removeAt(globalFromPosition)
            
            // Adjust target position if we removed an item before it
            val adjustedToPosition = if (globalFromPosition < globalToPosition) {
                globalToPosition - 1
            } else {
                globalToPosition
            }
            
            // Ensure the position is within bounds
            val finalPosition = adjustedToPosition.coerceIn(0, allItems.size)
            allItems.add(finalPosition, item)
        }
        
        // Update both adapters with the unified data
        updateAdapters()
    }
    
    private fun updateAdapters() {
        // Both adapters get their respective portions of the unified data
        leftAdapter?.submitList(allItems.take(10))
        rightAdapter?.submitList(allItems.drop(10))
    }
    
    fun getItemAtPosition(recyclerView: RecyclerView, position: Int): Item? {
        val globalPosition = if (recyclerView.id == R.id.left_recycler_view) {
            position
        } else {
            10 + position
        }
        
        return if (globalPosition < allItems.size) {
            allItems[globalPosition]
        } else {
            null
        }
    }
    
    fun getAllItems(): List<Item> = allItems.toList()
    
    fun getGlobalPosition(recyclerView: RecyclerView, position: Int): Int {
        return if (recyclerView.id == R.id.left_recycler_view) {
            position
        } else {
            10 + position
        }
    }
}
