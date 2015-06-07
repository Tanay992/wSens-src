

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

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileInputStream;

import java.io.FileWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

import java.text.SimpleDateFormat;
import java.text.DateFormat;
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
    public static ArrayList<SensorData> MicDataList = new ArrayList<SensorData>();


    String ReceiveBuffer = "";
    int ReceivedData = 0;

    final static int WINDOW_SIZE = 10;
    final static int DISABLE_LENGTH = CONSTANTS.DISABLE_LENGTH;
    static int DISABLE_COUNTER = 0;
    static int ConsecutiveZero = 0;
    static double LastReading = 0;
    static int vib_log_counter = 0;
    static int mic_log_counter = 0;
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

    String FOOD_MAP_ID = "FoodHistMap";
    String SWALLOW_MAP_ID = "SwallowHistMap";

    static Map<String,Integer> dailyFoodCnt;
    static Map<String, Integer> dailySwallowCnt;

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

        int beginVibration = ReceiveBuffer.indexOf("B");
        int beginMicrophone = ReceiveBuffer.indexOf("M");
        int end = ReceiveBuffer.indexOf("E");
        int micDist = end - beginMicrophone;
        int vibDist = end - beginVibration;


        if (micDist < vibDist && micDist > 0 )
        {
            //store mic data
            String newString = ReceiveBuffer.substring(beginMicrophone, end + 1);
            ReceiveBuffer = ReceiveBuffer.replace(newString, "");
            newString = newString.replace(" ", "");
            newString = newString.replace("M", "");
            newString = newString.replace("E", "");

            if (newString.contains(":")) {
                String[] data_split = newString.split(":");
                if (data_split.length == 2) {
                    SensorData NewData = new SensorData(data_split[0], data_split[1]);
                    MicDataList.add(NewData);
                    mic_log_counter++;
                    if (mic_log_counter == 50)
                    {
                        //record every 50 times
                        attemptRecordData(MicDataList, "micData.txt");
                        mic_log_counter = 0;
                    }
                    if (MicDataList.size() > 50) {
                        MicDataList.remove(0);
                    }
                }
            }
        }
        else if (vibDist > micDist && vibDist > 0)
        {
            //store vib data
            String newString = ReceiveBuffer.substring(beginVibration, end + 1);
            ReceiveBuffer = ReceiveBuffer.replace(newString, "");
            newString = newString.replace(" ", "");
            newString = newString.replace("B", "");
            newString = newString.replace("E", "");

            if (newString.contains(":")) {
                String[] data_split = newString.split(":");
                if (data_split.length == 2) {
                    SensorData NewData = new SensorData(data_split[0], data_split[1]);
                    VibrationDataList.add(NewData);
                    vib_log_counter++;
                    if (vib_log_counter == 50)
                    {
                        //record every 50 times
                        attemptRecordData(VibrationDataList, "vibData.txt");
                        vib_log_counter = 0;
                    }
                    if (VibrationDataList.size() > 50) {
                        VibrationDataList.remove(0);
                    }

                    dataUpdated = swallowOrBeverageDetected(VibrationDataList, VibrationDeviationList);

                }
            }
        }

        if (end > beginV) {


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
                        dailyFoodCnt = onlineData.getMap(FOOD_MAP_ID);
                        dailySwallowCnt = onlineData.getMap(SWALLOW_MAP_ID);
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
        String key = getTodayDateAsStringKey();
        Log.d("Step1", "fd cnt is " +  TODAY_FOOD_COUNT);
        dailyFoodCnt = data.getMap(FOOD_MAP_ID);
        if (dailyFoodCnt.containsKey(key))
        {
            int oldFoodCnt = dailyFoodCnt.get(key);
            TODAY_FOOD_COUNT+= oldFoodCnt;
            Log.d("Step1", "fd cnt is " +  TODAY_FOOD_COUNT);

        }
        dailyFoodCnt.put(key, TODAY_FOOD_COUNT);

        dailySwallowCnt = data.getMap(SWALLOW_MAP_ID);
        if (dailySwallowCnt.containsKey(key))
        {
            int oldSwallowCnt = dailySwallowCnt.get(key);
            TODAY_SWALLOW_COUNT+= oldSwallowCnt;
        }
        dailySwallowCnt.put(key, TODAY_SWALLOW_COUNT);

        needsMergeWithDB = false;
    }

    void setDatabaseValues(ParseObject data)
    {

        data.put(SWALLOW_ID, TODAY_SWALLOW_COUNT);
        data.put(FOOD_ID, TODAY_FOOD_COUNT);
        data.put(SWALLOW_MAP_ID, dailySwallowCnt);
        data.put(FOOD_MAP_ID, dailyFoodCnt);
        //data.put(BEVERAGE_LIST_ID, TODAY_BEVERAGE_COUNT);
    }

    void setupCurrentDayCounts()
    {
        String key = getTodayDateAsStringKey();

        setupSwallowCount(key);
        setupFoodCount(key);

    }

    void setupEmptyDataValues(String key) {

        TODAY_FOOD_COUNT = 0;
        dailyFoodCnt = new HashMap<String, Integer>();
        dailyFoodCnt.put(key, TODAY_FOOD_COUNT);

        //dailyBeverageCounts = new HashMap<String, Integer>();
        TODAY_SWALLOW_COUNT = 0;
        dailySwallowCnt = new HashMap<String, Integer>();
        dailySwallowCnt.put(key, TODAY_SWALLOW_COUNT);
    }


    void updateLists()
    {
        String key = getTodayDateAsStringKey();
        dailyFoodCnt.put(key, TODAY_FOOD_COUNT);
        dailySwallowCnt.put(key, TODAY_SWALLOW_COUNT);
    }

    String getTodayDateAsStringKey()
    {
        Calendar todayDate = Calendar.getInstance();
        return Integer.toString(todayDate.get(Calendar.MONTH)+1) + " " + Integer.toString(todayDate.get(Calendar.DATE));
    }

    void setupSwallowCount (String key) {

        if (dailySwallowCnt.containsKey(key))
        {
            //old day
            TODAY_SWALLOW_COUNT = dailySwallowCnt.get(key);

        }
        else
        {
            //create new day
            TODAY_SWALLOW_COUNT = 0;
            dailySwallowCnt.put(key, TODAY_SWALLOW_COUNT);

        }
    }

    void setupFoodCount(String key) {

        if (dailyFoodCnt.containsKey(key))
        {
            //old day
            TODAY_FOOD_COUNT = dailyFoodCnt.get(key);

        }
        else
        {
            //create new day
            TODAY_FOOD_COUNT = 0;
            dailyFoodCnt.put(key, TODAY_FOOD_COUNT);
        }
    }

    void attemptRecordData(ArrayList<SensorData> list, String filename)
    {
        SharedPreferences pref = this.getApplicationContext().getSharedPreferences("MyPref", 0);
        Boolean shouldRecord = pref.getBoolean("shouldLog", false);
        if (shouldRecord)
        {
            String total = "";
            for (int i = 0; i < list.size(); i++) {
                SensorData d= list.get(i);
                total += d.iValue + " ";
            }

            writeToFile(total, filename);


        }

    }

    public void writeToFile(String data, String filename) {

        try {
            FileOutputStream fileout=openFileOutput(filename, MODE_APPEND);
            OutputStreamWriter outputWriter=new OutputStreamWriter(fileout);
            DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
            Calendar cal = Calendar.getInstance();
            String s = (dateFormat.format(cal.getTime())); //2014/08/06 16:00:22
            outputWriter.write(s);
            outputWriter.write('\n');
            outputWriter.write(data);
            outputWriter.write('\n');
            String empty = "   ";
            outputWriter.write(empty);
            outputWriter.write('\n');
            outputWriter.close();

            //display file saved message
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void readFile(String filename) {
        //reading text from file
        try {
            FileInputStream fileIn=openFileInput(filename);
            InputStreamReader InputRead= new InputStreamReader(fileIn);

            char[] inputBuffer= new char[5*50];
            String s="";
            int charRead;


            while ((charRead=InputRead.read(inputBuffer))>0) {
                // char to string conversion
                String readstring=String.copyValueOf(inputBuffer,0,charRead);
                s +=readstring;

            }
            InputRead.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }



}


