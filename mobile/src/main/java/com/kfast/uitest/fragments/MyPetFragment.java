package com.kfast.uitest.fragments;


import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.CardView;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.kfast.uitest.R;
import com.kfast.uitest.activity.PetItemActivity;
import com.kfast.uitest.adapter.PetAdapter;
import com.kfast.uitest.model.Pet;

import java.util.ArrayList;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link MyPetFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class MyPetFragment extends Fragment {
    private static final String TAG = "MyPetFragment";

    private ArrayList<Pet> listPets;

    private View container;

    private PetAdapter adapter;
    private LinearLayoutManager manager;
    private RecyclerView rvPets;

    private CardView cvActivePet;

    public static MyPetFragment newInstance() {
        MyPetFragment fragment = new MyPetFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    public MyPetFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup vgContainer,
                             Bundle savedInstanceState) {
        //data
        listPets = new ArrayList<>();
        //test data
        String testDesc = "Lorem Ispum Lorem IspumLorem IspumLorem IspumLorem IspumLorem IspumLorem IspumLorem IspumLorem IspumLorem IspumLorem IspumLorem IspumLorem IspumLorem IspumLorem Ispum" +
                "Lorem IspumLorem IspumLorem IspumLorem IspumLorem IspumLorem IspumLorem IspumLorem IspumLorem IspumLorem IspumLorem IspumLorem IspumLorem IspumLorem IspumLorem IspumLorem Ispum" +
                "Lorem IspumLorem IspumLorem IspumLorem IspumLorem IspumLorem IspumLorem IspumLorem IspumLorem IspumLorem IspumLorem IspumLorem IspumLorem IspumLorem IspumLorem IspumLorem Ispum" +
                "Lorem IspumLorem IspumLorem IspumLorem IspumLorem IspumLorem IspumLorem IspumLorem IspumLorem IspumLorem IspumLorem IspumLorem IspumLorem IspumLorem IspumLorem IspumLorem Ispum";
        for(int i = 0; i < 10; i++){
            listPets.add(new Pet("Lorem Ispum " + i, 0.0, null, testDesc, false, null, null, null));
        }

        // Inflate the layout for this fragment
        container = inflater.inflate(R.layout.fragment_my_pet, vgContainer, false);

        rvPets = (RecyclerView) container.findViewById(R.id.rv_available_pets);
        cvActivePet = (CardView) container.findViewById(R.id.cv_active_pet);
        adapter = new PetAdapter(getActivity(), listPets,false);
        manager = new LinearLayoutManager(getActivity(), LinearLayoutManager.HORIZONTAL, false);

        rvPets.setLayoutManager(manager);
        rvPets.setAdapter(adapter);

        cvActivePet.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(v.getContext(), PetItemActivity.class).putExtra("pet", new Pet("The Active Pet", 0.0, null, "this is the active pet and active pet is this", true, null, null, null)));
            }
        });

        return container;
    }
}
