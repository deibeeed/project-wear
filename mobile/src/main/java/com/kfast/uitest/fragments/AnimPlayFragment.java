package com.kfast.uitest.fragments;


import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationSet;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.Asset;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;
import com.kfast.uitest.R;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link AnimPlayFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class AnimPlayFragment extends Fragment {
    private static final String TAG = "AnimPlayFragment";

    private View container;
    private AnimationDrawable animation;
    private ImageView ivAnimation;

    private Button btnPush;

    private PendingResult<DataApi.DataItemResult> wearPendingResult;

    private GoogleApiClient wearClient;

    public static AnimPlayFragment newInstance() {
        AnimPlayFragment fragment = new AnimPlayFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    public AnimPlayFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {

        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup vgContainer,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        container = inflater.inflate(R.layout.fragment_anim_play, vgContainer, false);
        ivAnimation = (ImageView) container.findViewById(R.id.ivAnimationPic);
        btnPush = (Button) container.findViewById(R.id.btnPush);
        animation = new AnimationDrawable();

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

        createAnimationFromResource(ivAnimation, animation, "cat_running_compressed");

        btnPush.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                File grandParent = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES) + File.separator + "fitpet" + File.separator);

                if(grandParent.listFiles().length > 0){
                    for(File parent : grandParent.listFiles()){
                        if(parent.listFiles().length > 0){
                            for(File childFile : parent.listFiles()){
                                Log.d(TAG, "data sent --------- ");
                                Log.d(TAG, "group name: " + parent.getName());
                                Log.d(TAG, "img name: " + childFile.getName());
                                Log.d(TAG, "end of sent --------- ");
                                sendPetImageToWear(BitmapFactory.decodeFile(childFile.getAbsolutePath()), parent.getName(), childFile.getName());
//                                Log.d("path_to_send", "child absolute path: " + childFile.getAbsolutePath() + " parent name: " + parent.getName() + " child name: " + childFile.getName());
                            }
                        }
                    }
                }
            }
        });

        return container;
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

    private Asset createAssetFromBitmap(Bitmap bmp){
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bmp.compress(Bitmap.CompressFormat.PNG, 100, baos);

        return Asset.createFromBytes(baos.toByteArray());
    }

    private void createAnimationFromResource(ImageView ivMedia, AnimationDrawable animation, String folderName){{
        File imgFile = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
                + File.separator + "fitpet" + File.separator + folderName);

        for(int i = 0; i < imgFile.listFiles().length; i++){
            Bitmap bmp = BitmapFactory.decodeFile(imgFile.listFiles()[i].getAbsolutePath());
            animation.addFrame(new BitmapDrawable(bmp), 55);
        }

        ivMedia.setImageDrawable(animation);
        animation.setOneShot(false);
        animation.start();
    }

    }
}
