package com.example.filter.adapters;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.filter.R;

import java.util.List;

public class RecommendFilterAdapter extends RecyclerView.Adapter<RecommendFilterAdapter.ViewHolder> {
    private List<String> items;

    public RecommendFilterAdapter(List<String> items) {
        this.items = items;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public LinearLayout filterItem;
        public ImageView filterImage;
        public TextView filterTitle;

        public ViewHolder(View itemView) {
            super(itemView);
            filterItem = itemView.findViewById(R.id.filterItem);
            filterImage = itemView.findViewById(R.id.filterImage);
            filterTitle = itemView.findViewById(R.id.filterTitle);
        }
    }

    @NonNull
    @Override
    public RecommendFilterAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.i_filter, parent, false);
        return new ViewHolder(view);
    }

    @NonNull
    @Override
    public void onBindViewHolder(@NonNull RecommendFilterAdapter.ViewHolder holder, int position) {
        String item = items.get(position);
        holder.filterTitle.setText(items.get(position));

        switch (item) {
            case "Cloud POP":
                holder.filterImage.setImageResource(R.drawable.cloud_pop);
                break;
            case "Bloom":
                holder.filterImage.setImageResource(R.drawable.bloom);
                break;
            case "별로":
                holder.filterImage.setImageResource(R.drawable.byeolro);
                break;
            case "푸른 필름 느낌":
                holder.filterImage.setImageResource(R.drawable.blue_film);
                break;
            case "청(淸靑)":
                holder.filterImage.setImageResource(R.drawable.cheong);
                break;
            case "Zandi":
                holder.filterImage.setImageResource(R.drawable.zandi);
                break;
            case "햇살 가득 공원":
                holder.filterImage.setImageResource(R.drawable.sunny_park);
                break;
            case "새콤달콤 과일 샐러드":
                holder.filterImage.setImageResource(R.drawable.fruit_salad);
                break;
            case "Warm Vintage":
                holder.filterImage.setImageResource(R.drawable.warm_vintage);
                break;
            case "오늘의 하늘":
                holder.filterImage.setImageResource(R.drawable.today_sky);
                break;
            case "고요한 횡단보도":
                holder.filterImage.setImageResource(R.drawable.crosswalk);
                break;
            case "노을 가득 하늘":
                holder.filterImage.setImageResource(R.drawable.sunset_sky);
                break;
            default:
                holder.filterImage.setBackgroundColor(Color.parseColor("#BDBDBD"));
                break;
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }
}
