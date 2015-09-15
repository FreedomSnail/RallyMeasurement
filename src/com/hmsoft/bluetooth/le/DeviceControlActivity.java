/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hmsoft.bluetooth.le;

import javax.xml.transform.Templates;

import com.example.bluetooth.le.R;

import android.R.integer;
import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

//import java.util.concurrent.ScheduledExecutorService;
import java.lang.*;
/**
 * For a given BLE device, this Activity provides the user interface to connect, display data,
 * and display GATT services and characteristics supported by the device.  The Activity
 * communicates with {@code BluetoothLeService}, which in turn interacts with the
 * Bluetooth LE API.
 */
public class DeviceControlActivity extends Activity {
    private final static String TAG = DeviceControlActivity.class.getSimpleName();
    protected static final int TIME = 50; 	// 单位ms
    public static final String EXTRAS_DEVICE_NAME = "DEVICE_NAME";
    public static final String EXTRAS_DEVICE_ADDRESS = "DEVICE_ADDRESS";

	protected static final int TIP = 1;
	protected static final int POINT_JUMP = 10;
	protected static final int ACCELERTE = 2;
	protected static final int MAX_OIL = 100;
	protected static final int MIN_OIL = 0;
	
	private TextView mCurWeightVal;
    private TextView mPeakVal;
    private TextView m6S1Val;
    private TextView m6S2Val;
    private TextView m12SVal;
    private TextView mAmpVal;
    private TextView mOil;
    int[] lastX= new int[2];
    
    
    private String mDeviceName;
    private String mDeviceAddress;
    private BluetoothLeService mBluetoothLeService;
    private boolean mConnected = false;
    private int  RecvCurWeightValue;
    private int  RecvPeakWeightValue;
    
    private int  _6S1Value;
    private int  _6S2Value;
    private int  _12SValue;
    private int  AmpValue;
    private int  OilValue = 0;
    private int  continueTouchCnt = 0;
    
    EditText edtSend;
	ScrollView svResult;
	Button btnSend;
	Button btnWeightClear;
	Button btnPeakValClear;
	
    // Code to manage Service lifecycle.
    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
            if (!mBluetoothLeService.initialize()) {
                Log.e(TAG, "Unable to initialize Bluetooth");
                finish();
            }
            
            Log.e(TAG, "mBluetoothLeService is okay");
            // Automatically connects to the device upon successful start-up initialization.
            //mBluetoothLeService.connect(mDeviceAddress);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBluetoothLeService = null;
        }
    };

    // Handles various events fired by the Service.
    // ACTION_GATT_CONNECTED: connected to a GATT server.
    // ACTION_GATT_DISCONNECTED: disconnected from a GATT server.
    // ACTION_GATT_SERVICES_DISCOVERED: discovered GATT services.
    // ACTION_DATA_AVAILABLE: received data from the device.  This can be a result of read
    //                        or notification operations.
    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {  //连接成功
            	Log.e(TAG, "Only gatt, just wait");
            } else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) { //断开连接
                mConnected = false;
                invalidateOptionsMenu();
                btnSend.setEnabled(false);
                btnWeightClear.setEnabled(false);
        		btnPeakValClear.setEnabled(false);
                clearUI();
            }else if(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) //可以开始干活了
            {
            	mConnected = true;
            	mCurWeightVal.setText("");
            	ShowDialog();
            	btnSend.setEnabled(true);
            	btnWeightClear.setEnabled(true);
        		btnPeakValClear.setEnabled(true);
        		handler.postDelayed(runnable, TIME); //每隔1s执行 
            	Log.e(TAG, "In what we need");
            	invalidateOptionsMenu();
            }else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) { //收到数据
            	//Log.e(TAG, "RECV DATA");
            	//收到的长字符串可能被截断，因此，需要做截断补齐处理
            	//或者简单的将下位机发送过来的字符串认为的做精简,以此来保证每次收到的数据不会被截断
            	String data = intent.getStringExtra(BluetoothLeService.EXTRA_DATA);
            	
            	if (data != null) {
            		//String data =  "24024015036010000";// 6S电压x0.1V，6s电压x0.1V，总电流A，GAP值，当前重量g
            		//Log.i(TAG, data);
            		String show = data.substring(12, 17);
            		int temp = Integer.parseInt(show);
            		if((temp>=0)&&(temp<=100000)) {
            			RecvCurWeightValue = temp;
            			mCurWeightVal.setText(String.valueOf(RecvCurWeightValue));
            			if(RecvPeakWeightValue<RecvCurWeightValue) {
            				RecvPeakWeightValue = RecvCurWeightValue;	//更新峰值
            				mPeakVal.setText(String.valueOf(RecvPeakWeightValue));
            			}
            		}
            		show = data.substring(0, 3);
            		temp = Integer.parseInt(show);
            		if((temp>=0)&&(temp<=600)) {
            			_12SValue = temp;
            			if(_12SValue<10) {
            				m12SVal.setText("0"+getString(String.valueOf(_12SValue), ".", 0)+"V");
            			}else if(_12SValue<100) {
            				m12SVal.setText(getString(String.valueOf(_12SValue), ".", 1)+"V");
            			} else {
            				m12SVal.setText(getString(String.valueOf(_12SValue), ".", 2)+"V");
            			}
            		}
            		show = data.substring(3, 6);
            		temp = Integer.parseInt(show);
            		if((temp>=0)&&(temp<=300)) {
            			_6S2Value = temp;
            			if(_12SValue > _6S2Value) {
            				_6S1Value = _12SValue - _6S2Value;
            			}
            			if(_6S1Value<10) {
            				m6S1Val.setText("0"+getString(String.valueOf(_6S1Value), ".", 0)+"V");
            			}else if(_6S1Value<100) {
            				m6S1Val.setText(getString(String.valueOf(_6S1Value), ".", 1)+"V");
            			} else {
            				m6S1Val.setText(getString(String.valueOf(_6S1Value), ".", 2)+"V");
            			}
            			if(_6S2Value<10) {
            				m6S2Val.setText("0"+getString(String.valueOf(_6S2Value), ".", 0)+"V");
            			}else if(_6S2Value<100) {
            				m6S2Val.setText(getString(String.valueOf(_6S2Value), ".", 1)+"V");
            			} else {
            				m6S2Val.setText(getString(String.valueOf(_6S2Value), ".", 2)+"V");
            			}
            		}
            		show = data.substring(6, 9);
            		temp = Integer.parseInt(show);
            		if((temp>=0)&&(temp<=150)) {
            			AmpValue = temp;
            			mAmpVal.setText(String.valueOf(AmpValue)+"A");
            			
            		}
                }
            }
        }
    };

    private void clearUI() {
        mCurWeightVal.setText(R.string.no_data);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {                                        //初始化
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON, WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.gatt_services_characteristics);

        final Intent intent = getIntent();
        mDeviceName = intent.getStringExtra(EXTRAS_DEVICE_NAME);
        mDeviceAddress = intent.getStringExtra(EXTRAS_DEVICE_ADDRESS);

        // Sets up UI references.
        m6S1Val = (TextView) findViewById(R.id._6s1Voltage);
        m6S2Val = (TextView) findViewById(R.id._6s2Voltage);
        m12SVal = (TextView) findViewById(R.id._12sVoltage);
        mAmpVal = (TextView) findViewById(R.id.Amp);
        mOil    = (TextView) findViewById(R.id.oil);
        
        
        mCurWeightVal = (TextView) findViewById(R.id.data_value);
        mCurWeightVal.setOnTouchListener(MyOnTouchListener);
        mPeakVal = (TextView) findViewById(R.id.PeakPull);
        edtSend = (EditText) this.findViewById(R.id.edtSend);
        edtSend.setText("219");
        svResult = (ScrollView) this.findViewById(R.id.svResult);
        
        btnSend = (Button) this.findViewById(R.id.btnSend);
        btnWeightClear = (Button) this.findViewById(R.id.btnWeightClear);
        btnPeakValClear = (Button) this.findViewById(R.id.btnPeakValClear);
		btnSend.setOnClickListener(listener);
		btnWeightClear.setOnClickListener(listener);
		btnPeakValClear.setOnClickListener(listener);
		btnSend.setEnabled(false);
		btnWeightClear.setEnabled(false);
		btnPeakValClear.setEnabled(false);
        //getActionBar().setTitle(mDeviceName);
        getActionBar().setTitle("桨叶拉力测试仪");
        getActionBar().setDisplayHomeAsUpEnabled(true);
        Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
        Log.d(TAG, "Try to bindService=" + bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE));
        
        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
        
    }

    @Override
    protected void onResume() {
        super.onResume();
        /*registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
        if (mBluetoothLeService != null) {
            final boolean result = mBluetoothLeService.connect(mDeviceAddress);
            Log.d(TAG, "Connect request result=" + result);
        }*/
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mGattUpdateReceiver);
        unbindService(mServiceConnection);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //this.unregisterReceiver(mGattUpdateReceiver);
        //unbindService(mServiceConnection);
        if(mBluetoothLeService != null)
        {
        	mBluetoothLeService.close();
        	mBluetoothLeService = null;
        }
        Log.d(TAG, "We are in destroy");
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.gatt_services, menu);
        if (mConnected) {
            menu.findItem(R.id.menu_connect).setVisible(false);
            menu.findItem(R.id.menu_disconnect).setVisible(true);
        } else {
            menu.findItem(R.id.menu_connect).setVisible(true);
            menu.findItem(R.id.menu_disconnect).setVisible(false);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {                              //点击按钮
        switch(item.getItemId()) {
            case R.id.menu_connect:
                mBluetoothLeService.connect(mDeviceAddress);
                return true;
            case R.id.menu_disconnect:
                mBluetoothLeService.disconnect();
                return true;
            case android.R.id.home:
            	if(mConnected)
            	{
            		mBluetoothLeService.disconnect();
            		mConnected = false;
            	}
                onBackPressed();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }
    
    private void ShowDialog()
    {
    	Toast.makeText(this, "连接成功，现在可以正常通信！", Toast.LENGTH_SHORT).show();
    }
	private OnClickListener listener = new OnClickListener() {
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				switch(v.getId()) {
					case R.id.btnSend:
						if(!mConnected) return;
						
						if (edtSend.length() < 1) {
							Toast.makeText(DeviceControlActivity.this, "请输入要发送的内容", Toast.LENGTH_SHORT).show();
							return;
						}
						mBluetoothLeService.WriteValue("$G"+edtSend.getText().toString()+"\r\n");
						
						InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
						if(imm.isActive())
							imm.hideSoftInputFromWindow(edtSend.getWindowToken(), 0);
						break;
					case R.id.btnWeightClear:
						if(!mConnected) return;
						mCurWeightVal.setText("0");
						mBluetoothLeService.WriteValue("$RST---\r\n");//通知下位机去皮重
						break;
					case R.id.btnPeakValClear:
						if(!mConnected) return;
						RecvPeakWeightValue = 0;
						mPeakVal.setText("0");
						break;
					default:
					break;
				}
			}
		};
 // 按钮事件
//	class ClickEvent implements View.OnClickListener {
//		@Override
//		public void onClick(View v) {
//			
//			if (v == btnSend) {
//				if(!mConnected) return;
//				
//				if (edtSend.length() < 1) {
//					Toast.makeText(DeviceControlActivity.this, "请输入要发送的内容", Toast.LENGTH_SHORT).show();
//					return;
//				}
//				mBluetoothLeService.WriteValue(edtSend.getText().toString());
//				
//				InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
//				if(imm.isActive())
//					imm.hideSoftInputFromWindow(edtSend.getWindowToken(), 0);
//				//todo Send data
//			}
//
//		}
//
//	}
	
    private static IntentFilter makeGattUpdateIntentFilter() {                        //注册接收的事件
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
        intentFilter.addAction(BluetoothDevice.ACTION_UUID);
        return intentFilter;
    }
    public static boolean isNumeric(String str){
    	  for (int i = 0; i < str.length(); i++){
    	  //System.out.println(str.charAt(i));
    	  if (!Character.isDigit(str.charAt(i))){
    	    return false;
    	  }
    	 }
    	 return true;
    }
    public static StringBuilder getString(String s1,String s2,int l){

    	StringBuilder sb=new StringBuilder();
    	sb.append(s1).insert(l, s2); 
    	return sb;
    }
    private OnTouchListener MyOnTouchListener = new OnTouchListener() {
		
		@Override
		public boolean onTouch(View v, MotionEvent event) {
			// TODO Auto-generated method stub
			int pointerIndex = (event.getAction() & MotionEvent.ACTION_POINTER_INDEX_MASK) >> MotionEvent.ACTION_POINTER_INDEX_SHIFT;
			int action=event.getAction()&MotionEvent.ACTION_MASK; 
			switch(action) {
			  case MotionEvent.ACTION_DOWN:  	//第一点
		        	lastX[pointerIndex] = (int)event.getX(pointerIndex);
		        	break;  
		        case MotionEvent.ACTION_POINTER_DOWN:  //第二点
		        	break;  
		        case MotionEvent.ACTION_MOVE:  //移动
		        	int tempx = (int)event.getX(pointerIndex);
		        	
		        	continueTouchCnt++;
		        	if(tempx>lastX[pointerIndex]) {
		        		
		        			lastX[pointerIndex] = tempx;
		        			
		        			if(OilValue < MAX_OIL) {
			        			if(continueTouchCnt<POINT_JUMP) {
			        				OilValue++;
			        			} else {
			        				if(OilValue < (MAX_OIL-ACCELERTE)) {
			        					OilValue += ACCELERTE;
			        				} else {
			        					OilValue = MAX_OIL;
			        				}
			        			}
			        			mOil.setText(String.valueOf(OilValue));
//			        			if(mConnected) {
//			        				if(OilValue<10) {
//				        				mBluetoothLeService.WriteValue("$P00"+String.valueOf(OilValue)+"\r\n");//通知下位机输出PWM
//				        			} else if(OilValue<100) {
//				        				mBluetoothLeService.WriteValue("$P0"+String.valueOf(OilValue)+"\r\n");
//				        			} else {
//				        				mBluetoothLeService.WriteValue("$P"+String.valueOf(OilValue)+"\r\n");
//				        			}
//			        				Log.i(TAG, String.valueOf(OilValue));
//			        			}
			        		}
		        	} else if(tempx<lastX[pointerIndex]) {
		        			lastX[pointerIndex] = tempx; 
		        			if(OilValue > MIN_OIL) {
		        				if(continueTouchCnt<POINT_JUMP) {
		        					OilValue--;
		        				} else {
		        					if(OilValue > ACCELERTE) {
			        					OilValue -= ACCELERTE;
			        				} else {
			        					OilValue = MIN_OIL;
			        				}
		        				}
			        			mOil.setText(String.valueOf(OilValue));
//			        			if(mConnected) {
//			        				if(OilValue<10) {
//				        				mBluetoothLeService.WriteValue("$P00"+String.valueOf(OilValue)+"\r\n");//通知下位机输出PWM
//				        			} else if(OilValue<100) {
//				        				mBluetoothLeService.WriteValue("$P0"+String.valueOf(OilValue)+"\r\n");
//				        			} else {
//				        				mBluetoothLeService.WriteValue("$P"+String.valueOf(OilValue)+"\r\n");
//				        			}
//			        				Log.i(TAG, String.valueOf(OilValue));
//			        			}
			        		}
		        	
		        	}
		        	break; 
		        case MotionEvent.ACTION_POINTER_UP:  // 0,1其一离开
		        	break; 
		        case MotionEvent.ACTION_UP:  
		        	continueTouchCnt = 0;
		            break;  
		        default:
		        	break;
			}
			return true;
		}
	};
	Handler handler = new Handler();
	Runnable runnable = new Runnable() { 
		
		@Override  
        public void run() {  
            // handler自带方法实现定时器  
        	char[] buffer = new char[4];
            try {  
            	if(mConnected) {
					handler.postDelayed(this, TIME);  
            	}
            	//SendData('0');
            	if(mConnected) {
            		int temp = (int)((float)OilValue*1.2f);
    				if(temp<10) {
        				mBluetoothLeService.WriteValue("$P00"+String.valueOf(temp)+"\r\n");//通知下位机输出PWM
        			} else if(temp<100) {
        				mBluetoothLeService.WriteValue("$P0"+String.valueOf(temp)+"\r\n");
        			} else {
        				mBluetoothLeService.WriteValue("$P"+String.valueOf(temp)+"\r\n");
        			}
    			//	Log.i(TAG, String.valueOf(OilValue));
    			}
            } catch (Exception e) {  
                // TODO Auto-generated catch block  
                e.printStackTrace();  
                System.out.println("exception...");  
            }

        }  
        
    };
}
