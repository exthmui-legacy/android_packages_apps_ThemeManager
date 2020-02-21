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
import android.content.om.IOverlayManager;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.UserHandle;
import android.util.Log;

import org.exthmui.theme.models.OverlayTarget;
import org.exthmui.theme.models.ThemeAccent;
import org.exthmui.theme.models.ThemeBase;
import org.exthmui.theme.models.ThemeItem;
import org.exthmui.theme.utils.PackageUtil;
import org.exthmui.theme.utils.SoundUtil;
import org.exthmui.theme.utils.WallpaperUtil;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

public class ThemeManageService extends Service {

    private final static String TAG = "ThemeManageService";

    public final static String BROADCAST_ACTION_APPLY_RESULT = "org.exthmui.theme.THEME_APPLY_RESULT";
    public final static String THEME_APPLY_SUCCEED = "APPLY_SUCCEED";
    public final static String THEME_APPLY_FAILED = "APPLY_FAILED";
    public final static String THEME_APPLYING = "APPLYING";
    public final static String THEME_APPLYING_RINGTONE = "APPLYING_RINGTONE";
    public final static String THEME_APPLYING_ALARM = "APPLYING_ALARM";
    public final static String THEME_APPLYING_NOTIFICATION = "APPLYING_NOTIFICATION";
    public final static String THEME_APPLYING_WALLPAPER = "APPLYING_WALLPAPER";
    public final static String THEME_APPLYING_LOCKSCREEN = "APPLYING_LOCKSCREEN";
    public final static String THEME_APPLYING_OVERLAY = "APPLYING_OVERLAY";

    private IOverlayManager mOverlayService;
    private PackageManager mPackageManager;
    private ThemeApplyStatusListener mApplyStatusListener;
    private Queue<Intent> mApplyStatusQueue;

    @Override
    public void onCreate() {
        mOverlayService = IOverlayManager.Stub.asInterface(ServiceManager.getService("overlay"));
        mPackageManager = getPackageManager();
        mApplyStatusQueue = new LinkedBlockingQueue<>();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return new ThemeManageBinder();
    }

    public class ThemeManageBinder extends Binder {
        public boolean applyTheme(ThemeItem theme, Bundle bundle) {
            return IApplyTheme(theme, bundle);
        }

        public boolean applyThemeAccent(ThemeAccent themeAccent, Bundle bundle) {
            return IApplyThemeAccent(themeAccent, bundle);
        }

        public void removeThemeOverlays(Bundle bundle) {
            IRemoveThemeOverlays(bundle);
        }

        public void setThemeApplyStatusListener(ThemeApplyStatusListener listener) {
            mApplyStatusListener = listener;
            notifyThemeApplyStatus();
        }
    }

    public interface ThemeApplyStatusListener {
        void update(Intent data);
    }

    private void IRemoveThemeOverlays(Bundle bundle) {

        List<PackageInfo> allPackages = mPackageManager.getInstalledPackages(0);
        boolean uninstallFlag = false;
        if (bundle != null) {
            uninstallFlag = bundle.getBoolean("uninstall");
        }

        int userId = UserHandle.myUserId();
        final AtomicBoolean removeResult = new AtomicBoolean();
        removeResult.set(false);

        for (PackageInfo pkgInfo : allPackages) {
            ApplicationInfo ai = pkgInfo.applicationInfo;

            if (isThemeOverlayPackage(pkgInfo.packageName)) {

                if (bundle != null && !bundle.getBoolean(pkgInfo.overlayTarget, true)) {
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
                    synchronized (removeResult) {
                        PackageUtil.uninstallPackage(this, ai.packageName, new PackageUtil.PackageInstallerCallback() {
                            @Override
                            public void onSuccess(String packageName) {
                                synchronized (removeResult) {
                                    removeResult.set(true);
                                    removeResult.notify();
                                }
                            }

                            @Override
                            public void onFailure(String packageName, int code) {
                                synchronized (removeResult) {
                                    removeResult.set(false);
                                    removeResult.notify();
                                }
                            }
                        });

                        try {
                            removeResult.wait();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }
    }

    private boolean IApplyTheme(ThemeItem theme, Bundle bundle) {
        final int userId = UserHandle.myUserId();
        boolean ret = true;
        mApplyStatusQueue.clear();

        try {
            List<OverlayTarget> overlayTargetPackages = theme.getOverlayTargets();
            Resources themeResources = mPackageManager.getResourcesForApplication(theme.getPackageName());
            AssetManager themeAssetManager = themeResources.getAssets();

            setThemeApplyStatus(THEME_APPLYING, theme);

            IRemoveThemeOverlays(bundle);

            if (theme.hasRingtone() && bundle.getBoolean("theme.ringtone")) {
                setThemeApplyStatus(THEME_APPLYING_RINGTONE, theme);
                InputStream is = themeAssetManager.open("sounds/" + theme.getRingtone());
                SoundUtil.setRingtone(this, theme.getRingtone(), is, SoundUtil.TYPE_RINGTONE);
            }

            if (theme.hasAlarmSound() && bundle.getBoolean("theme.alarm")) {
                setThemeApplyStatus(THEME_APPLYING_ALARM, theme);
                InputStream is = themeAssetManager.open("sounds/" + theme.getAlarmSound());
                SoundUtil.setRingtone(this, theme.getAlarmSound(), is, SoundUtil.TYPE_ALARM);
            }

            if (theme.hasNotificationSound() && bundle.getBoolean("theme.notification")) {
                setThemeApplyStatus(THEME_APPLYING_NOTIFICATION, theme);
                InputStream is = themeAssetManager.open("sounds/" + theme.getNotificationSound());
                SoundUtil.setRingtone(this, theme.getNotificationSound(), is, SoundUtil.TYPE_NOTIFICATION);
            }

            if (theme.hasWallpaper() && bundle.getBoolean("theme.wallpaper")) {
                setThemeApplyStatus(THEME_APPLYING_WALLPAPER, theme);
                InputStream is = themeAssetManager.open("backgrounds/" + theme.getWallpaper());
                WallpaperUtil.setWallpaper(this, is);
            }

            if (theme.hasLockScreen() && bundle.getBoolean("theme.lockscreen")) {
                setThemeApplyStatus(THEME_APPLYING_LOCKSCREEN, theme);
                InputStream is = themeAssetManager.open("backgrounds/" + theme.getLockScreen());
                WallpaperUtil.setLockScreen(this, is);
            }

            int progress = 0;
            Intent extDataIntent = new Intent();
            extDataIntent.putExtra("progressMax", overlayTargetPackages.size());

            for (OverlayTarget ovt : overlayTargetPackages) {
                progress++;
                if (!bundle.getBoolean(ovt.getPackageName())) {
                    continue;
                }
                extDataIntent.putExtra("progressVal", progress);
                extDataIntent.putExtra("nowPackageLabel", ovt.getLabel());

                setThemeApplyStatus(THEME_APPLYING_OVERLAY, theme, extDataIntent);

                // install & enable
                InputStream is = themeAssetManager.open("overlay/" + ovt.getPackageName());

                final AtomicBoolean installResult = new AtomicBoolean();
                installResult.set(false);

                synchronized (installResult) {
                    PackageUtil.installPackage(this, is, new PackageUtil.PackageInstallerCallback() {
                        @Override
                        public void onSuccess(String packageName) {
                            // check installed package for security reasons
                            if (!isThemeOverlayPackage(packageName)) {
                                Log.w(TAG, "Package " + packageName + " is not a verified overlay package!");
                                onFailure(packageName, 0);
                                return;
                            }
                            try {
                                while (mOverlayService.getOverlayInfo(packageName, userId) == null) {
                                    Thread.sleep(50);
                                }
                                mOverlayService.setEnabled(packageName, true, userId);
                            } catch (Exception e) {
                                Log.e(TAG, "Failed to enable overlay " + packageName);
                                onFailure(packageName, 0);
                                return;
                            }
                            synchronized (installResult) {
                                installResult.set(true);
                                installResult.notify();
                            }
                        }

                        @Override
                        public void onFailure(String packageName, int code) {
                            IRemoveThemeOverlays(bundle);

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
            }

        } catch (Exception e) {
            Log.e(TAG, "Failed to apply theme " + theme.getPackageName());
            ret = false;
        }

        if (ret) {
            setThemeApplyStatus(THEME_APPLY_SUCCEED, theme);
        } else {
            setThemeApplyStatus(THEME_APPLY_FAILED, theme);
        }
        return ret;
    }

    private boolean IApplyThemeAccent(ThemeAccent theme, Bundle bundle) {
        final int userId = UserHandle.myUserId();
        boolean ret = true;
        mApplyStatusQueue.clear();

        try {
            setThemeApplyStatus(THEME_APPLYING, theme);
            IRemoveThemeOverlays(bundle);
            mOverlayService.setEnabled(theme.getPackageName(), true, userId);
        } catch (Exception e) {
            Log.e(TAG, "Failed to apply theme " + theme.getPackageName());
            ret = false;
        }

        if (ret) {
            setThemeApplyStatus(THEME_APPLY_SUCCEED, theme);
        } else {
            setThemeApplyStatus(THEME_APPLY_FAILED, theme);
        }
        return ret;
    }

    private boolean isThemeOverlayPackage(String packageName) {
        boolean ret = false;
        try {
            PackageInfo pi = mPackageManager.getPackageInfo(packageName, PackageManager.GET_META_DATA);
            ApplicationInfo ai = pi.applicationInfo;
            ret =  pi.isOverlayPackage() &&
                    ((ai.flags & ApplicationInfo.FLAG_HAS_CODE) == 0) &&
                    ((ai.metaData.getBoolean("exthmui_theme_overlay", false)) ||
                    (ai.metaData.getInt("lineage_berry_accent_preview",0) != 0));
        } catch (Exception e) {
            Log.e(TAG, "check package " + packageName + " failed");
        }
        return ret;
    }

    private void setThemeApplyStatus(String action, ThemeBase theme) {
        setThemeApplyStatus(action, theme, null);
    }

    private void setThemeApplyStatus(String status, ThemeBase theme, Intent extraData) {
        Intent intent = new Intent(ThemeManageService.BROADCAST_ACTION_APPLY_RESULT);
        intent.setComponent(new ComponentName(getPackageName(), getPackageName() + ".broadcasts.ThemeStatusReceiver"));
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
        while (!mApplyStatusQueue.isEmpty() && mApplyStatusListener != null) {
            mApplyStatusListener.update(mApplyStatusQueue.poll());
        }
    }

}
