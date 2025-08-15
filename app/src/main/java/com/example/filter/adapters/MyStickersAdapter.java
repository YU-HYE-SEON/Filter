package com.example.filter.adapters;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.filter.R;

import java.util.List;

public class MyStickersAdapter extends RecyclerView.Adapter<MyStickersAdapter.ViewHolder> {
    private List<String> items;
    private OnStickerClickListener listener;

    public interface OnStickerClickListener {
        void onStickerClick(String stickerName);  // sticker_hearts 등 전달
    }

    public void setOnStickerClickListener(OnStickerClickListener listener) {
        this.listener = listener;
    }

    public MyStickersAdapter(List<String> items) {
        this.items = items;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public ImageView stickerImage;

        public ViewHolder(View itemView) {
            super(itemView);
            stickerImage = itemView.findViewById(R.id.stickerImage);
        }
    }

    @NonNull
    @Override
    public MyStickersAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.i_mystickers, parent, false);
        return new ViewHolder(view);
    }

    private final int[] stickerImages = {
            R.drawable.sticker_hearts_no,
            R.drawable.sticker_blueheart_no,
            R.drawable.sticker_cyanheart_no
    };

    @NonNull
    @Override
    public void onBindViewHolder(@NonNull MyStickersAdapter.ViewHolder holder, int position) {
        if (position < stickerImages.length) {
            holder.stickerImage.setImageResource(stickerImages[position]);

            // 클릭 시 yes 이미지로 바꾸고 callback 호출
            holder.stickerImage.setOnClickListener(v -> {
                // yes 버전 이미지 리소스 ID 가져오기
                String name = items.get(position);  // ex: "sticker_hearts"
                int yesResId = holder.itemView.getContext().getResources().getIdentifier(
                        name + "_yes", "drawable", holder.itemView.getContext().getPackageName());

                if (yesResId != 0) {
                    holder.stickerImage.setImageResource(yesResId);
                }

                if (listener != null) {
                    listener.onStickerClick(name); // sticker_hearts 등 전달
                }
            });
        } else {
            holder.stickerImage.setBackgroundColor(Color.parseColor("#BDBDBD"));
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }
}