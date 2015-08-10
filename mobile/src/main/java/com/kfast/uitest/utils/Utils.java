package com.kfast.uitest.utils;

import android.app.Activity;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.DisplayMetrics;

/**
 * Created by David on 2015-03-14.
 */
public class Utils {
    public static int sizeInDp(Context activity, int size){
        float scale = activity.getResources().getDisplayMetrics().density;

        return (int)(size * scale + 0.5f);
    }

    public static double screenSize(Activity activity){
        DisplayMetrics dm = new DisplayMetrics();
        activity.getWindowManager().getDefaultDisplay().getMetrics(dm);
        int width=dm.widthPixels;
        int height=dm.heightPixels;
        int dens=dm.densityDpi;
        double wi=(double)width/(double)dens;
        double hi=(double)height/(double)dens;
        double x = Math.pow(wi,2);
        double y = Math.pow(hi, 2);
        double screenInches = Math.sqrt(x + y);

        return screenInches;
    }

    public static class Network{
        public static boolean hasNetworkConnection(Context context){
            boolean hasConnection = false;

            ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo activeNetwork = cm.getActiveNetworkInfo();

            hasConnection = activeNetwork != null &&  activeNetwork.isConnectedOrConnecting();

            return hasConnection;
        }
    }
}
