package com.vaavud.util;

import android.content.Context;

import com.vaavud.android.R;

import java.text.SimpleDateFormat;
import java.util.Date;

public final class FormatUtil {

	public static String formatRelativeDate(Date date, Context context) {
		if (date == null || context == null) {
			return "-";
		}
	    long timeAgoSeconds = (System.currentTimeMillis() - date.getTime()) / 1000L;
	    if (timeAgoSeconds < 0) {
	        return context.getResources().getString(R.string.rel_time_future);
	    }
	    int minsAgo = Math.round(timeAgoSeconds / 60L);
	    int hoursAgo = Math.round(timeAgoSeconds / 3600L);
	    int daysAgo = Math.round(timeAgoSeconds / (3600L*24L));
	    if (minsAgo < 1) {
	        return context.getResources().getString(R.string.rel_time_now);
	    }
	    else if (minsAgo == 1) {
	        return context.getResources().getString(R.string.rel_time_1_min_ago);
	    }
	    else if (minsAgo < 60) {
	    	return String.format(context.getResources().getString(R.string.rel_time_x_mins_ago), minsAgo);
	    }
	    else if (hoursAgo == 1) {
	        return context.getResources().getString(R.string.rel_time_1_hour_ago);
	    }
	    else if (hoursAgo < 48) {
	    	return String.format(context.getResources().getString(R.string.rel_time_x_hours_ago), hoursAgo);
	    }
	    else if (daysAgo == 1) {
	        return context.getResources().getString(R.string.rel_time_1_day_ago);
	    }
	    else {
	    	return String.format(context.getResources().getString(R.string.rel_time_x_days_ago), hoursAgo);
	    }
	}
	
	public static String formatHourMinuteDate(Date date) {
				
		if (date == null) return "-";	
		return new SimpleDateFormat("HH:mm").format(date);
	}
	
	public static String formatDayDate(Date date) {
		
		if (date == null) return "-";	
		return new SimpleDateFormat("d").format(date);
	}
	public static String formatMonthDate(Date date) {
		
		if (date == null) return "-";	
		return new SimpleDateFormat("MMM").format(date);
	}
	
	public static String formatweekDayDate(Date date) {
		
		if (date == null) return "-";	
		return new SimpleDateFormat("MMM d yy").format(date);
	}
	
	private FormatUtil() {
	}
}
