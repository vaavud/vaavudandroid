package com.vaavud.android.measure;

import android.content.Context;
import android.os.Handler;
import android.util.Log;

import com.firebase.client.Firebase;
import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.vaavud.android.measure.sensor.DataManager;
import com.vaavud.android.measure.sensor.FFTManager;
import com.vaavud.android.measure.sensor.LocationUpdateManager;
import com.vaavud.android.measure.sensor.MagneticFieldSensorManager;
import com.vaavud.android.measure.sensor.OrientationSensorManager;
import com.vaavud.android.model.VaavudDatabase;
import com.vaavud.android.model.entity.Device;
import com.vaavud.android.model.entity.LatLng;
import com.vaavud.android.model.entity.MeasurementPoint;
import com.vaavud.android.model.entity.MeasurementSession;
import com.vaavud.android.network.UploadManager;
import com.vaavud.util.Constants;
import com.vaavud.util.UUIDUtil;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

public class VaavudCoreController implements MeasurementController {

		private Context mContext;
		private Context appContext;
		private Device device;
		private MagneticFieldSensorManager myMagneticFieldSensorManager;
		private OrientationSensorManager orientationSensorManager;
		private DataManager dataManager;
		private FFTManager myFFTManager;
		private UploadManager uploadManager;
		private LocationUpdateManager locationManager;
		private boolean isMeasuring = false;
		private MeasurementSession currentSession;
		private Handler handler;
		private List<MeasurementReceiver> measurementReceivers;
		private MeasureStatus status;

		private Firebase firebaseClient;
		private GeoFire geoFireSessionClient;
		private String firebaseSessionKey;

		private static final String TAG = "Vaavud:MjolnirCore";


		private Runnable readDataRunnable = new Runnable() {
				@Override
				public void run() {
						if (isMeasuring) {
								updateMeasureStatus();
								readData();
								handler.postDelayed(readDataRunnable, 500);
						}
				}
		};

		public VaavudCoreController(Context context, DataManager dataManager, UploadManager uploadManager, LocationUpdateManager locationManager) {
//				Log.d("VaavudCoreController", "Vaavud Core Controller Context: " + context);
				this.mContext = context;
				this.appContext = context.getApplicationContext();
				this.dataManager = dataManager;
				this.uploadManager = uploadManager;
				this.locationManager = locationManager;
				measurementReceivers = new ArrayList<MeasurementReceiver>();
				myMagneticFieldSensorManager = new MagneticFieldSensorManager(appContext, dataManager);
				orientationSensorManager = new OrientationSensorManager(appContext);
				myFFTManager = new FFTManager(appContext, dataManager); // add stuff?
				handler = new Handler();
				device = Device.getInstance(appContext);
				firebaseClient = new Firebase(Constants.FIREBASE_BASE_URL);
				geoFireSessionClient = new GeoFire(new Firebase(Constants.FIREBASE_BASE_URL+Constants.FIREBASE_GEO+Constants.FIREBASE_SESSION));

				startController();
		}

		public FFTManager getFFTManager() {
				return myFFTManager;
		}


		public void startController() {

				myMagneticFieldSensorManager = new MagneticFieldSensorManager(appContext, dataManager);
				orientationSensorManager = new OrientationSensorManager(appContext);
				myFFTManager = new FFTManager(mContext, dataManager); // add stuff?
				handler = new Handler();
				device = Device.getInstance(appContext);
		}

		public void startMeasuring() {
				isMeasuring = true;
				uploadManager.triggerUpload();
				handler.post(readDataRunnable);
				resumeMeasuring();
		}

		public void stopMeasuring() {
				pauseMeasuring();
				isMeasuring = false;
		}

		@Override
		public void pauseMeasuring() {
				if (isMeasuring) {
						myMagneticFieldSensorManager.stopLogging();
						if (orientationSensorManager.isSensorAvailable()) {
								orientationSensorManager.stop();
						}
						myFFTManager.stop();
				}
		}

		@Override
		public void resumeMeasuring() {
				if (isMeasuring) {
						myMagneticFieldSensorManager.startLogging();
						if (orientationSensorManager.isSensorAvailable()) {
								orientationSensorManager.start();
						}
						myFFTManager.start();
						status = MeasureStatus.MEASURING;
						sendStatus(status);
				}
		}

		public void clearData() {
				dataManager.clearData();
				myMagneticFieldSensorManager.clear();
		}

		public Integer getNumberOfMeassurements() {
				return dataManager.getNumberOfMeassurements();
		}

		public Float getTimeSinceStart() {
				return dataManager.getTimeSinceStart();
		}

		public Float getLastMag() {
				return dataManager.getLastMagX();
		}

		public Float getWindspeed() {
				return dataManager.getLastWindspeed();
		}

		public Double getAverageWindspeed() {
				return dataManager.getAverageWindspeed();
		}

		public Float getMaxWindspeed() {
				return dataManager.getMaxWindspeed();
		}

		public Float[] getLatestNewTimeAndWindspeed() {
				return dataManager.getLatestNewTimeAndWindspeed();
		}

		public Float[] getLastTimeAndWindspeed() {
				return dataManager.getLastTimeAndWindspeed();
		}

		public String getMagneticFieldSensorName() {
				return myMagneticFieldSensorManager.getMagneticFieldSensorName();
		}

		private void readData() {
				if (getLatestNewTimeAndWindspeed() != null) {

						Double currentMeanValueMS = getAverageWindspeed();
						Float currentActualValueMS = getWindspeed();
						Float currentMaxValueMS = getMaxWindspeed();

						// always update measurement session's endtime and summary info
						currentSession.setEndTime(new Date());
						if (currentMeanValueMS != null) {
								currentSession.setWindSpeedAvg(currentMeanValueMS.floatValue());
						}
						if (currentMaxValueMS != null) {
								currentSession.setWindSpeedMax(currentMaxValueMS);
						}

						LatLng location = locationManager.getLocation();
						if (location != null) {
								currentSession.setPosition(location);
						}

						VaavudDatabase.getInstance(appContext).updateDynamicMeasurementSession(currentSession);

						// add MeasurementPoint and save to database
						MeasurementPoint measurementPoint = new MeasurementPoint();
						measurementPoint.setSession(currentSession);
						measurementPoint.setTime(new Date());
						measurementPoint.setWindSpeed(currentActualValueMS);
						measurementPoint.setWindDirection(null);

						updateFirebaseDataPoint(measurementPoint);
						VaavudDatabase.getInstance(appContext).insertMeasurementPoint(measurementPoint);

						for (MeasurementReceiver measurementReceiver : measurementReceivers) {
								measurementReceiver.measurementAdded(currentSession, dataManager.getLastTime(), currentActualValueMS, currentMeanValueMS == null ? null : currentMeanValueMS.floatValue(), currentMaxValueMS, null);
						}
				}
		}

		private void updateMeasureStatus() {

				MeasureStatus newStatus = MeasureStatus.MEASURING;

				if (!dataManager.newMeasurementsAvailable()) {
						if (dataManager.getTimeSinceStart() > 2) {
								newStatus = MeasureStatus.NO_SIGNAL;
						}
				}
				if (orientationSensorManager.isSensorAvailable() && orientationSensorManager.isVertical() == false) {
						newStatus = MeasureStatus.KEEP_VERTICAL;
						dataManager.setMeasureIsValid(false);
				} else {
						dataManager.setMeasureIsValid(true);
				}
				if (!status.equals(newStatus)) {
						status = newStatus;
						sendStatus(status);
				}
		}

		private void sendStatus(MeasureStatus status) {
				for (MeasurementReceiver measurementReceiver : measurementReceivers) {
						measurementReceiver.measurementStatusChanged(status);
				}
		}

		@Override
		public MeasurementSession startSession() {
				clearData();

				currentSession = new MeasurementSession();
				currentSession.setUuid(UUIDUtil.generateUUID());
				currentSession.setDevice(device.getUuid());
				currentSession.setSource("vaavud");
				currentSession.setStartTime(new Date());
				currentSession.setTimezoneOffset((long) TimeZone.getDefault().getOffset(currentSession.getStartTime().getTime()));
				currentSession.setEndTime(currentSession.getStartTime());
				currentSession.setMeasuring(true);
				currentSession.setUploaded(false);
				currentSession.setStartIndex(0);
				currentSession.setEndIndex(0);
				currentSession.setPosition(locationManager.getLocation());


				VaavudDatabase.getInstance(appContext).insertMeasurementSession(currentSession);
				updateFirebaseDataSession(currentSession,true);
				startMeasuring();

				return currentSession;
		}

		@Override
		public void stopSession() {
				handler.removeCallbacks(readDataRunnable);
				stopMeasuring();

				currentSession.setMeasuring(false);
				VaavudDatabase.getInstance(appContext).updateDynamicMeasurementSession(currentSession);

				uploadManager.triggerUpload();
				updateFirebaseDataSession(currentSession, false);

				for (MeasurementReceiver measurementReceiver : measurementReceivers) {
						measurementReceiver.measurementFinished(currentSession);
				}

				currentSession = null;
		}

		@Override
		public void stopController() {
//				measurementReceivers.clear();
				measurementReceivers = null;
				myMagneticFieldSensorManager = null;
//				orientationSensorManager.stop();
				orientationSensorManager = null;
//				myFFTManager.stop();
				myFFTManager = null;
				handler = null;
		}

		@Override
		public MeasurementSession currentSession() {
				return currentSession;
		}

		@Override
		public boolean isMeasuring() {
				return currentSession != null;
		}

		@Override
		public void addMeasurementReceiver(MeasurementReceiver measurementReceiver) {
				if (!measurementReceivers.contains(measurementReceiver)) {
						measurementReceivers.add(measurementReceiver);
				}
		}

		@Override
		public void removeMeasurementReceiver(MeasurementReceiver measurementReceiver) {
				measurementReceivers.remove(measurementReceiver);
		}


		private void updateFirebaseDataPoint(MeasurementPoint point) {
				Map<String, Object> data = new HashMap<String, Object>();
				if (firebaseSessionKey.length()>0) {
						data.put("sessionKey", firebaseSessionKey);
						data.put("time", point.getTime().getTime());
						data.put("speed", point.getWindSpeed());
						if (point.getWindDirection() != null) {
								data.put("direction", point.getWindSpeed());
						}
						firebaseClient.child(Constants.FIREBASE_WIND).push().setValue(data);
				}
		}

		private void updateFirebaseDataSession(MeasurementSession session, boolean isFirstTime) {

				Map<String, Object> data = new HashMap<String, Object>();
				if (isFirstTime){

						data.put("timeStart", session.getStartTime().getTime());
						data.put("deviceKey", session.getDevice());
						firebaseSessionKey = firebaseClient.child(Constants.FIREBASE_SESSION).push().getKey();
						firebaseClient.child(Constants.FIREBASE_SESSION).child(firebaseSessionKey).setValue(data);

				}else {
//						Log.d(TAG,"FirebaseSessionKey: "+firebaseSessionKey);
						data.put("timeStart", session.getStartTime().getTime());
						data.put("deviceKey", session.getDevice());
						data.put("timeStop", session.getEndTime().getTime());
						data.put("timeUploaded", new Date().getTime());
						if (session.getWindSpeedAvg()!=null && session.getWindSpeedMax()!=null) {
								data.put("windMean", session.getWindSpeedAvg());
								data.put("windMax", session.getWindSpeedMax());
						}
						if (session.getWindDirection() != null) {
								data.put("windDirection", session.getWindDirection());
						}
						if (session.getPosition() != null) {
								data.put("locLat", session.getPosition().getLatitude());
								data.put("locLon", session.getPosition().getLongitude());
								geoFireSessionClient.setLocation(firebaseSessionKey, new GeoLocation(session.getPosition().getLatitude(), session.getPosition().getLongitude()));
						}
						firebaseClient.child(Constants.FIREBASE_SESSION).child(firebaseSessionKey).setValue(data);

				}
		}

}
