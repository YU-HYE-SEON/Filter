package com.example.filter.activities;

import android.content.Intent;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

import com.example.filter.R;
import com.example.filter.adapters.FilterAdapter;
import com.example.filter.etc.ClickUtils;
import com.example.filter.etc.FilterItem;
import com.example.filter.etc.GridSpaceItemDecoration;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends BaseActivity {
    private EditText searchTxt;
    private boolean maybeTap = false;
    private ImageButton filter;
    private ActivityResultLauncher<Intent> galleryLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri photoUri = result.getData().getData();

                    if (photoUri != null) {
                        Intent intent = new Intent(MainActivity.this, FilterActivity.class);
                        intent.setData(photoUri);
                        startActivity(intent);
                    } else {
                        Toast.makeText(this, "사진 선택 실패: URI 없음", Toast.LENGTH_SHORT).show();
                    }
                }
            });
    private RecyclerView recyclerView;
    private FilterAdapter filterAdapter;
    private boolean isLoading = false;
    private int page = 0;
    private final int PAGE_SIZE = 10;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.a_main);
        searchTxt = findViewById(R.id.searchTxt);
        recyclerView = findViewById(R.id.recyclerView);
        filter = findViewById(R.id.filter);

        View root = findViewById(android.R.id.content);
        root.getViewTreeObserver().addOnGlobalLayoutListener(() -> {
            Rect r = new Rect();
            root.getWindowVisibleDisplayFrame(r);
            int screenHeight = root.getRootView().getHeight();
            int keypadHeight = screenHeight - r.bottom;
            boolean keypadVisible = keypadHeight > screenHeight * 0.15;
            if (!keypadVisible) searchTxt.clearFocus();
        });

        StaggeredGridLayoutManager sglm = new StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL);
        sglm.setGapStrategy(StaggeredGridLayoutManager.GAP_HANDLING_MOVE_ITEMS_BETWEEN_SPANS);
        recyclerView.setLayoutManager(sglm);

        filterAdapter = new FilterAdapter();
        recyclerView.setAdapter(filterAdapter);
        recyclerView.addItemDecoration(new GridSpaceItemDecoration(2, dp(14), dp(18)));
        loadNextPage();

        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                if (dy <= 0 || isLoading) return;

                int[] lastVisible = sglm.findLastVisibleItemPositions(null);
                int last = Math.max(lastVisible[0], lastVisible[1]);
                int total = filterAdapter.getItemCount();

                if (last >= total - 4) loadNextPage();
            }
        });

        filterAdapter.setOnItemClickListener((v, item) -> {
            Intent intent = new Intent(MainActivity.this, FilterDetailActivity.class);
            intent.putExtra("imgUrl", item.filterImageUrl);
            intent.putExtra("filterTitle", item.filterTitle);
            intent.putExtra("price", item.price);
            intent.putExtra("nickname", item.nickname);
            intent.putExtra("count", item.count);
            startActivity(intent);
        });

        filter.setOnClickListener(v -> {
            if (ClickUtils.isFastClick(500)) return;

            Intent intent = new Intent(Intent.ACTION_PICK);
            intent.setType("image/*");
            galleryLauncher.launch(intent);
        });
    }

    private boolean isPoint(MotionEvent ev) {
        if (searchTxt == null) return false;
        Rect r = new Rect();
        boolean visible = searchTxt.getGlobalVisibleRect(r);
        if (!visible) return false;
        final int x = (int) ev.getRawX();
        final int y = (int) ev.getRawY();
        return r.contains(x, y);
    }

    private void hideKeypadAndClearFocus() {
        View v = getCurrentFocus();
        InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        if (imm != null && v != null) {
            imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
        }
        if (searchTxt != null) searchTxt.clearFocus();
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        switch (ev.getActionMasked()) {
            case MotionEvent.ACTION_DOWN: {
                maybeTap = true;
                break;
            }
            case MotionEvent.ACTION_UP: {
                if (maybeTap) {
                    View focused = getCurrentFocus();
                    boolean focusedIsEdit = focused instanceof EditText;
                    boolean tapInsideEdit = isPoint(ev);

                    if (focusedIsEdit && !tapInsideEdit) {
                        hideKeypadAndClearFocus();
                    }
                }
                break;
            }
        }
        return super.dispatchTouchEvent(ev);
    }

    private void loadNextPage() {
        if (isLoading) return;
        isLoading = true;

        List<FilterItem> fetched = mockFetch(page, PAGE_SIZE);
        filterAdapter.append(fetched);
        page++;
        isLoading = false;
    }

    private List<FilterItem> mockFetch(int page, int size) {
        List<FilterItem> list = new ArrayList<>();
        String[] demoImgs = new String[]{
                "https://picsum.photos/400/300",
                "https://picsum.photos/400/600",
                "https://picsum.photos/400/400",
                "https://picsum.photos/500/320",
                "https://picsum.photos/500/700",
                "https://picsum.photos/420/420",
                "https://picsum.photos/480/320",
                "https://picsum.photos/360/540",
                "https://picsum.photos/720/480",
                "https://picsum.photos/600/600"
        };

        for (int i = 0; i < size; i++) {
            int idx = (page * size + i) % demoImgs.length;
            list.add(new FilterItem(demoImgs[idx],
                    "필터이름" + (page * size + i + 1),
                    "닉네임" + (page * size + i + 1),
                    "000",
                    0
            ));
        }
        return list;
    }

    private int dp(int v) {
        return Math.round(getResources().getDisplayMetrics().density * v);
    }
}