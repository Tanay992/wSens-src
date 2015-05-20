package com.example.MainInterface;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Window;

import com.example.bluetoothsensor.R;

public class SplashScreen extends Activity{
		@Override
	protected void onCreate(Bundle savedInstanceState) {
		
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		
		getWindow().requestFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.splash_screen);
		
		/*
		 * This timer helps us detect when the device has been turned off. Ie.
		 * if the last received data does not change for some time
		 */
		Log.d("BT", "in the oncreate");
	        new Handler().postDelayed(new Runnable(){
            @Override
            public void run() {
                
                Intent mainIntent = new Intent(SplashScreen.this,MainUI.class);
                SplashScreen.this.startActivity(mainIntent);
                SplashScreen.this.finish();
            }
        }, 2000);
		
    }
	
}
