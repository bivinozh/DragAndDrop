package com.example.draganddrop

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView

class CrossRecyclerViewTouchHelper(
    private val dataManager: UnifiedDataManager,
    private val leftRecyclerView: RecyclerView,
    private val rightRecyclerView: RecyclerView,
    private val requiredOverlapPercentage: Double = 90.0 // Default 90% overlap required
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
        
        android.util.Log.d("CrossRecyclerViewTouchHelper", "onMove called: from=$fromPosition, to=$toPosition")
        
        if (fromPosition == RecyclerView.NO_POSITION || toPosition == RecyclerView.NO_POSITION) {
            android.util.Log.d("CrossRecyclerViewTouchHelper", "onMove: Invalid positions - NO_POSITION")
            return false
        }
        
        // Check if source item is draggable
        val adapter = recyclerView.adapter as? ItemAdapter
        if (adapter != null) {
            val sourceItem = adapter.getItemAt(fromPosition)
            if (sourceItem != null && !sourceItem.isDraggable) {
                android.util.Log.d("CrossRecyclerViewTouchHelper", "onMove: Source item is non-draggable")
                return false
            }
        }
        
        // For same RecyclerView, allow normal swapping
        if (recyclerView == draggedFromRecyclerView) {
            android.util.Log.d("CrossRecyclerViewTouchHelper", "Same RecyclerView move - allowing")
            dataManager.moveItem(recyclerView, fromPosition, recyclerView, toPosition)
            return true
        }
        
        // For cross-RecyclerView moves, we don't handle them in onMove
        // Instead, we'll handle them in clearView when the drag ends
        android.util.Log.d("CrossRecyclerViewTouchHelper", "Cross-RecyclerView move detected - deferring to clearView")
        return false
    }

    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
        // Not used in this implementation
    }

    override fun onSelectedChanged(viewHolder: RecyclerView.ViewHolder?, actionState: Int) {
        super.onSelectedChanged(viewHolder, actionState)
        
        when (actionState) {
            ItemTouchHelper.ACTION_STATE_DRAG -> {
                viewHolder?.let {
                    val position = it.adapterPosition
                    val recyclerView = it.itemView.parent as RecyclerView
                    val adapter = recyclerView.adapter as? ItemAdapter
                    
                    android.util.Log.d("CrossRecyclerViewTouchHelper", "=== DRAG STARTED ===")
                    android.util.Log.d("CrossRecyclerViewTouchHelper", "Position: $position, RecyclerView: ${recyclerView.id}")
                    
                    // Check if the item is draggable
                    if (adapter != null && position != RecyclerView.NO_POSITION) {
                        val item = adapter.getItemAt(position)
                        android.util.Log.d("CrossRecyclerViewTouchHelper", "Item: ${item?.text}, Draggable: ${item?.isDraggable}")
                        if (item != null && !item.isDraggable) {
                            android.util.Log.d("CrossRecyclerViewTouchHelper", "Drag blocked - item is non-draggable: ${item.text}")
                            return
                        }
                    }
                    
                    draggedFromPosition = position
                    draggedFromRecyclerView = recyclerView
                    it.itemView.alpha = 0.7f
                    it.itemView.elevation = 8f
                    android.util.Log.d("CrossRecyclerViewTouchHelper", "Drag state initialized - position: $draggedFromPosition")
                }
            }
            ItemTouchHelper.ACTION_STATE_IDLE -> {
                android.util.Log.d("CrossRecyclerViewTouchHelper", "=== DRAG ENDED ===")
                android.util.Log.d("CrossRecyclerViewTouchHelper", "Resetting drag state")
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
                if (targetViewHolder != null && isValidDropTarget(otherRecyclerView, targetViewHolder)) {
                    // Only highlight when there's a valid target with proper overlap
                    isDraggingOverOther = true
                    targetRecyclerView = otherRecyclerView
                    highlightTargetPosition(otherRecyclerView, targetPosition)
                    drawConnectionLine(c, recyclerView, otherRecyclerView)
                } else {
                    // No valid target or insufficient overlap - clear highlights
                    isDraggingOverOther = false
                    targetRecyclerView = null
                    clearHighlights(otherRecyclerView)
                    android.util.Log.d("CrossRecyclerViewTouchHelper", "No valid target - clearing highlights")
                }
            } else {
                // No target position found - clear highlights
                isDraggingOverOther = false
                targetRecyclerView = null
                clearHighlights(otherRecyclerView)
                android.util.Log.d("CrossRecyclerViewTouchHelper", "No target position - clearing highlights")
            }
        }
    }

    override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
        android.util.Log.d("CrossRecyclerViewTouchHelper", "clearView called for position: ${viewHolder.adapterPosition}")
        
        // Handle cross-RecyclerView drops here BEFORE calling super.clearView()
        if (draggedFromRecyclerView != null && draggedFromRecyclerView != recyclerView) {
            android.util.Log.d("CrossRecyclerViewTouchHelper", "Processing cross-RecyclerView drop before clearing")
            handleCrossRecyclerViewDrop(recyclerView, viewHolder)
        }
        
        // Now call super.clearView() to handle the normal cleanup
        super.clearView(recyclerView, viewHolder)
        viewHolder.itemView.alpha = 1.0f
        viewHolder.itemView.elevation = 0f
        
        // Clear highlights and reset state
        val otherRecyclerView = if (recyclerView == leftRecyclerView) rightRecyclerView else leftRecyclerView
        clearHighlights(otherRecyclerView)
        isDraggingOverOther = false
        targetRecyclerView = null
    }
    
    private fun handleCrossRecyclerViewDrop(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
        val otherRecyclerView = if (recyclerView == leftRecyclerView) rightRecyclerView else leftRecyclerView
        
        android.util.Log.d("CrossRecyclerViewTouchHelper", "=== HANDLING CROSS-RECYCLERVIEW DROP ===")
        android.util.Log.d("CrossRecyclerViewTouchHelper", "Dragged from: ${draggedFromRecyclerView?.id}, To: ${otherRecyclerView.id}")
        android.util.Log.d("CrossRecyclerViewTouchHelper", "Dragged position: $draggedFromPosition")
        
        // Validate drag state
        if (draggedFromRecyclerView == null || draggedFromPosition == -1) {
            android.util.Log.e("CrossRecyclerViewTouchHelper", "Invalid drag state - cannot process drop")
            return
        }
        
        // Check if source item is draggable
        val sourceRecyclerView = draggedFromRecyclerView
        val sourceAdapter = sourceRecyclerView?.adapter as? ItemAdapter
        if (sourceAdapter != null) {
            val sourceItem = sourceAdapter.getItemAt(draggedFromPosition)
            android.util.Log.d("CrossRecyclerViewTouchHelper", "Source item: ${sourceItem?.text}, Draggable: ${sourceItem?.isDraggable}")
            if (sourceItem != null && !sourceItem.isDraggable) {
                android.util.Log.d("CrossRecyclerViewTouchHelper", "Drop cancelled - source item is non-draggable: ${sourceItem.text}")
                return
            }
        } else {
            android.util.Log.e("CrossRecyclerViewTouchHelper", "Source adapter not found")
            return
        }
        
        // Check if we're over an empty area
        val overEmptyArea = isOverEmptyArea(otherRecyclerView, viewHolder.itemView)
        android.util.Log.d("CrossRecyclerViewTouchHelper", "Over empty area: $overEmptyArea")
        if (overEmptyArea) {
            android.util.Log.d("CrossRecyclerViewTouchHelper", "Drop cancelled - over empty area")
            return
        }
        
        val targetPosition = getTargetPosition(otherRecyclerView, viewHolder.itemView)
        android.util.Log.d("CrossRecyclerViewTouchHelper", "Target position: $targetPosition")
        
        if (targetPosition != -1) {
            val targetViewHolder = otherRecyclerView.findViewHolderForAdapterPosition(targetPosition)
            android.util.Log.d("CrossRecyclerViewTouchHelper", "Target ViewHolder: ${targetViewHolder != null}")
            
            if (targetViewHolder != null) {
                val overlapPercentage = calculateOverlapPercentage(viewHolder.itemView, targetViewHolder.itemView)
                android.util.Log.d("CrossRecyclerViewTouchHelper", "Overlap percentage: ${overlapPercentage}% (required: ${requiredOverlapPercentage}%)")
                
                if (overlapPercentage >= requiredOverlapPercentage) {
                    // Valid drop - replace target item
                    android.util.Log.d("CrossRecyclerViewTouchHelper", "=== EXECUTING CROSS-RECYCLERVIEW REPLACEMENT ===")
                    
                    try {
                        dataManager.moveItem(
                            sourceRecyclerView!!,
                            draggedFromPosition,
                            otherRecyclerView,
                            targetPosition
                        )
                        android.util.Log.d("CrossRecyclerViewTouchHelper", "=== REPLACEMENT SUCCESSFUL ===")
                    } catch (e: Exception) {
                        android.util.Log.e("CrossRecyclerViewTouchHelper", "Error during replacement", e)
                    }
                } else {
                    android.util.Log.d("CrossRecyclerViewTouchHelper", "Drop cancelled - insufficient overlap: ${overlapPercentage}% < ${requiredOverlapPercentage}%")
                }
            } else {
                android.util.Log.d("CrossRecyclerViewTouchHelper", "Drop cancelled - no target ViewHolder")
            }
        } else {
            android.util.Log.d("CrossRecyclerViewTouchHelper", "Drop cancelled - no valid target position")
        }
    }

    private fun getTargetPosition(targetRecyclerView: RecyclerView, draggedView: android.view.View): Int {
        return try {
            val targetLayoutManager = targetRecyclerView.layoutManager ?: return -1
            val adapter = targetRecyclerView.adapter
            
            if (adapter == null || adapter.itemCount == 0) {
                android.util.Log.d("CrossRecyclerViewTouchHelper", "No adapter or empty adapter")
                return -1
            }
            
            // Get the center point of the dragged view
            val centerX = draggedView.left + draggedView.width / 2
            val centerY = draggedView.top + draggedView.height / 2
            
            // Convert to target RecyclerView coordinates
            val location = IntArray(2)
            targetRecyclerView.getLocationOnScreen(location)
            val targetX = centerX - location[0]
            val targetY = centerY - location[1]
            
            android.util.Log.d("CrossRecyclerViewTouchHelper", "getTargetPosition: targetX=$targetX, targetY=$targetY, adapterCount=${adapter.itemCount}")
            
            // Check if the dragged view is over the target RecyclerView
            if (targetX >= 0 && targetX <= targetRecyclerView.width && 
                targetY >= 0 && targetY <= targetRecyclerView.height) {
                
                // Find the child view under the dragged item
                val childView = targetRecyclerView.findChildViewUnder(targetX.toFloat(), targetY.toFloat())
                if (childView != null) {
                    val position = targetRecyclerView.getChildAdapterPosition(childView)
                    // Only return position if it's valid and corresponds to an actual item
                    if (position != RecyclerView.NO_POSITION && position >= 0 && position < adapter.itemCount) {
                        // Additional check: ensure this is actually a child view (not empty space)
                        val viewHolder = targetRecyclerView.findViewHolderForAdapterPosition(position)
                        if (viewHolder != null && viewHolder.itemView == childView) {
                            android.util.Log.d("CrossRecyclerViewTouchHelper", "Valid target position found: $position")
                            return position
                        } else {
                            android.util.Log.d("CrossRecyclerViewTouchHelper", "ViewHolder mismatch for position $position")
                        }
                    } else {
                        android.util.Log.d("CrossRecyclerViewTouchHelper", "Invalid position: $position (count: ${adapter.itemCount})")
                    }
                } else {
                    // No child view found - this means we're over empty space
                    android.util.Log.d("CrossRecyclerViewTouchHelper", "No child view found - over empty space")
                }
            } else {
                android.util.Log.d("CrossRecyclerViewTouchHelper", "Outside RecyclerView bounds")
            }
            -1
        } catch (e: Exception) {
            android.util.Log.e("CrossRecyclerViewTouchHelper", "Error calculating target position", e)
            -1
        }
    }
    
    /**
     * Validates if the drop target is a valid item (not empty area)
     * and checks if the dragged item has at least 90% overlap with the target
     */
    private fun isValidDropTarget(recyclerView: RecyclerView, targetViewHolder: RecyclerView.ViewHolder?): Boolean {
        if (targetViewHolder == null) {
            android.util.Log.d("CrossRecyclerViewTouchHelper", "Invalid target - no ViewHolder")
            return false
        }
        
        val position = targetViewHolder.adapterPosition
        if (position == RecyclerView.NO_POSITION) {
            android.util.Log.d("CrossRecyclerViewTouchHelper", "Invalid target - NO_POSITION")
            return false
        }
        
        val adapter = recyclerView.adapter
        if (adapter == null || position >= adapter.itemCount) {
            android.util.Log.d("CrossRecyclerViewTouchHelper", "Invalid target - position out of bounds")
            return false
        }
        
        // Additional check: ensure the target view holder is actually bound to an item
        if (targetViewHolder.itemView.parent == null) {
            android.util.Log.d("CrossRecyclerViewTouchHelper", "Invalid target - no parent")
            return false
        }
        
        // Check if we have a dragged item to compare with
        val draggedViewHolder = draggedFromRecyclerView?.findViewHolderForAdapterPosition(draggedFromPosition)
        if (draggedViewHolder == null) {
            android.util.Log.d("CrossRecyclerViewTouchHelper", "Invalid target - no dragged item")
            return false
        }
        
        // Calculate overlap percentage between dragged item and target item
        val overlapPercentage = calculateOverlapPercentage(draggedViewHolder.itemView, targetViewHolder.itemView)
        android.util.Log.d("CrossRecyclerViewTouchHelper", "Overlap percentage: ${overlapPercentage}% (required: ${requiredOverlapPercentage}%)")
        
        // Require at least the specified overlap percentage
        val isValid = overlapPercentage >= requiredOverlapPercentage
        android.util.Log.d("CrossRecyclerViewTouchHelper", "Drop target validation: ${if (isValid) "VALID" else "INVALID"}")
        return isValid
    }

    private fun highlightTargetPosition(recyclerView: RecyclerView, position: Int) {
        val viewHolder = recyclerView.findViewHolderForAdapterPosition(position)
        viewHolder?.let { holder ->
            // Calculate overlap percentage for visual feedback
            val draggedViewHolder = draggedFromRecyclerView?.findViewHolderForAdapterPosition(draggedFromPosition)
            if (draggedViewHolder != null) {
                val overlapPercentage = calculateOverlapPercentage(draggedViewHolder.itemView, holder.itemView)
                
                // Color coding based on overlap percentage - indicates replacement will happen
                val backgroundColor = when {
                    overlapPercentage >= requiredOverlapPercentage -> android.graphics.Color.parseColor("#FF5722") // Red - will be replaced
                    overlapPercentage >= (requiredOverlapPercentage * 0.8) -> android.graphics.Color.parseColor("#FF9800") // Orange - close but not enough
                    overlapPercentage >= (requiredOverlapPercentage * 0.5) -> android.graphics.Color.parseColor("#FFC107") // Amber - getting closer
                    else -> android.graphics.Color.parseColor("#9E9E9E") // Gray - no overlap
                }
                
                holder.itemView.alpha = 0.8f
                holder.itemView.elevation = 8f
                holder.itemView.setBackgroundColor(backgroundColor)
                
                android.util.Log.d("CrossRecyclerViewTouchHelper", 
                    "Highlighting position $position with ${overlapPercentage}% overlap - ${if (overlapPercentage >= requiredOverlapPercentage) "WILL REPLACE" else "NOT ENOUGH OVERLAP"}")
            }
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

    /**
     * Checks if the dragged view is over an empty area (no items) in the target RecyclerView
     */
    private fun isOverEmptyArea(targetRecyclerView: RecyclerView, draggedView: android.view.View): Boolean {
        return try {
            // Get the center point of the dragged view
            val centerX = draggedView.left + draggedView.width / 2
            val centerY = draggedView.top + draggedView.height / 2
            
            // Convert to target RecyclerView coordinates
            val location = IntArray(2)
            targetRecyclerView.getLocationOnScreen(location)
            val targetX = centerX - location[0]
            val targetY = centerY - location[1]
            
            android.util.Log.d("CrossRecyclerViewTouchHelper", "Checking empty area: targetX=$targetX, targetY=$targetY, width=${targetRecyclerView.width}, height=${targetRecyclerView.height}")
            
            // Check if the dragged view is over the target RecyclerView
            if (targetX >= 0 && targetX <= targetRecyclerView.width && 
                targetY >= 0 && targetY <= targetRecyclerView.height) {
                
                // Find the child view under the dragged item
                val childView = targetRecyclerView.findChildViewUnder(targetX.toFloat(), targetY.toFloat())
                if (childView == null) {
                    // No child view found - we're over empty space
                    android.util.Log.d("CrossRecyclerViewTouchHelper", "Over empty area - no child view found at ($targetX, $targetY)")
                    return true
                } else {
                    // Child view found - check if it's actually a valid item
                    val position = targetRecyclerView.getChildAdapterPosition(childView)
                    if (position == RecyclerView.NO_POSITION) {
                        android.util.Log.d("CrossRecyclerViewTouchHelper", "Over empty area - invalid position for child view")
                        return true
                    } else {
                        // Additional check: ensure this child view is actually part of the RecyclerView
                        val viewHolder = targetRecyclerView.findViewHolderForAdapterPosition(position)
                        if (viewHolder == null || viewHolder.itemView != childView) {
                            android.util.Log.d("CrossRecyclerViewTouchHelper", "Over empty area - child view doesn't match ViewHolder")
                            return true
                        }
                        android.util.Log.d("CrossRecyclerViewTouchHelper", "Valid item found at position $position")
                    }
                }
            } else {
                android.util.Log.d("CrossRecyclerViewTouchHelper", "Outside RecyclerView bounds")
            }
            false
        } catch (e: Exception) {
            android.util.Log.e("CrossRecyclerViewTouchHelper", "Error checking empty area", e)
            true // Assume empty area on error to be safe
        }
    }
    
    /**
     * Calculates the overlap percentage between two views
     * Returns a value between 0.0 and 100.0 representing the percentage of overlap
     */
    private fun calculateOverlapPercentage(draggedView: android.view.View, targetView: android.view.View): Double {
        return try {
            // Get the screen coordinates of both views
            val draggedLocation = IntArray(2)
            val targetLocation = IntArray(2)
            draggedView.getLocationOnScreen(draggedLocation)
            targetView.getLocationOnScreen(targetLocation)
            
            // Calculate the bounds of both views
            val draggedLeft = draggedLocation[0]
            val draggedTop = draggedLocation[1]
            val draggedRight = draggedLeft + draggedView.width
            val draggedBottom = draggedTop + draggedView.height
            
            val targetLeft = targetLocation[0]
            val targetTop = targetLocation[1]
            val targetRight = targetLeft + targetView.width
            val targetBottom = targetTop + targetView.height
            
            // Calculate the intersection rectangle
            val intersectionLeft = maxOf(draggedLeft, targetLeft)
            val intersectionTop = maxOf(draggedTop, targetTop)
            val intersectionRight = minOf(draggedRight, targetRight)
            val intersectionBottom = minOf(draggedBottom, targetBottom)
            
            // If there's no intersection, return 0
            if (intersectionLeft >= intersectionRight || intersectionTop >= intersectionBottom) {
                return 0.0
            }
            
            // Calculate areas
            val intersectionArea = (intersectionRight - intersectionLeft) * (intersectionBottom - intersectionTop)
            val draggedArea = draggedView.width * draggedView.height
            val targetArea = targetView.width * targetView.height
            
            // Calculate overlap percentage based on the smaller view (more restrictive)
            val smallerArea = minOf(draggedArea, targetArea)
            val overlapPercentage = if (smallerArea > 0) {
                (intersectionArea.toDouble() / smallerArea.toDouble()) * 100.0
            } else {
                0.0
            }
            
            android.util.Log.d("CrossRecyclerViewTouchHelper", 
                "Dragged: $draggedArea, Target: $targetArea, Intersection: $intersectionArea, Overlap: ${overlapPercentage}%")
            
            overlapPercentage
        } catch (e: Exception) {
            android.util.Log.e("CrossRecyclerViewTouchHelper", "Error calculating overlap percentage", e)
            0.0
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
