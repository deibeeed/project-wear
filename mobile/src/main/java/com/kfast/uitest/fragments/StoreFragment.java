package com.kfast.uitest.fragments;


import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.kfast.uitest.R;
import com.kfast.uitest.adapter.PetAdapter;
import com.kfast.uitest.model.Pet;
import com.kfast.uitest.utils.Utils;
import com.kfast.uitest.view.SpacesItemDecoration;

import java.util.ArrayList;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link StoreFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class StoreFragment extends Fragment {
    private static final String TAG = "StoreFragment";

    private View container;

    private RecyclerView rvStorePets;
    private GridLayoutManager manager;
    private PetAdapter adapter;

    private ArrayList<Pet> listPets;

    public static StoreFragment newInstance() {
        StoreFragment fragment = new StoreFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    public StoreFragment() {
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
        for(int i = 0; i < 100; i++){
            listPets.add(new Pet("Lorem Ispum " + i, 0.0, null, testDesc, false, null, null, null));
        }
        // Inflate the layout for this fragment
        container = inflater.inflate(R.layout.fragment_store, vgContainer, false);

        rvStorePets = (RecyclerView) container.findViewById(R.id.rv_store_pets);
        manager = new GridLayoutManager(getActivity(), 3);
        adapter = new PetAdapter(getActivity(), listPets, true);

        rvStorePets.setAdapter(adapter);
        rvStorePets.setLayoutManager(manager);
        rvStorePets.addItemDecoration(new SpacesItemDecoration(Utils.sizeInDp(getActivity(), 5)));
        return container;
    }


}
