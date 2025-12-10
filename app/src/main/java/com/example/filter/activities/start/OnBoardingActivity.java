package com.example.filter.activities.start;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatButton;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

import com.example.filter.R;
import com.example.filter.activities.BaseActivity;
import com.example.filter.activities.MainActivity;
import com.example.filter.adapters.OnBoardingAdapter;
import com.example.filter.etc.ClickUtils;
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
        recyclerView.addItemDecoration(new GridSpaceItemDecoration(2, dp(10), dp(10)));

        //10개만 추가
        List<OnBoardingItem> itemList = new ArrayList<>();
        itemList.add(new OnBoardingItem(R.drawable.onboarding1));
        itemList.add(new OnBoardingItem(R.drawable.onboarding2));
        itemList.add(new OnBoardingItem(R.drawable.onboarding3));
        itemList.add(new OnBoardingItem(R.drawable.onboarding4));
        itemList.add(new OnBoardingItem(R.drawable.onboarding5));
        itemList.add(new OnBoardingItem(R.drawable.onboarding1));
        itemList.add(new OnBoardingItem(R.drawable.onboarding2));
        itemList.add(new OnBoardingItem(R.drawable.onboarding3));
        itemList.add(new OnBoardingItem(R.drawable.onboarding4));
        itemList.add(new OnBoardingItem(R.drawable.onboarding5));

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

        ClickUtils.clickDim(nextBtn);
        nextBtn.setOnClickListener(v -> {
            if (ClickUtils.isFastClick(v, 400)) return;
            Intent intent = new Intent(OnBoardingActivity.this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });
    }

    private int dp(int v) {
        return Math.round(getResources().getDisplayMetrics().density * v);
    }
}
