package com.vaavud.android.measure;

import com.vaavud.android.model.entity.MeasurementSession;

public interface MeasurementReceiver {

	public void measurementAdded(MeasurementSession session, Float time, Float currentWindSpeed, Float avgWindSpeed, Float maxWindSpeed,Float direction);
	
	public void measurementFinished(MeasurementSession session);
	
	public void measurementStatusChanged(MeasureStatus status);
}
