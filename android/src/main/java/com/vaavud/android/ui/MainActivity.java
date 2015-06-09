package com.vaavud.android.ui;

import android.app.Notification;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.view.ActionMode;
import android.util.Log;
import android.util.SparseArray;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.OrientationEventListener;
import android.view.ViewGroup;


import com.crittercism.app.Crittercism;
import com.facebook.AccessToken;
import com.facebook.AccessTokenSource;
import com.facebook.Session;
import com.mixpanel.android.mpmetrics.MixpanelAPI;
import com.vaavud.android.R;
import com.vaavud.android.measure.MeasurementController;
import com.vaavud.android.measure.SleipnirCoreController;
import com.vaavud.android.measure.VaavudCoreController;
import com.vaavud.android.measure.sensor.DataManager;
import com.vaavud.android.measure.sensor.LocationUpdateManager;
import com.vaavud.android.model.VaavudDatabase;
import com.vaavud.android.model.entity.Device;
import com.vaavud.android.model.entity.SpeedUnit;
import com.vaavud.android.model.entity.User;
import com.vaavud.android.network.UploadManager;
import com.vaavud.android.network.UserManager;
import com.vaavud.android.ui.calibration.CalibrationActivity;
import com.vaavud.android.ui.history.HistoryFragment;
import com.vaavud.android.ui.login.LoginActivity;
import com.vaavud.android.ui.map.MeasurementMapFragment;
import com.vaavud.android.ui.measure.MeasureFragment;
import com.vaavud.android.ui.settings.SettingsActivity;
import com.vaavud.sleipnirSDK.HeadsetIntentReceiver;
import com.vaavud.sleipnirSDK.listener.PlugListener;
import com.vaavud.util.MixpanelUtil;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.UUID;

public class MainActivity extends ActionBarActivity implements SelectedListener, PlugListener {

		private static final int ROTATION_THRESHOLD = 45;
		private static final long GRACE_TIME_BETWEEN_REGISTER_DEVICE_MS = 3600L * 1000L; // 1 hour
		private static final long GRACE_TIME_BETWEEN_RESUME_APP_MS = 1800L * 1000L; // 1 hour
		private static final String KEY_FIRST_TIME_SLEIPNIR = "firstTimeSleipnir";
		private static final String KEY_IS_FIRST_FLOW = "isFirstFlow";
		private static final int LOGIN_REQUEST = 702;
		private static final String TAG = "MAIN_ACTIVITY";

		private static final int MEASURE_TAB = 0;
		private static final int MAP_TAB = 1;
		private static final int HISTORY_TAB = 2;

		//MixPanel
		private static final String MIXPANEL_TOKEN = "757f6311d315f94cdfc8d16fb4d973c0";

		private Boolean firstTimeCalibrationDone = false;
		private Boolean shareToFacebook = false;
		private SharedPreferences pref = null;
		private Date lastRegisterDevice;
		private Date lastOpenApp;



		private OrientationEventListener orientationListener;
		private Orientation orientation = Orientation.PORTRAIT;
		private SensorManager sensorManager;
		private boolean hasCompass = false;
		private boolean userLogged = false;
		private TabPagerAdapter pagerAdapter;
		private ViewPager viewPager;
		private ActionMode mActionMode;
		private ProgressDialog progress;

		private DataManager dataManager;
		private MeasurementController myVaavudCoreController;
		private LocationUpdateManager locationUpdater;
		private HeadsetIntentReceiver receiver;
		private UploadManager uploadManager;
		private UserManager userManager;
		private User user;
		private Device device;

		@Override
		protected void onActivityResult(int requestCode, int resultCode, Intent data) {
				super.onActivityResult(requestCode, resultCode, data);
				if (Session.getActiveSession() != null) {
						Session.getActiveSession().onActivityResult(this, requestCode, resultCode, data);
				}
		}


		@Override
		protected void onCreate(Bundle savedInstanceState) {


				super.onCreate(savedInstanceState);

				Crittercism.initialize(getApplicationContext(), "520b8fa5558d6a2757000003");
//		getWindow().requestFeature(Window.FEATURE_ACTION_BAR);
				setContentView(R.layout.activity_main);
				VaavudDatabase.getInstance(getApplicationContext()).setPropertyAsBoolean(KEY_IS_FIRST_FLOW,false);
				sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
				//			Log.d("MainActivity","Has Compass");
//			Log.d("MainActivity","No Compass");
				hasCompass = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD) != null;
				IntentFilter receiverFilter = new IntentFilter(Intent.ACTION_HEADSET_PLUG);
				receiver = new HeadsetIntentReceiver(this);
				registerReceiver(receiver, receiverFilter);

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


				user = User.getInstance(getApplicationContext());
				device = Device.getInstance(getApplicationContext());
				dataManager = new DataManager();
				uploadManager = UploadManager.getInstance(getApplicationContext());
				uploadManager.setDataManager(dataManager);
				locationUpdater = LocationUpdateManager.getInstance(this);

				userManager = UserManager.getInstance(getApplicationContext());

				if (user.isUserLogged()) {
						userLogged = true;
						ArrayList<String> permissions = new ArrayList<String>();
						permissions.add("email");
						if (Session.getActiveSession() == null && user.getFacebookAccessToken() != null) { //Facebook token renewal
								AccessToken token = AccessToken.createFromExistingAccessToken(user.getFacebookAccessToken(), user.getFacebookAccessTokenExp(), user.getFacebookAccessTokenExpCheck(), AccessTokenSource.FACEBOOK_APPLICATION_SERVICE, permissions);
								Session.openActiveSessionWithAccessToken(getApplicationContext(), token, userManager.getUserCallback());
						}
						//MixPanel
						MixpanelUtil.registerUserAsMixpanelProfile(getApplicationContext(), user);
						MixpanelUtil.updateMeasurementProperties(getApplicationContext());
				}



				if (myVaavudCoreController == null) {
						myVaavudCoreController = new VaavudCoreController(getApplicationContext(), dataManager, uploadManager, locationUpdater);
				}
				uploadManager.setMeasurementController(myVaavudCoreController);
				if (myVaavudCoreController instanceof VaavudCoreController) {
						uploadManager.setFFTManager(((VaavudCoreController) myVaavudCoreController).getFFTManager());
				}

				pagerAdapter = new TabPagerAdapter(getSupportFragmentManager());

				viewPager = (ViewPager) findViewById(R.id.pager);
				viewPager.setAdapter(pagerAdapter);
				viewPager.setOnPageChangeListener(pagerAdapter);

				final ActionBar actionBar = getSupportActionBar();
				actionBar.setTitle("Vaavud");
				// Specify that tabs should be displayed in the action bar.
				actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
				setActionMode(mActionMode);
				// Create a tab listener that is called when the user changes tabs.
				ActionBar.TabListener tabListener = new ActionBar.TabListener() {
						public void onTabSelected(ActionBar.Tab tab, FragmentTransaction ft) {
								User user = User.getInstance(getApplicationContext());
								if (tab.getPosition() == 2 && !user.isUserLogged()) {
										Intent login = new Intent(getBaseContext(), LoginActivity.class);
										login.putExtra("position", 0);
										startActivity(login);
								} else {
										viewPager.setCurrentItem(tab.getPosition(), true);
								}
						}

						public void onTabUnselected(ActionBar.Tab tab, FragmentTransaction ft) {

						}

						public void onTabReselected(ActionBar.Tab tab, FragmentTransaction ft) {
//            	updateView = true;
						}
				};

				actionBar.addTab(actionBar.newTab().setText(getResources().getString(R.string.tab_measure)).setTabListener(tabListener), MEASURE_TAB, true);
				actionBar.addTab(actionBar.newTab().setText(getResources().getString(R.string.tab_map)).setTabListener(tabListener), MAP_TAB, false);
				actionBar.addTab(actionBar.newTab().setText(getResources().getString(R.string.tab_history)).setTabListener(tabListener), HISTORY_TAB, false);

				if (savedInstanceState != null) {
						actionBar.setSelectedNavigationItem(savedInstanceState.getInt("tab", 0));
				}

		}

		@Override
		protected void onSaveInstanceState(Bundle outState) {
				super.onSaveInstanceState(outState);
				outState.putInt("tab", getSupportActionBar().getSelectedNavigationIndex());
		}

		private boolean isPortrait(int orientation) {
				return (orientation >= (360 - ROTATION_THRESHOLD) && orientation <= 360) || (orientation >= 0 && orientation <= ROTATION_THRESHOLD);
		}

		private boolean isReversePortrait(int orientation) {
				return orientation >= (180 - ROTATION_THRESHOLD) && orientation <= (180 + ROTATION_THRESHOLD);
		}

		@Override
		public boolean onCreateOptionsMenu(Menu menu) {
				MenuInflater inflater = getMenuInflater();
				inflater.inflate(R.menu.options, menu);
				return true;
		}

		@Override
		public boolean onPrepareOptionsMenu(Menu menu) {
				if (User.getInstance(getApplicationContext()).isUserLogged()) {
						menu.getItem(0).setTitle(R.string.option_logout);
				} else {
						menu.getItem(0).setTitle(R.string.option_login);
				}
				return true;
		}

		@Override
		public boolean onOptionsItemSelected(MenuItem item) {
				// Handle item selection
//		JSONObject props = new JSONObject();
				User user = User.getInstance(getApplicationContext());
				switch (item.getItemId()) {
						case R.id.option_login:
								if (!user.isUserLogged()) {
										Intent login = new Intent(this, LoginActivity.class);
										login.putExtra("position", 0);
										startActivity(login);
								} else {
										//LOGOUT
//	        		Log.d(TAG,"Do Logout");
										if (Session.getActiveSession() != null) {
												Session.getActiveSession().closeAndClearTokenInformation();
												Session.setActiveSession(null);
										}
										user.eraseDataBase(getApplicationContext());
										device.renewUUID(getApplicationContext());
										VaavudDatabase.getInstance(getApplicationContext()).deleteTable("MeasurementSession");

										// create single request queue
										uploadManager.registerDevice(false);
										shareToFacebook = false;
										SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
										SharedPreferences.Editor editor = pref.edit();
										editor.clear();
										editor.commit();

										//MixPanel
										if (device.isMixpanelEnabled()) {
												MixpanelAPI.getInstance(getApplicationContext(), MIXPANEL_TOKEN).clearSuperProperties();
										}
										String tmpUUID = UUID.randomUUID().toString();
										if (device.isMixpanelEnabled()) {
												MixpanelAPI.getInstance(getApplicationContext(), MIXPANEL_TOKEN).identify(tmpUUID);
										}
										MixpanelAPI.getInstance(getApplicationContext(), MIXPANEL_TOKEN).getPeople().identify(tmpUUID);
										userLogged = false;
								}
								getSupportActionBar().setSelectedNavigationItem(MEASURE_TAB);
								return true;
						case R.id.option_settings:
								Intent settings = new Intent(this, SettingsActivity.class);
								startActivity(settings);
								return true;
						default:
								return super.onOptionsItemSelected(item);
				}
		}

		@Override
		public void onBackPressed() {
				Fragment fragment = pagerAdapter.getRegisteredFragment(viewPager.getCurrentItem());
				if (fragment instanceof BackPressedListener) {
						if (((BackPressedListener) fragment).onBackPressed()) {
								return;
						}
				}
				super.onBackPressed();
		}

		@Override
		protected void onStart() {
				super.onStart();
		}

		@Override
		protected void onRestart() {
				super.onRestart();
		}

		@Override
		protected void onResume() {
//		Log.i(TAG, "onResume");
				super.onResume();
				user = User.getInstance(getApplicationContext());
				device = Device.getInstance(getApplicationContext());

				if (getSupportActionBar() != null && getSupportActionBar().getSelectedNavigationIndex()==2 && !user.isUserLogged()) {
						getSupportActionBar().setSelectedNavigationItem(MEASURE_TAB);
				}

				pref = getSharedPreferences("Vaavud", Context.MODE_PRIVATE);
				device.setWindSpeedUnit(getApplicationContext(),SpeedUnit.valueOf(pref.getString("heading_unit", "MS")));

				orientationListener.enable();
				uploadManager.start();
				locationUpdater.start();

				// start measuring if we were measuring before being paused
				if (myVaavudCoreController != null) {
						myVaavudCoreController.resumeMeasuring();
				}

				firstTimeCalibrationDone = VaavudDatabase.getInstance(getApplicationContext()).getPropertyAsBoolean(KEY_FIRST_TIME_SLEIPNIR);
				if (lastRegisterDevice == null || (System.currentTimeMillis() - lastRegisterDevice.getTime()) > GRACE_TIME_BETWEEN_REGISTER_DEVICE_MS) {

						lastRegisterDevice = new Date();

						if (device.getMagneticFieldSensor() == null && myVaavudCoreController instanceof VaavudCoreController) {
								device.setMagneticFieldSensor(getApplicationContext(), ((VaavudCoreController) myVaavudCoreController).getMagneticFieldSensorName());
						}
						device.setTimezoneOffset((long) TimeZone.getDefault().getOffset(new Date().getTime()));

						uploadManager.registerDevice(true);
						if (device.isMixpanelEnabled()) {
								JSONObject props = new JSONObject();
								try {
										props.put("Speed Unit", device.getWindSpeedUnit().getDisplayName(this));
										props.put("Language", Locale.getDefault().getDisplayLanguage());
										props.put("Enable Share Dialog", shareToFacebook);
										if (user.getUserId() != null && user.getUserId() != 0) props.put("User", true);
										if (user.getFacebookAccessToken() != null && user.getFacebookAccessToken().length() > 0)
												props.put("Facebook", true);
								} catch (JSONException e) {
										e.printStackTrace();
								}
								MixpanelAPI.getInstance(getApplicationContext(), MIXPANEL_TOKEN).registerSuperProperties(props);
						}
				}
				if (lastOpenApp == null || (System.currentTimeMillis() - lastOpenApp.getTime()) > GRACE_TIME_BETWEEN_RESUME_APP_MS) {
						lastOpenApp = new Date();
						if (device.isMixpanelEnabled()) {
								MixpanelAPI.getInstance(getApplicationContext(), MIXPANEL_TOKEN).track("Open App", null);
						}
				}
				shareToFacebook = pref.getBoolean("FacebookSharing", false);

		}

		@Override
		protected void onPause() {
				super.onPause();
//	    Log.i(TAG, "onPause");
				orientationListener.disable();
				myVaavudCoreController.pauseMeasuring();
				locationUpdater.stop();
				uploadManager.stop();

				if (device.isMixpanelEnabled()) {
						MixpanelAPI.getInstance(getApplicationContext(), MIXPANEL_TOKEN).flush();
				}
		}

		@Override
		protected void onStop() {
				Log.i(TAG, "onStop");
				VaavudDatabase.getInstance(getApplicationContext()).setPropertyAsBoolean(KEY_FIRST_TIME_SLEIPNIR, firstTimeCalibrationDone);
				if (user.isUserLogged()) user.setDataBase(getApplicationContext());
				super.onStop();

		}

		@Override
		protected void onDestroy() {
//		Log.i(TAG, "onDestroy");
				unregisterReceiver(receiver);
				if (progress != null) {
//			Log.d(TAG,"Cancel progress: OnStop");
						progress.cancel();
						progress = null;
				}
				//MixPanel
				if (device.isMixpanelEnabled()) {
						MixpanelAPI.getInstance(getApplicationContext(), MIXPANEL_TOKEN).track("Close App", null);
						MixpanelAPI.getInstance(getApplicationContext(), MIXPANEL_TOKEN).flush();
				}
				super.onDestroy();
				orientationListener = null;
				dataManager = null;
				locationUpdater = null;
				uploadManager = null;
				pagerAdapter = null;
				viewPager = null;
				lastRegisterDevice = null;
		}

		public HeadsetIntentReceiver getHeadsetIntentReceiver() {
				return receiver;
		}

		public ViewPager getViewPager(){
				return viewPager;
		}

		public LocationUpdateManager getLocationUpdateManager() {
				return locationUpdater;
		}

		public MeasurementController getMeasurementController() {
				return myVaavudCoreController;
		}

		public boolean isCurrentTab(Fragment fragment) {
				boolean isCurrent = false;
				if (fragment == null || pagerAdapter == null || viewPager == null) {
						return false;
				}
				Fragment currentFragment = pagerAdapter.getRegisteredFragment(viewPager.getCurrentItem());
				isCurrent = (currentFragment == fragment);
				return isCurrent;
		}

		@Override
		public void onSelected() {
//		Log.d(TAG,"On Selected");
		}


		public void setActionMode(ActionMode mode) {
				mActionMode = mode;
		}


		private enum Orientation {
				PORTRAIT,
				REVERSE_PORTRAIT
		}

		public boolean isFacebookSharingEnabled() {
				return shareToFacebook;
		}

		public void setFacebookSharing(Boolean shareToFacebook) {
				this.shareToFacebook = shareToFacebook;
		}

		public boolean hasCompass() {
				return hasCompass;
		}

		private class TabPagerAdapter extends FragmentStatePagerAdapter implements ViewPager.OnPageChangeListener {

				private SparseArray<Fragment> registeredFragments = new SparseArray<Fragment>();

				public TabPagerAdapter(FragmentManager fm) {
						super(fm);
				}

				@Override
				public int getItemPosition(Object object) {
						return POSITION_NONE;
				}

				@Override
				public Fragment getItem(int item) {
						switch (item) {
								case MEASURE_TAB:
										return Fragment.instantiate(MainActivity.this, MeasureFragment.class.getName());
								case MAP_TAB:
										return Fragment.instantiate(MainActivity.this, MeasurementMapFragment.class.getName());
								case HISTORY_TAB:
										return Fragment.instantiate(MainActivity.this, HistoryFragment.class.getName());
								default:
										throw new IllegalArgumentException("Fragment instantiation not defined for tab position " + item);
						}
				}

				@Override
				public Object instantiateItem(ViewGroup container, int position) {
						Fragment fragment = (Fragment) super.instantiateItem(container, position);
						registeredFragments.put(position, fragment);
						return fragment;
				}

				@Override
				public void destroyItem(ViewGroup container, int position, Object object) {
						if (position != MAP_TAB) {
								registeredFragments.remove(position);
								super.destroyItem(container, position, object);
						}
				}

				public Fragment getRegisteredFragment(int position) {
						return registeredFragments.get(position);
				}

				@Override
				public int getCount() {
						return 3;
				}

				@Override
				public void onPageScrollStateChanged(int arg0) {
				}

				@Override
				public void onPageScrolled(int arg0, float arg1, int arg2) {
				}

				@Override
				public void onPageSelected(int position) {
						Fragment fragment = getRegisteredFragment(position);

						getSupportActionBar().setSelectedNavigationItem(position);
						final String screenName;
						switch (position) {
								case MEASURE_TAB:
										if (mActionMode != null) {
												mActionMode.finish();
										}
										screenName = "Measure Tab";
										break;
								case MAP_TAB:
										if (mActionMode != null) {
												mActionMode.finish();
										}
										screenName = "Map Tab";
										break;
								case HISTORY_TAB:
										screenName = "History Tab";
										break;
								default:
										throw new IllegalArgumentException("Screen name not defined for tab position " + position);
						}
						//MixPanel
						if (MainActivity.this != null && device.isMixpanelEnabled()) {
								MixpanelAPI.getInstance(getApplicationContext(), MIXPANEL_TOKEN).track(screenName, null);
						}
				}
		}

		@Override
		public void isSleipnirPlugged(boolean plugged) {
				if (myVaavudCoreController.isMeasuring()) {
						myVaavudCoreController.stopSession();
				}
				if (plugged) {
//			Log.d("MainActivity","Sleipnir Plugged");
						myVaavudCoreController = null;
								myVaavudCoreController = new SleipnirCoreController(this, dataManager, uploadManager, locationUpdater, false);
						uploadManager.setFFTManager(null);
						if (firstTimeCalibrationDone == null || !firstTimeCalibrationDone) {
								firstTimeCalibrationDone = true;
								Intent calibration = new Intent(this, CalibrationActivity.class);
								calibration.putExtra("firstTime", true);
								startActivity(calibration);
						}
				} else {
//			Log.d("MainActivity","Sleipnir Unplugged");
						if (myVaavudCoreController instanceof SleipnirCoreController) {
								myVaavudCoreController.stopController();
						}
						myVaavudCoreController = null;
						myVaavudCoreController = new VaavudCoreController(getApplicationContext(), dataManager, uploadManager, locationUpdater);
						uploadManager.setFFTManager(((VaavudCoreController) myVaavudCoreController).getFFTManager());
				}
		}
}
