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
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.IBinder;
import android.os.UserHandle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.exthmui.theme.adapters.PreviewImageAdapter;
import org.exthmui.theme.adapters.ThemeTargetAdapter;
import org.exthmui.theme.fragments.ThemeApplyingDialog;
import org.exthmui.theme.misc.Constants;
import org.exthmui.theme.models.ThemeItem;
import org.exthmui.theme.models.ThemeTarget;
import org.exthmui.theme.services.ThemeDataService;
import org.exthmui.theme.services.ThemeManageService;

import java.util.ArrayList;

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

    private ThemeTargetAdapter mThemeTargetAdapter;
    private ArrayList<ThemeTarget> mThemeTargets;
    private PreviewImageAdapter mPreviewImageAdapter;
    private ArrayList<Drawable> mPreviewImages;

    private TextView tvTitle;
    private TextView tvAuthor;
    private TextView tvSeparatorPreview;
    private ImageView imageBanner;
    private ImageView imagePreview;
    private RecyclerView targetsView;
    private RecyclerView previewsView;
    private FloatingActionButton btnApply;
    private Menu actionMenu;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.theme_preview_activity);

        Intent intent = getIntent();
        mThemePackageName = intent.getStringExtra("theme");

        tvTitle = findViewById(R.id.theme_title);
        tvAuthor = findViewById(R.id.theme_author);
        tvSeparatorPreview = findViewById(R.id.theme_separator_preview);
        imageBanner = findViewById(R.id.banner_image);
        imagePreview = findViewById(R.id.preview_image_view);
        btnApply = findViewById(R.id.apply_theme_button);
        targetsView = findViewById(R.id.theme_target_list);
        previewsView = findViewById(R.id.preview_images_list);

        mThemeTargets = new ArrayList<>();
        mThemeTargetAdapter = new ThemeTargetAdapter(mThemeTargets, this);
        mThemeTargetAdapter.setOnItemClickListener((v, position) -> onThemeTargetItemClick(position));
        targetsView.setLayoutManager(new GridLayoutManager(this, getResources().getInteger(R.integer.preview_target_span_count)));
        targetsView.setAdapter(mThemeTargetAdapter);

        mPreviewImages = new ArrayList<>();
        mPreviewImageAdapter = new PreviewImageAdapter(mPreviewImages);
        mPreviewImageAdapter.setOnItemClickListener((v, position) -> {
            imagePreview.setImageDrawable(mPreviewImages.get(position));
            imagePreview.setVisibility(View.VISIBLE);
        });
        previewsView.setLayoutManager(new LinearLayoutManager(this, RecyclerView.HORIZONTAL, false));
        previewsView.setAdapter(mPreviewImageAdapter);
        imagePreview.setOnClickListener(v -> {
            if (getSupportActionBar() != null) {
                getSupportActionBar().show();
            }
            imagePreview.setVisibility(View.GONE);
        });

        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle("");
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_back_arrow);
        }

        if (savedInstanceState != null) {
            themeTargetBundle = savedInstanceState.getBundle("target_bundle");
        } else {
            themeTargetBundle =  new Bundle();
        }

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
        mApplyingDialog = null;
    }

    private void onThemeTargetItemClick(int position) {
        ThemeTarget target = mThemeTargets.get(position);
        if (!target.isSwitchable() || target.isSubtitle()) return;
        target.select(!target.isSelected());
        themeTargetBundle.putBoolean(target.getTargetId(), target.isSelected());
        mThemeTargetAdapter.notifyDataSetChanged();
    }

    private void updateViewForTheme() {
        tvTitle.setText(mThemeItem.getTitle());
        tvAuthor.setText(mThemeItem.getAuthor());
        imageBanner.setImageDrawable(mThemeDataBinder.getThemeBanner(mThemeItem.getPackageName()));
        new Thread(() -> {
            mPreviewImages.addAll(mThemeDataBinder.getThemePreviewList(mThemeItem.getPackageName()));
            runOnUiThread(() -> {
                mPreviewImageAdapter.notifyDataSetChanged();
                if (mPreviewImages.isEmpty()) {
                    tvSeparatorPreview.setVisibility(View.GONE);
                }
            });
        }).start();

        // wallpaper
        if (mThemeItem.hasWallpaper()) {
            addThemeTarget(Constants.THEME_TARGET_WALLPAPER, ThemeTarget.TYPE_BACKGROUNDS, R.string.background_wallpaper, true);
        }
        if (mThemeItem.hasLockScreen()) {
            addThemeTarget(Constants.THEME_TARGET_LOCKSCREEN, ThemeTarget.TYPE_BACKGROUNDS, R.string.background_lockscreen, true);
        }
        if (mThemeItem.hasWallpaper() || mThemeItem.hasLockScreen()) {
            addSubtitle(ThemeTarget.TYPE_BACKGROUNDS, R.string.separator_background);
        }

        // sound
        if (mThemeItem.hasRingtone()) {
            addThemeTarget(Constants.THEME_TARGET_RINGTONE, ThemeTarget.TYPE_SOUNDS, R.string.sound_ringtone, true);
        }
        if (mThemeItem.hasAlarmSound()) {
            addThemeTarget(Constants.THEME_TARGET_ALARM, ThemeTarget.TYPE_SOUNDS, R.string.sound_alarm, true);
        }
        if (mThemeItem.hasNotificationSound()) {
            addThemeTarget(Constants.THEME_TARGET_NOTIFICATION, ThemeTarget.TYPE_SOUNDS, R.string.sound_notification, true);
        }
        if (mThemeItem.hasRingtone() || mThemeItem.hasAlarmSound() || mThemeItem.hasNotificationSound()) {
            addSubtitle(ThemeTarget.TYPE_SOUNDS, R.string.separator_sound);
        }

        // others
        if (mThemeItem.hasBootanimation) {
            addThemeTarget(Constants.THEME_TARGET_BOOTANIMATION, ThemeTarget.TYPE_OTHERS, R.string.others_bootanimation, true);
        }
        if (mThemeItem.hasFonts) {
            addThemeTarget(Constants.THEME_TARGET_FONTS, ThemeTarget.TYPE_OTHERS,  R.string.others_fonts, true);
        }
        if (mThemeItem.hasBootanimation || mThemeItem.hasFonts) {
            addSubtitle(ThemeTarget.TYPE_OTHERS, R.string.separator_others);
        }

        // apps
        if (mThemeItem.hasOverlays()) {
            int startPos = mThemeTargets.size() - 1;
            mThemeTargets.addAll(mThemeItem.getOverlayTargets());
            for (int i = startPos; i < mThemeTargets.size(); i++) {
                ThemeTarget target = mThemeTargets.get(i);
                target.select(themeTargetBundle.getBoolean(target.getTargetId(), true));
            }
            addSubtitle(ThemeTarget.TYPE_APPLICATIONS, R.string.separator_app);
        }

        mThemeTargets.sort(ThemeTarget::compareTo);
        mThemeTargetAdapter.notifyDataSetChanged();

        // previews
        /*List<Drawable> previewList = mThemeDataBinder.getThemePreviewList(mThemeItem.getPackageName());
        if (!previewList.isEmpty()) {
            for (Drawable drawable : previewList) {
                addPreview(drawable);
            }
        } else {
            previewLayout.setVisibility(View.GONE);
        }*/

    }

    private void addThemeTarget(final String id, int type, int textId, boolean enabled) {
        ThemeTarget target = new ThemeTarget(id, type);
        target.setLabel(getString(textId));
        target.setSwitchable(enabled);
        target.select(themeTargetBundle.getBoolean(id, true));
        target.setIsSubtitle(false);
        mThemeTargets.add(target);
    }

    private void addSubtitle(int type, int stringRes) {
        ThemeTarget subtitleTarget = new ThemeTarget("target.subtitle", type);
        subtitleTarget.setLabel(getString(stringRes));
        subtitleTarget.setIsSubtitle(true);
        mThemeTargets.add(subtitleTarget);
    }

/*
    private void addPreview(Drawable drawable) {

        int imageHeight = getResources().getDimensionPixelOffset(R.dimen.preview_image_height);

        ImageView imageView = new ImageView(this);
        imageView.setImageDrawable(drawable);

        imageView.setMaxHeight(imageHeight);
        imageView.setAdjustViewBounds(true);

        imageView.setOnClickListener(v -> {
            imagePreviewViewer.setImageDrawable(imageView.getDrawable());
            imagePreviewViewer.setVisibility(View.VISIBLE);
        });

        previewPicLayout.addView(imageView);

    }
*/
    public void applyTheme() {
        if (mApplyingDialog == null) mApplyingDialog = new ThemeApplyingDialog();
        if (mThemeItem != null) {
            mApplyingDialog.show(getSupportFragmentManager(), TAG);
            mThemeManageBinder.applyTheme(mThemeItem, themeTargetBundle);
        }
    }

    private class ThemeDataConn implements ServiceConnection {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            mThemeDataBinder = (ThemeDataService.ThemeDataBinder) iBinder;
            if (mThemeDataBinder.isThemePackage(mThemePackageName)) {
                mThemeItem = mThemeDataBinder.getThemeItem(mThemePackageName);
                updateViewForTheme();
            } else {
                finish();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mThemeDataBinder = null;
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
            mThemeManageBinder = null;
            finish();
        }
    }
}
