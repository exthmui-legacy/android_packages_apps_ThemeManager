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

package org.exthmui.theme.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.exthmui.theme.R;
import org.exthmui.theme.services.ThemeManageService;

public class ThemeApplyingDialog extends DialogFragment {

    private static final String TAG = "ThemeApplyingDialog";

    private Bundle dialogData = new Bundle();
    private View rootView;
    private TextView textApplying;
    private ProgressBar progApplying;
    private Button btnDismiss;

    public ThemeApplyingDialog() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        super.onCreateView(inflater, container, savedInstanceState);
        rootView = inflater.inflate(R.layout.theme_applying_dialog, container, false);
        textApplying = rootView.findViewById(R.id.applying_text);
        progApplying = rootView.findViewById(R.id.applying_prog);
        btnDismiss = rootView.findViewById(R.id.dismiss_button);
        btnDismiss.setText(android.R.string.ok);
        btnDismiss.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
            }
        });

        return rootView;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (savedInstanceState != null) {
            dialogData = savedInstanceState.getBundle("data");
            updateView();
        } else {
            dialogData = new Bundle();
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBundle("data", dialogData);
    }

    @Override
    public void onStart()
    {
        super.onStart();

        Window dialogWindow = getDialog().getWindow();
        if (dialogWindow != null) {
            WindowManager.LayoutParams layoutParams = dialogWindow.getAttributes();
            layoutParams.width = WindowManager.LayoutParams.MATCH_PARENT;
            layoutParams.height = WindowManager.LayoutParams.WRAP_CONTENT;

            layoutParams.gravity = Gravity.BOTTOM;
            layoutParams.windowAnimations = android.R.style.Animation_InputMethod;
            dialogWindow.setAttributes(layoutParams);
        }
    }

    public void updateData(Intent data) {
        if (data != null) {
            dialogData.putString("status", data.getStringExtra("status"));
            dialogData.putString("themeTitle", data.getStringExtra("themeTitle"));
            dialogData.putString("nowPackageLabel", data.getStringExtra("nowPackageLabel"));
            dialogData.putInt("progressMax", data.getIntExtra("progressMax", 0));
            dialogData.putInt("progressVal", data.getIntExtra("progressVal", 0));
            dialogData.putBoolean("indeterminate", data.getBooleanExtra("indeterminate", false));
        }
        if (isAdded()) updateView();
    }

    private void updateView() {
        String status = dialogData.getString("status", "ERROR");
        String msg = "";
        int max = 0, progress = 0;
        boolean indeterminate = true;
        switch (status) {
            case ThemeManageService.THEME_APPLY_SUCCEED:
                msg = getString(R.string.skin_apply_status_succeed, dialogData.getString("themeTitle", "ERROR"));
                break;
            case ThemeManageService.THEME_APPLY_FAILED:
                msg = getString(R.string.skin_apply_status_failed, dialogData.getString("themeTitle", "ERROR"));
                break;
            case ThemeManageService.THEME_APPLYING:
                msg = getString(R.string.skin_apply_status_running);
                break;
            case ThemeManageService.THEME_APPLYING_ALARM:
                msg = getString(R.string.skin_apply_status_alarm);
                break;
            case ThemeManageService.THEME_APPLYING_RINGTONE:
                msg = getString(R.string.skin_apply_status_ringtone);
                break;
            case ThemeManageService.THEME_APPLYING_NOTIFICATION:
                msg = getString(R.string.skin_apply_status_notification);
                break;
            case ThemeManageService.THEME_APPLYING_WALLPAPER:
                msg = getString(R.string.skin_apply_status_wallpaper);
                break;
            case ThemeManageService.THEME_APPLYING_LOCKSCREEN:
                msg = getString(R.string.skin_apply_status_lockscreen);
                break;
            case ThemeManageService.THEME_APPLYING_OVERLAY:
                max = dialogData.getInt("progressMax", 0);
                progress = dialogData.getInt("progressVal", 0);
                indeterminate = false;
                msg = getString(R.string.skin_apply_status_overlay, dialogData.getString("nowPackageLabel", "ERROR"));
                break;
            default:
                return;
        }
        if (ThemeManageService.THEME_APPLY_FAILED.equals(status) || ThemeManageService.THEME_APPLY_SUCCEED.equals(status)) {
            btnDismiss.setVisibility(View.VISIBLE);
            progApplying.setVisibility(View.GONE);
            this.setCancelable(true);
        } else {
            btnDismiss.setVisibility(View.GONE);
            progApplying.setVisibility(View.VISIBLE);
            this.setCancelable(false);
        }

        textApplying.setText(msg);
        progApplying.setIndeterminate(indeterminate);
        progApplying.setMax(max);
        progApplying.setProgress(progress);
    }
}
