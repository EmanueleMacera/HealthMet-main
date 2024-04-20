package com.example.healthmet;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    private BluetoothAdapter bluetoothAdapter;
    public static BluetoothSocket socket;
    private ArrayAdapter<String> adapter;
    private ArrayList<String> devices;
    private ListView deviceList;
    private Button btnConnect;

    private static final int REQUEST_ENABLE_BT = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btnConnect = findViewById(R.id.btnConnect);
        deviceList = findViewById(R.id.deviceList);

        devices = new ArrayList<>();
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_single_choice, devices);
        deviceList.setAdapter(adapter);

        requestBluetoothPermission();

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            Toast.makeText(this, "Il tuo dispositivo non supporta Bluetooth", Toast.LENGTH_SHORT).show();
            finish();
        }

        Button btnFind = findViewById(R.id.btnFind);
        btnFind.setOnClickListener(v -> findDevices());

        deviceList.setOnItemClickListener((parent, view, position, id) -> {
            deviceList.setItemChecked(position, true);
            btnConnect.setEnabled(true);
        });

        btnConnect.setOnClickListener(v -> {
            if (deviceList.getCheckedItemCount() > 0) {
                String device = deviceList.getItemAtPosition(deviceList.getCheckedItemPosition()).toString();
                if (!device.contains("HC-06")) {
                    showDeviceNotSupportedToast();
                } else {
                    connectToDevice(device);
                }
            } else {
                Toast.makeText(MainActivity.this, "Seleziona un dispositivo prima di connetterti", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void requestBluetoothPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            String[] permissions = {
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.BLUETOOTH_CONNECT
            };
            ActivityCompat.requestPermissions(this, permissions, REQUEST_ENABLE_BT);
        } else {
            enableBluetooth();
        }
    }

    private void enableBluetooth() {
        if (!bluetoothAdapter.isEnabled()) {
            Intent enableBT = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            requestBluetooth.launch(enableBT);
        }
    }

    private void connectToDevice(String device) {
        String address = device.substring(device.length() - 17);
        BluetoothDevice bluetoothDevice = bluetoothAdapter.getRemoteDevice(address);
        if (bluetoothDevice.getName().equals("HC-06")) {
            UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"); // standard SPP UUID
            try {
                socket = bluetoothDevice.createRfcommSocketToServiceRecord(uuid);
                socket.connect();
                Toast.makeText(this, "Connessione al dispositivo riuscita", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(MainActivity.this, ArduinoActivity.class);
                startActivity(intent);
            } catch (IOException e) {
                Toast.makeText(this, "Connessione al dispositivo non riuscita", Toast.LENGTH_SHORT).show();
            }
        } else {
            showDeviceNotSupportedToast();
        }
    }

    private void showDeviceNotSupportedToast() {
        Toast toast = Toast.makeText(this, "Questo dispositivo non è supportato", Toast.LENGTH_SHORT);
        View toastView = toast.getView();
        toastView.setBackgroundColor(Color.RED);
        TextView toastMessage = toastView.findViewById(android.R.id.message);
        toastMessage.setTextColor(Color.WHITE);
        toast.show();
    }

    private void findDevices() {
        devices.clear();
        adapter.notifyDataSetChanged();
        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
        if (pairedDevices.size() > 0) {
            for (BluetoothDevice device : pairedDevices) {
                devices.add(device.getName() + "\n" + device.getAddress());
                adapter.notifyDataSetChanged();
            }
        }
    }

    private final ActivityResultLauncher<Intent> requestBluetooth = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
        if (result.getResultCode() == RESULT_OK) {
            Toast.makeText(this, "Richiesta Bluetooth abilitata", Toast.LENGTH_SHORT).show();
            findDevices();
        } else {
            Toast.makeText(this, "Richiesta Bluetooth negata", Toast.LENGTH_SHORT).show();
        }
    });

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_ENABLE_BT) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                enableBluetooth();
            } else {
                Toast.makeText(this, "Per abilitare il Bluetooth è necessario fornire i permessi", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
