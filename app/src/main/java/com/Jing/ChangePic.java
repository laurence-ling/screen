package com.Jing;

import com.iraka.widget.ScreenEvent;
import com.ling.screen.ServerDevice;

import static java.lang.Math.atan;

/**
 * Created by jing on 2017/6/8.
 */

public class ChangePic {
    int finger_num_old = 0;
    double point_old[] = new double[4];
    int finger_num_new = 0;
    double point_new[] = new double[4];
    ServerDevice serverDevice;
    ServerDevice tempDevice;
    ScreenEvent screenEvent;
    public ChangePic(ServerDevice server, ServerDevice temp) {
        serverDevice = server;
        tempDevice = temp;
        finger_num_old = server.finger_num;
        finger_num_new = temp.finger_num;
    }
    public class Pos{
        double x;
        double y;
        public Pos(double _x, double _y){
            x = _x;
            y = _y;
        }
    }
    public double Dis(double x1, double y1, double x2, double y2)
    {
        return Math.sqrt((x1-x2)*(x1-x2) + (y1-y2)*(y1-y2));
    }
    public double Dis2(Pos pos1, Pos pos2)
    {
        return Math.sqrt((pos1.x-pos2.x)*(pos1.x-pos2.x) + (pos1.y-pos2.y)*(pos1.y-pos2.y));
    }
    public Pos midPos(Pos pos1, Pos pos2)
    {
        double x = (pos1.x + pos2.x)/2;
        double y = (pos1.y + pos2.y)/2;
        Pos res = new Pos(x, y);
        return res;
    }

    private Runnable myrun = new Runnable() {
        public void run() {
            if(finger_num_new == 1 && finger_num_old == 1)         //一个手指在屏幕上
            {
                if(!(point_new[0] == point_old[0] && point_new[1] == point_old[1]))         //两次位置不同，平移
                {
                    double transX = point_new[0] - point_old[0];
                    double transY = point_new[1] - point_old[1];
                    screenEvent = new ScreenEvent(1, transX, transY);
                }
            }
            else if(finger_num_new == 2 && finger_num_old == 2)
            {
                Pos posA = new Pos(point_old[0], point_old[1]);
                Pos posB = new Pos(point_old[2], point_old[3]);
                Pos posAA;
                Pos posBB;
                double dis1 = Dis(point_new[0], point_new[1], point_old[0], point_old[1]);
                double dis2 = Dis(point_new[2], point_new[3], point_old[0], point_old[1]);

                if(dis1 < dis2){
                    posAA = new Pos(point_new[0], point_new[1]);
                    posBB = new Pos(point_new[2], point_new[3]);
                }
                else{
                    posAA = new Pos(point_new[2], point_new[3]);
                    posBB = new Pos(point_new[0], point_new[1]);
                }
                Pos mid1 = midPos(posA, posB);
                Pos mid2 = midPos(posAA, posBB);
                double scale = Dis2(posAA, posBB)/Dis2(posA, posB);
                double k1 = (posA.y-posB.y)/(posA.x-posB.x);
                double k2 = (posAA.y-posBB.y)/(posAA.x-posBB.x);
                double angle = atan((k2- k1)/(1+ k1*k2));
                long t = System.currentTimeMillis();
                screenEvent = new ScreenEvent(2, t, mid2.x-mid1.x, mid2.y-mid1.y, scale, angle);
            }
        }
    };

}

