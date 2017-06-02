package com.ling.screen;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.support.v7.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

public class MainActivity extends Activity {
    public Device myDevice;
    public boolean isServer;
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
    }
}