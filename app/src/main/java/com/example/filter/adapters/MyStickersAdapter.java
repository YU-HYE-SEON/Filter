package com.example.filter.adapters;

import android.annotation.SuppressLint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide; // Glide 라이브러리 필요
import com.example.filter.R;
import com.example.filter.items.StickerItem;

import java.util.List;

public class MyStickersAdapter extends RecyclerView.Adapter<MyStickersAdapter.ViewHolder> {
    private List<StickerItem> items;
    private OnStickerClickListener clickListener;
    private OnStickerDeleteListener deleteListener;
    private int selectedPos = RecyclerView.NO_POSITION;

    // [수정] 클릭 시 StickerItem 객체 자체를 전달 (ID 접근을 위해)
    public interface OnStickerClickListener {
        void onStickerClick(int position, StickerItem item);
    }

    public interface OnStickerDeleteListener {
        void onDeleteRequested(int position, StickerItem item);
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

    // [추가] 서버 데이터 갱신용 메서드
    @SuppressLint("NotifyDataSetChanged")
    public void updateData(List<StickerItem> newItems) {
        this.items = newItems;
        notifyDataSetChanged();
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
        if (pos >= 0 && pos < items.size()) return items.get(pos);
        return null;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.i_mystickers, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder h, @SuppressLint("RecyclerView") int position) {
        StickerItem item = items.get(position);

        // [수정] Glide를 사용하여 이미지 로딩 통합 (URL, 파일 경로, 리소스 ID 모두 처리)
        Object imageSource;
        if (item.isFile()) {
            // 서버 URL 또는 로컬 파일 경로
            imageSource = item.getImageUrl();
        } else {
            // 리소스 이름으로 ID 찾기
            int resId = 0;
            if (item.getResName() != null) {
                resId = h.itemView.getResources().getIdentifier(
                        item.getResName(), "drawable", h.itemView.getContext().getPackageName());
            }
            imageSource = (resId != 0) ? resId : null;
        }

        Glide.with(h.itemView.getContext())
                .load(imageSource)
                // .placeholder(R.drawable.bg_trans_pattern) // 로딩 중 표시할 이미지 (선택)
                .error(R.drawable.btn_close) // 에러 시 표시할 이미지 (선택)
                .into(h.stickerImage);

        // 선택 상태 UI 처리
        if (position == selectedPos) {
            h.box.setBackgroundResource(R.drawable.bg_sticker_choose_yes);
        } else {
            h.box.setBackgroundResource(R.drawable.bg_sticker_choose_no);
        }

        // 클릭 리스너
        h.stickerImage.setOnClickListener(v -> {
            int old = selectedPos;
            selectedPos = position;
            if (old != RecyclerView.NO_POSITION) notifyItemChanged(old);
            notifyItemChanged(selectedPos);

            if (clickListener != null) {
                // [수정] Item 객체 통째로 전달
                clickListener.onStickerClick(position, item);
            }
        });

        // 롱클릭 리스너 (삭제)
        h.itemView.setOnLongClickListener(v -> {
            if (deleteListener != null) {
                deleteListener.onDeleteRequested(position, item);
            }
            return true;
        });
    }

    @Override
    public int getItemCount() {
        return items != null ? items.size() : 0;
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