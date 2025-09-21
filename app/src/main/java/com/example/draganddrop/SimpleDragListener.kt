package com.example.draganddrop

import android.view.DragEvent
import android.view.View
import androidx.recyclerview.widget.RecyclerView

class SimpleDragListener(
    private val dataManager: UnifiedDataManager,
    private val leftRecyclerView: RecyclerView,
    private val rightRecyclerView: RecyclerView
) : View.OnDragListener {

    private var draggedItem: Item? = null
    private var draggedFromPosition = -1
    private var draggedFromRecyclerView: RecyclerView? = null
    private var draggedItemView: View? = null
    private val COVERAGE_THRESHOLD = 0.9f // 90% coverage required
    
    // Target focus tracking
    private var currentFocusedView: View? = null
    private var currentFocusedPosition = -1
    private var currentFocusedRecyclerView: RecyclerView? = null

    override fun onDrag(view: View, event: DragEvent): Boolean {
        android.util.Log.d("SimpleDragListener", "onDrag called: action=${event.action}, view=${view.id}")
        when (event.action) {
            DragEvent.ACTION_DRAG_STARTED -> {
                android.util.Log.d("SimpleDragListener", "Drag started on view: ${view.id}")
                return true
            }
            DragEvent.ACTION_DRAG_ENTERED -> {
                android.util.Log.d("SimpleDragListener", "Drag entered: ${view.id}")
                view.alpha = 0.7f
                return true
            }
            DragEvent.ACTION_DRAG_EXITED -> {
                android.util.Log.d("SimpleDragListener", "Drag exited: ${view.id}")
                view.alpha = 1.0f
                clearTargetFocus()
                return true
            }
            DragEvent.ACTION_DRAG_LOCATION -> {
                // Update target focus as drag moves
                val targetRecyclerView = view as? RecyclerView
                if (targetRecyclerView != null) {
                    updateTargetFocus(targetRecyclerView, event)
                }
                return true
            }
            DragEvent.ACTION_DROP -> {
                android.util.Log.d("SimpleDragListener", "=== ACTION_DROP ===")
                android.util.Log.d("SimpleDragListener", "Drop on: ${view.id}")
                android.util.Log.d("SimpleDragListener", "Dragged item: ${draggedItem?.text}")
                android.util.Log.d("SimpleDragListener", "Dragged from position: $draggedFromPosition")
                android.util.Log.d("SimpleDragListener", "Dragged from RecyclerView: ${draggedFromRecyclerView?.id}")
                view.alpha = 1.0f
                
                val targetRecyclerView = view as RecyclerView
                val targetPosition = getDropPosition(targetRecyclerView, event)
                
                android.util.Log.d("SimpleDragListener", "Calculated target position: $targetPosition")
                android.util.Log.d("SimpleDragListener", "Dragged from position: $draggedFromPosition")
                android.util.Log.d("SimpleDragListener", "From RecyclerView ID: ${draggedFromRecyclerView?.id}")
                android.util.Log.d("SimpleDragListener", "To RecyclerView ID: ${targetRecyclerView.id}")
                
                if (targetPosition >= 0 && draggedFromRecyclerView != null) {
                    val fromRecycler = draggedFromRecyclerView!!
                    android.util.Log.d("SimpleDragListener", "=== EXECUTING MOVE ===")
                    android.util.Log.d("SimpleDragListener", "From: ${if (fromRecycler.id == R.id.left_recycler_view) "LEFT" else "RIGHT"} pos $draggedFromPosition")
                    android.util.Log.d("SimpleDragListener", "To: ${if (targetRecyclerView.id == R.id.left_recycler_view) "LEFT" else "RIGHT"} pos $targetPosition")
                    
                    dataManager.moveItem(
                        fromRecycler,
                        draggedFromPosition,
                        targetRecyclerView,
                        targetPosition
                    )
                } else {
                    if (targetPosition == -1) {
                        android.util.Log.d("SimpleDragListener", "Drop rejected: Insufficient coverage (need 90%)")
                    } else {
                        android.util.Log.e("SimpleDragListener", "Invalid drop: targetPosition=$targetPosition, draggedFromRecyclerView=$draggedFromRecyclerView")
                    }
                }
                return true
            }
            DragEvent.ACTION_DRAG_ENDED -> {
                android.util.Log.d("SimpleDragListener", "Drag ended")
                view.alpha = 1.0f
                clearTargetFocus()
                draggedItem = null
                draggedFromPosition = -1
                draggedFromRecyclerView = null
                draggedItemView = null
                return true
            }
            else -> return false
        }
    }

    fun startDrag(item: Item, fromRecyclerView: RecyclerView, fromPosition: Int) {
        draggedItem = item
        draggedFromPosition = fromPosition
        draggedFromRecyclerView = fromRecyclerView
        
        android.util.Log.d("SimpleDragListener", "Starting drag: ${item.text} from position $fromPosition")
        android.util.Log.d("SimpleDragListener", "Current left items: ${dataManager.getLeftItems().map { it.text }}")
        android.util.Log.d("SimpleDragListener", "Current right items: ${dataManager.getRightItems().map { it.text }}")
        
        val viewHolder = fromRecyclerView.findViewHolderForAdapterPosition(fromPosition)
        if (viewHolder != null) {
            draggedItemView = viewHolder.itemView
            val dragShadowBuilder = View.DragShadowBuilder(viewHolder.itemView)
            viewHolder.itemView.startDrag(null, dragShadowBuilder, null, 0)
        } else {
            android.util.Log.e("SimpleDragListener", "ViewHolder not found for position $fromPosition")
        }
    }

    private fun getDropPosition(targetRecyclerView: RecyclerView, event: DragEvent): Int {
        val x = event.x.toInt()
        val y = event.y.toInt()
        
        android.util.Log.d("SimpleDragListener", "=== DROP POSITION CALCULATION ===")
        android.util.Log.d("SimpleDragListener", "Drop coordinates: x=$x, y=$y")
        android.util.Log.d("SimpleDragListener", "RecyclerView bounds: ${targetRecyclerView.width}x${targetRecyclerView.height}")
        android.util.Log.d("SimpleDragListener", "RecyclerView ID: ${targetRecyclerView.id}")
        android.util.Log.d("SimpleDragListener", "Adapter item count: ${targetRecyclerView.adapter?.itemCount}")
        android.util.Log.d("SimpleDragListener", "Is right RecyclerView: ${targetRecyclerView.id == R.id.right_recycler_view}")
        
        val childView = targetRecyclerView.findChildViewUnder(x.toFloat(), y.toFloat())
        if (childView != null) {
            val position = targetRecyclerView.getChildAdapterPosition(childView)
            android.util.Log.d("SimpleDragListener", "Found child view at position: $position")
            
            if (position != RecyclerView.NO_POSITION && position >= 0) {
                // Temporarily disable coverage check for debugging
                android.util.Log.d("SimpleDragListener", "Coverage check disabled for debugging")
                // Check if coverage is sufficient (90% requirement)
                // if (!isCoverageSufficient(draggedItemView, childView, x, y)) {
                //     android.util.Log.d("SimpleDragListener", "Insufficient coverage - drop rejected")
                //     return -1
                // }
                // Check if we're dropping in the upper or lower half of the child view
                val childTop = childView.top
                val childBottom = childView.bottom
                
                val isRightRecyclerView = targetRecyclerView.id == R.id.right_recycler_view
                
                // Simple 50/50 drop zone calculation for predictable behavior
                val childHeight = childBottom - childTop
                val childCenter = childTop + (childHeight / 2)
                
                android.util.Log.d("SimpleDragListener", "Child bounds: top=$childTop, bottom=$childBottom, center=$childCenter, drop_y=$y")
                
                if (y < childCenter) {
                    // Dropping in upper half - insert before this item
                    android.util.Log.d("SimpleDragListener", "Dropping in upper half, position: $position")
                    return position
                } else {
                    // Dropping in lower half - insert after this item
                    val insertPosition = position + 1
                    
                    // For right RecyclerView, ensure we don't exceed valid positions
                    val finalPosition = if (isRightRecyclerView) {
                        // Right RecyclerView has positions 0, 1, and we allow 2 for the end
                        insertPosition.coerceAtMost(2)
                    } else {
                        // Left RecyclerView can have any position
                        insertPosition
                    }
                    
                    android.util.Log.d("SimpleDragListener", "Dropping in lower half, position: $finalPosition (original: $insertPosition, isRight: $isRightRecyclerView)")
                    return finalPosition
                }
            } else {
                // Found child view but invalid position - treat as empty space
                android.util.Log.d("SimpleDragListener", "Found child view but invalid position: $position, treating as empty space")
            }
        }
        
        // Check if we're in the RecyclerView bounds but no child found
        val adapter = targetRecyclerView.adapter
        val itemCount = adapter?.itemCount ?: 0
        
        if (x >= 0 && x <= targetRecyclerView.width && y >= 0 && y <= targetRecyclerView.height) {
            // We're within bounds but no child found - this means drop at the end
            val isRightRecyclerView = targetRecyclerView.id == R.id.right_recycler_view
            
            val finalPosition = if (isRightRecyclerView) {
                // For right RecyclerView, drop at the end (position 2 = global position 12)
                android.util.Log.d("SimpleDragListener", "Right RecyclerView empty space drop - using position 2")
                2
            } else {
                // For left RecyclerView, drop at the end
                val endPosition = if (itemCount > 0) itemCount else 0
                android.util.Log.d("SimpleDragListener", "Left RecyclerView empty space drop - using position $endPosition")
                endPosition
            }
            
            android.util.Log.d("SimpleDragListener", "No child found, dropping at end position: $finalPosition (isRight: $isRightRecyclerView)")
            return finalPosition
        }
        
        android.util.Log.d("SimpleDragListener", "Outside RecyclerView bounds")
        
        // Safety fallback - if we're outside bounds, try to drop at the end
        val fallbackAdapter = targetRecyclerView.adapter
        val fallbackItemCount = fallbackAdapter?.itemCount ?: 0
        val isRightRecyclerView = targetRecyclerView.id == R.id.right_recycler_view
        val fallbackPosition = if (isRightRecyclerView) {
            fallbackItemCount.coerceAtMost(2)
        } else {
            fallbackItemCount
        }
        
        android.util.Log.d("SimpleDragListener", "Using fallback position: $fallbackPosition")
        return fallbackPosition
    }
    
    // Emergency fallback - this should never be reached, but ensures we never return -1
    private fun getEmergencyFallbackPosition(targetRecyclerView: RecyclerView): Int {
        val isRightRecyclerView = targetRecyclerView.id == R.id.right_recycler_view
        return if (isRightRecyclerView) 0 else 0  // Always return 0 as last resort
    }
    
    private fun calculateCoveragePercentage(draggedView: View?, targetView: View, dropX: Int, dropY: Int): Float {
        if (draggedView == null) return 0f
        
        // Get the dragged item's dimensions
        val draggedWidth = draggedView.width
        val draggedHeight = draggedView.height
        
        // Get the target item's bounds
        val targetLeft = targetView.left
        val targetTop = targetView.top
        val targetRight = targetView.right
        val targetBottom = targetView.bottom
        
        // Calculate the dragged item's bounds at the drop position
        val draggedLeft = dropX - (draggedWidth / 2)
        val draggedTop = dropY - (draggedHeight / 2)
        val draggedRight = dropX + (draggedWidth / 2)
        val draggedBottom = dropY + (draggedHeight / 2)
        
        // Calculate intersection area
        val intersectionLeft = maxOf(draggedLeft, targetLeft)
        val intersectionTop = maxOf(draggedTop, targetTop)
        val intersectionRight = minOf(draggedRight, targetRight)
        val intersectionBottom = minOf(draggedBottom, targetBottom)
        
        // Check if there's any intersection
        if (intersectionLeft >= intersectionRight || intersectionTop >= intersectionBottom) {
            return 0f
        }
        
        val intersectionArea = (intersectionRight - intersectionLeft) * (intersectionBottom - intersectionTop)
        val draggedArea = draggedWidth * draggedHeight
        val coveragePercentage = intersectionArea.toFloat() / draggedArea.toFloat()
        
        android.util.Log.d("SimpleDragListener", "Coverage calculation:")
        android.util.Log.d("SimpleDragListener", "  Dragged bounds: ($draggedLeft, $draggedTop, $draggedRight, $draggedBottom)")
        android.util.Log.d("SimpleDragListener", "  Target bounds: ($targetLeft, $targetTop, $targetRight, $targetBottom)")
        android.util.Log.d("SimpleDragListener", "  Intersection: ($intersectionLeft, $intersectionTop, $intersectionRight, $intersectionBottom)")
        android.util.Log.d("SimpleDragListener", "  Coverage: ${(coveragePercentage * 100).toInt()}%")
        
        return coveragePercentage
    }
    
    private fun isCoverageSufficient(draggedView: View?, targetView: View, dropX: Int, dropY: Int): Boolean {
        val coverage = calculateCoveragePercentage(draggedView, targetView, dropX, dropY)
        val isSufficient = coverage >= COVERAGE_THRESHOLD
        
        android.util.Log.d("SimpleDragListener", "Coverage sufficient: $isSufficient (${(coverage * 100).toInt()}% >= ${(COVERAGE_THRESHOLD * 100).toInt()}%)")
        return isSufficient
    }
    
    private fun setTargetFocus(targetView: View?, targetPosition: Int, targetRecyclerView: RecyclerView?) {
        // Clear previous focus
        clearTargetFocus()
        
        if (targetView != null && targetPosition >= 0) {
            currentFocusedView = targetView
            currentFocusedPosition = targetPosition
            currentFocusedRecyclerView = targetRecyclerView
            
            // Apply focus visual effects
            targetView.alpha = 0.8f
            targetView.scaleX = 1.05f
            targetView.scaleY = 1.05f
            targetView.elevation = 8f
            
            android.util.Log.d("SimpleDragListener", "Target focus set: position $targetPosition")
        }
    }
    
    private fun clearTargetFocus() {
        currentFocusedView?.let { view ->
            // Reset visual effects
            view.alpha = 1.0f
            view.scaleX = 1.0f
            view.scaleY = 1.0f
            view.elevation = 0f
            
            android.util.Log.d("SimpleDragListener", "Target focus cleared")
        }
        
        currentFocusedView = null
        currentFocusedPosition = -1
        currentFocusedRecyclerView = null
    }
    
    private fun updateTargetFocus(targetRecyclerView: RecyclerView, event: DragEvent) {
        val x = event.x.toInt()
        val y = event.y.toInt()
        
        val childView = targetRecyclerView.findChildViewUnder(x.toFloat(), y.toFloat())
        if (childView != null) {
            val position = targetRecyclerView.getChildAdapterPosition(childView)
            if (position != RecyclerView.NO_POSITION && position >= 0) {
                // Check if this is a new target
                if (currentFocusedView != childView || currentFocusedPosition != position) {
                    setTargetFocus(childView, position, targetRecyclerView)
                }
            } else {
                clearTargetFocus()
            }
        } else {
            clearTargetFocus()
        }
    }
}
