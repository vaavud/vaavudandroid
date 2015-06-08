package com.vaavud.android.model.entity;

import android.content.Context;
import android.util.Log;

import com.google.ads.AdRequest.Gender;
import com.vaavud.android.model.VaavudDatabase;

import java.io.Serializable;
import java.util.Date;


@SuppressWarnings("serial")
public class User implements Serializable {


		private static final String KEY_USER_AUTH_TOKEN = "userAuthToken";
		private static final String KEY_USER_ID = "userID";
		private static final String KEY_USER_EMAIL = "userEmail";
		private static final String KEY_USER_FIRST_NAME = "userFirstName";
		private static final String KEY_USER_LAST_NAME = "userLastName";
		private static final String KEY_USER_FB_ID = "facebookID";
		private static final String KEY_USER_FB_TOKEN = "facebookToken";
		private static final String KEY_USER_FB_TOKEN_EXP = "facebookTokenExp";
		private static final String KEY_USER_FB_TOKEN_CHECK = "facebookTokenCheck";
		private static final String KEY_USER_CREATION_DATE = "creationDate";
		private static final String KEY_HAS_WIND_METER = "hasWindMeter";


		private Long userId;
		private String action;
		private String authToken;
		private String email;
		private String clientPasswordHash;
		private String facebookId;
		private String facebookAccessToken;
		private String firstName;
		private String lastName;
		private Gender gender = Gender.UNKNOWN;
		private boolean verified = false;
		private Date facebookAccessTokenExp;
		private Date facebookAccessTokenExpCheck;
		private Boolean hasWindMeter = Boolean.FALSE;
		private Date creationTime;

		private static User instance;


		public static synchronized User getInstance(Context context) {
				if (instance == null) {
						instance = new User(context);
				}
				return instance;
		}

		private User() {
		}

		private User(Context context) {

				VaavudDatabase db = VaavudDatabase.getInstance(context);

				authToken = db.getProperty(KEY_USER_AUTH_TOKEN);
				userId = db.getPropertyAsLong(KEY_USER_ID);
				email = db.getProperty(KEY_USER_EMAIL);
				firstName = db.getProperty(KEY_USER_FIRST_NAME);
				lastName = db.getProperty(KEY_USER_LAST_NAME);
				facebookId = db.getProperty(KEY_USER_FB_ID);
				facebookAccessToken = db.getProperty(KEY_USER_FB_TOKEN);
				facebookAccessTokenExp = new Date(Long.valueOf(db.getProperty(KEY_USER_FB_TOKEN_EXP) != null ? db.getProperty(KEY_USER_FB_TOKEN_EXP) : "0"));
				facebookAccessTokenExpCheck = new Date(Long.valueOf(db.getProperty(KEY_USER_FB_TOKEN_CHECK) != null ? db.getProperty(KEY_USER_FB_TOKEN_CHECK) : "0"));
				creationTime = new Date(Long.valueOf(db.getProperty(KEY_USER_CREATION_DATE) != null ? db.getProperty(KEY_USER_CREATION_DATE) : "0"));
				hasWindMeter = db.getPropertyAsBoolean(KEY_HAS_WIND_METER);
		}

		public String getEmail() {
				return email;
		}

		public void setEmail(String email) {
				this.email = email;
		}

		public String getPasswordHash() {
				return clientPasswordHash;
		}

		public void setPasswordHash(String passwordHash) {
				this.clientPasswordHash = passwordHash;
		}

		public String getFacebookId() {
				return facebookId;
		}

		public void setFacebookId(String facebookId) {
				this.facebookId = facebookId;
		}

		public String getFacebookAccessToken() {
				return facebookAccessToken;
		}

		public void setFacebookAccessToken(String facebookAccessToken) {
//		Log.d("USER", "Set Facebook Access Token: "+facebookAccessToken);
				this.facebookAccessToken = facebookAccessToken;
		}

		public String getFirstName() {
				return firstName;
		}

		public void setFirstName(String firstName) {
				this.firstName = firstName;
		}

		public String getLastName() {
				return lastName;
		}

		public void setLastName(String lastName) {
				this.lastName = lastName;
		}

		public Gender getGender() {
				return gender;
		}

		public void setGender(Gender gender) {
				this.gender = gender;
		}

		public boolean isVerified() {
				return verified;
		}

		public void setVerified(boolean verified) {
				this.verified = verified;
		}

		public void setAction(String action) {
				this.action = action;
		}

		public String getAction() {
				return action;
		}

		public Boolean getHasWindMeter() {
				if (hasWindMeter==null) return false;
				else return hasWindMeter;
		}

		public void setHasWindMeter(Boolean hasWindMeter) {
				this.hasWindMeter = hasWindMeter;
		}

		public void setDataBase(Context context) {

				VaavudDatabase db = VaavudDatabase.getInstance(context.getApplicationContext());

				db.setProperty(KEY_USER_AUTH_TOKEN, authToken);
				db.setPropertyAsLong(KEY_USER_ID, userId != null ? userId : 0);
				db.setProperty(KEY_USER_EMAIL, email);
				db.setProperty(KEY_USER_FIRST_NAME, firstName);
				db.setProperty(KEY_USER_LAST_NAME, lastName);
				db.setProperty(KEY_USER_FB_ID, facebookId);
				db.setProperty(KEY_USER_FB_TOKEN, facebookAccessToken);
				db.setPropertyAsLong(KEY_USER_FB_TOKEN_EXP, facebookAccessTokenExp != null ? facebookAccessTokenExp.getTime() : 0);
				db.setPropertyAsLong(KEY_USER_FB_TOKEN_CHECK, facebookAccessTokenExpCheck != null ? facebookAccessTokenExpCheck.getTime() : 0);
				db.setPropertyAsBoolean(KEY_HAS_WIND_METER, hasWindMeter);
				db.setPropertyAsLong(KEY_USER_CREATION_DATE, creationTime != null ? creationTime.getTime() : 0);
		}

		public void eraseDataBase(Context context) {

				VaavudDatabase db = VaavudDatabase.getInstance(context.getApplicationContext());

				userId = (long) 0;
				action = null;
				authToken = null;
				email = null;
				clientPasswordHash = null;
				facebookId = null;
				facebookAccessToken = null;
				firstName = null;
				lastName = null;
				gender = Gender.UNKNOWN;
				verified = false;
				facebookAccessTokenExp = null;
				facebookAccessTokenExpCheck = null;
				hasWindMeter = false;


				db.setProperty(KEY_USER_AUTH_TOKEN, null);
				db.setPropertyAsLong(KEY_USER_ID, null);
				db.setProperty(KEY_USER_EMAIL, null);
				db.setProperty(KEY_USER_FIRST_NAME, null);
				db.setProperty(KEY_USER_LAST_NAME, null);
				db.setProperty(KEY_USER_FB_ID, null);
				db.setProperty(KEY_USER_FB_TOKEN, null);
				db.setPropertyAsLong(KEY_USER_FB_TOKEN_EXP, (long) 0);
				db.setPropertyAsLong(KEY_USER_FB_TOKEN_CHECK, (long) 0);
				db.setPropertyAsBoolean(KEY_HAS_WIND_METER, false);

				db.close();


		}

		@Override
		public String toString() {
//		Log.d("User","User [action=" + action + ", email="+ email + ", clientPasswordHash=" + clientPasswordHash + ", facebookId="
//				+ facebookId + ", facebookAccessToken=" + facebookAccessToken
//				+ ", firstName="+ firstName + ", lastName=" + lastName +", gender="+gender+", verified="+verified+"]");
				return "User [action=" + action + ", email=" + email + ", clientPasswordHash=" + clientPasswordHash + ", facebookId="
								+ facebookId + ", facebookAccessToken=" + facebookAccessToken
								+ ", firstName=" + firstName + ", lastName=" + lastName + ", gender=" + gender + ", verified=" + verified + "]";
		}

		public String getAuthToken() {
				return authToken;
		}

		public void setAuthToken(String authToken) {
				this.authToken = authToken;
		}

		public Long getUserId() {
				return userId;
		}

		public void setUserId(Long userId) {
				this.userId = userId;
		}

		public void setFacebookAccessTokenExp(Date fbAccessTokenExp) {
				this.facebookAccessTokenExp = fbAccessTokenExp;
		}

		public Date getFacebookAccessTokenExp() {
				return facebookAccessTokenExp;
		}

		public void setFacebookAccessTokenExpCheck(Date fbAccessTokenExpCheck) {
				this.facebookAccessTokenExpCheck = fbAccessTokenExpCheck;
		}

		public Date getFacebookAccessTokenExpCheck() {
				return facebookAccessTokenExp;
		}

		public Date getCreationTime() {
				return creationTime;
		}

		public void setCreationTime(Date creationTime) {
				this.creationTime = creationTime;
		}


		public boolean isUserLogged() {
				Log.d("User","User email=+ "+getEmail());
				return (authToken != null && authToken.length() > 0
								&& email != null && email.length() > 0);
		}
}
