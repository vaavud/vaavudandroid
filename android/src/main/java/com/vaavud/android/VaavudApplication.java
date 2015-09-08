package com.vaavud.android;

import android.app.Application;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.hardware.SensorManager;
import android.support.multidex.MultiDex;
import android.view.OrientationEventListener;

import com.crittercism.app.Crittercism;
import com.vaavud.android.model.VaavudDatabase;
import com.vaavud.android.model.entity.Device;
import com.vaavud.android.model.entity.User;
import com.vaavud.android.network.UploadManager;

public class VaavudApplication extends Application {





    @Override
    public void onCreate() {
        super.onCreate();
        Crittercism.initialize(this, "520b8fa5558d6a2757000003");
        VaavudDatabase.getInstance(this);

    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        MultiDex.install(this);
    }






}
