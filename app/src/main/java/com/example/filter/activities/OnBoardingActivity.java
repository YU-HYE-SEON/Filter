package com.example.filter.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatButton;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

import com.example.filter.R;
import com.example.filter.adapters.OnBoardingAdapter;
import com.example.filter.etc.GridSpaceItemDecoration;
import com.example.filter.items.OnBoardingItem;

import java.util.ArrayList;
import java.util.List;

public class OnBoardingActivity extends BaseActivity {
    private View rootView;
    private RecyclerView recyclerView;
    private OnBoardingAdapter adapter;
    private AppCompatButton nextBtn;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.a_onboarding);
        rootView = findViewById(R.id.rootView);
        recyclerView = findViewById(R.id.recyclerView);
        nextBtn = findViewById(R.id.nextBtn);

        rootView.animate()
                .alpha(1.0f)
                .setDuration(350)
                .start();

        StaggeredGridLayoutManager sglm = new StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL);
        sglm.setGapStrategy(StaggeredGridLayoutManager.GAP_HANDLING_MOVE_ITEMS_BETWEEN_SPANS);
        recyclerView.setLayoutManager(sglm);

        adapter = new OnBoardingAdapter();
        recyclerView.setAdapter(adapter);
        recyclerView.addItemDecoration(new GridSpaceItemDecoration(2, dp(12), dp(18)));

        /*List<Integer> imageIds = new ArrayList<>();
        imageIds.add(R.drawable.);*/    // 이런 식으로 직접 추가

        //10개만 추가
        List<OnBoardingItem> itemList = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            itemList.add(new OnBoardingItem());
        }

        /*for (int resId : imageIds) {
            itemList.add(new OnBoardingItem(resId));
        }*/

        adapter.setItems(itemList);

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

        nextBtn.setOnClickListener(v -> {
            Intent intent = new Intent(OnBoardingActivity.this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });
    }

    private int dp(int v) {
        return Math.round(getResources().getDisplayMetrics().density * v);
    }
}
