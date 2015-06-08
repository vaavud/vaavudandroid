package com.vaavud.android.ui.map;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.util.LruCache;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.NetworkImageView;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.OnMapClickListener;
import com.google.android.gms.maps.GoogleMap.OnMarkerClickListener;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.Projection;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.VisibleRegion;
import com.google.maps.android.SphericalUtil;
import com.google.maps.android.ui.IconGenerator;
import com.mixpanel.android.mpmetrics.MixpanelAPI;
import com.sothree.slidinguppanel.SlidingUpPanelLayout;
import com.sothree.slidinguppanel.SlidingUpPanelLayout.SimplePanelSlideListener;
import com.vaavud.android.R;
import com.vaavud.android.measure.MeasureStatus;
import com.vaavud.android.measure.MeasurementController;
import com.vaavud.android.measure.MeasurementReceiver;
import com.vaavud.android.measure.sensor.LocationUpdateManager;
import com.vaavud.android.model.entity.Device;
import com.vaavud.android.model.entity.DirectionUnit;
import com.vaavud.android.model.entity.MapMeasurement;
import com.vaavud.android.model.entity.MeasurementSession;
import com.vaavud.android.model.entity.SpeedUnit;
import com.vaavud.android.network.UploadManager;
import com.vaavud.android.network.listener.MeasurementsResponseListener;
import com.vaavud.android.ui.MainActivity;
import com.vaavud.android.ui.SelectedListener;
import com.vaavud.util.FormatUtil;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class MeasurementMapFragment extends Fragment implements MeasurementsResponseListener, MeasurementReceiver, SelectedListener, OnMarkerClickListener, OnMapClickListener {

		private static final long GRACE_TIME_BETWEEN_READ_MEASUREMENTS = 300L * 1000L;
		private static final long GRACE_TIME_BETWEEN_NETWORK_ERROR_FEEDBACK = 300L * 1000L;
		private static final int MAX_NEARBY_MEASUREMENTS = 50;
		private static final int MARKER_BITMAP_CACHE_SIZE = 150;
		private static final double ANALYTICS_GRID_DEGREE = 0.125D;
		private static final String TAG = "MAP_MEASUREMENT_FRAGMENT";

		private static final String MIXPANEL_TOKEN = "757f6311d315f94cdfc8d16fb4d973c0";

		private static final Comparator<? super MapMeasurement> startTimeComparatorDescending = new Comparator<MapMeasurement>() {
				@Override
				public int compare(MapMeasurement a, MapMeasurement b) {
						long time1 = (a.getStartTime() == null) ? 0L : a.getStartTime().getTime();
						long time2 = (b.getStartTime() == null) ? 0L : b.getStartTime().getTime();
						return (time1 == time2) ? 0 : ((time2 - time1) < 0 ? -1 : 1);
				}
		};

		private static final Comparator<? super MapMeasurement> startTimeComparatorAscending = new Comparator<MapMeasurement>() {
				@Override
				public int compare(MapMeasurement a, MapMeasurement b) {
						long time1 = (a.getStartTime() == null) ? 0L : a.getStartTime().getTime();
						long time2 = (b.getStartTime() == null) ? 0L : b.getStartTime().getTime();
						return (time1 == time2) ? 0 : ((time2 - time1) > 0 ? -1 : 1);
				}
		};

		private boolean isFirst = true;

		private LruCache<String, BitmapDescriptor> markerBitmapCache;

		private int hours = 3;
		private SpeedUnit currentUnit;
		private DirectionUnit directionUnit;
		private Date lastReadMeasurements;
		private Date lastNetworkErrorFeedback;
		private boolean giveNetworkErrorFeedback = false;

		private SlidingUpPanelLayout layout;
		private MapView mapView;
		private GoogleMap map;
		private Map<Marker, MapMeasurement> markerToMeasurement;
		private Map<Marker, MapMeasurement> markerIconToMeasurement;
		private MapMeasurement selectedMeasurement;
		private Marker selectedMarker;

		private TextView mapHoursTextView;
		private TextView mapUnitTextView;

		private NetworkImageView infoMapThumbnailImageView;
		private TextView infoHoursAgoTextView;
		private TextView infoMaxUnitTextView;
		private TextView infoMaxTextView;
		private TextView infoAvgUnitTextView;
		private TextView infoAvgTextView;
		private TextView infoNearbyTextView;
		private TextView infoDirectionTextView;
		private ImageView infoDirectionArrowView;
		private ListView infoNearbyListView;
		private Typeface futuraMediumTypeface;

		private ImageLoader imageLoader;
		private UploadManager uploadManager;
		private Device device;
		private Context context;


		@Override
		public void onAttach(Activity activity) {
				super.onAttach(activity);
				context = activity;
//		Log.i(TAG, "onAttach");
		}

		@Override
		public void onCreate(Bundle savedInstanceState) {
				super.onCreate(savedInstanceState);
				uploadManager = UploadManager.getInstance(context.getApplicationContext());
				device = Device.getInstance(context.getApplicationContext());
//		Log.i(TAG, "onCreate, savedInstanceState" + (savedInstanceState == null ? "=null" : "!=null"));

		}

		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
//		Log.i(TAG, "onCreateView, savedInstanceState" + (savedInstanceState == null ? "=null" : "!=null"));

				selectedMeasurement = null;
				selectedMarker = null;

				if (savedInstanceState != null && savedInstanceState.containsKey("isFirst")) {
						isFirst = savedInstanceState.getBoolean("isFirst");
				}
				if (savedInstanceState != null && savedInstanceState.containsKey("hours")) {
						hours = savedInstanceState.getInt("hours");
				} else if (context != null) {
						float[] hourOptions = Device.getInstance(context.getApplicationContext()).getHourOptions();
						if (hourOptions.length > 0) {
								hours = Math.round(hourOptions[hourOptions.length - 1]);
						}
				}

				View view = inflater.inflate(R.layout.fragment_map, container, false);
				mapView = (MapView) view.findViewById(R.id.map_view);
				mapView.onCreate(savedInstanceState);

				mapHoursTextView = (TextView) view.findViewById(R.id.map_hoursTextView);
				mapHoursTextView.setOnClickListener(new OnClickListener() {
						@Override
						public void onClick(View view) {
								deselectCurrentMeasurement(true);

								if (context == null) {
										return;
								}

								float[] hourOptions = Device.getInstance(context.getApplicationContext()).getHourOptions();
								boolean isOptionChanged = false;
								for (float hourOption : hourOptions) {
										int hourOptionInt = Math.round(hourOption);
										if (hourOptionInt > hours) {
												hours = hourOptionInt;
												isOptionChanged = true;
												break;
										}
								}
								if (!isOptionChanged && hourOptions.length > 0) {
										hours = Math.round(hourOptions[0]);
								}

								mapHoursTextView.setText(String.format(context.getResources().getString(R.string.x_hours), hours));
								readMeasurements(true, true, true);

						}
				});
				mapHoursTextView.setText(String.format(context.getResources().getString(R.string.x_hours), hours));

				mapUnitTextView = (TextView) view.findViewById(R.id.map_unitTextView);
				mapUnitTextView.setOnClickListener(new OnClickListener() {
						@Override
						public void onClick(View view) {
								deselectCurrentMeasurement(true);
								device.setWindSpeedUnit(context.getApplicationContext(),SpeedUnit.nextUnit(currentUnit));
								updateUnit();
						}
				});

				layout = (SlidingUpPanelLayout) view.findViewById(R.id.sliding_layout);
				layout.setSaveEnabled(false);
				layout.setCoveredFadeColor(0x00000000);
				layout.setPanelHeight(0);
				layout.setDragView(view.findViewById(R.id.info_detailsLayout));
				ViewGroup.LayoutParams layoutParams = layout.getLayoutParams();
				layoutParams.height = Math.round(container.getResources().getDisplayMetrics().heightPixels * 0.55F);
				layout.setLayoutParams(layoutParams);

				layout.setPanelSlideListener(new SimplePanelSlideListener() {
						@Override
						public void onPanelCollapsed(View panel) {
								deselectCurrentMeasurement(false);
						}
				});

				infoMapThumbnailImageView = (NetworkImageView) view.findViewById(R.id.info_mapThumbnailImageView);
				infoMapThumbnailImageView.setOnClickListener(new OnClickListener() {
						@Override
						public void onClick(View view) {
								if (selectedMeasurement != null) {
										MapMeasurement measurement = selectedMeasurement;
										deselectCurrentMeasurement(true);
										if (context != null && Device.getInstance(context.getApplicationContext()).isMixpanelEnabled()) {
												MixpanelAPI.getInstance(context.getApplicationContext(), MIXPANEL_TOKEN).track("Map Marker Thumbnail Zoom", null);
										}
										getMap().animateCamera(CameraUpdateFactory.newLatLngZoom(measurement.getPosition(), 14), 700, null);
								}
						}
				});

				if (context != null) {
						((TextView) view.findViewById(R.id.info_maxLabelTextView)).setText(context.getResources().getString(R.string.heading_max).toUpperCase());
						((TextView) view.findViewById(R.id.info_nearbyTextView)).setText(context.getResources().getString(R.string.heading_nearby_measurements).toUpperCase());
				}

				infoHoursAgoTextView = (TextView) view.findViewById(R.id.info_hoursAgoTextView);
				infoMaxUnitTextView = (TextView) view.findViewById(R.id.info_maxUnitTextView);
				infoMaxTextView = (TextView) view.findViewById(R.id.info_maxTextView);
				infoAvgUnitTextView = (TextView) view.findViewById(R.id.info_unitTextView);
				infoAvgTextView = (TextView) view.findViewById(R.id.info_avgSpeedTextView);
				infoNearbyTextView = (TextView) view.findViewById(R.id.info_nearbyTextView);
				infoNearbyListView = (ListView) view.findViewById(R.id.info_nearbyListView);
				infoDirectionTextView = (TextView) view.findViewById(R.id.info_windTextView);
				infoDirectionArrowView = (ImageView) view.findViewById(R.id.info_windArrowView);

				futuraMediumTypeface = Typeface.createFromAsset(view.getContext().getAssets(), "futuraMedium.ttf");
				infoMaxTextView.setTypeface(futuraMediumTypeface);
				infoAvgTextView.setTypeface(futuraMediumTypeface);
				infoDirectionTextView.setTypeface(futuraMediumTypeface);
				mapHoursTextView.setTypeface(futuraMediumTypeface);
				mapUnitTextView.setTypeface(futuraMediumTypeface);

				updateUnit();

				imageLoader = new ImageLoader(Volley.newRequestQueue(context), new ImageLoader.ImageCache() {
						private final LruCache<String, Bitmap> mCache = new LruCache<String, Bitmap>(10);

						public void putBitmap(String url, Bitmap bitmap) {
								mCache.put(url, bitmap);
						}

						public Bitmap getBitmap(String url) {
								return mCache.get(url);
						}
				});

				getMeasurementController().addMeasurementReceiver(this);

				MapsInitializer.initialize(view.getContext());

				return view;
		}

		@Override
		public void onActivityCreated(Bundle savedInstanceState) {
				super.onActivityCreated(savedInstanceState);
//		Log.i(TAG, "onActivityCreated, savedInstanceState" + (savedInstanceState == null ? "=null" : "!=null"));
		}

		@Override
		public void onViewStateRestored(Bundle savedInstanceState) {
				super.onViewStateRestored(savedInstanceState);
		}

		@Override
		public void onStart() {
				super.onStart();
//		Log.i(TAG, "onStart");
		}

		@Override
		public void onResume() {
//		Log.i(TAG, "onResume");
				super.onResume();
				if (mapView != null) {
//			Log.i(TAG, "mapView.onResume");
						mapView.onResume();
				}
				getMap();

				readMeasurements(false, false, true);
		}

		@Override
		public void onPause() {
				super.onPause();
//		Log.i(TAG, "onPause");
				if (mapView != null) {
						mapView.onPause();
				}
		}

		@Override
		public void onStop() {
				super.onStop();
//		Log.i(TAG, "onStop");
		}

		@Override
		public void onDestroyView() {
				super.onDestroyView();
//		Log.i(TAG, "onDestroyView");
				getMeasurementController().removeMeasurementReceiver(this);
				markerBitmapCache = null;
		}

		@Override
		public void onDestroy() {
		Log.d(TAG, "onDestroy");
				if (mapView != null) {
						mapView.onDestroy();
				}
				super.onDestroy();
		}

//		@Override
//		public void onLowMemory() {
//				super.onLowMemory();
//				mapView.onLowMemory();
//		}

		@Override
		public void onDetach() {
				super.onDetach();
//		Log.i(TAG, "onDetach");
		}

		@Override
		public void onSaveInstanceState(Bundle outState) {
				super.onSaveInstanceState(outState);
//		Log.i(TAG, "onSaveInstanceState");
				if (mapView != null) {
						mapView.onSaveInstanceState(outState);
				}
				outState.putBoolean("isFirst", isFirst);
				outState.putInt("hours", hours);
		}



		@Override
		public void onSelected() {
//		Log.i(TAG, "onSelected");
				if (context != null && Device.getInstance(context.getApplicationContext()).isMixpanelEnabled()) {
						MixpanelAPI.getInstance(context.getApplicationContext(), MIXPANEL_TOKEN).track("Map Screen", null);
				}
				updateUnit();
				ensureInitialLocation(true, getMap());
				readMeasurements(false, false, true);
		}

		private MeasurementController getMeasurementController() {
				MainActivity activity = (MainActivity) context;
				if (activity != null) {
						return activity.getMeasurementController();
				}
				return null;
		}

		private boolean isCurrentTab() {
//		Log.i(TAG, "isCurrentTab");
				MainActivity activity = (MainActivity) context;
				if (activity != null) {
						return activity.isCurrentTab(this);
				}
				return false;
		}

		private GoogleMap getMap() {
				if (map != null) {
						return map;
				}
				if (mapView == null) {
						return null;
				}
				GoogleMap map = mapView.getMap();
				if (map == null) {
						return null;
				}

				try {
						MapsInitializer.initialize(context);
						map.setOnMarkerClickListener(this);
						map.setOnMapClickListener(this);
						map.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
						UiSettings settings = map.getUiSettings();
						settings.setCompassEnabled(false);
						settings.setMyLocationButtonEnabled(false);
						settings.setRotateGesturesEnabled(false);
						ensureInitialLocation(false, map);
				} catch (RuntimeException e) {
						return null;
				}
				this.map = map;
				return map;
		}

		private void ensureInitialLocation(boolean notAgain, GoogleMap map) {
				if (isFirst) {
//			Log.i(TAG, "isFirst=true");
						if (map != null) {
								com.vaavud.android.model.entity.LatLng location = LocationUpdateManager.getInstance(context).getLastLocation();
//				Log.i("MeasurementMapFragment", "Setting initial location: " + location);
								if (location != null) {
										map.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(location.getLatitude(), location.getLongitude()), 7));
								}
						}
						if (notAgain) {
								isFirst = false;
						}
				}
		}

		private void updateUnit() {
				SpeedUnit unit = Device.getInstance(context.getApplicationContext()).getWindSpeedUnit();
				directionUnit = Device.getInstance(context.getApplicationContext()).getWindDirectionUnit();
				if (unit != currentUnit) {
						currentUnit = unit;
//			infoMaxUnitTextView.setText(currentUnit.getDisplayName(context));
						infoAvgUnitTextView.setText(currentUnit.getDisplayName(context));
						mapUnitTextView.setText(currentUnit.getDisplayName(context));

						if (selectedMeasurement != null) {
//				Log.d(TAG,"SelectedMeasurement");
								infoMaxTextView.setText(currentUnit.format(selectedMeasurement.getWindSpeedMax()));
								infoAvgTextView.setText(currentUnit.format(selectedMeasurement.getWindSpeedAvg()));
								if (selectedMeasurement.getWindDirection() != null) {
										infoDirectionArrowView.setImageDrawable(getResources().getDrawable(R.drawable.wind_arrow));
										infoDirectionArrowView.setRotation(selectedMeasurement.getWindDirection());
										infoDirectionTextView.setText(directionUnit.format(selectedMeasurement.getWindDirection()));
								} else {
										infoDirectionTextView.setText("-");
								}
						}

						refreshMarkers();
				}
		}

		private void readMeasurements(boolean forceUpdate, boolean showActivityIndicator, boolean giveNetworkErrorFeedback) {
				if (!forceUpdate && lastReadMeasurements != null && ((System.currentTimeMillis() - lastReadMeasurements.getTime()) < GRACE_TIME_BETWEEN_READ_MEASUREMENTS)) {
						return;
				}

				if (showActivityIndicator && context != null) {
						((Activity)context).setProgressBarIndeterminateVisibility(true);
				}

				this.giveNetworkErrorFeedback = giveNetworkErrorFeedback;

				uploadManager.triggerReadMeasurements(hours, this);
		}

		@Override
		public void measurementsReceived(MapMeasurement[] measurements) {
//		Log.i(TAG, "Received " + measurements.length + " measurements");

				if (context == null) {
						// this is an indication that we've probably been detached from the activity
						return;
				}

				GoogleMap map = getMap();
				if (map == null) {
//			Log.e(TAG, "GoogleMap not initialized");
						return;
				}

				giveNetworkErrorFeedback = false;
				lastNetworkErrorFeedback = null;
				((Activity)context).setProgressBarIndeterminateVisibility(false);
				deselectCurrentMeasurement(true);

				lastReadMeasurements = new Date();
				markerToMeasurement = new HashMap<Marker, MapMeasurement>();
				markerIconToMeasurement = new HashMap<Marker, MapMeasurement>();
				map.clear();


				IconGenerator iconFactory = null;

				Arrays.sort(measurements, startTimeComparatorAscending);

				int maxMapMarkers = Device.getInstance(context.getApplicationContext()).getMaxMapMarkers();
				if (maxMapMarkers < measurements.length) {
						MapMeasurement[] truncatedMeasurements = new MapMeasurement[maxMapMarkers];
						System.arraycopy(measurements, measurements.length - truncatedMeasurements.length, truncatedMeasurements, 0, maxMapMarkers);
						measurements = truncatedMeasurements;
				}

				try {
						for (MapMeasurement mapMeasurement : measurements) {

								MarkerOptions optionsIcon = new MarkerOptions();
								optionsIcon.icon(mapMeasurement.getWindDirection() != null ?
												BitmapDescriptorFactory.fromResource(R.drawable.windmarker_direction) :
												BitmapDescriptorFactory.fromResource(R.drawable.windmarker));
								optionsIcon.position(mapMeasurement.getPosition());
								optionsIcon.anchor(0.5F, 0.5F);
								optionsIcon.rotation(mapMeasurement.getWindDirection() != null ? mapMeasurement.getWindDirection() : 0);
								Marker markerIcon = map.addMarker(optionsIcon);
								markerIconToMeasurement.put(markerIcon, mapMeasurement);

								MarkerOptions options = new MarkerOptions();
								iconFactory = createMarkerIconGenerator(false);
								options.icon(getBitmapDescriptorWithText(currentUnit.formatWithTwoDigits(mapMeasurement.getWindSpeedAvg()), iconFactory));
								options.position(mapMeasurement.getPosition());
								options.anchor(0.5F, 0.5F);
								Marker marker = map.addMarker(options);
								markerToMeasurement.put(marker, mapMeasurement);
						}
				} catch (RuntimeException e) {
						lastReadMeasurements = null;
						markerToMeasurement = new HashMap<Marker, MapMeasurement>();
						markerIconToMeasurement = new HashMap<Marker, MapMeasurement>();
						map.clear();
				}
		}

		@Override
		public void measurementsLoadingFailed() {
				if (context != null) {
						((Activity)context).setProgressBarIndeterminateVisibility(false);
						if (giveNetworkErrorFeedback) {
								if (lastNetworkErrorFeedback == null || ((System.currentTimeMillis() - lastNetworkErrorFeedback.getTime()) > GRACE_TIME_BETWEEN_NETWORK_ERROR_FEEDBACK)) {
										if (isCurrentTab()) {
												lastNetworkErrorFeedback = new Date();
												showNoDataFeedbackMessage();
										}
								}
						}
						giveNetworkErrorFeedback = false;
				}
		}

		private void showNoDataFeedbackMessage() {
				AlertDialog.Builder builder = new AlertDialog.Builder(context);
				builder.setTitle(R.string.map_refresh_error_title);
				builder.setMessage(R.string.map_refresh_error_message);
				builder.setPositiveButton(R.string.button_ok, new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface arg0, int arg1) {
						}
				});
				AlertDialog dialog = builder.create();
				dialog.show();
		}

		private IconGenerator createMarkerIconGenerator(boolean selected) {
				IconGenerator iconFactory = new IconGenerator(context);
				ViewGroup markerContainer = (ViewGroup) LayoutInflater.from(context).inflate(R.layout.marker_map, null);
				TextView textView = (TextView) markerContainer.findViewById(R.id.text);
				markerContainer.removeView(textView);
				textView.setId(com.google.maps.android.R.id.text);
				iconFactory.setBackground(new ColorDrawable(Color.TRANSPARENT));
				iconFactory.setContentPadding(5, 5, 5, 5);
				iconFactory.setContentView(textView);
				iconFactory.setTextAppearance(R.style.WindMarker_TextAppearance);

				return iconFactory;
		}

		private Marker getMarkerForMeasurement(MapMeasurement measurement) {
				Iterator<Map.Entry<Marker, MapMeasurement>> it = markerToMeasurement.entrySet().iterator();
				while (it.hasNext()) {
						Map.Entry<Marker, MapMeasurement> entry = it.next();
						if (entry.getValue() == measurement) {
								return entry.getKey();
						}
				}
				return null;
		}

		private Marker getMarkerIconForMeasurement(MapMeasurement measurement) {
				Iterator<Map.Entry<Marker, MapMeasurement>> it = markerIconToMeasurement.entrySet().iterator();
				while (it.hasNext()) {
						Map.Entry<Marker, MapMeasurement> entry = it.next();
						if (entry.getValue() == measurement) {
								return entry.getKey();
						}
				}
				return null;
		}

		private void refreshMarkers() {
				if (markerToMeasurement != null && markerToMeasurement.size() > 0) {
						MapMeasurement[] measurements = markerToMeasurement.values().toArray(new MapMeasurement[markerToMeasurement.size()]);
						measurementsReceived(measurements);
				}
		}

		@Override
		public boolean onMarkerClick(Marker marker) {
				deselectCurrentMeasurement(false);
				selectedMeasurement = (markerToMeasurement == null) ? null : markerToMeasurement.get(marker);
				if (selectedMeasurement == null) {
						selectedMeasurement = (markerIconToMeasurement == null) ? null : markerIconToMeasurement.get(marker);
				}
				if (selectedMeasurement != null) {
						JSONObject props = new JSONObject();
						try {
								props.put("Source", "Map");
						} catch (JSONException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
						}
						if (context != null && Device.getInstance(context.getApplicationContext()).isMixpanelEnabled()) {
								MixpanelAPI.getInstance(context.getApplicationContext(), MIXPANEL_TOKEN).track("Map Marker Selected", props);
						}

						selectMeasurement(selectedMeasurement, marker);
						return true;
				}
//				Log.i(TAG, "on Marker Click: False");
				return false;
		}

		private void selectMeasurement(MapMeasurement measurement, Marker marker) {

				selectedMarker = getMarkerIconForMeasurement(selectedMeasurement);

				selectedMarker.setIcon(selectedMeasurement.getWindDirection() != null ?
								BitmapDescriptorFactory.fromResource(R.drawable.windmarker_direction_selected) :
								BitmapDescriptorFactory.fromResource(R.drawable.windmarker_selected));
				selectedMarker.setRotation(selectedMeasurement.getWindDirection() != null ? selectedMeasurement.getWindDirection() : 0);

				Marker textMarker = getMarkerForMeasurement(selectedMeasurement);
				IconGenerator iconGenerator = createMarkerIconGenerator(true);
				textMarker.setIcon(BitmapDescriptorFactory.fromBitmap(iconGenerator.makeIcon(currentUnit.formatWithTwoDigits(measurement.getWindSpeedAvg()))));
				textMarker.getPosition();

				infoHoursAgoTextView.setText(FormatUtil.formatRelativeDate(measurement.getStartTime(), context));
				infoMaxTextView.setText(currentUnit.format(selectedMeasurement.getWindSpeedMax()));
				infoAvgTextView.setText(currentUnit.format(selectedMeasurement.getWindSpeedAvg()));
				if (selectedMeasurement.getWindDirection() != null) {
						infoDirectionArrowView.setImageDrawable(getResources().getDrawable(R.drawable.wind_arrow));
						infoDirectionArrowView.setRotation(selectedMeasurement.getWindDirection());
						infoDirectionTextView.setText(directionUnit.format(selectedMeasurement.getWindDirection()));
				} else {
						infoDirectionArrowView.setImageDrawable(null);
						infoDirectionTextView.setText("-");
				}

				int height = infoMapThumbnailImageView.getHeight();
//		Log.d(TAG,"Height: "+height);

				String iconUrl = "http://vaavud.com/appgfx/SmallWindMarker.png";
				String markers = "icon:" + iconUrl + "|shadow:false|" + selectedMeasurement.getPosition().latitude + "," + selectedMeasurement.getPosition().longitude;
				try {
						markers = URLEncoder.encode(markers, "utf-8");
				} catch (UnsupportedEncodingException e) {
						// shouldn't happen
				}
				String mapUrl = "http://maps.google.com/maps/api/staticmap?markers=" + markers + "&zoom=15&size=" + height + "x" + height + "&sensor=true";
//		Log.d(TAG,"URL: "+mapUrl);
				infoMapThumbnailImageView.setImageUrl(mapUrl, imageLoader);

				List<MapMeasurement> nearbyMeasurements = getNearbyMeasurements(measurement);
				NearbyArrayAdapter adapter = new NearbyArrayAdapter(context, nearbyMeasurements);
				infoNearbyListView.setAdapter(adapter);
				infoNearbyListView.setOnItemClickListener(adapter);

				infoNearbyTextView.setVisibility(nearbyMeasurements.isEmpty() ? View.GONE : View.VISIBLE);
				infoNearbyListView.setVisibility(nearbyMeasurements.isEmpty() ? View.GONE : View.VISIBLE);

				if (!layout.isExpanded()) {
						layout.expandPane();
				}

				GoogleMap map = getMap();
				if (map != null) {

						float mapHeight = mapView.getHeight();
						float panelHeight = layout.getHeight();
						int offset = Math.round((mapHeight / 2F) - ((mapHeight - panelHeight) / 2F));
						Projection projection = map.getProjection();

						Point markerPosition = projection.toScreenLocation(measurement.getPosition());
						Point target = new Point(markerPosition.x, markerPosition.y + offset);
						LatLng targetLatLng = projection.fromScreenLocation(target);

						//Log.i(TAG, "mapHeight=" + mapHeight + ", panelHeight=" + panelHeight + ", offset=" + offset);

						map.animateCamera(CameraUpdateFactory.newLatLng(targetLatLng), 250, null);
				}
		}

		private void deselectCurrentMeasurement(boolean collapseInfo) {
				IconGenerator iconGenerator = null;
				if (collapseInfo) {
						layout.collapsePane();
				}

				if (selectedMarker != null) {
						MapMeasurement measurement = markerIconToMeasurement.get(selectedMarker);
//				Log.d(TAG,"Deselected measurement: " + measurement.getWindSpeedAvg());	
						selectedMarker.setIcon(measurement.getWindDirection() != null ?
										BitmapDescriptorFactory.fromResource(R.drawable.windmarker_direction) :
										BitmapDescriptorFactory.fromResource(R.drawable.windmarker));
						selectedMarker.setRotation(measurement.getWindDirection() != null ? measurement.getWindDirection() : 0);
						try {
								Marker textMarker = getMarkerForMeasurement(measurement);
								textMarker.setIcon(getBitmapDescriptorWithText(currentUnit.formatWithTwoDigits(selectedMeasurement.getWindSpeedAvg()), iconGenerator));
						} catch (RuntimeException e) {
//				Log.e(TAG, e.toString());
								// protect from marker not being on the map anymore
						}
				}
				selectedMeasurement = null;
				selectedMarker = null;
				if (context != null) {
						infoNearbyListView.setAdapter(new NearbyArrayAdapter(context, Collections.<MapMeasurement>emptyList()));
				}
		}

		private List<MapMeasurement> getNearbyMeasurements(MapMeasurement measurement) {

				VisibleRegion visibleRegion = getMap().getProjection().getVisibleRegion();
				double horizontalDistanceMeters = SphericalUtil.computeDistanceBetween(visibleRegion.nearLeft, visibleRegion.nearRight);
				double nearbyRadius = horizontalDistanceMeters / 6.0D;

				List<MapMeasurement> nearbyMeasurements = new ArrayList<MapMeasurement>();

				if (markerToMeasurement != null) {
						for (MapMeasurement m : markerToMeasurement.values()) {
								if (m == measurement) {
										continue;
								}
								double distanceInMeters = Math.abs(SphericalUtil.computeDistanceBetween(measurement.getPosition(), m.getPosition()));
								if (distanceInMeters < nearbyRadius) {
										nearbyMeasurements.add(m);
								}
						}

						Collections.sort(nearbyMeasurements, startTimeComparatorDescending);
				}

				if (nearbyMeasurements.size() > MAX_NEARBY_MEASUREMENTS) {
						nearbyMeasurements = nearbyMeasurements.subList(0, MAX_NEARBY_MEASUREMENTS);
				}

				return nearbyMeasurements;
		}

		@Override
		public void onMapClick(LatLng latLng) {
//		Log.i(TAG, "onMapClick");
				deselectCurrentMeasurement(true);
		}

		@Override
		public void measurementAdded(MeasurementSession session, Float time, Float currentWindSpeed, Float avgWindSpeed, Float maxWindSpeed, Float direction) {
				// do nothing
		}

		@Override
		public void measurementFinished(MeasurementSession session) {
				// force update of map even if within grace period
				lastReadMeasurements = null;
		}

		@Override
		public void measurementStatusChanged(MeasureStatus status) {
				// do nothing
		}


		/**
		 * Note: ONLY call this method with identically configured IconGenerators since the BitmapDescriptor
		 * is fetched from a cache, if present, otherwise generated using the specified IconGenerator.
		 */
		private BitmapDescriptor getBitmapDescriptorWithText(String text, IconGenerator generator) {
				if (markerBitmapCache == null) {
						markerBitmapCache = new LruCache<String, BitmapDescriptor>(MARKER_BITMAP_CACHE_SIZE);
				}

				BitmapDescriptor bitmapDescriptor = markerBitmapCache.get(text);
				if (bitmapDescriptor == null) {
						bitmapDescriptor = BitmapDescriptorFactory.fromBitmap(generator.makeIcon(text));
						markerBitmapCache.put(text, bitmapDescriptor);
				}
				return bitmapDescriptor;
		}

		/**
		 * Note: ONLY call this method with identically configured IconGenerators since the BitmapDescriptor
		 * is fetched from a cache, if present, otherwise generated using the specified IconGenerator.
		 */
		private BitmapDescriptor getBitmapDescriptorWithIcon(String text, IconGenerator generator) {
				if (markerBitmapCache == null) {
						markerBitmapCache = new LruCache<String, BitmapDescriptor>(MARKER_BITMAP_CACHE_SIZE);
				}

				BitmapDescriptor bitmapDescriptor = markerBitmapCache.get(text);
				if (bitmapDescriptor == null) {
						bitmapDescriptor = BitmapDescriptorFactory.fromBitmap(generator.makeIcon(text));
						markerBitmapCache.put(text, bitmapDescriptor);
				}
				return bitmapDescriptor;
		}

		private String gridValueFromCoordinate(MapMeasurement mapMeasurement) {
				double latitude = mapMeasurement.getPosition().latitude;
				double longitude = mapMeasurement.getPosition().longitude;

				double latitudeInRadians = latitude * Math.PI / 180.0D;

				int latitudeGrid = (int) Math.floor(latitude / (Math.cos(latitudeInRadians) * 2 * ANALYTICS_GRID_DEGREE));
				int longitudeGrid = (int) Math.floor(longitude / ANALYTICS_GRID_DEGREE);

				return Double.toString(ANALYTICS_GRID_DEGREE) + ":" + latitudeGrid + "," + longitudeGrid;
		}

		private class NearbyArrayAdapter extends ArrayAdapter<MapMeasurement> implements OnItemClickListener {

				private final Context context;
				private final List<MapMeasurement> values;

				public NearbyArrayAdapter(Context context, List<MapMeasurement> values) {
						super(context, R.layout.listview_row_nearby, values);
						this.context = context;
						this.values = values;
				}

				@Override
				public View getView(int position, View convertView, ViewGroup parent) {
						MapMeasurement measurement = values.get(position);

						LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
						View rowView = inflater.inflate(R.layout.listview_row_nearby, parent, false);

						TextView speedTextView = (TextView) rowView.findViewById(R.id.nearby_speedTextView);
						speedTextView.setText(currentUnit.format(measurement.getWindSpeedAvg()));
						speedTextView.setTypeface(futuraMediumTypeface);

						TextView unitTextView = (TextView) rowView.findViewById(R.id.nearby_unitTextView);
						unitTextView.setText(currentUnit.getDisplayName(context));

						TextView hoursTextView = (TextView) rowView.findViewById(R.id.nearby_hoursTextView);
						hoursTextView.setText(FormatUtil.formatRelativeDate(measurement.getStartTime(), context));

						TextView directionTextView = (TextView) rowView.findViewById(R.id.nearby_windTextView);
						if (measurement.getWindDirection() != null) {
								directionTextView.setText(directionUnit.format(measurement.getWindDirection()));
						}

						return rowView;
				}

				@Override
				public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
						if (position < values.size()) {
								MapMeasurement measurement = values.get(position);
								JSONObject props = new JSONObject();
								try {
										props.put("Source", "Nearby Measurements");
								} catch (JSONException e) {
										// TODO Auto-generated catch block
										e.printStackTrace();
								}
								if (context != null && Device.getInstance(context.getApplicationContext()).isMixpanelEnabled()) {
										MixpanelAPI.getInstance(context.getApplicationContext(), MIXPANEL_TOKEN).track("Map Marker Selected", props);
								}
								deselectCurrentMeasurement(false);
								selectedMeasurement = measurement;
								selectMeasurement(selectedMeasurement, getMarkerForMeasurement(measurement));
						}
				}
		}

}
