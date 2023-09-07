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
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.ekn.gruzer.gaugelibrary.FullGauge;
import com.ekn.gruzer.gaugelibrary.HalfGauge;
import com.ekn.gruzer.gaugelibrary.Range;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class TestBothActivity extends AppCompatActivity {

    private String deviceAddress = null;
    public static Handler handler;
    public static BluetoothSocket mmSocket;
    public static TestSemiActivity.ConnectedThread connectedThread;
    public static TestSemiActivity.CreateConnectThread createConnectThread;

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
        Button buttonTest = findViewById(R.id.buttonTestLeakage);
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
            createConnectThread = new TestSemiActivity.CreateConnectThread(bluetoothAdapter,deviceAddress);
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
                testLeakageThread = new TestLeakageThread();
                testLeakageThread.start();
            }
        });
    }

    /* =============================== Thread for Leakage Test ================================= */
    public class TestLeakageThread extends Thread {

        TestLeakageThread() {

        }

        public void run() {

            int i = 0;
            String resultText, resultText2;
            double finalBoth, finalSemi, finalTrailer;

            pres1.setText("");
            pres2.setText("");
            pres3.setText("");
            presT1.setText("");
            presT2.setText("");
            presT3.setText("");
            leakage.setText("");
            leakageSemi.setText("");

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
            finalBoth = firstValue - thirdValue + fourthValue - sixthValue;

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
            finalSemi = firstValue - thirdValue;
            finalTrailer = finalBoth - finalSemi;

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
}

