/*
 * Copyright 2010-2014 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package com.vaavud.android.ui.calibration.s3upload;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.vaavud.android.R;
import com.vaavud.android.model.S3TransferModel;
import com.vaavud.android.model.S3TransferModel.Status;
import com.vaavud.android.network.TransferController;

/*
 * This view handles user interaction with a single transfer, such as giving the
 * option to pause and abort, and also showing the user the progress of the transfer.
 */
public class S3TransferView extends LinearLayout {
    private static final String TAG = "TransferView";
    private Context mContext;
    private S3TransferModel[] mModel;
    private TextView mText;
    private ProgressBar mProgress;
    private S3TransferModel.Status status = S3TransferModel.Status.IN_PROGRESS;
    private int progress = 0;

    public S3TransferView(Context context, S3TransferModel[] model) {
        super(context);
        LayoutInflater.from(context).inflate(
                R.layout.view_s3transfer,
                this,
                true);

        mContext = context;
        mModel = model;

        mText = ((TextView) findViewById(R.id.s3_upload_text));
        mProgress = ((ProgressBar) findViewById(R.id.s3_upload_progress));

        refresh();
    }

    /*
     * We use this method within the class so that we can have the UI update
     * quickly when the user selects something
     */
    public void refresh() {
        int newProgress = 0;
        S3TransferModel.Status newStatus;
        for (int i=0;i<mModel.length;i++){
        	newStatus = mModel[i].getStatus();
        	status = compareStatus(status,newStatus);
        	newProgress = mModel[i].getProgress();
        	if (progress<newProgress) progress=newProgress;
        	Log.d(TAG,"Progress:"+progress+" "+newProgress);
        }
        
        mText.setText(status.name());
        mProgress.setProgress(progress);
    }

    /* What to do when user presses pause button */
    private void onPause() {
    	for (int i=0;i<mModel.length;i++){
	        if (mModel[i].getStatus() == Status.IN_PROGRESS) {
	            TransferController.pause(mContext, mModel[i]);
	            refresh();
	        } else {
	            TransferController.resume(mContext, mModel[i]);
	            refresh();
	        }
    	}
    }

    /* What to do when user presses abort button */
    private void onAbort() {
    	for (int i=0;i<mModel.length;i++){
    		TransferController.abort(mContext, mModel[i]);
    		refresh();
    	}
    }
    
    private S3TransferModel.Status compareStatus(S3TransferModel.Status status1,S3TransferModel.Status status2){
    	if (status1.equals(S3TransferModel.Status.CANCELED) || status2.equals(S3TransferModel.Status.CANCELED)) return S3TransferModel.Status.CANCELED;
    	else if (status1.equals(S3TransferModel.Status.PAUSED) || status2.equals(S3TransferModel.Status.PAUSED)) return S3TransferModel.Status.PAUSED;
    	else if (status1.equals(S3TransferModel.Status.COMPLETED) && status2.equals(S3TransferModel.Status.COMPLETED)) return S3TransferModel.Status.COMPLETED;
    	else return S3TransferModel.Status.IN_PROGRESS;
    }
}
