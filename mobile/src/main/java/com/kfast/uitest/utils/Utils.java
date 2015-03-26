package com.kfast.uitest.utils;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

/**
 * Created by David on 2015-03-14.
 */
public class Utils {

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
