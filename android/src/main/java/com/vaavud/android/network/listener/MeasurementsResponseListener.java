package com.vaavud.android.network.listener;


import com.vaavud.android.model.entity.MapMeasurement;

public interface MeasurementsResponseListener {

	public void measurementsReceived(MapMeasurement[] measurements);
	
	public void measurementsLoadingFailed();

}
