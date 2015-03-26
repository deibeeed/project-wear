package com.kfast.uitest.fragments;

import android.app.Activity;
import android.app.Dialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.Scopes;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.fitness.Fitness;
import com.google.android.gms.fitness.FitnessStatusCodes;
import com.google.android.gms.fitness.data.Bucket;
import com.google.android.gms.fitness.data.DataPoint;
import com.google.android.gms.fitness.data.DataSet;
import com.google.android.gms.fitness.data.DataSource;
import com.google.android.gms.fitness.data.DataType;
import com.google.android.gms.fitness.data.Field;
import com.google.android.gms.fitness.request.DataDeleteRequest;
import com.google.android.gms.fitness.request.DataReadRequest;
import com.google.android.gms.fitness.result.DataReadResult;
import com.google.android.gms.location.ActivityRecognition;
import com.google.android.gms.location.ActivityRecognitionApi;
import com.google.android.gms.wearable.Asset;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;
import com.kfast.uitest.R;
import com.kfast.uitest.SettingsActivity;
import com.kfast.uitest.model.UnsentSteps;
import com.kfast.uitest.service.ActivityRecognitionIntentService;
import com.kfast.uitest.utils.ObjectSerializer;
import com.kfast.uitest.utils.Utils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link MainFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link MainFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class MainFragment extends Fragment implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, MessageApi.MessageListener{

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

    private OnFragmentInteractionListener mListener;

    private TextView textView;

    private ListView listView;
    private ItemsAdapter adapter;
    private ArrayList<String> listDetails;

    //Fit API constants
    private static final String FITNESS_TAG = "fitness_api";
    private static final int REQUEST_AUTH = 1;
    private static final String AUTH_PENDING = "auth_state_pending";
    private boolean authInProgress = false;
    private static final String DATE_FORMAT = "yyyy.MM.dd HH:mm:ss";

    private GoogleApiClient fitClient;

    private boolean fitnessClintConnected;

    // TODO: Rename and change types and number of parameters
    public static MainFragment newInstance() {
        MainFragment fragment = new MainFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    public MainFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {

        }

        if(savedInstanceState != null){
            authInProgress = savedInstanceState.getBoolean(AUTH_PENDING);
        }
        fitnessClintConnected = false;

        buildFitnessClient();

        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View mainView = inflater.inflate(R.layout.fragment_main, container, false);

        ivTestImage = (ImageView) mainView.findViewById(R.id.ivTestImage);


        listActivity = new ArrayList<String>();
        listDetails = new ArrayList<>();

        textView = (TextView) mainView.findViewById(R.id.textView);
        listView = (ListView) mainView.findViewById(R.id.listView);
        adapter = new ItemsAdapter(getActivity(), listDetails);

        listView.setAdapter(adapter);

        ivTestImage.setImageBitmap(getBitmapFromAssets());

        mInProgress = false;

        wearClient = new GoogleApiClient.Builder(getActivity())
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

        return mainView;
    }

    // TODO: Rename method, update argument and hook method into UI event
    public void onButtonPressed(Uri uri) {
        if (mListener != null) {
            mListener.onFragmentInteraction(uri);
        }
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (OnFragmentInteractionListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    @Override
    public void onStart() {
        super.onStart();

        fitClient.connect();
    }

    @Override
    public void onStop() {
        super.onStop();

        if(fitClient.isConnected()){
            fitClient.disconnect();
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        getActivity().supportInvalidateOptionsMenu();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {

        inflater.inflate(R.menu.my, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id){
            case R.id.action_settings2:
                startActivity(new Intent(getActivity(), SettingsActivity.class));
                break;
            case R.id.action_stop_activity_recognition:
                stopUpdates();
                break;
            case R.id.action_send_image_to_wear:
                startSendDataToWear();
                break;
            case R.id.action_stop_fitness_recording:
                stopFitnessRecording();
                break;
            case R.id.action_delete_all_data:
                new AsyncTask<Void, Void, Void>() {
                    @Override
                    protected Void doInBackground(Void... params) {
                        deleteAllFitnessData();
                        return null;
                    }
                }.execute();

                break;
            case R.id.action_read_all_data:
                new AsyncTask<Void, Void, Void>() {
                    @Override
                    protected Void doInBackground(Void... params) {
                        readFitnessData();
                        return null;
                    }
                }.execute();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode){
            case CONNECTION_FAILURE_RESOLUTION_REQUEST:
                //connect again

                switch (resultCode){
                    case Activity.RESULT_OK:
                        //request again
                        break;
                }
                break;
            case REQUEST_AUTH:
                if(resultCode == Activity.RESULT_OK){
                    if(!fitClient.isConnecting() && !fitClient.isConnected()){
                        fitClient.connect();
                    }
                }
                break;
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(AUTH_PENDING, authInProgress);
    }

    private void buildFitnessClient(){
        fitClient = new GoogleApiClient.Builder(getActivity())
                        .addApi(Fitness.API)
                        .addScope(new Scope(Scopes.FITNESS_LOCATION_READ_WRITE))
                        .addScope(new Scope(Scopes.FITNESS_ACTIVITY_READ_WRITE))
                        .addScope(new Scope(Scopes.FITNESS_BODY_READ_WRITE))
                        .addConnectionCallbacks(new GoogleApiClient.ConnectionCallbacks() {
                            @Override
                            public void onConnected(Bundle bundle) {
                                Log.d(FITNESS_TAG, "Connected!");
                                fitnessClintConnected = true;
//                                startFitnessRecording();

                                new AsyncTask<Void, Void, Void>() {
                                    @Override
                                    protected Void doInBackground(Void... params) {
                                        readFitnessData();
                                        return null;
                                    }
                                }.execute();
                            }

                            @Override
                            public void onConnectionSuspended(int i) {
                                // If your connection to the sensor gets lost at some point,
                                // you'll be able to determine the reason and react to it here.
                                if (i == GoogleApiClient.ConnectionCallbacks.CAUSE_NETWORK_LOST) {
                                    Log.i(FITNESS_TAG, "Connection lost.  Cause: Network Lost.");
                                } else if (i == GoogleApiClient.ConnectionCallbacks.CAUSE_SERVICE_DISCONNECTED) {
                                    Log.i(FITNESS_TAG, "Connection lost.  Reason: Service Disconnected");
                                }
                            }
                        })
                        .addOnConnectionFailedListener(new GoogleApiClient.OnConnectionFailedListener() {
                            @Override
                            public void onConnectionFailed(ConnectionResult connectionResult) {
                                Log.d(FITNESS_TAG, "Connection Failed. Cause: " + connectionResult.toString());

                                if (!connectionResult.hasResolution()) {
                                    GooglePlayServicesUtil.getErrorDialog(connectionResult.getErrorCode(), getActivity(), 0).show();

                                    return;
                                }

                                if (!authInProgress) {
                                    try {
                                        Log.d(FITNESS_TAG, "Attempting to resolve failed connection...");
                                        authInProgress = true;
                                        connectionResult.startResolutionForResult(getActivity(), REQUEST_AUTH);
                                    } catch (IntentSender.SendIntentException e) {
                                        e.printStackTrace();

                                        Log.d(FITNESS_TAG, "Exception while starting resolution activity", e);
                                    }
                                }
                            }
                        })
                        .build();
    }

    private void startFitnessRecording(){
        Fitness.RecordingApi.subscribe(fitClient, DataType.TYPE_STEP_COUNT_DELTA)
                .setResultCallback(new ResultCallback<Status>() {
                    @Override
                    public void onResult(Status status) {
                        if(status.isSuccess()){
                            if(status.getStatusCode() == FitnessStatusCodes.SUCCESS_ALREADY_SUBSCRIBED){
                                Log.d(FITNESS_TAG, "Existing subscription for activity detected.");
                            }else{
                                Log.d(FITNESS_TAG, "Successfully subscribed");
                            }
                        }else{
                            Log.d(FITNESS_TAG, "There was a problem subscribing. Status: " + status.getStatusMessage());

                        }
                    }
                });
    }

    private void stopFitnessRecording(){
        Fitness.RecordingApi.unsubscribe(fitClient, DataType.TYPE_STEP_COUNT_DELTA)
                .setResultCallback(new ResultCallback<Status>() {
                    @Override
                    public void onResult(Status status) {
                        if(status.isSuccess()){
                            Log.d(FITNESS_TAG, "Successfully unsubscribed for data type: " + DataType.TYPE_STEP_COUNT_DELTA.getName());
                        }else{
                            Log.d(FITNESS_TAG, "Failed unsubscribed for data type: " + DataType.TYPE_STEP_COUNT_DELTA.getName());
                        }
                    }
                });
    }

    private void readFitnessData(){
        // [START build_read_data_request]
        // Setting a start and end date using a range of 1 week before this moment.
        Calendar cal = Calendar.getInstance();
        Date now = new Date();
        cal.setTime(now);
        long endTime = cal.getTimeInMillis();
        cal.add(Calendar.YEAR, -1);
        long startTime = cal.getTimeInMillis();

        SimpleDateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT);
        Log.i(FITNESS_TAG, "Range Start: " + dateFormat.format(startTime));
        Log.i(FITNESS_TAG, "Range End: " + dateFormat.format(endTime));
        DataReadRequest readRequest = new DataReadRequest.Builder()
                                            .aggregate(DataType.TYPE_STEP_COUNT_DELTA, DataType.AGGREGATE_STEP_COUNT_DELTA)
//                                            .read(DataType.TYPE_STEP_COUNT_DELTA)
                                            .bucketByTime(1, TimeUnit.DAYS)
                                            .setTimeRange(startTime, endTime, TimeUnit.MILLISECONDS)
                                            .build();

        DataReadResult readResult = Fitness.HistoryApi.readData(fitClient, readRequest).await(1, TimeUnit.MINUTES);

        printData(readResult);
    }

    private void insertFitnessData(int steps){
        // Set a start and end time for our data, using a start time of 1 hour before this moment.
        Calendar cal = Calendar.getInstance();
        Date now = new Date();
        cal.setTime(now);

        long endTime = cal.getTimeInMillis();
        cal.add(Calendar.SECOND, -1);
        long startTime = cal.getTimeInMillis();

        //Create DataSource
        DataSource dataSource = new DataSource.Builder()
                                    .setAppPackageName(getActivity())
                                    .setDataType(DataType.TYPE_STEP_COUNT_DELTA)
                                    .setName("project-watch step count")
                                    .setType(DataSource.TYPE_RAW)
                                    .build();

        //Create DataSet
        final DataSet dataSet = DataSet.create(dataSource);

        // For each data point, specify a start time, end time, and the data value -- in this case,
        // the number of new steps.
        DataPoint dataPoint = dataSet.createDataPoint()
                                    .setTimeInterval(startTime, endTime, TimeUnit.MILLISECONDS);

        dataPoint.getValue(Field.FIELD_STEPS).setInt(steps);
        dataSet.add(dataPoint);

        // Then, invoke the History API to insert the data and await the result, which is
        // possible here because of the {@link AsyncTask}. Always include a timeout when calling
        // await() to prevent hanging that can occur from the service being shutdown because
        // of low memory or other conditions.
        Log.i(FITNESS_TAG, "Inserting the dataset in the History API");

        Status insertStatus = Fitness.HistoryApi.insertData(fitClient, dataSet).await(1, TimeUnit.MINUTES);

        // Before querying the data, check to see if the insertion succeeded.
        if(!insertStatus.isSuccess()){
            Log.d(FITNESS_TAG, "There was a problem inserting the dataset.");
            return;
        }

        // At this point, the data has been inserted and can be read.
        Log.i(FITNESS_TAG, "Data insert was successful!");

        readFitnessData();
    }

    private void insertFitnessData(int steps, GregorianCalendar date){
        // Set a start and end time for our data, using a start time of 1 hour before this moment.
        Calendar cal = date;
//        Date now = new Date();
//        cal.setTime(now);

        long endTime = cal.getTimeInMillis();
        cal.add(Calendar.SECOND, -1);
        long startTime = cal.getTimeInMillis();

        //Create DataSource
        DataSource dataSource = new DataSource.Builder()
                .setAppPackageName(getActivity())
                .setDataType(DataType.TYPE_STEP_COUNT_DELTA)
                .setName("project-watch step count")
                .setType(DataSource.TYPE_RAW)
                .build();

        //Create DataSet
        final DataSet dataSet = DataSet.create(dataSource);

        // For each data point, specify a start time, end time, and the data value -- in this case,
        // the number of new steps.
        DataPoint dataPoint = dataSet.createDataPoint()
                .setTimeInterval(startTime, endTime, TimeUnit.MILLISECONDS);

        dataPoint.getValue(Field.FIELD_STEPS).setInt(steps);
        dataSet.add(dataPoint);

        // Then, invoke the History API to insert the data and await the result, which is
        // possible here because of the {@link AsyncTask}. Always include a timeout when calling
        // await() to prevent hanging that can occur from the service being shutdown because
        // of low memory or other conditions.
        Log.i(FITNESS_TAG, "Inserting the dataset in the History API");

        Status insertStatus = Fitness.HistoryApi.insertData(fitClient, dataSet).await(1, TimeUnit.MINUTES);

        // Before querying the data, check to see if the insertion succeeded.
        if(!insertStatus.isSuccess()){
            Log.d(FITNESS_TAG, "There was a problem inserting the dataset.");
            return;
        }

        // At this point, the data has been inserted and can be read.
        Log.i(FITNESS_TAG, "Data insert was successful!");

        readFitnessData();
    }

    private void deleteAllFitnessData(){
        Calendar cal = Calendar.getInstance();
        Date now = new Date();
        cal.setTime(now);
        long endTime = cal.getTimeInMillis();
        cal.add(Calendar.YEAR, -1);
        long startTime = cal.getTimeInMillis();

        //Create delete request object, providing a data type and a time interval.
        DataDeleteRequest deleteRequest = new DataDeleteRequest.Builder()
                                                .setTimeInterval(startTime, endTime, TimeUnit.MILLISECONDS)
//                                                .addDataType(DataType.TYPE_STEP_COUNT_DELTA)
                                                .deleteAllData()
                                                .deleteAllSessions()
                                                .build();

        // Invoke the History API with the google API client object and delete request, and then
        // specify a callback that will check the result.
        Fitness.HistoryApi.deleteData(fitClient, deleteRequest)
                            .setResultCallback(new ResultCallback<Status>() {
                                @Override
                                public void onResult(Status status) {
                                    if(status.isSuccess()){
                                        Toast.makeText(getActivity(), "successfully deleted this year's step count data", Toast.LENGTH_SHORT).show();
                                        Log.d(FITNESS_TAG, "successfully deleted this year's step count data");
                                    }else{
                                        Log.d(FITNESS_TAG, "failed to delete this year's step count data");
                                    }
                                }
                            });

    }

    private void printData(DataReadResult dataReadResult) {
        // [START parse_read_data_result]
        // If the DataReadRequest object specified aggregated data, dataReadResult will be returned
        // as buckets containing DataSets, instead of just DataSets.
        Log.d(FITNESS_TAG, "bucket size: " + dataReadResult.getBuckets().size());
        listDetails.clear();
        if (dataReadResult.getBuckets().size() > 0) {
            Log.i(FITNESS_TAG, "Number of returned buckets of DataSets is: "
                    + dataReadResult.getBuckets().size());
            for (Bucket bucket : dataReadResult.getBuckets()) {
                List<DataSet> dataSets = bucket.getDataSets();
                for (DataSet dataSet : dataSets) {
                    dumpDataSet(dataSet);
                }
            }

            textView.setVisibility(View.GONE);
            listView.setVisibility(View.VISIBLE);
        } else if (dataReadResult.getDataSets().size() > 0) {
            Log.i(FITNESS_TAG, "Number of returned DataSets is: "
                    + dataReadResult.getDataSets().size());
            for (DataSet dataSet : dataReadResult.getDataSets()) {
                dumpDataSet(dataSet);
            }

            textView.setVisibility(View.GONE);
            listView.setVisibility(View.VISIBLE);
        }else{
            textView.setVisibility(View.VISIBLE);
            listView.setVisibility(View.GONE);
        }
        // [END parse_read_data_result]
    }

    // [START parse_dataset]
    private void dumpDataSet(DataSet dataSet) {
        Log.i(FITNESS_TAG, "Data returned for Data type: " + dataSet.getDataType().getName());
        SimpleDateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT);

        for (DataPoint dp : dataSet.getDataPoints()) {
            Log.i(FITNESS_TAG, "Data point:");
            Log.i(FITNESS_TAG, "\tType: " + dp.getDataType().getName());
            Log.i(FITNESS_TAG, "\tStart: " + dateFormat.format(dp.getStartTime(TimeUnit.MILLISECONDS)));
            Log.i(FITNESS_TAG, "\tEnd: " + dateFormat.format(dp.getEndTime(TimeUnit.MILLISECONDS)));

            String str = "Data point:" +
                    "\nType: " + dp.getDataType().getName() +
                    "\nStart: " + dateFormat.format(dp.getStartTime(TimeUnit.MILLISECONDS)) +
                    "\nEnd: " + dateFormat.format(dp.getEndTime(TimeUnit.MILLISECONDS));


            for(Field field : dp.getDataType().getFields()) {
                Log.i(FITNESS_TAG, "\tField: " + field.getName() +
                        " Value: " + dp.getValue(field));

                str += "\nField: " + field.getName() +
                        " Value: " + dp.getValue(field);
            }

            final String finalStr = str;
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    listDetails.add(finalStr);
                    adapter.notifyDataSetChanged();
                }
            });
        }
    }
    // [END parse_dataset]

    private boolean servicesConnected(){
        int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(getActivity());

        if(resultCode == ConnectionResult.SUCCESS){
            Log.d("Play services", "Google Play Services available");

            return true;
        }else{
            Dialog errorDialog = GooglePlayServicesUtil.getErrorDialog(resultCode, getActivity(), CONNECTION_FAILURE_RESOLUTION_REQUEST);

            if(errorDialog != null){
                ErrorDialogFragment errorFragment =  new ErrorDialogFragment();
                errorFragment.setDialog(errorDialog);
                errorFragment.show(this.getFragmentManager(), "Google Play Services");
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

        Toast.makeText(getActivity(), "Activity Recognition Stopped", Toast.LENGTH_SHORT).show();
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
                connectionResult.startResolutionForResult(getActivity(), CONNECTION_FAILURE_RESOLUTION_REQUEST);
            } catch (IntentSender.SendIntentException e) {
                e.printStackTrace();
            }
        }else{
            int errorCode = connectionResult.getErrorCode();
            Dialog errorDialog = GooglePlayServicesUtil.getErrorDialog(errorCode, getActivity(), CONNECTION_FAILURE_RESOLUTION_REQUEST);

            if(errorDialog != null){
                ErrorDialogFragment errorFragment = new ErrorDialogFragment();
                errorFragment.setDialog(errorDialog);
                errorFragment.show(this.getFragmentManager(), "Connection Failed");
            }
        }
    }

    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        Log.d("message_received", "message path: " + messageEvent.getPath());
        String messagePath = messageEvent.getPath();

        if(messagePath.equals(MESSAGE_PATH)){
//            activityRecognitionSetup();
        }else if(messagePath.contains("/date/")){
            if(fitnessClintConnected){
                String[] str2 = messagePath.split(Pattern.quote("/"));
                String[] str = str2[0].split(Pattern.quote("-"));

                final int stepCount = Integer.parseInt(str[2]);

                String[] strDate = str2[2].split(Pattern.quote("-"));
                final GregorianCalendar datePosted = new GregorianCalendar(Integer.parseInt(strDate[0]), Integer.parseInt(strDate[1]), Integer.parseInt(strDate[2]));

                Log.d("message_received", messagePath);

                if(Utils.Network.hasNetworkConnection(getActivity())){
                    new AsyncTask<Void, Void, Void>() {
                        @Override
                        protected Void doInBackground(Void... params) {
                            insertFitnessData(stepCount, datePosted);
                            return null;
                        }
                    }.execute();
                }else{
                    //TODO: send fitness data according to date

//                    Calendar cal = Calendar.getInstance();
//                    cal.setTimeInMillis(System.currentTimeMillis());

                    SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
                    String formattedDate = formatter.format(datePosted.getTime());

                    boolean hasSameDate = false;

                    ArrayList<UnsentSteps> listSteps = (ArrayList<UnsentSteps>) ObjectSerializer.loadSerializedObject(getActivity(), "MySteps", false);

                    try{
                        for(UnsentSteps stepDetail : listSteps){
                            if(stepDetail.getDate().equals(formattedDate)){
                                hasSameDate = true;

                                stepDetail.setStepCount(stepDetail.getStepCount() + stepCount);
                                break;
                            }
                        }

                        if(!hasSameDate){
                            listSteps.add(new UnsentSteps(formattedDate, stepCount));
                        }

                    }catch (NullPointerException e){
                        e.printStackTrace();
                        listSteps.add(new UnsentSteps(formattedDate, stepCount));
                    }

                    ObjectSerializer.saveSerializedObject(getActivity(), listSteps, "MySteps");
                }
            }
        }else if(messagePath.contains("/step-count")){
                if(fitnessClintConnected){
                    String[] str = messagePath.split(Pattern.quote("-"));
                    final int stepCount = Integer.parseInt(str[2]);

                    Log.d("message_received", messagePath);

                    if(Utils.Network.hasNetworkConnection(getActivity())){
                        new AsyncTask<Void, Void, Void>() {
                            @Override
                            protected Void doInBackground(Void... params) {
                                insertFitnessData(stepCount);
                                return null;
                            }
                        }.execute();
                    }else{
                        //TODO: send fitness data according to date

                        Calendar cal = Calendar.getInstance();
                        cal.setTimeInMillis(System.currentTimeMillis());

                        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
                        String formattedDate = formatter.format(cal.getTime());

                        boolean hasSameDate = false;

                        ArrayList<UnsentSteps> listSteps = (ArrayList<UnsentSteps>) ObjectSerializer.loadSerializedObject(getActivity(), "MySteps", false);

                        try{
                            for(UnsentSteps stepDetail : listSteps){
                                if(stepDetail.getDate().equals(formattedDate)){
                                    hasSameDate = true;

                                    stepDetail.setStepCount(stepDetail.getStepCount() + stepCount);
                                    break;
                                }
                            }

                            if(!hasSameDate){
                                listSteps.add(new UnsentSteps(formattedDate, stepCount));
                            }

                        }catch (NullPointerException e){
                            e.printStackTrace();
                            listSteps = new ArrayList<>();
                            listSteps.add(new UnsentSteps(formattedDate, stepCount));
                        }

                        ObjectSerializer.saveSerializedObject(getActivity(), listSteps, "MySteps");
                    }
                }
        }
    }

    public static class ErrorDialogFragment extends DialogFragment {
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

    private void initRecognitionClient(){
        recognitionClient = new GoogleApiClient.Builder(getActivity())
                .addApi(ActivityRecognition.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();

        Intent intent = new Intent(getActivity(), ActivityRecognitionIntentService.class);
        mActivityRecognitionPendingIntent = PendingIntent.getService(getActivity(), 1000, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        recognitionApi = ActivityRecognition.ActivityRecognitionApi;
    }

    private void activityRecognitionSetup(){
        Intent intent = new Intent(getActivity(), ActivityRecognitionIntentService.class);
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
        mActivityRecognitionPendingIntent = PendingIntent.getService(getActivity(), 1000, intent, PendingIntent.FLAG_UPDATE_CURRENT);

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
                Toast.makeText(getActivity(), "status: " + dataItemResult.getStatus().getStatus() + "result: " + dataItemResult.getDataItem().getUri(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private Bitmap getBitmapFromAssets(){
        AssetManager assetManager = getActivity().getAssets();
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

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p/>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        public void onFragmentInteraction(Uri uri);
    }

    private class ItemsAdapter extends BaseAdapter{
        Context context;
        ArrayList<String> list;

        public ItemsAdapter(Context context, ArrayList<String> list){
            this.context = context;
            this.list = list;
        }

        @Override
        public int getCount() {
            return list.size();
        }

        @Override
        public Object getItem(int position) {
            return list.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {

            if(convertView == null){
                convertView = new TextView(context);
            }

            try{
                ((TextView)convertView).setText(list.get(position));
            }catch (IndexOutOfBoundsException e){
                e.printStackTrace();
            }

            return convertView;
        }
    }

}
