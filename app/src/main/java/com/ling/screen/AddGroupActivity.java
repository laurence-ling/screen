package com.ling.screen;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by ling on 2017/5/25.
 */

public class AddGroupActivity extends Activity{
    Device myDevice;
    LinearLayout serverBox;
    Map<String, InetSocketAddress> serverMap;

    @Override
	protected void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_add_group);
        myDevice = new ClientDevice();
        serverBox = (LinearLayout)findViewById(R.id.serverBox);
        serverMap = new HashMap<>();
        ((ClientDevice)myDevice).findServer(AddGroupActivity.this);
	}
	private void clearOutput(){
		for(int cnt = serverBox.getChildCount(); cnt > 0; cnt = serverBox.getChildCount()){
			serverBox.removeViewAt(cnt - 1);
		}
	}
	private void refreshServerList(){
        clearOutput();
		for(String name : serverMap.keySet()){
			String addr = serverMap.get(name).getAddress().toString();

            Button btn = new Button(this);
            btn.setText(name + "  " + addr);
            //btn.setTextColor(Color.LTGRAY);
            btn.setOnClickListener(new ConnectServerListener(serverMap.get(name)));
            serverBox.addView(btn);
		}
    }
    class ConnectServerListener implements View.OnClickListener{
        InetSocketAddress saddr;
        public ConnectServerListener(InetSocketAddress _addr){saddr = _addr;}
        @Override
        public void onClick(View v){
            ((ClientDevice)myDevice).connectServer(saddr);
        }
    }
    public Handler handler=new Handler(){
		@Override
		public void handleMessage(Message msg){
			if(msg.what == 1){ // find new group
                refreshServerList();
			}
			if(msg.what == 2){ // Connection successful
                myDevice.status = Device.CALIBRATE_STATUS;
                Intent calibrate = new Intent(AddGroupActivity.this, CalibrateActivity.class);
                AddGroupActivity.this.startActivity(calibrate);
			}
		}
	};
}
