package com.ling.screen;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.*;
import android.widget.*;

/**
 * Created by ling on 2017/5/25.
 */

public class NewGroupActivity extends Activity {
    public static final String TAG = "NewGroupActivity";
    Device myDevice;
    LinearLayout memberBox;
	Button calibrateBtn;

	@Override
	protected void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_new_group);
        myDevice = new ServerDevice();
        memberBox = (LinearLayout)findViewById(R.id.memberBox);
        calibrateBtn = (Button)findViewById(R.id.calibrateButton);
        popCreateDialogWindow();
        calibrateBtn.setOnClickListener(new CalibrateBtnListener());
	}

    public void popCreateDialogWindow(){
        LayoutInflater layoutInflater=LayoutInflater.from(this);
        View popupView=layoutInflater.inflate(R.layout.create_group_popup,null);

        new AlertDialog.Builder(this)
        .setMessage("Enter your group name")
        .setView(popupView)
        .setPositiveButton("Create", new CreateBtnListener(popupView))
        .show();
    }

    class CreateBtnListener implements DialogInterface.OnClickListener{
        View popView;
        public CreateBtnListener(View v) {
            popView = v;
        }
        public void onClick(DialogInterface dialogInterface,int i){
            EditText nameText = (EditText)popView.findViewById(R.id.groupName);
            String name = nameText.getText().toString();
            Log.i(TAG, "get name " + name);
            ((ServerDevice)myDevice).createGroup(name, NewGroupActivity.this);
        }
    }

    class CalibrateBtnListener implements View.OnClickListener{
        @Override
        public void onClick(View v){
            myDevice.status = Device.CALIBRATE_STATUS;
            Intent calibrate = new Intent(NewGroupActivity.this, CalibrateActivity.class);
	        Device.myDevice=myDevice;
            NewGroupActivity.this.startActivity(calibrate);
	        NewGroupActivity.this.finish();
        }
    }

    private TextView addMemberBox(String text){
		TextView clientText=new TextView(this);
		clientText.setText(text);
		clientText.setTextSize(18);
		clientText.setTextColor(Color.parseColor("#50a0be"));
		clientText.setGravity(Gravity.CENTER);
		memberBox.addView(clientText);
		return clientText;
	}

	public Handler handler=new Handler(){
		@Override
		public void handleMessage(Message msg){
			if(msg.what==1){ // Connection failed

			}

			if(msg.what == 2){ // new member added
				Bundle b = msg.getData();
				addMemberBox(b.getString("addr").substring(1));
			}

		}
	};
}
