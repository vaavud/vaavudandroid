package com.vaavud.sleipnirSDK.algorithm;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Date;

import com.vaavud.sleipnirSDK.listener.SignalListener;
import com.vaavud.sleipnirSDK.listener.SpeedListener;

import android.content.Context;
import android.media.AudioTrack;
import android.util.Log;

public class VaavudAudioProcessing {

	private int windowAveragingSize;
	//Sound processing
	private int[] mvgAvg = new int[3];
	private int mvgAvgSum;
	private int[] mvgDiff = new int[3];
	private int mvgDiffSum;
	private int lastValue;
	private double gapBlock;
	private long counter;
	private long lastTick;
	private short mvgState;
	private short diffState;
	private int mvgMax;
    private int mvgMin;
    private int lastMvgMax;
    private int lastMvgMin;
    private int diffMax;
    private int diffMin;
    private int lastDiffMax;
    private int lastDiffMin;
    private int diffGap;
    private int mvgGapMax;
    private int lastMvgGapMax;
    private int mvgDropHalf;
    private int diffRiseThreshold;
    private boolean mCalibrationMode;

	
	
	//Buffer
	private short[] buffer;
	
	private FileOutputStream os = null;
	private VaavudWindProcessing vwp=null;
	private String mFileName;
	
	//Sound calibration
	private AudioTrack mPlayer;
	private final static int CALIBRATE_AUDIO_EVERY_X_BUFFER=10;
	private int calibrationCounter=0;
//	private float currentVolume=0.35f;
	private float currentVolume=1.0f;
	private float maxVolume;


	
	public VaavudAudioProcessing(){
		buffer = null;
	}
	
	public VaavudAudioProcessing(int bufferSizeRecording,SpeedListener speedListener,SignalListener signalListener, String fileName, boolean calibrationMode,AudioTrack player){
		Log.d("SleipnirSDK","VaavudAudioProcessing");
		mCalibrationMode = calibrationMode;
		mPlayer = player;
		
		maxVolume = AudioTrack.getMaxVolume();
		
		if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.LOLLIPOP){
			mPlayer.setStereoVolume(maxVolume*currentVolume, maxVolume*currentVolume);
		}else{
			mPlayer.setVolume(maxVolume*currentVolume);
		}
		
		buffer = new short[bufferSizeRecording];
		
		//SoundProcessing Init
		counter = 0;
	    mvgAvgSum = 0;
	    mvgDiffSum = 0;
	    lastValue = 0;
	    
	    lastDiffMax = 1000;
	    lastDiffMin = 0;
	    lastMvgMax = 500;
	    lastMvgMin = -500;
	    lastMvgGapMax = 0;
	    lastTick = 0;
	    
	    mvgMax = 0;
	    mvgMin = 0;
	    diffMax = 0;
	    diffMin = 0;

	    
	    gapBlock=0;
	    mvgState = 0;
	    diffState = 0;
	    
	    if (mCalibrationMode){
	    	
		    String filePath = fileName;
//		    Log.d("VaavudAudioProcessing", "FilePath: "+filePath);
			try {
		    	os = new FileOutputStream(filePath+".rec");
		    } catch (FileNotFoundException e) {
		        e.printStackTrace();
		    }
	    }
	    
	    vwp = new VaavudWindProcessing(speedListener,signalListener,mCalibrationMode);
	}
	
	public void writeToDataFile(){
		try {
			os.write(short2byte(buffer));
		} catch (IOException e) {
			e.printStackTrace();
		}
		 
	}
	
	
	private void applyFilter(){
		
		int maxDiff=0;
		int currentSample=0;
		
//		int lDiffMax = 0;
//	    int lDiffMin = 10000;
//	    long lDiffSum = 0;
//	    
//	    int avgMax = -10000;
//	    int avgMin = 10000;
		
		
		for(int i=0;i<buffer.length;i++){
			int bufferIndex = (int) (mod(counter,3));
	        int bufferIndexLast = (int) (mod(counter-1,3));
	        
	        // Moving Avg subtract
	        mvgAvgSum -= mvgAvg[bufferIndex];
	        // Moving Diff subtrack
	        mvgDiffSum -= mvgDiff[bufferIndex];
	        
	        currentSample= (int) (1000*((float)buffer[i]/Short.MAX_VALUE));
//	        Log.d("VaavudAudioProcessing","Current Sample: "+currentSample);

	        // Moving Diff Update buffer value
	        mvgDiff[bufferIndex] = Math.abs(currentSample - mvgAvg[bufferIndexLast]); // ! need to use old mvgAvgValue so place before mvgAvg update
	        // Moving avg Update buffer value
	        mvgAvg[bufferIndex] = currentSample;
	        
	     // Moving Avg update SUM
	        mvgAvgSum += mvgAvg[bufferIndex];
	        mvgDiffSum += mvgDiff[bufferIndex];

	        if (maxDiff < mvgDiffSum) {
	            maxDiff = mvgDiffSum;
	        }
	        
	        if (detectTick((int) (counter - lastTick))) {
	            //Direction Detection Algorithm
//	        	Log.d("AudioProcessing","sampleSinceTick: "+ counter + " : " + lastTick);	
	            
	            lastMvgMax = mvgMax;
	            lastMvgMin = mvgMin;
	            lastDiffMax = diffMax;
	            lastDiffMin = diffMin;
	            lastMvgGapMax = mvgGapMax;
//	            Log.d("AudioProcessing",lastMvgMax+":"+lastMvgMin+":"+lastDiffMax+":"+lastDiffMin+":"+lastMvgGapMax);
	            
	            mvgMax = 0;
	            mvgMin = 0;
	            diffMax = 0;
	            diffMin = 6*1000;
	            mvgState = 0;
	            diffState = 0;
	            		

	        	boolean longTick = vwp.newTick((int)(counter - lastTick));
	            lastTick = counter;
	        }

	        counter++;
		}
		
		if (diffMax > 3.8*1000 && calibrationCounter > CALIBRATE_AUDIO_EVERY_X_BUFFER) {
			Log.d("SleipnirSDK","diffMax: "+diffMax);
			currentVolume -= 0.01;
	        adjustVolume();
	        calibrationCounter = 0;
	    }
		Log.d("SleipnirSDK","mvgMin: "+ mvgMin);
	    if ((mvgMin < -1.4*1000 && diffMax > 1*1000) && calibrationCounter > CALIBRATE_AUDIO_EVERY_X_BUFFER) {
	    	Log.d("SleipnirSDK","mvgMin: "+ mvgMin + " diffMax: "+diffMax);
	    	currentVolume -= 0.01;
	    	adjustVolume();
	        calibrationCounter = 0;
	    }
	    
	    calibrationCounter++;		
	}
	
	private void adjustVolume(){

	    if (currentVolume>1.0f) currentVolume=1.0f;
	    if (currentVolume < 0.1) currentVolume=1.0f;
	    Log.d("SleipnirSDK","Adjusting Volume: "+ currentVolume);
	    if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.LOLLIPOP){
			mPlayer.setStereoVolume(maxVolume*currentVolume, maxVolume*currentVolume);
		}else{
			mPlayer.setVolume(maxVolume*currentVolume);
		}	    
	}
	
	
	private boolean detectTick(int sampleSinceTick){
//		Log.d("AudioProcessing","MvgState: "+mvgState + " diffState: "+ diffState);
	    switch (mvgState) {
	        case 0:
	            if (sampleSinceTick < 60) {
	                if (mvgAvgSum > 0.5*lastMvgMax) {
	                    mvgState = 1;
	                }
	            } else {
	                mvgState = -1;
	            }
	            break;
	        case 1:
	            if (sampleSinceTick < 90) {
	                if (mvgAvgSum < 0.5*lastMvgMin) {
	                    return true;
	                }
	            } else {
	                mvgState = -1;
	            }	
	            break;
	        default:
	            break;
	    }
	    
	    switch (diffState) {
	        case 0:
        		if (mvgAvgSum<mvgMin)
        			mvgMin=mvgAvgSum;
        		if (mvgDiffSum > 0.3*lastDiffMax)
        			diffState = 1;
	            break;
	        case 1:
	        	if (mvgAvgSum<mvgMin)
        			mvgMin=mvgAvgSum;
	        	if (mvgAvgSum>0)
	        		diffState=2;
	            break;
	        case 2:
	        	if (mvgDiffSum < 0.35*lastDiffMax) {
	                diffState = 3;
	                gapBlock = sampleSinceTick * 2.5;
	                if (gapBlock > 5000){
	                	gapBlock = 5000;
	                }
	            }
	        	break;
	        case 3:
	            if (sampleSinceTick > gapBlock) {
	            	diffState=4;
	                diffGap = mvgDiffSum;
	                mvgGapMax = mvgAvgSum;
	                diffRiseThreshold = (int) (diffGap + 0.1 * (lastDiffMax - diffGap));
	                mvgDropHalf =  (lastMvgGapMax - mvgMin)/2 ;
	            }
	            break;
	        case 4:
	            if (mvgAvgSum > mvgGapMax)
	                mvgGapMax = mvgAvgSum;
	            
//	            if (mvgDiffSum > 0.3*lastDiffMax && mvgAvgSum < 0.2*lastMvgMin) { // diff was 1200
	            if (((mvgAvgSum < mvgGapMax - mvgDropHalf) && ( mvgDiffSum > diffRiseThreshold ))  || (mvgDiffSum > 0.5*lastDiffMax) ) {
	                return  true;
	            }
	            break;
	        default:
	            break;
	    }
//	    Log.d("AudioProcessing","mvgAvgSum: "+mvgAvgSum+" mvgMax: "+mvgMax);
	    if (mvgAvgSum > mvgMax)
	        mvgMax=mvgAvgSum;
	    
	    if (mvgDiffSum> diffMax)
	        diffMax = mvgDiffSum;
	    
	    if (mvgDiffSum<diffMin)
	        diffMin = mvgDiffSum;
	    
	    if (sampleSinceTick == 6000){
//	    	Log.d("AudioProcessing", "Reset State machine: "+sampleSinceTick);
	    	resetStateMachine();
	    }
	    
	    return false;


	}

	public int getWindowAveragingSize() {
		return windowAveragingSize;
	}

	public void setWindowAveragingSize(int windowAveragingSize) {
		this.windowAveragingSize = windowAveragingSize;
	}
	

	public void signalChanged(short[] signal) {
		if (signal!=null){
			System.arraycopy(signal, 0, buffer, 0, signal.length);
			applyFilter();
			if(mCalibrationMode){
				writeToDataFile();
			}
		}
	}
	
	private void resetStateMachine() {
//		Log.d("AudioProcessing", "ResetStateMachine");
	    mvgState = 0;
	    diffState = 0;
	    gapBlock = 0;
	    
	    mvgMax = 0;
	    mvgMin = 0;
	    diffMax = 0;
	    diffMin = 0;
	    
	    lastMvgMax = 500;
	    lastMvgMin = -500;
	    lastDiffMax = 1000;
	    lastDiffMin = 0;
	    lastMvgGapMax = 0;
	}
	
	public void setPlayer(AudioTrack player){
		mPlayer = player;
	}

	
	public void close(){
		buffer = null;
		mvgAvg = null;
		mvgDiff = null;
		vwp.close();
		vwp = null;
		if (mCalibrationMode && os!=null){
			try {
				os.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	//convert short to byte
	private byte[] short2byte(short[] sData) {
	    int shortArrsize = sData.length;
	    byte[] bytes = new byte[shortArrsize * 2];
	    for (int i = 0; i < shortArrsize; i++) {
	        bytes[i * 2] = (byte) (sData[i] & 0x00FF);
	        bytes[(i * 2) + 1] = (byte) (sData[i] >> 8);
	        sData[i] = 0;
	    }
	    return bytes;

	}
	
	private int mod(long l, int y)
	{
	    int result = (int) (l % y);
	    if (result < 0)
	    {
	        result += y;
	    }
	    return result;
	}

	public void setCoefficients(Float[] coefficients) {
		if (vwp!=null){
			vwp.setCoefficients(coefficients);
		}
		
	}

}
