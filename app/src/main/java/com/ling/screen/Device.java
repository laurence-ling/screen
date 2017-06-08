package com.ling.screen;

import android.os.Bundle;
import android.os.Message;
import android.util.Log;

import com.iraka.widget.ScreenEvent;

import java.io.IOException;
import java.io.Serializable;
import java.net.*;
import java.util.List;

/**
 * Created by ling on 2017/5/23.
 */

public class Device implements Serializable{

    private static final String TAG = "Device";
    public static final int WAITING_STATUS = 1;
    public static final int CALIBRATE_STATUS = 2;
    public static final int CLIENT_UDP_PORT = 9999;
    public static final int CLIENT_TCP_PORT = 9998;
    public static final String IMAGE_ROUTE = "";
    
    double posX=0, posY=0, angle=0; // global coord (mm,mm,rad_CW)
    double deltaT=0;
    
    InetAddress father=null; // your present daddy when calibrating, use SET to merge groups
    boolean isCalibrated=false; // is this device calibrated to the global coordinate ?
    
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
    
    public String reportCalibrationMotionSequence(List<ScreenEvent> historyTouch,CalibrateActivity ca){
        final int size=historyTouch.size();
        if(size<6){
            Log.i(TAG,"Touch Sequence Too Short");
            return "Invalid";
        }
        
        /*for(int i=0;i<historyTouch.size();i++){
            Log.i(TAG,historyTouch.get(i).toString());
        }
        Log.i(TAG,"Length = "+historyTouch.size());*/
        
        // Shall be using 5 point central diff
        // Temp: use first-order approx. instead
        
        // Filter out first & last event to prevent side effect
        ScreenEvent evS1=historyTouch.get(1); // Start
        ScreenEvent evS2=historyTouch.get(5);
        ScreenEvent evE1=historyTouch.get(size-2); // End
        ScreenEvent evE2=historyTouch.get(size-6);
        
        double vSX=(evS1.posX-evS2.posX)/(evS1.timestamp-evS2.timestamp);
        double vSY=(evS1.posY-evS2.posY)/(evS1.timestamp-evS2.timestamp);
        double vEX=(evE1.posX-evE2.posX)/(evE1.timestamp-evE2.timestamp);
        double vEY=(evE1.posY-evE2.posY)/(evE1.timestamp-evE2.timestamp);
    
        ScreenEvent evSt=new ScreenEvent(ScreenEvent.MOVE,evS1.timestamp,evS1.posX,evS1.posY,vSX,vSY);
        ScreenEvent evEd=new ScreenEvent(ScreenEvent.MOVE,evE1.timestamp,evE1.posX,evE1.posY,vEX,vEY);
        Log.i(TAG,"Start = "+evSt.toString());
        Log.i(TAG,"End   = "+evEd.toString());
    
        new Thread(new SendCalibrateDataThread(ca,evSt,evEd)).start();
        
        return "("+Math.round(vSX*1000)/1000.+","+Math.round(vSY*1000)/1000.+")~("
        +Math.round(vEX*1000)/1000.+","+Math.round(vEY*1000)/1000.+")";
    }
    
    DatagramSocket calibDataSocket;
    public void initCalibrationSocket(){
        new Thread(){
            @Override
            public void run(){
                try{
                    calibDataSocket=new DatagramSocket();
                    calibDataSocket.setSoTimeout(1000);
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
    
    public class SendCalibrateDataThread implements Runnable{
        CalibrateActivity ca;
        ScreenEvent eventStart,eventEnd;
        SendCalibrateDataThread(CalibrateActivity ca_,ScreenEvent evSt,ScreenEvent evEd){
            ca=ca_;
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
                Log.i(TAG,"Calib data sent to "+serverAddr+", waiting for response ...");
                
                // get calibration data response
                buffer=new byte[2];
                calibDataPacket=new DatagramPacket(buffer,buffer.length);
                calibDataSocket.receive(calibDataPacket);
                buffer=calibDataPacket.getData();
                Log.i(TAG,"Calib data response received. buffer[0] = "+buffer[0]);
                
                if(buffer[0]!='#'){ // format error
                    return;
                }
                
                if(buffer[1]==0x00){ // Successful
                    isCalibrated=true;
                    Message msg=new Message();
                    msg.what=0;
                    ca.handler.sendMessage(msg);
                }
    
                if(buffer[1]==0x01){ // Not Precise Enough
                    
                }
            }catch(IOException e){
                Log.e(TAG, "Calibration Data Communication Error",e);
            }
        }
    }
}
