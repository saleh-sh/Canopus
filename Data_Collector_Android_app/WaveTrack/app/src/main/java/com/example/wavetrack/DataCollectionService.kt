package com.example.wavetrack

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.telephony.CellInfo
import android.telephony.CellInfoLte
import android.telephony.TelephonyManager
import android.util.Log
import androidx.annotation.RequiresPermission
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.*
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.*

class DataCollectionService : Service() {

    companion object {
        const val TAG = "DataCollectionSvc"
        const val CHANNEL_ID = "wavetrack_channel"
        const val NOTIF_ID = 12345
        const val EXTRA_INTERVAL_SECONDS = "interval_seconds"
        const val EXTRA_FILENAME = "filename"
    }

    private lateinit var fusedClient: FusedLocationProviderClient
    private lateinit var telephonyManager: TelephonyManager
    private var csvFile: File? = null
    private var csvWriter: FileWriter? = null
    private var intervalSeconds: Long = 5L
    private var lastWriteMs: Long = 0L

    override fun onCreate() {
        super.onCreate()
        fusedClient = LocationServices.getFusedLocationProviderClient(this)
        telephonyManager = getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intervalSeconds = intent?.getLongExtra(EXTRA_INTERVAL_SECONDS, 5L) ?: 5L
        val filename = intent?.getStringExtra(EXTRA_FILENAME) ?: "wavetrack_data.csv"

        createNotificationChannel()
        startForeground(NOTIF_ID, buildNotification("Collecting Data..."))

        // Prepare CSV file
        csvFile = File(getExternalFilesDir(null), filename)
        val isNew = !csvFile!!.exists()
        csvWriter = FileWriter(csvFile!!, true)
        if (isNew) {
            csvWriter!!.append("timestamp,latitude,longitude,cellId,rsrp,mcc,mnc,tac,source\n")
            csvWriter!!.flush()
        }

        // Create location request
        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            intervalSeconds * 1000
        )
            .setMinUpdateIntervalMillis(1000)
            .setMaxUpdateDelayMillis(intervalSeconds * 1000)
            .build()

        // Location callback defined here to have access to lastWriteMs safely
        val locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { loc ->
                    val now = System.currentTimeMillis()
                    if (now - lastWriteMs >= intervalSeconds * 1000) {
                        lastWriteMs = now
                        recordSample(loc.latitude, loc.longitude, loc.time)
                    } else {
                        // فاصله زمانی رعایت نشده، نمونه را رد می‌کنیم
                    }
                }
            }
        }

        try {
            fusedClient.requestLocationUpdates(locationRequest, locationCallback, mainLooper)
        } catch (ex: SecurityException) {
            Log.e(TAG, "Missing location permission", ex)
            stopSelf()
        }

        return START_STICKY
    }

    @RequiresPermission(Manifest.permission.ACCESS_FINE_LOCATION)
    private fun recordSample(lat: Double, lon: Double, timestampUtc: Long) {
        try {
            val cellList: List<CellInfo>? = telephonyManager.allCellInfo
            var chosenCellId = -1
            var chosenRsrp: Int? = null
            var chosenMcc: String? = null
            var chosenMnc: String? = null
            var chosenTac: Int? = null
            var source = "unknown"

            if (cellList != null) {
                for (ci in cellList) {
                    if (ci.isRegistered) {
                        when (ci) {
                            is CellInfoLte -> {
                                chosenCellId = ci.cellIdentity.ci
                                chosenRsrp = ci.cellSignalStrength.rsrp
                                chosenMcc = ci.cellIdentity.mccString
                                chosenMnc = ci.cellIdentity.mncString
                                chosenTac = ci.cellIdentity.tac
                                source = "LTE_registered"
                                break
                            }
                            else -> {
                                // handle other RAT types if needed
                            }
                        }
                    }
                }
                if (chosenCellId == -1) {
                    for (ci in cellList) {
                        if (ci is CellInfoLte) {
                            chosenCellId = ci.cellIdentity.ci
                            chosenRsrp = ci.cellSignalStrength.rsrp
                            chosenMcc = ci.cellIdentity.mccString
                            chosenMnc = ci.cellIdentity.mncString
                            chosenTac = ci.cellIdentity.tac
                            source = "LTE_any"
                            break
                        }
                    }
                }
            }

            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
            val ts = sdf.format(Date(timestampUtc))
            val row = "$ts,$lat,$lon,$chosenCellId,${chosenRsrp ?: ""},${chosenMcc ?: ""},${chosenMnc ?: ""},${chosenTac ?: ""},$source\n"
            csvWriter?.append(row)
            csvWriter?.flush()
            Log.d(TAG, "Wrote: $row")
        } catch (ex: Exception) {
            Log.e(TAG, "Error writing sample", ex)
        }
    }

    private fun buildNotification(content: String): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pending = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Wavetrack - Data Collecting")
            .setContentText(content)
            .setSmallIcon(android.R.drawable.ic_dialog_map)
            .setContentIntent(pending)
            .setOngoing(true)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val ch = NotificationChannel(CHANNEL_ID, "Wavetrack Data Collection", NotificationManager.IMPORTANCE_LOW)
            nm.createNotificationChannel(ch)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            fusedClient.removeLocationUpdates(object : LocationCallback() {}) // safe dummy callback
        } catch (_: SecurityException) { }
        try {
            csvWriter?.flush()
            csvWriter?.close()
        } catch (_: Exception) { }
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
