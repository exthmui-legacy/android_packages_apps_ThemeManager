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

public class ThemeBase implements Parcelable {

    private String mTitle;
    private final String mPackageName;
    private String mAuthor;
    private boolean mRemovable;

    public ThemeBase(String packageName) {
        mPackageName = packageName;
    }

    public ThemeBase(Parcel in) {
        mTitle = in.readString();
        mPackageName = in.readString();
        mAuthor = in.readString();
        mRemovable = in.readBoolean();
    }

    public static final Creator<ThemeBase> CREATOR = new Creator<ThemeBase>() {
        @Override
        public ThemeBase createFromParcel(Parcel in) {
            return new ThemeBase(in);
        }

        @Override
        public ThemeBase[] newArray(int size) {
            return new ThemeBase[size];
        }
    };

    public void setTitle(String title) {
        mTitle = title;
    }

    public void setAuthor(String author) {
        mAuthor = author;
    }

    public void setRemovable(boolean val) {
        mRemovable = val;
    }

    public String getTitle() {
        return mTitle;
    }

    public String getAuthor() {
        return mAuthor;
    }

    public String getPackageName() {
        return mPackageName;
    }

    public boolean isRemovable() {
        return mRemovable;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(mTitle);
        dest.writeString(mPackageName);
        dest.writeString(mAuthor);
        dest.writeBoolean(mRemovable);
    }
}
