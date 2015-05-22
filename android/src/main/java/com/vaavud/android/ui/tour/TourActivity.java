package com.vaavud.android.ui.tour;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.crittercism.app.Crittercism;
import com.mixpanel.android.mpmetrics.MixpanelAPI;
import com.vaavud.android.R;
import com.vaavud.android.VaavudApplication;
import com.vaavud.android.model.entity.Device;
import com.vaavud.android.ui.MainActivity;
import com.vaavud.android.ui.login.LoginActivity;
import com.viewpagerindicator.CirclePageIndicator;

public class TourActivity extends FragmentActivity {


		public static String TAG = "TOUR_ACTIVITY";
		private static final String MIXPANEL_TOKEN = "757f6311d315f94cdfc8d16fb4d973c0";

		private static final int SIGNUP_REQUEST = 701;
		private static final int LOGIN_REQUEST = 702;

		private SectionsPagerAdapter mSectionsPagerAdapter;
		private int numberPages = 3;
		private int startPosition = 0;
		/**
		 * The {@link ViewPager} that will host the section contents.
		 */
		private static ViewPager mViewPager;

		//USER CHARACTERISTICS
		private boolean isLoggedIn = false;
		private boolean hasWindMeter = false;
		private boolean tips = false;

		@Override
		protected void onCreate(Bundle savedInstanceState) {

				this.requestWindowFeature(Window.FEATURE_NO_TITLE);

				super.onCreate(savedInstanceState);
				Crittercism.initialize(getApplicationContext(), "520b8fa5558d6a2757000003");

				Intent i = getIntent();
				if (i != null) {
						startPosition = i.getIntExtra("startPosition", 0);
						numberPages = i.getIntExtra("numberPages", 3);
						tips = i.getBooleanExtra("tips", false);
				}

				if (!((VaavudApplication) getApplication()).isFirstFlow() && !tips) {
						Intent noTourIntent = new Intent(this, MainActivity.class);
						startActivity(noTourIntent);
						finish();
				}

				setContentView(R.layout.activity_tour);

				isLoggedIn = ((VaavudApplication) getApplication()).isUserLogged();
				hasWindMeter = ((VaavudApplication) getApplication()).hasWindMeter();


				// Create the adapter that will return a fragment for each of the three
				// primary sections of the activity.
				mSectionsPagerAdapter = new SectionsPagerAdapter(
								getSupportFragmentManager());


				if (startPosition == 0 && numberPages == 3 && !tips) {
						if (isLoggedIn && hasWindMeter) {
//				Log.d(TAG,"Has user has windmeter");
								startPosition = 5;
								numberPages = 4;
						} else if (isLoggedIn && !hasWindMeter) {
//				Log.d(TAG,"Has user no has windmeter");
								startPosition = 3;
								numberPages = 1;
						} else {
//				Log.d(TAG,"No has user no has windmeter");
						}
				} else if (tips) {
						startPosition = 5;
						numberPages = 4;
				}

				// Set up the ViewPager with the sections adapter.
				mViewPager = (ViewPager) findViewById(R.id.pager);
				mViewPager.setAdapter(mSectionsPagerAdapter);
				mViewPager.getCurrentItem();

		}

		@Override
		public void onResume() {
				super.onResume();
		}

		/**
		 * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
		 * one of the sections/tabs/pages.
		 */
		public class SectionsPagerAdapter extends FragmentPagerAdapter {

				public SectionsPagerAdapter(FragmentManager fm) {
						super(fm);

				}

				@Override
				public Fragment getItem(int position) {
						// getItem is called to instantiate the fragment for the given page.
						// Return a PlaceholderFragment (defined as a static inner class
						// below).
						if (position < startPosition) {
								position = position + startPosition;
						}
						return PlaceholderFragment.newInstance(position, tips);
				}

				@Override
				public int getCount() {
						// Show 9 total pages.
						return numberPages;
				}

				@Override
				public CharSequence getPageTitle(int position) {
//			switch (position) {
//			case 0:
//			case 3:
//			case 6:
//			case 1:
//			case 4:
//			case 7:
//			case 2:
//			case 5:
//			case 8:
//				return "Tour";
//			}
//			return null;
						return "First Flow";
				}

		}

		/**
		 * A placeholder fragment containing a simple view.
		 */
		public static class PlaceholderFragment extends Fragment {
				/**
				 * The fragment argument representing the section number for this
				 * fragment.
				 */
				private static final String ARG_SECTION_NUMBER = "section_number";
				private static final String ARG_IS_TIPS = "is_tips";

				/**
				 * Returns a new instance of this fragment for the given section number.
				 */
				public static PlaceholderFragment newInstance(int sectionNumber, boolean tips) {
						PlaceholderFragment fragment = new PlaceholderFragment();
						Bundle args = new Bundle();
						args.putInt(ARG_SECTION_NUMBER, sectionNumber);
						args.putBoolean(ARG_IS_TIPS, tips);

						fragment.setArguments(args);
						return fragment;
				}

				public PlaceholderFragment() {
				}

				@Override
				public View onCreateView(LayoutInflater inflater, ViewGroup container,
																 Bundle savedInstanceState) {
						View rootView = inflater.inflate(R.layout.fragment_tour, container,
										false);
						int displayHeight = getActivity().getWindowManager().getDefaultDisplay().getHeight();
						int displayWidth = getActivity().getWindowManager().getDefaultDisplay().getWidth();

//			Log.d("PLACE_HOLDER_FRAGMENT","On Create View: "+getTag());

						CirclePageIndicator mIndicator = (CirclePageIndicator) rootView.findViewById(R.id.pagerIndicator);
						mIndicator.setRadius(16);
						mIndicator.setViewPager(mViewPager);

						TextView title = (TextView) rootView.findViewById(R.id.section_label);
						TextView question = (TextView) rootView.findViewById(R.id.question_label);
						Button blue = (Button) rootView.findViewById(R.id.button_blue);
						Button white = (Button) rootView.findViewById(R.id.button_white);
						Button skip = (Button) rootView.findViewById(R.id.button_skip);
						Button gotIt = (Button) rootView.findViewById(R.id.button_gotIt);

						ImageView background = (ImageView) rootView.findViewById(R.id.imageBackground);
						Bitmap bgBitmap = null;

						switch (getArguments().getInt(ARG_SECTION_NUMBER)) {
								case 0:
										title.setText(getActivity().getResources().getString(R.string.intro_flow_screen_1));
										if (getActivity() != null && Device.getInstance(getActivity()).isMixpanelEnabled()) {
												MixpanelAPI.getInstance(getActivity(), MIXPANEL_TOKEN).track("Intro Flow Screen 1", null);
										}
										blue.setVisibility(View.GONE);
										white.setVisibility(View.GONE);
										skip.setVisibility(View.GONE);
										question.setVisibility(View.GONE);
										gotIt.setVisibility(View.GONE);
										bgBitmap = decodeSampledBitmapFromResource(getActivity().getResources(), R.drawable.basejumper, displayWidth, displayHeight);
										background.setImageBitmap(bgBitmap);
										bgBitmap = null;
										break;
								case 1:
										title.setText(getActivity().getResources().getString(R.string.intro_flow_screen_2));
										if (getActivity() != null && Device.getInstance(getActivity()).isMixpanelEnabled()) {
												MixpanelAPI.getInstance(getActivity(), MIXPANEL_TOKEN).track("Intro Flow Screen 2", null);
										}
										blue.setVisibility(View.GONE);
										white.setVisibility(View.GONE);
										skip.setVisibility(View.GONE);
										question.setVisibility(View.GONE);
										gotIt.setVisibility(View.GONE);
										bgBitmap = decodeSampledBitmapFromResource(getActivity().getResources(), R.drawable.map, displayWidth, displayHeight);
										background.setImageBitmap(bgBitmap);
										bgBitmap = null;
										break;
								case 2:
										title.setVisibility(View.GONE);
										if (getActivity() != null && Device.getInstance(getActivity()).isMixpanelEnabled()) {
												MixpanelAPI.getInstance(getActivity(), MIXPANEL_TOKEN).track("Intro Flow Register Screen", null);
										}
										blue.setText(R.string.register_title_signup);
										blue.setOnClickListener(new OnClickListener() {
												@Override
												public void onClick(View v) {
														Intent i = new Intent(getActivity(), LoginActivity.class);
														i.putExtra("position", 1);
														startActivityForResult(i, SIGNUP_REQUEST);
//							getActivity().finish();
												}
										});
										white.setText(R.string.register_title_login);
										white.setOnClickListener(new OnClickListener() {
												@Override
												public void onClick(View v) {
														Intent i = new Intent(getActivity(), LoginActivity.class);
														i.putExtra("position", 2);
														startActivityForResult(i, LOGIN_REQUEST);
//							getActivity().finish();
												}
										});
										skip.setText(R.string.intro_flow_skip_button);
										question.setVisibility(View.GONE);
										gotIt.setVisibility(View.GONE);
										skip.setOnClickListener(new OnClickListener() {
												@Override
												public void onClick(View v) {
														if (getActivity() != null && Device.getInstance(getActivity()).isMixpanelEnabled()) {
																MixpanelAPI.getInstance(getActivity(), MIXPANEL_TOKEN).track("Intro Flow Clicked Skip", null);
														}
														Intent i = new Intent(getActivity(), TourActivity.class);
														i.putExtra("startPosition", 3);
														i.putExtra("numberPages", 1);
														startActivity(i);
														getActivity().finish();
												}
										});
										bgBitmap = decodeSampledBitmapFromResource(getActivity().getResources(), R.drawable.sign_up, displayWidth, displayHeight);
										background.setImageBitmap(bgBitmap);
										bgBitmap = null;
										break;
								case 3:
										if (getActivity() != null && Device.getInstance(getActivity()).isMixpanelEnabled()) {
												MixpanelAPI.getInstance(getActivity(), MIXPANEL_TOKEN).track("Intro Flow Have Wind Meter Screen", null);
										}
										title.setVisibility(View.GONE);
										blue.setText(R.string.intro_flow_yes_button);
										blue.setOnClickListener(new OnClickListener() {
												@Override
												public void onClick(View v) {
														Intent i = new Intent(getActivity(), TourActivity.class);
														i.putExtra("startPosition", 5);
														i.putExtra("numberPages", 4);
//							i.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
														startActivity(i);
														getActivity().finish();
												}
										});
										white.setText(R.string.intro_flow_no_button);
										white.setOnClickListener(new OnClickListener() {
												@Override
												public void onClick(View v) {
														Intent i = new Intent(getActivity(), TourActivity.class);
														i.putExtra("startPosition", 4);
														i.putExtra("numberPages", 1);
														startActivity(i);
														getActivity().finish();
												}
										});
										skip.setVisibility(View.GONE);
										mIndicator.setVisibility(View.GONE);
										question.setText(R.string.intro_flow_have_a_wind_meter);
										gotIt.setVisibility(View.GONE);
										bgBitmap = decodeSampledBitmapFromResource(getActivity().getResources(), R.drawable.wind_meter, displayWidth, displayHeight);
										background.setImageBitmap(bgBitmap);
										bgBitmap = null;
										break;
								case 4:
										if (getActivity() != null && Device.getInstance(getActivity()).isMixpanelEnabled()) {
												MixpanelAPI.getInstance(getActivity(), MIXPANEL_TOKEN).track("Intro Flow Buy Screen", null);
										}
										title.setVisibility(View.GONE);
										blue.setText(R.string.intro_flow_yes_button);
										blue.setOnClickListener(new OnClickListener() {
												@Override
												public void onClick(View v) {
														if (getActivity() != null && Device.getInstance(getActivity()).isMixpanelEnabled()) {
																MixpanelAPI.getInstance(getActivity(), MIXPANEL_TOKEN).track("Intro Flow Clicked Buy", null);
														}
														TelephonyManager tm = (TelephonyManager) getActivity().getSystemService(Context.TELEPHONY_SERVICE);
														Intent i = null;
														if (getActivity() != null && Device.getInstance(getActivity()).isMixpanelEnabled()) {
																i = new Intent(Intent.ACTION_VIEW, Uri.parse("http://vaavud.com/mobile-shop-redirect/?country=" + tm.getNetworkCountryIso() + "&language=en&ref=" +
																				MixpanelAPI.getInstance(getActivity(), MIXPANEL_TOKEN).getDistinctId() + "&source=intro"));
														} else {
																i = new Intent(Intent.ACTION_VIEW, Uri.parse("http://vaavud.com/mobile-shop-redirect/?country=" + tm.getNetworkCountryIso() + "&language=en&source=intro"));
														}
														tm = null;
														startActivity(i);
														getActivity().finish();
												}
										});
										white.setText(R.string.intro_flow_later_button);
										white.setBackgroundColor(Color.TRANSPARENT);
										white.setOnClickListener(new OnClickListener() {
												@Override
												public void onClick(View v) {
														Intent i = new Intent(getActivity(), MainActivity.class);
														startActivity(i);
														getActivity().finish();
												}
										});
										skip.setVisibility(View.GONE);
										question.setText(R.string.intro_flow_want_buy);
										gotIt.setVisibility(View.GONE);
										mIndicator.setVisibility(View.GONE);
										bgBitmap = decodeSampledBitmapFromResource(getActivity().getResources(), R.drawable.wind_meter, displayWidth, displayHeight);
										background.setImageBitmap(bgBitmap);
										bgBitmap = null;
										break;
								case 5:
										title.setText(getActivity().getResources().getString(R.string.instruction_flow_screen_1));
										if (getActivity() != null && Device.getInstance(getActivity()).isMixpanelEnabled()) {
												MixpanelAPI.getInstance(getActivity(), MIXPANEL_TOKEN).track("Instruction Flow Screen 1", null);
										}
										blue.setVisibility(View.GONE);
										white.setVisibility(View.GONE);
										skip.setVisibility(View.GONE);
										question.setVisibility(View.GONE);
										gotIt.setVisibility(View.GONE);
										bgBitmap = decodeSampledBitmapFromResource(getActivity().getResources(), R.drawable.paraglider, displayWidth, displayHeight);
										background.setImageBitmap(bgBitmap);
										bgBitmap = null;
										break;
								case 6:
										title.setText(getActivity().getResources().getString(R.string.instruction_flow_screen_2));
										if (getActivity() != null && Device.getInstance(getActivity()).isMixpanelEnabled()) {
												MixpanelAPI.getInstance(getActivity(), MIXPANEL_TOKEN).track("Instruction Flow Screen 2", null);
										}
										blue.setVisibility(View.GONE);
										white.setVisibility(View.GONE);
										skip.setVisibility(View.GONE);
										question.setVisibility(View.GONE);
										gotIt.setVisibility(View.GONE);
										bgBitmap = decodeSampledBitmapFromResource(getActivity().getResources(), R.drawable.hold_top, displayWidth, displayHeight);
										background.setImageBitmap(bgBitmap);
										bgBitmap = null;
										break;
								case 7:
										title.setText(getActivity().getResources().getString(R.string.instruction_flow_screen_3));
										if (getActivity() != null && Device.getInstance(getActivity()).isMixpanelEnabled()) {
												MixpanelAPI.getInstance(getActivity(), MIXPANEL_TOKEN).track("Instruction Flow Screen 3", null);
										}
										blue.setVisibility(View.GONE);
										white.setVisibility(View.GONE);
										skip.setVisibility(View.GONE);
										question.setVisibility(View.GONE);
										gotIt.setVisibility(View.GONE);
										bgBitmap = decodeSampledBitmapFromResource(getActivity().getResources(), R.drawable.open_space, displayWidth, displayHeight);
										background.setImageBitmap(bgBitmap);
										bgBitmap = null;
										break;
								case 8:
										title.setText(getActivity().getResources().getString(R.string.instruction_flow_screen_4));
										if (getActivity() != null && Device.getInstance(getActivity()).isMixpanelEnabled()) {
												MixpanelAPI.getInstance(getActivity(), MIXPANEL_TOKEN).track("Instruction Flow Screen 4", null);
										}
										blue.setVisibility(View.GONE);
										white.setVisibility(View.GONE);
										skip.setVisibility(View.GONE);
										question.setVisibility(View.GONE);
										gotIt.setText(getActivity().getResources().getString(R.string.intro_flow_got_it_button));
										gotIt.setOnClickListener(new OnClickListener() {
												@Override
												public void onClick(View v) {
														((VaavudApplication) getActivity().getApplication()).setIsFirstFlow(false);
														if (!getArguments().getBoolean(ARG_IS_TIPS)) {
																Intent i = new Intent(getActivity(), MainActivity.class);
																startActivity(i);
														}
														getActivity().finish();
												}
										});
										bgBitmap = decodeSampledBitmapFromResource(getActivity().getResources(), R.drawable.reading, displayWidth, displayHeight);
										background.setImageBitmap(bgBitmap);
										bgBitmap = null;
										break;
								default:
										break;
						}

						return rootView;
				}
		}

		private static Bitmap decodeSampledBitmapFromResource(Resources res, int resId,
																													int reqWidth, int reqHeight) {

				// First decode with inJustDecodeBounds=true to check dimensions
				final BitmapFactory.Options options = new BitmapFactory.Options();
				options.inJustDecodeBounds = true;
				options.inPurgeable = true;
//	    options.inPreferredConfig = Config.RGB_565;
				options.inPreferQualityOverSpeed = false;
				options.inScaled = true;
				options.inDensity = 0;

				BitmapFactory.decodeResource(res, resId, options);

				// Calculate inSampleSize
				options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);
				// Decode bitmap with inSampleSize set
				options.inJustDecodeBounds = false;
				return BitmapFactory.decodeResource(res, resId, options);
		}

		@Override
		protected void onActivityResult(int requestCode, int resultCode, Intent data) {
				// Check which request we're responding to
				super.onActivityResult(requestCode, resultCode, data);
				Log.d(TAG, "Request Code: " + requestCode + " Result Code: " + resultCode);
				if (requestCode == LOGIN_REQUEST || requestCode == SIGNUP_REQUEST) {
						// Make sure the request was successful
						if (resultCode == RESULT_OK) {
								Intent i = new Intent(this, TourActivity.class);
								i.putExtra("startPosition", 3);
								i.putExtra("numberPages", 1);
								startActivity(i);
								finish();
						}
				}
		}

		private static int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
				// Raw height and width of image
				final int height = options.outHeight;
				final int width = options.outWidth;
				int inSampleSize = 1;

				if (height > reqHeight || width > reqWidth) {

						// Calculate ratios of height and width to requested height and width
						final int heightRatio = Math.round((float) height / (float) reqHeight);
						final int widthRatio = Math.round((float) width / (float) reqWidth);

						// Choose the smallest ratio as inSampleSize value, this will guarantee
						// a final image with both dimensions larger than or equal to the
						// requested height and width.
						inSampleSize = heightRatio < widthRatio ? heightRatio : widthRatio;
				}

				return inSampleSize;
		}

}
