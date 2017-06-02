package com.ling.screen;

import android.util.Log;

import java.io.IOException;
import java.io.Serializable;
import java.net.*;

/**
 * Created by ling on 2017/5/23.
 */

public class Device implements Serializable{

    private static final String TAG = "Device";
    public static final int WAITING_STATUS = 1;
    public static final int CALIBRATE_STATUS = 2;
    public static final int CLIENT_UDP_PROT = 9898;
    public static final int CLIENT_TCP_PORT = 9998;
    public static final String IMAGE_ROUTE = "";
    double posX, posY, angle;
    double scr_width, scr_height;
    int resX, resY;
    InetAddress address; // this device's own ip address
    DatagramSocket udpSocket;
    volatile int status; // current status

    public Device(){
        new Thread(new SetLocalAddressThread()).start();
    }
    public Device(InetAddress _addr){
        address = _addr;
    }
    public class SetLocalAddressThread implements Runnable{
        public void run(){
            try{
                address = InetAddress.getLocalHost();
            }catch(UnknownHostException e){
                Log.e(TAG, "set local host error", e);
            }
        }
    }
    public class CreateSocketThread implements Runnable{
        public void run() {
            try {
                udpSocket = new DatagramSocket(CLIENT_UDP_PROT, InetAddress.getByName("0.0.0.0"));
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
}
