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
    
    // Floating animation
    private var floatingAnimation: Animation? = null
    private var isFloating = false
    
    // Slow-motion animation settings
    private val SLOW_MOTION_DURATION = 1500L // 1.5 seconds for smooth transitions
    private val SLOW_MOTION_INTERPOLATOR = DecelerateInterpolator(2.0f)

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
                    
                    // Execute move immediately but with visual feedback
                    android.util.Log.d("SimpleDragListener", "Executing move with visual feedback")
                    
                    // Show move in progress visual feedback
                    showMoveInProgress(targetRecyclerView, targetPosition)
                    
                    // Execute the move
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
                // Calculate drop position based on center of target item
                val dropPosition = calculateCenterBasedPosition(childView, position, targetRecyclerView, x, y)
                android.util.Log.d("SimpleDragListener", "Center-based drop position: $dropPosition")
                return dropPosition
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
            
            // Apply slow-motion space occupation visual effects
            animateToOccupiedState(targetView)
            
            android.util.Log.d("SimpleDragListener", "Target space occupied: position $targetPosition")
        }
    }
    
    private fun clearTargetFocus() {
        currentFocusedView?.let { view ->
            // Stop floating animation
            stopFloatingAnimation()
            
            // Animate back to normal state with slow-motion
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
            // Create a pulsing animation to show move in progress
            val pulseAnimation = ScaleAnimation(
                1.0f, 1.1f,  // scaleX from 1.0 to 1.1
                1.0f, 1.1f,  // scaleY from 1.0 to 1.1
                Animation.RELATIVE_TO_SELF, 0.5f,  // pivotX center
                Animation.RELATIVE_TO_SELF, 0.5f   // pivotY center
            )
            pulseAnimation.duration = 250
            pulseAnimation.repeatCount = 1
            pulseAnimation.repeatMode = Animation.REVERSE
            pulseAnimation.interpolator = AccelerateDecelerateInterpolator()
            
            // Start the pulse animation
            targetView.startAnimation(pulseAnimation)
            
            android.util.Log.d("SimpleDragListener", "Move in progress animation started")
        }
    }
    
    private fun calculateCenterBasedPosition(childView: View, position: Int, targetRecyclerView: RecyclerView, dropX: Int, dropY: Int): Int {
        val childTop = childView.top
        val childBottom = childView.bottom
        val childLeft = childView.left
        val childRight = childView.right
        
        val isRightRecyclerView = targetRecyclerView.id == R.id.right_recycler_view
        
        // Calculate the center of the target item
        val childCenterY = childTop + (childBottom - childTop) / 2
        
        android.util.Log.d("SimpleDragListener", "=== CENTER-BASED POSITION CALCULATION ===")
        android.util.Log.d("SimpleDragListener", "Target item bounds: top=$childTop, bottom=$childBottom, centerY=$childCenterY")
        android.util.Log.d("SimpleDragListener", "Drop coordinates: ($dropX, $dropY)")
        android.util.Log.d("SimpleDragListener", "Target position: $position")
        android.util.Log.d("SimpleDragListener", "Is right RecyclerView: $isRightRecyclerView")
        
        // Simple center-based logic: if drop is above center, insert before; if below center, insert after
        val isAboveCenter = dropY < childCenterY
        
        android.util.Log.d("SimpleDragListener", "Drop is above center: $isAboveCenter (dropY=$dropY, centerY=$childCenterY)")
        
        if (isAboveCenter) {
            // Dropping above center - insert before this item
            android.util.Log.d("SimpleDragListener", "Above center - inserting before position: $position")
            return position
        } else {
            // Dropping below center - insert after this item
            val insertPosition = position + 1
            
            // For right RecyclerView, ensure we don't exceed valid positions
            val finalPosition = if (isRightRecyclerView) {
                // Right RecyclerView has positions 0, 1, and we allow 2 for the end
                insertPosition.coerceAtMost(2)
            } else {
                // Left RecyclerView can have any position
                insertPosition
            }
            
            android.util.Log.d("SimpleDragListener", "Below center - inserting after position: $finalPosition (original: $insertPosition)")
            return finalPosition
        }
    }
}
