package com.vaavud.android.model.entity;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;

import com.vaavud.android.R;
import com.vaavud.android.model.VaavudDatabase;
import com.vaavud.util.AlgorithmConstantsUtil;
import com.vaavud.util.UUIDUtil;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Locale;

public class Device implements Serializable {
	
	private static final String TAG = "Device";
	private static final String KEY_DEVICE_UUID = "deviceUuid";
	private static final String KEY_AUTH_TOKEN = "authToken";
	private static final String KEY_APP = "app";
	private static final String KEY_APP_VERSION = "appVersion";
	private static final String KEY_APP_VERSION_CODE = "appVersionCode";
	private static final String KEY_OS = "os";
	private static final String KEY_OS_VERSION = "osVersion";
	private static final String KEY_OS_API_VERSION = "osApiLevel";
	private static final String KEY_VENDOR = "vendor";
	private static final String KEY_MODEL = "model";
	private static final String KEY_COUNTRY = "country";
	private static final String KEY_LANGUAGE = "language";
	private static final String KEY_WIND_SPEED_UNIT = "windSpeedUnit";
	private static final String KEY_WIND_DIRECTION_UNIT = "windDirectionUnit";
	private static final String KEY_MAGNETIC_FIELD_SENSOR = "magneticFieldSensor";
	private static final String KEY_UPLOAD_MAGNETIC_DATA = "uploadMagneticData";
	private static final String KEY_FREQUENCY_START = "frequencyStart";
	private static final String KEY_FREQUENCY_FACTOR = "frequencyFactor";
	private static final String KEY_HOUR_OPTIONS = "hourOptions";
	private static final String KEY_CREATION_TIME = "creationTime";
	private static final String KEY_MAX_MAP_MARKERS = "maxMapMarkers";
	private static final String KEY_ENABLE_MIXPANEL = "enableMixpanel";
	private static final String KEY_ENABLE_MIXPANEL_PEOPLE = "enableMixpanelPeople";

	private static Device instance;
	
	public static synchronized Device getInstance(Context context) {
		if (instance == null) {
			instance = new Device(context);
		}
		return instance;
	}

	private String uuid;
	private String authToken;
	private String vendor;
	private String model;
	private String os;
	private String osVersion;
	private int osApiLevel;
	private String app;
	private String appVersion;
	private int appVersionCode;
	private String country;
	private String language;
	private Long timezoneOffset;
	private SpeedUnit windSpeedUnit;
	private String magneticFieldSensor;
	private boolean uploadMagneticData;
	private double frequencyStart;
	private double frequencyFactor;
	private float[] hourOptions;
	private int maxMapMarkers;
//	private Date creationTime;
	private boolean mixpanelEnabled = true;
	private boolean mixpanelPeopleEnabled = true;
	private DirectionUnit windDirectionUnit;

	private Device(Context context) {
		VaavudDatabase db = VaavudDatabase.getInstance(context);
		
		if (db.getProperty(KEY_DEVICE_UUID) == null) {
			uuid = UUIDUtil.generateUUID();
			db.setProperty(KEY_DEVICE_UUID, uuid);
		}
		else {
			uuid = db.getProperty(KEY_DEVICE_UUID);
//			Log.d(TAG,"Device Constructor: "+uuid);
		}
		
		authToken = db.getProperty(KEY_AUTH_TOKEN);
		
//		creationTime = db.getProperty(KEY_CREATION_TIME)!=null?new Date(Long.valueOf(db.getProperty(KEY_CREATION_TIME))):new Date();
//		Log.d("Device","Creation Time: "+creationTime);
		
		if (db.getPropertyAsBoolean(KEY_ENABLE_MIXPANEL) == null) {
			mixpanelEnabled = true;
			db.setPropertyAsBoolean(KEY_ENABLE_MIXPANEL, mixpanelEnabled);
		}
		else {
			mixpanelEnabled = db.getPropertyAsBoolean(KEY_ENABLE_MIXPANEL);
		}
		
		if (db.getPropertyAsBoolean(KEY_ENABLE_MIXPANEL_PEOPLE) == null) {
			mixpanelPeopleEnabled = true;
			db.setPropertyAsBoolean(KEY_ENABLE_MIXPANEL_PEOPLE, mixpanelPeopleEnabled);
		}
		else {
			mixpanelPeopleEnabled = db.getPropertyAsBoolean(KEY_ENABLE_MIXPANEL_PEOPLE);
		}
		
		PackageInfo packageInfo = null;
		try {
			packageInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
		}
		catch (NameNotFoundException e) {
			// shouldn't happen
			//Log.e("Device", "Error getting PackageInfo", e);
		}
		
		app = context.getResources().getString(R.string.app_name);
	    appVersion = (packageInfo == null) ? "Unknown" : packageInfo.versionName;
	    appVersionCode = (packageInfo == null) ? -1 : packageInfo.versionCode;
	    os = "Android";
	    osVersion = android.os.Build.VERSION.RELEASE;
	    osApiLevel = android.os.Build.VERSION.SDK_INT;
	    vendor = android.os.Build.MANUFACTURER;
	    model = android.os.Build.MODEL;
	    country = Locale.getDefault().getCountry();
		language = Locale.getDefault().getLanguage();
		
		if (!app.equals(db.getProperty(KEY_APP))) {
			db.setProperty(KEY_APP, app);
		}
		if (!appVersion.equals(db.getProperty(KEY_APP_VERSION))) {
			db.setProperty(KEY_APP_VERSION, appVersion);
		}
		Integer dbAppVersionCode = db.getPropertyAsInteger(KEY_APP_VERSION_CODE);
		if (dbAppVersionCode == null || dbAppVersionCode != appVersionCode) {
			db.setPropertyAsInteger(KEY_APP_VERSION_CODE, appVersionCode);
		}
		if (!os.equals(db.getProperty(KEY_OS))) {
			db.setProperty(KEY_OS, os);
		}
		if (!osVersion.equals(db.getProperty(KEY_OS_VERSION))) {
			db.setProperty(KEY_OS_VERSION, osVersion);
		}
		Integer dbOsApiVersion = db.getPropertyAsInteger(KEY_OS_API_VERSION);
		if (dbOsApiVersion == null || dbOsApiVersion != osApiLevel) {
			db.setPropertyAsInteger(KEY_OS_API_VERSION, osApiLevel);
		}
		if (!vendor.equals(db.getProperty(KEY_VENDOR))) {
			db.setProperty(KEY_VENDOR, vendor);
		}
		if (!model.equals(db.getProperty(KEY_MODEL))) {
			db.setProperty(KEY_MODEL, model);
		}
		if (!country.equals(db.getProperty(KEY_COUNTRY))) {
			db.setProperty(KEY_COUNTRY, country);
		}
		if (!language.equals(db.getProperty(KEY_LANGUAGE))) {
			db.setProperty(KEY_LANGUAGE, language);
		}
		
		if (db.getProperty(KEY_WIND_SPEED_UNIT) == null) {
			// first run, so set default wind speed unit from locale
			windSpeedUnit = SpeedUnit.defaultUnitForCountry(country);
			db.setPropertyAsEnum(KEY_WIND_SPEED_UNIT, windSpeedUnit);
		}		
		else {
			windSpeedUnit = db.getPropertyAsEnum(KEY_WIND_SPEED_UNIT, SpeedUnit.class);
		}
		
		if (db.getProperty(KEY_WIND_DIRECTION_UNIT) == null) {
			windDirectionUnit = DirectionUnit.CARDINAL;
			db.setPropertyAsEnum(KEY_WIND_DIRECTION_UNIT, windDirectionUnit);
		}		
		else {
			windDirectionUnit = db.getPropertyAsEnum(KEY_WIND_DIRECTION_UNIT, DirectionUnit.class);
		}

		
		if (db.getPropertyAsBoolean(KEY_UPLOAD_MAGNETIC_DATA) == null) {
			uploadMagneticData = false;
			db.setPropertyAsBoolean(KEY_UPLOAD_MAGNETIC_DATA, uploadMagneticData);
		}
		else {
			uploadMagneticData = db.getPropertyAsBoolean(KEY_UPLOAD_MAGNETIC_DATA);
		}
		
		if (db.getProperty(KEY_FREQUENCY_START) == null) {
			frequencyStart = AlgorithmConstantsUtil.getFrequencyStart(model);
			db.setPropertyAsDouble(KEY_FREQUENCY_START, frequencyStart);
		}
		else {
			frequencyStart = db.getPropertyAsDouble(KEY_FREQUENCY_START);
		}
		
		if (db.getProperty(KEY_FREQUENCY_FACTOR) == null) {
			frequencyFactor = AlgorithmConstantsUtil.getFrequencyFactor(model);
			db.setPropertyAsDouble(KEY_FREQUENCY_FACTOR, frequencyFactor);
		}
		else {
			frequencyFactor = db.getPropertyAsDouble(KEY_FREQUENCY_FACTOR);
		}
		
		hourOptions = db.getPropertyAsFloatArray(KEY_HOUR_OPTIONS);
		if (hourOptions == null) {
			hourOptions = new float[] {3F, 6F, 12F, 24F};
			db.setPropertyAsFloatArray(KEY_HOUR_OPTIONS, hourOptions);
		}
		
		if (db.getProperty(KEY_MAX_MAP_MARKERS) == null) {
			maxMapMarkers = 10000;
			db.setPropertyAsInteger(KEY_MAX_MAP_MARKERS, maxMapMarkers);
		}
		else {
			maxMapMarkers = db.getPropertyAsInteger(KEY_MAX_MAP_MARKERS);
		}
	}
	
	public void renewUUID(Context context){
		VaavudDatabase db = VaavudDatabase.getInstance(context);
		uuid = UUIDUtil.generateUUID();
//		Log.d(TAG,"Renew UUID: "+uuid);
		db.setProperty(KEY_DEVICE_UUID, uuid);
	}

	public String getUuid() {
		return uuid;
	}

	public String getAuthToken() {
		return authToken;
	}
	
	public void setAuthToken(Context context, String authToken) {
		this.authToken = authToken;
		VaavudDatabase.getInstance(context).setProperty(KEY_AUTH_TOKEN, authToken);
	}

	public String getVendor() {
		return vendor;
	}

	public String getModel() {
		return model;
	}

	public String getOs() {
		return os;
	}

	public String getOsVersion() {
		return osVersion;
	}

	public int getOsApiLevel() {
		return osApiLevel;
	}

	public String getApp() {
		return app;
	}

	public String getAppVersion() {
		return appVersion;
	}

	public int getAppVersionCode() {
		return appVersionCode;
	}

	public String getCountry() {
		return country;
	}

	public String getLanguage() {
		return language;
	}

	public Long getTimezoneOffset() {
		return timezoneOffset;
	}
	
	public void setTimezoneOffset(Long timezoneOffset) {
		this.timezoneOffset = timezoneOffset;
	}

	public SpeedUnit getWindSpeedUnit() {
		return windSpeedUnit;
	}

	public void setWindSpeedUnit(Context context, SpeedUnit speedUnit) {
		this.windSpeedUnit = speedUnit;
		VaavudDatabase.getInstance(context).setPropertyAsEnum(KEY_WIND_SPEED_UNIT, speedUnit);
	}
	
	public String getMagneticFieldSensor() {
		return magneticFieldSensor;
	}
	
	public void setMagneticFieldSensor(Context context, String magneticFieldSensor) {
		this.magneticFieldSensor = magneticFieldSensor;
		VaavudDatabase.getInstance(context).setProperty(KEY_MAGNETIC_FIELD_SENSOR, magneticFieldSensor);
	}
	
	public boolean getUploadMagneticData() {
		return uploadMagneticData;
	}
	
	public void setUploadMagneticData(Context context, boolean uploadMagneticData) {
		this.uploadMagneticData = uploadMagneticData;
		VaavudDatabase.getInstance(context).setPropertyAsBoolean(KEY_UPLOAD_MAGNETIC_DATA, uploadMagneticData);
	}
	
	public double getFrequencyStart() {
		return frequencyStart;
	}
	
	public void setFrequencyStart(Context context, double frequencyStart) {
		this.frequencyStart = frequencyStart;
		VaavudDatabase.getInstance(context).setPropertyAsDouble(KEY_FREQUENCY_START, frequencyStart);
	}

	public double getFrequencyFactor() {
		return frequencyFactor;
	}
	
	public void setFrequencyFactor(Context context, double frequencyFactor) {
		this.frequencyFactor = frequencyFactor;
		VaavudDatabase.getInstance(context).setPropertyAsDouble(KEY_FREQUENCY_FACTOR, frequencyFactor);
	}

	public float[] getHourOptions() {
		return hourOptions;
	}
	
	public void setHourOptions(Context context, float[] hourOptions) {
		Arrays.sort(hourOptions);
		this.hourOptions = hourOptions;
		VaavudDatabase.getInstance(context).setPropertyAsFloatArray(KEY_HOUR_OPTIONS, hourOptions);
	}

	public int getMaxMapMarkers() {
		return maxMapMarkers;
	}
	
	public void setMaxMapMarkers(Context context, int maxMapMarkers) {
		this.maxMapMarkers = maxMapMarkers;
		VaavudDatabase.getInstance(context).setPropertyAsInteger(KEY_MAX_MAP_MARKERS, maxMapMarkers);
	}

	private Device() {
	}
	
	public boolean isMixpanelEnabled(){
		return mixpanelEnabled;
	}
	
	public void setMixpanelEnabled(Context context,boolean mixpanelEnabled){
		this.mixpanelEnabled=mixpanelEnabled;
		VaavudDatabase.getInstance(context).setPropertyAsBoolean(KEY_ENABLE_MIXPANEL, mixpanelEnabled);
	}
	
	public boolean isMixpanelPeopleEnabled(){
		return mixpanelPeopleEnabled;
	}
	
	public void setMixpanelPeopleEnabled(Context context,boolean mixpanelPeopleEnabled){
		this.mixpanelPeopleEnabled=mixpanelPeopleEnabled;
		VaavudDatabase.getInstance(context).setPropertyAsBoolean(KEY_ENABLE_MIXPANEL_PEOPLE, mixpanelPeopleEnabled);
	}

	public void setWindDirectionUnit(Context context, DirectionUnit windDirectionUnit) {
		this.windDirectionUnit = windDirectionUnit;
		VaavudDatabase.getInstance(context).setPropertyAsEnum(KEY_WIND_DIRECTION_UNIT, windDirectionUnit);
	}

	public DirectionUnit getWindDirectionUnit() {
		return windDirectionUnit;
	}
}
