package eu.sarmale.phonewidgetandroid

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import eu.sarmale.phonewidgetandroid.ui.theme.PhoneWidgetAndroidTheme

class MainActivity : ComponentActivity() {

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) {
            // Foreground service still runs even if this is denied; the
            // notification just won't be visible on Android 13+.
            startMonitorService()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            PhoneWidgetAndroidTheme {
                Scaffold { padding ->
                    Box(modifier = Modifier.fillMaxSize().padding(padding)) {
                        MessageScreen()
                    }
                }
            }
        }

        ensureServiceRunning()
    }

    private fun ensureServiceRunning() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            startMonitorService()
        }
    }

    private fun startMonitorService() {
        val intent = Intent(this, PhoneMonitorService::class.java)
        ContextCompat.startForegroundService(this, intent)
    }
}

@Composable
fun MessageScreen() {
    val message by PhoneMonitorState.lastMessage.collectAsState()
    Text(text = message, fontSize = 20.sp, modifier = Modifier.padding(16.dp))
}