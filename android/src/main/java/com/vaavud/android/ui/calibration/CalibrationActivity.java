package com.vaavud.android.ui.calibration;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.OrientationEventListener;

import com.vaavud.android.R;
import com.vaavud.android.model.entity.Device;
import com.vaavud.sleipnirSDK.HeadsetIntentReceiver;
import com.vaavud.sleipnirSDK.listener.PlugListener;

import java.io.File;

public class CalibrationActivity extends AppCompatActivity implements PlugListener {

		private Boolean firstTime;
		private Float[] coefficients = new Float[15];
		private boolean isSleipnirPlugged = false;
		private HeadsetIntentReceiver receiver;
		private static final int ROTATION_THRESHOLD = 45;
		private OrientationEventListener orientationListener;
		private Orientation orientation = Orientation.PORTRAIT;

		private enum Orientation {
				PORTRAIT,
				REVERSE_PORTRAIT
		}

		@Override
		protected void onCreate(Bundle savedInstanceState) {
				super.onCreate(savedInstanceState);
				setContentView(R.layout.activity_calibration);

				getSupportActionBar().setDisplayHomeAsUpEnabled(true);
				getSupportActionBar().setDisplayUseLogoEnabled(false);
				getSupportActionBar().setDisplayShowTitleEnabled(true);
				getSupportActionBar().setTitle(R.string.title_activity_calibration);

				orientationListener = new OrientationEventListener(this, SensorManager.SENSOR_DELAY_UI) {
						@Override
						public void onOrientationChanged(int orientationDegrees) {
								if (isPortrait(orientationDegrees) && orientation != Orientation.PORTRAIT) {
										//Log.i("MainActivity", "Portrait");
										orientation = Orientation.PORTRAIT;
										setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
								} else if (isReversePortrait(orientationDegrees) && orientation != Orientation.REVERSE_PORTRAIT) {
										//Log.i("MainActivity", "Reverse Portrait");
										orientation = Orientation.REVERSE_PORTRAIT;
										setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT);
								}
						}
				};

				IntentFilter receiverFilter = new IntentFilter(Intent.ACTION_HEADSET_PLUG);
				receiver = new HeadsetIntentReceiver(this);
				registerReceiver(receiver, receiverFilter);

				Intent i = getIntent();
				if (i != null) {
						firstTime = i.getBooleanExtra("firstTime", false);
				}
				deleteCache(this);
				getSupportFragmentManager().beginTransaction()
								.add(R.id.container, new InitCalibrationFragment()).commit();

		}

		@Override
		protected void onSaveInstanceState(Bundle _outState) {
				super.onSaveInstanceState(_outState);
		}

		@Override
		public void onResume(){
				super.onResume();
				orientationListener.enable();
		}

		@Override
		protected void onPause() {
				super.onPause();
				orientationListener.disable();
		}

		private boolean isPortrait(int orientation) {
				return (orientation >= (360 - ROTATION_THRESHOLD) && orientation <= 360) || (orientation >= 0 && orientation <= ROTATION_THRESHOLD);
		}

		private boolean isReversePortrait(int orientation) {
				return orientation >= (180 - ROTATION_THRESHOLD) && orientation <= (180 + ROTATION_THRESHOLD);
		}


		@Override
		protected void onDestroy() {
//		Log.i(TAG, "onDestroy");
				unregisterReceiver(receiver);
				super.onDestroy();
		}

		public void calibrationCoefficients(Float[] coefficients) {
				this.coefficients = coefficients;


//		Log.d("VaavudCalibration","Coefficients: "+ coefficientsString);
				Device.getInstance(getApplicationContext()).setCalibrationCoefficients(getApplicationContext(), coefficients);
//		VaavudDatabase.getInstance(this).setProperty(KEY_CALIBRATION_COEFFICENTS, coefficientsString);
		}

		public boolean isFirstTime() {
				return firstTime;
		}

		private void deleteCache(Context context) {
				try {
						File dir = context.getExternalCacheDir();
						if (dir != null && dir.isDirectory()) {
								deleteDir(dir);
						}
				} catch (Exception e) {
				}
		}

		private boolean deleteDir(File dir) {
				if (dir != null && dir.isDirectory()) {
						String[] children = dir.list();
						for (int i = 0; i < children.length; i++) {
								if (children[i].contains(".raw")) {
//    	        	Log.d("CalibrationActivity","Children: "+children[i]+ " Device: "+Device.getInstance(this).getModel());
										boolean success = deleteDir(new File(dir, children[i]));
										if (!success) {
												return false;
										}
								}
						}
				}
				return dir.delete();
		}

		@Override
		public void isSleipnirPlugged(boolean plugged) {
//		Log.d("Calibration Activity","Sleipnir plugged: "+plugged);
				isSleipnirPlugged = plugged;
		}

		public boolean getIsSleipnirPlugged() {
				return isSleipnirPlugged;
		}
}
