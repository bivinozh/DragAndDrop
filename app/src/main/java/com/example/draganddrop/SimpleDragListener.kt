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
        
        val childView = targetRecyclerView.findChildViewUnder(x.toFloat(), y.toFloat())
        return if (childView != null) {
            val position = targetRecyclerView.getChildAdapterPosition(childView)
            if (position != RecyclerView.NO_POSITION) position else -1
        } else {
            // Drop at the end
            targetRecyclerView.adapter?.itemCount?.minus(1) ?: -1
        }
    }
}
