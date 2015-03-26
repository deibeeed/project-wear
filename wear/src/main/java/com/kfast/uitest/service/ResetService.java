package com.kfast.uitest.service;

import android.app.IntentService;
import android.content.Intent;
import android.content.Context;
import android.util.Log;

import com.kfast.uitest.util.Config;
import com.kfast.uitest.util.PreferenceHelper;

/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 * <p/>
 * TODO: Customize class - update intent actions, extra parameters and static
 * helper methods.
 */
public class ResetService extends IntentService {

    public ResetService() {
        super("ResetService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            final String action = intent.getAction();
            PreferenceHelper.getInstance(this).setInt(Config.Keys.KEY_STEP_COUNT, 0);
            Log.d("alarm_service", "service done");
        }
    }
}
