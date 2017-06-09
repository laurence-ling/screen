package com.ling.screen;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.support.v4.content.ContextCompat;
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
    //Bitmap tempBitmap;
    Matrix matrix;
    ScreenEvent screenEvent;
    public byte [] buffer=new byte[100];;
    //public DatagramPacket Package;
    Button openPicBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_working);
        Log.i(TAG,"working activity start");
        myDevice=Device.myDevice;
        matrix = new Matrix();
        myDevice.touchImage = (TouchImageView)findViewById(R.id.imgView);
        openPicBtn = (Button)findViewById(R.id.button1);
        if(!MainActivity.isServer){
            openPicBtn.setVisibility(View.GONE);
            ((ClientDevice)myDevice).acceptFile(WorkingActivity.this);
        }
        else {
           Log.i(TAG,"This device is server: "+MainActivity.isServer);
           myDevice.serverAddr = myDevice.address;
           openPicBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent i = new Intent(
                            Intent.ACTION_PICK,
                            MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                    startActivityForResult(i,RESULT_LOAD_IMAGE);
                }
           });
            new Thread(new ReceiveEventThread()).start();
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
                    myDevice.touchImage.task(myDevice.serverAddr,new Coordinate(myDevice.posX,myDevice.posY,myDevice.angle));
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
                DatagramPacket pack=new DatagramPacket(buffer, buffer.length);
                try {
                    myDevice.udpSocket.receive(pack);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                genEvent(pack);

                changepic.myrun();
                screenEvent = changepic.screenEvent;
                showPic();
            }
        }
    }
     public void genEvent(DatagramPacket pack){
         InetAddress address = pack.getAddress();
        //Log.i(TAG,"   Device: "+address.toString());
        temp_device=((ServerDevice)myDevice).deviceMap.get(address);
        
        if(temp_device==null){
            Log.w("TIVIV","Not Found, "+address);
            temp_device=myDevice;
        }
        buffer = pack.getData();
        ScreenEvent Sevent=new ScreenEvent(buffer,0);

        
        Log.i("ISJDI",address+" type " + Sevent.type+" 0");

        if(Sevent.type==-1){
            temp_device.finger_num=0;
            Log.i(TAG,"finger_num 0");
        }
        else {
            temp_device.finger_num = 1;
            Log.i(TAG, "finger_num 1");
            double x = Sevent.posX;
            double y = Sevent.posY;
            Log.i(TAG,"x:"+x+"  y:"+y);
            temp_device.point[0] = x;
            temp_device.point[1] = y;
            if (Sevent.type == 20) {
                Log.i(TAG, "finger_num 2");
                temp_device.finger_num++;
                Sevent = new ScreenEvent(buffer, 44);
                x = Sevent.posX;
                y = Sevent.posY;
                temp_device.point[2] = x;
                temp_device.point[3] = y;
            }
        }
    
         Log.w("TIVIV","cnt="+temp_device.finger_num+" f0 = ("+temp_device.point[0]+","+temp_device.point[1]
         +") f1 = ("+temp_device.point[2]+","+temp_device.point[3]+")");
    }
    public void showPic(){
        Log.i(TAG, "in showPicThread");
        
        double px = (float)(temp_device.point[0] + temp_device.point[2])/2;
        double py = (float)(temp_device.point[1] + temp_device.point[3])/2;
        //matrix.postTranslate((float)screenEvent.posX, (float)screenEvent.posY);
        Coordinate coord = new Coordinate(screenEvent.posX, screenEvent.posY).toLocal2(myDevice);
        Coordinate midCoord = new Coordinate(px, py).toLocal(new Coordinate(myDevice.posX,myDevice.posY,myDevice.angle));
        double dx = coord.x * Device.ppmX;
        double dy = coord.y * Device.ppmY;
        Log.i(TAG, "transdx"+dx+" transdy" + dy);
        matrix.postTranslate((float)dx, (float)dy);
        matrix.postRotate((float)(-screenEvent.velX/Math.PI*180), (float)midCoord.x, (float)midCoord.y);
        matrix.postScale((float)screenEvent.velY, (float)screenEvent.velY, (float)midCoord.x, (float)midCoord.y);
        
        //tempBitmap = Bitmap.createBitmap(myDevice.bitmap, 0, 0, myDevice.bitmap.getWidth(), myDevice.bitmap.getHeight(), matrix, true);
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
            myDevice.bitmap = BitmapFactory.decodeFile(picturePath);
            setImage(myDevice.bitmap, new Matrix());
            Log.w(TAG,"   picture showed");
            ((ServerDevice)myDevice).sendFile(myDevice.bitmap);
            openPicBtn.setVisibility(View.GONE);
        }
    }
    public static Paint paint = new Paint();
    public void setImage(Bitmap bitmap, Matrix matrix_){
        if(myDevice.touchImage.getWidth()==0||myDevice.touchImage.getHeight()==0||bitmap==null)return;
        Bitmap background = Bitmap.createBitmap(myDevice.touchImage.getWidth(), myDevice.touchImage.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(background);
        canvas.drawBitmap(bitmap, matrix_, paint);
        //paint.setARGB(255,0,0,255);
        //canvas.drawCircle(100,100,50,paint);
        myDevice.touchImage.setImageBitmap(background);

    }
    public Handler handler=new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == 1) { // client receive image file
                Log.w(TAG, "Matrix = "+matrix);
                //myDevice.touchImage.setImageBitmap(tempBitmap);
                setImage(myDevice.bitmap, matrix);
            }
        }
    };
}
