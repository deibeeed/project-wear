package com.kfast.uitest;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.Dialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.AnimationDrawable;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.wearable.view.DismissOverlayView;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.location.ActivityRecognition;
import com.google.android.gms.location.ActivityRecognitionApi;
import com.google.android.gms.plus.Plus;
import com.google.android.gms.wearable.Asset;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;
import com.kfast.uitest.receiver.ActivityRecognitionReceiver;
import com.kfast.uitest.service.ActivityRecognitionIntentService;
import com.kfast.uitest.service.StepService;
import com.kfast.uitest.util.Config;
import com.kfast.uitest.util.PreferenceHelper;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Random;
import java.util.concurrent.TimeUnit;

public class MainActivity extends Activity implements SimpleGestureFilter.SimpleGestureListener,
		SensorEventListener, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, DataApi.DataListener {

    private enum REQUEST_TYPE {
        START, STOP
    }

    private REQUEST_TYPE mRequestType;

    private static final String MESSAGE_PATH = "/start/activity-recognition";

	private SimpleGestureFilter gestureFilter;
	private DismissOverlayView dismissOverlay;
	private Sensor stepDetector;
	private Sensor significantMotion;
	private SensorManager sensorManager;
	private int currentAnim = 0;
	private ImageView canvas;
	private ProgressBar progressBar;

	private LinearLayout layoutTop;
	private LinearLayout layoutBottom;
	private LinearLayout layoutLeft;
	private LinearLayout layoutRight;

	private Animation animUp;
	private Animation animDown;
	private Animation animLeft;
	private Animation animRight;
    private GoogleApiClient wearClient;

    private Handler handler;
    private ArrayList<Integer> listAnimIds;
    private int petHappiness;

    private GoogleApiClient recognitionClient;
    private ActivityRecognitionApi recognitionApi;

    private final static int CONNECTION_FAILURE_RESOLUTION_REQUEST = 9000;
    // Constants that define the activity detection interval
    public static final int MILLISECONDS_PER_SECOND = 1000;
    public static final int DETECTION_INTERVAL_SECONDS = 1;
    public static final int DETECTION_INTERVAL_MILLISECONDS =
            MILLISECONDS_PER_SECOND * DETECTION_INTERVAL_SECONDS;

    private PendingIntent mActivityRecognitionPendingIntent;

    private PendingResult mActivityRecognitionPendingResult;

    private boolean mInProgress;

    private ImageView ivSettings;

    private boolean isSwipeUpUnlocked;
    private boolean isSwipeDownUnlocked;
    private boolean isSwipeLeftUnlocked;
    private boolean isSwipeRightUnlocked;

    private TextView tvProgressCount;
    private TextView tvProgressTotal;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
//		setContentView(R.layout.activity_main);
        setContentView(R.layout.activity_main_2);

        handler = new Handler();
        listAnimIds = new ArrayList<Integer>();
        petHappiness = 0;

		initGestureFilter();
		initViews();
		initSensors();
//		hideSlideLayouts();
		initSlideLayoutAnimation();
		playAnimalAnim(R.drawable.hold_still_b);

        //initialize google client
        initGoogleApiClient();

	}

    private void initGoogleApiClient(){
        wearClient = new GoogleApiClient.Builder(this)
                            .addConnectionCallbacks(this)
                            .addOnConnectionFailedListener(this)
                            .addApi(Wearable.API)
                            .build();

        wearClient.connect();

        if(hasGPS()){
            recognitionClient = new GoogleApiClient.Builder(this)
                    .addApi(ActivityRecognition.API)
                    .addConnectionCallbacks(new GoogleApiClient.ConnectionCallbacks() {
                        @Override
                        public void onConnected(Bundle bundle) {
                            Log.d("recognition_client", "connected");

                            switch (mRequestType){
                                case START:
                                    recognitionApi.requestActivityUpdates(recognitionClient, DETECTION_INTERVAL_MILLISECONDS, mActivityRecognitionPendingIntent);
//                ActivityRecognitionResult activityResult = (ActivityRecognitionResult) recognitionApi.requestActivityUpdates(recognitionClient, DETECTION_INTERVAL_MILLISECONDS, mActivityRecognitionPendingIntent);
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
                            Log.d("recognition_client", "connection suspended");
                        }
                    })
                    .addOnConnectionFailedListener(new GoogleApiClient.OnConnectionFailedListener() {
                        @Override
                        public void onConnectionFailed(ConnectionResult connectionResult) {
                            Log.d("recognition_client", "connection failed");
                            mInProgress = false;

                            if(connectionResult.hasResolution()){
                                try {
                                    connectionResult.startResolutionForResult(MainActivity.this, CONNECTION_FAILURE_RESOLUTION_REQUEST);
                                } catch (IntentSender.SendIntentException e) {
                                    e.printStackTrace();
                                }
                            }else{
                                int errorCode = connectionResult.getErrorCode();
                                Toast.makeText(MainActivity.this, GooglePlayServicesUtil.getErrorString(errorCode), Toast.LENGTH_SHORT).show();
//            }
                            }
                        }
                    })
                    .build();

            Intent intent = new Intent(/*Intent.ACTION_SYNC, null,*/ this, ActivityRecognitionIntentService.class);
            ActivityRecognitionReceiver resultReceiver = new ActivityRecognitionReceiver(new Handler());
            resultReceiver.setReceiver(new ActivityRecognitionReceiver.Receiver() {
                @Override
                public void onReceivedResult(int resultCode, Bundle resultData) {
                    if(resultCode == RESULT_OK){
                        Toast.makeText(MainActivity.this, "activity: " + resultData.getString("activity"), Toast.LENGTH_SHORT).show();
                    }
                }
            });

            intent.putExtra("receiver", resultReceiver);
            mActivityRecognitionPendingIntent = PendingIntent.getService(this, 1000, intent, PendingIntent.FLAG_UPDATE_CURRENT);

            recognitionApi = ActivityRecognition.ActivityRecognitionApi;

            startUpdates();
        }else{
            new AsyncTask<Void, Void, Void>(){

                @Override
                protected Void doInBackground(Void... params) {
                    Collection<String> nodes = getNodes();

                    for(String node : nodes){
                        Log.d("node", "node id: " + node);
                        Wearable.MessageApi.sendMessage(wearClient, node, MESSAGE_PATH, new byte[0]);
                    }
                    return null;
                }
            }.execute();

        }
    }

    public void startUpdates(){
        mRequestType = REQUEST_TYPE.START;

        if(!servicesConnected()){
            return;
        }

        if(!mInProgress){
            recognitionClient.connect();
            if(recognitionClient.isConnected())
                Log.d("recognition_client", "connected");
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
    }

    private boolean servicesConnected(){
        int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);

        if(resultCode == ConnectionResult.SUCCESS){
            Log.d("Play services", "Google Play Services available");

            return true;
        }else{
            Toast.makeText(this, GooglePlayServicesUtil.getErrorString(resultCode), Toast.LENGTH_SHORT).show();
//            Dialog errorDialog = GooglePlayServicesUtil.getErrorDialog(resultCode, this, CONNECTION_FAILURE_RESOLUTION_REQUEST);
//
//            if(errorDialog != null){
//                ErrorDialogFragment errorFragment =  new ErrorDialogFragment();
//                errorFragment.setDialog(errorDialog);
//                errorFragment.show(getSupportFragmentManager(), "Google Play Services");
//            }

            return false;
        }
    }

    private boolean hasGPS(){
        return getPackageManager().hasSystemFeature(PackageManager.FEATURE_LOCATION_GPS);
    }

    private Collection<String> getNodes(){
        if(!wearClient.isConnected())
            wearClient.connect();

        HashSet<String> results = new HashSet<String>();

        NodeApi.GetConnectedNodesResult nodes = Wearable.NodeApi.getConnectedNodes(wearClient).await();

        for(Node node : nodes.getNodes()){
            results.add(node.getId());
        }

        return results;
    }

	private void initGestureFilter() {
		gestureFilter = new SimpleGestureFilter(this, this);
	}

	private void initViews() {
		dismissOverlay = (DismissOverlayView) findViewById(R.id.dismiss);
		canvas = (ImageView) findViewById(R.id.canvas);
		progressBar = (ProgressBar) findViewById(R.id.progress);
        ivSettings = (ImageView) findViewById(R.id.ivSettings);
        tvProgressCount = (TextView) findViewById(R.id.tvProgressCount);
        tvProgressTotal = (TextView) findViewById(R.id.tvProgressTotal);
//		initSlideLayouts();

        listAnimIds.add(R.drawable.scratch_the_ear);
        listAnimIds.add(R.drawable.sitting);
        listAnimIds.add(R.drawable.hold_still_b);
        listAnimIds.add(R.drawable.scratching);
        listAnimIds.add(R.drawable.smile);

        tvProgressTotal.setText(Config.PROGRESS_BAR_STEPS + "");

        canvas.setOnTouchListener(new View.OnTouchListener(){

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                gestureFilter.onTouchEvent(event);
                return true;
            }
        });

        ivSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(v.getContext(), ConfigActivity.class));
            }
        });
	}

	private void initSlideLayoutAnimation() {
		animUp = AnimationUtils.loadAnimation(this, R.anim.anim_up);
		animDown = AnimationUtils.loadAnimation(this, R.anim.anim_down);
		animLeft = AnimationUtils.loadAnimation(this, R.anim.anim_left);
		animRight = AnimationUtils.loadAnimation(this, R.anim.anim_right);
	}

	private void initSlideLayouts() {
		layoutTop = (LinearLayout) findViewById(R.id.layout_top);
		layoutBottom = (LinearLayout) findViewById(R.id.layout_bottom);
		layoutLeft = (LinearLayout) findViewById(R.id.layout_left);
		layoutRight = (LinearLayout) findViewById(R.id.layout_right);
	}



    private void initSensors() {
		sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
		stepDetector = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR);

		//TODO implement the significant motion to detect biking, running, is in a car, etc.
		//TODO implement jumping sensor
		// http://developer.android.com/guide/topics/sensors/sensors_motion.html
		// https://source.android.com/devices/sensors/composite_sensors.html
//		significantMotion = sensorManager
//				.getDefaultSensor(Sensor.TYPE_SIGNIFICANT_MOTION);
//		TriggerEventListener triggerEvent = new TriggerEventListener() {
//			public void onTrigger(TriggerEvent event) {
//				// Do work
//			}
//		};
//		sensorManager.requestTriggerSensor(triggerEvent, significantMotion);
	}

	private void playAnimalAnim(int animDrawable) {
		if (currentAnim == animDrawable) return;
        canvas.setBackgroundResource(0);
		currentAnim = animDrawable;
		canvas.setBackgroundResource(animDrawable);
		AnimationDrawable animation = (AnimationDrawable) canvas.getBackground();
		animation.start();
	}

	protected void onResume() {
		super.onResume();
		sensorManager.registerListener(this, stepDetector,
				SensorManager.SENSOR_DELAY_FASTEST);
	}

	protected void onStop() {
		super.onStop();
		sensorManager.unregisterListener(this, stepDetector);

        if(wearClient != null && wearClient.isConnected()){
            Wearable.DataApi.removeListener(wearClient, this);
        }
	}

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode){
            case CONNECTION_FAILURE_RESOLUTION_REQUEST:
                //connect again
                recognitionClient.connect();
                Log.d("google_api_client", "activity result");

                switch (resultCode){
                    case RESULT_OK:
                        //request again
                        break;
                }
                break;
        }
    }

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
	}

	public void onSensorChanged(SensorEvent event) {
		Sensor sensor = event.sensor;
		float[] values = event.values;
		int value = -1;

//		if (values.length > 0) {
//			value = (int) values[0];
//			progressBar.incrementProgressBy(value);
//		}

		if (sensor.getType() == Sensor.TYPE_STEP_DETECTOR) {
//			playAnimalAnim(WALKING);
            if(progressBar.getProgress() <= Config.PROGRESS_BAR_STEPS){
                progressBar.incrementProgressBy(1);
                tvProgressCount.setText(progressBar.getProgress() + "");

                PreferenceHelper.getInstance(this).setInt("stepCount", progressBar.getProgress());
            }
//            if(petHappiness > 0)
//                petHappiness--;

            checkAccomplishments();
		}
	}

//	@Override
//	public boolean dispatchTouchEvent(MotionEvent me) {
//		this.gestureFilter.onTouchEvent(me);
//		return super.dispatchTouchEvent(me);
//	}

	@Override
	public void onSwipe(int direction) {
//		hideSlideLayouts();

		switch (direction) {
		case SimpleGestureFilter.SWIPE_LEFT:
            if(isSwipeLeftUnlocked) {
                playAnimalAnim(R.drawable.fetch);
            }
            else
                Toast.makeText(this, "You have not unlock this trick!", Toast.LENGTH_SHORT).show();
//            Toast.makeText(this, "IDLE TIME = " + Config.IDLE_TIME, Toast.LENGTH_SHORT).show();
			break;
		case SimpleGestureFilter.SWIPE_UP:
            if(isSwipeUpUnlocked) {
                playAnimalAnim(R.drawable.roll_over);
            }
            else
                Toast.makeText(this, "You have not unlock this trick!", Toast.LENGTH_SHORT).show();
//            Toast.makeText(this, "NUM STEPS TO TRIGGER MORAL LOSS = " + Config.NUM_STEPS_TO_TRIGGER_MORAL_LOSS, Toast.LENGTH_SHORT).show();
			break;
		case SimpleGestureFilter.SWIPE_RIGHT:
            if(isSwipeRightUnlocked) {
                playAnimalAnim(R.drawable.play_dead);
            }
            else
                Toast.makeText(this, "You have not unlock this trick!", Toast.LENGTH_SHORT).show();
//            Toast.makeText(this, "PROGRESS BAR STEPS = " + Config.PROGRESS_BAR_STEPS, Toast.LENGTH_SHORT).show();
			break;
		case SimpleGestureFilter.SWIPE_DOWN:
            if(isSwipeDownUnlocked) {
                playAnimalAnim(R.drawable.scratch_the_ear);
            }
            else
                Toast.makeText(this, "You have not unlock this trick!", Toast.LENGTH_SHORT).show();
			break;
		}

//        switch (petHappiness){
//            case 30:
//                Toast.makeText(this, "Pet's Happiness is at MAX!", Toast.LENGTH_SHORT).show();
//                break;
//            case 25:
//                Toast.makeText(this, "Almost there!", Toast.LENGTH_SHORT).show();
//                break;
//            case 20:
//                Toast.makeText(this, "Pet now is hyped!", Toast.LENGTH_SHORT).show();
//                break;
//            case 10:
//                Toast.makeText(this, "Play with your pet more!", Toast.LENGTH_SHORT).show();
//                break;
//            case 1:
//                Toast.makeText(this, "You've made your first pet Trick! Keep it up!", Toast.LENGTH_SHORT).show();
//                break;
//        }
	}

	private void hideSlideLayouts() {
		layoutTop.setVisibility(View.GONE);
		layoutBottom.setVisibility(View.GONE);
		layoutLeft.setVisibility(View.GONE);
		layoutRight.setVisibility(View.GONE);
	}

	@Override
	public void onDoubleTap() {
		playAnimalAnim(R.drawable.hold_still_b);
//		hideSlideLayouts();
	}

	@Override
	public void onSingleTapConfirmed() {
//		hideSlideLayouts();
        playAnimalAnim(R.drawable.eat_a_treat);
	}

	@Override
	public void onLongPress() {

//        if(canvas.hasFocus())
		    dismissOverlay.show();
        Config.HAS_EXIT_APP = true;
        PreferenceHelper.getInstance(this).setInt("stepCount", 0);
        stopService(new Intent(this, StepService.class));
	}

    @Override
    protected void onStart() {
        super.onStart();

        if(!wearClient.isConnected())
            wearClient.connect();

        if(isServiceRunning(StepService.class)){
            stopService(new Intent(this, StepService.class));
        }

        tvProgressCount.setText(PreferenceHelper.getInstance(this).getInt("stepCount", 0) + "");
        progressBar.setProgress(PreferenceHelper.getInstance(this).getInt("stepCount", 0));
        Config.HAS_EXIT_APP = false;
        checkAccomplishments();
    }

    @Override
    protected void onPause() {
        super.onPause();

        if(!isServiceRunning(StepService.class) && !Config.HAS_EXIT_APP){
            startService(new Intent(this, StepService.class));
        }
    }

    @Override
    public void onConnected(Bundle bundle) {
        Log.d("wear device", "wear api connected");
        Wearable.DataApi.addListener(wearClient, this);

    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.d("wear device", "wear api connection suspended");
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {


    }

    @Override
    public void onDataChanged(DataEventBuffer dataEvents) {
        for(DataEvent event : dataEvents){
            DataMapItem item = DataMapItem.fromDataItem(event.getDataItem());
            Log.d("from phone data", "uri from  phone data: " + event.getDataItem().getUri() + ", Message: " + item.getDataMap().getString("activityName"));

            if(item.getUri().toString().contains("/activity-recognized")){
                final String activityName = item.getDataMap().getString("activityName");
                int activityType = item.getDataMap().getInt("activityType");

//                switch (activityType){
//                    case DetectedActivity
//                }
                //TODO: uncomment later after testing
//                runAnimationCorrespondingActivityRecognized(activityName);

            }


            canvas.setImageBitmap(loadBitmapFromAsset(item.getDataMap().getAsset("img")));
        }
    }

    private void runAnimationCorrespondingActivityRecognized(final String activityRecognized){

        runOnUiThread(new Runnable() {
            @Override
            public void run() {

                Toast.makeText(MainActivity.this, "activity: " + activityRecognized, Toast.LENGTH_SHORT).show();

                if(activityRecognized.equalsIgnoreCase("walking")) {
                    handler.removeCallbacksAndMessages(null);
                    playAnimalAnim(R.drawable.walking);
                }else if(activityRecognized.equalsIgnoreCase("running")){
                    handler.removeCallbacksAndMessages(null);
                    playAnimalAnim(R.drawable.running);
                }else if(activityRecognized.equalsIgnoreCase("still")){
//                    playAnimalAnim(R.drawable.scratch_the_ear);

                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            Random random = new Random();
                            playAnimalAnim(listAnimIds.get(random.nextInt(listAnimIds.size() - 1)));

                            handler.postDelayed(this, Config.REFRESH_ANIM * 1000);
                        }
                    }, Config.REFRESH_ANIM * 1000);
                }else{
                    playAnimalAnim(R.drawable.roll_over);
                }
            }
        });
    }

    public Bitmap loadBitmapFromAsset(Asset asset){
        if(asset == null){
            throw new IllegalArgumentException("Asset must not be null!");
        }

        ConnectionResult result = wearClient.blockingConnect(10, TimeUnit.SECONDS);

        if(!result.isSuccess()){
            return  null;
        }

        InputStream iStream = Wearable.DataApi.getFdForAsset(wearClient, asset).await().getInputStream();
//        wearClient.disconnect();

        if(iStream == null){
            Log.d("MainActivity Wear", "Requested unknown Asset");
            return null;
        }

        return BitmapFactory.decodeStream(iStream);
    }

    private boolean isServiceRunning(Class service){
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for(ActivityManager.RunningServiceInfo serviceInfo : manager.getRunningServices(Integer.MAX_VALUE)){
            if(serviceInfo.service.getClassName().equalsIgnoreCase(service.getName())){
                return true;
            }
        }

        return false;
    }

    public void checkAccomplishments(){
        if(progressBar.getProgress() >= (Config.PROGRESS_BAR_STEPS * .25) && !isSwipeLeftUnlocked) {
            isSwipeLeftUnlocked = true;
            Toast.makeText(this, "Hoooray! Swipe Left trick unlocked!", Toast.LENGTH_SHORT).show();
        }

        if(petHappiness >= (Config.PROGRESS_BAR_STEPS * .75) && !isSwipeUpUnlocked) {
            isSwipeUpUnlocked = true;
            Toast.makeText(this, "Hoooray! Swipe Up trick unlocked!", Toast.LENGTH_SHORT).show();
        }

        if(petHappiness >= (Config.PROGRESS_BAR_STEPS * .50) && !isSwipeRightUnlocked) {
            isSwipeRightUnlocked =  true;
            Toast.makeText(this, "Hoooray! Swipe Right trick unlocked!", Toast.LENGTH_SHORT).show();
        }

        if(petHappiness >= Config.PROGRESS_BAR_STEPS && !isSwipeDownUnlocked) {
            isSwipeDownUnlocked = true;
            Toast.makeText(this, "Hoooray! Swipe down trick unlocked!", Toast.LENGTH_SHORT).show();
        }
    }
}
