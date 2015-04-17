package com.vaavud.sleipnirSDK.audio;

import com.vaavud.sleipnirSDK.algorithm.VaavudAudioProcessing;
import com.vaavud.sleipnirSDK.listener.SignalListener;
import com.vaavud.sleipnirSDK.listener.SpeedListener;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.media.MediaRecorder.AudioSource;
import android.util.Log;
import android.widget.Toast;

public class VaavudAudioRecording extends Thread {
	private boolean stopped = false;
	private final int sampleRate = 44100;
	private SignalListener mSignalListener = null;
	private int bufferSizeRecording;
	private VaavudAudioProcessing vap=null;
	private AudioRecord mRecorder = null;
	private short[] buffer = null;
	private int N=0;
    /**
     * Give the thread high priority so that it's not canceled unexpectedly, and start it
     */
    public VaavudAudioRecording(AudioRecord recorder,AudioTrack player,SpeedListener speedListener, SignalListener signalListener, String fileName, boolean calibrationMode)
    { 
        android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);
        mSignalListener = signalListener;
        bufferSizeRecording = AudioRecord.getMinBufferSize(sampleRate,AudioFormat.CHANNEL_IN_MONO,AudioFormat.ENCODING_PCM_16BIT);
        vap = new VaavudAudioProcessing(bufferSizeRecording/10,speedListener,mSignalListener,fileName,calibrationMode,player);
//        vap = new VaavudAudioProcessing(bufferSizeRecording,speedListener,mSignalListener,fileName,calibrationMode,player);

        mRecorder = recorder;
    	if (mRecorder != null && mRecorder.getState() != AudioRecord.STATE_UNINITIALIZED) {
    		if (mRecorder.getRecordingState() == AudioRecord.RECORDSTATE_RECORDING)
    		{
//    			Log.d("VaavudAudioRecording","Recorder RECORDING");
            	mRecorder.stop();
            }
        }
//    	else{
//        	Log.d("VaavudAudioRecording","Recorder UNINITIALIZED");
//        }
    }

    @Override
    public void run()
    {         
        /*
         * Initialize buffer to hold continuously recorded audio data, start recording, and start
         * playback.
         */
        try
        {
        	stopped = false;
//        	Log.d("Audio","Buffer Size: "+ (int) (bufferSizeRecording*Math.floor((44000/bufferSizeRecording))));
        	
        	mRecorder.startRecording();
        	buffer = new short[bufferSizeRecording/10];
//        	buffer = new short[bufferSizeRecording];
            /*
             * Loops until something outside of this thread stops it.
             * Reads the data from the recorder and writes it to the audio track for playback.
             */
            
            while(!stopped)
            { 
                N=mRecorder.read(buffer,0,buffer.length);
//                Log.d("VaavudAudioRecording","Numer of bits read : "+ N);
            	vap.signalChanged(buffer);
            	if (mSignalListener!=null){
            		mSignalListener.signalChanged(buffer);
            	}
//              buffer=null;
            }
//            Log.d("AudioRecorder","Stop");
        }
        catch(Throwable x)
        { 
            Log.w("Audio", "Error reading voice audio", x);
        }
        /*
         * Frees the thread's resources after the loop completes so that it can be run again
         */
        finally
        { 
//        	Log.d("VaavudAudioRecording", "Executing Finally");
        	buffer=null;
        	vap.close();
        	vap = null;
        }
    }
    
    public void setCoefficients(Float[] coefficients){
    	vap.setCoefficients(coefficients);
    }
    /**
     * Called from outside of the thread in order to stop the recording/playback loop
     */
    public void close()
    { 
    	stopped = true;
    	buffer = null;
    	if (mRecorder!=null && mRecorder.getState() == AudioRecord.RECORDSTATE_RECORDING){
    		mRecorder.stop();
     	}    	
    }
}
