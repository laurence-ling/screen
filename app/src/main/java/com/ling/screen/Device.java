package com.ling.screen;

import android.util.Log;

import com.datouhou.TouchImageView;

import java.io.Serializable;
import java.net.*;

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
    public static final int MAX_IMAGE_SIZE = 1024*1024;

    TouchImageView touchImage;
    double posX=0, posY=0, angle=0; // global coord (mm,mm,rad_CW)
    double deltaT=0;

    int finger_num=0;
    double point[]=new double[4];
    InetAddress father=null; // your present daddy when calibrating
    
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
                Log.i(TAG, "local address " + address.toString());
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
        }
    }
    
    public static void printScreenInfo(){
        Log.i(TAG,"Screen Resolution = "+resX+" * "+resY);
        Log.i(TAG,"Screen Size = "+scr_width+"mm * "+scr_height+"mm");
    }
}
