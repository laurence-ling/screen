package com.ling.screen;

import android.content.Context;
import android.net.DhcpInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Message;
import android.util.Log;

import java.io.IOException;
import java.net.*;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by ling on 2017/5/23.
 */

public class ServerDevice extends Device {
    private static final String TAG = "Server Device";
    String groupName;
    Map<InetAddress, Device> deviceMap;
    DatagramSocket beaconSocket; //server only socket, non-bind
    Thread beaconThread;
    private NewGroupActivity ngActivity;

    public ServerDevice(){
        deviceMap = new HashMap<>();
        deviceMap.put(this.address, this);
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
                    Thread.sleep(100);
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

    class SendFileThread implements Runnable{
        @Override
        public void run() {
            for (InetAddress clientAddr : deviceMap.keySet())
                try {
                    Socket socket = new Socket(clientAddr, CLIENT_TCP_PORT);
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
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
}