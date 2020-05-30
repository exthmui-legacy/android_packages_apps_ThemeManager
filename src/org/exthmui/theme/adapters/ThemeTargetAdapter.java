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

import android.annotation.StyleRes;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.exthmui.theme.R;
import org.exthmui.theme.models.ThemeTarget;

import java.util.List;

public class ThemeTargetAdapter extends AdapterBase<RecyclerView.ViewHolder> {

    private static final String TAG = "ThemeTargetAdapter";

    private static final int VIEW_TYPE_TITLE= 0;
    private static final int VIEW_TYPE_ITEM = 1;

    private Context mContext;
    private List<ThemeTarget> targetList;

    public ThemeTargetAdapter(List<ThemeTarget> data, Context context) {
        targetList = data;
        mContext = context;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int viewType) {
        if (viewType == VIEW_TYPE_TITLE) {
            View view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.theme_subtitle, viewGroup, false);
            return new SubtitleViewHolder(view);
        } else {
            View view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.theme_target_item, viewGroup, false);
            return new TargetViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        super.onBindViewHolder(holder, position);

        ThemeTarget target = targetList.get(position);
        if (target.isSubtitle()) {
            SubtitleViewHolder viewHolder = (SubtitleViewHolder) holder;
            viewHolder.mTitle.setText(target.getLabel());
        } else {
            TargetViewHolder viewHolder = (TargetViewHolder) holder;
            boolean changeIconColor = true;
            viewHolder.mTitle.setText(target.getLabel());
            viewHolder.mId.setText(target.getTargetId());
            viewHolder.mId.setVisibility(View.GONE);
            if (target.getTargetType() == ThemeTarget.TYPE_APPLICATIONS) {
                try {
                    viewHolder.mIcon.setImageDrawable(mContext.getPackageManager().getApplicationIcon(target.getTargetId()));
                } catch (PackageManager.NameNotFoundException e) {
                    e.printStackTrace();
                    viewHolder.mIcon.setImageResource(android.R.drawable.sym_def_app_icon);
                }
                viewHolder.mId.setVisibility(View.VISIBLE);
                changeIconColor = false;
            } else {
                int resourceId = mContext.getResources().getIdentifier("ic_" + target.getTargetId().replace(".", "_"), "drawable", mContext.getPackageName());
                if (resourceId == 0) {
                    resourceId = R.drawable.ic_theme_target_others;
                }
                viewHolder.mIcon.setImageResource(resourceId);
            }
            if (!target.isSwitchable()) {
                parseStyle(viewHolder, R.style.theme_target_disabled, changeIconColor);
            } else if (target.isSelected()) {
                parseStyle(viewHolder, R.style.theme_target_selected, changeIconColor);
            } else {
                parseStyle(viewHolder, R.style.theme_target, changeIconColor);
            }
        }
    }

    @Override
    public int getItemCount() {
        return targetList.size();
    }

    @Override
    public int getItemViewType(int position) {
        if (targetList.get(position).isSubtitle()) {
            return VIEW_TYPE_TITLE;
        } else {
            return VIEW_TYPE_ITEM;
        }
    }

    @Override
    public void onAttachedToRecyclerView(RecyclerView recyclerView) {
        GridLayoutManager manager = (GridLayoutManager) recyclerView.getLayoutManager();
        if (manager == null) return;
        manager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                if(targetList.get(position).isSubtitle()){
                    return manager.getSpanCount();
                }else {
                    return 1;
                }
            }
        });
    }

    private void parseStyle(TargetViewHolder holder, @StyleRes int styleId, boolean changeIconColor) {
        Drawable background = new ContextThemeWrapper(mContext, styleId).getDrawable(R.drawable.theme_target_background);
        TypedArray array = mContext.obtainStyledAttributes(styleId, R.styleable.themeTarget);
        int nameTextColor = array.getColor(R.styleable.themeTarget_nameTextColor, 0);
        int idTextColor = array.getColor(R.styleable.themeTarget_idTextColor, 0);
        int iconColor = array.getColor(R.styleable.themeTarget_iconColor, 0);
        holder.mBackground.setBackground(background);
        holder.mTitle.setTextColor(nameTextColor);
        holder.mId.setTextColor(idTextColor);
        if (changeIconColor) {
            holder.mIcon.getDrawable().setTint(iconColor);
        }
        array.recycle();
    }

    static class TargetViewHolder extends RecyclerView.ViewHolder {
        TextView mTitle;
        TextView mId;
        ImageView mIcon;
        View mBackground;
        TargetViewHolder(View view) {
            super(view);
            mTitle = view.findViewById(R.id.theme_target_name);
            mIcon = view.findViewById(R.id.theme_target_icon);
            mId = view.findViewById(R.id.theme_target_id);
            mBackground = view.findViewById(R.id.theme_target);
        }

    }

    static class SubtitleViewHolder extends RecyclerView.ViewHolder {
        TextView mTitle;
        SubtitleViewHolder(View view) {
            super(view);
            mTitle = view.findViewById(R.id.theme_subtitle_text);
        }
    }

}
