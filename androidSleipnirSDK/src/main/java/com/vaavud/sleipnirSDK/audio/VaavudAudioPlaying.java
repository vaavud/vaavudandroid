package com.vaavud.sleipnirSDK.audio;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import android.media.AudioTrack;

public class VaavudAudioPlaying extends Thread{

	private AudioTrack mPlayer;
	private double offset;
	private boolean isPlaying;
	
	private final int duration = 1; // seconds
    private final int sampleRate = 44100; //Hz
    private final int numSamples = duration * sampleRate;
    private short sample[] = new short[numSamples*2];
    private final double freqOfTone = 14700; // hz
    
    private boolean mCalibrationMode = false;
    private String mFileName;
    private FileOutputStream os;
	
	public VaavudAudioPlaying(AudioTrack player,String fileName, boolean calibrationMode){
		mPlayer = player;
		mFileName = fileName;
		mCalibrationMode = calibrationMode;
		
		if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.LOLLIPOP){
			mPlayer.setStereoVolume(AudioTrack.getMaxVolume(), AudioTrack.getMaxVolume());
		}else{
			mPlayer.setVolume(AudioTrack.getMaxVolume());
		}
		
		if (mPlayer != null && mPlayer.getState() != AudioTrack.STATE_UNINITIALIZED ) {
            if (mPlayer.getPlayState() == AudioTrack.PLAYSTATE_PLAYING) {
            	mPlayer.stop();
            }
        }
      
    	offset = Math.PI;
      
        for (int i = 0; i < numSamples*2; i=i+2) {
            sample[i] = (short) ((Math.sin((2 * Math.PI * i / (sampleRate/freqOfTone))) * Short.MAX_VALUE));
            sample[i+1] = (short) ((Math.sin((2 * Math.PI * i / (sampleRate/freqOfTone))+offset) * Short.MAX_VALUE));
        }
        
        if (mCalibrationMode){
	    	
		    String filePath = fileName;
//		    Log.d("VaavudAudioProcessing", "FilePath: "+filePath);
			try {
		    	os = new FileOutputStream(filePath+".play");
				os.write(short2byte(sample));
				os.close();
		    } catch (IOException e) {
		        e.printStackTrace();
		    }
			os = null;
		}
	}
	
	@Override
    public void run()
    { 
        isPlaying=true;
        if (mPlayer.getState() == AudioTrack.STATE_INITIALIZED){
        	mPlayer.play();
	        while (isPlaying) {
	        	if (sample!=null){
	        		
	        		mPlayer.write(sample, 0, sample.length);
	        	}
	        }
//	        Log.d("AudioPlayer","Stop");
        }
    }
	
	/**
     * Called from outside of the thread in order to stop the playback loop
     */
	public void close()
    { 
		isPlaying = false;
		sample = null;
		if (mPlayer!=null && mPlayer.getState() == AudioTrack.PLAYSTATE_PLAYING){
			mPlayer.flush();
			mPlayer.stop();
		}
//		player.release();
    }
	
	//convert short to byte
		private byte[] short2byte(short[] sData) {
		    int shortArrsize = sData.length;
		    byte[] bytes = new byte[shortArrsize * 2];
		    for (int i = 0; i < shortArrsize; i++) {
		        bytes[i * 2] = (byte) (sData[i] & 0x00FF);
		        bytes[(i * 2) + 1] = (byte) (sData[i] >> 8);
//		        sData[i] = 0;
		    }
		    return bytes;

		}
	

}
