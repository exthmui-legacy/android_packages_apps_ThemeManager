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

package org.exthmui.thememanager.models;

public class OverlayTarget {

    private String packageName;
    private String label;

    private boolean isSwitchable;

    public OverlayTarget(String packageName) {
        this.packageName = packageName;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public void setSwitchable(boolean switchable) {
        this.isSwitchable = switchable;
    }

    public String getLabel() {
        return label;
    }

    public String getPackageName() {
        return packageName;
    }

    public boolean getSwitchable() {
        return isSwitchable;
    }

}
