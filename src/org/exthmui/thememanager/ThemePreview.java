/*
 * Copyright (C) 2019-2020 The exTHmUI Open Source Project
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

package org.exthmui.thememanager;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.IBinder;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;

import org.exthmui.thememanager.models.OverlayTarget;
import org.exthmui.thememanager.models.Theme;
import org.exthmui.thememanager.services.ThemeDataService;
import org.exthmui.thememanager.services.ThemeManageService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ThemePreview extends Activity {

    private Theme mTheme;

    private Intent mThemeDataService;
    private ThemeDataService.ThemeDataBinder mThemeDataBinder;
    private ThemeDataConn mThemeDataConn;
    private boolean themeDataServiceBindFlag = false;
    private ThemeDataService.ThemeDataUpdateListener themeDataUpdateListener;
    private Intent mThemeManageService;
    private ThemeManageService.ThemeManageBinder mThemeManageBinder;
    private ThemeManageConn mThemeManageConn;
    private boolean themeManageServiceBindFlag = false;

    private LinearLayout appLayout;
    private LinearLayout previewLayout;
    private LinearLayout previewPicLayout;
    private LinearLayout soundLayout;
    private LinearLayout wallpaperLayout;

    private TextView tvTitle;
    private TextView tvAuthor;
    private ImageView imageBanner;
    private ImageView imagePreviewViewer;

    private Button btnApply;
    private ImageButton btnBack;

    private HashMap<String, Boolean> switchStatus;
    private int themeIndex;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_theme_preview);

        Intent intent = getIntent();

        mThemeManageService = new Intent(this, ThemeManageService.class);
        mThemeManageConn = new ThemeManageConn();
        mThemeDataService = new Intent(this, ThemeDataService.class);
        mThemeDataConn = new ThemeDataConn();

        switchStatus = new HashMap<>();
        themeIndex = intent.getIntExtra("themeIndex",0);

        appLayout = findViewById(R.id.app_container);
        previewLayout = findViewById(R.id.layout_previews);
        previewPicLayout = findViewById(R.id.layout_preview_pic);
        soundLayout = findViewById(R.id.sound_container);
        wallpaperLayout = findViewById(R.id.wallpaper_container);

        tvTitle = findViewById(R.id.theme_title);
        tvAuthor = findViewById(R.id.theme_author);
        imageBanner = findViewById(R.id.banner_image);
        imagePreviewViewer = findViewById(R.id.preview_viewer);

        btnApply = findViewById(R.id.apply_theme_button);
        btnBack = findViewById(R.id.button_back);

        btnApply.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new Thread(new Runnable() {
                    public void run() {
                        mThemeDataBinder.setIsApplying(true);
                        if (applyTheme()) {
                            mThemeDataBinder.setActedPackage(mTheme.getPackageName());
                        }
                        mThemeDataBinder.setIsApplying(false);
                        finish();
                    }
                }).start();
            }
        });

        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

        imagePreviewViewer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                imagePreviewViewer.setVisibility(View.GONE);
            }
        });

        themeDataUpdateListener = new ThemeDataService.ThemeDataUpdateListener() {
            @Override
            public void onActedPackageChangeListener(String val) {
            }

            @Override
            public void onApplyingListener(boolean val) {
                runOnUiThread(new Runnable() {
                    public void run() {
                        btnApply.setEnabled(!val);
                    }
                });
            }

            @Override
            public void onThemeListChangedListener(int index, String act, Theme val) {
            }
        };

        bindService(mThemeManageService, mThemeManageConn, Context.BIND_AUTO_CREATE);

    }

    private void updateView() {
        // base info
        tvTitle.setText(mTheme.getName());
        tvAuthor.setText(mTheme.getAuthor());
        imageBanner.setImageDrawable(mThemeManageBinder.getThemeBanner(mTheme.getPackageName()));

        switchStatus.put("wallpaper", false);
        switchStatus.put("lockscreen", false);

        switchStatus.put("ringtone", false);
        switchStatus.put("alarm", false);
        switchStatus.put("notification", false);

        btnApply.setEnabled(!mThemeDataBinder.getIsApplying());

        // wallpaper
        if (mTheme.hasWallpaper()) {
            addSwitch(wallpaperLayout, "wallpaper", R.string.skin_picker_switch_wallpaper, true);
        }
        if (mTheme.hasLockScreen()) {
            addSwitch(wallpaperLayout, "lockscreen", R.string.skin_picker_switch_lockscreen, true);
        }

        if (!mTheme.hasWallpaper() && !mTheme.hasLockScreen()) {
            wallpaperLayout.setVisibility(View.GONE);
        }

        // sound
        if (mTheme.hasRingtone()) {
            addSwitch(soundLayout, "ringtone", R.string.skin_picker_switch_ringtone, true);
        }
        if (mTheme.hasAlarmSound()) {
            addSwitch(soundLayout, "alarm", R.string.skin_picker_switch_alarm, true);
        }
        if (mTheme.hasNotificationSound()) {
            addSwitch(soundLayout, "notification", R.string.skin_picker_switch_notification, true);
        }

        if (!mTheme.hasRingtone() && !mTheme.hasAlarmSound() && !mTheme.hasNotificationSound()) {
            soundLayout.setVisibility(View.GONE);
        }

        // apps
        if (mTheme.hasOverlays()) {
            for (OverlayTarget overlayTarget : mTheme.getOverlayTargets()) {
                addSwitch(appLayout, overlayTarget.getPackageName(), overlayTarget.getLabel(), true);
            }
        } else {
            appLayout.setVisibility(View.GONE);
        }

        // previews
        List<Drawable> previewList = mThemeManageBinder.getThemePreviewList(mTheme.getPackageName());
        if (!previewList.isEmpty()) {
            for (Drawable drawable : previewList) {
                addPreview(drawable);
            }
        } else {
            previewLayout.setVisibility(View.GONE);
        }

    }

    private boolean applyTheme() {
        Bundle bundle = new Bundle();

        bundle.putString("package", mTheme.getPackageName());

        bundle.putBoolean("ringtone", switchStatus.get("ringtone"));
        bundle.putBoolean("alarm", switchStatus.get("alarm"));
        bundle.putBoolean("notification", switchStatus.get("notification"));

        bundle.putBoolean("wallpaper", switchStatus.get("wallpaper"));
        bundle.putBoolean("lockscreen", switchStatus.get("lockscreen"));

        ArrayList<String> whiteList = new ArrayList<>();

        for (Map.Entry<String, Boolean> entry : switchStatus.entrySet()) {
            String key = entry.getKey();
            if (!entry.getValue() && !key.equals("wallpaper") && !key.equals("lockscreen")
                    && !key.equals("ringtone") && !key.equals("alarm") && !key.equals("notification")) {
                whiteList.add(key);
            }
        }
        bundle.putStringArrayList("whitelist", whiteList);

        mThemeManageBinder.removeThemeOverlays(bundle);
        return mThemeManageBinder.applyTheme(bundle);
    }

    private void addSwitch(LinearLayout layout, final String id, int text, boolean defaultValue) {
        addSwitch(layout, id, getString(text), defaultValue);
    }

    private void addSwitch(LinearLayout layout, final String id, String text, boolean defaultValue) {
        int paddingTop = getResources().getDimensionPixelOffset(R.dimen.picker_switch_padding_top);
        int paddingBottom = getResources().getDimensionPixelOffset(R.dimen.picker_switch_padding_bottom);
        int paddingLeft = getResources().getDimensionPixelOffset(R.dimen.picker_switch_padding_left);
        int paddingRight = getResources().getDimensionPixelOffset(R.dimen.picker_switch_padding_right);

        Switch tmpSwitch = new Switch(this);

        tmpSwitch.setText(text);
        tmpSwitch.setChecked(defaultValue);
        tmpSwitch.setPadding(paddingLeft, paddingTop, paddingRight, paddingBottom);
        switchStatus.put(id, true);

        tmpSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                switchStatus.put(id, b);
            }
        });

        layout.addView(tmpSwitch);
    }

    private void addPreview(Drawable drawable) {

        int imageHeight = getResources().getDimensionPixelOffset(R.dimen.preview_image_height);

        ImageView imageView = new ImageView(this);
        imageView.setImageDrawable(drawable);

        imageView.setMaxHeight(imageHeight);
        imageView.setAdjustViewBounds(true);

        imageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                imagePreviewViewer.setImageDrawable(imageView.getDrawable());
                imagePreviewViewer.setVisibility(View.VISIBLE);
            }
        });

        previewPicLayout.addView(imageView);

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (themeManageServiceBindFlag) unbindService(mThemeManageConn);
        if (themeDataServiceBindFlag) unbindService(mThemeDataConn);
    }

    private class ThemeManageConn implements ServiceConnection {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            mThemeManageBinder = (ThemeManageService.ThemeManageBinder) iBinder;
            themeManageServiceBindFlag = true;
            bindService(mThemeDataService, mThemeDataConn, Context.BIND_AUTO_CREATE);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
        }
    }

    private class ThemeDataConn implements ServiceConnection{
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            mThemeDataBinder = (ThemeDataService.ThemeDataBinder) iBinder;
            mTheme = mThemeDataBinder.getThemeList().get(themeIndex);
            mThemeDataBinder.setDataUpdateListener(themeDataUpdateListener);
            themeDataServiceBindFlag = true;
            updateView();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mThemeDataBinder.removeDataUpdateListener(themeDataUpdateListener);
        }
    }
}
