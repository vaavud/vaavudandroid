package com.vaavud.android.measure;

import android.content.Context;
import android.os.Handler;
import android.util.Log;

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
import com.vaavud.util.UUIDUtil;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

public class VaavudCoreController implements MeasurementController {

		private Context context;
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
		private List<MeasurementReceiver> measurementReceivers = new ArrayList<MeasurementReceiver>();
		private MeasureStatus status;

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
				Log.d("VaavudCoreController", "Vaavud Core Controller Context: " + context);
				this.context = context;
				this.dataManager = dataManager;
				this.uploadManager = uploadManager;
				this.locationManager = locationManager;
				myMagneticFieldSensorManager = new MagneticFieldSensorManager(context, dataManager);
				orientationSensorManager = new OrientationSensorManager(context);
				myFFTManager = new FFTManager(context, dataManager); // add stuff?
				handler = new Handler();
				device = Device.getInstance(context.getApplicationContext());
		}

		public FFTManager getFFTManager() {
				return myFFTManager;
		}

		public void startMeasuring() {
				isMeasuring = true;
				uploadManager.triggerUpload();
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

						VaavudDatabase.getInstance(context).updateDynamicMeasurementSession(currentSession);

						// add MeasurementPoint and save to database
						MeasurementPoint measurementPoint = new MeasurementPoint();
						measurementPoint.setSession(currentSession);
						measurementPoint.setTime(new Date());
						measurementPoint.setWindSpeed(currentActualValueMS);
						measurementPoint.setWindDirection(null);

						VaavudDatabase.getInstance(context).insertMeasurementPoint(measurementPoint);

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

				VaavudDatabase.getInstance(context).insertMeasurementSession(currentSession);

				startMeasuring();

				handler.post(readDataRunnable);

				return currentSession;
		}

		@Override
		public void stopSession() {
				handler.removeCallbacks(readDataRunnable);
				stopMeasuring();

				currentSession.setMeasuring(false);
				VaavudDatabase.getInstance(context).updateDynamicMeasurementSession(currentSession);

				uploadManager.triggerUpload();

				for (MeasurementReceiver measurementReceiver : measurementReceivers) {
						measurementReceiver.measurementFinished(currentSession);
				}

				currentSession = null;
		}

		@Override
		public void stopController() {
				measurementReceivers.clear();
				measurementReceivers = null;
				myMagneticFieldSensorManager = null;
				orientationSensorManager.stop();
				orientationSensorManager = null;
				myFFTManager.stop();
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
}
