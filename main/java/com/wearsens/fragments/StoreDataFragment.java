package com.wearsens.fragments;

import android.app.Activity;
import android.net.Uri;
import android.os.Bundle;
import android.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseException;
import com.parse.GetCallback;
import com.parse.SaveCallback;
import com.example.bluetoothsensor.R;
import com.wearsens.fragments.MyEatingFragment;


public class StoreDataFragment extends Fragment implements OnClickListener{


    Button send_button;
    View view_store_data;
    public StoreDataFragment() {
        // Required empty public constructor
    }

    /*
    @Override
    public void onCreate(Bundle savedInstanceState) {

        Log.d("StoreDataFrag", "Entered onCreate");
    }
*/
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        view_store_data = inflater.inflate(R.layout.fragment_store_data, container, false);
        send_button = (Button) view_store_data.findViewById(R.id.send_data_button);
        send_button.setOnClickListener(this);
        // Inflate the layout for this fragment
        return view_store_data;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.send_data_button:
                EditText editText = (EditText) view_store_data.findViewById(R.id.food_tag);
                String food_tag = editText.getText().toString();
                Log.d("StoreDataFragment", "send_data_button Clicked");

                //upload food tag and data to database;
                if (!food_tag.equals("")) {
                    ParseObject data = new ParseObject("DataRow");
                    data.put("tag", food_tag);
                    data.put("rawData", MyEatingFragment.VibrationDataList);
                    data.saveInBackground();
                    editText.setText("");
                }

                break;

            default:
                break;
        }
    }

}
