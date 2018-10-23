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

import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;

import serial.sample.R;

import static com.serial.sample.ReceiveService.mRec;

public class ConsoleActivity extends Activity {
    ThreadFactory threadFactory = Executors.defaultThreadFactory();
    ThreadPoolExecutor mExecutorService = new ThreadPoolExecutor(3, 200, 0L, TimeUnit.MILLISECONDS,
            new LinkedBlockingQueue<Runnable>(1024), threadFactory, new ThreadPoolExecutor.AbortPolicy());

	private EditText mReception;
	private EditText mEmission;

	private CharSequence[] mCommand = {"AT+SYSINFO","AT+CGACT=0,1","AT+CGDCONT=1,\"IP\"","AT+CGACT=1,1","at+cgpaddr","at+cgpdns","at+dhcpctrl=1","AT+ NETCARDS=1"};

    private class MyBroadCastReceiver extends BroadcastReceiver{

        @Override
        public void onReceive(Context context, Intent intent) {
            String mRec = intent.getStringExtra("mRec");
            //Log.i("Huang,BroadCastReceiver", "mRec = " + mRec);
            mReception.append(mRec);

        }
    }

    private MyBroadCastReceiver mReceiver;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.console);
		String s = getSharedPreferences("4g", MODE_PRIVATE).getString("mRec", "");
//		setTitle("Loopback test");
		mReception = (EditText) findViewById(R.id.EditTextReception);
		mEmission = (EditText) findViewById(R.id.EditTextEmission);

        IntentFilter filter = new IntentFilter();
        filter.addAction("com.android.huang");
        mReceiver = new MyBroadCastReceiver();
        registerReceiver(mReceiver, filter);
//		Emission.setOnEditorActionListener(new OnEditorActionListener() {
//			public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
//				/*int i;
//				CharSequence t = v.getText();
//				char[] text = new char[t.length()];
//				for (i=0; i<t.length(); i++) {
//					text[i] = t.charAt(i);
//				}
//				try {
//					mOutputStream.write(new String(text).getBytes());
//					mOutputStream.write('\n');
//				} catch (IOException e) {
//					e.printStackTrace();
//				}*/
//				return false;
//			}
//		});
//		
		
		mReception.setText(s);
		
		
		Button send = (Button)findViewById(R.id.Send);
		send.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				mReception.setText("");
                mRec = "";
                getSharedPreferences("4g", MODE_PRIVATE).edit()
                        .putString("mRec", mRec).apply();
				int i;
				CharSequence t = mEmission.getText();

				char[] text = new char[t.length()];
				for (i=0; i<t.length(); i++) {
					text[i] = t.charAt(i);
				}
				try {
					ReceiveService.mOutputStream.write((new String(text) + "\n").getBytes());
//					mOutputStream.write('\r');
//					mOutputStream.write('\n');
				} catch (IOException e) {
					e.printStackTrace();
				}
				
			}
        });
		
		Button start = (Button)findViewById(R.id.Start);
		start.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View arg0) {
                mReception.setText("");
                mRec = "";
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
                        Log.i("Huang, ConsoleActivity", "==========================\n" + s);
                    }
                });
			}	
		});
		
		Button close = (Button)findViewById(R.id.Close);
		close.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
//				try {
//					mOutputStream.close();
//				} catch (IOException e) {
//					// TODO Auto-generated catch block
//					e.printStackTrace();
//				}
				mReception.setText("");
				mEmission.setText("");
			}
		});
	
	}

    @Override
    protected void onStop() {
        getSharedPreferences("4g", MODE_PRIVATE).edit().putBoolean("bound", false).apply();
        super.onStop();
    }

    @Override
    public void onBackPressed() {
        getSharedPreferences("4g", MODE_PRIVATE).edit().putBoolean("bound", true).apply();
        super.onBackPressed();
    }

    @Override
    protected void onDestroy() {
	    unregisterReceiver(mReceiver);
        super.onDestroy();
    }

    protected void onDataReceived(final byte[] buffer, final int size) {
				Log.d(" ", String.valueOf(size));

	}
	
}
