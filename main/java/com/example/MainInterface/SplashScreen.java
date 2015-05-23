package com.example.MainInterface;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Window;

import com.UserLogin.Login;
import com.example.bluetoothsensor.R;
import com.parse.Parse;

public class SplashScreen extends Activity{
		@Override
	protected void onCreate(Bundle savedInstanceState) {
		
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		
		getWindow().requestFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.splash_screen);

            //PARSE
            Parse.enableLocalDatastore(this);
            Parse.initialize(this, "Dmjvhy9oBSoQogAmMdN7EucZg24reQkWSLBijHJl", "kZxdH4bs6CpLpH7BUSCu7HESweNiZaDivmkjU5TZ");

		/*
		 * This timer helps us detect when the device has been turned off. Ie.
		 * if the last received data does not change for some time
		 */
		Log.d("SplashScreen", "in the oncreate");
	        new Handler().postDelayed(new Runnable(){
            @Override
            public void run() {



                //Create an intent to launch the Login Activity.
                Intent mainIntent = new Intent(SplashScreen.this, Login.class);
                SplashScreen.this.startActivity(mainIntent);
                SplashScreen.this.finish();
            }
        }, 2000);
		
    }
	
}
