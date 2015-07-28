package com.vaavud.android.measure;


import android.content.Context;

import android.os.Handler;
import android.util.Log;

import com.vaavud.android.R;
import com.vaavud.android.measure.sensor.DataManager;
import com.vaavud.android.measure.sensor.LocationUpdateManager;
import com.vaavud.android.model.VaavudDatabase;
import com.vaavud.android.model.entity.Device;
import com.vaavud.android.model.entity.LatLng;
import com.vaavud.android.model.entity.MeasurementPoint;
import com.vaavud.android.model.entity.MeasurementSession;
import com.vaavud.android.model.entity.WindMeasurement;
import com.vaavud.android.model.entity.WindMeter;
import com.vaavud.android.network.UploadManager;
import com.vaavud.android.ui.calibration.CalibrationActivity;

import com.vaavud.sleipnirSDK.SleipnirSDKController;
import com.vaavud.sleipnirSDK.listener.SpeedListener;

import com.vaavud.util.UUIDUtil;


import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

public class SleipnirCoreController implements MeasurementController, SpeedListener {

		private static final String KEY_CALIBRATION_COEFFICENTS = "calibrationCoefficients";
		private static final String KEY_PLAYER_VOLUME = "playerVolume";

		private Context mContext;
		private Context appContext;
		private Device device;

		private UploadManager uploadManager;
		private LocationUpdateManager locationManager;

		private MeasurementSession currentSession;
		private DataManager dataManager;
		private boolean mCalibrationMode;
		private Handler handler;
		private List<MeasurementReceiver> measurementReceivers;
		private MeasureStatus status;


		private SleipnirSDKController sleipnirSDK;
		private long initialTime;
		private WindMeasurement wind;
		private Float[] coefficients;
		private Float playerVolume;
		private String mFileName;

		private float calibrationProgress = 0F;


//		private String mFileName;

		private Runnable readDataRunnable = new Runnable() {
				@Override
				public void run() {
						updateMeasureStatus();
						readData();
						handler.postDelayed(readDataRunnable, 200);
				}
		};

		public SleipnirCoreController(Context context, DataManager dataManager, UploadManager uploadManager, LocationUpdateManager locationManager, boolean calibrationMode) {
//				Log.d("SleipnirCoreController", "Sleipnir Core Controller Context: " + context);
				mContext = context;
				appContext = context.getApplicationContext();
				this.uploadManager = uploadManager;
				this.locationManager = locationManager;
				this.dataManager = dataManager;
				mCalibrationMode = calibrationMode;
				device = Device.getInstance(appContext);
				coefficients = device.getCalibrationCoefficients();

				measurementReceivers = new ArrayList<MeasurementReceiver>();

				if (mCalibrationMode){
						String name = device.getModel() + "_" + device.getUuid() + "_" + new SimpleDateFormat("ddMMyy-HHmmss").format(new Date()) + ".raw";
						mFileName = appContext.getExternalCacheDir().getAbsolutePath() + "/" + name;
//						Log.d("SleipnirCoreController",mFileName);
				}
				sleipnirSDK = new SleipnirSDKController(mContext,mCalibrationMode,this,null,coefficients,mFileName);

		}

		public void startController() {
//				Log.d("SleipnirCoreController","Start Controller");
				wind = new WindMeasurement();
				handler = new Handler();
				initialTime = 0;
				sleipnirSDK.startController();
		}

		public void startMeasuring() {
				clearData();
//		Log.d("SleipnirCoreController", "Start Measuring");
				sleipnirSDK.startMeasuring();
				initialTime = new Date().getTime();
				if (!mCalibrationMode) {
						uploadManager.triggerUpload();
						status = MeasureStatus.MEASURING;
						sendStatus(status);
				}
		}

		public void stopMeasuring() {
				sleipnirSDK.stopMeasuring();
		}

		@Override
		public void pauseMeasuring() {
//				sleipnirSDK.pauseMeasuring();
		}

		@Override
		public void resumeMeasuring() {
//				sleipnirSDK.resumeMeasuring();
		}

		public void clearData() {
				if (dataManager != null) dataManager.clearData();
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

		public Float getLastWindDirection() {
				return dataManager.getLastWindDirection();
		}

		private void readData() {

				if (getLatestNewTimeAndWindspeed() != null) {
						Double currentMeanValueMS = getAverageWindspeed();
						Float currentActualValueMS = getWindspeed();
						Float currentMaxValueMS = getMaxWindspeed();
						Float currentDirection = getLastWindDirection();
						Float orientationAngle = (float) sleipnirSDK.getOrientationAngle();


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
						Float direction = 0f;
						if (currentDirection != null) {
								if (orientationAngle != null) {
//	        		Log.d("SleipnirCoreController","Current Direction: "+currentDirection + " Current Orientation: "+orientationAngle);
										direction = currentDirection + orientationAngle;
										if (direction > 360) {
												direction -= 360;
										}
										currentSession.setWindDirection(direction);
										currentDirection = direction;
								} else {
										currentSession.setWindDirection(currentDirection);
								}
//	        	Log.d("SleipnirCoreController","Current Direction: "+currentDirection + " Current Orientation: "+orientationAngle + " estimated Direction: "+direction);
						}
//	        Log.d("SleipnirCoreController","Read Data WindMeter: "+currentSession.getWindMeter());
						VaavudDatabase.getInstance(appContext).updateDynamicMeasurementSession(currentSession);

						// add MeasurementPoint and save to database
						MeasurementPoint measurementPoint = new MeasurementPoint();
						measurementPoint.setSession(currentSession);
						measurementPoint.setTime(new Date());
						measurementPoint.setWindSpeed(currentActualValueMS);
						measurementPoint.setWindDirection(currentDirection);

						VaavudDatabase.getInstance(appContext).insertMeasurementPoint(measurementPoint);

						for (MeasurementReceiver measurementReceiver : measurementReceivers) {
								measurementReceiver.measurementAdded(currentSession, dataManager.getLastTime(), currentActualValueMS, currentMeanValueMS == null ? null : currentMeanValueMS.floatValue(), currentMaxValueMS, currentDirection);
						}
				}
		}

		private void updateMeasureStatus() {
//		Log.d("SleipnirCoreController", "updateMeasureStatus");
				MeasureStatus newStatus = MeasureStatus.MEASURING;

				if (!dataManager.newMeasurementsAvailable()) {
						if (dataManager.getLastTime() + 2 < (new Date().getTime()/ (float) 1000)) {
								newStatus = MeasureStatus.NO_AUDIO_SIGNAL;
						}
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
//    	Log.d("SleipnirCoreController", "Start Session");
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
				currentSession.setWindMeter(WindMeter.SLEIPNIR);

				VaavudDatabase.getInstance(appContext).insertMeasurementSession(currentSession);

				startMeasuring();

				handler.post(readDataRunnable);

				return currentSession;
		}

		@Override
		public void stopSession() {
//		Log.d("SleipnirCoreController", "Stop Session "+currentSession.getWindMeter());
				handler.removeCallbacks(readDataRunnable);

				stopMeasuring();
				currentSession.setMeasuring(false);
				VaavudDatabase.getInstance(appContext).updateDynamicMeasurementSession(currentSession);

				uploadManager.triggerUpload();

				for (MeasurementReceiver measurementReceiver : measurementReceivers) {
						measurementReceiver.measurementFinished(currentSession);
				}
				currentSession = null;
				handler = null;
				measurementReceivers.clear();
		}

		@Override
		public void stopController() {

				sleipnirSDK.stopController();
		}

		@Override
		public MeasurementSession currentSession() {
				return currentSession;
		}

		@Override
		public boolean isMeasuring() {
				return (currentSession != null || sleipnirSDK.isMeasuring());
		}

//		public boolean isStarted() {
//				return sleipnirSDK.getOrientationAngle() != null;
//		}

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

		@Override
		public void speedChanged(float speed, float windDirection, long timestamp, float velocityProfileError) {
				wind = new WindMeasurement();
				wind.windspeed = (float) ((speed * 0.325) + 0.2);
				wind.windDirection = windDirection;
				wind.time = (float) (timestamp - initialTime) / (float) 1000;
				if (dataManager != null) {
						dataManager.addWindMeasurement(wind);
				}

//		Log.d("SpeedChanged", "Timestamp: " + timestamp + " Speed: " + wind.windspeed + " Wind Time: " + wind.time);
		}

		public String getFileName() {
				return mFileName;
		}

		public float getCalibrationProgress() {
				return calibrationProgress;
		}

		public Float getPlayerVolume() {
				return playerVolume;
		}

		public void setCalibrationProgress(float calibrationProgress) {
//				Log.d("SleipnirCoreController", "SetCalibrationProgress "+calibrationProgress);
				this.calibrationProgress = calibrationProgress;
		}

		@Override
		public void calibrationPercentageComplete(float percentage) {
				setCalibrationProgress(percentage);
		}

		@Override
		public void calibrationCoefficients(Float[] coefficients) {
				if (mContext instanceof CalibrationActivity) {
						CalibrationActivity activity = (CalibrationActivity) mContext;
						activity.calibrationCoefficients(coefficients);
				}
		}


}
