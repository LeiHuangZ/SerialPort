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

import cn.pda.serialport.SerialPort;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.SwitchPreference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceActivity;
import android.util.Log;
import android.view.KeyEvent;

import com.serial.SerialPortFinder;

import serial.sample.R;

/**
 * @author huang
 */
public class SerialPortPreferences extends PreferenceActivity {

	private Application mApplication;
	private SerialPortFinder mSerialPortFinder;

	private SerialPort mSerialPort;
	private boolean open;
	private SharedPreferences sp;
	boolean falg ;
	private boolean runFlag = true;
	String[] entries;
	ListPreference devices;
	String[] entryValues;

	private boolean mBound;
	/**
	 * Flag for log
	 */
	private final static boolean mLogFlag = false;

	/**
	 * GPIO number
	 * nb801 --> 80, nb501 --> 86, H711 --> 83
	 * */
	private final static int POWER_GPIO_NUM = 83;

	private class UpdateThread extends Thread{
		@Override
		public void run() {
			while (runFlag) {
//				mBound = getSharedPreferences("4g", MODE_PRIVATE).getBoolean("bound", false);
//				if (mBound){
//					continue;
//				}
				String[] entries = mSerialPortFinder.getAllDevices();
				String[] entryValues = mSerialPortFinder.getAllDevicesPath();
				updateDevices(entries, entryValues);
				for (String string : entryValues) {
					if (string.contains("/dev/ttyUSB0")|| string.contains("/dev/ttyUSB2")) {
						falg = false;
						runOnUiThread(new Runnable() {
							@Override
							public void run() {
								devices.setEnabled(true);
							}
						});
						SharedPreferences sp = getSharedPreferences("com.serial_preferences", MODE_PRIVATE);
						sp.edit().putString("DEVICE", "/dev/ttyUSB0").apply();
						if (mLogFlag) {
                        	startLogService();
						}
//						finish();
						break;
					}
//					else if (string.contains("/dev/ttyUSB2")){
//						/*
//						 * 如果是USB1端口，自动关闭开启
//						 */
//						mSerialPort.setGPIOlow(83);
//						try {
//							Thread.sleep(100);
//						} catch (InterruptedException e) {
//							e.printStackTrace();
//						}
//						mSerialPort.setGPIOhigh(141);
//						open = false;
//						falg = false;
//						devices.setSummary("");
//						devices.setValue("");
//						devices.setEnabled(false);
//						if (MainMenu.mIntent != null){
//							stopService(MainMenu.mIntent);
//						}
//
//						try {
//							Thread.sleep(1000);
//						} catch (InterruptedException e) {
//							e.printStackTrace();
//						}
//
//						mSerialPort.setGPIOhigh(83);
//						try {
//							Thread.sleep(100);
//						} catch (InterruptedException e) {
//							e.printStackTrace();
//						}
//						mSerialPort.setGPIOlow(141);
//						open = true;
//						falg =true;
//						devices.setEnabled(false);
//						devices.setSummary("");
//						devices.setValue("");
//						break;
//					}
				}
				try {
					Thread.sleep(500);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			super.run();
		}
	}

	private void updateDevices(String[] entrie,String[] entryValues){
		this.entries = null;
		this.entryValues = null;
		this.entries = entrie;
		this.entryValues = entryValues;
		devices.setEntries(entries);
		devices.setEntryValues(entryValues);
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		mApplication = (Application) getApplication();
		mSerialPortFinder = mApplication.mSerialPortFinder;

		mSerialPort = new SerialPort();
		mBound = getSharedPreferences("4g", MODE_PRIVATE).getBoolean("bound", false);

		sp = getSharedPreferences("4g", MODE_PRIVATE);
		open = sp.getBoolean("open", false);

		if (!mBound || ReceiveService.mOutputStream == null) {
			if (open) {
				new UpdateThread().start();
			}
		}

		addPreferencesFromResource(R.xml.serial_port_preferences);

		// Devices
		devices = (ListPreference)findPreference("DEVICE");
		devices.setEnabled(false);

		devices.setSummary(devices.getValue());
		devices.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {

			@Override
			public boolean onPreferenceClick(Preference preference) {
				String[] entries = mSerialPortFinder.getAllDevices();
				String[] entryValues = mSerialPortFinder.getAllDevicesPath();
				devices.setEntries(entries);
				devices.setEntryValues(entryValues);
				devices.setSummary(devices.getValue());
				return false;
			}
		});
		devices.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
			@Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
				preference.setSummary((String)newValue);
				return true;
			}
		});

		// Baud rates
//		final ListPreference baudrates = (ListPreference)findPreference("BAUDRATE");
//		baudrates.setSummary(baudrates.getValue());
//		baudrates.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
//			@Override
//            public boolean onPreferenceChange(Preference preference, Object newValue) {
//				preference.setSummary((String)newValue);
//				return true;
//			}
//		});

		SwitchPreference fourG = (SwitchPreference) findPreference("4GSWITCH");
		fourG.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {

			@Override
			public boolean onPreferenceChange(Preference preference, Object newValue) {
				String value = String.valueOf(newValue);
				Log.d("SerialPort", "fourG = " + value);
				if (preference.getKey().equals("4GSWITCH")) {
					getSharedPreferences("4g", MODE_PRIVATE).edit().putBoolean("isNeedUnbind", true).apply();
					if (value.equals("false")) {
						if (mLogFlag) {
							stopLogService();
						}
						mSerialPort.setGPIOlow(POWER_GPIO_NUM);
						try {
							Thread.sleep(100);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
						mSerialPort.setGPIOhigh(141);
						open = false;
						falg = false;
						devices.setSummary("");
						devices.setValue("");
						devices.setEnabled(false);
						if (MainMenu.mIntent != null){
							stopService(MainMenu.mIntent);
						}
						Intent intent = new Intent();
						intent.setAction("com.android.huang.stopservice");
						sendBroadcast(intent);
					}else{
						new UpdateThread().start();
						mSerialPort.setGPIOhigh(POWER_GPIO_NUM);
						try {
							Thread.sleep(100);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
						mSerialPort.setGPIOlow(141);
						open = true;
						falg =true;
						devices.setEnabled(false);
						devices.setSummary("");
						devices.setValue("");
					}
					sp.edit().putBoolean("open", open).apply();
				}
				return true;
			}
		});
		boolean isReBoot = getSharedPreferences("ReBoot", Context.MODE_PRIVATE).getBoolean("isReBoot", false);
		if (isReBoot){
			open = false;
			devices.setSummary("");
			devices.setValue("");
			devices.setEnabled(false);
			if (MainMenu.mIntent != null){
				stopService(MainMenu.mIntent);
			}
			getSharedPreferences("ReBoot", Context.MODE_PRIVATE).edit().putBoolean("isReBoot", false);
		}
	}

	@Override
	public boolean onKeyDown(int keyCode,KeyEvent event){
		if(keyCode==KeyEvent.KEYCODE_BACK && falg)

			return true;//不执行父类点击事件
		return super.onKeyDown(keyCode, event);//继续执行父类其他点击事件
	}

	@Override
	protected void onDestroy() {
		runFlag = false;
		super.onDestroy();
	}

	/**
	 * 开启日志打印线程
	 */
	private void startLogService(){
		Intent intent = new Intent(this, LogService.class);
		startService(intent);
	}

	/**
	 * 关闭日志打印线程
	 */
	private void stopLogService(){
		Intent intent = new Intent(this, LogService.class);
		stopService(intent);
	}
}
