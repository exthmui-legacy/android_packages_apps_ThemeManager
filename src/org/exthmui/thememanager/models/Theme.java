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

package org.exthmui.thememanager.models;

import android.graphics.drawable.Drawable;

import java.util.List;

public class Theme {

    private String mName;
    private String mPackageName;
    private String mAuthor;
    private boolean mIsSystemPackage;

    private String mWallpaper;
    private String mLockScreen;

    private String mAlarmSound;
    private String mAlarmName;
    private String mNotificationSound;
    private String mNotificationName;
    private String mRingtone;
    private String mRingtoneName;

    private List<OverlayTarget> mOverlayTargets;

    private Drawable mThemeImage;

    public Theme(String packageName) {
        mPackageName = packageName;
    }

    public void setName(String name) {
        mName = name;
    }

    public void setAuthor(String author) {
        mAuthor = author;
    }

    public void setIsSystemPackage(boolean val) {
        mIsSystemPackage = val;
    }

    public void setAlarmSound(String file, String name) {
        mAlarmSound = file;
        mAlarmName = name;
    }

    public void setNotificationSound(String file, String name) {
        mNotificationSound = file;
        mNotificationName = name;
    }

    public void setRingtone(String file, String name) {
        mRingtone = file;
        mRingtoneName = name;
    }

    public void setLockScreen(String lockScreen) {
        mLockScreen = lockScreen;
    }

    public void setWallpaper(String wallpaper) {
        mWallpaper = wallpaper;
    }

    public void setThemeImage(Drawable themeImage) {
        mThemeImage = themeImage;
    }

    public void setOverlayTargets(List<OverlayTarget> overlayTargets) {
        mOverlayTargets = overlayTargets;
    }

    public List<OverlayTarget> getOverlayTargets() {
        return mOverlayTargets;
    }

    public String getName() {
        return mName;
    }

    public String getAuthor() {
        return mAuthor;
    }

    public String getPackageName() {
        return mPackageName;
    }

    public boolean hasOverlays() {
        return !mOverlayTargets.isEmpty();
    }

    public Drawable getThemeImage() {
        return mThemeImage;
    }

    public boolean hasWallpaper() {
        return mWallpaper != null && !mWallpaper.equals("");
    }

    public boolean hasLockScreen() {
        return mLockScreen != null && !mLockScreen.equals("");
    }

    public boolean hasAlarmSound() {
        return mAlarmSound != null && !mAlarmSound.equals("");
    }

    public boolean hasNotificationSound() {
        return mNotificationSound != null && !mNotificationSound.equals("");
    }

    public boolean hasRingtone() {
        return mRingtone != null && !mRingtone.equals("");
    }

    public boolean isSystemPackage() {
        return mIsSystemPackage;
    }

    public String getWallpaper() {
        return mWallpaper;
    }

    public String getLockScreen() {
        return mLockScreen;
    }

    public String getAlarmSound() {
        return mAlarmSound;
    }

    public String getAlarmName() {
        return "".equals(mAlarmName) ? getAlarmSound() : mAlarmName;
    }

    public String getNotificationName() {
        return "".equals(mNotificationName) ? getNotificationSound() : mNotificationName;
    }

    public String getRingtoneName() {
        return "".equals(mRingtoneName) ? getRingtone() : mRingtoneName;
    }

    public String getNotificationSound() {
        return mNotificationSound;
    }

    public String getRingtone() {
        return mRingtone;
    }

}
