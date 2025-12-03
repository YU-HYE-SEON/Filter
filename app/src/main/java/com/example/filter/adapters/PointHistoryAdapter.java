package com.example.filter.adapters;

import android.annotation.SuppressLint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.filter.R;
import com.example.filter.items.PointHistoryItem;

import java.util.List;

public class PointHistoryAdapter extends RecyclerView.Adapter<PointHistoryAdapter.VH> {
    public final List<PointHistoryItem> items;

    public PointHistoryAdapter(List<PointHistoryItem> list) {
        this.items = list;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.i_history, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, @SuppressLint("RecyclerView") int position) {
        PointHistoryItem item = items.get(position);

        if (!item.isBuyHistory) {
            holder.point1.setText(String.format("%,dP 충전", item.point1));
            holder.point2.setText(String.format("%,dP", item.point2));
            holder.txt.setText(item.price + " · 웰컴포인트지급");
        } else {
            holder.point1.setText(String.format("%,dP 사용", item.point1));
            holder.point2.setText(String.format("%,dP", item.point2));
            holder.txt.setText(item.filterTitle);
        }

        holder.date.setText(item.date);

        if (position == 0) {
            holder.borderLine.setVisibility(View.GONE);
        } else {
            holder.borderLine.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public static class VH extends RecyclerView.ViewHolder {
        TextView point1, point2, txt, date;
        View borderLine;

        public VH(View itemView) {
            super(itemView);
            point1 = itemView.findViewById(R.id.point1);
            point2 = itemView.findViewById(R.id.point2);
            txt = itemView.findViewById(R.id.txt);
            date = itemView.findViewById(R.id.date);
            borderLine = itemView.findViewById(R.id.borderLine);
        }
    }
}
