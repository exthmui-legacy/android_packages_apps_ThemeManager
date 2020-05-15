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
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.IBinder;
import android.os.UserHandle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;
import android.widget.AdapterView;
import android.widget.GridView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;

import org.exthmui.theme.adapters.CommonAdapter;
import org.exthmui.theme.misc.Constants;
import org.exthmui.theme.models.ThemeBase;
import org.exthmui.theme.services.ThemeDataService;
import org.exthmui.theme.services.ThemeManageService;
import org.exthmui.theme.utils.NotificationUtil;
import org.exthmui.theme.utils.PackageUtil;
import org.exthmui.theme.utils.PermissionUtil;

import java.util.List;

public class ThemePickerActivity extends AppCompatActivity {

    private static final String TAG = "ThemePickerActivity";

    private List<ThemeBase> mThemeList;
    private ThemeDataService.ThemeDataBinder mThemeDataBinder;
    private ThemeDataConn mThemeDataConn;

    private View mRefreshIconView;
    private RotateAnimation mRefreshAnimation;

    private ThemeAdapter mThemeAdapter;
    private GridView mGridView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.theme_picker_activity);

        NotificationUtil.createNotificationChannel(this, Constants.CHANNEL_APPLY_STATUS, getString(R.string.channel_apply_status), NotificationUtil.IMPORTANCE_DEFAULT);
        PermissionUtil.verifyStoragePermission(this);
        PermissionUtil.verifyWriteSettingsPermission(this);

        mRefreshAnimation = new RotateAnimation(0, 360, Animation.RELATIVE_TO_SELF, 0.5f,
                Animation.RELATIVE_TO_SELF, 0.5f);
        mRefreshAnimation.setInterpolator(new LinearInterpolator());
        mRefreshAnimation.setDuration(1000);

        mGridView = findViewById(R.id.themesGrid);
        int cardWidth = getResources().getDimensionPixelOffset(R.dimen.theme_card_image_height) / 16 * 10;
        mGridView.setNumColumns(getResources().getDisplayMetrics().widthPixels / cardWidth);
        mGridView.setOnItemClickListener(this::onThemeItemClick);
        mGridView.setOnItemLongClickListener(this::onThemeItemLongClick);

        Intent mThemeDataService = new Intent(this, ThemeDataService.class);
        mThemeDataConn = new ThemeDataConn();
        Intent mThemeManageService = new Intent(this, ThemeManageService.class);
        startServiceAsUser(mThemeManageService, UserHandle.CURRENT_OR_SELF);
        startServiceAsUser(mThemeDataService, UserHandle.CURRENT_OR_SELF);
        bindServiceAsUser(mThemeDataService, mThemeDataConn, Context.BIND_AUTO_CREATE, UserHandle.CURRENT_OR_SELF);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_picker, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_refresh: {
                refreshAnimationStart();
                new Thread(() -> {
                    updateThemeList();
                    runOnUiThread(() -> {
                        mThemeAdapter.notifyDataSetChanged();
                        refreshAnimationStop();
                    });
                }).start();
                return true;
            }
            case R.id.action_preferences: {
                Intent intent = new Intent(this, SettingsActivity.class);
                startActivity(intent);
                return true;
            }
        }
        return super.onOptionsItemSelected(item);
    }

    private void refreshAnimationStart() {
        if (mRefreshIconView == null) {
            mRefreshIconView = findViewById(R.id.action_refresh);
        }
        if (mRefreshIconView != null) {
            mRefreshAnimation.setRepeatCount(Animation.INFINITE);
            mRefreshIconView.startAnimation(mRefreshAnimation);
            mRefreshIconView.setEnabled(false);
        }
    }

    private void refreshAnimationStop() {
        if (mRefreshIconView != null) {
            mRefreshAnimation.setRepeatCount(0);
            mRefreshIconView.setEnabled(true);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mThemeDataConn != null) {
            unbindService(mThemeDataConn);
        }
    }

    public void updateThemeList() {
        mThemeDataBinder.updateThemeList();
    }

    public void onThemeItemClick(AdapterView<?> parent, View view, int position, long id) {
        Intent intent = new Intent(this, ThemePreviewActivity.class);
        intent.putExtra("theme", mThemeList.get(position).getPackageName());
        startActivity(intent);
    }

    public boolean onThemeItemLongClick(AdapterView<?> parent, View view, int position, long id) {
        askUninstallTheme(position);
        return true;
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
                            mThemeAdapter.notifyDataSetChanged();
                        }

                        @Override
                        public void onFailure(String packageName, int code) {

                        }
                    });

                })
                .setNegativeButton(android.R.string.cancel,  (dialogInterface, i) -> dialogInterface.dismiss())
                .show();
    }

    private class ThemeDataConn implements ServiceConnection {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            mThemeDataBinder = (ThemeDataService.ThemeDataBinder) iBinder;
            new Thread(() -> {
                updateThemeList();
                mThemeList = mThemeDataBinder.getThemeBaseList();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mThemeAdapter = new ThemeAdapter(mThemeList, R.layout.theme_card);
                        mGridView.setAdapter(mThemeAdapter);
                    }
                });
            }).start();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
        }
    }


    private class ThemeAdapter extends CommonAdapter<ThemeBase> {

        public ThemeAdapter(List<ThemeBase> data, int layoutRes) {
            super(data, layoutRes);
        }

        @Override
        public void bindView(ViewHolder holder, ThemeBase obj) {
            new Thread(() -> {
                Drawable drawable =  mThemeDataBinder.getThemeImage(obj.getPackageName());
                runOnUiThread(() -> holder.setImageResource(R.id.theme_card_image, drawable));
            }).start();

            holder.setText(R.id.theme_card_title, obj.getTitle());
        }
    }
}
