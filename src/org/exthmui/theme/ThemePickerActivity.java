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
import android.os.Bundle;
import android.os.IBinder;
import android.os.UserHandle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.snackbar.Snackbar;

import org.exthmui.theme.adapters.ThemeBaseAdapter;
import org.exthmui.theme.misc.Constants;
import org.exthmui.theme.models.ThemeBase;
import org.exthmui.theme.services.ThemeDataService;
import org.exthmui.theme.services.ThemeManageService;
import org.exthmui.theme.utils.NotificationUtil;
import org.exthmui.theme.utils.PackageUtil;
import org.exthmui.theme.utils.PermissionUtil;

import java.util.ArrayList;
import java.util.List;

public class ThemePickerActivity extends AppCompatActivity {

    private static final String TAG = "ThemePickerActivity";

    private List<ThemeBase> mThemeList;
    private ThemeDataService.ThemeDataBinder mThemeDataBinder;
    private ThemeDataConn mThemeDataConn;

    private View mRefreshIconView;
    private Animation mRefreshAnimation;

    private ThemeBaseAdapter mThemeBaseAdapter;
    private RecyclerView mGridView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.theme_picker_activity);

        NotificationUtil.createNotificationChannel(this, Constants.CHANNEL_APPLY_STATUS, getString(R.string.channel_apply_status), NotificationUtil.IMPORTANCE_DEFAULT);
        PermissionUtil.verifyStoragePermission(this);
        PermissionUtil.verifyWriteSettingsPermission(this);

        mRefreshAnimation = AnimationUtils.loadAnimation(this, R.anim.rotate_anim);

        mThemeList = new ArrayList<>();
        mThemeBaseAdapter = new ThemeBaseAdapter(mThemeList);
        mThemeBaseAdapter.setOnItemClickListener(ThemePickerActivity.this::onThemeItemClick);
        mThemeBaseAdapter.setOnItemLongClickListener(ThemePickerActivity.this::onThemeItemLongClick);
        mGridView = findViewById(R.id.themesGrid);
        int cardWidth = getResources().getDimensionPixelOffset(R.dimen.theme_card_image_height) / 16 * 10;
        GridLayoutManager gridLayoutManager = new GridLayoutManager(this, getResources().getDisplayMetrics().widthPixels / cardWidth);
        mGridView.setLayoutManager(gridLayoutManager);
        mGridView.setAdapter(mThemeBaseAdapter);

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
                if (mRefreshIconView == null) mRefreshIconView = findViewById(R.id.action_refresh);
                mRefreshAnimation.setRepeatCount(Animation.INFINITE);
                mRefreshIconView.startAnimation(mRefreshAnimation);
                mRefreshIconView.setEnabled(false);
                new Thread(() -> {
                    updateThemeList();
                    runOnUiThread(() -> {
                        mThemeBaseAdapter.notifyDataSetChanged();
                        mRefreshAnimation.setRepeatCount(Animation.ABSOLUTE);
                        mRefreshIconView.setEnabled(true);
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

    @Override
    protected void onDestroy() {
        if (mThemeDataConn != null) {
            unbindService(mThemeDataConn);
        }
        super.onDestroy();
    }

    public void updateThemeList() {
        mThemeDataBinder.updateThemeList();
        mThemeList.clear();
        mThemeList.addAll(mThemeDataBinder.getThemeBaseList());
    }

    private void onThemeItemClick(View view, int position) {
        Intent intent = new Intent(this, ThemePreviewActivity.class);
        intent.putExtra("theme", mThemeList.get(position).getPackageName());
        startActivity(intent);
    }

    private boolean onThemeItemLongClick(View view, int position) {
        ThemeBase theme = mThemeList.get(position);
        if (theme.isRemovable()) return true;
        new AlertDialog.Builder(this)
            .setTitle(R.string.dialog_remove_package_title)
            .setMessage(getString(R.string.dialog_remove_package_text, theme.getTitle()))
            .setPositiveButton(android.R.string.ok, (dialogInterface, i) -> {
                dialogInterface.dismiss();
                // do uninstall
                PackageUtil.uninstallPackage(this, theme.getPackageName(), new PackageUtil.PackageInstallerCallback() {
                    @Override
                    public void onResponse(String packageName, int code) {
                        if (code == 0) {
                            mThemeList.remove(position);
                            mThemeBaseAdapter.notifyDataSetChanged();
                            Snackbar.make(mGridView, R.string.uninstall_theme_succeed, Snackbar.LENGTH_SHORT).show();
                        } else {
                            Snackbar.make(mGridView, getString(R.string.uninstall_theme_failed, code), Snackbar.LENGTH_LONG).show();
                        }
                    }
                });

            })
            .setNegativeButton(android.R.string.cancel,  (dialogInterface, i) -> dialogInterface.dismiss())
            .show();
        return true;
    }

    private class ThemeDataConn implements ServiceConnection {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            mThemeDataBinder = (ThemeDataService.ThemeDataBinder) iBinder;
            mThemeBaseAdapter.setThemeDataBinder(mThemeDataBinder);
            if (mThemeList.isEmpty()) {
                new Thread(() -> {
                    updateThemeList();
                    runOnUiThread(() -> mThemeBaseAdapter.notifyDataSetChanged());
                }).start();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mThemeDataBinder = null;
            mThemeBaseAdapter.setThemeDataBinder(null);
        }
    }

}
