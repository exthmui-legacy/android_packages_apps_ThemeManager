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

package org.exthmui.thememanager.services;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.om.IOverlayManager;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageInstaller;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.content.res.XmlResourceParser;
import android.graphics.drawable.Drawable;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.UserHandle;
import android.util.DisplayMetrics;
import android.util.Log;

import android.support.v4.content.LocalBroadcastManager;

import org.exthmui.thememanager.broadcasts.ThemeStatusReceiver;
import org.exthmui.thememanager.models.OverlayTarget;
import org.exthmui.thememanager.models.Theme;
import org.exthmui.thememanager.utils.PackageUtil;
import org.exthmui.thememanager.utils.SoundUtil;
import org.exthmui.thememanager.utils.WallpaperUtil;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.concurrent.atomic.AtomicBoolean;

public class ThemeManageService extends Service {

    private final static String TAG = "ThemeManageService";
    private final String OverlayPackageHeader = "exthmui.theme.overlay";

    public final static String BROADCAST_ACTION_APPLY_SUCCEED = "APPLY_SUCCEED";
    public final static String BROADCAST_ACTION_APPLY_FAILED = "APPLY_FAILED";
    public final static String BROADCAST_ACTION_APPLYING = "APPLYING";
    public final static String BROADCAST_ACTION_APPLYING_RINGTONE = "APPLYING_RINGTONE";
    public final static String BROADCAST_ACTION_APPLYING_ALARM = "APPLYING_ALARM";
    public final static String BROADCAST_ACTION_APPLYING_NOTIFICATION = "APPLYING_NOTIFICATION";
    public final static String BROADCAST_ACTION_APPLYING_WALLPAPER = "APPLYING_WALLPAPER";
    public final static String BROADCAST_ACTION_APPLYING_LOCKSCREEN = "APPLYING_LOCKSCREEN";
    public final static String BROADCAST_ACTION_APPLYING_OVERLAY = "APPLYING_OVERLAY";

    private LocalBroadcastManager mLocalBroadcastManager;
    private BroadcastReceiver mReceiver;
    private IOverlayManager mOverlayService;
    private PackageInstaller mPackageInstaller;
    private PackageManager mPackageManager;

    public ThemeManageService() {
    }

    @Override
    public void onCreate() {
        mLocalBroadcastManager = LocalBroadcastManager.getInstance(this);
        mReceiver = new ThemeStatusReceiver();
        mOverlayService = IOverlayManager.Stub.asInterface(ServiceManager.getService("overlay"));
        mPackageManager = getPackageManager();
        mPackageInstaller = mPackageManager.getPackageInstaller();

        IntentFilter tIntentFilter = new IntentFilter();

        tIntentFilter.addAction(BROADCAST_ACTION_APPLY_SUCCEED);
        tIntentFilter.addAction(BROADCAST_ACTION_APPLY_FAILED);
        tIntentFilter.addAction(BROADCAST_ACTION_APPLYING);
        tIntentFilter.addAction(BROADCAST_ACTION_APPLYING_RINGTONE);
        tIntentFilter.addAction(BROADCAST_ACTION_APPLYING_ALARM);
        tIntentFilter.addAction(BROADCAST_ACTION_APPLYING_NOTIFICATION);
        tIntentFilter.addAction(BROADCAST_ACTION_APPLYING_WALLPAPER);
        tIntentFilter.addAction(BROADCAST_ACTION_APPLYING_LOCKSCREEN);
        tIntentFilter.addAction(BROADCAST_ACTION_APPLYING_OVERLAY);

        mLocalBroadcastManager.registerReceiver(mReceiver, tIntentFilter);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return new ThemeManageBinder();
    }

    public class ThemeManageBinder extends Binder {

        public Theme getThemeInfo(String packageName) {
            return serviceGetThemeInfo(packageName, false);
        }

        public List<Theme> getThemesList() {
            return serviceGetThemesList();
        }

        public void removeThemeOverlays(Bundle bundle) {
            serviceRemoveThemeOverlays(bundle);
        }

        public boolean applyTheme(Bundle bundle) {
            return serviceApplyTheme(bundle);
        }

        public Drawable getThemeBanner(String packageName) {
            return serviceGetThemeBanner(packageName);
        }

        public List<Drawable> getThemePreviewList(String packageName) {
            return serviceGetThemePreviewList(packageName);
        }
    }

    // 判断是否主题包
    private boolean isThemePackage(String packageName) {
        boolean ret = false;
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

    private Drawable serviceGetThemeBanner(String packageName) {
        try {
            Resources resources = mPackageManager.getResourcesForApplication(packageName);
            int bannerResId = resources.getIdentifier("banner", "drawable", packageName);
            return  resources.getDrawable(bannerResId, null);
        } catch (Exception e) {
            Log.e(TAG, "Failed to get banner of " + packageName);
        }
        return null;
    }

    private Drawable getThemeImage(String packageName) {
        try {
            Resources resources = mPackageManager.getResourcesForApplication(packageName);
            int imageResId = resources.getIdentifier("image", "drawable", packageName);
            return  resources.getDrawable(imageResId, null);
        } catch (Exception e) {
            Log.e(TAG, "Failed to get image of " + packageName);
        }
        return null;
    }

    private List<Drawable> serviceGetThemePreviewList(String packageName) {
        List<Drawable> previewList = new ArrayList<>();

        try {
            Resources resources = mPackageManager.getResourcesForApplication(packageName);
            AssetManager assetManager = resources.getAssets();

            // get previews
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

    private Theme serviceGetThemeInfo(String packageName, boolean getImage) {
        try {
            DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
            ApplicationInfo ai = mPackageManager.getApplicationInfo(packageName, PackageManager.GET_META_DATA);
            Resources resources = mPackageManager.getResourcesForApplication(packageName);
            Theme theme = new Theme(packageName);
            theme.setName(ai.loadLabel(mPackageManager).toString());

            Stack<String> xmlTags = new Stack<>();
            Map<String, Integer> attrMap = new HashMap<>();
            List<OverlayTarget> overlayTargetList = new ArrayList<>();
            int themeInfoXmlResId = resources.getIdentifier("theme_info", "xml", packageName);
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
                            // basic info
                            case "author":
                                theme.setAuthor(themeInfoXml.getText());
                                break;

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

            theme.setIsSystemPackage((ai.flags & ApplicationInfo.FLAG_SYSTEM) != 0);

            if (getImage) {
                int imageResId = resources.getIdentifier("image", "drawable", packageName);
                theme.setThemeImage(resources.getDrawable(imageResId, null));
            }

            return theme;
        } catch (Exception e) {
            Log.e(TAG, "Failed to get theme info: " + packageName);
            return null;
        }
    }

    // 取得已安装的主题列表
    private List<Theme> serviceGetThemesList() {
        List<Theme> themeArrayList = new ArrayList<>();
        List<PackageInfo> allPackages = mPackageManager.getInstalledPackages(0);

        for (PackageInfo pkgInfo : allPackages) {
            try {
                if (isThemePackage(pkgInfo.packageName)) {
                    Theme theme = serviceGetThemeInfo(pkgInfo.packageName, true);
                    if (theme != null) themeArrayList.add(theme);
                }
            } catch (Exception e) {
                Log.e(TAG, "Failed to get themes list");
            }
        }

        return themeArrayList;
    }

    // 停用(卸载)主题叠加层
    private void serviceRemoveThemeOverlays(Bundle bundle) {

        List<PackageInfo> allPackages = mPackageManager.getInstalledPackages(0);
        List<String> whiteList = bundle.getStringArrayList("whitelist");

        boolean uninstallFlag = bundle.getBoolean("uninstall");
        int userId = UserHandle.myUserId();

        for (PackageInfo pkgInfo : allPackages) {
            ApplicationInfo ai = pkgInfo.applicationInfo;

            if (pkgInfo.packageName.startsWith(OverlayPackageHeader)) {

                if (whiteList.contains(pkgInfo.overlayTarget)) {
                    continue;
                }

                //disable
                try {
                    mOverlayService.setEnabled(ai.packageName, false, userId);
                } catch (RemoteException e) {
                    Log.e(TAG, "Failed to disable overlay " + ai.packageName);
                }

                // uninstall
                if (uninstallFlag) {
                    PackageUtil.uninstallPackage(this, ai.packageName, null);
                }
            }
        }
    }

    // 应用主题
    private boolean serviceApplyTheme(Bundle bundle) {

        Theme theme = serviceGetThemeInfo(bundle.getString("package"), false);
        ArrayList<String> whiteList = bundle.getStringArrayList("whitelist");

        final int userId = UserHandle.myUserId();
        boolean ret = true;

        try {
            List<OverlayTarget> overlayTargetPackages = theme.getOverlayTargets();
            Resources themeResources = mPackageManager.getResourcesForApplication(theme.getPackageName());
            AssetManager themeAssetManager = themeResources.getAssets();

            mLocalBroadcastManager.sendBroadcast(getBroadcastIntent(BROADCAST_ACTION_APPLYING, theme));

            if (theme.hasRingtone() && bundle.getBoolean("ringtone")) {
                mLocalBroadcastManager.sendBroadcast(getBroadcastIntent(BROADCAST_ACTION_APPLYING_RINGTONE, theme));
                InputStream is = themeAssetManager.open("sounds/" + theme.getRingtone());
                SoundUtil.setRingtone(this, theme.getRingtone(), is, SoundUtil.TYPE_RINGTONE);
            }

            if (theme.hasAlarmSound() && bundle.getBoolean("alarm")) {
                mLocalBroadcastManager.sendBroadcast(getBroadcastIntent(BROADCAST_ACTION_APPLYING_ALARM, theme));
                InputStream is = themeAssetManager.open("sounds/" + theme.getAlarmSound());
                SoundUtil.setRingtone(this, theme.getAlarmSound(), is, SoundUtil.TYPE_ALARM);
            }

            if (theme.hasNotificationSound() && bundle.getBoolean("notification")) {
                mLocalBroadcastManager.sendBroadcast(getBroadcastIntent(BROADCAST_ACTION_APPLYING_NOTIFICATION, theme));
                InputStream is = themeAssetManager.open("sounds/" + theme.getNotificationSound());
                SoundUtil.setRingtone(this, theme.getNotificationSound(), is, SoundUtil.TYPE_NOTIFICATION);
            }

            if (theme.hasWallpaper() && bundle.getBoolean("wallpaper")) {
                mLocalBroadcastManager.sendBroadcast(getBroadcastIntent(BROADCAST_ACTION_APPLYING_WALLPAPER, theme));
                InputStream is = themeAssetManager.open("backgrounds/" + theme.getWallpaper());
                WallpaperUtil.setWallpaper(this, is);
            }

            if (theme.hasLockScreen() && bundle.getBoolean("lockscreen")) {
                mLocalBroadcastManager.sendBroadcast(getBroadcastIntent(BROADCAST_ACTION_APPLYING_LOCKSCREEN, theme));
                InputStream is = themeAssetManager.open("backgrounds/" + theme.getLockScreen());
                WallpaperUtil.setLockScreen(this, is);
            }

            int nowApplied = 1;
            Intent tIntent = getBroadcastIntent(BROADCAST_ACTION_APPLYING_OVERLAY, theme);
            tIntent.putExtra("max", overlayTargetPackages.size());

            for (OverlayTarget ovt : overlayTargetPackages) {
                if (whiteList.contains(ovt.getPackageName())) {
                    continue;
                }

                tIntent.putExtra("progress", nowApplied);
                tIntent.putExtra("nowPackageLabel", ovt.getLabel());
                mLocalBroadcastManager.sendBroadcast(tIntent);

                // install & enable
                InputStream is = themeAssetManager.open("overlay/" + ovt.getPackageName());

                AtomicBoolean installResult = new AtomicBoolean();
                installResult.set(false);

                synchronized (installResult) {
                    PackageUtil.installPackage(this, is, new PackageUtil.PackageInstallerStatusListener() {
                        @Override
                        public void onSuccessListener(String packageName) {
                            // check installed package for security reasons
                            if (!packageName.startsWith(OverlayPackageHeader) || !serviceIsOverlayPackage(packageName)) {
                                Log.w(TAG, "Package " + packageName + " is not a verified overlay package!");
                                onFailureListener(packageName, 0);
                                return;
                            }
                            try {
                                mOverlayService.setEnabled(packageName, true, userId);
                            } catch (RemoteException e) {
                                Log.e(TAG, "Failed to enable overlay " + packageName);
                                onFailureListener(packageName, 0);
                                return;
                            }
                            synchronized (installResult) {
                                installResult.set(true);
                                installResult.notify();
                            }
                        }

                        @Override
                        public void onFailureListener(String packageName, int code) {
                            Bundle removeBundle = new Bundle();
                            removeBundle.putStringArrayList("whitelist", whiteList);
                            removeBundle.putBoolean("uninstall", true);

                            serviceRemoveThemeOverlays(removeBundle);

                            synchronized (installResult) {
                                installResult.set(false);
                                installResult.notify();
                            }
                        }
                    });

                    installResult.wait();

                    if (!installResult.get()) {
                        ret = false;
                        break;
                    }
                }
                nowApplied++;
            }

        } catch (Exception e) {
            Log.e(TAG, "Failed to apply theme " + theme.getPackageName());
            ret = false;
        }

        if (ret) {
            mLocalBroadcastManager.sendBroadcast(getBroadcastIntent(BROADCAST_ACTION_APPLY_SUCCEED, theme));
        } else {
            mLocalBroadcastManager.sendBroadcast(getBroadcastIntent(BROADCAST_ACTION_APPLY_FAILED, theme));
        }

        return ret;
    }

    // 判断是否 Overlay 包
    private boolean serviceIsOverlayPackage(String packageName) {
        boolean ret = false;
        try {
            PackageInfo pi = mPackageManager.getPackageInfo(packageName, 0);
            ApplicationInfo ai = pi.applicationInfo;
            ret =  pi.isOverlayPackage() && ((ai.flags & ApplicationInfo.FLAG_HAS_CODE) == 0);
        } catch (Exception e) {
            Log.e(TAG, "check package " + packageName + " failed");
        }
        return ret;
    }

    // 获取叠加层目标的信息
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

    private static Intent getBroadcastIntent(String action, Theme theme) {
        Intent intent = new Intent(action);
        intent.putExtra("themeName", theme.getName());
        intent.putExtra("themePackage", theme.getPackageName());
        return intent;
    }
}
