package com.example.filter.adapters;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.bumptech.glide.signature.ObjectKey;
import com.example.filter.R;
import com.example.filter.items.FilterItem;

import java.util.ArrayList;
import java.util.List;

public class FilterAdapter extends RecyclerView.Adapter<FilterAdapter.VH> {
    public interface OnItemClickListener {
        void onClick(View v, FilterItem item, String displayedTitle, String displayedNickname);
    }

    private OnItemClickListener listener;

    public void setOnItemClickListener(OnItemClickListener l) {
        this.listener = l;
    }

    private final List<FilterItem> items = new ArrayList<>();
    private int maxItems = Integer.MAX_VALUE;
    private Context context;


    public FilterAdapter() {
    }

    public FilterAdapter(Context context) {
        this.context = context.getApplicationContext();
    }

    public void validateAndAppend(List<FilterItem> itemsToValidate) {
        if (context == null || itemsToValidate.isEmpty()) return;

        //FilterItem item = itemsToValidate.get(0);

        for (FilterItem item : itemsToValidate) {
            Glide.with(context)
                    .load(item.filterImageUrl)
                    .signature(new ObjectKey(item.id))
                    .listener(new RequestListener<Drawable>() {
                        @Override
                        public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                            return false;
                        }

                        @Override
                        public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                            items.add(item);
                            notifyItemInserted(items.size() - 1);
                            return false;
                        }
                    })
                    .preload();
        }
    }

    /*public void append(List<FilterItem> more) {
        int start = items.size();
        items.addAll(more);
        notifyItemRangeInserted(start, more.size());
    }*/

    public void prepend(FilterItem item) {
        items.add(0, item);
        if (items.size() > maxItems) {
            notifyDataSetChanged();
        } else {
            notifyItemInserted(0);
        }
    }

    public void removeItemById(String id) {
        if (id == null) return;

        int targetIndex = -1;
        for (int i = 0; i < items.size(); i++) {
            FilterItem item = items.get(i);
            if (item != null && id.equals(item.id)) {
                targetIndex = i;
                break;
            }
        }

        if (targetIndex == -1) return;
        items.remove(targetIndex);
        if (targetIndex < maxItems) notifyDataSetChanged();

    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.i_filter, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        if (position < 0 || position >= items.size()) {
            return;
        }

        FilterItem item = items.get(position);
        if (item == null) return;

        String titleToShow;
        String nicknameToShow;
        if (item.isMockData) {
            int mockIndex = 0;
            for (int i = 0; i <= position; i++) {
                if (items.get(i).isMockData) {
                    mockIndex++;
                }
            }
            titleToShow = "필터이름" + mockIndex;
            nicknameToShow = "@" + "닉네임" + mockIndex;
        } else {
            titleToShow = item.filterTitle;
            nicknameToShow = item.nickname;
        }
        holder.nickname.setText(nicknameToShow);
        holder.filterTitle.setText(titleToShow);

        //holder.nickname.setText(item.nickname);
        //holder.filterTitle.setText(item.filterTitle);
        holder.price.setText(String.valueOf(item.price));
        holder.count.setText(String.valueOf(item.count + "회 사용"));

        Glide.with(holder.filterImage.getContext())
                .load(item.filterImageUrl)
                .signature(new ObjectKey(item.id))
                .fitCenter()
                .into(holder.filterImage);

        if (titleToShow != null) {
            holder.filterTitle.post(() -> {
                int currentPosition = holder.getAdapterPosition();
                if (currentPosition == RecyclerView.NO_POSITION) {
                    return;
                }
                if (currentPosition < 0 || currentPosition >= items.size()) {
                    return;
                }

                FilterItem currentItem = items.get(currentPosition);
                if (currentItem != null && currentItem.filterTitle != null) {
                    float threshold = holder.filterTitle.getWidth();
                    float textWidth = holder.filterTitle.getPaint().measureText(titleToShow);

                    if (textWidth > threshold) {
                        holder.filterTitle.setSingleLine(true);
                        holder.filterTitle.setEllipsize(TextUtils.TruncateAt.END);
                    } else {
                        holder.filterTitle.setSingleLine(false);
                        holder.filterTitle.setEllipsize(null);
                    }
                }
            });
        } else {
            holder.filterTitle.setSingleLine(false);
            holder.filterTitle.setEllipsize(null);
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
                        String title = filterTitle.getText().toString();
                        String nick = nickname.getText().toString();

                        listener.onClick(v, items.get(pos), title, nick);
                    }
                }
            });
        }
    }
}