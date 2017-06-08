package com.ling.screen;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import com.datouhou.TouchImageView;
import com.iraka.widget.ScreenEvent;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.Map;


import static android.R.attr.data;


/**
 * Created by datouhou on 2017/6/8.
 */

public class WorkingActivity extends Activity{
    private static int RESULT_LOAD_IMAGE=1;
    private static final String TAG = "WorkingActivity";
    private TouchImageView touchImg;
    public Handler handler= new Handler();
    public Handler handler2 = new Handler();
    Device myDevice;
    public byte [] buffer=new byte[100];;
    public DatagramPacket Package;
    int count=0;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_working);
        touchImg = (TouchImageView)findViewById(R.id.imgView);

        myDevice=Device.myDevice;
       if(MainActivity.isServer){
           Log.i(TAG,"This device is server: "+MainActivity.isServer);
           myDevice.serverAddr = myDevice.address;
           Button button1=(Button)findViewById(R.id.button1);
           button1.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent i = new Intent(
                            Intent.ACTION_PICK,
                            MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                    startActivityForResult(i,RESULT_LOAD_IMAGE);
                }
           });
           new Thread(new myRunnable1()).start();
        }
     new Thread(new Runnable2()).start();
    }
    public class Runnable2 implements Runnable{
        @Override
        public void run() {
            while (true) {
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                try {
                    touchImg.task(myDevice.serverAddr);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
    public class myRunnable1 implements Runnable{

        @Override
        public void run() {
            while(true){
                Package=new DatagramPacket(buffer,buffer.length);
                try {
                    myDevice.udpSocket.receive(Package);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                InetAddress address = Package.getAddress();
                Log.i(TAG,"   Device: "+address.toString());
                Device temp_device = null;
                boolean flag = false;
                Iterator <Map.Entry<InetAddress,Device>> it = ((ServerDevice)myDevice).deviceMap.entrySet().iterator();
                while (it.hasNext()) {
                    Map.Entry<InetAddress, Device> entry = it.next();
                    if(entry.getKey().toString()==address.toString()){
                        flag=true;
                        temp_device=entry.getValue();
                        break;
                    }
                }
                if(!flag)   temp_device=myDevice;
                buffer = Package.getData();
                ScreenEvent Sevent=new ScreenEvent(buffer,0);
                Log.i(TAG,"type" + Sevent.type+"0");
                if(Sevent.type==-1){
                    temp_device.finger_num=0;
                    Log.i(TAG,"finger_num 0");
                }
                else{
                    temp_device.finger_num=1;
                    Log.i(TAG,"finger_num 1");
                    temp_device.point[0]=Sevent.posX;
                    temp_device.point[1]=Sevent.posY;
                    if(Sevent.type==20){
                        temp_device.finger_num++;
                        Sevent=new ScreenEvent(buffer,44);
                        temp_device.point[2]=Sevent.posX;
                        temp_device.point[3]=Sevent.posY;
                    }
                }
                buffer = new byte[100];
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode,int resultCode,Intent data){
        super.onActivityResult(requestCode,resultCode,data);

        if(requestCode == RESULT_LOAD_IMAGE && resultCode == RESULT_OK && null != data){
            //socket;
            Uri selectedImage = data.getData();
            String[] filePathColumn = {MediaStore.Images.Media.DATA};

            Cursor cursor = getContentResolver().query(selectedImage,filePathColumn,null,null,null);
            cursor.moveToFirst();

            int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
            String picturePath = cursor.getString(columnIndex);
            cursor.close();
            //send;
            touchImg = (TouchImageView) findViewById(R.id.imgView);
            touchImg.setImageBitmap(BitmapFactory.decodeFile(picturePath));
            Bitmap bitmap = BitmapFactory.decodeFile(picturePath);
            ((ServerDevice)myDevice).sendFile(bitmap);
        }
    }

}
