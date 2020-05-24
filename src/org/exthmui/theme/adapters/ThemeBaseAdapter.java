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

package org.exthmui.theme.adapters;

import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import org.exthmui.theme.R;
import org.exthmui.theme.models.ThemeBase;
import org.exthmui.theme.services.ThemeDataService;

import java.util.List;

public class ThemeBaseAdapter extends AdapterBase<ThemeBaseAdapter.ThemeViewHolder> {
    private ThemeDataService.ThemeDataBinder mThemeDataBinder;
    private List<ThemeBase> mData;
    public ThemeBaseAdapter(List<ThemeBase> data) {
        mData = data;
    }

    public void setThemeDataBinder(ThemeDataService.ThemeDataBinder binder) {
        mThemeDataBinder = binder;
    }

    @NonNull
    @Override
    public ThemeViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.theme_card, parent, false);
        return new ThemeViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ThemeViewHolder holder, int position) {
        super.onBindViewHolder(holder, position);
        ThemeBase theme = mData.get(position);
        holder.mTitle.setText(theme.getTitle());
        if (mThemeDataBinder != null) {
            Drawable drawable =  mThemeDataBinder.getThemeImage(theme.getPackageName());
            holder.mImage.setImageDrawable(drawable);
        }
    }

    @Override
    public int getItemCount() {
        return mData.size();
    }

    static class ThemeViewHolder extends RecyclerView.ViewHolder {
        TextView mTitle;
        ImageView mImage;
        ThemeViewHolder(View view) {
            super(view);
            mTitle = view.findViewById(R.id.theme_card_title);
            mImage = view.findViewById(R.id.theme_card_image);
        }
    }
}