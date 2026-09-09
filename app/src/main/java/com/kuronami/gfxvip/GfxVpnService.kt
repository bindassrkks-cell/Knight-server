package com.kuronami.gfxvip

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import androidx.core.app.NotificationCompat

class GfxVpnService : VpnService() {
    private var vpnInterface: ParcelFileDescriptor? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        createNotification()
        val isLowPing = intent?.getBooleanExtra("LOW_PING", false) ?: false

        try {
            val builder = Builder()
                .setSession("Kuronami VIP Shield")
                .addAddress("10.1.10.1", 24)
                .addDnsServer(if (isLowPing) "1.1.1.1" else "8.8.8.8")
                .addRoute("0.0.0.0", 0)

            vpnInterface?.close()
            vpnInterface = builder.establish()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return START_STICKY
    }

    override fun onDestroy() {
        try {
            vpnInterface?.close()
            vpnInterface = null
        } catch (e: Exception) {
            e.printStackTrace()
        }
        super.onDestroy()
    }

    private fun createNotification() {
        val channelId = "vpn_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val chan = NotificationChannel(channelId, "VIP Shield", NotificationManager.IMPORTANCE_LOW)
            getSystemService(NotificationManager::class.java)?.createNotificationChannel(chan)
        }
        val notif = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Kuronami VIP Active")
            .setContentText("DNS Routing & Update Blocker Running")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .build()
        startForeground(1001, notif)
    }
}
