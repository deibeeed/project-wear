package com.kfast.uitest;

import android.app.Activity;
import android.os.Bundle;
import android.support.wearable.view.WatchViewStub;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.kfast.uitest.util.Config;
import com.kfast.uitest.util.PreferenceHelper;

public class CustomConfirmationActivty extends Activity {

    private TextView tvMessage;
    private ImageView ivYes;
    private ImageView ivNo;
    private Bundle bun;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_custom_confirmation_activty);

        bun = getIntent().getExtras();

        tvMessage = (TextView) findViewById(R.id.tvMessage);
        ivYes = (ImageView) findViewById(R.id.ivYes);
        ivNo = (ImageView) findViewById(R.id.ivNo);

        tvMessage.setText(bun.getString("msg"));

        ivYes.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if(bun.getString("action").equalsIgnoreCase(Config.Action.ACTION_RESTORE_DEFAULT_CONFIG)){
                    Config.IDLE_TIME = 15;
                    Config.NUM_STEPS_TO_TRIGGER_MORAL_LOSS = 30;
                    Config.PROGRESS_BAR_STEPS = 200;

                    Toast.makeText(v.getContext(), "Restore defaults successful", Toast.LENGTH_SHORT).show();
                }else{
                    PreferenceHelper.getInstance(v.getContext()).setInt("stepCount", 0);
                    Toast.makeText(v.getContext(), "Reset successful", Toast.LENGTH_SHORT).show();
                }

                onBackPressed();
            }
        });

        ivNo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });
    }
}
