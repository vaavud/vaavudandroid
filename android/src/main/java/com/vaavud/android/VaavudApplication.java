package com.vaavud.android;

import android.app.Application;

import com.crittercism.app.Crittercism;
import com.vaavud.android.model.VaavudDatabase;
import com.vaavud.android.model.entity.Device;
import com.vaavud.android.model.entity.User;

public class VaavudApplication extends Application {
    private boolean userLogged = false;
    private boolean hasWindMeter = false;
    private static final String KEY_IS_FIRST_FLOW = "isFirstFlow";
    User user;


    @Override
    public void onCreate() {
        super.onCreate();
//        Device.getInstance(this);
        Crittercism.initialize(this, "520b8fa5558d6a2757000003");
        VaavudDatabase.getInstance(this).getWritableDatabase();
    }

    public boolean isUserLogged() {
        user = User.getInstance(this);
        if (user.getAuthToken() != null && user.getAuthToken().length() > 0
                && user.getEmail() != null && user.getEmail().length() > 0) {
            userLogged = true;
        }
        return userLogged;
    }

    public boolean hasWindMeter() {
        user = User.getInstance(this);
        if (user.getHasWindMeter() != null && user.getHasWindMeter().booleanValue())
            hasWindMeter = true;
        return hasWindMeter;
    }

    public boolean isFirstFlow() {
        Boolean isFirstFlow = VaavudDatabase.getInstance(this).getPropertyAsBoolean(KEY_IS_FIRST_FLOW);
        if (isFirstFlow != null) return isFirstFlow;
        return true;
    }

    public void setIsFirstFlow(Boolean status) {
        VaavudDatabase.getInstance(this).setPropertyAsBoolean(KEY_IS_FIRST_FLOW, status);
    }
    
    public User getUser(){
    	return user;
    }
}
