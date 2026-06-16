package com.example.criptografia;
import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresPermission;
import androidx.appcompat.app.AlertDialog;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executor;

public class MainActivity extends AppCompatActivity implements GerenciadorGiroscopio.onGiroDetectedListener {

    private Button button;

    private Button rButton;
    private Executor executor;
    private BiometricPrompt biometricPrompt;

    private BiometricPrompt.PromptInfo promptInfo;

    private GerenciadorGiroscopio gerenciadorGiroscopio;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        gerenciadorGiroscopio = new GerenciadorGiroscopio(this,this);

        button  = findViewById(R.id.button);

        rButton = findViewById(R.id.rstBtn);

        executor= ContextCompat.getMainExecutor(this);

        biometricPrompt = new BiometricPrompt(MainActivity.this,
                executor, new BiometricPrompt.AuthenticationCallback() {

            @Override
            public void onAuthenticationError(int errorCode, @NonNull CharSequence errString){
                super.onAuthenticationError(errorCode,errString);

                switch (errorCode){
                    case BiometricPrompt.ERROR_USER_CANCELED:
                    case BiometricPrompt.ERROR_NEGATIVE_BUTTON:
                        Toast.makeText(getApplicationContext(),"Canceled",Toast.LENGTH_SHORT).show();
                        break;
                    case BiometricPrompt.ERROR_LOCKOUT:
                        Toast.makeText(getApplicationContext(),"Too many tries. Sensor blocked temporaly", Toast.LENGTH_SHORT).show();
                        break;
                    default:
                        Toast.makeText(getApplicationContext(),"Error" + errString, Toast.LENGTH_SHORT).show();
                        break;
                }
            }

            @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
            public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result){
                super.onAuthenticationSucceeded(result);

                int authType= result.getAuthenticationType();

                if(authType == BiometricPrompt.AUTHENTICATION_RESULT_TYPE_DEVICE_CREDENTIAL){
                    Toast.makeText(getApplicationContext(), "Granted access by model/password", Toast.LENGTH_SHORT).show();
                }else {
                    if (gerenciadorGiroscopio.isSuportado()) {
                        Toast.makeText(getApplicationContext(), "Fingerprint verified! Turn your cellphone.", Toast.LENGTH_LONG).show();
                        gerenciadorGiroscopio.iniciarEscuta();
                    } else {
                        Toast.makeText(getApplicationContext(), "Fingerprint verified.", Toast.LENGTH_SHORT).show();
                        notifyAccess();
                    }
                }
            }

            @Override
            public void onAuthenticationFailed(){
                super.onAuthenticationFailed();
                Toast.makeText(getApplicationContext(),"Could not verify the fingerprint\"", Toast.LENGTH_SHORT).show();
            }
        });

        promptInfo = new BiometricPrompt.PromptInfo.Builder()
                .setTitle("Cofre Digital")
                .setSubtitle("Use your fingerprint")
                .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG | BiometricManager.Authenticators.DEVICE_CREDENTIAL)
                .build();

        button.setOnClickListener(v -> {
            biometricPrompt.authenticate(promptInfo);
        });

        rButton.setOnClickListener(v -> {
            // Open the shared preferences and wipe them completely clean
            SharedPreferences prefs = getSharedPreferences("LockPrefs", MODE_PRIVATE);
            prefs.edit().clear().apply();
            Toast.makeText(getApplicationContext(), "PC Forgotten. You will see the menu next time.", Toast.LENGTH_LONG).show();
        });



        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }
    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    private void notifyAccess() {
        // Ask android to hand over control of bluetooth level devices
        BluetoothManager manager = getSystemService(BluetoothManager.class);
        // The actual bluetooth antenna (driver)
        BluetoothAdapter adapter = manager.getAdapter();
        // Controller (bluetooth logic)
        GerenciadorBluetooth controller = new GerenciadorBluetooth();

        if(adapter==null) { // Checks if there's a bluetooth antenna
            Toast.makeText(getApplicationContext(),"Adapter is null",Toast.LENGTH_LONG).show();
        } else if(!adapter.isEnabled()){ // Check if bluetooth is enabled
            Toast.makeText(getApplicationContext(),"Turn on your bluetooth",Toast.LENGTH_SHORT).show();

        } else {
            // Get paired devices list
            Set<BluetoothDevice> pairedDevices = adapter.getBondedDevices();

            if(pairedDevices.isEmpty()) {
                Toast.makeText(getApplicationContext(),"Pair with your PC first",Toast.LENGTH_SHORT).show();
                return;
            }

            SharedPreferences prefs = getSharedPreferences("LockPrefs", MODE_PRIVATE);
            String savedMac = prefs.getString("pc_mac_address", null);

            if (savedMac != null) {
                for (BluetoothDevice device : pairedDevices) {
                    if (device.getAddress().equals(savedMac)) {
                        Toast.makeText(getApplicationContext(),"Auto-connecting to PC...",Toast.LENGTH_SHORT).show();
                        controller.connectNSend(device, adapter, "UNLOCK");
                        return;
                    }
                }
            }
            // If no pc remembered
            List<BluetoothDevice> bluetoothDeviceList = new ArrayList<>(pairedDevices);
            String[] listName = new String[bluetoothDeviceList.size()];

            for(int i=0;i<pairedDevices.size();i++) {
                String name = bluetoothDeviceList.get(i).getName();
                listName[i]= (name!=null)? name : bluetoothDeviceList.get(i).getAddress();
            }

            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Select your PC (One-time Setup)");

            builder.setItems(listName, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int position) {
                    BluetoothDevice chosenDevice = bluetoothDeviceList.get(position);

                    // Save the chosen PC to permanent memory
                    SharedPreferences.Editor editor = prefs.edit();
                    editor.putString("pc_mac_address", chosenDevice.getAddress());
                    editor.apply();

                    Toast.makeText(MainActivity.this, "PC Saved! Connecting...", Toast.LENGTH_SHORT).show();
                    controller.connectNSend(chosenDevice, adapter, "UNLOCK");
                }
            });

            AlertDialog popUp = builder.create();
            popUp.show();
        }

    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    @Override
    public void onGiroDetected() {
        Toast.makeText(getApplicationContext(), "Moviment detected! Sending command to PC...", Toast.LENGTH_SHORT).show();
        notifyAccess(); // Aqui ele chama a sua função original de parear e enviar o sinal!
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (gerenciadorGiroscopio != null) {
            gerenciadorGiroscopio.stopListener();
        }
    }
}