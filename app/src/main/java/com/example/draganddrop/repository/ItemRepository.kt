package com.example.draganddrop.repository

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.draganddrop.Item

/**
 * Repository pattern implementation for managing item arrays.
 * Handles data operations and provides reactive data streams.
 */
class ItemRepository {
    
    // Private mutable data source
    private val _allItems = MutableLiveData<List<Item>>()
    val allItems: LiveData<List<Item>> = _allItems
    
    // Store the saved order (initially the original order)
    private var savedOrder: List<Item> = Item.createSampleItems()

    // Computed properties for left and right items
    val leftItems: LiveData<List<Item>> = MutableLiveData<List<Item>>().apply {
        _allItems.observeForever { items ->
            // Arrange items for display: 1,6,2,7,3,8,4,9,5,10
            val displayOrder = mutableListOf<Item>()
            val itemsList = items.take(10)
            
            // Add items in alternating pattern for column display
            for (i in 0..4) {
                if (i < itemsList.size) {
                    displayOrder.add(itemsList[i])      // Items 1,2,3,4,5
                }
                if (i + 5 < itemsList.size) {
                    displayOrder.add(itemsList[i + 5])  // Items 6,7,8,9,10
                }
            }
            
            (this as MutableLiveData).value = displayOrder
        }
    }
    
    val rightItems: LiveData<List<Item>> = MutableLiveData<List<Item>>().apply {
        _allItems.observeForever { items ->
            (this as MutableLiveData).value = items.drop(10)
        }
    }
    
    init {
        // Initialize with sample data
        _allItems.value = Item.createSampleItems()
    }

    /**
     * Moves an item between RecyclerViews (left/right sections)
     */
    fun moveItemBetweenSections(fromRecyclerView: RecyclerViewType, fromPosition: Int, 
                               toRecyclerView: RecyclerViewType, toPosition: Int) {
        val currentItems = _allItems.value?.toMutableList() ?: return
        
        // Calculate global positions
        val globalFromPosition = when (fromRecyclerView) {
            RecyclerViewType.LEFT -> leftDisplayPositionToArray(fromPosition)
            RecyclerViewType.RIGHT -> 10 + fromPosition
        }
        
        val globalToPosition = when (toRecyclerView) {
            RecyclerViewType.LEFT -> leftDisplayPositionToArray(toPosition)
            RecyclerViewType.RIGHT -> 10 + toPosition
        }
        
        if (globalFromPosition in currentItems.indices && globalToPosition in currentItems.indices) {
            val item = currentItems[globalFromPosition]
            if (!item.isDraggable) {
                throw IllegalStateException("Cannot move non-draggable item: ${item.text}")
            }
            
            // Check if target position contains a locked item (prevent replacement)
            val targetItem = currentItems[globalToPosition]
            if (!targetItem.isDraggable) {
                throw IllegalStateException("Cannot replace locked item: ${targetItem.text}")
            }
            
            currentItems.removeAt(globalFromPosition)
            currentItems.add(globalToPosition, item)
            _allItems.value = currentItems
        }
    }
    
    /**
     * Resets the data to initial state
     */
    fun resetToInitialState() {
        _allItems.value = Item.createSampleItems()
    }


    /**
     * Gets the current size of the unified array
     */
    fun getItemCount(): Int {
        return _allItems.value?.size ?: 0
    }

    /**
     * Checks if an item at a specific position is draggable
     */
    fun isItemDraggable(position: Int): Boolean {
        return _allItems.value?.getOrNull(position)?.isDraggable ?: false
    }

    /**
     * Checks if an item at a specific RecyclerView position is draggable
     */
    fun isItemDraggable(recyclerViewType: RecyclerViewType, position: Int): Boolean {
        val globalPosition = when (recyclerViewType) {
            RecyclerViewType.LEFT -> leftDisplayPositionToArray(position)
            RecyclerViewType.RIGHT -> 10 + position
        }
        return isItemDraggable(globalPosition)
    }
    
    /**
     * Gets all items from the repository
     */
    fun getAllItems(): List<Item> {
        return _allItems.value ?: emptyList()
    }
    
    /**
     * Refreshes and updates all repository values
     */
    fun refreshAllValues() {
        val currentItems = _allItems.value ?: emptyList()
        // Trigger LiveData update by setting the same value
        _allItems.value = currentItems.toList()
        android.util.Log.d("ItemRepository", "Repository values refreshed - Total items: ${currentItems.size}")
    }
    
    /**
     * Saves the current order as the new default order
     */
    fun saveCurrentOrder() {
        val currentItems = _allItems.value ?: emptyList()
        savedOrder = currentItems.toList()
        android.util.Log.d("ItemRepository", "Current order saved as new default - Total items: ${savedOrder.size}")
    }
    
    /**
     * Resets the data to the saved order (not original order)
     */
    fun resetToSavedOrder() {
        _allItems.value = savedOrder.toList()
        android.util.Log.d("ItemRepository", "Data reset to saved order - Total items: ${savedOrder.size}")
    }
    
    /**
     * Maps display position to array position for left RecyclerView
     * Display order: 1,6,2,7,3,8,4,9,5,10
     * Array order: 1,2,3,4,5,6,7,8,9,10
     */
    private fun leftDisplayPositionToArray(displayPosition: Int): Int {
        return when (displayPosition) {
            0 -> 0  // Display Item 1 -> Array Item 1
            1 -> 5  // Display Item 6 -> Array Item 6
            2 -> 1  // Display Item 2 -> Array Item 2
            3 -> 6  // Display Item 7 -> Array Item 7
            4 -> 2  // Display Item 3 -> Array Item 3
            5 -> 7  // Display Item 8 -> Array Item 8
            6 -> 3  // Display Item 4 -> Array Item 4
            7 -> 8  // Display Item 9 -> Array Item 9
            8 -> 4  // Display Item 5 -> Array Item 5
            9 -> 9  // Display Item 10 -> Array Item 10
            else -> displayPosition
        }
    }
    
    /**
     * Maps array position to display position for left RecyclerView
     */
    private fun leftArrayPositionToDisplay(arrayPosition: Int): Int {
        return when (arrayPosition) {
            0 -> 0  // Array Item 1 -> Display Item 1
            1 -> 2  // Array Item 2 -> Display Item 2
            2 -> 4  // Array Item 3 -> Display Item 3
            3 -> 6  // Array Item 4 -> Display Item 4
            4 -> 8  // Array Item 5 -> Display Item 5
            5 -> 1  // Array Item 6 -> Display Item 6
            6 -> 3  // Array Item 7 -> Display Item 7
            7 -> 5  // Array Item 8 -> Display Item 8
            8 -> 7  // Array Item 9 -> Display Item 9
            9 -> 9  // Array Item 10 -> Display Item 10
            else -> arrayPosition
        }
    }
    
    /**
     * Enum to represent RecyclerView types
     */
    enum class RecyclerViewType {
        LEFT, RIGHT
    }
}
