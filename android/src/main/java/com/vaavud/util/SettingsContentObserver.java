package com.vaavud.util;

import android.content.Context;
import android.database.ContentObserver;
import android.media.AudioManager;
import android.os.Handler;
import android.util.Log;


public class SettingsContentObserver extends ContentObserver {
    int previousVolume;
    Context context;

    public SettingsContentObserver(Context c, Handler handler) {
        super(handler);
        context = c;
        AudioManager audio = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        previousVolume = audio.getStreamVolume(AudioManager.STREAM_MUSIC);
    }

    @Override
    public boolean deliverSelfNotifications() {
        return super.deliverSelfNotifications();
    }

    @Override
    public void onChange(boolean selfChange) {
        AudioManager audio = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        int currentVolume = audio.getStreamVolume(AudioManager.STREAM_MUSIC);
//        Log.d("SettingsContentObserver","Previous Volume:"+ previousVolume+ " Current Volume: " + currentVolume);
        int delta = previousVolume - currentVolume;
        
        if (delta > 0) {
            previousVolume = currentVolume;
            audio.setStreamVolume(AudioManager.STREAM_MUSIC, audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC), AudioManager.FLAG_REMOVE_SOUND_AND_VIBRATE);
        } else if (delta < 0) {
            previousVolume = currentVolume;
        }
        super.onChange(selfChange);
    }
}