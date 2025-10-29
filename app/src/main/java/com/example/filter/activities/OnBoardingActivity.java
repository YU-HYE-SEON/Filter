package com.example.filter.activities;

import android.os.Bundle;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatButton;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

import com.example.filter.R;
import com.example.filter.adapters.OnBoardingAdapter;
import com.example.filter.etc.GridSpaceItemDecoration;

public class OnBoardingActivity extends BaseActivity {
    private RecyclerView recyclerView;
    private OnBoardingAdapter adapter;
    private AppCompatButton nextBtn;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.a_onboarding);
        recyclerView = findViewById(R.id.recyclerView);
        nextBtn = findViewById(R.id.nextBtn);

        StaggeredGridLayoutManager sglm = new StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL);
        sglm.setGapStrategy(StaggeredGridLayoutManager.GAP_HANDLING_MOVE_ITEMS_BETWEEN_SPANS);
        recyclerView.setLayoutManager(sglm);

        adapter = new OnBoardingAdapter();
        recyclerView.setAdapter(adapter);
        recyclerView.addItemDecoration(new GridSpaceItemDecoration(2, dp(12), dp(18)));

        adapter.setOnSelectionChangeListener(selectedCount -> {
            if (selectedCount >= 3) {
                if (nextBtn.getVisibility() != View.VISIBLE) {
                    nextBtn.setAlpha(0.0f);
                    nextBtn.setVisibility(View.VISIBLE);
                    nextBtn.animate()
                            .alpha(1.0f)
                            .setDuration(400)
                            .start();
                }
            } else {
                if (nextBtn.getVisibility() == View.VISIBLE) {
                    nextBtn.animate()
                            .alpha(0.0f)
                            .setDuration(400)
                            .withEndAction(() -> nextBtn.setVisibility(View.INVISIBLE))
                            .start();
                }
            }
        });
    }

    private int dp(int v) {
        return Math.round(getResources().getDisplayMetrics().density * v);
    }
}
