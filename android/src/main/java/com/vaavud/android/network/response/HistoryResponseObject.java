package com.vaavud.android.network.response;

import com.vaavud.android.model.entity.MeasurementPoint;
import com.vaavud.android.model.entity.MeasurementSession;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class HistoryResponseObject implements Serializable {


	private String uuid;
    private Date startTime;
    private Date endTime;
    private Double latitude;
    private Double longitude;
    private Float windSpeedAvg;
    private Float windSpeedMax;
    private Float windDirection;
    private ResponsePointObject[] points;
    
    public HistoryResponseObject() {
    }
    
    public HistoryResponseObject(MeasurementSession measurementSession) {
    	this.uuid = measurementSession.getUuid();
    	this.startTime = measurementSession.getStartTime();
    	this.endTime = measurementSession.getEndTime();
    	if (measurementSession.getPosition() != null &&
    			measurementSession.getPosition().getLatitude() != null && measurementSession.getPosition().getLongitude() != null &&
    			measurementSession.getPosition().getLatitude() != 0D && measurementSession.getPosition().getLongitude() != 0D) {
    		this.latitude = measurementSession.getPosition().getLatitude();
    		this.longitude = measurementSession.getPosition().getLongitude();
    	}
    	this.windSpeedAvg = measurementSession.getWindSpeedAvg();
    	this.windSpeedMax = measurementSession.getWindSpeedMax();
    	this.windDirection = measurementSession.getWindDirection();
    	
    	List<MeasurementPoint> originalPoints = measurementSession.getPoints();
    	List<ResponsePointObject> points = new ArrayList<ResponsePointObject>(originalPoints.size());
    	
    	if (originalPoints.size() > 1000) {
//    		Log.i("HistoryResponseObject", "History service requesting session with more than 1000 points, skipping");
    	}
    	else {
	    	for (MeasurementPoint point : originalPoints) {
	    		if (point.getTime() != null && point.getWindSpeed() != null && point.getWindSpeed() >= 0.0) {
	    			points.add(new ResponsePointObject(point));
	    		}
	    	}
    	}
    	this.points = points.toArray(new ResponsePointObject[points.size()]);
    }
    
	public String getUuid() {
		return uuid;
	}
	
	public void setUuid(String uuid) {
		this.uuid = uuid;
	}
	
	public Date getStartTime() {
		return startTime;
	}
	
	public void setStartTime(Date startTime) {
		this.startTime = startTime;
	}
		
	public Date getEndTime() {
		return endTime;
	}

	public void setEndTime(Date endTime) {
		this.endTime = endTime;
	}
	public void setEndTime(Long endTime){
		this.endTime = new Date(endTime);
	}

	public Double getLatitude() {
		return latitude;
	}
	
	public void setLatitude(Double latitude) {
		this.latitude = latitude;
	}
	
	public Double getLongitude() {
		return longitude;
	}
	
	public void setLongitude(Double longitude) {
		this.longitude = longitude;
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

	public ResponsePointObject[] getPoints() {
		return points;
	}

	public void setPoints(ResponsePointObject[] points) {
		this.points = points;
	}

	public void setStartTime(Long startTime) {
		this.startTime = new Date(startTime);
	}
	
	public String toString(){
		return uuid+" "+startTime.toString()+" "+endTime.toString()+" "+windSpeedAvg+" "+windSpeedMax+" "+windDirection;
	}
}