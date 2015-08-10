package com.kfast.uitest.utils;

import android.content.Context;
import android.graphics.Bitmap;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.Volley;

/**
 * Created by David on 2015-03-22.
 */
public class VolleyHelper {
    private static VolleyHelper ourInstance;
    private Context context;
    private RequestQueue requestQueue;
//    private ImageLoader imageLoader;

    public static VolleyHelper getInstance(Context context) {

        if(ourInstance == null){
            ourInstance = new VolleyHelper(context);
        }

        return ourInstance;
    }

    private VolleyHelper(Context context) {
        this.context = context;

        requestQueue = getRequestQueue();

//        imageLoader = new ImageLoader(requestQueue, new DiskLruImageCache(context, "squadzz-app-cache", 1024 * 1024 * 500, Bitmap.CompressFormat.PNG, 70));
    }

    public RequestQueue getRequestQueue(){
        if(requestQueue == null){
            requestQueue = Volley.newRequestQueue(context.getApplicationContext());
        }

        return requestQueue;
    }

    public void addToRequestQueue(Request request){
        request.setRetryPolicy(new DefaultRetryPolicy(10 * 60 * 1000, DefaultRetryPolicy.DEFAULT_MAX_RETRIES, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        getRequestQueue().add(request);
    }

//    public ImageLoader getImageLoader(){
//        return imageLoader;
//    }
}
