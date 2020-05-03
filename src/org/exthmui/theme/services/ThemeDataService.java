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

import org.exthmui.theme.misc.Constants;
import org.exthmui.theme.models.OverlayTarget;
import org.exthmui.theme.models.ThemeBase;
import org.exthmui.theme.models.ThemeItem;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

public class ThemeDataService extends Service {

    private final static String TAG = "ThemeDataService";

    private PackageManager mPackageManager;
    private List<ThemeBase> mThemeBaseList;

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
            return mThemeBaseList;
        }

        public void updateThemeList() {
            IUpdateThemeList();
        }

        public boolean isThemePackage(String packageName) {
            return IsThemePackage(packageName);
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

        for (PackageInfo pkgInfo : allPackages) {
            if (IsThemePackage(pkgInfo.packageName)) {
                ThemeBase themeBase = new ThemeBase(pkgInfo.packageName);
                IGetThemeBaseInfo(themeBase);
                mThemeBaseList.add(themeBase);
            }
        }
    }

    private boolean IsThemePackage(String packageName) {
        boolean ret = false;
        try {
            ApplicationInfo ai = mPackageManager.getApplicationInfo(packageName, PackageManager.GET_META_DATA);
            Bundle metadata = ai.metaData;
            if (metadata != null) {
                ret = metadata.getBoolean(Constants.THEME_DATA_FLAG,false);
            }
        } catch (Exception e) {
            Log.e(TAG, "check package " + packageName + " failed");
        }
        return ret;
    }

    private Drawable IGetThemeBanner(String packageName) {
        try {
            Resources resources = mPackageManager.getResourcesForApplication(packageName);
            int bannerResId = resources.getIdentifier(Constants.THEME_DATA_BANNER, "drawable", packageName);
            return resources.getDrawable(bannerResId, null);
        } catch (Exception e) {
            Log.e(TAG, "Failed to get banner of " + packageName);
        }
        return null;
    }

    private Drawable IGetThemeImage(String packageName) {
        try {
            Resources resources = mPackageManager.getResourcesForApplication(packageName);
            int imageResId = resources.getIdentifier(Constants.THEME_DATA_IMAGE, "drawable", packageName);
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

            String[] previewsArray = assetManager.list(Constants.THEME_DATA_ASSETS_PREVIEWS);
            if (previewsArray != null) {
                for (String preview : previewsArray) {
                    InputStream inputStream = assetManager.open(Constants.THEME_DATA_ASSETS_PREVIEWS + "/" + preview);
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
            int themeTitleResId = resources.getIdentifier(Constants.THEME_DATA_TITLE, "string", packageName);
            int themeAuthorResId = resources.getIdentifier(Constants.THEME_DATA_AUTHOR, "string", packageName);

            themeBase.setTitle(resources.getString(themeTitleResId));
            themeBase.setAuthor(resources.getString(themeAuthorResId));
            themeBase.setRemovable((ai.flags & ApplicationInfo.FLAG_SYSTEM) != 0);
        } catch (Exception e) {
            Log.e(TAG, "Failed to get theme info: " + packageName);
        }
    }

    private ThemeItem IGetThemeItem(String packageName) {
        try {
            DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
            Resources resources = mPackageManager.getResourcesForApplication(packageName);
            ThemeItem theme = new ThemeItem(packageName) ;
            IGetThemeBaseInfo(theme);
            int hasBootanimResId = resources.getIdentifier(Constants.THEME_DATA_HAS_BOOTANIM, "bool", packageName);
            theme.hasBootanimation = (hasBootanimResId != 0) && resources.getBoolean(hasBootanimResId);

            Stack<String> xmlTags = new Stack<>();
            List<OverlayTarget> overlayTargetList = new ArrayList<>();
            int themeInfoXmlResId = resources.getIdentifier(Constants.THEME_DATA_XML_FILE, "xml", packageName);
            XmlResourceParser themeInfoXml = resources.getXml(themeInfoXmlResId);
            int eventType = themeInfoXml.getEventType();
            boolean overlaySwitchable = true;
            int ratioWidth = -1, ratioHeight = -1;

            while (eventType != XmlResourceParser.END_DOCUMENT) {
                switch (eventType) {
                    case XmlResourceParser.START_TAG:
                        xmlTags.push(themeInfoXml.getName().toLowerCase());
                        switch (xmlTags.peek()) {
                            case Constants.THEME_DATA_XML_OVERLAY:
                                overlayTargetList.clear();
                                break;
                            case Constants.THEME_DATA_XML_OVERLAY_TARGET:
                                overlaySwitchable = true;
                                for (int i = 0; i < themeInfoXml.getAttributeCount(); i++) {
                                    if (themeInfoXml.getAttributeName(i).equals(Constants.THEME_DATA_XML_OVERLAY_TARGET_ATTR_SWITCHABLE)) {
                                        overlaySwitchable = themeInfoXml.getAttributeBooleanValue(i, true);
                                        break;
                                    }
                                }
                                break;
                            case Constants.THEME_DATA_XML_BACKGROUND_WALLPAPER: case Constants.THEME_DATA_XML_BACKGROUND_LOCKSCREEN:
                                ratioWidth = ratioHeight = -1;
                                for (int i = 0; i < themeInfoXml.getAttributeCount(); i++) {
                                    if (themeInfoXml.getAttributeName(i).equals(Constants.THEME_DATA_XML_BACKGROUND_RATIO_WIDTH)) {
                                        ratioWidth = themeInfoXml.getAttributeIntValue(i, -1);
                                    } else if (themeInfoXml.getAttributeName(i).equals(Constants.THEME_DATA_XML_BACKGROUND_RATIO_HEIGHT)) {
                                        ratioHeight = themeInfoXml.getAttributeIntValue(i, -1);
                                    }
                                }
                                break;
                        }
                        break;
                    case XmlResourceParser.TEXT:
                        switch (xmlTags.peek()) {
                            // sounds
                            case Constants.THEME_DATA_XML_SOUND_RINGTONE:
                                theme.setRingtone(themeInfoXml.getText());
                                break;
                            case Constants.THEME_DATA_XML_SOUND_ALARM:
                                theme.setAlarmSound(themeInfoXml.getText());
                                break;
                            case Constants.THEME_DATA_XML_SOUND_NOTIFICATION:
                                theme.setNotificationSound(themeInfoXml.getText());
                                break;

                            // background
                            case Constants.THEME_DATA_XML_BACKGROUND_WALLPAPER:
                                if ((ratioHeight == -1 || ratioWidth == -1 && !theme.hasWallpaper()) ||
                                        (displayMetrics.widthPixels / ratioWidth == displayMetrics.heightPixels / ratioHeight)) {
                                    theme.setWallpaper(themeInfoXml.getText());
                                }
                                break;
                            case Constants.THEME_DATA_XML_BACKGROUND_LOCKSCREEN:
                                if ((ratioHeight == -1 || ratioWidth == -1 && !theme.hasLockScreen()) ||
                                        (displayMetrics.widthPixels / ratioWidth == displayMetrics.heightPixels / ratioHeight)) {
                                    theme.setLockScreen(themeInfoXml.getText());
                                }
                                break;

                            // overlay
                            case Constants.THEME_DATA_XML_OVERLAY_TARGET:
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
