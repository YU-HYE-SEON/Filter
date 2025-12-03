package com.example.filter.etc;

import android.content.Context;
import android.widget.ImageView;

import com.example.filter.R;
import com.github.mikephil.charting.components.MarkerView;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.utils.MPPointF;

public class BestMarkerView extends MarkerView {

    private final ImageView icon;

    public BestMarkerView(Context context, int layoutResource) {
        super(context, layoutResource);
        icon = findViewById(R.id.bestIcon);
    }

    @Override
    public void refreshContent(Entry e, Highlight highlight) {
        super.refreshContent(e, highlight);
    }

    @Override
    public MPPointF getOffset() {
        return new MPPointF(-(getWidth() / 2f), -getHeight() * 1.5f);
    }
}