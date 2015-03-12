package com.kfast.uitest;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.wearable.view.WatchViewStub;
import android.support.wearable.view.WearableListView;
import android.widget.TextView;

import java.util.ArrayList;

public class ChoiceActivity extends Activity {

    private ArrayList<String> listChoices;
    private WearableListView lvChoices;
    private int from;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_config);

        from = getIntent().getExtras().getInt("from");
        listChoices = getListChoices(from);

        lvChoices = (WearableListView) findViewById(R.id.lvConfig);

        lvChoices.setAdapter(new MyWearListAdapter(this, listChoices));
        lvChoices.setClickListener(new WearableListView.ClickListener() {
            @Override
            public void onClick(WearableListView.ViewHolder viewHolder) {
                Intent result = new Intent();
                result.putExtra("choice", Integer.parseInt(listChoices.get((Integer) viewHolder.itemView.getTag())));
                setResult(Activity.RESULT_OK, result);
                finish();
            }

            @Override
            public void onTopEmptyRegionClick() {

            }
        });
    }

    private ArrayList<String> getListChoices(int from){
        ArrayList<String> list = new ArrayList<String>();

        switch (from){
            case 0:
                list.add("5");
                list.add("10");
                list.add("15");
                list.add("20");
                list.add("30");
                break;
            case 1:
                list.add("10");
                list.add("20");
                list.add("30");
                list.add("40");
                list.add("50");
                break;
            case 2:
                list.add("100");
                list.add("200");
                list.add("300");
                list.add("400");
                list.add("500");
                break;
            case 5:
                list.add("has step counter: " + getPackageManager().hasSystemFeature(PackageManager.FEATURE_SENSOR_STEP_COUNTER));
                list.add("has step detector: " + getPackageManager().hasSystemFeature(PackageManager.FEATURE_SENSOR_STEP_DETECTOR));
        }

        return list;
    }
}
