package com.vaavud.android.network.response;

import com.vaavud.android.model.entity.MeasurementPoint;

import java.io.Serializable;
import java.util.Date;

public class ResponsePointObject implements Serializable {

	private Date time;
	private Float speed;
	
	public ResponsePointObject(Date time, Float speed) {
		this.time = time;
		this.speed = speed;
	}

	public ResponsePointObject(MeasurementPoint point) {
		this.time = point.getTime();
		this.speed = point.getWindSpeed();
	}

	public Date getTime() {
		return time;
	}
	
	public void setTime(Date time) {
		this.time = time;
	}
	
	public Float getSpeed() {
		return speed;
	}
	
	public void setSpeed(Float speed) {
		this.speed = speed;
	}
}
