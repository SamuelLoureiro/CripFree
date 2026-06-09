package com.example.criptografia;

import android.bluetooth.BluetoothSocket;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class BluetoothManager {
    private static final String TAG = "MY_APP_DEBUG_TAG";
    private Handler handler;

    private interface MessageConstants{
        public static final int MESSAGE_READ=0;
        public static final int MESSAGE_WRITE=1;
        public static final int MESSAGE_TOAST=2;
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
                    Message readMsg = handler.obtainMessage(
                            MessageConstants.MESSAGE_READ,numBytes, -1,
                            mmBuffer
                    );
                    readMsg.sendToTarget();
                }catch(IOException e){
                    Log.d(TAG, "Input stream was disconnected", e);
                    break;

                }
            }
        }

        public void write (byte[] bytes){
            try{
                mmOutStream.write(bytes);

                Message writtenMsg = handler.obtainMessage(
                        MessageConstants.MESSAGE_WRITE,-1,-1,mmBuffer
                );
                writtenMsg.sendToTarget();
            }catch (IOException e){
                Log.e(TAG, "Error occurred when sending data", e);

                Message writeErrorMsg =
                        handler.obtainMessage(MessageConstants.MESSAGE_TOAST);
                Bundle bundle = new Bundle();
                bundle.putString("toast",
                        "Couldn't send data to the other device");
                writeErrorMsg.setData(bundle);
                handler.sendMessage(writeErrorMsg);
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
