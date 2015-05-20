package com.wearsens.fragments;



import android.app.Fragment;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.MainInterface.BarGraph;
import com.example.MainInterface.CustomTextView;
import com.example.MainInterface.HabitItemAdapter;
import com.BluetoothSetup.HexAsciiHelper;
import com.example.MainInterface.LineGraph;
import com.example.MainInterface.MyHabitsItems;
import com.example.bluetoothsensor.R;
import com.BluetoothSetup.RFduinoService;
import com.example.MainInterface.SensorData;
import com.example.MainInterface.CONSTANTS;

import org.achartengine.GraphicalView;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import com.parse.Parse;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseException;
import com.parse.GetCallback;
import com.parse.SaveCallback;
import android.content.SharedPreferences.Editor;



public class MyEatingFragment extends Fragment implements BluetoothAdapter.LeScanCallback {
    // constructor
    public MyEatingFragment() {
    }

    //We reference VibrationDataList directly in StoreDataFragment
    //So if we change the name here we need to update in StoreDataFragment also
    public static ArrayList<SensorData> VibrationDataList = new ArrayList<SensorData>();
    public static ArrayList<Double> VibrationDeviationList = new ArrayList<Double>();

    String ReceiveBuffer = "";

    int ReceivedData = 0;
    int LastData = 0;

    final private static int STATE_BLUETOOTH_OFF = 1;
    final private static int STATE_DISCONNECTED = 2;
    final private static int STATE_CONNECTING = 3;
    final private static int STATE_CONNECTED = 4;

    private int state;

    private BluetoothAdapter bluetoothAdapter;
    private BluetoothDevice bluetoothDevice;
    private RFduinoService rfduinoService;

    boolean Connected = false;
    public static boolean RegisterSwallows = false;

    FrameLayout layout1;
    LinearLayout layout2, foodLayout, beverageLayout;
    TextView foodcount, foodcounttxt;

    GraphicalView view, goalView;
    // Today graph
    double[] todayRange = { 0, 60, 0, 30 };
    LineGraph today = new LineGraph("#00CC00", "#3366FF", "Food\t","Beverage\t", todayRange);

    static int food = 0;
    static int beverage = 0;

    // goal graph
    double[] goalRange = { 0, 12, 130, 160 };
    LineGraph goalGraph = new LineGraph("#CC00FF", "#FF0000", "Your weight\t",
            "Goal weight\t", goalRange);

    // this week graph
    String[] days = { "M", "T", " W", " T", " F ", "S", "S" };
    BarGraph week = new BarGraph(7, days);

    // this month graph
    String[] weeks = { "W1", "W2", "W3", "W4" };
    BarGraph month = new BarGraph(4, weeks);

    static int current = 0;
    TextView day;
    TextView goal;

    // my habits list variables
    ListView list;
    HabitItemAdapter adapter;

    // spinner variables
    ImageView image;
    AnimationDrawable loadingViewAnim;
    boolean countIncreased;

    long counter = 0;

    String SWALLOW_ID = "SwallowCount";
    String FOOD_ID = "FoodCount";
    String BEVERAGE_ID = "BeverageCount";


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        View rootView = inflater.inflate(R.layout.fragment_myeating, container,false);

        TextView text = (TextView) rootView.findViewById(R.id.txtLabel);
        day = (TextView) rootView.findViewById(R.id.day);
        Typeface tf = Typeface.createFromAsset(getActivity().getAssets(),
                "Aaargh.ttf");
        text.setTypeface(tf);
        day.setTypeface(tf);

        Drawable bg = rootView.findViewById(R.id.txtLabel).getBackground();
        bg.setAlpha(127);

        // food counts
        foodLayout = (LinearLayout) rootView.findViewById(R.id.foodLayout);
        foodcount = (TextView) rootView.findViewById(R.id.food);
        foodcounttxt = (TextView) rootView.findViewById(R.id.foodTxt);

        // next and previous buttons
        ImageButton prev = (ImageButton) rootView.findViewById(R.id.prev);
        ImageButton next = (ImageButton) rootView.findViewById(R.id.next);

        prev.setOnClickListener(prevListener);
        next.setOnClickListener(nextListener);

        // all the my habits list stuff
        list = (ListView) rootView.findViewById(R.id.habitslist);
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

        adapter = new HabitItemAdapter(getActivity(), R.layout.my_habits_item,
                items);
        list.setAdapter(adapter);
        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapter, View v,
                                    int position, long id) {
                // TODO Auto-generated method stub
                CustomTextView description = (CustomTextView) v
                        .findViewById(R.id.desc);

                if (description.getVisibility() == View.GONE) {
                    description.setVisibility(View.VISIBLE);
                } else if (description.getVisibility() == View.VISIBLE) {
                    description.setVisibility(View.GONE);
                }
            }
        });

        // the spinner animation stuff
        image = (ImageView) rootView.findViewById(R.id.spinner1);
        image.setBackgroundResource(R.anim.spinner_animation);
        loadingViewAnim = (AnimationDrawable) image.getBackground();
        countIncreased = false;

        // swallow detection variables
        RegisterSwallows = true;
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        bluetoothConnect();

        // the daily progress graph
        layout1 = (FrameLayout) rootView.findViewById(R.id.layout1);

        loadDataFromDatabase();
        return rootView;
    }

    @Override
    public void onResume(){
        super.onResume();
        loadDataFromDatabase();
        createTodayGraph(food, beverage);

    }

    View.OnClickListener prevListener = new View.OnClickListener()
    {
        @Override
        public void onClick(View v) {
            // TODO Auto-generated method stub
            if (state == STATE_CONNECTED)
            {
                getActivity().unbindService(rfduinoServiceConnection);
                rfduinoService.disconnect();
            }

            if (current == 0)
            {
                current = 2;
                showMyHabits();
            }
            else if (current == 1)
            {
                current = 0;
                bluetoothConnect();
                createTodayGraph(food, beverage);
            }
            else if (current == 2)
            {
                current = 1;
                createWeekGraph();
            }
        }
    };

    View.OnClickListener nextListener = new View.OnClickListener()
    {
        @Override
        public void onClick(View v) {
            // TODO click the next button
            if (state == STATE_CONNECTED) {
                getActivity().unbindService(rfduinoServiceConnection);
                rfduinoService.disconnect();
            }
            if (current == 0) {
                current = 1;
                createWeekGraph();
            } else if (current == 1) {
                current = 2;
                showMyHabits();
            } else if (current == 2) {
                current = 0;
                bluetoothConnect();
                createTodayGraph(food, beverage);
            }

        }
    };

    public void createTodayGraph(int food, int beverage) {
        day.setText("TODAY");
        if(food<10)
            foodcount.setText(" "+Integer.toString(food));
        else
            foodcount.setText(Integer.toString(food));
        layout1.removeAllViews();
        layout1.addView(image);
        layout1.addView(foodLayout);


        if(loadingViewAnim.isRunning()){
            image.setBackground(null);
            image.setBackgroundResource(R.anim.spinner_animation);
            loadingViewAnim = (AnimationDrawable) image.getBackground();
        }
        if(!loadingViewAnim.isRunning() && countIncreased){
            loadingViewAnim.start();
            countIncreased = false;
        }
    }

    public void createWeekGraph()
    {
        day.setText("THIS WEEK");
        view = null;
        layout1.removeAllViews();
        view = week.getView(getActivity());
        layout1.removeAllViews();
        layout1.addView(view, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        int[] food = { 4, 6, 3, 7, 2, 0, 0 };
        int[] beverage = { 2, 5, 5, 10, 6, 0, 0 };
        week.UpdateActivityCount(food, beverage);
        view.repaint();

    }

    public void showMyHabits() {
        day.setText("MY HABITS");
        view = null;
        layout1.removeAllViews();
        layout1.addView(list);

    }

    public void createGoalGraph()
    {
        goalView = goalGraph.getView(getActivity());
        layout2.addView(goalView, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.FILL_PARENT,
                ViewGroup.LayoutParams.FILL_PARENT));

        // update current weight here
        ArrayList<Integer> val = new ArrayList<Integer>();
        for (int month = 0; month < 5; month++) {
            val.add((CONSTANTS.myWeight - month));
        }

        int[] values2 = toInt(val);
        goalGraph.addGoalPoints(values2, CONSTANTS.goalWeight);
        goalView.repaint();

    }

    int[] toInt(ArrayList<Integer> arr)
    {
        int[] iarr = new int[arr.size()];
        for (int i = 0; i < arr.size(); i++) {
            iarr[i] = arr.get(i);
        }
        return iarr;
    }

    long count = 0;



    /*
    In this function, we processed received sensor data from Bluetooth.
    We extract the data based on the formatting convention and add to the relevant data structures.
     */
    public void ProcessReceivedData(String data)
    {
        if (Connected == false)
        {
            Connected = true; // we received the data, its a "heartbeat"
            ReceiveBuffer = "";
            VibrationDataList.clear();
            VibrationDeviationList.clear();
        }

        ReceivedData++;

        ReceiveBuffer = ReceiveBuffer + data;

        int begin = ReceiveBuffer.indexOf("B");
        int end = ReceiveBuffer.indexOf("E");
        if (end > begin)
        {
            String newString = ReceiveBuffer.substring(begin, end + 1);
            ReceiveBuffer = ReceiveBuffer.replace(newString, "");
            newString = newString.replace(" ", "");
            newString = newString.replace("B", "");
            newString = newString.replace("E", "");

            if (newString.contains(":"))
            {
                String[] data_split = newString.split(":");
                if (data_split.length == 2)
                {
                    SensorData NewData = new SensorData(data_split[0], data_split[1]);

                    VibrationDataList.add(NewData);

                    if (VibrationDataList.size() > 50)
                    {
                        VibrationDataList.remove(0);
                    }

                    DetectSwallows();

                }
            }
        }
    }

    /*
     * Some helper functions for Bluetooth
     */
    @Override
    public void onStart()
    {
        super.onStart();

        getActivity().registerReceiver(bluetoothStateReceiver, new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED));
        getActivity().registerReceiver(rfduinoReceiver, RFduinoService.getIntentFilter());
        updateState(bluetoothAdapter.isEnabled() ? STATE_DISCONNECTED : STATE_BLUETOOTH_OFF);
    }

    @Override
    public void onPause()
    {
        super.onPause();
        getActivity().unregisterReceiver(bluetoothStateReceiver);
        getActivity().unregisterReceiver(rfduinoReceiver);

    }

    private void upgradeState(int newState)
    {
        if (newState > state) {
            updateState(newState);
        }
    }

    private void downgradeState(int newState)
    {
        if (newState < state) {
            updateState(newState);
        }
    }

    private void updateState(int newState)
    {
        state = newState;
    }

    private void addData(byte[] data) {
        String ascii = HexAsciiHelper.bytesToAsciiMaybe(data);
        if (ascii != null) {
            ProcessReceivedData(ascii);
        }
    }

    void bluetoothConnect()
    {
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (Connected == false)
        {
            onStart();
            bluetoothAdapter.startLeScan(new UUID[] { RFduinoService.UUID_SERVICE }, MyEatingFragment.this);
        }
    }

    @Override
    public void onLeScan(BluetoothDevice device, final int rssi, final byte[] scanRecord)
    {
        bluetoothAdapter.stopLeScan(this);
        bluetoothDevice = device;

        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {

                /*
                We only connect to a Bluetooth Device whose name follows a particular naming convention.
                 */
                if (bluetoothDevice.getName().contains(CONSTANTS.CONNECTION_STRING))
                {
					/*
					 * We have scanned and found the RFDuino
					 */

                    Intent rfduinoIntent = new Intent(getActivity(),RFduinoService.class);

                    getActivity().bindService(rfduinoIntent, rfduinoServiceConnection, 1);

                }
            }
        });
    }

    private final BroadcastReceiver bluetoothStateReceiver = new BroadcastReceiver()
    {
        public void onReceive(Context context, Intent intent)
        {
            int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, 0);

            if (state == BluetoothAdapter.STATE_ON)
            {
                upgradeState(STATE_DISCONNECTED);
            }
            else if (state == BluetoothAdapter.STATE_OFF)
            {
                downgradeState(STATE_BLUETOOTH_OFF);
            }
        }
    };

    private ServiceConnection rfduinoServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            rfduinoService = ((RFduinoService.LocalBinder) service).getService();
            if (rfduinoService.initialize()) {
                boolean result = rfduinoService.connect(bluetoothDevice
                        .getAddress());

                if (result == true) {
                    upgradeState(STATE_CONNECTING);
                }
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.d("BT", "in service disconnected");
            rfduinoService = null;
            downgradeState(STATE_DISCONNECTED);
        }
    };
    //Mark: Important
    private final BroadcastReceiver rfduinoReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();

            if (RFduinoService.ACTION_CONNECTED.equals(action)) {
                Toast.makeText(getActivity(), "Connected to sensor",
                        Toast.LENGTH_LONG).show();
                Log.d("BT", "in myeating, connected");
                upgradeState(STATE_CONNECTED);
            } else if (RFduinoService.ACTION_DISCONNECTED.equals(action)) {
                downgradeState(STATE_DISCONNECTED);
            } else if (RFduinoService.ACTION_DATA_AVAILABLE.equals(action)) {
                //Mark:  only called when screen is open

                food = intent.getIntExtra(FOOD_ID, food);
                beverage = intent.getIntExtra(BEVERAGE_ID, beverage);
                SWALLOW_COUNT = intent.getIntExtra(SWALLOW_ID, SWALLOW_COUNT);
                Log.d("shouldn't happen", "called");
                createTodayGraph(food, beverage);
                //addData(intent.getByteArrayExtra(RFduinoService.EXTRA_DATA));
            }
        }
    };

    final static int WINDOW_SIZE = 10;
    final static int DISABLE_LENGTH = CONSTANTS.DISABLE_LENGTH;
    static int DISABLE_COUNTER = 0;
    static int SWALLOW_COUNT = 0;
    static int ConsecutiveZero = 0;
    static double LastReading = 0;
    static int called = 0;
    static double mean, stddev, stddev_last;

    public void DetectSwallows()
    {

        called++;
        List<SensorData> VibrationDataList = MyEatingFragment.VibrationDataList;
        List<Double> VibrationDeviationList = MyEatingFragment.VibrationDeviationList;
        mean = 0;
        stddev = 0;
		/*
		 * Not enough data has accumulated to detect anything
		 */
        if (VibrationDataList.size() < WINDOW_SIZE)
            return;
		/*
		 * Here we calculate the average value in a given window
		 */
        int sum = 0;
        int VibrationDataSize = VibrationDataList.size();

        for (int i = VibrationDataSize - WINDOW_SIZE; i < VibrationDataSize; i++) {
            try {
                sum = sum + Integer.parseInt(VibrationDataList.get(i).iValue);
            } catch (java.lang.NumberFormatException exc) {
                continue;
            }
        }

        mean = sum / WINDOW_SIZE;
        mean = mean / 100;
        double sum_variance = 0;
		/*
		 * Here, we calculate the standard deviation of each point within a
		 * detection window. This is an important feature used to detect
		 * swallows.
		 */
        for (int i = VibrationDataSize - WINDOW_SIZE; i < VibrationDataSize; i++) {
            SensorData current = VibrationDataList.get(i);
            try {
                int val = Integer.parseInt(current.iValue);
                double valD = (double) val;
                valD = valD / 100;

                double diff = Math.abs(mean - valD);
                sum_variance = sum_variance + diff;

            } catch (java.lang.NumberFormatException exc) {
                continue;
            }
        }

        stddev = Math.floor(100 * Math.sqrt(sum_variance)) / 100;

		/*
		 * We can prevent the array from getting too big this way.... We only
		 * need the last 50 samples anyway
		 */
        if (VibrationDeviationList.size() > 50) {
            VibrationDeviationList.remove(0);
        }

        VibrationDeviationList.add(Double.valueOf(stddev));

        DISABLE_COUNTER++;
        boolean swallowDetected = false;


        if (stddev > CONSTANTS.threshold)
        {
            if (DISABLE_COUNTER >= DISABLE_LENGTH)
            {
				/*
				 * Swallow detected!
				 */
                swallowDetected = true;
                DISABLE_COUNTER = 0;
                if (MyEatingFragment.RegisterSwallows == true)
                {
                    SWALLOW_COUNT++;
                    // beverage = addElement(beverage, sw_count);
                    food++;

                    syncDataWithDatabase();
                    countIncreased = true;
                    createTodayGraph(food, beverage);
                    Log.d("BT", "increasing swallow count");

                }
            }
        }

        if (swallowDetected == false && called % 40 == 0) {

            // beverage = addElement(beverage, SWALLOW_COUNT);
            beverage = addElement(beverage, 1);
            //createTodayGraph(food, beverage);

        }

    }

    // int[] addElement(int[] a, int e) {
    int addElement(int a, int e) {
        // a = Arrays.copyOf(a, a.length + 1);
        // a[a.length - 1] = e;
        return a + e;
    }

    //NEW FUNCTIONS


    void syncDataWithDatabase() {

        SharedPreferences pref = this.getActivity().getApplicationContext().getSharedPreferences("MyPref", 0);
        final Editor editor = pref.edit();
        String id = pref.getString("parse_object_id", "empty");
        if (id == "empty")
        {
            final ParseObject data = new ParseObject("UserData");
            data.put(SWALLOW_ID, SWALLOW_COUNT);
            data.put(FOOD_ID, food);
            data.put(BEVERAGE_ID, beverage);

            data.saveInBackground(new SaveCallback() {
                public void done(ParseException e) {
                    if (e == null) {
                          String id2 = data.getObjectId();
                        editor.putString("parse_object_id", id2);
                        editor.commit();
                    }
                }
            });


        }
        else
        {
            ParseQuery<ParseObject> query = ParseQuery.getQuery("UserData");

            query.getInBackground(id, new GetCallback<ParseObject>() {
                public void done(ParseObject data, ParseException e) {
                    if (e == null) {
                        data.put(SWALLOW_ID, SWALLOW_COUNT);
                        data.put(FOOD_ID, food);
                        data.put(BEVERAGE_ID, beverage);
                        data.saveInBackground();
                    }
                }
            });
        }
    }

    void loadDataFromDatabase() {

        SharedPreferences pref = this.getActivity().getApplicationContext().getSharedPreferences("MyPref", 0);
        String id = pref.getString("parse_object_id", "empty");

        if (id == "empty") {
            SWALLOW_COUNT = 0;
            food = 0;
            beverage = 0;
            createTodayGraph(food, beverage);
        }
        else {
            ParseQuery<ParseObject> query = ParseQuery.getQuery("UserData");
            query.getInBackground(id, new GetCallback<ParseObject>() {
                public void done(ParseObject onlineData, ParseException e) {
                    if (e == null) {
                        SWALLOW_COUNT = onlineData.getInt(SWALLOW_ID);
                        food = onlineData.getInt(FOOD_ID);
                        beverage = onlineData.getInt(BEVERAGE_ID);
                        //Date dataDate = onlineData.getDate("Date");
                        /*
                        if (!sameAsCurrentDay(dataDate))
                        {
                            SWALLOW_COUNT = food = beverage = 0;
                        }
                        */
                        createTodayGraph(food, beverage);
                    }
                }
            });
        }

    }

    void writeToFile() {

        //TO DO: Implement
    }

    Boolean sameAsCurrentDay(Date d) {

        Calendar calendar = Calendar.getInstance();
        int curDay = calendar.get(Calendar.DAY_OF_WEEK);

        Calendar c = Calendar.getInstance();
        c.setTime(d);
        int dataDay = c.get(Calendar.DAY_OF_WEEK);

        return (curDay == dataDay);
    }
}