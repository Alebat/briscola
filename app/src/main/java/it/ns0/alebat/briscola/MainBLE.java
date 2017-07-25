package it.ns0.alebat.briscola;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.util.Log;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static android.bluetooth.BluetoothAdapter.STATE_CONNECTED;
import static android.bluetooth.BluetoothAdapter.STATE_DISCONNECTED;

public class MainBLE {
    private static final int REQUEST_ENABLE_BT = 1;
    private static final long SCAN_PERIOD = 10000;
    public static final String ACTION_GATT_CONNECTED = "ACTION_GATT_CONNECTED";
    public static final String ACTION_GATT_DISCONNECTED = "ACTION_GATT_DISCONNECTED";
    public static final String ACTION_GATT_SERVICES_DISCOVERED = "ACTION_GATT_SERVICES_DISCOVERED";
    public static final String ACTION_DATA_AVAILABLE = "ACTION_DATA_AVAILABLE";
    public static final String BRISCOLA_EXTRA_DATA = "BRISCOLA_EXTRA_DATA";
    private static final UUID UUID_BRISCOLA = UUID.nameUUIDFromBytes("BriscolAppAleBat".getBytes());
    private static final UUID UUID_PLAYER_NAME = UUID.nameUUIDFromBytes("Bris_PLAYER_NAME".getBytes());
    private final String TAG = getClass().getSimpleName();
    BluetoothAdapter ada = BluetoothAdapter.getDefaultAdapter();
    BluetoothLeScanner scanner;
    Activity activity;
    Handler handler;
    private boolean scanning;

    public boolean isScanning() {
        return scanning;
    }

    public MainBLE(Activity a) {
        activity = a;
        handler = new Handler(a.getMainLooper());
        if (ada == null || !ada.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            a.startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            ada = BluetoothAdapter.getDefaultAdapter();
        }
        scanner = ada.getBluetoothLeScanner();
    }

    public void scan(boolean enable) {
        if (enable) {
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    scanner.stopScan(callback);
                    Log.d(TAG, "Scan stopped");
                }
            }, SCAN_PERIOD);
            scanning = true;
            // TODO Add filter
            List<ScanFilter> l = Collections.singletonList(new ScanFilter.Builder().build());
            ScanSettings.Builder s = new ScanSettings.Builder()
                    .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                    .setReportDelay(0);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                s       // TODO test this on M
                        .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
                        .setNumOfMatches(ScanSettings.MATCH_NUM_FEW_ADVERTISEMENT);
            }
            scanner.startScan(l, s.build(), callback);
            Log.d(TAG, "Scan started");
        } else {
            scanning = false;
            scanner.stopScan(callback);
        }
    }

    private int mConnectionState = STATE_DISCONNECTED;
    BluetoothGatt bluetoothGatt;

    ScanCallback callback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            Log.i(TAG, "Scan result: " + result.toString());
            BluetoothGattCallback btc = new BluetoothGattCallback() {
                @Override
                public void onConnectionStateChange(BluetoothGatt gatt, int status,
                                                    int newState) {
                    String intentAction;
                    if (newState == BluetoothProfile.STATE_CONNECTED) {
                        intentAction = ACTION_GATT_CONNECTED;
                        mConnectionState = STATE_CONNECTED;
                        broadcastUpdate(intentAction);
                        Log.i(TAG, "Connected to GATT server.");
                        Log.i(TAG, "Attempting to start service discovery:" +
                                gatt.discoverServices());

                    } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                        intentAction = ACTION_GATT_DISCONNECTED;
                        mConnectionState = STATE_DISCONNECTED;
                        Log.i(TAG, "Disconnected from GATT server.");
                        broadcastUpdate(intentAction);
                    }
                }

                @Override
                // New services discovered
                public void onServicesDiscovered(BluetoothGatt gatt, int status) {
                    if (status == BluetoothGatt.GATT_SUCCESS) {
                        broadcastUpdate(ACTION_GATT_SERVICES_DISCOVERED);
                    } else {
                        Log.w(TAG, "onServicesDiscovered received: " + status);
                    }
                }

                @Override
                // Result of a characteristic read operation
                public void onCharacteristicRead(BluetoothGatt gatt,
                                                 BluetoothGattCharacteristic characteristic,
                                                 int status) {
                    if (status == BluetoothGatt.GATT_SUCCESS) {
                        broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
                    }
                }
            };
            // Implicit LE (instead of BR/EDR)?
            // Connection triggered
            bluetoothGatt = result.getDevice().connectGatt(activity, true, btc);
        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            Log.e(getClass().getSimpleName(), "Wasted results!");
        }

        @Override
        public void onScanFailed(int errorCode) {
            switch (errorCode) {
                case SCAN_FAILED_ALREADY_STARTED:
                    break;
                case SCAN_FAILED_APPLICATION_REGISTRATION_FAILED:
                    break;
                case SCAN_FAILED_FEATURE_UNSUPPORTED:
                    break;
                case SCAN_FAILED_INTERNAL_ERROR:
                    break;
            }
        }
    };

    private void broadcastUpdate(final String action) {
        final Intent intent = new Intent(action);
        activity.sendBroadcast(intent);
    }

    private void broadcastUpdate(final String action,
                                 final BluetoothGattCharacteristic characteristic) {
        final Intent intent = new Intent(action);

        // This is special handling for the Heart Rate Measurement profile. Data
        // parsing is carried out as per profile specifications.
        if (UUID_PLAYER_NAME.equals(characteristic.getUuid())) {
            final String name = characteristic.getStringValue(0);
            Log.d(TAG, String.format("Received name: %s", name));
            intent.putExtra(BRISCOLA_EXTRA_DATA, name);
        } else {
            // For all other profiles, writes the data formatted in HEX.
            final byte[] data = characteristic.getValue();
            if (data != null && data.length > 0) {
                final StringBuilder stringBuilder = new StringBuilder(data.length);
                for(byte byteChar : data)
                    stringBuilder.append(String.format("%02X ", byteChar));
                intent.putExtra(BRISCOLA_EXTRA_DATA, new String(data) + "\n" +
                        stringBuilder.toString());
            }
        }
        activity.sendBroadcast(intent);
    }
}
