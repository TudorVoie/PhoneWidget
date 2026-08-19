package eu.sarmale.phonewidgetandroid

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.garmin.android.connectiq.ConnectIQ
import com.garmin.android.connectiq.IQApp
import com.garmin.android.connectiq.IQDevice
import com.garmin.android.connectiq.exception.InvalidStateException
import com.garmin.android.connectiq.exception.ServiceUnavailableException
import eu.sarmale.phonewidgetandroid.ui.theme.PhoneWidgetAndroidTheme

class MainActivity : ComponentActivity() {
    private lateinit var connectIQ: ConnectIQ
    private var isSdkReady = false

    // Must match the app id / uuid of the app running on the watch
    private val watchAppId = "f7b66f8b-12df-4e2a-8aab-e754bda5bd62"
    private lateinit var myApp: IQApp

    // Holds the latest message; Compose recomposes Text() whenever this changes
    private val lastMessage = mutableStateOf("Waiting for a message from the watch...")
    private var knownDevices: List<IQDevice> = emptyList()

    private var lastBattery = -1

    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            sendBattery(getBatteryPercentage(intent))
        }
    }

    private fun getBatteryPercentage(intent: Intent): Int {
        val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
        return (level * 100) / scale
    }

    private fun sendBattery(pct: Int) {
        if(pct < 0 || pct == lastBattery) return
        lastBattery = pct
        knownDevices.forEach { device ->
            try {
                connectIQ.sendMessage(device, myApp, pct) { _, _, status ->

                }
            } catch (_: InvalidStateException) {
            } catch (_: ServiceUnavailableException) {
            }
        }
    }

    private val connectIQListener: ConnectIQ.ConnectIQListener =
        object : ConnectIQ.ConnectIQListener {
            override fun onInitializeError(errStatus: ConnectIQ.IQSdkErrorStatus) {
                isSdkReady = false
                lastMessage.value = "SDK init error: ${errStatus.name}"
            }

            override fun onSdkReady() {
                isSdkReady = true
                loadDevices()

                sendBattery(67)
            }

            override fun onSdkShutDown() {
                isSdkReady = false
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        myApp = IQApp(watchAppId)

        setContent {
            PhoneWidgetAndroidTheme {
                Scaffold { padding ->
                    Box(modifier = Modifier.fillMaxSize().padding(padding)) {
                        MessageScreen(message = lastMessage.value)
                    }
                }
            }
        }

        setupConnectIQSdk()
    }

    override fun onResume() {
        super.onResume()
        if (isSdkReady) {
            loadDevices()
        }

        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        val stickyIntent = registerReceiver(batteryReceiver, filter)
        stickyIntent?.let { sendBattery(getBatteryPercentage(it)) }
    }

    override fun onPause() {
        super.onPause()
        try {
            knownDevices.forEach { device ->
                connectIQ.unregisterForApplicationEvents(device, myApp)
            }
        } catch (_: InvalidStateException) {
        }

        unregisterReceiver(batteryReceiver)
    }

    public override fun onDestroy() {
        super.onDestroy()
        releaseConnectIQSdk()
    }

    private fun releaseConnectIQSdk() {
        try {
            connectIQ.unregisterAllForEvents()
            connectIQ.shutdown(this)
        } catch (_: InvalidStateException) {
            // Usually just means it was already shut down.
        }
    }

    private fun setupConnectIQSdk() {
        connectIQ = ConnectIQ.getInstance(this, ConnectIQ.IQConnectType.WIRELESS)
        connectIQ.initialize(this, true, connectIQListener)
    }

    private fun loadDevices() {
        try {
            knownDevices = connectIQ.knownDevices ?: emptyList()
            knownDevices.forEach { device ->
                listenByMyAppEvents(device)
            }
        } catch (_: InvalidStateException) {
            Toast.makeText(this, "ConnectIQ is not in a valid state", Toast.LENGTH_SHORT).show()
        }
    }

    private fun listenByMyAppEvents(device: IQDevice) {
        try {
            connectIQ.registerForAppEvents(device, myApp) { _, _, message, _ ->
                runOnUiThread {
                    lastMessage.value = message.joinToString(", ")
                }
            }
        } catch (_: InvalidStateException) {
            Toast.makeText(this, "ConnectIQ is not in a valid state", Toast.LENGTH_SHORT).show()
        } catch (_: ServiceUnavailableException) {
            Toast.makeText(
                this,
                "ConnectIQ service is unavailable. Is Garmin Connect Mobile installed and running?",
                Toast.LENGTH_LONG
            ).show()
        }
    }
}

@Composable
fun MessageScreen(message: String) {
    Text(text = message, fontSize = 20.sp, modifier = Modifier.padding(16.dp))
}