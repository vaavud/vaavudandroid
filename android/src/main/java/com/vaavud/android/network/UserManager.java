package com.vaavud.android.network;

import android.content.Context;
import android.util.Log;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.RequestQueue;
import com.android.volley.Response.ErrorListener;
import com.android.volley.Response.Listener;
import com.android.volley.VolleyError;
import com.facebook.Session;
import com.facebook.Session.StatusCallback;
import com.facebook.SessionState;
import com.vaavud.android.model.entity.Device;
import com.vaavud.android.model.entity.User;
import com.vaavud.android.network.listener.UserResponseListener;
import com.vaavud.android.network.request.AuthGsonRequest;
import com.vaavud.android.network.response.RegisterUserResponse;
import com.vaavud.android.ui.MainActivity;


public class UserManager {

		private static final int REQUEST_TIMEOUT_MS = 8000; // 8 seconds
		private static final int MAGNETIC_RETRIES = 3;
		private static final String TAG = "USER_MANAGER";

		public static final String BASE_URL = "https://mobile-api.vaavud.com";
//	public static final String BASE_URL = "http://54.75.224.219";

		private Context context;
		private RequestQueue requestQueue;


		int timeAgo;
		private boolean registerUserFired = false;

		private UserResponseListener userResponseListener;
		private static UserManager instance;

		private User user;

		private class UserStatusCallback implements StatusCallback {

				@Override
				public void call(Session session, SessionState state, Exception exception) {
//			Log.d(TAG,"User Manager CallBack");
						if (exception != null) {
//				Log.d(TAG,"Exception" + exception.toString());
								return;
						}
						if (state.isOpened()) {
//				Log.d(TAG, "Session State: " + session.getState());
								if (session.isPermissionGranted("email") || !session.isPermissionGranted("user_friends") || !session.isPermissionGranted("public_profile")) {
										if (user != null) {
												((MainActivity) context).restartUser();
//						Log.d(TAG, "Session Closed");
										}
								} else {
										if (user != null) {
												user.setFacebookAccessToken(session.getAccessToken());
										}
								}
						} else {
//				Log.d(TAG, "Session State: " + session.getState());
								((MainActivity) context).restartUser();
						}
				}
		}

		public static synchronized UserManager getInstance(Context context, RequestQueue requestQueue, User user) {
				if (instance == null) {
						instance = new UserManager(context, requestQueue, user);
				}
				return instance;
		}

		private UserManager(Context context, RequestQueue requestQueue, User user) {
				this.context = context;
				this.requestQueue = requestQueue;
				this.user = user;
		}

		public boolean registerUser(User user, UserResponseListener listener) {
//		Log.d("UserManager","Registering User");
				if (listener == null)
						return false;

				userResponseListener = listener;

				String authToken = Device.getInstance(context).getAuthToken();
//		Log.d("UserManager","Device Token: "+dev.getAuthToken());
				if (authToken == null || authToken.length() == 0) {
						//Log.i("UploadManager", "No authToken so skipping upload");
						return false;
				}
				AuthGsonRequest<RegisterUserResponse> request = new AuthGsonRequest<RegisterUserResponse>(BASE_URL + "/api/user/register", authToken, user, RegisterUserResponse.class,
								new Listener<RegisterUserResponse>() {
										@Override
										public void onResponse(RegisterUserResponse object) {
//						Log.i("UserManager", "Got successful response registering user");
												// USER RESPONSE ACTION
												userResponseListener.userResponseReceived(object);
										}
								}, new ErrorListener() {
						@Override
						public void onErrorResponse(VolleyError error) {
								userResponseListener.ErrorResponseReceived("Got error from server registering user");
								if (((MainActivity) context).getProgressDialog() != null)
										((MainActivity) context).getProgressDialog().dismiss();
								Log.e("UserManager", "Got error from server registering user: " + error.getMessage());
						}
				});
				request.setRetryPolicy(new DefaultRetryPolicy(REQUEST_TIMEOUT_MS, MAGNETIC_RETRIES, 1.5F));
//    	Log.d(TAG,"Creating request: "+"userRequest");
				request.setTag("userRequest");
				requestQueue.add(request);
				return true;
		}

		public boolean loginUser(User user, UserResponseListener listener) {

//		Log.d(TAG,"Login In User");
				if (listener != null) userResponseListener = listener;
				else return false;

				String authToken = Device.getInstance(context).getAuthToken();
//		Log.d(TAG,"Device Token: "+authToken);
				if (authToken == null || authToken.length() == 0) {
//			Log.i(TAG, "No authToken so skipping upload");
						return false;
				}
//		Log.d(TAG,user.toString());
				AuthGsonRequest<RegisterUserResponse> request = new AuthGsonRequest<RegisterUserResponse>(BASE_URL + "/api/user/register", authToken, user, RegisterUserResponse.class,
								new Listener<RegisterUserResponse>() {
										@Override
										public void onResponse(RegisterUserResponse object) {
//						Log.i(TAG, "Got successful response registering user");
												// USER RESPONSE ACTION
												userResponseListener.userResponseReceived(object);
										}
								}, new ErrorListener() {
						@Override
						public void onErrorResponse(VolleyError error) {
								userResponseListener.ErrorResponseReceived("Got error from server logging user");
//						if (((MainActivity)context).getProgressDialog()!=null) ((MainActivity)context).getProgressDialog().dismiss();
								Log.e(TAG, "Got error from server registering user: " + error.getMessage());
						}
				});

				request.setRetryPolicy(new DefaultRetryPolicy(REQUEST_TIMEOUT_MS, MAGNETIC_RETRIES, 1.5F));
				request.setTag("userRequest");
//    	Log.d(TAG,"Creating request: "+request.toString());
				requestQueue.add(request);
				return true;

		}

		public void cancelRequestQueue(String tag) {
//		Log.d(TAG,"Cancelling request: "+tag);
//		Log.d(TAG,"Request Queue: "+requestQueue.getCache().toString());
				requestQueue.cancelAll(tag);
		}

		public boolean getRegisterUserFired() {
//		Log.d("UserManager","GetRegisterUserFired: "+Boolean.toString(registerUserFired));
				return registerUserFired;
		}

		public StatusCallback getUserCallback() {
				// TODO Auto-generated method stub
				return new UserStatusCallback();
		}

}
