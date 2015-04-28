package com.vaavud.android.ui.calibration.s3upload;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.TextureView;
import android.view.View;
import android.widget.ProgressBar;

import com.mixpanel.android.mpmetrics.MixpanelAPI;
import com.vaavud.android.R;
import com.vaavud.android.model.S3TransferModel;
import com.vaavud.android.model.entity.Device;
import com.vaavud.android.network.TransferController;
import com.vaavud.android.ui.calibration.CalibrationActivity;

import java.io.File;

public class UploadSoundFilesDialog extends DialogFragment {

	static String TAG = "UPLOAD_SOUND_DIALOG";
	private static final String MIXPANEL_TOKEN = "757f6311d315f94cdfc8d16fb4d973c0";
	
	
	
	/*Dialog fields*/
	private View view = null;

	private Context context;
	
//	private ProgressDialog progress;
	private S3TransferModel[] mModels = new S3TransferModel[0];
	private ProgressBar progressBar;
	private TextureView text;
	private boolean completed=false;
	private static final int REFRESH_DELAY = 500;
	
	
	private Handler handler = new Handler();
    private Runnable updateProgressBar = new Runnable() {
    	
        @Override
        public void run() {
        	
        	syncModels();
        	if (completed){
//        		Log.d(TAG,"Completed");
        		progressBar.setVisibility(View.GONE);
            	dismiss();
            	((CalibrationActivity)context).finish();
        	}else{
            	if (mModels.length>0){
	            	int progress = mModels[0].getProgress();
	            	for (int i=1;i<mModels.length;i++){
	            		int newProgress = mModels[i].getProgress();
//	            		Log.d(TAG,"NEW PROGRESS: "+progress);
	            		if (progress > newProgress) progress = newProgress;
	            	}
//	            	Log.d(TAG,"PROGRESS: "+progress);
	        		progressBar.setProgress(progress);
            	}
            	handler.postDelayed(updateProgressBar, REFRESH_DELAY);
            }
        }
    };
	private String mFileName;


	public UploadSoundFilesDialog(Context context){
		this.context = context;
	}
	
	public UploadSoundFilesDialog(Context context,String fileName){

		this.context = context;
		mFileName = fileName; 
	}
	
	/** The system calls this only when creating the layout in a dialog. */
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		if (getActivity()!=null && Device.getInstance(getActivity()).isMixpanelEnabled()){
			MixpanelAPI.getInstance(context,MIXPANEL_TOKEN).track("Upload Sound Dialog", null);
		}
		AlertDialog.Builder builder = new AlertDialog.Builder(context);
		LayoutInflater inflater = ((Activity) context).getLayoutInflater();
		view = inflater.inflate(R.layout.dialog_upload_sound, null);
		progressBar = (ProgressBar) view.findViewById(R.id.upload_progress_bar);
		progressBar.setIndeterminate(false);
		builder.setView(view);
		builder.setTitle(context.getResources().getString(R.string.share_to_facebook_title));

		// Add the buttons
		builder.setNeutralButton(context.getResources().getString(R.string.button_cancel), new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int id) {
				if (getActivity()!=null && Device.getInstance(getActivity()).isMixpanelEnabled()){
					MixpanelAPI.getInstance(context,MIXPANEL_TOKEN).track("Upload File Dialog Cancelled", null);
				}
				for (int i=0;i<mModels.length;i++){
					TransferController.abort(context, mModels[i]);
				}
				handler.removeCallbacks(updateProgressBar);
				updateProgressBar=null;
				handler=null;
				((CalibrationActivity)context).finish();
			}
		});
		
		if (mFileName!=null){
			Uri uriRec = Uri.fromFile(new File(mFileName+".rec"));
			Uri uriPlay = Uri.fromFile(new File(mFileName+".play"));
			if (uriRec != null && uriPlay!=null) {
				TransferController.upload(context, uriRec);
				TransferController.upload(context, uriPlay);
				progressBar.setVisibility(View.VISIBLE);
				handler.post(updateProgressBar);
			}
		}
		
		
		AlertDialog dialog = builder.create();
		dialog.setOwnerActivity(getActivity());
		dialog.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
		dialog.setCancelable(false);
		dialog.setCanceledOnTouchOutside(false);
		dialog.setTitle(getResources().getString(R.string.calibration_sending_dialog_title));
		return dialog;

	}

	/* makes sure that we are up to date on the transfers */
    private void syncModels() {
        S3TransferModel[] models = S3TransferModel.getAllTransfers();
        
//        Log.d(TAG, "S3TransferModel.getAllTransfers(): "+models.length);
        if (mModels.length != models.length) {
            // add the transfers we haven't seen yet
            mModels = models;
        }
        int completedModels=0;
        for(int i=0;i<mModels.length;i++){
        	if (mModels[i].getStatus()!=null && mModels[i].getStatus().equals(S3TransferModel.Status.COMPLETED)) completedModels++;
        }
        if (completedModels==2){
//        	Log.d(TAG,"Completed");
        	S3TransferModel.resetTransfers();
        	completed=true;
        }
    }
	
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
//		Log.d(TAG,"OnActivityResult");
		super.onActivityResult(requestCode, resultCode, data);
	}

}

