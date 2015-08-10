package com.kfast.uitest;

import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.support.design.widget.NavigationView;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.view.Menu;
import android.view.MenuItem;

import com.kfast.uitest.activity.SettingsActivity;
import com.kfast.uitest.fragments.AnimPlayFragment;
import com.kfast.uitest.fragments.MainFragment;
import com.kfast.uitest.fragments.MyPetFragment;
import com.kfast.uitest.fragments.StoreFragment;


public class MainActivity extends BaseActivity {

    /**
     * Fragment managing the behaviors, interactions and presentation of the navigation drawer.
     */

    /**
     * Used to store the last screen title. For use in {@link #restoreActionBar()}.
     */
    private CharSequence mTitle;

    private NavigationView navigationView;
    private DrawerLayout layoutDrawer;

    FragmentManager fragmentManager;
    FragmentTransaction transaction;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initializeToolbar();

        navigationView = (NavigationView) findViewById(R.id.navigation_view);
        layoutDrawer = (DrawerLayout) findViewById(R.id.drawer_layout);

        fragmentManager = getSupportFragmentManager();

        fragmentManager.beginTransaction().replace(R.id.container, MainFragment.newInstance(), "MainFragment").commit();

        navigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(MenuItem menuItem) {

                transaction = fragmentManager.beginTransaction();

                menuItem.setChecked(true);

                switch (menuItem.getItemId()){
                    case R.id.menu_home:
                        setAppBarTitle(getString(R.string.menu_nav_home));
                        transaction.replace(R.id.container, MainFragment.newInstance(), "MainFragment");
                        break;
                    case R.id.menu_my_pets:
                        setAppBarTitle(getString(R.string.menu_nav_my_pets));
                        transaction.replace(R.id.container, MyPetFragment.newInstance(), "MyPetsFragment");
                        break;
                    case R.id.menu_store:
                        setAppBarTitle(getString(R.string.menu_nav_store));
                        transaction.replace(R.id.container, StoreFragment.newInstance(), "StoreFragment");
                        break;
                    case R.id.menu_animation:
                        setAppBarTitle(getString(R.string.menu_nav_animation));
                        transaction.replace(R.id.container, AnimPlayFragment.newInstance(), "AnimationPlay");
                        break;
                    case R.id.menu_settings:
                        startActivity(new Intent(MainActivity.this, SettingsActivity.class));
                        break;
                }

                transaction.commit();
                layoutDrawer.closeDrawers();

                return true;
            }
        });

    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        switch (id){
            case android.R.id.home:
                layoutDrawer.openDrawer(GravityCompat.START);
                return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
