package com.vaavud.android.ui;

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

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;
import com.crittercism.app.Crittercism;
import com.facebook.AccessToken;
import com.facebook.AccessTokenSource;
import com.facebook.Session;
import com.mixpanel.android.mpmetrics.MixpanelAPI;
import com.vaavud.android.R;
import com.vaavud.android.VaavudApplication;
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
import com.vaavud.android.ui.about.AboutFragment;
import com.vaavud.android.ui.calibration.CalibrationActivity;
import com.vaavud.android.ui.history.HistoryFragment;
import com.vaavud.android.ui.login.LoginFragment;
import com.vaavud.android.ui.login.SelectorFragment;
import com.vaavud.android.ui.login.SignUpFragment;
import com.vaavud.android.ui.map.MeasurementMapFragment;
import com.vaavud.android.ui.measure.MeasureFragment;
import com.vaavud.android.ui.settings.SettingsActivity;
import com.vaavud.android.ui.settings.SettingsFragment;
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

public class MainActivity extends ActionBarActivity implements SelectedListener, SelectorListener, PlugListener {

		private static final int ROTATION_THRESHOLD = 45;
		private static final long GRACE_TIME_BETWEEN_REGISTER_DEVICE_MS = 3600L * 1000L; // 1 hour
		private static final long GRACE_TIME_BETWEEN_RESUME_APP_MS = 1800L * 1000L; // 1 hour
		private static final String KEY_FIRST_TIME_SLEIPNIR = "firstTimeSleipnir";

		private static final int MEASURE_TAB = 0;
		private static final int MAP_TAB = 1;
		private static final int HISTORY_TAB = 2;

		//MixPanel
		private static final String MIXPANEL_TOKEN = "757f6311d315f94cdfc8d16fb4d973c0";

		/**
		 * User State Machine
		 * userStatus = {false,false,false} -> User not logged in -> Selector Fragment
		 * userStatus = {false,false,true} -> User logged in -> History Fragment
		 * userStatus = {true,false,false} -> User choose sign Up -> SignUp Fragment
		 * userStatus = {false,true,false} -> User choose log in -> LogIn Fragment
		 */
		private Boolean[] userStatus = {false, false, false};
		private Boolean settings = false;
		private Boolean about = false;
		private Boolean updateView = false;
		private boolean login = false;
		private boolean signUp = false;
		private Boolean firstTimeCalibrationDone = false;
		private SharedPreferences pref = null;

		private static final String TAG = "MAIN_ACTIVITY";

		private OrientationEventListener orientationListener;
		private Orientation orientation = Orientation.PORTRAIT;
		private SensorManager sensorManager;
		private boolean hasCompass = false;
		private TabPagerAdapter pagerAdapter;
		private ViewPager viewPager;
		private ActionMode mActionMode;
		private ProgressDialog progress;

		private DataManager dataManager;
		private MeasurementController myVaavudCoreController;
		private LocationUpdateManager locationUpdater;
		private HeadsetIntentReceiver receiver;

		/**
		 * Single request queue to use for all HTTP requests.
		 */
		private RequestQueue requestQueue;
		private RequestQueue userQueue;

		private UploadManager uploadManager;

		private Date lastRegisterDevice;
		private Date lastOpenApp;

		private UserManager userManager;

		private User user;

		private Boolean shareToFacebook = false;


		@Override
		protected void onActivityResult(int requestCode, int resultCode, Intent data) {
//		Log.d(TAG,"OnActivityResult:"+requestCode+" "+resultCode);
				super.onActivityResult(requestCode, resultCode, data);
				if (Session.getActiveSession() != null) {
//			Log.d(TAG, "Session is not null");
						Session.getActiveSession().onActivityResult(this, requestCode, resultCode, data);
						((VaavudApplication) getApplication()).hasWindMeter();
				}
		}


		@Override
		protected void onCreate(Bundle savedInstanceState) {


				super.onCreate(savedInstanceState);

				Crittercism.initialize(getApplicationContext(), "520b8fa5558d6a2757000003");
//		getWindow().requestFeature(Window.FEATURE_ACTION_BAR);
				setContentView(R.layout.activity_main);

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

				// create measurements request queue
				if (requestQueue != null) {
						requestQueue.stop();
				}
				requestQueue = Volley.newRequestQueue(this);
				// create user request queue
				if (userQueue != null) {
						userQueue.stop();
				}
				userQueue = Volley.newRequestQueue(this);

				dataManager = new DataManager();
				locationUpdater = new LocationUpdateManager(this);
				uploadManager = new UploadManager(this, requestQueue);

				userManager = UserManager.getInstance(this, userQueue, user);


				user = User.getInstance(this);
				if (((VaavudApplication) getApplication()).isUserLogged()) {
						userStatus[0] = false;
						userStatus[1] = false;
						userStatus[2] = true;

						ArrayList<String> permissions = new ArrayList<String>();
						permissions.add("email");
//			Log.d(TAG,"User Creation Date: "+user.getCreationTime());
						if (Session.getActiveSession() == null && user.getFacebookAccessToken() != null) { //Facebook token renewal
								AccessToken token = AccessToken.createFromExistingAccessToken(user.getFacebookAccessToken(), user.getFacebookAccessTokenExp(), user.getFacebookAccessTokenExpCheck(), AccessTokenSource.FACEBOOK_APPLICATION_SERVICE, permissions);
								Session.openActiveSessionWithAccessToken(this, token, userManager.getUserCallback());
						}
						//MixPanel
						MixpanelUtil.registerUserAsMixpanelProfile(this, user);
						MixpanelUtil.updateMeasurementProperties(this);
				}

				pref = getSharedPreferences("Vaavud",Context.MODE_PRIVATE);


				Device.getInstance(this).setWindSpeedUnit(this, SpeedUnit.valueOf(pref.getString("heading_unit", "MS")));

				uploadManager.setDataManager(dataManager);

				if (myVaavudCoreController == null) {
						myVaavudCoreController = new VaavudCoreController(this, dataManager, uploadManager, locationUpdater);
				}
				uploadManager.setMeasurementController(myVaavudCoreController);
				if (myVaavudCoreController instanceof VaavudCoreController) {
						uploadManager.setFFTManager(((VaavudCoreController) myVaavudCoreController).getFFTManager());
				}

				pagerAdapter = new TabPagerAdapter(getSupportFragmentManager());

				viewPager = (ViewPager) findViewById(R.id.pager);
				viewPager.setAdapter(pagerAdapter);
				viewPager.setOnPageChangeListener(pagerAdapter);

				ActionBar actionBar = getSupportActionBar();
				actionBar.setTitle("Vaavud");
				// Specify that tabs should be displayed in the action bar.
				actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
				setActionMode(mActionMode);
				// Create a tab listener that is called when the user changes tabs.
				ActionBar.TabListener tabListener = new ActionBar.TabListener() {
						public void onTabSelected(ActionBar.Tab tab, FragmentTransaction ft) {
								if (updateView) {
										updateView = false;
										viewPager.getAdapter().notifyDataSetChanged();
								}
								viewPager.setCurrentItem(tab.getPosition(), false);
						}

						public void onTabUnselected(ActionBar.Tab tab, FragmentTransaction ft) {
								if (tab.getPosition() == MEASURE_TAB) {
										settings = false;
										about = false;
								}
								if (tab.getPosition() == MAP_TAB) {
										updateView = true;
								}
						}

						public void onTabReselected(ActionBar.Tab tab, FragmentTransaction ft) {
//            	updateView = true;
						}
				};

				actionBar.addTab(actionBar.newTab().setText(getResources().getString(R.string.tab_measure)).setTabListener(tabListener));
				actionBar.addTab(actionBar.newTab().setText(getResources().getString(R.string.tab_map)).setTabListener(tabListener));
				actionBar.addTab(actionBar.newTab().setText(getResources().getString(R.string.tab_history)).setTabListener(tabListener));

				if (savedInstanceState != null) {
						actionBar.setSelectedNavigationItem(savedInstanceState.getInt("tab", 0));
				}

				Intent i = getIntent();
				if (i != null) {
						login = i.getBooleanExtra("login", false);
						signUp = i.getBooleanExtra("signUp", false);
				}
				if (login) {
						onMenuOptionSelected(1);
				} else if (signUp) {
						onMenuOptionSelected(0);
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
				if ((user.getEmail() != null && user.getEmail().length() > 0) || userStatus[2]) {
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
				switch (item.getItemId()) {
						case R.id.option_login:
								if (!userStatus[2]) {
										//LOGIN/SIGNUP
								} else {
										//LOGOUT
//	        		Log.d(TAG,"Do Logout");
										if (Session.getActiveSession() != null)
												Session.getActiveSession().closeAndClearTokenInformation();
										Session.setActiveSession(null);
										user.eraseDataBase(this);
//		        	Log.d(TAG,"After Erase Database");
										Device.getInstance(this).renewUUID(this);
//		        	Log.d(TAG,"After Renew UUID");
										VaavudDatabase.getInstance(this).deleteTable("MeasurementSession");

										// create single request queue
										uploadManager.registerDevice();
//		        	Log.d(TAG,"After Register Device");
										userStatus[0] = false;
										userStatus[1] = false;
										userStatus[2] = false;
										shareToFacebook = true;
										SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
										SharedPreferences.Editor editor = pref.edit();
										editor.clear();
										editor.commit();
										((VaavudApplication) getApplication()).setIsFirstFlow(true);
										//MixPanel
										if (Device.getInstance(this).isMixpanelEnabled()) {
												MixpanelAPI.getInstance(this, MIXPANEL_TOKEN).clearSuperProperties();
										}
										String tmpUUID = UUID.randomUUID().toString();
										if (Device.getInstance(this).isMixpanelEnabled()) {
												MixpanelAPI.getInstance(this, MIXPANEL_TOKEN).identify(tmpUUID);
										}
										MixpanelAPI.getInstance(this, MIXPANEL_TOKEN).getPeople().identify(tmpUUID);

								}
								onMenuOptionSelected(-1);
								return true;
						case R.id.option_settings:
								Intent settings = new Intent(this, SettingsActivity.class);
								startActivity(settings);
//								onMenuOptionSelected(3);
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
								// listener handled back press, so bail out

								if (about) {
//					Log.d(TAG,"OnBackPressed: About");
										about = false;
										settings = true;
										viewPager.getAdapter().notifyDataSetChanged();
								} else if (settings) {
//					Log.d(TAG,"OnBackPressed: Settings");
										settings = false;
										about = false;
										viewPager.getAdapter().notifyDataSetChanged();
								} else {
//					Log.d(TAG,"OnBackPressed: Else");
										userStatus[0] = false;
										userStatus[1] = false;
										userStatus[2] = false;
										settings = false;
										about = false;
										viewPager.getAdapter().notifyDataSetChanged();
								}

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

				orientationListener.enable();
				uploadManager.start();
				locationUpdater.start();

				// start measuring if we were measuring before being paused
				if (myVaavudCoreController != null) {
						myVaavudCoreController.resumeMeasuring();
				}

				firstTimeCalibrationDone = VaavudDatabase.getInstance(this).getPropertyAsBoolean(KEY_FIRST_TIME_SLEIPNIR);
				Device device = Device.getInstance(this);
				if (lastRegisterDevice == null || (System.currentTimeMillis() - lastRegisterDevice.getTime()) > GRACE_TIME_BETWEEN_REGISTER_DEVICE_MS) {

						lastRegisterDevice = new Date();

						if (device.getMagneticFieldSensor() == null && myVaavudCoreController instanceof VaavudCoreController) {
								device.setMagneticFieldSensor(this, ((VaavudCoreController) myVaavudCoreController).getMagneticFieldSensorName());
						}
						device.setTimezoneOffset((long) TimeZone.getDefault().getOffset(new Date().getTime()));

						uploadManager.registerDevice();
						if (Device.getInstance(this).isMixpanelEnabled()) {
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
								MixpanelAPI.getInstance(this, MIXPANEL_TOKEN).registerSuperProperties(props);
						}
				}
				if (lastOpenApp == null || (System.currentTimeMillis() - lastOpenApp.getTime()) > GRACE_TIME_BETWEEN_RESUME_APP_MS) {
						lastOpenApp = new Date();
						if (Device.getInstance(this).isMixpanelEnabled()) {
								MixpanelAPI.getInstance(this, MIXPANEL_TOKEN).track("Open App", null);
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

				if (Device.getInstance(this).isMixpanelEnabled()) {
						MixpanelAPI.getInstance(this, MIXPANEL_TOKEN).flush();
				}
		}

		@Override
		protected void onStop() {
				Log.i(TAG, "onStop");
				VaavudDatabase.getInstance(this).setPropertyAsBoolean(KEY_FIRST_TIME_SLEIPNIR, firstTimeCalibrationDone);
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
				if (Device.getInstance(this).isMixpanelEnabled()) {
						MixpanelAPI.getInstance(this, MIXPANEL_TOKEN).track("Close App", null);
						MixpanelAPI.getInstance(this, MIXPANEL_TOKEN).flush();
				}
				super.onDestroy();
//    	myVaavudCoreController = null;
				orientationListener = null;
				dataManager = null;
				locationUpdater = null;
				uploadManager = null;
				pagerAdapter = null;
				viewPager = null;
				lastRegisterDevice = null;
				requestQueue.stop();
				requestQueue = null;
		}


		public ProgressDialog getProgressDialog() {
				return progress;
		}

		public UploadManager getUploadManager() {
				return uploadManager;
		}

		public RequestQueue getRequestQueue() {
				return requestQueue;
		}

		public UserManager getUserManager() {
				return userManager;
		}

		public HeadsetIntentReceiver getHeadsetIntentReceiver() {
				return receiver;
		}

		public boolean getLogin() {
				return login;
		}

		public boolean hasCompass() {
				return hasCompass;
//		return false;
		}

		public boolean getSignUp() {
				return signUp;
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

		@Override
		public void onMenuOptionSelected(int position) {
//		Log.d(TAG,"On Menu Option Selected: "+position);
				int actionBarPosition = MEASURE_TAB;
				switch (position) {
						case 0:
								//SignUp
//			Log.d(TAG,"On Menu Option Selected: SignUp");
								userStatus[0] = true;
								userStatus[1] = false;
								userStatus[2] = false;
								actionBarPosition = HISTORY_TAB;
								break;
						case 1:
								//LogIn
//			Log.d(TAG,"On Menu Option Selected: LogIn");
								userStatus[0] = false;
								userStatus[1] = true;
								userStatus[2] = false;
								actionBarPosition = HISTORY_TAB;
								break;
						case 2:
								//History
//			Log.d(TAG,"On Menu Option Selected: History");
								userStatus[0] = false;
								userStatus[1] = false;
								userStatus[2] = true;
								actionBarPosition = HISTORY_TAB;
								break;
						case 3:
								//Settings
//			Log.d(TAG,"On Menu Option Selected: Settings");
								settings = true;
								about = false;
								actionBarPosition = MEASURE_TAB;
								break;
						case 4:
								//About
//			Log.d(TAG,"On Menu Option Selected: About");
								about = true;
								settings = false;
								actionBarPosition = MEASURE_TAB;
								break;
						default:
								//Selector
//			Log.d(TAG,"On Menu Option Selected: Selector");
								userStatus[0] = false;
								userStatus[1] = false;
								userStatus[2] = false;
								actionBarPosition = HISTORY_TAB;
								break;
				}

				if (getSupportActionBar().getSelectedNavigationIndex() == actionBarPosition) {
//			Log.d(TAG,"Same TAB");
						viewPager.getAdapter().notifyDataSetChanged();
				} else {
//			Log.d(TAG,"Different TAB");
						getSupportActionBar().setSelectedNavigationItem(actionBarPosition);
						return;
				}
		}

		public void setActionMode(ActionMode mode) {
				mActionMode = mode;
		}

		private enum Orientation {
				PORTRAIT,
				REVERSE_PORTRAIT
		}

		public void setProgressDialog(ProgressDialog progressDialog) {
				// TODO Auto-generated method stub
				this.progress = progressDialog;
		}

		public void restartUser() {
				userStatus[0] = false;
				userStatus[1] = false;
				userStatus[2] = false;
				if (user != null) {
						user.eraseDataBase(this);
				}
		}

		public boolean isFacebookSharingEnabled() {
				return shareToFacebook;
		}

		public void setFacebookSharing(Boolean shareToFacebook) {
				this.shareToFacebook = shareToFacebook;
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
										if (about) {
												return Fragment.instantiate(MainActivity.this, AboutFragment.class.getName());
										} else if (settings) {
												return Fragment.instantiate(MainActivity.this, SettingsFragment.class.getName());
										} else {
												return Fragment.instantiate(MainActivity.this, MeasureFragment.class.getName());
										}
								case MAP_TAB:
										return Fragment.instantiate(MainActivity.this, MeasurementMapFragment.class.getName());
								case HISTORY_TAB:
										/**
										 * User State Machine
										 * userStatus = {false,false,false} -> User not logged in -> Selector Fragment
										 * userStatus = {false,false,true} -> User logged in -> History Fragment
										 * userStatus = {true,false,false} -> User choose sign Up -> SignUp Fragment
										 * userStatus = {false,true,false} -> User choose log in -> LogIn Fragment
										 */
										if (userStatus[2])
												return Fragment.instantiate(MainActivity.this, HistoryFragment.class.getName());
										else if (!userStatus[1] && userStatus[0])
												return Fragment.instantiate(MainActivity.this, SignUpFragment.class.getName());
										else if (userStatus[1] && !userStatus[0])
												return Fragment.instantiate(MainActivity.this, LoginFragment.class.getName());
										else
												return Fragment.instantiate(MainActivity.this, SelectorFragment.class.getName());
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
						if (fragment instanceof SelectedListener) {
								((SelectedListener) fragment).onSelected();
						}
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
						if (MainActivity.this != null && Device.getInstance(MainActivity.this).isMixpanelEnabled()) {
								MixpanelAPI.getInstance(MainActivity.this, MIXPANEL_TOKEN).track(screenName, null);
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
						myVaavudCoreController = new VaavudCoreController(this, dataManager, uploadManager, locationUpdater);
						uploadManager.setFFTManager(((VaavudCoreController) myVaavudCoreController).getFFTManager());
				}
		}
}
