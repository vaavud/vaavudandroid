package com.vaavud.android.ui.login;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
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
import com.vaavud.android.model.entity.Device;
import com.vaavud.android.model.entity.User;
import com.vaavud.android.network.InternetManager;
import com.vaavud.android.network.UserManager;
import com.vaavud.android.network.listener.UserResponseListener;
import com.vaavud.android.network.response.RegisterUserResponse;
import com.vaavud.android.network.response.UserRequestStatus;
import com.vaavud.android.ui.BackPressedListener;
import com.vaavud.android.ui.MainActivity;
import com.vaavud.android.ui.SelectedListener;
import com.vaavud.android.ui.SelectorListener;
import com.vaavud.util.MixpanelUtil;
import com.vaavud.util.PasswordUtil;

import org.json.JSONException;
import org.json.JSONObject;

import java.security.InvalidKeyException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Pattern;

public class SignUpFragment extends Fragment implements UserResponseListener,BackPressedListener,SelectorListener,SelectedListener {
	
	private View view;
	private SelectorListener mCallback;
	
	private EditText userFirstName;
	private EditText userLastName;
	private EditText userEmail;
	private EditText userPassword;
	private Button signUpButton;
	
	private ProgressDialog progress;
	
	private UserResponseListener listener = (UserResponseListener)this;
	
	/*DEBUG TAG*/
	private static final String TAG = "SIGNUP_FRAGMENT";
	
	private static final long LOGIN_DELAY= 30000;
	
	private static final String MIXPANEL_TOKEN = "757f6311d315f94cdfc8d16fb4d973c0"; 
	
	/*FACEBOOK LOGIN STUFF*/

	private String fbAccessToken;
	private Date fbAccessTokenExp;
	private Boolean again=true;
	private static List<String> permissions;

	private Session.StatusCallback mStatusCallback = new Session.StatusCallback() {
		
		@Override
		public void call(Session session, SessionState state, Exception exception) {
//			Log.d(TAG,"SignUp Callback");
			if (exception != null) {
//				Log.d("Facebook", exception.getMessage());
				if (progress !=null && progress.isShowing()) progress.dismiss();
				return;
			}
//			Log.d("Facebook", "Session State: " + session.getState());
			if (state.equals(SessionState.OPENED_TOKEN_UPDATED)){
//				Log.d(TAG,"OPENED_TOKEN_UPDATE");
				return;
			}
			else if (state.isOpened()) {
				fbAccessToken=session.getAccessToken();
				fbAccessTokenExp=session.getExpirationDate();
				if (session.getDeclinedPermissions().size()>0){
			        if (again){
			        	again=false;
			        	session.requestNewReadPermissions(new NewPermissionsRequest(getActivity(), session.getDeclinedPermissions()));
			        }else{
			        	if (progress !=null && progress.isShowing()) progress.dismiss();
			        	Toast.makeText(getActivity(), getActivity().getResources().getString(R.string.register_feedback_invalid_credentials_title), Toast.LENGTH_LONG).show();
			        	fbAccessToken = null;
				        user.eraseDataBase(getActivity());
				        again=true;
			        }
				}else{
					// make request to the /me API
					Request.newMeRequest(session, new Request.GraphUserCallback() {
	
						// callback after Graph API response with user object
						@Override
						public void onCompleted(GraphUser user, Response response) {
	//						Log.d(TAG,"onCompleted");
							timerDelayRemoveProgressDialog(LOGIN_DELAY);
							fbSignUp(response);
						}
					}).executeAsync();
					Session.setActiveSession(session);
				}
			}else{
//		        Log.d(TAG, "Login Session State: " + session.getState());
		        fbAccessToken = null;
		        user.eraseDataBase(getActivity());
			}
			
		}
	};
 
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
	private Boolean userLogged=false;
	private UserManager userManager;

	/**
	 * Validate email wrt. RFC 822.
	 * <p>
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
		try {
	    	  mCallback = (SelectorListener) activity;
		}catch (ClassCastException e) {
			throw new ClassCastException(activity.toString() + " must implement SelectorListener");
		}
		
		progress = ((MainActivity)activity).getProgressDialog();
		
		listener = (UserResponseListener)this;
		
		userManager = UserManager.getInstance(activity,((MainActivity)activity).getRequestQueue(),user);

	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
//		mMixpanel = ((MainActivity)getActivity()).getMixPanel();
//		Log.i(TAG, "onCreate, savedInstanceState" + (savedInstanceState == null ? "=null" : "!=null"));
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		
		user = User.getInstance(getActivity());
		view = inflater.inflate(R.layout.fragment_signup, container, false);
		
		userFirstName = (EditText) view.findViewById(R.id.userFirstNametext);
		userLastName = (EditText) view.findViewById(R.id.userLastNametext);
	    userEmail = (EditText) view.findViewById(R.id.userEmailtext);
	    userPassword = (EditText) view.findViewById(R.id.userPasswordtext);
	    
	    
	    signUpButton = (Button) view.findViewById(R.id.signupButton);
	    signUpButton.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				if (!InternetManager.Check(getActivity())){
					Toast.makeText(getActivity(), getActivity().getResources().getString(R.string.register_feedback_no_reachability_message),
		                    Toast.LENGTH_LONG).show();
		            return;
				}
				if(isValid(userEmail.getText().toString()))
				{
					if (progress==null){
						progress=new ProgressDialog(getActivity());
						progress.setCancelable(false);
						progress.setMessage(getActivity().getResources().getString(R.string.register_feedback_conecting_server));
						((MainActivity)getActivity()).setProgressDialog(progress);
					}
					progress.show();
					emailSignUp();
					timerDelayRemoveProgressDialog(LOGIN_DELAY);
					//MixPanel
					if (getActivity()!=null && Device.getInstance(getActivity()).isMixpanelEnabled()){
						MixpanelAPI.getInstance(getActivity(),MIXPANEL_TOKEN).track("Email Sign Up",null);
					}
				}else{
					Toast.makeText(getActivity(), getActivity().getResources().getString(R.string.register_feedback_malformed_email_message),
							Toast.LENGTH_SHORT).show();
				}
			}
		});
	    
	    Button fbLoginButton = (Button) view.findViewById(R.id.fbAuthButton);
		/***** FB Permissions *****/
		permissions = new ArrayList<String>();
		permissions.add("email");
		permissions.add("public_profile");
		permissions.add("user_friends");
		/***** End FB Permissions *****/
		
		fbLoginButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				if (!InternetManager.Check(getActivity())){
					Toast.makeText(getActivity(), getActivity().getResources().getString(R.string.register_feedback_no_reachability_message),
		                    Toast.LENGTH_LONG).show();
		            return;
				}
//				Log.d(TAG,"On Click");
				if (progress==null){
					progress =new ProgressDialog(getActivity());
					progress.setCancelable(false);
					progress.setMessage(getActivity().getResources().getString(R.string.register_feedback_conecting_server));
					((MainActivity)getActivity()).setProgressDialog(progress);
				}
				progress.show();
				//MixPanel
				if (getActivity()!=null && Device.getInstance(getActivity()).isMixpanelEnabled()){
					MixpanelAPI.getInstance(getActivity(),MIXPANEL_TOKEN).track("Facebook Sign Up",null);
				}
				
				Session session = openActiveSession(getActivity(), true, permissions, mStatusCallback);
				if (session==null) progress.dismiss();
				else Session.setActiveSession(session);
				
			}
		});
	    
	    return view;
	}
	
	private static Session openActiveSession(Activity activity, boolean allowLoginUI, List<String> permissions, StatusCallback callback) {
		OpenRequest openRequest = new OpenRequest(activity).setPermissions(permissions).setCallback(callback);
		Session session = new Session.Builder(activity).build();
		
		if (SessionState.CREATED_TOKEN_LOADED.equals(session.getState()) || allowLoginUI) {
			Session.setActiveSession(session);
			session.openForRead(openRequest);
			return session;
		}
		return null;
	}
	
	public void timerDelayRemoveProgressDialog(long time) {
	    Handler handler = new Handler(); 
	    handler.postDelayed(new Runnable() {           
	        public void run() {
	        	if (((MainActivity)getActivity())!=null){
		        	ProgressDialog pd = ((MainActivity)getActivity()).getProgressDialog();
		        	if(pd!=null && pd.isShowing() && !userLogged){
	//	        		Log.d(TAG,"TimerDelayRemoveProgressDialog");
			            pd.dismiss();
			        	fbAccessToken = null;
				        user.eraseDataBase(getActivity());
				        userManager.cancelRequestQueue("userRequest");
				        Toast.makeText(getActivity(), getActivity().getResources().getString(R.string.register_feedback_invalid_credentials_title), Toast.LENGTH_LONG).show();
		        	}   
		        }
	        }
	    }, time); 
	}

	@Override
	public boolean onBackPressed() {
	    return true;
	}
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
//		Log.i(TAG, "onActivityCreated, savedInstanceState" + (savedInstanceState == null ? "=null" : "!=null"));
	}
	
	@Override
	public void onViewStateRestored(Bundle savedInstanceState) {
		super.onViewStateRestored(savedInstanceState);
	}
	
	@Override
	public void onSelected() {
		JSONObject props = new JSONObject();
		try {
			props.put("Screen", "Signup");
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		if (getActivity()!=null && Device.getInstance(getActivity()).isMixpanelEnabled()){
			MixpanelAPI.getInstance(getActivity(),MIXPANEL_TOKEN).track("Signup/Login Screen", props);
		}
//		Log.i(TAG, "onSelected");
	}

	@Override
	public void onStart() {
		super.onStart();
//		Log.i(TAG, "onStart");
	}

	@Override
	public void onResume() {
		super.onResume();
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
	public void onMenuOptionSelected(int position) {
		mCallback.onMenuOptionSelected(position);
	}
	
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
	    super.onActivityResult(requestCode, resultCode, data);
	}
	
	private void emailSignUp() {
		String passwd=":";
		user.setAction("SIGNUP");
		if (isValid(userEmail.getText().toString())){
			user.setEmail(userEmail.getText().toString());
		}else{
			Toast.makeText(getActivity(), getResources().getString(R.string.register_create_feedback_email_empty_message),Toast.LENGTH_SHORT).show();
			return;
		}
		
		if (userFirstName.getText().toString().length()>0){
			user.setFirstName(userFirstName.getText().toString());
		}else{
			Toast.makeText(getActivity(), getResources().getString(R.string.register_create_feedback_first_name_empty_message),Toast.LENGTH_SHORT).show();
			return;
		}
		user.setLastName(userLastName.getText().toString());
		if(userPassword.getText().toString().length()<4){
			Toast.makeText(getActivity(), getResources().getString(R.string.register_create_feedback_password_short_message),Toast.LENGTH_SHORT).show();
			return;
		}
		try {
			passwd = PasswordUtil.createHash(userPassword.getText().toString(),userEmail.getText().toString());
		} catch (InvalidKeyException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return;
		}
		user.setPasswordHash(passwd);
		
		userManager.registerUser(user,listener);
	}
	
	private void fbSignUp(Response fbResponse){
		if (fbResponse==null || fbResponse.getError()!=null){ 
			if (progress !=null && progress.isShowing()) progress.dismiss();
			return;
		}
		user.setAction("SIGNUP");
		if(fbResponse.getGraphObject().getProperty("email")!=null){
			user.setEmail(fbResponse.getGraphObject().getProperty("email").toString());
		}else{
			if (progress !=null && progress.isShowing()) progress.dismiss();
			return;
		}
		user.setFirstName(fbResponse.getGraphObject().getProperty("first_name").toString());
		user.setLastName(fbResponse.getGraphObject().getProperty("last_name").toString());
		user.setGender(Gender.valueOf(fbResponse.getGraphObject().getProperty("gender").toString().toUpperCase()));
		user.setFacebookAccessToken(fbAccessToken);
		user.setFacebookAccessTokenExp(fbAccessTokenExp);
		user.setFacebookAccessTokenExpCheck(new Date());
		user.setFacebookId(fbResponse.getGraphObject().getProperty("id").toString());
		
		userManager.registerUser(user,listener);
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
//			user.setCreationTime(new Date(object.getCreationTime()));
//			Log.d(TAG,"Creation Time From Server:"+ object.getCreationTime());
			user.setCreationTime(object.getCreationTime());
			if (getActivity()!=null && Device.getInstance(getActivity()).isMixpanelEnabled()){
				MixpanelAPI.getInstance(getActivity(),MIXPANEL_TOKEN).alias(user.getUserId().toString(), MixpanelAPI.getInstance(getActivity(), MIXPANEL_TOKEN).getDistinctId());
			}
			validated=true;
			break;
		case EMAIL_USED_PROVIDE_PASSWORD:
//			Log.d(TAG, "EMAIL_USED_PROVIDE_PASSWORD");
			user.setEmail(object.getEmail());
			if (Session.getActiveSession()!=null) Session.getActiveSession().closeAndClearTokenInformation();

			LayoutInflater li = LayoutInflater.from(getActivity());
			View promptsView = li.inflate(R.layout.dialog_prompt_password, null);

			AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(
					getActivity());

			alertDialogBuilder.setView(promptsView);

			final EditText userInput = (EditText) promptsView
					.findViewById(R.id.userPasswordtext);
			
			alertDialogBuilder
				.setCancelable(false)
				.setPositiveButton(getResources().getString(R.string.button_ok),
					new DialogInterface.OnClickListener() {
					    public void onClick(DialogInterface dialog,int id) {
					    	if(userInput.getText()!=null && user.getEmail()!=null){
								try {
									user.setPasswordHash(PasswordUtil.createHash(userInput.getText().toString(),user.getEmail()).split(":")[2]);
								} catch (InvalidKeyException e) {
									e.printStackTrace();
								}
								((MainActivity)getActivity()).getUserManager().loginUser(user,listener);
					    	}else{
					    		dialog.cancel();
					    	}
					    }
					})
				.setNegativeButton(getResources().getString(R.string.button_cancel),
					new DialogInterface.OnClickListener() {
					    public void onClick(DialogInterface dialog,int id) {
					    	dialog.cancel();
					    }
					});
			AlertDialog alertDialog = alertDialogBuilder.create();
			alertDialog.show();
			break;
		case INVALID_CREDENTIALS:
//			Log.d(TAG, "INVALID_CREDENTIALS");
			if (Session.getActiveSession()!=null) Session.getActiveSession().closeAndClearTokenInformation();
			Toast.makeText(getActivity(),getResources().getString(R.string.register_feedback_invalid_credentials_title),Toast.LENGTH_LONG).show();
			validated=false;
			break;
		case INVALID_FACEBOOK_ACCESS_TOKEN:
//			Log.d(TAG, "INVALID_FACEBOOK_ACCESS_TOKEN");
			if (Session.getActiveSession()!=null) Session.getActiveSession().closeAndClearTokenInformation();
			Toast.makeText(getActivity(), getResources().getString(R.string.register_feedback_invalid_credentials_title),Toast.LENGTH_LONG).show();
			validated=false;
			break;
		case LOGIN_WITH_FACEBOOK:
//			Log.d(TAG, "LOGIN_WITH_FACEBOOK");
			Toast.makeText(getActivity(), getResources().getString(R.string.register_feedback_account_exists_login_with_facebook),Toast.LENGTH_LONG).show();
			validated=false;
			break;
		case MALFORMED_EMAIL:
//			Log.d(TAG, "MALFORMED_EMAIL");
			Toast.makeText(getActivity(), getResources().getString(R.string.register_feedback_malformed_email_title),Toast.LENGTH_LONG).show();
			validated=false;
			break;
		case PAIRED:
//			Log.d(TAG, "PAIRED");
			user.setAuthToken(object.getAuthToken());
			user.setUserId(object.getUserId());
			user.setEmail(object.getEmail());
			user.setFirstName(object.getFirstName());
			user.setLastName(object.getLastName());
			user.setHasWindMeter(object.getHasWindMeter());
//			user.setCreationTime(new Date(object.getCreationTime()));
//			Log.d(TAG,"Creation Time From Server:"+ object.getCreationTime());
			user.setCreationTime(object.getCreationTime());
			validated=true;
			break;
		default:
			validated=false;
			break;
		}
		if (validated){
			userLogged=true;
			if (getActivity()!=null && Device.getInstance(getActivity()).isMixpanelEnabled()){
				MixpanelAPI.getInstance(getActivity(),MIXPANEL_TOKEN).identify(user.getUserId().toString());
			}
			MixpanelUtil.registerUserAsMixpanelProfile(getActivity(), user);
			user.setDataBase(getActivity());
			onMenuOptionSelected(2);
		}else{
			if (progress !=null && progress.isShowing()) progress.dismiss();
			MixpanelUtil.registerUserErrorToMixpanel(getActivity(),user,status.ordinal(),"Signup");
			user.eraseDataBase(getActivity());
		}
	}

	@Override
	public void ErrorResponseReceived(String errorText) {
		Toast.makeText(getActivity(), getString(R.string.register_feedback_no_reachability_title), Toast.LENGTH_SHORT).show();
		if (progress !=null && progress.isShowing()) progress.dismiss();
		if (Session.getActiveSession() !=null) Session.getActiveSession().closeAndClearTokenInformation();
	}
}
