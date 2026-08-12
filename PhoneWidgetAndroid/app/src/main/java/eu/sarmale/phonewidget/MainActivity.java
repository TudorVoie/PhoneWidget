package eu.sarmale.phonewidget;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.garmin.android.connectiq.ConnectIQ;
import com.garmin.android.connectiq.IQDevice;
import com.garmin.android.connectiq.IQApp;
import com.garmin.android.connectiq.exception.InvalidStateException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private ConnectIQ connectIQ;
    private IQDevice pairedDevice;
    private boolean deviceConnected = false;
    private int lastSentPercent = -1;

    private final IQApp app = new IQApp("39223cfd-3dcc-4f6c-8e79-974ff07002de");
    private final ExecutorService ioExecutor = Executors.newSingleThreadExecutor();

    private TextView textViewStatus;

    // battery % change
    private final BroadcastReceiver batteryReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
            int scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);

            if (level == -1 || scale == -1) return;

            int pct = (int) (level * 100 / (float) scale);

            if (pct != lastSentPercent && deviceConnected) {
                lastSentPercent = pct;
                sendInfo();
            }

        }
    };

    ConnectIQ.ConnectIQListener listener = new ConnectIQ.ConnectIQListener() {
        @Override
        public void onSdkReady() {
            try {
                List<IQDevice> devices = connectIQ.getKnownDevices();
                if (devices.isEmpty()) {
                    runOnUiThread(() -> textViewStatus.setText("No devices"));
                    return;
                }

                pairedDevice = devices.get(0);

                // Live device connection status
                connectIQ.registerForDeviceEvents(pairedDevice, (device, status) -> {
                    runOnUiThread(() -> textViewStatus.setText("Device status: " + status));

                    boolean wasConnected = deviceConnected;
                    deviceConnected = (status == IQDevice.IQDeviceStatus.CONNECTED);

                    // Only push on the transition into CONNECTED, not every callback
                    if (deviceConnected && !wasConnected) {
                        sendInfo();
                    }
                });

                // Handle watch commands
                connectIQ.registerForAppEvents(pairedDevice, app, (iqDevice, iqApp, list, status) -> {
                    if (!list.isEmpty() && list.get(0) instanceof Map) {
                        Map<?, ?> msg = (Map<?, ?>) list.get(0);
                        if ("info".equals(msg.get("cmd"))) {
                            sendInfo();
                        } else if("silent".equals(msg.get("cmd"))) {
                            setSilent();
                        }
                    }
                });

            } catch (Exception e) {
                runOnUiThread(() -> textViewStatus.setText("Error: " + e.getClass().getSimpleName()));
            }
        }

        @Override
        public void onInitializeError(ConnectIQ.IQSdkErrorStatus iqSdkErrorStatus) {
            runOnUiThread(() -> textViewStatus.setText("Init error: " + iqSdkErrorStatus));
        }

        @Override
        public void onSdkShutDown() {
        }
    };

    private void setSilent() {
        NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        if (nm != null && !nm.isNotificationPolicyAccessGranted()) {
            runOnUiThread(() -> textViewStatus.setText("Need DND permission - opening settings"));
            Intent intent = new Intent(android.provider.Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS);
            startActivity(intent);
            return;
        }

        try {
            AudioManager am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
            am.setRingerMode(AudioManager.RINGER_MODE_SILENT);
            runOnUiThread(() -> textViewStatus.setText("Silenced"));
        } catch (SecurityException e) {
            runOnUiThread(() -> textViewStatus.setText("Silent failed: " + e.getMessage()));
        }
    }

    private int getCurrentBatteryPercent() {
        IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent batteryStatus = registerReceiver(null, ifilter);
        int level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
        return (int) (level * 100 / (float) scale);
    }

    private int isCharghing() {
        IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent batteryStatus = registerReceiver(null, ifilter);

        if (batteryStatus.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1) != 0) {
            return 1;
        }
        return 0;
    }

    int oldcharge = -1;

    private void sendInfo() {
        ioExecutor.execute(() -> {
            try {
                int pct = getCurrentBatteryPercent();

                Map<String, Object> payload = new HashMap<>();
                payload.put("battery", String.valueOf(pct));
                payload.put("name", Build.MODEL);
                int charge = isCharghing();
                if(charge != oldcharge) {
                    payload.put("charge", charge);
                    oldcharge = charge;
                }


                connectIQ.sendMessage(pairedDevice, app, payload,
                        (dev, a, msgStatus) ->
                                runOnUiThread(() ->
                                        textViewStatus.setText("Info sent: " + pct + "% / " + Build.MODEL + " (" + msgStatus + ")")));
            } catch (Exception e) {
                runOnUiThread(() -> textViewStatus.setText("Send failed: " + e.getClass().getSimpleName()));
            }
        });
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        textViewStatus = findViewById(R.id.textViewStatus);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        registerReceiver(batteryReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));

        try {
            // tethered - android simulator, wireless - real
            connectIQ = ConnectIQ.getInstance(this, ConnectIQ.IQConnectType.TETHERED);
            connectIQ.initialize(this, true, listener);
        } catch (Exception e) {
            textViewStatus.setText("Init failed: " + e.getClass().getSimpleName());
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            unregisterReceiver(batteryReceiver);
        } catch (Exception ignored) {}

        try {
            if (connectIQ != null) {
                connectIQ.unregisterAllForEvents();
                connectIQ.shutdown(this);
            }
        } catch (InvalidStateException ignored) {}

        ioExecutor.shutdown();
    }
}
