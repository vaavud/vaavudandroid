package com.vaavud.android.network.request;


import com.vaavud.android.model.entity.MeasurementSession;

import java.io.Serializable;

public class DeleteMeasurementRequestParameters implements Serializable{
		
		private String uuid;

		public DeleteMeasurementRequestParameters(MeasurementSession measurement) {
			this.setUuid(measurement.getUuid());
		}

		public String getUuid() {
			return uuid;
		}

		public void setUuid(String uuid) {
			this.uuid = uuid;
		}
		
		
}