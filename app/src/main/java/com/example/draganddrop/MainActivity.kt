package com.example.draganddrop

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.graphics.Rect
import android.view.View
import android.widget.Button
import android.widget.Toast
import com.example.draganddrop.viewmodel.DragDropViewModel

class MainActivity : AppCompatActivity() {
    
    // Custom ItemDecoration for spacing
    class ItemSpacingDecoration(private val spacing: Int) : RecyclerView.ItemDecoration() {
        override fun getItemOffsets(
            outRect: Rect,
            view: View,
            parent: RecyclerView,
            state: RecyclerView.State
        ) {
            outRect.left = spacing
            outRect.right = spacing
            outRect.top = spacing
            outRect.bottom = spacing
        }
    }
    
    private lateinit var leftRecyclerView: RecyclerView
    private lateinit var rightRecyclerView: RecyclerView
    private lateinit var leftAdapter: ItemAdapter
    private lateinit var rightAdapter: ItemAdapter
    private lateinit var dragListener: SimpleDragListener
    
    // MVVM Components
    private val viewModel: DragDropViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        
        setupRecyclerViews()
        setupButtons()
        setupViewModelObservers()
    }
    
    private fun setupRecyclerViews() {
        leftRecyclerView = findViewById(R.id.left_recycler_view)
        rightRecyclerView = findViewById(R.id.right_recycler_view)
        
        // Initialize drag listener with ViewModel
        dragListener = SimpleDragListener(viewModel)
        
        // Setup Left RecyclerView (Grid)
        leftAdapter = ItemAdapter(
            onItemClick = { item -> 
                android.util.Log.d("MainActivity", "Left item clicked: ${item.text}")
            },
            onItemLongClick = { item -> 
                android.util.Log.d("MainActivity", "Left item long clicked: ${item.text}")
                val position = viewModel.leftItems.value?.indexOf(item) ?: -1
                if (position != -1 && item.isDraggable) {
                    dragListener.startDrag(item, leftRecyclerView, position)
                } else if (!item.isDraggable) {
                    Toast.makeText(this, "Item ${item.text} cannot be dragged", Toast.LENGTH_SHORT).show()
                }
            }
        )
        
        leftRecyclerView.apply {
            layoutManager = GridLayoutManager(this@MainActivity, 2)
            adapter = leftAdapter
            setOnDragListener(dragListener)
            addItemDecoration(ItemSpacingDecoration(8))
        }
        
        // Setup Right RecyclerView (Linear)
        rightAdapter = ItemAdapter(
            onItemClick = { item -> 
                android.util.Log.d("MainActivity", "Right item clicked: ${item.text}")
            },
            onItemLongClick = { item -> 
                android.util.Log.d("MainActivity", "Right item long clicked: ${item.text}")
                val position = viewModel.rightItems.value?.indexOf(item) ?: -1
                if (position != -1 && item.isDraggable) {
                    dragListener.startDrag(item, rightRecyclerView, position)
                } else if (!item.isDraggable) {
                    Toast.makeText(this, "Item ${item.text} cannot be dragged", Toast.LENGTH_SHORT).show()
                }
            }
        )
        
        rightRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@MainActivity, LinearLayoutManager.VERTICAL, false)
            adapter = rightAdapter
            setOnDragListener(dragListener)
            addItemDecoration(ItemSpacingDecoration(8))
        }
        
        // Observe ViewModel data for adapters
        viewModel.leftItems.observe(this) { items ->
            leftAdapter.submitList(items)
        }
        
        viewModel.rightItems.observe(this) { items ->
            rightAdapter.submitList(items)
        }
    }
    
    private fun setupViewModelObservers() {
        // Observe drag operation results
        viewModel.dragOperationResult.observe(this) { result ->
            result?.let {
                Toast.makeText(this, it, Toast.LENGTH_SHORT).show()
                viewModel.clearDragResult()
            }
        }
        
        // Observe loading state
        viewModel.isLoading.observe(this) { isLoading ->
            // You can show/hide loading indicators here if needed
            android.util.Log.d("MainActivity", "Loading state: $isLoading")
        }
    }
    
    private fun setupButtons() {
        val resetButton = findViewById<Button>(R.id.reset_button)
        val logButton = findViewById<Button>(R.id.log_button)
        
        resetButton.setOnClickListener {
            viewModel.resetData()
            Toast.makeText(this, "List reset to initial state", Toast.LENGTH_SHORT).show()
        }
        
        logButton.setOnClickListener {
            // Refresh all repository values first
            viewModel.refreshAllValues()
            // Then log the current array order
            logCurrentArrayOrder()
        }
    }
    
    private fun logCurrentArrayOrder() {
        val allItems = viewModel.getAllItems()
        val leftItems = viewModel.leftItems.value ?: emptyList()
        val rightItems = viewModel.rightItems.value ?: emptyList()
        
        val logMessage = buildString {
            appendLine("=== CURRENT ARRAY ORDER ===")
            appendLine("Total Items: ${allItems.size}")
            appendLine()
            appendLine("Left RecyclerView (${leftItems.size} items):")
            leftItems.forEachIndexed { index, item ->
                val status = if (item.isDraggable) "Draggable" else "Locked"
                appendLine("  [$index] ${item.text} ($status)")
            }
            appendLine()
            appendLine("Right RecyclerView (${rightItems.size} items):")
            rightItems.forEachIndexed { index, item ->
                val status = if (item.isDraggable) "Draggable" else "Locked"
                appendLine("  [$index] ${item.text} ($status)")
            }
            appendLine()
            appendLine("Global Array Order:")
            allItems.forEachIndexed { index, item ->
                val status = if (item.isDraggable) "Draggable" else "Locked"
                appendLine("  [$index] ${item.text} ($status)")
            }
        }
        
        android.util.Log.d("MainActivity", logMessage)
        Toast.makeText(this, "Values refreshed and logged to console", Toast.LENGTH_SHORT).show()
    }
}