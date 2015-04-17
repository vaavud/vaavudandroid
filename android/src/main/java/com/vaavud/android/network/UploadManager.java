package com.vaavud.android.network;

import android.content.Context;
import android.os.Handler;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.NetworkResponse;
import com.android.volley.RequestQueue;
import com.android.volley.Response.ErrorListener;
import com.android.volley.Response.Listener;
import com.android.volley.VolleyError;
import com.google.gson.JsonArray;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.mixpanel.android.mpmetrics.MixpanelAPI;
import com.vaavud.android.measure.MeasurementController;
import com.vaavud.android.measure.sensor.DataManager;
import com.vaavud.android.measure.sensor.FFTManager;
import com.vaavud.android.model.VaavudDatabase;
import com.vaavud.android.model.entity.Device;
import com.vaavud.android.model.entity.LatLng;
import com.vaavud.android.model.entity.MagneticSession;
import com.vaavud.android.model.entity.MapMeasurement;
import com.vaavud.android.model.entity.MeasurementPoint;
import com.vaavud.android.model.entity.MeasurementSession;
import com.vaavud.android.model.entity.User;
import com.vaavud.android.network.listener.DeleteMeasurementResponseListener;
import com.vaavud.android.network.listener.HistoryMeasurementsResponseListener;
import com.vaavud.android.network.listener.MeasurementsResponseListener;
import com.vaavud.android.network.request.AuthGsonRequest;
import com.vaavud.android.network.request.DeleteMeasurementRequestParameters;
import com.vaavud.android.network.request.GsonRequest;
import com.vaavud.android.network.request.HistoryMeasurementRequestParameters;
import com.vaavud.android.network.request.MeasurementsRequestParameters;
import com.vaavud.android.network.response.RegisterDeviceResponse;
import com.vaavud.android.ui.MainActivity;

import org.apache.http.HttpStatus;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class UploadManager {

	private static final int UPLOAD_INTERVAL = 10 * 1000; // 10 seconds
	private static final long IDLE_TIME_BEFORE_MARKING_AS_MEASURED = 600 * 1000L; // 1 hour
	
	private static final int REQUEST_TIMEOUT_MS = 8000; // 8 seconds
	private static final int MAGNETIC_RETRIES = 1;

	public static final String BASE_URL = "https://mobile-api.vaavud.com";
//	public static final String BASE_URL = "http://54.75.224.219";
	private static final String MIXPANEL_TOKEN = "757f6311d315f94cdfc8d16fb4d973c0"; 
	
	
	private Context context;
	private RequestQueue requestQueue;
	private Handler handler = new Handler();
	private MeasurementController measurementController;
	private DataManager dataManager;
	private FFTManager fftManager;
	
	/*
	 * If this is non-null there is a pending request to read measurements.
	 * Once measurements have been received, this will be set to null. 
	 */
	private MeasurementsResponseListener measurementsResponseListener;
	private int hoursAgo;
	private boolean measurementsRequestFired = false;
	private boolean registerDeviceFired = false;
	
	private Date lastReadMeasurement=new Date(0);
	private HistoryMeasurementsResponseListener historyMeasurementsResponseListener;
	private boolean historyMeasurementsRequestFired = false;
	private DeleteMeasurementResponseListener deleteMeasurementResponseListener;
	
	private Runnable uploadDataRunnable = new Runnable() {
		@Override
		public void run() {
			checkForUnUploadedData();
			handler.postDelayed(uploadDataRunnable, UPLOAD_INTERVAL);
		}
	};
	
		
	public UploadManager(Context context, RequestQueue requestQueue) {
		this.context = context;
		this.requestQueue = requestQueue;
	}
	
	public void setMeasurementController(MeasurementController measurementController) {
		this.measurementController = measurementController;
	}

	public void setDataManager(DataManager dataManager) {
		this.dataManager = dataManager;
	}
	
	public void setFFTManager(FFTManager fftManager) {
		this.fftManager = fftManager;
	}

	public void start() {
		//Log.i("UploadManager", "start");
		handler.post(uploadDataRunnable);
	}
	
	public void stop() {
		//Log.i("UploadManager", "stop");
		handler.removeCallbacks(uploadDataRunnable);
	}
	
	public void triggerUpload() {
		//Log.i("UploadManager", "triggerUpload");
		checkForUnUploadedData();
	}
	
	private void checkForUnUploadedData() {
		
		String authToken = Device.getInstance(context).getAuthToken();
		
		if (authToken == null || authToken.length() == 0) {
			registerDevice();
			return;
		}
//		Log.i("UploadManager", "checkForUnUploadedData");

		List<MeasurementSession> measurementSessions = VaavudDatabase.getInstance(context).getUnUploadedMeasurementSessions();
//		Log.i("UploadManager","Numer of UnUploadedMeasurentSessions: "+measurementSessions.size());
		if ( measurementSessions!=null && measurementSessions.size()>0){
			for (MeasurementSession measurementSession : measurementSessions) {
//				Log.d("UploadManager","Device Used: "+measurementSession.getWindMeter());			
//				List<MeasurementPoint> measurementPoints = VaavudDatabase.getInstance(context).getMeasurementPoints(measurementSession, measurementSession.getStartIndex(), false);
				List<MeasurementPoint> measurementPoints = VaavudDatabase.getInstance(context).getMeasurementPoints(measurementSession, 0, false);

				int pointCount = measurementPoints.size();
				// check if it is an old session that is still measuring and, if so, mark it as not measuring
				if (measurementSession.isMeasuring() && (System.currentTimeMillis() - measurementSession.getEndTime().getTime()) > IDLE_TIME_BEFORE_MARKING_AS_MEASURED) {
//					Log.i("UploadManager", "Found old MeasurementSession that is still measuring - setting it to not measuring, id=" + measurementSession.getLocalId());
					measurementSession.setMeasuring(false);
					VaavudDatabase.getInstance(context).updateUploadingMeasurementSession(measurementSession);
					// check if session has no new points and is not measuring and, if so, mark it as uploaded
				}
//				Log.d("UploadManager","Measurement Sesion Start Index: "+measurementSession.getStartIndex() + "measurementPoints Size: "+measurementPoints.size() + "Is Measuring: "+measurementSession.isMeasuring());
				if (measurementSession.getStartIndex() == pointCount && (pointCount>0 || !measurementSession.isMeasuring())) {
					if (!measurementSession.isMeasuring()) {
//						Log.i("UploadManager", "Found MeasurementSession that is not measuring and has no new points, so setting it as uploaded, id=" + measurementSession.getLocalId());
						measurementSession.setUploaded(true);
						VaavudDatabase.getInstance(context).updateUploadingMeasurementSession(measurementSession);
					}
					else{
//						Log.i("UploadManager",  "Found MeasurementSession that is not uploaded, is still measuring, but has no new points, so skipping");
					}
				}
				else {
					final int newEndIndex = pointCount;
					final long measurementSessionId = measurementSession.getLocalId();
					measurementSession.setPoints(measurementPoints);
					measurementSession.setEndIndex(newEndIndex);
	
//					Log.d("UploadManager", "Uploading measurement session from startIndex=" + measurementSession.getStartIndex() + " to endIndex=" + measurementSession.getEndIndex() + " number of points=" + pointCount);
	
					AuthGsonRequest<Object> request = new AuthGsonRequest<Object>(BASE_URL + "/api/measure", authToken, measurementSession, Object.class,
							new Listener<Object>() {
								@Override
								public void onResponse(Object object) {
//									Log.i("UploadManager", "Got successful response, setting startIndex to " + newEndIndex);
									MeasurementSession measurementSession = VaavudDatabase.getInstance(context).getMeasurementSession(measurementSessionId);
									if (measurementSession == null) {
//										Log.e("UploadManager", "Unexpectedly didn't find measurement session in database, id=" + measurementSessionId);
										return;
									}
									measurementSession.setStartIndex(newEndIndex);
									VaavudDatabase.getInstance(context).updateUploadingMeasurementSession(measurementSession);
								}
							}, new ErrorListener() {
								@Override
								public void onErrorResponse(VolleyError error) {
//									Log.e("UploadManager", "Got error from server uploading: " + error.getMessage());
									NetworkResponse networkResponse = error.networkResponse;
								    if (networkResponse != null && networkResponse.statusCode == HttpStatus.SC_UNAUTHORIZED) {
								       registerDevice();
								    }

								}
							});
					request.setRetryPolicy(new DefaultRetryPolicy(REQUEST_TIMEOUT_MS, 0, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
	//				Log.d("UploadManager","Creating request: "+request.toString());
					requestQueue.add(request);
					break;
				}
			}
		}
		uploadMagneticData();
	}

	private void uploadMagneticData() {
		if (Device.getInstance(context).getUploadMagneticData()) {
			MeasurementSession measurementSession = measurementController.currentSession();
			if (measurementSession != null) {
				MagneticSession magneticSession = dataManager.getNewMagneticfieldMeasurements(measurementSession.getUuid());

				if (magneticSession != null) {
					String authToken = Device.getInstance(context).getAuthToken();
					AuthGsonRequest<Object> request = new AuthGsonRequest<Object>(BASE_URL + "/api/magnetic/measure", authToken, magneticSession, Object.class,
							new Listener<Object>() {
								@Override
								public void onResponse(Object object) {
									//Log.i("UploadManager", "Got successful response uploading magnetic data");
								}
							}, new ErrorListener() {
								@Override
								public void onErrorResponse(VolleyError error) {
									//Log.e("UploadManager", "Got error from server uploading magnetic data: " + error.getMessage());
								}
							});
					request.setRetryPolicy(new DefaultRetryPolicy(REQUEST_TIMEOUT_MS, MAGNETIC_RETRIES, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
					requestQueue.add(request);
				}
			}
		}
	}
	
	public void registerDevice() {
//		Log.d("UploadManager", "Register Device");
    	Device device = Device.getInstance(context);
//    	Log.d("UploadManager", "UUID: "+device.getUuid()+ " AuthToken:" + device.getAuthToken());
    	GsonRequest<RegisterDeviceResponse> request = new GsonRequest<RegisterDeviceResponse>(UploadManager.BASE_URL + "/api/device/register",
				device, RegisterDeviceResponse.class,
				new Listener<RegisterDeviceResponse>() {
					@Override
					public void onResponse(RegisterDeviceResponse object) {
//						Log.i("UploadManager", "Got successful response registering device");

				    	registerDeviceFired = false;

						Device device = Device.getInstance(context);

						if (object.getAuthToken() != null && object.getAuthToken().length() > 0) {
							device.setAuthToken(context, object.getAuthToken());
						}
						else {
							//Log.e("UploadManager", "Got no authToken");
						}
						
						if (object.getUploadMagneticData() != null) {
							//Log.i("UploadManager", "Setting upload magnetic data to " + object.getUploadMagneticData());
							device.setUploadMagneticData(context, object.getUploadMagneticData());
						}
						
						if (object.getHourOptions() != null && object.getHourOptions().length > 0) {
							device.setHourOptions(context, object.getHourOptionsAsPrimitive());
						}
						
						if (object.getMaxMapMarkers() != null && object.getMaxMapMarkers() > 0) {
							device.setMaxMapMarkers(context, object.getMaxMapMarkers());
						}
						
						boolean reconfigureFFTManager = false;
						
						if (object.getFrequencyStart() != null) {
							device.setFrequencyStart(context, object.getFrequencyStart());
							reconfigureFFTManager = true;
						}
						
						if (object.getFrequencyFactor() != null) {
							device.setFrequencyFactor(context, object.getFrequencyFactor());
							reconfigureFFTManager = true;
						}
						
						if (object.getCreationTime() != null){
							JSONObject props = new JSONObject();
					    	try {
					    		props.put("Creation Time", new Date(object.getCreationTime()));
					    	}catch (JSONException e) {
								e.printStackTrace();
							}
					    	if (device.isMixpanelEnabled() && context!=null){
					    		MixpanelAPI.getInstance(context,MIXPANEL_TOKEN).registerSuperPropertiesOnce(props);
					    	}
//							device.setCreationTime(context,new Date(object.getCreationTime()));
						}
						
						if(object.isEnableMixpanel()){
							device.setMixpanelEnabled(context, true);
						}else{
							device.setMixpanelEnabled(context, false);
						}
						
						if(object.isEnableMixpanelPeople()){
							device.setMixpanelPeopleEnabled(context, true);
						}else{
							device.setMixpanelPeopleEnabled(context, false);
						}
						
						if (reconfigureFFTManager & fftManager!=null) {
							fftManager.configure(context);
						}
												
						triggerUpload();
						
						if (measurementsResponseListener != null) {
							readMeasurements();
						}
					}
				}, new ErrorListener() {
					@Override
					public void onErrorResponse(VolleyError error) {
//						Log.e("UploadManager", "Got error from server registering device: " + error.getMessage());
				    	registerDeviceFired = false;
					}
				});
    	request.setRetryPolicy(new DefaultRetryPolicy(REQUEST_TIMEOUT_MS, 3, 1.5F));
    	registerDeviceFired = true;
		requestQueue.add(request);
	}
	
	
	public void deleteMeasurement(MeasurementSession measurement) {
		String authToken=null;
		
		authToken = User.getInstance(context).getAuthToken();
//		}else{
//			Log.e("UploadManager", "User is NULL");
		if (historyMeasurementsRequestFired) {
//			Log.i("UploadManager", "historyMeasurementsRequestFired=true");
			return;
		}
		else if (authToken == null || authToken.length() == 0) {
//			Log.i("UploadManager", "authToken null");
			if (!registerDeviceFired && historyMeasurementsResponseListener != null) {
				
				historyMeasurementsResponseListener.measurementsLoadingFailed();
				historyMeasurementsResponseListener = null;
				historyMeasurementsRequestFired = false;
			}
			return;
		}
		
		//		Log.i("UploadManager", "Launch Request Date: "+lastReadMeasurement.toString());
		DeleteMeasurementRequestParameters measurementsRequestParameters = new DeleteMeasurementRequestParameters(measurement);

		AuthGsonRequest<JsonObject> request = new AuthGsonRequest<JsonObject>(UploadManager.BASE_URL + "/api/measurement/delete",
				authToken, measurementsRequestParameters, JsonObject.class,
				new Listener<JsonObject>() {
					@Override
					public void onResponse(JsonObject measurementValues) {
//						Log.i("UploadManager", "Got successful response reading measurements");
					}
				}, new ErrorListener() {
					@Override
					public void onErrorResponse(VolleyError error) {
//						Log.e("UploadManager", "Got error from server reading measurements: " + error.getMessage());
						if (((MainActivity)context).getProgressDialog()!=null) ((MainActivity)context).getProgressDialog().dismiss();
						if (deleteMeasurementResponseListener != null) {
							deleteMeasurementResponseListener.deleteMeasurementFailed();
						}
						deleteMeasurementResponseListener = null;
					}
				});
//		Log.d("UploadManager",request.toString());
    	request.setRetryPolicy(new DefaultRetryPolicy(REQUEST_TIMEOUT_MS, 3, 1.5F));
		requestQueue.add(request);
	}
	
	public void triggerReadMeasurements(int hoursAgo, final MeasurementsResponseListener listener) {
		this.hoursAgo = hoursAgo;
		this.measurementsResponseListener = listener;
		readMeasurements();
	}
	
	public void triggerReadHistoryMeasurements(Date lastReadMeasurement, String hash , final HistoryMeasurementsResponseListener listener) {
//		Log.i("UploadManager", "triggerReadHistoryMeasurements = " + lastReadMeasurement.toString());
		this.lastReadMeasurement = lastReadMeasurement;
		this.historyMeasurementsResponseListener = listener;
		readHistoryMeasurements(lastReadMeasurement,hash);
	}
	
	private void readHistoryMeasurements(Date lastReadMeasurement,String hash) {
		String authToken=null;
		authToken = User.getInstance(context).getAuthToken();
		
		if (authToken == null || authToken.length() == 0) {
//			Log.i("UploadManager", "authToken null");
			if (!registerDeviceFired && historyMeasurementsResponseListener != null) {
				historyMeasurementsResponseListener.measurementsLoadingFailed();
				historyMeasurementsResponseListener = null;
				historyMeasurementsRequestFired = false;
			}
			return;
		}
		if (historyMeasurementsRequestFired) {
//			Log.i("UploadManager", "historyMeasurementsRequestFired=true");
			return;
		}

		historyMeasurementsRequestFired = true;
		
//		Log.i("UploadManager", "Launch Request Date: "+lastReadMeasurement.toString());
		HistoryMeasurementRequestParameters measurementsRequestParameters = new HistoryMeasurementRequestParameters(lastReadMeasurement,hash);
//		if (measurementsRequestParameters.getEndTime()!=null){
//			Log.d("UploadManager","Request Parameters get Time not null");
//		}
		AuthGsonRequest<JsonObject> request = new AuthGsonRequest<JsonObject>(UploadManager.BASE_URL + "/api/history",
				authToken, measurementsRequestParameters, JsonObject.class,
				new Listener<JsonObject>() {
					@Override
					public void onResponse(JsonObject measurementValues) {
//						Log.i("UploadManager", "Got successful response reading measurements");
						ArrayList<MeasurementSession> histObjList = null;
						if (historyMeasurementsResponseListener != null) {
							JsonArray histJson =  measurementValues.getAsJsonArray("measurements");
							if (histJson != null && histJson.size() > 0) {
								histObjList = new ArrayList<MeasurementSession>();
								
								for (int i=0;i<histJson.size();i++){
									MeasurementSession tmp = new MeasurementSession();
									JsonObject obj = histJson.get(i).getAsJsonObject();
									tmp.setEndTime(new Date(obj.get("endTime").getAsLong()));
									tmp.setDevice(obj.get("deviceUuid").getAsString());
									tmp.setStartTime(new Date(obj.get("startTime").getAsLong()));
									tmp.setUuid(obj.get("uuid").getAsString());
									tmp.setMeasuring(false);
									tmp.setUploaded(true);
									if(!(obj.get("windSpeedAvg") instanceof JsonNull)) tmp.setWindSpeedAvg(obj.get("windSpeedAvg").getAsFloat());
									if(!(obj.get("windSpeedMax") instanceof JsonNull)) tmp.setWindSpeedMax(obj.get("windSpeedMax").getAsFloat());
									if(!(obj.get("windDirection") instanceof JsonNull)) tmp.setWindDirection(obj.get("windDirection").getAsFloat());
									if(!(obj.get("latitude") instanceof JsonNull || obj.get("longitude") instanceof JsonNull)){
										tmp.setPosition(new LatLng(obj.get("latitude").getAsDouble(),obj.get("longitude").getAsDouble()));
									}
									JsonArray pointsJson = obj.getAsJsonArray("points");
									List<MeasurementPoint> pointsObj = new ArrayList<MeasurementPoint>();
									for(int j=0;j<pointsObj.size();j++){
										MeasurementPoint point = new MeasurementPoint();
										point.setSession(tmp);
										point.setTime(new Date(pointsJson.get(j).getAsJsonObject().get("time").getAsLong()));
										point.setWindSpeed(pointsJson.get(j).getAsJsonObject().get("speed").getAsFloat());
										pointsObj.add(point);
										point = null;
									}
									tmp.setPoints(pointsObj);
									histObjList.add(tmp);
									tmp=null;
								}
								//Log.d("UploadManager","histObj "+0+": "+histObjList.get(0).toString());
							}
							historyMeasurementsResponseListener.measurementsReceived(histObjList);
						}
						historyMeasurementsResponseListener = null;
						historyMeasurementsRequestFired = false;
					}
				}, new ErrorListener() {
					@Override
					public void onErrorResponse(VolleyError error) {
//						Log.e("UploadManager", "Got error from server reading measurements: " + error.getMessage());
						if (historyMeasurementsResponseListener != null) {
							historyMeasurementsResponseListener.measurementsLoadingFailed();
						}
						historyMeasurementsResponseListener = null;
						historyMeasurementsRequestFired = false;
					}
				});
//		Log.d("UploadManager",request.toString());
    	request.setRetryPolicy(new DefaultRetryPolicy(REQUEST_TIMEOUT_MS, 3, 1.5F));
		requestQueue.add(request);

	}



	private void readMeasurements() {
		
		String authToken = Device.getInstance(context).getAuthToken();
		if (measurementsRequestFired) {
			return;
		}
		else if (authToken == null || authToken.length() == 0) {
			if (!registerDeviceFired && measurementsResponseListener != null) {
				measurementsResponseListener.measurementsLoadingFailed();
				measurementsResponseListener = null;
				measurementsRequestFired = false;
			}
			return;
		}

		measurementsRequestFired = true;
		
		Date startTime = new Date(System.currentTimeMillis() - hoursAgo * 3600L * 1000L);
		MeasurementsRequestParameters measurementsRequestParameters = new MeasurementsRequestParameters(startTime);
		
		AuthGsonRequest<Object[][]> request = new AuthGsonRequest<Object[][]>(UploadManager.BASE_URL + "/api/measurements",
				authToken, measurementsRequestParameters, Object[][].class,
				new Listener<Object[][]>() {
					@Override
					public void onResponse(Object[][] measurementValues) {
						//Log.i("UploadManager", "Got successful response reading measurements, length=" + measurementValues.length);
						if (measurementsResponseListener != null) {	
							if (measurementValues != null && measurementValues.length > 0) {
								MapMeasurement[] measurements = new MapMeasurement[measurementValues.length];
								
								for (int i = 0; i < measurementValues.length; i++) {
									Object[] values = measurementValues[i];
									// latitude, longitude, startTime, windSpeedAvg, windSpeedMax
									measurements[i] = new MapMeasurement(((Number)values[0]).doubleValue(),
											                             ((Number)values[1]).doubleValue(),
											                             new Date(((Number)values[2]).longValue()),
											                             values[3] == null ? null : ((Number)values[3]).floatValue(),
											                             values[4] == null ? null : ((Number)values[4]).floatValue(),
									                            		 values[5] == null ? null : ((Number)values[5]).floatValue());
								}
								
								measurementsResponseListener.measurementsReceived(measurements);
							}
							else {
								measurementsResponseListener.measurementsReceived(new MapMeasurement[0]);
							}
						}
						
						measurementsResponseListener = null;
						measurementsRequestFired = false;
					}
				}, new ErrorListener() {
					@Override
					public void onErrorResponse(VolleyError error) {
//						Log.e("UploadManager", "Got error from server reading measurements: " + error.getMessage());
						if (measurementsResponseListener != null) {
							measurementsResponseListener.measurementsLoadingFailed();
						}
						measurementsResponseListener = null;
						measurementsRequestFired = false;
					}
				});
    	request.setRetryPolicy(new DefaultRetryPolicy(REQUEST_TIMEOUT_MS, 3, 1.5F));
    	
//    	Log.d("UploadManager",request.getUrl());
//    	try {
//			Log.d("UploadManager",request.getHeaders().toString());
//		} catch (AuthFailureError e1) {
//			// TODO Auto-generated catch block
//			e1.printStackTrace();
//		}
//    	try {
//			Log.d("UploadManager",new String(request.getBody(), "UTF-8"));
//		} catch (UnsupportedEncodingException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
		requestQueue.add(request);
	}
}
