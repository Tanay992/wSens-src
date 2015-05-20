package com.example.MainInterface;

import android.app.Activity;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.widget.TextView;

import com.example.bluetoothsensor.R;

public class SelectExistingUser extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		setContentView(R.layout.select_existing_user);
		
		TextView text = (TextView) findViewById(R.id.txtLabel);
		Typeface tf = Typeface.createFromAsset(getAssets(), "Aaargh.ttf");
		text.setTypeface(tf);
		Drawable bg = findViewById(R.id.txtLabel).getBackground();
		bg.setAlpha(127);
	}
	
	

}
