package com.ling.screen;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import java.util.Timer;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.datouhou.TouchImageView;
import com.iraka.widget.Coordinate;
import com.iraka.widget.ScreenEvent;

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
		else{

		}
		Intent calibrate = new Intent(CalibrateActivity.this, WorkingActivity.class);
		startActivity(calibrate);
		/*infoText=(TextView)findViewById(R.id.info_text);
		infoText.setText("Not Calibrated");
		
		mainFrame=(FrameLayout)findViewById(R.id.main_frame);
		mainFrame.setOnTouchListener(new View.OnTouchListener(){
			@Override
			public boolean onTouch(View view,MotionEvent ev){
				//Log.i(TAG,"Touch: "+ev.toString());
				if(ev.getAction()==MotionEvent.ACTION_DOWN){
					Coordinate deviceCoord=new Coordinate(myDevice.posX,myDevice.posY,myDevice.angle);
					double x=ev.getRawX();
					double y=ev.getRawY();
					Coordinate globalCoord=(new Coordinate(x,y)).toGlobal(deviceCoord);
					Log.i(TAG,globalCoord.toString());
					
					ScreenEvent sev=new ScreenEvent(ev,deviceCoord);
					byte[] buffer=new byte[44];
					sev.writeEventBuffer(buffer,0);
					ScreenEvent sevDeco=new ScreenEvent(buffer,0);
					Log.i(TAG,sev.toString());
					Log.i(TAG,sevDeco.toString());
				}
				
				
				return true;
			}
		});*/
    }
}
