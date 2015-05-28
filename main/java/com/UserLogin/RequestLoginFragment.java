package com.UserLogin;


import android.os.Bundle;
import android.app.Fragment;
import android.app.FragmentManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;

import com.parse.LogInCallback;
import com.parse.ParseUser;
import com.parse.ParseException;
import com.example.bluetoothsensor.R;
import android.util.Log;
import android.widget.TextView;

public class RequestLoginFragment extends Fragment {

    View login_view;
    TextView output_log;

    public RequestLoginFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        login_view = inflater.inflate(R.layout.fragment_request_login, container, false);

        Log.d ("RequestLoginFragment", "in onCreateView");

        //Listen if someone clicks the register button
        Button register = (Button) login_view.findViewById(R.id.register_button);
        register.setOnClickListener(new LoginOnClickListener());

        //Listen if someone clicks the login button
        Button login = (Button) login_view.findViewById(R.id.login_button);
        login.setOnClickListener(new LoginOnClickListener());

        //Set output log for error messages
        output_log = (TextView) login_view.findViewById(R.id.output_log);

        //TODO: Maybe add support for email IDs

        return login_view;
    }

    public class LoginOnClickListener implements OnClickListener {
        @Override
        public void onClick(final View v) {
            switch (v.getId()) {
                case R.id.login_button:
                    String username = ((EditText) login_view.findViewById(R.id.username)).getText().toString();
                    String password = ((EditText) login_view.findViewById(R.id.password)).getText().toString();
                    String o_log = "";

                    if (username == null || username.equals("")) {
                        o_log = "Please Enter a Username";
                        output_log.setText(o_log);
                    } else if (password == null || password.equals("")) {
                        o_log = "Please Enter a Password";
                        output_log.setText(o_log);
                    } else {
                        //Disable the Login button and attempt to login
                        v.setEnabled(false);
                        ParseUser.logInInBackground(username, password, new LogInCallback() {
                            @Override
                            public void done(ParseUser user, ParseException e) {
                                v.setEnabled(true);
                                String out_log = "";

                                if (e == null) {
                                    out_log = "Login Successful";
                                    ((Login) getActivity()).loadMainUI();
                                } else {
                                    Integer error_code = e.getCode();

                                    if (error_code.equals(ParseException.OBJECT_NOT_FOUND))
                                        out_log = "Invalid Username or Password";
                                    else if (error_code.equals(ParseException.CONNECTION_FAILED)
                                            || error_code.equals(ParseException.TIMEOUT))
                                        out_log = "Could not connect to WearSens server, please check your internet connection";
                                    else {
                                        Log.d("RequestLoginFragment",
                                                "While attempting login, Parse Error Code: " + Integer.toString(error_code));
                                    }

                                    output_log.setText(out_log);
                                }
                            }
                        });

                    }
                    break;

                case R.id.register_button:
                    ((Login) getActivity()).requestRegister();
                    break;

                default:
                    Log.d("RequestLoginFragment", "onClick switch statement selected default");

            }
        }

    }



}
