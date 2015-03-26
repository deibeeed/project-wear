package com.kfast.uitest.dialogprefs;

import android.content.Context;
import android.content.res.TypedArray;
import android.preference.DialogPreference;
import android.util.AttributeSet;

import com.kfast.uitest.R;

/**
 * Created by David on 2015-03-07.
 */
public class RestoreDefaultDialogPreference extends DialogPreference {
    public RestoreDefaultDialogPreference(Context context, AttributeSet attrs) {
        super(context, attrs);

        setDialogLayoutResource(R.layout.restore_default_preference_layout);
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        super.onDialogClosed(positiveResult);

        if(positiveResult)
            persistBoolean(true);
        else
            persistBoolean(false);
    }

    @Override
    protected void onSetInitialValue(boolean restorePersistedValue, Object defaultValue) {
        super.onSetInitialValue(restorePersistedValue, defaultValue);

        if(!restorePersistedValue)
            persistBoolean(false);
    }

    @Override
    protected Object onGetDefaultValue(TypedArray a, int index) {
        return a.getBoolean(0, false);
    }
}
