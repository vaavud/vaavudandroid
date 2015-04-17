package com.vaavud.android.network.request;

import java.io.Serializable;
import java.util.Date;

public class MeasurementsRequestParameters implements Serializable {
	
	private Date startTime;

	public MeasurementsRequestParameters(Date startTime) {
		this.startTime = startTime;
	}
	
	public Date getStartTime() {
		return startTime;
	}

	public void setStartTime(Date startTime) {
		this.startTime = startTime;
	}
}
