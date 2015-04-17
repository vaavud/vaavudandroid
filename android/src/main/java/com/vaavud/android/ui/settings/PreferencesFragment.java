package com.vaavud.android.ui.settings;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.net.Uri;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.SwitchPreference;
import android.support.v4.preference.PreferenceFragment;
import android.telephony.TelephonyManager;

import com.mixpanel.android.mpmetrics.MixpanelAPI;
import com.vaavud.android.R;
import com.vaavud.android.measure.SleipnirCoreController;
import com.vaavud.android.model.entity.Device;
import com.vaavud.android.model.entity.DirectionUnit;
import com.vaavud.android.model.entity.SpeedUnit;
import com.vaavud.android.ui.BackPressedListener;
import com.vaavud.android.ui.MainActivity;
import com.vaavud.android.ui.SelectedListener;
import com.vaavud.android.ui.SelectorListener;
import com.vaavud.android.ui.calibration.CalibrationActivity;
import com.vaavud.android.ui.tour.TourActivity;

public class PreferencesFragment extends PreferenceFragment implements BackPressedListener,SelectedListener,SelectorListener{
	
		private static final String TAG = "PreferencesFragment";
		private SelectorListener mCallback;
		private Preference headingUnit;
		private Preference directionUnit;
		private Preference about;
		private Preference buy;
		private Preference useFlow;
		private Preference calibrationFlow;
		private SwitchPreference facebook;
		private CheckBoxPreference facebookGB;
		SharedPreferences pref;
		
		private static final String MIXPANEL_TOKEN = "757f6311d315f94cdfc8d16fb4d973c0";

		@Override
		public void onAttach(Activity activity) {
		  super.onAttach(activity);
		  try {
	    	  mCallback = (SelectorListener) activity;
	      } catch (ClassCastException e) {
	          throw new ClassCastException(activity.toString() + " must implement SelectorListener");
	      }
		}
		
		@Override
		public void onCreate(Bundle savedInstanceState) {
//			Log.i(TAG, "onCreate");
			super.onCreate(savedInstanceState);
			if (android.os.Build.VERSION.SDK_INT > android.os.Build.VERSION_CODES.GINGERBREAD_MR1){
				addPreferencesFromResource(R.xml.preferences);
			}else{
				addPreferencesFromResource(R.xml.preferences_gb);
			}
			
			
			pref = getActivity().getPreferences(Context.MODE_PRIVATE);
			
			about = (Preference) findPreference("about_fragment");
			about.setOnPreferenceClickListener(new OnPreferenceClickListener() {
				
				@Override
				public boolean onPreferenceClick(Preference preference) {
					onMenuOptionSelected(4);
					return false;
				}
			});
			headingUnit = findPreference("heading_unit");

			((ListPreference)headingUnit).setValue(pref.getString(headingUnit.getKey(), "MS"));
	        headingUnit.setSummary(SpeedUnit.valueOf(pref.getString(headingUnit.getKey(), "MS")).getDisplayName(getActivity()));
			headingUnit.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
				
				@Override
				public boolean onPreferenceChange(Preference preference, Object newValue) {
					Device.getInstance(getActivity()).setWindSpeedUnit(getActivity(), SpeedUnit.valueOf((String)newValue));
			        preference.setSummary(SpeedUnit.valueOf((String)newValue).getDisplayName(getActivity()));
			        Editor edit = pref.edit();
			        edit.putString(headingUnit.getKey(), (String)newValue);
			        edit.commit();
			        edit = null;
					return false;
				}
			});
			
			directionUnit = findPreference("direction_unit");

			((ListPreference)directionUnit).setValue(pref.getString(directionUnit.getKey(), "CARDINAL"));
			directionUnit.setSummary(DirectionUnit.valueOf(pref.getString(directionUnit.getKey(), "CARDINAL")).getDisplayName(getActivity()));
			directionUnit.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
				@Override
				public boolean onPreferenceChange(Preference preference, Object newValue) {
					Device.getInstance(getActivity()).setWindDirectionUnit(getActivity(), DirectionUnit.valueOf((String)newValue));
			        preference.setSummary(DirectionUnit.valueOf((String)newValue).getDisplayName(getActivity()));
			        Editor edit = pref.edit();
			        edit.putString(directionUnit.getKey(), (String)newValue);
			        edit.commit();
			        edit = null;
					return false;
				}
			});

			
			if (android.os.Build.VERSION.SDK_INT > android.os.Build.VERSION_CODES.GINGERBREAD_MR1){
				facebook = (SwitchPreference) findPreference("FacebookSharing");
				facebook.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
					@Override
					public boolean onPreferenceChange(Preference preference, Object newValue) {
						((MainActivity)getActivity()).setFacebookSharing((Boolean) newValue);
						Editor edit = pref.edit();
				        edit.putBoolean(facebook.getKey(), (Boolean)newValue);
				        edit.commit();
				        edit = null;
						return true;
					}
				});
			}else{
				facebookGB = (CheckBoxPreference) findPreference("FacebookSharing");
				facebookGB.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
					@Override
					public boolean onPreferenceChange(Preference preference, Object newValue) {
						((MainActivity)getActivity()).setFacebookSharing((Boolean) newValue);
						Editor edit = pref.edit();
				        edit.putBoolean(facebookGB.getKey(), (Boolean)newValue);
				        edit.commit();
				        edit = null;
						return true;
					}
				});
			}
			buy = (Preference) findPreference("buy");
			
			TelephonyManager tm = (TelephonyManager) getActivity().getSystemService(Context.TELEPHONY_SERVICE);
			tm.getNetworkCountryIso();
			Intent iBuy=null;
			if (getActivity()!=null && Device.getInstance(getActivity()).isMixpanelEnabled()){
				iBuy = new Intent(Intent.ACTION_VIEW, Uri.parse("http://vaavud.com/mobile-shop-redirect/?country="+tm.getNetworkCountryIso()+"&language=en&ref="+
					MixpanelAPI.getInstance(getActivity(), MIXPANEL_TOKEN).getDistinctId()+"&source=settings"));
			}else{
				iBuy = new Intent(Intent.ACTION_VIEW, Uri.parse("http://vaavud.com/mobile-shop-redirect/?country="+tm.getNetworkCountryIso()+"&language=en&source=settings"));
			}
			tm=null;
			buy.setIntent(iBuy);
			buy.setOnPreferenceClickListener(new OnPreferenceClickListener() {
				
				@Override
				public boolean onPreferenceClick(Preference preference) {
					if (getActivity()!=null && Device.getInstance(getActivity()).isMixpanelEnabled()){
						MixpanelAPI.getInstance(getActivity(), MIXPANEL_TOKEN).track("Settings Clicked Buy", null);
					}
					return false;
				}
			});
			
			useFlow = (Preference) findPreference("useFlow");
			Intent iTour = new Intent(getActivity(),TourActivity.class);
			iTour.putExtra("tips", true);
			useFlow.setIntent(iTour);
			useFlow.setOnPreferenceClickListener(new OnPreferenceClickListener() {
				
				@Override
				public boolean onPreferenceClick(Preference preference) {
					if (getActivity()!=null && Device.getInstance(getActivity()).isMixpanelEnabled()){
						MixpanelAPI.getInstance(getActivity(), MIXPANEL_TOKEN).track("Settings Clicked Measuring Tips", null);
					}
					return false;
				}
			});
			
			calibrationFlow = (Preference) findPreference("calibrationFlow");
			Intent calibration = new Intent(getActivity(),CalibrationActivity.class);
			calibration.putExtra("firstTime", false);
			calibrationFlow.setIntent(calibration);
			calibrationFlow.setOnPreferenceClickListener(new OnPreferenceClickListener() {
				
				@Override
				public boolean onPreferenceClick(Preference preference) {
					if (((MainActivity)getActivity()).getMeasurementController() instanceof SleipnirCoreController){
						SleipnirCoreController controller = (SleipnirCoreController) ((MainActivity)getActivity()).getMeasurementController(); 
						if (controller.isMeasuring()){
							controller.stopSession();
						}
						controller.stopController();
					}
					if (getActivity()!=null && Device.getInstance(getActivity()).isMixpanelEnabled()){
						MixpanelAPI.getInstance(getActivity(), MIXPANEL_TOKEN).track("Settings Clicked Calibration", null);
					}
					return false;
				}
			});
		}
		


		@Override
		public boolean onBackPressed() {
			// TODO Auto-generated method stub
			return true;
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
		@Override
		public void onSelected() {
			if (getActivity()!=null && Device.getInstance(getActivity()).isMixpanelEnabled()){
				MixpanelAPI.getInstance(getActivity(),MIXPANEL_TOKEN).track("Settings Screen", null);
			}
//			Log.i(TAG, "onLowMemory");
		}

		@Override
		public void onMenuOptionSelected(int position) {
			mCallback.onMenuOptionSelected(position);
		}
}