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

package org.exthmui.theme.fragments;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Bundle;


import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;

import org.exthmui.theme.R;
import org.exthmui.theme.interfaces.ThemePreviewInterface;
import org.exthmui.theme.models.OverlayTarget;
import org.exthmui.theme.models.ThemeItem;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ThemePreviewFragment extends Fragment {

    private static final String TAG = "ThemePreviewFragment";

    private ThemeItem mThemeItem;
    private ThemePreviewInterface mCallback;
    private View view;
    private Map<String, Boolean> mThemeTargetMap;

    private LinearLayout appLayout;
    private LinearLayout previewLayout;
    private LinearLayout previewPicLayout;
    private LinearLayout soundLayout;
    private LinearLayout wallpaperLayout;
    private Button btnApply;
    private ImageView btnBack;
    private TextView tvTitle;
    private TextView tvAuthor;
    private ImageView imageBanner;
    private ImageView imagePreviewViewer;

    public static ThemePreviewFragment newInstance() {
        return new ThemePreviewFragment();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context != null) {
            mCallback = (ThemePreviewInterface) context;
        }
        mThemeTargetMap = new HashMap<>();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.theme_preview_fragment, container, false);

        appLayout = view.findViewById(R.id.app_container);
        previewLayout = view.findViewById(R.id.layout_previews);
        previewPicLayout = view.findViewById(R.id.layout_preview_pic);
        soundLayout = view.findViewById(R.id.sound_container);
        wallpaperLayout = view.findViewById(R.id.wallpaper_container);
        tvTitle = view.findViewById(R.id.theme_title);
        tvAuthor = view.findViewById(R.id.theme_author);
        imageBanner = view.findViewById(R.id.banner_image);
        imagePreviewViewer = view.findViewById(R.id.preview_viewer);
        btnApply = view.findViewById(R.id.apply_theme_button);
        btnBack = view.findViewById(R.id.button_back);

        btnApply.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Bundle bundle = new Bundle();
                bundle.putString("package", mThemeItem.getPackageName());
                bundle.putBoolean("ringtone", mThemeTargetMap.get("ringtone"));
                bundle.putBoolean("alarm", mThemeTargetMap.get("alarm"));
                bundle.putBoolean("notification", mThemeTargetMap.get("notification"));
                bundle.putBoolean("wallpaper", mThemeTargetMap.get("wallpaper"));
                bundle.putBoolean("lockscreen", mThemeTargetMap.get("lockscreen"));

                ArrayList<String> whiteList = new ArrayList<>();
                for (Map.Entry<String, Boolean> entry : mThemeTargetMap.entrySet()) {
                    String key = entry.getKey();
                    if (!entry.getValue() && !key.equals("wallpaper") && !key.equals("lockscreen")
                            && !key.equals("ringtone") && !key.equals("alarm") && !key.equals("notification")) {
                        whiteList.add(key);
                    }
                }
                bundle.putStringArrayList("whitelist", whiteList);
                mCallback.applyTheme(bundle);
            }
        });

        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getActivity().onBackPressed();
            }
        });

        imagePreviewViewer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                imagePreviewViewer.setVisibility(View.GONE);
            }
        });

        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }
    
    public void setThemeItem(ThemeItem themeItem) {
        mThemeItem = themeItem;
        updateView();
    }

    private void updateView() {
        // base info
        tvTitle.setText(mThemeItem.getTitle());
        tvAuthor.setText(mThemeItem.getAuthor());
        imageBanner.setImageDrawable(mCallback.getThemeBanner(mThemeItem.getPackageName()));

        mThemeTargetMap.put("wallpaper", false);
        mThemeTargetMap.put("lockscreen", false);

        mThemeTargetMap.put("ringtone", false);
        mThemeTargetMap.put("alarm", false);
        mThemeTargetMap.put("notification", false);

        // wallpaper
        if (mThemeItem.hasWallpaper()) {
            addSwitch(wallpaperLayout, "wallpaper", R.string.background_wallpaper, true, true);
        }
        if (mThemeItem.hasLockScreen()) {
            addSwitch(wallpaperLayout, "lockscreen", R.string.background_lockscreen, true, true);
        }

        if (!mThemeItem.hasWallpaper() && !mThemeItem.hasLockScreen()) {
            wallpaperLayout.setVisibility(View.GONE);
        }

        // sound
        if (mThemeItem.hasRingtone()) {
            addSwitch(soundLayout, "ringtone", R.string.sound_ringtone, true, true);
        }
        if (mThemeItem.hasAlarmSound()) {
            addSwitch(soundLayout, "alarm", R.string.sound_alarm, true, true);
        }
        if (mThemeItem.hasNotificationSound()) {
            addSwitch(soundLayout, "notification", R.string.sound_notification, true, true);
        }

        if (!mThemeItem.hasRingtone() && !mThemeItem.hasAlarmSound() && !mThemeItem.hasNotificationSound()) {
            soundLayout.setVisibility(View.GONE);
        }

        // apps
        if (mThemeItem.hasOverlays()) {
            for (OverlayTarget overlayTarget : mThemeItem.getOverlayTargets()) {
                addSwitch(appLayout, overlayTarget.getPackageName(), overlayTarget.getLabel(), true, overlayTarget.getSwitchable());
            }
        } else {
            appLayout.setVisibility(View.GONE);
        }

        // previews
        List<Drawable> previewList = mCallback.getThemePreviewList(mThemeItem.getPackageName());
        if (!previewList.isEmpty()) {
            for (Drawable drawable : previewList) {
                addPreview(drawable);
            }
        } else {
            previewLayout.setVisibility(View.GONE);
        }

    }

    private void addSwitch(LinearLayout layout, final String id, int text, boolean defaultValue, boolean enabled) {
        addSwitch(layout, id, getString(text), defaultValue, enabled);
    }

    private void addSwitch(LinearLayout layout, final String id, String text, boolean defaultValue, boolean enabled) {
        int paddingTop = getResources().getDimensionPixelOffset(R.dimen.picker_switch_padding_top);
        int paddingBottom = getResources().getDimensionPixelOffset(R.dimen.picker_switch_padding_bottom);
        int paddingLeft = getResources().getDimensionPixelOffset(R.dimen.picker_switch_padding_left);
        int paddingRight = getResources().getDimensionPixelOffset(R.dimen.picker_switch_padding_right);

        Switch tmpSwitch = new Switch(view.getContext());

        tmpSwitch.setText(text);
        tmpSwitch.setChecked(defaultValue);
        tmpSwitch.setPadding(paddingLeft, paddingTop, paddingRight, paddingBottom);
        mThemeTargetMap.put(id, defaultValue);

        tmpSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                mThemeTargetMap.put(id, b);
            }
        });

        tmpSwitch.setEnabled(enabled);

        layout.addView(tmpSwitch);
    }

    private void addPreview(Drawable drawable) {

        int imageHeight = getResources().getDimensionPixelOffset(R.dimen.preview_image_height);

        ImageView imageView = new ImageView(view.getContext());
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
}

