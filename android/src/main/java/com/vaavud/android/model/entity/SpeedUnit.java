package com.vaavud.android.model.entity;

import android.content.Context;

import com.vaavud.android.R;

import java.text.NumberFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public enum SpeedUnit {

	MS(R.string.unit_ms, 1D),
	KMH(R.string.unit_kmh, 3.6D),
	MPH(R.string.unit_mph, 3600.0D / 1609.344D /* statute mile in meters */),
	KN(R.string.unit_kn, 3600.0D / 1852.0D /* nautical mile in meters */),
	BFT(R.string.unit_bft, Double.NaN);
	
	private static final Set<String> countriesUsingMph = Collections.unmodifiableSet(new HashSet<String>(Arrays.asList("US", "UM", "GB", "CA", "VG", "VI")));
	
	public static SpeedUnit nextUnit(SpeedUnit speedUnit) {
		if (speedUnit == null) {
			return MS;
		}
		
		switch (speedUnit) {
		case MS:
			return KMH;
		case KMH:
			return MPH;
		case MPH:
			return KN;
		case KN:
			return BFT;
		case BFT:
			return MS;
		default:
			return MS;
		}
	}
	
	public static SpeedUnit defaultUnitForCountry(String country) {
		if (countriesUsingMph.contains(country)) {
			return MPH;
		}
		else {
			return MS;
		}
	}
	
	private final int displayId;
	private final double factor;
	private final NumberFormat numberFormat;
	
	private SpeedUnit(int displayId, double factor) {
		this.displayId = displayId;
		this.factor = factor;
		this.numberFormat = NumberFormat.getNumberInstance(Locale.US);
		this.numberFormat.setMaximumFractionDigits(1);
		this.numberFormat.setMinimumFractionDigits(1);
	}
	
	public String getDisplayName(Context context) {
		return (context == null) ? "" : context.getResources().getString(displayId);
	}

	public Float toDisplayValue(Float windSpeedMS) {
		if (windSpeedMS == null) {
			return null;
		}
		
		switch (this) {
		case BFT:
			// Conversion is from http://en.wikipedia.org/wiki/Beaufort_scale
			if (windSpeedMS < 0.3)
	            return 0F;
	        else if (windSpeedMS < 1.6)
	            return 1F;
	        else if (windSpeedMS < 3.5)
	            return 2F;
	        else if (windSpeedMS < 5.5)
	            return 3F;
	        else if (windSpeedMS < 8.0)
	            return 4F;
	        else if (windSpeedMS < 10.8)
	            return 5F;
	        else if (windSpeedMS < 13.9)
	            return 6F;
	        else if (windSpeedMS < 17.2)
	            return 7F;
	        else if (windSpeedMS < 20.8)
	            return 8F;
	        else if (windSpeedMS < 24.5)
	            return 9F;
	        else if (windSpeedMS < 28.5)
	            return 10F;
	        else if (windSpeedMS < 32.7)
	            return 11F;
	        else
	            return 12F; 
		default:
			return windSpeedMS * (float) factor;
		}
	}
	
	public Double toDisplayValue(Double windSpeedMS) {
		if (windSpeedMS == null) {
			return null;
		}
		
		switch (this) {
		case BFT:
			if (windSpeedMS < 0.3)
	            return 0D;
	        else if (windSpeedMS < 1.6)
	            return 1D;
	        else if (windSpeedMS < 3.5)
	            return 2D;
	        else if (windSpeedMS < 5.5)
	            return 3D;
	        else if (windSpeedMS < 8.0)
	            return 4D;
	        else if (windSpeedMS < 10.8)
	            return 5D;
	        else if (windSpeedMS < 13.9)
	            return 6D;
	        else if (windSpeedMS < 17.2)
	            return 7D;
	        else if (windSpeedMS < 20.8)
	            return 8D;
	        else if (windSpeedMS < 24.5)
	            return 9D;
	        else if (windSpeedMS < 28.5)
	            return 10D;
	        else if (windSpeedMS < 32.7)
	            return 11D;
	        else
	            return 12D; 
		default:
			return windSpeedMS * factor;
		}
	}
	
	public String format(Float windSpeedMS) {
		if (windSpeedMS == null || windSpeedMS.floatValue() == Float.NaN) {
			return "-";
		}
		else {
			return numberFormat.format(toDisplayValue(windSpeedMS));
		}
	}

	public String format(Double windSpeedMS) {
		if (windSpeedMS == null || windSpeedMS.doubleValue() == Double.NaN) {
			return "-";
		}
		else {
			return numberFormat.format(toDisplayValue(windSpeedMS));
		}
	}
	
	public String formatWithTwoDigits(Float windSpeedMS) {
		if (windSpeedMS == null || windSpeedMS.floatValue() == Float.NaN) {
			return "-";
		}
		else if (Math.round(toDisplayValue(windSpeedMS)) >= 10) {
	        return Integer.toString(Math.round(toDisplayValue(windSpeedMS)));
	    }
	    else {
	        return numberFormat.format(toDisplayValue(windSpeedMS));
	    }
	}

	public String getEnglishDisplayName(Context context) {
		// TODO Auto-generated method stub
		switch (this) {
		case MS:
			return "m/s";
		case KMH:
			return "km/h";
		case MPH:
			return "mph";
		case KN:
			return "knots";
		case BFT:
			return "bft";
		default:
			return "m/s";
		}
	}
}
