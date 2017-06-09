package com.ling.screen;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.DhcpInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;
import com.iraka.widget.Coordinate;
import com.iraka.widget.CoordinateMatch;
import com.iraka.widget.ScreenEvent;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.DataFormatException;

/**
 * Created by ling on 2017/5/23.
 */

public class ServerDevice extends Device{
    private static final String TAG = "Server Device";
    String groupName;
    public Map<InetAddress, Device> deviceMap;
    DatagramSocket beaconSocket; //server only socket, non-bind
    Thread beaconThread;
    private NewGroupActivity ngActivity;

    public ServerDevice(){
        super();
        deviceMap = new HashMap<>();
        deviceMap.put(this.address, this); // After address has been initialized or will be NULL
        status = Device.WAITING_STATUS;
        new Thread(new CreateSocketThread()).start();
    }
    public void createGroup(String name, NewGroupActivity _ngActivity){
        groupName = name;
        ngActivity = _ngActivity;
        beaconThread = new Thread(new BeaconThread());
        beaconThread.start();
        new Thread(new AckThread()).start();
    }
    class AckThread implements Runnable{
        DatagramPacket recvPacket;
        DatagramPacket ackPacket;
        @Override
        public void run(){
            String ack = "3@" + groupName; // ack message
            byte[] buf = new byte[100];
            ackPacket = new DatagramPacket(ack.getBytes(), ack.length());
            while(status == Device.WAITING_STATUS) {
                try {
                    if(beaconSocket == null) {
                        Thread.sleep(100);
                    }
                    recvPacket = new DatagramPacket(buf, buf.length);
                    beaconSocket.receive(recvPacket);
                    responseConnection();
                } catch (IOException e) {
                    Log.e(TAG, "udp receive error", e);
                }catch (InterruptedException e){
                    Log.e(TAG, "sleep interrupted", e);
                }
            }
        }
        public void responseConnection(){
            Log.i(TAG, "receive packet from " + recvPacket.getAddress().toString());
            int len = recvPacket.getLength();
            String recvStr = new String(recvPacket.getData()).substring(0, len);
            String type = recvStr.split("@")[0];
            if (!type.equals("2")){
                Log.i(TAG, "receive non-connect type");
                return;
            }
            ackPacket.setAddress(recvPacket.getAddress());
            ackPacket.setPort(recvPacket.getPort());
            try {
                beaconSocket.send(ackPacket);
            } catch (IOException e) {
                Log.e(TAG, "send error", e);
            }
            Log.i(TAG, "send ack");
            /* add new member to device map and update UI */
            InetAddress addr = recvPacket.getAddress();
            if (deviceMap.containsKey(addr))
                return;
            deviceMap.put(addr, new Device(addr));
            Message msg = new Message();
            msg.what = 2;
            Bundle b = new Bundle();
            b.putString("addr", addr.toString());
            msg.setData(b);
            ngActivity.handler.sendMessage(msg);
        }
    }
    class BeaconThread implements Runnable{
        DatagramPacket sendPacket;
        public void run(){
            try {
                beaconSocket = new DatagramSocket();
                beaconSocket.setBroadcast(true);
            } catch (SocketException e) {
                Log.e(TAG, "create socket error", e);
            }
            String msg = "1@" + groupName; //broadcast beacon message
            try {
                //InetAddress.getByName("255.255.255.255")
                sendPacket = new DatagramPacket(msg.getBytes(), msg.length(),
                       getBroadcastAddress() , CLIENT_UDP_PORT);
            } catch (IOException e) {
                Log.e(TAG, "get broadcast address error", e);
            }
            while(status == Device.WAITING_STATUS) {
                try {
                    beaconSocket.send(sendPacket);
                    Thread.sleep(200);
                } catch (IOException e) {
                    Log.e(TAG, "send error", e);
                } catch(InterruptedException e){
                    Log.e(TAG, "sleep interrupted", e);
                }
            }
        }
    }
    InetAddress getBroadcastAddress() throws IOException {
        WifiManager wifi = (WifiManager) ngActivity.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        DhcpInfo dhcp = wifi.getDhcpInfo();
        // handle null somehow

        int broadcast = (dhcp.ipAddress & dhcp.netmask) | ~dhcp.netmask;
        byte[] quads = new byte[4];
        for (int k = 0; k < 4; k++)
          quads[k] = (byte) ((broadcast >> k * 8) & 0xFF);
        return InetAddress.getByAddress(quads);
    }

    public void sendFile(Bitmap bitmap){
        ByteArrayOutputStream oStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, oStream);
        byte[] buffer = oStream.toByteArray();
        Log.i(TAG, "bitmap bytes " + buffer.length);
        new Thread(new SendFileThread(buffer)).start();
    }

    class SendFileThread implements Runnable {
        byte[] buffer;

        public SendFileThread(byte[] buf) {
            buffer = buf;
        }

        @Override
        public void run() {
            for (InetAddress clientAddr : deviceMap.keySet())
                try {
                    if (address == clientAddr)
                        continue; // server self
                    Socket socket = new Socket(clientAddr, CLIENT_TCP_PORT);
                    Log.i(TAG, "connect client successfully");
                    DataOutputStream oStream = new DataOutputStream(socket.getOutputStream());
                    Log.i(TAG, "write " + buffer.length + " bytes");
                    oStream.write(buffer);
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
        }
    }

    public void sendEventToClient(byte[] buffer){
        new Thread(new SendEventToClientThread(buffer)).start();
    }

    class SendEventToClientThread implements Runnable{
        byte[] buffer;
        public SendEventToClientThread(byte[] _buf){
            buffer = _buf;
        }
        @Override
        public void run() {
            try {
                DatagramSocket socket = new DatagramSocket();
                for (InetAddress clientAddr : deviceMap.keySet()) {
                    if (address == clientAddr)
                        continue; // server self
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length,
                            clientAddr, Device.CLIENT_UDP_PORT);
                    socket.send(packet);
                    Log.i(TAG, "send event to client " + clientAddr.toString());
                }
            }catch (IOException e) {

            }
        }
    }

    @Override
    public void closeSocket(){
        super.closeSocket();
        if(beaconSocket != null){
            new Thread(new CloseServerSocketThread()).start();
        }
    }
    class CloseServerSocketThread implements Runnable{
        public void run(){
            if (beaconSocket != null){
                beaconSocket.close();
            }
        }
    }
    
    public void printInfo(){
        Log.i(TAG,"Group name = "+groupName);
        for(InetAddress key : deviceMap.keySet()) {
	        if(key!=null){
		        Log.i(TAG,"   Device: "+key.toString()+" dT="+deviceMap.get(key).bootTimeDiffFromServer);
	        }
        }
    }

    // ===================== functions while calibration ======================
    private CalibrateActivity ca;
    
    public void syncAllTime(CalibrateActivity ca_){ // Only SERVER has to start this process
        ca=ca_;
        new Thread(){
            @Override
            public void run(){
                try{
                    Log.i(TAG,"Start Sync all time");
                    DatagramSocket timeSocket=new DatagramSocket();
                    //timeSocket.setSoTimeout(1000);
                    Log.i(TAG,"   server: "+serverAddr.toString());
                    for(InetAddress key : deviceMap.keySet()){
                        if(key!=null&&!key.equals(serverAddr)){
                            Log.i(TAG,"   Device to sync: "+key.toString());
                            long sendTime,recAckTime;
                            byte[] buffer=new byte[88];
                            new ScreenEvent(ScreenEvent.TIME_SYNC,0,0,0,0,0).writeEventBuffer(buffer,0);
                            new ScreenEvent(ScreenEvent.TIME_SYNC,0,0,0,0,0).writeEventBuffer(buffer,44);
    
                            DatagramPacket timeSyncServerPacket=new DatagramPacket(buffer,buffer.length);
                            timeSyncServerPacket.setSocketAddress(new InetSocketAddress(key,CLIENT_UDP_PORT));
                            sendTime=SystemClock.uptimeMillis();
                            timeSocket.send(timeSyncServerPacket);
    
                            Log.i(TAG,"   Sync data send from server");
                            timeSyncServerPacket=new DatagramPacket(buffer,buffer.length);
                            timeSocket.receive(timeSyncServerPacket);
                            recAckTime=SystemClock.uptimeMillis();
                            
                            buffer=timeSyncServerPacket.getData();
                            ScreenEvent test1=new ScreenEvent(buffer,0);
                            ScreenEvent test2=new ScreenEvent(buffer,44);
    
                            long deltaT=(test1.timestamp+test2.timestamp-sendTime-recAckTime)/2;
                            deviceMap.get(key).bootTimeDiffFromServer=deltaT;
                            Log.i(TAG,"   Time Sync Data From Device:"+key
                                +" Received, ts="+sendTime+"tr="+test1.timestamp
                                +" ta="+test2.timestamp+" tra="+recAckTime+" dT="+deltaT);
                            
                        }
                    }
                    isTimeSync=true;
                    Message msg=new Message();
                    msg.what=2;
                    ca.handler.sendMessage(msg);
                    printInfo();
                    //startReceiveCalibrateResult(ca); // for Device
                    receiveCalibrationData(ca); // for Client Device
                }catch(IOException e){
                    Log.e(TAG, "Server Time Initializing Error",e);
                }
            }
        }.start();
    }
    
    public void receiveCalibrationData(CalibrateActivity ca_){ // Only after time sync
        ca=ca_;
        isCalibrated=true;
        Message msg=new Message();
        msg.what=0;
        ca.handler.sendMessage(msg);
        new Thread(new CalibrationDataReceivingThread()).start();
    }
    
    private class CalibrationDataReceivingThread implements Runnable{
        DatagramPacket touchEventPacket;
        DatagramPacket ackPacket;
        @Override
        public void run(){
            byte[] buffer= new byte[88];
            try{
                touchEventPacket=new DatagramPacket(buffer,buffer.length);
                ScreenEvent evLastStart=null,evLastEnd=null;
                ScreenEvent evNowStart=null,evNowEnd=null;
                long lastStartTime=0,lastEndTime=0;
                long nowStartTime=0,nowEndTime=0;
                InetSocketAddress lastAddr=null,nowAddr=null;
                
                int calibratedDeviceCnt=1;
                Log.i(TAG,"Receive Calib Data Initialized, start calibrating");
                
                while(status==Device.CALIBRATE_STATUS){
                    
                    if(deviceMap.size()==1){ // Only Server
                        Message msg=new Message();
                        msg.what=3;
                        ca.handler.sendMessage(msg);
                        break;
                    }
                    
                    try{
                        while (udpSocket == null){
                            try{
                                Thread.sleep(50);
                            }catch (InterruptedException e){
                                Log.e(TAG, "interrupted", e);
                            }

                        }
                        udpSocket.setSoTimeout(0);
                        udpSocket.receive(touchEventPacket);
                    }catch(SocketTimeoutException e){
                        // Do Nothing
                        Log.i(TAG,"Server time out, restarting ...");
                        continue;
                    }
                    buffer=touchEventPacket.getData();
                    ScreenEvent evNowStartTemp=new ScreenEvent(buffer,0);
                    ScreenEvent evNowEndTemp=new ScreenEvent(buffer,44);
                    // if this is omitted, LOTS of garbage packets will be received ! WHY ?
                    if(evNowStartTemp.type!=ScreenEvent.MOVE||evNowEndTemp.type!=ScreenEvent.MOVE)continue;
                    
                    
                    evLastStart=evNowStart;evLastEnd=evNowEnd;
                    evNowStart=evNowStartTemp;evNowEnd=evNowEndTemp;
                    lastAddr=nowAddr;
                    nowAddr=new InetSocketAddress(touchEventPacket.getAddress(),touchEventPacket.getPort());
                    long dT=deviceMap.get(nowAddr.getAddress()).bootTimeDiffFromServer;
                    lastStartTime=nowStartTime;nowStartTime=evNowStart.timestamp-dT;
                    lastEndTime=nowEndTime;nowEndTime=evNowEnd.timestamp-dT;
    
                    //Log.i(TAG,"Receive Event st: "+evNowStart);
                    //Log.i(TAG,"Receive Event ed: "+evNowEnd);
                    //Log.i(TAG,"Start Time = "+nowStartTime);
                    //Log.i(TAG,"End Time   = "+nowEndTime);
                    
                    double vLastEnd;
                    double vNowStart;
                    double vDiffRatio;
                    Device lastDevice;
                    Device nowDevice;
                    try{
                        if(evLastStart==null){ // Only received 1 packet
                            throw new DataFormatException("First Calib Motion Datapack");
                        }
                        if(nowAddr.equals(lastAddr)){ // from same device
                            throw new DataFormatException("Same Device");
                        }
                        if(nowStartTime-lastEndTime>1000){
                            throw new DataFormatException("Interval Too Long");
                        }
                        vLastEnd=Math.hypot(evLastEnd.velX,evLastEnd.velY);
                        vNowStart=Math.hypot(evNowStart.velX,evNowStart.velY);
                        vDiffRatio=Math.abs(vLastEnd-vNowStart)/Math.max(vLastEnd,vNowStart);
                        if(vDiffRatio>0.15){
                            throw new DataFormatException("Unstable Connection: "+vDiffRatio);
                        }
                        lastDevice=deviceMap.get(lastAddr.getAddress());
                        nowDevice=deviceMap.get(nowAddr.getAddress());
                        /*if(lastDevice.isCalibrated==nowDevice.isCalibrated){
                            throw new DataFormatException("Both/None Calibrated");
                        }*/
                        if(!lastDevice.isCalibrated&&!nowDevice.isCalibrated){
                            throw new DataFormatException("Both Not Calibrated");
                        }
                    }catch(DataFormatException e){
                        Log.i(TAG,e.getMessage());
                        continue;
                    }
                    
                    if(lastDevice.isCalibrated&&!nowDevice.isCalibrated){ // from old to new
                        calibratedDeviceCnt++;
                        
                        double v_avg=(vLastEnd+vNowStart)/2;
                        double dis=v_avg*(nowStartTime-lastEndTime);
                        
                        double v1_a=Math.atan2(evLastEnd.velY,evLastEnd.velX);
                        Coordinate v1=new Coordinate(evLastEnd.posX,evLastEnd.posY,v1_a);
                        double v2_a=Math.atan2(evNowStart.velY,evNowStart.velX);
                        Coordinate v2=new Coordinate(evNowStart.posX,evNowStart.posY,v2_a);
                        Coordinate nowCoord=CoordinateMatch.match(v1,v2,dis);
                        
                        nowDevice.isCalibrated=true;
                        Log.i(TAG,"DIS = "+dis+" Coord = "+nowCoord);
                        Log.i(TAG,"Calib Successful vdiff="+vDiffRatio);
                        Log.i(TAG,"Send Success to "+nowAddr);
                        nowDevice.posX=nowCoord.x;
                        nowDevice.posY=nowCoord.y;
                        nowDevice.angle=nowCoord.a;
    
                        ScreenEvent ackEvent=new ScreenEvent(ScreenEvent.CALIB_OK,0,nowCoord.x,nowCoord.y,nowCoord.a,0);
                        ackEvent.writeEventBuffer(buffer,0); // write twice for fun
                        ackEvent.writeEventBuffer(buffer,44);
                        ackPacket=new DatagramPacket(buffer,buffer.length); // Now Successful
                        ackPacket.setSocketAddress(nowAddr);
                        udpSocket.send(ackPacket);
                    }
                    
                    if(calibratedDeviceCnt==deviceMap.size()){
                        Log.i(TAG,"All Devices Calibrated");
                        for(InetAddress key : deviceMap.keySet()){
                            if(key!=null&&!key.equals(serverAddr)){
                                Log.i(TAG,"Send All Complete to "+key);
                                new ScreenEvent(ScreenEvent.ALL_CALIB_OK,0,0,0,0,0).writeEventBuffer(buffer,0);
                                new ScreenEvent(ScreenEvent.ALL_CALIB_OK,0,0,0,0,0).writeEventBuffer(buffer,44);
            
                                DatagramPacket successPacket=new DatagramPacket(buffer,buffer.length);
                                successPacket.setSocketAddress(new InetSocketAddress(key,CLIENT_UDP_PORT));
    
                                udpSocket.send(successPacket);
                            }
                        }
    
                        Log.i(TAG,"Send Complete Finish");
                        Message msg=new Message();
                        msg.what=3;
                        ca.handler.sendMessage(msg);
                        break;
                    }
                }
            }catch(IOException e){
                Log.i(TAG,"Receiving Calibration Data Initializing failed");
                e.printStackTrace();
            }
        }
    }
}
