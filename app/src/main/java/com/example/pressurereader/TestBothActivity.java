package com.example.pressurereader;

import static android.content.ContentValues.TAG;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.ekn.gruzer.gaugelibrary.FullGauge;
import com.ekn.gruzer.gaugelibrary.HalfGauge;
import com.ekn.gruzer.gaugelibrary.Range;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class TestBothActivity extends AppCompatActivity {

    private String deviceAddress = null;
    public static Handler handler;
    public static BluetoothSocket mmSocket;
    public static ConnectedThread connectedThread;
    public static CreateConnectThread createConnectThread;

    public static TestLeakageThread testLeakageThread;

    public static double firstValue = 0;
    public static double secondValue = 0;
    public static double thirdValue = 0;
    public static double fourthValue = 0;
    public static double fifthValue = 0;
    public static double sixthValue = 0;

    public static String result;
    public int resultNum;

    public TextView pres1;
    public TextView pres2;
    public TextView pres3;
    public TextView presT1;
    public TextView presT2;
    public TextView presT3;
    public TextView res_text;
    public TextView leakage;
    public TextView leakageSemi;
    public HalfGauge gauge;
    public HalfGauge gauge2;
    public FullGauge timeGauge;
    public Range range;
    public Range range2;
    public Range range3;
    public Button contButton;
    public Button buttonTest;
    public boolean contTest, isSemiLeaking, isTrailerLeaking;
    private DatabaseReference rDatabase, rDatabase2, rDatabase3;

    public static BluetoothSocket getMmSocket() {
        return mmSocket;
    }

    // The following variables used in bluetooth handler to identify message status
    private final static int CONNECTION_STATUS = 1;
    private final static int MESSAGE_READ = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test_both);

        // Instantiate UI
        final TextView bluetoothStatus = findViewById(R.id.textBluetoothStatus);
        Button buttonConnect = findViewById(R.id.buttonConnect);
        Button buttonDisconnect = findViewById(R.id.buttonDisconnect);
        final TextView sensorStatus = findViewById(R.id.textLedStatus);
        Button buttonOn = findViewById(R.id.buttonOn);
        Button buttonOff = findViewById(R.id.buttonOff);
        buttonTest = findViewById(R.id.buttonTestLeakage);
        contButton = findViewById(R.id.buttonContinue);
        pres1 = findViewById(R.id.press1);
        pres2 = findViewById(R.id.press2);
        pres3 = findViewById(R.id.press3);
        presT1 = findViewById(R.id.pressT1);
        presT2 = findViewById(R.id.pressT2);
        presT3 = findViewById(R.id.pressT3);
        res_text = findViewById(R.id.resultText);
        leakage = findViewById(R.id.leakage);
        leakageSemi = findViewById(R.id.leakageSemi);
        timeGauge = findViewById(R.id.timerGauge);
        EditText plateSemi = findViewById(R.id.plateInputSemi);
        EditText plateTrailer = findViewById(R.id.plateInputTrailer);
        Button saveTrailer = findViewById(R.id.saveTrailerRecord);
        Button saveSemi = findViewById(R.id.saveSemiRecord);

        //Initializing gauge
        gauge = findViewById(R.id.pressureGauge);
        gauge2 = findViewById(R.id.pressure2Gauge);

        range = new Range();
        range.setColor(Color.parseColor("#ce0000"));
        range.setFrom(0.0);
        range.setTo(7.5);

        range2 = new Range();
        range2.setColor(Color.parseColor("#E3E500"));
        range2.setFrom(7.5);
        range2.setTo(22.5);

        range3 = new Range();
        range3.setColor(Color.parseColor("#00b20b"));
        range3.setFrom(22.5);
        range3.setTo(30.0);

        //add color ranges to gauge
        gauge.addRange(range);
        gauge.addRange(range2);
        gauge.addRange(range3);

        gauge2.addRange(range);
        gauge2.addRange(range2);
        gauge2.addRange(range3);

        //set min max and current value
        gauge.setMinValue(0.0);
        gauge.setMaxValue(30.0);
        gauge.setValue(0.0);

        gauge2.setMinValue(0.0);
        gauge2.setMaxValue(30.0);
        gauge2.setValue(0.0);

        timeGauge.setMinValue(0.0);
        timeGauge.setMaxValue(4.0);

        // Code for the "Connect" button
        buttonConnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // This is the code to move to another screen
                Intent intent = new Intent(TestBothActivity.this, SelectDeviceActivity.class);
                startActivity(intent);
            }
        });

        // Get Device Address from SelectDeviceActivity.java to create connection
        deviceAddress = getIntent().getStringExtra("deviceAddress");
        if (deviceAddress != null){
            bluetoothStatus.setText("Connecting...");
            /*
            This is the most important piece of code.
            When "deviceAddress" is found, the code will call the create connection thread
            to create bluetooth connection to the selected device using the device Address
             */
            BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            createConnectThread = new CreateConnectThread(bluetoothAdapter,deviceAddress);
            createConnectThread.start();
        }



        /*
        Second most important piece of Code.
        This handler is used to update the UI whenever a Thread produces a new output
        and passes through the values to Main Thread
         */
        handler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message msg){
                switch (msg.what){
                    // If the updates come from the Thread to Create Connection
                    case CONNECTION_STATUS:
                        switch(msg.arg1){
                            case 1:
                                bluetoothStatus.setText("Bluetooth Connected");
                                break;
                            case -1:
                                bluetoothStatus.setText("Connection Failed");
                                break;
                        }
                        break;

                    // If the updates come from the Thread for Data Exchange
                    case MESSAGE_READ:
                        String statusText = msg.obj.toString().replace("/n","");
                        sensorStatus.setText(statusText);
                        result = sensorStatus.getText().toString().trim();
                        if(isNumeric(result)){
                            resultNum = Integer.parseInt(result);
                            double res1 = ((double)(resultNum % 10000))/1024*30;
                            res1 = Math.round(res1*100)/100.0;
                            double res2 = ((double)(resultNum / 10000))/1024*30;
                            res2 = Math.round(res2*100)/100.0;
                            gauge2.setValue(res1);
                            gauge.setValue(res2);
                            sensorStatus.setText(String.valueOf(res2));
                        }
                        else{
                            gauge.setValue(0);
                            gauge2.setValue(0);
                        }
                        break;
                }
            }
        };

        // Code for the disconnect button
        buttonDisconnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (createConnectThread != null){
                    createConnectThread.cancel();
                    bluetoothStatus.setText("Bluetooth is Disconnected");
                }
            }
        });

        // Code to turn ON LED
        buttonOn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String androidCmd = "w";
                connectedThread.write(androidCmd);
            }
        });

        // Code to turn OFF LED
        buttonOff.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String androidCmd = "s";
                connectedThread.write(androidCmd);
            }
        });

        // Code to make the LED blinking
        buttonTest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                contTest = false;
                testLeakageThread = new TestLeakageThread();
                testLeakageThread.start();
            }
        });

        rDatabase = FirebaseDatabase.getInstance().getReference().child("vehicles");
        saveSemi.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String sPlate = plateSemi.getText().toString();

                /*addVehicleThread add1 = new addVehicleThread(sPlate, 0, isSemiLeaking);
                addVehicleThread add2 = new addVehicleThread(tPlate, 1, isTrailerLeaking);
                add1.start();
                add2.start();*/

                addVehicle(sPlate, 0, isSemiLeaking);

            }
        });
        saveTrailer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String sPlate = plateSemi.getText().toString();
                String tPlate = plateTrailer.getText().toString();

                /*addVehicleThread add1 = new addVehicleThread(sPlate, 0, isSemiLeaking);
                addVehicleThread add2 = new addVehicleThread(tPlate, 1, isTrailerLeaking);
                add1.start();
                add2.start();*/

                addVehicle(sPlate, 0, isSemiLeaking);
                addVehicle(tPlate, 1, isTrailerLeaking);

            }
        });

        contButton.getLayoutParams();
    }

    /* ============================ Thread to Create Connection ================================= */
    public static class CreateConnectThread extends Thread {

        @SuppressLint("MissingPermission")
        public CreateConnectThread(BluetoothAdapter bluetoothAdapter, String address) {
            // Opening connection socket with the Arduino board
            BluetoothDevice bluetoothDevice = bluetoothAdapter.getRemoteDevice(address);
            BluetoothSocket tmp = null;
            UUID uuid;
            uuid = bluetoothDevice.getUuids()[0].getUuid();
            try {
                // Get a BluetoothSocket to connect with the given BluetoothDevice.
                // MY_UUID is the app's UUID string, also used in the server code.
                tmp = bluetoothDevice.createInsecureRfcommSocketToServiceRecord(uuid);
            } catch (IOException e) {
                Log.e(TAG, "Socket's create() method failed", e);
            }
            mmSocket = tmp;
        }

        @SuppressLint("MissingPermission")
        public void run() {
            // Cancel discovery because it otherwise slows down the connection.
            BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            bluetoothAdapter.cancelDiscovery();
            try {
                // Connect to the Arduino board through the socket. This call blocks
                // until it succeeds or throws an exception.
                mmSocket.connect();
                handler.obtainMessage(CONNECTION_STATUS, 1, -1).sendToTarget();
            } catch (IOException connectException) {
                // Unable to connect; close the socket and return.
                try {
                    mmSocket.close();
                    handler.obtainMessage(CONNECTION_STATUS, -1, -1).sendToTarget();
                } catch (IOException closeException) { }
                return;
            }

            // The connection attempt succeeded. Perform work associated with
            // the connection in a separate thread.
            // Calling for the Thread for Data Exchange (see below)
            connectedThread = new ConnectedThread(mmSocket);
            connectedThread.start();
        }

        // Closes the client socket and causes the thread to finish.
        // Disconnect from Arduino board
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) { }
        }
    }

    /* =============================== Thread for Data Exchange ================================= */
    public static class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        // Getting Input and Output Stream when connected to Arduino Board
        public ConnectedThread(BluetoothSocket socket) {
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) { }
            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        // Read message from Arduino device and send it to handler in the Main Thread
        public void run() {
            byte[] buffer = new byte[1024];  // buffer store for the stream
            int bytes = 0; // bytes returned from read()
            // Keep listening to the InputStream until an exception occurs
            while (true) {
                try {
                    // Read from the InputStream
                    buffer[bytes] = (byte) mmInStream.read();
                    String arduinoMsg = null;

                    // Parsing the incoming data stream
                    if (buffer[bytes] == '\n'){
                        arduinoMsg = new String(buffer,0,bytes);
                        Log.e("Arduino Message",arduinoMsg);
                        handler.obtainMessage(MESSAGE_READ,arduinoMsg).sendToTarget();
                        bytes = 0;
                    } else {
                        bytes++;
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    break;
                }
            }
        }

        // Send command to Arduino Board
        // This method must be called from Main Thread
        public void write(String input) {
            byte[] bytes = input.getBytes(); //converts entered String into bytes
            try {
                mmOutStream.write(bytes);
            } catch (IOException e) { }
        }
    }

    /* =============================== Thread for Leakage Test ================================= */
    public class TestLeakageThread extends Thread {

        TestLeakageThread() {

        }

        public void run() {

            int i = 0;
            String resultText = null, resultText2;
            double finalBoth, finalSemi, finalTrailer;

            pres1.setText("");
            pres2.setText("");
            pres3.setText("");
            presT1.setText("");
            presT2.setText("");
            presT3.setText("");
            leakage.setText("");
            leakageSemi.setText("");
            final boolean[] contTest = {false};

            for(i=0; i<3; ++i){
                connectedThread.write("w");
                try {
                    this.sleep(500);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }

            firstValue = gauge.getValue();
            fourthValue = gauge.getValue();
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    pres1.setText(String.valueOf(firstValue));
                    presT1.setText(String.valueOf(fourthValue));
                }
            });
            TimeCountdownThread timeCountdownThread = new TimeCountdownThread();
            timeCountdownThread.start();
            try {
                Thread.sleep(60000, 1);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            secondValue = gauge.getValue();
            fifthValue = gauge2.getValue();
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    pres2.setText(String.valueOf(secondValue));
                    presT2.setText(String.valueOf(fifthValue));
                }
            });
            try {
                Thread.sleep(180000, 1);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            thirdValue = gauge.getValue();
            sixthValue = gauge2.getValue();
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    pres3.setText(String.valueOf(thirdValue));
                    presT3.setText(String.valueOf(sixthValue));
                }
            });

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    double leak = Math.round((firstValue - thirdValue + fourthValue - sixthValue)*100)/100.0;
                    leakage.setText(String.valueOf(leak));
                }
            });
            finalBoth = thirdValue + sixthValue;

            while (!contTest[0]){
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                contButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        contTest[0] = true;
                    }
                });
            }

            //Sadece çekici ölçümü

            pres1.setText("");
            pres2.setText("");
            pres3.setText("");
            presT1.setText("");
            presT2.setText("");
            presT3.setText("");

            firstValue = gauge.getValue();
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    pres1.setText(String.valueOf(firstValue));
                }
            });
            timeCountdownThread = new TimeCountdownThread();
            timeCountdownThread.start();
            try {
                Thread.sleep(60000, 1);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            secondValue = gauge.getValue();
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    pres2.setText(String.valueOf(secondValue));
                }
            });
            try {
                Thread.sleep(180000, 1);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            thirdValue = gauge.getValue();

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    double leak = Math.round((firstValue - thirdValue)*100)/100.0;
                    leakageSemi.setText(String.valueOf(leak));
                }
            });
            finalSemi = thirdValue;
            finalTrailer = finalBoth - finalSemi;

            if(finalBoth < (firstValue + thirdValue)*0.95){
                if (finalSemi < firstValue*0.95){
                    resultText = "Semi is leaking!\n";
                    isSemiLeaking = true;
                }
                if(finalTrailer < fourthValue*0.95){
                    resultText = resultText + " Trailer is leaking!";
                    isTrailerLeaking = true;
                }
            }
            else {
                resultText = "No leakage";
                isSemiLeaking = false;
                isTrailerLeaking = false;
            }
            String finalResultText = resultText;
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    res_text.setText(finalResultText);
                }
            });

            for(i=0; i<10; ++i){
                connectedThread.write("s");
                try {pres3.setText(String.valueOf(thirdValue));
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }

        }
    }

    public class TimeCountdownThread extends Thread {
        TimeCountdownThread(){}
        public void run(){
            long elapsedTime = 0;
            long startTime = System.currentTimeMillis();
            while (elapsedTime < 4*60*1000){
                elapsedTime = System.currentTimeMillis() - startTime;
                //timeGauge.setValue(elapsedTime);
                double elapsedTimeMinutes = TimeUnit.MILLISECONDS.toMinutes(elapsedTime) + (double)(TimeUnit.MILLISECONDS.toSeconds(elapsedTime)%60)/100;
                elapsedTimeMinutes = Math.round(elapsedTimeMinutes*100)/100.0;
                timeGauge.setValue(elapsedTimeMinutes);
            }
        }
    }

    public static boolean isNumeric(String strNum) {
        if (strNum == null) {
            return false;
        }
        try {
            double d = Double.parseDouble(strNum);
        } catch (NumberFormatException nfe) {
            return false;
        }
        return true;
    }
    /*public class addVehicleThread extends Thread {
        private String plate;
        private int vehicleType;
        private boolean isLeaking;
        addVehicleThread(String plate, int vehicleType, boolean isLeaking){
            this.plate = plate;
            this.vehicleType = vehicleType;
            this.isLeaking = isLeaking;
        }
        public void run(){
            rDatabase2 = rDatabase.child(plate);
            String id = rDatabase2.push().getKey();//getting unique id

            Vehicle vehicle = new Vehicle(plate, id, vehicleType, isLeaking);

            ValueEventListener listener = new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    rDatabase2.setValue(vehicle);
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Log.w("TAG", "loadPost:onCancelled", error.toException());
                }
            };
            rDatabase2.addValueEventListener(listener);
        }
    }*/
    public void addVehicle(String plate, int vehicleType, boolean isLeaking){
        rDatabase2 = rDatabase.child(plate);
        String id = rDatabase2.push().getKey();//getting unique id

        Vehicle vehicle = new Vehicle(plate, id, vehicleType, isLeaking);

        ValueEventListener listener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                rDatabase2.setValue(vehicle);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.w("TAG", "loadPost:onCancelled", error.toException());
            }
        };
        rDatabase2.addValueEventListener(listener);
    }
}

