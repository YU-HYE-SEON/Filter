package com.example.filter.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.filter.R;
import com.example.filter.etc.FilterItem;

import java.util.ArrayList;
import java.util.List;

public class FilterAdapter extends RecyclerView.Adapter<FilterAdapter.VH> {
    public interface OnItemClickListener {
        void onClick(View v, FilterItem item);
    }

    private OnItemClickListener listener;

    public void setOnItemClickListener(OnItemClickListener l) {
        this.listener = l;
    }

    private final List<FilterItem> items = new ArrayList<>();

    public void append(List<FilterItem> more) {
        int start = items.size();
        items.addAll(more);
        notifyItemRangeInserted(start, more.size());
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.i_filter, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        FilterItem item = items.get(position);
        holder.filterTitle.setText(ellipsize(item.filterTitle, 6));
        holder.nickname.setText(ellipsize(item.nickname, 6));
        holder.price.setText(String.valueOf(item.price));
        holder.count.setText(String.valueOf(item.count));

        Glide.with(holder.filterImage.getContext())
                .load(item.filterImageUrl)
                .fitCenter()
                .into(holder.filterImage);
    }

    private String ellipsize(String src, int keep) {
        if (src == null) return "";
        int len = src.codePointCount(0, src.length());
        if (len <= keep) return src;
        int endIndex = src.offsetByCodePoints(0, keep);
        return src.substring(0, endIndex) + "…";
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public class VH extends RecyclerView.ViewHolder {
        ImageView filterImage;
        TextView filterTitle, nickname, price, count;

        public VH(@NonNull View itemView) {
            super(itemView);
            filterImage = itemView.findViewById(R.id.filterImage);
            filterTitle = itemView.findViewById(R.id.filterTitle);
            nickname = itemView.findViewById(R.id.nickname);
            price = itemView.findViewById(R.id.price);
            count = itemView.findViewById(R.id.count);

            itemView.setOnClickListener(v -> {
                int pos = getAdapterPosition();
                if (pos != RecyclerView.NO_POSITION && listener != null) {
                    listener.onClick(v, items.get(pos));
                }
            });
        }
    }
}