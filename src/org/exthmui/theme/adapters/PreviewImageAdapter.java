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

import java.util.List;

public class PreviewImageAdapter extends AdapterBase<PreviewImageAdapter.PreviewImageHolder> {

    private List<Drawable> mData;

    public PreviewImageAdapter(List<Drawable> data) {
        mData = data;
    }

    @NonNull
    @Override
    public PreviewImageHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.preview_image_item, parent, false);
        return new PreviewImageHolder(view);
    }

    @Override
    public int getItemCount() {
        return mData.size();
    }

    @Override
    public void onBindViewHolder(@NonNull PreviewImageHolder holder, int position) {
        super.onBindViewHolder(holder, position);
        holder.mImage.setImageDrawable(mData.get(position));
    }

    static class PreviewImageHolder extends RecyclerView.ViewHolder {
        ImageView mImage;
        PreviewImageHolder(View view) {
            super(view);
            mImage = view.findViewById(R.id.preview_img);
        }
    }
}
