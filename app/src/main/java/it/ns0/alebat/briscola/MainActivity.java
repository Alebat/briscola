package it.ns0.alebat.briscola;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnCheckedChanged;
import butterknife.OnClick;

public class MainActivity extends AppCompatActivity {

    @BindView(R.id.textview)
    TextView console;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
    }

    @OnClick(R.id.textview)
    public void onAction() {
        new MainBLE(this).scan(true);
        console.append("Yes\n");
        console.append(System.nanoTime() + "\n");
    }

    private boolean mConnected = false;
    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            Log.i(getClass().getSimpleName(), "Broadcast " + action);
            if (MainBLE.ACTION_GATT_CONNECTED.equals(action)) {
                mConnected = true;
//                updateConnectionState(R.string.connected);
                invalidateOptionsMenu();
            } else if (MainBLE.ACTION_GATT_DISCONNECTED.equals(action)) {
                mConnected = false;
//                updateConnectionState(R.string.disconnected);
                invalidateOptionsMenu();
//                clearUI();
            } else if (MainBLE.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                // Show all the supported services and characteristics on the
                // user interface.
//                displayGattServices(mBluetoothLeService.getSupportedGattServices());
            } else if (MainBLE.ACTION_DATA_AVAILABLE.equals(action)) {
//                displayData(intent.getStringExtra(BluetoothLeService.EXTRA_DATA));
            }
        }
    };
}
