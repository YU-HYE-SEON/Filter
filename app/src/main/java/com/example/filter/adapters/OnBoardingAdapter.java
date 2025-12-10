package com.example.filter.adapters;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Shader;
import android.graphics.drawable.BitmapDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.filter.R;
import com.example.filter.items.OnBoardingItem;

import java.util.ArrayList;
import java.util.List;

public class OnBoardingAdapter extends RecyclerView.Adapter<OnBoardingAdapter.VH> {
    public interface OnSelectionChangeListener {
        void onSelectionChanged(int selectedCount);
    }

    private OnSelectionChangeListener listener;

    public void setOnSelectionChangeListener(OnSelectionChangeListener l) {
        this.listener = l;
    }

    private final List<OnBoardingItem> items = new ArrayList<>();

    public void setItems(List<OnBoardingItem> newItems) {
        items.clear();
        items.addAll(newItems);
        notifyDataSetChanged();
    }

    private int getSelectedCount() {
        int count = 0;
        for (OnBoardingItem item : items) {
            if (item.isSelected()) count++;
        }
        return count;
    }

    private void addInnerBorderOverlay(ImageView imageView, int strokeWidth) {
        int w = imageView.getWidth();
        int h = imageView.getHeight();
        if (w == 0 || h == 0) return;

        Bitmap overlay = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(overlay);
        int[] colors = new int[]{Color.parseColor("#FFC2FA7A"), Color.parseColor("#FF6FD6C6"), Color.parseColor("#FFC2FA7A")};
        float[] positions = new float[]{0f, 0.5f, 1f};
        LinearGradient gradient;

        float ratio = (float) w / (float) h;
        if (ratio > 0.95f && ratio < 1.05f) {
            double angleRad = Math.toRadians(30);
            float dx = (float) Math.cos(angleRad);
            float dy = (float) Math.sin(angleRad);
            float x = w * dx;
            float y = h * dy;
            gradient = new LinearGradient(0, 0, x, y, colors, positions, Shader.TileMode.CLAMP);
        } else {
            gradient = new LinearGradient(w, 0, 0, h, colors, positions, Shader.TileMode.CLAMP);
        }

        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setStyle(Paint.Style.FILL);

        float outerRadius = 0f;
        float innerRadius = 12f;

        Path outer = new Path();
        Path inner = new Path();

        float density = imageView.getResources().getDisplayMetrics().density;
        int extraInsetDp = 3;
        int inset = (int) (strokeWidth + extraInsetDp * density);

        outer.addRoundRect(0, 0, w, h, outerRadius, outerRadius, Path.Direction.CW);
        inner.addRoundRect(inset, inset, w - inset, h - inset, innerRadius, innerRadius, Path.Direction.CW);

        Path borderPath = new Path();
        borderPath.op(outer, inner, Path.Op.DIFFERENCE);

        paint.setShader(gradient);
        canvas.drawPath(borderPath, paint);

        Paint innerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        innerPaint.setStyle(Paint.Style.FILL);
        innerPaint.setColor(Color.parseColor("#80000000"));

        canvas.drawPath(inner, innerPaint);

        BitmapDrawable drawable = new BitmapDrawable(imageView.getResources(), overlay);
        drawable.setBounds(0, 0, w, h);

        imageView.getOverlay().clear();
        imageView.getOverlay().add(drawable);
    }

    private void removeInnerBorderOverlay(ImageView imageView) {
        imageView.getOverlay().clear();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.i_onboarding, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        OnBoardingItem item = items.get(position);

        Glide.with(holder.itemView.getContext())
                .load(item.getImageResId())
                .into(holder.image1);

        if (item.isSelected()) {
            holder.image1.postDelayed(() -> addInnerBorderOverlay(holder.image1, 5), 0);
            holder.image2.setVisibility(View.VISIBLE);
        } else {
            holder.image1.postDelayed(() -> removeInnerBorderOverlay(holder.image1), 0);
            holder.image2.setVisibility(View.INVISIBLE);
        }

        holder.image1.setOnClickListener(v -> {
            item.setSelected(!item.isSelected());
            notifyItemChanged(position);

            if (listener != null) {
                int count = getSelectedCount();
                listener.onSelectionChanged(count);
            }
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public class VH extends RecyclerView.ViewHolder {
        ImageView image1, image2;

        public VH(@NonNull View itemView) {
            super(itemView);
            image1 = itemView.findViewById(R.id.image1);
            image2 = itemView.findViewById(R.id.image2);
        }
    }
}