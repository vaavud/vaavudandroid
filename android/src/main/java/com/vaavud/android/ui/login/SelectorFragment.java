package com.vaavud.android.ui.login;

import android.app.Activity;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.mixpanel.android.mpmetrics.MixpanelAPI;
import com.vaavud.android.R;
import com.vaavud.android.model.entity.Device;
import com.vaavud.android.ui.BackPressedListener;
import com.vaavud.android.ui.SelectedListener;
import com.vaavud.android.ui.SelectorListener;


public class SelectorFragment extends Fragment implements BackPressedListener,SelectedListener,SelectorListener{
		
	private SelectorListener mCallback;
	private View view;
	
	private Button signUpButton;
	private Button logInButton;
	private TextView labelText;
	private Typeface futuraMediumTypeface;
	
	private static final String MIXPANEL_TOKEN = "757f6311d315f94cdfc8d16fb4d973c0"; 

	@Override
	public void onAttach(Activity activity) {
	  super.onAttach(activity);
	      try {
	    	  mCallback = (SelectorListener) activity;
	      } catch (ClassCastException e) {
	          throw new ClassCastException(activity.toString() + " must implement SelectorListener");
	      }
	      
	}

	final String LOG_TAG = "myLogs";
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		futuraMediumTypeface = Typeface.createFromAsset(getActivity().getAssets(), "futuraMedium.ttf");
	}
	
	@Override		
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
	    Bundle savedInstanceState) {
		view = inflater.inflate(R.layout.fragment_selector, container, false);
		
		signUpButton = (Button) view.findViewById(R.id.signUpButton);
		logInButton = (Button) view.findViewById(R.id.logInButton);
		labelText = (TextView) view.findViewById(R.id.labelText);
	
		
		signUpButton.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				onMenuOptionSelected(0);				
			}
		});
		logInButton.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				onMenuOptionSelected(1);				
			}
		});
		return view;
	}

	@Override
	public boolean onBackPressed() {
		// TODO Auto-generated method stub
		return false;
	}
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
//		Log.i("SelectorFragment", "onActivityCreated, savedInstanceState" + (savedInstanceState == null ? "=null" : "!=null"));
	}
	
	@Override
	public void onViewStateRestored(Bundle savedInstanceState) {
		super.onViewStateRestored(savedInstanceState);
	}

	@Override
	public void onStart() {
		super.onStart();
//		Log.i("SelectorFragment", "onStart");
	}

	@Override
	public void onResume() {
		super.onResume();
		
	}

	@Override
	public void onPause() {
		super.onPause();
//		Log.i("SelectorFragment", "onPause");
	}
	
	@Override
	public void onStop() {
		super.onStop();
//		Log.i("SelectorFragment", "onStop");
	}

	@Override
	public void onDestroyView() {
		super.onDestroyView();
//		Log.i("SelectorFragment", "onDestroyView");
		
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
//		Log.i("SelectorFragment", "onDestroy");
		
	}

	@Override
	public void onDetach() {
		super.onDetach();
//		Log.i("SelectorFragment", "onDetach");
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
//		Log.i("SelectorFragment", "onSaveInstanceState");
		
	}
	
	@Override
	public void onLowMemory() {
		super.onLowMemory();
//		Log.i("SelectorFragment", "onLowMemory");
		
	}

	@Override
	public void onSelected() {
//		super.onSelected();
		if (getActivity()!=null && Device.getInstance(getActivity()).isMixpanelEnabled()){
			MixpanelAPI.getInstance(getActivity(),MIXPANEL_TOKEN).track("Signup/Login Selection Screen", null);
		}
//		Log.i("SelectorFragment", "onSelected");
	}

	@Override
	public void onMenuOptionSelected(int position) {
		mCallback.onMenuOptionSelected(position);
	}
}
