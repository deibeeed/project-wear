package com.kfast.uitest.service;

import android.os.Binder;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.data.FreezableUtils;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.WearableListenerService;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Created by David on 2014-12-14.
 */
public class DataLayerListenerService extends WearableListenerService {

    private static final String TAG = "DataLayerListener";
    private static final String START_aCTIVITY_PATH = "/start-activity";
    private static final String DATA_ITEM_RECEIVED_PATH = "/data-item-received";

    @Override
    public void onDataChanged(DataEventBuffer dataEvents) {
        super.onDataChanged(dataEvents);

        final List<DataEvent> events = FreezableUtils.freezeIterable(dataEvents);

//        GoogleApiClient client = new GoogleApiClient.Builder(this)
//                                    .addApi(Wearable.API)
//                                    .build();
//
//        ConnectionResult result = client.blockingConnect(30, TimeUnit.SECONDS);
//
//        if(!result.isSuccess()){
//            Log.e(TAG, "failed to connect to google api client");
//        }

        for(DataEvent event : events){
            String uri = event.getDataItem().getUri().toString();

//                if(uri.equalsIgnoreCase("/test")){
            DataMapItem dataItem = DataMapItem.fromDataItem(event.getDataItem());
            String message = dataItem.getDataMap().getString("message");

            Log.d(TAG, "detected message from device: "  + message);

//                }
        }
    }


}
