package com.example.criptografia;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

public class GerenciadorGiroscopio {

    private SensorManager sensorManager;
    private Sensor giroscopio;
    private onGiroDetectedListener listener;
    private boolean listening = false;


    public interface onGiroDetectedListener {
        void onGiroDetected();
    }


    public GerenciadorGiroscopio(Context context, onGiroDetectedListener listener) {
        this.listener = listener;
        sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        if (sensorManager != null) {
            giroscopio = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        }
    }


    private final SensorEventListener sensorEventListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {
            if (event.sensor.getType() == Sensor.TYPE_GYROSCOPE) {
                float speedGiroY = event.values[1];

               
                if (speedGiroY > 3.0f || speedGiroY < -3.0f) {

                    stopListener(); 


                    if (listener != null) {
                        listener.onGiroDetected();
                    }
                }
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {

        }
    };


    public void iniciarEscuta() {
        if (giroscopio != null && !listening) {
            sensorManager.registerListener(sensorEventListener, giroscopio, SensorManager.SENSOR_DELAY_NORMAL);
            listening = true;
        }
    }

    public void stopListener() {
        if (giroscopio != null && listening) {
            sensorManager.unregisterListener(sensorEventListener);
            listening = false;
        }
    }

    public boolean isSuportado() {
        return giroscopio != null;
    }
}