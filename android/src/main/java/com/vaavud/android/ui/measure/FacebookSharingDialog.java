package com.vaavud.android.ui.measure;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.app.DialogFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;

import com.facebook.FacebookRequestError;
import com.facebook.HttpMethod;
import com.facebook.Request;
import com.facebook.RequestBatch;
import com.facebook.Response;
import com.facebook.Session;
import com.facebook.Session.OpenRequest;
import com.facebook.Session.StatusCallback;
import com.facebook.SessionState;
import com.facebook.model.OpenGraphAction;
import com.facebook.model.OpenGraphObject;
import com.mixpanel.android.mpmetrics.MixpanelAPI;
import com.vaavud.android.R;
import com.vaavud.android.model.entity.Device;
import com.vaavud.android.model.entity.LatLng;
import com.vaavud.android.model.entity.SpeedUnit;
import com.vaavud.android.ui.MainActivity;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

public class FacebookSharingDialog extends DialogFragment {

	static String TAG = "FACEBOOK_SHARING_DIALOG";
	private static final String MIXPANEL_TOKEN = "757f6311d315f94cdfc8d16fb4d973c0";
	
	/*FACEBOOK SHARING*/
	private static final List<String> publishPermisions = Arrays.asList("publish_actions");
	private List<Bitmap> bitMapArray = null;
	
	
	/*PICTURE DIALOG*/
	private static final int CHOOSE_IMAGE_CAMERA_REQUEST_CODE = 100;
	private static final int CHOOSE_IMAGE_GALLERY_REQUEST_CODE = 101;
	private Uri tmpUri = null;
	private static int MAX_IMAGES = 3;
	
	/*Dialog fields*/
	private ImageView imageViewLeft = null;
	private ImageView imageViewCenter = null;
	private ImageView imageViewRight = null;
	private View view = null;
	private EditText text = null;

	private Context context;

	private SpeedUnit currentUnit;
	private Float currentMeanValueMS;
	private Float currentMaxValueMS;
	private LatLng currentPosition;
	
	private ProgressDialog progress;
	private FBStatusCallback fbStatusCallback = null; 
	
	
	private class FBStatusCallback implements StatusCallback {
		
		@Override
		public void call(Session session, SessionState state, Exception exception) {
			if (state.isOpened()) {
				if (session.isPermissionGranted("publish_actions")) publishStory();
			}
		}

	};

	public FacebookSharingDialog(Context context){
		this.context = context;
	}
	
	public FacebookSharingDialog(Context context, Float currentMeanValueMS, Float currentMaxValueMS,LatLng currentPosition) {
		this.context = context;
		this.currentMeanValueMS = currentMeanValueMS;
		this.currentMaxValueMS = currentMaxValueMS;
		this.currentPosition = currentPosition;
		bitMapArray = new ArrayList<Bitmap>();
//		Settings.addLoggingBehavior(LoggingBehavior.REQUESTS);
	}
	
	/** The system calls this only when creating the layout in a dialog. */
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		if (getActivity()!=null && Device.getInstance(getActivity()).isMixpanelEnabled()){
			MixpanelAPI.getInstance(context,MIXPANEL_TOKEN).track("Share Dialog", null);
		}
		AlertDialog.Builder builder = new AlertDialog.Builder(context);
		LayoutInflater inflater = ((Activity) context).getLayoutInflater();
		view = inflater.inflate(R.layout.dialog_facebook, null);
		
		imageViewLeft = (ImageView) view.findViewById(R.id.fbImageViewLeft);
		imageViewCenter = (ImageView) view.findViewById(R.id.fbImageViewCenter);
		imageViewRight = (ImageView) view.findViewById(R.id.fbImageViewRight);
		
		currentUnit = Device.getInstance(context).getWindSpeedUnit();
		
		text = ((EditText)view.findViewById(R.id.fbEditText));
		
		((ImageButton)view.findViewById(R.id.fbImageButton)).setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				if(bitMapArray.size()<MAX_IMAGES){
					startPictureDialog();
					view.invalidate();
				}
//				else{
//						Toast.makeText(context,"You can't add more images.", Toast.LENGTH_LONG);
//					}
			}
		});
		builder.setView(view);
		builder.setTitle(((Activity)context).getResources().getString(R.string.share_to_facebook_title));
		builder.setCancelable(false);

		// Add the buttons
		builder.setPositiveButton(((Activity)context).getResources().getString(R.string.button_ok), new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int id) {
				// User clicked OK button
				checkPermisions();
			}
		});
		builder.setNegativeButton(((Activity)context).getResources().getString(R.string.button_cancel), new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int id) {
				if (getActivity()!=null && Device.getInstance(getActivity()).isMixpanelEnabled()){
					MixpanelAPI.getInstance(context,MIXPANEL_TOKEN).track("Share Dialog Cancelled", null);
				}
			}
		});
		// Set other dialog properties

		// Create the AlertDialog
		AlertDialog dialog = builder.create();
		dialog.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
		return dialog;

	}


	private void startPictureDialog() {
		
		
		AlertDialog.Builder pictureBuilder = new AlertDialog.Builder(context);
		CharSequence[] itemlist ={getResources().getString(R.string.share_take_photo),getResources().getString(R.string.share_choose_existing)};
		pictureBuilder.setItems(itemlist, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				Intent pictureActionIntent = null;
				switch (which) {
				case 0:
					pictureActionIntent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
					tmpUri = getOutputMediaFileUri(); // create a file to save the image
//					Log.d(TAG,"Camera FileURI: "+tmpUri.toString());
					
					pictureActionIntent.putExtra(MediaStore.EXTRA_OUTPUT, tmpUri); // set the image file name
					startActivityForResult(pictureActionIntent,CHOOSE_IMAGE_CAMERA_REQUEST_CODE);
					break;
				case 1:
					pictureActionIntent = new Intent(Intent.ACTION_GET_CONTENT, null);
					pictureActionIntent.setType("image/*");
					startActivityForResult(pictureActionIntent,CHOOSE_IMAGE_GALLERY_REQUEST_CODE);
					break;
				default:
					break;
				}
			}
		});
		AlertDialog dialog = pictureBuilder.create();
		dialog.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
		dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
		dialog.show();
	}

	// Create a file Uri for saving an image or video 
	private Uri getOutputMediaFileUri(){
		return Uri.fromFile(getOutputMediaFile());
	}

	// Create a File for saving an image 
	private File getOutputMediaFile(){

		File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(
				Environment.DIRECTORY_PICTURES), "Vaavud");

		//Environment.getExternalStorageDirectory().getAbsolutePath();
		// Create the storage directory if it does not exist
		if (! mediaStorageDir.exists()){
			if (! mediaStorageDir.mkdirs()){
				return null;
			}
		}

		// Create a media file name
		String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
		return new File(mediaStorageDir.getPath() + File.separator +"IMG_"+ timeStamp + ".jpg");

	}


	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
//		Log.d(TAG,"OnActivityResult");
		super.onActivityResult(requestCode, resultCode, data);
		Boolean imageTaken=false;
		switch (requestCode) {
		case CHOOSE_IMAGE_CAMERA_REQUEST_CODE:
			if (resultCode == Activity.RESULT_OK) {
				// Image captured and saved to fileUri specified in the Intent
//				Toast.makeText(getActivity(), "OK", Toast.LENGTH_SHORT).show();
				imageTaken=true;

			} else if (resultCode == Activity.RESULT_CANCELED) {
				imageTaken=false;
//				Toast.makeText(getActivity(), "Cancelled", Toast.LENGTH_SHORT).show();
			} else {
				imageTaken=false;
//				Toast.makeText(getActivity(), "Fail", Toast.LENGTH_SHORT).show();
			}
			break;
		case CHOOSE_IMAGE_GALLERY_REQUEST_CODE:
			if (resultCode == Activity.RESULT_OK) {
//				Toast.makeText(getActivity(), "Gallery OK", Toast.LENGTH_SHORT).show();
				tmpUri=data.getData();
				imageTaken=true;
			} else if (resultCode == Activity.RESULT_CANCELED) {
				imageTaken=false;
//				Toast.makeText(getActivity(), "Gallery Cancelled", Toast.LENGTH_SHORT).show();
			} else {
				imageTaken=false;
//				Toast.makeText(getActivity(), "Gallery Fail", Toast.LENGTH_SHORT).show();
			}
			break;
		default:
//			Log.d(TAG,"Default");
//			checkPermisions();
			break;
		}
		
		if (imageTaken){

			Bitmap bitmap=generateReduceBitmapFromURI(tmpUri);
			if (bitmap!=null){
				bitMapArray.add(bitmap);
				int index=bitMapArray.size()-1;
				switch (index) {
				case 0:
					imageViewLeft.setImageBitmap(bitMapArray.get(index));
					break;
				case 1:
					imageViewCenter.setImageBitmap(bitMapArray.get(index));
					break;
				case 2:
					imageViewRight.setImageBitmap(bitMapArray.get(index));
					break;
				default:
					break;
				}
			}
		}
	}
	
	private Bitmap generateReduceBitmapFromURI(Uri tmpUri) {
		InputStream input=null;
		Bitmap bitmap = null;
		int width=0;
		int height=0;
		float ratio=0;
		int angle=0;
		try {
			input = context.getContentResolver().openInputStream(tmpUri);
		} catch (FileNotFoundException e1) {
			e1.printStackTrace();
		}
		
		angle=getOrientation(context, tmpUri);
		
		if (input!=null){
			bitmap=BitmapFactory.decodeStream(input);
			width=bitmap.getWidth();
			height=bitmap.getHeight();
//			Log.d(TAG,"Width: "+ width + " Height: "+ height);
			ratio=(float)width/height;
			if (width>=1024 || height >= 1024){
				if (width>height){
					width=1024;
					height=(int) (1024/ratio);
				}else{
					height=1024;
					width=(int) (1024*ratio);
				}
			
				Matrix matrix = new Matrix();

				matrix.postRotate(angle);
				
				Bitmap scaledBitmap = Bitmap.createScaledBitmap(bitmap, width,height,true);
				bitmap = null;
				return Bitmap.createBitmap(scaledBitmap , 0, 0, scaledBitmap.getWidth(), scaledBitmap.getHeight(), matrix, true);
			}
		}
		return bitmap;
	}
	
	private int getOrientation(Context context, Uri photoUri) {
	    /* it's on the external media. */
	    Cursor cursor = context.getContentResolver().query(photoUri,new String[] { MediaStore.Images.ImageColumns.ORIENTATION }, null, null, null);

	    if (cursor==null){
	    	return 90;
	    }
	    if (cursor.getCount() != 1) {
	        return 0;
	    }

	    cursor.moveToFirst();
	    return cursor.getInt(0);
	}

	private void checkPermisions(){
		final Session session = Session.getActiveSession();
		if (session!=null && session.isOpened()){
//			Log.d(TAG,"Session ON");
			// Check for publish permissions
			new Request(
					session,
					"/me/permissions",
					null,
					HttpMethod.GET,
					new Request.Callback() {
						public void onCompleted(Response response) {
							String permission;
							Boolean permGranted=false;
							try {
								if(response.getGraphObject()!=null){
									for (int i=0;i<response.getGraphObject().getInnerJSONObject().getJSONArray("data").length();i++){
										permission=response.getGraphObject().getInnerJSONObject().getJSONArray("data").getJSONObject(i).getString("permission");
										if (permission.equals("publish_actions")){
											if(response.getGraphObject().getInnerJSONObject().getJSONArray("data").getJSONObject(i).getString("status").equals("granted")){
												permGranted=true;
												break;
											}
										}
									}
								}

							} catch (JSONException e) {
								e.printStackTrace();
							}
							if (permGranted){
								publishStory();
							}
							else{
								Session.NewPermissionsRequest newPermissionsRequest = new Session
										.NewPermissionsRequest((Activity) context, publishPermisions);
								fbStatusCallback=new FBStatusCallback();
								newPermissionsRequest.setCallback(fbStatusCallback);
								session.requestNewPublishPermissions(newPermissionsRequest);
								return;
							}
						}
					}
					).executeAsync();
		}
		else{
			if (progress==null){
				progress=new ProgressDialog(context);
				progress.setCancelable(false);
				progress.setMessage(context.getResources().getString(R.string.register_feedback_conecting_server));
				((MainActivity)context).setProgressDialog(progress);
			}
			progress.show();
			fbStatusCallback=new FBStatusCallback();
			List<String> permissions = new ArrayList<String>();
			permissions.add("publish_actions");
			Session newSession = openActiveSession((Activity) context, true, permissions, fbStatusCallback);
			if (newSession==null){
//				Log.d(TAG,"Session is null");
				progress.dismiss();
			}
			else{
				Session.setActiveSession(newSession);
				progress.dismiss();
			}
		}
	}
	
	private Session openActiveSession(Activity activity, boolean allowLoginUI, List<String> permissions, StatusCallback callback) {
//		Log.d(TAG, "On Activity: "+activity.getPackageName()+" "+activity.getLocalClassName());
		
	 	OpenRequest openRequest = new OpenRequest(activity).setPermissions(permissions).setCallback(callback);
		Session session = new Session.Builder(activity).build();
		if (SessionState.CREATED_TOKEN_LOADED.equals(session.getState()) || allowLoginUI) {
			Session.setActiveSession(session);
			session.openForPublish(openRequest);
			return session;
		}else if (SessionState.OPENED.equals(session.getState())){
//			Log.d(TAG,"OPENED: Session State:"+session.getState());
			return session;
		}
		
		return null;
	}


	protected void publishStory() {
		
//		Settings.addLoggingBehavior(LoggingBehavior.REQUESTS);
		progress = ((MainActivity)context).getProgressDialog();
		if (progress == null){
			progress=new ProgressDialog(context);
			progress.setCancelable(false);
			progress.setMessage(context.getResources().getString(R.string.register_feedback_conecting_server));
			((MainActivity)context).setProgressDialog(progress);
		}
		progress.show();

		RequestBatch requestBatch = new RequestBatch();
		

		OpenGraphObject wind = OpenGraphObject.Factory.createForPost("vaavudapp:wind_speed");
		wind.setProperty("title", currentUnit.format(currentMeanValueMS) + " "+ currentUnit.getEnglishDisplayName(context));
		wind.setProperty("image", "http://vaavud.com/FacebookOpenGraphObjectImage.png");
		wind.setProperty("url", "http://www.vaavud.com");
		wind.setProperty("description",currentUnit.format(currentMeanValueMS) + " "+ currentUnit.getEnglishDisplayName(context));

		JSONObject data = new JSONObject();
		JSONObject location = new JSONObject();
		
		
		
		try {
			data.put("speed", currentUnit.format(currentMeanValueMS));
			data.put("max_speed", currentUnit.format(currentMaxValueMS));
			data.put("unit", currentUnit.getEnglishDisplayName(context));

			if(currentPosition!=null){
				location.put("latitude", currentPosition.getLatitude());
				location.put("longitude", currentPosition.getLongitude());
			}else{
				location.put("latitude", null);
				location.put("longitude", null);
			}
			data.put("location",location);
		} catch (JSONException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		wind.setProperty("data",data);
		// Set up the object request callback
		Request.Callback objectCallback = new Request.Callback() {

			@Override
			public void onCompleted(Response response) {
				// Log any response error
				FacebookRequestError error = response.getError();
				if (error != null) {
					Log.i(TAG, error.getErrorMessage());
					return;
				}
			}
		};

		// Create the request for object creation
		Request objectRequest = Request.newPostOpenGraphObjectRequest(Session.getActiveSession(),
				wind, objectCallback);

		// Set the batch name so you can refer to the result
		// in the follow-on publish action request
		objectRequest.setBatchEntryName("objectCreate");

		// Add the request to the batch
		requestBatch.add(objectRequest);

		if (bitMapArray.size()>0) {
		    // Set up image upload request parameters

				for (int i =0;i<bitMapArray.size();i++){
			
				    // Set up the image upload request callback
				    Request.Callback imageCallback = new Request.Callback() {
		
				        @Override
				        public void onCompleted(Response response) {
				            // Log any response error
				            FacebookRequestError error = response.getError();
				            if (error != null) {
				                Log.i(TAG, error.getErrorMessage());
				                return;
				            }
				        }
				    };
		
				    // Create the request for the image upload
				    Request imageRequest = Request.newUploadStagingResourceWithImageRequest(Session.getActiveSession(),bitMapArray.get(i), imageCallback);
				    
				    // Set the batch name so you can refer to the result
				    // in the follow-on object creation request
				    imageRequest.setBatchEntryName("imageUpload"+i);
				    
				    // Add the request to the batch
				    requestBatch.add(imageRequest);
				}
			}
		

		OpenGraphAction action = OpenGraphAction.Factory.createForPost("vaavudapp:measure");
		action.setProperty("wind_speed", "{result=objectCreate:$.id}");
		action.setProperty("fb:explicitly_shared", "true");
		
		for (int i=0;i<bitMapArray.size();i++){
				action.setProperty("image["+i+"][url]","{result=imageUpload"+i+":$.uri}");
				action.setProperty("image["+i+"][user_generated]", "true");
		}
		
		action.setProperty("message",text.getText().toString());
		// Set up the action request callback
		Request.Callback actionCallback = new Request.Callback() {

			@Override
			public void onCompleted(Response response) {
				progress.dismiss();
				FacebookRequestError error = response.getError();
				if (error != null) {
//					Log.d(TAG,error.getErrorMessage());
					return;
				} else {
					JSONObject props = new JSONObject();
	        		try {
	        			if (text.getText().length()>0)
	        				props.put("Message", true);
	        			else
	        				props.put("Message", false);
	        			props.put("Photos",bitMapArray.size());
					} catch (JSONException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
	        		if (getActivity()!=null && Device.getInstance(getActivity()).isMixpanelEnabled()){
	        			MixpanelAPI.getInstance(context,MIXPANEL_TOKEN).track("Share Dialog Successful", props);
	        		}
				}
			}
		};

		// Create the publish action request
		Request actionRequest = Request.newPostOpenGraphActionRequest(Session.getActiveSession(),
				action, actionCallback);

		// Add the request to the batch
		requestBatch.add(actionRequest);
		requestBatch.executeAsync();
	}



}

