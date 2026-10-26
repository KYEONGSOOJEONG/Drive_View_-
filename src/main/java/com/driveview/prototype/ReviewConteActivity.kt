package com.driveview.prototype

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.driveview.prototype.databinding.ActivityReviewConteBinding

class ReviewConteActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityReviewConteBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.scenes.inAnimation = null
        binding.scenes.outAnimation = null

        val beats = listOf(
            getString(R.string.conte_title_1) to getString(R.string.conte_note_1),
            getString(R.string.conte_title_2) to getString(R.string.conte_note_2),
            getString(R.string.conte_title_3) to getString(R.string.conte_note_3),
            getString(R.string.conte_title_4) to getString(R.string.conte_note_4),
            getString(R.string.conte_title_5) to getString(R.string.conte_note_5),
        )

        fun show(index: Int) {
            val (title, note) = beats[index]
            binding.scenePage.text = getString(R.string.conte_page, index + 1, beats.size)
            binding.sceneTitle.text = title
            binding.sceneNote.text = note
            binding.prevScene.isEnabled = index > 0
            binding.nextScene.isEnabled = index < beats.lastIndex
        }

        show(0)
        binding.prevScene.setOnClickListener {
            val index = binding.scenes.displayedChild
            if (index == 0) return@setOnClickListener
            binding.scenes.displayedChild = index - 1
            show(index - 1)
        }
        binding.nextScene.setOnClickListener {
            val index = binding.scenes.displayedChild
            if (index == beats.lastIndex) return@setOnClickListener
            binding.scenes.displayedChild = index + 1
            show(index + 1)
        }
        binding.closeConte.setOnClickListener { finish() }
    }
}
