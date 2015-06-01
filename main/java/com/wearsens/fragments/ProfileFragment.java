package com.wearsens.fragments;

import android.app.Fragment;
import android.content.Intent;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.Mail.SendEmail;
import com.UserLogin.Login;
import com.example.MainInterface.SplashScreen;
import com.parse.ParseUser;
import com.example.bluetoothsensor.R;
import android.widget.Button;


public class ProfileFragment extends Fragment implements View.OnClickListener
{
    Button logout_button;
    ParseUser currentUser;
    Button sendLog;

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
		Drawable bg = rootView.findViewById(R.id.txtLabel).getBackground();
		bg.setAlpha(127);

        logout_button = (Button) rootView.findViewById(R.id.logoutButton);
        logout_button.setOnClickListener(this);

        sendLog = (Button) rootView.findViewById(R.id.sendLogButton);
        sendLog.setOnClickListener(this);

        currentUser = ParseUser.getCurrentUser();

        if (currentUser == null)
            ;
            //TODO: Decide whether the app should be runnable without a user logged in or not


        else
        {
            logout_button.setText(currentUser.getUsername() + ": Logout");

        }
		return rootView;
	}

    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.logoutButton:
                Intent mainIntent = new Intent(getActivity(), Login.class);
                mainIntent.putExtra(Login.LOGOUT_REQUEST, true);
                mainIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(mainIntent);
                break;

            case R.id.sendLogButton:
                Intent intent = new Intent(getActivity(), SendEmail.class);
                startActivity(intent);

            default:
                ;
        }
    }
}
