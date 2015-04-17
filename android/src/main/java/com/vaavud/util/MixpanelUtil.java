package com.vaavud.util;

import android.content.Context;

import com.mixpanel.android.mpmetrics.MixpanelAPI;
import com.vaavud.android.model.VaavudDatabase;
import com.vaavud.android.model.entity.Device;
import com.vaavud.android.model.entity.MeasurementSession;
import com.vaavud.android.model.entity.User;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

public final class MixpanelUtil {
	
	private static final String MIXPANEL_TOKEN = "757f6311d315f94cdfc8d16fb4d973c0";
	private static final String DATEFORMAT_DAY = "yyyy-MM-dd";
	private static final String DATEFORMAT_HOUR = "HH:mm:ss";
	
	private static final Comparator<? super MeasurementSession> startTimeComparatorDescending = new Comparator<MeasurementSession>() {
		@Override
		public int compare(MeasurementSession a, MeasurementSession b) {
			long time1 = (a.getStartTime() == null) ? 0L : a.getStartTime().getTime();
			long time2 = (b.getStartTime() == null) ? 0L : b.getStartTime().getTime();
			return (time1 == time2) ? 0 : ((time2 - time1) < 0 ? -1 : 1);
		}
	};
	
	public static void registerUserAsMixpanelProfile(Context context, User user){
		
		//MixPanel
		if(context!=null && Device.getInstance(context).isMixpanelPeopleEnabled()){
			String id = MixpanelAPI.getInstance(context, MIXPANEL_TOKEN).getDistinctId();
	//		Log.d("MIXPANEL_API","Distinct ID:"+id);
	    	MixpanelAPI.getInstance(context, MIXPANEL_TOKEN).getPeople().identify(id);
			
			JSONObject props = new JSONObject();
			try {
				props.put("Distinct Id", id);
				props.put("$email",user.getEmail());
				props.put("$created", GetUTCdatetimeAsString(user.getCreationTime()));
				props.put("$first_name",user.getFirstName());
				props.put("$last_name",user.getLastName());
			}catch (JSONException e) {
				e.printStackTrace();
			}
			MixpanelAPI.getInstance(context,MIXPANEL_TOKEN).getPeople().set(props);
			
			props = new JSONObject();
			try {
				props.put("User", "true");
				props.put("Creation Time",GetUTCdatetimeAsString(user.getCreationTime()));
			} catch (JSONException e) {
				e.printStackTrace();
			}
			MixpanelAPI.getInstance(context,MIXPANEL_TOKEN).registerSuperProperties(props);
			
			if(user.getFacebookAccessToken()!=null){
				props = new JSONObject();
				try {
					props.put("Facebook", "true");
				} catch (JSONException e) {
					e.printStackTrace();
				}
				MixpanelAPI.getInstance(context,MIXPANEL_TOKEN).registerSuperProperties(props);
			}
			
			props = new JSONObject();
			try {
				if (user.getFacebookAccessToken()!=null)
					props.put("Method", "Facebook");
				else
					props.put("Method", "Password");
			} catch (JSONException e) {
				e.printStackTrace();
			}
			MixpanelAPI.getInstance(context,MIXPANEL_TOKEN).track("Login",props);
		}
	}
	
	public static void registerUserErrorToMixpanel(Context context,User user,int statusOrdinal,String screen){
		//MixPanel
		if(context!=null && Device.getInstance(context).isMixpanelEnabled()){
			JSONObject props = new JSONObject();
			try {
				props.put("Response", statusOrdinal);
				props.put("Screen", screen);
				if (user.getFacebookAccessToken()!=null && user.getFacebookAccessToken().length() > 0)
					props.put("Method", "Facebook");
				else
					props.put("Method", "Password");
			} catch (JSONException e) {
				e.printStackTrace();
			}
			MixpanelAPI.getInstance(context,MIXPANEL_TOKEN).track("Register Error", props);
		}
	}
	
	public static void updateMeasurementProperties(Context context){
		if(context!=null && Device.getInstance(context).isMixpanelPeopleEnabled()){
			int measurementCount = 0;
			int realMeasurementCount = 0;
			Date firstMeasurement = null;
			Date lastMeasurement = null;
			
			List<MeasurementSession> measurementList = getMeasurementsFromDB(context);
			measurementCount = measurementList.size();
			if (measurementCount>0){
	//			Log.d("MixpanelUtil","Measurement Count > 0 "+measurementCount);
				for (int i=0;i<measurementCount;i++){
					if(measurementList.get(i).getWindSpeedAvg()!=null && measurementList.get(i).getWindSpeedAvg()>0){
	//					Log.d("MixpanelUtil","Real Measurement ++");
						realMeasurementCount++;
					}
				}
				firstMeasurement = measurementList.get(measurementCount-1).getEndTime();
				lastMeasurement = measurementList.get(0).getEndTime();
			}
	
			JSONObject props = new JSONObject();
			try {
				props.put("Measurements", measurementCount);
				props.put("Real Measurements", realMeasurementCount);
				props.put("First Measurement", GetUTCdatetimeAsString(firstMeasurement));
				props.put("Last Measurement", GetUTCdatetimeAsString(lastMeasurement));
			} catch (JSONException e) {
				e.printStackTrace();
			}
			MixpanelAPI.getInstance(context,MIXPANEL_TOKEN).getPeople().set(props);
		}
	}
	
	public static void addMapInteractionToProfile(Context context){
		MixpanelAPI.getInstance(context,MIXPANEL_TOKEN).getPeople().increment("Map Interactions",1);
		MixpanelAPI.getInstance(context,MIXPANEL_TOKEN).getPeople().set("Last Map Interaction",GetUTCdatetimeAsString(new Date()));
	}
	
	private static List<MeasurementSession> getMeasurementsFromDB(Context context){
		//Log.i(TAG, "getMeasurementsFromDB");
		List<MeasurementSession> measurementsList = VaavudDatabase.getInstance(context).getMeasurementSessions();
		Collections.sort(measurementsList, startTimeComparatorDescending);
		return measurementsList;
	}
	
	private static String GetUTCdatetimeAsString(Date date)
	{
		if (date==null){
			return null;
		}
	    final SimpleDateFormat sdf_day = new SimpleDateFormat(DATEFORMAT_DAY);
	    sdf_day.setTimeZone(TimeZone.getTimeZone("UTC"));
	    final String utcTime_day = sdf_day.format(date);
	    
	    final SimpleDateFormat sdf_hour = new SimpleDateFormat(DATEFORMAT_HOUR);
	    sdf_hour.setTimeZone(TimeZone.getTimeZone("UTC"));
	    final String utcTime_hour = sdf_hour.format(date);

	    return utcTime_day+"T"+utcTime_hour;
	}
}
