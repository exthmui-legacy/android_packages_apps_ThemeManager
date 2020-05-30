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
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.util.DisplayMetrics;
import android.util.Log;

public class WallpaperUtil {

    private static final String TAG = "WallpaperUtil";

    public static void setBackground(Context context, Bitmap bitmap, int flag, boolean center) {

        WallpaperManager wallpaperManager = WallpaperManager.getInstance(context);

        DisplayMetrics dm = context.getResources().getDisplayMetrics();
        Rect visibleCropHint = new Rect();
        if (center)
        {
            double ratio = (double) dm.heightPixels / dm.widthPixels;
            double imageRatio = (double) bitmap.getHeight() / bitmap.getWidth();
            if (ratio < imageRatio) {
                int offset = (int) (bitmap.getHeight() - bitmap.getWidth() * ratio) / 2;
                visibleCropHint.set(0, offset, bitmap.getWidth(), bitmap.getHeight() - offset);
            } else {
                int offset = (int) (bitmap.getWidth() - bitmap.getHeight() / ratio) / 2;
                visibleCropHint.set(offset, 0, bitmap.getWidth() - offset, bitmap.getHeight());
            }
        }

        try {
            wallpaperManager.setBitmap(bitmap, center ? visibleCropHint : null,true, flag);
        } catch (Exception e) {
            Log.e(TAG, "Failed to set background, flag=" + flag, e);
        }
    }


    public static void setWallpaper(Context context, Bitmap bitmap, boolean center) {
        setBackground(context, bitmap, WallpaperManager.FLAG_SYSTEM, center);
    }

    public static void setLockScreen(Context context, Bitmap bitmap, boolean center) {
        setBackground(context, bitmap, WallpaperManager.FLAG_LOCK, center);
    }
}
