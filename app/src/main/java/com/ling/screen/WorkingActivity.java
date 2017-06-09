package com.ling.screen;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.Jing.ChangePic;
import com.datouhou.TouchImageView;
import com.iraka.widget.Coordinate;
import com.iraka.widget.ScreenEvent;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.util.Iterator;
import java.util.Map;


/**
 * Created by datouhou on 2017/6/8.
 */

public class WorkingActivity extends Activity{
    private static int RESULT_LOAD_IMAGE=1;
    private static final String TAG = "WorkingActivity";
    Device myDevice;
    Device temp_device;
    Bitmap tempBitmap;
    Matrix matrix;
    ScreenEvent screenEvent;
    public byte [] buffer=new byte[100];;
    public DatagramPacket Package;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_working);
        Log.i(TAG,"working activity start");
        myDevice=Device.myDevice;
        matrix = new Matrix();
        myDevice.touchImage = (TouchImageView)findViewById(R.id.imgView);
        Button button1=(Button)findViewById(R.id.button1);
        if(!MainActivity.isServer){
            button1.setVisibility(View.GONE);
            ((ClientDevice)myDevice).acceptFile(WorkingActivity.this);
        }
        else {
           Log.i(TAG,"This device is server: "+MainActivity.isServer);
           myDevice.serverAddr = myDevice.address;

           button1.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent i = new Intent(
                            Intent.ACTION_PICK,
                            MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                    startActivityForResult(i,RESULT_LOAD_IMAGE);
                }
           });
            new Thread(new ReceiveEventThread()).start();
            //new Thread(new showPicThread()).start();
        }
        new Thread(new SendEventThread()).start();

    }
    public class SendEventThread implements Runnable{
        @Override
        public void run() {
            while (true) {
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                try {
                    myDevice.touchImage.task(myDevice.serverAddr);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public class ReceiveEventThread implements Runnable{
        @Override
        public void run() {
            ChangePic changepic = new ChangePic(myDevice);
            while(true){
                byte[] buffer = new byte[100];
                Package=new DatagramPacket(buffer, buffer.length);
                try {
                    myDevice.udpSocket.receive(Package);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                genEvent();
                
                changepic.myrun();
                screenEvent = changepic.screenEvent;
                showPic();
            }
        }
    }
     public void genEvent(){
         InetAddress address = Package.getAddress();
        //Log.i(TAG,"   Device: "+address.toString());
        temp_device = null;
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

        Coordinate deviceCoord=new Coordinate(myDevice.posX,myDevice.posY,myDevice.angle);
        Log.i(TAG,"type" + Sevent.type+"0");

        if(Sevent.type==-1){
            temp_device.finger_num=0;
            Log.i(TAG,"finger_num 0");
        }
        else {
            temp_device.finger_num = 1;
            Log.i(TAG, "finger_num 1");
            double x = Sevent.posX;
            double y = Sevent.posY;
            Coordinate globalCoord = (new Coordinate(x, y)).toGlobal(deviceCoord);
            temp_device.point[0] = globalCoord.x;
            temp_device.point[1] = globalCoord.y;
            if (Sevent.type == 20) {
                Log.i(TAG, "finger_num 2");
                temp_device.finger_num++;
                Sevent = new ScreenEvent(buffer, 44);
                x = Sevent.posX;
                y = Sevent.posY;
                globalCoord = (new Coordinate(x, y)).toGlobal(deviceCoord);
                temp_device.point[2] = globalCoord.x;
                temp_device.point[3] = globalCoord.y;
            }
        }
    }
    public void showPic(){
        Log.i(TAG, "in showPicThread");

        if(screenEvent.type == 1){
            Log.i(TAG, "screenEvent type 1");
            //matrix.postTranslate(0.5f, 0.5f);
            Coordinate coord = new Coordinate(screenEvent.posX, screenEvent.posY).toLocal2(myDevice);
            double dx = coord.x * Device.ppmX;
            double dy = coord.y * Device.ppmY;
            Log.i(TAG, "transdx"+dx+" transdy" + dy);
            matrix.postTranslate((float)dx, (float)dy);
        }
        else if(screenEvent.type == 2){
            Log.i(TAG, "screenEvent type 2");
            float px = (float)(temp_device.point[0] + temp_device.point[2])/2;
            float py = (float)(temp_device.point[1] + temp_device.point[3])/2;
            //matrix.postTranslate((float)screenEvent.posX, (float)screenEvent.posY);
            Coordinate coord = new Coordinate(screenEvent.posX, screenEvent.posY).toLocal2(myDevice);
            double dx = coord.x * Device.ppmX;
            double dy = coord.y * Device.ppmY;
            Log.i(TAG, "transdx"+dx+" transdy" + dy);
            matrix.postTranslate((float)dx, (float)dy);
            matrix.postRotate((float)screenEvent.velY, px, py);
            matrix.postScale((float)screenEvent.velX, (float)screenEvent.velX, px, py);
        } else {
            return;
        }
        tempBitmap = Bitmap.createBitmap(myDevice.bitmap, 0, 0, myDevice.bigit commtmap.getWidth(), myDevice.bitmap.getHeight(), matrix, true);
        Message msg = new Message();
        msg.what = 1;
        handler.sendMessage(msg);
    }

    @Override
    protected void onActivityResult(int requestCode,int resultCode,Intent data){
        super.onActivityResult(requestCode,resultCode,data);

        if(requestCode == RESULT_LOAD_IMAGE && resultCode == RESULT_OK && null != data){
            //socket;
            Log.i(TAG,"   picture selected");
            Uri selectedImage = data.getData();
            String[] filePathColumn = {MediaStore.Images.Media.DATA};

            Cursor cursor = getContentResolver().query(selectedImage,filePathColumn,null,null,null);
            cursor.moveToFirst();

            int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
            String picturePath = cursor.getString(columnIndex);
            cursor.close();
            //send;
            myDevice.touchImage.setImageBitmap(BitmapFactory.decodeFile(picturePath));
            myDevice.bitmap = BitmapFactory.decodeFile(picturePath);
            Log.i(TAG,"   picture showed");
            ((ServerDevice)myDevice).sendFile(myDevice.bitmap);
        }
    }
    public Handler handler=new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == 1) { // client receive image file
                Log.i(TAG, "handle Message");
                myDevice.touchImage.setImageBitmap(tempBitmap);
            }
        }
    };
}
