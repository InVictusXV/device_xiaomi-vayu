/*
 * Copyright (C) 2020 The LineageOS Project
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

package org.lineageos.settings.thermal;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.RemoteException;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.view.Display;
import android.view.Surface;
import android.view.WindowManager;

import androidx.preference.PreferenceManager;

import java.util.NoSuchElementException;

import vendor.xiaomi.hardware.touchfeature.V1_0.ITouchFeature;

public final class ThermalUtils {

    protected static final int STATE_DEFAULT = 0;
    protected static final int STATE_GAME = 1;

    private static final String THERMAL_PROP = "vendor.thermal.active";

    private static final String THERMAL_STATE_DEFAULT = "normal";
    private static final String THERMAL_STATE_GAME = "game";

    private static final String KEY_THERMAL_GAME_PREF = "thermal_mode_game";

    private boolean mTouchModeChanged;
    private String mCurrentState = THERMAL_STATE_DEFAULT;

    private Display mDisplay;
    private ITouchFeature mTouchFeature = null;
    private SharedPreferences mSharedPrefs;

    protected ThermalUtils(Context context) {
        mSharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);

        WindowManager mWindowManager = context.getSystemService(WindowManager.class);
        mDisplay = mWindowManager.getDefaultDisplay();

        try {
            mTouchFeature = ITouchFeature.getService();
        } catch (RemoteException e) {
            // Do nothing
        } catch (NoSuchElementException e) {
            // Do nothing
        }

    }

    public static void startService(Context context) {
        context.startServiceAsUser(new Intent(context, ThermalService.class),
                UserHandle.CURRENT);
    }

    private String getThermalGamePref() {
        return mSharedPrefs.getString(KEY_THERMAL_GAME_PREF, "");
    }

    protected void writePackage(String packageName, int mode) {
        String value = getThermalGamePref().replace(packageName + ",", "");
        if (mode == STATE_GAME) {
            value = value + packageName + ",";
        }
        mSharedPrefs.edit().putString(KEY_THERMAL_GAME_PREF, value).apply();
    }

    protected int getStateForPackage(String packageName) {
        int state = STATE_DEFAULT;
        if (getThermalGamePref().contains(packageName + ",")) {
            state = STATE_GAME;
        }

        return state;
    }

    private void setThermalState(String state) {
        if (mCurrentState.equals(state))
            return;

        SystemProperties.set(THERMAL_PROP, state);
        mCurrentState = state;
    }

    protected void setDefaultThermalProfile() {
        setThermalState(THERMAL_STATE_DEFAULT);
    }

    protected void setThermalProfile(String packageName) {
        boolean isGame = getThermalGamePref().contains(packageName + ",");

        setThermalState(isGame ? THERMAL_STATE_GAME : THERMAL_STATE_DEFAULT);

        if (isGame) {
            updateTouchModes(packageName);
        } else if (mTouchModeChanged) {
            resetTouchModes();
        }
    }

    protected void setThermalProfileForce(int mode) {
        setThermalState(mode == STATE_GAME ? THERMAL_STATE_GAME : THERMAL_STATE_DEFAULT);
    }

    private void updateTouchModes(String packageName) {
        String values = mSharedPrefs.getString(packageName, null);
        resetTouchModes();

        if (values == null || values.isEmpty()) {
            return;
        }

        String[] value = values.split(",");
        int gameMode = Integer.parseInt(value[Constants.TOUCH_GAME_MODE]);
        int touchResponse = Integer.parseInt(value[Constants.TOUCH_RESPONSE]);
        int touchSensitivity = Integer.parseInt(value[Constants.TOUCH_SENSITIVITY]);
        int touchResistant = Integer.parseInt(value[Constants.TOUCH_RESISTANT]);
        int touchActiveMode = (touchResponse != 0 && touchSensitivity != 0 && touchResistant != 0)
                ? 1 : 0;
        try {
            mTouchFeature.setTouchMode(Constants.MODE_TOUCH_TOLERANCE, touchSensitivity);
            mTouchFeature.setTouchMode(Constants.MODE_TOUCH_UP_THRESHOLD, touchResponse);
            mTouchFeature.setTouchMode(Constants.MODE_TOUCH_EDGE_FILTER, touchResistant);
            mTouchFeature.setTouchMode(Constants.MODE_TOUCH_GAME_MODE, gameMode);
            mTouchFeature.setTouchMode(Constants.MODE_TOUCH_ACTIVE_MODE, touchActiveMode);
        } catch (RemoteException e) {
            // Do nothing
        }

        mTouchModeChanged = true;
        updateTouchRotation();
    }

    protected void resetTouchModes() {
        if (!mTouchModeChanged) {
            return;
        }

        try {
            mTouchFeature.resetTouchMode(Constants.MODE_TOUCH_GAME_MODE);
            mTouchFeature.resetTouchMode(Constants.MODE_TOUCH_ACTIVE_MODE);
            mTouchFeature.resetTouchMode(Constants.MODE_TOUCH_UP_THRESHOLD);
            mTouchFeature.resetTouchMode(Constants.MODE_TOUCH_TOLERANCE);
            mTouchFeature.resetTouchMode(Constants.MODE_TOUCH_EDGE_FILTER);
            mTouchFeature.resetTouchMode(Constants.MODE_TOUCH_ROTATION);
        } catch (RemoteException e) {
            // Do nothing
        }

        mTouchModeChanged = false;
    }

    protected void updateTouchRotation() {
        if (!mTouchModeChanged) {
            return;
        }

        int touchRotation = 0;
        switch (mDisplay.getRotation()) {
            case Surface.ROTATION_0:
                touchRotation = 0;
                break;
            case Surface.ROTATION_90:
                touchRotation = 1;
                break;
            case Surface.ROTATION_180:
                touchRotation = 2;
                break;
            case Surface.ROTATION_270:
                touchRotation = 3;
                break;
        }

        try {
            mTouchFeature.setTouchMode(Constants.MODE_TOUCH_ROTATION, touchRotation);
        } catch (RemoteException e) {
            // Do nothing
        }
    }
}
