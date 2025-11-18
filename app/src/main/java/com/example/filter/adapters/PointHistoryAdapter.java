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
        holder.point.setText(item.point + "P 충전");
        holder.currentPoint.setText(String.format("%,dP", item.currentPoint));
        holder.txt.setText(item.price + " · Google 인앱결제");
        holder.date.setText(item.date);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public static class VH extends RecyclerView.ViewHolder {
        TextView point, currentPoint, txt, date;
        public VH(View itemView) {
            super(itemView);
            point = itemView.findViewById(R.id.point);
            currentPoint = itemView.findViewById(R.id.currentPoint);
            txt = itemView.findViewById(R.id.txt);
            date = itemView.findViewById(R.id.date);
        }
    }
}
