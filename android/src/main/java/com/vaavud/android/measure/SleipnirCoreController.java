package com.vaavud.android.measure;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.os.Handler;
import android.util.Log;

import com.vaavud.android.R;
import com.vaavud.android.measure.sensor.DataManager;
import com.vaavud.android.measure.sensor.LocationUpdateManager;
import com.vaavud.android.model.VaavudDatabase;
import com.vaavud.android.model.entity.Device;
import com.vaavud.android.model.entity.LatLng;
import com.vaavud.android.model.entity.MeasurementPoint;
import com.vaavud.android.model.entity.MeasurementSession;
import com.vaavud.android.model.entity.WindMeasurement;
import com.vaavud.android.model.entity.WindMeter;
import com.vaavud.android.network.UploadManager;
import com.vaavud.android.ui.calibration.CalibrationActivity;
import com.vaavud.sleipnirSDK.OrientationSensorManagerSleipnir;
import com.vaavud.sleipnirSDK.audio.VaavudAudioPlaying;
import com.vaavud.sleipnirSDK.audio.VaavudAudioRecording;
import com.vaavud.sleipnirSDK.listener.SpeedListener;
import com.vaavud.util.SettingsContentObserver;
import com.vaavud.util.UUIDUtil;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

public class SleipnirCoreController implements MeasurementController, SpeedListener {

    private static final String KEY_CALIBRATION_COEFFICENTS = "calibrationCoefficients";

    private Context context;
    private OrientationSensorManagerSleipnir orientationSensorManager;
    private UploadManager uploadManager;
    private LocationUpdateManager locationManager;
    private boolean isMeasuring = false;
    private MeasurementSession currentSession;
    private DataManager dataManager;
    private boolean mCalibrationMode;
    private Handler handler;
    private List<MeasurementReceiver> measurementReceivers = new ArrayList<MeasurementReceiver>();
    ;
    private MeasureStatus status;

    private AudioManager myAudioManager;
    private AudioRecord recorder;
    private AudioTrack player;

    private final int duration = 1; // seconds
    private final int sampleRate = 44100; //Hz
    private final int numSamples = duration * sampleRate;
    private final int N = 3; //Hz
    private long initialTime;
    private WindMeasurement wind;
    private Float[] coefficients;

    private float calibrationProgress = 0F;


    private VaavudAudioPlaying audioPlayer;
    private VaavudAudioRecording audioRecording;
    private String mFileName;

    private Runnable readDataRunnable = new Runnable() {
        @Override
        public void run() {
            updateMeasureStatus();
            readData();
            handler.postDelayed(readDataRunnable, 200);
        }
    };

    private SettingsContentObserver mSettingsContentObserver;

    

    public SleipnirCoreController(Context context, DataManager dataManager, UploadManager uploadManager, LocationUpdateManager locationManager, boolean calibrationMode) {
//		Log.d("SleipnirCoreController","Sleipnir Core Controller");
        this.context = context;
        this.uploadManager = uploadManager;
        this.locationManager = locationManager;
        this.dataManager = dataManager;
        mCalibrationMode = calibrationMode;

    }

    public void startController() {
        String coefficientsString;
        orientationSensorManager = new OrientationSensorManagerSleipnir(context);
        wind = new WindMeasurement();
        handler = new Handler();
        if (!mCalibrationMode) {
            coefficientsString = VaavudDatabase.getInstance(context).getProperty(KEY_CALIBRATION_COEFFICENTS);
            if (coefficientsString != null) {
                coefficients = new Float[15];
                String[] f = coefficientsString.split(",");
                for (int i = 0; i < f.length; i++) {
                    coefficients[i] = Float.parseFloat(f[i]);
                }
            }
        }

        if (player != null) {
            player.flush();
            player.release();
        }
        if (recorder != null) {
            recorder.release();
        }

        player = null;
        recorder = null;
        initialTime = 0;
    }

    public void startMeasuring() {
//		Log.d("SleipnirCoreController","Start Measuring");
        mSettingsContentObserver = new SettingsContentObserver(context, new Handler());
        context.getContentResolver().registerContentObserver(android.provider.Settings.System.CONTENT_URI, true, mSettingsContentObserver);
        
        if (player == null)
            player = new AudioTrack(AudioManager.STREAM_MUSIC, sampleRate, AudioFormat.CHANNEL_OUT_STEREO, AudioFormat.ENCODING_PCM_16BIT, numSamples * 2, AudioTrack.MODE_STREAM);
        if (recorder == null)
            recorder = new AudioRecord(MediaRecorder.AudioSource.MIC, sampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, sampleRate * N);

        myAudioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        myAudioManager.requestAudioFocus(null, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE);
        
        myAudioManager.setMicrophoneMute(false);
        if (!mCalibrationMode) {
            uploadManager.triggerUpload();
            status = MeasureStatus.MEASURING;
            sendStatus(status);
        }
        else{
            String name = Device.getInstance(context).getModel() + "_" + Device.getInstance(context).getUuid() + "_" + new SimpleDateFormat("ddMMyy-HHmmss").format(new Date()) + ".raw";
            mFileName = context.getExternalCacheDir().getAbsolutePath() + "/" + name;
        }
        
        initialTime = new Date().getTime();
        isMeasuring = true;
        resumeMeasuring();
    }

    public void stopMeasuring() {
//		Log.d("SleipnirCoreController","Stop Measuring");
        pauseMeasuring();
        isMeasuring = false;
        context.getContentResolver().unregisterContentObserver(mSettingsContentObserver);
    }

    @Override
    public void pauseMeasuring() {
        if (isMeasuring) {
//			Log.d("SleipnirCoreController","Pause Measuring");
            if (audioPlayer != null) audioPlayer.close();
            if (audioRecording != null) audioRecording.close();

            if (orientationSensorManager != null && orientationSensorManager.isSensorAvailable()) {
                orientationSensorManager.stop();
            }

            //Object and thread destroying
            audioPlayer = null;
            audioRecording = null;
        }
    }

    @Override
    public void resumeMeasuring() {

        if (isMeasuring) {
        	
        	int volume = myAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
            final int maxVolume = myAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        	
        	AlertDialog.Builder builder1 = new AlertDialog.Builder(context);
        	builder1.setTitle(context.getResources().getString(R.string.sound_disclaimer_title));
            builder1.setMessage(context.getResources().getString(R.string.sound_disclaimer));
            builder1.setCancelable(false);
            builder1.setNeutralButton(context.getResources().getString(R.string.button_ok),
                    new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    dialog.dismiss();
                    myAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, maxVolume, AudioManager.FLAG_REMOVE_SOUND_AND_VIBRATE);
                }
            });
            
            AlertDialog alert = builder1.create();
            
            if (volume < maxVolume){
            	Log.d("SleipnirCoreController","Volume: "+volume+" "+maxVolume);
            	alert.show();
        	}
            if (orientationSensorManager.isSensorAvailable()) {
                orientationSensorManager.start();
            }
//			
            audioPlayer = new VaavudAudioPlaying(player,mFileName, mCalibrationMode);

            audioRecording = new VaavudAudioRecording(recorder, player, this, null, mFileName, mCalibrationMode);
            audioRecording.setCoefficients(coefficients);
            audioPlayer.start();
            audioRecording.start();

        }
    }

    public void clearData() {
        dataManager.clearData();
    }

    public Integer getNumberOfMeassurements() {
        return dataManager.getNumberOfMeassurements();
    }

    public Float getTimeSinceStart() {
        return dataManager.getTimeSinceStart();
    }

    public Float getLastMag() {
        return dataManager.getLastMagX();
    }

    public Float getWindspeed() {
        return dataManager.getLastWindspeed();
    }

    public Double getAverageWindspeed() {
        return dataManager.getAverageWindspeed();
    }

    public Float getMaxWindspeed() {
        return dataManager.getMaxWindspeed();
    }

    public Float[] getLatestNewTimeAndWindspeed() {
        return dataManager.getLatestNewTimeAndWindspeed();
    }

    public Float[] getLastTimeAndWindspeed() {
        return dataManager.getLastTimeAndWindspeed();
    }

    public Float getLastWindDirection() {
        return dataManager.getLastWindDirection();
    }

    private void readData() {
//		Log.d("SleipnirCoreController", "Read Data: "+wind.windspeed +" "+wind.windDirection);
    	Log.d("SleipnirCoreController","Playing Volume: "+myAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC));
        if (getLatestNewTimeAndWindspeed() != null) {
            Double currentMeanValueMS = getAverageWindspeed();
            Float currentActualValueMS = getWindspeed();
            Float currentMaxValueMS = getMaxWindspeed();
            Float currentDirection = getLastWindDirection();
            Float orientationAngle = (float) orientationSensorManager.getAngle();


            // always update measurement session's endtime and summary info
            currentSession.setEndTime(new Date());
            if (currentMeanValueMS != null) {
                currentSession.setWindSpeedAvg(currentMeanValueMS.floatValue());
            }
            if (currentMaxValueMS != null) {
                currentSession.setWindSpeedMax(currentMaxValueMS);
            }

            LatLng location = locationManager.getLocation();
            if (location != null) {
                currentSession.setPosition(location);
            }
            Float direction = 0f;
            if (currentDirection != null) {
                if (orientationAngle != null) {
//	        		Log.d("SleipnirCoreController","Current Direction: "+currentDirection + " Current Orientation: "+orientationAngle);
                    direction = currentDirection + orientationAngle;
                    if (direction > 360) {
                        direction -= 360;
                    }
                    currentSession.setWindDirection(direction);
                    currentDirection = direction;
                } else {
                    currentSession.setWindDirection(currentDirection);
                }
//	        	Log.d("SleipnirCoreController","Current Direction: "+currentDirection + " Current Orientation: "+orientationAngle + " estimated Direction: "+direction);
            }
//	        Log.d("SleipnirCoreController","Read Data WindMeter: "+currentSession.getWindMeter());
            VaavudDatabase.getInstance(context).updateDynamicMeasurementSession(currentSession);

            // add MeasurementPoint and save to database
            MeasurementPoint measurementPoint = new MeasurementPoint();
            measurementPoint.setSession(currentSession);
            measurementPoint.setTime(new Date());
            measurementPoint.setWindSpeed(currentActualValueMS);
            measurementPoint.setWindDirection(currentDirection);

            VaavudDatabase.getInstance(context).insertMeasurementPoint(measurementPoint);

            for (MeasurementReceiver measurementReceiver : measurementReceivers) {
                measurementReceiver.measurementAdded(currentSession, dataManager.getLastTime(), currentActualValueMS, currentMeanValueMS == null ? null : currentMeanValueMS.floatValue(), currentMaxValueMS, currentDirection);
            }
        }
    }

    private void updateMeasureStatus() {
//		Log.d("SleipnirCoreController", "updateMeasureStatus");
        MeasureStatus newStatus = MeasureStatus.MEASURING;

        if (!dataManager.newMeasurementsAvailable()) {
            if (dataManager.getTimeSinceStart() > 2) {
                newStatus = MeasureStatus.NO_SIGNAL;
            }
        }
        if (orientationSensorManager.isSensorAvailable() && orientationSensorManager.isVertical() == false) {
            newStatus = MeasureStatus.KEEP_VERTICAL;
            dataManager.setMeasureIsValid(false);
        } else {
            dataManager.setMeasureIsValid(true);
        }
        if (!status.equals(newStatus)) {
            status = newStatus;
            sendStatus(status);
        }
    }

    private void sendStatus(MeasureStatus status) {
        for (MeasurementReceiver measurementReceiver : measurementReceivers) {
            measurementReceiver.measurementStatusChanged(status);
        }
    }

    @Override
    public MeasurementSession startSession() {
        clearData();
//    	Log.d("SleipnirCoreController", "Start Session");
        currentSession = new MeasurementSession();
        currentSession.setUuid(UUIDUtil.generateUUID());
        currentSession.setDevice(Device.getInstance(context).getUuid());
        currentSession.setSource("vaavud");
        currentSession.setStartTime(new Date());
        currentSession.setTimezoneOffset((long) TimeZone.getDefault().getOffset(currentSession.getStartTime().getTime()));
        currentSession.setEndTime(currentSession.getStartTime());
        currentSession.setMeasuring(true);
        currentSession.setUploaded(false);
        currentSession.setStartIndex(0);
        currentSession.setEndIndex(0);
        currentSession.setPosition(locationManager.getLocation());
        currentSession.setWindMeter(WindMeter.SLEIPNIR);

        VaavudDatabase.getInstance(context).insertMeasurementSession(currentSession);

        startMeasuring();

        handler.post(readDataRunnable);

        return currentSession;
    }

    @Override
    public void stopSession() {
//		Log.d("SleipnirCoreController", "Stop Session "+currentSession.getWindMeter());
        handler.removeCallbacks(readDataRunnable);
        stopMeasuring();
        currentSession.setMeasuring(false);
        VaavudDatabase.getInstance(context).updateDynamicMeasurementSession(currentSession);

        uploadManager.triggerUpload();

        for (MeasurementReceiver measurementReceiver : measurementReceivers) {
            measurementReceiver.measurementFinished(currentSession);
        }
        currentSession = null;
    }

    @Override
    public void stopController() {
        measurementReceivers.clear();
        if (orientationSensorManager != null) {
            orientationSensorManager.stop();
            orientationSensorManager = null;
        }
        if (currentSession != null) {
            handler.removeCallbacks(readDataRunnable);
        }
        handler = null;
        if (player != null) player.release();
        if (recorder != null) recorder.release();
        player = null;
        recorder = null;
    }

    @Override
    public MeasurementSession currentSession() {
        return currentSession;
    }

    @Override
    public boolean isMeasuring() {
        return (currentSession != null || isMeasuring);
    }

    public boolean isStarted() {
        return orientationSensorManager != null;
    }

    @Override
    public void addMeasurementReceiver(MeasurementReceiver measurementReceiver) {
        if (!measurementReceivers.contains(measurementReceiver)) {
            measurementReceivers.add(measurementReceiver);
        }
    }

    @Override
    public void removeMeasurementReceiver(MeasurementReceiver measurementReceiver) {
        measurementReceivers.remove(measurementReceiver);
    }

    @Override
    public void speedChanged(float speed, float windDirection, long timestamp,float velocityProfileError) {
        wind = new WindMeasurement();
        wind.windspeed = (float) ((speed * 0.325) + 0.2);
        wind.windDirection = windDirection;
        wind.time = (float) (timestamp - initialTime) / (float) 1000;
        if (dataManager != null) {
            dataManager.addWindMeasurement(wind);
        }

//		Log.d("SpeedChanged","Timestamp: "+ timestamp +" initialTime: "+ initialTime +" Wind Time: "+wind.time);
    }

    @Override
    public void calibrationPercentageComplete(float percentage) {
        setCalibrationProgress(percentage);
    }

    public String getFileName(){
    	return mFileName;
    }
    public float getCalibrationProgress() {
        return calibrationProgress;
    }

    public void setCalibrationProgress(float calibrationProgress) {
        this.calibrationProgress = calibrationProgress;
    }

    @Override
    public void calibrationCoefficients(Float[] coefficients) {
        if (context instanceof CalibrationActivity) {
            CalibrationActivity activity = (CalibrationActivity) context;
            activity.calibrationCoefficients(coefficients);
        }
    }
}
