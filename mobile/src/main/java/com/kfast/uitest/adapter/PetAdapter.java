package com.kfast.uitest.adapter;

import android.content.Context;
import android.content.Intent;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.kfast.uitest.R;
import com.kfast.uitest.activity.PetItemActivity;
import com.kfast.uitest.model.Pet;
import com.kfast.uitest.model.PetSkill;

import java.util.ArrayList;

/**
 * Created by David on 8/8/2015.
 */
public class PetAdapter extends RecyclerView.Adapter<PetAdapter.PetViewHolder>{
    private Context context;
    private ArrayList list;
    private boolean isStoreList;

    public PetAdapter(Context context, ArrayList list, boolean isStoreList) {
        this.context = context;
        this.list = list;
        this.isStoreList = isStoreList;
    }

    @Override
    public PetViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new PetViewHolder(LayoutInflater.from(context).inflate(R.layout.layout_item_pet_card, null));
    }

    @Override
    public void onBindViewHolder(PetViewHolder holder, final int position) {
        if(list.get(position) instanceof Pet){
            holder.ivQuickMenu.setVisibility(View.GONE);
            holder.tvTest.setText("");

            if(isStoreList) {
                holder.ivQuickMenu.setVisibility(View.VISIBLE);
            }

            holder.tvTest.setText(((Pet) list.get(position)).getName());

            holder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    context.startActivity(new Intent(v.getContext(), PetItemActivity.class).putExtra("pet", (Pet) list.get(position)).putExtra("isStoreItem", isStoreList));
                }
            });
        }else if(list.get(position) instanceof PetSkill){
            holder.tvTest.setText(((PetSkill) list.get(position)).getSkillName());
        }
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    public class PetViewHolder extends RecyclerView.ViewHolder{

        public ImageView ivPet;
        public TextView tvTest;
        public ImageView ivQuickMenu;

        public PetViewHolder(View itemView) {
            super(itemView);

            ivPet = (ImageView) itemView.findViewById(R.id.iv_pet);
            tvTest = (TextView) itemView.findViewById(R.id.tv_test);
            ivQuickMenu = (ImageView) itemView.findViewById(R.id.iv_quick_menu);
        }
    }
}
