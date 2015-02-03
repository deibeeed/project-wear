package com.kfast.uitest;

import android.app.IntentService;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.location.ActivityRecognitionResult;
import com.google.android.gms.location.DetectedActivity;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Date;


/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 * <p/>
 * TODO: Customize class - update intent actions and extra parameters.
 */
public class ActivityRecognitionIntentService extends IntentService {

    private GoogleApiClient client;
    private PendingResult<DataApi.DataItemResult> pendingResult;

    public ActivityRecognitionIntentService() {
        super("ActivityRecognitionIntentService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if(ActivityRecognitionResult.hasResult(intent)){
            client = new GoogleApiClient.Builder(this)
                    .addApi(Wearable.API)
                    .addConnectionCallbacks(new GoogleApiClient.ConnectionCallbacks() {
                        @Override
                        public void onConnected(Bundle bundle) {
                            Log.d("intent_service:onConnected", "connected");
                        }

                        @Override
                        public void onConnectionSuspended(int i) {
                            Log.d("intent_service:onConnectionSuspended", "connection suspended");
                        }
                    })
                    .addOnConnectionFailedListener(new GoogleApiClient.OnConnectionFailedListener() {
                        @Override
                        public void onConnectionFailed(ConnectionResult connectionResult) {
                            Log.d("intent_service:onConnectionFailed", "connection failed");
                        }
                    })
                    .build();

            client.connect();

            ActivityRecognitionResult recognitionResult = ActivityRecognitionResult.extractResult(intent);
            DetectedActivity mostProbableActivity = recognitionResult.getMostProbableActivity();

            int confidence = mostProbableActivity.getConfidence();
            int activityType = mostProbableActivity.getType();
            String activityName = getNameFromType(activityType);

            File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), "project-watch-logs.txt");
            try {
                FileWriter writer = new FileWriter(file);
                String txt = new Date().toString() + ": Activity Name: " + activityName + "\n";
                writer.append(txt);
                writer.flush();
                writer.close();

            } catch (IOException e) {
                e.printStackTrace();
            }
            Log.d("activity detect service", activityName);

            sendDataToWear(activityName, activityType);

            if(client.isConnected()){
                client.disconnect();
            }
        }
    }

    private String getNameFromType(int activityType) {
        switch(activityType) {
            case DetectedActivity.IN_VEHICLE:
                return "in_vehicle";
            case DetectedActivity.ON_BICYCLE:
                return "on_bicycle";
            case DetectedActivity.ON_FOOT:
                return "on_foot";
            case DetectedActivity.RUNNING:
                return "running";
            case DetectedActivity.WALKING:
                return "walking";
            case DetectedActivity.STILL:
                return "still";
            case DetectedActivity.UNKNOWN:
                return "unknown";
            case DetectedActivity.TILTING:
                return "tilting";
        }
        return "unknown";
    }


    private void sendDataToWear(String dataToSend, int activityType){
        PutDataMapRequest dataMap = PutDataMapRequest.create("/test-service");
        dataMap.getDataMap().putString("message_service", dataToSend);
        dataMap.getDataMap().putInt("activityType", activityType);
        PutDataRequest request = dataMap.asPutDataRequest();
        pendingResult = Wearable.DataApi.putDataItem(client, request);

        pendingResult.setResultCallback(new ResultCallback<DataApi.DataItemResult>() {
            @Override
            public void onResult(DataApi.DataItemResult dataItemResult) {
                Log.d("recognition_service result callback", "status: " + dataItemResult.getStatus().getStatus() + "result: " + dataItemResult.getDataItem().getUri());
            }
        });
    }
}
