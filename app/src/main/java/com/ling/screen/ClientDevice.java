package com.ling.screen;

import android.os.Bundle;
import android.os.Message;
import android.util.Log;

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
import java.util.HashMap;
import java.util.Map;

/**
 * Created by ling on 2017/5/23.
 */

public class ClientDevice extends Device {
    private static final String TAG = "Client Device";
    InetAddress serverAddr; // server's udp address
    ServerSocket listenSocket; // tcp socket for receive file
    AddGroupActivity agActivity;

    public ClientDevice(){
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
        int round = 10;
        public ConnectServerThread(InetSocketAddress addr){
            saddr = addr;
        }
        @Override
        public void run(){
            byte[] buf = new byte[100];
            recvPacket = new DatagramPacket(buf, buf.length);
            String msg = "2@";
            while (round-- > 0) { // try sending 10 times
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
    class AcceptFileThread implements Runnable{
        @Override
        public void run(){
            try {
                listenSocket = new ServerSocket(CLIENT_TCP_PORT);
                Socket soket = listenSocket.accept();
            } catch (IOException e) {
                Log.e(TAG, "listen socket error", e);
            }
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
