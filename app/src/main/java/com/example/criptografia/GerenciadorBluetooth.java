package com.example.criptografia;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

@SuppressLint("MissingPermission")
public class GerenciadorBluetooth {
    private static final String TAG = "CofreDigital_BT";
    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private Handler handler;


    public void connectNSend(BluetoothDevice device, BluetoothAdapter adapter, String mensagem) {
        ConnectThread connectThread = new ConnectThread(device, adapter, mensagem);
        connectThread.start();
    }

    private interface MessageConstants{
        public static final int MESSAGE_READ=0;
        public static final int MESSAGE_WRITE=1;
        public static final int MESSAGE_TOAST=2;
    }

    private class ConnectThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final BluetoothAdapter bluetoothAdapter;
        private final String mensagemParaEnviar;

        public ConnectThread(BluetoothDevice device, BluetoothAdapter adapter, String mensagem) {
            BluetoothSocket tmp = null;
            bluetoothAdapter = adapter;
            mensagemParaEnviar = mensagem;

            try {
                tmp = device.createInsecureRfcommSocketToServiceRecord(MY_UUID);
            } catch (IOException e) {
                Log.e(TAG, "Socket's create() method failed", e);
            }
            mmSocket = tmp;
        }

        // ConnectThread.run()
        public void run() {
            bluetoothAdapter.cancelDiscovery();
            BluetoothSocket socketFinal = mmSocket;

            // LOG 1 — início da tentativa
            Log.i(TAG, ">>> TENTATIVA DE CONEXÃO INICIADA");
            Log.i(TAG, "Socket estado antes do connect: isConnected=" + mmSocket.isConnected());

            try {
                socketFinal.connect();
                Log.i(TAG, ">>> MÉTODO A OK");

            } catch (IOException connectException) {
                Log.e(TAG, ">>> MÉTODO A FALHOU: " + connectException.getMessage());

                try {
                    socketFinal = (BluetoothSocket) mmSocket.getRemoteDevice()
                            .getClass()
                            .getMethod("createRfcommSocket", new Class[]{int.class})
                            .invoke(mmSocket.getRemoteDevice(), 1);

                    socketFinal.connect();
                    Log.i(TAG, ">>> MÉTODO B OK");

                } catch (Exception fallbackException) {
                    Log.e(TAG, ">>> MÉTODO B FALHOU: " + fallbackException.getMessage());
                    try { mmSocket.close(); } catch (IOException e) {}
                    return;
                }
            }

            Log.i(TAG, "Socket após connect: isConnected=" + socketFinal.isConnected());

            ConnectedThread connectedThread = new ConnectedThread(socketFinal);
            connectedThread.start();

            Log.i(TAG, "Aguardando 800ms antes de escrever...");
            try {
                Thread.sleep(800);
            } catch (InterruptedException e) { e.printStackTrace(); }

            Log.i(TAG, ">>> ENVIANDO MENSAGEM: [" + mensagemParaEnviar + "]");
            Log.i(TAG, "Socket estado antes do write: isConnected=" + socketFinal.isConnected());

            try {
                Thread.sleep(800);
            } catch (InterruptedException e) { e.printStackTrace(); }

            if (!socketFinal.isConnected()) {
                Log.e(TAG, ">>> Socket fechado pelo servidor durante o sleep. Abortando.");
                try { socketFinal.close(); } catch (IOException e) {}
                return;
            }

            Log.i(TAG, ">>> ENVIANDO MENSAGEM: [" + mensagemParaEnviar + "]");
            connectedThread.write(mensagemParaEnviar.getBytes());


        }
    }
    private class ConnectedThread extends Thread{
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;
        private byte[] mmBuffer;

        public ConnectedThread(BluetoothSocket socket){
            mmSocket = socket;
            InputStream tmpIn= null;
            OutputStream tmpOut = null;

            try{
                tmpIn = socket.getInputStream();
            }catch (IOException e){
                Log.e(TAG, "Error occurred when creating input stream", e);
            }
            try{
                tmpOut = socket.getOutputStream();
            }catch(IOException e){
                Log.e(TAG, "Error occurred when creating output stream", e);
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run(){
            mmBuffer = new byte[1024];
            int numBytes;

            while(true){
                try{
                    numBytes = mmInStream.read(mmBuffer);
                    if(numBytes<0){
                        Log.i(TAG, "Read return -1, Input stream disconnected");
                        break;
                    }
                    if(handler!=null) {
                        Message readMsg = handler.obtainMessage(
                                MessageConstants.MESSAGE_READ, numBytes, -1,
                                mmBuffer
                        );
                        readMsg.sendToTarget();
                    }
                }catch(IOException e){
                    Log.d(TAG, "Input stream was disconnected", e);
                    break;

                }
            }
        }

        public void write(byte[] bytes) {
            Log.i(TAG, "write() chamado, tamanho=" + bytes.length + " bytes");
            Log.i(TAG, "mmOutStream é null? " + (mmOutStream == null));

            try {
                mmOutStream.write(bytes);
                Log.i(TAG, ">>> WRITE OK — bytes enviados com sucesso");
                mmOutStream.flush();
                // ...
            } catch (IOException e) {
                Log.e(TAG, ">>> WRITE FALHOU na linha " + e.getStackTrace()[0].getLineNumber(), e);
            } finally {
                try {
                    Thread.sleep(500);
                    mmSocket.close();
                    Log.i(TAG, "Socket fechado no finally");
                } catch (Exception e) {
                    Log.e(TAG, "Erro ao fechar socket", e);
                }
            }
        }
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "Could not close the connect socket", e);
            }
        }
    }
}
