package com.ling.screen;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

/**
 * Created by ling on 2017/5/25.
 */

public class CalibrateActivity extends Activity{
	private static final String TAG = "CalibrateActivity";
	Device myDevice;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_calibration);
		Log.i(TAG,"calibrate start");
		
		myDevice=Device.myDevice;
		Log.i(TAG,"This device is server: "+MainActivity.isServer);
		if(MainActivity.isServer){
			((ServerDevice)myDevice).printInfo();
		}
    }
}
