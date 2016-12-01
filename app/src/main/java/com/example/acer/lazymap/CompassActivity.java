package com.example.acer.lazymap;

import android.app.Activity;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.view.animation.AccelerateInterpolator;

public class CompassActivity extends Activity implements SensorEventListener {

    private SensorManager mSensorManager;

    Sensor mOrientationSensor;
    private final float MAX_ROTA_DEGREE = 1.0f;
    private AccelerateInterpolator mInterpolator;


    private boolean mStopDrawing;

    private float mDirection;
    private float mTargetDirection;
    private float mRoll, mTargetRoll;
    private float mPitch, mTargetPitch;
    protected final Handler mHandler = new Handler();

    CompassView compassView;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mDirection = 0.0f;
        mTargetDirection = 0.0f;
        mStopDrawing = true;

        setContentView(R.layout.compass_main);
        compassView = (CompassView) findViewById(R.id.compass);

        mInterpolator = new AccelerateInterpolator();

        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mOrientationSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION);


    }

    @Override
    protected void onResume() {
        super.onResume();

        mSensorManager.registerListener(this, mOrientationSensor, SensorManager.SENSOR_DELAY_UI);


        mStopDrawing = false;
        mHandler.postDelayed(mCompassViewUpdater, 20);


    }


    @Override
    protected void onPause() {
        super.onPause();
        mStopDrawing = true;

        mSensorManager.unregisterListener(this, mOrientationSensor);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {

        float direction = event.values[0] * -1.0f;
        mTargetDirection = normalizeDegree(direction);

        mTargetPitch = event.values[1];
        mTargetRoll = event.values[2];
        // Azimuth Pitch  Roll
//        String sensorValue = String.format("x = %f, y=%f, z=%f", mTargetDirection, mPitch, mRoll);
//        Log.d("sensorValue", "sensorValue = " + sensorValue);
    }


    private float normalizeDegree(float degree) {
        return (degree + 720) % 360;
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    protected Runnable mCompassViewUpdater = new Runnable() {
        @Override
        public void run() {
            if (compassView != null && !mStopDrawing) {
//                Log.d("mPitch", "" + mPitch);
//                Log.d("mRoll", "" + mRoll);
                if (mDirection != mTargetDirection || mTargetRoll != mRoll || mTargetPitch != mTargetPitch) {

                    // calculate the short routine
                    float to = mTargetDirection;
                    if (to - mDirection > 180) {
                        to -= 360;
                    } else if (to - mDirection < -180) {
                        to += 360;
                    }

                    // limit the max speed to MAX_ROTATE_DEGREE
                    float distance = to - mDirection;
                    if (Math.abs(distance) > MAX_ROTA_DEGREE) {
                        distance = distance > 0 ? MAX_ROTA_DEGREE : (-1.0f * MAX_ROTA_DEGREE);
                    }

                    // need to slow down if the distance is short
                    mDirection = normalizeDegree(mDirection + ((to - mDirection) * mInterpolator.getInterpolation(Math.abs(distance) > MAX_ROTA_DEGREE ? 0.4f : 0.3f)));


                    to = mTargetPitch;
                    distance = to - mPitch;
                    if (Math.abs(distance) > MAX_ROTA_DEGREE) {
                        distance = distance > 0 ? MAX_ROTA_DEGREE : (-1.0f * MAX_ROTA_DEGREE);
                    }
                    mPitch = mPitch + ((to - mPitch) * mInterpolator.getInterpolation(Math.abs(distance) > MAX_ROTA_DEGREE ? 0.4f : 0.3f));


                    to = mTargetRoll;
                    distance = to - mRoll;
                    if (Math.abs(distance) > MAX_ROTA_DEGREE) {
                        distance = distance > 0 ? MAX_ROTA_DEGREE : (-1.0f * MAX_ROTA_DEGREE);
                    }
                    mRoll = mRoll + ((to - mRoll) * mInterpolator.getInterpolation(Math.abs(distance) > MAX_ROTA_DEGREE ? 0.4f : 0.3f));


                    compassView.updateDegree(mDirection, mPitch, mRoll);
                }

                mHandler.postDelayed(mCompassViewUpdater, 20);
            }
        }
    };


}
