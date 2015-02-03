package com.kfast.uitest;

import android.content.Context;
import android.support.wearable.view.WearableListView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;

/**
 * Created by David on 2015-01-30.
 */
public class MyWearListAdapter extends WearableListView.Adapter {

    ArrayList<String> item;
    Context context;

    public MyWearListAdapter(Context context, ArrayList<String> item){
        this.context = context;
        this.item = item;
    }

    public static class MyWearListViewHolder extends WearableListView.ViewHolder{
        private TextView tvTitle;
        private ImageView ivIcon;
        private View itemView;

        public MyWearListViewHolder(View itemView) {
            super(itemView);

            this.itemView = itemView;
            tvTitle = (TextView) itemView.findViewById(R.id.tvTitle);
            ivIcon = (ImageView) itemView.findViewById(R.id.ivIcon);
        }
    }

    @Override
    public WearableListView.ViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {

        return new MyWearListViewHolder(LayoutInflater.from(context).inflate(R.layout.wearable_list_item, null));
    }

    @Override
    public void onBindViewHolder(WearableListView.ViewHolder viewHolder, int i) {

        MyWearListViewHolder holder = (MyWearListViewHolder) viewHolder;

        holder.tvTitle.setText(item.get(i));

        holder.itemView.setTag(i);
    }

    @Override
    public int getItemCount() {
        return item.size();
    }
}
