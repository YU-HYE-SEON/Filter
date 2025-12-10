package com.example.filter.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;

import com.example.filter.R;

import java.util.ArrayList;
import java.util.List;

public class SearchKeywordAdapter extends RecyclerView.Adapter<SearchKeywordAdapter.SKVH> {
    public interface OnKeywordClickListener {
        void onClick(String keyword);
    }

    private OnKeywordClickListener listener;

    private List<String> items = new ArrayList<>();

    public void addItem(String keyword) {
        int existingPosition = -1;
        for (int i = 0; i < items.size(); i++) {
            if (items.get(i).equals(keyword)) {
                existingPosition = i;
                break;
            }
        }

        if (existingPosition != -1) {
            if (existingPosition == 0) return;
            items.remove(existingPosition);
            notifyItemRemoved(existingPosition);
            notifyItemRangeChanged(existingPosition, items.size());
        }

        items.add(0, keyword);
        notifyItemInserted(0);
    }

    public void removeItem(int position) {
        if (position < 0 || position >= items.size()) return;
        items.remove(position);
        notifyItemRemoved(position);
        notifyItemRangeChanged(position, items.size());
    }

    public void clearAll() {
        int size = items.size();
        if (size > 0) {
            items.clear();
            notifyItemRangeRemoved(0, size);
        }
    }

    public void setOnKeywordClickListener(OnKeywordClickListener l) {
        this.listener = l;
    }

    public SearchKeywordAdapter(List<String> savedList) {
        this.items = savedList;
    }

    @NonNull
    @Override
    public SKVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.i_search_keyword, parent, false);
        return new SearchKeywordAdapter.SKVH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull SKVH holder, int position) {
        String keyword = items.get(position);
        holder.keywordTxt.setText(keyword);

        holder.searchHistory.setOnClickListener(v -> {
            if (listener != null) listener.onClick(keyword);
        });

        holder.deleteHistory.setOnClickListener(v -> {
            int currentPosition = holder.getAdapterPosition();
            if (currentPosition != RecyclerView.NO_POSITION) {
                removeItem(currentPosition);
            }
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public static class SKVH extends RecyclerView.ViewHolder {
        ConstraintLayout searchHistory;
        TextView keywordTxt;
        ImageView deleteHistory;

        public SKVH(View itemView) {
            super(itemView);
            searchHistory = itemView.findViewById(R.id.searchHistory);
            keywordTxt = itemView.findViewById(R.id.keywordTxt);
            deleteHistory = itemView.findViewById(R.id.deleteHistory);
        }
    }
}
