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
import com.example.filter.api_datas.response_dto.ReviewResponse;

import java.util.ArrayList;
import java.util.List;

public class ReviewInfoAdapter extends RecyclerView.Adapter<ReviewInfoAdapter.VH> {
    public interface OnDeleteClickListener {
        void onDelete(long reviewId, int position);
    }

    private OnDeleteClickListener listener;

    public void setOnItemDeleteListener(OnDeleteClickListener l) {
        listener = l;
    }

    private final List<ReviewResponse> items = new ArrayList<>();

    public void addItems(List<ReviewResponse> list) {
        int start = items.size();
        items.addAll(list);
        notifyItemRangeInserted(start, list.size());
    }

    public void removeItem(int position) {
        if (position < 0 || position >= items.size()) return;
        items.remove(position);
        notifyItemRemoved(position);
    }

    public List<ReviewResponse> getItems() {
        return items;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.i_review_info, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        ReviewResponse item = items.get(position);
        Glide.with(holder.itemView.getContext())
                .load(item.imageUrl)
                .fitCenter()
                .into(holder.img);

        holder.nickname.setText(item.reviewerNickname);
        holder.snsId.setText(item.socialValue);

        holder.deleteBtn.setVisibility(item.isMine ? View.VISIBLE : View.GONE);

        Log.d("리뷰내것인가", "Review ID: " + item.id + ", isMine: " + item.isMine);

        holder.deleteBtn.setOnClickListener(v -> {
            if (listener != null) listener.onDelete(item.id, position);
        });

        String socialType = item.socialType;
        switch (socialType) {
            case "INSTAGRAM":
                holder.snsIcon.setImageResource(R.drawable.btn_review_sns_insta_yes);
                break;
            case "X":
                holder.snsIcon.setImageResource(R.drawable.btn_review_sns_twitter_yes);
                break;
            case "NONE":
            default:
                holder.snsIcon.setImageResource(R.drawable.btn_review_sns_none);
                break;
        }

        if (item.socialValue == null || item.socialValue.isEmpty() || item.socialType.equals("NONE")) {
            holder.snsId.setText("선택 안 함");
        } else {
            holder.snsId.setText("@" + item.socialValue);
        }
    }


    @Override
    public int getItemCount() {
        return items.size();
    }

    public class VH extends RecyclerView.ViewHolder {
        ImageView img, snsIcon;
        TextView nickname, snsId, deleteBtn;

        public VH(@NonNull View itemView) {
            super(itemView);
            img = itemView.findViewById(R.id.img);
            snsIcon = itemView.findViewById(R.id.snsIcon);
            nickname = itemView.findViewById(R.id.nickname);
            snsId = itemView.findViewById(R.id.snsId);
            deleteBtn = itemView.findViewById(R.id.deleteBtn);
        }
    }
}