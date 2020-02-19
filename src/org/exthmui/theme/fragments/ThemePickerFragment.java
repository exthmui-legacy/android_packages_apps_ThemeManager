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

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Bundle;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridView;

import org.exthmui.theme.R;
import org.exthmui.theme.adapters.CommonAdapter;
import org.exthmui.theme.interfaces.ThemePickerInterface;
import org.exthmui.theme.models.ThemeAccent;
import org.exthmui.theme.models.ThemeBase;

import java.util.List;

public class ThemePickerFragment extends Fragment {

    private static final String TAG = "ThemePickerFragment";

    private ThemeAdapter mThemeAdapter;
    private ThemePickerInterface mCallback;
    private GridView mGridView;

    public static ThemePickerFragment newInstance() {
        return new ThemePickerFragment();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context != null) {
            mCallback = (ThemePickerInterface) context;
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.theme_picker_fragment, container, false);
        mGridView = view.findViewById(R.id.themesGrid);

        int cardWidth = getResources().getDimensionPixelOffset(R.dimen.card_image_height) / 16 * 10;
        mGridView.setNumColumns(getResources().getDisplayMetrics().widthPixels / cardWidth);
        mGridView.setOnItemClickListener((parent, view1, position, id) -> mCallback.onThemeItemClick(parent, view1, position, id));

        mGridView.setOnItemLongClickListener((parent, view12, position, id) -> mCallback.onThemeItemLongClick(parent, view12, position, id));

        return view;
    }

    public void updateView() {
        mCallback.updateThemeList();
        mThemeAdapter = new ThemeAdapter(mCallback.getThemeList(), R.layout.theme_item);
        mGridView.setAdapter(mThemeAdapter);
    }

    public void updateAdapter() {
        mThemeAdapter.notifyDataSetChanged();
    }

    private class ThemeAdapter extends CommonAdapter<ThemeBase> {

        public ThemeAdapter(List<ThemeBase> data, int layoutRes) {
            super(data, layoutRes);
        }

        @Override
        public void bindView(ViewHolder holder, ThemeBase obj) {
            holder.setImageResource(R.id.theme_card_image, R.drawable.theme_default_src);
            new Thread(() -> {
                if (obj instanceof ThemeAccent) {
                    getActivity().runOnUiThread(() -> {
                        holder.setImageResource(R.id.theme_card_image, R.drawable.theme_accent_image);
                        holder.setBackgroundColor(R.id.theme_card_image, ((ThemeAccent) obj).getAccentColor());
                    });
                } else {
                    Drawable drawable =  mCallback.getThemeImage(obj.getPackageName());
                    getActivity().runOnUiThread(() -> holder.setImageResource(R.id.theme_card_image, drawable));
                }

            }).start();

            holder.setText(R.id.theme_card_title, obj.getTitle());
        }
    }

}
