package com.kfast.uitest;

import android.app.Activity;
import android.os.Bundle;
import android.support.wearable.view.WatchViewStub;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.kfast.uitest.util.Config;

public class CustomConfirmationActivty extends Activity {

    private TextView tvMessage;
    private ImageView ivYes;
    private ImageView ivNo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_custom_confirmation_activty);

        tvMessage = (TextView) findViewById(R.id.tvMessage);
        ivYes = (ImageView) findViewById(R.id.ivYes);
        ivNo = (ImageView) findViewById(R.id.ivNo);

        tvMessage.setText(getIntent().getExtras().getString("msg"));

        ivYes.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Config.IDLE_TIME = 15;
                Config.NUM_STEPS_TO_TRIGGER_MORAL_LOSS = 30;
                Config.PROGRESS_BAR_STEPS = 200;

                Toast.makeText(v.getContext(), "Restore defaults successful", Toast.LENGTH_SHORT).show();

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
