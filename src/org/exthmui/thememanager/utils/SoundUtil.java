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

package org.exthmui.thememanager.utils;

import android.content.ContentValues;
import android.content.Context;
import android.media.RingtoneManager;
import android.net.Uri;
import android.provider.MediaStore;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

public class SoundUtil {

    public final static int TYPE_ALARM = RingtoneManager.TYPE_ALARM;
    public final static int TYPE_NOTIFICATION = RingtoneManager.TYPE_NOTIFICATION;
    public final static int TYPE_RINGTONE = RingtoneManager.TYPE_RINGTONE;

    public static void setSound(Context context, File file, int type) {

        ContentValues values = new ContentValues();
        values.put(MediaStore.MediaColumns.DATA, file.getAbsolutePath());
        values.put(MediaStore.MediaColumns.TITLE, file.getName());
        values.put(MediaStore.MediaColumns.SIZE, file.length());
        values.put(MediaStore.MediaColumns.MIME_TYPE, "audio/*");
        values.put(MediaStore.Audio.AudioColumns.IS_RINGTONE, false);
        values.put(MediaStore.Audio.AudioColumns.IS_NOTIFICATION, false);
        values.put(MediaStore.Audio.AudioColumns.IS_ALARM, false);
        values.put(MediaStore.Audio.AudioColumns.IS_MUSIC, false);

        switch (type) {
            case TYPE_ALARM:
                values.put(MediaStore.Audio.AudioColumns.IS_ALARM, true);
                break;
            case TYPE_NOTIFICATION:
                values.put(MediaStore.Audio.AudioColumns.IS_NOTIFICATION, true);
                break;
            case TYPE_RINGTONE:
                values.put(MediaStore.Audio.AudioColumns.IS_RINGTONE, true);
                break;
        }

        Uri uri = MediaStore.Audio.Media.getContentUriForPath(file.getAbsolutePath());
        context.getContentResolver().delete(uri, MediaStore.MediaColumns.DATA + "=\"" + file.getAbsolutePath() + "\"", null);
        Uri newUri = context.getContentResolver().insert(uri, values);

        RingtoneManager.setActualDefaultRingtoneUri(context, type, newUri);
    }

    public static void setRingtone(Context context, String name, InputStream inputStream, int type) throws IOException {
        String ringtonePath = SoundUtil.getRingtonePath(context, name);

        FileUtil.createPath(ringtonePath);
        FileUtil.saveInputStream(ringtonePath, inputStream);
        SoundUtil.setSound(context, new File(ringtonePath) , type);
    }

    public static String getRingtonePath(Context context, String ringtoneName) {
        return context.getExternalCacheDir().getPath() + "/ringtone/" + ringtoneName ;
    }

}
