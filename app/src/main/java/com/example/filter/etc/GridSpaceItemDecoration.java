package com.example.filter.etc;

import android.graphics.Rect;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class GridSpaceItemDecoration extends RecyclerView.ItemDecoration {
    private final int spanCount;
    private final int hSpacing;
    private final int vSpacing;

    public GridSpaceItemDecoration(int spanCount, int hSpacing, int vSpacing) {
        this.spanCount = spanCount;
        this.hSpacing = hSpacing;
        this.vSpacing = vSpacing;
    }

    @Override
    public void getItemOffsets(@NonNull Rect outRect, @NonNull View view, @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
        outRect.left = hSpacing / 2;
        outRect.right = hSpacing / 2;
        outRect.top = vSpacing / 2;
        outRect.bottom = vSpacing / 2;
    }
}