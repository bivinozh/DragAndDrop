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
        
        android.util.Log.d("UnifiedDataManager", "Moving item from ${if (isFromLeft) "left" else "right"} pos $fromPosition to ${if (isToLeft) "left" else "right"} pos $toPosition")
        
        // Calculate global positions in the unified list
        val globalFromPosition = if (isFromLeft) fromPosition else 10 + fromPosition
        val globalToPosition = if (isToLeft) toPosition else 10 + toPosition
        
        android.util.Log.d("UnifiedDataManager", "Global positions: from $globalFromPosition to $globalToPosition")
        
        // Perform the move in the unified list
        if (globalFromPosition != globalToPosition && globalFromPosition < allItems.size) {
            val item = allItems.removeAt(globalFromPosition)
            
            // Adjust target position if we removed an item before it
            val adjustedToPosition = if (globalFromPosition < globalToPosition) {
                globalToPosition - 1
            } else {
                globalToPosition
            }
            
            // Special handling for right RecyclerView last position
            val finalPosition = if (isToLeft) {
                // For left RecyclerView, ensure position is within 0-9
                adjustedToPosition.coerceIn(0, 10)
            } else {
                // For right RecyclerView, ensure position is within 10-11
                val rightPosition = adjustedToPosition - 10
                val clampedRightPosition = rightPosition.coerceIn(0, 1) // 0 or 1 for right RecyclerView
                10 + clampedRightPosition
            }
            
            android.util.Log.d("UnifiedDataManager", "Adjusted position: $adjustedToPosition, Final position: $finalPosition, allItems size: ${allItems.size}")
            
            // Insert at the calculated position
            allItems.add(finalPosition, item)
            
            android.util.Log.d("UnifiedDataManager", "After move - Left items: ${allItems.take(10).map { it.text }}, Right items: ${allItems.drop(10).map { it.text }}")
        } else if (globalFromPosition == globalToPosition) {
            android.util.Log.d("UnifiedDataManager", "Same position, no move needed")
        } else {
            android.util.Log.d("UnifiedDataManager", "Invalid move: from $globalFromPosition (size: ${allItems.size}) to $globalToPosition")
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
