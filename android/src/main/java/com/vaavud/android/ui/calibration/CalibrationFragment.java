package com.vaavud.android.ui.calibration;

import android.animation.Animator;
import android.animation.Animator.AnimatorListener;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.animation.ValueAnimator.AnimatorUpdateListener;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.mixpanel.android.mpmetrics.MixpanelAPI;
import com.pascalwelsch.holocircularprogressbar.HoloCircularProgressBar;
import com.vaavud.android.R;
import com.vaavud.android.measure.SleipnirCoreController;
import com.vaavud.android.model.entity.Device;
import com.vaavud.android.network.InternetManager;
import com.vaavud.android.ui.calibration.s3upload.UploadSoundFilesDialog;

import java.util.Date;


public class CalibrationFragment extends Fragment {

		private static final int UPLOAD_INTERVAL = 100;
		private static final int SUPPORT_INTERVAL = 10000;
		private static final float PERCENTAGE_MINIMUM_INCREMENT = 0.01f;
		private static final String MIXPANEL_TOKEN = "757f6311d315f94cdfc8d16fb4d973c0";
		private static final String TAG = "Vaavud:CalibrationFrag";

		private HoloCircularProgressBar mCircularBar;

		private ObjectAnimator mProgressBarAnimator;

		private SleipnirCoreController mController;

		private Context mContext;

		private TextView percentage;
		private long startTime;

		private UploadSoundFilesDialog uploadDialog;
		private AlertDialog askUploadDialog;

		private Handler handler = new Handler();

		private Runnable updateUI = new Runnable() {

				@Override
				public void run() {
						if (mController.isMeasuring()) {
								float oldPercentage = mCircularBar.getProgress();
								float percentageValue = mController.getCalibrationProgress();

								float calibrationPercentageIncrement = (percentageValue - oldPercentage);
								animate(mCircularBar, null, percentageValue);
								long time = new Date().getTime();
//                Log.d(TAG, "Time: " + time + " startTime: " + startTime + " CalibrationPercentageIncrement: " + calibrationPercentageIncrement);
								if ((time - startTime) > SUPPORT_INTERVAL && calibrationPercentageIncrement < PERCENTAGE_MINIMUM_INCREMENT) {
//										Log.d(TAG,"currentPlayerVolume: "+currentPlayerVolume);
										askUploadDialog.show();
								} else {
										if (calibrationPercentageIncrement > PERCENTAGE_MINIMUM_INCREMENT) {
												startTime = new Date().getTime();
										}
										handler.postDelayed(updateUI, UPLOAD_INTERVAL);
								}
						}
				}
		};

		public CalibrationFragment() {
		}

		@Override
		public void onAttach(Activity activity) {
				super.onAttach(activity);
				mContext = activity;
		}

		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container,
														 Bundle savedInstanceState) {

				View rootView = inflater.inflate(R.layout.fragment_calibration,
								container, false);

				Typeface robotoLight = Typeface.createFromAsset(mContext.getAssets(), "Roboto-Light.ttf");
				Typeface robotoRegular = Typeface.createFromAsset(mContext.getAssets(), "Roboto-Regular.ttf");

				mCircularBar = (HoloCircularProgressBar) rootView.findViewById(R.id.holoCircularProgressBar);
				mCircularBar.setVisibility(View.VISIBLE);

				percentage = (TextView) rootView.findViewById(R.id.calibration_percentage);
				percentage.setTypeface(robotoLight);

				TextView message = (TextView) rootView.findViewById(R.id.calibration_message);
				message.setTypeface(robotoLight);

				Button button = (Button) rootView.findViewById(R.id.calibration_cancel);
				button.setTypeface(robotoRegular);

				button.setOnClickListener(new OnClickListener() {
						@Override
						public void onClick(View v) {
								if (mController.isMeasuring()) {
										handler.removeCallbacks(updateUI);
										mController.stopMeasuring();
								}
								mController.stopController();
								if (mContext != null && Device.getInstance(mContext.getApplicationContext()).isMixpanelEnabled()) {
										MixpanelAPI.getInstance(mContext.getApplicationContext(), MIXPANEL_TOKEN).track("Calibration Cancelled", null);
								}
								((Activity) mContext).finish();
						}
				});

				askUploadDialog = new AlertDialog.Builder(mContext)
								.setPositiveButton(R.string.button_ok,
												new DialogInterface.OnClickListener() {
														public void onClick(DialogInterface dialog, int whichButton) {
																if (mController.isMeasuring()) {
																		mController.stopMeasuring();
																		mController.stopController();
																		handler.removeCallbacks(updateUI);
																}

																if (InternetManager.Check(mContext)) {
																		uploadDialog = new UploadSoundFilesDialog(getActivity(), mController.getFileName());
																		uploadDialog.show(getFragmentManager(), "UploadDialog");
																} else {
																		Toast.makeText(mContext, getResources().getString(R.string.conectivity_error_message), Toast.LENGTH_LONG).show();
																		((Activity) mContext).finish();
																}
														}
												}
								)
								.setNegativeButton(R.string.button_cancel,
												new DialogInterface.OnClickListener() {
														public void onClick(DialogInterface dialog, int whichButton) {
																if (mController.isMeasuring()) {
																		mController.stopMeasuring();
																		mController.stopController();
																		handler.removeCallbacks(updateUI);
																}
																if (mContext != null && Device.getInstance(mContext.getApplicationContext()).isMixpanelEnabled()) {
																		MixpanelAPI.getInstance(mContext.getApplicationContext(), MIXPANEL_TOKEN).track("Calibration Cancelled", null);
																}
																((Activity) mContext).finish();

														}
												}
								)
								.create();
				askUploadDialog.setTitle(getResources().getString(R.string.calibration_upload_dialog_title));
				askUploadDialog.setMessage(getResources().getString(R.string.calibration_upload_dialog_text));

				startTime = new Date().getTime();

				return rootView;
		}

		@Override
		public void onResume() {
				super.onResume();
						mController.startMeasuring();
						handler.post(updateUI);
		}

		@Override
		public void onCreate(Bundle savedInstanceState) {
				super.onCreate(savedInstanceState);
				mController = new SleipnirCoreController(mContext, null, null, null, true);
				mController.startController();
		}

		@Override
		public void onStop() {
				super.onStop();
				if (mController.isMeasuring()) {
						mController.stopMeasuring();
						handler.removeCallbacks(updateUI);
						mController.stopController();
						((Activity) mContext).finish();
				}

		}

		@Override
		public void onDestroy() {
				super.onDestroy();
				updateUI = null;
				handler = null;
				mController = null;
				mContext = null;
		}

		/**
		 * Animate.
		 *
		 * @param progressBar the progress bar
		 * @param listener    the listener
		 */
		protected void animate(final HoloCircularProgressBar progressBar,
													 final AnimatorListener listener, float progress) {
				int duration = 200;
				animate(progressBar, listener, progress, duration);
		}

		private void animate(final HoloCircularProgressBar progressBar, final AnimatorListener listener,
												 final float progress, final int duration) {
				mProgressBarAnimator = ObjectAnimator.ofFloat(progressBar, "progress", progress);
				mProgressBarAnimator.setDuration(duration);
				mProgressBarAnimator.addListener(new AnimatorListener() {
						@Override
						public void onAnimationCancel(final Animator animation) {
						}

						@Override
						public void onAnimationEnd(final Animator animation) {
								progressBar.setProgress(progress);
								if (progress >= 1 && mController != null) {
										handler.removeCallbacks(updateUI);
										mController.stopMeasuring();
										mController.stopController();
										((CalibrationActivity) mContext).getSupportFragmentManager().beginTransaction()
														.replace(R.id.container, new FinishCalibrationFragment()).commit();
								}
						}

						@Override
						public void onAnimationRepeat(final Animator animation) {
						}

						@Override
						public void onAnimationStart(final Animator animation) {
						}
				});
				if (listener != null) {
						mProgressBarAnimator.addListener(listener);
				}
				mProgressBarAnimator.reverse();
				mProgressBarAnimator.addUpdateListener(new AnimatorUpdateListener() {
						public void onAnimationUpdate(final ValueAnimator animation) {
								int progress = (int) (((Float) animation.getAnimatedValue()) * 100);
								percentage.setText(progress + " %");
								progressBar.setProgress((Float) animation.getAnimatedValue());
						}
				});
				progressBar.setMarkerProgress(progress);
				mProgressBarAnimator.start();
		}

}
