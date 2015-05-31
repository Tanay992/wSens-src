package com.wearsens.fragments;

import android.app.Fragment;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.view.View.OnClickListener;
import android.widget.CompoundButton.OnCheckedChangeListener;
import com.example.bluetoothsensor.R;

/**
 * Created by Mahir on 5/26/15.
 */
public class DeveloperFragment extends Fragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_developer, container, false);

        SharedPreferences pref = this.getActivity().getApplicationContext().getSharedPreferences("MyPref", 0);
        final SharedPreferences.Editor editor = pref.edit();

        final CheckBox checkbox = (CheckBox) rootView.findViewById(R.id.checkBox);
        boolean shouldLog = pref.getBoolean("shouldLog", false);
        checkbox.setChecked(shouldLog);
        checkbox.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (checkbox.isChecked()) {
                    editor.putBoolean("shouldLog", true);

                } else {
                    editor.putBoolean("shouldLog", false);
                }
                editor.commit();
            }
        });

            return rootView;
    }

}

