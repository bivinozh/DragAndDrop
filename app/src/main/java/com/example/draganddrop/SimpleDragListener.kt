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
        when (event.action) {
            DragEvent.ACTION_DRAG_STARTED -> {
                return true
            }
            DragEvent.ACTION_DRAG_ENTERED -> {
                view.alpha = 0.7f
                return true
            }
            DragEvent.ACTION_DRAG_EXITED -> {
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
                view.alpha = 1.0f
                
                val targetRecyclerView = view as RecyclerView
                val targetPosition = getDropPosition(targetRecyclerView, event)
                
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
                    
                    // Use ViewModel to handle the move
                    viewModel.moveItemBetweenRecyclerViews(fromType, draggedFromPosition, toType, targetPosition)
                }
                return true
            }
            DragEvent.ACTION_DRAG_ENDED -> {
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
            viewHolder.itemView.startDrag(null, dragShadowBuilder, null, 0)
        }
    }

    private fun getDropPosition(targetRecyclerView: RecyclerView, event: DragEvent): Int {
        val x = event.x.toInt()
        val y = event.y.toInt()
        
        val childView = targetRecyclerView.findChildViewUnder(x.toFloat(), y.toFloat())
        if (childView != null) {
            val position = targetRecyclerView.getChildAdapterPosition(childView)
            if (position != RecyclerView.NO_POSITION && position >= 0) {
                return calculateCenterBasedPosition(childView, position, targetRecyclerView, x, y)
            }
        }
        
        // Drop at the end if no child found
        val itemCount = targetRecyclerView.adapter?.itemCount ?: 0
        val isRightRecyclerView = targetRecyclerView.id == R.id.right_recycler_view
        
        return if (isRightRecyclerView) {
            itemCount.coerceAtMost(2)
        } else {
            itemCount
        }
    }
    
    private fun getItemAtPosition(recyclerView: RecyclerView, position: Int): Item? {
        val adapter = recyclerView.adapter as? ItemAdapter
        return adapter?.getItemAt(position)
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
