package com.serial.sample;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import com.serial.SerialPort;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author huang
 */
public class ReceiveService extends Service {
    protected boolean open;
    private ReadThread mReadThread;

    public ReceiveService() {
        Log.i("Huang, ReceiveService", "onCreate");
    }

    public class MyBinder extends Binder {
        ReceiveService getService() {
            return ReceiveService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.i("Huang, ReceiveService", "onBind");
        mApplication = (Application) getApplication();
        open = getSharedPreferences("4g", MODE_PRIVATE).getBoolean("open",
                false);
        if (open) {
            try {
                mSerialPort = mApplication.getSerialPort();
                mOutputStream = mSerialPort.getOutputStream();
                mInputStream = mSerialPort.getInputStream();
                /* Create a receiving thread */
                if (open && mInputStream != null) {
                    mReadThread = new ReadThread();
                    mReadThread.start();
                }
                getSharedPreferences("4g", MODE_PRIVATE).edit().putBoolean("bound", true).apply();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return new MyBinder();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i("Huang, ReceiveService", "onStartCommand");
        mApplication = (Application) getApplication();
        open = getSharedPreferences("4g", MODE_PRIVATE).getBoolean("open",
                false);
        boolean bound = getSharedPreferences("4g", MODE_PRIVATE).getBoolean("bound", false);
//        if (!bound && open) {
        try {
            mSerialPort = mApplication.getSerialPort();
            mOutputStream = mSerialPort.getOutputStream();
            mInputStream = mSerialPort.getInputStream();
            /* Create a receiving thread */
            if (open && mInputStream != null) {
                mReadThread = new ReadThread();
                mReadThread.start();
            }
            getSharedPreferences("4g", MODE_PRIVATE).edit().putBoolean("bound", true).apply();
            Intent intent1 = new Intent();
            intent1.setAction("com.android.huang.bound");
            sendBroadcast(intent);
        } catch (Exception e) {
            e.printStackTrace();
        }
//        }
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.i("Huang, ReceiveService", "onUnbind");
        if (mReadThread != null) {
            mReadThread.interrupt();
            mApplication.closeSerialPort();
        }
        getSharedPreferences("4g", MODE_PRIVATE).edit().putBoolean("bound", false).apply();
        return super.onUnbind(intent);
    }

    @Override
    public void onDestroy() {
        Log.i("Huang, ReceiveService", "onDestroy");
        if (mReadThread != null) {
            mReadThread.interrupt();
            mApplication.closeSerialPort();
        }
        getSharedPreferences("4g", MODE_PRIVATE).edit().putBoolean("bound", false).apply();
        super.onDestroy();
    }

    static String mRec;
    protected Application mApplication;
    protected SerialPort mSerialPort;
    public static FileOutputStream mOutputStream;
    protected FileInputStream mInputStream;

    private class ReadThread extends Thread {

        @Override
        public void run() {
            super.run();
            while (!isInterrupted()) {
                int size;
                try {
                    if (mInputStream.available() > 0) {
                        byte[] buffer = new byte[64];
                        size = mInputStream.read(buffer);
                        if (size > 0) {
                            onDataReceived(buffer, size);
                        }
                    }
                    Thread.sleep(10);
                } catch (Exception e) {
                    e.printStackTrace();
                    return;
                }
            }
        }
    }

    protected void onDataReceived(final byte[] buffer, final int size) {
        String rec = new String(buffer, 0, size);
        mRec = mRec + rec + "\n";
        getSharedPreferences("4g", MODE_PRIVATE).edit().putString("mRec", mRec).apply();
        Log.i("Huang, ReceiveService", "rec = " + rec);
        Intent intent = new Intent();
        intent.setAction("com.android.huang");
        intent.putExtra("mRec", rec);
//intent.addFlags(Intent.FLAG_RECEIVER_FOREGROUND);//前台广播（默认是后台广播）
        sendBroadcast(intent);
    }
}
