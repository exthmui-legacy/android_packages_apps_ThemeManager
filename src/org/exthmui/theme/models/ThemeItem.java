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

package org.exthmui.theme.models;

import android.os.Parcel;

import java.util.ArrayList;
import java.util.List;

public class ThemeItem extends ThemeBase {

    private String mWallpaper;
    private String mLockScreen;

    private String mAlarmSound;
    private String mNotificationSound;
    private String mRingtone;
    private String mAlarmTitle;
    private String mRingtoneTitle;
    private String mNotificationTitle;
    public boolean hasBootanimation = false;
    public boolean hasFonts = false;

    private List<ThemeTarget> mOverlayTargets;

    public ThemeItem(String packageName) {
        super(packageName);
    }

    private ThemeItem(Parcel in) {
        super(in);
        mWallpaper = in.readString();
        mLockScreen = in.readString();
        mAlarmTitle = in.readString();
        mAlarmSound = in.readString();
        mNotificationTitle = in.readString();
        mNotificationSound = in.readString();
        mRingtoneTitle = in.readString();
        mRingtone = in.readString();
        hasBootanimation = in.readBoolean();
        hasFonts = in.readBoolean();
        mOverlayTargets = in.readParcelableList(new ArrayList<>(), ThemeTarget.class.getClassLoader());
    }

    public static final Creator<ThemeBase> CREATOR = new Creator<ThemeBase>() {
        @Override
        public ThemeBase createFromParcel(Parcel in) {
            return new ThemeItem(in);
        }

        @Override
        public ThemeBase[] newArray(int size) {
            return new ThemeItem[size];
        }
    };

    public void setAlarmSound(String alarmSound) {
        mAlarmSound = alarmSound;
    }

    public void setNotificationSound(String notificationSound) {
        mNotificationSound = notificationSound;
    }

    public void setRingtone(String ringtone) {
        mRingtone = ringtone;
    }

    public void setLockScreen(String lockScreen) {
        mLockScreen = lockScreen;
    }

    public void setAlarmTitle(String title) {
        mAlarmTitle = title;
    }
    
    public void setRingtoneTitle(String title) {
        mRingtoneTitle = title;
    }

    public void setNotificationTitle(String title) {
        mNotificationTitle = title;
    }

    public void setWallpaper(String wallpaper) {
        mWallpaper = wallpaper;
    }

    public void setOverlayTargets(List<ThemeTarget> overlayTargets) {
        mOverlayTargets = overlayTargets;
    }

    public List<ThemeTarget> getOverlayTargets() {
        return mOverlayTargets;
    }

    public boolean hasOverlays() {
        return !mOverlayTargets.isEmpty();
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

    public String getWallpaper() {
        return mWallpaper;
    }

    public String getLockScreen() {
        return mLockScreen;
    }

    public String getAlarmSound() {
        return mAlarmSound;
    }

    public String getNotificationSound() {
        return mNotificationSound;
    }

    public String getRingtone() {
        return mRingtone;
    }

    public String getRingtoneTitle() {
        return mRingtoneTitle;
    }

    public String getAlarmTitle() {
        return mAlarmTitle;
    }

    public String getNotificationTitle() {
        return mNotificationTitle;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeString(mWallpaper);
        dest.writeString(mLockScreen);
        dest.writeString(mAlarmTitle);
        dest.writeString(mAlarmSound);
        dest.writeString(mNotificationTitle);
        dest.writeString(mNotificationSound);
        dest.writeString(mRingtoneTitle);
        dest.writeString(mRingtone);
        dest.writeBoolean(hasBootanimation);
        dest.writeBoolean(hasFonts);
        dest.writeParcelableList(mOverlayTargets, flags);
    }
}
