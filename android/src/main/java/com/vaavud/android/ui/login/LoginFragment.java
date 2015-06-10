package com.vaavud.android.ui.login;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.facebook.Request;
import com.facebook.Response;
import com.facebook.Session;
import com.facebook.Session.NewPermissionsRequest;
import com.facebook.Session.OpenRequest;
import com.facebook.Session.StatusCallback;
import com.facebook.SessionState;
import com.facebook.model.GraphUser;
import com.google.ads.AdRequest.Gender;
import com.mixpanel.android.mpmetrics.MixpanelAPI;
import com.vaavud.android.R;
import com.vaavud.android.model.VaavudDatabase;
import com.vaavud.android.model.entity.Device;
import com.vaavud.android.model.entity.MeasurementSession;
import com.vaavud.android.model.entity.User;
import com.vaavud.android.network.InternetManager;
import com.vaavud.android.network.UploadManager;
import com.vaavud.android.network.UserManager;
import com.vaavud.android.network.listener.HistoryMeasurementsResponseListener;
import com.vaavud.android.network.listener.UserResponseListener;
import com.vaavud.android.network.response.RegisterUserResponse;
import com.vaavud.android.network.response.UserRequestStatus;
import com.vaavud.android.ui.BackPressedListener;
import com.vaavud.util.MixpanelUtil;
import com.vaavud.util.PasswordUtil;

import org.json.JSONException;
import org.json.JSONObject;

import java.security.InvalidKeyException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Pattern;

public class LoginFragment extends Fragment implements UserResponseListener,HistoryMeasurementsResponseListener {

		private View view;
		private Context context;

		private EditText userEmail;
		private EditText userPassword;
		private Button loginButton;

		private UserResponseListener listener;

		private ProgressDialog progress;

		/*DEBUG TAG*/
		private static final String TAG = "LOGIN_FRAGMENT";

		private static final long LOGIN_DELAY = 30000;


		private static final String MIXPANEL_TOKEN = "757f6311d315f94cdfc8d16fb4d973c0";
	
	/*FACEBOOK LOGIN STUFF*/

		private String fbAccessToken;
		private Date fbAccessTokenExp;
		private Boolean again = true;
		private static List<String> permissions;

		private LoginStatusCallback mStatusCallback = null;

		private class LoginStatusCallback implements StatusCallback {

				@Override
				public void call(Session session, SessionState state, Exception exception) {
//			Log.d(TAG,"LogIn Callback");
						if (exception != null) {
//				Log.d("LOGIN_FRAGMENT", exception.getMessage());
								if (progress != null && progress.isShowing()) progress.dismiss();
								return;
						}
//			Log.d(TAG, "Login Session State: " + session.getState());
						if (state.equals(SessionState.OPENED_TOKEN_UPDATED)) {
//				Log.d(TAG,"OPENED_TOKEN_UPDATE");
								return;
						} else if (state.isOpened()) {
								fbAccessToken = session.getAccessToken();
								fbAccessTokenExp = session.getExpirationDate();
//								Log.d(TAG, "Session State: " + session.getState()+ "Permisions: "+ session.getPermissions());
								if (session.getDeclinedPermissions().size() > 0 && !session.getDeclinedPermissions().contains("publish_actions")) {
										if (again) {
												again = false;
												session.requestNewReadPermissions(new NewPermissionsRequest((Activity) context, session.getDeclinedPermissions()));
										} else {
												if (progress != null && progress.isShowing()) progress.dismiss();
												Toast.makeText(context, context.getResources().getString(R.string.register_feedback_invalid_credentials_title), Toast.LENGTH_LONG).show();
												fbAccessToken = null;
												user.eraseDataBase(context.getApplicationContext());
												again = true;
										}
								} else {
										// make request to the /me API
//					Log.d(TAG, "State is Opened: " + session.getState());
										Request.newMeRequest(session, new Request.GraphUserCallback() {
												// callback after Graph API response with user object
												@Override
												public void onCompleted(GraphUser user, Response response) {
//							Log.d(TAG,"Facebook Log In");
														timerDelayRemoveProgressDialog(LOGIN_DELAY);
														fbLogIn(response);
												}
										}).executeAsync();
										Session.setActiveSession(session);
								}
						} else {
//				Log.d(TAG, "Login Session State: " + session.getState());
								fbAccessToken = null;
								user.eraseDataBase(context.getApplicationContext());
						}
				}
		}


		/* This validator uses the regular expression taken from the Perl implementation of RFC 822.
			 * @see <a href="http://www.ex-parrot.com/~pdw/Mail-RFC822-Address.html">Perl Regex implementation *
			 *      of RFC 822< /a>
			 * @see <a href="http://www.ietf.org/rfc/rfc0822.txt?number=822">RFC 822< /a>
			 */
		private static final String EMAIL_PATTERN = "(?:(?:\\r\\n)?[ \\t])*(?:(?:(?:[^()<>@,;:\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t]"
						+ ")+|\\Z|(?=[\\[\"()<>@,;:\\\".\\[\\]]))|\"(?:[^\\\"\\r\\\\]|\\\\.|(?:(?:\\r\\n)?[ \\t]))*\"(?:(?:"
						+ "\\r\\n)?[ \\t])*)(?:\\.(?:(?:\\r\\n)?[ \\t])*(?:[^()<>@,;:\\\".\\[\\] \\000-\\031]+(?:(?:("
						+ "?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\".\\[\\]]))|\"(?:[^\\\"\\r\\\\]|\\\\.|(?:(?:\\r\\n)?[ "
						+ "\\t]))*\"(?:(?:\\r\\n)?[ \\t])*))*@(?:(?:\\r\\n)?[ \\t])*(?:[^()<>@,;:\\\".\\[\\] \\000-\\0"
						+ "31]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\".\\[\\]]))|\\[([^\\[\\]\\r\\\\]|\\\\.)*\\"
						+ "](?:(?:\\r\\n)?[ \\t])*)(?:\\.(?:(?:\\r\\n)?[ \\t])*(?:[^()<>@,;:\\\".\\[\\] \\000-\\031]+"
						+ "(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\".\\[\\]]))|\\[([^\\[\\]\\r\\\\]|\\\\.)*\\](?:"
						+ "(?:\\r\\n)?[ \\t])*))*|(?:[^()<>@,;:\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z"
						+ "|(?=[\\[\"()<>@,;:\\\".\\[\\]]))|\"(?:[^\\\"\\r\\\\]|\\\\.|(?:(?:\\r\\n)?[ \\t]))*\"(?:(?:\\r\\n)"
						+ "?[ \\t])*)*\\<(?:(?:\\r\\n)?[ \\t])*(?:@(?:[^()<>@,;:\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\"
						+ "r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\".\\[\\]]))|\\[([^\\[\\]\\r\\\\]|\\\\.)*\\](?:(?:\\r\\n)?["
						+ " \\t])*)(?:\\.(?:(?:\\r\\n)?[ \\t])*(?:[^()<>@,;:\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)"
						+ "?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\".\\[\\]]))|\\[([^\\[\\]\\r\\\\]|\\\\.)*\\](?:(?:\\r\\n)?[ \\t]"
						+ ")*))*(?:,@(?:(?:\\r\\n)?[ \\t])*(?:[^()<>@,;:\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?["
						+ " \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\".\\[\\]]))|\\[([^\\[\\]\\r\\\\]|\\\\.)*\\](?:(?:\\r\\n)?[ \\t])*"
						+ ")(?:\\.(?:(?:\\r\\n)?[ \\t])*(?:[^()<>@,;:\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t]"
						+ ")+|\\Z|(?=[\\[\"()<>@,;:\\\".\\[\\]]))|\\[([^\\[\\]\\r\\\\]|\\\\.)*\\](?:(?:\\r\\n)?[ \\t])*))*)"
						+ "*:(?:(?:\\r\\n)?[ \\t])*)?(?:[^()<>@,;:\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+"
						+ "|\\Z|(?=[\\[\"()<>@,;:\\\".\\[\\]]))|\"(?:[^\\\"\\r\\\\]|\\\\.|(?:(?:\\r\\n)?[ \\t]))*\"(?:(?:\\r"
						+ "\\n)?[ \\t])*)(?:\\.(?:(?:\\r\\n)?[ \\t])*(?:[^()<>@,;:\\\".\\[\\] \\000-\\031]+(?:(?:(?:"
						+ "\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\".\\[\\]]))|\"(?:[^\\\"\\r\\\\]|\\\\.|(?:(?:\\r\\n)?[ \\t"
						+ "]))*\"(?:(?:\\r\\n)?[ \\t])*))*@(?:(?:\\r\\n)?[ \\t])*(?:[^()<>@,;:\\\".\\[\\] \\000-\\031"
						+ "]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\".\\[\\]]))|\\[([^\\[\\]\\r\\\\]|\\\\.)*\\]("
						+ "?:(?:\\r\\n)?[ \\t])*)(?:\\.(?:(?:\\r\\n)?[ \\t])*(?:[^()<>@,;:\\\".\\[\\] \\000-\\031]+(?"
						+ ":(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\".\\[\\]]))|\\[([^\\[\\]\\r\\\\]|\\\\.)*\\](?:(?"
						+ ":\\r\\n)?[ \\t])*))*\\>(?:(?:\\r\\n)?[ \\t])*)|(?:[^()<>@,;:\\\".\\[\\] \\000-\\031]+(?:(?"
						+ ":(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\".\\[\\]]))|\"(?:[^\\\"\\r\\\\]|\\\\.|(?:(?:\\r\\n)?"
						+ "[ \\t]))*\"(?:(?:\\r\\n)?[ \\t])*)*:(?:(?:\\r\\n)?[ \\t])*(?:(?:(?:[^()<>@,;:\\\".\\[\\] "
						+ "\\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\".\\[\\]]))|\"(?:[^\\\"\\r\\\\]|"
						+ "\\\\.|(?:(?:\\r\\n)?[ \\t]))*\"(?:(?:\\r\\n)?[ \\t])*)(?:\\.(?:(?:\\r\\n)?[ \\t])*(?:[^()<>"
						+ "@,;:\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\".\\[\\]]))|\""
						+ "(?:[^\\\"\\r\\\\]|\\\\.|(?:(?:\\r\\n)?[ \\t]))*\"(?:(?:\\r\\n)?[ \\t])*))*@(?:(?:\\r\\n)?[ \\t]"
						+ ")*(?:[^()<>@,;:\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\"
						+ "\".\\[\\]]))|\\[([^\\[\\]\\r\\\\]|\\\\.)*\\](?:(?:\\r\\n)?[ \\t])*)(?:\\.(?:(?:\\r\\n)?[ \\t])*(?"
						+ ":[^()<>@,;:\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\".\\["
						+ "\\]]))|\\[([^\\[\\]\\r\\\\]|\\\\.)*\\](?:(?:\\r\\n)?[ \\t])*))*|(?:[^()<>@,;:\\\".\\[\\] \\000-"
						+ "\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\".\\[\\]]))|\"(?:[^\\\"\\r\\\\]|\\\\.|("
						+ "?:(?:\\r\\n)?[ \\t]))*\"(?:(?:\\r\\n)?[ \\t])*)*\\<(?:(?:\\r\\n)?[ \\t])*(?:@(?:[^()<>@,;"
						+ ":\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\".\\[\\]]))|\\[(["
						+ "^\\[\\]\\r\\\\]|\\\\.)*\\](?:(?:\\r\\n)?[ \\t])*)(?:\\.(?:(?:\\r\\n)?[ \\t])*(?:[^()<>@,;:\\\""
						+ ".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\".\\[\\]]))|\\[([^\\[\\"
						+ "]\\r\\\\]|\\\\.)*\\](?:(?:\\r\\n)?[ \\t])*))*(?:,@(?:(?:\\r\\n)?[ \\t])*(?:[^()<>@,;:\\\".\\"
						+ "[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\".\\[\\]]))|\\[([^\\[\\]\\"
						+ "r\\\\]|\\\\.)*\\](?:(?:\\r\\n)?[ \\t])*)(?:\\.(?:(?:\\r\\n)?[ \\t])*(?:[^()<>@,;:\\\".\\[\\] "
						+ "\\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\".\\[\\]]))|\\[([^\\[\\]\\r\\\\]"
						+ "|\\\\.)*\\](?:(?:\\r\\n)?[ \\t])*))*)*:(?:(?:\\r\\n)?[ \\t])*)?(?:[^()<>@,;:\\\".\\[\\] \\0"
						+ "00-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\".\\[\\]]))|\"(?:[^\\\"\\r\\\\]|\\\\"
						+ ".|(?:(?:\\r\\n)?[ \\t]))*\"(?:(?:\\r\\n)?[ \\t])*)(?:\\.(?:(?:\\r\\n)?[ \\t])*(?:[^()<>@,"
						+ ";:\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\".\\[\\]]))|\"(?"
						+ ":[^\\\"\\r\\\\]|\\\\.|(?:(?:\\r\\n)?[ \\t]))*\"(?:(?:\\r\\n)?[ \\t])*))*@(?:(?:\\r\\n)?[ \\t])*"
						+ "(?:[^()<>@,;:\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\"."
						+ "\\[\\]]))|\\[([^\\[\\]\\r\\\\]|\\\\.)*\\](?:(?:\\r\\n)?[ \\t])*)(?:\\.(?:(?:\\r\\n)?[ \\t])*(?:["
						+ "^()<>@,;:\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\".\\[\\]"
						+ "]))|\\[([^\\[\\]\\r\\\\]|\\\\.)*\\](?:(?:\\r\\n)?[ \\t])*))*\\>(?:(?:\\r\\n)?[ \\t])*)(?:,\\s*("
						+ "?:(?:[^()<>@,;:\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\"
						+ "\".\\[\\]]))|\"(?:[^\\\"\\r\\\\]|\\\\.|(?:(?:\\r\\n)?[ \\t]))*\"(?:(?:\\r\\n)?[ \\t])*)(?:\\.(?:("
						+ "?:\\r\\n)?[ \\t])*(?:[^()<>@,;:\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=["
						+ "\\[\"()<>@,;:\\\".\\[\\]]))|\"(?:[^\\\"\\r\\\\]|\\\\.|(?:(?:\\r\\n)?[ \\t]))*\"(?:(?:\\r\\n)?[ \\t"
						+ "])*))*@(?:(?:\\r\\n)?[ \\t])*(?:[^()<>@,;:\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t"
						+ "])+|\\Z|(?=[\\[\"()<>@,;:\\\".\\[\\]]))|\\[([^\\[\\]\\r\\\\]|\\\\.)*\\](?:(?:\\r\\n)?[ \\t])*)(?"
						+ ":\\.(?:(?:\\r\\n)?[ \\t])*(?:[^()<>@,;:\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|"
						+ "\\Z|(?=[\\[\"()<>@,;:\\\".\\[\\]]))|\\[([^\\[\\]\\r\\\\]|\\\\.)*\\](?:(?:\\r\\n)?[ \\t])*))*|(?:"
						+ "[^()<>@,;:\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\".\\[\\"
						+ "]]))|\"(?:[^\\\"\\r\\\\]|\\\\.|(?:(?:\\r\\n)?[ \\t]))*\"(?:(?:\\r\\n)?[ \\t])*)*\\<(?:(?:\\r\\n)"
						+ "?[ \\t])*(?:@(?:[^()<>@,;:\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\""
						+ "()<>@,;:\\\".\\[\\]]))|\\[([^\\[\\]\\r\\\\]|\\\\.)*\\](?:(?:\\r\\n)?[ \\t])*)(?:\\.(?:(?:\\r\\n)"
						+ "?[ \\t])*(?:[^()<>@,;:\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>"
						+ "@,;:\\\".\\[\\]]))|\\[([^\\[\\]\\r\\\\]|\\\\.)*\\](?:(?:\\r\\n)?[ \\t])*))*(?:,@(?:(?:\\r\\n)?["
						+ " \\t])*(?:[^()<>@,;:\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,"
						+ ";:\\\".\\[\\]]))|\\[([^\\[\\]\\r\\\\]|\\\\.)*\\](?:(?:\\r\\n)?[ \\t])*)(?:\\.(?:(?:\\r\\n)?[ \\t]"
						+ ")*(?:[^()<>@,;:\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\"
						+ "\".\\[\\]]))|\\[([^\\[\\]\\r\\\\]|\\\\.)*\\](?:(?:\\r\\n)?[ \\t])*))*)*:(?:(?:\\r\\n)?[ \\t])*)?"
						+ "(?:[^()<>@,;:\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\"."
						+ "\\[\\]]))|\"(?:[^\\\"\\r\\\\]|\\\\.|(?:(?:\\r\\n)?[ \\t]))*\"(?:(?:\\r\\n)?[ \\t])*)(?:\\.(?:(?:"
						+ "\\r\\n)?[ \\t])*(?:[^()<>@,;:\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\["
						+ "\"()<>@,;:\\\".\\[\\]]))|\"(?:[^\\\"\\r\\\\]|\\\\.|(?:(?:\\r\\n)?[ \\t]))*\"(?:(?:\\r\\n)?[ \\t])"
						+ "*))*@(?:(?:\\r\\n)?[ \\t])*(?:[^()<>@,;:\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])"
						+ "+|\\Z|(?=[\\[\"()<>@,;:\\\".\\[\\]]))|\\[([^\\[\\]\\r\\\\]|\\\\.)*\\](?:(?:\\r\\n)?[ \\t])*)(?:\\"
						+ ".(?:(?:\\r\\n)?[ \\t])*(?:[^()<>@,;:\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z"
						+ "|(?=[\\[\"()<>@,;:\\\".\\[\\]]))|\\[([^\\[\\]\\r\\\\]|\\\\.)*\\](?:(?:\\r\\n)?[ \\t])*))*\\>(?:("
						+ "?:\\r\\n)?[ \\t])*))*)?;\\s*)";

		private static final Pattern PATTERN = Pattern.compile(EMAIL_PATTERN);

		private User user;
		private Device device;
		private Boolean userLogged = false;
		private UserManager userManager;
		private UploadManager uploadManager;


		/**
		 * Validate email wrt. RFC 822.
		 * <p/>
		 * The check is as follows:<p>
		 * 1. null check<p>
		 * 2. leading/trailing whitespace check<p>
		 * 3. rfc check
		 *
		 * @param email in question
		 * @return validity of email address
		 */
		public static boolean isValid(String email) {
				return email != null &&
								email.length() == email.trim().length() &&
								PATTERN.matcher(email).matches();
		}

		@Override
		public void onAttach(Activity activity) {
				super.onAttach(activity);
				context = activity;
				listener = (UserResponseListener) this;
		}

		@Override
		public void onCreate(Bundle savedInstanceState) {
				super.onCreate(savedInstanceState);

				user = User.getInstance(context.getApplicationContext());
				userManager = UserManager.getInstance(context.getApplicationContext());
				uploadManager = UploadManager.getInstance(context.getApplicationContext());
				device = Device.getInstance(context.getApplicationContext());

				if (device.getAuthToken()==null){
						uploadManager.registerDevice(false);
				}
				JSONObject props = new JSONObject();
				try {
						props.put("Screen", "Login");
				} catch (JSONException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
				}
				if (context != null && device.isMixpanelEnabled()) {
						MixpanelAPI.getInstance(context.getApplicationContext(), MIXPANEL_TOKEN).track("Signup/Login Screen", props);
				}
//		Log.i(TAG, "onCreate, savedInstanceState" + (savedInstanceState == null ? "=null" : "!=null"));
		}

		@Override
		public View onCreateView(LayoutInflater inflater, final ViewGroup container, Bundle savedInstanceState) {


				view = inflater.inflate(R.layout.fragment_login, container, false);
//		Log.d(TAG,"OnCreateView");

				userEmail = (EditText) view.findViewById(R.id.userEmailtext);
				userPassword = (EditText) view.findViewById(R.id.userPasswordtext);


				loginButton = (Button) view.findViewById(R.id.loginButton);
				loginButton.setOnClickListener(new OnClickListener() {

						@Override
						public void onClick(View v) {
								if (!InternetManager.Check(context)) {
										Toast.makeText(context, context.getResources().getString(R.string.register_feedback_no_reachability_message),
														Toast.LENGTH_LONG).show();
										return;
								}

								if (isValid(userEmail.getText().toString())) {
										if (progress == null) {
												progress = new ProgressDialog(context);
												progress.setCancelable(false);
												progress.setMessage(context.getResources().getString(R.string.register_feedback_conecting_server));
//												((MainActivity) context).setProgressDialog(progress);
										}
										progress.show();
										emailLogIn();
										timerDelayRemoveProgressDialog(LOGIN_DELAY);
										//MixPanel
										if (context != null && device.isMixpanelEnabled()) {
												MixpanelAPI.getInstance(context.getApplicationContext(), MIXPANEL_TOKEN).track("Email Login", null);
										}
								} else {
										Toast.makeText(context, context.getResources().getString(R.string.register_feedback_malformed_email_message),
														Toast.LENGTH_SHORT).show();
								}

						}
				});

				Button fbLoginButton = (Button) view.findViewById(R.id.fbLoginButton);
				/***** FB Permissions *****/
				permissions = new ArrayList<String>();
				permissions.add("email");
				permissions.add("public_profile");
				permissions.add("user_friends");
				/***** End FB Permissions *****/

				fbLoginButton.setOnClickListener(new OnClickListener() {

						@Override
						public void onClick(View v) {
								if (!InternetManager.Check(context)) {
										Toast.makeText(context, context.getResources().getString(R.string.register_feedback_no_reachability_message),
														Toast.LENGTH_LONG).show();
										return;
								}
								if (progress == null) {
										progress = new ProgressDialog(context);
										progress.setCancelable(false);
										progress.setMessage(context.getResources().getString(R.string.register_feedback_conecting_server));
//					((MainActivity)context).setProgressDialog(progress);
								}
								progress.show();
								//MixPanel
								if (context != null && device.isMixpanelEnabled()) {
										MixpanelAPI.getInstance(context.getApplicationContext(), MIXPANEL_TOKEN).track("Facebook Login", null);
								}
								mStatusCallback = new LoginStatusCallback();
//								Session session = Session.openActiveSession(context.getApplicationContext(),getParentFragment(),true,permissions,mStatusCallback);
								Session session = openActiveSession((Activity)context, true, permissions, mStatusCallback);
								if (session == null) progress.dismiss();
								else Session.setActiveSession(session);
						}
				});

				return view;
		}

		private Session openActiveSession(Activity activity, boolean allowLoginUI, List<String> permissions, StatusCallback callback) {
				OpenRequest openRequest = new OpenRequest(activity).setPermissions(permissions).setCallback(callback);
				Session session = new Session.Builder(activity).build();

				if (SessionState.CREATED_TOKEN_LOADED.equals(session.getState()) || allowLoginUI) {
						Session.setActiveSession(session);
						session.openForRead(openRequest);
						return session;
				} else if (SessionState.OPENED.equals(session.getState())) {
//			Log.d(TAG,"OpenActiveSession Session State:"+session.getState());
						return session;
				}
				return null;
		}

		private void timerDelayRemoveProgressDialog(long time) {
				Handler handler = new Handler();
				handler.postDelayed(new Runnable() {
						public void run() {
								if (context != null) {

										if (progress != null && progress.isShowing() && !userLogged) {
												progress.dismiss();
												fbAccessToken = null;
//												Log.d("LoginFragment","Login Fragment Handler");
												user.eraseDataBase(context.getApplicationContext());
												userManager.cancelRequestQueue("userRequest");
												Toast.makeText(context, context.getResources().getString(R.string.register_feedback_invalid_credentials_title), Toast.LENGTH_LONG).show();
										}
								}
						}
				}, time);
		}

//		@Override
//		public boolean onBackPressed() {
//				return true;
//		}

		@Override
		public void onActivityCreated(Bundle savedInstanceState) {
				super.onActivityCreated(savedInstanceState);
		}

		@Override
		public void onViewStateRestored(Bundle savedInstanceState) {
				super.onViewStateRestored(savedInstanceState);
		}

		@Override
		public void onStart() {
				super.onStart();
//		Log.i(TAG, "onStart");
		}

		@Override
		public void onResume() {
				super.onResume();
//		Log.i(TAG, "onResume");
		}

		@Override
		public void onPause() {
				super.onPause();
//		Log.i(TAG, "onPause");
		}

		@Override
		public void onStop() {
				super.onStop();
//		Log.i(TAG, "onStop");
		}

		@Override
		public void onDestroyView() {
				super.onDestroyView();
//		Log.i(TAG, "onDestroyView");
		}

		@Override
		public void onDestroy() {
				super.onDestroy();
//		Log.i(TAG, "onDestroy");

		}

		@Override
		public void onDetach() {
				super.onDetach();
//		Log.i(TAG, "onDetach");
		}

		@Override
		public void onSaveInstanceState(Bundle outState) {
				super.onSaveInstanceState(outState);
//		Log.i(TAG, "onSaveInstanceState");
		}

		@Override
		public void onLowMemory() {
				super.onLowMemory();
//		Log.i(TAG, "onLowMemory");
		}


		@Override
		public void onActivityResult(int requestCode, int resultCode, Intent data) {
//		Log.d(TAG,"OnActivityResultFragment");
				super.onActivityResult(requestCode, resultCode, data);
		}

		private void emailLogIn() {
				String passwd = ":";

				user.setAction("LOGIN");
				if (isValid(userEmail.getText().toString())) {
						user.setEmail(userEmail.getText().toString());
				} else {
						Toast.makeText(context, getResources().getString(R.string.register_create_feedback_email_empty_message), Toast.LENGTH_SHORT).show();
						return;
				}
				try {
						passwd = PasswordUtil.createHash(userPassword.getText().toString(), userEmail.getText().toString());
				} catch (InvalidKeyException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
				}
				user.setPasswordHash(passwd);

				if (!userManager.loginUser(user, listener)) progress.cancel();
		}

		private void fbLogIn(Response fbResponse) {
				user.setAction("LOGIN");
				if (fbResponse == null || fbResponse.getError() != null) {
						if (progress != null && progress.isShowing()) progress.dismiss();
						return;
				}
				//Check if the email is given by facebook
				if (fbResponse.getGraphObject().getProperty("email") != null) {
						user.setEmail(fbResponse.getGraphObject().getProperty("email").toString());
				} else {
						if (progress != null && progress.isShowing()) progress.dismiss();
						return;
				}
				user.setFacebookId(fbResponse.getGraphObject().getProperty("id").toString());
				user.setFacebookAccessToken(fbAccessToken);
				user.setFacebookAccessTokenExp(fbAccessTokenExp);
				user.setFacebookAccessTokenExpCheck(new Date());
				user.setFirstName(fbResponse.getGraphObject().getProperty("first_name").toString());
				user.setLastName(fbResponse.getGraphObject().getProperty("last_name").toString());
				user.setGender(Gender.valueOf(fbResponse.getGraphObject().getProperty("gender").toString().toUpperCase()));
				user.setVerified((Boolean) fbResponse.getGraphObject().getProperty("verified"));

				if (!userManager.loginUser(user, listener)) progress.cancel();
		}

		@Override
		public void userResponseReceived(RegisterUserResponse object) {
				boolean validated = false;
//		Log.d(TAG,"User Received");
				UserRequestStatus status = UserRequestStatus.valueOf(object.getStatus());

				switch (status) {
						case CREATED:
//			Log.d(TAG, "CREATED");
								user.setAuthToken(object.getAuthToken());
								user.setUserId(object.getUserId());
								user.setEmail(object.getEmail());
								user.setFirstName(object.getFirstName());
								user.setLastName(object.getLastName());
								user.setHasWindMeter(object.getHasWindMeter());
								user.setCreationTime(object.getCreationTime());
								if (context != null && device.isMixpanelEnabled()) {
										MixpanelAPI.getInstance(context.getApplicationContext(), MIXPANEL_TOKEN).alias(user.getUserId().toString(), MixpanelAPI.getInstance(context.getApplicationContext(), MIXPANEL_TOKEN).getDistinctId());
								}
								validated = true;
								break;
						case EMAIL_USED_PROVIDE_PASSWORD:
//			Log.d(TAG, "EMAIL_USED_PROVIDE_PASSWORD");
								user.setEmail(object.getEmail());
								if (Session.getActiveSession() != null)
										Session.getActiveSession().closeAndClearTokenInformation();

								LayoutInflater li = LayoutInflater.from(context);
								View promptsView = li.inflate(R.layout.dialog_prompt_password, null);

								AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(
												context);

								alertDialogBuilder.setView(promptsView);

								final EditText userInput = (EditText) promptsView
												.findViewById(R.id.userPasswordtext);
								alertDialogBuilder.setCancelable(false)
												.setPositiveButton(context.getResources().getString(R.string.button_ok),
																new DialogInterface.OnClickListener() {
																		public void onClick(DialogInterface dialog, int id) {
																				if (userInput.getText() != null && user.getEmail() != null) {
																						try {
																								user.setPasswordHash(PasswordUtil.createHash(userInput.getText().toString(), user.getEmail()).split(":")[2]);
																						} catch (InvalidKeyException e) {
																								e.printStackTrace();
																						}
																						if(userManager.loginUser(user, listener)) progress.cancel();
																				} else {
																						dialog.cancel();
																				}
																		}
																})
												.setNegativeButton(context.getResources().getString(R.string.button_cancel),
																new DialogInterface.OnClickListener() {
																		public void onClick(DialogInterface dialog, int id) {
																				dialog.cancel();
																		}
																});
								AlertDialog alertDialog = alertDialogBuilder.create();
								alertDialog.show();
								break;

						case INVALID_CREDENTIALS:
//			Log.d(TAG, "INVALID_CREDENTIALS");
								if (Session.getActiveSession() != null)
										Session.getActiveSession().closeAndClearTokenInformation();
								Toast.makeText(context, context.getResources().getString(R.string.register_feedback_invalid_credentials_message),
												Toast.LENGTH_LONG).show();
								validated = false;
								break;

						case INVALID_FACEBOOK_ACCESS_TOKEN:
//			Log.d(TAG, "INVALID_FACEBOOK_ACCESS_TOKEN");
								if (Session.getActiveSession() != null)
										Session.getActiveSession().closeAndClearTokenInformation();
								Toast.makeText(context, context.getResources().getString(R.string.register_feedback_invalid_credentials_message),
												Toast.LENGTH_LONG).show();
								validated = false;
								break;

						case LOGIN_WITH_FACEBOOK:
//			Log.d(TAG, "LOGIN_WITH_FACEBOOK");
								Toast.makeText(context, context.getResources().getString(R.string.register_feedback_account_exists_login_with_facebook),
												Toast.LENGTH_LONG).show();
								validated = false;
								break;

						case MALFORMED_EMAIL:
//			Log.d(TAG, "MALFORMED_EMAIL");
								if (Session.getActiveSession() != null)
										Session.getActiveSession().closeAndClearTokenInformation();
								Toast.makeText(context, context.getResources().getString(R.string.register_feedback_malformed_email_title),
												Toast.LENGTH_LONG).show();
								validated = false;
								break;

						case PAIRED:
//			Log.d(TAG, "PAIRED");
								user.setAuthToken(object.getAuthToken());
								user.setUserId(object.getUserId());
								user.setEmail(object.getEmail());
								user.setFirstName(object.getFirstName());
								user.setLastName(object.getLastName());
								user.setHasWindMeter(object.getHasWindMeter());
								user.setCreationTime(object.getCreationTime());
								validated = true;
								break;
						default:
								validated = false;
								break;
				}
				if (validated) {
						userLogged = true;
						if (context != null && device.isMixpanelEnabled()) {
								MixpanelAPI.getInstance(context.getApplicationContext(), MIXPANEL_TOKEN).identify(user.getUserId().toString());
						}
						MixpanelUtil.registerUserAsMixpanelProfile(context.getApplicationContext(), user);
						user.setDataBase(context.getApplicationContext());
						uploadManager.triggerReadHistoryMeasurements(new Date(0),null,this);

				} else {
						if (progress != null) progress.dismiss();
						MixpanelUtil.registerUserErrorToMixpanel(context, user, status.ordinal(), "Login");
						user.eraseDataBase(context.getApplicationContext());
				}
		}

		@Override
		public void measurementsLoadingFailed() {
//			Log.d(TAG, "Loading Failed");
		}

		@Override
		public void measurementsReceived(ArrayList<MeasurementSession> histObjList) {
//				Log.d(TAG,"Measurements Received");
				if (histObjList!=null && histObjList.size()>0){
						for (int i = 0; i < histObjList.size(); i++) {
								VaavudDatabase.getInstance(context.getApplicationContext()).insertMeasurementSession(histObjList.get(i));
						}
				}
				if (progress != null) progress.dismiss();
				if (((LoginActivity)getActivity()).getFromTour()){
						Intent returnIntent = new Intent();
						getActivity().setResult(Activity.RESULT_OK,returnIntent);
				}
				getActivity().finish();

		}
		@Override
		public void ErrorResponseReceived(String errorText) {
//			Log.d(TAG,"ErrorResponseReceived: "+errorText);
				if (context != null) Toast.makeText(context, getString(R.string.register_feedback_no_reachability_title), Toast.LENGTH_SHORT).show();
				if (progress != null && progress.isShowing()) progress.dismiss();
				if (Session.getActiveSession() != null) Session.getActiveSession().closeAndClearTokenInformation();
		}
}
