package com.example.filter.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.filter.R;

import java.util.List;

public class StudioItemPreviewAdapter extends RecyclerView.Adapter<StudioItemPreviewAdapter.ImagePreviewViewHolder> {
    private List<Integer> imagePreviewList;

    public StudioItemPreviewAdapter(List<Integer> imagePreviewList) {
        this.imagePreviewList = imagePreviewList;
    }

    @NonNull
    @Override
    public ImagePreviewViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_studio_preview, parent, false);
        return new ImagePreviewViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ImagePreviewViewHolder holder, int position) {
        int imageResId = imagePreviewList.get(position);
        holder.previewImage.setImageResource(imageResId);
    }

    @Override
    public int getItemCount() {
        return imagePreviewList.size();
    }

    public static class ImagePreviewViewHolder extends RecyclerView.ViewHolder {
        ImageView previewImage;

        public ImagePreviewViewHolder(@NonNull View itemView) {
            super(itemView);
            previewImage = itemView.findViewById(R.id.previewImage);
        }
    }
}
