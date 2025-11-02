package com.example.filter.adapters;

import android.annotation.SuppressLint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.filter.R;
import com.example.filter.items.OnBoardingItem;

import java.util.ArrayList;
import java.util.List;

public class OnBoardingAdapter extends RecyclerView.Adapter<OnBoardingAdapter.VH> {
    public interface OnSelectionChangeListener {
        void onSelectionChanged(int selectedCount);
    }

    private OnSelectionChangeListener listener;

    public void setOnSelectionChangeListener(OnSelectionChangeListener l) {
        this.listener = l;
    }

    private final List<OnBoardingItem> items = new ArrayList<>();

    public void setItems(List<OnBoardingItem> newItems) {
        items.clear();
        items.addAll(newItems);
        notifyDataSetChanged();
    }

    private int getSelectedCount() {
        int count = 0;
        for (OnBoardingItem item : items) {
            if (item.isSelected()) count++;
        }
        return count;
    }


    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.i_onboarding, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        OnBoardingItem item = items.get(position);

        Glide.with(holder.image.getContext())
                .load(item.getImageResId())
                .centerInside()
                .into(new com.bumptech.glide.request.target.CustomTarget<android.graphics.drawable.Drawable>() {
                    @Override
                    public void onResourceReady(@NonNull android.graphics.drawable.Drawable resource,
                                                @Nullable com.bumptech.glide.request.transition.Transition<? super android.graphics.drawable.Drawable> transition) {
                        holder.image.setImageDrawable(resource);

                        int width = resource.getIntrinsicWidth();
                        int height = resource.getIntrinsicHeight();

                        if (width > 0 && height > 0) {
                            float ratio = (float) width / height;
                            int overlayRes = getClosestOverlayRes(ratio);
                            holder.image2.setImageResource(overlayRes);
                        }
                    }

                    @Override
                    public void onLoadCleared(@Nullable android.graphics.drawable.Drawable placeholder) {
                        holder.image.setImageDrawable(placeholder);
                    }
                });

        if (item.isSelected()) {
            holder.image2.setAlpha(0.3f);
            holder.image2.setVisibility(View.VISIBLE);
        } else {
            holder.image2.setVisibility(View.INVISIBLE);
            holder.image2.setAlpha(0.0f);
        }

        holder.image.setOnClickListener(v -> {
            item.setSelected(!item.isSelected());
            notifyItemChanged(position);

            if (listener != null) {
                int count = getSelectedCount();
                listener.onSelectionChanged(count);
            }
        });
    }

    private int getClosestOverlayRes(float ratio) {
        float ratio1_1 = 1f;
        float ratio3_4 = 3f / 4f;
        float ratio9_16 = 9f / 16f;

        float diff1 = Math.abs(ratio - ratio1_1);
        float diff2 = Math.abs(ratio - ratio3_4);
        float diff3 = Math.abs(ratio - ratio9_16);

        if (diff1 <= diff2 && diff1 <= diff3) {
            return R.drawable.dim_onboarding_1_1;
        } else if (diff2 <= diff1 && diff2 <= diff3) {
            return R.drawable.dim_onboarding_3_4;
        } else {
            return R.drawable.dim_onboarding_9_16;
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public class VH extends RecyclerView.ViewHolder {
        ImageView image, image2;

        public VH(@NonNull View itemView) {
            super(itemView);
            image = itemView.findViewById(R.id.image);
            image2 = itemView.findViewById(R.id.image2);

            itemView.setOnClickListener(v -> {

            });
        }
    }
}