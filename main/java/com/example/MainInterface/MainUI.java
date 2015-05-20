package com.example.MainInterface;

import com.example.bluetoothsensor.R;
import com.wearsens.fragments.MyEatingFragment;
import com.wearsens.fragments.MyHabitsFragment;
import com.wearsens.fragments.ProfileFragment;
import com.wearsens.fragments.SettingsFragment;
import com.wearsens.fragments.StoreDataFragment;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.parse.Parse;

public class MainUI extends Activity {

	// navigation drawer variables
	DrawerLayout drawer;
	ListView drawerList;
	ActionBarDrawerToggle drawerToggle;



    //Change this menu to get more items on the left menu
    //Then update the switch statement in method "selectItem"
    //The XML element that handles this view is the List View "left_drawer"
    //in activity_main.xml
	String[] menuItems = { "My Eating", "My Habits", "My Profile (TODO)",
            "My Settings (TODO)", "Store Data"};

	@Override
	protected void onCreate(Bundle savedInstanceState)
    {
        Log.d("MainUI", "in the oncreate");

		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_ACTION_BAR);
		setContentView(R.layout.activity_main);


        //Create MyEatingFragment as our starting fragment
		Fragment fragment = new MyEatingFragment();
		FragmentManager fragmentManager = getFragmentManager();
		fragmentManager.beginTransaction().replace(R.id.content_frame, fragment).commit();

		drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
		drawerList = (ListView) findViewById(R.id.left_drawer);
		drawer.setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.START);
		drawerList.setAdapter(new ArrayAdapter<String>(this,R.layout.drawer_list_item, menuItems));

		drawerList.setOnItemClickListener(new DrawerItemClickListener());


		drawerToggle = new ActionBarDrawerToggle(this, drawer, R.drawable.ic_drawer, R.string.drawer_open, R.string.drawer_close )
        {
			public void onDrawerClosed(View view)
            {
				invalidateOptionsMenu();
			}

			public void onDrawerOpened(View drawerView)
            {
				invalidateOptionsMenu();
			}
		};

		drawer.setDrawerListener(drawerToggle);

        //PARSE
        Parse.enableLocalDatastore(this);
        Parse.initialize(this, "Dmjvhy9oBSoQogAmMdN7EucZg24reQkWSLBijHJl", "kZxdH4bs6CpLpH7BUSCu7HESweNiZaDivmkjU5TZ");


    }

	private class DrawerItemClickListener implements ListView.OnItemClickListener
    {
		public void onItemClick(AdapterView<?> parent, View view, int position,long id)
        {
			selectItem(position);
		}
	}

    /*
    This defines which screen the user has picked in the sliding panel to the left.
     */
	private void selectItem(int position)
    {
		Fragment fragment = null;
		switch (position)
        {
		case 0:
            Log.d("MainUI", "Creating Eating Fragment");
			fragment = new MyEatingFragment();
			break;
		case 1:
			fragment = new MyHabitsFragment();
			break;
		case 2:
            Log.d("MainUI", "Creating Profile Fragment");
			fragment = new ProfileFragment();
			break;
		case 3:
			fragment = new SettingsFragment();
			break;

        case 4:
            fragment = new StoreDataFragment();
            break;

		default:
			break;
		}

		if (fragment != null)
        {
			FragmentManager fragmentManager = getFragmentManager();
			fragmentManager.beginTransaction().replace(R.id.content_frame, fragment).commit();

			drawerList.setSelection(position);
			drawerList.setItemChecked(position, true);
			setTitle(menuItems[position]);
			drawer.closeDrawer(drawerList);
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu)
    {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.main, menu);
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	protected void onPostCreate(Bundle savedInstanceState)
    {
		super.onPostCreate(savedInstanceState);
		drawerToggle.syncState();
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig)
    {
		super.onConfigurationChanged(newConfig);
		drawerToggle.onConfigurationChanged(newConfig);
	}

	public boolean onPrepareOptionsMenu(Menu menu)
    {
		boolean drawerOpen = drawer.isDrawerOpen(drawerList);
		return super.onPrepareOptionsMenu(menu);
	}

	@Override
	protected void onDestroy()
    {
		super.onDestroy();
	}
};








