package com.vaavud.android.measure;

import com.vaavud.android.model.entity.MeasurementSession;

public interface MeasurementController {

	public MeasurementSession startSession();
	
	public void stopSession();
	
	public MeasurementSession currentSession();
	
	public boolean isMeasuring();
	
	public void addMeasurementReceiver(MeasurementReceiver measurementReceiver);
	
	public void removeMeasurementReceiver(MeasurementReceiver measurementReceiver);

	public void resumeMeasuring();

	public void pauseMeasuring();

	public void stopController();
}
