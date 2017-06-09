package com.ling.screen;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.icu.util.Measure;
import android.os.Bundle;
import android.os.Message;
import android.util.Log;

import com.datouhou.TouchImageView;
import com.iraka.widget.ScreenEvent;

import java.io.DataInputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

import javax.crypto.spec.DESedeKeySpec;

/**
 * Created by ling on 2017/5/23.
 */

public class ClientDevice extends Device {
    private static final String TAG = "Client Device";
    ServerSocket listenSocket; // tcp socket for receive file
    AddGroupActivity agActivity;
    WorkingActivity wkAcitivity;

    public ClientDevice(){
        super();
        new Thread(new CreateSocketThread()).start();
        status = Device.WAITING_STATUS;
    }

    public void connectServer(InetSocketAddress addr){
        new Thread(new ConnectServerThread(addr)).start();
    }
    public void findServer(AddGroupActivity _agAcitivity){
        agActivity = _agAcitivity;
        while(udpSocket == null){
            try{
                Thread.sleep(20);
            }catch(InterruptedException e){
                Log.e(TAG, "sleep interrupted", e);
            }
        }
        Log.i(TAG, "find server started");
        new Thread(new FindServerThread()).start();
    }

    class FindServerThread implements Runnable{
        DatagramPacket packet;
        public FindServerThread(){
        }
        @Override
        public void run(){
            while(status == Device.WAITING_STATUS) {
                try {
                    byte[] buf = new byte[100];
                    packet = new DatagramPacket(buf, buf.length);
                    udpSocket.setSoTimeout(100);
                    udpSocket.receive(packet);
                    parsePacket();
                } catch (SocketTimeoutException e) {

                } catch (IOException e) {
                    Log.e(TAG, "receive errorr", e);
                }
            }
        }
        public void parsePacket(){
            int len = packet.getLength();
            String recvStr = new String(packet.getData()).substring(0, len);
            String type = recvStr.split("@")[0];
            String serverName = recvStr.split("@")[1];
            Log.i(TAG, "find server " + serverName);
            InetSocketAddress saddr = new InetSocketAddress(packet.getAddress(), packet.getPort());
            if(!agActivity.serverMap.containsKey(serverName)) {
                agActivity.serverMap.put(serverName, saddr);
                Message msg = new Message();
                msg.what = 1;  //refresh server list
                agActivity.handler.sendMessage(msg);
            }
        }
    }
    class ConnectServerThread implements Runnable{
        DatagramPacket sendPacket;
        DatagramPacket recvPacket;
        InetSocketAddress saddr;
        int round = 3;
        public ConnectServerThread(InetSocketAddress addr){
            saddr = addr;
        }
        @Override
        public void run(){
            byte[] buf = new byte[100];
            recvPacket = new DatagramPacket(buf, buf.length);
            String msg = "2@";
            while (round-- > 0 && status == Device.WAITING_STATUS) { // try sending 10 times
                try {
                    sendPacket = new DatagramPacket(msg.getBytes(), msg.length(), saddr);
                    udpSocket.send(sendPacket);

                    udpSocket.setSoTimeout(100);
                    udpSocket.receive(recvPacket);
                    String recvStr = new String(recvPacket.getData());
                    String type = recvStr.split("@")[0];
                    if (type.equals("3")) {// receive ack sucessfully
                        serverAddr = saddr.getAddress();
                        udpSocket.setSoTimeout(0);
                        status = Device.CALIBRATE_STATUS;
                        Message sucMsg = new Message();
                        sucMsg.what = 2;
                        agActivity.handler.sendMessage(sucMsg);
                        break;
                    }
                }catch (SocketTimeoutException e){

                }catch (IOException e) {
                    Log.e(TAG, "connect server error", e);
                }
            }
        }
    }
    public void receiveServerEvent(WorkingActivity _wkActivity){
        wkAcitivity = _wkActivity;
        Log.i(TAG, "start receive server event");
        new Thread(new ReceiveServerEventThread()).start();

    }
    class ReceiveServerEventThread implements Runnable{
        @Override
        public void run(){
            Log.i(TAG, "receive event thread started");
            while(true) {
                byte[] buf = new byte[128];
                DatagramPacket recvPacket = new DatagramPacket(buf, buf.length);
                try{
                    wkAcitivity.myDevice.udpSocket.receive(recvPacket);
                    ScreenEvent event = new ScreenEvent(recvPacket.getData(), 0);
                    Log.i(TAG, "receive data " + recvPacket.getLength() + "bytes");
                    wkAcitivity.screenEvent = event;
                } catch(IOException e){
                    Log.e(TAG, "", e);
                }
                Message msg = new Message();
                msg.what = 2;
                wkAcitivity.handler.sendMessage(msg);
            }
        }
    }
    public void acceptFile(WorkingActivity _wkActivity){
        wkAcitivity = _wkActivity;
        new Thread(new AcceptFileThread()).start();
    }
    class AcceptFileThread implements Runnable{
        @Override
        public void run(){
            try {
                Log.i(TAG, "create listen socket");
                listenSocket = new ServerSocket(Device.CLIENT_TCP_PORT);
                Socket socket = listenSocket.accept();
                Log.i(TAG, "accept file from " + socket.getInetAddress());
                DataInputStream iStream = new DataInputStream(socket.getInputStream());
                byte[] barray = new byte[Device.MAX_IMAGE_SIZE];
                byte[] temp = new byte[4096];
                int len, totalSize = 0;
                while((len = iStream.read(temp)) > 0){
                    Log.i(TAG, "receive bytes len " + len);
                    System.arraycopy(temp, 0, barray, totalSize, len);
                    totalSize += len;
                    temp = new byte[4096];
                }
                Log.i(TAG, "total len " + totalSize);
                bitmap = BitmapFactory.decodeByteArray(barray, 0, totalSize);
                Log.i(TAG, "recover bit map " + bitmap.getHeight()+"*"+bitmap.getWidth());

            } catch (IOException e) {
                Log.e(TAG, "listen socket error", e);
            }
            Message mapMsg = new Message();
            mapMsg.what = 1;
            wkAcitivity.handler.sendMessage(mapMsg);
        }
    }
    @Override
    public void closeSocket(){
        super.closeSocket();
        if(listenSocket != null){
            new Thread(new CloseClientSocketThread()).start();
        }
    }
    class CloseClientSocketThread implements Runnable{
        public void run(){
            try {
                listenSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "close listen socket error", e);
            }
        }
    }
}
