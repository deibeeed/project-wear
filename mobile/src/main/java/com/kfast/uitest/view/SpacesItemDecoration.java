package com.kfast.uitest.view;

import android.graphics.Rect;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;

/**
 * Created by David on 2015-03-24.
 * Source: http://stackoverflow.com/questions/28531996/android-recyclerview-gridlayoutmanager-column-spacing
 */
public class SpacesItemDecoration extends RecyclerView.ItemDecoration {
    private int space;

    public SpacesItemDecoration(int space) {
        this.space = space;
    }

    @Override
    public void getItemOffsets(Rect outRect, View view,
                               RecyclerView parent, RecyclerView.State state) {
        outRect.left = space;
        outRect.right = space;
        outRect.bottom = space;

        //detect if layout manager is a GridLayoutManager
        if(parent.getLayoutManager() instanceof GridLayoutManager){
            GridLayoutManager manager = (GridLayoutManager) parent.getLayoutManager();

            int childPosition = parent.getChildAdapterPosition(view);

            //check if spanCount == 2
            if(manager.getSpanCount() == 2){
                if(childPosition == 0 || childPosition == 1)
                    outRect.top = space;

                if(childPosition % 2 == 0){
                    outRect.right = space / 2;
                }else{
                    outRect.left = space / 2;
                }
            }
        }else{
            // Add top margin only for the first item to avoid double space between items
            if(parent.getChildPosition(view) == 0)
                outRect.top = space;
        }
    }
}
