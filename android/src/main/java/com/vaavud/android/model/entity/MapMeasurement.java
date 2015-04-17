package com.vaavud.android.model.entity;

import java.io.Serializable;
import java.util.Date;

public class MapMeasurement implements Serializable {

	private com.google.android.gms.maps.model.LatLng position;
	private Date startTime;
	private Float windSpeedAvg;
	private Float windSpeedMax;
	private Float windDirection;
	
	public MapMeasurement() {
	}
	
	public MapMeasurement(double latitude, double longitude, Date startTime, Float windSpeedAvg, Float windSpeedMax,Float windDirection) {
		this.position = new com.google.android.gms.maps.model.LatLng(latitude, longitude);
		this.startTime = startTime;
		this.windSpeedAvg = windSpeedAvg;
		this.windSpeedMax = windSpeedMax;
		this.windDirection = windDirection;
	}
	
	public com.google.android.gms.maps.model.LatLng getPosition() {
		return position;
	}

	public void setPosition(com.google.android.gms.maps.model.LatLng position) {
		this.position = position;
	}

	public Date getStartTime() {
		return startTime;
	}
	
	public void setStartTime(Date startTime) {
		this.startTime = startTime;
	}
	
	public Float getWindSpeedAvg() {
		return windSpeedAvg;
	}
	
	public void setWindSpeedAvg(Float windSpeedAvg) {
		this.windSpeedAvg = windSpeedAvg;
	}
	
	public Float getWindSpeedMax() {
		return windSpeedMax;
	}
	
	public void setWindSpeedMax(Float windSpeedMax) {
		this.windSpeedMax = windSpeedMax;
	}

	public Float getWindDirection() {
		return windDirection;
	}

	public void setWindDirection(Float windDirection) {
		this.windDirection = windDirection;
	}
}
