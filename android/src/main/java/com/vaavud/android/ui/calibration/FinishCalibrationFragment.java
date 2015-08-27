package com.vaavud.android.ui.calibration;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.mixpanel.android.mpmetrics.MixpanelAPI;
import com.vaavud.android.R;
import com.vaavud.android.model.VaavudDatabase;
import com.vaavud.android.model.entity.Device;
import com.vaavud.android.network.InternetManager;
import com.vaavud.android.ui.calibration.s3upload.UploadSoundFilesDialog;

public class FinishCalibrationFragment extends Fragment {

		private Context mContext;
		private static final String KEY_FIRST_TIME_SLEIPNIR = "firstTimeSleipnir";
		private static final String MIXPANEL_TOKEN = "757f6311d315f94cdfc8d16fb4d973c0";
		private boolean mFinished = false;
		private String mFilename;

		private UploadSoundFilesDialog uploadDialog;
		private AlertDialog askUploadDialog;

		public FinishCalibrationFragment(boolean finished, String filename) {
				mFinished = finished;
				mFilename = filename;
		}

		@Override
		public void onAttach(Activity activity) {
				super.onAttach(activity);
				mContext = activity;
		}

		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container,
														 Bundle savedInstanceState) {
				Typeface robotoLight = Typeface.createFromAsset(mContext.getAssets(), "Roboto-Light.ttf");
				Typeface robotoRegular = Typeface.createFromAsset(mContext.getAssets(), "Roboto-Regular.ttf");
				View rootView;
				if (mFinished) {
						rootView = inflater.inflate(R.layout.fragment_end_calibration, container, false);

						TextView completed = (TextView) rootView.findViewById(R.id.calibration_completed);

						completed.setTypeface(robotoLight);

						Button done = (Button) rootView.findViewById(R.id.calibration_done);

						done.setTypeface(robotoRegular);
						done.setOnClickListener(new OnClickListener() {

								@Override
								public void onClick(View v) {
										VaavudDatabase.getInstance(mContext.getApplicationContext()).setPropertyAsBoolean(KEY_FIRST_TIME_SLEIPNIR, true);
										if (mContext != null && Device.getInstance(mContext.getApplicationContext()).isMixpanelEnabled()) {
												MixpanelAPI.getInstance(mContext.getApplicationContext(), MIXPANEL_TOKEN).track("Calibration Finished", null);
										}
										((Activity) mContext).finish();
								}
						});
				}
				else{

						rootView = inflater.inflate(R.layout.fragment_failed_calibration, container, false);
						askUploadDialog = new AlertDialog.Builder(mContext)
										.setPositiveButton(R.string.button_ok,
														new DialogInterface.OnClickListener() {
																public void onClick(DialogInterface dialog, int whichButton) {
																		if (InternetManager.Check(mContext)) {
																				uploadDialog = new UploadSoundFilesDialog(getActivity(), mFilename);
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
						askUploadDialog.show();

				}
				return rootView;
		}
		@Override
		public void onResume(){
				super.onResume();

		}

}
