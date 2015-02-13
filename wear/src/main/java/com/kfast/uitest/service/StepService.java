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

    public StepService() {
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        stepSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR);

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

            if(stepCount <= Config.PROGRESS_BAR_STEPS){
                PreferenceHelper.getInstance(this).setInt("stepCount", stepCount);
            }

            Log.d("step_service", "step service detected");
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }


}
