package com.example.filter.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.filter.R;
import com.example.filter.api_datas.response_dto.ReviewResponse;

import java.util.ArrayList;
import java.util.List;

public class ReviewAdapter extends RecyclerView.Adapter<ReviewAdapter.VH> {
    public interface OnItemClickListener {
        //void onItemClick(View v, ReviewItem item);
        void onItemClick(View v, ReviewResponse item, int position);
    }

    private OnItemClickListener listener;

    public void setOnItemClickListener(OnItemClickListener l) {
        listener = l;
    }

    //private final List<ReviewItem> items = new ArrayList<>();

    private final List<ReviewResponse> items = new ArrayList<>();

    public void addItem(ReviewResponse item) {
        items.add(0, item);
        notifyItemInserted(0);
    }

    public void addItems(List<ReviewResponse> list) {
        items.addAll(0, list);
        notifyItemRangeInserted(0, list.size());
    }

    public void clearItems() {
        items.clear();
        notifyDataSetChanged();
    }

    /*public void setItems(List<ReviewItem> list) {
        items.clear();
        items.addAll(list);
        notifyDataSetChanged();
    }*/

    /*public void removeItem(String imageUrl) {
        int index = -1;
        for (int i = 0; i < items.size(); i++) {
            if (items.get(i).imageUrl.equals(imageUrl)) {
                index = i;
                break;
            }
        }
        if (index != -1) {
            items.remove(index);
            notifyItemRemoved(index);
        }
    }*/

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.i_review, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        //ReviewItem item = items.get(position);
        ReviewResponse item = items.get(position);

        Glide.with(holder.itemView.getContext())
                .load(item.imageUrl)
                .fitCenter()
                .into(holder.image);

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onItemClick(v, item, position);
        });
    }


    @Override
    public int getItemCount() {
        return items.size();
    }

    public class VH extends RecyclerView.ViewHolder {
        ImageView image;

        public VH(@NonNull View itemView) {
            super(itemView);
            image = itemView.findViewById(R.id.image);
        }
    }
}