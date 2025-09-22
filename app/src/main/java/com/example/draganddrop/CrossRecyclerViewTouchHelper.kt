package com.example.draganddrop

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView

class CrossRecyclerViewTouchHelper(
    private val dataManager: UnifiedDataManager,
    private val leftRecyclerView: RecyclerView,
    private val rightRecyclerView: RecyclerView
) : ItemTouchHelper.SimpleCallback(
    ItemTouchHelper.UP or ItemTouchHelper.DOWN or ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT,
    0
) {

    private var draggedFromPosition = -1
    private var draggedFromRecyclerView: RecyclerView? = null
    private var isDraggingOverOther = false
    private var targetRecyclerView: RecyclerView? = null

    override fun onMove(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        target: RecyclerView.ViewHolder
    ): Boolean {
        val fromPosition = viewHolder.adapterPosition
        val toPosition = target.adapterPosition
        
        if (fromPosition == RecyclerView.NO_POSITION || toPosition == RecyclerView.NO_POSITION) {
            return false
        }
        
        // Only allow drops on existing items (not empty areas)
        if (!isValidDropTarget(recyclerView, target)) {
            android.util.Log.d("CrossRecyclerViewTouchHelper", "Invalid drop target - not dropping on an item")
            return false
        }
        
        return try {
            // Handle moves within the same RecyclerView
            if (recyclerView == draggedFromRecyclerView) {
                dataManager.moveItem(recyclerView, fromPosition, recyclerView, toPosition)
                true
            } else if (draggedFromRecyclerView != null && draggedFromRecyclerView != recyclerView) {
                // Handle moves between different RecyclerViews
                dataManager.moveItem(draggedFromRecyclerView!!, draggedFromPosition, recyclerView, toPosition)
                true
            } else {
                false
            }
        } catch (e: Exception) {
            android.util.Log.e("CrossRecyclerViewTouchHelper", "Error during move operation", e)
            false
        }
    }

    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
        // Not used in this implementation
    }

    override fun onSelectedChanged(viewHolder: RecyclerView.ViewHolder?, actionState: Int) {
        super.onSelectedChanged(viewHolder, actionState)
        
        when (actionState) {
            ItemTouchHelper.ACTION_STATE_DRAG -> {
                viewHolder?.let {
                    draggedFromPosition = it.adapterPosition
                    draggedFromRecyclerView = it.itemView.parent as RecyclerView
                    it.itemView.alpha = 0.7f
                    it.itemView.elevation = 8f
                }
            }
            ItemTouchHelper.ACTION_STATE_IDLE -> {
                draggedFromPosition = -1
                draggedFromRecyclerView = null
                isDraggingOverOther = false
                targetRecyclerView = null
            }
        }
    }

    override fun onChildDraw(
        c: Canvas,
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        dX: Float,
        dY: Float,
        actionState: Int,
        isCurrentlyActive: Boolean
    ) {
        super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
        
        if (isCurrentlyActive) {
            // Check if we're hovering over the other RecyclerView
            val otherRecyclerView = if (recyclerView == leftRecyclerView) rightRecyclerView else leftRecyclerView
            val targetPosition = getTargetPosition(otherRecyclerView, viewHolder.itemView)
            
            if (targetPosition != -1) {
                val targetViewHolder = otherRecyclerView.findViewHolderForAdapterPosition(targetPosition)
                if (isValidDropTarget(otherRecyclerView, targetViewHolder)) {
                    isDraggingOverOther = true
                    targetRecyclerView = otherRecyclerView
                    highlightTargetPosition(otherRecyclerView, targetPosition)
                    drawConnectionLine(c, recyclerView, otherRecyclerView)
                } else {
                    isDraggingOverOther = false
                    targetRecyclerView = null
                    clearHighlights(otherRecyclerView)
                }
            } else {
                isDraggingOverOther = false
                targetRecyclerView = null
                clearHighlights(otherRecyclerView)
            }
        }
    }

    override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
        super.clearView(recyclerView, viewHolder)
        viewHolder.itemView.alpha = 1.0f
        viewHolder.itemView.elevation = 0f
        
        // Check if we need to move to the other RecyclerView
        val otherRecyclerView = if (recyclerView == leftRecyclerView) rightRecyclerView else leftRecyclerView
        val targetPosition = getTargetPosition(otherRecyclerView, viewHolder.itemView)
        
        if (targetPosition != -1 && draggedFromRecyclerView != null && draggedFromRecyclerView != recyclerView) {
            // Only allow drops on existing items (not empty areas)
            val targetViewHolder = otherRecyclerView.findViewHolderForAdapterPosition(targetPosition)
            if (isValidDropTarget(otherRecyclerView, targetViewHolder)) {
                // Move item to the other RecyclerView
                dataManager.moveItem(
                    draggedFromRecyclerView!!,
                    draggedFromPosition,
                    otherRecyclerView,
                    targetPosition
                )
                android.util.Log.d("CrossRecyclerViewTouchHelper", "Item dropped on valid target at position: $targetPosition")
            } else {
                android.util.Log.d("CrossRecyclerViewTouchHelper", "Drop cancelled - not on valid item")
            }
        }
        
        // Clear highlights
        clearHighlights(otherRecyclerView)
        isDraggingOverOther = false
        targetRecyclerView = null
    }

    private fun getTargetPosition(targetRecyclerView: RecyclerView, draggedView: android.view.View): Int {
        return try {
            val targetLayoutManager = targetRecyclerView.layoutManager ?: return -1
            
            // Get the center point of the dragged view
            val centerX = draggedView.left + draggedView.width / 2
            val centerY = draggedView.top + draggedView.height / 2
            
            // Convert to target RecyclerView coordinates
            val location = IntArray(2)
            targetRecyclerView.getLocationOnScreen(location)
            val targetX = centerX - location[0]
            val targetY = centerY - location[1]
            
            // Check if the dragged view is over the target RecyclerView
            if (targetX >= 0 && targetX <= targetRecyclerView.width && 
                targetY >= 0 && targetY <= targetRecyclerView.height) {
                
                // Find the child view under the dragged item
                val childView = targetRecyclerView.findChildViewUnder(targetX.toFloat(), targetY.toFloat())
                if (childView != null) {
                    val position = targetRecyclerView.getChildAdapterPosition(childView)
                    // Only return position if it's valid and corresponds to an actual item
                    if (position != RecyclerView.NO_POSITION && position < targetRecyclerView.adapter?.itemCount ?: 0) {
                        return position
                    }
                }
            }
            -1
        } catch (e: Exception) {
            android.util.Log.e("CrossRecyclerViewTouchHelper", "Error calculating target position", e)
            -1
        }
    }
    
    /**
     * Validates if the drop target is a valid item (not empty area)
     */
    private fun isValidDropTarget(recyclerView: RecyclerView, targetViewHolder: RecyclerView.ViewHolder?): Boolean {
        if (targetViewHolder == null) {
            return false
        }
        
        val position = targetViewHolder.adapterPosition
        if (position == RecyclerView.NO_POSITION) {
            return false
        }
        
        val adapter = recyclerView.adapter
        if (adapter == null || position >= adapter.itemCount) {
            return false
        }
        
        // Additional check: ensure the target view holder is actually bound to an item
        return targetViewHolder.itemView.parent != null
    }

    private fun highlightTargetPosition(recyclerView: RecyclerView, position: Int) {
        val viewHolder = recyclerView.findViewHolderForAdapterPosition(position)
        viewHolder?.let { holder ->
            // Valid drop target - highlight in green
            holder.itemView.alpha = 0.8f
            holder.itemView.elevation = 8f
            holder.itemView.setBackgroundColor(android.graphics.Color.parseColor("#E8F5E8")) // Light green
        }
    }

    private fun clearHighlights(recyclerView: RecyclerView) {
        for (i in 0 until recyclerView.childCount) {
            val child = recyclerView.getChildAt(i)
            child.alpha = 1.0f
            child.elevation = 0f
            // Reset background color - the adapter will handle setting the proper item color
            child.setBackgroundColor(android.graphics.Color.TRANSPARENT)
        }
    }

    private fun drawConnectionLine(canvas: Canvas, fromRecyclerView: RecyclerView, toRecyclerView: RecyclerView) {
        if (!isDraggingOverOther) return
        
        val paint = Paint().apply {
            color = 0x8000BCD4.toInt() // Semi-transparent cyan
            strokeWidth = 4f
            style = Paint.Style.STROKE
            pathEffect = android.graphics.DashPathEffect(floatArrayOf(10f, 5f), 0f)
        }
        
        // Get positions of both RecyclerViews
        val fromLocation = IntArray(2)
        val toLocation = IntArray(2)
        fromRecyclerView.getLocationOnScreen(fromLocation)
        toRecyclerView.getLocationOnScreen(toLocation)
        
        // Calculate connection points
        val fromX = fromLocation[0] + fromRecyclerView.width / 2f
        val fromY = fromLocation[1] + fromRecyclerView.height / 2f
        val toX = toLocation[0] + toRecyclerView.width / 2f
        val toY = toLocation[1] + toRecyclerView.height / 2f
        
        // Draw the connection line
        canvas.drawLine(fromX, fromY, toX, toY, paint)
    }
}
