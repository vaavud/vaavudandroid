package com.vaavud.android.measure.sensor;

import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;

import com.vaavud.android.model.entity.LatLng;

public class LocationUpdateManager {

		private static final long TWO_MINUTES = 1000L * 60L * 2L;

		private static LocationUpdateManager instance;
		private LocationManager locationManager;
		private LocationListener locationListener;
		private Location lastLocation;

		public static synchronized LocationUpdateManager getInstance(Context context) {
				if (instance == null) {
						instance = new LocationUpdateManager(context);
				}
				return instance;
		}

		private LocationUpdateManager(Context context) {
				locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);

				locationListener = new LocationListener() {
						public void onLocationChanged(Location location) {
								if (isBetterLocation(location, lastLocation)) {
										//Log.i("LocationUpdateManager", "Got better location (" + location.getLatitude() + "," + location.getLongitude() + ", " + location.getAccuracy() + ")");
										lastLocation = location;
								}
						}

						public void onStatusChanged(String provider, int status, Bundle extras) {
						}

						public void onProviderEnabled(String provider) {
						}

						public void onProviderDisabled(String provider) {
						}
				};
		}

		public void start() {

				if (locationManager.getProvider(LocationManager.GPS_PROVIDER) != null) {
						//Log.i("LocationUpdateManager", "Requesting GPS location updates");
						locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
						if (lastLocation == null) {
								lastLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
						}
				}

				if (locationManager.getProvider(LocationManager.NETWORK_PROVIDER) != null) {
						//Log.i("LocationUpdateManager", "Requesting network location updates");
						locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, locationListener);
						if (lastLocation == null) {
								lastLocation = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
						}
				}
		}

		public void stop() {
				//Log.i("LocationUpdateManager", "removing location listener");
				locationManager.removeUpdates(locationListener);
		}

		public LatLng getLocation() {
				if (lastLocation != null && (System.currentTimeMillis() - lastLocation.getTime()) < TWO_MINUTES) {
						try {
								return new LatLng(lastLocation.getLatitude(), lastLocation.getLongitude());
						} catch (IllegalArgumentException e) {
								return null;
						}
				}
				return null;
		}

		public LatLng getLastLocation() {
				if (lastLocation != null) {
						try {
								return new LatLng(lastLocation.getLatitude(), lastLocation.getLongitude());
						} catch (IllegalArgumentException e) {
								return null;
						}
				}
				return null;
		}

		/**
		 * Determines whether one Location reading is better than the current Location fix
		 *
		 * @param location            The new Location that you want to evaluate
		 * @param currentBestLocation The current Location fix, to which you want to compare the new one
		 */
		private boolean isBetterLocation(Location location, Location currentBestLocation) {
				if (currentBestLocation == null) {
						// A new location is always better than no location
						return true;
				}

				// Check whether the new location fix is newer or older
				long timeDelta = location.getTime() - currentBestLocation.getTime();
				boolean isSignificantlyNewer = timeDelta > TWO_MINUTES;
				boolean isSignificantlyOlder = timeDelta < -TWO_MINUTES;
				boolean isNewer = timeDelta > 0;

				// If it's been more than two minutes since the current location, use the new location
				// because the user has likely moved
				if (isSignificantlyNewer) {
						return true;
						// If the new location is more than two minutes older, it must be worse
				} else if (isSignificantlyOlder) {
						return false;
				}

				// Check whether the new location fix is more or less accurate
				int accuracyDelta = (int) (location.getAccuracy() - currentBestLocation.getAccuracy());
				boolean isLessAccurate = accuracyDelta > 0;
				boolean isMoreAccurate = accuracyDelta < 0;
				boolean isSignificantlyLessAccurate = accuracyDelta > 200;

				// Check if the old and new location are from the same provider
				boolean isFromSameProvider = isSameProvider(location.getProvider(),
								currentBestLocation.getProvider());

				// Determine location quality using a combination of timeliness and accuracy
				if (isMoreAccurate) {
						return true;
				} else if (isNewer && !isLessAccurate) {
						return true;
				} else if (isNewer && !isSignificantlyLessAccurate && isFromSameProvider) {
						return true;
				}
				return false;
		}

		/**
		 * Checks whether two providers are the same
		 */
		private boolean isSameProvider(String provider1, String provider2) {
				if (provider1 == null) {
						return provider2 == null;
				}
				return provider1.equals(provider2);
		}
}
