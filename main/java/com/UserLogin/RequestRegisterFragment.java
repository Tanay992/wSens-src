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

/**
 * A simple {@link Fragment} subclass.
 */
public class RequestRegisterFragment extends Fragment implements OnClickListener{

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
        register.setOnClickListener(this);

        //Set output log for error messages
        output_log = (TextView) register_view.findViewById(R.id.output_log);
        return register_view;
    }

    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.register_button:
                String username = ((EditText) register_view.findViewById(R.id.username)).getText().toString();
                String password = ((EditText) register_view.findViewById(R.id.password)).getText().toString();
                ParseUser currentUser = new ParseUser();
                currentUser.setUsername(username);
                currentUser.setPassword(password);
                String o_log = "Error with Registration";

                try {
                    if (username == null || username.equals(""))
                        o_log = "Please Enter a Username";
                    else if (password == null || password.equals(""))
                        o_log = "Please Enter a Password";
                    else
                    {
                        //TODO: Switch to signUpInBackground()
                        currentUser.signUp();

                        //This line won't execute if signUp() throws an exception,
                        //So there is no concern of storing an invalid user object to currentUser
                        Login.currentUser = currentUser;
                        o_log = "Registration Successful";
                        ((Login) getActivity()).loadMainUI();
                    }
                } catch (ParseException e) {
                    Integer error_code = e.getCode();

                    //TODO: Deal with the various errors

                    if (error_code.equals(ParseException.PASSWORD_MISSING))
                        o_log = "Please Enter a Password";
                    else if (error_code.equals(ParseException.USERNAME_MISSING))
                        o_log = "Please Enter a Username";
                    else if (error_code.equals(ParseException.USERNAME_TAKEN))
                        o_log = "That Username is taken, please enter a new username";
                    else if (error_code.equals(ParseException.CONNECTION_FAILED)
                            || error_code.equals(ParseException.TIMEOUT))
                        o_log = "Could not connect to WearSens server, please check your internet connection";
                    else {
                        Log.d("RequestRegisterFragment",
                                "While attempting registration, Parse Error Code:" + Integer.toString(error_code));
                    }

                }
                output_log.setText(o_log);
                break;

            default:
                Log.d("RequestRegisterFragment", "onClick switch statement selected default");
        }
    }


}
