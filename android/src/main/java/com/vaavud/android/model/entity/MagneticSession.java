package com.vaavud.android.model.entity;

//import java.util.ArrayList;
import java.util.List;

public class MagneticSession {

	private String measurementSessionUuid;
	private int startIndex;
	private int endIndex;
	private List<Float[]> points;
	
	public MagneticSession() {
	}
	
	public MagneticSession(String measurementSessionUuid, int startIndex, int endIndex, List<Float[]> points) {
		this.measurementSessionUuid = measurementSessionUuid;
		this.startIndex = startIndex;
		this.endIndex = endIndex;
		this.points = points;
	}
	
	public String getMeasurementSessionUuid() {
		return measurementSessionUuid;
	}

	public void setMeasurementSessionUuid(String measurementSessionUuid) {
		this.measurementSessionUuid = measurementSessionUuid;
	}

	public int getStartIndex() {
		return startIndex;
	}

	public void setStartIndex(int startIndex) {
		this.startIndex = startIndex;
	}

	public int getEndIndex() {
		return endIndex;
	}

	public void setEndIndex(int endIndex) {
		this.endIndex = endIndex;
	}

	public List<Float[]> getPoints() {
		return points;
	}

	public void setPoints(List<Float[]> points) {
		this.points = points;
	}
}
