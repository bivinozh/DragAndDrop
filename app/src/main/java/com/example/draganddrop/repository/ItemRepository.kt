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
    
    // Computed properties for left and right items
    val leftItems: LiveData<List<Item>> = MutableLiveData<List<Item>>().apply {
        _allItems.observeForever { items ->
            (this as MutableLiveData).value = items.take(10)
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
     * Moves an item from one position to another within the unified array
     */
    fun moveItem(fromPosition: Int, toPosition: Int) {
        val currentItems = _allItems.value?.toMutableList() ?: return
        
        if (fromPosition in currentItems.indices && toPosition in currentItems.indices) {
            val item = currentItems[fromPosition]
            if (!item.isDraggable) {
                throw IllegalStateException("Cannot move non-draggable item: ${item.text}")
            }
            
            // Check if target position contains a locked item (prevent replacement)
            val targetItem = currentItems[toPosition]
            if (!targetItem.isDraggable) {
                throw IllegalStateException("Cannot replace locked item: ${targetItem.text}")
            }
            
            currentItems.removeAt(fromPosition)
            currentItems.add(toPosition, item)
            _allItems.value = currentItems
        }
    }
    
    /**
     * Moves an item between RecyclerViews (left/right sections)
     */
    fun moveItemBetweenSections(fromRecyclerView: RecyclerViewType, fromPosition: Int, 
                               toRecyclerView: RecyclerViewType, toPosition: Int) {
        val currentItems = _allItems.value?.toMutableList() ?: return
        
        // Calculate global positions
        val globalFromPosition = when (fromRecyclerView) {
            RecyclerViewType.LEFT -> fromPosition
            RecyclerViewType.RIGHT -> 10 + fromPosition
        }
        
        val globalToPosition = when (toRecyclerView) {
            RecyclerViewType.LEFT -> toPosition
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
     * Gets an item at a specific position
     */
    fun getItemAt(position: Int): Item? {
        return _allItems.value?.getOrNull(position)
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
            RecyclerViewType.LEFT -> position
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
     * Enum to represent RecyclerView types
     */
    enum class RecyclerViewType {
        LEFT, RIGHT
    }
}
