package com.kfast.uitest.service;

import android.app.IntentService;
import android.app.NotificationManager;
import android.content.Intent;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.kfast.uitest.R;
import com.kfast.uitest.receiver.NotificationBroadcastReceiver;

/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 * <p>
 * TODO: Customize class - update intent actions, extra parameters and static
 * helper methods.
 */
public class NotificationIntentService extends IntentService {
    private static final String TAG = NotificationIntentService.class.getName();
    private static final int NOTIFICATION_ID = 1;


    public NotificationIntentService() {
        super("NotificationIntentService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            processNotification(intent.getExtras());

            NotificationBroadcastReceiver.completeWakefulIntent(intent);
        }
    }


    private void processNotification(Bundle bundle) {
        String msg = "";

        final NotificationManager mNotificationManager = (NotificationManager)
                this.getSystemService(Context.NOTIFICATION_SERVICE);

//        PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
//                new Intent(this, NotificationActivity.class), 0);


        //TODO: check for priority in notification
        final NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(this)
                        .setSmallIcon(R.drawable.ic_launcher)
                        .setContentTitle("Downloading Pets...")
                        .setStyle(new NotificationCompat.BigTextStyle()
                                .bigText(msg))
                        .setContentText(msg);


        mBuilder.setAutoCancel(true);
//        mBuilder.setContentIntent(contentIntent);
        //TODO: set NOTIFICATION_ID constant. this is for testing purposes
//        mNotificationManager.notify(1, mBuilder.build());

        new Thread(
                new Runnable() {
                    @Override
                    public void run() {
                        int incr;
                        // Do the "lengthy" operation 20 times
                        for (incr = 0; incr <= 100; incr+=5) {
                            // Sets the progress indicator to a max value, the
                            // current completion percentage, and "determinate"
                            // state
                            mBuilder.setProgress(100, incr, false);
                            // Displays the progress bar for the first time.
                            mNotificationManager.notify(NOTIFICATION_ID, mBuilder.build());
                            // Sleeps the thread, simulating an operation
                            // that takes time
                            try {
                                // Sleep for 5 seconds
                                Thread.sleep(5*1000);
                            } catch (InterruptedException e) {
                                Log.d(TAG, "sleep failure");
                            }
                        }
                        // When the loop is finished, updates the notification
                        mBuilder.setContentText("Download complete")
                                // Removes the progress bar
                                .setProgress(0,0,false);
                        mNotificationManager.notify(NOTIFICATION_ID, mBuilder.build());
                    }
                }
// Starts the thread by calling the run() method in its Runnable
        ).start();
    }
}
