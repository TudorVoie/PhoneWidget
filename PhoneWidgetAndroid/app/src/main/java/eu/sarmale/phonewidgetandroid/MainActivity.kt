package eu.sarmale.phonewidgetandroid

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) {

            startMonitorService()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        supportActionBar?.title = "Phone Widget"

        ensureServiceRunning()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.checksilencepermissions -> {
                checksilencepermissions()
                true
            }


            else -> super.onOptionsItemSelected(item)
        }
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

    private fun hasNotificationPolicyAccess(): Boolean {
        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager

        return notificationManager.isNotificationPolicyAccessGranted
    }

    private fun openNotificationPolicySettings() {
        val intent = Intent(
            android.provider.Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS
        )
        startActivity(intent)
    }

    private fun checksilencepermissions() {
        if (!hasNotificationPolicyAccess()) {
            val builder: AlertDialog.Builder = AlertDialog.Builder(this)
            builder.setMessage("Do you want to set Do Not Disturb permissions now?").setTitle("No Do Not Disturb Permissions")
                .setNegativeButton("No", null)
                .setPositiveButton("Yes") {_, _ ->
                    openNotificationPolicySettings()
                }

            val dialog: AlertDialog = builder.create()
            dialog.show()

            return
        } else {
            val builder: AlertDialog.Builder = AlertDialog.Builder(this)
            builder.setMessage("No further actions needed.").setTitle("Permissions are granted.")
                .setNeutralButton("OK", null)

            val dialog: AlertDialog = builder.create()
            dialog.show()
        }
    }
}

