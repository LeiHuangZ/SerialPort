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

import java.io.File;
import java.io.IOException;
import java.security.InvalidParameterException;


import android.content.SharedPreferences;
import android.util.Log;

import com.serial.SerialPort;
import com.serial.SerialPortFinder;

/**
 * @author huang
 */
public class Application extends android.app.Application {

	public SerialPortFinder mSerialPortFinder = new SerialPortFinder();
	private SerialPort mSerialPort = null;
	private SerialPort mLogPort = null;

	public SerialPort getSerialPort() throws SecurityException, IOException, InvalidParameterException {
		if (mSerialPort == null) {
			/* Read com.serial port parameters */
			SharedPreferences sp = getSharedPreferences("com.serial_preferences", MODE_PRIVATE);
			String path = sp.getString("DEVICE", "");
//			int baudrate = Integer.decode(sp.getString("BAUDRATE", "-1"));
			int baudrate = 9600;

			/* Check parameters */
			if ( (path.length() == 0) || (baudrate == -1)) {
				//throw new InvalidParameterException();
			}

			/* Open the com.serial port */
			mSerialPort = new SerialPort(new File(path), baudrate, 0);
		}
		return mSerialPort;
	}

	public SerialPort getLogPort() {
		try {
			mLogPort = new SerialPort(new File("/dev/ttyUSB1"), 9600, 0);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return mLogPort;
	}

	@Override
	public void onTerminate() {
		Log.i("Huang, Application", "onTerminate()");
		super.onTerminate();
	}

	public void closeSerialPort() {
		if (mSerialPort != null) {
			mSerialPort.close();
			mSerialPort = null;
		}
	}

	public void closeLogPort(){
		if (mLogPort != null) {
			mLogPort.close();
			mLogPort = null;
		}
	}
}
