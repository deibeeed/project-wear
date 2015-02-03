package com.kfast.uitest.view;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.support.wearable.view.WearableListView;
import android.util.AttributeSet;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.kfast.uitest.R;

/**
 * Created by David on 2015-01-30.
 */
public class WearableListItemLayout extends LinearLayout implements WearableListView.OnCenterProximityListener {

    private ImageView ivIcon;
    private TextView tvTitle;

    private final float mFadedTextAlpha;
    private final int mFadedCircleColor;
    private final int mChosenCircleColor;

    public WearableListItemLayout(Context context) {
        this(context, null);
    }

    public WearableListItemLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public WearableListItemLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        mFadedTextAlpha = getResources().getInteger(R.integer.action_text_faded_alpha) / 100f;
        mFadedCircleColor = getResources().getColor(R.color.grey);
        mChosenCircleColor = getResources().getColor(R.color.blue);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        ivIcon = (ImageView) findViewById(R.id.ivIcon);
        tvTitle = (TextView) findViewById(R.id.tvTitle);
    }

    @Override
    public void onCenterPosition(boolean b) {
        tvTitle.setAlpha(1f);
//        ((GradientDrawable)ivIcon.getDrawable()).setColor(mChosenCircleColor);
//        ivIcon.setBackgroundColor(mChosenCircleColor);
        ivIcon.setBackgroundResource(R.drawable.wl_circle);
    }

    @Override
    public void onNonCenterPosition(boolean b) {
//        ((GradientDrawable)ivIcon.getDrawable()).setColor(mFadedCircleColor);
        ivIcon.setBackgroundColor(mFadedCircleColor);
        tvTitle.setAlpha(mFadedTextAlpha);
    }
}
