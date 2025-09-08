package com.example.wavetrack

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
//import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    private lateinit var tvStatus: TextView
    private lateinit var tvPath: TextView
    private lateinit var etInterval: EditText
    private lateinit var btnStart: Button
    private lateinit var btnStop: Button

    private val requiredPermissions = arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
    )

    private val requestPermLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { perms ->
            // no-op here; start/stop will check permissions again
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvStatus = findViewById(R.id.tvStatus)
        tvPath = findViewById(R.id.tvPath)
        etInterval = findViewById(R.id.etInterval)
        btnStart = findViewById(R.id.btnStart)
        btnStop = findViewById(R.id.btnStop)

        btnStart.setOnClickListener {
            if (!hasPermissions()) {
                requestPermissions()
                return@setOnClickListener
            }
            val intervalSeconds = etInterval.text.toString().toLongOrNull() ?: 5L
            val svc = Intent(this, DataCollectionService::class.java).apply {
                putExtra(DataCollectionService.EXTRA_INTERVAL_SECONDS, intervalSeconds)
                putExtra(DataCollectionService.EXTRA_FILENAME, "canopus_data.csv")
            }
            ContextCompat.startForegroundService(this, svc)
            tvStatus.text = "Status: Collecting"
            val path = getExternalFilesDir(null)?.absolutePath + "/wavetrack_data.csv"
            tvPath.text = "Save Location: $path"
        }

        btnStop.setOnClickListener {
            val svc = Intent(this, DataCollectionService::class.java)
            stopService(svc)
            tvStatus.text = "Status: Not Collecting"
        }

        // initial permission request if missing
        if (!hasPermissions()) requestPermissions()
    }

    private fun hasPermissions(): Boolean {
        for (p in requiredPermissions) {
            if (ContextCompat.checkSelfPermission(this, p) != PackageManager.PERMISSION_GRANTED) {
                return false
            }
        }
        return true
    }

    private fun requestPermissions() {
        requestPermLauncher.launch(requiredPermissions)
    }
}
