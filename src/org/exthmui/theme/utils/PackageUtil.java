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

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender;
import android.content.pm.PackageInstaller;
import android.util.Log;

import java.io.InputStream;
import java.io.OutputStream;

public class PackageUtil {

    private static final String TAG = "PackageUtil";

    public static void installPackage(Context context, InputStream inputStream, final PackageInstallerCallback callback) {
        String installId = "install_" + System.currentTimeMillis();
        PackageInstaller packageInstaller = context.getPackageManager().getPackageInstaller();

        context.registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                context.unregisterReceiver(this);
                int statusCode = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE);

                String packageName = intent.getStringExtra(PackageInstaller.EXTRA_PACKAGE_NAME);

                boolean successFlag = PackageInstaller.STATUS_SUCCESS == statusCode;

                if (callback == null) return;
                if (successFlag) {
                    callback.onSuccess(packageName);
                } else {
                    callback.onFailure(packageName, statusCode);
                }
            }
        }, new IntentFilter(installId));

        PackageInstaller.SessionParams sessionParams = new PackageInstaller.SessionParams(
                PackageInstaller.SessionParams.MODE_FULL_INSTALL);

        // install
        try {
            // set params
            int sessionId = packageInstaller.createSession(sessionParams);
            PackageInstaller.Session session = packageInstaller.openSession(sessionId);
            OutputStream outputStream = session.openWrite(installId, 0, -1);

            byte[] buffer = new byte[65536];
            int tmpByte = -1;

            while ((tmpByte = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, tmpByte);
            }

            session.fsync(outputStream);
            outputStream.close();
            session.commit(createIntentSender(context, sessionId, installId));
        } catch (Exception e) {
            Log.e(TAG, "Failed to install package");
        }
    }

    public static void uninstallPackage(Context context, final String packageName, final PackageInstallerCallback callback) {

        String uninstallId = "uninstall_" + packageName + System.currentTimeMillis();

        context.registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                context.unregisterReceiver(this);
                int statusCode = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE);

                boolean successFlag = PackageInstaller.STATUS_SUCCESS == statusCode;

                if (callback == null) return;
                if (successFlag) {
                    callback.onSuccess(packageName);
                } else {
                    callback.onFailure(packageName, statusCode);
                }
            }
        }, new IntentFilter(uninstallId));

        context.getPackageManager().getPackageInstaller().uninstall(packageName, createIntentSender(context, 0, uninstallId));
    }

    private static IntentSender createIntentSender(Context context, int sessionId, String name) {
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, sessionId, new Intent(name), 0);
        return pendingIntent.getIntentSender();
    }

    public interface PackageInstallerCallback {
        void onSuccess(String packageName);
        void onFailure(String packageName, int code);
    }

}
