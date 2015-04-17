package com.vaavud.android.measure.sensor;

import android.content.Context;
import android.os.Handler;

import com.vaavud.android.measure.sensor.FFTHandler.FreqAmp;
import com.vaavud.android.model.entity.Device;
import com.vaavud.android.model.entity.WindMeasurement;

import java.util.ArrayList;
import java.util.List;
//import android.util.Log;

//import com.vaavud.util.AlgorithmConstantsUtil;

public class FFTManager {

//	private FFTHandler shortFFT;
	private FFTHandler normalFFT;
//	private FFTHandler longFFT;
	
	private DataManager myDataManager;
	private int largestFFTDataLength;
	
	private Handler myHandler = new Handler();
	private boolean isRunning;
	private double frequencyStart;
	private double frequencyFactor;
	
	private Runnable calculateWindspeed = new Runnable() {
		@Override
		public void run() {
			generateWindMeasurement();
			myHandler.postDelayed(calculateWindspeed, 200);
		}
	};
	
	public FFTManager(Context context, DataManager _myDataManager) {
		this.myDataManager = _myDataManager;
		normalFFT = new FFTHandler(100, 128, FFTHandler.WELCH_WINDOW, FFTHandler.QUADRATIC_INTERPOLATION);
		configure(context);
		isRunning = false;
	}
	
	public void configure(Context context) {
		frequencyStart = Device.getInstance(context).getFrequencyStart();
		frequencyFactor = Device.getInstance(context).getFrequencyFactor();
		largestFFTDataLength = 100; // quick implementation
		//Log.i("FFTManager", "Using frequencyStart=" + frequencyStart + " and frequencyFactor=" + frequencyFactor);
	}
	
	public void generateWindMeasurement() {
		
		// get data from dataManager
		List<Float[]> magneticFieldData = myDataManager.getLastXMagneticfieldMeasurements(largestFFTDataLength);
		FreqAmp myFreqAndAmp;
		// check if enough data is available
		
		if (magneticFieldData.size() >= normalFFT.getDataLength() ) {
			
			// Prepere data
			List<Float[]> threeAxisData = new ArrayList<Float[]>(normalFFT.getDataLength());
			for (int i = 0; i < magneticFieldData.size(); i++ ) {
				threeAxisData.add( new Float[] {magneticFieldData.get(i)[1], magneticFieldData.get(i)[2], magneticFieldData.get(i)[3]} );
			}
			
			double timeDiff = magneticFieldData.get(magneticFieldData.size()-1)[0] - magneticFieldData.get(0)[0];
			double sampleFrequency = (normalFFT.getDataLength() -1) / timeDiff;
						
			myFreqAndAmp = normalFFT.getFreqAndAmpThreeAxisFFT(threeAxisData, sampleFrequency);
			
			if (myFreqAndAmp != null) {
				WindMeasurement myWindMeasurement = new WindMeasurement();
				myWindMeasurement.amplitude = myFreqAndAmp.amplitude;
				myWindMeasurement.windspeed = frequencyToWindspeed(myFreqAndAmp.frequency);
				myWindMeasurement.time = magneticFieldData.get(magneticFieldData.size()-1)[0];
				
				myDataManager.addWindMeasurement(myWindMeasurement);
			}
			
		}
		else {
			// do nothing
		}
	}
	
	private Float frequencyToWindspeed(Float frequency) {
		
		// Based on 09.07.2013 Windtunnel test. Parametes can be found in windTunnelAnalysis_9_07_2013.xlsx
		// Corrected base on data from Windtunnel test Experiment26Aug2013Data.xlsx
	    
		double windspeed = this.frequencyFactor * frequency + frequencyStart;
	    
		if (frequency > 17.65D && frequency < 28.87D) {
	        windspeed = windspeed + -0.068387D * Math.pow((frequency - 23.2667D), 2) + 2.153493D;
	    } 
	    
	    return (float) windspeed;
	}
	
//	private Double estimateSampleFrequency() {
//		
//	}
//	
	
	public void start() {
		if (!isRunning) {
			myHandler.post(calculateWindspeed);
		}	
	}
	
	public void stop() {
		myHandler.removeCallbacks(calculateWindspeed);
	}
}
