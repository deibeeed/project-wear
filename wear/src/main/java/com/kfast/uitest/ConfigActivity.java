package com.kfast.uitest;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.wearable.activity.ConfirmationActivity;
import android.support.wearable.view.WatchViewStub;
import android.support.wearable.view.WearableListView;
import android.widget.TextView;
import android.widget.Toast;

import com.kfast.uitest.util.Config;
import com.kfast.uitest.util.PreferenceHelper;

import java.util.ArrayList;

public class ConfigActivity extends Activity {

    private WearableListView lvConfig;
    private ArrayList<String> listItems;
    private int selectedItemPosition;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_config);

        listItems = configMenu();

        lvConfig = (WearableListView) findViewById(R.id.lvConfig);

        lvConfig.setAdapter(new MyWearListAdapter(this, listItems));
        lvConfig.setClickListener(new WearableListView.ClickListener() {
            @Override
            public void onClick(final WearableListView.ViewHolder viewHolder) {
                Integer tag = (Integer) viewHolder.itemView.getTag();
//                Toast.makeText(viewHolder.itemView.getContext(), "selected item: " + listItems.get(tag), Toast.LENGTH_SHORT).show();

                selectedItemPosition = tag;

                switch (tag){
                    case 5:
                        startActivity(new Intent(viewHolder.itemView.getContext(), ChoiceActivity.class).putExtra("from", 5));
                        break;
                    case 4:
                        startActivity(new Intent(viewHolder.itemView.getContext(), CustomConfirmationActivty.class).putExtra("action", Config.Action.ACTION_RESTORE_DEFAULT_CONFIG).putExtra("msg", "Are you sure?"));
                        break;
                    case 3:
                        startActivity(new Intent(viewHolder.itemView.getContext(), CustomConfirmationActivty.class).putExtra("action", Config.Action.ACTION_RESET_STEP_COUNT).putExtra("msg", "Reset Step Count?"));
                        break;
                    default:
//                        startActivityForResult(new Intent(viewHolder.itemView.getContext(), ChoiceActivity.class).putExtra("from", selectedItemPosition), selectedItemPosition);
                        break;
                }

            }

            @Override
            public void onTopEmptyRegionClick() {

            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(resultCode == RESULT_OK){
            int result = data.getIntExtra("choice", 0);
            switch (requestCode){
                case 0:
                    Config.IDLE_TIME = result;
                    Toast.makeText(this, "Successfully changed IDLE TIME to " + result, Toast.LENGTH_SHORT).show();
                    break;
                case 1:
                    Config.NUM_STEPS_TO_TRIGGER_MORAL_LOSS = result;
                    Toast.makeText(this, "Successfully changed NUM STEPS TO TRIGGER MORAL LOSS to " + result, Toast.LENGTH_SHORT).show();
                    break;
                case 2:
                    Config.PROGRESS_BAR_STEPS = result;
                    Toast.makeText(this, "Successfully changed PROGRESS BAR STEPS to " + result, Toast.LENGTH_SHORT).show();
                    break;
                case 3:
                    break;
            }
        }

    }

    private ArrayList<String> configMenu(){
        ArrayList<String> list = new ArrayList<String>();

        list.add("Set Idle Time to " + PreferenceHelper.getInstance(this).getInt(Config.Keys.KEY_IDLE_TIME, Config.IDLE_TIME));
        list.add("Set steps before moral loss to " + PreferenceHelper.getInstance(this).getInt(Config.Keys.KEY_STEP_MORAL_LOSS, Config.NUM_STEPS_TO_TRIGGER_MORAL_LOSS));
        list.add("Set Progress steps to " + PreferenceHelper.getInstance(this).getInt(Config.Keys.KEY_PROGRESS_STEPS, Config.PROGRESS_BAR_STEPS));
        list.add("Reset Step Count");
        list.add("Restore to Default");
        list.add("Check sensors");

        return list;
    }
}
