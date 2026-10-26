package com.driveview.prototype

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.TextPaint
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.driveview.prototype.databinding.ActivityPlaceListBinding

class PlaceListActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityPlaceListBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.previousButton.setOnClickListener { finish() }
        val centerId = intent.getStringExtra(EXTRA_CENTER_ID)
        val places = if (centerId != null) {
            Places.around(Places.byId(centerId))
        } else {
            Places.inCategory(intent.getStringExtra(EXTRA_CATEGORY).orEmpty())
        }
        showPlaces(binding.placeList, places)
    }

    private fun showPlaces(list: LinearLayout, places: List<Place>) {
        val rowHeight = dp(ROW_HEIGHT_DP).toInt()
        val sidePad = dp(8f).toInt()
        val textHeight = rowHeight * TEXT_HEIGHT_RATIO
        if (places.isNotEmpty()) list.addView(tableRule())
        places.forEach { place ->
            val nameSize = textSizeFilling(TextPaint(), textHeight)
            val distanceLabel = "${place.distanceKm} km"
            val nameView = placeLabel(place.name, nameSize)
            val distanceView = placeLabel(distanceLabel, nameSize * DISTANCE_SCALE).apply {
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                ).apply { marginStart = dp(12f).toInt() }
            }
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL or Gravity.START
                setPadding(sidePad, 0, sidePad, 0)
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    rowHeight,
                )
                setOnClickListener {
                    startActivity(PlaceDetailActivity.intent(this@PlaceListActivity, place.id))
                }
                addView(nameView)
                addView(distanceView)
            }
            list.addView(row)
            list.addView(tableRule())
            row.post {
                val available = row.width - row.paddingLeft - row.paddingRight
                val used = nameView.paint.measureText(place.name) +
                    distanceView.paint.measureText(distanceLabel) +
                    dp(12f)
                if (available > 0 && used > available) {
                    val scale = available / used
                    nameView.setTextSize(TypedValue.COMPLEX_UNIT_PX, nameView.textSize * scale)
                    distanceView.setTextSize(TypedValue.COMPLEX_UNIT_PX, distanceView.textSize * scale)
                }
            }
        }
    }

    private fun placeLabel(label: String, textSizePx: Float): TextView =
        TextView(this).apply {
            text = label
            includeFontPadding = false
            maxLines = 1
            gravity = Gravity.CENTER_VERTICAL
            setTextColor(getColor(R.color.ink))
            setTextSize(TypedValue.COMPLEX_UNIT_PX, textSizePx)
        }

    private fun textSizeFilling(paint: TextPaint, targetPx: Float): Float {
        paint.textSize = 100f
        val unit = paint.fontMetrics.descent - paint.fontMetrics.ascent
        return 100f * targetPx / unit
    }

    private fun tableRule(): View =
        View(this).apply {
            setBackgroundColor(0xFFE4E4E4.toInt())
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(1f).toInt().coerceAtLeast(1),
            )
        }

    private fun dp(value: Float): Float = value * resources.displayMetrics.density

    companion object {
        private const val EXTRA_CATEGORY = "category"
        private const val EXTRA_CENTER_ID = "center_id"
        private const val ROW_HEIGHT_DP = 180f
        private const val TEXT_HEIGHT_RATIO = 0.8f * 0.8f
        private const val DISTANCE_SCALE = 0.7f

        fun intent(context: Context, category: String): Intent =
            Intent(context, PlaceListActivity::class.java).putExtra(EXTRA_CATEGORY, category)

        fun intentAround(context: Context, placeId: String): Intent =
            Intent(context, PlaceListActivity::class.java).putExtra(EXTRA_CENTER_ID, placeId)
    }
}
