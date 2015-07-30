package com.vaavud.android.ui.login;

import android.app.Activity;
import android.content.Context;
import android.graphics.PorterDuff;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
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


public class SelectorFragment extends Fragment implements BackPressedListener {

		private View view;
		private Context context;

		private Button signUpButton;
		private Button logInButton;


		private static final String MIXPANEL_TOKEN = "757f6311d315f94cdfc8d16fb4d973c0";

		private final String LOG_TAG = "SELECTOR_FRAGMENT";

		@Override
		public void onAttach(Activity activity) {
				super.onAttach(activity);
				context = activity;
		}

		@Override
		public void onCreate(Bundle savedInstanceState) {
				super.onCreate(savedInstanceState);
		}

		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container,
														 Bundle savedInstanceState) {
				view = inflater.inflate(R.layout.fragment_selector, container, false);

				signUpButton = (Button) view.findViewById(R.id.signUpButton);
				logInButton = (Button) view.findViewById(R.id.logInButton);
				signUpButton.getBackground().setColorFilter(view.getResources().getColor(R.color.blue), PorterDuff.Mode.SRC_ATOP);

				signUpButton.setOnClickListener(new OnClickListener() {

						@Override
						public void onClick(View v) {
								Fragment signUpFragment = new SignUpFragment();
								FragmentTransaction transaction = getActivity().getSupportFragmentManager()
												.beginTransaction();
								transaction.replace(android.R.id.content, signUpFragment);
								transaction.addToBackStack(null);

								transaction.commit();

						}
				});
				logInButton.setOnClickListener(new OnClickListener() {

						@Override
						public void onClick(View v) {
								Fragment loginFragment = new LoginFragment();
								FragmentTransaction transaction = getActivity().getSupportFragmentManager()
												.beginTransaction();
								transaction.replace(android.R.id.content, loginFragment);
								transaction.addToBackStack(null);

								transaction.commit();
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
				if (context != null && Device.getInstance(context.getApplicationContext()).isMixpanelEnabled()) {
						MixpanelAPI.getInstance(context.getApplicationContext(), MIXPANEL_TOKEN).track("Signup/Login Selection Screen", null);
				}
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


}
