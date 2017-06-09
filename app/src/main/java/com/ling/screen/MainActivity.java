package com.ling.screen;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Point;
import android.support.v7.app.AlertDialog;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

public class MainActivity extends Activity {
    private static final String TAG = "MainActivity";
    
    public static boolean isServer;
    Button newGpBtn;
    Button findGpBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        init();
    }

    public void init() {
        newGpBtn = (Button) findViewById(R.id.newGroupButton);
        findGpBtn = (Button) findViewById(R.id.findGroupButton);
        newGpBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isServer = true;
                Intent newGroup = new Intent(MainActivity.this, NewGroupActivity.class);
                //newGroup.putExtra("device", myDevice);
                MainActivity.this.startActivity(newGroup);
            }
        });
        findGpBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isServer = false;
                Intent addGroup = new Intent(MainActivity.this, AddGroupActivity.class);
                //addGroup.putExtra("device", myDevice);
                MainActivity.this.startActivity(addGroup);
            }
        });
        
        setScreenParams();
    }
    
    private void setScreenParams(){
        Point point = new Point();
        getWindowManager().getDefaultDisplay().getRealSize(point);
    
        Device.resX=point.x;
        Device.resY=point.y;
        
        DisplayMetrics dm=getResources().getDisplayMetrics();
        double x=point.x/dm.xdpi;
        double y=point.y/dm.ydpi;
        
        // Portrait ? Horizontal ?
        Log.i(TAG,"dpi-x = "+dm.xdpi+" dpi-y = "+dm.ydpi);
        
        Device.scr_width=x*25.4;
        Device.scr_height=y*25.4;
        Device.ppmX=dm.xdpi/25.4;
        Device.ppmY=dm.ydpi/25.4;
        
        Device.printScreenInfo();
    
    }
}