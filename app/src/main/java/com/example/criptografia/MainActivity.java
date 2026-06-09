package com.example.criptografia;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

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

            public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result){
                super.onAuthenticationSucceeded(result);

                int authType= result.getAuthenticationType();

                if(authType == BiometricPrompt.AUTHENTICATION_RESULT_TYPE_DEVICE_CREDENTIAL){
                    Toast.makeText(getApplicationContext(), "Accès accordé par modèle/mot de passe", Toast.LENGTH_SHORT).show();
                }else {
                    Toast.makeText(getApplicationContext(), "Empreinte digitale vérifiée", Toast.LENGTH_SHORT).show();
                }
                //notifyAcess();
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
    private void notifyAccess(){

    }
}