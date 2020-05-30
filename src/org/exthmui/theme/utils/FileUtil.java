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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class FileUtil {

    private static final String TAG = "FileUtil";

    public static void saveInputStream(String path, InputStream inputStream, boolean isThemeData) throws IOException {
        int index;
        byte[] bytes = new byte[1024];
        FileOutputStream fos = new FileOutputStream(path);

        while ((index = inputStream.read(bytes)) != -1) {
            fos.write(bytes, 0, index);
            fos.flush();
        }

        fos.close();
        inputStream.close();
        if (isThemeData) {
            try {
                Runtime.getRuntime().exec("chmod 0644 " + path).waitFor();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public static void createPath(File file) {
        File parent = file.getParentFile();

        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }
    }

}
