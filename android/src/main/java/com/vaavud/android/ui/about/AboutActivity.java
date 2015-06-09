package com.vaavud.android.ui.about;

import android.content.pm.ActivityInfo;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.support.v7.app.AppCompatActivity;
import android.view.OrientationEventListener;

import com.vaavud.android.R;
import com.vaavud.android.ui.settings.SettingsFragment;


public class AboutActivity extends AppCompatActivity {

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
				getSupportActionBar().setTitle(R.string.title_activity_about);


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

				getSupportFragmentManager().beginTransaction()
								.replace(android.R.id.content, new AboutFragment())
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
