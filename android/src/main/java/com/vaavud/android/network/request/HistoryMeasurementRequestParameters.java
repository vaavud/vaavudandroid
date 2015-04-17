package com.vaavud.android.network.request;

import java.io.Serializable;
import java.util.Date;

public class HistoryMeasurementRequestParameters implements Serializable {
		
		private Date latestEndTime;
		private String hash;

		public HistoryMeasurementRequestParameters(Date endTime,String hash) {
			this.latestEndTime = endTime;
			this.hash = hash;
		}
		
		public Date getEndTime() {
			return latestEndTime;
		}

		public void setEndTime(Date startTime) {
			this.latestEndTime = startTime;
		}

		public String getHash() {
			return hash;
		}

		public void setHash(String hash) {
			this.hash = hash;
		}
}
