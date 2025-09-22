package com.example.draganddrop

import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.graphics.Rect
import android.view.View

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
    
    // ViewModel
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
        setupDragAndDrop()
        observeViewModel()
    }
    
    private fun setupRecyclerViews() {
        leftRecyclerView = findViewById(R.id.left_recycler_view)
        rightRecyclerView = findViewById(R.id.right_recycler_view)
        
        // Setup Left RecyclerView (Grid)
        leftAdapter = ItemAdapter(
            onItemClick = { item -> 
                android.util.Log.d("MainActivity", "Left item clicked: ${item.text}")
                showToast("Left: ${item.text}")
            },
            onItemLongClick = { item -> 
                android.util.Log.d("MainActivity", "Left item long clicked: ${item.text}")
                showToast("Long clicked: ${item.text}")
            }
        )
        leftRecyclerView.apply {
            layoutManager = GridLayoutManager(this@MainActivity, 2)
            adapter = leftAdapter
            addItemDecoration(ItemSpacingDecoration(8)) // 8dp spacing
        }
        
        // Setup Right RecyclerView (Linear)
        rightAdapter = ItemAdapter(
            onItemClick = { item -> 
                android.util.Log.d("MainActivity", "Right item clicked: ${item.text}")
                showToast("Right: ${item.text}")
            },
            onItemLongClick = { item -> 
                android.util.Log.d("MainActivity", "Right item long clicked: ${item.text}")
                showToast("Long clicked: ${item.text}")
            }
        )
        rightRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@MainActivity, LinearLayoutManager.VERTICAL, false)
            adapter = rightAdapter
            addItemDecoration(ItemSpacingDecoration(8)) // 8dp spacing
        }
    }
    
    private fun setupDragAndDrop() {
        val leftTouchHelper = CrossRecyclerViewTouchHelper(
            viewModel = viewModel,
            leftRecyclerView = leftRecyclerView,
            rightRecyclerView = rightRecyclerView,
            requiredOverlapPercentage = 90.0
        )
        
        val rightTouchHelper = CrossRecyclerViewTouchHelper(
            viewModel = viewModel,
            leftRecyclerView = leftRecyclerView,
            rightRecyclerView = rightRecyclerView,
            requiredOverlapPercentage = 90.0
        )
        
        val leftItemTouchHelper = androidx.recyclerview.widget.ItemTouchHelper(leftTouchHelper)
        val rightItemTouchHelper = androidx.recyclerview.widget.ItemTouchHelper(rightTouchHelper)
        
        leftItemTouchHelper.attachToRecyclerView(leftRecyclerView)
        rightItemTouchHelper.attachToRecyclerView(rightRecyclerView)
    }
    
    private fun observeViewModel() {
        // Observe left items
        viewModel.leftItems.observe(this, Observer { items ->
            android.util.Log.d("MainActivity", "Left items updated: ${items.map { it.text }}")
            leftAdapter.submitList(items)
        })
        
        // Observe right items
        viewModel.rightItems.observe(this, Observer { items ->
            android.util.Log.d("MainActivity", "Right items updated: ${items.map { it.text }}")
            rightAdapter.submitList(items)
        })
        
        // Observe drag state
        viewModel.isDragging.observe(this, Observer { isDragging ->
            android.util.Log.d("MainActivity", "Drag state: $isDragging")
            // You can add UI feedback based on drag state here
        })
        
        // Observe drag source info
        viewModel.dragSourceInfo.observe(this, Observer { dragSourceInfo ->
            if (dragSourceInfo != null) {
                android.util.Log.d("MainActivity", "Drag source: ${dragSourceInfo.item.text} at position ${dragSourceInfo.position}")
            } else {
                android.util.Log.d("MainActivity", "Drag source cleared")
            }
        })
    }
    
    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}