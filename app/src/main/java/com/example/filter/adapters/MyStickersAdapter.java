package com.example.filter.adapters;

import android.annotation.SuppressLint;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.filter.R;
import com.example.filter.items.StickerItem;

import java.io.File;
import java.util.List;

public class MyStickersAdapter extends RecyclerView.Adapter<MyStickersAdapter.ViewHolder> {
    private final List<StickerItem> items;
    private OnStickerClickListener clickListener;
    private OnStickerDeleteListener deleteListener;
    private int selectedPos = RecyclerView.NO_POSITION;

    public interface OnStickerClickListener {
        void onStickerClick(int position, String stickerNameOrPath);
    }

    public interface OnStickerDeleteListener {
        void onDeleteRequested(int position, String stickerNameOrPath);
    }

    public void setOnStickerClickListener(OnStickerClickListener l) {
        this.clickListener = l;
    }

    public void setOnStickerDeleteListener(OnStickerDeleteListener l) {
        this.deleteListener = l;
    }

    public MyStickersAdapter(List<StickerItem> items) {
        this.items = items;
    }

    public void insertAtFront(StickerItem item) {
        items.add(0, item);
        notifyItemInserted(0);
        if (selectedPos != RecyclerView.NO_POSITION) {
            selectedPos++;
        }
    }

    public void removeAt(int position) {
        if (position >= 0 && position < items.size()) {
            items.remove(position);
            notifyItemRemoved(position);
            if (selectedPos == position) selectedPos = RecyclerView.NO_POSITION;
            notifyItemRangeChanged(position, items.size() - position);
        }
    }

    public void clearSelection() {
        int old = selectedPos;
        selectedPos = RecyclerView.NO_POSITION;
        if (old != RecyclerView.NO_POSITION) {
            notifyItemChanged(old);
        }
    }

    public int getSelectedPos() {
        return selectedPos;
    }

    public StickerItem getItem(int pos) {
        return items.get(pos);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.i_mystickers, parent, false);
        return new ViewHolder(v);
    }

    private final int[] fallback = {};

    @Override
    public void onBindViewHolder(@NonNull ViewHolder h, @SuppressLint("RecyclerView") int position) {
        StickerItem item = items.get(position);

        if (item.isFile()) {
            File f = new File(item.getImageUrl());
            if (f.exists()) {
                h.stickerImage.setImageURI(Uri.fromFile(f));
                if (h.stickerImage.getDrawable() == null) {
                    h.stickerImage.setImageBitmap(BitmapFactory.decodeFile(f.getAbsolutePath()));
                }
            } else if (fallback.length > 0) {
                h.stickerImage.setImageResource(fallback[Math.min(position, fallback.length - 1)]);
            } else {
                h.stickerImage.setImageDrawable(null);
            }
        } else {
            if (item.getResName() != null) {
                int resId = h.itemView.getResources()
                        .getIdentifier(item.getResName(), "drawable", h.itemView.getContext().getPackageName());
                if (resId != 0) h.stickerImage.setImageResource(resId);
                else if (fallback.length > 0)
                    h.stickerImage.setImageResource(fallback[Math.min(position, fallback.length - 1)]);
                else
                    h.stickerImage.setImageDrawable(null);
            }
        }

        if (position == selectedPos) {
            h.box.setBackgroundResource(R.drawable.bg_sticker_choose_yes);
        } else {
            h.box.setBackgroundResource(R.drawable.bg_sticker_choose_no);
        }

        h.stickerImage.setOnClickListener(v -> {
            int old = selectedPos;
            selectedPos = position;
            if (old != RecyclerView.NO_POSITION) notifyItemChanged(old);
            notifyItemChanged(selectedPos);
            if (clickListener != null) {
                String key = item.isFile() ? item.getImageUrl() : item.getResName();
                clickListener.onStickerClick(position, key);
            }
        });

        h.itemView.setOnLongClickListener(v -> {
            if (deleteListener != null) {
                String key = item.isFile() ? item.getImageUrl() : item.getResName();
                deleteListener.onDeleteRequested(position, key);
            }
            return true;
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView box, stickerImage;

        public ViewHolder(View itemView) {
            super(itemView);
            box = itemView.findViewById(R.id.box);
            stickerImage = itemView.findViewById(R.id.stickerImage);
        }
    }
}
