package com.UserLogin;


import android.os.Bundle;
import android.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.view.View.OnClickListener;
import android.widget.EditText;
import android.widget.TextView;

import com.parse.Parse;
import com.parse.ParseUser;
import com.parse.ParseException;

import com.example.bluetoothsensor.R;
import com.parse.ParseUser;
import com.parse.SignUpCallback;

/**
 * A simple {@link Fragment} subclass.
 */
public class RequestRegisterFragment extends Fragment{

    View register_view;
    TextView output_log;


    public RequestRegisterFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        register_view =  inflater.inflate(R.layout.fragment_request_register, container, false);

        //Listen if someone clicks the register button
        Button register = (Button) register_view.findViewById(R.id.register_button);
        register.setOnClickListener(new RegisterOnClickListener());

        //Set output log for error messages
        output_log = (TextView) register_view.findViewById(R.id.output_log);
        return register_view;
    }

    public class RegisterOnClickListener implements OnClickListener {

        public void onClick(final View v) {
            switch (v.getId()) {
                case R.id.register_button:
                    String username = ((EditText) register_view.findViewById(R.id.username)).getText().toString();
                    String password = ((EditText) register_view.findViewById(R.id.password)).getText().toString();
                    ParseUser currentUser = new ParseUser();
                    currentUser.setUsername(username);
                    currentUser.setPassword(password);
                    String o_log = "";

                    if (username == null || username.equals("")) {
                        o_log = "Please Enter a Username";
                        output_log.setText(o_log);
                    } else if (password == null || password.equals("")) {
                        o_log = "Please Enter a Password";
                        output_log.setText(o_log);
                    } else {
                        //Disable the Register button and attempt to register
                        v.setEnabled(false);
                        currentUser.signUpInBackground(new SignUpCallback() {
                            @Override
                            public void done(ParseException e) {
                                v.setEnabled(true);
                                String out_log = "";
                                if (e == null) {
                                    out_log = "Registration Successful";
                                    ((Login) getActivity()).loadMainUI();
                                } else {
                                    Integer error_code = e.getCode();

                                    if (error_code.equals(ParseException.USERNAME_TAKEN))
                                        out_log = "That Username is taken, please enter a new username";
                                    else if (error_code.equals(ParseException.CONNECTION_FAILED)
                                            || error_code.equals(ParseException.TIMEOUT))
                                        out_log = "Could not connect to WearSens server, please check your internet connection";
                                    else {
                                        Log.d("RequestRegisterFragment",
                                                "While attempting registration, Parse Error Code:" + Integer.toString(error_code));
                                    }
                                }
                                output_log.setText(out_log);
                            }
                        });
                    }
                    break;

                default:
                    Log.d("RequestRegisterFragment", "onClick switch statement selected default");
            }
        }
    }


}
