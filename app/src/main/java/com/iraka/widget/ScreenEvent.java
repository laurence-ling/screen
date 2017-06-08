package com.iraka.widget;

import android.view.MotionEvent;

import java.io.Serializable;

/**
 * Created by Iraka Crow on 2017/6/8.
 */

public class ScreenEvent implements Serializable{
	
	public static final int DOWN=MotionEvent.ACTION_DOWN;
	public static final int MOVE=MotionEvent.ACTION_MOVE;
	public static final int UP=MotionEvent.ACTION_UP;
	public static final int CANCEL=MotionEvent.ACTION_CANCEL;
	
	public final int type;
	public final long timestamp;
	public final double posX,posY; // in mm
	public final double velX,velY; // in mm
	
	// Transfer a local touch event into a ScreenEvent object
	public ScreenEvent(MotionEvent ev,Coordinate deviceCoord){
		type=ev.getAction();
		timestamp=ev.getEventTime();
		Coordinate pos=(new Coordinate(ev.getX(),ev.getY())).toGlobal(deviceCoord);
		posX=pos.x;
		posY=pos.y;
		velX=0;
		velY=0;
	}
	
	// Manually construct a ScreenEvent
	public ScreenEvent(int type_,long timestamp_,double posX_,double posY_,double velX_,double velY_){
		type=type_;
		timestamp=timestamp_;
		posX=posX_;posY=posY_;
		velX=velX_;velY=velY_;
	}
	
	
	// Decode an event from a buffer at certain position (at least 44 bytes)
	public ScreenEvent(byte[] eventBuffer,int pos){
		type=getInt(eventBuffer,pos);
		timestamp=getLong(eventBuffer,pos+4);
		posX=getDouble(eventBuffer,pos+12);posY=getDouble(eventBuffer,pos+20);
		velX=getDouble(eventBuffer,pos+28);velY=getDouble(eventBuffer,pos+36);
	}
	private static int getInt(byte[] b,int pos){ // Little-Endian
		int r=0;
		for(int i=0;i<4;i++){
			r|=(b[pos+i]&0xFF)<<(8*i);
		}
		return r;
	}
	private static long getLong(byte[] b,int pos){ // Little-Endian
		long r=0;
		for(int i=0;i<8;i++){
			r|=(long)(b[pos+i]&0xFF)<<(8*i);
		}
		return r;
	}
	private static double getDouble(byte[] b,int pos){
		return Double.longBitsToDouble(getLong(b,pos));
	}
	
	// Encode an event to a buffer of least length 44
	public void writeEventBuffer(byte[] eventBuffer,int pos){
		toIntBuffer(eventBuffer,pos,type);
		toLongBuffer(eventBuffer,pos+4,timestamp);
		toDoubleBuffer(eventBuffer,pos+12,posX);toDoubleBuffer(eventBuffer,pos+20,posY);
		toDoubleBuffer(eventBuffer,pos+28,velX);toDoubleBuffer(eventBuffer,pos+36,velY);
	}
	private static void toIntBuffer(byte[] b,int pos,int v){ // Little-Endian
		for(int i=0;i<4;i++){
			b[pos+i]=(byte)((v>>>(8*i))&0xFF);
		}
	}
	private static void toLongBuffer(byte[] b,int pos,long v){ // Little-Endian
		for(int i=0;i<8;i++){
			b[pos+i]=(byte)((v>>>(8*i))&0xFF);
		}
	}
	private static void toDoubleBuffer(byte[] b,int pos,double v){
		toLongBuffer(b,pos,Double.doubleToLongBits(v));
	}

	@Override
	public String toString(){
		return "TYPE = "+type+" timestamp = "+timestamp
		+" pos = ("+posX+","+posY+") vel = ("+velX+","+velY+")";
	}
}
