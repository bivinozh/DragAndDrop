package com.example.draganddrop.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.draganddrop.Item
import com.example.draganddrop.repository.ItemRepository
import kotlinx.coroutines.launch

/**
 * ViewModel for managing drag and drop operations using MVVM pattern.
 * Acts as a bridge between the UI and the repository.
 */
class DragDropViewModel : ViewModel() {
    
    private val repository = ItemRepository()
    
    // Expose repository data to UI
    val allItems: LiveData<List<Item>> = repository.allItems
    val leftItems: LiveData<List<Item>> = repository.leftItems
    val rightItems: LiveData<List<Item>> = repository.rightItems
    
    // UI state management
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading
    
    private val _dragOperationResult = MutableLiveData<String>()
    val dragOperationResult: LiveData<String> = _dragOperationResult
    
    /**
     * Moves an item within the same RecyclerView
     */
    fun moveItemWithinRecyclerView(fromPosition: Int, toPosition: Int) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                repository.moveItem(fromPosition, toPosition)
                _dragOperationResult.value = "Item moved successfully"
            } catch (e: IllegalStateException) {
                // Handle specific locked item errors
                _dragOperationResult.value = e.message ?: "Cannot move item"
            } catch (e: Exception) {
                _dragOperationResult.value = "Error moving item: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * Moves an item between different RecyclerViews
     */
    fun moveItemBetweenRecyclerViews(
        fromRecyclerView: ItemRepository.RecyclerViewType,
        fromPosition: Int,
        toRecyclerView: ItemRepository.RecyclerViewType,
        toPosition: Int
    ) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                repository.moveItemBetweenSections(
                    fromRecyclerView, fromPosition,
                    toRecyclerView, toPosition
                )
                _dragOperationResult.value = "Item moved between sections successfully"
            } catch (e: IllegalStateException) {
                // Handle specific locked item errors
                _dragOperationResult.value = e.message ?: "Cannot move item"
            } catch (e: Exception) {
                _dragOperationResult.value = "Error moving item between sections: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * Resets the data to initial state
     */
    fun resetData() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                repository.resetToInitialState()
                _dragOperationResult.value = "Data reset successfully"
            } catch (e: Exception) {
                _dragOperationResult.value = "Error resetting data: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * Gets an item at a specific position
     */
    fun getItemAt(position: Int): Item? {
        return repository.getItemAt(position)
    }
    
    /**
     * Gets the current item count
     */
    fun getItemCount(): Int {
        return repository.getItemCount()
    }
    
    /**
     * Clears the drag operation result message
     */
    fun clearDragResult() {
        _dragOperationResult.value = null
    }
    
    /**
     * Checks if an item at a specific position is draggable
     */
    fun isItemDraggable(position: Int): Boolean {
        return repository.isItemDraggable(position)
    }
    
    /**
     * Checks if an item at a specific RecyclerView position is draggable
     */
    fun isItemDraggable(recyclerViewType: ItemRepository.RecyclerViewType, position: Int): Boolean {
        return repository.isItemDraggable(recyclerViewType, position)
    }
    
    /**
     * Gets all items from the repository
     */
    fun getAllItems(): List<Item> {
        return repository.getAllItems()
    }
}
