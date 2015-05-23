package com.UserLogin;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.util.Log;

import com.example.MainInterface.MainUI;
import com.parse.ParseUser;

import com.example.bluetoothsensor.R;

public class Login extends Activity {

    static ParseUser currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        currentUser = ParseUser.getCurrentUser();
        if (currentUser != null) {
            Log.d("Login", "currentUser exists");
            loadMainUI();
        }
        else
            requestLogin();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_login, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void loadMainUI() {
        Log.d("Login", "Loading MainUI");
        Intent mainIntent = new Intent(this, MainUI.class);
        startActivity(mainIntent);
        finish();
    }

    public void requestLogin() {
        Log.d("Login", "requesting User Login");
        Fragment fragment = new RequestLoginFragment();
        FragmentManager fragmentManager = getFragmentManager();
        fragmentManager.beginTransaction().replace(R.id.fragment_frame, fragment).commit();
    }

    public void requestRegister() {
        Fragment fragment = new RequestRegisterFragment();
        FragmentManager manager = getFragmentManager();
        manager.beginTransaction().replace(R.id.fragment_frame, fragment).commit();
    }
}
