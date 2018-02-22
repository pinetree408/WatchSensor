package com.pinetree408.research.watchsensor;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.AsyncTask;
import android.support.annotation.Nullable;
import android.support.wearable.activity.WearableActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.net.URISyntaxException;

import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;

public class MainActivity extends WearableActivity implements SensorEventListener {

    private SensorManager mSensorManager;
    private Sensor mAccel;
    private Sensor mGyro;
    private Sensor mLinear;

    private TextView mTextView;
    private Button mButton;

    private int modeFlag = 0;

    Socket socket;

    String ip = "143.248.197.102";
    int port = 5000;

    public class MyAsyncTask extends AsyncTask<String, Void, String> {

        @Override
        protected void onPreExecute() {
        }

        @Override
        protected String doInBackground(String... params) {
            socket.emit("request", params[0]);
            return "done";
        }

        @Override
        protected void onPostExecute(String result) {
        }

        @Override
        protected void onCancelled() {
        }

    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setAmbientEnabled();

        try {
            socket = IO.socket("http://" + ip + ":" + port + "/mynamespace");
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }

        socket.on(Socket.EVENT_CONNECT, new Emitter.Listener() {

            @Override
            public void call(Object... args) {
                Log.d("Socket", "connect");
            }

        }).on("response", new Emitter.Listener() {

            @Override
            public void call(Object... args) {
            }

        }).on(Socket.EVENT_DISCONNECT, new Emitter.Listener() {

            @Override
            public void call(Object... args) {}

        });
        socket.connect();

        mTextView = findViewById(R.id.textView);
        mButton = findViewById(R.id.button1);
        mButton.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
                if (modeFlag == 0) {
                    mTextView.setText("Recording");
                    modeFlag = 1;
                } else {
                    mTextView.setText("Ready");
                    modeFlag = 0;
                    finish();
                    System.exit(0);
                }
            }
        });

        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mAccel = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mGyro = mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        mLinear = mSensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mSensorManager.registerListener(this, mAccel, SensorManager.SENSOR_DELAY_FASTEST);
        mSensorManager.registerListener(this, mGyro, SensorManager.SENSOR_DELAY_FASTEST);
        mSensorManager.registerListener(this, mLinear, SensorManager.SENSOR_DELAY_FASTEST);
    }

    @Override
    protected void onPause() {
        super.onPause();
        mSensorManager.unregisterListener(this);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (modeFlag == 1) {
            /*
            new MyAsyncTask().execute(
                    Integer.toString(event.sensor.getType()) + "," +
                    event.timestamp + "," +
                    Float.toString(event.values[0]) + "," +
                    Float.toString(event.values[1]) + "," +
                    Float.toString(event.values[2])
            );
            */
            socket.emit("request",
                    Integer.toString(event.sensor.getType()) + "," +
                    event.timestamp + "," +
                    Float.toString(event.values[0]) + "," +
                    Float.toString(event.values[1]) + "," +
                    Float.toString(event.values[2]));
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }
}
