package com.wearsens.fragments;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.app.Fragment;
import android.os.IBinder;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;

import com.BluetoothSetup.HexAsciiHelper;
import com.example.bluetoothsensor.R;
import com.BluetoothSetup.RFduinoService;
import com.example.MainInterface.CONSTANTS;

import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

public class SettingsFragment extends Fragment implements BluetoothAdapter.LeScanCallback
{
    // constructor
    public SettingsFragment()
    {
    }

    /*
     * Structures used for saving data received via BT
     */
    String ReceiveBuffer = "";

    static ImageButton connectingButton;
    static TextView TextStdDev;
    static TextView LabelOnOff;
    static ProgressBar MainProgressBar;
    static ProgressBar progressBarConnecting;
    static EditText setThreshold;

    /*
     * For detecting when the necklace has been taken off
     */
    int ReceivedData = 0;
    int LastData = 0;
    /*
     * State machine for Bluetooth
     */
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

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {

        View rootView = inflater.inflate(R.layout.fragment_settings, container, false);

        TextView text = (TextView) rootView.findViewById(R.id.txtLabel);
        Typeface tf = Typeface.createFromAsset(getActivity().getAssets(),"Aaargh.ttf");
        text.setTypeface(tf);
        Drawable bg = rootView.findViewById(R.id.txtLabel).getBackground();
        bg.setAlpha(127);

        TextStdDev = (TextView) rootView.findViewById(R.id.stdDevData);
        connectingButton = (ImageButton) rootView.findViewById(R.id.connectingButton);
        LabelOnOff = (TextView) rootView.findViewById(R.id.labelOnOff);
        MainProgressBar = (ProgressBar) rootView.findViewById(R.id.progressBar1);
        progressBarConnecting = (ProgressBar) rootView.findViewById(R.id.progressBarConnecting);


		/*
		 * This timer helps us detect when the device has been turned off. Ie.
		 * if the last received data does not change for some time
		 */
        Timer timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                if ((LastData == ReceivedData) && (ReceivedData > 2)) {
                    ReceivedData = 0;
                    LastData = 0;

					/*
					 * Here we detect when the device has been turned off after
					 * being turned on previously But we do nothing....for now!
					 */

                    Connected = false;

                    try
                    {
						/*
						 * Close the connections
						 */
                        getActivity().unbindService(rfduinoServiceConnection);
                        rfduinoService.disconnect();
                    } catch (java.lang.IllegalArgumentException ex)
                    {

                    }

                    getActivity().runOnUiThread(new Runnable()
                    {
                        @Override
                        public void run()
                        {
                            connectingButton.setVisibility(ImageButton.INVISIBLE);
                        }
                    });

                }
                LastData = ReceivedData;
            }
        }, 2000, 2000); // 2 seconds is the interval of the timer

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

		/*
		 * A user moving the seekbar will change the value of the threshold for
		 * detecting swallows. This is necessary for calibration
		 */
        SeekBar pg1 = (SeekBar) rootView.findViewById(R.id.seekBar1);
        pg1.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar arg0, int arg1, boolean arg2) {
                // MyEatingFragment.THRESHOLD = (((double) arg1) / ((double)
                // 1000));
                CONSTANTS.threshold = (((double) arg1) / ((double) 1000));
            }

            @Override
            public void onStartTrackingTouch(SeekBar arg0)
            {
            }

            @Override
            public void onStopTrackingTouch(SeekBar arg0)
            {
            }

        });

		/*
		 * If the user presses the "connect" button
		 */
        ImageButton scanButton = (ImageButton) rootView.findViewById(R.id.scan);
        scanButton.setOnClickListener(new ImageButton.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (Connected == false)
                {
                    onStart();
                    progressBarConnecting.setVisibility(ProgressBar.VISIBLE);
                    bluetoothAdapter.startLeScan(new UUID[] { RFduinoService.UUID_SERVICE }, SettingsFragment.this);
                }
            }
        });

		/*
		 * If the user presses the "connect" button
		 */
        ImageButton startButton = (ImageButton) rootView
                .findViewById(R.id.profile);

		/*
		 * Exit the app
		 */
        ImageButton QuitButton = (ImageButton) rootView
                .findViewById(R.id.quitButton);
        QuitButton.setOnClickListener(new ImageButton.OnClickListener() {
            @Override
            public void onClick(View v) {
                // MainUI.MainActivity.finish();
            }
        });



        return rootView;
    }







    /*
     *
     *
     * Some helper functions for Bluetooth
     */
    @Override
    public void onStart() {
        super.onStart();

        getActivity().registerReceiver(bluetoothStateReceiver,
                new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED));
        getActivity().registerReceiver(rfduinoReceiver,
                RFduinoService.getIntentFilter());
        updateState(bluetoothAdapter.isEnabled() ? STATE_DISCONNECTED
                : STATE_BLUETOOTH_OFF);
    }

    private void upgradeState(int newState) {
        if (newState > state) {
            updateState(newState);
        }
    }

    private void downgradeState(int newState)
    {
        if (newState < state)
        {
            updateState(newState);
        }
    }

    private void updateState(int newState)
    {
        state = newState;
    }

    private void addData(byte[] data)
    {
        String ascii = HexAsciiHelper.bytesToAsciiMaybe(data);
        if (ascii != null)
        {
            //	ProcessReceivedData(ascii);
        }
    }

    @Override
    public void onLeScan(BluetoothDevice device, final int rssi, final byte[] scanRecord)
    {
        bluetoothAdapter.stopLeScan(this);
        bluetoothDevice = device;

        getActivity().runOnUiThread(new Runnable()
        {
            @Override
            public void run()
            {

                if (bluetoothDevice.getName().contains("RF"))
                {
					/*
					 * We have scanned and found the RFDuino
					 */

                    Intent rfduinoIntent = new Intent(getActivity(), RFduinoService.class);

                    getActivity().bindService(rfduinoIntent, rfduinoServiceConnection, 1);

                }
            }
        });
    }

    private final BroadcastReceiver bluetoothStateReceiver = new BroadcastReceiver()
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, 0);
            if (state == BluetoothAdapter.STATE_ON)
            {
                upgradeState(STATE_DISCONNECTED);
            } else if (state == BluetoothAdapter.STATE_OFF)
            {
                downgradeState(STATE_BLUETOOTH_OFF);
            }
        }
    };

    private ServiceConnection rfduinoServiceConnection = new ServiceConnection()
    {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service)
        {
            rfduinoService = ((RFduinoService.LocalBinder) service)
                    .getService();
            if (rfduinoService.initialize())
            {
                boolean result = rfduinoService.connect(bluetoothDevice.getAddress());

                if (result == true)
                {
                    upgradeState(STATE_CONNECTING);
                }
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name)
        {
            rfduinoService = null;
            downgradeState(STATE_DISCONNECTED);
        }
    };

    private final BroadcastReceiver rfduinoReceiver = new BroadcastReceiver()
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            final String action = intent.getAction();

            if (RFduinoService.ACTION_CONNECTED.equals(action))
            {
                connectingButton.setVisibility(ImageButton.VISIBLE);

                upgradeState(STATE_CONNECTED);
            }
            else if (RFduinoService.ACTION_DISCONNECTED.equals(action))
            {
                downgradeState(STATE_DISCONNECTED);
            }
            else if (RFduinoService.ACTION_DATA_AVAILABLE.equals(action))
            {
                addData(intent.getByteArrayExtra(RFduinoService.EXTRA_DATA));
            }
        }
    };

};