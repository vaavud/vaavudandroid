package com.vaavud.sleipnirSDK.listener;

public interface SignalListener {
	
	public void signalChanged(short[] signal);

	public void signalChanged(float[] signal,float[] signalEstimated);
	
}
