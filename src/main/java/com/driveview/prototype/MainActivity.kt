package com.driveview.prototype

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.driveview.prototype.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.mapChoice.setOnClickListener {
            startActivity(Intent(this, MapActivity::class.java))
        }
        binding.categoryChoice.setOnClickListener {
            startActivity(Intent(this, CategoryActivity::class.java))
        }
        binding.previousButton.setOnClickListener { finish() }
        binding.leaveScenery.setOnClickListener {
            startActivity(Intent(this, ReviewConteActivity::class.java))
        }
    }
}
