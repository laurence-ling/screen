package com.datouhou;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;

import com.iraka.widget.Coordinate;
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
public class TouchImageView extends SurfaceView implements SurfaceHolder.Callback{
    private final static String TAG="MatrixImageView";
    
    private SurfaceHolder holder;
    @Override
    public void surfaceCreated(SurfaceHolder holder){}
    @Override
    public void surfaceChanged(SurfaceHolder holder,int format,int width,int height){}
    @Override
    public void surfaceDestroyed(SurfaceHolder holder){}
    
    private static Paint paint=new Paint();
    private Rect bgRect;
    private Matrix lastMatrix=new Matrix();
    private boolean firstBitmapDrawn=false;
    public void setImageBitmap(Bitmap bitmap,Matrix matrix){
        //Log.i(TAG,"Draw BG");
        if(firstBitmapDrawn&&matrix.equals(lastMatrix)){
            Log.i(TAG,"Not Changed");
            return;
        }
        lastMatrix.set(matrix);
        Canvas canvas=holder.lockCanvas();
        if(canvas==null)return;
        firstBitmapDrawn=true;
        canvas.drawRect(bgRect,paint);
        canvas.drawBitmap(bitmap,matrix,paint);
        holder.unlockCanvasAndPost(canvas);
    }
    
    private GestureDetector mGestureDetector;
    private double point[]=new double[4];
    private int finger_count=0;
    private int mode[]=new int[2];
    DatagramSocket socket = new DatagramSocket();
    public TouchImageView(Context context, AttributeSet attrs) throws SocketException {
        super(context, attrs);
        holder=getHolder();
        holder.addCallback(this);
        holder.setFormat(PixelFormat.TRANSLUCENT);
        setZOrderOnTop(true);
        paint.setARGB(255,0,0,0);
        
        MatrixTouchListener mListener=new MatrixTouchListener();
        setOnTouchListener(mListener);
        mGestureDetector=new GestureDetector(getContext(), new GestureListener(mListener));
    }

    public void onWindowFocusChanged(boolean hasFocus){
        super.onWindowFocusChanged(hasFocus);
        resizeCanvasArea();
    }

    public void resizeCanvasArea(){
        bgRect=new Rect(0,0,getWidth(),getHeight());
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
                if(event.getAction()==MotionEvent.ACTION_UP){
                    finger_count=0;
                }
            }
            //Log.w("TIVIV","Finger"+finger_count);
            return mGestureDetector.onTouchEvent(event);
        }
    }

    byte [] buffer = new byte[100];
    public void task(InetAddress server_address,Coordinate deviceCoord) throws IOException {
            if(finger_count==1){
                Coordinate global1=new Coordinate(point[0],point[1]).toGlobal(deviceCoord);
                ScreenEvent Sevent = new ScreenEvent(10,global1.x,global1.y);
                Sevent.writeEventBuffer(buffer,0);
	            Log.w("TIVIE","1:"+Sevent);
            }
            else if(finger_count==2){
                Coordinate global1=new Coordinate(point[0],point[1]).toGlobal(deviceCoord);
                Coordinate global2=new Coordinate(point[2],point[3]).toGlobal(deviceCoord);
                ScreenEvent Sevent = new ScreenEvent(20,global1.x,global1.y);
                Sevent.writeEventBuffer(buffer,0);
	            Log.w("TIVIE","2:"+Sevent);
                Sevent = new ScreenEvent(20,global2.x,global2.y);
                Sevent.writeEventBuffer(buffer,44);
                Log.w("TIVIE","3:"+Sevent);
            }
            else{
                ScreenEvent Sevent=new ScreenEvent(-1,point[0],point[1]);
                Sevent.writeEventBuffer(buffer,0);
	            Log.w("TIVIE","0:"+Sevent);
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
