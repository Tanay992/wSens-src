

package com.BluetoothSetup;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Date;
import java.util.UUID;

import android.Manifest;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import com.example.MainInterface.CONSTANTS;
import com.example.MainInterface.SensorData;
import com.parse.GetCallback;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseException;
import com.parse.SaveCallback;
import com.parse.FindCallback;
import android.net.ConnectivityManager;
import android.util.Pair;

import org.json.JSONArray;
import com.example.MainInterface.Stats;

/*
 * Adapted from:
 * http://developer.android.com/samples/BluetoothLeGatt/src/com.example.android.bluetoothlegatt/BluetoothLeService.html
 */
public class RFduinoService extends Service {
    private final static String TAG = RFduinoService.class.getSimpleName();

    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;
    private String mBluetoothDeviceAddress;
    private BluetoothGatt mBluetoothGatt;
    private BluetoothGattService mBluetoothGattService;

    public final static String ACTION_CONNECTED =
            "com.rfduino.ACTION_CONNECTED";
    public final static String ACTION_DISCONNECTED =
            "com.rfduino.ACTION_DISCONNECTED";
    public final static String ACTION_DATA_AVAILABLE =
            "com.rfduino.ACTION_DATA_AVAILABLE";
    public final static String EXTRA_DATA =
            "com.rfduino.EXTRA_DATA";

    public final static UUID UUID_SERVICE = BluetoothHelper.sixteenBitUuid(0x2220);
    public final static UUID UUID_RECEIVE = BluetoothHelper.sixteenBitUuid(0x2221);
    public final static UUID UUID_SEND = BluetoothHelper.sixteenBitUuid(0x2222);
    public final static UUID UUID_DISCONNECT = BluetoothHelper.sixteenBitUuid(0x2223);
    public final static UUID UUID_CLIENT_CONFIGURATION = BluetoothHelper.sixteenBitUuid(0x2902);

    //ADDED
    public static ArrayList<SensorData> VibrationDataList = new ArrayList<SensorData>();
    public static ArrayList<Double> VibrationDeviationList = new ArrayList<Double>();

    String ReceiveBuffer = "";
    int ReceivedData = 0;

    final static int WINDOW_SIZE = 10;
    final static int DISABLE_LENGTH = CONSTANTS.DISABLE_LENGTH;
    static int DISABLE_COUNTER = 0;
    static int ConsecutiveZero = 0;
    static double LastReading = 0;
    static int called = 0;
    static double mean, stddev, stddev_last;

    static int TODAY_SWALLOW_COUNT = 0;
    static int TODAY_FOOD_COUNT = 0;
    static int TODAY_BEVERAGE_COUNT = 0;
    static boolean needsMergeWithDB;

    String SWALLOW_ID = "SwallowCount";
    String FOOD_ID = "FoodCount";
    String BEVERAGE_ID = "BeverageCount";

    String FOOD_LIST_ID = "FoodHistory";
    String BEVERAGE_LIST_ID = "BeverageHistory";
    String SWALLOW_LIST_ID = "SwallowHistory";

    //stores counts for past 30 days
    //each object is a map from the date as a string to the count
    static List<Map<String,Integer>>dailyFoodCounts;
    static List<Map<String,Integer>>dailySwallowCounts;
    static List<Map<String,Integer>>dailyBeverageCounts;

    // Implements callback methods for GATT events that the app cares about.  For example,
    // connection change and services discovered.
    public final BluetoothGattCallback mGattCallback = new BluetoothGattCallback()
    {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState)
        {
            if (newState == BluetoothProfile.STATE_CONNECTED)
            {
                Log.i(TAG, "Connected to RFduino.");
                Log.i(TAG, "Attempting to start service discovery:" +
                        mBluetoothGatt.discoverServices());
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED)
            {
                Log.i(TAG, "Disconnected from RFduino.");
                broadcastUpdate(ACTION_DISCONNECTED);
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status)
        {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                mBluetoothGattService = gatt.getService(UUID_SERVICE);
                if (mBluetoothGattService == null) {
                    Log.e(TAG, "RFduino GATT service not found!");
                    return;
                }

                BluetoothGattCharacteristic receiveCharacteristic =
                        mBluetoothGattService.getCharacteristic(UUID_RECEIVE);
                if (receiveCharacteristic != null) {
                    BluetoothGattDescriptor receiveConfigDescriptor =
                            receiveCharacteristic.getDescriptor(UUID_CLIENT_CONFIGURATION);
                    if (receiveConfigDescriptor != null) {
                        gatt.setCharacteristicNotification(receiveCharacteristic, true);

                        receiveConfigDescriptor.setValue(
                                BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                        gatt.writeDescriptor(receiveConfigDescriptor);
                    } else {
                        Log.e(TAG, "RFduino receive config descriptor not found!");
                    }

                } else {
                    Log.e(TAG, "RFduino receive characteristic not found!");
                }

                broadcastUpdate(ACTION_CONNECTED);
            } else {
                Log.w(TAG, "onServicesDiscovered received: " + status);
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt,
                                         BluetoothGattCharacteristic characteristic,
                                         int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt,
                                            BluetoothGattCharacteristic characteristic) {
            broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
        }
    };

    private void broadcastUpdate(final String action)
    {
        final Intent intent = new Intent(action);
        sendBroadcast(intent, Manifest.permission.BLUETOOTH);
    }

    private void broadcastUpdate(final String action, final BluetoothGattCharacteristic characteristic)
    {
        if (UUID_RECEIVE.equals(characteristic.getUuid()))
        {
            //intent.putExtra(EXTRA_DATA, characteristic.getValue());
            //mark: sends data
            String ascii = HexAsciiHelper.bytesToAsciiMaybe(characteristic.getValue());
            if (ascii != null) {
                boolean shouldBroadcast = ProcessReceivedData(ascii);
                if (shouldBroadcast)
                {
                    sendDataToEatingFrag(action);
                }
            }

        }
    }

    public class LocalBinder extends Binder
    {
        public RFduinoService getService()
        {
            return RFduinoService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        // After using a given device, you should make sure that BluetoothGatt.close() is called
        // such that resources are cleaned up properly.  In this particular example, close() is
        // invoked when the UI is disconnected from the Service.
        close();
        return super.onUnbind(intent);
    }

    private final IBinder mBinder = new LocalBinder();

    /**
     * Initializes a reference to the local Bluetooth adapter.
     *
     * @return Return true if the initialization is successful.
     */
    public boolean initialize() {

        loadDataFromDatabase();
        // For API level 18 and above, get a reference to BluetoothAdapter through
        // BluetoothManager.
        if (mBluetoothManager == null)
        {
            mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            if (mBluetoothManager == null)
            {
                Log.e(TAG, "Unable to initialize BluetoothManager.");
                return false;
            }
        }

        mBluetoothAdapter = mBluetoothManager.getAdapter();
        if (mBluetoothAdapter == null)
        {
            Log.e(TAG, "Unable to obtain a BluetoothAdapter.");
            return false;
        }

        return true;
    }

    /**
     * Connects to the GATT server hosted on the Bluetooth LE device.
     *
     * @param address The device address of the destination device.
     *
     * @return Return true if the connection is initiated successfully. The connection result
     *         is reported asynchronously through the
     *         {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
     *         callback.
     */
    public boolean connect(final String address)
    {
        if (mBluetoothAdapter == null || address == null)
        {
            Log.w(TAG, "BluetoothAdapter not initialized or unspecified address.");
            return false;
        }

        // Previously connected device.  Try to reconnect.
        if (mBluetoothDeviceAddress != null && address.equals(mBluetoothDeviceAddress)
                && mBluetoothGatt != null)
        {
            Log.d(TAG, "Trying to use an existing mBluetoothGatt for connection.");
            return mBluetoothGatt.connect();
        }

        final BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        // We want to directly connect to the device, so we are setting the autoConnect
        // parameter to false.
        mBluetoothGatt = device.connectGatt(this, false, mGattCallback);
        mBluetoothDeviceAddress = address;
        return true;
    }

    /**
     * Disconnects an existing connection or cancel a pending connection. The disconnection result
     * is reported asynchronously through the
     * {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
     * callback.
     */
    public void disconnect()
    {
        if (mBluetoothAdapter == null || mBluetoothGatt == null)
        {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        mBluetoothGatt.disconnect();
    }

    /**
     * After using a given BLE device, the app must call this method to ensure resources are
     * released properly.
     */
    public void close()
    {
        if (mBluetoothGatt == null) {
            return;
        }
        mBluetoothGatt.close();
        mBluetoothGatt = null;
    }

    public void read() {
        if (mBluetoothGatt == null || mBluetoothGattService == null) {
            Log.w(TAG, "BluetoothGatt not initialized");
            return;
        }

        BluetoothGattCharacteristic characteristic =
                mBluetoothGattService.getCharacteristic(UUID_RECEIVE);

        mBluetoothGatt.readCharacteristic(characteristic);
    }

    public boolean send(byte[] data)
    {
        if (mBluetoothGatt == null || mBluetoothGattService == null) {
            Log.w(TAG, "BluetoothGatt not initialized");
            return false;
        }

        BluetoothGattCharacteristic characteristic =
                mBluetoothGattService.getCharacteristic(UUID_SEND);

        if (characteristic == null)
        {
            Log.w(TAG, "Send characteristic not found");
            return false;
        }

        characteristic.setValue(data);
        characteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE);
        return mBluetoothGatt.writeCharacteristic(characteristic);
    }

    public static IntentFilter getIntentFilter() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_CONNECTED);
        filter.addAction(ACTION_DISCONNECTED);
        filter.addAction(ACTION_DATA_AVAILABLE);
        return filter;
    }

    public boolean ProcessReceivedData(String data) {
        //returns true if detected swallow or TODAY_BEVERAGE_COUNT

    /*
        if (Connected == false)
        {
            Connected = true; // we received the data, its a "heartbeat"
            ReceiveBuffer = "";
            VibrationDataList.clear();
            VibrationDeviationList.clear();
        }
        */
        boolean dataUpdated = false;
        ReceivedData++;
        ReceiveBuffer = ReceiveBuffer + data;

        int begin = ReceiveBuffer.indexOf("B");
        int end = ReceiveBuffer.indexOf("E");
        if (end > begin) {
            String newString = ReceiveBuffer.substring(begin, end + 1);
            ReceiveBuffer = ReceiveBuffer.replace(newString, "");
            newString = newString.replace(" ", "");
            newString = newString.replace("B", "");
            newString = newString.replace("E", "");

            if (newString.contains(":")) {
                String[] data_split = newString.split(":");
                if (data_split.length == 2) {
                    SensorData NewData = new SensorData(data_split[0], data_split[1]);

                    VibrationDataList.add(NewData);

                    if (VibrationDataList.size() > 50) {
                        VibrationDataList.remove(0);
                    }

                    dataUpdated = swallowOrBeverageDetected(VibrationDataList, VibrationDeviationList);

                }
            }
        }
        return dataUpdated;
    }
    public boolean swallowOrBeverageDetected(ArrayList<SensorData> VibrationDataList, ArrayList<Double> VibrationDeviationList)
    {

        called++;
        mean = 0;
        stddev = 0;
		/*
		 * Not enough data has accumulated to detect anything
		 */
        if (VibrationDataList.size() < WINDOW_SIZE)
            return false;
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
                Log.d("success", "detected the swallows");
                //SWALLOW DETECTED
                DISABLE_COUNTER = 0;
                swallowDetected = true;
                //if (RegisterSwallows == true)
                //{

                TODAY_SWALLOW_COUNT++;
                // TODAY_BEVERAGE_COUNT = addElement(TODAY_BEVERAGE_COUNT, sw_count);
                TODAY_FOOD_COUNT++;
                updateLists();
                syncDataWithDatabase();
                //TO DO: Add to fragment
                //countIncreased = true;
                //createTodayGraph(TODAY_FOOD_COUNT, TODAY_BEVERAGE_COUNT);
                Log.d("BT", "increasing the swallow count");

                //}

            }
        }
/*
        if (swallowDetected == false && called % 40 == 0) {

            // TODAY_BEVERAGE_COUNT = addElement(TODAY_BEVERAGE_COUNT, TODAY_SWALLOW_COUNT);
            TODAY_BEVERAGE_COUNT++;
            return true;
            //createTodayGraph(TODAY_FOOD_COUNT, TODAY_BEVERAGE_COUNT);
        }
*/
        return swallowDetected;

    }
    void loadDataFromDatabase() {

        SharedPreferences pref = this.getApplicationContext().getSharedPreferences("MyPref", 0);
        final SharedPreferences.Editor editor = pref.edit();
        String id = pref.getString("parse_object_id", "empty");
        boolean isOnline = isOnline();

        if (id == "empty" || !isOnline) {
            String key = getTodayDateAsStringKey();
            setupEmptyDataValues(key);
        }

        if (!isOnline) {
            needsMergeWithDB = true;
        }
        else {
            ParseQuery<ParseObject> query = ParseQuery.getQuery("UserData");
            query.getInBackground(id, new GetCallback<ParseObject>() {
                public void done(ParseObject onlineData, ParseException e) {
                    if (e == null) {
                        dailyFoodCounts = onlineData.getList(FOOD_LIST_ID);
                        dailyBeverageCounts = onlineData.getList(BEVERAGE_LIST_ID);
                        dailySwallowCounts = onlineData.getList(SWALLOW_LIST_ID);
                        setupCurrentDayCounts();
                    }
                    else{
                        String key = getTodayDateAsStringKey();
                        setupEmptyDataValues(key);
                    }
                }
            });
        }

    }
    void syncDataWithDatabase() {

        boolean isOnline = isOnline();
        if (!isOnline)
            return;

        SharedPreferences pref = this.getApplicationContext().getSharedPreferences("MyPref", 0);
        final SharedPreferences.Editor editor = pref.edit();
        String id = pref.getString("parse_object_id", "empty");
        if (id == "empty")
        {
            final ParseObject data = new ParseObject("UserData");
            setDatabaseValues(data);
            data.saveInBackground(new SaveCallback() {
                public void done(ParseException e) {
                    if (e == null) {
                        editor.putString("parse_object_id", data.getObjectId());
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
                        if (needsMergeWithDB) {
                            resolveMergeConflicts(data);
                        }
                        setDatabaseValues(data);
                        data.saveInBackground();
                    }
                }
            });
        }
    }

    boolean isOnline() {
        ConnectivityManager cm =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

        return cm.getActiveNetworkInfo() != null &&
                cm.getActiveNetworkInfo().isConnectedOrConnecting();
    }

    void sendDataToEatingFrag(final String action)

    {
        final Intent intent = new Intent(action);
        intent.putExtra(SWALLOW_ID, TODAY_SWALLOW_COUNT);
        intent.putExtra(FOOD_ID, TODAY_FOOD_COUNT);
        intent.putExtra(BEVERAGE_ID, TODAY_BEVERAGE_COUNT);
        // Log.d("BT", "sending broadcast");
        sendBroadcast(intent, Manifest.permission.BLUETOOTH);
    }

    void resolveMergeConflicts(ParseObject data)
    {
        dailyFoodCounts = data.getList(FOOD_LIST_ID);
        dailyBeverageCounts = data.getList(BEVERAGE_LIST_ID);
        dailySwallowCounts = data.getList(SWALLOW_LIST_ID);        //if same date, update last item only

        //if different date, append date
        Calendar cur =  Calendar.getInstance();
        Date lastUpdate = data.getUpdatedAt();
        Calendar last = Calendar.getInstance();
        last.setTime(lastUpdate);
        last.add(Calendar.HOUR, -7); //convert to PST


        if (cur.get(Calendar.MONTH) == last.get(Calendar.MONTH) && cur.get(Calendar.DATE) == last.get(Calendar.DATE))
        {
            TODAY_SWALLOW_COUNT += data.getInt(SWALLOW_ID);
            TODAY_FOOD_COUNT += data.getInt(FOOD_ID);
            updateLists();
        }

        else
        {
            Map<String, Integer> newDayFood = new HashMap<String, Integer>();
            Map<String, Integer> newDaySwallows = new HashMap<String, Integer>();

            String key = getTodayDateAsStringKey();
            newDayFood.put(key, TODAY_FOOD_COUNT);
            newDaySwallows.put(key, TODAY_SWALLOW_COUNT);

            dailyFoodCounts.add(newDayFood);
            dailySwallowCounts.add(newDaySwallows);
        }
        needsMergeWithDB = false;
    }

    void setDatabaseValues(ParseObject data)
    {
        data.remove(SWALLOW_LIST_ID);
        data.remove(FOOD_LIST_ID);

        data.addAll(SWALLOW_LIST_ID, dailySwallowCounts);
        data.addAll(FOOD_LIST_ID, dailyFoodCounts);

        data.put(SWALLOW_ID, TODAY_SWALLOW_COUNT);
        data.put(FOOD_ID, TODAY_FOOD_COUNT);
        //data.put(BEVERAGE_LIST_ID, TODAY_BEVERAGE_COUNT);
    }

    void setupCurrentDayCounts()
    {
        TODAY_SWALLOW_COUNT = 0;
        TODAY_FOOD_COUNT = 0;

        //check date of last stored count
        // if same day, add to lasts item, otherwise create new item
        String key = getTodayDateAsStringKey();

        setupSwallowCount(key);
        setupFoodCount(key);

    }

    void setupEmptyDataValues(String key) {

        dailyFoodCounts = new ArrayList<Map<String, Integer>>();
        //dailyBeverageCounts = new ArrayList<Map<String, Integer>>();
        dailySwallowCounts = new ArrayList<Map<String, Integer>>();

//       TODAY_BEVERAGE_COUNT = 0;
        TODAY_FOOD_COUNT = 0;
        TODAY_SWALLOW_COUNT = 0;

        Map<String, Integer> newDaySwallow = new HashMap<String, Integer>();
        Map<String, Integer> newDayFood = new HashMap<String, Integer>();

        newDaySwallow.put(key, TODAY_SWALLOW_COUNT);
        dailySwallowCounts.add(newDaySwallow);

        newDayFood.put(key, TODAY_FOOD_COUNT);
        dailyFoodCounts.add(newDayFood);

    }


    void updateLists()
    {

        String key = getTodayDateAsStringKey();
        Map<String,Integer> lastSwallowDay = dailySwallowCounts.get(dailySwallowCounts.size()-1);
        lastSwallowDay.put(key, TODAY_SWALLOW_COUNT);

        Map<String,Integer> lastFoodDay = dailyFoodCounts.get(dailyFoodCounts.size()-1);
        lastFoodDay.put(key, TODAY_FOOD_COUNT);
    }

    String getTodayDateAsStringKey()
    {
        Calendar todayDate = Calendar.getInstance();
        return Integer.toString(todayDate.get(Calendar.MONTH)+1) + " " + Integer.toString(todayDate.get(Calendar.DATE));
    }

    void setupSwallowCount (String key) {

        if (dailySwallowCounts.size() > 0)
        {
            Map<String,Integer> lastSwallowDay = dailySwallowCounts.get(dailySwallowCounts.size()-1);

            if (lastSwallowDay.containsKey(key)) //data for date exists in database
            {
                TODAY_SWALLOW_COUNT = lastSwallowDay.get(key);
            }
            else
            {
                TODAY_SWALLOW_COUNT = 0;
                Map<String, Integer> newDay = new HashMap<String, Integer>();
                newDay.put(key, TODAY_SWALLOW_COUNT);
                dailySwallowCounts.add(newDay);
            }
        }
        //no data has been stored
        else{
            TODAY_SWALLOW_COUNT = 0;
            Map<String, Integer> newDay = new HashMap<String, Integer>();
            newDay.put(key, TODAY_SWALLOW_COUNT);
            dailySwallowCounts = new ArrayList<Map<String, Integer>>();
            dailySwallowCounts.add(newDay);
        }
    }

    void setupFoodCount(String key) {

        if (dailyFoodCounts.size() > 0)
        {
            Map<String,Integer> lastFoodDay = dailyFoodCounts.get(dailyFoodCounts.size()-1);

            if (lastFoodDay.containsKey(key)) //data for date exists in database
            {
                TODAY_FOOD_COUNT = lastFoodDay.get(key);
            }
            else
            {
                TODAY_FOOD_COUNT = 0;
                Map<String, Integer> newDay = new HashMap<String, Integer>();
                newDay.put(key, TODAY_FOOD_COUNT);
                dailyFoodCounts.add(newDay);
            }
        }
        //no data has been stored
        else{
            TODAY_FOOD_COUNT = 0;
            Map<String, Integer> newDay = new HashMap<String, Integer>();
            newDay.put(key, TODAY_FOOD_COUNT);
            dailyFoodCounts = new ArrayList<Map<String, Integer>>();
            dailyFoodCounts.add(newDay);
        }

    }
}


