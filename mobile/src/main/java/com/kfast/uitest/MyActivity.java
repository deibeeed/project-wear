package com.kfast.uitest;

import android.app.Activity;
import android.app.Dialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.location.ActivityRecognition;
import com.google.android.gms.location.ActivityRecognitionApi;
import com.google.android.gms.wearable.Asset;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
//import com.google.android.gms.location.ActivityRecognitionClient;


public class MyActivity extends ActionBarActivity implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, MessageApi.MessageListener {

    private static final String MESSAGE_PATH = "/start/activity-recognition";

    private final static int CONNECTION_FAILURE_RESOLUTION_REQUEST = 9000;
    // Constants that define the activity detection interval
    public static final int MILLISECONDS_PER_SECOND = 100;
    public static final int DETECTION_INTERVAL_SECONDS = 1;
    public static final int DETECTION_INTERVAL_MILLISECONDS =
            MILLISECONDS_PER_SECOND * DETECTION_INTERVAL_SECONDS;

    private PendingIntent mActivityRecognitionPendingIntent;

    private PendingResult<DataApi.DataItemResult> wearPendingResult;

    private GoogleApiClient wearClient;
    private GoogleApiClient recognitionClient;
    private ActivityRecognitionApi recognitionApi;

    private boolean mInProgress;

    private enum REQUEST_TYPE {START, STOP}
    private REQUEST_TYPE mRequestType;

    private ImageView ivTestImage;

    private ArrayList<String> listActivity;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my);

        ivTestImage = (ImageView) findViewById(R.id.ivTestImage);

        listActivity = new ArrayList<String>();

        ivTestImage.setImageBitmap(getBitmapFromAssets());

        mInProgress = false;

        wearClient = new GoogleApiClient.Builder(this)
                        .addApi(Wearable.API)
                        .addConnectionCallbacks(new GoogleApiClient.ConnectionCallbacks() {
                            @Override
                            public void onConnected(Bundle bundle) {
                                Log.d("wear api  mobile", "connected");
                            }

                            @Override
                            public void onConnectionSuspended(int i) {
                                Log.d("wear api mobile", "connection suspended");
                            }
                        })
                        .addOnConnectionFailedListener(new GoogleApiClient.OnConnectionFailedListener() {
                            @Override
                            public void onConnectionFailed(ConnectionResult connectionResult) {
                                Log.d("wear api mobile", "connection failed");
                            }
                        })
                        .build();

        wearClient.connect();

        Wearable.MessageApi.addListener(wearClient, this);

        initRecognitionClient();
    }

    private void initRecognitionClient(){
        recognitionClient = new GoogleApiClient.Builder(this)
                .addApi(ActivityRecognition.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();

        Intent intent = new Intent(this, ActivityRecognitionIntentService.class);
        mActivityRecognitionPendingIntent = PendingIntent.getService(this, 1000, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        recognitionApi = ActivityRecognition.ActivityRecognitionApi;
    }

    private void activityRecognitionSetup(){
        Intent intent = new Intent(this, ActivityRecognitionIntentService.class);
//        ActivityRecognitionReceiver resultReceiver = new ActivityRecognitionReceiver(new Handler());
//        resultReceiver.setReceiver(new ActivityRecognitionReceiver.Receiver() {
//            @Override
//            public void onReceivedResult(int resultCode, Bundle resultData) {
//                Log.d("activityDetected", "activityDetected: " + resultData.getString("activityDetected"));
//
//                if(resultCode == RESULT_OK){
//                    listActivity.add(resultData.getString("activityDetected"));
//                    adapter.notifyDataSetChanged();
//                }
//            }
//        });
//
//        intent.putExtra("receiver", resultReceiver);
        mActivityRecognitionPendingIntent = PendingIntent.getService(this, 1000, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        recognitionApi = ActivityRecognition.ActivityRecognitionApi;

        startUpdates();
    }

    private void startSendDataToWear(){
        PutDataMapRequest dataMap = PutDataMapRequest.create("/test");
        dataMap.getDataMap().putString("message", "this is test message");
        dataMap.getDataMap().putAsset("img", createAssetFromBitmap(getBitmapFromAssets()));
        dataMap.getDataMap().putLong("timestamp", System.currentTimeMillis());
        PutDataRequest request = dataMap.asPutDataRequest();
        wearPendingResult = Wearable.DataApi.putDataItem(wearClient,  request);

        wearPendingResult.setResultCallback(new ResultCallback<DataApi.DataItemResult>() {
            @Override
            public void onResult(DataApi.DataItemResult dataItemResult) {
                Log.d("wear pending result", "status: " + dataItemResult.getStatus().getStatus() + "result: " + dataItemResult.getDataItem().getUri());
                Toast.makeText(MyActivity.this, "status: " + dataItemResult.getStatus().getStatus() + "result: " + dataItemResult.getDataItem().getUri(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private Bitmap getBitmapFromAssets(){
        AssetManager assetManager = getAssets();
        InputStream iStream = null;

        try{
            iStream = assetManager.open("chrome_icon.png");
        }catch (IOException e){
            e.printStackTrace();
        }

        return BitmapFactory.decodeStream(iStream);
    }

    private Asset createAssetFromBitmap(Bitmap bmp){
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bmp.compress(Bitmap.CompressFormat.PNG, 100, baos);

        return Asset.createFromBytes(baos.toByteArray());
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.my, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        switch (id){
            case R.id.action_settings:
                startActivity(new Intent(this, SettingsActivity.class));
                break;
            case R.id.action_stop_activity_recognition:
                stopUpdates();
                break;
            case R.id.action_send_image_to_wear:
                startSendDataToWear();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode){
            case CONNECTION_FAILURE_RESOLUTION_REQUEST:
                //connect again

                switch (resultCode){
                    case RESULT_OK:
                        //request again
                        break;
                }
                break;
        }
    }

    private boolean servicesConnected(){
        int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);

        if(resultCode == ConnectionResult.SUCCESS){
            Log.d("Play services", "Google Play Services available");

            return true;
        }else{
            Dialog errorDialog = GooglePlayServicesUtil.getErrorDialog(resultCode, this, CONNECTION_FAILURE_RESOLUTION_REQUEST);

            if(errorDialog != null){
                ErrorDialogFragment errorFragment =  new ErrorDialogFragment();
                errorFragment.setDialog(errorDialog);
                errorFragment.show(getSupportFragmentManager(), "Google Play Services");
            }

            return false;
        }
    }

    public void startUpdates(){
        mRequestType = REQUEST_TYPE.START;

        if(!servicesConnected()){
            return;
        }

        if(!mInProgress){
            recognitionClient.connect();
        }else {

        }
    }

    public void stopUpdates(){
        mRequestType = REQUEST_TYPE.STOP;

        if(!servicesConnected()){
            return;
        }

        if(!mInProgress){
            mInProgress = true;
            recognitionClient.connect();
        }else{

        }

        Toast.makeText(this, "Activity Recognition Stopped", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onConnected(Bundle bundle) {

        switch (mRequestType){
            case START:
                recognitionApi.requestActivityUpdates(recognitionClient, DETECTION_INTERVAL_MILLISECONDS, mActivityRecognitionPendingIntent);
                break;
            case STOP:
                recognitionApi.removeActivityUpdates(recognitionClient, mActivityRecognitionPendingIntent);
                break;
        }

        mInProgress = false;
        recognitionClient.disconnect();
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        mInProgress = false;

        if(connectionResult.hasResolution()){
            try {
                connectionResult.startResolutionForResult(this, CONNECTION_FAILURE_RESOLUTION_REQUEST);
            } catch (IntentSender.SendIntentException e) {
                e.printStackTrace();
            }
        }else{
            int errorCode = connectionResult.getErrorCode();
            Dialog errorDialog = GooglePlayServicesUtil.getErrorDialog(errorCode, this, CONNECTION_FAILURE_RESOLUTION_REQUEST);

            if(errorDialog != null){
                ErrorDialogFragment errorFragment = new ErrorDialogFragment();
                errorFragment.setDialog(errorDialog);
                errorFragment.show(getSupportFragmentManager(), "Connection Failed");
            }
        }
    }

    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        Log.d("message_received", "message path: " + messageEvent.getPath());

        if(messageEvent.getPath().equals(MESSAGE_PATH)){
            activityRecognitionSetup();
        }
    }

    public static class ErrorDialogFragment extends DialogFragment{
        private Dialog mDialog;

        public ErrorDialogFragment(){
            super();
            mDialog = null;
        }

        public void setDialog(Dialog dialog){
            mDialog = dialog;
        }

        @NonNull
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            return mDialog;
        }
    }
}
