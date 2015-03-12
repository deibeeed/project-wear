package com.kfast.uitest.service;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.IBinder;
import android.util.Log;

import com.kfast.uitest.util.Config;
import com.kfast.uitest.util.PreferenceHelper;

public class StepService extends Service implements SensorEventListener{

    private SensorManager sensorManager;
    private Sensor stepSensor;

    private int progressBarMax;

    public StepService() {
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        stepSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR);

        progressBarMax = PreferenceHelper.getInstance(this).getInt(Config.Keys.KEY_PROGRESS_STEPS, Config.PROGRESS_BAR_STEPS);

        sensorManager.registerListener(this, stepSensor, SensorManager.SENSOR_DELAY_FASTEST);
        Log.d("step_service", "service started");
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if(event.sensor.getType() == Sensor.TYPE_STEP_DETECTOR){
            int stepCount = PreferenceHelper.getInstance(this).getInt("stepCount", 0);
            stepCount++;

            if(stepCount <= progressBarMax){
                PreferenceHelper.getInstance(this).setInt("stepCount", stepCount);
            }

            Log.d("step_service", "step service detected");
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }


}
