package com.example.healthmet;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.location.Location;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.provider.ContactsContract;
import android.telephony.SmsManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.tasks.Task;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class ArduinoActivity extends AppCompatActivity {

    private static final int REQUEST_CODE_PERMISSIONS = 101;
    private static final String[] PERMISSIONS = {
            Manifest.permission.BLUETOOTH,
            Manifest.permission.SEND_SMS,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.READ_PHONE_STATE
    };

    private FusedLocationProviderClient fusedLocationClient;

    private static final int REQUEST_CONTACT = 1;
    private TextView selectedContactView;
    private String phoneNumber;

    private boolean send = false;
    private boolean toastShown = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_arduino);

        checkPermissions();
        checkAndRequestGPS();

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        HandlerThread handlerThread = new HandlerThread("ReadDataThread");
        handlerThread.start();
        Intent serviceIntent = new Intent(this, MyService.class);
        startService(serviceIntent);

        selectedContactView = findViewById(R.id.selectedContactTextView);
        Button selectContactButton = findViewById(R.id.selectContactButton);
        selectContactButton.setOnClickListener(view -> selectContact());

        SharedPreferences sharedPreferences = getPreferences(Context.MODE_PRIVATE);
        phoneNumber = sharedPreferences.getString("PHONE_NUMBER", null);
        if (phoneNumber != null) {
            selectedContactView.setText(phoneNumber);
        } else {
            selectedContactView.setText("Ricordati di selezionare il contatto");
        }

        final Handler handler = new Handler(handlerThread.getLooper());
        final Runnable runnable = () -> {
            readDataFromArduino();
            handler.postDelayed(this::readDataFromArduino, 1000);
        };
        handler.post(runnable);
    }

    private void checkPermissions() {
        List<String> unGrantedPermissions = new ArrayList<>();

        for (String permission : PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, permission)
                    != PackageManager.PERMISSION_GRANTED) {
                unGrantedPermissions.add(permission);
            }
        }
        if (!unGrantedPermissions.isEmpty()) {
            ActivityCompat.requestPermissions(this,
                    unGrantedPermissions.toArray(new String[0]),
                    REQUEST_CODE_PERMISSIONS);
        }
    }

    private void checkAndRequestGPS() {
        LocationRequest locationRequest = LocationRequest.create();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder().addLocationRequest(locationRequest);
        SettingsClient settingsClient = LocationServices.getSettingsClient(this);
        Task<LocationSettingsResponse> task = settingsClient.checkLocationSettings(builder.build());
        task.addOnSuccessListener(this, locationSettingsResponse -> {
            // GPS already enabled, nothing to do
        });
        task.addOnFailureListener(this, e -> {
            int statusCode = ((ApiException) e).getStatusCode();
            if (statusCode == LocationSettingsStatusCodes.RESOLUTION_REQUIRED) {
                try {
                    ResolvableApiException resolvable = (ResolvableApiException) e;
                    resolvable.startResolutionForResult(ArduinoActivity.this, REQUEST_CODE_PERMISSIONS);
                } catch (IntentSender.SendIntentException sendEx) {
                    // Unable to start resolution for GPS activation
                }
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            boolean allGranted = true;
            for (int grantResult : grantResults) {
                if (grantResult != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }

            if (!allGranted) {
                Toast.makeText(this, "Per utilizzare l'applicazione è necessario fornire i permessi",
                        Toast.LENGTH_LONG).show();
                new Handler().postDelayed(this::finish, 5000);
            }
        }
    }

    private void readDataFromArduino() {
        try {
            InputStream inputStream = MainActivity.socket.getInputStream();
            byte[] buffer = new byte[256];
            int bytes = inputStream.read(buffer);

            if (!send) {
                requestCurrentLocation();
                send = true;
            }

        } catch (IOException e) {
            if (!toastShown) {
                Toast.makeText(this, "Il bluetooth si è disconnesso",
                        Toast.LENGTH_LONG).show();
                toastShown = true;
                new Handler().postDelayed(() -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        finishAndRemoveTask();
                    }
                }, 5000);
            }
        }
    }

    private void requestCurrentLocation() {
        LocationRequest locationRequest = LocationRequest.create();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(10000);
        locationRequest.setFastestInterval(5000);
        locationRequest.setExpirationDuration(10000);

        LocationCallback locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) {
                    Toast.makeText(ArduinoActivity.this, "Unable to get current location", Toast.LENGTH_SHORT).show();
                    return;
                }
                for (Location location : locationResult.getLocations()) {
                    if (location != null) {
                        sendEmergencySms(location);
                        fusedLocationClient.removeLocationUpdates(this);
                        break;
                    }
                }
            }
        };

        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                .addLocationRequest(locationRequest);

        SettingsClient settingsClient = LocationServices.getSettingsClient(this);
        Task<LocationSettingsResponse> task = settingsClient.checkLocationSettings(builder.build());
        task.addOnSuccessListener(this, locationSettingsResponse -> fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null));
        task.addOnFailureListener(this, e -> {
            int statusCode = ((ApiException) e).getStatusCode();
            switch (statusCode) {
                case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                    // Location settings are not satisfied. Show the user a dialog to upgrade location settings
                    break;
                case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                    Toast.makeText(ArduinoActivity.this, "Location settings are inadequate", Toast.LENGTH_SHORT).show();
                    // Location settings are inadequate, and cannot be fixed here.
                    // Dialog not created
                    break;
            }
        });
    }

    private void sendEmergencySms(Location location) {
        Toast.makeText(this, "Sto provando a mandare il messaggio", Toast.LENGTH_SHORT).show();
        String message = "Aiuto! La mia posizione attuale è: latitudine " + location.getLatitude() + ", longitudine " + location.getLongitude();
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.SEND_SMS}, 0);
            return;
        }
        SmsManager smsManager = SmsManager.getDefault();
        smsManager.sendTextMessage(phoneNumber, null, message, null, null);
        Toast.makeText(this, "Messaggio inviato", Toast.LENGTH_SHORT).show();
    }

    private void selectContact() {
        Intent pickContactIntent = new Intent(Intent.ACTION_PICK, Uri.parse("content://contacts"));
        pickContactIntent.setType(ContactsContract.CommonDataKinds.Phone.CONTENT_TYPE);
        startActivityForResult(pickContactIntent, REQUEST_CONTACT);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CONTACT && resultCode == RESULT_OK) {
            Uri contactUri = data.getData();
            String[] projection = {ContactsContract.CommonDataKinds.Phone.NUMBER, ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME};
            Cursor cursor = getContentResolver().query(contactUri, projection, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                int numberIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER);
                phoneNumber = cursor.getString(numberIndex);
                int nameIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME);
                String selectedContact = cursor.getString(nameIndex);
                selectedContactView.setText(selectedContact);

                SharedPreferences sharedPreferences = getPreferences(Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putString("PHONE_NUMBER", phoneNumber);
                editor.apply();

                cursor.close();
            }
        }
    }
}
