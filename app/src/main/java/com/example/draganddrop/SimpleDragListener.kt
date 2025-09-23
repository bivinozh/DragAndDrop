package com.example.draganddrop

import android.view.DragEvent
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.Animation
import android.view.animation.TranslateAnimation
import android.view.animation.AlphaAnimation
import android.view.animation.ScaleAnimation
import android.view.animation.AnimationSet
import android.view.animation.DecelerateInterpolator
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.GridLayoutManager
import com.example.draganddrop.viewmodel.DragDropViewModel
import com.example.draganddrop.repository.ItemRepository

class SimpleDragListener(
    private val viewModel: DragDropViewModel,
) : View.OnDragListener {

    private var draggedItem: Item? = null
    private var draggedFromPosition = -1
    private var draggedFromRecyclerView: RecyclerView? = null
    private var draggedItemView: View? = null

    // Target focus tracking
    private var currentFocusedView: View? = null
    private var currentFocusedPosition = -1
    private var currentFocusedRecyclerView: RecyclerView? = null
    
    // Floating animation
    private var floatingAnimation: Animation? = null
    private var isFloating = false

    override fun onDrag(view: View, event: DragEvent): Boolean {
        android.util.Log.d("SimpleDragListener", "=== DRAG EVENT RECEIVED ===")
        android.util.Log.d("SimpleDragListener", "Action: ${event.action}, View: ${view.id}")
        
        when (event.action) {
            DragEvent.ACTION_DRAG_STARTED -> {
                android.util.Log.d("SimpleDragListener", "Drag started")
                return true
            }
            DragEvent.ACTION_DRAG_ENTERED -> {
                android.util.Log.d("SimpleDragListener", "Drag entered")
                view.alpha = 0.3f
                return true
            }
            DragEvent.ACTION_DRAG_EXITED -> {
                android.util.Log.d("SimpleDragListener", "Drag exited")
                view.alpha = 1.0f
                clearTargetFocus()
                return true
            }
            DragEvent.ACTION_DRAG_LOCATION -> {
                android.util.Log.d("SimpleDragListener", "Drag location: (${event.x}, ${event.y})")
                // Update target focus as drag moves
                val targetRecyclerView = view as? RecyclerView
                if (targetRecyclerView != null) {
                    updateTargetFocus(targetRecyclerView, event)
                }
                return true
            }
            DragEvent.ACTION_DROP -> {
                android.util.Log.d("SimpleDragListener", "Drop action received")
                view.alpha = 1.0f
                
                val targetRecyclerView = view as RecyclerView
                val targetPosition = getDropPosition(targetRecyclerView, event)
                
                android.util.Log.d("SimpleDragListener", "=== SIMPLE DROP ACTION ===")
                android.util.Log.d("SimpleDragListener", "Target position: $targetPosition")
                
                if (targetPosition >= 0 && draggedFromRecyclerView != null) {
                    // Check if target position contains a locked item
                    val targetItem = getItemAtPosition(targetRecyclerView, targetPosition)
                    if (targetItem != null && !targetItem.isDraggable) {
                        android.util.Log.w("SimpleDragListener", "Cannot drop on locked item: ${targetItem.text}")
                        return true // Drop rejected
                    }
                    
                    showMoveInProgress(targetRecyclerView, targetPosition)
                    
                    // Determine RecyclerView types
                    val fromType = if (draggedFromRecyclerView!!.id == R.id.left_recycler_view) {
                        ItemRepository.RecyclerViewType.LEFT
                    } else {
                        ItemRepository.RecyclerViewType.RIGHT
                    }
                    
                    val toType = if (targetRecyclerView.id == R.id.left_recycler_view) {
                        ItemRepository.RecyclerViewType.LEFT
                    } else {
                        ItemRepository.RecyclerViewType.RIGHT
                    }
                    
                    android.util.Log.d("SimpleDragListener", "Executing move: from $fromType position $draggedFromPosition to $toType position $targetPosition")
                    // Use ViewModel to handle the move
                    viewModel.moveItemBetweenRecyclerViews(fromType, draggedFromPosition, toType, targetPosition)
                } else {
                    android.util.Log.w("SimpleDragListener", "Drop rejected: invalid position or no dragged item")
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
        android.util.Log.d("SimpleDragListener", "=== START DRAG CALLED ===")
        android.util.Log.d("SimpleDragListener", "Item: ${item.text}, Position: $fromPosition, RecyclerView: ${fromRecyclerView.id}")
        
        // Double-check that the item is draggable
        if (!item.isDraggable) {
            android.util.Log.w("SimpleDragListener", "Attempted to drag non-draggable item: ${item.text}")
            return
        }
        
        draggedItem = item
        draggedFromPosition = fromPosition
        draggedFromRecyclerView = fromRecyclerView
        
        val viewHolder = fromRecyclerView.findViewHolderForAdapterPosition(fromPosition)
        if (viewHolder != null) {
            draggedItemView = viewHolder.itemView
            val dragShadowBuilder = View.DragShadowBuilder(viewHolder.itemView)
            android.util.Log.d("SimpleDragListener", "Starting drag with shadow builder")
            viewHolder.itemView.startDrag(null, dragShadowBuilder, null, 0)
        } else {
            android.util.Log.w("SimpleDragListener", "Could not find ViewHolder for position: $fromPosition")
        }
    }

    private fun getDropPosition(targetRecyclerView: RecyclerView, event: DragEvent): Int {
        val x = event.x.toInt()
        val y = event.y.toInt()
        
        android.util.Log.d("SimpleDragListener", "=== SIMPLE DROP POSITION CALCULATION ===")
        android.util.Log.d("SimpleDragListener", "Drop coordinates: ($x, $y)")
        android.util.Log.d("SimpleDragListener", "Target RecyclerView: ${targetRecyclerView.id}")
        
        // Use simple approach first to test basic functionality
        val childView = targetRecyclerView.findChildViewUnder(x.toFloat(), y.toFloat())
        if (childView != null) {
            val position = targetRecyclerView.getChildAdapterPosition(childView)
            android.util.Log.d("SimpleDragListener", "Found child at position: $position")
            
            if (position != RecyclerView.NO_POSITION && position >= 0) {
                val calculatedPosition = calculateCenterBasedPosition(childView, position, targetRecyclerView, x, y)
                android.util.Log.d("SimpleDragListener", "Calculated drop position: $calculatedPosition")
                return calculatedPosition
            }
        }
        
        // Drop at the end if no child found
        val itemCount = targetRecyclerView.adapter?.itemCount ?: 0
        val isRightRecyclerView = targetRecyclerView.id == R.id.right_recycler_view
        
        val finalPosition = if (isRightRecyclerView) {
            itemCount.coerceAtMost(2)
        } else {
            itemCount
        }
        
        android.util.Log.d("SimpleDragListener", "No child found - dropping at position: $finalPosition")
        return finalPosition
    }
    
    private fun getItemAtPosition(recyclerView: RecyclerView, position: Int): Item? {
        val adapter = recyclerView.adapter as? ItemAdapter
        return adapter?.getItemAt(position)
    }
    
    private data class OverlapTarget(
        val view: View,
        val position: Int,
        val overlapPercentage: Float
    )
    
    private fun findBestOverlapTarget(targetRecyclerView: RecyclerView, dropX: Int, dropY: Int): OverlapTarget? {
        android.util.Log.d("SimpleDragListener", "=== FINDING BEST OVERLAP TARGET ===")
        
        val childCount = targetRecyclerView.childCount
        var bestTarget: OverlapTarget? = null
        var maxOverlap = 0f
        
        android.util.Log.d("SimpleDragListener", "Checking $childCount children for overlap")
        
        for (i in 0 until childCount) {
            val childView = targetRecyclerView.getChildAt(i)
            val position = targetRecyclerView.getChildAdapterPosition(childView)
            
            if (position != RecyclerView.NO_POSITION && position >= 0) {
                val overlapPercentage = calculateOverlapPercentage(childView, dropX, dropY)
                
                android.util.Log.d("SimpleDragListener", "Child $i (position $position): overlap = ${overlapPercentage}%")
                
                if (overlapPercentage >= 30f && overlapPercentage > maxOverlap) {
                    maxOverlap = overlapPercentage
                    bestTarget = OverlapTarget(childView, position, overlapPercentage)
                    android.util.Log.d("SimpleDragListener", "New best target: position $position with ${overlapPercentage}% overlap")
                }
            }
        }
        
        if (bestTarget != null) {
            android.util.Log.d("SimpleDragListener", "Best overlap target: position ${bestTarget.position} with ${bestTarget.overlapPercentage}% overlap")
        } else {
            android.util.Log.d("SimpleDragListener", "No valid overlap target found (30% threshold not met)")
        }
        
        return bestTarget
    }
    
    private fun calculateOverlapPercentage(childView: View, dropX: Int, dropY: Int): Float {
        val childLeft = childView.left
        val childRight = childView.right
        val childTop = childView.top
        val childBottom = childView.bottom
        
        android.util.Log.d("SimpleDragListener", "=== CALCULATING OVERLAP PERCENTAGE ===")
        android.util.Log.d("SimpleDragListener", "Child bounds: left=$childLeft, right=$childRight, top=$childTop, bottom=$childBottom")
        android.util.Log.d("SimpleDragListener", "Drop point: ($dropX, $dropY)")
        
        // Calculate the area of the child view
        val childWidth = childRight - childLeft
        val childHeight = childBottom - childTop
        val childArea = childWidth * childHeight
        
        android.util.Log.d("SimpleDragListener", "Child dimensions: ${childWidth}x${childHeight}, area=$childArea")
        
        // Calculate the overlap area (50x50 drop area)
        val overlapLeft = Math.max(childLeft, dropX - 25)
        val overlapRight = Math.min(childRight, dropX + 25)
        val overlapTop = Math.max(childTop, dropY - 25)
        val overlapBottom = Math.min(childBottom, dropY + 25)
        
        val overlapWidth = Math.max(0, overlapRight - overlapLeft)
        val overlapHeight = Math.max(0, overlapBottom - overlapTop)
        val overlapArea = overlapWidth * overlapHeight
        
        android.util.Log.d("SimpleDragListener", "Overlap bounds: left=$overlapLeft, right=$overlapRight, top=$overlapTop, bottom=$overlapBottom")
        android.util.Log.d("SimpleDragListener", "Overlap dimensions: ${overlapWidth}x${overlapHeight}, area=$overlapArea")
        
        // Calculate overlap percentage
        val overlapPercentage = if (childArea > 0) {
            (overlapArea.toFloat() / childArea.toFloat()) * 100f
        } else {
            0f
        }
        
        android.util.Log.d("SimpleDragListener", "Overlap percentage: ${overlapPercentage}%")
        
        return overlapPercentage
    }
    
    
    private fun setTargetFocus(targetView: View?, targetPosition: Int, targetRecyclerView: RecyclerView?) {
        // Clear previous focus
        clearTargetFocus()
        
        if (targetView != null && targetPosition >= 0) {
            currentFocusedView = targetView
            currentFocusedPosition = targetPosition
            currentFocusedRecyclerView = targetRecyclerView
            
            android.util.Log.d("SimpleDragListener", "Setting target focus on position: $targetPosition")
            
            // Apply clear visual effects to show drop zone
            animateToDropZoneState(targetView)
        }
    }
    
    private fun animateToDropZoneState(view: View) {
        android.util.Log.d("SimpleDragListener", "Setting drop zone state (no animation)")
        
        // Clear any existing animations
        view.clearAnimation()
        
        // Apply simple visual effects without animation
        view.alpha = 0.8f
        view.scaleX = 1.05f
        view.scaleY = 1.05f
        view.elevation = 4f
        view.setBackgroundColor(0x40FF6B35) // Orange background
        view.setPadding(8, 8, 8, 8) // Add padding for clear zone indication
        
        android.util.Log.d("SimpleDragListener", "Drop zone state set without animation")
    }
    
    private fun clearTargetFocus() {
        currentFocusedView?.let { view ->
            // Stop any animations
            view.clearAnimation()
            
            // Restore normal state without animation
            animateToNormalState(view)
            
            android.util.Log.d("SimpleDragListener", "Target space cleared")
        }
        
        currentFocusedView = null
        currentFocusedPosition = -1
        currentFocusedRecyclerView = null
    }
    
    private fun updateTargetFocus(targetRecyclerView: RecyclerView, event: DragEvent) {
        val x = event.x.toInt()
        val y = event.y.toInt()
        
        android.util.Log.d("SimpleDragListener", "=== UPDATING TARGET FOCUS ===")
        android.util.Log.d("SimpleDragListener", "Focus coordinates: ($x, $y)")
        
        val childView = targetRecyclerView.findChildViewUnder(x.toFloat(), y.toFloat())
        if (childView != null) {
            val position = targetRecyclerView.getChildAdapterPosition(childView)
            android.util.Log.d("SimpleDragListener", "Found child view at position: $position")
            
            if (position != RecyclerView.NO_POSITION && position >= 0) {
                // Check if this is a new target
                if (currentFocusedView != childView || currentFocusedPosition != position) {
                    android.util.Log.d("SimpleDragListener", "Setting new target focus on position: $position")
                    setTargetFocus(childView, position, targetRecyclerView)
                } else {
                    android.util.Log.d("SimpleDragListener", "Same target focus maintained")
                }
            } else {
                android.util.Log.d("SimpleDragListener", "Invalid position - clearing focus")
                clearTargetFocus()
            }
        } else {
            android.util.Log.d("SimpleDragListener", "No child view found - clearing focus")
            clearTargetFocus()
        }
    }
    
    private fun setOverlapTargetFocus(targetView: View?, targetPosition: Int, targetRecyclerView: RecyclerView?, overlapPercentage: Float) {
        // Clear previous focus
        clearTargetFocus()

        if (targetView != null && targetPosition >= 0) {
            currentFocusedView = targetView
            currentFocusedPosition = targetPosition
            currentFocusedRecyclerView = targetRecyclerView

            android.util.Log.d("SimpleDragListener", "Setting overlap target focus on position: $targetPosition with ${overlapPercentage}% overlap")
            
            // Apply enhanced visual effects based on overlap percentage
            animateToOverlapState(targetView, overlapPercentage)
        }
    }
    
    private fun animateToOverlapState(view: View, overlapPercentage: Float) {
        android.util.Log.d("SimpleDragListener", "Animating to overlap state with ${overlapPercentage}% overlap")
        
        // Clear any existing animations
        view.clearAnimation()
        
        // Set visual properties based on overlap percentage
        when {
            overlapPercentage >= 60f -> {
                // High overlap - strong visual feedback
                view.alpha = 0.2f
                view.scaleX = 0.9f
                view.scaleY = 0.9f
                view.elevation = 4f
                view.setBackgroundColor(0x60FF0000) // Red with high opacity
                view.setPadding(6, 6, 6, 6)
                
                // Strong pulse animation
                val pulseAnimation = ScaleAnimation(
                    0.9f, 1.1f,
                    0.9f, 1.1f,
                    Animation.RELATIVE_TO_SELF, 0.5f,
                    Animation.RELATIVE_TO_SELF, 0.5f
                )
                pulseAnimation.duration = 200 // Very fast for high overlap
                pulseAnimation.repeatCount = Animation.INFINITE
                pulseAnimation.repeatMode = Animation.REVERSE
                pulseAnimation.interpolator = AccelerateDecelerateInterpolator()
                view.startAnimation(pulseAnimation)
            }
            overlapPercentage >= 45f -> {
                // Medium-high overlap - moderate visual feedback
                view.alpha = 0.3f
                view.scaleX = 0.95f
                view.scaleY = 0.95f
                view.elevation = 3f
                view.setBackgroundColor(0x40FF8800) // Orange with medium opacity
                view.setPadding(4, 4, 4, 4)
                
                // Moderate pulse animation
                val pulseAnimation = ScaleAnimation(
                    0.95f, 1.05f,
                    0.95f, 1.05f,
                    Animation.RELATIVE_TO_SELF, 0.5f,
                    Animation.RELATIVE_TO_SELF, 0.5f
                )
                pulseAnimation.duration = 300 // Fast for medium overlap
                pulseAnimation.repeatCount = Animation.INFINITE
                pulseAnimation.repeatMode = Animation.REVERSE
                pulseAnimation.interpolator = AccelerateDecelerateInterpolator()
                view.startAnimation(pulseAnimation)
            }
            else -> {
                // Low overlap (30-45%) - subtle visual feedback
                view.alpha = 0.4f
                view.scaleX = 0.98f
                view.scaleY = 0.98f
                view.elevation = 2f
                view.setBackgroundColor(0x30FFAA00) // Yellow with low opacity
                view.setPadding(2, 2, 2, 2)
                
                // Subtle pulse animation
                val pulseAnimation = ScaleAnimation(
                    0.98f, 1.02f,
                    0.98f, 1.02f,
                    Animation.RELATIVE_TO_SELF, 0.5f,
                    Animation.RELATIVE_TO_SELF, 0.5f
                )
                pulseAnimation.duration = 400 // Normal speed for low overlap
                pulseAnimation.repeatCount = Animation.INFINITE
                pulseAnimation.repeatMode = Animation.REVERSE
                pulseAnimation.interpolator = AccelerateDecelerateInterpolator()
                view.startAnimation(pulseAnimation)
            }
        }
    }
    
    private fun startFloatingAnimation(view: View) {
        if (isFloating) return
        
        isFloating = true
        
        // Create floating animation (up and down movement)
        val floatUp = TranslateAnimation(0f, 0f, 0f, -8f)
        floatUp.duration = 800
        floatUp.interpolator = AccelerateDecelerateInterpolator()
        
        val floatDown = TranslateAnimation(0f, 0f, -8f, 0f)
        floatDown.duration = 800
        floatDown.interpolator = AccelerateDecelerateInterpolator()
        floatDown.startOffset = 800
        
        // Create animation set
        val animationSet = android.view.animation.AnimationSet(false)
        animationSet.addAnimation(floatUp)
        animationSet.addAnimation(floatDown)
        animationSet.repeatCount = Animation.INFINITE
        animationSet.repeatMode = Animation.REVERSE
        
        floatingAnimation = animationSet
        view.startAnimation(animationSet)
        
        android.util.Log.d("SimpleDragListener", "Floating animation started")
    }
    
    private fun stopFloatingAnimation() {
        if (!isFloating) return
        
        floatingAnimation?.let { animation ->
            animation.cancel()
            currentFocusedView?.clearAnimation()
        }
        
        floatingAnimation = null
        isFloating = false
        
        android.util.Log.d("SimpleDragListener", "Floating animation stopped")
    }
    
    private fun animateToOccupiedState(view: View) {
        // Apply visual changes immediately
        view.alpha = 0.3f
        view.scaleX = 0.95f
        view.scaleY = 0.95f
        view.elevation = 2f
        view.setBackgroundColor(0x40FF0000) // Semi-transparent red background
        view.setPadding(4, 4, 4, 4) // Add padding to create border effect
        
        // Add a simple pulse animation
        val pulseAnimation = ScaleAnimation(
            0.95f, 1.0f,  // scaleX from 0.95 to 1.0
            0.95f, 1.0f,  // scaleY from 0.95 to 1.0
            Animation.RELATIVE_TO_SELF, 0.5f,  // pivotX center
            Animation.RELATIVE_TO_SELF, 0.5f   // pivotY center
        )
        pulseAnimation.duration = 1000
        pulseAnimation.repeatCount = Animation.INFINITE
        pulseAnimation.repeatMode = Animation.REVERSE
        pulseAnimation.interpolator = AccelerateDecelerateInterpolator()
        
        view.startAnimation(pulseAnimation)
        
        android.util.Log.d("SimpleDragListener", "Occupied animation started")
    }
    
    private fun animateToNormalState(view: View) {
        // Stop any current animation
        view.clearAnimation()
        
        // Reset visual properties immediately
        view.alpha = 1.0f
        view.scaleX = 1.0f
        view.scaleY = 1.0f
        view.elevation = 0f
        view.setBackgroundColor(0x00000000) // Clear background
        view.setPadding(0, 0, 0, 0) // Reset padding
        
        android.util.Log.d("SimpleDragListener", "Normal state restored")
    }
    
    private fun showMoveInProgress(targetRecyclerView: RecyclerView, targetPosition: Int) {
        // Find the target view to show move in progress
        val targetView = targetRecyclerView.findViewHolderForAdapterPosition(targetPosition)?.itemView
        
        if (targetView != null) {
            // Simple visual feedback without animation
            targetView.alpha = 0.9f
            targetView.scaleX = 1.02f
            targetView.scaleY = 1.02f
            
            android.util.Log.d("SimpleDragListener", "Move in progress visual feedback applied")
        }
    }
    
    private fun calculateCenterBasedPosition(childView: View, position: Int, targetRecyclerView: RecyclerView, dropX: Int, dropY: Int): Int {
        val childTop = childView.top
        val childBottom = childView.bottom
        val childLeft = childView.left
        val childRight = childView.right
        
        val isRightRecyclerView = targetRecyclerView.id == R.id.right_recycler_view
        val isLeftRecyclerView = targetRecyclerView.id == R.id.left_recycler_view
        
        android.util.Log.d("SimpleDragListener", "=== INTUITIVE DROP ZONE CALCULATION ===")
        android.util.Log.d("SimpleDragListener", "Target item bounds: top=$childTop, bottom=$childBottom, left=$childLeft, right=$childRight")
        android.util.Log.d("SimpleDragListener", "Drop coordinates: ($dropX, $dropY)")
        android.util.Log.d("SimpleDragListener", "Target position: $position")
        
        // Calculate drop zones with clear boundaries
        val itemHeight = childBottom - childTop
        val itemWidth = childRight - childLeft
        
        // Create three zones: top third, middle third, bottom third
        val topThird = childTop + (itemHeight / 3)
        val bottomThird = childBottom - (itemHeight / 3)
        
        android.util.Log.d("SimpleDragListener", "Drop zones: topThird=$topThird, bottomThird=$bottomThird")
        android.util.Log.d("SimpleDragListener", "Item height: $itemHeight")
        
        // Determine which zone the drop is in
        val dropZone = when {
            dropY <= topThird -> "TOP"
            dropY >= bottomThird -> "BOTTOM"
            else -> "CENTER"
        }
        
        android.util.Log.d("SimpleDragListener", "Drop zone: $dropZone")
        
        // For grid layout (left RecyclerView), consider both X and Y coordinates
        if (isLeftRecyclerView) {
            val layoutManager = targetRecyclerView.layoutManager as? GridLayoutManager
            if (layoutManager != null) {
                val spanCount = layoutManager.spanCount
                android.util.Log.d("SimpleDragListener", "Grid layout detected with span count: $spanCount")
                
                // Calculate which column the drop is in
                val columnWidth = itemWidth
                val relativeX = dropX - childLeft
                val columnIndex = (relativeX / columnWidth).toInt().coerceIn(0, spanCount - 1)
                
                android.util.Log.d("SimpleDragListener", "Drop column index: $columnIndex")
                
                // Use intuitive drop zone logic
                val finalPosition = when (dropZone) {
                    "TOP" -> {
                        android.util.Log.d("SimpleDragListener", "Top zone - inserting before position: $position")
                        position
                    }
                    "BOTTOM" -> {
                        val insertPosition = position + 1
                        android.util.Log.d("SimpleDragListener", "Bottom zone - inserting after position: $insertPosition")
                        insertPosition
                    }
                    "CENTER" -> {
                        // For center drops, replace the current item (insert at same position)
                        android.util.Log.d("SimpleDragListener", "Center zone - replacing at position: $position")
                        position
                    }
                    else -> position
                }
                
                return finalPosition
            }
        }
        
        // For linear layout (right RecyclerView), use simple zone-based logic
        if (isRightRecyclerView) {
            val finalPosition = when (dropZone) {
                "TOP" -> {
                    android.util.Log.d("SimpleDragListener", "Top zone - inserting before position: $position")
                    position
                }
                "BOTTOM" -> {
                    val insertPosition = position + 1
                    val clampedPosition = insertPosition.coerceAtMost(2) // Right RecyclerView max 2 items
                    android.util.Log.d("SimpleDragListener", "Bottom zone - inserting after position: $clampedPosition")
                    clampedPosition
                }
                "CENTER" -> {
                    // For center drops, replace the current item
                    android.util.Log.d("SimpleDragListener", "Center zone - replacing at position: $position")
                    position
                }
                else -> position
            }
            
            return finalPosition
        }
        
        // Fallback: use zone-based logic
        val finalPosition = when (dropZone) {
            "TOP" -> {
                android.util.Log.d("SimpleDragListener", "Fallback top zone - inserting before position: $position")
                position
            }
            "BOTTOM" -> {
                val insertPosition = position + 1
                android.util.Log.d("SimpleDragListener", "Fallback bottom zone - inserting after position: $insertPosition")
                insertPosition
            }
            "CENTER" -> {
                android.util.Log.d("SimpleDragListener", "Fallback center zone - replacing at position: $position")
                position
            }
            else -> position
        }
        
        return finalPosition
    }
}
