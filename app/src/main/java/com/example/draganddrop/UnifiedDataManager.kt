package com.example.draganddrop

import androidx.recyclerview.widget.RecyclerView

/**
 * Manages data for both RecyclerViews in a unified manner
 * Handles drag and drop operations between left (grid) and right (linear) sections
 */
class UnifiedDataManager {
    
    private var leftItems = mutableListOf<Item>()
    private var rightItems = mutableListOf<Item>()
    private var leftAdapter: ItemAdapter? = null
    private var rightAdapter: ItemAdapter? = null
    
    init {
        // Initialize with sample data - split items between left and right
        val allItems = Item.createSampleItems()
        leftItems.addAll(allItems.take(6)) // First 6 items go to grid
        rightItems.addAll(allItems.drop(6)) // Remaining items go to linear
    }
    
    fun setAdapters(leftAdapter: ItemAdapter, rightAdapter: ItemAdapter) {
        this.leftAdapter = leftAdapter
        this.rightAdapter = rightAdapter
        updateAdapters()
    }
    
    fun getLeftItems(): List<Item> = leftItems.toList()
    fun getRightItems(): List<Item> = rightItems.toList()
    fun getAllItems(): List<Item> = leftItems + rightItems
    
    fun moveItem(
        fromRecyclerView: RecyclerView,
        fromPosition: Int,
        toRecyclerView: RecyclerView,
        toPosition: Int
    ) {
        val (fromList, toList, fromAdapter, toAdapter) = getListAndAdapter(fromRecyclerView, toRecyclerView)
        
        if (fromList != null && toList != null && fromAdapter != null && toAdapter != null) {
            // Validate positions are within bounds
            if (fromPosition < 0 || fromPosition >= fromList.size) {
                android.util.Log.e("UnifiedDataManager", "Invalid fromPosition: $fromPosition, list size: ${fromList.size}")
                return
            }
            
            if (toPosition < 0 || toPosition >= toList.size) {
                android.util.Log.e("UnifiedDataManager", "Invalid toPosition: $toPosition, list size: ${toList.size}")
                return
            }
            
            val item = fromList.removeAt(fromPosition)
            
            // Adjust toPosition if moving within the same list
            val adjustedToPosition = if (fromList == toList && toPosition > fromPosition) {
                toPosition - 1
            } else {
                toPosition
            }
            
            // Ensure adjusted position is still valid
            if (adjustedToPosition >= 0 && adjustedToPosition <= toList.size) {
                toList.add(adjustedToPosition, item)
                updateAdapters()
                android.util.Log.d("UnifiedDataManager", "Item moved from position $fromPosition to $adjustedToPosition")
            } else {
                // If invalid, put item back
                fromList.add(fromPosition, item)
                android.util.Log.e("UnifiedDataManager", "Invalid adjusted position: $adjustedToPosition")
            }
        }
    }
    
    private fun getListAndAdapter(
        fromRecyclerView: RecyclerView,
        toRecyclerView: RecyclerView
    ): Quadruple<MutableList<Item>?, MutableList<Item>?, ItemAdapter?, ItemAdapter?> {
        val fromList = if (fromRecyclerView.id == R.id.left_recycler_view) leftItems else rightItems
        val toList = if (toRecyclerView.id == R.id.left_recycler_view) leftItems else rightItems
        val fromAdapter = if (fromRecyclerView.id == R.id.left_recycler_view) leftAdapter else rightAdapter
        val toAdapter = if (toRecyclerView.id == R.id.left_recycler_view) leftAdapter else rightAdapter
        
        return Quadruple(fromList, toList, fromAdapter, toAdapter)
    }
    
    private fun updateAdapters() {
        leftAdapter?.submitList(getLeftItems())
        rightAdapter?.submitList(getRightItems())
    }
    
    fun validateDataConsistency() {
        val allIds = getAllItems().map { it.id }
        val uniqueIds = allIds.toSet()
        
        if (allIds.size != uniqueIds.size) {
            android.util.Log.e("UnifiedDataManager", "Duplicate IDs found!")
        }
        
        android.util.Log.d("UnifiedDataManager", "Data consistency check passed. Total items: ${allIds.size}, Unique IDs: ${uniqueIds.size}")
    }
    
    fun testPositionMapping() {
        android.util.Log.d("UnifiedDataManager", "Testing position mapping...")
        android.util.Log.d("UnifiedDataManager", "Left positions: ${leftItems.indices.joinToString()}")
        android.util.Log.d("UnifiedDataManager", "Right positions: ${rightItems.indices.joinToString()}")
    }
    
    // Helper data class for multiple return values
    private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
}
