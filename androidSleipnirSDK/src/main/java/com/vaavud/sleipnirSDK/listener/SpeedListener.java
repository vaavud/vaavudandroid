package com.vaavud.sleipnirSDK.listener;

public interface SpeedListener {
	
	public void speedChanged(float speed,float windDirection, long timestamp, float velocityProfileError);

	public void calibrationPercentageComplete(float percentage);
	
	public void calibrationCoefficients(Float[] coefficients);
	
}
