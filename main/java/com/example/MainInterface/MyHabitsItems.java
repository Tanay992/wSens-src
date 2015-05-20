package com.example.MainInterface;

import android.widget.LinearLayout;

public class MyHabitsItems {
	public int icon;
	public String habit;
	public LinearLayout layout;
	public String desc;
	
	public MyHabitsItems(int icon, String habit){
		super();
		this.icon = icon;
		this.habit = habit;
		
	}
    public MyHabitsItems(int icon, String habit,String desc){
		super();
		this.icon = icon;
		this.habit = habit;
		this.desc = desc;
		
	}

    public MyHabitsItems(int icon, String habit, LinearLayout layout, String desc){
		super();
		this.icon = icon;
		this.habit = habit;
		this.layout = layout;
		this.desc = desc;
		
	}
}
