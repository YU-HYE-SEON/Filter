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

import java.util.ArrayList;
import java.util.List;

public class FilterListAdapter extends RecyclerView.Adapter<FilterListAdapter.VH> {

    public interface OnItemClickListener {
        void onClick(View v, FilterListItem item);
    }

    private OnItemClickListener listener;

    public void setOnItemClickListener(OnItemClickListener l) {
        this.listener = l;
    }

    private final List<FilterListItem> items = new ArrayList<>();
    private int maxItems = Integer.MAX_VALUE;

    public FilterListAdapter() {
    }

    // ✅ 데이터 초기화 메서드 추가 (MainActivity에서 사용됨)
    public void clear() {
        this.items.clear();
        notifyDataSetChanged();
    }

    /// 어댑터에 이미 존재하는 필터인지 판단 ///
    public boolean containsId(String id) {
        if (id == null) return false;
        for (FilterListItem item : items) {
            // FilterListItem의 id는 Long이므로 String으로 변환해 비교
            if (item != null && id.equals(String.valueOf(item.id))) {
                return true;
            }
        }
        return false;
    }

    /// 새로운 필터 추가 ///
    public void addItem(FilterListItem item) {
        items.add(item); // ✅ 수정 코드 (맨 뒤에 추가 -> 정순 유지)

        // 갱신 알림 (전체 갱신보다는 효율적으로)
        notifyItemInserted(items.size() - 1);

        // (MaxItems 로직이 필요하다면 유지하되, 뒤에 추가하는 로직에 맞게 조정 필요)
        /* if (items.size() > maxItems) {
            items.remove(0); // 개수 초과 시 맨 앞(오래된 것?) 삭제? -> 상황에 따라 다름
            notifyItemRemoved(0);
        }
        */
    }

    // 리스트 전체를 받아서 갱신하는 메서드 (서버에서 받아온 순서 보장)
    public void setItems(List<FilterListItem> newItems) {
        this.items.clear();
        this.items.addAll(newItems);
        notifyDataSetChanged();
    }

    // ✅ [수정됨] FilterListItem 생성자에 맞춰 수정
    public void updatePriceItem(String id, String newPriceStr) {
        if (id == null) return;
        int targetIndex = -1;

        int newPrice = 0;
        try {
            newPrice = Integer.parseInt(newPriceStr);
        } catch (NumberFormatException e) { return; }

        for (int i = 0; i < items.size(); i++) {
            FilterListItem item = items.get(i);
            // ID 비교 (Long vs String)
            if (item != null && id.equals(String.valueOf(item.id))) {
                targetIndex = i;

                // ✅ FilterListItem 생성자 규격에 맞게 수정
                FilterListItem updatedItem = new FilterListItem(
                        item.id,
                        item.filterTitle,
                        item.thumbmailUrl,
                        item.nickname,
                        newPrice,       // 업데이트된 가격
                        item.useCount,
                        item.usage,
                        item.bookmark
                );
                items.set(i, updatedItem);
                break;
            }
        }

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

        // ✅ [수정됨] 복잡한 Mock 데이터 로직 제거하고 단순 매핑
        // FilterListItem에는 표시할 데이터가 다 들어있다고 가정합니다.

        holder.nickname.setText(item.nickname);
        holder.filterTitle.setText(item.filterTitle);
        holder.price.setText(String.valueOf(item.price));

        // useCount 사용
        holder.count.setText(item.useCount + "회 사용");

        // ✅ [수정됨] 필드명 변경 (filterImageUrl -> thumbmailUrl)
        if (item.thumbmailUrl != null) {
            Glide.with(holder.filterImage.getContext())
                    .load(item.thumbmailUrl)
                    .signature(new ObjectKey(String.valueOf(item.id))) // Long ID -> String
                    .fitCenter()
                    .into(holder.filterImage);
        }

        // 제목 길이 처리 로직 (기존 유지)
        if (item.filterTitle != null) {
            holder.filterTitle.post(() -> {
                int currentPosition = holder.getAdapterPosition();
                if (currentPosition == RecyclerView.NO_POSITION || currentPosition >= items.size()) return;

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
    }

    public void setMaxItems(int maxItems) {
        this.maxItems = maxItems;
    }

    @Override
    public int getItemCount() {
        return Math.min(items.size(), maxItems);
    }

    public class VH extends RecyclerView.ViewHolder {
        ImageView filterImage, coin;
        TextView nickname, filterTitle, price, count;

        public VH(@NonNull View itemView) {
            super(itemView);
            nickname = itemView.findViewById(R.id.nickname);
            filterImage = itemView.findViewById(R.id.filterImage);
            coin = itemView.findViewById(R.id.coin);
            filterTitle = itemView.findViewById(R.id.filterTitle);
            price = itemView.findViewById(R.id.price);
            count = itemView.findViewById(R.id.countTxt);

            itemView.setOnClickListener(v -> {
                int pos = getAdapterPosition();
                if (pos != RecyclerView.NO_POSITION && listener != null) {
                    if (pos >= 0 && pos < items.size()) {
                        listener.onClick(v, items.get(pos));
                    }
                }
            });
        }
    }
}