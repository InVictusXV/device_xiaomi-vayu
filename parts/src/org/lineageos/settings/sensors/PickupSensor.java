/*
 * Copyright (C) 2015 The CyanogenMod Project
 *               2017-2020 The LineageOS Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.lineageos.settings.sensors;

import android.content.Context;
import android.content.res.Resources;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.SystemClock;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.util.Log;

import org.lineageos.settings.R;
import org.lineageos.settings.doze.DozeUtils;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class PickupSensor {

    private static final boolean DEBUG = false;
    private static final String TAG = "PickupSensor";

    private static final int MIN_PULSE_INTERVAL_MS = 2500;
    private static final int WAKELOCK_TIMEOUT_MS = 300;
    private static final float GYROSCOPE_X_AXIS_THRESHOLD = 1.5f;

    private final SensorManager mSensorManager;
    private final Sensor mGyroscopeSensor;
    private final Sensor mPickupSensor;
    private final Context mContext;
    private final ExecutorService mExecutorService;
    private final PowerManager mPowerManager;
    private final WakeLock mWakeLock;

    private long mEntryTimestamp;
    private boolean mUseGyroscope = false;

    public PickupSensor(Context context) {
        mContext = context;
        mSensorManager = mContext.getSystemService(SensorManager.class);
        mPickupSensor = SensorsUtils.getSensor(mSensorManager, "xiaomi.sensor.pickup");
        mGyroscopeSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE_UNCALIBRATED, false);
        mPowerManager = (PowerManager) mContext.getSystemService(Context.POWER_SERVICE);
        mWakeLock = mPowerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG);
        mExecutorService = Executors.newSingleThreadExecutor();

        try {
            mUseGyroscope = context.getResources().getBoolean(R.bool.config_dozePickupWithGyroscope);
        } catch (Resources.NotFoundException ignored) {
        }
    }

    public void runSensorAction(SensorEvent event) {
        boolean isRaiseToWake = DozeUtils.isRaiseToWakeEnabled(mContext);
        if (DEBUG) Log.d(TAG, "Got sensor event: " + event.values[0]);

        long delta = SystemClock.elapsedRealtime() - mEntryTimestamp;
        long earlyTimeout = isRaiseToWake ? 0 : MIN_PULSE_INTERVAL_MS;
        if (delta < earlyTimeout)
            return;

        mEntryTimestamp = SystemClock.elapsedRealtime();
        if (isRaiseToWake) {
            mWakeLock.acquire(WAKELOCK_TIMEOUT_MS);
            mPowerManager.wakeUp(SystemClock.uptimeMillis(),
                PowerManager.WAKE_REASON_GESTURE, TAG);
        } else {
            DozeUtils.launchDozePulse(mContext);
        }
    }

    private final SensorEventListener mPickupListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {
            if (event.values[0] == 1) {
                runSensorAction(event);
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }
    };

    private final SensorEventListener mGyroscopeListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {
            if (event.values[0] > GYROSCOPE_X_AXIS_THRESHOLD) {
                runSensorAction(event);
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }
    };

    public void enable() {
        if (DEBUG) Log.d(TAG, "Enabling");
        mExecutorService.submit(() -> {
            if (mUseGyroscope) {
                mSensorManager.registerListener(mGyroscopeListener, mGyroscopeSensor,
                        SensorManager.SENSOR_DELAY_NORMAL);
            } else {
                mSensorManager.registerListener(mPickupListener, mPickupSensor,
                        SensorManager.SENSOR_DELAY_NORMAL);
            }
            mEntryTimestamp = SystemClock.elapsedRealtime();
        });
    }

    public void disable() {
        if (DEBUG) Log.d(TAG, "Disabling");
        mExecutorService.submit(() -> {
            if (mUseGyroscope) {
                mSensorManager.unregisterListener(mGyroscopeListener, mGyroscopeSensor);
            } else {
                mSensorManager.unregisterListener(mPickupListener, mPickupSensor);
            }
        });
    }
}
