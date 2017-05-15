package com.example.lenovo.pmcrowdssensev1;


        import android.app.Activity;
        import android.app.AlertDialog;
        import android.content.DialogInterface;
        import android.database.Cursor;
        import android.database.sqlite.SQLiteDatabase;
        import android.media.AudioManager;
        import android.media.ToneGenerator;
        import android.os.Bundle;
        import android.os.Environment;
        import android.text.method.ScrollingMovementMethod;
        import android.util.Log;
        import android.view.View;
        import android.widget.CompoundButton;
        import android.widget.EditText;
        import android.widget.Switch;
        import android.widget.TextView;

        import java.io.File;
        import java.io.FileWriter;
        import java.lang.*;
        import java.text.DateFormat;
        import android.bluetooth.BluetoothAdapter;
        import android.bluetooth.BluetoothAdapter.LeScanCallback;
        import android.bluetooth.BluetoothDevice;
        import android.bluetooth.BluetoothGatt;
        import android.bluetooth.BluetoothGattCallback;
        import android.bluetooth.BluetoothGattCharacteristic;
        import android.bluetooth.BluetoothGattDescriptor;
        import java.nio.ByteBuffer;
        import java.nio.ByteOrder;
        import java.nio.charset.Charset;
        import java.text.SimpleDateFormat;
        import java.util.ArrayList;
        import java.util.Date;
        import java.util.List;
        import java.util.TimeZone;
        import java.util.UUID;
        import android.Manifest;
        import android.content.BroadcastReceiver;
        import android.content.Context;
        import android.content.Intent;
        import android.content.IntentFilter;
        import android.content.pm.PackageManager;
        import android.os.Build;
        import android.support.annotation.NonNull;
        import android.support.v4.content.ContextCompat;
        import android.widget.Button;
        import android.widget.Toast;

        import com.opencsv.CSVWriter;

        import static android.R.attr.tag;


public class MainActivity extends Activity {

    // UUIDs for UAT service and associated characteristics.
    public static UUID UART_UUID = UUID.fromString("6E400001-B5A3-F393-E0A9-E50E24DCCA9E");
    public static UUID TX_UUID = UUID.fromString("6E400002-B5A3-F393-E0A9-E50E24DCCA9E");
    public static UUID RX_UUID = UUID.fromString("6E400003-B5A3-F393-E0A9-E50E24DCCA9E");
    // UUID for the BTLE client characteristic which is necessary for notifications.
    public static UUID CLIENT_UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");

    // UI elements
    private TextView BTmess;
    // private EditText input;

    // BTLE state
    private BluetoothAdapter adapter;
    private BluetoothGatt gatt;
    private BluetoothGattCharacteristic tx;
    private BluetoothGattCharacteristic rx;
    public String macAdd = "not set";

    //SQL
    // EditText userInput;
    TextView recordsTextView;
    TextView timeNow;
    TextView timeNow2;
    MyDBHandler dbHandler;
    public long millis;
    public String realtime;
    public String coordinates = " ";
    public String longitude;
    public String latitude;
    public String datamess;
    public String datastr;
    public boolean servstat;
    public String datapack;
    public boolean packstat;
    public String dataarray;
    public String tempMac;
    //  public String[] coorarray = new String[2];
    //GPS
    private Button btn_start, btn_stop, turnON, turnOFF;
    private BroadcastReceiver broadcastReceiver;

    //export
    final Context context = this;
    public String exportName;


    // Main BTLE device callback where much of the logic occurs.
    private BluetoothGattCallback callback = new BluetoothGattCallback() {
        // Called whenever the device connection state changes, i.e. from disconnected to connected.
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);
            if (newState == BluetoothGatt.STATE_CONNECTED) {
                writeLine("Connected! DO NOT CLICK ANY BUTTONS! \nWait for completion of service discovery.");
                ToneGenerator toneG = new ToneGenerator(AudioManager.STREAM_ALARM, 100);
                toneG.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 200);
                // Discover services.
                if (!gatt.discoverServices()) {
                    writeLine("Failed to start discovering services!");
                }
            } else if (newState == BluetoothGatt.STATE_DISCONNECTED) {
                ToneGenerator toneG = new ToneGenerator(AudioManager.STREAM_ALARM, 100);
                toneG.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 500);
                writeLine("Disconnected!");
            } else {
                writeLine("Connection state changed.  New state: " + newState);
            }
        }

        // Called when services have been discovered on the remote device.
        // It seems to be necessary to wait for this discovery to occur before
        // manipulating any services or characteristics.
        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            super.onServicesDiscovered(gatt, status);
            if (status == BluetoothGatt.GATT_SUCCESS) {
                writeLine("Service discovery completed!\nYou may now click START!");
            } else {
                writeLine("Service discovery failed with status: " + status);
            }
            // Save reference to each characteristic.
            tx = gatt.getService(UART_UUID).getCharacteristic(TX_UUID);
            rx = gatt.getService(UART_UUID).getCharacteristic(RX_UUID);
            // Setup notifications on RX characteristic changes (i.e. data received).
            // First call setCharacteristicNotification to enable notification.
            if (!gatt.setCharacteristicNotification(rx, true)) {
                writeLine("Couldn't set notifications for RX characteristic!");
            }
            // Next update the RX characteristic's client descriptor to enable notifications.
            if (rx.getDescriptor(CLIENT_UUID) != null) {
                BluetoothGattDescriptor desc = rx.getDescriptor(CLIENT_UUID);
                desc.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                if (!gatt.writeDescriptor(desc)) {
                    writeLine("Couldn't write RX client descriptor value!");
                }
            } else {
                writeLine("Couldn't get RX client descriptor!");
            }
        }

        // Called when a remote characteristic changes (like the RX characteristic).
        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicChanged(gatt, characteristic);
            String sendmessage = "M\n";
            datamess = characteristic.getStringValue(0);
            //  writeLine("Received: " + datamess);
            writeLine("Receiving... ");
            millis = System.currentTimeMillis();
            TimeZone tz = TimeZone.getDefault();
            DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss"); // Quoted "Z" to indicate UTC, no timezone offset
            df.setTimeZone(tz);
            String realtime = df.format(new Date());
            //  String realtime = String.valueOf(millis);
            //      writeLine("servstat = " + servstat);
            if ((datamess.substring(0, 1).equals("P")) && (servstat)) {
                //    writeLine("servstat = " + servstat);
                packstat = true;
                //start of edit

                //   if (datamess.substring(0,3).equals("KRL")){
                datapack = datamess.substring(3);
                //    }
                //   else if (datamess.substring(0,2).equals("RL")){
                //      datapack = datamess.substring(2);
                //  } else if (datamess.substring(0,1).equals("L")) {
                //        datapack = datamess.substring(1);
                //   }

                //end of edit
                //originally datapack = datamess.substring(3);

                if ((datamess.substring(datamess.length() - 1).equals("M")) && (packstat)) {
                    packstat = false;
                    datapack = datapack.substring(0, datapack.length() - 3);
                    //        writeLine("Saving " + datapack);
                    writeLine("Saving...");
                    try {
                        Thread.sleep(1000);                 //1000 milliseconds is one second.
                    } catch (InterruptedException ex) {
                    }
                    // Update TX characteristic value.  Note the setValue overload that takes a byte array must be used.
                    tx.setValue(sendmessage.getBytes(Charset.forName("UTF-8")));
                    if (gatt.writeCharacteristic(tx)) {
                        writeLine("Ask for data");
                    } else {
                        writeLine("Couldn't write TX characteristic!");
                    }
                    dataarray = datapack;
                    //      coorarray = coordinates.split(",");
                    //           writeLine("test: " + dataarray[5]);
                    //          writeLine("test: " + dataarray[1]);
                    Messages message = new Messages(realtime, dataarray, coordinates, macAdd);
                    //   Messages message = new Messages(datapack, dataarray[0], dataarray[1], dataarray[2], dataarray[3], dataarray[4], dataarray[5], dataarray[6], millis, realtime, coordinates, longitude, latitude);
                    //        Messages message = new Messages(datapack, millis, realtime, coordinates);
                    dbHandler.addMessage(message);
                    printDatabase();
                }
            } else if ((datamess.substring(datamess.length() - 1).equals("M")) && (packstat)) {
                packstat = false;
                datapack = datapack + datamess;
                datapack = datapack.substring(0, datapack.length() - 3);
                //    writeLine("Saving " + datapack);
                writeLine("Saving...");
                try {
                    Thread.sleep(1000);                 //1000 milliseconds is one second.
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                }
                // Update TX characteristic value.  Note the setValue overload that takes a byte array must be used.
                tx.setValue(sendmessage.getBytes(Charset.forName("UTF-8")));
                if (gatt.writeCharacteristic(tx)) {
                    writeLine("Ask for data");
                } else {
                    writeLine("Couldn't write TX characteristic!");
                }
                dataarray = datapack;
                //    coorarray = coordinates.split(",");
                //  writeLine("test: " + dataarray[5]);
                //   writeLine("test: " + coorarray[1]);
                Messages message = new Messages(realtime, dataarray, coordinates, macAdd);
                //  Messages message = new Messages(datapack, dataarray[0], dataarray[1], dataarray[2], dataarray[3], dataarray[4], dataarray[5], dataarray[6], millis, realtime, coordinates, longitude, latitude);
                //   Messages message = new Messages(datapack, millis, realtime, coordinates);
                dbHandler.addMessage(message);
                printDatabase();
            } else if (packstat == true) {
                datapack = datapack + datamess;
            }
/*            if (datamess == null || datamess.isEmpty()) {
                // Do nothing if there is no device or message to send.

                if (datastr != null || datastr != "") {
                    Messages message = new Messages(datastr, millis, realtime, coordinates);
                    dbHandler.addMessage(message);
                    printDatabase();
                }
                datastr = "";
                return;
            } else {
                datastr += datamess;
                }
         */
        }
    };

    // BTLE device scanning callback.
    private LeScanCallback scanCallback = new LeScanCallback() {
        // Called when a device is found.
        @Override
        public void onLeScan(BluetoothDevice bluetoothDevice, int i, byte[] bytes) {
            writeLine("Found device: " + bluetoothDevice.getAddress());
            tempMac = bluetoothDevice.getAddress();
            // Check if the device has the UART service.
            if ((parseUUIDs(bytes).contains(UART_UUID)) && (bluetoothDevice.getAddress().equals(macAdd))) {
                // Found a device, stop the scan.
                adapter.stopLeScan(scanCallback);
                writeLine("Found UART service!");
                // Connect to the device.
                // Control flow will now go to the callback functions when BTLE events occur.
                gatt = bluetoothDevice.connectGatt(getApplicationContext(), false, callback);
            }
        }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (broadcastReceiver != null) {
            unregisterReceiver(broadcastReceiver);
        }
    }

    // OnCreate, called once to initialize the activity.
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Grab references to UI elements.
        BTmess = (TextView) findViewById(R.id.BTmess);
        // input = (EditText) findViewById(R.id.input);


        //   btn_start = (Button) findViewById(R.id.button);
        //   btn_stop = (Button) findViewById(R.id.button2);

        // turnON = (Button) findViewById(R.id.turnON);
        //   turnOFF = (Button) findViewById(R.id.turnOFF);

        adapter = BluetoothAdapter.getDefaultAdapter();
        // userInput = (EditText) findViewById(R.id.user_Input);
        // input = userInput;
        recordsTextView = (TextView) findViewById(R.id.records_TextView);
        timeNow = (TextView) findViewById(R.id.timenow);

        // initiate a Switch
        Switch Switch1 = (Switch) findViewById(R.id.switch1);
        Switch1.setTextOn("On"); // displayed text of the Switch whenever it is in checked or on state
        Switch1.setTextOff("Off"); // displayed text of the Switch whenever it is in unchecked i.e. off state

        Switch Switch2 = (Switch) findViewById(R.id.switch2);
        Switch2.setTextOn("On"); // displayed text of the Switch whenever it is in checked or on state
        Switch2.setTextOff("Off"); // displayed text of the Switch whenever it is in unchecked i.e. off state

        Switch1.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(CompoundButton buttonView,
                                         boolean isChecked) {

                if (isChecked) {
                    //writeLine("Switch is currently ON");
                    Intent i =new Intent(getApplicationContext(),GPS_Service.class);
                    startService(i);
                } else {
                    // writeLine("Switch is currently OFF");
                    Intent i =new Intent(getApplicationContext(),GPS_Service.class);
                    stopService(i);
                }

            }
        });

        //check the current state before we display the screen
        if (Switch1.isChecked()) {
            // writeLine("Switch is currently ON");
            Intent i =new Intent(getApplicationContext(),GPS_Service.class);
            startService(i);
        } else {
            // writeLine("Switch is currently OFF");
            Intent i =new Intent(getApplicationContext(),GPS_Service.class);
            stopService(i);
        }

        Switch2.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(CompoundButton buttonView,
                                         boolean isChecked) {

                if (isChecked) {
                    //  writeLine("Switch is currently ON");
                    startActivityForResult(new Intent(
                            BluetoothAdapter.ACTION_REQUEST_ENABLE), 0);
                } else {
                    //   writeLine("Switch is currently OFF");
                    adapter.disable();
                }

            }
        });

        //check the current state before we display the screen
        if (Switch2.isChecked()) {
            //     writeLine("Switch is currently ON");
            startActivityForResult(new Intent(
                    BluetoothAdapter.ACTION_REQUEST_ENABLE), 0);
        } else {
            //   writeLine("Switch is currently OFF");
            adapter.disable();
        }


        /* Can pass nulls becausnce of the constants in the helper.
         * the 1 means version 1 so don't run update.
         */
        dbHandler = new MyDBHandler(this, null, null, 1);
        millis = System.currentTimeMillis();

        BTmess.setMovementMethod(new ScrollingMovementMethod());
        recordsTextView.setMovementMethod(new ScrollingMovementMethod());

        if(!runtime_permissions())
            enable_buttons();
        Thread t = new Thread() {

            @Override
            public void run() {
                try {
                    while (!isInterrupted()) {
                        Thread.sleep(1000);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                //    timeNow.setText("last update: " + DateFormat.getDateTimeInstance().format(millis));
                                timeNow.setText( "Database Entries: " + String.valueOf(dbHandler.countMessage()));

                                String dbString = dbHandler.getTableAsString();
                                recordsTextView.setText(dbString);
                            }
                        });
                    }
                } catch (InterruptedException e) {
                }
            }
        };

        t.start();
        writeLine(" ");

    }

    // OnResume, called right before UI is displayed.  Start the BTLE connection.
    @Override
    protected void onResume() {
        super.onResume();
        // Scan for all BTLE devices.
        // The first one with the UART service will be chosen--see the code in the scanCallback.
        //     writeLine("Scanning for devices...");
        adapter.startLeScan(scanCallback);

        if(broadcastReceiver == null){
            broadcastReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    coordinates = (String) intent.getExtras().get("coordinates");
                    //     latitude = (String) intent.getExtras().get("latitude");
                    //     longitude = (String) intent.getExtras().get("longitude");
                    //     BTmess.append( "long: " + longitude);
                    //     BTmess.append( "lat: " + latitude);
                    //    BTmess.append( "loc: " + coordinates + "\n");
                    // attempt to scroll down //      scroll.fullScroll(View.FOCUS_DOWN);

                }
            };
        }
        registerReceiver(broadcastReceiver,new IntentFilter("location_update"));
        printDatabase();
    }

/*
    // OnStop, called right before the activity loses foreground focus.  Close the BTLE connection.
    @Override
    protected void onStop() {
        super.onStop();
   /    if (gatt != null) {
            // For better reliability be careful to disconnect and close the connection.
            gatt.disconnect();
            gatt.close();
            gatt = null;
            tx = null;
            rx = null;
        }
    } */

  /*  // Handler for mouse click on the send button.
    public void sendButtonClicked(View view) {
        String sendmessage = input.getText().toString();
        if (tx == null || sendmessage == null || sendmessage.isEmpty()) {
            // Do nothing if there is no device or message to send.
            return;
        }
        // Update TX characteristic value.  Note the setValue overload that takes a byte array must be used.
        tx.setValue(sendmessage.getBytes(Charset.forName("UTF-8")));
        if (gatt.writeCharacteristic(tx)) {
            writeLine("Sent: " + sendmessage);
        }
        else {
            writeLine("Couldn't write TX characteristic!");
        }

        input.setText("");
    } */

    // Write some text to the BTmess text view.
    // Care is taken to do this on the main UI thread so writeLine can be called
    // from any thread (like the BTLE callback).
    private void writeLine(final CharSequence text) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                BTmess.append(text);
                BTmess.append("\n");
            }
        });
    }

    // Filtering by custom UUID is broken in Android 4.3 and 4.4, see:
    //   http://stackoverflow.com/questions/18019161/startlescan-with-128-bit-uuids-doesnt-work-on-native-android-ble-implementation?noredirect=1#comment27879874_18019161
    // This is a workaround function from the SO thread to manually parse advertisement data.
    private List<UUID> parseUUIDs(final byte[] advertisedData) {
        List<UUID> uuids = new ArrayList<UUID>();

        int offset = 0;
        while (offset < (advertisedData.length - 2)) {
            int len = advertisedData[offset++];
            if (len == 0)
                break;

            int type = advertisedData[offset++];
            switch (type) {
                case 0x02: // Partial list of 16-bit UUIDs
                case 0x03: // Complete list of 16-bit UUIDs
                    while (len > 1) {
                        int uuid16 = advertisedData[offset++];
                        uuid16 += (advertisedData[offset++] << 8);
                        len -= 2;
                        uuids.add(UUID.fromString(String.format("%08x-0000-1000-8000-00805f9b34fb", uuid16)));
                    }
                    break;
                case 0x06:// Partial list of 128-bit UUIDs
                case 0x07:// Complete list of 128-bit UUIDs
                    // Loop through the advertised 128-bit UUID's.
                    while (len >= 16) {
                        try {
                            // Wrap the advertised bits and order them.
                            ByteBuffer buffer = ByteBuffer.wrap(advertisedData, offset++, 16).order(ByteOrder.LITTLE_ENDIAN);
                            long mostSignificantBit = buffer.getLong();
                            long leastSignificantBit = buffer.getLong();
                            uuids.add(new UUID(leastSignificantBit,
                                    mostSignificantBit));
                        } catch (IndexOutOfBoundsException e) {
                            // Defensive programming.
                            //Log.e(LOG_TAG, e.toString());
                            continue;
                        } finally {
                            // Move the offset to read the next uuid.
                            offset += 15;
                            len -= 16;
                        }
                    }
                    break;
                default:
                    offset += (len - 1);
                    break;
            }
        }
        return uuids;
    }



    //Print the database
    public void printDatabase(){
        //   timeNow.setText("last update: " + DateFormat.getDateTimeInstance().format(millis));
        timeNow.setText( "Database Entries: " + String.valueOf(dbHandler.countMessage()));
        String dbString = dbHandler.getTableAsString();
        recordsTextView.setText(dbString);
    }

    //add your elements onclick methods.
    //Add a message to the database
    public void addButtonClicked(View view){
        // dbHandler.add needs an object parameter.
        // millis = System.currentTimeMillis();
        //Messages message = new Messages(userInput.getText().toString(), millis);
        // dbHandler.addMessage(message);
        printDatabase();
    }

    //Delete items
    public void deleteButtonClicked(View view){
        // dbHandler delete needs string to find in the db
        AlertDialog.Builder builder1 = new AlertDialog.Builder(context);
        builder1.setMessage("Are you sure you want to delete the current database?");
        builder1.setCancelable(true);

        builder1.setPositiveButton(
                "Yes",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dbHandler.deleteMessage();
                        printDatabase();
                        BTmess.append("\n" + "Database deleted." + "\n");
                        dialog.cancel();
                    }
                });

        builder1.setNegativeButton(
                "No",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                });

        AlertDialog alert11 = builder1.create();
        alert11.show();




    }

    public void exportButtonClicked(View arg0){
        //File dbFile=getDatabasePath("productDB.db");
        //MyDBHandler dbhelper = new MyDBHandler(getApplicationContext());


        AlertDialog.Builder alert = new AlertDialog.Builder(context);
        final EditText edittext = new EditText(context);

        TimeZone tz = TimeZone.getDefault();
        DateFormat df = new SimpleDateFormat("MMddyy"); // Quoted "Z" to indicate UTC, no timezone offset
        df.setTimeZone(tz);
        String realdate = df.format(new Date());
        edittext.setText(realdate + "-" + macAdd.substring(0,2));
        alert.setMessage("Enter file name");
        alert.setTitle("Export to CSV");
        alert.setView(edittext);

        alert.setPositiveButton("Export", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                //What ever you want to do with the value
                exportName = edittext.getText().toString();
                File exportDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), "");
                if (!exportDir.exists())
                {
                    exportDir.mkdirs();
                }

                File file = new File(exportDir, exportName + ".csv");
                try
                {
                    file.createNewFile();
                    CSVWriter csvWrite = new CSVWriter(new FileWriter(file));
                    SQLiteDatabase db = dbHandler.getReadableDatabase();
                    Cursor curCSV = db.rawQuery("SELECT * FROM messages",null);
                    csvWrite.writeNext(curCSV.getColumnNames());
                    while(curCSV.moveToNext())
                    {
                        //Which column you want to export
                        String arrStr[] ={curCSV.getString(0), curCSV.getString(1), curCSV.getString(2), curCSV.getString(3)};
                        csvWrite.writeNext(arrStr);
                    }
                    csvWrite.close();
                    curCSV.close();
                }
                catch(Exception sqlEx)
                {
                    Log.e("MainActivity", sqlEx.getMessage(), sqlEx);
                }

            }
        });

        alert.show();



        BTmess.append("Database exported." + "\n");
    }


    public void onClickClearBT(View view){
        BTmess.setText("Notifications: \n");
    }

    /* public void getStringButtonClicked(View view) {
        String sendmessage = "s";
        if (tx == null || sendmessage == null || sendmessage.isEmpty()) {
            // Do nothing if there is no device or message to send.
            return;
        }
        // Update TX characteristic value.  Note the setValue overload that takes a byte array must be used.ffff
        tx.setValue(sendmessage.getBytes(Charset.forName("UTF-8")));
        if (gatt.writeCharacteristic(tx)) {
            writeLine("Get String");
        }
        else {
            writeLine("Couldn't write TX characteristic!");
        }

    } */

    public void resetButtonClicked(View view) {
        String sendmessage = "R";
        if (tx == null || sendmessage == null || sendmessage.isEmpty()) {
            // Do nothing if there is no device or message to send.
            return;
        }
        // Update TX characteristic value.  Note the setValue overload that takes a byte array must be used.
        tx.setValue(sendmessage.getBytes(Charset.forName("UTF-8")));
        if (gatt.writeCharacteristic(tx)) {
            writeLine("Reset");
        }
        else {
            writeLine("Couldn't write TX characteristic!");
        }
    }

    public void getMessageButtonClicked(View view) {
        String sendmessage = "M";
        if (tx == null || sendmessage == null || sendmessage.isEmpty()) {
            // Do nothing if there is no device or message to send.
            return;
        }
        // Update TX characteristic value.  Note the setValue overload that takes a byte array must be used.
        tx.setValue(sendmessage.getBytes(Charset.forName("UTF-8")));
        if (gatt.writeCharacteristic(tx)) {
            writeLine("get binary message");
        }
        else {
            writeLine("Couldn't write TX characteristic!");
        }
    }

    public void startButtonClicked(View view) {
        String sendmessage = "M\n";
        servstat = true;
        packstat = false;
        if (tx == null || sendmessage == null || sendmessage.isEmpty()) {
            // Do nothing if there is no device or message to send.
            return;
        }
        // Update TX characteristic value.  Note the setValue overload that takes a byte array must be used.
       /* try {
           Thread.sleep(1000);                 //1000 milliseconds is one second.
        } catch(InterruptedException ex) {
            Thread.currentThread().interrupt();
        } */
        tx.setValue(sendmessage.getBytes(Charset.forName("UTF-8")));
        if (gatt.writeCharacteristic(tx)) {
            writeLine("Start");
            writeLine("Ask for data");
        }
        else {
            writeLine("Couldn't write TX characteristic!");
        }
    }

    public void stopButtonClicked(View view) {
        servstat = false;
        writeLine("Stop");

    }


    /*   public void pButtonClicked(View view) {
           String p = "P";
           char s = 0x01;
           String sendmessage = p + s;
           if (tx == null || sendmessage == null || sendmessage.isEmpty()) {
               // Do nothing if there is no device or message to send.
               return;
           }
           // Update TX characteristic value.  Note the setValue overload that takes a byte array must be used.
           tx.setValue(sendmessage.getBytes(Charset.forName("UTF-8")));
           if (gatt.writeCharacteristic(tx)) {
               writeLine("Set sampling rate to 1s");
           }
           else {
               writeLine("Couldn't write TX characteristic!");
           }
       }

       public void ackButtonClicked(View view) {
           char sendmessage = 0x06;
           if (tx == null) {
               // Do nothing if there is no device or message to send.
               return;
           }
           // Update TX characteristic value.  Note the setValue overload that takes a byte array must be used.
           tx.setValue(sendmessage, 17, 0);
           if (gatt.writeCharacteristic(tx)) {
               writeLine("ACK");
           }
           else {
               writeLine("Couldn't write TX characteristic!");
           }
       }

       public void nackButtonClicked(View view) {
           char sendmessage = 0x15;
           if (tx == null) {
               // Do nothing if there is no device or message to send.
               return;
           }
           // Update TX characteristic value.  Note the setValue overload that takes a byte array must be used.
           tx.setValue(sendmessage, 17, 0);
           if (gatt.writeCharacteristic(tx)) {
               writeLine("NACK");
           }
           else {
               writeLine("Couldn't write TX characteristic!");
           }
       }
   */
    private void enable_buttons() {
/*
        btn_start.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i =new Intent(getApplicationContext(),GPS_Service.class);
                startService(i);
            }
        });

        btn_stop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                Intent i = new Intent(getApplicationContext(),GPS_Service.class);
                stopService(i);

            }
        });

        turnON.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (!adapter.isEnabled()) {
                    Toast.makeText(getApplicationContext(),
                            "Turning ON Bluetooth", Toast.LENGTH_LONG);
                    // Intent enableBtIntent = new
                    // Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    startActivityForResult(new Intent(
                            BluetoothAdapter.ACTION_REQUEST_ENABLE), 0);
                }
            }
        });

        turnOFF.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                adapter.disable();
                Toast.makeText(getApplicationContext(),
                        "TURNING OFF BLUETOOTH", Toast.LENGTH_LONG);
            }
        });*/
    }

    public void setMacAdd(View arg0){


        AlertDialog.Builder alert = new AlertDialog.Builder(context);
        final EditText edittext = new EditText(context);
        // edittext.setText("D7:E4:E4:46:19:2B");
        edittext.setText(tempMac);
        alert.setMessage("Enter MAC Address");
        alert.setTitle("Device you want to get connected.");
        alert.setView(edittext);

        alert.setPositiveButton("Set", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                //What ever you want to do with the value
                macAdd = edittext.getText().toString();
            }
        });

        alert.show();

        BTmess.append("Device address set as " + macAdd + "\n");
    }

    private boolean runtime_permissions() {
        if(Build.VERSION.SDK_INT >= 23 && ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED){

            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},100);

            return true;
        }
        return false;
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(requestCode == 100){
            if( grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED){
                enable_buttons();
            }else {
                runtime_permissions();
            }
        }
    }
}

