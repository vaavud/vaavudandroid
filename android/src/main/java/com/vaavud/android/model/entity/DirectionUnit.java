package com.vaavud.android.model.entity;

import android.content.Context;

import com.vaavud.android.R;

public enum DirectionUnit {

	CARDINAL(R.string.unit_cardinal),
	DEGREES(R.string.unit_degrees);

	public static DirectionUnit nextUnit(DirectionUnit speedUnit) {
		if (speedUnit == null) {
			return CARDINAL;
		}
		
		switch (speedUnit) {
		case CARDINAL:
			return DEGREES;
		default:
			return CARDINAL;
		}
	}
	
	private final int displayId;
	
	private DirectionUnit(int displayId) {
		this.displayId = displayId;
	}
	
	public String getDisplayName(Context context) {
		return (context == null) ? "" : context.getResources().getString(displayId);
	}

	public String toDisplayValue(Float windDirectionDegree) {
		if (windDirectionDegree == null) {
			return null;
		}
		switch (this) {
		case CARDINAL:
			// Conversion is from http://en.wikipedia.org/wiki/Beaufort_scale
			if (windDirectionDegree> 348.75 && windDirectionDegree < 11.25)
	            return "N";
	        else if (windDirectionDegree < 33.75)
	            return "NNE";
	        else if (windDirectionDegree < 56.25)
	            return "NE";
	        else if (windDirectionDegree < 78.75)
	            return "ENE";
	        else if (windDirectionDegree < 101.25)
	            return "E";
	        else if (windDirectionDegree < 123.75)
	            return "ESE";
	        else if (windDirectionDegree < 146.25)
	            return "SE";
	        else if (windDirectionDegree < 168.75)
	            return "SSE";
	        else if (windDirectionDegree < 191.25)
	            return "S";
	        else if (windDirectionDegree < 213.75)
	            return "SSW";
	        else if (windDirectionDegree < 236.25)
	            return "SW";
	        else if (windDirectionDegree < 258.75)
	        	return "WSW";
        	else if (windDirectionDegree < 281.25)
	            return "W";
        	else if (windDirectionDegree < 303.75)
	            return "WNW";
        	else if (windDirectionDegree < 326.25)
	            return "NW";
	        else
	            return "NNW"; 
		default:
			return windDirectionDegree.intValue()+"°";
		}
	}
	
	
	public String format(Float windDirectionDegree) {
		if (windDirectionDegree == null || windDirectionDegree.floatValue() == Float.NaN) {
			return "-";
		}
		else {
			return toDisplayValue(windDirectionDegree);
		}
	}

}
