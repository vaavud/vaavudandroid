package com.vaavud.android.measure.sensor;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

public class OrientationSensorManager implements SensorEventListener {

	private SensorManager sensorManager;
	private Sensor accelerometer;
	private Sensor magneticSensor;
	private boolean sensorAvailable;

	private float[] acc = new float[3];
	private float[] mag = new float[3];
	private float[] R = new float[9];
	private float[] Ori = new float[3];;
	
	private float oriMinValue;

	public OrientationSensorManager(Context mainContext) {
		sensorManager = (SensorManager) mainContext.getSystemService(Context.SENSOR_SERVICE);
		initializeOrientation();
		if (initializeOrientation()) {
			sensorAvailable = true;
		} else {
			sensorAvailable = false;
		}
		
		this.oriMinValue = (float) (Math.PI/2 - 0.6f);
		
		
	}
	
	private boolean initializeOrientation() {
		accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
		magneticSensor = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
		if (accelerometer != null && magneticSensor != null) {
			return true;
		} else {
			return false;
		}
		
	}
	
	public void start() {
		if (sensorAvailable) {
			sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
			sensorManager.registerListener(this, magneticSensor, SensorManager.SENSOR_DELAY_FASTEST);
		}	
	}
	
	public void stop() {
		sensorManager.unregisterListener(this);
	}
	
	public boolean isSensorAvailable	() {
		return sensorAvailable;
	}
	
	
//	public Float[] getOrientation() {
//		return new Float[]{Ori[0], Ori[1], Ori[2]};
//	}
	
	private void computeOrientation() {
		if (SensorManager.getRotationMatrix(R, null, acc, mag)) {
			SensorManager.getOrientation(R, Ori);
			//Log.v(null, String.valueOf(Ori[1]) + " " + String.valueOf(isVertical()));
		}
	}
	
	public Boolean isVertical() {
		if (!sensorAvailable) {
			return null;
		}
		
		if (Math.abs(Ori[1]) > oriMinValue) {
			return true;
		}
		else {
			return false;
		}
	}
	
	
	@Override
	public void onAccuracyChanged(Sensor arg0, int arg1) {	
	}

	@Override
	public void onSensorChanged(SensorEvent event) {
		
		if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
			acc = event.values;
			computeOrientation();
		}
		if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
			mag = event.values;
		}
	}

}
