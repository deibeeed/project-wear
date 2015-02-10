package com.kfast.uitest.service;

import android.app.Activity;
import android.app.IntentService;
import android.content.Intent;
import android.os.Bundle;
import android.os.ResultReceiver;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.location.ActivityRecognitionResult;
import com.google.android.gms.location.DetectedActivity;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.Wearable;


/**
 * An {@link android.app.IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 * <p/>
 * TODO: Customize class - update intent actions and extra parameters.
 */
public class ActivityRecognitionIntentService extends IntentService {

//    private GoogleApiClient client;
    private PendingResult<DataApi.DataItemResult> pendingResult;

    public ActivityRecognitionIntentService() {
        super("ActivityRecognitionIntentService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if(ActivityRecognitionResult.hasResult(intent)){
//            client = new GoogleApiClient.Builder(this)
//                    .addApi(Wearable.API)
//                    .addConnectionCallbacks(new GoogleApiClient.ConnectionCallbacks() {
//                        @Override
//                        public void onConnected(Bundle bundle) {
//                            Log.d("intent_service:onConnected", "connected");
//                        }
//
//                        @Override
//                        public void onConnectionSuspended(int i) {
//                            Log.d("intent_service:onConnectionSuspended", "connection suspended");
//                        }
//                    })
//                    .addOnConnectionFailedListener(new GoogleApiClient.OnConnectionFailedListener() {
//                        @Override
//                        public void onConnectionFailed(ConnectionResult connectionResult) {
//                            Log.d("intent_service:onConnectionFailed", "connection failed");
//                        }
//                    })
//                    .build();
//
//            client.connect();

            ActivityRecognitionResult recognitionResult = ActivityRecognitionResult.extractResult(intent);
            DetectedActivity mostProbableActivity = recognitionResult.getMostProbableActivity();

            int confidence = mostProbableActivity.getConfidence();
            int activityType = mostProbableActivity.getType();
            String activityName = getNameFromType(activityType);

//            File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), "project-watch-logs.txt");
//            try {
//                FileWriter writer = new FileWriter(file);
//                String txt = new Date().toString() + ": Activity Name: " + activityName + "\n";
//                writer.append(txt);
//                writer.flush();
//                writer.close();
//
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
            Log.d("activity_detect_service", "activity detected: " + activityName);

            sendDataToActivity(intent, activityName, activityType);


//            if(client.isConnected()){
//                client.disconnect();
//            }
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


    private void sendDataToActivity(Intent intent, String dataToSend, int activityType){
        try {
            ResultReceiver receiver = intent.getParcelableExtra("receiver");
            Bundle bun = new Bundle();
            bun.putString("activity", dataToSend);
            receiver.send(Activity.RESULT_OK, bun);
        } catch (Exception e){
            e.printStackTrace();
        }
    }
}
