package com.kfast.uitest.receiver;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.SystemClock;
import android.util.Log;

import com.kfast.uitest.service.ResetService;
import com.kfast.uitest.util.Config;

import java.util.Calendar;

public class FitPetBroadcastReceiver extends BroadcastReceiver {

    public FitPetBroadcastReceiver() {

    }

    @Override
    public void onReceive(Context context, Intent intent) {
        // TODO: This method is called when the BroadcastReceiver is receiving
        // an Intent broadcast.
//        throw new UnsupportedOperationException("Not yet implemented");

        if(intent.getAction() != null && intent.getAction().equals("android.intent.action.BOOT_COMPLETED")){
            setAlarmEvent(context);
        }else if(intent.hasExtra("startedFrom")){
            setAlarmEvent(context);
            Log.d("alarm_receiver", "started from activity.....");
        }
    }

    private void setAlarmEvent(Context context){
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(System.currentTimeMillis());
        calendar.set(Calendar.HOUR_OF_DAY, 0);

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, ResetService.class);
        intent.setAction(Config.Action.ACTION_SERVICE_ALARM);
//        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, intent, 0);
        PendingIntent pendingIntent = PendingIntent.getService(context, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT);

        alarmManager.setInexactRepeating(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), AlarmManager.INTERVAL_DAY, pendingIntent);
    }
}
