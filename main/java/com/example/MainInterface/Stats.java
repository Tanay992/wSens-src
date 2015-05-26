package com.example.MainInterface;
import java.util.Calendar;
/**
 * Created by Mahir on 5/24/15.
 */
public class Stats {

    public int m_data;
    public Calendar m_timestamp;

    public Stats(int data, Calendar timestamp)
    {
        m_data = data;
        m_timestamp = timestamp;
    }
}
