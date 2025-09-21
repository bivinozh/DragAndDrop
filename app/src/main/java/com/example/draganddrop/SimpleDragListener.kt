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

    override fun onDrag(view: View, event: DragEvent): Boolean {
        when (event.action) {
            DragEvent.ACTION_DRAG_STARTED -> {
                android.util.Log.d("SimpleDragListener", "Drag started")
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
                return true
            }
            DragEvent.ACTION_DROP -> {
                android.util.Log.d("SimpleDragListener", "Drop on: ${view.id}")
                view.alpha = 1.0f
                
                val targetRecyclerView = view as RecyclerView
                val targetPosition = getDropPosition(targetRecyclerView, event)
                
                if (targetPosition != -1 && draggedFromRecyclerView != null) {
                    android.util.Log.d("SimpleDragListener", "Moving item from ${draggedFromRecyclerView?.id} to ${targetRecyclerView.id}")
                    dataManager.moveItem(
                        draggedFromRecyclerView!!,
                        draggedFromPosition,
                        targetRecyclerView,
                        targetPosition
                    )
                }
                return true
            }
            DragEvent.ACTION_DRAG_ENDED -> {
                android.util.Log.d("SimpleDragListener", "Drag ended")
                view.alpha = 1.0f
                draggedItem = null
                draggedFromPosition = -1
                draggedFromRecyclerView = null
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
        
        val viewHolder = fromRecyclerView.findViewHolderForAdapterPosition(fromPosition)
        if (viewHolder != null) {
            val dragShadowBuilder = View.DragShadowBuilder(viewHolder.itemView)
            viewHolder.itemView.startDrag(null, dragShadowBuilder, null, 0)
        }
    }

    private fun getDropPosition(targetRecyclerView: RecyclerView, event: DragEvent): Int {
        val x = event.x.toInt()
        val y = event.y.toInt()
        
        android.util.Log.d("SimpleDragListener", "Drop position calculation: x=$x, y=$y, RecyclerView bounds: ${targetRecyclerView.width}x${targetRecyclerView.height}")
        
        val childView = targetRecyclerView.findChildViewUnder(x.toFloat(), y.toFloat())
        if (childView != null) {
            val position = targetRecyclerView.getChildAdapterPosition(childView)
            android.util.Log.d("SimpleDragListener", "Found child view at position: $position")
            
            if (position != RecyclerView.NO_POSITION) {
                // Check if we're dropping in the upper or lower half of the child view
                val childTop = childView.top
                val childBottom = childView.bottom
                val childCenter = childTop + (childBottom - childTop) / 2
                
                if (y < childCenter) {
                    // Dropping in upper half - insert before this item
                    android.util.Log.d("SimpleDragListener", "Dropping in upper half, position: $position")
                    return position
                } else {
                    // Dropping in lower half - insert after this item
                    val insertPosition = position + 1
                    android.util.Log.d("SimpleDragListener", "Dropping in lower half, position: $insertPosition")
                    return insertPosition
                }
            }
        }
        
        // Check if we're in the RecyclerView bounds but no child found
        val adapter = targetRecyclerView.adapter
        val itemCount = adapter?.itemCount ?: 0
        
        if (x >= 0 && x <= targetRecyclerView.width && y >= 0 && y <= targetRecyclerView.height) {
            // We're within bounds but no child found - this means drop at the end
            val endPosition = if (itemCount > 0) itemCount else 0
            
            // Special handling for right RecyclerView - ensure we can drop at position 1 (last position)
            val isRightRecyclerView = targetRecyclerView.id == R.id.right_recycler_view
            val finalPosition = if (isRightRecyclerView && endPosition >= 2) {
                // For right RecyclerView, max position is 1 (since it only has 2 items)
                1
            } else {
                endPosition
            }
            
            android.util.Log.d("SimpleDragListener", "No child found, dropping at end position: $finalPosition (original: $endPosition, isRight: $isRightRecyclerView)")
            return finalPosition
        }
        
        android.util.Log.d("SimpleDragListener", "Outside RecyclerView bounds")
        return -1
    }
}
