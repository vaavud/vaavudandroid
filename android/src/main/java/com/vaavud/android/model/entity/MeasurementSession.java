package com.vaavud.android.model.entity;

import android.database.Cursor;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class MeasurementSession implements Serializable {

		private Long localId;
		private String uuid;
		private String device;
		private boolean measuring = false;
		private boolean uploaded = false;
		private int startIndex = 0;
		private int endIndex = 0;
		private Long timezoneOffset;
		private Date startTime;
		private Date endTime;
		private LatLng position;
		private Float windSpeedAvg;
		private Float windSpeedMax;
		private Float windDirection;
		private String source;
		private WindMeter windMeter = WindMeter.MJOLNIR;
		private List<MeasurementPoint> points = new ArrayList<MeasurementPoint>();

		public MeasurementSession() {
				super();
		}

		public MeasurementSession(Cursor cursor) {
				super();
				setLocalId(cursor.getLong(0));
				setUuid(cursor.getString(1));
				setDevice(cursor.getString(2));
				setMeasuring(cursor.getInt(3) == 1);
				setUploaded(cursor.getInt(4) == 1);
				if (!cursor.isNull(5)) {
						setStartIndex(cursor.getInt(5));
				}
				if (!cursor.isNull(6)) {
						setEndIndex(cursor.getInt(6));
				}
				if (!cursor.isNull(7)) {
						setTimezoneOffset(cursor.getLong(7));
				}
				if (!cursor.isNull(8)) {
						setStartTime(new Date(cursor.getLong(8)));
				}
				if (!cursor.isNull(9)) {
						setEndTime(new Date(cursor.getLong(9)));
				}
				if (!cursor.isNull(10) && !cursor.isNull(11)) {
						try {
//				Log.d("MEASUREMENT_SESSION","Measurement Latitude: "+cursor.getDouble(10)+" Measurement Longitude: "+cursor.getDouble(10));
								setPosition(new LatLng(cursor.getDouble(10), cursor.getDouble(11)));
						} catch (IllegalArgumentException e) {
//				Log.d("MEASUREMENT_SESSION","Is Null");
								//Log.e("MeasurementSession", "Invalid latitude or longitude: " + e.getMessage());
								// ignore and leave null
						}
				}
				if (!cursor.isNull(12)) {
						setWindSpeedAvg(cursor.getFloat(12));
				}
				if (!cursor.isNull(13)) {
						setWindSpeedMax(cursor.getFloat(13));
				}
				if (!cursor.isNull(14)) {
						setWindDirection(cursor.getFloat(14));
				}
				if (!cursor.isNull(15)) {
						setSource(cursor.getString(15));
				}
				if (!cursor.isNull(16)) {
						setWindMeter(WindMeter.values()[cursor.getInt(16)]);
				}
		}

		public Long getLocalId() {
				return localId;
		}

		public void setLocalId(Long id) {
				this.localId = id;
		}

		public String getUuid() {
				return uuid;
		}

		public void setUuid(String uuid) {
				this.uuid = uuid;
		}

		public String getDevice() {
				return device;
		}

		public void setDevice(String device) {
				this.device = device;
		}

		public int getStartIndex() {
				return startIndex;
		}

		public void setStartIndex(int startIndex) {
				this.startIndex = startIndex;
		}

		public int getEndIndex() {
				return endIndex;
		}

		public void setEndIndex(int endIndex) {
				this.endIndex = endIndex;
		}

		public boolean isMeasuring() {
				return measuring;
		}

		public void setMeasuring(boolean measuring) {
				this.measuring = measuring;
		}

		public boolean isUploaded() {
				return uploaded;
		}

		public void setUploaded(boolean uploaded) {
				this.uploaded = uploaded;
		}

		public Long getTimezoneOffset() {
				return timezoneOffset;
		}

		public void setTimezoneOffset(Long timezoneOffset) {
				this.timezoneOffset = timezoneOffset;
		}

		public Date getStartTime() {
				return startTime;
		}

		public void setStartTime(Date startTime) {
				this.startTime = startTime;
		}

		public Date getEndTime() {
				return endTime;
		}

		public void setEndTime(Date endTime) {
				this.endTime = endTime;
		}

		public LatLng getPosition() {
				return position;
		}

		public void setPosition(LatLng position) {
				this.position = position;
		}

		public Float getWindSpeedAvg() {
				return windSpeedAvg;
		}

		public void setWindSpeedAvg(Float windSpeedAvg) {
				this.windSpeedAvg = windSpeedAvg;
		}

		public Float getWindSpeedMax() {
				return windSpeedMax;
		}

		public void setWindSpeedMax(Float windSpeedMax) {
				this.windSpeedMax = windSpeedMax;
		}

		public Float getWindDirection() {
				return windDirection;
		}

		public void setWindDirection(Float windDirection) {
				this.windDirection = windDirection;
		}

		public String getSource() {
				return source;
		}

		public void setSource(String source) {
				this.source = source;
		}

		public List<MeasurementPoint> getPoints() {
				return points;
		}

		public void setPoints(List<MeasurementPoint> points) {
				this.points = points;
		}

		public boolean equals(Object object) {
				boolean result = false;
				if (object == null || object.getClass() != getClass()) {
						result = false;
//		    Log.d("MeasurementSession","Is not the same object");
				} else {
						MeasurementSession measurement = (MeasurementSession) object;
						if (this.uuid.compareTo(measurement.getUuid()) == 0) {
//		    	Log.d("MeasurementSession","this.uuid="+this.uuid+" employee.getUuid()="+measurement.getUuid());
								result = true;
						}
				}
				return result;
		}

		@Override
		public String toString() {
				return "MeasurementSession [localId=" + localId + ", uuid=" + uuid
								+ ", measuring=" + measuring + ", uploaded=" + uploaded
								+ ", startTime=" + startTime
								+ ", endTime=" + endTime + ", position=" + position
								+ ", windSpeedAvg=" + windSpeedAvg + ", windSpeedMax="
								+ windSpeedMax + ", windDirection=" + windDirection + "]";
		}

		public WindMeter getWindMeter() {
				return windMeter;
		}

		public void setWindMeter(WindMeter windMeter) {
				this.windMeter = windMeter;
		}
}
