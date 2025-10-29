package com.example.filter.items;

import android.widget.ImageView;
import android.widget.TextView;

import androidx.constraintlayout.widget.ConstraintLayout;

public class SearchKeywordItem {
    private ConstraintLayout searchHistory;
    private TextView keywordTxt;
    private ImageView deleteHistory;

    public SearchKeywordItem(ConstraintLayout searchHistory, TextView keywordTxt, ImageView deleteHistory) {
        this.searchHistory = searchHistory;
        this.keywordTxt = keywordTxt;
        this.deleteHistory = deleteHistory;
    }
}