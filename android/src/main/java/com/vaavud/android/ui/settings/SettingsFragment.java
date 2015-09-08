package com.vaavud.android.ui.settings;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.net.Uri;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.SwitchPreference;
import android.preference.PreferenceFragment;
import android.telephony.TelephonyManager;
import android.widget.ImageSwitcher;

import com.google.android.gms.maps.GoogleMap;
import com.mixpanel.android.mpmetrics.MixpanelAPI;
import com.vaavud.android.R;
import com.vaavud.android.model.entity.Device;
import com.vaavud.android.model.entity.DirectionUnit;
import com.vaavud.android.model.entity.SpeedUnit;
import com.vaavud.android.ui.about.AboutActivity;
import com.vaavud.android.ui.calibration.CalibrationActivity;
import com.vaavud.android.ui.tour.TourActivity;

public class SettingsFragment extends PreferenceFragment {

		private static final String TAG = "PreferencesFragment";
		private ListPreference headingUnit;
		private ListPreference directionUnit;
		private Preference aboutFlow;
		private Preference buy;
		private Preference useFlow;
		private Preference calibrationFlow;
		private SwitchPreference facebook;
		private SharedPreferences pref;
		private Context context;

		private static final String MIXPANEL_TOKEN = "757f6311d315f94cdfc8d16fb4d973c0";

		@Override
		public void onAttach(Activity activity) {
		  super.onAttach(activity);
				context = activity;
		}

		@Override
		public void onCreate(Bundle savedInstanceState) {
//			Log.i(TAG, "onCreate");
				super.onCreate(savedInstanceState);
				addPreferencesFromResource(R.xml.preferences);


				pref = context.getApplicationContext().getSharedPreferences("Vaavud", Context.MODE_PRIVATE);

				aboutFlow =  findPreference("about_fragment");
				Intent about = new Intent(context.getApplicationContext(), AboutActivity.class);
				aboutFlow.setIntent(about);
				aboutFlow.setOnPreferenceClickListener(new OnPreferenceClickListener() {
						@Override
						public boolean onPreferenceClick(Preference preference) {
//								DONOTHING;
								return false;
						}
				});

				headingUnit = (ListPreference) findPreference("heading_unit");

				headingUnit.setValue(pref.getString(headingUnit.getKey(), "MS"));
				headingUnit.setSummary(SpeedUnit.valueOf(pref.getString(headingUnit.getKey(), "MS")).getDisplayName(context));
				headingUnit.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {

						@Override
						public boolean onPreferenceChange(Preference preference, Object newValue) {
								Device.getInstance(context.getApplicationContext()).setWindSpeedUnit(context.getApplicationContext(),SpeedUnit.valueOf((String) newValue));
								preference.setSummary(SpeedUnit.valueOf((String) newValue).getDisplayName(context));
								Editor edit = pref.edit();
								edit.putString(headingUnit.getKey(), (String) newValue);
								edit.commit();
								edit = null;
								return false;
						}
				});

				directionUnit = (ListPreference)findPreference("direction_unit");

				directionUnit.setValue(pref.getString(directionUnit.getKey(),"CARDINAL"));
				directionUnit.setSummary(DirectionUnit.valueOf(pref.getString(directionUnit.getKey(),"CARDINAL")).getDisplayName(context));
				directionUnit.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
						@Override
						public boolean onPreferenceChange(Preference preference, Object newValue) {
								Device.getInstance(context.getApplicationContext()).setWindDirectionUnit(context.getApplicationContext(),DirectionUnit.valueOf((String) newValue));
								preference.setSummary(DirectionUnit.valueOf((String) newValue).getDisplayName(context));
								Editor edit = pref.edit();
								edit.putString(directionUnit.getKey(), (String) newValue);
								edit.commit();
								edit = null;
								return false;
						}
				});

				buy = findPreference("buy");

				TelephonyManager tm = (TelephonyManager) getActivity().getSystemService(Context.TELEPHONY_SERVICE);
				tm.getNetworkCountryIso();
				Intent iBuy = null;
				if (context != null && Device.getInstance(context.getApplicationContext()).isMixpanelEnabled()) {
						iBuy = new Intent(Intent.ACTION_VIEW, Uri.parse("http://vaavud.com/mobile-shop-redirect/?country=" + tm.getNetworkCountryIso() + "&language=en&ref=" +
										MixpanelAPI.getInstance(getActivity(), MIXPANEL_TOKEN).getDistinctId() + "&source=settings"));
				} else {
						iBuy = new Intent(Intent.ACTION_VIEW, Uri.parse("http://vaavud.com/mobile-shop-redirect/?country=" + tm.getNetworkCountryIso() + "&language=en&source=settings"));
				}
				tm = null;
				buy.setIntent(iBuy);
				buy.setOnPreferenceClickListener(new OnPreferenceClickListener() {

						@Override
						public boolean onPreferenceClick(Preference preference) {
								if (context != null && Device.getInstance(context.getApplicationContext()).isMixpanelEnabled()) {
										MixpanelAPI.getInstance(context.getApplicationContext(), MIXPANEL_TOKEN).track("Settings Clicked Buy", null);
								}
								return false;
						}
				});

				useFlow = (Preference) findPreference("useFlow");
				Intent iTour = new Intent(context, TourActivity.class);
				iTour.putExtra("tips", true);
				useFlow.setIntent(iTour);
				useFlow.setOnPreferenceClickListener(new OnPreferenceClickListener() {

						@Override
						public boolean onPreferenceClick(Preference preference) {
								if (context != null && Device.getInstance(context.getApplicationContext()).isMixpanelEnabled()) {
										MixpanelAPI.getInstance(context.getApplicationContext(), MIXPANEL_TOKEN).track("Settings Clicked Measuring Tips", null);
								}
								return false;
						}
				});

				calibrationFlow = (Preference) findPreference("calibrationFlow");
				Intent calibration = new Intent(getActivity(), CalibrationActivity.class);
				calibration.putExtra("firstTime", false);
				calibrationFlow.setIntent(calibration);
				calibrationFlow.setOnPreferenceClickListener(new OnPreferenceClickListener() {

						@Override
						public boolean onPreferenceClick(Preference preference) {
								if (context != null && Device.getInstance(context.getApplicationContext()).isMixpanelEnabled()) {
										MixpanelAPI.getInstance(context.getApplicationContext(), MIXPANEL_TOKEN).track("Settings Clicked Calibration", null);
								}
								return false;
						}
				});
		}


		@Override
		public void onActivityCreated(Bundle savedInstanceState) {
				super.onActivityCreated(savedInstanceState);
//			Log.i(TAG, "onActivityCreated, savedInstanceState" + (savedInstanceState == null ? "=null" : "!=null"));
		}

		@Override
		public void onViewStateRestored(Bundle savedInstanceState) {
				super.onViewStateRestored(savedInstanceState);
		}

		@Override
		public void onStart() {
				super.onStart();
//			Log.i(TAG, "onStart");
		}

		@Override
		public void onResume() {
				super.onResume();
		}

		@Override
		public void onPause() {
				super.onPause();
//			Log.i(TAG, "onPause");
		}

		@Override
		public void onStop() {
				super.onStop();
//			Log.i(TAG, "onStop");
		}

		@Override
		public void onDestroyView() {
				super.onDestroyView();
//			Log.i(TAG, "onDestroyView");

		}

		@Override
		public void onDestroy() {
				super.onDestroy();
//			Log.i(TAG, "onDestroy");

		}

		@Override
		public void onDetach() {
				super.onDetach();
//			Log.i(TAG, "onDetach");
		}

		@Override
		public void onSaveInstanceState(Bundle outState) {
				super.onSaveInstanceState(outState);
//			Log.i(TAG, "onSaveInstanceState");

		}

		@Override
		public void onLowMemory() {
				super.onLowMemory();
//			Log.i(TAG, "onLowMemory");

		}
}