package com.vaavud.android.ui.measure;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint.Align;
import android.graphics.PorterDuff;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Environment;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.mixpanel.android.mpmetrics.MixpanelAPI;
import com.vaavud.android.R;
import com.vaavud.android.measure.MeasureStatus;
import com.vaavud.android.measure.MeasurementController;
import com.vaavud.android.measure.MeasurementReceiver;
import com.vaavud.android.measure.SleipnirCoreController;
import com.vaavud.android.measure.sensor.LocationUpdateManager;
import com.vaavud.android.model.entity.Device;
import com.vaavud.android.model.entity.DirectionUnit;
import com.vaavud.android.model.entity.LatLng;
import com.vaavud.android.model.entity.MeasurementSession;
import com.vaavud.android.model.entity.SpeedUnit;
import com.vaavud.android.ui.MainActivity;
import com.vaavud.android.ui.SelectedListener;

import org.achartengine.ChartFactory;
import org.achartengine.GraphicalView;
import org.achartengine.model.XYMultipleSeriesDataset;
import org.achartengine.renderer.XYMultipleSeriesRenderer;
import org.achartengine.renderer.XYSeriesRenderer;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

// TODO: plot what has been going on while fragment wasn't visible
// TODO: scroll view to last x-position
public class MeasureFragment extends Fragment implements MeasurementReceiver, SelectedListener, SharedPreferences.OnSharedPreferenceChangeListener {

		private static final double YAXISSTARTMAXVALUE = 6.2D;
		private static final double XAXISSTARTMAXVALUE = 10D;
		private static final int MAX_TIME_BAR = 30000; //in msec
		private static final int COUNT_DOWN_TICK = 1000; //in msec


		private static final int TEXT_SIZE_XXHDPI = 32;
		private static final int TEXT_SIZE_XHDPI = 24;
		private static final int TEXT_SIZE_HDPI = 20;
		private static final int TEXT_SIZE_MDPI = 18;
		private static final int TEXT_SIZE_LDPI = 13;


		private View view;
		private View measurementView;
		private TextView meanText;
		private TextView actualText;
		private TextView maxText;
		private TextView directionText;
		private TextView informationText;
		private ImageView arrowView;
		private Button unitButton;

		private LinearLayout startButtonLayout;
		private Button startButton;
		private Button shareButton;
		private ProgressBar progressBar;

		private int progressStatus = 0;
		private CountDownTimer countDown;

		private Float currentMeanValueMS;
		private Float currentActualValueMS;
		private Float currentMaxValueMS;
		private LatLng currentPosition;
		private Float currentDirection;

		private GraphicalView mChartView;

		private XYSeriesUnitSupport xySeries;
		private XYSeriesUnitSupport averageSeries;
		private XYMultipleSeriesDataset dataset;
		private XYMultipleSeriesRenderer renderer;

		private double yAxisMaxValue;
		private float xAxisTime;

		private SpeedUnit currentUnit;
		private DirectionUnit currentDirectionUnit;
		private boolean measurementStarted=false;
		private boolean UIupdate = false;
		private Context context;

		/*DEBUG TAG*/
		private static final String TAG = "Vaavud:MeasureFrag";

		private static final String MIXPANEL_TOKEN = "757f6311d315f94cdfc8d16fb4d973c0";
		private boolean shareButtonEnabled=false;

		public MeasureFragment() {
				super();
				//Log.i("MeasureFragment", "constructor");
		}

		@Override
		public void onAttach(Activity activity) {
				super.onAttach(activity);
				context = activity;
		}

		@Override
		public void onCreate(Bundle savedInstanceState) {
				super.onCreate(savedInstanceState);
//				Log.d(TAG, "onCreate, savedInstanceState" + (savedInstanceState == null ? "=null" : "!=null"));
		}

		@Override
		public View onCreateView(LayoutInflater inflater, final ViewGroup container, Bundle savedInstanceState) {
//				Log.d(TAG, "onCreateView, savedInstanceState" + (savedInstanceState == null ? "=null" : "!=null"));

				// create view
				view = inflater.inflate(R.layout.fragment_measure, container, false);
				measurementView = view.findViewById(R.id.measurement_layout);

				currentUnit = Device.getInstance(context.getApplicationContext()).getWindSpeedUnit();
				currentDirectionUnit = Device.getInstance(context.getApplicationContext()).getWindDirectionUnit();

				((TextView) view.findViewById(R.id.meanLabeltext)).setText(getResources().getString(R.string.heading_average).toUpperCase());
				((TextView) view.findViewById(R.id.actualLabelText)).setText(getResources().getString(R.string.heading_current).toUpperCase());
				((TextView) view.findViewById(R.id.maxLabelText)).setText(getResources().getString(R.string.heading_max).toUpperCase());
				((TextView) view.findViewById(R.id.unitLabelText)).setText(getResources().getString(R.string.heading_unit).toUpperCase());
				((TextView) view.findViewById(R.id.directionLabelText)).setText(getResources().getString(R.string.direction_unit).toUpperCase());


				informationText = (TextView) view.findViewById(R.id.informationText);
				informationText.setVisibility(View.INVISIBLE);
				meanText = (TextView) view.findViewById(R.id.meanText);
				actualText = (TextView) view.findViewById(R.id.actualText);
				maxText = (TextView) view.findViewById(R.id.maxText);
				directionText = (TextView) view.findViewById(R.id.directionText);
				arrowView = (ImageView) view.findViewById(R.id.arrowView);

				// crete progress bar
				progressBar = (ProgressBar) view.findViewById(R.id.progressBar);
				clearProgressBar();

				// create buttons


				startButtonLayout = (LinearLayout) view.findViewById(R.id.startButtonLayout);
				startButton = (Button) view.findViewById(R.id.startButton);
				startButton.setText(getResources().getString(R.string.button_start));
				startButton.setBackgroundResource(R.drawable.button_rounded_blue);

				shareButton = new Button(context);
				LinearLayout.LayoutParams paramsShare = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 0.60f);
				paramsShare.setMargins(10, 0, 10, 0);
				shareButton.setLayoutParams(paramsShare);
				shareButton.setBackgroundResource(R.drawable.button_rounded_blue);
				shareButton.setText(getResources().getString(R.string.share_to_facebook_title).toUpperCase());
				shareButton.setTextColor(getResources().getColor(R.color.white));
				shareButton.setTextSize(23);

				shareButton.setOnClickListener(new View.OnClickListener() {
						@Override
						public void onClick(View v) {
								new ScreenshotGenerator().execute();
								MixpanelAPI.getInstance(context.getApplicationContext(), MIXPANEL_TOKEN).track("Start Share Dialog", null);
						}
				});
				shareButtonEnabled = false;


				startButton.setOnClickListener(new View.OnClickListener() {
						@Override
						public void onClick(View v) {
								if (measurementStarted) {
										//Stop Measurement
										stop();
								} else {
										//Start Measurement
										start();
										if (context != null && Device.getInstance(context.getApplicationContext()).isMixpanelEnabled()) {
												//MixPanel
												MixpanelAPI.getInstance(context.getApplicationContext(), MIXPANEL_TOKEN).track("Start Measurement", null);
										}
								}
						}
				});

				unitButton = (Button) view.findViewById(R.id.unitButton);
				unitButton.setText(currentUnit.getDisplayName(getActivity()));
				unitButton.setOnClickListener(new View.OnClickListener() {
						@Override
						public void onClick(View v) {
								changeUnit();
						}
				});


				// create graph

				dataset = new XYMultipleSeriesDataset();

				renderer = new XYMultipleSeriesRenderer();
				renderer.setXLabelsColor(Color.DKGRAY);
				renderer.setYLabelsColor(0, Color.DKGRAY);
				renderer.setFitLegend(true);
				renderer.setPanEnabled(true, false);
				renderer.setZoomEnabled(true, false);
				renderer.setZoomButtonsVisible(false);
				renderer.setBarSpacing(10);
//		renderer.setMarginsColor(Color.argb(0x00, 0x01, 0x01, 0x01)); // Transparent (beacuse of bug).

				renderer.setMarginsColor(getResources().getColor(R.color.lightgray)); // same grey as view background.

				renderer.setShowLegend(false);
				renderer.setYLabelsAlign(Align.RIGHT);

				renderer.setXAxisMin(0);
				renderer.setYAxisMin(0);
				renderer.setXAxisMax(XAXISSTARTMAXVALUE);
				yAxisMaxValue = (savedInstanceState != null && savedInstanceState.containsKey("yAxisMaxValue")) ? savedInstanceState.getDouble("yAxisMaxValue") : currentUnit.toDisplayValue(YAXISSTARTMAXVALUE);
				renderer.setYAxisMax(yAxisMaxValue);

				XYSeriesRenderer rendererSeries = new XYSeriesRenderer();
				renderer.addSeriesRenderer(rendererSeries);
				rendererSeries.setColor(view.getResources().getColor(R.color.blue));

				XYSeriesRenderer averageSeriesRenderer = new XYSeriesRenderer();
				renderer.addSeriesRenderer(averageSeriesRenderer);
				averageSeriesRenderer.setColor(view.getResources().getColor(R.color.red));

				switch (view.getContext().getResources().getDisplayMetrics().densityDpi) {
						case DisplayMetrics.DENSITY_XXHIGH: /* 480 */
								renderer.setMargins(new int[]{10, 50, 0, 20});
								renderer.setLabelsTextSize(TEXT_SIZE_XXHDPI);
								renderer.setYLabelsPadding(TEXT_SIZE_XXHDPI / 2);
								renderer.setYLabelsVerticalPadding(-TEXT_SIZE_XXHDPI / 3);
								rendererSeries.setLineWidth(10f);
								averageSeriesRenderer.setLineWidth(10f);
								break;

						case DisplayMetrics.DENSITY_XHIGH: /* 320 */
								renderer.setMargins(new int[]{7, 42, 0, 14});
								renderer.setLabelsTextSize(TEXT_SIZE_XHDPI);
								renderer.setYLabelsPadding(TEXT_SIZE_XHDPI / 2);
								renderer.setYLabelsVerticalPadding(-TEXT_SIZE_XHDPI / 3);
								rendererSeries.setLineWidth(7f);
								averageSeriesRenderer.setLineWidth(7f);
								break;

						case DisplayMetrics.DENSITY_HIGH: /* 240 */
								renderer.setMargins(new int[]{5, 35, 0, 10});
								renderer.setLabelsTextSize(TEXT_SIZE_HDPI);
								renderer.setYLabelsPadding(TEXT_SIZE_HDPI / 2);
								renderer.setYLabelsVerticalPadding(-TEXT_SIZE_HDPI / 3);
								rendererSeries.setLineWidth(5f);
								averageSeriesRenderer.setLineWidth(5f);
								break;

						case DisplayMetrics.DENSITY_MEDIUM: /* 160 */
								renderer.setMargins(new int[]{3, 25, 0, 6});
								renderer.setLabelsTextSize(TEXT_SIZE_MDPI);
								renderer.setYLabelsPadding(TEXT_SIZE_MDPI / 2);
								renderer.setYLabelsVerticalPadding(-TEXT_SIZE_MDPI / 3);
								rendererSeries.setLineWidth(3f);
								averageSeriesRenderer.setLineWidth(3f);
								break;

						default:
								renderer.setMargins(new int[]{3, 15, 0, 6});
								renderer.setLabelsTextSize(TEXT_SIZE_LDPI);
								renderer.setYLabelsPadding(TEXT_SIZE_LDPI / 2);
								renderer.setYLabelsVerticalPadding(-TEXT_SIZE_LDPI / 3);
								rendererSeries.setLineWidth(3f);
								averageSeriesRenderer.setLineWidth(3f);
								break;
				}

				if (!((MainActivity) context).hasCompass()) {
						arrowView.setVisibility(View.VISIBLE);
						directionText.setText(getResources().getString(R.string.no_compass_measurements).toUpperCase());
						directionText.setTextSize(9.6f);
						arrowView.setImageDrawable(getResources().getDrawable(R.drawable.no_compass));
						arrowView.setScaleType(ScaleType.CENTER);
				}

//				if (savedInstanceState != null) {
//						xySeries = savedInstanceState.containsKey("xySeries") ? (XYSeriesUnitSupport) savedInstanceState.getSerializable("xySeries") : new XYSeriesUnitSupport("actual", currentUnit);
//						averageSeries = savedInstanceState.containsKey("averageSeries") ? (XYSeriesUnitSupport) savedInstanceState.getSerializable("averageSeries") : new XYSeriesUnitSupport("mean", currentUnit);
//				} else if (xySeries == null) {
				xySeries = new XYSeriesUnitSupport("actual", currentUnit);
				averageSeries = new XYSeriesUnitSupport("mean", currentUnit);
//				}

				dataset.addSeries(xySeries);
				dataset.addSeries(averageSeries);

				//Log.i("MeasureFragment", "xySeries.count=" + xySeries.getItemCount() + ", averageSeries.count=" + averageSeries.getItemCount());

				LinearLayout layout = (LinearLayout) view.findViewById(R.id.chart);
				mChartView = ChartFactory.getCubeLineChartView(view.getContext(), dataset, renderer, 0.3f);
				layout.addView(mChartView);
				mChartView.setBackgroundColor(getResources().getColor(R.color.lightgray));
				mChartView.repaint();

				return view;
		}

		@Override
		public void onActivityCreated(Bundle savedInstanceState) {
				super.onActivityCreated(savedInstanceState);
//				Log.d(TAG, "onActivityCreated, savedInstanceState" + (savedInstanceState == null ? "=null" : "!=null"));
		}

		@Override
		public void onViewStateRestored(Bundle savedInstanceState) {
				super.onViewStateRestored(savedInstanceState);
//				Log.d(TAG, "onViewStateRestored, savedInstanceState" + (savedInstanceState == null ? "=null" : "!=null"));
		}

		@Override
		public void onStart() {
				super.onStart();
//				Log.i(TAG, "onStart");
		}

		@Override
		public void onResume() {
				super.onResume();
				((MainActivity) context).getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
//				Log.d(TAG, "onResume");
				// restore state of start/stop button and possibly register for receiving measurement updates if measuring
				if (getMeasurementController().isMeasuring()) {
						getMeasurementController().addMeasurementReceiver(this);
						startButton.setText(getResources().getString(R.string.button_stop));
						startButton.setTextColor(getResources().getColor(R.color.white));
						startButton.setBackgroundResource(R.drawable.button_rounded_red);
						measurementStarted = true;
				} else {
						startButton.setText(getResources().getString(R.string.button_start));
						if (shareButtonEnabled) {
								startButton.setBackgroundResource(R.drawable.button_rounded_white);
								startButton.setTextColor(getResources().getColor(R.color.blue));
						}else{
								if (shareButton!=null) {
										startButtonLayout.removeView(shareButton);
								}
								startButton.setBackgroundResource(R.drawable.button_rounded_blue);
								startButton.setTextColor(getResources().getColor(R.color.white));
						}
						measurementStarted = false;
				}
		}

		@Override
		public void onPause() {
				super.onPause();
//				Log.d(TAG, "onPause");
		}

		@Override
		public void onStop() {
				stop();
				((MainActivity) context).getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
//				Log.i(TAG, "onStop");
				super.onStop();
		}

		@Override
		public void onDestroyView() {
//				Log.i(TAG, "onDestroyView");
				super.onDestroyView();
		}

		@Override
		public void onDestroy() {
				super.onDestroy();
//				Log.i(TAG, "onDestroy");
				progressBar = null;
				UIupdate = false;
				measurementStarted = false;
				meanText = null;
				actualText = null;
				maxText = null;
				startButton = null;
				unitButton = null;
				mChartView = null;
				dataset = null;
				renderer = null;
				view = null;
		}

		@Override
		public void onDetach() {
				super.onDetach();
				//Log.i("MeasureFragment", "onDetach");
		}


		private MeasurementController getMeasurementController() {
				MainActivity activity = (MainActivity) getActivity();
				if (activity != null) {
						return activity.getMeasurementController();
				}
				return null;
		}

		private LocationUpdateManager getLocationController() {
				MainActivity activity = (MainActivity) getActivity();
				if (activity != null) {
						return activity.getLocationUpdateManager();
				}
				return null;
		}


		private void start() {
				if (((MainActivity)getActivity()).hasCompass()) {
						arrowView.setVisibility(View.INVISIBLE);
				}
				if (!measurementStarted) {

//			Log.d(TAG,"Start Measurement");
						if (getMeasurementController() instanceof SleipnirCoreController) {
								((SleipnirCoreController) getMeasurementController()).startController();
						}
						getMeasurementController().addMeasurementReceiver(this);
						getMeasurementController().startSession();

						clearGraphData();
						resetGraphAxis();
						startProgressBar();

						if (mChartView != null) {
								mChartView.repaint();
						}

						if (shareButton != null) {
								startButtonLayout.removeView(shareButton);
						}
						LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1.0f);
						startButton.setLayoutParams(params);
						startButton.setTextColor(getResources().getColor(R.color.white));
						startButton.setText(getResources().getString(R.string.button_stop));
						startButton.setBackgroundResource(R.drawable.button_rounded_red);
						shareButtonEnabled = false;

						informationText.setVisibility(View.VISIBLE);

						currentDirection = null;
						currentActualValueMS = null;
						currentMeanValueMS = null;
						currentMaxValueMS = null;
						updateWindspeedTextValues();

						measurementStarted = true;
				}
		}

		private void stop() {
//				Log.d(TAG, "Stop Measurement: MeasurementStarted "+measurementStarted);
				if (measurementStarted) {
						measurementStarted = false;
						if (getMeasurementController().isMeasuring()) {
								getMeasurementController().stopSession();

						}
////						getMeasurementController().removeMeasurementReceiver(this);
						if (getMeasurementController() instanceof SleipnirCoreController) {
								getMeasurementController().stopController();
						}
						clearProgressBar();


						startButton.setBackgroundResource(R.drawable.button_rounded_white);
						startButton.setText(getResources().getString(R.string.button_start));
						startButton.setTextColor(getResources().getColor(R.color.blue));

						LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 0.40f);
						startButton.setLayoutParams(params);
						shareButton.setVisibility(View.VISIBLE);
						startButtonLayout.addView(shareButton);
						shareButtonEnabled = true;

						informationText.setVisibility(View.INVISIBLE);
				}
		}

		@Override
		public void measurementAdded(MeasurementSession session, Float time, Float currentWindSpeed, Float avgWindSpeed, Float maxWindSpeed, Float direction) {

				currentMeanValueMS = avgWindSpeed;
				currentActualValueMS = currentWindSpeed;
				currentMaxValueMS = maxWindSpeed;
				currentDirection = direction;
				currentPosition = getMeasurementController().currentSession().getPosition();

				if (UIupdate) {
						updateWindspeedTextValues();

						if (currentWindSpeed != null) {
								xySeries.add(time, currentWindSpeed);
								updateXaxis(time);
								updateYaxis(currentUnit.toDisplayValue(currentWindSpeed), false);
						}

						if (currentMeanValueMS != null) {
								averageSeries.add(time, currentMeanValueMS);
						}

						mChartView.repaint();
				}
		}

		@Override
		public void measurementFinished(MeasurementSession session) {

				if (context != null && Device.getInstance(context.getApplicationContext()).isMixpanelEnabled()) {
						//MixPanel
						JSONObject props = new JSONObject();
						try {
								props.put("Duration", (getMeasurementController().currentSession().getEndTime().getTime() - getMeasurementController().currentSession().getStartTime().getTime()) / 1000);
								if (currentMeanValueMS != null && currentMaxValueMS != null) {
										props.put("Avg Wind Speed", currentUnit.format(currentMeanValueMS));
										props.put("Max Wind Speed", currentUnit.format(currentMaxValueMS));
								}

						} catch (JSONException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
						}
						MixpanelAPI.getInstance(context.getApplicationContext(), MIXPANEL_TOKEN).track("Stop Measurement", props);
				}
				getMeasurementController().removeMeasurementReceiver(this);
//				if (measurementStarted) {
//						stop();
//				}

		}

		private void updateYaxis(Float windspeedInUnit, boolean forceSet) {
				if (forceSet || windspeedInUnit > yAxisMaxValue) {
						yAxisMaxValue = (float) (Math.ceil(windspeedInUnit) + 0.2f);
						renderer.setYAxisMax(yAxisMaxValue);
				}
		}

		private void updateXaxis(float time) {
				xAxisTime = time;

				double[] panLimits = {0d, time, 0d, yAxisMaxValue};
				renderer.setPanLimits(panLimits);
				renderer.setZoomLimits(panLimits);

				if (time > XAXISSTARTMAXVALUE) {
						renderer.setXAxisMin(time - XAXISSTARTMAXVALUE);
						renderer.setXAxisMax(time);
				}
		}

		private void updateWindspeedTextValues() {
//		Log.d(TAG,"Update Wind Speed");

				if (getMeasurementController().isMeasuring()) {
						if (currentDirection == null && ((MainActivity)getActivity()).hasCompass()){
								directionText.setText("-");
						}
						else if (currentDirection != null && ((MainActivity)getActivity()).hasCompass()) {
								arrowView.setVisibility(View.VISIBLE);
								Bitmap arrow = BitmapFactory.decodeResource(context.getResources(), R.drawable.wind_arrow);
								if (arrow == null) return;
								Matrix matrix = new Matrix();
								matrix.postRotate(currentDirection);
								directionText.setText(currentDirectionUnit.format(currentDirection));
								Bitmap rotatedBitmap = Bitmap.createBitmap(arrow, 0, 0,
												arrow.getWidth(),
												arrow.getHeight(), matrix, true);
								arrowView.setImageBitmap(rotatedBitmap);
								arrowView.setScaleType(ScaleType.CENTER);

						}
				}
				actualText.setText(currentUnit.format(currentActualValueMS));
				meanText.setText(currentUnit.format(currentMeanValueMS));
				maxText.setText(currentUnit.format(currentMaxValueMS));
		}

		private void clearGraphData() {
				xySeries.clear();
				averageSeries.clear();
		}

		private void resetGraphAxis() {
				renderer.setXAxisMin(0);
				renderer.setYAxisMin(0);
				renderer.setXAxisMax(XAXISSTARTMAXVALUE);
				yAxisMaxValue = currentUnit.toDisplayValue(YAXISSTARTMAXVALUE);
				renderer.setYAxisMax(yAxisMaxValue);
		}

		private void startProgressBar() {
				//Log.d("PROGRESSBAR","Start progress bar");
				countDown = new CountDownTimer(MAX_TIME_BAR, COUNT_DOWN_TICK) {

						@Override
						public void onTick(long millisUntilFinished) {
								progressStatus += 1; // (int)(MAX_TIME_BAR - millisUntilFinished)/1000;
								//Log.d("PROGRESS BAR", "Value: "+progressStatus);
								//Log.d("PROGRESS BAR", "Tick Value: "+millisUntilFinished);
								progressBar.setProgress(progressStatus);
						}

						@Override
						public void onFinish() {
								//Log.d("PROGRESS BAR", "On Finish Value: "+progressStatus);
								progressBar.setProgress(30);
								// TODO Auto-generated method stub
						}
				}.start();
		}

		private void restartProgressBar() {
				//Log.d("PROGRESS_BAR","Restart progress bar progressStatus= "+progressStatus);
				countDown = new CountDownTimer(MAX_TIME_BAR - (progressStatus * 1000), COUNT_DOWN_TICK) {

						@Override
						public void onTick(long millisUntilFinished) {
								progressStatus += 1; // (int)(MAX_TIME_BAR - millisUntilFinished)/1000;
								//Log.d("PROGRESS BAR", "Value: "+progressStatus);
								//Log.d("PROGRESS BAR", "Tick Value: "+millisUntilFinished);
								progressBar.setProgress(progressStatus);
						}

						@Override
						public void onFinish() {
								//Log.d("PROGRESS BAR", "On Finish Value: "+progressStatus);
								progressBar.setProgress(30);
								// TODO Auto-generated method stub
						}
				}.start();
		}

		private void clearProgressBar() {
				if (countDown != null) {
						countDown.cancel();
				}
				countDown = null;
				progressStatus = 0;
				progressBar.setProgress(progressStatus);
//				Log.d(TAG, "Clear progress bar: " + progressStatus);
		}

		private void pauseProgressBar() {
				if (countDown != null) countDown.cancel();
				countDown = null;
				//Log.d("PROGRESS_BAR","Pause progress bar");
		}

		private void changeUnit() {
				Device.getInstance(context.getApplicationContext()).setWindSpeedUnit(context.getApplicationContext(), SpeedUnit.nextUnit(currentUnit));
				updateUnit();
		}

		@Override
		public void onSelected() {
				//		super.onSelected();
				if (context != null && Device.getInstance(context.getApplicationContext()).isMixpanelEnabled()) {
						MixpanelAPI.getInstance(context.getApplicationContext(), MIXPANEL_TOKEN).track("Measure Screen", null);
				}
				//		Log.i("MeasureFragment", "onSelected");
				updateUnit();
		}

		private void updateUnit() {
				SpeedUnit unit = Device.getInstance(context.getApplicationContext()).getWindSpeedUnit();
				if (unit != currentUnit) {
						currentUnit = unit;
						xySeries.setUnit(currentUnit);
						averageSeries.setUnit(currentUnit);
						updateYaxis((float) xySeries.getMaxY(), true);
						mChartView.repaint();
						unitButton.setText(currentUnit.getDisplayName(getActivity()));
						updateWindspeedTextValues();
				}
		}

		@Override
		public void measurementStatusChanged(MeasureStatus status) {
				// TODO Auto-generated method stub
				UIupdate = false;
				informationText.setText(getResources().getString(status.getResourceId()));
				if (status.getResourceId() == MeasureStatus.NO_SIGNAL.getResourceId() & measurementStarted)
						pauseProgressBar();
				if (status.getResourceId() == MeasureStatus.NO_AUDIO_SIGNAL.getResourceId() & measurementStarted)
						pauseProgressBar();
				if (status.getResourceId() == MeasureStatus.KEEP_VERTICAL.getResourceId() & measurementStarted)
						pauseProgressBar();
				if (status.getResourceId() == MeasureStatus.MEASURING.getResourceId() & measurementStarted) {
						restartProgressBar();
						UIupdate = true;
				}

		}

		@Override
		public void onActivityResult(int requestCode, int resultCode, Intent data) {
				super.onActivityResult(requestCode, resultCode, data);
//				Log.d(TAG, "OnActivityResult Fragment Request Code: " + requestCode + " Result Code: " + resultCode + " data: " + data);
		}


		@Override
		public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String s) {
				updateUnit();
		}

		private class ScreenshotGenerator extends AsyncTask<Void, Void, String> {

				@Override
				protected String doInBackground(Void... params) {
						return getScreenshot();
				}

				@Override
				protected void onPreExecute() {
				}

				@Override
				protected void onPostExecute(String result) {
						Uri uri = Uri.fromFile(new File(result));
						Intent sendIntent = new Intent();
						sendIntent.setAction(Intent.ACTION_SEND);
						sendIntent.putExtra(Intent.EXTRA_TEXT, "#VaavudWather");
						sendIntent.putExtra(Intent.EXTRA_STREAM, uri);
						sendIntent.setType("image/png");

//								sendIntent.setType("text/plain");
						startActivity(sendIntent);
//						startButtonLayout.removeView(shareButton);
//						LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1.0f);
//						startButton.setLayoutParams(params);
//						startButton.setBackgroundResource(R.drawable.button_rounded_blue);
//						startButton.setTextColor(getResources().getColor(R.color.white));

				}
		}


		private String getScreenshot() {

				int screenshotWidth = 1080;
				int screenshotHeight = 1080;
				String mPath = Environment.getExternalStorageDirectory().toString() + "/Screenshot.png";
				SharingView shareView = new SharingView(context, currentMeanValueMS, currentDirection, currentMeanValueMS, currentMaxValueMS, dataset, renderer, currentUnit, currentDirectionUnit, currentPosition,getLocationController().getGeoLocation());
				int specWidth = View.MeasureSpec.makeMeasureSpec(screenshotWidth, View.MeasureSpec.AT_MOST);
				int specHeight = View.MeasureSpec.makeMeasureSpec(screenshotHeight, View.MeasureSpec.AT_MOST);
				shareView.measure(specWidth, specHeight);
//				Log.d(TAG, "Rendered View: " + measurementView.getWidth() + " " + measurementView.getHeight());
//				Log.d(TAG, "Measured: " + shareView.getMeasuredWidth() + " " + shareView.getMeasuredHeight());
				Bitmap bitmap = Bitmap.createBitmap(shareView.getMeasuredWidth(), shareView.getMeasuredHeight(), Bitmap.Config.ARGB_8888);
				Canvas c = new Canvas(bitmap);
//				Log.d(TAG, "Measured Height: " + measurementView.getHeight());
				shareView.layout(0, 0, shareView.getMeasuredWidth(), shareView.getMeasuredHeight());
				shareView.draw(c);
				// create bitmap screen capture
				OutputStream fout = null;
				File imageFile = new File(mPath);
				try
				{
						fout = new FileOutputStream(imageFile);
						bitmap.compress(Bitmap.CompressFormat.PNG, 100, fout);
						fout.flush();
						fout.close();
				} catch (FileNotFoundException e) {
						e.printStackTrace();
				} catch (IOException e) {
						e.printStackTrace();
				}

				return mPath;
		}
}
