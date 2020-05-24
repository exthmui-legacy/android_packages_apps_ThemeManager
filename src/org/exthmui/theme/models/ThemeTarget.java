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
import android.os.Parcelable;

public class ThemeTarget implements Parcelable, Comparable {

    public final static int TYPE_BACKGROUNDS = 1;
    public final static int TYPE_SOUNDS = 2;
    public final static int TYPE_OTHERS = 3;
    public final static int TYPE_APPLICATIONS = 4;

    private String mTargetLabel;
    private String mTargetId;
    private String mData;
    private boolean mIsSwitchable;
    private boolean mIsSelected = true;
    private boolean mIsSubtitle;
    private final int mTargetType;

    public ThemeTarget(String id, int type) {
        this.mTargetId = id;
        this.mTargetType = type;
    }

    private ThemeTarget(Parcel in) {
        mTargetLabel = in.readString();
        mTargetId = in.readString();
        mData = in.readString();
        mIsSwitchable = in.readBoolean();
        mIsSelected = in.readBoolean();
        mIsSubtitle = in.readBoolean();
        mTargetType = in.readInt();
    }

    public static final Creator<ThemeTarget> CREATOR = new Creator<ThemeTarget>() {
        @Override
        public ThemeTarget createFromParcel(Parcel in) {
            return new ThemeTarget(in);
        }

        @Override
        public ThemeTarget[] newArray(int size) {
            return new ThemeTarget[size];
        }
    };

    public void setLabel(String label) {
        mTargetLabel = label;
    }

    public void setData(String data) {
        mData = data;
    }

    public void setIsSubtitle(boolean val) {
        mIsSubtitle = val;
    }

    public void setSwitchable(boolean switchable) {
        mIsSwitchable = switchable;
    }

    public void select(boolean val) {
        mIsSelected = val;
    }

    public String getLabel() {
        return mTargetLabel;
    }

    public String getTargetId() {
        return mTargetId;
    }

    public boolean isSwitchable() {
        return mIsSwitchable;
    }

    public boolean isSubtitle() {
        return mIsSubtitle;
    }

    public boolean isSelected() {
        return mIsSelected;
    }

    public int getTargetType() {
        return mTargetType;
    }

    public String getData() {
        return mData;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        ThemeTarget other = (ThemeTarget) obj;
        return (this.mTargetId.equals(other.mTargetId) &&
                this.mTargetLabel.equals(other.mTargetLabel) &&
                this.mTargetType == other.mTargetType);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(mTargetLabel);
        dest.writeString(mTargetId);
        dest.writeString(mData);
        dest.writeBoolean(mIsSwitchable);
        dest.writeBoolean(mIsSelected);
        dest.writeBoolean(mIsSelected);
        dest.writeInt(mTargetType);
    }

    @Override
    public int compareTo(Object o) {
        ThemeTarget target = (ThemeTarget) o;
        if (this.mTargetType == target.mTargetType) {
            if (this.mIsSubtitle) {
                return -1;
            } else {
                return 0;
            }
        } else {
            return this.mTargetType - target.mTargetType;
        }
    }
}
