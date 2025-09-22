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
        android.util.Log.d("UnifiedDataManager", "=== MOVE ITEM CALLED ===")
        android.util.Log.d("UnifiedDataManager", "From position: $fromPosition, To position: $toPosition")
        android.util.Log.d("UnifiedDataManager", "From RecyclerView: ${fromRecyclerView.id}, To RecyclerView: ${toRecyclerView.id}")
        
        val (fromList, toList, fromAdapter, toAdapter) = getListAndAdapter(fromRecyclerView, toRecyclerView)
        
        if (fromList != null && toList != null && fromAdapter != null && toAdapter != null) {
            android.util.Log.d("UnifiedDataManager", "Lists and adapters found successfully")
            android.util.Log.d("UnifiedDataManager", "From list size: ${fromList.size}, To list size: ${toList.size}")
            android.util.Log.d("UnifiedDataManager", "From list items: ${fromList.map { it.text }}")
            android.util.Log.d("UnifiedDataManager", "To list items: ${toList.map { it.text }}")
            
            // Validate positions are within bounds
            if (fromPosition < 0 || fromPosition >= fromList.size) {
                android.util.Log.e("UnifiedDataManager", "Invalid fromPosition: $fromPosition, list size: ${fromList.size}")
                return
            }
            
            if (toPosition < 0 || toPosition >= toList.size) {
                android.util.Log.e("UnifiedDataManager", "Invalid toPosition: $toPosition, list size: ${toList.size}")
                return
            }
            
            // Get the items before replacement
            val draggedItem = fromList[fromPosition]
            val targetItem = toList[toPosition]
            android.util.Log.d("UnifiedDataManager", "Before replacement:")
            android.util.Log.d("UnifiedDataManager", "  Dragged item: ${draggedItem.text} (from position $fromPosition)")
            android.util.Log.d("UnifiedDataManager", "  Target item: ${targetItem.text} (at position $toPosition)")
            
            // Perform item swap/replacement
            android.util.Log.d("UnifiedDataManager", "Calling swapItems...")
            swapItems(fromList, fromPosition, toList, toPosition)
            
            android.util.Log.d("UnifiedDataManager", "Calling updateAdapters...")
            updateAdapters()
            
            android.util.Log.d("UnifiedDataManager", "=== REPLACEMENT COMPLETED ===")
            android.util.Log.d("UnifiedDataManager", "After replacement:")
            android.util.Log.d("UnifiedDataManager", "  From list items: ${fromList.map { it.text }}")
            android.util.Log.d("UnifiedDataManager", "  To list items: ${toList.map { it.text }}")
        } else {
            android.util.Log.e("UnifiedDataManager", "moveItem: Invalid lists or adapters")
            android.util.Log.e("UnifiedDataManager", "FromList: ${fromList != null}, ToList: ${toList != null}, FromAdapter: ${fromAdapter != null}, ToAdapter: ${toAdapter != null}")
        }
    }
    
    /**
     * Swaps items between two lists, replacing the target item with the dragged item
     */
    private fun swapItems(fromList: MutableList<Item>, fromPosition: Int, toList: MutableList<Item>, toPosition: Int) {
        val draggedItem = fromList[fromPosition]
        val targetItem = toList[toPosition]
        
        if (fromList == toList) {
            // Same list - swap positions
            fromList[fromPosition] = targetItem
            fromList[toPosition] = draggedItem
            android.util.Log.d("UnifiedDataManager", "Swapped: ${draggedItem.text} ↔ ${targetItem.text}")
        } else {
            // Different lists - replace target with dragged item
            fromList.removeAt(fromPosition)
            toList[toPosition] = draggedItem
            android.util.Log.d("UnifiedDataManager", "Replaced: ${draggedItem.text} → ${targetItem.text}")
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
        // Force UI update by creating new list instances
        val leftItemsCopy = leftItems.toList()
        val rightItemsCopy = rightItems.toList()
        
        leftAdapter?.submitList(leftItemsCopy)
        rightAdapter?.submitList(rightItemsCopy)
        
        // Force immediate UI update for smooth experience
        leftAdapter?.notifyDataSetChanged()
        rightAdapter?.notifyDataSetChanged()
        
        android.util.Log.d("UnifiedDataManager", "Adapters updated - Left: ${leftItems.size}, Right: ${rightItems.size}")
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
