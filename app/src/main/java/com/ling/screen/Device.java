package com.ling.screen;


import android.graphics.Bitmap;
import android.os.Message;
import android.os.SystemClock;
import android.util.Log;

import com.datouhou.TouchImageView;
import com.iraka.widget.Coordinate;
import com.iraka.widget.ScreenEvent;

import java.io.IOException;
import java.io.Serializable;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.List;

/**
 * Created by ling on 2017/5/23.
 */

public class Device implements Serializable{

    private static final String TAG = "Device";
    public static final int WAITING_STATUS = 1;
    public static final int CALIBRATE_STATUS = 2;
    public static final int WORKING_STATUS = 3;
    public static final int CLIENT_UDP_PORT = 9999;
    public static final int CLIENT_TCP_PORT = 10010;
    public static final String IMAGE_ROUTE = "";
    public static final int MAX_IMAGE_SIZE = 1024*1024*10;

    Bitmap bitmap;
    TouchImageView touchImage;
    public double posX=0, posY=0, angle=0; // global coord (mm,mm,rad_CW)
    public int finger_num=0;
    public double point[]=new double[4];

    public int finger_num_old=0;
    public double point_old[]=new double[4];
    
    // local device boot time - server boot time
    // this is a value ONLY used by SERVER when calibrating
    long bootTimeDiffFromServer=0;
    
    InetAddress father=null; // your present daddy when calibrating, use SET to merge groups
    boolean isCalibrated=false; // is this device calibrated to the global coordinate ?
    boolean isTimeSync=false; // is time synchronized

    public static double scr_width, scr_height; // in mm
    public static double ppmX, ppmY; // pixel per mmm
    public static int resX, resY; // resolution
    
    public static Device myDevice;
    
    InetAddress address; // this device's own ip address
    InetAddress serverAddr; // server's udp address
    DatagramSocket udpSocket;
    volatile int status; // current status

    public Device(){
        Thread setLocalAddressThread=new Thread(new SetLocalAddressThread());
        setLocalAddressThread.start();
        // Not good enough, though
        try{
            setLocalAddressThread.join();
        }catch(InterruptedException e){
            Log.i(TAG,"Device Initialization Interrupted!");
        }
        
    }
    public Device(InetAddress _addr){
        address = _addr;
    }
    
    public class SetLocalAddressThread implements Runnable{
        public void run(){
            try{
                address = InetAddress.getLocalHost();
                if(MainActivity.isServer){ // set itself as server
                    serverAddr=address;
                }
            }catch(UnknownHostException e){
                Log.e(TAG, "set local host error", e);
            }
        }
    }
    public class CreateSocketThread implements Runnable{
        public void run() {
            try {
                udpSocket = new DatagramSocket(CLIENT_UDP_PORT, InetAddress.getByName("0.0.0.0"));
                udpSocket.setBroadcast(true);
            } catch (SocketException e) {
                Log.e(TAG, "create socket error", e);
            }catch (UnknownHostException e){
                Log.e(TAG, "get local host error", e);
            }
        }
    }
    public void closeSocket(){
        new Thread(new CloseUdpSocket()).start();
    }
    class CloseUdpSocket implements Runnable{
        public void run(){
            if (udpSocket != null){
                udpSocket.close();
            }
            udpSocket=null;
        }
    }
    
    public static void printScreenInfo(){
        Log.i(TAG,"Screen Resolution = "+resX+" * "+resY);
        Log.i(TAG,"Screen Size = "+scr_width+"mm * "+scr_height+"mm");
    }
    
    //====================== function for calibration =============================
    
    private Coordinate calcSpeed6(List<ScreenEvent> historyTouch,int pos){
        double vX=0;
        double vY=0;
        for(int i=0;i<3;i++){ // {3,4,5}-{0,1,2}
            ScreenEvent ev1=historyTouch.get(i+pos);
            ScreenEvent ev2=historyTouch.get(i+pos+3);
            vX+=(ev1.posX-ev2.posX)/(ev1.timestamp-ev2.timestamp);
            vY+=(ev1.posY-ev2.posY)/(ev1.timestamp-ev2.timestamp);
        }
        
        return new Coordinate(vX/3,vY/3);
    }
    
    private ScreenEvent calcPosition3(List<ScreenEvent> historyTouch,int pos){
        double pX=0;
        double pY=0;
        long timestamp=0;
        for(int i=0;i<3;i++){
            ScreenEvent ev=historyTouch.get(i+pos);
            pX+=ev.posX;
            pY+=ev.posY;
            timestamp+=ev.timestamp;
        }
    
        return new ScreenEvent(0,timestamp/3,pX/3,pY/3,0,0); // * timestamp overflow ?
    }
    
    public String reportCalibrationMotionSequence(List<ScreenEvent> historyTouch,CalibrateActivity ca){
        final int size=historyTouch.size();
        if(size<8){
            Log.i(TAG,"Touch Sequence Too Short");
            return "Touch Sequence Too Short";
        }
        
        // Filter out first & last event to prevent screen side effect
        // Apply avg filter to Position & Velocity
        ScreenEvent pSt=calcPosition3(historyTouch,1);
        ScreenEvent pEd=calcPosition3(historyTouch,size-4);
        Coordinate vSt=calcSpeed6(historyTouch,1);
        Coordinate vEd=calcSpeed6(historyTouch,size-7);
    
        ScreenEvent evSt=new ScreenEvent(ScreenEvent.MOVE,pSt.timestamp,pSt.posX,pSt.posY,vSt.x,vSt.y);
        ScreenEvent evEd=new ScreenEvent(ScreenEvent.MOVE,pEd.timestamp,pEd.posX,pEd.posY,vEd.x,vEd.y);
        Log.i(TAG,"Start = "+evSt.toString());
        Log.i(TAG,"End   = "+evEd.toString());
        
        if(isTimeSync){
            Log.i(TAG,"Send Calibration Data ...");
            new Thread(new SendCalibrateDataThread(evSt,evEd)).start();
        }
        
        return "Velocity = ("+Math.round(Math.hypot(vSt.x,vSt.y)*1000)/1000.+")~("
        +Math.round(Math.hypot(vEd.x,vEd.y)*1000)/1000.+")";
    }
    
    DatagramSocket calibDataSocket;
    public void initCalibrationSocket(){
        new Thread(){ // initialize socket
            @Override
            public void run(){
                try{
                    calibDataSocket=new DatagramSocket();
                    //calibDataSocket.setSoTimeout(1000);
                    calibDataSocket.setSoTimeout(0);
                }catch(IOException e){
                    Log.e(TAG, "Calibration Data Communication Initializing Error",e);
                }
            }
        }.start();
    }
    public void closeCalibrationSocket(){
        new Thread(){
            @Override
            public void run(){
                calibDataSocket.close();
            }
        }.start();
    }
    
    public void respondTimeSync(final CalibrateActivity ca){
        new Thread(){
            @Override
            public void run(){
                try{
                    Log.i(TAG,"Start Responding Time Sync");
                    byte[] buffer=new byte[88];
                    DatagramPacket timeSyncPacket=new DatagramPacket(buffer,buffer.length);
                    while(true){
                        long recTime,ackTime;
                        while(true){
                            udpSocket.setSoTimeout(0);
                            udpSocket.receive(timeSyncPacket);
                            recTime=SystemClock.uptimeMillis();
                            ScreenEvent test1=new ScreenEvent(buffer,0);
                            ScreenEvent test2=new ScreenEvent(buffer,44);
                            Log.i(TAG,"Time Sync data received");
                            if(test1.type==ScreenEvent.TIME_SYNC&&test2.type==ScreenEvent.TIME_SYNC)
                                break;
                            /*if(test1.type==ScreenEvent.TIME_SYNC_OK&&test2.type==ScreenEvent.TIME_SYNC_OK){
                                isTimeSync=true;
                                Log.i(TAG,"Time Synchronized");
                                return;
                            }*/
                        }
                        Log.i(TAG,"Valid time sync data received");
    
                        new ScreenEvent(ScreenEvent.TIME_SYNC,recTime,0,0,0,0).writeEventBuffer(buffer,0);
                        ackTime=SystemClock.uptimeMillis();
                        new ScreenEvent(ScreenEvent.TIME_SYNC,ackTime,0,0,0,0).writeEventBuffer(buffer,44);
                        DatagramPacket ackSyncPacket=new DatagramPacket(buffer,buffer.length);
                        ackSyncPacket.setSocketAddress(timeSyncPacket.getSocketAddress());
                        udpSocket.send(ackSyncPacket);
                        
                        isTimeSync=true;
                        Log.i(TAG,"Time sync data responded to "+timeSyncPacket.getSocketAddress());
                        Message msg=new Message();
                        msg.what=2;
                        ca.handler.sendMessage(msg);
                        
                        startReceiveCalibrateResult(ca);
                        break;
                    }
                }catch(IOException e){
                    Log.e(TAG, "Time Sync Respond Error",e);
                }
            }
        }.start();
    }
    
    public void startReceiveCalibrateResult(CalibrateActivity ca){ // Only needed by client device, though
        new Thread(new ReceiveCalibrateResultThread(ca)).start();
        new Thread(new ReceiveCalibrateCompleteThread(ca)).start();
    }
    
    public class ReceiveCalibrateResultThread implements Runnable{
        CalibrateActivity ca;
        ReceiveCalibrateResultThread(CalibrateActivity ca_){
            ca=ca_;
        }
    
        @Override
        public void run(){
            DatagramPacket calibDataPacket;
            try{
                Log.i(TAG,"Start listening for calib success mark");
                while(status==CALIBRATE_STATUS){
                    // get calibration data response
                    byte[] buffer=new byte[88];
                    calibDataPacket=new DatagramPacket(buffer,buffer.length);
                    calibDataSocket.receive(calibDataPacket);
                    buffer=calibDataPacket.getData();
                    ScreenEvent sev=new ScreenEvent(buffer,0);
    
                    if(sev.type==ScreenEvent.CALIB_OK){ // successful
                        isCalibrated=true;
                        posX=sev.posX;
                        posY=sev.posY;
                        angle=sev.velX;
                        
                        Log.i(TAG,"Get Coord = "+new Coordinate(posX,posY,angle));
                        
                        Message msg=new Message();
                        msg.what=0;
                        ca.handler.sendMessage(msg);
                    }
                }
            }catch(IOException e){
                Log.e(TAG, "Calibration Data Receive Error",e);
            }
        }
    }
    
    public class ReceiveCalibrateCompleteThread implements Runnable{
        CalibrateActivity ca;
        ReceiveCalibrateCompleteThread(CalibrateActivity ca_){
            ca=ca_;
        }
        
        @Override
        public void run(){
            DatagramPacket calibDataPacket;
            try{
                Log.i(TAG,"Start listening for calib complete mark");
                while(status==CALIBRATE_STATUS){
                    byte[] buffer=new byte[88];
                    calibDataPacket=new DatagramPacket(buffer,buffer.length);
                    udpSocket.receive(calibDataPacket);
                    buffer=calibDataPacket.getData();
                    ScreenEvent sev=new ScreenEvent(buffer,0);
                    
                    if(sev.type==ScreenEvent.ALL_CALIB_OK){ // successful
                        
                        Message msg=new Message();
                        msg.what=3;
                        ca.handler.sendMessage(msg);
                        break;
                    }
                    
                }
            }catch(IOException e){
                Log.e(TAG, "Calibration Complete Receive Error",e);
            }
        }
    }
    
    public class SendCalibrateDataThread implements Runnable{
        ScreenEvent eventStart,eventEnd;
        SendCalibrateDataThread(ScreenEvent evSt,ScreenEvent evEd){
            eventStart=evSt;
            eventEnd=evEd;
        }
        
        @Override
        public void run(){
            DatagramPacket calibDataPacket;
            try{
                // send two ScreenEvent objects
                byte[] buffer=new byte[88];
                eventStart.writeEventBuffer(buffer,0);
                eventEnd.writeEventBuffer(buffer,44);
                
                calibDataPacket=new DatagramPacket(buffer,buffer.length);
                calibDataPacket.setSocketAddress(new InetSocketAddress(serverAddr,CLIENT_UDP_PORT));
                calibDataSocket.send(calibDataPacket);
                Log.i(TAG,"Calib data sent to "+serverAddr);
                
            }catch(IOException e){
                Log.e(TAG, "Calibration Data Send Error",e);
            }
        }
    }
}
