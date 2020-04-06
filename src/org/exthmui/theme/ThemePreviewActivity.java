/*
 * Copyright (C) 2019-2020 The exTHmUI Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.exthmui.theme;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

import org.exthmui.theme.fragments.ThemeApplyingDialog;
import org.exthmui.theme.fragments.ThemePreviewFragment;
import org.exthmui.theme.interfaces.ThemePreviewInterface;
import org.exthmui.theme.misc.Constants;
import org.exthmui.theme.models.ThemeItem;
import org.exthmui.theme.services.ThemeDataService;
import org.exthmui.theme.services.ThemeManageService;

import java.util.List;

public class ThemePreviewActivity extends FragmentActivity implements ThemePreviewInterface {

    private static final String TAG = "ThemePreviewActivity";

    private ThemeDataService.ThemeDataBinder mThemeDataBinder;
    private ThemeDataConn mThemeDataConn;
    private ThemeManageService.ThemeManageBinder mThemeManageBinder;
    private ThemeManageConn mThemeManageConn;

    private String mThemePackageName;
    private ThemePreviewFragment mFragment;
    private ThemeApplyingDialog mApplyingDialog;
    private ThemeItem mThemeItem;

    private SharedPreferences mPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.theme_preview_activity);

        Intent intent = getIntent();
        mThemePackageName = intent.getStringExtra("theme");

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.container, ThemePreviewFragment.newInstance())
                    .commitNow();
            mApplyingDialog = new ThemeApplyingDialog();
        }

        mPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        Intent mThemeDataService = new Intent(this, ThemeDataService.class);
        mThemeDataConn = new ThemeDataConn();
        Intent mThemeManageService = new Intent(this, ThemeManageService.class);
        mThemeManageConn = new ThemeManageConn();
        bindService(mThemeDataService, mThemeDataConn, Context.BIND_AUTO_CREATE);
        bindService(mThemeManageService, mThemeManageConn, Context.BIND_AUTO_CREATE);
    }

    @Override
    public void onAttachFragment(Fragment fragment) {
        super.onAttachFragment(fragment);
        if (fragment instanceof ThemePreviewFragment) {
            mFragment = (ThemePreviewFragment) fragment;
        } else if (mApplyingDialog == null && fragment instanceof ThemeApplyingDialog) {
            mApplyingDialog = (ThemeApplyingDialog) fragment;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mThemeDataConn != null) {
            unbindService(mThemeDataConn);
        }
        if (mThemeManageConn != null) {
            try {
                mThemeManageBinder.setThemeApplyStatusListener(null);
            } catch (Exception e) {
                Log.e(TAG, "Failed to unset theme apply status listener!");
            }

            unbindService(mThemeManageConn);
        }
    }

    @Override
    public List<Drawable> getThemePreviewList(String packageName) {
        return mThemeDataBinder.getThemePreviewList(packageName);
    }

    @Override
    public Drawable getThemeBanner(String packageName) {
        return mThemeDataBinder.getThemeBanner(packageName);
    }

    @Override
    public void applyTheme(Bundle bundle) {
        mApplyingDialog.show(getSupportFragmentManager(), TAG);
        bundle.putBoolean(Constants.PREFERENCES_OVERLAY_REMOVE_FLAG, mPreferences.getBoolean(Constants.PREFERENCES_OVERLAY_REMOVE_FLAG, false));
        new Thread(() -> {
            if (mThemeItem != null) {
                mThemeManageBinder.applyTheme(mThemeItem, bundle);
            }
        }).start();
    }

    private class ThemeDataConn implements ServiceConnection {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            mThemeDataBinder = (ThemeDataService.ThemeDataBinder) iBinder;
            if (mThemeDataBinder.isThemePackage(mThemePackageName)) {
                mThemeItem = mThemeDataBinder.getThemeItem(mThemePackageName);
                mFragment.setThemeItem(mThemeItem);
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
        }
    }

    private class ThemeManageConn implements ServiceConnection {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            mThemeManageBinder = (ThemeManageService.ThemeManageBinder) iBinder;
            mThemeManageBinder.setThemeApplyStatusListener(data -> {
                if (mFragment == null || mApplyingDialog == null) return;
                sendBroadcast(data);
                runOnUiThread(() -> mApplyingDialog.updateData(data));
            });
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
        }
    }
}
