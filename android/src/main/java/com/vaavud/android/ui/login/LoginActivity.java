package com.vaavud.android.ui.login;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.support.v7.app.AppCompatActivity;
import android.view.OrientationEventListener;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;
import com.facebook.Session;
import com.vaavud.android.R;
import com.vaavud.android.VaavudApplication;
import com.vaavud.android.model.VaavudDatabase;
import com.vaavud.android.model.entity.MeasurementSession;
import com.vaavud.android.network.listener.HistoryMeasurementsResponseListener;
import com.vaavud.android.ui.SelectedListener;
import com.vaavud.android.ui.SelectorListener;
import com.vaavud.android.ui.about.AboutFragment;

import java.nio.channels.Selector;
import java.util.ArrayList;

/**
 * A {@link PreferenceActivity} that presents a set of application settings. On
 * handset devices, settings are presented as a single list. On tablets,
 * settings are split by category, with category headers shown to the left of
 * the list of settings.
 * <p/>
 * See <a href="http://developer.android.com/design/patterns/settings.html">
 * Android Design: Settings</a> for design guidelines and the <a
 * href="http://developer.android.com/guide/topics/ui/settings.html">Settings
 * API Guide</a> for more information on developing a Settings UI.
 */
public class LoginActivity extends AppCompatActivity {

		private RequestQueue userQueue;
		private RequestQueue dataQueue;

		private boolean fromTour = false;

		private static final int ROTATION_THRESHOLD = 45;
		private OrientationEventListener orientationListener;
		private Orientation orientation = Orientation.PORTRAIT;

		private enum Orientation {
				PORTRAIT,
				REVERSE_PORTRAIT
		}

		@Override
		protected void onActivityResult(int requestCode, int resultCode, Intent data) {
//		Log.d(TAG,"OnActivityResult:"+requestCode+" "+resultCode);
				super.onActivityResult(requestCode, resultCode, data);
				if (Session.getActiveSession() != null) {
//			Log.d(TAG, "Session is not null");
						Session.getActiveSession().onActivityResult(this, requestCode, resultCode, data);
				}
		}

		@Override
		public void onCreate(Bundle savedInstanceState){
				super.onCreate(savedInstanceState);
//				getSupportActionBar().setDisplayHomeAsUpEnabled(true);

				getSupportActionBar().setDisplayUseLogoEnabled(false);
				getSupportActionBar().setDisplayShowTitleEnabled(true);
				getSupportActionBar().setTitle(R.string.title_activity_login);


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

				int position = getIntent().getExtras().getInt("position",0);

				if (userQueue != null) {
						userQueue.stop();
				}
				userQueue = Volley.newRequestQueue(this);

				if (dataQueue != null) {
						dataQueue.stop();
				}
				dataQueue = Volley.newRequestQueue(this);

				switch (position){
						case 1:
								fromTour = true;
								getSupportFragmentManager().beginTransaction()
												.replace(android.R.id.content, new SignUpFragment())
												.commit();
								break;
						case 2:
								fromTour = true;
								getSupportFragmentManager().beginTransaction()
												.replace(android.R.id.content, new LoginFragment())
												.commit();
								break;
						default:
								fromTour = false;
								getSupportFragmentManager().beginTransaction()
												.replace(android.R.id.content, new SelectorFragment())
												.commit();
								break;
				}

		}


		protected RequestQueue getUserQueue(){
			return userQueue;
		}

		protected boolean getFromTour() {
				return fromTour;
		}

		protected RequestQueue getDataQueue(){
				return dataQueue;
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

}
