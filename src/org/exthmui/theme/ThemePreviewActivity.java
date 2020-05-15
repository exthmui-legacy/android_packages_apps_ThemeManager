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
import android.os.UserHandle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;

import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.exthmui.theme.fragments.ThemeApplyingDialog;
import org.exthmui.theme.misc.Constants;
import org.exthmui.theme.models.OverlayTarget;
import org.exthmui.theme.models.ThemeItem;
import org.exthmui.theme.services.ThemeDataService;
import org.exthmui.theme.services.ThemeManageService;

import java.util.List;

public class ThemePreviewActivity extends AppCompatActivity {

    private static final String TAG = "ThemePreviewActivity";

    private ThemeDataService.ThemeDataBinder mThemeDataBinder;
    private ThemeDataConn mThemeDataConn;
    private ThemeManageService.ThemeManageBinder mThemeManageBinder;
    private ThemeManageConn mThemeManageConn;
    private ThemeManageService.ThemeApplyStatusListener mThemeApplyStatusListener;

    private String mThemePackageName;
    private ThemeApplyingDialog mApplyingDialog;
    private ThemeItem mThemeItem;
    private Bundle themeTargetBundle;

    private SharedPreferences mPreferences;

    private LinearLayout appLayout;
    private LinearLayout previewLayout;
    private LinearLayout previewPicLayout;
    private LinearLayout soundLayout;
    private LinearLayout wallpaperLayout;
    private LinearLayout othersLayout;
    private TextView tvTitle;
    private TextView tvAuthor;
    private ImageView imageBanner;
    private FloatingActionButton btnApply;

    private Menu actionMenu;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.theme_preview_activity);

        Intent intent = getIntent();
        mThemePackageName = intent.getStringExtra("theme");

        mPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        appLayout = findViewById(R.id.app_container);
        previewLayout = findViewById(R.id.layout_previews);
        previewPicLayout = findViewById(R.id.layout_preview_pic);
        soundLayout = findViewById(R.id.sound_container);
        wallpaperLayout = findViewById(R.id.wallpaper_container);
        othersLayout = findViewById(R.id.others_container);
        tvTitle = findViewById(R.id.theme_title);
        tvAuthor = findViewById(R.id.theme_author);
        imageBanner = findViewById(R.id.banner_image);
        btnApply = findViewById(R.id.apply_theme_button);

        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle("");
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_back_arrow);
        }

        if (savedInstanceState != null) themeTargetBundle = savedInstanceState.getBundle("target_bundle");
        if (themeTargetBundle == null) themeTargetBundle =  new Bundle();

        btnApply.setOnClickListener(v -> applyTheme());

        AppBarLayout appBarLayout = findViewById(R.id.app_bar);
        appBarLayout.addOnOffsetChangedListener(new AppBarLayout.OnOffsetChangedListener() {
            @Override
            public void onOffsetChanged(AppBarLayout appBarLayout, int i) {
                if (actionMenu == null) return;
                if (i < -imageBanner.getHeight()) {
                    actionMenu.findItem(R.id.action_apply).setVisible(true);
                } else {
                    actionMenu.findItem(R.id.action_apply).setVisible(false);
                }
            }
        });

        Intent mThemeDataService = new Intent(this, ThemeDataService.class);
        mThemeDataConn = new ThemeDataConn();
        Intent mThemeManageService = new Intent(this, ThemeManageService.class);
        mThemeManageConn = new ThemeManageConn();
        bindServiceAsUser(mThemeDataService, mThemeDataConn, Context.BIND_AUTO_CREATE, UserHandle.CURRENT_OR_SELF);
        bindServiceAsUser(mThemeManageService, mThemeManageConn, Context.BIND_AUTO_CREATE, UserHandle.CURRENT_OR_SELF);
    }

    @Override
    public void onAttachFragment(Fragment fragment) {
        super.onAttachFragment(fragment);
        if (mApplyingDialog == null && fragment instanceof ThemeApplyingDialog) {
            mApplyingDialog = (ThemeApplyingDialog) fragment;
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBundle("target_bundle", themeTargetBundle);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_preview, menu);
        actionMenu = menu;
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        switch (id) {
            case android.R.id.home:
                finish();
                break;
            case R.id.action_apply:
                applyTheme();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mThemeDataConn != null) {
            unbindService(mThemeDataConn);
        }
        if (mThemeManageConn != null) {
            try {
                mThemeManageBinder.removeThemeApplyStatusListener(mThemeApplyStatusListener);
            } catch (Exception e) {
                Log.e(TAG, "Failed to remove theme apply status listener!");
            }
            unbindService(mThemeManageConn);
        }
    }


    private void updateViewForTheme() {
        tvTitle.setText(mThemeItem.getTitle());
        tvAuthor.setText(mThemeItem.getAuthor());
        imageBanner.setImageDrawable(mThemeDataBinder.getThemeBanner(mThemeItem.getPackageName()));

        // wallpaper
        if (mThemeItem.hasWallpaper()) {
            addSwitch(wallpaperLayout, Constants.THEME_TARGET_WALLPAPER, R.string.background_wallpaper);
        }
        if (mThemeItem.hasLockScreen()) {
            addSwitch(wallpaperLayout, Constants.THEME_TARGET_LOCKSCREEN, R.string.background_lockscreen);
        }
        if (!mThemeItem.hasWallpaper() && !mThemeItem.hasLockScreen()) {
            wallpaperLayout.setVisibility(View.GONE);
        }

        // sound
        if (mThemeItem.hasRingtone()) {
            addSwitch(soundLayout, Constants.THEME_TARGET_RINGTONE, R.string.sound_ringtone);
        }
        if (mThemeItem.hasAlarmSound()) {
            addSwitch(soundLayout, Constants.THEME_TARGET_ALARM, R.string.sound_alarm);
        }
        if (mThemeItem.hasNotificationSound()) {
            addSwitch(soundLayout, Constants.THEME_TARGET_NOTIFICATION, R.string.sound_notification);
        }
        if (!mThemeItem.hasRingtone() && !mThemeItem.hasAlarmSound() && !mThemeItem.hasNotificationSound()) {
            soundLayout.setVisibility(View.GONE);
        }

        // others
        if (mThemeItem.hasBootanimation) {
            addSwitch(othersLayout, Constants.THEME_TARGET_BOOTANIMATION, R.string.others_bootanimation);
        }
        if (mThemeItem.hasFonts) {
            addSwitch(othersLayout, Constants.THEME_TARGET_FONTS, R.string.others_fonts);
        }
        if (!mThemeItem.hasFonts && !mThemeItem.hasBootanimation){
            othersLayout.setVisibility(View.GONE);
        }

        // apps
        if (mThemeItem.hasOverlays()) {
            for (OverlayTarget overlayTarget : mThemeItem.getOverlayTargets()) {
                addSwitch(appLayout, overlayTarget.getPackageName(), overlayTarget.getLabel(), overlayTarget.getSwitchable());
            }
        } else {
            appLayout.setVisibility(View.GONE);
        }

        // previews
        List<Drawable> previewList = mThemeDataBinder.getThemePreviewList(mThemeItem.getPackageName());
        if (!previewList.isEmpty()) {
            for (Drawable drawable : previewList) {
                addPreview(drawable);
            }
        } else {
            previewLayout.setVisibility(View.GONE);
        }

    }

    private void addSwitch(LinearLayout layout, final String id, int text) {
        addSwitch(layout, id, getString(text), true);
    }

    private void addSwitch(LinearLayout layout, final String id, String text, boolean enabled) {

        Switch tmpSwitch = new Switch(this);

        tmpSwitch.setText(text);
        tmpSwitch.setChecked(themeTargetBundle.getBoolean(id, true));

        tmpSwitch.setOnCheckedChangeListener((compoundButton, b) -> themeTargetBundle.putBoolean(id, b));

        tmpSwitch.setEnabled(enabled);

        layout.addView(tmpSwitch);
    }

    private void addPreview(Drawable drawable) {

        int imageHeight = getResources().getDimensionPixelOffset(R.dimen.preview_image_height);

        ImageView imageView = new ImageView(this);
        imageView.setImageDrawable(drawable);

        imageView.setMaxHeight(imageHeight);
        imageView.setAdjustViewBounds(true);

        imageView.setOnClickListener(v -> {
            /*
            imagePreviewViewer.setImageDrawable(imageView.getDrawable());
            imagePreviewViewer.setVisibility(View.VISIBLE);
            */
        });

        previewPicLayout.addView(imageView);

    }

    public void applyTheme() {
        if (mApplyingDialog == null) mApplyingDialog = new ThemeApplyingDialog();
        mApplyingDialog.show(getSupportFragmentManager(), TAG);
        themeTargetBundle.putBoolean(Constants.PREFERENCES_OVERLAY_REMOVE_FLAG, mPreferences.getBoolean(Constants.PREFERENCES_OVERLAY_REMOVE_FLAG, false));
        themeTargetBundle.putBoolean(Constants.PREFERENCES_FORCED_CENTER_WALLPAPER, mPreferences.getBoolean(Constants.PREFERENCES_FORCED_CENTER_WALLPAPER, false));
        new Thread(() -> {
            if (mThemeItem != null) {
                mThemeManageBinder.applyTheme(mThemeItem, themeTargetBundle);
            }
        }).start();
    }

    private class ThemeDataConn implements ServiceConnection {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            mThemeDataBinder = (ThemeDataService.ThemeDataBinder) iBinder;
            if (mThemeDataBinder.isThemePackage(mThemePackageName)) {
                mThemeItem = mThemeDataBinder.getThemeItem(mThemePackageName);
                updateViewForTheme();
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
            mThemeApplyStatusListener = data -> {
                if (mApplyingDialog == null) return false;
                sendBroadcastAsUser(data, UserHandle.CURRENT_OR_SELF);
                runOnUiThread(() -> mApplyingDialog.updateData(data));
                return true;
            };
            mThemeManageBinder.addThemeApplyStatusListener(mThemeApplyStatusListener);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
        }
    }
}
