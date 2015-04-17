package com.vaavud.android.network.request;

import com.android.volley.AuthFailureError;
import com.android.volley.Response.ErrorListener;
import com.android.volley.Response.Listener;

import java.util.HashMap;
import java.util.Map;

public class AuthGsonRequest<T> extends GsonRequest<T> {

	private final String authToken;
	
	public AuthGsonRequest(String url, String authToken, Object postObject, Class<T> responseClass, Listener<T> listener, ErrorListener errorListener) {	
		super(url, postObject, responseClass, listener, errorListener);
//		Log.d("AUTH_GSON_REQUEST","Creating AuthGsonRequest");
		this.authToken = authToken;
	}
	
	public Map<String,String> getHeaders() throws AuthFailureError {
//		Log.d("AUTH_GSON_REQUEST","Creating Headers");
		HashMap<String,String> headers = new HashMap<String,String>();
		headers.putAll(super.getHeaders());
		headers.put("authToken", authToken);
		return headers;
	}
}
