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

import com.example.bluetoothsensor.R;

public class WhatsYourGoal extends Activity {
	EditText setGoal;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		setContentView(R.layout.your_goal);
		
		TextView text = (TextView) findViewById(R.id.txtLabel);
		Typeface tf = Typeface.createFromAsset(getAssets(), "Aaargh.ttf");
		text.setTypeface(tf);
		Drawable bg = findViewById(R.id.txtLabel).getBackground();
		bg.setAlpha(127);
		TextView howMuch = (TextView) findViewById(R.id.howMuch);
		howMuch.setTypeface(tf);
		
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
	
	OnClickListener loseWtListener = new OnClickListener() {
		
		@Override
		public void onClick(View v) {
			// TODO Auto-generated method stub
			LinearLayout setGoalLayout = (LinearLayout) findViewById(R.id.set_goal);
			setGoalLayout.setVisibility(View.VISIBLE);
			
		}
	};
	
	OnClickListener maintainWtListener = new OnClickListener() {
		
		@Override
		public void onClick(View v) {
			// TODO Auto-generated method stub
			
			
		}
	};
	
	OnClickListener gainWtListener = new OnClickListener() {
		
		@Override
		public void onClick(View v) {
			// TODO Auto-generated method stub
			LinearLayout setGoalLayout = (LinearLayout) findViewById(R.id.set_goal);
			setGoalLayout.setVisibility(View.VISIBLE);
			
		}
	};
	
	OnClickListener letsGoListener = new OnClickListener() {
		
		@Override
		public void onClick(View v) {
			// TODO Auto-generated method stub
			
			try {
				CONSTANTS.goalWeight = Integer.parseInt(setGoal.getText().toString());
			} catch (NumberFormatException e) {
				// TODO set default goal weight to 140lbs
				CONSTANTS.goalWeight = 140;
			}
			
			Intent mainIntent = new Intent(WhatsYourGoal.this,MainUI.class);
            WhatsYourGoal.this.startActivity(mainIntent);
            //WhatsYourGoal.this.finish();			
			
		}
	};


}
