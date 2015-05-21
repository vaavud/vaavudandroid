package com.vaavud.android.ui.history;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Typeface;

import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.Fragment;
import android.support.v7.view.ActionMode;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Gravity;
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
import android.widget.LinearLayout.LayoutParams;
import android.widget.ListView;
import android.widget.TextView;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.NetworkImageView;
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
import com.vaavud.android.ui.tour.TourActivity;
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

public class HistoryFragment extends Fragment implements BackPressedListener,SelectedListener,HistoryMeasurementsResponseListener{

	private static final String TAG = "HISTORY_FRAGMENT";

	private SelectorListener mCallback;

	private static final long GRACE_TIME_BETWEEN_READ_MEASUREMENTS = 11*1000L;

	private static int DISK_IMAGECACHE_SIZE = 1024*1024*10;
	private static CompressFormat DISK_IMAGECACHE_COMPRESS_FORMAT = CompressFormat.PNG;
	private static int DISK_IMAGECACHE_QUALITY = 100;


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
	public void onCreate(Bundle savedInstanceState) {
		//				Log.i(TAG, "onCreate, savedInstanceState" + (savedInstanceState == null ? "=null" : "!=null"));

		super.onCreate(savedInstanceState);
		if (measurmentSessions == null){
			measurmentSessions = getMeasurementsFromDB();
		}
		unit = Device.getInstance(getActivity()).getWindSpeedUnit();
		directionUnit = Device.getInstance(getActivity()).getWindDirectionUnit();
		futuraMediumTypeface = Typeface.createFromAsset(getActivity().getAssets(), "futuraMedium.ttf");
//		mActionModeAvailable = Build.VERSION.SDK_INT > Build.VERSION_CODES.GINGERBREAD_MR1;

	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		//		Log.i(TAG, "onCreateView, savedInstanceState" + (savedInstanceState == null ? "=null" : "!=null"));
		if (progress != null && progress.isShowing()) return new LinearLayout(getActivity());
		if (measurmentSessions.size() > 0 ){
			view = inflater.inflate(R.layout.fragment_history, container, false);
			historyListView = (ListView) view.findViewById(R.id.history_ListView);
			historyListView.setItemsCanFocus(true);
			historyAdapter = new HistoryArrayAdapter(getActivity(), measurmentSessions);
			historyListView.setAdapter(historyAdapter);historyListView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
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
								((MainActivity)getActivity()).setActionMode(mode);
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
																	VaavudDatabase.getInstance(getActivity()).deleteMeasurementSession(removeList.get(i));
																	getUploadManager().deleteMeasurement(removeList.get(i));
																	//MixPanel
																	JSONObject props = new JSONObject();
																	try {
																			props.put("Measurement Speed", measurmentSessions.get(measurmentSessions.indexOf(removeList.get(i))).getWindSpeedAvg());
																	} catch (JSONException e) {
																			e.printStackTrace();
																	}
																	if (getActivity() != null && Device.getInstance(getActivity()).isMixpanelEnabled()) {
																			MixpanelAPI.getInstance(getActivity(), MIXPANEL_TOKEN).track("Delete Measurement", props);
																	}
																	measurmentSessions.remove(removeList.get(i));
															}
															historyAdapter.clearSelection();
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
					DISK_IMAGECACHE_SIZE, DISK_IMAGECACHE_COMPRESS_FORMAT, DISK_IMAGECACHE_QUALITY, CacheType.DISK, getRequestQueue());

			imageLoader = imageCacheManager.getImageLoader();
		}
		else{
			view = createArrowLayout(new LinearLayout(getActivity())); 					
		}
		MixpanelUtil.updateMeasurementProperties(getActivity());
		return view;
	}

	private View createArrowLayout(LinearLayout view){
		LinearLayout.LayoutParams params = new LayoutParams(LayoutParams.MATCH_PARENT, 0);
		params.weight=1;
		view.setOrientation(LinearLayout.VERTICAL);
		view.setLayoutParams(params);
		CustomDrawableView arrowView = new CustomDrawableView(getActivity());
		LayoutParams arrowParams = new LayoutParams(LayoutParams.MATCH_PARENT, 0);
		arrowParams.weight=(float) 0.7;
		arrowParams.width= LayoutParams.MATCH_PARENT;
		arrowView.setLayoutParams(arrowParams);
		TextView text = new TextView(getActivity());
		text.setText(getActivity().getResources().getString(R.string.history_no_measurements));
		text.setTextColor(getActivity().getResources().getColor(R.color.blue));
		text.setTextSize(24);
		text.setLines(2);

		TextView subtext = new TextView(getActivity());
		subtext.setText(getActivity().getResources().getString(R.string.history_go_to_measure));
		subtext.setTextColor(Color.BLACK);
		subtext.setTextSize(16);

		LayoutParams textParams = new LayoutParams(LayoutParams.MATCH_PARENT, 0);
		textParams.weight=(float) 0.15;
		textParams.width= LayoutParams.WRAP_CONTENT;
		textParams.topMargin=40;
		textParams.leftMargin=20;
		textParams.rightMargin=20;
		textParams.gravity=Gravity.CENTER_HORIZONTAL;
		text.setLayoutParams(textParams);


		LayoutParams subtextParams = new LayoutParams(LayoutParams.MATCH_PARENT, 0);
		subtextParams.weight=(float) 0.15;
		subtextParams.width= LayoutParams.WRAP_CONTENT;
		subtextParams.topMargin=-40;
		subtextParams.leftMargin=20;
		subtextParams.rightMargin=20;
		subtextParams.gravity=Gravity.CENTER_HORIZONTAL;

		text.setLayoutParams(textParams);
		subtext.setLayoutParams(subtextParams);

		view.addView(arrowView, 0);
		view.addView(text, 1);
		view.addView(subtext, 2);

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
	public boolean onContextItemSelected(MenuItem item) {
		int position;
		//		Log.d(TAG,"MENU");
		AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
		position = (int) info.id;
		switch (item.getItemId()) {
		case R.id.action_delete:
			VaavudDatabase.getInstance(getActivity()).deleteMeasurementSession(measurmentSessions.get(position));
			getUploadManager().deleteMeasurement(measurmentSessions.get(position));
			measurmentSessions.remove(position);
			historyAdapter.notifyDataSetChanged();
			return true;
		}
		return false;
	}

//	@Override
//	public void onAttach(Activity activity) {
//		super.onAttach(activity);
////		try {
////			mCallback = (SelectorListener) activity;
////		} catch (ClassCastException e) {
////			throw new ClassCastException(activity.toString() + " must implement SelectorListener");
////		}
//		progress = ((MainActivity)activity).getProgressDialog();
//		//		Log.i(TAG, "onAttach");
//	}

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
		if(isCurrentTab()){
			if(measurmentSessions.size()>0){
				readHistoryMeasurements( true, false, true, measurmentSessions.get(0).getEndTime(), getUUIDHash());
			}else{
				readHistoryMeasurements( true, true, true, new Date(0L), getUUIDHash());
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
		if (historyAdapter!=null){
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


	private List<MeasurementSession> getMeasurementsFromDB(){
		//Log.i(TAG, "getMeasurementsFromDB");
		List<MeasurementSession> measurementsList = VaavudDatabase.getInstance(getActivity()).getMeasurementSessions();
		Collections.sort(measurementsList, startTimeComparatorDescending);
		return measurementsList;
	}

	private RequestQueue getRequestQueue() {
		MainActivity activity = (MainActivity) getActivity();
		if (activity != null) {
			return activity.getRequestQueue();
		}
		return null;
	}

	private void readHistoryMeasurements(boolean forceUpdate, boolean showActivityIndicator, boolean giveNetworkErrorFeedback, Date lastHistoryReadMeasurement, String hash) {

		if (!forceUpdate && lastHistoryReadMeasurement != null) {
			return;
		}
		getUploadManager().triggerReadHistoryMeasurements(lastHistoryReadMeasurement, hash, this);
	}

	private UploadManager getUploadManager() {
		MainActivity activity = (MainActivity) getActivity();
		if (activity != null) {
			return activity.getUploadManager();
		}
		return null;
	}

	private String getUUIDHash(){
		String hashedUUIDs;

		StringBuilder sb1 = new StringBuilder(measurmentSessions.size() * (36 + 10));
		if(measurmentSessions.size()>0){
			for (int i=measurmentSessions.size()-1;i>=0;i--){
				String endTimeSecondsString = Long.toString((long) Math.ceil(((double) measurmentSessions.get(i).getEndTime().getTime()) / 1000D));
				sb1.append(measurmentSessions.get(i).getUuid());
				sb1.append(endTimeSecondsString);
			}

			hashedUUIDs = UUIDUtil.md5Hash(sb1.toString().toUpperCase(Locale.US));

			return hashedUUIDs;
		}else{
			return null;
		}
	}

	@Override
	public void onSelected() {
		//		Log.d(TAG,"On Selected");
		if (getActivity()!=null && Device.getInstance(getActivity()).isMixpanelEnabled()){
			MixpanelAPI.getInstance(getActivity(),MIXPANEL_TOKEN).track("History Screen",null);
		}
	}

	@Override
	public void measurementsLoadingFailed() {
		if (getActivity()!=null && Device.getInstance(getActivity()).isMixpanelEnabled()){
			MixpanelAPI.getInstance(getActivity(),MIXPANEL_TOKEN).track("Measurements Loading Failed", null);
		//		Log.e(TAG,"Measurements Loading Failed");
		}
	}

	@Override
	public void measurementsReceived(ArrayList<MeasurementSession> histObjList) {
		lastReadMeasurements = new Date();
		Iterator<MeasurementSession> itr = measurmentSessions.iterator();
		if (histObjList!=null){
			while(itr.hasNext()){
				MeasurementSession current = itr.next();
				for(int i=0;i<histObjList.size();i++){
					if (current.equals(histObjList.get(i))){
						if (current.getEndTime().getTime() < histObjList.get(i).getEndTime().getTime()){
							current.setEndTime(histObjList.get(i).getEndTime());
							current.setPoints(histObjList.get(i).getPoints());
							current.setWindSpeedAvg(histObjList.get(i).getWindSpeedAvg());
							current.setWindSpeedMax(histObjList.get(i).getWindSpeedMax());
							current.setWindDirection(histObjList.get(i).getWindDirection());
							VaavudDatabase.getInstance(getActivity()).updateCompleteMeasurementSession(current);
						}
						histObjList.remove(i);
						break;
					}
				}
			}
			if (histObjList.size()>0){
				for (int i = 0; i < histObjList.size(); i++) {
					VaavudDatabase.getInstance(getActivity()).insertMeasurementSession(histObjList.get(i));
				}
			}
		}else{
			if (getActivity()!=null && Device.getInstance(getActivity()).isMixpanelEnabled()){
				MixpanelAPI.getInstance(getActivity(),MIXPANEL_TOKEN).track("Empty History Screen", null);
			}
		}
		measurmentSessions = getMeasurementsFromDB();
		JSONObject props = new JSONObject();
		try {
			props.put("Measurements", measurmentSessions.size());
		} catch (JSONException e) {
			e.printStackTrace();
		}
		if (getActivity()!=null){
			if (progress!=null && progress.isShowing()) progress.dismiss();
			if (Device.getInstance(getActivity()).isMixpanelEnabled()){
				MixpanelAPI.getInstance(getActivity(),MIXPANEL_TOKEN).registerSuperProperties(props);
			}
			MixpanelUtil.updateMeasurementProperties(getActivity());
			if (((MainActivity)getActivity()).getLogin() || ((MainActivity)getActivity()).getSignUp()){
				Intent i = new Intent(getActivity(),TourActivity.class);
				i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
				startActivity(i);
				getActivity().finish();
			}
		}
//		if (isCurrentTab()) onMenuOptionSelected(2);

	}


//	@Override
//	public void onMenuOptionSelected(int position) {
//		mCallback.onMenuOptionSelected(position);
//	}

	private class CustomDrawableView extends View {

		public CustomDrawableView(Context context) {
			super(context);
		}

		protected void onDraw(Canvas canvas) {

			Paint circlePaint = new Paint();

			int left = canvas.getWidth()/6;
			int top = canvas.getHeight()/15-canvas.getHeight();
			int right = canvas.getWidth()-canvas.getWidth()/6;
			int bottom = canvas.getHeight();
			//to draw an arrow, just lines needed, so style is only STROKE
			circlePaint.setStyle(Paint.Style.STROKE);
			circlePaint.setPathEffect(new DashPathEffect(new float[] {20,15}, 10));

			circlePaint.setStrokeWidth(5.0F);
			circlePaint.setColor(getActivity().getResources().getColor(R.color.blue));
			//create a path to draw on
			Path arrowPath = new Path();

			//create an invisible oval. the oval is for "behind the scenes" ,to set the path´
			//area. Imagine this is an egg behind your circles. the circles are in the middle of this egg
			final RectF arrowOval = new RectF();
			arrowOval.set(left,top,right,bottom);

			//add the oval to path
			arrowPath.addArc(arrowOval,-180,-90);

			canvas.drawPath(arrowPath, circlePaint);
			arrowPath.moveTo(canvas.getWidth()/6,canvas.getHeight()/30); //move to the center of first circle
			arrowPath.lineTo(canvas.getWidth()/6+canvas.getHeight()/20,canvas.getHeight()/10+canvas.getHeight()/20);
			arrowPath.moveTo(canvas.getWidth()/6,canvas.getHeight()/30); //move to the center of first circle
			arrowPath.lineTo(canvas.getWidth()/6-canvas.getHeight()/20,canvas.getHeight()/10+canvas.getHeight()/20);
			canvas.drawPath(arrowPath,circlePaint);

		}
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
			int height = historyMapThumbnailImageView.getLayoutParams().height;

			String iconUrl = "http://vaavud.com/appgfx/SmallWindMarker.png";
			String mapUrl = "http://maps.google.com/maps/api/staticmap";

			if (measurement.getPosition()!=null){
				String markers = "icon:" + iconUrl + "|shadow:false|" + measurement.getPosition().getLatitude() + "," + measurement.getPosition().getLongitude();

				try {
					markers = URLEncoder.encode(markers, "utf-8");
				}
				catch (UnsupportedEncodingException e) {
					// shouldn't happen
				}
				mapUrl += "?markers=" + markers + "&zoom=15&size=" + height + "x" + height + "&sensor=true";

				historyMapThumbnailImageView.setImageUrl(mapUrl, imageLoader);
			}else{
				historyMapThumbnailImageView.setDefaultImageResId(R.drawable.no_location);
			}

			maxSpeed.setText(unit.format(measurement.getWindSpeedMax()));
			averageSpeed.setText(unit.format(measurement.getWindSpeedAvg()));
			startTime.setText(FormatUtil.formatHourMinuteDate(measurement.getStartTime()));
			speedUnit.setText(unit.getDisplayName(getActivity()));
			if (measurement.getWindDirection()!=null){
				windText.setText(directionUnit.format(measurement.getWindDirection()));
				windArrowView.setImageDrawable(getResources().getDrawable(R.drawable.wind_arrow));
				windArrowView.setRotation(measurement.getWindDirection());
			}else{
				windText.setText("-");
				windArrowView.setImageDrawable(null);
			}

			maxSpeed.setTypeface(futuraMediumTypeface);
			averageSpeed.setTypeface(futuraMediumTypeface);
			startTime.setTypeface(futuraMediumTypeface);
			speedUnit.setTypeface(futuraMediumTypeface);
			windText.setTypeface(futuraMediumTypeface);

			if (isSplitterNeeded(position)){
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
			if(position==0) return true;
			else if(position<values.size()-1){
				if (FormatUtil.formatweekDayDate(values.get(position).getStartTime()).compareTo(FormatUtil.formatweekDayDate(values.get(position-1).getStartTime()))!=0){
					return true;
				}
			}
			else if(position==values.size()-1){
				if (FormatUtil.formatweekDayDate(values.get(position).getStartTime()).compareTo(FormatUtil.formatweekDayDate(values.get(position-1).getStartTime()))!=0){
					return true;
				}
			}
			return false;
		}

	}

}
