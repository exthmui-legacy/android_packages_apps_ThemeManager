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

package org.exthmui.theme.utils;

import android.app.WallpaperManager;
import android.content.Context;
import android.util.Log;

import java.io.InputStream;

public class WallpaperUtil {

    private static final String TAG = "WallpaperUtil";

    public static void setWallpaper(Context context, InputStream inputStream) {

        WallpaperManager wallpaperManager = WallpaperManager.getInstance(context);

        try {
            wallpaperManager.setStream(inputStream,null,true, WallpaperManager.FLAG_SYSTEM);
        } catch (Exception e) {
            Log.e(TAG, "Failed to set wallpaper", e);
        }
    }

    public static void setLockScreen(Context context, InputStream inputStream) {

        WallpaperManager wallpaperManager = WallpaperManager.getInstance(context);

        try {
            wallpaperManager.setStream(inputStream,null,true, WallpaperManager.FLAG_LOCK);
        } catch (Exception e) {
            Log.e(TAG, "Failed to set lockscreen wallpaper", e);
        }
    }
}
