package com.vaavud.android.model.entity;

import java.io.Serializable;

public class LatLng implements Serializable {

	private static final void checkValid(Double latitude, Double longitude) {
		if (latitude == null) {
			throw new IllegalArgumentException("Latitude is null for LatLng(" + latitude + ", " + longitude + ")");
		}
		else if (longitude == null) {
			throw new IllegalArgumentException("Longitude is null for LatLng(" + latitude + ", " + longitude + ")");			
		}
		else if (latitude.isInfinite()) {
			throw new IllegalArgumentException("Latitude is infinite for LatLng(" + latitude + ", " + longitude + ")");			
		}
		else if (longitude.isInfinite()) {
			throw new IllegalArgumentException("Longitude is infitite for LatLng(" + latitude + ", " + longitude + ")");			
		}
		else if (latitude.isNaN()) {
			throw new IllegalArgumentException("Latitude is NaN for LatLng(" + latitude + ", " + longitude + ")");			
		}		
		else if (longitude.isNaN()) {
			throw new IllegalArgumentException("Longitude is NaN for LatLng(" + latitude + ", " + longitude + ")");			
		}		
		else if (latitude < -90) {
			throw new IllegalArgumentException("Latitude is less than -90 for LatLng(" + latitude + ", " + longitude + ")");			
		}
		else if (latitude > 90) {
			throw new IllegalArgumentException("Latitude is greater than 90 for LatLng(" + latitude + ", " + longitude + ")");			
		}
		else if (longitude < -180) {
			throw new IllegalArgumentException("Longitude is less than -180 for LatLng(" + latitude + ", " + longitude + ")");			
		}		
		else if (longitude > 180) {
			throw new IllegalArgumentException("Longitude is greater than 180 for LatLng(" + latitude + ", " + longitude + ")");			
		}
		else if (latitude == 0 && longitude == 0) {
			throw new IllegalArgumentException("Conspicuous latitude-longitude of (0,0)");			
		}
	}

	private Double latitude;
	private Double longitude;

	public LatLng(LatLng latLng) {
		if (latLng == null) {
			throw new IllegalArgumentException("LatLng is null");
		}
		checkValid(latLng.getLatitude(), latLng.getLongitude());
		latitude = latLng.latitude;
		longitude = latLng.longitude;
	}

	public LatLng(Double latitude, Double longitude) {
		checkValid(latitude, longitude);
		this.latitude = latitude;
		this.longitude = longitude;
	}

	public Double getLatitude() {
		return latitude;
	}

	public void setLatitude(Double latitude) {
		this.latitude = latitude;
	}

	public Double getLongitude() {
		return longitude;
	}

	public void setLongitude(Double longitude) {
		this.longitude = longitude;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((latitude == null) ? 0 : latitude.hashCode());
		result = prime * result + ((longitude == null) ? 0 : longitude.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		LatLng other = (LatLng) obj;
		if (latitude == null) {
			if (other.latitude != null) {
				return false;
			}
		}
		else if (!latitude.equals(other.latitude)) {
			return false;
		}
		if (longitude == null) {
			if (other.longitude != null) {
				return false;
			}
		}
		else if (!longitude.equals(other.longitude)) {
			return false;
		}
		return true;
	}

	@Override
	public String toString() {
		return "(" + latitude + ", " + longitude + ")";
	}
}
