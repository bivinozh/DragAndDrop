package com.example.draganddrop

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
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
    private lateinit var dataManager: UnifiedDataManager
    private lateinit var dragListener: SimpleDragListener

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
    }
    
    private fun setupRecyclerViews() {
        leftRecyclerView = findViewById(R.id.left_recycler_view)
        rightRecyclerView = findViewById(R.id.right_recycler_view)
        
        // Initialize data manager
        dataManager = UnifiedDataManager()
        
        // Initialize drag listener
        dragListener = SimpleDragListener(dataManager, leftRecyclerView, rightRecyclerView)
        
        // Setup Left RecyclerView (Grid)
        leftAdapter = ItemAdapter(
            onItemClick = { item -> 
                android.util.Log.d("MainActivity", "Left item clicked: ${item.text}")
            },
            onItemLongClick = { item -> 
                android.util.Log.d("MainActivity", "Left item long clicked: ${item.text}")
                val position = dataManager.getLeftItems().indexOf(item)
                if (position != -1) {
                    dragListener.startDrag(item, leftRecyclerView, position)
                }
            }
        )
               leftRecyclerView.apply {
                   layoutManager = GridLayoutManager(this@MainActivity, 2)
                   adapter = leftAdapter
                   setOnDragListener(dragListener)
                   addItemDecoration(ItemSpacingDecoration(8)) // 8dp spacing
               }
        
        // Setup Right RecyclerView (Linear)
        rightAdapter = ItemAdapter(
            onItemClick = { item -> 
                android.util.Log.d("MainActivity", "Right item clicked: ${item.text}")
            },
            onItemLongClick = { item -> 
                android.util.Log.d("MainActivity", "Right item long clicked: ${item.text}")
                val position = dataManager.getRightItems().indexOf(item)
                if (position != -1) {
                    dragListener.startDrag(item, rightRecyclerView, position)
                }
            }
        )
               rightRecyclerView.apply {
                   layoutManager = LinearLayoutManager(this@MainActivity, LinearLayoutManager.VERTICAL, false)
                   adapter = rightAdapter
                   setOnDragListener(dragListener)
                   addItemDecoration(ItemSpacingDecoration(8)) // 8dp spacing
               }
        
        // Set up data manager with adapters
        dataManager.setAdapters(leftAdapter, rightAdapter)
        
        // Log initial data
        android.util.Log.d("MainActivity", "Unified data count: ${dataManager.getAllItems().size}")
        android.util.Log.d("MainActivity", "Left items count: ${dataManager.getLeftItems().size}")
        android.util.Log.d("MainActivity", "Right items count: ${dataManager.getRightItems().size}")
        android.util.Log.d("MainActivity", "Left items: ${dataManager.getLeftItems().map { it.text }}")
        android.util.Log.d("MainActivity", "Right items: ${dataManager.getRightItems().map { it.text }}")
        
               // Validate data consistency
               dataManager.validateDataConsistency()
               
               // Test position mapping
               dataManager.testPositionMapping()
    }
}