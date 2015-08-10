package com.kfast.uitest.fragments;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DownloadManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.EditText;
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
import com.kfast.uitest.activity.SettingsActivity;
import com.kfast.uitest.model.UnsentSteps;
import com.kfast.uitest.service.ActivityRecognitionIntentService;
import com.kfast.uitest.utils.ObjectSerializer;
import com.kfast.uitest.utils.PreferenceHelper;
import com.kfast.uitest.utils.Utils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
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

//    private OnFragmentInteractionListener mListener;

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

    private boolean forClientRelease = false;

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
        listDetails = new ArrayList<String>();

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
//    public void onButtonPressed(Uri uri) {
//        if (mListener != null) {
//            mListener.onFragmentInteraction(uri);
//        }
//    }
//
//    @Override
//    public void onAttach(Activity activity) {
//        super.onAttach(activity);
//        try {
//            mListener = (OnFragmentInteractionListener) activity;
//        } catch (ClassCastException e) {
//            throw new ClassCastException(activity.toString()
//                    + " must implement OnFragmentInteractionListener");
//        }
//    }
//
//    @Override
//    public void onDetach() {
//        super.onDetach();
//        mListener = null;
//    }

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
//                startSendDataToWear();

                File grandParent = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES) + File.separator + "fitpet" + File.separator);

                if(grandParent.listFiles().length > 0){
                    for(File parent : grandParent.listFiles()){
                        if(parent.listFiles().length > 0){
                            for(File childFile : parent.listFiles()){
                                sendPetImageToWear(BitmapFactory.decodeFile(childFile.getAbsolutePath()), parent.getName(), childFile.getName());
//                                Log.d("path_to_send", "child absolute path: " + childFile.getAbsolutePath() + " parent name: " + parent.getName() + " child name: " + childFile.getName());
                            }
                        }
                    }
                }
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

            case R.id.action_notif:
                processNotification(null, false);
                break;

            case R.id.action_download:

                if(!forClientRelease){
                    //without ip address implementation
                    ArrayList<String> listUrls = new ArrayList<String>();
                    listUrls.add("http://192.168.8.105/fitpet/cat_animations_compressed/cat_running_compressed/cat_running0001.png");
                    listUrls.add("http://192.168.8.105/fitpet/cat_animations_compressed/cat_running_compressed/cat_running0002.png");
                    listUrls.add("http://192.168.8.105/fitpet/cat_animations_compressed/cat_running_compressed/cat_running0003.png");
                    listUrls.add("http://192.168.8.105/fitpet/cat_animations_compressed/cat_running_compressed/cat_running0004.png");
                    listUrls.add("http://192.168.8.105/fitpet/cat_animations_compressed/cat_running_compressed/cat_running0005.png");
                    listUrls.add("http://192.168.8.105/fitpet/cat_animations_compressed/cat_running_compressed/cat_running0006.png");
                    listUrls.add("http://192.168.8.105/fitpet/cat_animations_compressed/cat_running_compressed/cat_running0007.png");
                    listUrls.add("http://192.168.8.105/fitpet/cat_animations_compressed/cat_running_compressed/cat_running0008.png");
                    listUrls.add("http://192.168.8.105/fitpet/cat_animations_compressed/cat_running_compressed/cat_running0009.png");
                    listUrls.add("http://192.168.8.105/fitpet/cat_animations_compressed/cat_running_compressed/cat_running0010.png");
                    listUrls.add("http://192.168.8.105/fitpet/cat_animations_compressed/cat_running_compressed/cat_running0011.png");
                    listUrls.add("http://192.168.8.105/fitpet/cat_animations_compressed/cat_running_compressed/cat_running0012.png");
                    listUrls.add("http://192.168.8.105/fitpet/cat_animations_compressed/cat_running_compressed/cat_running0013.png");
                    listUrls.add("http://192.168.8.105/fitpet/cat_animations_compressed/cat_running_compressed/cat_running0014.png");
                    //cat walking
                    listUrls.add("http://192.168.8.105/fitpet/cat_animations_compressed/cat_walking_compressed/cat_walking0001.png");
                    listUrls.add("http://192.168.8.105/fitpet/cat_animations_compressed/cat_walking_compressed/cat_walking0002.png");
                    listUrls.add("http://192.168.8.105/fitpet/cat_animations_compressed/cat_walking_compressed/cat_walking0003.png");
                    listUrls.add("http://192.168.8.105/fitpet/cat_animations_compressed/cat_walking_compressed/cat_walking0004.png");
                    listUrls.add("http://192.168.8.105/fitpet/cat_animations_compressed/cat_walking_compressed/cat_walking0005.png");
                    listUrls.add("http://192.168.8.105/fitpet/cat_animations_compressed/cat_walking_compressed/cat_walking0006.png");
                    listUrls.add("http://192.168.8.105/fitpet/cat_animations_compressed/cat_walking_compressed/cat_walking0007.png");
                    listUrls.add("http://192.168.8.105/fitpet/cat_animations_compressed/cat_walking_compressed/cat_walking0008.png");
                    listUrls.add("http://192.168.8.105/fitpet/cat_animations_compressed/cat_walking_compressed/cat_walking0009.png");
                    listUrls.add("http://192.168.8.105/fitpet/cat_animations_compressed/cat_walking_compressed/cat_walking0010.png");
                    listUrls.add("http://192.168.8.105/fitpet/cat_animations_compressed/cat_walking_compressed/cat_walking0011.png");
                    listUrls.add("http://192.168.8.105/fitpet/cat_animations_compressed/cat_walking_compressed/cat_walking0012.png");
                    listUrls.add("http://192.168.8.105/fitpet/cat_animations_compressed/cat_walking_compressed/cat_walking0013.png");
                    listUrls.add("http://192.168.8.105/fitpet/cat_animations_compressed/cat_walking_compressed/cat_walking0014.png");
                    listUrls.add("http://192.168.8.105/fitpet/cat_animations_compressed/cat_walking_compressed/cat_walking0015.png");
                    listUrls.add("http://192.168.8.105/fitpet/cat_animations_compressed/cat_walking_compressed/cat_walking0016.png");
                    listUrls.add("http://192.168.8.105/fitpet/cat_animations_compressed/cat_walking_compressed/cat_walking0017.png");
                    listUrls.add("http://192.168.8.105/fitpet/cat_animations_compressed/cat_walking_compressed/cat_walking0018.png");
                    listUrls.add("http://192.168.8.105/fitpet/cat_animations_compressed/cat_walking_compressed/cat_walking0019.png");
                    listUrls.add("http://192.168.8.105/fitpet/cat_animations_compressed/cat_walking_compressed/cat_walking0020.png");
                    listUrls.add("http://192.168.8.105/fitpet/cat_animations_compressed/cat_walking_compressed/cat_walking0021.png");
                    listUrls.add("http://192.168.8.105/fitpet/cat_animations_compressed/cat_walking_compressed/cat_walking0022.png");
                    listUrls.add("http://192.168.8.105/fitpet/cat_animations_compressed/cat_walking_compressed/cat_walking0023.png");
                    listUrls.add("http://192.168.8.105/fitpet/cat_animations_compressed/cat_walking_compressed/cat_walking0024.png");

                    //cat sill 1
                    listUrls.add("http://192.168.8.105/fitpet/cat_animations_compressed/cat_still1_compressed/cat_still10001.png");
                    listUrls.add("http://192.168.8.105/fitpet/cat_animations_compressed/cat_still1_compressed/cat_still10002.png");
                    listUrls.add("http://192.168.8.105/fitpet/cat_animations_compressed/cat_still1_compressed/cat_still10003.png");
                    listUrls.add("http://192.168.8.105/fitpet/cat_animations_compressed/cat_still1_compressed/cat_still10004.png");
                    listUrls.add("http://192.168.8.105/fitpet/cat_animations_compressed/cat_still1_compressed/cat_still10005.png");
                    listUrls.add("http://192.168.8.105/fitpet/cat_animations_compressed/cat_still1_compressed/cat_still10006.png");
                    listUrls.add("http://192.168.8.105/fitpet/cat_animations_compressed/cat_still1_compressed/cat_still10007.png");
                    listUrls.add("http://192.168.8.105/fitpet/cat_animations_compressed/cat_still1_compressed/cat_still10008.png");
                    listUrls.add("http://192.168.8.105/fitpet/cat_animations_compressed/cat_still1_compressed/cat_still10009.png");
                    listUrls.add("http://192.168.8.105/fitpet/cat_animations_compressed/cat_still1_compressed/cat_still10010.png");
                    listUrls.add("http://192.168.8.105/fitpet/cat_animations_compressed/cat_still1_compressed/cat_still10011.png");
                    listUrls.add("http://192.168.8.105/fitpet/cat_animations_compressed/cat_still1_compressed/cat_still10012.png");
                    listUrls.add("http://192.168.8.105/fitpet/cat_animations_compressed/cat_still1_compressed/cat_still10013.png");
                    listUrls.add("http://192.168.8.105/fitpet/cat_animations_compressed/cat_still1_compressed/cat_still10014.png");
                    listUrls.add("http://192.168.8.105/fitpet/cat_animations_compressed/cat_still1_compressed/cat_still10015.png");
                    listUrls.add("http://192.168.8.105/fitpet/cat_animations_compressed/cat_still1_compressed/cat_still10016.png");
                    listUrls.add("http://192.168.8.105/fitpet/cat_animations_compressed/cat_still1_compressed/cat_still10017.png");
                    listUrls.add("http://192.168.8.105/fitpet/cat_animations_compressed/cat_still1_compressed/cat_still10018.png");
                    listUrls.add("http://192.168.8.105/fitpet/cat_animations_compressed/cat_still1_compressed/cat_still10019.png");
                    listUrls.add("http://192.168.8.105/fitpet/cat_animations_compressed/cat_still1_compressed/cat_still10020.png");
                    listUrls.add("http://192.168.8.105/fitpet/cat_animations_compressed/cat_still1_compressed/cat_still10021.png");
                    listUrls.add("http://192.168.8.105/fitpet/cat_animations_compressed/cat_still1_compressed/cat_still10022.png");
                    listUrls.add("http://192.168.8.105/fitpet/cat_animations_compressed/cat_still1_compressed/cat_still10023.png");
                    listUrls.add("http://192.168.8.105/fitpet/cat_animations_compressed/cat_still1_compressed/cat_still10024.png");
                    listUrls.add("http://192.168.8.105/fitpet/cat_animations_compressed/cat_still1_compressed/cat_still10025.png");
                    listUrls.add("http://192.168.8.105/fitpet/cat_animations_compressed/cat_still1_compressed/cat_still10026.png");
                    listUrls.add("http://192.168.8.105/fitpet/cat_animations_compressed/cat_still1_compressed/cat_still10027.png");
                    listUrls.add("http://192.168.8.105/fitpet/cat_animations_compressed/cat_still1_compressed/cat_still10028.png");
                    listUrls.add("http://192.168.8.105/fitpet/cat_animations_compressed/cat_still1_compressed/cat_still10029.png");
                    listUrls.add("http://192.168.8.105/fitpet/cat_animations_compressed/cat_still1_compressed/cat_still10030.png");
                    listUrls.add("http://192.168.8.105/fitpet/cat_animations_compressed/cat_still1_compressed/cat_still10031.png");

                    //cat still 2
                    listUrls.add("http://192.168.8.105/fitpet/cat_animations_compressed/cat_still2_compressed/cat_still20001.png");
                    listUrls.add("http://192.168.8.105/fitpet/cat_animations_compressed/cat_still2_compressed/cat_still20002.png");
                    listUrls.add("http://192.168.8.105/fitpet/cat_animations_compressed/cat_still2_compressed/cat_still20003.png");
                    listUrls.add("http://192.168.8.105/fitpet/cat_animations_compressed/cat_still2_compressed/cat_still20004.png");
                    listUrls.add("http://192.168.8.105/fitpet/cat_animations_compressed/cat_still2_compressed/cat_still20005.png");
                    listUrls.add("http://192.168.8.105/fitpet/cat_animations_compressed/cat_still2_compressed/cat_still20006.png");
                    listUrls.add("http://192.168.8.105/fitpet/cat_animations_compressed/cat_still2_compressed/cat_still20007.png");
                    listUrls.add("http://192.168.8.105/fitpet/cat_animations_compressed/cat_still2_compressed/cat_still20010.png");
                    listUrls.add("http://192.168.8.105/fitpet/cat_animations_compressed/cat_still2_compressed/cat_still20011.png");
                    listUrls.add("http://192.168.8.105/fitpet/cat_animations_compressed/cat_still2_compressed/cat_still20009.png");
                    listUrls.add("http://192.168.8.105/fitpet/cat_animations_compressed/cat_still2_compressed/cat_still20008.png");
                    listUrls.add("http://192.168.8.105/fitpet/cat_animations_compressed/cat_still2_compressed/cat_still20012.png");
                    listUrls.add("http://192.168.8.105/fitpet/cat_animations_compressed/cat_still2_compressed/cat_still20013.png");
                    listUrls.add("http://192.168.8.105/fitpet/cat_animations_compressed/cat_still2_compressed/cat_still20014.png");
                    listUrls.add("http://192.168.8.105/fitpet/cat_animations_compressed/cat_still2_compressed/cat_still20015.png");
                    listUrls.add("http://192.168.8.105/fitpet/cat_animations_compressed/cat_still2_compressed/cat_still20016.png");
                    listUrls.add("http://192.168.8.105/fitpet/cat_animations_compressed/cat_still2_compressed/cat_still20017.png");
                    listUrls.add("http://192.168.8.105/fitpet/cat_animations_compressed/cat_still2_compressed/cat_still20018.png");
                    listUrls.add("http://192.168.8.105/fitpet/cat_animations_compressed/cat_still2_compressed/cat_still20019.png");
                    listUrls.add("http://192.168.8.105/fitpet/cat_animations_compressed/cat_still2_compressed/cat_still20020.png");
                    listUrls.add("http://192.168.8.105/fitpet/cat_animations_compressed/cat_still2_compressed/cat_still20021.png");
                    listUrls.add("http://192.168.8.105/fitpet/cat_animations_compressed/cat_still2_compressed/cat_still20022.png");
                    listUrls.add("http://192.168.8.105/fitpet/cat_animations_compressed/cat_still2_compressed/cat_still20023.png");
                    listUrls.add("http://192.168.8.105/fitpet/cat_animations_compressed/cat_still2_compressed/cat_still20024.png");
                    listUrls.add("http://192.168.8.105/fitpet/cat_animations_compressed/cat_still2_compressed/cat_still20025.png");
                    listUrls.add("http://192.168.8.105/fitpet/cat_animations_compressed/cat_still2_compressed/cat_still20026.png");
                    listUrls.add("http://192.168.8.105/fitpet/cat_animations_compressed/cat_still2_compressed/cat_still20027.png");
                    listUrls.add("http://192.168.8.105/fitpet/cat_animations_compressed/cat_still2_compressed/cat_still20028.png");
                    listUrls.add("http://192.168.8.105/fitpet/cat_animations_compressed/cat_still2_compressed/cat_still20029.png");
                    listUrls.add("http://192.168.8.105/fitpet/cat_animations_compressed/cat_still2_compressed/cat_still20030.png");
                    listUrls.add("http://192.168.8.105/fitpet/cat_animations_compressed/cat_still2_compressed/cat_still20031.png");

                    //cat still 3
                    listUrls.add("http://192.168.8.105/fitpet/cat_animations_compressed/cat_still3_compressed/cat_still30001.png");
                    listUrls.add("http://192.168.8.105/fitpet/cat_animations_compressed/cat_still3_compressed/cat_still30002.png");
                    listUrls.add("http://192.168.8.105/fitpet/cat_animations_compressed/cat_still3_compressed/cat_still30003.png");
                    listUrls.add("http://192.168.8.105/fitpet/cat_animations_compressed/cat_still3_compressed/cat_still30004.png");
                    listUrls.add("http://192.168.8.105/fitpet/cat_animations_compressed/cat_still3_compressed/cat_still30005.png");
                    listUrls.add("http://192.168.8.105/fitpet/cat_animations_compressed/cat_still3_compressed/cat_still30006.png");
                    listUrls.add("http://192.168.8.105/fitpet/cat_animations_compressed/cat_still3_compressed/cat_still30007.png");
                    listUrls.add("http://192.168.8.105/fitpet/cat_animations_compressed/cat_still3_compressed/cat_still30010.png");
                    listUrls.add("http://192.168.8.105/fitpet/cat_animations_compressed/cat_still3_compressed/cat_still30011.png");
                    listUrls.add("http://192.168.8.105/fitpet/cat_animations_compressed/cat_still3_compressed/cat_still30009.png");
                    listUrls.add("http://192.168.8.105/fitpet/cat_animations_compressed/cat_still3_compressed/cat_still30008.png");
                    listUrls.add("http://192.168.8.105/fitpet/cat_animations_compressed/cat_still3_compressed/cat_still30012.png");
                    listUrls.add("http://192.168.8.105/fitpet/cat_animations_compressed/cat_still3_compressed/cat_still30013.png");
                    listUrls.add("http://192.168.8.105/fitpet/cat_animations_compressed/cat_still3_compressed/cat_still30014.png");
                    listUrls.add("http://192.168.8.105/fitpet/cat_animations_compressed/cat_still3_compressed/cat_still30015.png");
                    listUrls.add("http://192.168.8.105/fitpet/cat_animations_compressed/cat_still3_compressed/cat_still30016.png");
                    listUrls.add("http://192.168.8.105/fitpet/cat_animations_compressed/cat_still3_compressed/cat_still30017.png");
                    listUrls.add("http://192.168.8.105/fitpet/cat_animations_compressed/cat_still3_compressed/cat_still30018.png");
                    listUrls.add("http://192.168.8.105/fitpet/cat_animations_compressed/cat_still3_compressed/cat_still30019.png");
                    listUrls.add("http://192.168.8.105/fitpet/cat_animations_compressed/cat_still3_compressed/cat_still30020.png");
                    listUrls.add("http://192.168.8.105/fitpet/cat_animations_compressed/cat_still3_compressed/cat_still30021.png");
                    listUrls.add("http://192.168.8.105/fitpet/cat_animations_compressed/cat_still3_compressed/cat_still30022.png");
                    listUrls.add("http://192.168.8.105/fitpet/cat_animations_compressed/cat_still3_compressed/cat_still30023.png");
                    listUrls.add("http://192.168.8.105/fitpet/cat_animations_compressed/cat_still3_compressed/cat_still30024.png");
                    listUrls.add("http://192.168.8.105/fitpet/cat_animations_compressed/cat_still3_compressed/cat_still30025.png");
                    listUrls.add("http://192.168.8.105/fitpet/cat_animations_compressed/cat_still3_compressed/cat_still30026.png");
                    listUrls.add("http://192.168.8.105/fitpet/cat_animations_compressed/cat_still3_compressed/cat_still30027.png");
                    listUrls.add("http://192.168.8.105/fitpet/cat_animations_compressed/cat_still3_compressed/cat_still30028.png");
                    listUrls.add("http://192.168.8.105/fitpet/cat_animations_compressed/cat_still3_compressed/cat_still30029.png");
                    listUrls.add("http://192.168.8.105/fitpet/cat_animations_compressed/cat_still3_compressed/cat_still30030.png");

                    //cat trick 1
                    listUrls.add("http://192.168.8.105/fitpet/cat_animations_compressed/cat_trick1_compressed/cat_trick10001.png");
                    listUrls.add("http://192.168.8.105/fitpet/cat_animations_compressed/cat_trick1_compressed/cat_trick10002.png");
                    listUrls.add("http://192.168.8.105/fitpet/cat_animations_compressed/cat_trick1_compressed/cat_trick10003.png");
                    listUrls.add("http://192.168.8.105/fitpet/cat_animations_compressed/cat_trick1_compressed/cat_trick10004.png");
                    listUrls.add("http://192.168.8.105/fitpet/cat_animations_compressed/cat_trick1_compressed/cat_trick10005.png");
                    listUrls.add("http://192.168.8.105/fitpet/cat_animations_compressed/cat_trick1_compressed/cat_trick10006.png");
                    listUrls.add("http://192.168.8.105/fitpet/cat_animations_compressed/cat_trick1_compressed/cat_trick10007.png");
                    listUrls.add("http://192.168.8.105/fitpet/cat_animations_compressed/cat_trick1_compressed/cat_trick10008.png");
                    listUrls.add("http://192.168.8.105/fitpet/cat_animations_compressed/cat_trick1_compressed/cat_trick10009.png");
                    listUrls.add("http://192.168.8.105/fitpet/cat_animations_compressed/cat_trick1_compressed/cat_trick10010.png");
                    listUrls.add("http://192.168.8.105/fitpet/cat_animations_compressed/cat_trick1_compressed/cat_trick10011.png");
                    listUrls.add("http://192.168.8.105/fitpet/cat_animations_compressed/cat_trick1_compressed/cat_trick10012.png");
                    listUrls.add("http://192.168.8.105/fitpet/cat_animations_compressed/cat_trick1_compressed/cat_trick10013.png");
                    listUrls.add("http://192.168.8.105/fitpet/cat_animations_compressed/cat_trick1_compressed/cat_trick10014.png");
                    listUrls.add("http://192.168.8.105/fitpet/cat_animations_compressed/cat_trick1_compressed/cat_trick10015.png");
                    listUrls.add("http://192.168.8.105/fitpet/cat_animations_compressed/cat_trick1_compressed/cat_trick10016.png");
                    listUrls.add("http://192.168.8.105/fitpet/cat_animations_compressed/cat_trick1_compressed/cat_trick10017.png");
                    listUrls.add("http://192.168.8.105/fitpet/cat_animations_compressed/cat_trick1_compressed/cat_trick10018.png");
                    listUrls.add("http://192.168.8.105/fitpet/cat_animations_compressed/cat_trick1_compressed/cat_trick10019.png");
                    listUrls.add("http://192.168.8.105/fitpet/cat_animations_compressed/cat_trick1_compressed/cat_trick10020.png");
                    listUrls.add("http://192.168.8.105/fitpet/cat_animations_compressed/cat_trick1_compressed/cat_trick10021.png");
                    listUrls.add("http://192.168.8.105/fitpet/cat_animations_compressed/cat_trick1_compressed/cat_trick10022.png");
                    listUrls.add("http://192.168.8.105/fitpet/cat_animations_compressed/cat_trick1_compressed/cat_trick10023.png");
                    listUrls.add("http://192.168.8.105/fitpet/cat_animations_compressed/cat_trick1_compressed/cat_trick10024.png");
                    listUrls.add("http://192.168.8.105/fitpet/cat_animations_compressed/cat_trick1_compressed/cat_trick10025.png");
                    listUrls.add("http://192.168.8.105/fitpet/cat_animations_compressed/cat_trick1_compressed/cat_trick10026.png");
                    listUrls.add("http://192.168.8.105/fitpet/cat_animations_compressed/cat_trick1_compressed/cat_trick10027.png");
                    listUrls.add("http://192.168.8.105/fitpet/cat_animations_compressed/cat_trick1_compressed/cat_trick10028.png");
                    listUrls.add("http://192.168.8.105/fitpet/cat_animations_compressed/cat_trick1_compressed/cat_trick10029.png");
                    listUrls.add("http://192.168.8.105/fitpet/cat_animations_compressed/cat_trick1_compressed/cat_trick10030.png");
                    listUrls.add("http://192.168.8.105/fitpet/cat_animations_compressed/cat_trick1_compressed/cat_trick10031.png");

                    //cat trick 2
                    listUrls.add("http://192.168.8.105/fitpet/cat_animations_compressed/cat_trick2_compressed/cat_trick20001.png");
                    listUrls.add("http://192.168.8.105/fitpet/cat_animations_compressed/cat_trick2_compressed/cat_trick20002.png");
                    listUrls.add("http://192.168.8.105/fitpet/cat_animations_compressed/cat_trick2_compressed/cat_trick20003.png");
                    listUrls.add("http://192.168.8.105/fitpet/cat_animations_compressed/cat_trick2_compressed/cat_trick20004.png");
                    listUrls.add("http://192.168.8.105/fitpet/cat_animations_compressed/cat_trick2_compressed/cat_trick20005.png");
                    listUrls.add("http://192.168.8.105/fitpet/cat_animations_compressed/cat_trick2_compressed/cat_trick20006.png");
                    listUrls.add("http://192.168.8.105/fitpet/cat_animations_compressed/cat_trick2_compressed/cat_trick20007.png");
                    listUrls.add("http://192.168.8.105/fitpet/cat_animations_compressed/cat_trick2_compressed/cat_trick20010.png");
                    listUrls.add("http://192.168.8.105/fitpet/cat_animations_compressed/cat_trick2_compressed/cat_trick20011.png");
                    listUrls.add("http://192.168.8.105/fitpet/cat_animations_compressed/cat_trick2_compressed/cat_trick20009.png");
                    listUrls.add("http://192.168.8.105/fitpet/cat_animations_compressed/cat_trick2_compressed/cat_trick20008.png");
                    listUrls.add("http://192.168.8.105/fitpet/cat_animations_compressed/cat_trick2_compressed/cat_trick20012.png");
                    listUrls.add("http://192.168.8.105/fitpet/cat_animations_compressed/cat_trick2_compressed/cat_trick20013.png");
                    listUrls.add("http://192.168.8.105/fitpet/cat_animations_compressed/cat_trick2_compressed/cat_trick20014.png");
                    listUrls.add("http://192.168.8.105/fitpet/cat_animations_compressed/cat_trick2_compressed/cat_trick20015.png");
                    listUrls.add("http://192.168.8.105/fitpet/cat_animations_compressed/cat_trick2_compressed/cat_trick20016.png");
                    listUrls.add("http://192.168.8.105/fitpet/cat_animations_compressed/cat_trick2_compressed/cat_trick20017.png");
                    listUrls.add("http://192.168.8.105/fitpet/cat_animations_compressed/cat_trick2_compressed/cat_trick20018.png");
                    listUrls.add("http://192.168.8.105/fitpet/cat_animations_compressed/cat_trick2_compressed/cat_trick20019.png");
                    listUrls.add("http://192.168.8.105/fitpet/cat_animations_compressed/cat_trick2_compressed/cat_trick20020.png");
                    listUrls.add("http://192.168.8.105/fitpet/cat_animations_compressed/cat_trick2_compressed/cat_trick20021.png");
                    listUrls.add("http://192.168.8.105/fitpet/cat_animations_compressed/cat_trick2_compressed/cat_trick20022.png");
                    listUrls.add("http://192.168.8.105/fitpet/cat_animations_compressed/cat_trick2_compressed/cat_trick20023.png");
                    listUrls.add("http://192.168.8.105/fitpet/cat_animations_compressed/cat_trick2_compressed/cat_trick20024.png");
                    listUrls.add("http://192.168.8.105/fitpet/cat_animations_compressed/cat_trick2_compressed/cat_trick20025.png");
                    listUrls.add("http://192.168.8.105/fitpet/cat_animations_compressed/cat_trick2_compressed/cat_trick20026.png");
                    listUrls.add("http://192.168.8.105/fitpet/cat_animations_compressed/cat_trick2_compressed/cat_trick20027.png");
                    listUrls.add("http://192.168.8.105/fitpet/cat_animations_compressed/cat_trick2_compressed/cat_trick20028.png");
                    listUrls.add("http://192.168.8.105/fitpet/cat_animations_compressed/cat_trick2_compressed/cat_trick20029.png");
                    listUrls.add("http://192.168.8.105/fitpet/cat_animations_compressed/cat_trick2_compressed/cat_trick20030.png");
                    listUrls.add("http://192.168.8.105/fitpet/cat_animations_compressed/cat_trick2_compressed/cat_trick20031.png");
                    listUrls.add("http://192.168.8.105/fitpet/cat_animations_compressed/cat_trick2_compressed/cat_trick20032.png");

                    //cat trick 3
                    listUrls.add("http://192.168.8.105/fitpet/cat_animations_compressed/cat_trick3_compressed/cat_trick30001.png");
                    listUrls.add("http://192.168.8.105/fitpet/cat_animations_compressed/cat_trick3_compressed/cat_trick30002.png");
                    listUrls.add("http://192.168.8.105/fitpet/cat_animations_compressed/cat_trick3_compressed/cat_trick30003.png");
                    listUrls.add("http://192.168.8.105/fitpet/cat_animations_compressed/cat_trick3_compressed/cat_trick30004.png");
                    listUrls.add("http://192.168.8.105/fitpet/cat_animations_compressed/cat_trick3_compressed/cat_trick30005.png");
                    listUrls.add("http://192.168.8.105/fitpet/cat_animations_compressed/cat_trick3_compressed/cat_trick30006.png");
                    listUrls.add("http://192.168.8.105/fitpet/cat_animations_compressed/cat_trick3_compressed/cat_trick30007.png");
                    listUrls.add("http://192.168.8.105/fitpet/cat_animations_compressed/cat_trick3_compressed/cat_trick30010.png");
                    listUrls.add("http://192.168.8.105/fitpet/cat_animations_compressed/cat_trick3_compressed/cat_trick30011.png");
                    listUrls.add("http://192.168.8.105/fitpet/cat_animations_compressed/cat_trick3_compressed/cat_trick30009.png");
                    listUrls.add("http://192.168.8.105/fitpet/cat_animations_compressed/cat_trick3_compressed/cat_trick30008.png");
                    listUrls.add("http://192.168.8.105/fitpet/cat_animations_compressed/cat_trick3_compressed/cat_trick30012.png");
                    listUrls.add("http://192.168.8.105/fitpet/cat_animations_compressed/cat_trick3_compressed/cat_trick30013.png");
                    listUrls.add("http://192.168.8.105/fitpet/cat_animations_compressed/cat_trick3_compressed/cat_trick30014.png");
                    listUrls.add("http://192.168.8.105/fitpet/cat_animations_compressed/cat_trick3_compressed/cat_trick30015.png");
                    listUrls.add("http://192.168.8.105/fitpet/cat_animations_compressed/cat_trick3_compressed/cat_trick30016.png");
                    listUrls.add("http://192.168.8.105/fitpet/cat_animations_compressed/cat_trick3_compressed/cat_trick30017.png");
                    listUrls.add("http://192.168.8.105/fitpet/cat_animations_compressed/cat_trick3_compressed/cat_trick30018.png");
                    listUrls.add("http://192.168.8.105/fitpet/cat_animations_compressed/cat_trick3_compressed/cat_trick30019.png");
                    listUrls.add("http://192.168.8.105/fitpet/cat_animations_compressed/cat_trick3_compressed/cat_trick30020.png");
                    listUrls.add("http://192.168.8.105/fitpet/cat_animations_compressed/cat_trick3_compressed/cat_trick30021.png");
                    listUrls.add("http://192.168.8.105/fitpet/cat_animations_compressed/cat_trick3_compressed/cat_trick30022.png");
                    listUrls.add("http://192.168.8.105/fitpet/cat_animations_compressed/cat_trick3_compressed/cat_trick30023.png");
                    listUrls.add("http://192.168.8.105/fitpet/cat_animations_compressed/cat_trick3_compressed/cat_trick30024.png");
                    listUrls.add("http://192.168.8.105/fitpet/cat_animations_compressed/cat_trick3_compressed/cat_trick30025.png");
                    listUrls.add("http://192.168.8.105/fitpet/cat_animations_compressed/cat_trick3_compressed/cat_trick30026.png");
                    listUrls.add("http://192.168.8.105/fitpet/cat_animations_compressed/cat_trick3_compressed/cat_trick30027.png");
                    listUrls.add("http://192.168.8.105/fitpet/cat_animations_compressed/cat_trick3_compressed/cat_trick30028.png");
                    listUrls.add("http://192.168.8.105/fitpet/cat_animations_compressed/cat_trick3_compressed/cat_trick30029.png");
                    listUrls.add("http://192.168.8.105/fitpet/cat_animations_compressed/cat_trick3_compressed/cat_trick30030.png");
                    listUrls.add("http://192.168.8.105/fitpet/cat_animations_compressed/cat_trick3_compressed/cat_trick30031.png");
                    listUrls.add("http://192.168.8.105/fitpet/cat_animations_compressed/cat_trick3_compressed/cat_trick30032.png");

                    //cat trick 4
                    listUrls.add("http://192.168.8.105/fitpet/cat_animations_compressed/cat_trick4_compressed/cat_trick40001.png");
                    listUrls.add("http://192.168.8.105/fitpet/cat_animations_compressed/cat_trick4_compressed/cat_trick40002.png");
                    listUrls.add("http://192.168.8.105/fitpet/cat_animations_compressed/cat_trick4_compressed/cat_trick40003.png");
                    listUrls.add("http://192.168.8.105/fitpet/cat_animations_compressed/cat_trick4_compressed/cat_trick40004.png");
                    listUrls.add("http://192.168.8.105/fitpet/cat_animations_compressed/cat_trick4_compressed/cat_trick40005.png");
                    listUrls.add("http://192.168.8.105/fitpet/cat_animations_compressed/cat_trick4_compressed/cat_trick40006.png");
                    listUrls.add("http://192.168.8.105/fitpet/cat_animations_compressed/cat_trick4_compressed/cat_trick40007.png");
                    listUrls.add("http://192.168.8.105/fitpet/cat_animations_compressed/cat_trick4_compressed/cat_trick40010.png");
                    listUrls.add("http://192.168.8.105/fitpet/cat_animations_compressed/cat_trick4_compressed/cat_trick40011.png");
                    listUrls.add("http://192.168.8.105/fitpet/cat_animations_compressed/cat_trick4_compressed/cat_trick40009.png");
                    listUrls.add("http://192.168.8.105/fitpet/cat_animations_compressed/cat_trick4_compressed/cat_trick40008.png");
                    listUrls.add("http://192.168.8.105/fitpet/cat_animations_compressed/cat_trick4_compressed/cat_trick40012.png");
                    listUrls.add("http://192.168.8.105/fitpet/cat_animations_compressed/cat_trick4_compressed/cat_trick40013.png");
                    listUrls.add("http://192.168.8.105/fitpet/cat_animations_compressed/cat_trick4_compressed/cat_trick40014.png");
                    listUrls.add("http://192.168.8.105/fitpet/cat_animations_compressed/cat_trick4_compressed/cat_trick40015.png");
                    listUrls.add("http://192.168.8.105/fitpet/cat_animations_compressed/cat_trick4_compressed/cat_trick40016.png");
                    listUrls.add("http://192.168.8.105/fitpet/cat_animations_compressed/cat_trick4_compressed/cat_trick40017.png");
                    listUrls.add("http://192.168.8.105/fitpet/cat_animations_compressed/cat_trick4_compressed/cat_trick40018.png");
                    listUrls.add("http://192.168.8.105/fitpet/cat_animations_compressed/cat_trick4_compressed/cat_trick40019.png");
                    listUrls.add("http://192.168.8.105/fitpet/cat_animations_compressed/cat_trick4_compressed/cat_trick40020.png");
                    listUrls.add("http://192.168.8.105/fitpet/cat_animations_compressed/cat_trick4_compressed/cat_trick40021.png");
                    listUrls.add("http://192.168.8.105/fitpet/cat_animations_compressed/cat_trick4_compressed/cat_trick40022.png");
                    listUrls.add("http://192.168.8.105/fitpet/cat_animations_compressed/cat_trick4_compressed/cat_trick40023.png");
                    listUrls.add("http://192.168.8.105/fitpet/cat_animations_compressed/cat_trick4_compressed/cat_trick40024.png");
                    listUrls.add("http://192.168.8.105/fitpet/cat_animations_compressed/cat_trick4_compressed/cat_trick40025.png");
                    listUrls.add("http://192.168.8.105/fitpet/cat_animations_compressed/cat_trick4_compressed/cat_trick40026.png");
                    listUrls.add("http://192.168.8.105/fitpet/cat_animations_compressed/cat_trick4_compressed/cat_trick40027.png");
                    listUrls.add("http://192.168.8.105/fitpet/cat_animations_compressed/cat_trick4_compressed/cat_trick40028.png");
                    listUrls.add("http://192.168.8.105/fitpet/cat_animations_compressed/cat_trick4_compressed/cat_trick40029.png");
                    listUrls.add("http://192.168.8.105/fitpet/cat_animations_compressed/cat_trick4_compressed/cat_trick40030.png");
                    listUrls.add("http://192.168.8.105/fitpet/cat_animations_compressed/cat_trick4_compressed/cat_trick40031.png");
                    listUrls.add("http://192.168.8.105/fitpet/cat_animations_compressed/cat_trick4_compressed/cat_trick40032.png");

                    //cat trick 5
                    listUrls.add("http://192.168.8.105/fitpet/cat_animations_compressed/cat_trick5_compressed/cat_trick50001.png");
                    listUrls.add("http://192.168.8.105/fitpet/cat_animations_compressed/cat_trick5_compressed/cat_trick50002.png");
                    listUrls.add("http://192.168.8.105/fitpet/cat_animations_compressed/cat_trick5_compressed/cat_trick50003.png");
                    listUrls.add("http://192.168.8.105/fitpet/cat_animations_compressed/cat_trick5_compressed/cat_trick50004.png");
                    listUrls.add("http://192.168.8.105/fitpet/cat_animations_compressed/cat_trick5_compressed/cat_trick50005.png");
                    listUrls.add("http://192.168.8.105/fitpet/cat_animations_compressed/cat_trick5_compressed/cat_trick50006.png");
                    listUrls.add("http://192.168.8.105/fitpet/cat_animations_compressed/cat_trick5_compressed/cat_trick50007.png");
                    listUrls.add("http://192.168.8.105/fitpet/cat_animations_compressed/cat_trick5_compressed/cat_trick50010.png");
                    listUrls.add("http://192.168.8.105/fitpet/cat_animations_compressed/cat_trick5_compressed/cat_trick50011.png");
                    listUrls.add("http://192.168.8.105/fitpet/cat_animations_compressed/cat_trick5_compressed/cat_trick50009.png");
                    listUrls.add("http://192.168.8.105/fitpet/cat_animations_compressed/cat_trick5_compressed/cat_trick50008.png");
                    listUrls.add("http://192.168.8.105/fitpet/cat_animations_compressed/cat_trick5_compressed/cat_trick50012.png");
                    listUrls.add("http://192.168.8.105/fitpet/cat_animations_compressed/cat_trick5_compressed/cat_trick50013.png");
                    listUrls.add("http://192.168.8.105/fitpet/cat_animations_compressed/cat_trick5_compressed/cat_trick50014.png");
                    listUrls.add("http://192.168.8.105/fitpet/cat_animations_compressed/cat_trick5_compressed/cat_trick50015.png");
                    listUrls.add("http://192.168.8.105/fitpet/cat_animations_compressed/cat_trick5_compressed/cat_trick50016.png");
                    listUrls.add("http://192.168.8.105/fitpet/cat_animations_compressed/cat_trick5_compressed/cat_trick50017.png");
                    listUrls.add("http://192.168.8.105/fitpet/cat_animations_compressed/cat_trick5_compressed/cat_trick50018.png");
                    listUrls.add("http://192.168.8.105/fitpet/cat_animations_compressed/cat_trick5_compressed/cat_trick50019.png");
                    listUrls.add("http://192.168.8.105/fitpet/cat_animations_compressed/cat_trick5_compressed/cat_trick50020.png");
                    listUrls.add("http://192.168.8.105/fitpet/cat_animations_compressed/cat_trick5_compressed/cat_trick50021.png");
                    listUrls.add("http://192.168.8.105/fitpet/cat_animations_compressed/cat_trick5_compressed/cat_trick50022.png");
                    listUrls.add("http://192.168.8.105/fitpet/cat_animations_compressed/cat_trick5_compressed/cat_trick50023.png");
                    listUrls.add("http://192.168.8.105/fitpet/cat_animations_compressed/cat_trick5_compressed/cat_trick50024.png");
                    listUrls.add("http://192.168.8.105/fitpet/cat_animations_compressed/cat_trick5_compressed/cat_trick50025.png");
                    listUrls.add("http://192.168.8.105/fitpet/cat_animations_compressed/cat_trick5_compressed/cat_trick50026.png");
                    listUrls.add("http://192.168.8.105/fitpet/cat_animations_compressed/cat_trick5_compressed/cat_trick50027.png");
                    listUrls.add("http://192.168.8.105/fitpet/cat_animations_compressed/cat_trick5_compressed/cat_trick50028.png");
                    listUrls.add("http://192.168.8.105/fitpet/cat_animations_compressed/cat_trick5_compressed/cat_trick50029.png");
                    listUrls.add("http://192.168.8.105/fitpet/cat_animations_compressed/cat_trick5_compressed/cat_trick50030.png");
                    listUrls.add("http://192.168.8.105/fitpet/cat_animations_compressed/cat_trick5_compressed/cat_trick50031.png");
                    listUrls.add("http://192.168.8.105/fitpet/cat_animations_compressed/cat_trick5_compressed/cat_trick50032.png");


                    new ImageDownloadTask(getActivity(), listUrls).execute();
                }else{
                    //with custom ip address implementation
                    String ipAddress = PreferenceHelper.getInstance(getActivity()).getString("ip_address", null);

                    if(ipAddress != null){
                        ArrayList<String> listUrls = new ArrayList<String>();
                        //cat running
                        listUrls.add("http://" + ipAddress + "/fitpet/cat_animations_compressed/cat_running_compressed/cat_running0001.png");
                        listUrls.add("http://" + ipAddress + "/fitpet/cat_animations_compressed/cat_running_compressed/cat_running0002.png");
                        listUrls.add("http://" + ipAddress + "/fitpet/cat_animations_compressed/cat_running_compressed/cat_running0003.png");
                        listUrls.add("http://" + ipAddress + "/fitpet/cat_animations_compressed/cat_running_compressed/cat_running0004.png");
                        listUrls.add("http://" + ipAddress + "/fitpet/cat_animations_compressed/cat_running_compressed/cat_running0005.png");
                        listUrls.add("http://" + ipAddress + "/fitpet/cat_animations_compressed/cat_running_compressed/cat_running0006.png");
                        listUrls.add("http://" + ipAddress + "/fitpet/cat_animations_compressed/cat_running_compressed/cat_running0007.png");
                        listUrls.add("http://" + ipAddress + "/fitpet/cat_animations_compressed/cat_running_compressed/cat_running0008.png");
                        listUrls.add("http://" + ipAddress + "/fitpet/cat_animations_compressed/cat_running_compressed/cat_running0009.png");
                        listUrls.add("http://" + ipAddress + "/fitpet/cat_animations_compressed/cat_running_compressed/cat_running0010.png");
                        listUrls.add("http://" + ipAddress + "/fitpet/cat_animations_compressed/cat_running_compressed/cat_running0011.png");
                        listUrls.add("http://" + ipAddress + "/fitpet/cat_animations_compressed/cat_running_compressed/cat_running0012.png");
                        listUrls.add("http://" + ipAddress + "/fitpet/cat_animations_compressed/cat_running_compressed/cat_running0013.png");
                        listUrls.add("http://" + ipAddress + "/fitpet/cat_animations_compressed/cat_running_compressed/cat_running0014.png");
                        //cat walking
                        listUrls.add("http://" + ipAddress + "/fitpet/cat_animations_compressed/cat_walking_compressed/cat_walking0001.png");
                        listUrls.add("http://" + ipAddress + "/fitpet/cat_animations_compressed/cat_walking_compressed/cat_walking0002.png");
                        listUrls.add("http://" + ipAddress + "/fitpet/cat_animations_compressed/cat_walking_compressed/cat_walking0003.png");
                        listUrls.add("http://" + ipAddress + "/fitpet/cat_animations_compressed/cat_walking_compressed/cat_walking0004.png");
                        listUrls.add("http://" + ipAddress + "/fitpet/cat_animations_compressed/cat_walking_compressed/cat_walking0005.png");
                        listUrls.add("http://" + ipAddress + "/fitpet/cat_animations_compressed/cat_walking_compressed/cat_walking0006.png");
                        listUrls.add("http://" + ipAddress + "/fitpet/cat_animations_compressed/cat_walking_compressed/cat_walking0007.png");
                        listUrls.add("http://" + ipAddress + "/fitpet/cat_animations_compressed/cat_walking_compressed/cat_walking0008.png");
                        listUrls.add("http://" + ipAddress + "/fitpet/cat_animations_compressed/cat_walking_compressed/cat_walking0009.png");
                        listUrls.add("http://" + ipAddress + "/fitpet/cat_animations_compressed/cat_walking_compressed/cat_walking0010.png");
                        listUrls.add("http://" + ipAddress + "/fitpet/cat_animations_compressed/cat_walking_compressed/cat_walking0011.png");
                        listUrls.add("http://" + ipAddress + "/fitpet/cat_animations_compressed/cat_walking_compressed/cat_walking0012.png");
                        listUrls.add("http://" + ipAddress + "/fitpet/cat_animations_compressed/cat_walking_compressed/cat_walking0013.png");
                        listUrls.add("http://" + ipAddress + "/fitpet/cat_animations_compressed/cat_walking_compressed/cat_walking0014.png");
                        listUrls.add("http://" + ipAddress + "/fitpet/cat_animations_compressed/cat_walking_compressed/cat_walking0015.png");
                        listUrls.add("http://" + ipAddress + "/fitpet/cat_animations_compressed/cat_walking_compressed/cat_walking0016.png");
                        listUrls.add("http://" + ipAddress + "/fitpet/cat_animations_compressed/cat_walking_compressed/cat_walking0017.png");
                        listUrls.add("http://" + ipAddress + "/fitpet/cat_animations_compressed/cat_walking_compressed/cat_walking0018.png");
                        listUrls.add("http://" + ipAddress + "/fitpet/cat_animations_compressed/cat_walking_compressed/cat_walking0019.png");
                        listUrls.add("http://" + ipAddress + "/fitpet/cat_animations_compressed/cat_walking_compressed/cat_walking0020.png");
                        listUrls.add("http://" + ipAddress + "/fitpet/cat_animations_compressed/cat_walking_compressed/cat_walking0021.png");
                        listUrls.add("http://" + ipAddress + "/fitpet/cat_animations_compressed/cat_walking_compressed/cat_walking0022.png");
                        listUrls.add("http://" + ipAddress + "/fitpet/cat_animations_compressed/cat_walking_compressed/cat_walking0023.png");
                        listUrls.add("http://" + ipAddress + "/fitpet/cat_animations_compressed/cat_walking_compressed/cat_walking0024.png");

                        //cat sill 1
                        listUrls.add("http://" + ipAddress + "/fitpet/cat_animations_compressed/cat_still1_compressed/cat_still10001.png");
                        listUrls.add("http://" + ipAddress + "/fitpet/cat_animations_compressed/cat_still1_compressed/cat_still10002.png");
                        listUrls.add("http://" + ipAddress + "/fitpet/cat_animations_compressed/cat_still1_compressed/cat_still10003.png");
                        listUrls.add("http://" + ipAddress + "/fitpet/cat_animations_compressed/cat_still1_compressed/cat_still10004.png");
                        listUrls.add("http://" + ipAddress + "/fitpet/cat_animations_compressed/cat_still1_compressed/cat_still10005.png");
                        listUrls.add("http://" + ipAddress + "/fitpet/cat_animations_compressed/cat_still1_compressed/cat_still10006.png");
                        listUrls.add("http://" + ipAddress + "/fitpet/cat_animations_compressed/cat_still1_compressed/cat_still10007.png");
                        listUrls.add("http://" + ipAddress + "/fitpet/cat_animations_compressed/cat_still1_compressed/cat_still10008.png");
                        listUrls.add("http://" + ipAddress + "/fitpet/cat_animations_compressed/cat_still1_compressed/cat_still10009.png");
                        listUrls.add("http://" + ipAddress + "/fitpet/cat_animations_compressed/cat_still1_compressed/cat_still10010.png");
                        listUrls.add("http://" + ipAddress + "/fitpet/cat_animations_compressed/cat_still1_compressed/cat_still10011.png");
                        listUrls.add("http://" + ipAddress + "/fitpet/cat_animations_compressed/cat_still1_compressed/cat_still10012.png");
                        listUrls.add("http://" + ipAddress + "/fitpet/cat_animations_compressed/cat_still1_compressed/cat_still10013.png");
                        listUrls.add("http://" + ipAddress + "/fitpet/cat_animations_compressed/cat_still1_compressed/cat_still10014.png");
                        listUrls.add("http://" + ipAddress + "/fitpet/cat_animations_compressed/cat_still1_compressed/cat_still10015.png");
                        listUrls.add("http://" + ipAddress + "/fitpet/cat_animations_compressed/cat_still1_compressed/cat_still10016.png");
                        listUrls.add("http://" + ipAddress + "/fitpet/cat_animations_compressed/cat_still1_compressed/cat_still10017.png");
                        listUrls.add("http://" + ipAddress + "/fitpet/cat_animations_compressed/cat_still1_compressed/cat_still10018.png");
                        listUrls.add("http://" + ipAddress + "/fitpet/cat_animations_compressed/cat_still1_compressed/cat_still10019.png");
                        listUrls.add("http://" + ipAddress + "/fitpet/cat_animations_compressed/cat_still1_compressed/cat_still10020.png");
                        listUrls.add("http://" + ipAddress + "/fitpet/cat_animations_compressed/cat_still1_compressed/cat_still10021.png");
                        listUrls.add("http://" + ipAddress + "/fitpet/cat_animations_compressed/cat_still1_compressed/cat_still10022.png");
                        listUrls.add("http://" + ipAddress + "/fitpet/cat_animations_compressed/cat_still1_compressed/cat_still10023.png");
                        listUrls.add("http://" + ipAddress + "/fitpet/cat_animations_compressed/cat_still1_compressed/cat_still10024.png");
                        listUrls.add("http://" + ipAddress + "/fitpet/cat_animations_compressed/cat_still1_compressed/cat_still10025.png");
                        listUrls.add("http://" + ipAddress + "/fitpet/cat_animations_compressed/cat_still1_compressed/cat_still10026.png");
                        listUrls.add("http://" + ipAddress + "/fitpet/cat_animations_compressed/cat_still1_compressed/cat_still10027.png");
                        listUrls.add("http://" + ipAddress + "/fitpet/cat_animations_compressed/cat_still1_compressed/cat_still10028.png");
                        listUrls.add("http://" + ipAddress + "/fitpet/cat_animations_compressed/cat_still1_compressed/cat_still10029.png");
                        listUrls.add("http://" + ipAddress + "/fitpet/cat_animations_compressed/cat_still1_compressed/cat_still10030.png");
                        listUrls.add("http://" + ipAddress + "/fitpet/cat_animations_compressed/cat_still1_compressed/cat_still10031.png");

                        //cat still 2
                        listUrls.add("http://" + ipAddress + "/fitpet/cat_animations_compressed/cat_still2_compressed/cat_still20001.png");
                        listUrls.add("http://" + ipAddress + "/fitpet/cat_animations_compressed/cat_still2_compressed/cat_still20002.png");
                        listUrls.add("http://" + ipAddress + "/fitpet/cat_animations_compressed/cat_still2_compressed/cat_still20003.png");
                        listUrls.add("http://" + ipAddress + "/fitpet/cat_animations_compressed/cat_still2_compressed/cat_still20004.png");
                        listUrls.add("http://" + ipAddress + "/fitpet/cat_animations_compressed/cat_still2_compressed/cat_still20005.png");
                        listUrls.add("http://" + ipAddress + "/fitpet/cat_animations_compressed/cat_still2_compressed/cat_still20006.png");
                        listUrls.add("http://" + ipAddress + "/fitpet/cat_animations_compressed/cat_still2_compressed/cat_still20007.png");
                        listUrls.add("http://" + ipAddress + "/fitpet/cat_animations_compressed/cat_still2_compressed/cat_still20010.png");
                        listUrls.add("http://" + ipAddress + "/fitpet/cat_animations_compressed/cat_still2_compressed/cat_still20011.png");
                        listUrls.add("http://" + ipAddress + "/fitpet/cat_animations_compressed/cat_still2_compressed/cat_still20009.png");
                        listUrls.add("http://" + ipAddress + "/fitpet/cat_animations_compressed/cat_still2_compressed/cat_still20008.png");
                        listUrls.add("http://" + ipAddress + "/fitpet/cat_animations_compressed/cat_still2_compressed/cat_still20012.png");
                        listUrls.add("http://" + ipAddress + "/fitpet/cat_animations_compressed/cat_still2_compressed/cat_still20013.png");
                        listUrls.add("http://" + ipAddress + "/fitpet/cat_animations_compressed/cat_still2_compressed/cat_still20014.png");
                        listUrls.add("http://" + ipAddress + "/fitpet/cat_animations_compressed/cat_still2_compressed/cat_still20015.png");
                        listUrls.add("http://" + ipAddress + "/fitpet/cat_animations_compressed/cat_still2_compressed/cat_still20016.png");
                        listUrls.add("http://" + ipAddress + "/fitpet/cat_animations_compressed/cat_still2_compressed/cat_still20017.png");
                        listUrls.add("http://" + ipAddress + "/fitpet/cat_animations_compressed/cat_still2_compressed/cat_still20018.png");
                        listUrls.add("http://" + ipAddress + "/fitpet/cat_animations_compressed/cat_still2_compressed/cat_still20019.png");
                        listUrls.add("http://" + ipAddress + "/fitpet/cat_animations_compressed/cat_still2_compressed/cat_still20020.png");
                        listUrls.add("http://" + ipAddress + "/fitpet/cat_animations_compressed/cat_still2_compressed/cat_still20021.png");
                        listUrls.add("http://" + ipAddress + "/fitpet/cat_animations_compressed/cat_still2_compressed/cat_still20022.png");
                        listUrls.add("http://" + ipAddress + "/fitpet/cat_animations_compressed/cat_still2_compressed/cat_still20023.png");
                        listUrls.add("http://" + ipAddress + "/fitpet/cat_animations_compressed/cat_still2_compressed/cat_still20024.png");
                        listUrls.add("http://" + ipAddress + "/fitpet/cat_animations_compressed/cat_still2_compressed/cat_still20025.png");
                        listUrls.add("http://" + ipAddress + "/fitpet/cat_animations_compressed/cat_still2_compressed/cat_still20026.png");
                        listUrls.add("http://" + ipAddress + "/fitpet/cat_animations_compressed/cat_still2_compressed/cat_still20027.png");
                        listUrls.add("http://" + ipAddress + "/fitpet/cat_animations_compressed/cat_still2_compressed/cat_still20028.png");
                        listUrls.add("http://" + ipAddress + "/fitpet/cat_animations_compressed/cat_still2_compressed/cat_still20029.png");
                        listUrls.add("http://" + ipAddress + "/fitpet/cat_animations_compressed/cat_still2_compressed/cat_still20030.png");
                        listUrls.add("http://" + ipAddress + "/fitpet/cat_animations_compressed/cat_still2_compressed/cat_still20031.png");

                        //cat still 3
                        listUrls.add("http://" + ipAddress + "/fitpet/cat_animations_compressed/cat_still3_compressed/cat_still30001.png");
                        listUrls.add("http://" + ipAddress + "/fitpet/cat_animations_compressed/cat_still3_compressed/cat_still30002.png");
                        listUrls.add("http://" + ipAddress + "/fitpet/cat_animations_compressed/cat_still3_compressed/cat_still30003.png");
                        listUrls.add("http://" + ipAddress + "/fitpet/cat_animations_compressed/cat_still3_compressed/cat_still30004.png");
                        listUrls.add("http://" + ipAddress + "/fitpet/cat_animations_compressed/cat_still3_compressed/cat_still30005.png");
                        listUrls.add("http://" + ipAddress + "/fitpet/cat_animations_compressed/cat_still3_compressed/cat_still30006.png");
                        listUrls.add("http://" + ipAddress + "/fitpet/cat_animations_compressed/cat_still3_compressed/cat_still30007.png");
                        listUrls.add("http://" + ipAddress + "/fitpet/cat_animations_compressed/cat_still3_compressed/cat_still30010.png");
                        listUrls.add("http://" + ipAddress + "/fitpet/cat_animations_compressed/cat_still3_compressed/cat_still30011.png");
                        listUrls.add("http://" + ipAddress + "/fitpet/cat_animations_compressed/cat_still3_compressed/cat_still30009.png");
                        listUrls.add("http://" + ipAddress + "/fitpet/cat_animations_compressed/cat_still3_compressed/cat_still30008.png");
                        listUrls.add("http://" + ipAddress + "/fitpet/cat_animations_compressed/cat_still3_compressed/cat_still30012.png");
                        listUrls.add("http://" + ipAddress + "/fitpet/cat_animations_compressed/cat_still3_compressed/cat_still30013.png");
                        listUrls.add("http://" + ipAddress + "/fitpet/cat_animations_compressed/cat_still3_compressed/cat_still30014.png");
                        listUrls.add("http://" + ipAddress + "/fitpet/cat_animations_compressed/cat_still3_compressed/cat_still30015.png");
                        listUrls.add("http://" + ipAddress + "/fitpet/cat_animations_compressed/cat_still3_compressed/cat_still30016.png");
                        listUrls.add("http://" + ipAddress + "/fitpet/cat_animations_compressed/cat_still3_compressed/cat_still30017.png");
                        listUrls.add("http://" + ipAddress + "/fitpet/cat_animations_compressed/cat_still3_compressed/cat_still30018.png");
                        listUrls.add("http://" + ipAddress + "/fitpet/cat_animations_compressed/cat_still3_compressed/cat_still30019.png");
                        listUrls.add("http://" + ipAddress + "/fitpet/cat_animations_compressed/cat_still3_compressed/cat_still30020.png");
                        listUrls.add("http://" + ipAddress + "/fitpet/cat_animations_compressed/cat_still3_compressed/cat_still30021.png");
                        listUrls.add("http://" + ipAddress + "/fitpet/cat_animations_compressed/cat_still3_compressed/cat_still30022.png");
                        listUrls.add("http://" + ipAddress + "/fitpet/cat_animations_compressed/cat_still3_compressed/cat_still30023.png");
                        listUrls.add("http://" + ipAddress + "/fitpet/cat_animations_compressed/cat_still3_compressed/cat_still30024.png");
                        listUrls.add("http://" + ipAddress + "/fitpet/cat_animations_compressed/cat_still3_compressed/cat_still30025.png");
                        listUrls.add("http://" + ipAddress + "/fitpet/cat_animations_compressed/cat_still3_compressed/cat_still30026.png");
                        listUrls.add("http://" + ipAddress + "/fitpet/cat_animations_compressed/cat_still3_compressed/cat_still30027.png");
                        listUrls.add("http://" + ipAddress + "/fitpet/cat_animations_compressed/cat_still3_compressed/cat_still30028.png");
                        listUrls.add("http://" + ipAddress + "/fitpet/cat_animations_compressed/cat_still3_compressed/cat_still30029.png");
                        listUrls.add("http://" + ipAddress + "/fitpet/cat_animations_compressed/cat_still3_compressed/cat_still30030.png");

                        //cat trick 1
                        listUrls.add("http://" + ipAddress + "/fitpet/cat_animations_compressed/cat_trick1_compressed/cat_trick10001.png");
                        listUrls.add("http://" + ipAddress + "/fitpet/cat_animations_compressed/cat_trick1_compressed/cat_trick10002.png");
                        listUrls.add("http://" + ipAddress + "/fitpet/cat_animations_compressed/cat_trick1_compressed/cat_trick10003.png");
                        listUrls.add("http://" + ipAddress + "/fitpet/cat_animations_compressed/cat_trick1_compressed/cat_trick10004.png");
                        listUrls.add("http://" + ipAddress + "/fitpet/cat_animations_compressed/cat_trick1_compressed/cat_trick10005.png");
                        listUrls.add("http://" + ipAddress + "/fitpet/cat_animations_compressed/cat_trick1_compressed/cat_trick10006.png");
                        listUrls.add("http://" + ipAddress + "/fitpet/cat_animations_compressed/cat_trick1_compressed/cat_trick10007.png");
                        listUrls.add("http://" + ipAddress + "/fitpet/cat_animations_compressed/cat_trick1_compressed/cat_trick10008.png");
                        listUrls.add("http://" + ipAddress + "/fitpet/cat_animations_compressed/cat_trick1_compressed/cat_trick10009.png");
                        listUrls.add("http://" + ipAddress + "/fitpet/cat_animations_compressed/cat_trick1_compressed/cat_trick10010.png");
                        listUrls.add("http://" + ipAddress + "/fitpet/cat_animations_compressed/cat_trick1_compressed/cat_trick10011.png");
                        listUrls.add("http://" + ipAddress + "/fitpet/cat_animations_compressed/cat_trick1_compressed/cat_trick10012.png");
                        listUrls.add("http://" + ipAddress + "/fitpet/cat_animations_compressed/cat_trick1_compressed/cat_trick10013.png");
                        listUrls.add("http://" + ipAddress + "/fitpet/cat_animations_compressed/cat_trick1_compressed/cat_trick10014.png");
                        listUrls.add("http://" + ipAddress + "/fitpet/cat_animations_compressed/cat_trick1_compressed/cat_trick10015.png");
                        listUrls.add("http://" + ipAddress + "/fitpet/cat_animations_compressed/cat_trick1_compressed/cat_trick10016.png");
                        listUrls.add("http://" + ipAddress + "/fitpet/cat_animations_compressed/cat_trick1_compressed/cat_trick10017.png");
                        listUrls.add("http://" + ipAddress + "/fitpet/cat_animations_compressed/cat_trick1_compressed/cat_trick10018.png");
                        listUrls.add("http://" + ipAddress + "/fitpet/cat_animations_compressed/cat_trick1_compressed/cat_trick10019.png");
                        listUrls.add("http://" + ipAddress + "/fitpet/cat_animations_compressed/cat_trick1_compressed/cat_trick10020.png");
                        listUrls.add("http://" + ipAddress + "/fitpet/cat_animations_compressed/cat_trick1_compressed/cat_trick10021.png");
                        listUrls.add("http://" + ipAddress + "/fitpet/cat_animations_compressed/cat_trick1_compressed/cat_trick10022.png");
                        listUrls.add("http://" + ipAddress + "/fitpet/cat_animations_compressed/cat_trick1_compressed/cat_trick10023.png");
                        listUrls.add("http://" + ipAddress + "/fitpet/cat_animations_compressed/cat_trick1_compressed/cat_trick10024.png");
                        listUrls.add("http://" + ipAddress + "/fitpet/cat_animations_compressed/cat_trick1_compressed/cat_trick10025.png");
                        listUrls.add("http://" + ipAddress + "/fitpet/cat_animations_compressed/cat_trick1_compressed/cat_trick10026.png");
                        listUrls.add("http://" + ipAddress + "/fitpet/cat_animations_compressed/cat_trick1_compressed/cat_trick10027.png");
                        listUrls.add("http://" + ipAddress + "/fitpet/cat_animations_compressed/cat_trick1_compressed/cat_trick10028.png");
                        listUrls.add("http://" + ipAddress + "/fitpet/cat_animations_compressed/cat_trick1_compressed/cat_trick10029.png");
                        listUrls.add("http://" + ipAddress + "/fitpet/cat_animations_compressed/cat_trick1_compressed/cat_trick10030.png");
                        listUrls.add("http://" + ipAddress + "/fitpet/cat_animations_compressed/cat_trick1_compressed/cat_trick10031.png");

                        //cat trick 2
                        listUrls.add("http://" + ipAddress + "/fitpet/cat_animations_compressed/cat_trick2_compressed/cat_trick20001.png");
                        listUrls.add("http://" + ipAddress + "/fitpet/cat_animations_compressed/cat_trick2_compressed/cat_trick20002.png");
                        listUrls.add("http://" + ipAddress + "/fitpet/cat_animations_compressed/cat_trick2_compressed/cat_trick20003.png");
                        listUrls.add("http://" + ipAddress + "/fitpet/cat_animations_compressed/cat_trick2_compressed/cat_trick20004.png");
                        listUrls.add("http://" + ipAddress + "/fitpet/cat_animations_compressed/cat_trick2_compressed/cat_trick20005.png");
                        listUrls.add("http://" + ipAddress + "/fitpet/cat_animations_compressed/cat_trick2_compressed/cat_trick20006.png");
                        listUrls.add("http://" + ipAddress + "/fitpet/cat_animations_compressed/cat_trick2_compressed/cat_trick20007.png");
                        listUrls.add("http://" + ipAddress + "/fitpet/cat_animations_compressed/cat_trick2_compressed/cat_trick20010.png");
                        listUrls.add("http://" + ipAddress + "/fitpet/cat_animations_compressed/cat_trick2_compressed/cat_trick20011.png");
                        listUrls.add("http://" + ipAddress + "/fitpet/cat_animations_compressed/cat_trick2_compressed/cat_trick20009.png");
                        listUrls.add("http://" + ipAddress + "/fitpet/cat_animations_compressed/cat_trick2_compressed/cat_trick20008.png");
                        listUrls.add("http://" + ipAddress + "/fitpet/cat_animations_compressed/cat_trick2_compressed/cat_trick20012.png");
                        listUrls.add("http://" + ipAddress + "/fitpet/cat_animations_compressed/cat_trick2_compressed/cat_trick20013.png");
                        listUrls.add("http://" + ipAddress + "/fitpet/cat_animations_compressed/cat_trick2_compressed/cat_trick20014.png");
                        listUrls.add("http://" + ipAddress + "/fitpet/cat_animations_compressed/cat_trick2_compressed/cat_trick20015.png");
                        listUrls.add("http://" + ipAddress + "/fitpet/cat_animations_compressed/cat_trick2_compressed/cat_trick20016.png");
                        listUrls.add("http://" + ipAddress + "/fitpet/cat_animations_compressed/cat_trick2_compressed/cat_trick20017.png");
                        listUrls.add("http://" + ipAddress + "/fitpet/cat_animations_compressed/cat_trick2_compressed/cat_trick20018.png");
                        listUrls.add("http://" + ipAddress + "/fitpet/cat_animations_compressed/cat_trick2_compressed/cat_trick20019.png");
                        listUrls.add("http://" + ipAddress + "/fitpet/cat_animations_compressed/cat_trick2_compressed/cat_trick20020.png");
                        listUrls.add("http://" + ipAddress + "/fitpet/cat_animations_compressed/cat_trick2_compressed/cat_trick20021.png");
                        listUrls.add("http://" + ipAddress + "/fitpet/cat_animations_compressed/cat_trick2_compressed/cat_trick20022.png");
                        listUrls.add("http://" + ipAddress + "/fitpet/cat_animations_compressed/cat_trick2_compressed/cat_trick20023.png");
                        listUrls.add("http://" + ipAddress + "/fitpet/cat_animations_compressed/cat_trick2_compressed/cat_trick20024.png");
                        listUrls.add("http://" + ipAddress + "/fitpet/cat_animations_compressed/cat_trick2_compressed/cat_trick20025.png");
                        listUrls.add("http://" + ipAddress + "/fitpet/cat_animations_compressed/cat_trick2_compressed/cat_trick20026.png");
                        listUrls.add("http://" + ipAddress + "/fitpet/cat_animations_compressed/cat_trick2_compressed/cat_trick20027.png");
                        listUrls.add("http://" + ipAddress + "/fitpet/cat_animations_compressed/cat_trick2_compressed/cat_trick20028.png");
                        listUrls.add("http://" + ipAddress + "/fitpet/cat_animations_compressed/cat_trick2_compressed/cat_trick20029.png");
                        listUrls.add("http://" + ipAddress + "/fitpet/cat_animations_compressed/cat_trick2_compressed/cat_trick20030.png");
                        listUrls.add("http://" + ipAddress + "/fitpet/cat_animations_compressed/cat_trick2_compressed/cat_trick20031.png");
                        listUrls.add("http://" + ipAddress + "/fitpet/cat_animations_compressed/cat_trick2_compressed/cat_trick20032.png");

                        //cat trick 3
                        listUrls.add("http://" + ipAddress + "/fitpet/cat_animations_compressed/cat_trick3_compressed/cat_trick30001.png");
                        listUrls.add("http://" + ipAddress + "/fitpet/cat_animations_compressed/cat_trick3_compressed/cat_trick30002.png");
                        listUrls.add("http://" + ipAddress + "/fitpet/cat_animations_compressed/cat_trick3_compressed/cat_trick30003.png");
                        listUrls.add("http://" + ipAddress + "/fitpet/cat_animations_compressed/cat_trick3_compressed/cat_trick30004.png");
                        listUrls.add("http://" + ipAddress + "/fitpet/cat_animations_compressed/cat_trick3_compressed/cat_trick30005.png");
                        listUrls.add("http://" + ipAddress + "/fitpet/cat_animations_compressed/cat_trick3_compressed/cat_trick30006.png");
                        listUrls.add("http://" + ipAddress + "/fitpet/cat_animations_compressed/cat_trick3_compressed/cat_trick30007.png");
                        listUrls.add("http://" + ipAddress + "/fitpet/cat_animations_compressed/cat_trick3_compressed/cat_trick30010.png");
                        listUrls.add("http://" + ipAddress + "/fitpet/cat_animations_compressed/cat_trick3_compressed/cat_trick30011.png");
                        listUrls.add("http://" + ipAddress + "/fitpet/cat_animations_compressed/cat_trick3_compressed/cat_trick30009.png");
                        listUrls.add("http://" + ipAddress + "/fitpet/cat_animations_compressed/cat_trick3_compressed/cat_trick30008.png");
                        listUrls.add("http://" + ipAddress + "/fitpet/cat_animations_compressed/cat_trick3_compressed/cat_trick30012.png");
                        listUrls.add("http://" + ipAddress + "/fitpet/cat_animations_compressed/cat_trick3_compressed/cat_trick30013.png");
                        listUrls.add("http://" + ipAddress + "/fitpet/cat_animations_compressed/cat_trick3_compressed/cat_trick30014.png");
                        listUrls.add("http://" + ipAddress + "/fitpet/cat_animations_compressed/cat_trick3_compressed/cat_trick30015.png");
                        listUrls.add("http://" + ipAddress + "/fitpet/cat_animations_compressed/cat_trick3_compressed/cat_trick30016.png");
                        listUrls.add("http://" + ipAddress + "/fitpet/cat_animations_compressed/cat_trick3_compressed/cat_trick30017.png");
                        listUrls.add("http://" + ipAddress + "/fitpet/cat_animations_compressed/cat_trick3_compressed/cat_trick30018.png");
                        listUrls.add("http://" + ipAddress + "/fitpet/cat_animations_compressed/cat_trick3_compressed/cat_trick30019.png");
                        listUrls.add("http://" + ipAddress + "/fitpet/cat_animations_compressed/cat_trick3_compressed/cat_trick30020.png");
                        listUrls.add("http://" + ipAddress + "/fitpet/cat_animations_compressed/cat_trick3_compressed/cat_trick30021.png");
                        listUrls.add("http://" + ipAddress + "/fitpet/cat_animations_compressed/cat_trick3_compressed/cat_trick30022.png");
                        listUrls.add("http://" + ipAddress + "/fitpet/cat_animations_compressed/cat_trick3_compressed/cat_trick30023.png");
                        listUrls.add("http://" + ipAddress + "/fitpet/cat_animations_compressed/cat_trick3_compressed/cat_trick30024.png");
                        listUrls.add("http://" + ipAddress + "/fitpet/cat_animations_compressed/cat_trick3_compressed/cat_trick30025.png");
                        listUrls.add("http://" + ipAddress + "/fitpet/cat_animations_compressed/cat_trick3_compressed/cat_trick30026.png");
                        listUrls.add("http://" + ipAddress + "/fitpet/cat_animations_compressed/cat_trick3_compressed/cat_trick30027.png");
                        listUrls.add("http://" + ipAddress + "/fitpet/cat_animations_compressed/cat_trick3_compressed/cat_trick30028.png");
                        listUrls.add("http://" + ipAddress + "/fitpet/cat_animations_compressed/cat_trick3_compressed/cat_trick30029.png");
                        listUrls.add("http://" + ipAddress + "/fitpet/cat_animations_compressed/cat_trick3_compressed/cat_trick30030.png");
                        listUrls.add("http://" + ipAddress + "/fitpet/cat_animations_compressed/cat_trick3_compressed/cat_trick30031.png");
                        listUrls.add("http://" + ipAddress + "/fitpet/cat_animations_compressed/cat_trick3_compressed/cat_trick30032.png");

                        //cat trick 4
                        listUrls.add("http://" + ipAddress + "/fitpet/cat_animations_compressed/cat_trick4_compressed/cat_trick40001.png");
                        listUrls.add("http://" + ipAddress + "/fitpet/cat_animations_compressed/cat_trick4_compressed/cat_trick40002.png");
                        listUrls.add("http://" + ipAddress + "/fitpet/cat_animations_compressed/cat_trick4_compressed/cat_trick40003.png");
                        listUrls.add("http://" + ipAddress + "/fitpet/cat_animations_compressed/cat_trick4_compressed/cat_trick40004.png");
                        listUrls.add("http://" + ipAddress + "/fitpet/cat_animations_compressed/cat_trick4_compressed/cat_trick40005.png");
                        listUrls.add("http://" + ipAddress + "/fitpet/cat_animations_compressed/cat_trick4_compressed/cat_trick40006.png");
                        listUrls.add("http://" + ipAddress + "/fitpet/cat_animations_compressed/cat_trick4_compressed/cat_trick40007.png");
                        listUrls.add("http://" + ipAddress + "/fitpet/cat_animations_compressed/cat_trick4_compressed/cat_trick40010.png");
                        listUrls.add("http://" + ipAddress + "/fitpet/cat_animations_compressed/cat_trick4_compressed/cat_trick40011.png");
                        listUrls.add("http://" + ipAddress + "/fitpet/cat_animations_compressed/cat_trick4_compressed/cat_trick40009.png");
                        listUrls.add("http://" + ipAddress + "/fitpet/cat_animations_compressed/cat_trick4_compressed/cat_trick40008.png");
                        listUrls.add("http://" + ipAddress + "/fitpet/cat_animations_compressed/cat_trick4_compressed/cat_trick40012.png");
                        listUrls.add("http://" + ipAddress + "/fitpet/cat_animations_compressed/cat_trick4_compressed/cat_trick40013.png");
                        listUrls.add("http://" + ipAddress + "/fitpet/cat_animations_compressed/cat_trick4_compressed/cat_trick40014.png");
                        listUrls.add("http://" + ipAddress + "/fitpet/cat_animations_compressed/cat_trick4_compressed/cat_trick40015.png");
                        listUrls.add("http://" + ipAddress + "/fitpet/cat_animations_compressed/cat_trick4_compressed/cat_trick40016.png");
                        listUrls.add("http://" + ipAddress + "/fitpet/cat_animations_compressed/cat_trick4_compressed/cat_trick40017.png");
                        listUrls.add("http://" + ipAddress + "/fitpet/cat_animations_compressed/cat_trick4_compressed/cat_trick40018.png");
                        listUrls.add("http://" + ipAddress + "/fitpet/cat_animations_compressed/cat_trick4_compressed/cat_trick40019.png");
                        listUrls.add("http://" + ipAddress + "/fitpet/cat_animations_compressed/cat_trick4_compressed/cat_trick40020.png");
                        listUrls.add("http://" + ipAddress + "/fitpet/cat_animations_compressed/cat_trick4_compressed/cat_trick40021.png");
                        listUrls.add("http://" + ipAddress + "/fitpet/cat_animations_compressed/cat_trick4_compressed/cat_trick40022.png");
                        listUrls.add("http://" + ipAddress + "/fitpet/cat_animations_compressed/cat_trick4_compressed/cat_trick40023.png");
                        listUrls.add("http://" + ipAddress + "/fitpet/cat_animations_compressed/cat_trick4_compressed/cat_trick40024.png");
                        listUrls.add("http://" + ipAddress + "/fitpet/cat_animations_compressed/cat_trick4_compressed/cat_trick40025.png");
                        listUrls.add("http://" + ipAddress + "/fitpet/cat_animations_compressed/cat_trick4_compressed/cat_trick40026.png");
                        listUrls.add("http://" + ipAddress + "/fitpet/cat_animations_compressed/cat_trick4_compressed/cat_trick40027.png");
                        listUrls.add("http://" + ipAddress + "/fitpet/cat_animations_compressed/cat_trick4_compressed/cat_trick40028.png");
                        listUrls.add("http://" + ipAddress + "/fitpet/cat_animations_compressed/cat_trick4_compressed/cat_trick40029.png");
                        listUrls.add("http://" + ipAddress + "/fitpet/cat_animations_compressed/cat_trick4_compressed/cat_trick40030.png");
                        listUrls.add("http://" + ipAddress + "/fitpet/cat_animations_compressed/cat_trick4_compressed/cat_trick40031.png");
                        listUrls.add("http://" + ipAddress + "/fitpet/cat_animations_compressed/cat_trick4_compressed/cat_trick40032.png");

                        //cat trick 5
                        listUrls.add("http://" + ipAddress + "/fitpet/cat_animations_compressed/cat_trick5_compressed/cat_trick50001.png");
                        listUrls.add("http://" + ipAddress + "/fitpet/cat_animations_compressed/cat_trick5_compressed/cat_trick50002.png");
                        listUrls.add("http://" + ipAddress + "/fitpet/cat_animations_compressed/cat_trick5_compressed/cat_trick50003.png");
                        listUrls.add("http://" + ipAddress + "/fitpet/cat_animations_compressed/cat_trick5_compressed/cat_trick50004.png");
                        listUrls.add("http://" + ipAddress + "/fitpet/cat_animations_compressed/cat_trick5_compressed/cat_trick50005.png");
                        listUrls.add("http://" + ipAddress + "/fitpet/cat_animations_compressed/cat_trick5_compressed/cat_trick50006.png");
                        listUrls.add("http://" + ipAddress + "/fitpet/cat_animations_compressed/cat_trick5_compressed/cat_trick50007.png");
                        listUrls.add("http://" + ipAddress + "/fitpet/cat_animations_compressed/cat_trick5_compressed/cat_trick50010.png");
                        listUrls.add("http://" + ipAddress + "/fitpet/cat_animations_compressed/cat_trick5_compressed/cat_trick50011.png");
                        listUrls.add("http://" + ipAddress + "/fitpet/cat_animations_compressed/cat_trick5_compressed/cat_trick50009.png");
                        listUrls.add("http://" + ipAddress + "/fitpet/cat_animations_compressed/cat_trick5_compressed/cat_trick50008.png");
                        listUrls.add("http://" + ipAddress + "/fitpet/cat_animations_compressed/cat_trick5_compressed/cat_trick50012.png");
                        listUrls.add("http://" + ipAddress + "/fitpet/cat_animations_compressed/cat_trick5_compressed/cat_trick50013.png");
                        listUrls.add("http://" + ipAddress + "/fitpet/cat_animations_compressed/cat_trick5_compressed/cat_trick50014.png");
                        listUrls.add("http://" + ipAddress + "/fitpet/cat_animations_compressed/cat_trick5_compressed/cat_trick50015.png");
                        listUrls.add("http://" + ipAddress + "/fitpet/cat_animations_compressed/cat_trick5_compressed/cat_trick50016.png");
                        listUrls.add("http://" + ipAddress + "/fitpet/cat_animations_compressed/cat_trick5_compressed/cat_trick50017.png");
                        listUrls.add("http://" + ipAddress + "/fitpet/cat_animations_compressed/cat_trick5_compressed/cat_trick50018.png");
                        listUrls.add("http://" + ipAddress + "/fitpet/cat_animations_compressed/cat_trick5_compressed/cat_trick50019.png");
                        listUrls.add("http://" + ipAddress + "/fitpet/cat_animations_compressed/cat_trick5_compressed/cat_trick50020.png");
                        listUrls.add("http://" + ipAddress + "/fitpet/cat_animations_compressed/cat_trick5_compressed/cat_trick50021.png");
                        listUrls.add("http://" + ipAddress + "/fitpet/cat_animations_compressed/cat_trick5_compressed/cat_trick50022.png");
                        listUrls.add("http://" + ipAddress + "/fitpet/cat_animations_compressed/cat_trick5_compressed/cat_trick50023.png");
                        listUrls.add("http://" + ipAddress + "/fitpet/cat_animations_compressed/cat_trick5_compressed/cat_trick50024.png");
                        listUrls.add("http://" + ipAddress + "/fitpet/cat_animations_compressed/cat_trick5_compressed/cat_trick50025.png");
                        listUrls.add("http://" + ipAddress + "/fitpet/cat_animations_compressed/cat_trick5_compressed/cat_trick50026.png");
                        listUrls.add("http://" + ipAddress + "/fitpet/cat_animations_compressed/cat_trick5_compressed/cat_trick50027.png");
                        listUrls.add("http://" + ipAddress + "/fitpet/cat_animations_compressed/cat_trick5_compressed/cat_trick50028.png");
                        listUrls.add("http://" + ipAddress + "/fitpet/cat_animations_compressed/cat_trick5_compressed/cat_trick50029.png");
                        listUrls.add("http://" + ipAddress + "/fitpet/cat_animations_compressed/cat_trick5_compressed/cat_trick50030.png");
                        listUrls.add("http://" + ipAddress + "/fitpet/cat_animations_compressed/cat_trick5_compressed/cat_trick50031.png");
                        listUrls.add("http://" + ipAddress + "/fitpet/cat_animations_compressed/cat_trick5_compressed/cat_trick50032.png");


                        new ImageDownloadTask(getActivity(), listUrls).execute();
                    }else{
                        final EditText etIpAddress = new EditText(getActivity());
                        etIpAddress.setHint("enter ip address");

                        new AlertDialog.Builder(getActivity())
                                .setTitle("Set IP Address")
                                .setMessage("Your IP address is not yet set. Please set your IP address and click again this option")
                                .setView(etIpAddress)
                                .setNeutralButton("OK", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        ((ViewGroup)etIpAddress.getParent()).removeView(etIpAddress);
                                        PreferenceHelper.getInstance(getActivity()).setString("ip_address", etIpAddress.getText().toString());
                                    }
                                })
                                .show();
                    }
                }

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
                        .addApi(Fitness.RECORDING_API)
                        .addApi(Fitness.HISTORY_API)
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
                            listSteps = new ArrayList<UnsentSteps>();
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
        //TODO: make getting and sending of name dynamic
        PutDataMapRequest dataMap = PutDataMapRequest.create("/test");
        dataMap.getDataMap().putString("message", "this is test message");
        dataMap.getDataMap().putAsset("img", createAssetFromBitmap(getBitmapFromAssets()));
        dataMap.getDataMap().putString("imgName", "chrome_icon.png");
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

    private void sendPetImageToWear(Bitmap bmp, String animGroup, String imgName){
        //TODO: make getting and sending of name dynamic
        PutDataMapRequest dataMap = PutDataMapRequest.create("/test");
        dataMap.getDataMap().putString("message", "this is test message");
        dataMap.getDataMap().putAsset("img", createAssetFromBitmap(bmp));
        dataMap.getDataMap().putString("img_name", imgName);
        dataMap.getDataMap().putString("anim_group", animGroup);
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
//    public interface OnFragmentInteractionListener {
//        // TODO: Update argument type and name
//        public void onFragmentInteraction(Uri uri);
//    }

    private void processNotification(Bundle bundle, boolean removeNotification) {
        String msg = "";
        try{
            msg = bundle.getString("message", "");
        }catch (NullPointerException e){
            e.printStackTrace();
        }

        final NotificationManager mNotificationManager = (NotificationManager)
                getActivity().getSystemService(Context.NOTIFICATION_SERVICE);

//        PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
//                new Intent(this, NotificationActivity.class), 0);


        //TODO: check for priority in notification
        final NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(getActivity())
                        .setSmallIcon(R.mipmap.ic_launcher)
                        .setContentTitle("Downloading Pets...")
                        .setStyle(new NotificationCompat.BigTextStyle()
                                .bigText(msg))
                        .setContentText(msg);


        mBuilder.setAutoCancel(true);
//        mBuilder.setContentIntent(contentIntent);
        //TODO: set NOTIFICATION_ID constant. this is for testing purposes
        if(!removeNotification) {
            mBuilder.setProgress(0, 0, true);
            mNotificationManager.notify(1, mBuilder.build());
        }else {
            mBuilder.setProgress(0, 0, false);
            mNotificationManager.notify(1, mBuilder.build());
            mNotificationManager.cancel(1);
        }

//        new Thread(
//                new Runnable() {
//                    @Override
//                    public void run() {
//                        int incr;
//                        // Do the "lengthy" operation 20 times
//                        for (incr = 0; incr <= 100; incr+=5) {
//                            // Sets the progress indicator to a max value, the
//                            // current completion percentage, and "determinate"
//                            // state
//                            mBuilder.setProgress(0, 0, true);
//                            // Displays the progress bar for the first time.
//                            mNotificationManager.notify(1, mBuilder.build());
//                            // Sleeps the thread, simulating an operation
//                            // that takes time
//                            try {
//                                // Sleep for 5 seconds
//                                Thread.sleep(5*1000);
//                            } catch (InterruptedException e) {
//                                Log.d("notification", "sleep failure");
//                            }
//                        }
//                        // When the loop is finished, updates the notification
//                        mBuilder.setContentText("Download complete")
//                                // Removes the progress bar
//                                .setProgress(0,0,false);
//                        mNotificationManager.notify(1, mBuilder.build());
//                    }
//                }
//// Starts the thread by calling the run() method in its Runnable
//        ).start();
    }

    public static Bitmap getBitmapFromURL(String link) {
    /*--- this method downloads an Image from the given URL,
     *  then decodes and returns a Bitmap object
     ---*/
        try {
            URL url = new URL(link);
            HttpURLConnection connection = (HttpURLConnection) url
                    .openConnection();
            connection.setDoInput(true);
            connection.connect();
            InputStream input = connection.getInputStream();
            Bitmap myBitmap = BitmapFactory.decodeStream(input);

            return myBitmap;

        } catch (IOException e) {
            e.printStackTrace();
            Log.e("getBmpFromUrl error: ", e.getMessage().toString());
            return null;
        }
    }

    private void saveImageToSD() {
        Bitmap bmp = null;
        FileOutputStream fos = null;

    /*--- this method will save your downloaded image to SD card ---*/

        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
    /*--- you can select your preferred CompressFormat and quality.
     * I'm going to use JPEG and 100% quality ---*/
        bmp.compress(Bitmap.CompressFormat.JPEG, 100, bytes);
    /*--- create a new file on SD card ---*/
        File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
                + File.separator + "myDownloadedImage.jpg");
        try {
            file.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
    /*--- create a new FileOutputStream and write bytes to file ---*/
        try {
            fos = new FileOutputStream(file);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        try {
            fos.write(bytes.toByteArray());
            fos.close();
            Toast.makeText(getActivity(), "Image saved", Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }


    private void imageDownloader(){
        DownloadManager dlManager = (DownloadManager) getActivity().getSystemService(Context.DOWNLOAD_SERVICE);

        DownloadManager.Request request = new DownloadManager.Request(Uri.parse("http://localhost:8081/fitpet/cat_animations_compressed/cat-trick3_compressed/cat_trick30001.png"));

        dlManager.enqueue(request);
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

    private class ImageDownloadTask extends AsyncTask<String, Intent, String>{
        private Context context;
        private ArrayList<String> listUrls;

        public ImageDownloadTask(Context context, ArrayList<String> listUrls) {
            this.context = context;
            this.listUrls = listUrls;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            Bundle msgBundle = new Bundle();
            msgBundle.putString("message", "Downloading... Please wait");
            processNotification(null, false);
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);

            processNotification(null, true);
        }

        @Override
        protected String doInBackground(String... params) {
            return downloadFiles(listUrls);
        }

        private String downloadFiles(ArrayList<String> listUrls){
            String result = "";
            int count = 0;

            for(String url : listUrls){
                String[] temp = url.split(Pattern.quote("/"));
                String filename = temp[temp.length - 1];
                String folderName = temp[temp.length - 2];
                saveImageToSD(getBitmapFromURL(url), filename, folderName);
                count++;
            }

            if(count == listUrls.size()){
                result = "successfully downloaded all images";
            }else{
                result = "Some images are not successfully downloaded";
            }

            return result;
        }

        public Bitmap getBitmapFromURL(String link) {
    /*--- this method downloads an Image from the given URL,
     *  then decodes and returns a Bitmap object
     ---*/
            try {
                URL url = new URL(link);
                HttpURLConnection connection = (HttpURLConnection) url
                        .openConnection();
                connection.setDoInput(true);
                connection.connect();
                InputStream input = connection.getInputStream();
                Bitmap myBitmap = BitmapFactory.decodeStream(input);

                return myBitmap;

            } catch (IOException e) {
                e.printStackTrace();
                Log.e("getBmpFromUrl error: ", e.getMessage().toString());
                return null;
            }
        }

        private void saveImageToSD(Bitmap bmp, String filename, String folderName) {
//            Bitmap bmp = null;
            FileOutputStream fos = null;

    /*--- this method will save your downloaded image to SD card ---*/

            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
    /*--- you can select your preferred CompressFormat and quality.
     * I'm going to use JPEG and 100% quality ---*/
            bmp.compress(Bitmap.CompressFormat.JPEG, 100, bytes);
    /*--- create a new file on SD card ---*/
            File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
                    + File.separator + "fitpet" + File.separator + folderName);
            try {
                file.mkdirs();
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }

            File mediaFile = new File(file.getPath() + File.separator + filename);
    /*--- create a new FileOutputStream and write bytes to file ---*/
            try {
                fos = new FileOutputStream(mediaFile);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
            try {
                fos.write(bytes.toByteArray());
                fos.close();
//                Toast.makeText(getActivity(), "Image saved", Toast.LENGTH_SHORT).show();
                Log.d("image", filename + " saved");
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    }
}
