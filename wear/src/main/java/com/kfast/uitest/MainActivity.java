package com.kfast.uitest;

import android.app.Activity;
import android.app.Dialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.AnimationDrawable;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.wearable.activity.ConfirmationActivity;
import android.support.wearable.view.DismissOverlayView;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
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
import com.google.android.gms.wearable.Wearable;
import com.kfast.uitest.util.Config;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.TimeUnit;

public class MainActivity extends Activity implements SimpleGestureFilter.SimpleGestureListener,
		SensorEventListener, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, DataApi.DataListener {

    private enum REQUEST_TYPE {
        START, STOP
    }

    private REQUEST_TYPE mRequestType;

	private static final int HOLD_STILL_B = R.drawable.hold_still_b;
	private static final int WALKING = R.drawable.walking;
    private static final int HOLD_STILL_A = R.drawable.hold_still_a;
    private static final int EAT_A_TREAT = R.drawable.eat_a_treat;
    private static final int PLAY_DEAD = R.drawable.play_dead;
    private static final int ROLL_OVER = R.drawable.roll_over;
    private static final int RUNNING = R.drawable.running;
    private static final int SCRATCHING = R.drawable.scratching;
    private static final int SMILE = R.drawable.smile;

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
		playAnimalAnim(HOLD_STILL_B);

        //initialize google client
        initGoogleApiClient();

	}

    private void initGoogleApiClient(){
        wearClient = new GoogleApiClient.Builder(this)
                            .addConnectionCallbacks(this)
                            .addOnConnectionFailedListener(this)
                            .addApi(Wearable.API)
                            .build();

        recognitionClient = new GoogleApiClient.Builder(this)
                .addApi(ActivityRecognition.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();

        Intent intent = new Intent(this, ActivityRecognitionIntentService.class);
        mActivityRecognitionPendingIntent = PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        recognitionApi = ActivityRecognition.ActivityRecognitionApi;

        startUpdates();
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

	private void initGestureFilter() {
		gestureFilter = new SimpleGestureFilter(this, this);
	}

	private void initViews() {
		dismissOverlay = (DismissOverlayView) findViewById(R.id.dismiss);
		canvas = (ImageView) findViewById(R.id.canvas);
		progressBar = (ProgressBar) findViewById(R.id.progress);
        ivSettings = (ImageView) findViewById(R.id.ivSettings);
//		initSlideLayouts();

        listAnimIds.add(R.drawable.sitting);
        listAnimIds.add(R.drawable.hold_still_b);
        listAnimIds.add(R.drawable.scratching);
        listAnimIds.add(R.drawable.smile);

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
//                startActivity(new Intent(v.getContext(), ConfigActivity.class));
                File file = new File(getExternalCacheDir(), "img");

                if(file.exists())
                    Toast.makeText(v.getContext(), "img exists", Toast.LENGTH_SHORT).show();
                else
                    Toast.makeText(v.getContext(), "img does not exists", Toast.LENGTH_SHORT).show();
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
            progressBar.incrementProgressBy(1);

            if(petHappiness > 0)
                petHappiness--;
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
//            if(petHappiness >= (Config.PROGRESS_BAR_STEPS * .25)) {
//                Toast.makeText(this, "Hoooray! Swipe Left trick unlocked!", Toast.LENGTH_SHORT).show();
//                playAnimalAnim(R.drawable.fetch);
//
//                if(petHappiness < Config.NUM_STEPS_TO_TRIGGER_MORAL_LOSS)
//                    petHappiness++;
//            }
//            else
//                Toast.makeText(this, "You have not unlock this trick!", Toast.LENGTH_SHORT).show();
            Toast.makeText(this, "IDLE TIME = " + Config.IDLE_TIME, Toast.LENGTH_SHORT).show();
			break;
		case SimpleGestureFilter.SWIPE_UP:
//            if(petHappiness >= (Config.PROGRESS_BAR_STEPS * .75)) {
//                Toast.makeText(this, "Hoooray! Swipe Up trick unlocked!", Toast.LENGTH_SHORT).show();
//                playAnimalAnim(R.drawable.roll_over);
//
//                if(petHappiness < Config.NUM_STEPS_TO_TRIGGER_MORAL_LOSS)
//                    petHappiness++;
//            }
//            else
//                Toast.makeText(this, "You have not unlock this trick!", Toast.LENGTH_SHORT).show();
            Toast.makeText(this, "NUM STEPS TO TRIGGER MORAL LOSS = " + Config.NUM_STEPS_TO_TRIGGER_MORAL_LOSS, Toast.LENGTH_SHORT).show();
			break;
		case SimpleGestureFilter.SWIPE_RIGHT:
//            if(petHappiness >= (Config.PROGRESS_BAR_STEPS * .50)) {
//                Toast.makeText(this, "Hoooray! Swipe Right trick unlocked!", Toast.LENGTH_SHORT).show();
//                playAnimalAnim(R.drawable.play_dead);
//
//                if(petHappiness < Config.NUM_STEPS_TO_TRIGGER_MORAL_LOSS)
//                    petHappiness++;
//            }
//            else
//                Toast.makeText(this, "You have not unlock this trick!", Toast.LENGTH_SHORT).show();
            Toast.makeText(this, "PROGRESS BAR STEPS = " + Config.PROGRESS_BAR_STEPS, Toast.LENGTH_SHORT).show();
			break;
		case SimpleGestureFilter.SWIPE_DOWN:
//            if(petHappiness >= Config.PROGRESS_BAR_STEPS) {
//                Toast.makeText(this, "Hoooray! Swipe down trick unlocked!", Toast.LENGTH_SHORT).show();
//                playAnimalAnim(R.drawable.scratch_the_ear);
//
//                if(petHappiness < Config.NUM_STEPS_TO_TRIGGER_MORAL_LOSS)
//                    petHappiness++;
//            }
//            else
//                Toast.makeText(this, "You have not unlock this trick!", Toast.LENGTH_SHORT).show();
			break;
		}

        switch (petHappiness){
            case 30:
                Toast.makeText(this, "Pet's Happiness is at MAX!", Toast.LENGTH_SHORT).show();
                break;
            case 25:
                Toast.makeText(this, "Almost there!", Toast.LENGTH_SHORT).show();
                break;
            case 20:
                Toast.makeText(this, "Pet now is hyped!", Toast.LENGTH_SHORT).show();
                break;
            case 10:
                Toast.makeText(this, "Play with your pet more!", Toast.LENGTH_SHORT).show();
                break;
            case 1:
                Toast.makeText(this, "You've made your first pet Trick! Keep it up!", Toast.LENGTH_SHORT).show();
                break;
        }
	}

	private void hideSlideLayouts() {
		layoutTop.setVisibility(View.GONE);
		layoutBottom.setVisibility(View.GONE);
		layoutLeft.setVisibility(View.GONE);
		layoutRight.setVisibility(View.GONE);
	}

	@Override
	public void onDoubleTap() {
		playAnimalAnim(HOLD_STILL_B);
//		hideSlideLayouts();
	}

	@Override
	public void onSingleTapConfirmed() {
//		hideSlideLayouts();
        playAnimalAnim(EAT_A_TREAT);
	}

	@Override
	public void onLongPress() {

//        if(canvas.hasFocus())
		    dismissOverlay.show();
	}

    @Override
    protected void onStart() {
        super.onStart();
        wearClient.connect();
    }



    @Override
    public void onConnected(Bundle bundle) {
        Log.d("wear device", "wear api connected");
        Wearable.DataApi.addListener(wearClient, this);
//        switch (mRequestType){
//            case START:
//                recognitionApi.requestActivityUpdates(recognitionClient, DETECTION_INTERVAL_MILLISECONDS, mActivityRecognitionPendingIntent);
////                ActivityRecognitionResult activityResult = (ActivityRecognitionResult) recognitionApi.requestActivityUpdates(recognitionClient, DETECTION_INTERVAL_MILLISECONDS, mActivityRecognitionPendingIntent);
//                break;
//            case STOP:
//                recognitionApi.removeActivityUpdates(recognitionClient, mActivityRecognitionPendingIntent);
//                break;
//        }
//
//        mInProgress = false;
//        recognitionClient.disconnect();
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.d("wear device", "wear api connection suspended");
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
            Toast.makeText(this, GooglePlayServicesUtil.getErrorString(errorCode), Toast.LENGTH_SHORT).show();
//            Dialog errorDialog = GooglePlayServicesUtil.getErrorDialog(errorCode, this, CONNECTION_FAILURE_RESOLUTION_REQUEST);
//
//            if(errorDialog != null){
//                ErrorDialogFragment errorFragment = new ErrorDialogFragment();
//                errorFragment.setDialog(errorDialog);
//                errorFragment.show(getSupportFragmentManager(), "Connection Failed");
//            }
        }
    }

    @Override
    public void onDataChanged(DataEventBuffer dataEvents) {
        for(DataEvent event : dataEvents){
            DataMapItem item = DataMapItem.fromDataItem(event.getDataItem());
            Log.d("from phone data", "uri from  phone data: " + event.getDataItem().getUri() + ", Message: " + item.getDataMap().getString("message_service"));

//            Toast.makeText(this, ", Message: " + item.getDataMap().getString("message_service"), Toast.LENGTH_LONG).show();
            canvas.setImageBitmap(loadBitmapFromAsset(item.getDataMap().getAsset("img")));
            if(item.getUri().toString().contains("/test-service")){
                final String message = item.getDataMap().getString("message_service");
                int activityType = item.getDataMap().getInt("activityType");

//                switch (activityType){
//                    case DetectedActivity
//                }

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if(message.equalsIgnoreCase("walking")) {
                            handler.removeCallbacksAndMessages(null);
                            playAnimalAnim(WALKING);
                        }else if(message.equalsIgnoreCase("running")){
                            handler.removeCallbacksAndMessages(null);
                            playAnimalAnim(RUNNING);
                        }else if(message.equalsIgnoreCase("still")){
                            playAnimalAnim(SCRATCHING);

                            handler.postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    Random random = new Random();
                                    playAnimalAnim(listAnimIds.get(random.nextInt(listAnimIds.size() - 1)));

                                    handler.postDelayed(this, Config.IDLE_TIME * 1000);
                                }
                            }, Config.IDLE_TIME * 1000);
                        }else{
                            playAnimalAnim(R.drawable.roll_over);
                        }
                    }
                });

            }
        }
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
}
