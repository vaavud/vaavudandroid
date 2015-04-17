package com.vaavud.android.network.listener;


public interface DeleteMeasurementResponseListener {
	public void deleteMeasurementFailed();

	public void deleteMeasurementReceived(Boolean status);
}
