package com.vaavud.android.ui.about;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.mixpanel.android.mpmetrics.MixpanelAPI;
import com.vaavud.android.R;
import com.vaavud.android.model.entity.Device;
import com.vaavud.android.ui.BackPressedListener;

public class AboutFragment extends Fragment implements BackPressedListener {

		private static final String TAG = "AboutFragment";
		private View view;
		private WebView webView;
		private Context context;
		private Device device;


		private static final String MIXPANEL_TOKEN = "757f6311d315f94cdfc8d16fb4d973c0";

		@Override
		public void onCreate(Bundle savedInstanceState) {
				super.onCreate(savedInstanceState);

		}

		@Override
		public void onAttach(Activity activity){
				super.onAttach(activity);
				context = activity;
				device = Device.getInstance(context.getApplicationContext());
		}

		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
				view = inflater.inflate(R.layout.fragment_about, container, false);

				webView = (WebView) view.findViewById(R.id.about_webView);
				webView.setWebViewClient(new WebViewClient());
				loadInitialData();


				return view;
		}

		private void loadInitialData() {

				if (getActivity() == null) {
						return;
				}

				String appVersion = device.getAppVersion();
//		Log.d(TAG,appVersion);
//		Log.d(TAG,getResources().getString(R.string.about_vaavud_text));
				String aboutVaavud = String.format(getResources().getString(R.string.about_vaavud_text), appVersion);
				aboutVaavud = aboutVaavud.replace("\n", "<br/><br/>");

				String html = String.format(
								"<html><head><style type='text/css'>a {color:#00aeef;text-decoration:none}</style></head><body>" +
												"<center style='padding-top:20px;font-family:helvetica,arial'>" +
												"%s<br/><br/><br/>" +
												"<a href='http://vaavud.com/legal/terms?source=app'>%s</a>&nbsp; &nbsp; <a href='http://vaavud.com/legal/privacy?source=app'>%s</a>" +
												"</center></body></html>",
								aboutVaavud,
								getResources().getString(R.string.link_terms_of_service),
								getResources().getString(R.string.link_privacy_policy));

				webView.loadDataWithBaseURL("http://vaavud.com", html, "text/html", "utf-8", null);
		}

		@Override
		public boolean onBackPressed() {
				if (webView.canGoBack()) {
						loadInitialData();
						webView.clearHistory();
				}
				return true;
		}

		@Override
		public void onResume() {
				super.onResume();
				if (context != null && device.isMixpanelEnabled()) {
						MixpanelAPI.getInstance(context.getApplicationContext(), MIXPANEL_TOKEN).track("About Screen", null);
				}
		}
}
