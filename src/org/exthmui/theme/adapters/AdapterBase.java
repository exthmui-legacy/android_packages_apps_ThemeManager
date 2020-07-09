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

import android.view.View;

import androidx.recyclerview.widget.RecyclerView;

public abstract class AdapterBase<TH extends RecyclerView.ViewHolder> extends RecyclerView.Adapter<TH> {

    private OnItemClickListener onItemClickListener;
    private OnItemLongClickListener onItemLongClickListener;

    public AdapterBase() {}

    @Override
    public void onBindViewHolder(TH holder, int position) {
        if (onItemClickListener != null) {
            holder.itemView.setOnClickListener(new ItemOnClickListener(position));
        }
        if (onItemLongClickListener != null) {
            holder.itemView.setOnLongClickListener(new ItemOnLongClickListener(position));
        }
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        onItemClickListener = listener;
    }

    public void setOnItemLongClickListener(OnItemLongClickListener listener) {
        onItemLongClickListener = listener;
    }

    public interface OnItemClickListener {
        void onItemClick(View v, int position);
    }

    public interface OnItemLongClickListener {
        boolean onItemLongClick(View v, int position);
    }

    private class ItemOnClickListener implements View.OnClickListener {
        private int position;

        public ItemOnClickListener(int position) {
            this.position = position;
        }

        @Override
        public void onClick(View v) {
            onItemClickListener.onItemClick(v, position);
        }
    }

    private class ItemOnLongClickListener implements View.OnLongClickListener {
        private int position;

        public ItemOnLongClickListener(int position) {
            this.position = position;
        }

        @Override
        public boolean onLongClick(View v) {
            return onItemLongClickListener.onItemLongClick(v, position);
        }
    }
}
