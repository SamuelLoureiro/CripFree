package com.example.criptografia;
import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.DialogInterface;
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

public class MainActivity extends AppCompatActivity {

    private Button button;
    private Executor executor;
    private BiometricPrompt biometricPrompt;

    private BiometricPrompt.PromptInfo promptInfo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        button  = findViewById(R.id.button);

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
                        Toast.makeText(getApplicationContext(),"Erro" + errString, Toast.LENGTH_SHORT).show();
                        break;
                }
            }

            @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
            public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result){
                super.onAuthenticationSucceeded(result);

                int authType= result.getAuthenticationType();

                if(authType == BiometricPrompt.AUTHENTICATION_RESULT_TYPE_DEVICE_CREDENTIAL){
                    Toast.makeText(getApplicationContext(), "Accès accordé par modèle/mot de passe", Toast.LENGTH_SHORT).show();
                }else {
                    Toast.makeText(getApplicationContext(), "Empreinte digitale vérifiée", Toast.LENGTH_SHORT).show();
                }
                notifyAccess();
            }

            @Override
            public void onAuthenticationFailed(){
                super.onAuthenticationFailed();
                Toast.makeText(getApplicationContext(),"Empreinte digitale non vérifiée", Toast.LENGTH_SHORT).show();
            }
        });

        promptInfo = new BiometricPrompt.PromptInfo.Builder()
                .setTitle("Cofre Digital")
                .setSubtitle("Use sua digital para abrir o cofre no PC")
                .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG | BiometricManager.Authenticators.DEVICE_CREDENTIAL)
                .build();

        button.setOnClickListener(v -> {
            biometricPrompt.authenticate(promptInfo);
        });



        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }
    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    private void notifyAccess(){
        BluetoothManager manager = getSystemService(BluetoothManager.class);
        BluetoothAdapter adapter = manager.getAdapter();
        GerenciadorBluetooth controller = new GerenciadorBluetooth();

        if(adapter==null){
            Toast.makeText(getApplicationContext(),"Adapter is null",Toast.LENGTH_LONG).show();
        }else if(!adapter.isEnabled()){
            Toast.makeText(getApplicationContext(),"Turn on your bluetooth",Toast.LENGTH_SHORT).show();

        }else{
           Set<BluetoothDevice> listaDispositivo = adapter.getBondedDevices();

           if(listaDispositivo.isEmpty()){
               Toast.makeText(getApplicationContext(),"Pair with your PC first",Toast.LENGTH_SHORT).show();
           }else if(listaDispositivo.size()==1){
               BluetoothDevice dispositive = listaDispositivo.iterator().next();
               Toast.makeText(getApplicationContext(),"Sending command to PC",Toast.LENGTH_SHORT).show();
               controller.connectNSend(dispositive,adapter,"DESBLOQUEAR");
           }else{
               List<BluetoothDevice> bluetoothDeviceList = new ArrayList<>(listaDispositivo);
               String[] listName = new String[bluetoothDeviceList.size()];
               for(int i=0;i<listaDispositivo.size();i++){
                    String name = bluetoothDeviceList.get(i).getName();
                    listName[i]= (name!=null)? name : bluetoothDeviceList.get(i).getAddress();
               }
               AlertDialog.Builder builder = new AlertDialog.Builder(this);
               builder.setTitle("Selecting PC");

               builder.setItems(listName, new DialogInterface.OnClickListener() {
                   @Override
                   public void onClick(DialogInterface dialog, int position) {

                       BluetoothDevice dispositivoEscolhido = bluetoothDeviceList.get(position);

                       Toast.makeText(MainActivity.this, "Conecting...", Toast.LENGTH_SHORT).show();

                       GerenciadorBluetooth meuGerenciador = new GerenciadorBluetooth();

                       meuGerenciador.connectNSend(dispositivoEscolhido, adapter, "DESBLOQUEAR");
                   }
               });

               AlertDialog popUp = builder.create();
               popUp.show();
           }

        }

    }
}