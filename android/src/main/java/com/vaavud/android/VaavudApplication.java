package com.vaavud.android;

import android.app.Application;

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
}
