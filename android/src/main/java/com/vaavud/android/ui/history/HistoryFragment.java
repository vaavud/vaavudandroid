package com.vaavud.android.ui.history;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.graphics.Bitmap.CompressFormat;

import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.Fragment;
import android.support.v7.view.ActionMode;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.NetworkImageView;
import com.android.volley.toolbox.Volley;
import com.mixpanel.android.mpmetrics.MixpanelAPI;
import com.vaavud.android.R;
import com.vaavud.android.model.VaavudDatabase;
import com.vaavud.android.model.entity.Device;
import com.vaavud.android.model.entity.DirectionUnit;
import com.vaavud.android.model.entity.MeasurementSession;
import com.vaavud.android.model.entity.SpeedUnit;
import com.vaavud.android.network.UploadManager;
import com.vaavud.android.network.listener.HistoryMeasurementsResponseListener;
import com.vaavud.android.ui.BackPressedListener;
import com.vaavud.android.ui.MainActivity;
import com.vaavud.android.ui.SelectedListener;
import com.vaavud.android.ui.SelectorListener;
import com.vaavud.android.ui.history.cache.ImageCacheManager;
import com.vaavud.android.ui.history.cache.ImageCacheManager.CacheType;
import com.vaavud.util.FormatUtil;
import com.vaavud.util.MixpanelUtil;
import com.vaavud.util.UUIDUtil;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class HistoryFragment extends Fragment implements BackPressedListener, SelectedListener, HistoryMeasurementsResponseListener {

		private static final String TAG = "HISTORY_FRAGMENT";

		private SelectorListener mCallback;

		private static final long GRACE_TIME_BETWEEN_READ_MEASUREMENTS = 11 * 1000L;

		private static int DISK_IMAGECACHE_SIZE = 1024 * 1024 * 10;
		private static CompressFormat DISK_IMAGECACHE_COMPRESS_FORMAT = CompressFormat.JPEG;
		private static int DISK_IMAGECACHE_QUALITY = 50;


		private List<MeasurementSession> measurmentSessions;
		private SpeedUnit unit;
		private DirectionUnit directionUnit;
		private String splitterDay;
		private String splitterMonth;

		private NetworkImageView historyMapThumbnailImageView;
		private TextView maxSpeed;
		private TextView averageSpeed;
		private TextView startTime;
		private TextView speedUnit;
		private TextView dayText;
		private TextView monthText;
		private TextView windText;
		private ImageView windArrowView;
		private LinearLayout dateView;
		private ImageLoader imageLoader;
		private ImageCacheManager imageCacheManager;
		private Typeface futuraMediumTypeface;

		private ProgressDialog progress;

		private HistoryArrayAdapter historyAdapter;

		private ListView historyListView;

		private View view;

		private Date lastReadMeasurements;

		private boolean mActionModeAvailable = false;
		private boolean isMeasurementReceived = false;

		private Context context;
		private UploadManager uploadManager;
		private Device device;

		private static final String MIXPANEL_TOKEN = "757f6311d315f94cdfc8d16fb4d973c0";


		private static final Comparator<? super MeasurementSession> startTimeComparatorDescending = new Comparator<MeasurementSession>() {
				@Override
				public int compare(MeasurementSession a, MeasurementSession b) {
						long time1 = (a.getStartTime() == null) ? 0L : a.getStartTime().getTime();
						long time2 = (b.getStartTime() == null) ? 0L : b.getStartTime().getTime();
						return (time1 == time2) ? 0 : ((time2 - time1) < 0 ? -1 : 1);
				}
		};

		@Override
		public void onAttach(Activity activity) {
				super.onAttach(activity);
				context = activity;
		}

		@Override
		public void onCreate(Bundle savedInstanceState) {
				//				Log.i(TAG, "onCreate, savedInstanceState" + (savedInstanceState == null ? "=null" : "!=null"));

				super.onCreate(savedInstanceState);
				if (measurmentSessions == null) {
						measurmentSessions = getMeasurementsFromDB();
				}
				device = Device.getInstance(context.getApplicationContext());
				unit = device.getWindSpeedUnit();
				directionUnit = device.getWindDirectionUnit();
				futuraMediumTypeface = Typeface.createFromAsset(getActivity().getAssets(), "futuraMedium.ttf");
				uploadManager = UploadManager.getInstance(context.getApplicationContext());

		}

		@Override
		public View onCreateView(LayoutInflater inflater, final ViewGroup container, Bundle savedInstanceState) {
				//		Log.i(TAG, "onCreateView, savedInstanceState" + (savedInstanceState == null ? "=null" : "!=null"));
				if (progress != null && progress.isShowing()) return new LinearLayout(getActivity());
				if (measurmentSessions.size() > 0) {
						view = inflater.inflate(R.layout.fragment_history, container, false);
						historyListView = (ListView) view.findViewById(R.id.history_ListView);
						historyListView.setItemsCanFocus(true);
						historyAdapter = new HistoryArrayAdapter(getActivity(), measurmentSessions);
						historyListView.setAdapter(historyAdapter);
						historyListView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
						historyListView.setOnItemLongClickListener(new OnItemLongClickListener() {

								@Override
								public boolean onItemLongClick(AdapterView<?> adapter,
																							 View view, int position, long arg3) {
										view.setSelected(true);

										ActionMode.Callback modeCallBack = new ActionMode.Callback() {
												private int nr = 0;

												public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
														return false;
												}

												public void onDestroyActionMode(ActionMode mode) {
														historyAdapter.clearSelection();
												}

												public boolean onCreateActionMode(ActionMode mode, Menu menu) {
														((MainActivity) getActivity()).setActionMode(mode);
														MenuInflater inflater = getActivity().getMenuInflater();
														inflater.inflate(R.menu.history, menu);
														if (historyAdapter.mSelection.size() > 0) {
																nr = historyAdapter.mSelection.size();
																mode.setTitle(nr + " selected");
														} else {
																nr = 0;
														}
														return true;
												}

												public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
														Log.d(TAG, "onActionItemClicked");
														int position = -1;
														switch (item.getItemId()) {
																case R.id.action_delete:

																		Iterator<Integer> it = historyAdapter.getCurrentCheckedPosition().iterator();
																		List<MeasurementSession> removeList = new ArrayList<MeasurementSession>();
																		while (it.hasNext()) {
																				position = it.next();
																				removeList.add(measurmentSessions.get(position));
																				historyAdapter.removeSelection(position);
																		}
																		for (int i = 0; i < removeList.size(); i++) {
																				VaavudDatabase.getInstance(context.getApplicationContext()).deleteMeasurementSession(removeList.get(i));
																				uploadManager.deleteMeasurement(removeList.get(i));
																				//MixPanel
																				JSONObject props = new JSONObject();
																				try {
																						props.put("Measurement Speed", measurmentSessions.get(measurmentSessions.indexOf(removeList.get(i))).getWindSpeedAvg());
																				} catch (JSONException e) {
																						e.printStackTrace();
																				}
																				if (context != null && device.isMixpanelEnabled()) {
																						MixpanelAPI.getInstance(context.getApplicationContext(), MIXPANEL_TOKEN).track("Delete Measurement", props);
																				}
																				measurmentSessions.remove(removeList.get(i));
																		}
																		historyAdapter.clearSelection();
																		((MainActivity)getActivity()).getViewPager().getAdapter().notifyDataSetChanged();
																		mode.finish();

																		return true;
														}
														return false;
												}
										};

										((MainActivity) getActivity()).startSupportActionMode(modeCallBack);
										historyListView.setItemChecked(position, !historyAdapter.isPositionChecked(position));
										historyAdapter.setNewSelection(position, true);
										return false;
								}
						});
						imageCacheManager = ImageCacheManager.getInstance();
						imageCacheManager.init(getActivity(), Environment.getExternalStorageDirectory().getAbsolutePath(),
										DISK_IMAGECACHE_SIZE, DISK_IMAGECACHE_COMPRESS_FORMAT, DISK_IMAGECACHE_QUALITY, CacheType.DISK, Volley.newRequestQueue(context));

						imageLoader = imageCacheManager.getImageLoader();
				} else {
						view = new ArrowLayoutView(context);
				}
				MixpanelUtil.updateMeasurementProperties(getActivity());
				return view;
		}

		@Override
		public void onCreateContextMenu(ContextMenu menu, View v,
																		ContextMenuInfo menuInfo) {
				super.onCreateContextMenu(menu, v, menuInfo);
				menu.setHeaderTitle(getActivity().getResources().getString(R.string.history_title));

				AdapterContextMenuInfo info = (AdapterContextMenuInfo) menuInfo;
				MenuInflater inflater = getActivity().getMenuInflater();
				historyAdapter.getView(info.position, historyListView.getChildAt(info.position), historyListView).setBackgroundColor(getActivity().getResources().getColor(R.color.lightgray));
				inflater.inflate(R.menu.history, menu);
		}

		@Override
		public void onActivityCreated(Bundle savedInstanceState) {
				super.onActivityCreated(savedInstanceState);
				//				Log.i(TAG, "onActivityCreated, savedInstanceState" + (savedInstanceState == null ? "=null" : "!=null"));
		}

		@Override
		public void onViewStateRestored(Bundle savedInstanceState) {
				super.onViewStateRestored(savedInstanceState);
		}

		@Override
		public void onStart() {
				super.onStart();
				//				Log.i(TAG, "onStart");
		}

		@Override
		public void onResume() {
				//		Log.i(TAG, "onResume");
				super.onResume();
				if (lastReadMeasurements != null && ((System.currentTimeMillis() - lastReadMeasurements.getTime()) > GRACE_TIME_BETWEEN_READ_MEASUREMENTS)) {
						return;
				}
				if (isCurrentTab()) {
						if (measurmentSessions.size() > 0) {
								readHistoryMeasurements(true, false, true, measurmentSessions.get(0).getEndTime(), getUUIDHash());
						} else {
								readHistoryMeasurements(true, true, true, new Date(0L), getUUIDHash());
						}
				}
		}

		@Override
		public void onPause() {
				//				Log.i(TAG, "onPause");
				super.onPause();
		}

		@Override
		public void onStop() {
				super.onStop();
				//				Log.i(TAG, "onStop");
		}

		@Override
		public void onDestroyView() {
				if (historyAdapter != null) {
						historyAdapter.clearSelection();
						unregisterForContextMenu(historyListView);
				}
				super.onDestroyView();
				//				Log.i(TAG, "onDestroyView");
		}

		@Override
		public void onDestroy() {
				super.onDestroy();
				//				Log.i(TAG, "onDestroy");
		}

		@Override
		public void onDetach() {
				super.onDetach();
				//				Log.i(TAG, "onDetach");
		}

		@Override
		public void onSaveInstanceState(Bundle outState) {
				super.onSaveInstanceState(outState);
				//				Log.i(TAG, "onSaveInstanceState");
		}

		@Override
		public void onLowMemory() {
				super.onLowMemory();
				//				Log.i(TAG, "onLowMemory");
		}

		@Override
		public boolean onBackPressed() {
				return false;
		}

		private boolean isCurrentTab() {
				//		Log.i(TAG, "isCurrentTab");
				MainActivity activity = (MainActivity) getActivity();
				if (activity != null) {
						return activity.isCurrentTab(this);
				}
				return false;
		}


		private List<MeasurementSession> getMeasurementsFromDB() {
				//Log.i(TAG, "getMeasurementsFromDB");
				List<MeasurementSession> measurementsList = VaavudDatabase.getInstance(context.getApplicationContext()).getMeasurementSessions();
				Collections.sort(measurementsList, startTimeComparatorDescending);
				return measurementsList;
		}

		private void readHistoryMeasurements(boolean forceUpdate, boolean showActivityIndicator, boolean giveNetworkErrorFeedback, Date lastHistoryReadMeasurement, String hash) {

				if (!forceUpdate && lastHistoryReadMeasurement != null) {
						return;
				}
				uploadManager.triggerReadHistoryMeasurements(lastHistoryReadMeasurement, hash, this);
		}


		private String getUUIDHash() {
				String hashedUUIDs;

				StringBuilder sb1 = new StringBuilder(measurmentSessions.size() * (36 + 10));
				if (measurmentSessions.size() > 0) {
						for (int i = measurmentSessions.size() - 1; i >= 0; i--) {
								String endTimeSecondsString = Long.toString((long) Math.ceil(((double) measurmentSessions.get(i).getEndTime().getTime()) / 1000D));
								sb1.append(measurmentSessions.get(i).getUuid());
								sb1.append(endTimeSecondsString);
						}

						hashedUUIDs = UUIDUtil.md5Hash(sb1.toString().toUpperCase(Locale.US));

						return hashedUUIDs;
				} else {
						return null;
				}
		}

		@Override
		public void onSelected() {
				//		Log.d(TAG,"On Selected");
				if (context != null && Device.getInstance(context.getApplicationContext()).isMixpanelEnabled()) {
						MixpanelAPI.getInstance(context.getApplicationContext(), MIXPANEL_TOKEN).track("History Screen", null);
				}
		}

		@Override
		public void measurementsLoadingFailed() {
				if (context != null && Device.getInstance(context.getApplicationContext()).isMixpanelEnabled()) {
						MixpanelAPI.getInstance(context.getApplicationContext(), MIXPANEL_TOKEN).track("Measurements Loading Failed", null);
						//		Log.e(TAG,"Measurements Loading Failed");
				}
		}

		@Override
		public void measurementsReceived(ArrayList<MeasurementSession> histObjList) {
				lastReadMeasurements = new Date();
				Iterator<MeasurementSession> itr = measurmentSessions.iterator();
				if (histObjList != null) {
						while (itr.hasNext()) {
								MeasurementSession current = itr.next();
								for (int i = 0; i < histObjList.size(); i++) {
										if (current.equals(histObjList.get(i))) {
												if (current.getEndTime().getTime() < histObjList.get(i).getEndTime().getTime()) {
														current.setEndTime(histObjList.get(i).getEndTime());
														current.setPoints(histObjList.get(i).getPoints());
														current.setWindSpeedAvg(histObjList.get(i).getWindSpeedAvg());
														current.setWindSpeedMax(histObjList.get(i).getWindSpeedMax());
														current.setWindDirection(histObjList.get(i).getWindDirection());
														VaavudDatabase.getInstance(context.getApplicationContext()).updateCompleteMeasurementSession(current);
												}
												histObjList.remove(i);
												break;
										}
								}
						}
						if (histObjList.size() > 0) {
								for (int i = 0; i < histObjList.size(); i++) {
										VaavudDatabase.getInstance(context.getApplicationContext()).insertMeasurementSession(histObjList.get(i));
								}
						}
				} else {
						if (context != null && Device.getInstance(context.getApplicationContext()).isMixpanelEnabled()) {
								MixpanelAPI.getInstance(context, MIXPANEL_TOKEN).track("Empty History Screen", null);
						}
				}
				measurmentSessions = getMeasurementsFromDB();
				JSONObject props = new JSONObject();
				try {
						props.put("Measurements", measurmentSessions.size());
				} catch (JSONException e) {
						e.printStackTrace();
				}
				if (progress != null && progress.isShowing()) progress.dismiss();
				if (context != null && Device.getInstance(context.getApplicationContext()).isMixpanelEnabled()) {
						MixpanelAPI.getInstance(context.getApplicationContext(), MIXPANEL_TOKEN).registerSuperProperties(props);
				}
				MixpanelUtil.updateMeasurementProperties(context.getApplicationContext());


		}

		private class HistoryArrayAdapter extends ArrayAdapter<MeasurementSession> {

				private final Context context;
				private final List<MeasurementSession> values;

				private HashMap<Integer, Boolean> mSelection = new HashMap<Integer, Boolean>();

				public HistoryArrayAdapter(Context context, List<MeasurementSession> values) {
						super(context, R.layout.listview_row_history, values);
						this.context = context;
						this.values = values;
				}

				public void setNewSelection(int position, boolean value) {
						//			Log.d(TAG,"Set New Selection: "+position);
						mSelection.put(position, value);
						notifyDataSetChanged();
				}

				public boolean isPositionChecked(int position) {
						Boolean result = mSelection.get(position);
						return result == null ? false : result;
				}

				public Set<Integer> getCurrentCheckedPosition() {
						return mSelection.keySet();
				}

				public void removeSelection(int position) {
						mSelection.remove(position);
						notifyDataSetChanged();
				}

				public void clearSelection() {
						mSelection = new HashMap<Integer, Boolean>();
						notifyDataSetChanged();
				}

				@Override
				public View getView(int position, View convertView, ViewGroup parent) {
						MeasurementSession measurement = values.get(position);
						LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

						View rowView = inflater.inflate(R.layout.listview_row_history, parent, false);

						historyMapThumbnailImageView = (NetworkImageView) rowView.findViewById(R.id.history_mapThumbnailImageView);
						maxSpeed = (TextView) rowView.findViewById(R.id.history_maxSpeedTextView);
						averageSpeed = (TextView) rowView.findViewById(R.id.history_averageTextView);
						startTime = (TextView) rowView.findViewById(R.id.history_timeTextView);
						speedUnit = (TextView) rowView.findViewById(R.id.history_unitTextView);
						dateView = (LinearLayout) rowView.findViewById(R.id.history_dateView);
						windText = (TextView) rowView.findViewById(R.id.history_windTextView);
						windArrowView = (ImageView) rowView.findViewById(R.id.history_windArrowView);

						historyMapThumbnailImageView.setErrorImageResId(R.drawable.no_connection);
						historyMapThumbnailImageView.setDefaultImageResId(R.drawable.loading);

						maxSpeed.setTypeface(futuraMediumTypeface);
						averageSpeed.setTypeface(futuraMediumTypeface);
						startTime.setTypeface(futuraMediumTypeface);
						speedUnit.setTypeface(futuraMediumTypeface);
						windText.setTypeface(futuraMediumTypeface);

						int height = historyMapThumbnailImageView.getLayoutParams().height;

						String iconUrl = "http://vaavud.com/appgfx/SmallWindMarker.png";
						String mapUrl = "http://maps.google.com/maps/api/staticmap";

						if (measurement.getPosition() != null) {
								String markers = "icon:" + iconUrl + "|shadow:false|" + measurement.getPosition().getLatitude() + "," + measurement.getPosition().getLongitude();

								try {
										markers = URLEncoder.encode(markers, "utf-8");
								} catch (UnsupportedEncodingException e) {
										// shouldn't happen
								}
								mapUrl += "?markers=" + markers + "&zoom=15&size=" + height + "x" + height + "&sensor=true";

								historyMapThumbnailImageView.setImageUrl(mapUrl, imageLoader);
						} else {
								historyMapThumbnailImageView.setDefaultImageResId(R.drawable.no_location);
						}

						maxSpeed.setText(unit.format(measurement.getWindSpeedMax()));
						averageSpeed.setText(unit.format(measurement.getWindSpeedAvg()));
						startTime.setText(FormatUtil.formatHourMinuteDate(measurement.getStartTime()));
						speedUnit.setText(unit.getDisplayName(context.getApplicationContext()));
						if (measurement.getWindDirection() != null) {
								windText.setText(directionUnit.format(measurement.getWindDirection()));
								windArrowView.setImageDrawable(getResources().getDrawable(R.drawable.wind_arrow));
								windArrowView.setRotation(measurement.getWindDirection());
						} else {
								windText.setText("-");
								windArrowView.setImageDrawable(null);
						}



						if (isSplitterNeeded(position)) {
								splitterDay = FormatUtil.formatDayDate(measurement.getStartTime());
								splitterMonth = FormatUtil.formatMonthDate(measurement.getStartTime()).toUpperCase();
								View dateRow = inflater.inflate(R.layout.splitter_day_history, parent, false);
								dayText = (TextView) dateRow.findViewById(R.id.history_dayText);
								monthText = (TextView) dateRow.findViewById(R.id.history_monthText);
								dayText.setText(splitterDay);
								monthText.setText(splitterMonth);
								dayText.setTypeface(futuraMediumTypeface);
								monthText.setTypeface(futuraMediumTypeface);
								dateView.addView(dateRow);
						}

						if (mSelection.get(position) != null) {
								rowView.setBackgroundColor(getResources().getColor(R.color.blue_20));// this is a selected position so make it red
						}
						return rowView;

				}

				private boolean isSplitterNeeded(int position) {
						if (position == 0) return true;
						else if (position < values.size() - 1) {
								if (FormatUtil.formatweekDayDate(values.get(position).getStartTime()).compareTo(FormatUtil.formatweekDayDate(values.get(position - 1).getStartTime())) != 0) {
										return true;
								}
						} else if (position == values.size() - 1) {
								if (FormatUtil.formatweekDayDate(values.get(position).getStartTime()).compareTo(FormatUtil.formatweekDayDate(values.get(position - 1).getStartTime())) != 0) {
										return true;
								}
						}
						return false;
				}

		}

}
