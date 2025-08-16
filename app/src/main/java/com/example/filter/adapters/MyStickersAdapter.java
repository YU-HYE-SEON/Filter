package com.example.filter.adapters;

import android.annotation.SuppressLint;
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
    private OnStickerClickListener clickListener;
    private OnStickerDeleteListener deleteListener;
    private int selectedPos = RecyclerView.NO_POSITION;

    public interface OnStickerClickListener {
        void onStickerClick(int position, String stickerName);
    }
    public interface OnStickerDeleteListener {
        void onDeleteRequested(int position, String stickerName);
    }

    public void setOnStickerClickListener(OnStickerClickListener l) { this.clickListener = l; }
    public void setOnStickerDeleteListener(OnStickerDeleteListener l) { this.deleteListener = l; }

    public MyStickersAdapter(List<String> items) { this.items = items; }

    public void removeAt(int position) {
        if (position >= 0 && position < items.size()) {
            items.remove(position);
            notifyItemRemoved(position);
            if (selectedPos == position) selectedPos = RecyclerView.NO_POSITION;
            notifyItemRangeChanged(position, items.size() - position);
        }
    }

    public int getSelectedPos() { return selectedPos; }
    public String getItem(int pos){ return items.get(pos); }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent,int viewType){
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.i_mystickers,parent,false);
        return new ViewHolder(v);
    }

    private final int[] stickerImages = {
            R.drawable.sticker_hearts_no,
            R.drawable.sticker_blueheart_no,
            R.drawable.sticker_cyanheart_no
    };

    @Override
    public void onBindViewHolder(@NonNull ViewHolder h, @SuppressLint("RecyclerView") int position) {
        String name = items.get(position);
        int noRes = h.itemView.getContext().getResources().getIdentifier(name + "_no","drawable",h.itemView.getContext().getPackageName());
        int yesRes = h.itemView.getContext().getResources().getIdentifier(name + "_yes","drawable",h.itemView.getContext().getPackageName());
        h.stickerImage.setImageResource(selectedPos == position && yesRes!=0 ? yesRes : (noRes!=0? noRes : stickerImages[Math.min(position, stickerImages.length-1)]));

        h.stickerImage.setOnClickListener(v -> {
            int old = selectedPos;
            selectedPos = position;
            notifyItemChanged(old);
            notifyItemChanged(selectedPos);
            if (clickListener != null) clickListener.onStickerClick(position, name);
        });

        h.itemView.setOnLongClickListener(v -> {
            if (deleteListener != null) deleteListener.onDeleteRequested(position, name);
            return true;
        });
    }

    @Override public int getItemCount(){ return items.size(); }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView stickerImage;
        public ViewHolder(View itemView){
            super(itemView);
            stickerImage = itemView.findViewById(R.id.stickerImage);
        }
    }
}
