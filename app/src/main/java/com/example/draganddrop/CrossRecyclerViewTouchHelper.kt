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
        
        // Handle moves within the same RecyclerView
        if (recyclerView == draggedFromRecyclerView) {
            dataManager.moveItem(recyclerView, fromPosition, recyclerView, toPosition)
            return true
        }
        
        // Handle moves between different RecyclerViews
        if (draggedFromRecyclerView != null && draggedFromRecyclerView != recyclerView) {
            dataManager.moveItem(draggedFromRecyclerView!!, draggedFromPosition, recyclerView, toPosition)
            return true
        }
        
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
                isDraggingOverOther = true
                targetRecyclerView = otherRecyclerView
                highlightTargetPosition(otherRecyclerView, targetPosition)
                drawConnectionLine(c, recyclerView, otherRecyclerView)
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
            // Move item to the other RecyclerView
            dataManager.moveItem(
                draggedFromRecyclerView!!,
                draggedFromPosition,
                otherRecyclerView,
                targetPosition
            )
        }
        
        // Clear highlights
        clearHighlights(otherRecyclerView)
        isDraggingOverOther = false
        targetRecyclerView = null
    }

    private fun getTargetPosition(targetRecyclerView: RecyclerView, draggedView: android.view.View): Int {
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
            return if (childView != null) {
                val position = targetRecyclerView.getChildAdapterPosition(childView)
                if (position != RecyclerView.NO_POSITION) position else -1
            } else {
                // If no child found, check if we should add to the end
                val adapter = targetRecyclerView.adapter
                if (adapter != null && adapter.itemCount > 0) {
                    adapter.itemCount - 1
                } else {
                    0
                }
            }
        }
        
        return -1
    }

    private fun highlightTargetPosition(recyclerView: RecyclerView, position: Int) {
        val viewHolder = recyclerView.findViewHolderForAdapterPosition(position)
        viewHolder?.itemView?.alpha = 0.6f
        viewHolder?.itemView?.elevation = 4f
    }

    private fun clearHighlights(recyclerView: RecyclerView) {
        for (i in 0 until recyclerView.childCount) {
            val child = recyclerView.getChildAt(i)
            child.alpha = 1.0f
            child.elevation = 0f
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
