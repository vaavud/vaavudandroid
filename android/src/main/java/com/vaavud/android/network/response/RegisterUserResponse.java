package com.vaavud.android.network.response;

import java.io.Serializable;
import java.util.Date;

public class RegisterUserResponse implements Serializable{
	
	private String status;
	private String authToken;
	private Long userId;
	private String email;
	private String firstName;
	private	String lastName;
	private Boolean hasWindMeter;
	private Date creationTime;
	

	public String getStatus() {
		// TODO Auto-generated method stub
		return status;
	}
	public void setStatus(String status){
		this.status=status;
	}

	public String getAuthToken() {
		return authToken;
	}
	public void setAuthToken(String authToken){
		this.authToken=authToken;
	}

	public Long getUserId() {
		return userId;
	}
	public void setUserId(Long userId){
		this.userId=userId;
	}
	public String getEmail() {
		return email;
	}
	public void setEmail(String email) {
		this.email = email;
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
	public Boolean getHasWindMeter(){
		return hasWindMeter;
	}
	public void setHasWindMeter(Boolean hasWindMeter){
		this.hasWindMeter=hasWindMeter;
	}
	public Date getCreationTime() {
		return creationTime;
	}
	public void setCreationTime(Date creationTime) {
		this.creationTime = creationTime;
	}
	
}
