package com.wearsens.fragments;

import android.app.Fragment;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.example.bluetoothsensor.R;


public class ProfileFragment extends Fragment
{
	public ProfileFragment()
    {
	}

	TextView text, welcome;
	String userName = "Krithika";

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,	Bundle savedInstanceState)
    {
		View rootView = inflater.inflate(R.layout.fragment_profile, container,false);

		text = (TextView) rootView.findViewById(R.id.txtLabel);
		text.setText("Profile");
		Typeface tf = Typeface.createFromAsset(getActivity().getAssets(),"Aaargh.ttf");
		text.setTypeface(tf);
		welcome = (TextView) rootView.findViewById(R.id.welcome);
		welcome.setTypeface(tf);
		Drawable bg = rootView.findViewById(R.id.txtLabel).getBackground();
		bg.setAlpha(127);
		return rootView;
	}

}
