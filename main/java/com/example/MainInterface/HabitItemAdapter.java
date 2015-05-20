package com.example.MainInterface;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.example.bluetoothsensor.R;

public class HabitItemAdapter extends ArrayAdapter<MyHabitsItems> {

	Context context;
	int layoutResourceId;
	MyHabitsItems data[];

	public HabitItemAdapter(Context context, int layoutResourceId,
			MyHabitsItems[] data) {
		super(context, layoutResourceId, data);
		this.layoutResourceId = layoutResourceId;
		this.context = context;
		this.data = data;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {

		View row = convertView;
		ItemHolder holder = null;

		if (row == null) {
			LayoutInflater inflater = ((Activity) context).getLayoutInflater();
			row = inflater.inflate(layoutResourceId, parent, false);

			holder = new ItemHolder();
			holder.imgIcon = (ImageView) row.findViewById(R.id.imgIcon);
			holder.habit = (CustomTextView) row.findViewById(R.id.txtTitle);
			holder.description = (CustomTextView) row.findViewById(R.id.desc);
			

			row.setTag(holder);
		} else {
			holder = (ItemHolder) row.getTag();
		}

		MyHabitsItems habit_obj = data[position];
		holder.habit.setText(habit_obj.habit);
		holder.imgIcon.setImageResource(habit_obj.icon);
		holder.description.setText(habit_obj.desc);
		return row;
	}

	static class ItemHolder {
		ImageView imgIcon;
		CustomTextView habit;
		CustomTextView description;
		LinearLayout layout;

	}

}
