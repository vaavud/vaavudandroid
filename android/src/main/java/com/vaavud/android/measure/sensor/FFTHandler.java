package com.vaavud.android.measure.sensor;

import java.util.ArrayList;
import java.util.List;

//import android.util.Log;


public class FFTHandler {

	public class FreqAmp {

		public float frequency;
		public float amplitude;
	}
	
	private Integer dataLength;
	private Integer FFTLength;
	private Integer windowType;
	private Integer interpolationType;
	private FFTAlgorithm myFFTAlgorithm;
	
	private double[] windowValues;
	
	// windowTypes
	public static final int RETANGULAR_WINDOW = 0;
	public static final int WELCH_WINDOW = 1;
	
	// interpolationTypes
	public static final int NO_INTERPOLATION = 0;
	public static final int QUADRATIC_INTERPOLATION = 1;
	public static final int LOGARITHMIC_INTERPOLATION = 2;
	
	// 
	private boolean lastValid = false;
	
	public FFTHandler(Integer _dataLength, Integer _FFTLength, Integer _windowType, Integer _interpolationType) {
		this.dataLength = _dataLength;
		this.FFTLength = _FFTLength;
		this.windowType = _windowType;
		this.interpolationType = _interpolationType;
		
		generatePrecalculatedWindowValues();
		
		myFFTAlgorithm = new FFTAlgorithm(FFTLength);
	}
	
	
	public FreqAmp getFreqAndAmpOneAxisFFT(List<Float> oneAxisData, Double sampleFrequency) {
		
		List<Float> fftresult;
        
		oneAxisData = applyZeroMean(oneAxisData);
		oneAxisData = windowData(oneAxisData);
		
		int i = 0;
		while (oneAxisData.get(i).floatValue() == 0f) {
			i++;
			if (i == oneAxisData.size())
				return null;
		}
		
		fftresult = myFFTAlgorithm.doFFT(oneAxisData, dataLength);
		
		FreqAmp mySpeedAndAmp = speedAndAmpFromFFTResult(fftresult, sampleFrequency.floatValue());
		
		return mySpeedAndAmp;
	}
	
	public FreqAmp getFreqAndAmpThreeAxisFFT(List<Float[]> threeAxisData, Double sampleFrequency) {
		
		double frequencyMean;
		double frequencyRMSD;
		double amplitudeMean;
		
		List<Float> xAxis = new ArrayList<Float>(dataLength);
		List<Float> yAxis = new ArrayList<Float>(dataLength);
		List<Float> zAxis = new ArrayList<Float>(dataLength);
		
		for (int i = 0; i < threeAxisData.size(); i++) {
			xAxis.add(threeAxisData.get(i)[0]);
			yAxis.add(threeAxisData.get(i)[1]);
			zAxis.add(threeAxisData.get(i)[2]);
		}
		
		FreqAmp myFAx = getFreqAndAmpOneAxisFFT(xAxis, sampleFrequency);
		if (myFAx == null) {
			return null;
		}
		
		FreqAmp myFAy = getFreqAndAmpOneAxisFFT(yAxis, sampleFrequency);
		if (myFAy == null) {
			return null;
		}
		
		FreqAmp myFAz = getFreqAndAmpOneAxisFFT(zAxis, sampleFrequency);
		if (myFAz == null) {
			return null;
		}
		
		// calculate frequency RMS
		
		frequencyMean = (myFAx.frequency + myFAy.frequency + myFAz.frequency) /3;
		frequencyRMSD = Math.sqrt( 0.3333333333D * Math.pow(myFAx.frequency - frequencyMean, 2) +  0.3333333333D * Math.pow(myFAy.frequency - frequencyMean, 2) + 0.3333333333D * Math.pow(myFAz.frequency - frequencyMean, 2));
		amplitudeMean = (myFAx.amplitude + myFAy.amplitude + myFAz.amplitude) /3;
				
				
//		FreqAmp mySpeedAndAmp = 
		
//		Log.v(MainActivity.TAG, String.format("wind: %f, %f, %f amp:  %f, %f, %f, fRMSD: %f",
//					myFAx.frequency, myFAy.frequency, myFAz.frequency,  myFAx.amplitude, myFAy.amplitude, myFAz.amplitude, frequencyRMSD));
		
		if (frequencyRMSD < 0.2 && frequencyMean > 1 && amplitudeMean > 0.3) {
			
			if (lastValid) {
				FreqAmp meanFreqAmp= new FreqAmp();
				meanFreqAmp.amplitude = (float) amplitudeMean;
				meanFreqAmp.frequency = (float) frequencyMean;
				lastValid = true;
				
				return meanFreqAmp;
			}
			lastValid = true;	
		}
		else {
			lastValid = false;
		}
		
		return null;
	}
	
	private static List<Float> applyZeroMean(List<Float> data) {
		double sum = 0d;		
		double mean; 
		
		for (int i = 0; i < data.size(); i++) {
			sum = sum + data.get(i);
		}
		
		mean = sum / data.size();
		
		for (int i = 0; i < data.size(); i++) {
			data.set(i, (float) (data.get(i)-mean));
		}
		
		return data;
	}
	
	private List<Float> windowData(List<Float> data) {
		for (int i = 0; i < data.size(); i++) {
			data.set(i, (float) windowValues[i] * data.get(i));
		}
		return data;
	}
	
	
	private void generatePrecalculatedWindowValues() {
		windowValues = new double[dataLength];
		
		if (windowType == RETANGULAR_WINDOW) {
			for (int i = 0; i < windowValues.length; i++) {
				windowValues[i] = 1;
			}	
		}
		else if (windowType == WELCH_WINDOW) {
			
			// MODIFY DATA (NOT IMPLEMENTED YET)
			for (int i = 0; i < windowValues.length; i++) {
				windowValues[i] = 1 - Math.pow((i -  (double) (dataLength-1)/2 ) / (  (double) (dataLength+1)/2), 2);
			}
		}
		else {
			
			//Log.e(MainActivity.TAG, "Unsuported WindowType");
			windowType = RETANGULAR_WINDOW;
			generatePrecalculatedWindowValues();
		}
	}
	
	private FreqAmp speedAndAmpFromFFTResult(List<Float> fftResult, Float sampleFrequency) {
		
        int maxBin = 1;
        float maxPeak = 0;
        float peakAmplitude;
        float peakFrequency;
        
        // find the highest peak (bin)
        for (int i=1; i < (FFTLength/2); i++) {
        	
            if (fftResult.get(i) > maxPeak) {
            	maxBin = i;
                maxPeak = fftResult.get(i).floatValue();
            }
		}
        
        switch (interpolationType) {
		case NO_INTERPOLATION:
        	peakFrequency = (maxBin)*sampleFrequency/FFTLength;
            peakAmplitude = maxPeak;
			break;

		case QUADRATIC_INTERPOLATION:
			double alph = fftResult.get(maxBin-1);
			double beta = fftResult.get(maxBin);
			double gamma = fftResult.get(maxBin+1);

            double p = 0.5d * (alph - gamma) / (alph - 2 * beta + gamma);

            peakFrequency = (float) ((maxBin+p)*sampleFrequency/FFTLength);
            peakAmplitude = (float) (beta - 1/4 * (alph - gamma) * p);
			break;			
			
		case LOGARITHMIC_INTERPOLATION:
			// not implemented yet
			//Log.e(MainActivity.TAG, "Wrong Interpolation type!");
			interpolationType = NO_INTERPOLATION;
			return speedAndAmpFromFFTResult(fftResult, sampleFrequency);
				
		default:
			//Log.e(MainActivity.TAG, "Wrong Interpolation type!");
			interpolationType = NO_INTERPOLATION;
			return speedAndAmpFromFFTResult(fftResult, sampleFrequency);
			
		}
            
        FreqAmp myFA = new FreqAmp();
        myFA.amplitude = peakAmplitude;
		myFA.frequency = peakFrequency;
		
		return myFA;
	}
		
	public Integer getDataLength() {
		return dataLength;
	}
}
