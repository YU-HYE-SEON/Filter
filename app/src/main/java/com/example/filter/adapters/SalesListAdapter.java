package com.example.filter.adapters;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.filter.R;
import com.example.filter.api_datas.response_dto.SalesListResponse;

import java.util.ArrayList;
import java.util.List;

public class SalesListAdapter extends RecyclerView.Adapter<SalesListAdapter.VH> {

    public interface OnItemClickListener {
        void onClick(View v, SalesListResponse item);
    }

    private OnItemClickListener listener;

    public void setOnItemClickListener(OnItemClickListener l) {
        this.listener = l;
    }

    private final List<SalesListResponse> items = new ArrayList<>();

    public SalesListAdapter() {
    }

    public void addItemList(List<SalesListResponse> list) {
        items.addAll(list);
        notifyDataSetChanged();
    }

    public void clear() {
        items.clear();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.i_my_filter, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        if (position < 0 || position >= items.size()) return;

        SalesListResponse item = items.get(position);
        if (item == null) return;

        if (item.getFilterImageUrl() != null) {
            Glide.with(holder.image.getContext())
                    .load(item.getFilterImageUrl())
                    .centerCrop()
                    .into(holder.image);
        }

        String rawDate = item.getFilterCreatedAt();
        String formattedDate = rawDate.substring(0, 10).replace("-", ".") + " 등록";

        holder.title.setText(item.getFilterName());
        holder.date.setText(formattedDate);
        holder.point.setText(item.getPrice() + "P");
        holder.sales.setText(String.valueOf(item.getSalesCount()));
        holder.bookmark.setText(String.valueOf(item.getSaveCount()));

        if (position == items.size() - 1) {
            holder.borderLine.setVisibility(View.GONE);
        } else {
            holder.borderLine.setVisibility(View.VISIBLE);
        }

        if (item.isDeleted()) {
            holder.deleteTxt.setVisibility(View.VISIBLE);
        } else {
            holder.deleteTxt.setVisibility(View.INVISIBLE);
        }

        Log.d("SalesListdeleted", "deleted? " + item.isDeleted());
    }


    @Override
    public int getItemCount() {
        return items.size();
    }

    public class VH extends RecyclerView.ViewHolder {
        ImageView image;
        TextView title, date, deleteTxt, point, sales, bookmark;
        View borderLine;

        public VH(@NonNull View itemView) {
            super(itemView);

            image = itemView.findViewById(R.id.image);
            title = itemView.findViewById(R.id.title);
            date = itemView.findViewById(R.id.date);
            deleteTxt = itemView.findViewById(R.id.deleteTxt);
            point = itemView.findViewById(R.id.point);
            sales = itemView.findViewById(R.id.sales);
            bookmark = itemView.findViewById(R.id.bookmark);
            borderLine = itemView.findViewById(R.id.borderLine);

            itemView.setOnClickListener(v -> {
                int pos = getAdapterPosition();
                if (pos != RecyclerView.NO_POSITION && listener != null) {
                    if (pos >= 0 && pos < items.size()) {
                        listener.onClick(v, items.get(pos));
                    }
                }
            });
        }
    }
}