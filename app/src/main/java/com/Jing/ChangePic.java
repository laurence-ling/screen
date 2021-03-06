package com.Jing;

import android.util.Log;

import com.iraka.widget.Coordinate;
import com.iraka.widget.ScreenEvent;
import com.ling.screen.Device;
import com.ling.screen.ServerDevice;

import java.net.InetAddress;
import java.util.Iterator;
import java.util.Map;

import static com.ling.screen.Device.myDevice;
//import static java.lang.Math.atan;

/**
 * Created by jing on 2017/6/8.
 */

public class ChangePic {
    int finger_num_old = 0;
    double point_old[] = new double[4];
    int finger_num_new = 0;
    double point_new[] = new double[4];
    Device serverDevice;
    public ScreenEvent screenEvent;
    private static final String TAG = "ChangePic";
    
    // for refinement, new data format to send
    public Coordinate bitmapCoord=new Coordinate(0,0,0);
    public double bitmapScale=1;

    public ChangePic(Device server) {
        serverDevice = server;
        finger_num_old = server.finger_num_old;
        point_old[0] = server.point_old[0];
        point_old[1] = server.point_old[1];
        point_old[2] = server.point_old[2];
        point_old[3] = server.point_old[3];
    }

    public void getScreenInfo() {
        if(myDevice==null)return;
        finger_num_old = serverDevice.finger_num_old;
        point_old[0] = serverDevice.point_old[0];
        point_old[1] = serverDevice.point_old[1];
        point_old[2] = serverDevice.point_old[2];
        point_old[3] = serverDevice.point_old[3];
        Device temp_device;
        boolean point1 = true;
        Iterator<Map.Entry<InetAddress, Device>> it = ((ServerDevice) myDevice).deviceMap.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<InetAddress, Device> entry = it.next();
            temp_device = entry.getValue();
            InetAddress address = entry.getKey();
            Log.i(TAG,address.toString());
            Log.w("TIVIS","Addr "+address+" has "+temp_device.finger_num+" fingers");
            if (temp_device.finger_num == 1) {
                if (point1) {
                    finger_num_new=1;
                    point_new[0] = temp_device.point[0];
                    point_new[1] = temp_device.point[1];
                    point1 = false;
                } else {
                    finger_num_new=2;
                    point_new[2] = temp_device.point[0];
                    point_new[3] = temp_device.point[1];
                    break;
                }
            } else if (temp_device.finger_num == 2 && point1) {
                finger_num_new = 2;
                point_new[0] = temp_device.point[0];
                point_new[1] = temp_device.point[1];
                point_new[2] = temp_device.point[2];
                point_new[3] = temp_device.point[3];
                break;
            }
        }
    
        Log.w("TIVIS","cnt="+finger_num_new+" f0 = ("+point_new[0]+","+point_new[1]
        +") f1 = ("+point_new[2]+","+point_new[3]+")");
    }

    public void setScreenInfo(Device server) {
        server.finger_num_old = finger_num_new;

        server.point_old[0] = point_new[0];
        server.point_old[1] = point_new[1];
        server.point_old[2] = point_new[2];
        server.point_old[3] = point_new[3];
    }

    public class Pos {
        double x;
        double y;

        public Pos(double _x, double _y) {
            x = _x;
            y = _y;
        }
    }

    public double Dis(double x1, double y1, double x2, double y2) {
        return Math.hypot(x1 - x2,y1 - y2);
    }

    public double Dis2(Pos pos1, Pos pos2) {
        return Math.hypot(pos1.x - pos2.x,pos1.y - pos2.y);
    }

    public Pos midPos(Pos pos1, Pos pos2) {
        double x = (pos1.x + pos2.x) / 2;
        double y = (pos1.y + pos2.y) / 2;
        return new Pos(x, y);
    }

    public void myrun() { // All global Coord
        Log.i(TAG, "start to change pic");
        finger_num_new=0;
        getScreenInfo();
        //Log.w("TIVIV","finger_num_new="+finger_num_new+" finger_num_old="+finger_num_old);
        
        Log.i(TAG, "start to change pic");
        Log.i(TAG,"finger_num_new "+finger_num_new+": ("+point_new[0]+","+point_new[1]+")~("+point_new[2]+","+point_new[3]+")");
        Log.i(TAG,"finger_num_old "+finger_num_old+": ("+point_old[0]+","+point_old[1]+")~("+point_old[2]+","+point_old[3]+")");
    
        
        if (finger_num_new == 1 && finger_num_old == 1)         //一个手指在屏幕上
        {
            double transX = point_new[0] - point_old[0];
            double transY = point_new[1] - point_old[1];
            screenEvent = new ScreenEvent(0,0, transX, transY,0,1);
    
            bitmapCoord=new Coordinate(bitmapCoord.x+transX,bitmapCoord.y+transY,bitmapCoord.a);
        } else if (finger_num_new == 2 && finger_num_old == 2) {
            Pos posA = new Pos(point_old[0], point_old[1]);
            Pos posB = new Pos(point_old[2], point_old[3]);
            Pos posAA;
            Pos posBB;
            double dis1 = Dis(point_new[0], point_new[1], point_old[0], point_old[1]);
            double dis2 = Dis(point_new[2], point_new[3], point_old[0], point_old[1]);

            if (dis1 < dis2) {
                posAA = new Pos(point_new[0], point_new[1]);
                posBB = new Pos(point_new[2], point_new[3]);
            } else {
                posAA = new Pos(point_new[2], point_new[3]);
                posBB = new Pos(point_new[0], point_new[1]);
            }
            
            Pos mid1 = midPos(posA, posB);
            Pos mid2 = midPos(posAA, posBB);
            double scale = Dis2(posAA, posBB) / Dis2(posA, posB);
            double ang1 = Math.atan2(posA.y - posB.y, posA.x - posB.x);
            double ang2 = Math.atan2(posAA.y - posBB.y, posAA.x - posBB.x);
            double angle = ang2-ang1;
            //long t = System.currentTimeMillis();
            screenEvent = new ScreenEvent(0, 0, mid2.x - mid1.x, mid2.y - mid1.y, angle, scale);
    
            // Translate
            double oX=bitmapCoord.x+screenEvent.posX;
            double oY=bitmapCoord.y+screenEvent.posY;
            // Move to O, rotate / scale around point mid2 (global)
            oX=oX-mid2.x;
            oY=oY-mid2.y;
            // Rotate
            double cosA=Math.cos(angle);
            double sinA=Math.sin(angle);
            double o1X=oX*cosA-oY*sinA;
            double o1Y=oX*sinA+oY*cosA;
            // Scale
            o1X*=scale;
            o1Y*=scale;
            // Move back from O
            oX=o1X+mid2.x;
            oY=o1Y+mid2.y;
    
            // Angle, Scale dispose
            double aNew=bitmapCoord.a+angle;
            double sNew=bitmapScale*scale;
    
            // renew Coord & Scale
            bitmapCoord=new Coordinate(oX,oY,aNew);
            bitmapScale=sNew;
        } else
            screenEvent = new ScreenEvent(0,0,0,0,0,1);
        
        Log.i(TAG,"New_coord : "+bitmapCoord+" scale = "+bitmapScale);
        //Log.i(TAG,"Dev_coord : "+"("+mX+","+mY+")");
        setScreenInfo(serverDevice);
        
    }
}