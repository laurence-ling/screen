package com.ling.screen;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.iraka.widget.Coordinate;
import com.iraka.widget.ScreenEvent;

import java.util.ArrayList;
import java.util.List;

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
		
		// myDevice network init
		myDevice=Device.myDevice;
		Log.i(TAG,"This device is server: "+MainActivity.isServer);
		myDevice.initCalibrationSocket();
		if(MainActivity.isServer){
			//((ServerDevice)myDevice).printInfo();
			//((ServerDevice)myDevice).receiveCalibrationData(this);
			((ServerDevice)myDevice).syncAllTime(this);
		}
		else{
			myDevice.respondTimeSync(this);
		}
		
		infoText=(TextView)findViewById(R.id.info_text);
		infoText.setText("Not Calibrated");
		
		mainFrame=(FrameLayout)findViewById(R.id.main_frame);
		mainFrame.setOnTouchListener(CalibrationTouchListener);
    }
	
    List<ScreenEvent> historyTouch=new ArrayList<>();
	View.OnTouchListener CalibrationTouchListener=new View.OnTouchListener(){
		@Override
		public boolean onTouch(View view,MotionEvent ev){
			Coordinate deviceCoord=new Coordinate(myDevice.posX,myDevice.posY,myDevice.angle);
			
			switch(ev.getAction()){
				case MotionEvent.ACTION_CANCEL:
				case MotionEvent.ACTION_UP:
					infoText.setText(myDevice.reportCalibrationMotionSequence(historyTouch,CalibrateActivity.this));
					drawMotionSequence(historyTouch);
					break;
				case MotionEvent.ACTION_DOWN: // Do not add DOWN to list in case same as move[0] (sometimes)
					historyTouch.clear();
					break;
				case MotionEvent.ACTION_MOVE:
					historyTouch.add(new ScreenEvent(ev,deviceCoord));
					break;
				default:
			}
			
			return true;
		}
	};
	
	private void drawMotionSequence(List<ScreenEvent> historyTouch){
		Bitmap bgBMP=Bitmap.createBitmap(mainFrame.getWidth(),mainFrame.getHeight(),Bitmap.Config.ARGB_8888);
		Canvas canvas=new Canvas(bgBMP);
		Paint paint=new Paint();
		Coordinate deviceCoord=new Coordinate(myDevice.posX,myDevice.posY,myDevice.angle);
		
		paint.setARGB(255,0,0,0);
		paint.setTextSize(24);
		for(int i=1;i<historyTouch.size();i++){
			ScreenEvent ev1=historyTouch.get(i);
			ScreenEvent ev2=historyTouch.get(i-1);
			Coordinate cd1=(new Coordinate(ev1.posX,ev1.posY)).toLocal(deviceCoord);
			Coordinate cd2=(new Coordinate(ev2.posX,ev2.posY)).toLocal(deviceCoord);
			canvas.drawLine((float)cd1.x,(float)cd1.y,(float)cd2.x,(float)cd2.y,paint);
			canvas.drawCircle((float)cd1.x,(float)cd1.y,10f,paint);
			if(i==1){
				canvas.drawCircle((float)cd2.x,(float)cd2.y,10f,paint);
			}
			
			//double vX=Math.round((ev1.posX-ev2.posX)/(ev1.timestamp-ev2.timestamp)*1000)/1000.;
			//double vY=Math.round((ev1.posY-ev2.posY)/(ev1.timestamp-ev2.timestamp)*1000)/1000.;
			//canvas.drawText("("+vX+","+vY+")",(float)cd1.x+10,(float)cd1.y+10,paint);
		}
		mainFrame.setBackground(new BitmapDrawable(CalibrateActivity.this.getResources(),bgBMP));
	}
	
	public Handler handler=new Handler(){
		@Override
		public void handleMessage(Message msg){
			if(msg.what==0){ // Calibration Successful
				//infoText.setText("Success");
				infoText.setBackgroundColor(Color.parseColor("#00FFC0"));
			}
			
			if(msg.what==1){ // Calibration Failed
			}
			
			if(msg.what==2){ // Time Synchronized
				infoText.setBackgroundColor(Color.parseColor("#0080FF"));
			}
			
			if(msg.what==3){ // Calibration Complete
				infoText.setBackgroundColor(Color.parseColor("#FF8000"));
				infoText.setText(new Coordinate(myDevice.posX,myDevice.posY,myDevice.angle).toString());
				Intent wkIntent = new Intent(CalibrateActivity.this, WorkingActivity.class);
				CalibrateActivity.this.startActivity(wkIntent);
			}
		}
	};
}
