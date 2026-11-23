package eu.sarmale.phonewidgetandroid

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.net.wifi.WifiManager
import android.os.BatteryManager
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.garmin.android.connectiq.ConnectIQ
import com.garmin.android.connectiq.IQApp
import com.garmin.android.connectiq.IQDevice
import com.garmin.android.connectiq.exception.InvalidStateException
import com.garmin.android.connectiq.exception.ServiceUnavailableException

class PhoneMonitorService : Service() {

    companion object {
        private const val CHANNEL_ID = "phone_monitor_channel"
        private const val NOTIFICATION_ID = 1001

        // Must match the app id / uuid of the app running on the watch
        private const val WATCH_APP_ID = "f7b66f8b-12df-4e2a-8aab-e754bda5bd62"
        const val ACTION_REQUEST_WATCH_BATTERY = "eu.sarmale.phonewidgetandroid.REQUEST_WATCH_BATTERY"
    }

    private lateinit var connectIQ: ConnectIQ
    private lateinit var myApp: IQApp
    private var isSdkReady = false
    private var knownDevices: List<IQDevice> = emptyList()

    private var lastBattery = -1
    private var lastRingerMode = -1

    private val connectIQListener = object : ConnectIQ.ConnectIQListener {
        override fun onInitializeError(errStatus: ConnectIQ.IQSdkErrorStatus) {
            isSdkReady = false
        }

        override fun onSdkReady() {
            isSdkReady = true
            loadDevices()

            // Force a resend now that devices are actually known
            lastBattery = -1
            lastRingerMode = -1
            sendBattery(getCurrentBatteryPercentage())
            sendRingerMode()
        }

        override fun onSdkShutDown() {
            isSdkReady = false
        }
    }

    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            sendBattery(getBatteryPercentage(intent))
        }
    }

    private val ringerReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            sendRingerMode()
        }
    }

    override fun onCreate() {
        super.onCreate()
        myApp = IQApp(WATCH_APP_ID)

        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification())

        val batteryFilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        val stickyIntent = registerReceiver(batteryReceiver, batteryFilter)
        stickyIntent?.let { sendBattery(getBatteryPercentage(it)) }

        registerReceiver(ringerReceiver, IntentFilter(AudioManager.RINGER_MODE_CHANGED_ACTION))

        connectIQ = ConnectIQ.getInstance(this, ConnectIQ.IQConnectType.WIRELESS)
        connectIQ.initialize(this, true, connectIQListener)

        PhoneMonitorState.isServiceRunning.value = true
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_REQUEST_WATCH_BATTERY) {
            sendToWatch("getwatchbat")
        }
        return START_STICKY
    }

    private fun sendToWatch(text: String) {
        knownDevices.forEach { device ->
            try {
                connectIQ.sendMessage(device, myApp, text) { _, _, _ -> }
            } catch (_: InvalidStateException) {
            } catch (_: ServiceUnavailableException) {
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        try {
            unregisterReceiver(batteryReceiver)
        } catch (_: IllegalArgumentException) {
        }
        try {
            unregisterReceiver(ringerReceiver)
        } catch (_: IllegalArgumentException) {
        }
        try {
            connectIQ.unregisterAllForEvents()
            connectIQ.shutdown(this)
        } catch (_: InvalidStateException) {
        }
        PhoneMonitorState.isServiceRunning.value = false
    }

    private fun loadDevices() {
        try {
            knownDevices = connectIQ.knownDevices ?: emptyList()
            knownDevices.forEach { device ->
                listenByMyAppEvents(device)
            }
        } catch (_: InvalidStateException) {
        }
    }

    private fun listenByMyAppEvents(device: IQDevice) {
        try {
            connectIQ.registerForAppEvents(device, myApp) { _, _, message, _ ->
    val text = message.joinToString(", ")
    when {
        text == "silent" -> setRingerMode(0)
        text == "normal" -> setRingerMode(2)
        text == "sendinfo" -> resendPhoneInfo()
        text.startsWith("WATCHBAT:") -> {
            val pct = text.removePrefix("WATCHBAT:").toIntOrNull()
            if (pct != null) WatchBatteryWidget.save(applicationContext, pct)
            // Ceasul nu mai cere "sendinfo" la 5 minute, așa că retrimitem
            // starea telefonului la fiecare WATCHBAT
            resendPhoneInfo()
        }
    }
}
        } catch (_: InvalidStateException) {
        } catch (_: ServiceUnavailableException) {
        }
    }

    private fun resendPhoneInfo() {
        lastBattery = -1
        lastRingerMode = -1
        sendBattery(getCurrentBatteryPercentage())
        sendRingerMode()
    }

    private fun getBatteryPercentage(intent: Intent): Int {
        val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
        return if (level >= 0 && scale > 0) (level * 100) / scale else -1
    }

    private fun getCurrentBatteryPercentage(): Int {
        val bm = getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        return bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
    }

    private fun sendBattery(pct: Int) {
        if (pct < 0 || pct == lastBattery) return
        lastBattery = pct
        knownDevices.forEach { device ->
            try {
                connectIQ.sendMessage(device, myApp, "BATTERY:$pct") { _, _, _ -> }
            } catch (_: InvalidStateException) {
            } catch (_: ServiceUnavailableException) {
            }
        }
    }

    private fun getRingerMode(): Int {
        val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        return audioManager.ringerMode
    }

    private fun togglevolume() {
        val mode = getRingerMode()
        if(mode == AudioManager.RINGER_MODE_SILENT || mode == AudioManager.RINGER_MODE_VIBRATE)
        {
            val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
            audioManager.ringerMode = AudioManager.RINGER_MODE_NORMAL
        } else if(mode == AudioManager.RINGER_MODE_NORMAL)
        {
            val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
            audioManager.ringerMode = AudioManager.RINGER_MODE_SILENT
        }
    }

    private fun sendRingerMode() {
        val mode = getRingerMode()
        if (mode == lastRingerMode) return
        lastRingerMode = mode

        // Some OEM ROMs (e.g. Samsung) report VIBRATE for what the UI shows as
        // "Silent," so silent and vibrate are combined into one state here.
        val send = when (mode) {
            AudioManager.RINGER_MODE_SILENT, AudioManager.RINGER_MODE_VIBRATE -> "silent"
            AudioManager.RINGER_MODE_NORMAL -> "normal"
            else -> "unknown"
        }

        knownDevices.forEach { device ->
            try {
                connectIQ.sendMessage(device, myApp, "RINGER:$send") { _, _, _ -> }
                connectIQ.sendMessage(device, myApp, "BATTERY:${getCurrentBatteryPercentage()}") { _, _, _ -> }
            } catch (_: InvalidStateException) {
            } catch (_: ServiceUnavailableException) {
            }
        }
    }

    /*
    private fun silentringermode() {
        val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        audioManager.ringerMode = AudioManager.RINGER_MODE_SILENT
    }

    private fun normalringermode() {
        val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        audioManager.ringerMode = AudioManager.RINGER_MODE_NORMAL
    }
    */

    private fun setRingerMode(x : Int) {
        val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        audioManager.ringerMode = x
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Watch Sync",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Keeps your Garmin watch updated with phone status"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification {
        val openAppIntent = packageManager.getLaunchIntentForPackage(packageName)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, openAppIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Watch Sync Active")
            .setContentText("Sending battery and ringer updates to your watch")
            .setSmallIcon(android.R.drawable.stat_sys_data_bluetooth)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }
}