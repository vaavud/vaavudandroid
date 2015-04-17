package com.vaavud.android.network.listener;

import com.vaavud.android.network.response.RegisterUserResponse;

public interface UserResponseListener {

	public void userResponseReceived(RegisterUserResponse object);

	public void ErrorResponseReceived(String string);
	
}
