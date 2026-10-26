package com.driveview.prototype

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.driveview.prototype.databinding.ActivityPlaceDetailBinding

class PlaceDetailActivity : AppCompatActivity() {
    private lateinit var binding: ActivityPlaceDetailBinding
    private lateinit var place: Place
    private lateinit var guidance: Guidance

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPlaceDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)
        place = Places.byId(intent.getStringExtra(EXTRA_ID)!!)
        guidance = Guidance(this, guidanceCallbacks)
        binding.placeName.text = place.name
        binding.distance.text = getString(R.string.distance_km, place.distanceKm)
        binding.averageRating.text = getString(R.string.average_rating, SAMPLE_AVERAGE_RATING)
        binding.eta.text = getString(R.string.eta_minutes, place.etaMin)
        binding.photos.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        binding.photos.adapter = PhotoStripAdapter(samplePhotos, ::showPhoto)
        binding.photoPager.adapter = PhotoPagerAdapter(samplePhotos, ::hidePhoto)
        binding.photoPager.registerOnPageChangeCallback(object : androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) = showCaption(position)
        })
        binding.photoPreviousButton.setOnClickListener { hidePhoto() }
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (binding.photoViewer.visibility == View.VISIBLE) hidePhoto() else leave()
            }
        })
        binding.backButton.setOnClickListener { leave() }
        binding.startButton.setOnClickListener { guidance.start(place) }
        binding.playlistButton.setOnClickListener { showPlaylists() }
        binding.closePlaylistButton.setOnClickListener {
            binding.playlistPanel.visibility = View.GONE
        }
        binding.nearbyButton.setOnClickListener { guidance.guideNearby(place) }
        binding.cancelGuidanceButton.setOnClickListener { showIdle() }
    }

    override fun onDestroy() {
        guidance.release()
        super.onDestroy()
    }

    private val guidanceCallbacks = object : Guidance.Callbacks {
        override fun onChecking() = showStatus(getString(R.string.checking_screen), decision = false, nearby = false)

        override fun onHandingOff() = showStatus(getString(R.string.handing_off), decision = false, nearby = false)

        override fun onNotGuidable() = showStatus(getString(R.string.nearby_question), decision = true, nearby = true)

        override fun onUnavailable() = showStatus(getString(R.string.navi_unavailable), decision = true, nearby = false)
    }

    private fun showStatus(message: String, decision: Boolean, nearby: Boolean) {
        binding.playlistPanel.visibility = View.GONE
        binding.guidanceStatus.text = message
        binding.guidancePanel.visibility = View.VISIBLE
        binding.guidanceChoices.visibility = if (decision) View.VISIBLE else View.GONE
        binding.nearbyButton.visibility = if (nearby) View.VISIBLE else View.GONE
        binding.startButton.isEnabled = false
    }

    private fun showPlaylists() {
        binding.playlistTitles.removeAllViews()
        val titles = Places.playlistsFor(place.id)
        val rows = if (titles.isEmpty()) listOf(getString(R.string.no_playlist)) else titles
        rows.forEach { title ->
            val row = TextView(this).apply {
                text = title
                textSize = 22f
                setTextColor(Color.BLACK)
                setPadding(12, 18, 12, 18)
            }
            binding.playlistTitles.addView(
                row,
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                ),
            )
        }
        binding.playlistPanel.visibility = View.VISIBLE
    }

    private fun showIdle() {
        guidance.cancel()
        binding.guidancePanel.visibility = View.GONE
        binding.startButton.isEnabled = true
    }

    private fun leave() {
        guidance.cancel()
        if (intent.getBooleanExtra(EXTRA_FROM_MAP, false)) {
            startActivity(
                Intent(this, MapActivity::class.java).addFlags(
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP,
                ),
            )
        }
        finish()
    }

    private fun showPhoto(index: Int) {
        binding.photoPager.setCurrentItem(index, false)
        showCaption(index)
        binding.photoViewer.visibility = View.VISIBLE
    }

    private fun showCaption(index: Int) {
        val caption = samplePhotos[index].caption
        binding.photoCaption.text = caption
        binding.photoCaption.visibility = if (caption.isBlank()) View.INVISIBLE else View.VISIBLE
    }

    private fun hidePhoto() {
        binding.photoViewer.visibility = View.GONE
    }

    companion object {
        private const val EXTRA_ID = "place_id"
        private const val EXTRA_FROM_MAP = "from_map"
        private const val SAMPLE_AVERAGE_RATING = 4.6

        fun intent(context: Context, placeId: String, fromMap: Boolean = false): Intent =
            Intent(context, PlaceDetailActivity::class.java)
                .putExtra(EXTRA_ID, placeId)
                .putExtra(EXTRA_FROM_MAP, fromMap)
    }
}

private data class SamplePhoto(
    val color: Int,
    val likes: Int,
    val caption: String,
)

private val samplePhotos = listOf(
    SamplePhoto(0xFFE7EEF6.toInt(), 12, "야경이 좋았습니다"),
    SamplePhoto(0xFFF8EFE6.toInt(), 7, "노을이 길게 남았습니다"),
    SamplePhoto(0xFFF4F4F4.toInt(), 3, ""),
    SamplePhoto(0xFFEAF4EA.toInt(), 1, "바람이 선선했습니다"),
).sortedByDescending { it.likes }

private class PhotoStripAdapter(
    private val photos: List<SamplePhoto>,
    private val onPhotoClick: (Int) -> Unit,
) : RecyclerView.Adapter<PhotoStripAdapter.Holder>() {
    class Holder(val label: TextView) : RecyclerView.ViewHolder(label)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val density = parent.resources.displayMetrics.density
        val label = TextView(parent.context).apply {
            layoutParams = RecyclerView.LayoutParams((616 * density).toInt(), ViewGroup.LayoutParams.MATCH_PARENT)
                .apply { marginEnd = (16 * density).toInt() }
            gravity = Gravity.CENTER
            textSize = 22f
            setTextColor(Color.BLACK)
            isClickable = true
        }
        val holder = Holder(label)
        label.setOnClickListener {
            val index = holder.bindingAdapterPosition
            if (index != RecyclerView.NO_POSITION) onPhotoClick(index)
        }
        return holder
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val photo = photos[position]
        holder.label.text = holder.label.context.getString(R.string.photo_with_likes, position + 1, photo.likes)
        holder.label.setBackgroundColor(photo.color)
    }

    override fun getItemCount(): Int = photos.size
}

private class PhotoPagerAdapter(
    private val photos: List<SamplePhoto>,
    private val onTap: () -> Unit,
) : RecyclerView.Adapter<PhotoPagerAdapter.Holder>() {
    class Holder(val page: View) : RecyclerView.ViewHolder(page)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val page = View(parent.context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
            )
            isClickable = true
        }
        page.setOnClickListener { onTap() }
        return Holder(page)
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        holder.page.setBackgroundColor(photos[position].color)
    }

    override fun getItemCount(): Int = photos.size
}
