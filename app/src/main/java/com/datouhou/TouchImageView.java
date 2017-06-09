package com.datouhou;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.MotionEvent;
import android.view.View;

import com.iraka.widget.ScreenEvent;
import com.ling.screen.Device;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;

/**
 * @ClassName: TouchImageView
 * @author datouhou
 * @date 2017-06-07
 *
 */
public class TouchImageView extends android.support.v7.widget.AppCompatImageView{
    //private final static String TAG="MatrixImageView";
    private GestureDetector mGestureDetector;
    private double point[]=new double[4];
    private int finger_count=0;
    private int mode[]=new int[2];
    DatagramSocket socket = new DatagramSocket();
    public TouchImageView(Context context, AttributeSet attrs) throws SocketException {
        super(context, attrs);
        MatrixTouchListener mListener=new MatrixTouchListener();
        setOnTouchListener(mListener);
        mGestureDetector=new GestureDetector(getContext(), new GestureListener(mListener));
        //背景设置为balck
        setBackgroundColor(Color.BLACK);
        //将缩放类型设置为FIT_CENTER，表示把图片按比例扩大/缩小到View的宽度，居中显示
        setScaleType(ScaleType.FIT_CENTER);
    }

    @Override
    public void setImageBitmap(Bitmap bm) {
        // TODO Auto-generated method stub

    }

    public class MatrixTouchListener implements OnTouchListener{
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            // TODO Auto-generated method stub
            finger_count=event.getPointerCount();
            if(finger_count==2){
                point[0]=event.getX(0);
                point[1]=event.getY(0);
                point[2]=event.getX(1);
                point[3]=event.getY(1);
            }
            if(finger_count==1){
                point[0]=event.getX();
                point[1]=event.getY();
            }
            return mGestureDetector.onTouchEvent(event);
        }
    }

    byte [] buffer = new byte[100];
    public void task(InetAddress server_address) throws IOException {
            if(finger_count==1){
                ScreenEvent Sevent = new ScreenEvent(10,point[0],point[1]);
                Sevent.writeEventBuffer(buffer,0);
            }
            else if(finger_count==2){
                ScreenEvent Sevent = new ScreenEvent(20,point[0],point[1]);
                Sevent.writeEventBuffer(buffer,0);
                Sevent = new ScreenEvent(20,point[2],point[3]);
                Sevent.writeEventBuffer(buffer,22);
            }
            else{
                ScreenEvent Sevent = new ScreenEvent(-1,point[0],point[1]);
                Sevent.writeEventBuffer(buffer,0);
            }
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length,server_address, Device.CLIENT_UDP_PORT);
        socket.send(packet);
        buffer = new byte[1024];
    }


    private class  GestureListener extends SimpleOnGestureListener{
        private final MatrixTouchListener listener;
        public GestureListener(MatrixTouchListener listener) {
            this.listener=listener;
        }
        @Override
        public boolean onDown(MotionEvent e) {
            //捕获Down事件
            return true;
        }
        @Override
        public boolean onDoubleTap(MotionEvent e) {
            //触发双击事件
            return super.onDoubleTap(e);
        }
        @Override
        public boolean onSingleTapUp(MotionEvent e) {
            // TODO Auto-generated method stub
            return super.onSingleTapUp(e);
        }

        @Override
        public void onLongPress(MotionEvent e) {
            // TODO Auto-generated method stub
            super.onLongPress(e);
        }

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2,
                                float distanceX, float distanceY) {
            return super.onScroll(e1, e2, distanceX, distanceY);
        }

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX,
                               float velocityY) {
            // TODO Auto-generated method stub

            return super.onFling(e1, e2, velocityX, velocityY);
        }

        @Override
        public void onShowPress(MotionEvent e) {
            // TODO Auto-generated method stub
            super.onShowPress(e);
        }


        @Override
        public boolean onDoubleTapEvent(MotionEvent e) {
            // TODO Auto-generated method stub
            return super.onDoubleTapEvent(e);
        }

        @Override
        public boolean onSingleTapConfirmed(MotionEvent e) {
            // TODO Auto-generated method stub
            return super.onSingleTapConfirmed(e);
        }

    }


}
