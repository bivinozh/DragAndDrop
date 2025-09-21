package com.example.draganddrop

data class Item(
    val id: Int,
    val text: String,
    val color: Int
) {
    companion object {
        fun createSampleItems(): List<Item> {
            val colors = listOf(
                android.graphics.Color.RED,
                android.graphics.Color.BLUE,
                android.graphics.Color.GREEN,
                android.graphics.Color.YELLOW,
                android.graphics.Color.MAGENTA,
                android.graphics.Color.CYAN,
                android.graphics.Color.parseColor("#FF9800"),
                android.graphics.Color.parseColor("#9C27B0"),
                android.graphics.Color.parseColor("#795548"),
                android.graphics.Color.parseColor("#607D8B"),
                android.graphics.Color.parseColor("#E91E63"),
                android.graphics.Color.parseColor("#3F51B5")
            )
            
            return (1..12).map { index ->
                Item(
                    id = index,
                    text = "Item $index",
                    color = colors[index - 1]
                )
            }
        }
    }
}
