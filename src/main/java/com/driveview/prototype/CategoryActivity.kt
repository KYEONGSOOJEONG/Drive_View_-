package com.driveview.prototype

import android.graphics.Paint
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.driveview.prototype.databinding.ActivityCategoryBinding

class CategoryActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityCategoryBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.previousButton.setOnClickListener { finish() }

        val menu = binding.categoryMenu
        Places.categories.chunked(COLUMNS).forEach { rowCategories ->
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    0,
                    1f,
                )
            }
            rowCategories.forEach { category ->
                row.addView(categoryLink(category), slotParams())
            }
            repeat(COLUMNS - rowCategories.size) {
                row.addView(View(this), slotParams())
            }
            menu.addView(row)
        }
    }

    private fun categoryLink(category: String): TextView =
        TextView(this, null, 0, R.style.Widget_DriveView_CategoryLink).apply {
            text = category
            paintFlags = paintFlags or Paint.UNDERLINE_TEXT_FLAG
            setOnClickListener {
                startActivity(PlaceListActivity.intent(this@CategoryActivity, category))
            }
        }

    private fun slotParams(): LinearLayout.LayoutParams =
        LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f)

    companion object {
        private const val COLUMNS = 3
    }
}
