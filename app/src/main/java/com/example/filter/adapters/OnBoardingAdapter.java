package com.example.filter.adapters;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.bumptech.glide.signature.ObjectKey;
import com.example.filter.R;
import com.example.filter.etc.FilterItem;
import com.example.filter.etc.OnBoardingItem;

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

        if (item.isSelected()) {
            holder.image2.setAlpha(0.7f);
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