package com.kfast.uitest.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;

import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.NetworkImageView;

/**
 * Created by David on 2015-01-28.
 */
public class CustomNetworkImageView extends NetworkImageView {

    private Bitmap mLocalBitmap;
    private Drawable mDrawable;

    private boolean mShowLocal;
    private boolean mShowDrawable;
    private boolean isCircularImage;

    /**
     * method to invoke setImageBitmap in NetworkImageView
     * @param bitmap - bitmap image to be placed.
     */
    public void setLocalImageBitmap(Bitmap bitmap) {
        if (bitmap != null) {
            mShowLocal = true;
        }
        this.mLocalBitmap = bitmap;
        requestLayout();
    }

    /**
     * method to invoke setImageDrawable in NetworkImageView
     * @param drawable - drawable image to be placed.
     */
    public void setLocalImageDrawable(Drawable drawable){
        if(drawable != null){
            mShowDrawable = true;
        }
        this.mDrawable = drawable;
        requestLayout();
    }

    /**
     * method to invoke circular image transformation
     * @param isCircularImage - if true, will transform image into circular images,
     *                        else, will retain its original form. No transformation is
     *                        done.
     */
    public void setCircularImage(boolean isCircularImage){
        this.isCircularImage = isCircularImage;
    }

    /**
     * source code taken from: https://gist.github.com/bkurzius/99c945bd1bdcf6af8f99
     *
     * Creates a circular bitmap and uses whichever dimension is smaller to determine the width
     * <br/>Also constrains the circle to the leftmost part of the image
     *
     * @param bitmap
     * @return bitmap
     */
//    public Bitmap getCircularBitmap(Bitmap bitmap) {
//        Bitmap output = Bitmap.createBitmap(bitmap.getWidth(),
//                bitmap.getHeight(), Bitmap.Config.ARGB_8888);
//        Canvas canvas = new Canvas(output);
//        int width = bitmap.getWidth();
//
//        if(bitmap.getWidth()>bitmap.getHeight())
//            width = bitmap.getHeight();
//
//        final int color = 0xff424242;
//        final Paint paint = new Paint();
//        final Rect rect = new Rect(0, 0, width, width);
//        final RectF rectF = new RectF(rect);
//        final float roundPx = width / 2;
//
//        paint.setAntiAlias(true);
//        canvas.drawARGB(0, 0, 0, 0);
//        paint.setColor(color);
//        canvas.drawRoundRect(rectF, roundPx, roundPx, paint);
//
//        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
//        canvas.drawBitmap(bitmap, rect, rect, paint);
//
//        return output;
//    }

    public Bitmap getCircularBitmap(Bitmap bitmap) {
        int size = Math.min(bitmap.getWidth(), bitmap.getHeight());

        Bitmap output = null;
        try{
            output = Bitmap.createBitmap(size,
                    size, Bitmap.Config.ARGB_8888);
        }catch (OutOfMemoryError e){
            e.printStackTrace();

//            output = BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher);
        }

        Canvas canvas = new Canvas(output);

        BitmapShader shader;
        shader = new BitmapShader(bitmap, Shader.TileMode.MIRROR,
                Shader.TileMode.CLAMP);

        Paint paint = new Paint();
        paint.setAntiAlias(true);
        paint.setFilterBitmap(true);
        paint.setDither(true);
        paint.setShader(shader);

        RectF rect = new RectF(0, 0 ,size,size);
        int radius = size/2;
//        canvas.drawRoundRect(rect, radius, radius, paint);
        canvas.drawCircle(rect.centerX(), rect.centerY(), radius, paint);
        return output;
    }

    @Override
    public void setImageBitmap(Bitmap bm) {

        if(isCircularImage){
            if(bm != null){
                super.setImageBitmap(getCircularBitmap(bm));
            }else{
                super.setImageBitmap(bm);
            }
        }else{
            super.setImageBitmap(bm);
        }
    }

    @Override
    public void setImageUrl(String url, ImageLoader imageLoader) {
        mShowLocal = false;
        super.setImageUrl(url, imageLoader);
    }

    public CustomNetworkImageView(Context context) {
        this(context, null);
    }

    public CustomNetworkImageView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CustomNetworkImageView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {

        super.onLayout(changed, left, top, right, bottom);
        if (mShowLocal) {
            setImageBitmap(mLocalBitmap);
        }else if(mShowDrawable){
            setImageDrawable(mDrawable);
        }
    }

}