package com.vaavud.android.measure.sensor;

import com.vaavud.android.model.entity.MagneticSession;
import com.vaavud.android.model.entity.WindMeasurement;

import java.util.ArrayList;
import java.util.List;


public class DataManager {
	
	private List<WindMeasurement> windspeedMeasurements;
	private List<Float[]> magneticfieldMeasurements;
	
	private int uploadCounter =0;
	private int counter=0;
	private int lastServedWindmeasurement = 0;
	private Double windSpeedAverage = 0.0d;
	private Float windSpeedMax = 0.0f;
	private Float windDirection;
	private boolean measureIsValid = true;
	
	
	public DataManager() {
		windspeedMeasurements = new ArrayList<WindMeasurement>();
		magneticfieldMeasurements = new ArrayList<Float[]>();		
	}
	
	public void resetData() {
		magneticfieldMeasurements = new ArrayList<Float[]>();
		uploadCounter = 0;
		counter = 0;
		windSpeedMax = 0.0f;
		windSpeedAverage = 0.0d;
		windDirection = null;
	}
	
	public void addMagneticFieldReading(Float[] magReading) {
		magneticfieldMeasurements.add(magReading);
	}
	
	public void addWindMeasurement(WindMeasurement windMeasurement) {
		if (measureIsValid) {
			counter++;
			windspeedMeasurements.add(windMeasurement);
			windSpeedAverage = windSpeedAverage + ((windMeasurement.windspeed - windSpeedAverage)/counter);
			if (windSpeedMax < windMeasurement.windspeed) windSpeedMax = windMeasurement.windspeed;
			windDirection = windMeasurement.windDirection;
//			Log.d("DataManager","Add Wind Measurement: "+counter);
		}
	}
	
	public List<Float[]> getLastXMagneticfieldMeasurements(Integer numberOfMeasurements ) {
		
		int listSize = magneticfieldMeasurements.size();
		List<Float[]> magneticfieldMeasurementsList;
		
		if (listSize > numberOfMeasurements) {
			magneticfieldMeasurementsList = magneticfieldMeasurements.subList(listSize - numberOfMeasurements, listSize);
			
			return magneticfieldMeasurementsList;
		}
		else {
			
			magneticfieldMeasurementsList = new ArrayList<Float[]>();
			magneticfieldMeasurementsList.addAll(magneticfieldMeasurements);
			
			return magneticfieldMeasurementsList;
		}
	}
	
	public List<Float[]> getMagneticfieldMeasurements() {
		return magneticfieldMeasurements;
	}
	
	public MagneticSession getNewMagneticfieldMeasurements(String measurementSessionUuid) {
		
		int listSize = magneticfieldMeasurements.size();		
		if (listSize != uploadCounter) {
			List<Float[]> newMagneticfieldMeasurements = magneticfieldMeasurements.subList(uploadCounter, listSize);
			
			int startIndex = uploadCounter;
			uploadCounter = listSize;
			
			return new MagneticSession(measurementSessionUuid, startIndex, listSize, newMagneticfieldMeasurements);
		}
		else {
			return null;
		}
	}
	
	public void clearData() {
		
		windspeedMeasurements = new ArrayList<WindMeasurement>();
		magneticfieldMeasurements = new ArrayList<Float[]>();
		uploadCounter = 0;
		lastServedWindmeasurement = 0;
	}
	
	public Float getTimeSinceStart() {
		if (magneticfieldMeasurements.size() >= 1) {
			return magneticfieldMeasurements.get(magneticfieldMeasurements.size() -1)[0];
		}
		else {
			return 0f;
		}
	}
	
	public Integer getNumberOfMeassurements() {
		return magneticfieldMeasurements.size();
	}
	
	public String getNumberOfMeasurementsString() {
		return Integer.toString(magneticfieldMeasurements.size());
	}
	
	public Float getLastMagX() {
		if (magneticfieldMeasurements.size() >= 1) {
			return magneticfieldMeasurements.get(magneticfieldMeasurements.size() -1)[1];
		}
		else {
			return null;
		}
	}
	
	public Float getLastWindspeed() {
		if (windspeedMeasurements.size() >= 1) {
			return windspeedMeasurements.get(windspeedMeasurements.size() -1).windspeed;
		}
		else {
			return null;
		}	
	}
	
	public Double getAverageWindspeed() {
//		if (windspeedMeasurements.size() >= 1) {
//			
//			double sum = 0D;
//			for (WindMeasurement measurement : windspeedMeasurements) {
////				Log.d("DataManager","===== Wind speed: "+ measurement.windspeed);
//				sum += measurement.windspeed;
//			}
////			Log.d("DataManager", "Sum Measurements: "+sum);
//			return sum / ((double) windspeedMeasurements.size());
//		}
//		else {
//			return null;
//		}
		return windSpeedAverage;
	}
	
	public Float getMaxWindspeed() {
//		if (windspeedMeasurements.size() >= 1) {
//			float max = 0F;
//			for (WindMeasurement measurement : windspeedMeasurements) {
//				if (measurement.windspeed > max) {
//					max = measurement.windspeed;
//				}
//			}
//			return max;
//		}
//		else {
//			return null;
//		}
		return windSpeedMax;
	}
	
	public Float  getLastTime() {
		if (windspeedMeasurements.size() >= 1) {
			return windspeedMeasurements.get(windspeedMeasurements.size() -1).time;
		}
		else {
			return null;
		}
	}
	
	public Float[] getLastTimeAndWindspeed() {
		return new Float[] {getLastTime() , getLastWindspeed()};
	}
	
	public Float getLastWindDirection() {		
//		if (windspeedMeasurements.size() >= 1) {
//			return windspeedMeasurements.get(windspeedMeasurements.size() -1).windDirection;
//		}
//		else {
//			return null;
//		}
		return windDirection;
	}
	
	public Float[] getLatestNewTimeAndWindspeed() {
		
		if (windspeedMeasurements.size() > lastServedWindmeasurement) {
			lastServedWindmeasurement = windspeedMeasurements.size();
			return getLastTimeAndWindspeed();
		}
		else {
			return null;
		}	
	}
	
	public boolean newMeasurementsAvailable() {
		return windspeedMeasurements.size() > lastServedWindmeasurement;
	}
	
	public void setMeasureIsValid(boolean isValid) {
		measureIsValid = isValid;
	}

	
}
