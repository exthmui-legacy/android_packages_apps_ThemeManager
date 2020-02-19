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

package org.exthmui.theme.services;

import android.app.Service;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.content.res.XmlResourceParser;
import android.graphics.drawable.Drawable;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.util.DisplayMetrics;
import android.util.Log;

import org.exthmui.theme.R;
import org.exthmui.theme.models.OverlayTarget;
import org.exthmui.theme.models.ThemeAccent;
import org.exthmui.theme.models.ThemeBase;
import org.exthmui.theme.models.ThemeItem;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

public class ThemeDataService extends Service {

    private final static String TAG = "ThemeDataService";

    private PackageManager mPackageManager;
    private List<ThemeBase> mThemeBaseList;

    public ThemeDataService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        return new ThemeDataBinder();
    }

    @Override
    public void onCreate() {
        mPackageManager = getPackageManager();
        mThemeBaseList = new ArrayList<>();
    }

    public class ThemeDataBinder extends Binder {
        public List<ThemeBase> getThemeBaseList() {
            if (mThemeBaseList.isEmpty()) updateThemeList();
            return mThemeBaseList;
        }

        public void updateThemeList() {
            IUpdateThemeList();
        }

        public boolean isThemePackage(String packageName) {
            return IsThemePackage(packageName);
        }

        public boolean isAccentColorPackage(String packageName) {
            return IsAccentColorPackage(packageName);
        }

        public ThemeAccent getThemeAccent(String packageName) {
            return IGetThemeAccent(packageName);
        }

        public ThemeItem getThemeItem(String packageName) {
            return IGetThemeItem(packageName);
        }

        public Drawable getThemeImage(String packageName) {
            return IGetThemeImage(packageName);
        }

        public Drawable getThemeBanner(String packageName) {
            return IGetThemeBanner(packageName);
        }

        public List<Drawable> getThemePreviewList(String packageName) {
            return IGetThemePreviewList(packageName);
        }
    }

    private void IUpdateThemeList() {
        List<PackageInfo> allPackages = mPackageManager.getInstalledPackages(0);
        mThemeBaseList.clear();

        // add default theme
        ThemeBase defaultTheme = new ThemeBase(getPackageName());
        IGetThemeBaseInfo(defaultTheme);
        mThemeBaseList.add(defaultTheme);

        for (PackageInfo pkgInfo : allPackages) {
            if (IsThemePackage(pkgInfo.packageName)) {
                ThemeBase themeBase = new ThemeBase(pkgInfo.packageName);
                IGetThemeBaseInfo(themeBase);
                mThemeBaseList.add(themeBase);
            } else if (IsAccentColorPackage(pkgInfo.packageName)) {
                ThemeAccent themeAccent = IGetThemeAccent(pkgInfo.packageName);
                mThemeBaseList.add(themeAccent);
            }
        }
    }

    private boolean IsThemePackage(String packageName) {
        boolean ret = false;
        if (getPackageName().equals(packageName)) return true;
        try {
            ApplicationInfo ai = mPackageManager.getApplicationInfo(packageName, PackageManager.GET_META_DATA);
            Bundle metadata = ai.metaData;
            if (metadata != null) {
                ret = metadata.getBoolean("exthmui_theme",false);
            }
        } catch (Exception e) {
            Log.e(TAG, "check package " + packageName + " failed");
        }
        return ret;
    }

    private boolean IsAccentColorPackage(String packageName) {
        boolean ret = false;
        try {
            ApplicationInfo ai = mPackageManager.getApplicationInfo(packageName, PackageManager.GET_META_DATA);
            Bundle metadata = ai.metaData;
            if (metadata != null) {
                ret = metadata.getInt("lineage_berry_accent_preview",0) != 0;
            }
        } catch (Exception e) {
            Log.e(TAG, "check package " + packageName + " failed");
        }
        return ret;
    }

    private Drawable IGetThemeBanner(String packageName) {
        try {
            Resources resources = mPackageManager.getResourcesForApplication(packageName);
            int bannerResId = resources.getIdentifier("theme_banner", "drawable", packageName);
            return resources.getDrawable(bannerResId, null);
        } catch (Exception e) {
            Log.e(TAG, "Failed to get banner of " + packageName);
        }
        return null;
    }

    private Drawable IGetThemeImage(String packageName) {
        try {
            Resources resources = mPackageManager.getResourcesForApplication(packageName);
            int imageResId = resources.getIdentifier("theme_image", "drawable", packageName);
            return resources.getDrawable(imageResId, null);
        } catch (Exception e) {
            Log.e(TAG, "Failed to get image of " + packageName);
        }
        return null;
    }

    private List<Drawable> IGetThemePreviewList(String packageName) {
        List<Drawable> previewList = new ArrayList<>();

        try {
            Resources resources = mPackageManager.getResourcesForApplication(packageName);
            AssetManager assetManager = resources.getAssets();

            String[] previewsArray = assetManager.list("previews");
            if (previewsArray != null) {
                for (String preview : previewsArray) {
                    InputStream inputStream = assetManager.open("previews/" + preview);
                    Drawable drawable = Drawable.createFromStream(inputStream, preview);
                    previewList.add(drawable);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to get previews of " + packageName);
        }

        return previewList;
    }

    private OverlayTarget getOverlayTarget(String packageName) {
        try {
            ApplicationInfo ai = mPackageManager.getApplicationInfo(packageName, 0);

            OverlayTarget overlayTarget = new OverlayTarget(packageName);
            overlayTarget.setLabel(ai.loadLabel(mPackageManager).toString());
            return overlayTarget;
        } catch (Exception e) {
            Log.e(TAG, "Failed to get info of " + packageName);
        }
        return null;
    }

    private void IGetThemeBaseInfo(ThemeBase themeBase) {
        String packageName = themeBase.getPackageName();
        try {
            Resources resources = mPackageManager.getResourcesForApplication(packageName);
            ApplicationInfo ai = mPackageManager.getApplicationInfo(packageName, PackageManager.GET_META_DATA);
            int themeTitleResId = resources.getIdentifier("theme_title", "string", packageName);
            int themeAuthorResId = resources.getIdentifier("theme_author", "string", packageName);

            themeBase.setTitle(resources.getString(themeTitleResId));
            themeBase.setAuthor(resources.getString(themeAuthorResId));
            themeBase.setRemovable((ai.flags & ApplicationInfo.FLAG_SYSTEM) != 0);
        } catch (Exception e) {
            Log.e(TAG, "Failed to get theme info: " + packageName);
        }
    }

    private ThemeAccent IGetThemeAccent(String packageName) {
        try {
            ApplicationInfo ai = mPackageManager.getApplicationInfo(packageName, PackageManager.GET_META_DATA);
            ThemeAccent themeAccent = new ThemeAccent(packageName);

            themeAccent.setTitle(ai.loadLabel(mPackageManager).toString());
            themeAccent.setAuthor("LineageOS");
            themeAccent.setAccentColor(ai.metaData.getInt("lineage_berry_accent_preview",0));
            themeAccent.setRemovable((ai.flags & ApplicationInfo.FLAG_SYSTEM) != 0);
            themeAccent.setOverlayTarget(getOverlayTarget("android"));
            return themeAccent;
        } catch (Exception e) {
            Log.e(TAG, "Failed to get theme info: " + packageName);
            return null;
        }
    }

    private ThemeItem IGetThemeItem(String packageName) {
        try {
            DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
            Resources resources = mPackageManager.getResourcesForApplication(packageName);
            ThemeItem theme = new ThemeItem(packageName) ;
            IGetThemeBaseInfo(theme);

            Stack<String> xmlTags = new Stack<>();
            Map<String, Integer> attrMap = new HashMap<>();
            List<OverlayTarget> overlayTargetList = new ArrayList<>();
            int themeInfoXmlResId = resources.getIdentifier("theme_data", "xml", packageName);
            XmlResourceParser themeInfoXml = resources.getXml(themeInfoXmlResId);
            int eventType = themeInfoXml.getEventType();
            boolean overlaySwitchable = true;

            while (eventType != XmlResourceParser.END_DOCUMENT) {
                switch (eventType) {
                    case XmlResourceParser.START_TAG:
                        xmlTags.push(themeInfoXml.getName().toLowerCase());
                        switch (xmlTags.peek()) {
                            case "overlay":
                                overlayTargetList.clear();
                                break;
                            case "target":
                                overlaySwitchable = true;
                                for (int i = 0; i < themeInfoXml.getAttributeCount(); i++) {
                                    if (themeInfoXml.getAttributeName(i).equals("switchable")) {
                                        overlaySwitchable = themeInfoXml.getAttributeBooleanValue(i, true);
                                        break;
                                    }
                                }
                                break;
                            case "wallpaper": case "lockscreen":
                                attrMap.clear();
                                for (int i = 0; i < themeInfoXml.getAttributeCount(); i++) {
                                    attrMap.put(themeInfoXml.getAttributeName(i), themeInfoXml.getAttributeIntValue(i, -1));
                                }
                                break;
                        }
                        break;
                    case XmlResourceParser.TEXT:
                        switch (xmlTags.peek()) {
                            // sounds
                            case "ringtone":
                                theme.setRingtone(themeInfoXml.getText());
                                break;
                            case "alarm":
                                theme.setAlarmSound(themeInfoXml.getText());
                                break;
                            case "notification":
                                theme.setNotificationSound(themeInfoXml.getText());
                                break;

                            // background
                            case "wallpaper":
                                if ((attrMap.isEmpty() && !theme.hasWallpaper()) ||
                                        (displayMetrics.widthPixels / attrMap.getOrDefault("ratioX", 1) == displayMetrics.heightPixels / attrMap.getOrDefault("ratioY", 1))) {
                                    theme.setWallpaper(themeInfoXml.getText());
                                }
                                break;
                            case "lockscreen":
                                if ((attrMap.isEmpty() && !theme.hasLockScreen()) ||
                                        (displayMetrics.widthPixels / attrMap.getOrDefault("ratioX", 1) == displayMetrics.heightPixels / attrMap.getOrDefault("ratioY", 1))) {
                                    theme.setLockScreen(themeInfoXml.getText());
                                }
                                break;

                            // overlay
                            case "target":
                                OverlayTarget ovt = getOverlayTarget(themeInfoXml.getText());
                                if (ovt != null) {
                                    ovt.setSwitchable(overlaySwitchable);
                                    overlayTargetList.add(ovt);
                                }
                                break;
                        }
                        break;
                    case XmlResourceParser.END_TAG:
                        xmlTags.pop();
                }
                eventType = themeInfoXml.next();
            }

            theme.setOverlayTargets(overlayTargetList);
            return theme;
        } catch (Exception e) {
            Log.e(TAG, "Failed to get theme info: " + packageName);
            return null;
        }
    }

}
