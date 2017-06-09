package com.iraka.widget;

import com.ling.screen.Device;

/**
 * Created by Iraka Crow on 2017/6/7.
 */

public class Coordinate{
	
	public final double x;
	public final double y;
	public final double a;
	
	public Coordinate(double x_,double y_){
		x=x_;y=y_;a=0;
	}
	
	public Coordinate(double x_,double y_,double a_){
		x=x_;y=y_;a=a_;
	}
	
	/*
		local :
			this.x, this.y : in pixel
			this.a : in radian, CW
		return global :
			x, y : in mm
			a : in radian, CW
	*/
	public Coordinate toGlobal(Coordinate deviceGlobalPose){
		double x_mm=x/Device.ppmX;
		double y_mm=y/Device.ppmY;
		
		double cos=Math.cos(deviceGlobalPose.a);
		double sin=Math.sin(deviceGlobalPose.a);
		
		double x_gl=x_mm*cos-y_mm*sin+deviceGlobalPose.x;
		double y_gl=x_mm*sin+y_mm*cos+deviceGlobalPose.y;
		double a_gl=a+deviceGlobalPose.a;
		
		return new Coordinate(x_gl,y_gl,a_gl);
	}
	
	/*
		global :
			this.x, this.y : in mm
			this.a : in radian, CW
		return local :
			x, y : in pixel
			a : in radian, CW
	*/
	public Coordinate toLocal(Coordinate deviceGlobalPose){
		double x_delta_mm=x-deviceGlobalPose.x;
		double y_delta_mm=y-deviceGlobalPose.y;
		double a_lc=a-deviceGlobalPose.a;
		
		double cos=Math.cos(-deviceGlobalPose.a);
		double sin=Math.sin(-deviceGlobalPose.a);
		
		double x_mm=x_delta_mm*cos-y_delta_mm*sin;
		double y_mm=x_delta_mm*sin+y_delta_mm*cos;
		
		double x_lc=x_mm*Device.ppmX;
		double y_lc=y_mm*Device.ppmY;
		
		return new Coordinate(x_lc,y_lc,a_lc);
	}
	public Coordinate toLocal2(Device device){
		double p = Math.sqrt((x * x) + (y * y));
		double q = Math.atan(y/x);
		q = q - device.angle;
		Coordinate coord = new Coordinate(p * Math.cos(q), p * Math.sin(q));
		return coord;
	}
	@Override
	public String toString(){
		return "Position = ("+x+","+y+") Angle = "+a+" rad";
	}
}
