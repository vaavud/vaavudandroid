package com.vaavud.android.network.response;

import java.io.Serializable;

public class RegisterDeviceResponse implements Serializable {

	private String authToken;
	private Boolean uploadMagneticData;
	private Double frequencyStart;
	private Double frequencyFactor;
	private Float[] hourOptions;
	private Integer maxMapMarkers;
	private Long creationTime;
	private boolean enableMixpanel;
	private boolean enableMixpanelPeople;

	public String getAuthToken() {
		return authToken;
	}

	public void setAuthToken(String authToken) {
		this.authToken = authToken;
	}

	public Boolean getUploadMagneticData() {
		return uploadMagneticData;
	}

	public void setUploadMagneticData(Boolean uploadMagneticData) {
		this.uploadMagneticData = uploadMagneticData;
	}

	public Double getFrequencyStart() {
		return frequencyStart;
	}

	public void setFrequencyStart(Double frequencyStart) {
		this.frequencyStart = frequencyStart;
	}

	public Double getFrequencyFactor() {
		return frequencyFactor;
	}

	public void setFrequencyFactor(Double frequencyFactor) {
		this.frequencyFactor = frequencyFactor;
	}

	public Float[] getHourOptions() {
		return hourOptions;
	}

	public float[] getHourOptionsAsPrimitive() {
		if (hourOptions == null) {
			return null;
		}
		try {
			float[] result = new float[hourOptions.length];
			for (int i = 0; i < hourOptions.length; i++) {
				result[i] = hourOptions[i];
			}
			return result;
		}
		catch (RuntimeException e) {
			return null;
		}
	}

	public void setHourOptions(Float[] hourOptions) {
		this.hourOptions = hourOptions;
	}

	public Integer getMaxMapMarkers() {
		return maxMapMarkers;
	}

	public void setMaxMapMarkers(Integer maxMapMarkers) {
		this.maxMapMarkers = maxMapMarkers;
	}

	public Long getCreationTime() {
		return creationTime;
	}

	public void setCreationTime(Long creationTime) {
		this.creationTime = creationTime;
	}

	public boolean isEnableMixpanel() {
		return enableMixpanel;
	}

	public void setEnableMixpanel(boolean enableMixpanel) {
		this.enableMixpanel = enableMixpanel;
	}

	public boolean isEnableMixpanelPeople() {
		return enableMixpanelPeople;
	}

	public void setEnableMixpanelPeople(boolean enableMixpanelPeople) {
		this.enableMixpanelPeople = enableMixpanelPeople;
	}
}
