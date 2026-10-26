package com.driveview.prototype

import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import android.view.ViewConfiguration
import android.widget.FrameLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.driveview.prototype.databinding.ActivityMapBinding
import kotlin.math.abs
import kotlin.math.hypot

class MapActivity : AppCompatActivity() {
    private var scale = 1f
    private var panX = 0f
    private var panY = 0f
    private var lastX = 0f
    private var lastY = 0f
    private var lastFocusX = 0f
    private var lastFocusY = 0f
    private var dragging = false
    private var transformed = false
    private var activePointerId = MotionEvent.INVALID_POINTER_ID

    private val pins = mutableListOf<PinTarget>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityMapBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.previousButton.setOnClickListener { finish() }
        binding.zoomInButton.setOnClickListener { zoomBy(binding, 1.25f) }
        binding.zoomOutButton.setOnClickListener { zoomBy(binding, 0.8f) }
        binding.mapViewport.post { bindMap(binding) }
    }

    private fun bindMap(binding: ActivityMapBinding) {
        val content = binding.mapContent
        val viewport = binding.mapViewport
        drawAreas(content)
        Places.all.forEach { place ->
            addPin(content, place.pinX, place.pinY, getColor(R.color.pin), 22, place.name, place.id)
        }
        addPin(content, HERE_X, HERE_Y, getColor(R.color.here), 30, getString(R.string.here), null)
        content.pivotX = 0f
        content.pivotY = 0f

        val touchSlop = ViewConfiguration.get(this).scaledTouchSlop.toFloat()
        val scaler = ScaleGestureDetector(this, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScaleBegin(detector: ScaleGestureDetector): Boolean {
                lastFocusX = detector.focusX
                lastFocusY = detector.focusY
                transformed = true
                dragging = false
                return true
            }

            override fun onScale(detector: ScaleGestureDetector): Boolean {
                val focusX = detector.focusX
                val focusY = detector.focusY
                zoomAt(binding, detector.scaleFactor, focusX, focusY)
                panX += focusX - lastFocusX
                panY += focusY - lastFocusY
                lastFocusX = focusX
                lastFocusY = focusY
                clamp(viewport)
                applyTransform(content)
                return true
            }
        }).also {
            it.isQuickScaleEnabled = false
            it.isStylusScaleEnabled = false
        }

        viewport.setOnTouchListener { _, event ->
            scaler.onTouchEvent(event)
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    activePointerId = event.getPointerId(0)
                    lastX = event.x
                    lastY = event.y
                    dragging = false
                    transformed = false
                    viewport.parent?.requestDisallowInterceptTouchEvent(true)
                }
                MotionEvent.ACTION_POINTER_DOWN -> {
                    transformed = true
                    dragging = false
                }
                MotionEvent.ACTION_MOVE -> if (!scaler.isInProgress && event.pointerCount == 1) {
                    val index = event.findPointerIndex(activePointerId).let { if (it >= 0) it else 0 }
                    val x = event.getX(index)
                    val y = event.getY(index)
                    val dx = x - lastX
                    val dy = y - lastY
                    if (!dragging && (abs(dx) > touchSlop || abs(dy) > touchSlop)) {
                        dragging = true
                        lastX = x
                        lastY = y
                    } else if (dragging) {
                        panX += dx
                        panY += dy
                        lastX = x
                        lastY = y
                        clamp(viewport)
                        applyTransform(content)
                    }
                }
                MotionEvent.ACTION_POINTER_UP -> {
                    val lifted = event.actionIndex
                    if (event.getPointerId(lifted) == activePointerId && event.pointerCount > 1) {
                        val next = if (lifted == 0) 1 else 0
                        activePointerId = event.getPointerId(next)
                        lastX = event.getX(next)
                        lastY = event.getY(next)
                    }
                    dragging = false
                }
                MotionEvent.ACTION_UP -> {
                    if (!dragging && !transformed) openPinAt(event.x, event.y)
                    activePointerId = MotionEvent.INVALID_POINTER_ID
                }
                MotionEvent.ACTION_CANCEL -> activePointerId = MotionEvent.INVALID_POINTER_ID
            }
            true
        }
    }

    private fun drawAreas(content: FrameLayout) {
        listOf(
            "영도" to (0.36f to 0.22f),
            "남구" to (0.48f to 0.68f),
            "해운대" to (0.74f to 0.52f),
            "금정" to (0.62f to 0.30f),
            "기장" to (0.30f to 0.46f),
            "사하" to (0.18f to 0.74f),
        ).forEach { (name, point) ->
            val label = TextView(this).apply {
                text = name
                setTextColor(getColor(R.color.area_label))
                textSize = 20f
            }
            content.addView(label, FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
            ))
            label.post {
                label.x = point.first * content.width - label.width / 2f
                label.y = point.second * content.height - label.height / 2f
            }
        }
    }

    private fun addPin(
        content: FrameLayout,
        xFraction: Float,
        yFraction: Float,
        color: Int,
        sizeDp: Int,
        label: String,
        placeId: String?,
    ) {
        val size = (sizeDp * resources.displayMetrics.density).toInt()
        val pin = View(this).apply {
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(color)
            }
            contentDescription = label
        }
        content.addView(pin, FrameLayout.LayoutParams(size, size, Gravity.START or Gravity.TOP))
        pin.post {
            val left = xFraction * content.width - size / 2f
            val top = yFraction * content.height - size / 2f
            pin.x = left
            pin.y = top
            pins += PinTarget(left, top, size.toFloat(), placeId)
        }
        if (placeId == null) {
            val caption = TextView(this).apply {
                text = label
                setTextColor(getColor(R.color.here))
                textSize = 14f
            }
            content.addView(caption)
            caption.post {
                caption.x = xFraction * content.width + size / 2f + 8f
                caption.y = yFraction * content.height - caption.height / 2f
            }
        }
    }

    private fun openPinAt(viewX: Float, viewY: Float) {
        val contentX = (viewX - panX) / scale
        val contentY = (viewY - panY) / scale
        val hit = pins.filter { it.placeId != null }.minByOrNull { target ->
            hypot(contentX - (target.left + target.size / 2f), contentY - (target.top + target.size / 2f))
        } ?: return
        val centerX = hit.left + hit.size / 2f
        val centerY = hit.top + hit.size / 2f
        if (hypot(contentX - centerX, contentY - centerY) > hit.size) return
        startActivity(PlaceListActivity.intentAround(this, hit.placeId!!))
    }

    private fun zoomBy(binding: ActivityMapBinding, factor: Float) {
        val viewport = binding.mapViewport
        zoomAt(binding, factor, viewport.width / 2f, viewport.height / 2f)
    }

    private fun zoomAt(binding: ActivityMapBinding, factor: Float, focusX: Float, focusY: Float) {
        val previous = scale
        scale = (scale * factor).coerceIn(MIN_SCALE, MAX_SCALE)
        val applied = scale / previous
        panX = focusX - applied * (focusX - panX)
        panY = focusY - applied * (focusY - panY)
        clamp(binding.mapViewport)
        applyTransform(binding.mapContent)
    }

    private fun clamp(viewport: View) {
        val minX = viewport.width - viewport.width * scale
        val minY = viewport.height - viewport.height * scale
        panX = panX.coerceIn(minX, 0f)
        panY = panY.coerceIn(minY, 0f)
    }

    private fun applyTransform(content: View) {
        content.scaleX = scale
        content.scaleY = scale
        content.translationX = panX
        content.translationY = panY
    }

    private data class PinTarget(val left: Float, val top: Float, val size: Float, val placeId: String?)

    companion object {
        private const val HERE_X = 0.50f
        private const val HERE_Y = 0.56f
        private const val MIN_SCALE = 1f
        private const val MAX_SCALE = 3.2f
    }
}
