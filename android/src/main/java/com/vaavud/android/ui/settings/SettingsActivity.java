package com.vaavud.android.ui.settings;

import android.content.pm.ActivityInfo;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.preference.PreferenceActivity;

import android.support.v7.app.AppCompatActivity;
import android.view.OrientationEventListener;

import com.vaavud.android.R;

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
public class SettingsActivity extends AppCompatActivity {

		private static final int ROTATION_THRESHOLD = 45;
		private OrientationEventListener orientationListener;
		private Orientation orientation = Orientation.PORTRAIT;

		private enum Orientation {
				PORTRAIT,
				REVERSE_PORTRAIT
		}

		@Override
		public void onCreate(Bundle savedInstanceState){
				super.onCreate(savedInstanceState);

				getSupportActionBar().setDisplayHomeAsUpEnabled(true);
				getSupportActionBar().setDisplayUseLogoEnabled(false);
				getSupportActionBar().setDisplayShowTitleEnabled(true);
				getSupportActionBar().setTitle(R.string.title_activity_settings);

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

				getFragmentManager().beginTransaction()
								.replace(android.R.id.content, new SettingsFragment())
								.commit();
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
