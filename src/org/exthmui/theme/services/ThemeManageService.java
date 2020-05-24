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
import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.om.IOverlayManager;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.graphics.BitmapFactory;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.util.Log;

import androidx.preference.PreferenceManager;

import org.exthmui.theme.misc.Constants;
import org.exthmui.theme.models.ThemeBase;
import org.exthmui.theme.models.ThemeItem;
import org.exthmui.theme.models.ThemeTarget;
import org.exthmui.theme.utils.FileUtil;
import org.exthmui.theme.utils.PackageUtil;
import org.exthmui.theme.utils.SoundUtil;
import org.exthmui.theme.utils.WallpaperUtil;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Vector;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

public class ThemeManageService extends Service {

    private final static String TAG = "ThemeManageService";

    private IOverlayManager mOverlayService;
    private PackageManager mPackageManager;
    private List<ThemeApplyStatusListener> mApplyStatusListenerList;
    private Queue<Intent> mApplyStatusQueue;

    @Override
    public void onCreate() {
        mOverlayService = IOverlayManager.Stub.asInterface(ServiceManager.getService("overlay"));
        mPackageManager = getPackageManager();
        mApplyStatusQueue = new LinkedBlockingQueue<>();
        mApplyStatusListenerList = new Vector<>();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return new ThemeManageBinder();
    }

    public class ThemeManageBinder extends Binder {
        public void applyTheme(ThemeItem theme, Bundle bundle) {
            new Thread(() -> {
                setThemeApplyStatus(Constants.THEME_APPLYING, theme);
                boolean ret = IApplyTheme(theme, bundle);
                if (ret) {
                    setThemeApplyStatus(Constants.THEME_APPLY_SUCCEED, theme);
                } else {
                    setThemeApplyStatus(Constants.THEME_APPLY_FAILED, theme);
                }
            }).start();
        }

        public void addThemeApplyStatusListener(ThemeApplyStatusListener listener) {
            mApplyStatusListenerList.add(listener);
            notifyThemeApplyStatus();
        }

        public void removeThemeApplyStatusListener(ThemeApplyStatusListener listener) {
            mApplyStatusListenerList.remove(listener);
        }
    }

    public interface ThemeApplyStatusListener {
        // returns true when data is successfully processed
        boolean update(Intent data);
    }

    private boolean IApplyTheme(ThemeItem theme, Bundle bundle) {
        final int userId = UserHandle.myUserId();
        final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        final boolean uninstallFlag = preferences.getBoolean(Constants.PREFERENCES_OVERLAY_REMOVE_FLAG, false);
        final boolean wallpaperCenterFlag = preferences.getBoolean(Constants.PREFERENCES_FORCED_CENTER_WALLPAPER, false);
        boolean needCleanFonts = true;
        boolean needCleanBootanim = true;

        List<ThemeTarget> overlayTargetPackages = theme.getOverlayTargets();
        Map<String, String> themeOverlays = new HashMap<>();

        Resources themeResources;
        try {
            themeResources = mPackageManager.getResourcesForApplication(theme.getPackageName());
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            return false;
        }

        AssetManager themeAssetManager = themeResources.getAssets();

        // install overlay
        if (!overlayTargetPackages.isEmpty()) setThemeApplyStatus(Constants.THEME_INSTALLING_OVERLAY, theme);
        for (ThemeTarget ovt : overlayTargetPackages) {
            if (!bundle.getBoolean(ovt.getTargetId(), true)) {
                continue;
            }
            InputStream is;
            try {
                is = themeAssetManager.open(Constants.THEME_DATA_ASSETS_OVERLAY + "/" + ovt.getTargetId());
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }

            final AtomicBoolean installResult = new AtomicBoolean();

            synchronized (installResult) {
                PackageUtil.installPackage(this, is, new PackageUtil.PackageInstallerCallback() {
                    @Override
                    public void onResponse(String packageName, int code) {
                        // check installed package for security reasons
                        if (!isThemeOverlayPackage(packageName)) {
                            Log.w(TAG, "Package " + packageName + " is not a verified overlay package!");
                        } else {
                            themeOverlays.put(ovt.getTargetId(), packageName);
                        }
                        synchronized (installResult) {
                            installResult.set(code == 0);
                            installResult.notify();
                        }
                    }
                });

                try {
                    installResult.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    return false;
                }

                if (!installResult.get()) {
                    return false;
                }
            }
        }

        // wallpaper and lockscreen
        if (theme.hasWallpaper() && bundle.getBoolean(Constants.THEME_TARGET_WALLPAPER, true)) {
            setThemeApplyStatus(Constants.THEME_APPLYING_WALLPAPER, theme);
            try {
                InputStream is = themeAssetManager.open(Constants.THEME_DATA_ASSETS_BACKGROUNDS + "/" + theme.getWallpaper());
                WallpaperUtil.setWallpaper(this, BitmapFactory.decodeStream(is), wallpaperCenterFlag);
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
        }

        if (theme.hasLockScreen() && bundle.getBoolean(Constants.THEME_TARGET_LOCKSCREEN, true)) {
            setThemeApplyStatus(Constants.THEME_APPLYING_LOCKSCREEN, theme);
            try {
                InputStream is = themeAssetManager.open(Constants.THEME_DATA_ASSETS_BACKGROUNDS + "/" + theme.getLockScreen());
                WallpaperUtil.setLockScreen(this, BitmapFactory.decodeStream(is), true);
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
        }

        // sounds
        if (theme.hasRingtone() && bundle.getBoolean(Constants.THEME_TARGET_RINGTONE, true)) {
            setThemeApplyStatus(Constants.THEME_APPLYING_RINGTONE, theme);
            try {
                InputStream is = themeAssetManager.open(Constants.THEME_DATA_ASSETS_SOUNDS + "/" + theme.getRingtone());
                if (!SoundUtil.setRingtone(this, theme.getRingtone(), is, SoundUtil.TYPE_RINGTONE)) return false;
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
        }

        if (theme.hasAlarmSound() && bundle.getBoolean(Constants.THEME_TARGET_ALARM, true)) {
            setThemeApplyStatus(Constants.THEME_APPLYING_ALARM, theme);
            try {
                InputStream is = themeAssetManager.open(Constants.THEME_DATA_ASSETS_SOUNDS + "/" + theme.getAlarmSound());
                if (!SoundUtil.setRingtone(this, theme.getAlarmSound(), is, SoundUtil.TYPE_ALARM)) return false;
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
        }

        if (theme.hasNotificationSound() && bundle.getBoolean(Constants.THEME_TARGET_NOTIFICATION, true)) {
            setThemeApplyStatus(Constants.THEME_APPLYING_NOTIFICATION, theme);
            try {
                InputStream is = themeAssetManager.open(Constants.THEME_DATA_ASSETS_SOUNDS + "/" + theme.getNotificationSound());
                if (!SoundUtil.setRingtone(this, theme.getNotificationSound(), is, SoundUtil.TYPE_NOTIFICATION)) return false;
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
        }

        // bootanimation
        if (theme.hasBootanimation && bundle.getBoolean(Constants.THEME_TARGET_BOOTANIMATION, true)) {
            setThemeApplyStatus(Constants.THEME_APPLYING_BOOTANIMATION, theme);
            needCleanBootanim = false;
            try {
                File bootanimFile = new File(Constants.THEME_DATA_BOOTANIMATION_PATH + "/bootanimation.zip");
                File darkBootanimFile = new File(Constants.THEME_DATA_BOOTANIMATION_PATH + "/bootanimation-dark.zip");
                bootanimFile.delete(); darkBootanimFile.delete();
                InputStream is = themeAssetManager.open(Constants.THEME_DATA_ASSETS_MEDIA + "/bootanimation.zip");
                FileUtil.saveInputStream(bootanimFile.getAbsolutePath(), is, true);
                try {
                    InputStream darkIS = themeAssetManager.open(Constants.THEME_DATA_ASSETS_MEDIA + "/bootanimation-dark.zip");
                    FileUtil.saveInputStream(darkBootanimFile.getAbsolutePath(), darkIS, true);
                } catch (IOException e) {
                    // could not found dark mode animation
                    Runtime.getRuntime().exec("ln -s " + bootanimFile.getAbsolutePath() + " " + darkBootanimFile.getAbsolutePath());
                }
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
        }

        // fonts
        if (theme.hasFonts && bundle.getBoolean(Constants.THEME_TARGET_FONTS, true)) {
            setThemeApplyStatus(Constants.THEME_APPLYING_FONTS, theme);
            needCleanFonts = false;
            File fontDir = new File(Constants.THEME_DATA_FONTS_PATH);
            File[] existFontFiles = fontDir.listFiles();
            if (existFontFiles != null) {
                for (File fontFile : existFontFiles) fontFile.delete();
            }
            try {
                String[] fontsArray = themeAssetManager.list(Constants.THEME_DATA_ASSETS_FONTS);
                if (fontsArray != null) {
                    for (String fileName : fontsArray) {
                        InputStream is = themeAssetManager.open(Constants.THEME_DATA_ASSETS_FONTS + "/" + fileName);
                        FileUtil.saveInputStream(Constants.THEME_DATA_FONTS_PATH + fileName, is, true);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }

            // use fake-fonts overlay to refresh font cache
            try {
                SystemProperties.set(Constants.PROP_REFRESH_FONTS, "true");
                mOverlayService.setEnabled(Constants.FAKE_FONTS_OVERLAY, true, userId);
                Thread.sleep(1000);
                mOverlayService.setEnabled(Constants.FAKE_FONTS_OVERLAY, false, userId);
            } catch (RemoteException | InterruptedException e) {
                e.printStackTrace();
                return false;
            }
        }

        // enable overlay
        int progress = 0;
        Intent extDataIntent = new Intent();
        extDataIntent.putExtra("progressMax", overlayTargetPackages.size());

        for (ThemeTarget ovt : overlayTargetPackages) {
            progress++;
            if (!bundle.getBoolean(ovt.getTargetId(), true)) {
                continue;
            }
            extDataIntent.putExtra("progressVal", progress);
            extDataIntent.putExtra("nowPackageLabel", ovt.getLabel());

            setThemeApplyStatus(Constants.THEME_APPLYING_OVERLAY, theme, extDataIntent);

            try {
                mOverlayService.setEnabled(themeOverlays.get(ovt.getTargetId()), true, userId);
            } catch (RemoteException e) {
                e.printStackTrace();
                return false;
            }
        }

        setThemeApplyStatus(Constants.THEME_CLEANING, theme);
        // delete fonts and bootanimation
        if (needCleanBootanim) {
            File bootanimFile = new File(Constants.THEME_DATA_BOOTANIMATION_PATH + "/bootanimation.zip");
            File darkBootanimFile = new File(Constants.THEME_DATA_BOOTANIMATION_PATH + "/bootanimation-dark.zip");
            bootanimFile.delete(); darkBootanimFile.delete();
        }
        if (needCleanFonts) {
            File fontDir = new File(Constants.THEME_DATA_FONTS_PATH);
            File[] existFontFiles = fontDir.listFiles();
            if (existFontFiles != null) {
                for (File fontFile : existFontFiles) fontFile.delete();
            }
            // use fake-fonts overlay to refresh font cache
            try {
                SystemProperties.set(Constants.PROP_REFRESH_FONTS, "true");
                mOverlayService.setEnabled(Constants.FAKE_FONTS_OVERLAY, true, userId);
                Thread.sleep(1000);
                mOverlayService.setEnabled(Constants.FAKE_FONTS_OVERLAY, false, userId);
            } catch (RemoteException | InterruptedException e) {
                e.printStackTrace();
                return false;
            }
        }
        // disable and uninstall old overlays
        List<PackageInfo> allPackages = mPackageManager.getInstalledPackages(0);
        for (PackageInfo pkgInfo : allPackages) {
            if (isThemeOverlayPackage(pkgInfo.packageName) && !themeOverlays.containsValue(pkgInfo.packageName)) {
                try {
                    mOverlayService.setEnabled(pkgInfo.packageName, false, userId);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
                if (uninstallFlag) {
                    PackageUtil.uninstallPackage(this, pkgInfo.packageName, null);
                }
            }
        }

        return true;
    }

    private boolean isThemeOverlayPackage(String packageName) {
        boolean ret = false;
        try {
            PackageInfo pi = mPackageManager.getPackageInfo(packageName, PackageManager.GET_META_DATA);
            ApplicationInfo ai = pi.applicationInfo;
            ret =  pi.isOverlayPackage() &&
                    ((ai.flags & ApplicationInfo.FLAG_HAS_CODE) == 0) && ai.metaData != null &&
                    ((ai.metaData.getBoolean(Constants.THEME_DATA_OVERLAY_FLAG, false)));
        } catch (Exception e) {
            Log.e(TAG, "check package " + packageName + " failed");
        }
        return ret;
    }

    private void setThemeApplyStatus(String action, ThemeBase theme) {
        setThemeApplyStatus(action, theme, null);
    }

    private void setThemeApplyStatus(String status, ThemeBase theme, Intent extraData) {
        Intent intent = new Intent(Constants.BROADCAST_ACTION_APPLY_RESULT);
        intent.setComponent(new ComponentName(getPackageName(), getPackageName() + Constants.BROADCAST_RECEIVER_NAME));
        intent.putExtra("status", status);
        intent.putExtra("themeTitle", theme.getTitle());
        intent.putExtra("themePackage", theme.getPackageName());
        if (extraData != null) {
            intent.putExtras(extraData);
        }
        mApplyStatusQueue.offer(intent);
        notifyThemeApplyStatus();
    }

    private void notifyThemeApplyStatus() {
        while (!mApplyStatusQueue.isEmpty() && !mApplyStatusListenerList.isEmpty()) {
            boolean popFlag = false;
            Intent data = mApplyStatusQueue.peek();
            for (ThemeApplyStatusListener listener : mApplyStatusListenerList) {
                if (listener.update(data)) popFlag = true;
            }
            if (popFlag) mApplyStatusQueue.poll();
        }
    }

}
