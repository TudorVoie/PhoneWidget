package eu.sarmale.phonewidget;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
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

    private final BroadcastReceiver batteryReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
            int scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
            if (level == -1 || scale == -1) return;

            int pct = (int) (level * 100 / (float) scale);

            if (pct != lastSentPercent && deviceConnected) {
                lastSentPercent = pct;
                sendBattery(pct);
            }
        }
    };

    ConnectIQ.ConnectIQListener listener = new ConnectIQ.ConnectIQListener() {
        @Override
        public void onSdkReady() {
            try {
                var devices = connectIQ.getKnownDevices();
                if (devices.isEmpty()) {
                    runOnUiThread(() -> textViewStatus.setText("No devices"));
                    return;
                }

                pairedDevice = devices.get(0);

                connectIQ.registerForDeviceEvents(pairedDevice, (device, status) -> {
                    runOnUiThread(() -> textViewStatus.setText("Device status: " + status));

                    deviceConnected = (status == IQDevice.IQDeviceStatus.CONNECTED);

                    // Send current battery immediately on (re)connect
                    if (deviceConnected) {
                        sendBattery(getCurrentBatteryPercent());
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

    private int getCurrentBatteryPercent() {
        IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent batteryStatus = registerReceiver(null, ifilter);
        int level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
        return (int) (level * 100 / (float) scale);
    }

    private void sendBattery(int pct) {
        ioExecutor.execute(() -> {
            try {
                Map<String, Object> payload = new HashMap<>();
                payload.put("battery", String.valueOf(pct));

                connectIQ.sendMessage(pairedDevice, app, payload,
                        (dev, a, msgStatus) ->
                                runOnUiThread(() -> textViewStatus.setText("Sent: " + pct + "% (" + msgStatus + ")")));
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