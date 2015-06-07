package com.kfast.uitest;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.BitmapDrawable;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Vibrator;
import android.support.wearable.view.DismissOverlayView;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextClock;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.location.ActivityRecognition;
import com.google.android.gms.location.ActivityRecognitionApi;
import com.google.android.gms.wearable.Asset;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;
import com.kfast.uitest.model.AnimationDirectory;
import com.kfast.uitest.model.UnsentSteps;
import com.kfast.uitest.receiver.ActivityRecognitionReceiver;
import com.kfast.uitest.receiver.FitPetBroadcastReceiver;
import com.kfast.uitest.service.ActivityRecognitionIntentService;
import com.kfast.uitest.service.AlarmService;
import com.kfast.uitest.service.StepService;
import com.kfast.uitest.util.Config;
import com.kfast.uitest.util.ObjectSerializer;
import com.kfast.uitest.util.PreferenceHelper;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashSet;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

public class MainActivity extends Activity implements SimpleGestureFilter.SimpleGestureListener,
		SensorEventListener, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, DataApi.DataListener {

    private enum REQUEST_TYPE {
        START, STOP
    }

    private REQUEST_TYPE mRequestType;

    private static final String MESSAGE_PATH = "/start/activity-recognition";
    private static final String RECORD_STEPS_PATH = "/step-count-";

	private SimpleGestureFilter gestureFilter;
	private DismissOverlayView dismissOverlay;
	private Sensor stepDetector;
    private Sensor stepCounter;
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

//    private ImageView ivSettings;

    private boolean isSwipeUpUnlocked;
    private boolean isSwipeDownUnlocked;
    private boolean isSwipeLeftUnlocked;
    private boolean isSwipeRightUnlocked;

    private TextView tvProgressCount;
    private TextView tvProgressTotal;

    private int stepHolder;

    private Handler motionHandler;
    private int stillAnimationPosition;
    private int stillTimeHolder;

    private boolean firstRun;
    private boolean startStepCount;
    private int totalStepCount;

    private Vibrator vibrator;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
//		setContentView(R.layout.activity_main);
        setContentView(R.layout.activity_main_2);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        handler = new Handler();
        listAnimIds = new ArrayList<Integer>();
        petHappiness = 0;

        motionHandler = new Handler();

		initGestureFilter();
		initViews();
		initSensors();
//		hideSlideLayouts();
		initSlideLayoutAnimation();
		playAnimalAnim(R.drawable.hold_still_b);

        //initialize google client
        initGoogleApiClient();

//        Log.wtf("img_exist", "does chrome_icon.png exist? " + isBitmapExist("chrome_icon.png"));
//
//        if(isBitmapExist("chrome_icon.png")){
//            canvas.setImageBitmap(getBitmapFromDisk("chrome_icon.png"));
//        }
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

            sendToPhone(MESSAGE_PATH);
        }
    }

    private void sendToPhone(final String path){
        new AsyncTask<Void, Void, Void>(){

            @Override
            protected Void doInBackground(Void... params) {
                Collection<String> nodes = getNodes();

                if(nodes.size() > 0){
                    for(String node : nodes){
                        Log.d("node", "node id: " + node);

                        Wearable.MessageApi.sendMessage(wearClient, node, path, new byte[0]);

                        if(path.contains(RECORD_STEPS_PATH)){
                            String[] str = path.split(Pattern.quote("-"));
                            int stepCount = Integer.parseInt(str[2]);

                            ArrayList<UnsentSteps> listSteps = (ArrayList<UnsentSteps>) ObjectSerializer.loadSerializedObject(MainActivity.this, "MySteps", false);

                            if(listSteps != null){
                                for(UnsentSteps steps : listSteps){
                                    String newPath = RECORD_STEPS_PATH + steps.getStepCount() + "/date/" + steps.getDate();
                                    Wearable.MessageApi.sendMessage(wearClient, node, newPath, new byte[0]);

                                    listSteps.remove(steps);
                                }

                                ObjectSerializer.saveSerializedObject(MainActivity.this, listSteps, "MySteps");
                            }
                        }
                    }
                }else{

                    if(path.contains(RECORD_STEPS_PATH)){
                        String[] str = path.split(Pattern.quote("-"));
                        int stepCount = Integer.parseInt(str[2]);

                        Calendar cal = Calendar.getInstance();
                        cal.setTimeInMillis(System.currentTimeMillis());

                        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
                        String formattedDate = formatter.format(cal.getTime());

                        boolean hasSameDate = false;

                        ArrayList<UnsentSteps> listSteps = (ArrayList<UnsentSteps>) ObjectSerializer.loadSerializedObject(MainActivity.this, "MySteps", false);

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

                        ObjectSerializer.saveSerializedObject(MainActivity.this, listSteps, "MySteps");
                    }
                }
                return null;
            }
        }.execute();
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
//        ivSettings = (ImageView) findViewById(R.id.ivSettings);
        tvProgressCount = (TextView) findViewById(R.id.tvProgressCount);
        tvProgressTotal = (TextView) findViewById(R.id.tvProgressTotal);
        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
//		initSlideLayouts();

//        listAnimIds.add(R.drawable.scratch_the_ear);
        listAnimIds.add(R.drawable.scratching);
        listAnimIds.add(R.drawable.sitting);
        listAnimIds.add(R.drawable.hold_still_b);
        listAnimIds.add(R.drawable.smile);

        tvProgressTotal.setText(Config.PROGRESS_BAR_STEPS + "");

//        canvas.setOnTouchListener(new View.OnTouchListener(){
//
//            @Override
//            public boolean onTouch(View v, MotionEvent event) {
//                gestureFilter.onTouchEvent(event);
//                return true;
//            }
//        });

//        ivSettings.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                startActivity(new Intent(v.getContext(), ConfigActivity.class));
//            }
//        });
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
        stepCounter = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);

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

    private void startAnimation(boolean restart){

        if(restart)
            motionHandler.removeCallbacksAndMessages(null);

        motionHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                Log.d("step counter", "num of steps: " + stepHolder);
                if (stepHolder > 0 && stepHolder <= 3) {
//                    Toast.makeText(MainActivity.this, "walking", Toast.LENGTH_SHORT).show();
                    Log.d("action", "walking");
                    stillTimeHolder = 0;
                    runAnimationCorrespondingActivityRecognized("walking");
                } else if (stepHolder > 3) {
//                    Toast.makeText(MainActivity.this, "running", Toast.LENGTH_SHORT).show();
                    Log.d("action", "running");
                    stillTimeHolder = 0;
                    runAnimationCorrespondingActivityRecognized("running");
                } else {
                    Log.d("action", "still");

                    if (stillTimeHolder == PreferenceHelper.getInstance(MainActivity.this).getInt(Config.Keys.KEY_IDLE_TIME, Config.IDLE_TIME)/*Config.REFRESH_ANIM*/) {
                        stillTimeHolder = 0;
                        runAnimationCorrespondingActivityRecognized("still");
                    } else if (stillTimeHolder == 0) {
                        playAnimalAnim(R.drawable.hold_still_b);
                    }

                    stillTimeHolder++;
                }

                stepHolder = 0;

                motionHandler.postDelayed(this, 1000);
            }
        }, 1000);
    }

    @Override
    protected void onStart() {
        super.onStart();

        if(!wearClient.isConnected())
            wearClient.connect();

        if(isServiceRunning(StepService.class)){
            stopService(new Intent(this, StepService.class));
        }

        firstRun = true;
        startStepCount = false;

        TextClock clock = (TextClock) findViewById(R.id.clock);

//        if(clock.getText().equals("00:00")){
//            totalStepCount = 0;
//            PreferenceHelper.getInstance(this).setInt("stepCount", 0);
//        }else{
//            totalStepCount = PreferenceHelper.getInstance(this).getInt("stepCount", 0);
//        }

        totalStepCount = PreferenceHelper.getInstance(this).getInt(Config.Keys.KEY_STEP_COUNT, 0);
        int maxProgress = PreferenceHelper.getInstance(this).getInt(Config.Keys.KEY_PROGRESS_STEPS, Config.PROGRESS_BAR_STEPS);

        if(totalStepCount > maxProgress){
            progressBar.setProgress(maxProgress);
        }else{
            progressBar.setProgress(PreferenceHelper.getInstance(this).getInt(Config.Keys.KEY_STEP_COUNT, 0));
        }

        tvProgressCount.setText(PreferenceHelper.getInstance(this).getInt(Config.Keys.KEY_STEP_COUNT, 0) + "");

        progressBar.setMax(PreferenceHelper.getInstance(this).getInt(Config.Keys.KEY_PROGRESS_STEPS, Config.PROGRESS_BAR_STEPS));
        tvProgressTotal.setText(progressBar.getMax() + "");

        Config.HAS_EXIT_APP = false;
        checkAccomplishments(true);

        startAnimation(false);
    }

    @Override
    protected void onPause() {
        super.onPause();

//        if(!isServiceRunning(StepService.class) && !Config.HAS_EXIT_APP){
//            startService(new Intent(this, StepService.class));
//        }
    }

    @Override
	protected void onResume() {
		super.onResume();

        if(!wearClient.isConnected())
            wearClient.connect();

//        try{
//            registerReceiver(new FitPetBroadcastReceiver(), new IntentFilter(Config.Action.ACTION_BROADCAST_ALARM));
//            sendBroadcast(new Intent(Config.Action.ACTION_BROADCAST_ALARM).putExtra("startedFrom", "activity"));
//        }catch (Exception e){
//            e.printStackTrace();
//        }

        if(!isServiceRunning(AlarmService.class))
            startService(new Intent(this, AlarmService.class));

		sensorManager.registerListener(this, stepDetector, SensorManager.SENSOR_DELAY_FASTEST);
        sensorManager.registerListener(this, stepCounter, SensorManager.SENSOR_DELAY_FASTEST);
	}

    @Override
	protected void onStop() {
		super.onStop();
		sensorManager.unregisterListener(this, stepDetector);
        sensorManager.unregisterListener(this, stepCounter);

        if(wearClient != null && wearClient.isConnected()){
            Wearable.DataApi.removeListener(wearClient, this);
            wearClient.disconnect();
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

		if (sensor.getType() == Sensor.TYPE_STEP_DETECTOR || sensor.getType() == Sensor.TYPE_STEP_COUNTER) {
            Log.d("sensor", "fire!!");
//			playAnimalAnim(WALKING);
            if(progressBar.getProgress() <= progressBar.getMax() && !firstRun){
                startStepCount = true;
                totalStepCount++;

                progressBar.incrementProgressBy(1);
//                tvProgressCount.setText(progressBar.getProgress() + "");
                tvProgressCount.setText(totalStepCount + "");

//                PreferenceHelper.getInstance(this).setInt("stepCount", progressBar.getProgress());
                PreferenceHelper.getInstance(this).setInt(Config.Keys.KEY_STEP_COUNT, totalStepCount);

//                stepHolder += 1;

//            if(petHappiness > 0)
//                petHappiness--;

                checkAccomplishments(false);

                if(totalStepCount % 50 == 0){
                    Log.d("wear-steps", "Data send to phone: " +  progressBar.getProgress());
                    sendToPhone(RECORD_STEPS_PATH + 50/*progressBar.getProgress()*/);
                }
            }else{
                firstRun = false;
            }
		}

        if(startStepCount)
            stepHolder += 1;
	}

	@Override
	public boolean dispatchTouchEvent(MotionEvent me) {
		this.gestureFilter.onTouchEvent(me);
		return super.dispatchTouchEvent(me);
	}

	@Override
	public void onSwipe(int direction) {
//		hideSlideLayouts();

		switch (direction) {
		case SimpleGestureFilter.SWIPE_LEFT:
//            Log.d("config_swipe", "idle_time: " + PreferenceHelper.getInstance(this).getInt(Config.Keys.KEY_IDLE_TIME, Config.IDLE_TIME));
            if(isSwipeLeftUnlocked) {
                playAnimalAnim(R.drawable.fetch);

                startAnimation(true);
            }
            else
                Toast.makeText(this, "Not enough steps, trick Fetch is not yet unlocked!", Toast.LENGTH_SHORT).show();

			break;
		case SimpleGestureFilter.SWIPE_UP:
//            Log.d("config_swipe", "steps_to_moral_loss: " + PreferenceHelper.getInstance(this).getInt(Config.Keys.KEY_STEP_MORAL_LOSS, Config.NUM_STEPS_TO_TRIGGER_MORAL_LOSS));
            if(isSwipeUpUnlocked) {
                playAnimalAnim(R.drawable.roll_over);

                startAnimation(true);
            }
            else
                Toast.makeText(this, "Not enough steps, trick Roll Over is not yet unlocked!", Toast.LENGTH_SHORT).show();

			break;
		case SimpleGestureFilter.SWIPE_RIGHT:
//            Log.d("config_swipe", "progress_steps: " + PreferenceHelper.getInstance(this).getInt(Config.Keys.KEY_PROGRESS_STEPS, Config.PROGRESS_BAR_STEPS));
            if(isSwipeRightUnlocked) {
                playAnimalAnim(R.drawable.play_dead);

                startAnimation(true);
            }
            else
                Toast.makeText(this, "Not enough steps, trick Play Dead is not yet unlocked!", Toast.LENGTH_SHORT).show();

			break;
		case SimpleGestureFilter.SWIPE_DOWN:
            if(isSwipeDownUnlocked) {
                playAnimalAnim(R.drawable.scratch_the_ear);

                startAnimation(true);
            }
            else
                Toast.makeText(this, "Not enough steps, trick Scratch the Ear is not yet unlocked!", Toast.LENGTH_SHORT).show();
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

        startAnimation(true);
//		hideSlideLayouts();
	}

	@Override
	public void onSingleTapConfirmed() {
//		hideSlideLayouts();
        //TODO: uncomment codes below after testing
//        playAnimalAnim(R.drawable.eat_a_treat);
//
//        startAnimation(true);

        playAnimationFromDisk("trick5");
	}

	@Override
	public void onLongPress() {

//        if(canvas.hasFocus())
		    dismissOverlay.show();
        Config.HAS_EXIT_APP = true;
//        PreferenceHelper.getInstance(this).setInt("stepCount", 0);
//        stopService(new Intent(this, StepService.class));

//        int progress = (progressBar.getProgress() % 50 != 0 ? progressBar.getProgress() % 50 : 50);
        int progress = (totalStepCount % 50 != 0 ? totalStepCount % 50 : 50);

        sendToPhone(RECORD_STEPS_PATH + progress);
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
                //TODO: uncomment line below after testing
//                runAnimationCorrespondingActivityRecognized(activityName);

            }else if(item.getUri().toString().contains("/config")){
                String key = item.getDataMap().getString("key");
                final String value = item.getDataMap().getString("value");

                Log.d("config", "key: " + key + ", value: " + value);

                if(key.equals(Config.Keys.KEY_PROGRESS_STEPS)){
                    PreferenceHelper.getInstance(this).setInt(key, Integer.parseInt(value));

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            progressBar.setMax(Integer.parseInt(value));
                            tvProgressTotal.setText(value);
                            checkAccomplishments(true);
                        }
                    });
                }else if(key.equals(Config.Keys.KEY_RESET_STEP_COUNT)){
                    PreferenceHelper.getInstance(this).setInt(Config.Keys.KEY_STEP_COUNT, 0);

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            tvProgressCount.setText("0");
                            progressBar.setProgress(0);
                            checkAccomplishments(true);
                        }
                    });
                }else if(key.equals(Config.Keys.KEY_RESTORE_DEFAULT)){
//                    PreferenceHelper.getInstance(this).setBoolean(key, Boolean.parseBoolean(value));

                    PreferenceHelper.getInstance(this).setInt(Config.Keys.KEY_IDLE_TIME, Config.IDLE_TIME);
                    PreferenceHelper.getInstance(this).setInt(Config.Keys.KEY_STEP_MORAL_LOSS, Config.NUM_STEPS_TO_TRIGGER_MORAL_LOSS);
                    PreferenceHelper.getInstance(this).setInt(Config.Keys.KEY_PROGRESS_STEPS, Config.PROGRESS_BAR_STEPS);

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            tvProgressTotal.setText(Config.PROGRESS_BAR_STEPS + "");
                            progressBar.setMax(Config.PROGRESS_BAR_STEPS);
                        }
                    });
                }else{
                    PreferenceHelper.getInstance(this).setInt(key, Integer.parseInt(value));
                }
            }else{
                Bitmap bmp = loadBitmapFromAsset(item.getDataMap().getAsset("img"));
//                canvas.setImageBitmap(bmp);

                String imgName = item.getDataMap().getString("img_name");
                String animGroup = item.getDataMap().getString("anim_group");
                Log.d("data_from_phone", "anim group: " + animGroup + " img name: " + imgName);
                saveBitmapToDisk(bmp, animGroup, imgName);
            }
        }
    }

    private void runAnimationCorrespondingActivityRecognized(final String activityRecognized){

        runOnUiThread(new Runnable() {
            @Override
            public void run() {

//                Toast.makeText(MainActivity.this, "activity: " + activityRecognized, Toast.LENGTH_SHORT).show();

                if(activityRecognized.equalsIgnoreCase("walking")) {
//                    handler.removeCallbacksAndMessages(null);
                    //TODO: uncomment file below after testing
//                    playAnimalAnim(R.drawable.walking);
                    playAnimationFromDisk("walk");
                }else if(activityRecognized.equalsIgnoreCase("running")){
//                    handler.removeCallbacksAndMessages(null);

                    //TODO: uncomment file bellow after testing
//                    playAnimalAnim(R.drawable.running);
                    playAnimationFromDisk("run");
                }else if(activityRecognized.equalsIgnoreCase("still")){
//                    playAnimalAnim(R.drawable.scratch_the_ear);

//                    handler.postDelayed(new Runnable() {
//                        @Override
//                        public void run() {
////                            Random random = new Random();
//                            playAnimalAnim(listAnimIds.get(stillAnimationPosition));
//
//                            if(stillAnimationPosition == 4)
//                                stillAnimationPosition = 0;
//                            else
//                                stillAnimationPosition++;
//
//                            handler.postDelayed(this, Config.REFRESH_ANIM * 1000);
//
//                        }
//                    }, Config.REFRESH_ANIM * 1000);

                    playAnimalAnim(listAnimIds.get(stillAnimationPosition));

                    if(stillAnimationPosition == listAnimIds.size() - 1)
                        stillAnimationPosition = 0;
                    else
                        stillAnimationPosition++;
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

    private void saveBitmapToDisk(Bitmap bitmap, String animGroup, String imgName){
//        String imgPath = Environment.getExternalStorageDirectory() + "/fitpet/";

        String imgPath = getExternalCacheDir() + "/";
        File file = new File(imgPath);

        file.mkdirs();

        try {
            BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(imgPath + imgName));
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, bos);
            bos.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        ArrayList<AnimationDirectory> listAnims = (ArrayList<AnimationDirectory>) ObjectSerializer.loadSerializedObject(this, "fitpet-anims", false);
        ArrayList<AnimationDirectory> listAnimToAdd = new ArrayList<>();

        if(listAnims != null){
            boolean hasInserted = false;
            for(AnimationDirectory animationDirectory : listAnims){
                if(animationDirectory.getAnimationGroup().equalsIgnoreCase(animGroup)){
                    ArrayList<String> listAnimationItem = animationDirectory.getAnimationItem();

                    listAnimationItem.add(imgName);
                    hasInserted = true;
                }
            }

            if(!hasInserted){
                ArrayList<String> listAnimItems = new ArrayList<>();
                listAnimItems.add(imgName);
                listAnims.add(new AnimationDirectory(animGroup, listAnimItems));
            }
        }else{
            listAnims = new ArrayList<>();
            ArrayList<String> listAnimItems = new ArrayList<>();
            listAnimItems.add(imgName);
            listAnims.add(new AnimationDirectory(animGroup, listAnimItems));
        }

        if(listAnimToAdd.size() > 0){
            listAnims.addAll(listAnimToAdd);
            Log.d("save_to_disk", "added new group");
        }



        ObjectSerializer.saveSerializedObject(this, listAnims, "fitpet-anims");
    }

    private boolean isBitmapExist(String imgName){
        boolean flag = false;
//        String imgPath = Environment.getExternalStorageDirectory() + "/fitpet/" + imgName;
        String imgPath = getExternalCacheDir() + "/" + imgName;

        try {
            FileInputStream fis = new FileInputStream(imgPath);
            if(fis != null){
                flag = true;
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        return flag;
    }

    private Bitmap getBitmapFromDisk(String imgName){
//        String imgPath = Environment.getExternalStorageDirectory() + "/fitpet/" + imgName;
        String imgPath = getExternalCacheDir() + "/" + imgName;
        try {
            FileInputStream fis = new FileInputStream(imgPath);

            return BitmapFactory.decodeStream(fis);
        } catch (FileNotFoundException e) {
            e.printStackTrace();

            return null;
        }
    }

    public void playAnimationFromDisk(String action){
        ArrayList<AnimationDirectory> listAnims = (ArrayList<AnimationDirectory>) ObjectSerializer.loadSerializedObject(this, "fitpet-anims", false);
        AnimationDrawable animationDrawable = new AnimationDrawable();

        if(listAnims != null){
            if(listAnims.size() > 0){
                for (AnimationDirectory animationDirectory : listAnims){
                    Log.d("img_", "img path" + animationDirectory.getAnimationGroup());
                    if(animationDirectory.getAnimationGroup().contains(action)){
                        for(String imgName : animationDirectory.getAnimationItem()){
                            String imgPath = getExternalCacheDir() + "/" + imgName;
                            Log.d("path_to_exec", "img path: " + imgPath + " imgGroup: " + animationDirectory.getAnimationGroup());
                            animationDrawable.addFrame(new BitmapDrawable(BitmapFactory.decodeFile(imgPath)), 55);
                        }

                        break;
                    }else{
                        Log.d("img_not_found", "animation group: " + animationDirectory.getAnimationGroup());
                    }
                }
            }

            canvas.setImageDrawable(animationDrawable);
            animationDrawable.setOneShot(false);
            animationDrawable.start();
        }

        //checking

//        File file = getExternalCacheDir();
//
//        for(File f : file.listFiles()){
//            Log.d("in_file", f.getAbsolutePath());
//        }
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

    public void checkAccomplishments(boolean reset){
        int progressBarSteps = PreferenceHelper.getInstance(this).getInt(Config.Keys.KEY_PROGRESS_STEPS, Config.PROGRESS_BAR_STEPS);
        int progress = progressBar.getProgress();

        if(reset){
            isSwipeLeftUnlocked = false;
            isSwipeUpUnlocked = false;
            isSwipeRightUnlocked =  false;
            isSwipeDownUnlocked = false;
        }

        if(progress >= (progressBarSteps * .25) && !isSwipeLeftUnlocked) {
            vibrator.vibrate(40);
            isSwipeLeftUnlocked = true;
            Toast.makeText(this, "Hoooray! Swipe Left trick unlocked!", Toast.LENGTH_SHORT).show();
        }

        if(progress >= (progressBarSteps * .75) && !isSwipeUpUnlocked) {
            vibrator.vibrate(40);
            isSwipeUpUnlocked = true;
            Toast.makeText(this, "Hoooray! Swipe Up trick unlocked!", Toast.LENGTH_SHORT).show();
        }

        if(progress >= (progressBarSteps * .50) && !isSwipeRightUnlocked) {
            vibrator.vibrate(40);
            isSwipeRightUnlocked =  true;
            Toast.makeText(this, "Hoooray! Swipe Right trick unlocked!", Toast.LENGTH_SHORT).show();
        }

        if(progress >= progressBarSteps && !isSwipeDownUnlocked) {
            vibrator.vibrate(40);
            isSwipeDownUnlocked = true;
            Toast.makeText(this, "Hoooray! Swipe down trick unlocked!", Toast.LENGTH_SHORT).show();
        }
    }
}
