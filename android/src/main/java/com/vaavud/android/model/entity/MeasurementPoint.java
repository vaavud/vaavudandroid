package com.vaavud.android.model.entity;

import android.database.Cursor;

import java.io.Serializable;
import java.util.Date;

public class MeasurementPoint implements Serializable {

	private Long localId;
	private MeasurementSession session;
	private Date time;
	private Float windSpeed;
	private Float windDirection;

	public MeasurementPoint() {
	}
	
    public MeasurementPoint(MeasurementSession measurementSession, Cursor cursor) {
    	super();
    	
    	this.session = measurementSession;
    	
		setLocalId(cursor.getLong(0));
		if (!cursor.isNull(2)) {
			setTime(new Date(cursor.getLong(2)));
		}
		if (!cursor.isNull(3)) {
			setWindSpeed(cursor.getFloat(3));
		}
		if (!cursor.isNull(4)) {
			setWindDirection(cursor.getFloat(4));
		}
    }

	public Long getLocalId() {
		return localId;
	}

	public void setLocalId(Long id) {
		this.localId = id;
	}

	public MeasurementSession getSession() {
		return session;
	}

	public void setSession(MeasurementSession session) {
		this.session = session;
	}

	public Date getTime() {
		return time;
	}

	public void setTime(Date time) {
		this.time = time;
	}

	public Float getWindSpeed() {
		return windSpeed;
	}

	public void setWindSpeed(Float windSpeed) {
		this.windSpeed = windSpeed;
	}

	public Float getWindDirection() {
		return windDirection;
	}

	public void setWindDirection(Float windDirection) {
		this.windDirection = windDirection;
	}
}
