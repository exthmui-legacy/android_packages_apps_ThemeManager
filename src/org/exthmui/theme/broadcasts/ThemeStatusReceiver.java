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

package org.exthmui.theme.broadcasts;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import org.exthmui.theme.R;
import org.exthmui.theme.misc.Constants;
import org.exthmui.theme.utils.NotificationUtil;

public class ThemeStatusReceiver extends BroadcastReceiver {

    private final static String TAG = "ThemeStatusReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        String msg;
        int max = 0, progress = 0;
        boolean indeterminate = false;
        int notificationId = intent.getStringExtra("themePackage").hashCode();
        String themeTitle = intent.getStringExtra("themeTitle");

        switch (intent.getStringExtra("status")) {
            case Constants.THEME_APPLYING:
                msg = context.getString(R.string.skin_apply_status_running);
                indeterminate = true;
                break;
            case Constants.THEME_APPLY_SUCCEED:
                msg = context.getString(R.string.skin_apply_status_succeed, themeTitle);
                break;
            case Constants.THEME_APPLY_FAILED:
                msg = context.getString(R.string.skin_apply_status_failed, themeTitle);
                break;
            default:
                return;
        }

        NotificationUtil.showNotification(
                context, null,
                Constants.CHANNEL_APPLY_STATUS, notificationId,
                msg, null, R.drawable.ic_stat_notification,
                max, progress, indeterminate);
    }
}
