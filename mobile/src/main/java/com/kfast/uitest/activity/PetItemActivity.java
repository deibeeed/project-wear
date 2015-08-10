package com.kfast.uitest.activity;

import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.kfast.uitest.BaseActivity;
import com.kfast.uitest.R;
import com.kfast.uitest.adapter.PetAdapter;
import com.kfast.uitest.model.Pet;
import com.kfast.uitest.model.PetSkill;
import com.kfast.uitest.utils.VolleyHelper;

import org.json.JSONObject;

import java.util.ArrayList;

public class PetItemActivity extends BaseActivity {
    private static final String TAG = "PetItemActivity";

    private Pet pet;

    private TextView tvPetName;
    private TextView tvPetDescription;
    private TextView tvPetSkills;
    private RecyclerView rvPetSkills;

    private LinearLayout contaierStoreButtons;

    private PetAdapter adapter;
    private LinearLayoutManager manager;

    private ArrayList<PetSkill> listSkills;
    private Bundle bundle;
    private boolean isStorePet;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pet_item);
        initializeToolbar();

        bundle = getIntent().getExtras();
        pet = (Pet) bundle.getSerializable("pet");
        isStorePet = bundle.getBoolean("isStoreItem");
        listSkills = new ArrayList<>();
        initTestSkills();

        tvPetName = (TextView) findViewById(R.id.tv_pet_name);
        tvPetDescription = (TextView) findViewById(R.id.tv_description);
        tvPetSkills = (TextView) findViewById(R.id.tv_pet_skills);
        contaierStoreButtons = (LinearLayout) findViewById(R.id.container_store_buttons);

        rvPetSkills = (RecyclerView) findViewById(R.id.rv_pet_skills);
        adapter = new PetAdapter(this, listSkills, false);
        manager = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);

        if(isStorePet){
            setAppBarTitle(getString(R.string.menu_nav_store));
            contaierStoreButtons.setVisibility(View.VISIBLE);
        }

        setupViews();

        rvPetSkills.setLayoutManager(manager);
        rvPetSkills.setAdapter(adapter);

        testConnection();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_pet_item, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        switch (id){
            case android.R.id.home:
                onBackPressed();
                return true;
            case R.id.action_settings:
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void setupViews(){
        tvPetName.setText(pet.getName());
        tvPetDescription.setText(pet.getDescription());
        tvPetSkills.setText(pet.getName() + " skills");
    }

    private void initTestSkills(){
        for(int i = 0; i < 10; i++){
            listSkills.add(new PetSkill(null, "skill " + i));
        }
    }

    private void testConnection(){
        String url = "http://192.168.1.11:8080/api/fitpet/getPets";

        JsonObjectRequest request = new JsonObjectRequest(url, null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                Log.d(TAG, response.toString());
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.wtf(TAG, error.getMessage());
            }
        });

        VolleyHelper.getInstance(this).addToRequestQueue(request);
    }
}
