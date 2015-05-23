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
import com.parse.ParseUser;
import com.parse.ParseException;
import com.example.bluetoothsensor.R;
import android.util.Log;
import android.widget.TextView;

/**
 * A simple {@link Fragment} subclass.
 */

public class RequestLoginFragment extends Fragment implements OnClickListener {

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
        register.setOnClickListener(this);

        //Listen if someone clicks the login button
        Button login = (Button) login_view.findViewById(R.id.login_button);
        login.setOnClickListener(this);

        //Set output log for error messages
        output_log = (TextView) login_view.findViewById(R.id.output_log);

        //TODO: Maybe add support for email IDs

        return login_view;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.login_button:
                String username = ((EditText) login_view.findViewById(R.id.username)).getText().toString();
                String password = ((EditText) login_view.findViewById(R.id.password)).getText().toString();
                ParseUser currentUser = new ParseUser();
                currentUser.setUsername(username);
                currentUser.setPassword(password);
                String o_log = "Error with Login";

                try {
                    if (username == null || username.equals(""))
                        o_log = "Please Enter a Username";
                    else if (password == null || password.equals(""))
                        o_log = "Please Enter a Password";
                    else
                    {
                        //TODO: Switch .signUp() to .signUpInBackground and use callbacks
                        //Currently our application hangs while waiting for the signUp to return;
                        ParseUser.logIn(username, password);
                        Login.currentUser = currentUser;
                        o_log = "Login Successful";
                        ((Login) getActivity()).loadMainUI();
                    }

                } catch (ParseException e) {
                    Integer error_code = e.getCode();

                    if (error_code.equals(ParseException.PASSWORD_MISSING))
                        o_log = "Please Enter a Password";
                    else if (error_code.equals(ParseException.USERNAME_MISSING))
                        o_log = "Please Enter a Username";
                    else if(error_code.equals(ParseException.OBJECT_NOT_FOUND))
                        o_log = "Invalid Username or Password";
                    else if (error_code.equals(ParseException.CONNECTION_FAILED)
                            || error_code.equals(ParseException.TIMEOUT))
                        o_log = "Could not connect to WearSens server, please check your internet connection";
                    else {
                        Log.d("RequestLoginFragment",
                                "While attempting login, Parse Error Code: " + Integer.toString(error_code));
                    }

                }
                output_log.setText(o_log);
                break;

            case R.id.register_button:
                //TODO: Launch a new fragment
                ((Login) getActivity()).requestRegister();
                break;

            default:
                Log.d("RequestLoginFragment", "onClick switch statement selected default");

        }
    }



}
