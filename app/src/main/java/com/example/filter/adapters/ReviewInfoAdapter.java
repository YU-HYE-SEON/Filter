package com.example.filter.adapters;

import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.filter.R;
import com.example.filter.activities.filter.RegisterActivity;
import com.example.filter.api_datas.response_dto.ReviewResponse;
import com.example.filter.apis.StickerApi;
import com.example.filter.apis.client.AppRetrofitClient;
import com.example.filter.dialogs.ReviewDeleteDialog;
import com.example.filter.dialogs.StickerDeleteDialog;
import com.example.filter.etc.Controller;
import com.example.filter.items.StickerItem;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

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