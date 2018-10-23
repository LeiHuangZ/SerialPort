/*
 * Copyright 2009 Cedric Priscal
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.serial.sample;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.serial.SerialPort;

import serial.sample.R;

/**
 * @author huang
 */
public class MainMenu extends Activity {
    ThreadFactory threadFactory = Executors.defaultThreadFactory();
    ThreadPoolExecutor mExecutorService = new ThreadPoolExecutor(3, 200, 0L, TimeUnit.MILLISECONDS,
            new LinkedBlockingQueue<Runnable>(1024), threadFactory, new ThreadPoolExecutor.AbortPolicy());

    private CharSequence[] mCommand = {"AT+SYSINFO", "AT+CGACT=0,1",
            "AT+CGDCONT=1,\"IP\"", "AT+CGACT=1,1", "at+cgpaddr", "at+cgpdns",
            "at+dhcpctrl=1", "AT+ NETCARDS=1"};
    TextView tvStatus;
    private Button buttonSend;
    String mRec;
    protected boolean open;
    private ProgressDialog mProgressDialog;
    Pattern p = Pattern.compile("OK", Pattern.CASE_INSENSITIVE);
    private final int mOkCount = 8;
    private boolean mIsHigh = false;
    public static Intent mIntent;
    private Intent mIntent1;

    private MyBroadCastReceiver mReceiver;
    private MyReceiver mReceiver2;

    private class MyBroadCastReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            buttonSend.setClickable(true);
        }
    }

    private class MyReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            Log.i("Huang, MainMenu", "onReceive: ");
            try {
                unbindService(sc);
            }catch (Exception e){
                e.printStackTrace();
            }
        }
    }


    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        initView();

        IntentFilter filter = new IntentFilter();
        filter.addAction("com.android.huang");
        mReceiver = new MyBroadCastReceiver();
        registerReceiver(mReceiver, filter);

        IntentFilter filter2 = new IntentFilter();
        filter2.addAction("com.android.huang.stopservice");
        mReceiver2 = new MyReceiver();
        registerReceiver(mReceiver2, filter2);
    }

    private void getGpioStat(){
        try {
            Process process = Runtime.getRuntime().exec("cat /sys/devices/virtual/misc/mtgpio/pin");
            InputStream inputStream = process.getInputStream();
            OutputStream outputStream = process.getOutputStream();
            DataInputStream dataInputStream = new DataInputStream(inputStream);
            DataOutputStream dataOutputStream = new DataOutputStream(outputStream);
            String s = null;
            while ((s=dataInputStream.readLine()) != null) {
                //Log.e("Huang, MainMenu", "readLine = " + s);
                if (s.startsWith(" 46")){
                    s = s.substring(s.indexOf(" 46"), s.indexOf(" 46") + 11);
                    mIsHigh = s.charAt(6) == '1' && s.charAt(7) == '1';
                    Log.e("Huang, MainMenu", "substring = " + s);
                }else if (s.startsWith("125")){
                    s = s.substring(s.indexOf("125"), s.indexOf("125") + 11);
                    Log.e("Huang, MainMenu", "substring = " + s);
                    mIsHigh = s.charAt(6) == '1' && s.charAt(7) == '1';
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        Log.e("Huang, MainMenu", "mIsHigh = " + mIsHigh);
    }

    private void initView(){
        tvStatus = findViewById(R.id.TvStatus);
        final Button buttonSetup = findViewById(R.id.ButtonSetup);
        buttonSetup.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivityForResult(new Intent(MainMenu.this,
                        SerialPortPreferences.class), 1);

            }
        });

        final Button buttonConsole = findViewById(R.id.ButtonConsole);
        buttonConsole.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainMenu.this, ConsoleActivity.class));
            }
        });

        buttonSend = findViewById(R.id.ButtonSend);
        buttonSend.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View arg0) {
                Log.e("Huang, MainMenu", "mBound = " + mBound);
                if (!mBound){
                    return;
                }
                showProgress();
                ReceiveService.mRec = "";
                getSharedPreferences("4g", MODE_PRIVATE).edit()
                        .putString("mRec", mRec).apply();
                mExecutorService.execute(new Runnable() {
                    @Override
                    public void run() {
                        int commandCount = 0;
                        for (CharSequence aMCommand : mCommand) {
                            char[] text = new char[aMCommand.length()];
                            for (int i = 0; i < aMCommand.length(); i++) {
                                text[i] = aMCommand.charAt(i);
                            }
                            try {
                                ReceiveService.mOutputStream.write((new String(text) + "\n")
                                        .getBytes());
                                Log.i("Huang, MainMenu", "send cmd = " + new String(text));
                                if (commandCount == 3){
                                    Thread.sleep(2000);
                                }else {
                                    Thread.sleep(900);
                                }
                                commandCount++;
                            } catch (IOException e) {
                                e.printStackTrace();
                                return;
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                        String s = getSharedPreferences("4g", MODE_PRIVATE).getString("mRec", "");
                        Log.e("Huang, MainMenu", "==========================\n" + s);
                        Matcher m = p.matcher(s);
                        int count = 0;
                        while (m.find()) {
                            count++;
                        }
                        Log.e("Huang, MainMenu", "OkCount = " + count);
                        final int finalCount = count;
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if (finalCount == mOkCount) {
                                    tvStatus.setText("连接成功");
                                    buttonSend.setClickable(true);
                                } else {
                                    tvStatus.setText("连接失败");
                                    buttonSend.setClickable(true);
                                }
                            }
                        });
                        mProgressDialog.dismiss();
                    }
                });
            }
        });

        final Button buttonQuit = findViewById(R.id.ButtonQuit);
        buttonQuit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MainMenu.this.finish();
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 1) {
            open = getSharedPreferences("4g", MODE_PRIVATE).getBoolean("open",
                    false);
            if (!open) {
                tvStatus.setText("设备未打开。");
            } else {
                tvStatus.setText("状态");
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void showProgress() {
        mProgressDialog = new ProgressDialog(MainMenu.this);
        mProgressDialog.setMessage("连接中....");
        mProgressDialog.setMax(100);
        mProgressDialog.show();
    }

    private ServiceConnection sc = new MyServiceConnection();
    private boolean mBound;

    private class MyServiceConnection implements ServiceConnection {

        @Override
        public void onServiceConnected(ComponentName name, IBinder binder) {
            Log.i("Huang, Connection", "onServiceConnected");
            mBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.i("Huang, Connection", "onServiceDisconnected");
            mBound = false;
        }

    }

    @Override
    protected void onStart() {
        getGpioStat();
        open = getSharedPreferences("4g", MODE_PRIVATE).getBoolean("open",
                false);
        mBound = getSharedPreferences("4g", MODE_PRIVATE).getBoolean("bound", false);
        Log.i("Huang, MainMenu", "mBound = " + mBound);
        Log.i("Huang, MainMenu", "open = " + open);
        if (!mBound && open) {
            boolean isNeedUnbind = getSharedPreferences("4g", MODE_PRIVATE).getBoolean("isNeedUnbind", false);
            if (isNeedUnbind && mIntent1 != null){
                try {
                    unbindService(sc);
                    mIntent1 = null;
                }catch (Exception e){
                    e.printStackTrace();
                }
            }
            mIntent1 = new Intent(MainMenu.this, ReceiveService.class);
            bindService(mIntent1, sc, Context.BIND_AUTO_CREATE);
//            mIntent = new Intent(MainMenu.this, ReceiveService.class);
//            startService(mIntent);
        }
        else if (ReceiveService.mOutputStream == null && open){
            mIntent1 = new Intent(MainMenu.this, ReceiveService.class);
            bindService(mIntent1, sc, Context.BIND_AUTO_CREATE);
        }
        super.onStart();
    }

    @Override
    protected void onDestroy() {
        mBound = getSharedPreferences("4g", MODE_PRIVATE).getBoolean("bound", false);
        if (mBound) {
            unbindService(sc);
        }
        unregisterReceiver(mReceiver);
        unregisterReceiver(mReceiver2);
        super.onDestroy();
    }
}
