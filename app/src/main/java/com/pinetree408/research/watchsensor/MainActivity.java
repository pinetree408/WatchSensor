package com.pinetree408.research.watchsensor;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.wearable.activity.WearableActivity;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

import org.json.JSONObject;

import java.net.URISyntaxException;
import java.util.ArrayList;

import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;

import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends WearableActivity implements SensorEventListener {

    private SensorManager mSensorManager;
    private Sensor mAccel;
    private Sensor mGyro;
    private Sensor mLinear;

    private TextView mTargetView;
    private TextView mResultView;
    private TextView mRecordFlagView;
    private Button mButton;

    private int modeFlag = 0;

    Socket socket;

    String ip = "143.248.56.249";
    int port = 5000;

    int targetIndex;
    ArrayList<String> targetList = new ArrayList<>();

    Timer mTimer = new Timer();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        targetIndex = 0;
        for (int j = 0; j < 2; j++)
        {
            String tempTarget = (j == 0) ? "Pinch" : "Wave";
            for (int i = 0; i < 10; i++)
            {
                targetList.add(tempTarget);
            }
        }

        IO.Options opts = new IO.Options();
        opts.forceNew = true;

        try {
            socket = IO.socket("http://" + ip + ":" + port + "/mynamespace", opts);
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }

        socket.on(Socket.EVENT_CONNECT, new Emitter.Listener() {

            @Override
            public void call(Object... args) {
                modeFlag = 1;
                Log.d("Socket", "connect");
                socket.emit("start");
                mTimer.scheduleAtFixedRate(new TimerTask() {
                    public void run() {
                        mHandler.obtainMessage(1).sendToTarget();
                    }
                }, 0, 3000);
            }

        }).on("response", new Emitter.Listener() {

            @Override
            public void call(Object... args) {
                try {
                    final JSONObject response = (JSONObject) args[0];
                    if (response.get("type").toString().equals("Predicted"))
                    {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    if (response.get("data").toString().equals("1")){
                                        mResultView.setText("Pinch");
                                    } else if (response.get("data").toString().equals("2")){
                                        mResultView.setText("Wave");
                                    } else {
                                        mResultView.setText("Nothing");
                                    }
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                        });
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

        }).on(Socket.EVENT_DISCONNECT, new Emitter.Listener() {

            @Override
            public void call(Object... args) {
                Log.d("Socket", "disconnect");
            }

        });

        mTargetView = findViewById(R.id.targetView);
        mResultView = findViewById(R.id.resultView);
        mRecordFlagView = findViewById(R.id.recordFlagView);
        mButton = findViewById(R.id.recordButton);
        mButton.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
                if (modeFlag == 0) {
                    socket.connect();
                    mRecordFlagView.setText("Recording");
                } else {
                    socket.emit("done");
                    mRecordFlagView.setText("Ready");
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

    public Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            if (targetIndex < 20){
                mTargetView.setText(Integer.toString(targetIndex + 1) + " " + targetList.get(targetIndex));
                targetIndex += 1;
            }
            else
            {
                mTargetView.setText("Done");
            }
        }
    };

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
    public void onSensorChanged(final SensorEvent event) {
        if (modeFlag == 1) {
            socket.emit("request",
                    Integer.toString(event.sensor.getType()),
                    event.timestamp / 1000000.0,
                    Float.toString(event.values[0]),
                    Float.toString(event.values[1]),
                    Float.toString(event.values[2]));
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        socket.disconnect();
        mTimer.cancel();
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}
}
