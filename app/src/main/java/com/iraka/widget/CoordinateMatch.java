package com.iraka.widget;

/**
 * Created by Iraka Crow on 2017/6/9.
 */

public class CoordinateMatch{
	/*
		Match the second coordinate axis with the first
		return the pose of the second one
		start: the starting point in Coord 1 (global)
		end: the ending point in Coord 2
		dis: the distance between starting & ending point
	 */
	public static Coordinate match(Coordinate start,Coordinate end,double dis){
		double a_2=start.a-end.a;
		double r1=Math.hypot(end.x,end.y);
		double p1=Math.atan2(end.y,end.x);
		double p0=p1+a_2;
		double qX=start.x+dis*Math.cos(start.a);
		double qY=start.y+dis*Math.sin(start.a);
		double o1X=qX-r1*Math.cos(p0);
		double o1Y=qY-r1*Math.sin(p0);
		return new Coordinate(o1X,o1Y,a_2);
	}
}
