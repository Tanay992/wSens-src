

package com.BluetoothSetup;

import java.util.ArrayList;
import java.util.List;
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

import android.net.ConnectivityManager;
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

    static int SWALLOW_COUNT = 0;
    static int food = 0;
    static int beverage = 0;
    static boolean needsMergeWithDB;

    String SWALLOW_ID = "SwallowCount";
    String FOOD_ID = "FoodCount";
    String BEVERAGE_ID = "BeverageCount";


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
            final Intent intent = new Intent(action);
            //intent.putExtra(EXTRA_DATA, characteristic.getValue());
            //mark: sends data
            String ascii = HexAsciiHelper.bytesToAsciiMaybe(characteristic.getValue());
            if (ascii != null) {
                boolean shouldBroadcast = ProcessReceivedData(ascii);
                if (shouldBroadcast)
                {
                   intent.putExtra(SWALLOW_ID, SWALLOW_COUNT);
                   intent.putExtra(FOOD_ID, food);
                   intent.putExtra(BEVERAGE_ID, beverage);
                   Log.d("BT", "sending broadcast");
                   sendBroadcast(intent, Manifest.permission.BLUETOOTH);
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

    private void addData(byte[] data) {
        String ascii = HexAsciiHelper.bytesToAsciiMaybe(data);
        if (ascii != null) {
            ProcessReceivedData(ascii);
        }
    }

    public boolean ProcessReceivedData(String data) {
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
        //List<SensorData> VibrationDataList = MyEatingFragment.VibrationDataList;
        //List<Double> VibrationDeviationList = MyEatingFragment.VibrationDeviationList;
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
                Log.d("success", "detected swallows");
				//SWALLOW DETECTED
                DISABLE_COUNTER = 0;
                swallowDetected = true;
                //if (MyEatingFragment.RegisterSwallows == true)
                //{
                    SWALLOW_COUNT++;
                    // beverage = addElement(beverage, sw_count);
                    food++;

                    syncDataWithDatabase();
                    //TO DO: Add to fragment
                    //countIncreased = true;
                    //createTodayGraph(food, beverage);
                    Log.d("BT", "increasing swallow count");

                //}

            }
        }
/*
        if (swallowDetected == false && called % 40 == 0) {

            // beverage = addElement(beverage, SWALLOW_COUNT);
            beverage++;
            return true;
            //createTodayGraph(food, beverage);
        }
*/
            return swallowDetected;

    }
    void loadDataFromDatabase() {

        SharedPreferences pref = this.getApplicationContext().getSharedPreferences("MyPref", 0);
        String id = pref.getString("parse_object_id", "empty");
        boolean isOnline = isOnline();

        if (isOnline)
             Log.d("Internet Status:", " has internet");
        if (id == "empty" || !isOnline) {
            SWALLOW_COUNT = 0;
            food = 0;
            beverage = 0;
            //createTodayGraph(food, beverage);
        }
        if (!isOnline)
               needsMergeWithDB = true;
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
                        //createTodayGraph(food, beverage);
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

                        if (needsMergeWithDB) {
                            //get current database values
                            SWALLOW_COUNT += data.getInt(SWALLOW_ID);
                            food += data.getInt(FOOD_ID);
                            beverage += data.getInt(BEVERAGE_ID);
                            needsMergeWithDB = false;
                        }

                        data.put(SWALLOW_ID, SWALLOW_COUNT);
                        data.put(FOOD_ID, food);
                        data.put(BEVERAGE_ID, beverage);
                        data.saveInBackground();
                    }
                }
            });
        }
    }

    public boolean isOnline() {
        ConnectivityManager cm =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

        return cm.getActiveNetworkInfo() != null &&
                cm.getActiveNetworkInfo().isConnectedOrConnecting();
    }
}


