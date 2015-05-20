package com.example.MainInterface;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.example.bluetoothsensor.R;
import com.wearsens.fragments.*;


public class HomeScreen extends Activity
{

	EditText setGoal;
	ToggleButton male, female;
	EditText myWeight;
	Button existingUser;

	@Override
	protected void onCreate(Bundle savedInstanceState)
    {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.home_screen);

		TextView text = (TextView) findViewById(R.id.txtLabel);
		Typeface tf = Typeface.createFromAsset(getAssets(), "Aaargh.ttf");
		text.setTypeface(tf);
		Drawable bg = findViewById(R.id.txtLabel).getBackground();
		bg.setAlpha(127);

		male = (ToggleButton) findViewById(R.id.male);
		male.toggle();
		female = (ToggleButton) findViewById(R.id.female);
		myWeight = (EditText) findViewById(R.id.wt);

		female.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				male.toggle();

			}
		});

		male.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				female.toggle();

			}
		});

		
		Button loseWeight = (Button) findViewById(R.id.loseWt);
		Button maintainWeight = (Button) findViewById(R.id.maintainWt);
		Button gainWeight = (Button) findViewById(R.id.gainWt);
		Button letsGo = (Button) findViewById(R.id.letsGo);
		setGoal = (EditText) findViewById(R.id.goalWeight);
		
		loseWeight.setOnClickListener(loseWtListener);
		gainWeight.setOnClickListener(gainWtListener);
		maintainWeight.setOnClickListener(maintainWtListener);
		letsGo.setOnClickListener(letsGoListener);
	}
	
OnClickListener loseWtListener = new OnClickListener()
{
		public void onClick(View v)
        {
			LinearLayout setGoalLayout = (LinearLayout) findViewById(R.id.set_goal);
			setGoalLayout.setVisibility(View.VISIBLE);
			setGoalLayout.setFocusable(true);
		}
};
	
OnClickListener maintainWtListener = new OnClickListener()
{
	public void onClick(View v)
    {
		// TODO Auto-generated method stub
		LinearLayout setGoalLayout = (LinearLayout) findViewById(R.id.set_goal);
		setGoalLayout.setVisibility(View.VISIBLE);
			
	}
};
	
OnClickListener gainWtListener = new OnClickListener()
{
		@Override
		public void onClick(View v)
        {
	        LinearLayout setGoalLayout = (LinearLayout) findViewById(R.id.set_goal);
	        setGoalLayout.setVisibility(View.VISIBLE);
        }
};
	
OnClickListener letsGoListener = new OnClickListener()
{
		
	@Override
	public void onClick(View v)
    {
        try
        {
            CONSTANTS.goalWeight = Integer.parseInt(setGoal.getText().toString());
        }
        catch (NumberFormatException e)
        {
            CONSTANTS.goalWeight = 140;
        }

        Intent mainIntent = new Intent(HomeScreen.this,MainUI.class);
        HomeScreen.this.startActivity(mainIntent);
	}



};


}
