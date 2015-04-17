package com.vaavud.android.network.listener;

import com.vaavud.android.model.entity.MeasurementSession;

import java.util.ArrayList;

public interface HistoryMeasurementsResponseListener {
	
	public void measurementsLoadingFailed();

	public void measurementsReceived(ArrayList<MeasurementSession> histObjList);
}
