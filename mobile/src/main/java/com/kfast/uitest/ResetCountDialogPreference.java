package com.kfast.uitest;

import android.content.Context;
import android.content.DialogInterface;
import android.content.res.TypedArray;
import android.preference.DialogPreference;
import android.util.AttributeSet;

/**
 * Created by David on 2015-03-07.
 */
public class ResetCountDialogPreference extends DialogPreference {
    public ResetCountDialogPreference(Context context, AttributeSet attrs) {
        super(context, attrs);

        setDialogLayoutResource(R.layout.reset_count_layout);
//        setPositiveButtonText("OK");
//        setNegativeButtonText("Cancel");
//        setTitle("Reset Step Count");
//        setSummary("Reset step count taken of the watch");
//        setKey("reset_step_count");
//        setDefaultValue(false);
//        setDialogIcon(null);

//        setPersistent(false);
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        super.onDialogClosed(positiveResult);

        if(positiveResult){
            persistBoolean(true);
        }else{
            persistBoolean(false);
        }

    }

    @Override
    protected void onSetInitialValue(boolean restorePersistedValue, Object defaultValue) {
        super.onSetInitialValue(restorePersistedValue, defaultValue);

        if(!restorePersistedValue){
            persistBoolean(false);
        }

    }

    @Override
    protected Object onGetDefaultValue(TypedArray a, int index) {
        return a.getBoolean(0, false);
    }
}
