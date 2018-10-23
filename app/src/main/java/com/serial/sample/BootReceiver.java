package com.serial.sample;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.SystemClock;
import android.util.Log;

import cn.pda.serialport.SerialPort;

import static android.content.Context.MODE_PRIVATE;

public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Log.i("Huang, BootReceiver", "onReceive: " + intent.getAction() );
        try {
            SerialPort serialPort = new SerialPort();
            serialPort.setGPIOlow(141);
            Thread.sleep(100);
            serialPort.setGPIOhigh(141);
            context.getSharedPreferences("ReBoot", MODE_PRIVATE).edit().putBoolean("isReBoot", true);
            context.getSharedPreferences("4g", MODE_PRIVATE).edit().putBoolean("open", false).apply();
            context.getSharedPreferences("4g", MODE_PRIVATE).edit().putBoolean("bound", false).apply();
            SharedPreferences sp = context.getSharedPreferences("com.serial_preferences", MODE_PRIVATE);
            sp.edit().putString("DEVICE", "").apply();
            context.getSharedPreferences("com.serial_preferences", MODE_PRIVATE).edit().putString("BAUDRATE", "").apply();
            context.getSharedPreferences("com.serial_preferences", MODE_PRIVATE).edit().putBoolean("4GSWITCH", false).apply();
        }catch (Exception e){
            e.printStackTrace();
        }
        Log.i("Huang, BootReceiver", "set completed！" );
    }
}
