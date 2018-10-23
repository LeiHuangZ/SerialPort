package com.serial.sample;

import android.app.Service;
import android.content.Intent;
import android.net.Uri;
import android.os.Binder;
import android.os.Environment;
import android.os.IBinder;
import android.util.Log;

import com.serial.SerialPort;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class LogService extends Service {
    private LogReadThread mLogReadThread;
    private File mLogFile;
    private String mLogPath;

    public LogService() {
        Log.i("Huang, LogService", "onCreate");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i("Huang, LogService", "onStartCommand");
        String dirPath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/bdlog";
        File dirFile = new File(dirPath);
        if (!dirFile.exists()){
            boolean mkdir = dirFile.mkdir();
            Log.v("Huang, LogService", "Dir bdlog make success? = " + mkdir);
        }
        long l = System.currentTimeMillis();
        mLogPath = dirPath + "/bdlog-" + l + ".log";
        mLogFile = new File(mLogPath);
        if (!mLogFile.exists()){
            try {
                boolean newFile = mLogFile.createNewFile();
                Log.v("Huang, LogService", "bdlog.log make success? = " + newFile);
                notifySystemToScan(mLogPath);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        mApplication = (Application) getApplication();
        try {
            mLogPort = mApplication.getLogPort();
            mLogInputStream = mLogPort.getInputStream();
            /* Create a log receiving thread */
            mLogReadThread = new LogReadThread();
            mLogReadThread.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        Log.i("Huang, LogService", "onDestroy");
        if (mLogPort != null) {
            mLogReadThread.interrupt();
            mApplication.closeLogPort();
        }
        getSharedPreferences("4g", MODE_PRIVATE).edit().putBoolean("bound", false).apply();
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    protected Application mApplication;

    protected SerialPort mLogPort;
    protected FileInputStream mLogInputStream;

    private class LogReadThread extends Thread {

        @Override
        public void run() {
            super.run();
            while (!isInterrupted()) {
                int size;
                try {
                    if (mLogInputStream.available() > 0) {
                        byte[] buffer = new byte[64];
                        size = mLogInputStream.read(buffer);
                        if (size > 0) {
                            String rec = new String(buffer, 0, size);
                            Log.i("Huang, LogReadThread", "rec = " + rec);
                            saveLog(rec + "\n");
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

    private void saveLog(String str){
        try {
            FileOutputStream fos = new FileOutputStream(mLogFile, true);
            fos.write(str.getBytes("UTF-8"));
            fos.flush();
            fos.close();
            notifySystemToScan(mLogPath);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void notifySystemToScan(String filePath) {
        Intent intent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        File file = new File(filePath);

        Uri uri = Uri.fromFile(file);
        intent.setData(uri);
        this.getApplication().sendBroadcast(intent);
    }
}
