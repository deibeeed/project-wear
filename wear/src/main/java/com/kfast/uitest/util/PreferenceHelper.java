package com.kfast.uitest.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Parcelable;

/**
 * Created by David on 2015-02-13.
 */
public class PreferenceHelper {
    private static PreferenceHelper ourInstance;
    private Context context;
    private SharedPreferences preferences;
    private SharedPreferences.Editor editor;

    private PreferenceHelper(Context context) {
        this.context = context;
        this.preferences = context.getSharedPreferences("uitest", Context.MODE_PRIVATE);
        this.editor = preferences.edit();
    }

    public static PreferenceHelper getInstance(Context context) {
        if(ourInstance == null)
            ourInstance = new PreferenceHelper(context);

        return ourInstance;
    }

    public void setString(String key, String value){
        editor.putString(key, value);
        editor.commit();
    }

    public String getString(String key, String defValue){
        return preferences.getString(key, defValue);
    }

    public void setInt(String key, int value){
        editor.putInt(key, value);
        editor.commit();
    }

    public int getInt(String key, int defValue){
        return preferences.getInt(key, defValue);
    }

    public void setBoolean(String key, boolean value){
        editor.putBoolean(key, value);
        editor.commit();
    }

    public boolean getBoolean(String key, boolean defValue){
        return preferences.getBoolean(key, defValue);
    }
}
