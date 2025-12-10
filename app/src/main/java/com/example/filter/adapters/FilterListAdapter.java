package com.example.filter.adapters;

import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.signature.ObjectKey;
import com.example.filter.R;
import com.example.filter.items.FilterListItem; // ✅ 올바른 Import 확인
import com.example.filter.items.PriceDisplayEnum;

import java.util.ArrayList;
import java.util.List;

public class FilterListAdapter extends RecyclerView.Adapter<FilterListAdapter.VH> {
    public interface OnItemClickListener {
        void onClick(View v, FilterListItem item);
    }

    // 북마크 클릭 리스너 인터페이스
    public interface OnBookmarkClickListener {
        void onBookmarkClick(View v, FilterListItem item, int position);
    }

    private OnItemClickListener listener;
    private OnBookmarkClickListener bookmarkListener;

    public void setOnItemClickListener(OnItemClickListener l) {
        this.listener = l;
    }

    public void setOnBookmarkClickListener(OnBookmarkClickListener l) {
        this.bookmarkListener = l;
    }

    private final List<FilterListItem> items = new ArrayList<>();
    private boolean reviewMode = false;

    public void setReviewMode(boolean reviewMode) {
        this.reviewMode = reviewMode;
        notifyDataSetChanged();
    }

    public FilterListAdapter() {
    }

    // ✅ 데이터 초기화 메서드 추가 (MainActivity에서 사용됨)
    public void clear() {
        this.items.clear();
        notifyDataSetChanged();
    }

    // 리스트 전체를 받아서 갱신하는 메서드 (서버에서 받아온 순서 보장)
    public void setItems(List<FilterListItem> newItems) {
        this.items.clear();
        this.items.addAll(newItems);
        notifyDataSetChanged();
    }

    public void updateItem(int position, FilterListItem newItem) {
        if (position >= 0 && position < items.size()) {
            items.set(position, newItem);
            notifyItemChanged(position); // 깜빡임 없이 해당 줄만 갱신
        }
    }

    public void updateBookmarkState(String filterId, boolean newState) {
        if (filterId == null) return;
        int targetIndex = -1;

        // 1. 리스트를 순회하며 해당 ID를 가진 아이템을 찾고, 상태를 변경한 새 객체로 교체
        for (int i = 0; i < items.size(); i++) {
            FilterListItem oldItem = items.get(i);
            if (oldItem != null && filterId.equals(String.valueOf(oldItem.id))) {
                targetIndex = i;

                // FilterListItem은 생성자를 통해 새 객체를 만들어야 합니다.
                FilterListItem newItem = new FilterListItem(
                        oldItem.id,
                        oldItem.filterTitle,
                        oldItem.thumbmailUrl,
                        oldItem.nickname,
                        oldItem.price,
                        oldItem.useCount,
                        oldItem.type,
                        newState // ★ 변경된 북마크 상태
                );

                items.set(i, newItem); // 리스트의 아이템을 새 객체로 교체
                break;
            }
        }

        // 2. 해당 아이템의 뷰만 갱신
        if (targetIndex != -1) {
            notifyItemChanged(targetIndex);
        }
    }

    ///  필터 삭제 ///
    public void removeItem(String id) {
        if (id == null) return;

        int targetIndex = -1;
        for (int i = 0; i < items.size(); i++) {
            FilterListItem item = items.get(i);
            if (item != null && id.equals(String.valueOf(item.id))) {
                targetIndex = i;
                break;
            }
        }

        if (targetIndex == -1) return;
        items.remove(targetIndex);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.i_filter, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        if (position < 0 || position >= items.size()) return;

        FilterListItem item = items.get(position);
        if (item == null) return;

        // 복잡한 Mock 데이터 로직 제거하고 단순 매핑
        // FilterListItem에는 표시할 데이터가 다 들어있다고 가정합니다.

        holder.nickname.setText(item.nickname);
        holder.filterTitle.setText(item.filterTitle);

        // 가격 표시
        if (item.type.equals(PriceDisplayEnum.NONE)) {
            holder.price.setText("무료");
            holder.coin.setVisibility(View.GONE);
        } else if (item.type.equals(PriceDisplayEnum.PURCHASED)) {
            holder.price.setText("구매 완료");
            holder.coin.setVisibility(View.GONE);
        } else if (item.type.equals(PriceDisplayEnum.NUMBER)) {
            holder.price.setText(String.valueOf(item.price));
            holder.coin.setVisibility(View.VISIBLE);
        }

        // useCount 사용
        holder.count.setText(item.useCount + "회 사용");

        // 필드명 변경 (filterImageUrl -> thumbmailUrl)
        if (item.thumbmailUrl != null) {
            Glide.with(holder.filterImage.getContext())
                    .load(item.thumbmailUrl)
                    .signature(new ObjectKey(String.valueOf(item.id))) // Long ID -> String
                    .fitCenter()
                    .into(holder.filterImage);
        }

        // (i_filter.xml에 bookmark 아이콘 id가 'bookmark'라고 가정)
        if (item.bookmark) {
            holder.bookmark.setImageResource(R.drawable.icon_bookmark_yes_lime);
            setBookmarkSize(holder.bookmark, 30f, 36f, 12f);
        } else {
            holder.bookmark.setImageResource(R.drawable.icon_bookmark_no_gray);
            setBookmarkSize(holder.bookmark, 30f, 30f, 15f);
        }

        // 제목 길이 처리 로직 (기존 유지)
        if (item.filterTitle != null) {
            holder.filterTitle.post(() -> {
                int currentPosition = holder.getAdapterPosition();
                if (currentPosition == RecyclerView.NO_POSITION || currentPosition >= items.size())
                    return;

                // 텍스트 길이 체크 로직 (뷰가 재활용되므로 안전장치 필요)
                // ... (기존 로직 유지하되 item 참조만 주의)
                if (holder.filterTitle.getPaint().measureText(item.filterTitle) > holder.filterTitle.getWidth()) {
                    holder.filterTitle.setSingleLine(true);
                    holder.filterTitle.setEllipsize(TextUtils.TruncateAt.END);
                } else {
                    holder.filterTitle.setSingleLine(false);
                    holder.filterTitle.setEllipsize(null);
                }
            });
        }

        if (reviewMode) {
            holder.nickname.setVisibility(View.GONE);
            holder.count.setVisibility(View.GONE);
            holder.bookmark.setVisibility(View.GONE);
        } else {
            holder.nickname.setVisibility(View.VISIBLE);
            holder.count.setVisibility(View.VISIBLE);
            holder.bookmark.setVisibility(View.VISIBLE);
        }
    }

    private void setBookmarkSize(ImageView bookmark, float dp1, float dp2, float dp3) {
        int px1 = (int) dp(dp1, bookmark.getContext());
        int px2 = (int) dp(dp2, bookmark.getContext());

        ViewGroup.LayoutParams lp = bookmark.getLayoutParams();
        lp.width = px1;
        lp.height = px2;
        bookmark.setLayoutParams(lp);

        bookmark.requestLayout();

        ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) bookmark.getLayoutParams();
        params.rightMargin = (int) dp(dp3, bookmark.getContext());
        bookmark.setLayoutParams(params);
    }

    private float dp(float dp, Context context) {
        return Math.round(dp * context.getResources().getDisplayMetrics().density);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public class VH extends RecyclerView.ViewHolder {
        ImageView filterImage, coin, bookmark;
        TextView nickname, filterTitle, price, count;

        public VH(@NonNull View itemView) {
            super(itemView);
            nickname = itemView.findViewById(R.id.nickname);
            filterImage = itemView.findViewById(R.id.filterImage);
            coin = itemView.findViewById(R.id.coin);
            filterTitle = itemView.findViewById(R.id.filterTitle);
            price = itemView.findViewById(R.id.price);
            count = itemView.findViewById(R.id.countTxt);
            bookmark = itemView.findViewById(R.id.bookmark);

            itemView.setOnClickListener(v -> {
                int pos = getAdapterPosition();
                if (pos != RecyclerView.NO_POSITION && listener != null) {
                    if (pos >= 0 && pos < items.size()) {
                        listener.onClick(v, items.get(pos));
                    }
                }
            });

            // 북마크 아이콘 클릭
            if (bookmark != null) {
                bookmark.setOnClickListener(v -> {
                    int pos = getAdapterPosition();
                    if (pos != RecyclerView.NO_POSITION && bookmarkListener != null) {
                        bookmarkListener.onBookmarkClick(v, items.get(pos), pos);
                    }
                });
            }
        }
    }
}