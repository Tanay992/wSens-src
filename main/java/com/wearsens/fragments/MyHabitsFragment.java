package com.wearsens.fragments;

import com.example.MainInterface.MyHabitsItems;
import android.app.Fragment;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;

import com.example.MainInterface.CustomTextView;
import com.example.MainInterface.HabitItemAdapter;
import com.example.bluetoothsensor.R;

public class MyHabitsFragment extends Fragment
{
    ListView list;

    public MyHabitsFragment()
    {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        View rootView = inflater.inflate(R.layout.fragment_myhabits, container, false);
        TextView label = (TextView) rootView.findViewById(R.id.intro);
        Typeface tf = Typeface.createFromAsset(getActivity().getAssets(), "Aaargh.ttf");
        label.setTypeface(tf);
        Drawable bg = rootView.findViewById(R.id.intro).getBackground();
        bg.setAlpha(127);

        list = (ListView) rootView.findViewById(R.id.listView1);
        MyHabitsItems items[] = {
                new MyHabitsItems(
                        R.drawable.green_flag,
                        "You ate breakfast today",
                        "Great job! Keep it up. Breakfast is your never-miss meal. Breakfast gives you energy, it tops up your energy stores for the day and helps to regulate blood sugar."),
                new MyHabitsItems(
                        R.drawable.green_flag,
                        "You ate at good speed",
                        "Eating slowly is a habit that you seem to have acquired, keep it up! Eating slowly is not only a good trick for weight loss, but it's also a way to savor your food, rather than just scarf it down. It's a good practice in mindfulness and can ease digestion."),
                new MyHabitsItems(
                        R.drawable.green_flag,
                        "You are at 30% of your goal",
                        "Live longer and healthier. You lost 3 pounds and you have 7 more pounds to go. Don't stop now!"),
                new MyHabitsItems(
                        R.drawable.red_flag,
                        "You skipped meals",
                        "You skipped 3 lunches and 1 breakfast in the past week. Meal skipping is often the root of fatigue and afternoon slump, but could also harm you in the long run. Every night or morning, pack enough healthy snacks, smalls meals, and drinks to last all day."),
                new MyHabitsItems(
                        R.drawable.red_flag,
                        "You may be dehydrated",
                        "You were dehydrated for more than 8 hours in the past week. Remember every cell, tissue and organ in your body needs water to function correctly. Your body uses water to maintain its temperature, remove waste and lubricate joints. Water is essential for good health."),
                new MyHabitsItems(
                        R.drawable.red_flag,
                        "You may be eating too much",
                        "You had 2 large dinners this past week. Remember having a large dinner late at night slows your metabolism and makes your body hang onto as much energy (calories, fat, etc) as possible. At the very least, have 3 meals a day. Add in some exercise and you are off to a good start.") };

        HabitItemAdapter adapter = new HabitItemAdapter(getActivity(), R.layout.my_habits_item, items);
        list.setAdapter(adapter);

        list.setOnItemClickListener(new AdapterView.OnItemClickListener()
        {
            @Override
            public void onItemClick(AdapterView<?> adapter, View v, int position, long id)
            {
                CustomTextView description = (CustomTextView) v.findViewById(R.id.desc);

                if (description.getVisibility() == View.GONE)
                {
                    description.setVisibility(View.VISIBLE);
                }
                else if (description.getVisibility() == View.VISIBLE)
                {
                    description.setVisibility(View.GONE);
                }

            }
        });
        return rootView;
    }

}