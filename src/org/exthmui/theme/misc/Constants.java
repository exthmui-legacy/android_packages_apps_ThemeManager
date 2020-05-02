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

package org.exthmui.theme.misc;

import android.os.Environment;

public class Constants {

    // Broadcast
    public final static String BROADCAST_ACTION_APPLY_RESULT = "org.exthmui.theme.THEME_APPLY_RESULT";
    public final static String BROADCAST_RECEIVER_NAME = ".broadcasts.ThemeStatusReceiver";

    // Notification
    public final static String CHANNEL_APPLY_STATUS = "apply_status";

    // Theme data
    public final static String THEME_DATA_FLAG = "exthmui_theme";
    public final static String THEME_DATA_OVERLAY_FLAG = "is_theme_overlay";
    public final static String THEME_DATA_TITLE = "theme_title";
    public final static String THEME_DATA_AUTHOR = "theme_author";
    public final static String THEME_DATA_IMAGE = "theme_image";
    public final static String THEME_DATA_BANNER = "theme_banner";
    public final static String THEME_DATA_ASSETS_SOUNDS = "sounds";
    public final static String THEME_DATA_ASSETS_BACKGROUNDS = "backgrounds";
    public final static String THEME_DATA_ASSETS_PREVIEWS = "previews";
    public final static String THEME_DATA_ASSETS_OVERLAY = "overlay";
    public final static String THEME_DATA_XML_FILE = "theme_data";
    public final static String THEME_DATA_XML_OVERLAY = "overlay";
    public final static String THEME_DATA_XML_OVERLAY_TARGET = "target";
    public final static String THEME_DATA_XML_OVERLAY_TARGET_ATTR_SWITCHABLE = "switchable";
    public final static String THEME_DATA_XML_SOUND_RINGTONE = "ringtone";
    public final static String THEME_DATA_XML_SOUND_ALARM = "alarm";
    public final static String THEME_DATA_XML_SOUND_NOTIFICATION = "notification";
    public final static String THEME_DATA_XML_BACKGROUND_RATIO_WIDTH = "ratio_width";
    public final static String THEME_DATA_XML_BACKGROUND_RATIO_HEIGHT = "ratio_height";
    public final static String THEME_DATA_XML_BACKGROUND_WALLPAPER = "wallpaper";
    public final static String THEME_DATA_XML_BACKGROUND_LOCKSCREEN = "lockscreen";

    // Theme apply status
    public final static String THEME_APPLY_SUCCEED = "APPLY_SUCCEED";
    public final static String THEME_APPLY_FAILED = "APPLY_FAILED";
    public final static String THEME_APPLYING = "APPLYING";
    public final static String THEME_APPLYING_RINGTONE = "APPLYING_RINGTONE";
    public final static String THEME_APPLYING_ALARM = "APPLYING_ALARM";
    public final static String THEME_APPLYING_NOTIFICATION = "APPLYING_NOTIFICATION";
    public final static String THEME_APPLYING_WALLPAPER = "APPLYING_WALLPAPER";
    public final static String THEME_APPLYING_LOCKSCREEN = "APPLYING_LOCKSCREEN";
    public final static String THEME_APPLYING_OVERLAY = "APPLYING_OVERLAY";
    public final static String THEME_INSTALLING_OVERLAY = "INSTALLING_OVERLAY";
    public final static String THEME_CLEANING = "CLEANING";

    // Theme target key
    public final static String THEME_TARGET_WALLPAPER = "theme.wallpaper";
    public final static String THEME_TARGET_LOCKSCREEN = "theme.lockscreen";
    public final static String THEME_TARGET_ALARM = "theme.alarm";
    public final static String THEME_TARGET_NOTIFICATION = "theme.notification";
    public final static String THEME_TARGET_RINGTONE = "theme.ringtone";

    // Preferences key
    public final static String PREFERENCES_OVERLAY_REMOVE_FLAG = "overlay_uninstall_flag";
    public final static String PREFERENCES_LIST_ACCENT_PACKAGES = "list_accents";

}
