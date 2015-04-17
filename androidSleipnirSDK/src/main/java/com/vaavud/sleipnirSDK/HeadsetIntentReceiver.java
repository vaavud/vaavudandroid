package com.vaavud.sleipnirSDK;

import com.vaavud.sleipnirSDK.listener.PlugListener;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class HeadsetIntentReceiver extends BroadcastReceiver {
	private String TAG = "HeadSet";
	private PlugListener listener;

	public HeadsetIntentReceiver(Context context) {
//		Log.d(TAG, "Created");
		listener = (PlugListener) context;
	}

	@Override
	public void onReceive(Context context, Intent intent) {
		if(intent.getAction().equals(Intent.ACTION_HEADSET_PLUG)) {
			int status = intent.getIntExtra("state", -1);
			boolean state = status==1;
			
			int microphoneStatus = intent.getIntExtra("microphone",-1);
			boolean connectedMicrophone =  microphoneStatus == 1;
//			Log.d(TAG,"Microphone Intent Receiver: "+ connectedMicrophone + " Status: "+ state);
			listener.isSleipnirPlugged(state && connectedMicrophone);
		}
	}
}
