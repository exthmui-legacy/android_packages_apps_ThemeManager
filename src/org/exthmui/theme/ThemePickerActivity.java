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

import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.view.View;
import android.widget.AdapterView;

import org.exthmui.theme.fragments.ThemePickerFragment;
import org.exthmui.theme.interfaces.ThemePickerInterface;
import org.exthmui.theme.models.ThemeBase;
import org.exthmui.theme.services.ThemeDataService;
import org.exthmui.theme.services.ThemeManageService;
import org.exthmui.theme.utils.NotificationUtil;
import org.exthmui.theme.utils.PackageUtil;
import org.exthmui.theme.utils.PermissionUtil;

import java.util.List;

public class ThemePickerActivity extends FragmentActivity implements ThemePickerInterface {

    private static final String TAG = "ThemePickerActivity";

    private List<ThemeBase> mThemeList;
    private ThemeDataService.ThemeDataBinder mThemeDataBinder;
    private ThemeDataConn mThemeDataConn;
    private Intent mThemeDataService;
    private Intent mThemeManageService;
    private ThemePickerFragment mFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.theme_picker_activity);
        NotificationUtil.createNotificationChannel(this, NotificationUtil.CHANNEL_APPLY_STATUS, getString(R.string.channel_apply_status), NotificationUtil.IMPORTANCE_DEFAULT);
        PermissionUtil.verifyStoragePermission(this);
        PermissionUtil.verifyWriteSettingsPermission(this);

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.container, ThemePickerFragment.newInstance())
                    .commitNow();
        }

        mThemeDataService = new Intent(this, ThemeDataService.class);
        mThemeDataConn = new ThemeDataConn();
        mThemeManageService = new Intent(this, ThemeManageService.class);
        startService(mThemeManageService);
        startService(mThemeDataService);
        bindService(mThemeDataService, mThemeDataConn, Context.BIND_AUTO_CREATE);
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mThemeDataConn != null) {
            unbindService(mThemeDataConn);
        }
    }

    @Override
    public Drawable getThemeImage(String packageName) {
        return mThemeDataBinder.getThemeImage(packageName);
    }

    @Override
    public List<ThemeBase> getThemeList() {
        return mThemeList;
    }

    @Override
    public void updateThemeList() {
        mThemeDataBinder.updateThemeList();
    }

    @Override
    public void onThemeItemClick(AdapterView<?> parent, View view, int position, long id) {
        Intent intent = new Intent(this, ThemePreviewActivity.class);
        intent.putExtra("theme", mThemeList.get(position).getPackageName());
        startActivity(intent);
    }

    @Override
    public boolean onThemeItemLongClick(AdapterView<?> parent, View view, int position, long id) {
        askUninstallTheme(position);
        return true;
    }

    @Override
    public void onAttachFragment(Fragment fragment) {
        mFragment = (ThemePickerFragment) fragment;
    }

    private void askUninstallTheme(int index) {
        ThemeBase theme = mThemeList.get(index);
        if (theme.isRemovable()) return;
        new AlertDialog.Builder(this)
                .setTitle(R.string.dialog_remove_package_title)
                .setMessage(getString(R.string.dialog_remove_package_text, theme.getTitle()))
                .setPositiveButton(android.R.string.ok, (dialogInterface, i) -> {
                    dialogInterface.dismiss();
                    // do uninstall
                    PackageUtil.uninstallPackage(this, theme.getPackageName(), new PackageUtil.PackageInstallerCallback() {
                        @Override
                        public void onSuccess(String packageName) {
                            mThemeList.remove(index);
                            mFragment.updateAdapter();
                        }

                        @Override
                        public void onFailure(String packageName, int code) {

                        }
                    });

                })
                .setNegativeButton(android.R.string.cancel,  (dialogInterface, i) -> {
                    dialogInterface.dismiss();
                })
                .show();
    }

    private class ThemeDataConn implements ServiceConnection {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            mThemeDataBinder = (ThemeDataService.ThemeDataBinder) iBinder;
            new Thread(new Runnable() {
                @Override
                public void run() {
                    mThemeList = mThemeDataBinder.getThemeBaseList();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mFragment.updateView();
                        }
                    });
                }
            }).start();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
        }
    }
}
