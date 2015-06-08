package com.vaavud.android.model;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.vaavud.android.model.entity.MeasurementPoint;
import com.vaavud.android.model.entity.MeasurementSession;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class VaavudDatabase extends SQLiteOpenHelper {

		private static final String DATABASE_NAME = "vaavud.db";
		private static final int DATABASE_VERSION = 8;

		private static final String[] MEASUREMENT_SESSION_FIELDS = new String[]{
						"_id", "uuid", "device", "measuring", "uploaded", "startIndex", "endIndex",
						"timezoneOffset", "startTime", "endTime", "latitude", "longitude",
						"windSpeedAvg", "windSpeedMax", "windDirection", "source", "windMeter"};

		private static final String CREATE_TABLE_MEASUREMENT_SESSION =
						"CREATE TABLE MeasurementSession (" +
										"_id integer primary key autoincrement, " +
										"uuid text not null, " +
										"device text not null, " +
										"measuring integer not null, " +
										"uploaded integer not null, " +
										"startIndex integer, " +
										"endIndex integer, " +
										"timezoneOffset integer, " +
										"startTime integer, " +
										"endTime integer, " +
										"latitude real, " +
										"longitude real, " +
										"windSpeedAvg real, " +
										"windSpeedMax real, " +
										"windDirection real, " +
										"source text, " +
										"windMeter integer " +
										");";

		private static final String[] MEASUREMENT_POINT_FIELDS = new String[]{
						"_id", "session_id", "time", "windSpeed", "windDirection"};

		private static final String CREATE_TABLE_MEASUREMENT_POINT =
						"CREATE TABLE MeasurementPoint (" +
										"_id integer primary key autoincrement, " +
										"session_id integer not null, " +
										"time integer, " +
										"windSpeed real, " +
										"windDirection real " +
										");";

		private static final String CREATE_TABLE_PROPERTY =
						"CREATE TABLE Property (" +
										"_id integer primary key autoincrement, " +
										"name text not null, " +
										"value text " +
										");";

		private static VaavudDatabase instance;

		public static synchronized VaavudDatabase getInstance(Context context) {
				Log.d("VaavudDatabase", "Vaavud Database get Instance Context: " + context);
				if (instance == null) {
						instance = new VaavudDatabase(context.getApplicationContext());
				}
				return instance;
		}

		private Context mContext;

		private VaavudDatabase(Context context) {
				super(context, DATABASE_NAME, null, DATABASE_VERSION);
				Log.d("VaavudDatabase", "Vaavud Database Constructor Context: " + context);
				mContext = context.getApplicationContext();

		}

		@Override
		public void onCreate(SQLiteDatabase database) {
				database.execSQL(CREATE_TABLE_PROPERTY);
				database.execSQL(CREATE_TABLE_MEASUREMENT_SESSION);
				database.execSQL(CREATE_TABLE_MEASUREMENT_POINT);
		}

		@Override
		public void onUpgrade(SQLiteDatabase database, int oldVersion, int newVersion) {
				Log.w("VaavudDatabase", "Upgrading database from version " + oldVersion + " to " + newVersion);

				while (oldVersion < newVersion) {
						if (oldVersion == 5) {
								// upgrade from DB version 5 to DB version 6

								// drop foreign key constraint on MeasurementPoint since it is not being enforced and might cause trouble
								// when altering the database schema if for some reason it is enforced on some versions
								database.execSQL("ALTER TABLE MeasurementPoint RENAME TO MeasurementPoint_old;");
								database.execSQL("CREATE TABLE MeasurementPoint (_id integer primary key autoincrement, session_id integer not null, time integer, windSpeed real, windDirection real);");
								database.execSQL("INSERT INTO MeasurementPoint ('_id', session_id, time, windSpeed, windDirection) SELECT _id, session_id, time, windSpeed, windDirection FROM MeasurementPoint_old;");
								database.execSQL("DROP TABLE MeasurementPoint_old;");

//				// rename uploadedIndex to endIndex on MeasurementSession and add column startIndex (defaulting to 0)
								database.execSQL("ALTER TABLE MeasurementSession RENAME TO MeasurementSession_old;");
								database.execSQL("CREATE TABLE MeasurementSession (_id integer primary key autoincrement, uuid text not null, device text not null, measuring integer not null, uploaded integer not null, startIndex integer, endIndex integer, timezoneOffset integer, startTime integer, endTime integer, latitude real, longitude real, windSpeedAvg real, windSpeedMax real, windDirection real, source text);");
								database.execSQL("INSERT INTO MeasurementSession (_id, uuid, device, measuring, uploaded, startIndex, endIndex, timezoneOffset, startTime, endTime, latitude, longitude, windSpeedAvg, windSpeedMax, windDirection, source) SELECT _id, uuid, device, measuring, uploaded, 0, uploadedIndex, timezoneOffset, startTime, endTime, latitude, longitude, windSpeedAvg, windSpeedMax, windDirection, 'vaavud' FROM MeasurementSession_old;");
								database.execSQL("DROP TABLE MeasurementSession_old");
						}

						if (oldVersion == 7) {
								// Aimed at fix DB upgrade bug
								database.execSQL("ALTER TABLE MeasurementSession RENAME TO MeasurementSession_old;");
								database.execSQL("CREATE TABLE MeasurementSession (_id integer primary key autoincrement, uuid text not null, device text not null, measuring integer not null, uploaded integer not null, startIndex integer, endIndex integer, timezoneOffset integer, startTime integer, endTime integer, latitude real, longitude real, windSpeedAvg real, windSpeedMax real, windDirection real, source text, windMeter int);");
								database.execSQL("INSERT INTO MeasurementSession (_id, uuid, device, measuring, uploaded, startIndex, endIndex, timezoneOffset, startTime, endTime, latitude, longitude, windSpeedAvg, windSpeedMax, windDirection, source, windMeter) SELECT _id, uuid, device, measuring, uploaded, 0, endIndex, timezoneOffset, startTime, endTime, latitude, longitude, windSpeedAvg, windSpeedMax, windDirection, 'vaavud', '1' FROM MeasurementSession_old;");
								database.execSQL("DROP TABLE MeasurementSession_old");
						}

						oldVersion++;
				}
		}

		public String getProperty(String name) {
				String value = null;
				SQLiteDatabase db = null;
				Cursor cursor = null;
				try {
						db = getWritableDatabase();
						cursor = db.query("Property", new String[]{"value"}, "name=?", new String[]{name}, null, null, null);
						if (cursor.moveToFirst()) {
								value = cursor.getString(0);
						}
				} catch (RuntimeException e) {
						//Log.e("VaavudDatabase", e.getMessage(), e);
				} finally {
						if (cursor != null) {
								cursor.close();
						}
						if (db != null) {
								db.close();
						}
				}
				return value;
		}

		public void setProperty(String name, String value) {
				SQLiteDatabase db = null;
				Cursor cursor = null;
				try {
						ContentValues values = new ContentValues();
						values.put("name", name);
						values.put("value", value);

						db = getWritableDatabase();
						cursor = db.query("Property", new String[]{"value"}, "name=?", new String[]{name}, null, null, null);
						if (cursor.moveToFirst()) {
								// property already exists
								db.update("Property", values, "name=?", new String[]{name});
						} else {
								// new property
								db.insert("Property", null, values);
						}
				} catch (RuntimeException e) {
						//Log.e("VaavudDatabase", e.getMessage(), e);
				} finally {
						if (cursor != null) {
								cursor.close();
						}
						if (db != null) {
								db.close();
						}
				}
		}

		public Integer getPropertyAsInteger(String name) {
				try {
						String value = getProperty(name);
						return value == null ? null : Integer.parseInt(value);
				} catch (NumberFormatException e) {
						return null;
				}
		}

		public void setPropertyAsInteger(String name, Integer integerValue) {
				setProperty(name, (integerValue == null) ? null : integerValue.toString());
		}

		public Long getPropertyAsLong(String name) {
				try {
						String value = getProperty(name);
						return value == null ? null : Long.parseLong(value);
				} catch (NumberFormatException e) {
						return null;
				}
		}

		public void setPropertyAsLong(String name, Long longValue) {
				setProperty(name, (longValue == null) ? null : longValue.toString());
		}

		public void setPropertyAsFloat(String name, Float floatValue) {
				setProperty(name, (floatValue == null) ? null : floatValue.toString());
		}

		public Float getPropertyAsFloat(String name) {
				try {
						String value = getProperty(name);
						return value == null ? null : Float.parseFloat(value);
				} catch (NumberFormatException e) {
						return null;
				}
		}

		public Double getPropertyAsDouble(String name) {
				try {
						String value = getProperty(name);
						return value == null ? null : Double.parseDouble(value);
				} catch (NumberFormatException e) {
						return null;
				}
		}

		public void setPropertyAsDouble(String name, Double doubleValue) {
				setProperty(name, (doubleValue == null) ? null : doubleValue.toString());
		}

		public Boolean getPropertyAsBoolean(String name) {
				String value = getProperty(name);
				return value == null ? null : Boolean.parseBoolean(value);
		}

		public void setPropertyAsBoolean(String name, Boolean booleanValue) {
				setProperty(name, (booleanValue == null) ? null : booleanValue.toString());
		}

		public <E extends Enum<E>> E getPropertyAsEnum(String name, Class<E> type) {
				try {
						String value = getProperty(name);
						return value == null ? null : Enum.valueOf(type, value);
				} catch (NumberFormatException e) {
						return null;
				}
		}

		public <E extends Enum<E>> void setPropertyAsEnum(String name, E enumValue) {
				setProperty(name, (enumValue == null) ? null : enumValue.name());
		}

		public float[] getPropertyAsFloatArray(String name) {
				try {
						String value = getProperty(name);
						if (value == null) {
								return null;
						}
						value = value.replace("[", "").replace("]", "");
						String[] values = value.split(",");
						float[] result = new float[values.length];
						for (int i = 0; i < values.length; i++) {
								String v = values[i];
								result[i] = Float.parseFloat(v);
						}
						return result;
				} catch (RuntimeException e) {
						Log.e("VaavudDatabase", "Error reading float array property", e);
						return null;
				}
		}

		public Float[] getPropertyAsFloatObjectArray(String name) {
				try {
						String value = getProperty(name);
						if (value == null) {
								return null;
						}
						value = value.replace("[", "").replace("]", "");
						String[] values = value.split(",");
						Float[] result = new Float[values.length];
						for (int i = 0; i < values.length; i++) {
								String v = values[i];
								result[i] = Float.parseFloat(v);
						}
						return result;
				} catch (RuntimeException e) {
						Log.e("VaavudDatabase", "Error reading float array property", e);
						return null;
				}
		}

		public void setPropertyAsFloatArray(String name, float[] array) {
				setProperty(name, (array == null) ? null : Arrays.toString(array));
		}

		public void setPropertyAsFloatObjectArray(String name, Float[] array) {
				setProperty(name, (array == null) ? null : Arrays.toString(array));
		}

		public void deleteTable(String tableName) {
				executeDelete(tableName, null, null);

		}


		public void deleteMeasurementSession(MeasurementSession measurementSession) {
				final String KEY_NAME = "_id";
				executeDelete("MeasurementSession", KEY_NAME + "=?", new String[]{measurementSession.getLocalId().toString()});
		}

		/**
		 * Inserts the specified MeasurementSession which must have a null ID into the database.
		 * After successfully inserting the object, the ID will be set.
		 */
		public void insertMeasurementSession(MeasurementSession measurementSession) {

				if (measurementSession.getLocalId() != null) {
						throw new IllegalArgumentException("Object shouldn't have local ID when inserting");
				}

				ContentValues values = new ContentValues();
				values.put("uuid", measurementSession.getUuid());
				values.put("device", measurementSession.getDevice());
				values.put("measuring", measurementSession.isMeasuring() ? 1 : 0);
				values.put("uploaded", measurementSession.isUploaded() ? 1 : 0);
				values.put("startIndex", measurementSession.getStartIndex());
				values.put("endIndex", measurementSession.getEndIndex());
				values.put("timezoneOffset", measurementSession.getTimezoneOffset());
				values.put("startTime", (measurementSession.getStartTime() == null) ? null : measurementSession.getStartTime().getTime());
				values.put("endTime", (measurementSession.getEndTime() == null) ? null : measurementSession.getEndTime().getTime());
				values.put("latitude", (measurementSession.getPosition() == null) ? null : measurementSession.getPosition().getLatitude());
				values.put("longitude", (measurementSession.getPosition() == null) ? null : measurementSession.getPosition().getLongitude());
				values.put("windSpeedAvg", measurementSession.getWindSpeedAvg());
				values.put("windSpeedMax", measurementSession.getWindSpeedMax());
				values.put("windDirection", measurementSession.getWindDirection());
				values.put("windMeter", measurementSession.getWindMeter().ordinal());

				long id = executeInsert("MeasurementSession", values);
				if (id != -1) {
						measurementSession.setLocalId(id);
				}
		}

		/**
		 * Updates the specified MeasurementSession which must have a non-null ID in the database.
		 */
		public void updateCompleteMeasurementSession(MeasurementSession measurementSession) {

				if (measurementSession.getLocalId() == null) {
						throw new IllegalArgumentException("Cannot update object without a local ID");
				}

				ContentValues values = new ContentValues();
				values.put("uuid", measurementSession.getUuid());
				values.put("device", measurementSession.getDevice());
				values.put("measuring", measurementSession.isMeasuring() ? 1 : 0);
				values.put("uploaded", measurementSession.isUploaded() ? 1 : 0);
				values.put("startIndex", measurementSession.getStartIndex());
				values.put("endIndex", measurementSession.getEndIndex());
				values.put("timezoneOffset", measurementSession.getTimezoneOffset());
				values.put("startTime", (measurementSession.getStartTime() == null) ? null : measurementSession.getStartTime().getTime());
				values.put("endTime", (measurementSession.getEndTime() == null) ? null : measurementSession.getEndTime().getTime());
				values.put("latitude", (measurementSession.getPosition() == null) ? null : measurementSession.getPosition().getLatitude());
				values.put("longitude", (measurementSession.getPosition() == null) ? null : measurementSession.getPosition().getLongitude());
				values.put("windSpeedAvg", measurementSession.getWindSpeedAvg());
				values.put("windSpeedMax", measurementSession.getWindSpeedMax());
				values.put("windDirection", measurementSession.getWindDirection());
				values.put("windMeter", measurementSession.getWindMeter().ordinal());

				executeUpdate("MeasurementSession", values, measurementSession.getLocalId());
		}

		public void updateDynamicMeasurementSession(MeasurementSession measurementSession) {

				if (measurementSession.getLocalId() == null) {
						throw new IllegalArgumentException("Cannot update object without a local ID");
				}

				ContentValues values = new ContentValues();
				values.put("endTime", (measurementSession.getEndTime() == null) ? null : measurementSession.getEndTime().getTime());
				values.put("latitude", (measurementSession.getPosition() == null) ? null : measurementSession.getPosition().getLatitude());
				values.put("longitude", (measurementSession.getPosition() == null) ? null : measurementSession.getPosition().getLongitude());
				values.put("windSpeedAvg", measurementSession.getWindSpeedAvg());
				values.put("windSpeedMax", measurementSession.getWindSpeedMax());
				values.put("windDirection", measurementSession.getWindDirection());
				values.put("windMeter", measurementSession.getWindMeter().ordinal());

				executeUpdate("MeasurementSession", values, measurementSession.getLocalId());
		}

		public void updateUploadingMeasurementSession(MeasurementSession measurementSession) {

				if (measurementSession.getLocalId() == null) {
						throw new IllegalArgumentException("Cannot update object without a local ID");
				}

				ContentValues values = new ContentValues();
				values.put("measuring", measurementSession.isMeasuring() ? 1 : 0);
				values.put("uploaded", measurementSession.isUploaded() ? 1 : 0);
				values.put("startIndex", measurementSession.getStartIndex());
				values.put("endIndex", measurementSession.getEndIndex());

				executeUpdate("MeasurementSession", values, measurementSession.getLocalId());
		}

		public void insertMeasurementPoint(MeasurementPoint measurementPoint) {

				if (measurementPoint.getLocalId() != null) {
						throw new IllegalArgumentException("Object shouldn't have local ID when inserting");
				}

				if (measurementPoint.getSession() == null) {
						throw new IllegalArgumentException("MeasurementPoint must be associated with a MeasurementSession to get inserted in the database");
				}

				if (measurementPoint.getSession().getLocalId() == null) {
						throw new IllegalArgumentException("MeasurementSession associated with MeasurementPoint has null local ID");
				}

				ContentValues values = new ContentValues();
				values.put("session_id", measurementPoint.getSession().getLocalId());
				values.put("time", (measurementPoint.getTime() == null) ? null : measurementPoint.getTime().getTime());
				values.put("windSpeed", measurementPoint.getWindSpeed());
				values.put("windDirection", measurementPoint.getWindDirection());

				long id = executeInsert("MeasurementPoint", values);
				if (id != -1) {
						measurementPoint.setLocalId(id);
				}
		}

		public MeasurementSession getMeasurementSession(Long id) {
				if (id == null) {
						return null;
				}

				MeasurementSession value = null;
				SQLiteDatabase db = null;
				Cursor cursor = null;
				try {
						db = getWritableDatabase();
						cursor = db.query("MeasurementSession", MEASUREMENT_SESSION_FIELDS, "_id=?", new String[]{id.toString()}, null, null, null);
						if (cursor.moveToFirst()) {
								value = new MeasurementSession(cursor);
						}
				} catch (RuntimeException e) {
						//Log.e("VaavudDatabase", e.getMessage(), e);
				} finally {
						if (cursor != null) {
								cursor.close();
						}
						if (db != null) {
								db.close();
						}
				}
				return value;
		}

		public List<MeasurementSession> getUnUploadedMeasurementSessions() {
				List<MeasurementSession> measurementSessions = new ArrayList<MeasurementSession>();
				SQLiteDatabase db = null;
				Cursor cursor = null;
				try {
						db = getWritableDatabase();
						cursor = db.query("MeasurementSession", MEASUREMENT_SESSION_FIELDS, "uploaded=0", null, null, null, null);
						while (cursor.moveToNext()) {
								measurementSessions.add(new MeasurementSession(cursor));
						}
				} catch (RuntimeException e) {
						//Log.e("VaavudDatabase", e.getMessage(), e);
				} finally {
						if (cursor != null) {
								cursor.close();
						}
						if (db != null) {
								db.close();
						}
				}
				return measurementSessions;
		}


		public List<MeasurementSession> getMeasurementSessions() {
				List<MeasurementSession> measurementSessions = new ArrayList<MeasurementSession>();
				SQLiteDatabase db = null;
				Cursor cursor = null;
				try {
						db = getWritableDatabase();
						cursor = db.query("MeasurementSession", MEASUREMENT_SESSION_FIELDS, null, null, null, null, null);
						while (cursor.moveToNext()) {
								measurementSessions.add(new MeasurementSession(cursor));
						}
				} catch (RuntimeException e) {
						Log.e("VaavudDatabase", e.getMessage(), e);
				} finally {
						if (cursor != null) {
								cursor.close();
						}
						if (db != null) {
								db.close();
						}
				}
				return measurementSessions;
		}


		public List<MeasurementPoint> getMeasurementPoints(MeasurementSession measurementSession, Integer offset, boolean setSessionOnPoints) {
				List<MeasurementPoint> measurementPoints = new ArrayList<MeasurementPoint>();

				if (measurementSession.getLocalId() == null) {
						return measurementPoints;
				}

				SQLiteDatabase db = null;
				Cursor cursor = null;
				try {
						db = getWritableDatabase();
						cursor = db.rawQuery("select _id, session_id, time, windSpeed, windDirection from MeasurementPoint where session_id=? order by _id limit 600 offset ?", new String[]{measurementSession.getLocalId().toString(), offset == null ? "0" : offset.toString()});
						while (cursor.moveToNext()) {
								measurementPoints.add(new MeasurementPoint(setSessionOnPoints ? measurementSession : null, cursor));
						}
				} catch (RuntimeException e) {
						//Log.e("VaavudDatabase", e.getMessage(), e);
				} finally {
						if (cursor != null) {
								cursor.close();
						}
						if (db != null) {
								db.close();
						}
				}
				return measurementPoints;
		}

		private long executeInsert(String table, ContentValues values) {

				SQLiteDatabase db = null;
				long id = -1;
				try {
						db = getWritableDatabase();
						id = db.insert(table, null, values);
				} catch (RuntimeException e) {
						//Log.e("VaavudDatabase", e.getMessage(), e);
				} finally {
						if (db != null) {
								db.close();
						}
				}
				return id;
		}


		private long executeDelete(String table, String whereClause, String[] whereArgs) {

				SQLiteDatabase db = null;
				long id = -1;
				try {
						db = getWritableDatabase();
						id = db.delete(table, whereClause, whereArgs);
				} catch (RuntimeException e) {
						//Log.e("VaavudDatabase", e.getMessage(), e);
				} finally {
						if (db != null) {
								db.close();
						}
				}
				return id;
		}

		private void executeUpdate(String table, ContentValues values, long id) throws SQLException {

				SQLiteDatabase db = null;
				try {
						db = getWritableDatabase();
						int rowsUpdated = db.update(table, values, "_id=?", new String[]{String.valueOf(id)});
						if (rowsUpdated == 0) {
								//Log.w("VaavudDatabase", "Expected to update one row but found none matching ID " + id);
						} else if (rowsUpdated > 1) {
								//Log.e("VaavudDatabase", "Expected to update one row but updated " + rowsUpdated + " rows");
						}
				} catch (RuntimeException e) {
						//Log.e("VaavudDatabase", e.getMessage(), e);
				} finally {
						if (db != null) {
								db.close();
						}
				}
		}

	/*
	private void executeSQLUpdate(String sql) {

		SQLiteDatabase db = null;
		try {
			db = getWritableDatabase();
			db.execSQL(sql);
		}
		catch (RuntimeException e) {
//			Log.d("VaavudDatabase", e.getMessage(), e);
		}
		finally {
			if (db != null) {
				db.close();
			}
		}
	}
	*/
}
