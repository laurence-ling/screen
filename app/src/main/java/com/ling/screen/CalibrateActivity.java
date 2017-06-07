package com.ling.screen;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.iraka.widget.Coordinate;

/**
 * Created by ling on 2017/5/25.
 */

public class CalibrateActivity extends Activity{
	private static final String TAG = "CalibrateActivity";
	Device myDevice;
	FrameLayout mainFrame;
	TextView infoText;
	
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
		
		infoText=(TextView)findViewById(R.id.info_text);
		infoText.setText("Not Calibrated");
		
		mainFrame=(FrameLayout)findViewById(R.id.main_frame);
		mainFrame.setOnTouchListener(new View.OnTouchListener(){
			@Override
			public boolean onTouch(View view,MotionEvent ev){
				//Log.i(TAG,"Touch: "+ev.toString());
				if(ev.getAction()==MotionEvent.ACTION_DOWN){
					double x=ev.getX();
					double y=ev.getY();
					Coordinate deviceCoord=new Coordinate(myDevice.posX,myDevice.posY,myDevice.angle);
					Coordinate globalCoord=(new Coordinate(x,y)).toGlobal(deviceCoord);
					Log.i(TAG,globalCoord.toString());
				}
				return true;
			}
		});
    }
}
