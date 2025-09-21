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
        android.util.Log.d("UnifiedDataManager", "Before move - Left items: ${allItems.take(10).map { it.text }}, Right items: ${allItems.drop(10).map { it.text }}")
        
        // Calculate global positions in the unified list
        val globalFromPosition = if (isFromLeft) fromPosition else 10 + fromPosition
        val globalToPosition = if (isToLeft) toPosition else 10 + toPosition
        
        android.util.Log.d("UnifiedDataManager", "Position mapping:")
        android.util.Log.d("UnifiedDataManager", "  From: ${if (isFromLeft) "left" else "right"} pos $fromPosition -> global $globalFromPosition")
        android.util.Log.d("UnifiedDataManager", "  To: ${if (isToLeft) "left" else "right"} pos $toPosition -> global $globalToPosition")
        
        android.util.Log.d("UnifiedDataManager", "Global positions: from $globalFromPosition to $globalToPosition")
        
        // Validate positions before proceeding
        if (globalFromPosition < 0 || globalFromPosition >= allItems.size) {
            android.util.Log.e("UnifiedDataManager", "Invalid from position: $globalFromPosition (size: ${allItems.size})")
            return
        }
        
        if (globalToPosition < 0 || globalToPosition > allItems.size) {
            android.util.Log.e("UnifiedDataManager", "Invalid to position: $globalToPosition (size: ${allItems.size})")
            return
        }
        
        // Perform the move in the unified list
        if (globalFromPosition != globalToPosition) {
            val item = allItems.removeAt(globalFromPosition)
            
            // Adjust target position if we removed an item before it
            val adjustedToPosition = if (globalFromPosition < globalToPosition) {
                globalToPosition - 1
            } else {
                globalToPosition
            }
            
            // Ensure the position is within bounds
            val finalPosition = adjustedToPosition.coerceIn(0, allItems.size)
            
            android.util.Log.d("UnifiedDataManager", "Adjusted position: $adjustedToPosition, Final position: $finalPosition, allItems size: ${allItems.size}")
            
            // Insert at the calculated position
            allItems.add(finalPosition, item)
            
            android.util.Log.d("UnifiedDataManager", "After move - Left items: ${allItems.take(10).map { it.text }}, Right items: ${allItems.drop(10).map { it.text }}")
        } else {
            android.util.Log.d("UnifiedDataManager", "Same position, no move needed")
        }
        
        // Update both adapters with the unified data
        updateAdapters()
        
        // Debug the final state
        debugCurrentState()
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
    
    fun validateDataConsistency() {
        android.util.Log.d("UnifiedDataManager", "Validating data consistency...")
        android.util.Log.d("UnifiedDataManager", "Total items: ${allItems.size}")
        android.util.Log.d("UnifiedDataManager", "Left items count: ${allItems.take(10).size}")
        android.util.Log.d("UnifiedDataManager", "Right items count: ${allItems.drop(10).size}")
        
        if (allItems.size != 12) {
            android.util.Log.e("UnifiedDataManager", "Data inconsistency: Expected 12 items, got ${allItems.size}")
        }
        
        if (allItems.take(10).size != 10) {
            android.util.Log.e("UnifiedDataManager", "Left RecyclerView inconsistency: Expected 10 items, got ${allItems.take(10).size}")
        }
        
        if (allItems.drop(10).size != 2) {
            android.util.Log.e("UnifiedDataManager", "Right RecyclerView inconsistency: Expected 2 items, got ${allItems.drop(10).size}")
        }
    }
    
    fun debugCurrentState() {
        android.util.Log.d("UnifiedDataManager", "=== CURRENT STATE DEBUG ===")
        android.util.Log.d("UnifiedDataManager", "All items: ${allItems.mapIndexed { index, item -> "$index:${item.text}" }}")
        android.util.Log.d("UnifiedDataManager", "Left items (0-9): ${allItems.take(10).mapIndexed { index, item -> "$index:${item.text}" }}")
        android.util.Log.d("UnifiedDataManager", "Right items (10-11): ${allItems.drop(10).mapIndexed { index, item -> "${index + 10}:${item.text}" }}")
        android.util.Log.d("UnifiedDataManager", "=== END DEBUG ===")
    }
}
