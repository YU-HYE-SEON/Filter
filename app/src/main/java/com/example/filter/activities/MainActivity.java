package com.example.filter.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

import com.example.filter.R;
import com.example.filter.adapters.FilterAdapter;
import com.example.filter.apis.repositories.StickerRepository;
import com.example.filter.etc.ClickUtils;
import com.example.filter.apis.dto.FilterDtoCreateRequest;
import com.example.filter.etc.StickerStore;
import com.example.filter.items.FilterItem;
import com.example.filter.etc.GridSpaceItemDecoration;
import com.example.filter.fragments.SearchMainFragment;

public class MainActivity extends BaseActivity {
    private ConstraintLayout mainActivity;
    private ImageView logo;
    private EditText searchTxt;
    //private boolean maybeTap = false;
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
    private TextView textView;
    private RecyclerView recyclerView;
    private FilterAdapter filterAdapter;
    private static final int MAX_ITEMS = 50;
    /*private boolean isLoading = false;
    private int page = 0;
    private final int PAGE_SIZE = 10;*/
    private ActivityResultLauncher<Intent> detailActivityLauncher;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // ✅ ① StickerStore 초기화 및 서버 업로더 설정
        StickerStore store = StickerStore.get();
        store.init(getApplicationContext());
        store.setUploader(new StickerRepository(getApplicationContext()));


        // UI 초기화
        setContentView(R.layout.a_main);
        mainActivity = findViewById(R.id.mainActivity);
        logo = findViewById(R.id.logo);
        searchTxt = findViewById(R.id.searchTxt);
        textView = findViewById(R.id.textView);
        recyclerView = findViewById(R.id.recyclerView);
        filter = findViewById(R.id.filter);

        detailActivityLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        String deletedId = result.getData().getStringExtra("deleted_filter_id");
                        if (deletedId != null && filterAdapter != null) {
                            filterAdapter.removeItemById(deletedId);
                        }
                    }
                });

        /*View root = findViewById(android.R.id.content);
        root.getViewTreeObserver().addOnGlobalLayoutListener(() -> {
            Rect r = new Rect();
            root.getWindowVisibleDisplayFrame(r);
            int screenHeight = root.getRootView().getHeight();
            int keypadHeight = screenHeight - r.bottom;
            boolean keypadVisible = keypadHeight > screenHeight * 0.15;
            if (!keypadVisible) searchTxt.clearFocus();
        });*/

        searchTxt.setOnClickListener(v -> {
            FrameLayout frameLayout = findViewById(R.id.frameLayout);

            frameLayout.setVisibility(View.VISIBLE);
            mainActivity.setVisibility(View.GONE);

            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.frameLayout, new SearchMainFragment())
                    .addToBackStack(null)
                    .commit();
        });

        StaggeredGridLayoutManager sglm = new StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL);
        sglm.setGapStrategy(StaggeredGridLayoutManager.GAP_HANDLING_MOVE_ITEMS_BETWEEN_SPANS);
        recyclerView.setLayoutManager(sglm);

        filterAdapter = new FilterAdapter();
        filterAdapter.setMaxItems(MAX_ITEMS);
        recyclerView.setAdapter(filterAdapter);
        recyclerView.addItemDecoration(new GridSpaceItemDecoration(2, dp(12), dp(18)));

        filterAdapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override
            public void onChanged() {
                super.onChanged();
                updateRecyclerVisibility();
            }

            @Override
            public void onItemRangeInserted(int positionStart, int itemCount) {
                super.onItemRangeInserted(positionStart, itemCount);
                updateRecyclerVisibility();
            }

            @Override
            public void onItemRangeRemoved(int positionStart, int itemCount) {
                super.onItemRangeRemoved(positionStart, itemCount);
                updateRecyclerVisibility();
            }
        });

        updateRecyclerVisibility();

        //loadNextPage();

        /*recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                if (dy <= 0 || isLoading) return;

                int[] lastVisible = sglm.findLastVisibleItemPositions(null);
                int last = Math.max(lastVisible[0], lastVisible[1]);
                int total = filterAdapter.getItemCount();

                if (last >= total - 4) loadNextPage();
            }
        });*/

        filterAdapter.setOnItemClickListener((v, item, title, nickname) -> {
            Intent intent = new Intent(MainActivity.this, FilterDetailActivity.class);
            intent.putExtra("filterId", item.id);
            //intent.putExtra("nickname", item.nickname);
            intent.putExtra("nickname", nickname);
            intent.putExtra("original_image_path", item.originalPath);
            intent.putExtra("imgUrl", item.filterImageUrl);
            //intent.putExtra("filterTitle", item.filterTitle);
            intent.putExtra("filterTitle", title);
            intent.putExtra("tags", item.tags);
            intent.putExtra("price", item.price);
            intent.putExtra("count", item.count);

            intent.putExtra("color_adjustments", item.colorAdjustments);
            intent.putExtra("brush_image_path", item.brushPath);
            intent.putExtra("sticker_image_path", item.stickerPath);

            detailActivityLauncher.launch(intent);
        });

        logo.setOnClickListener(v -> {
            recyclerView.post(() -> {
                sglm.invalidateSpanAssignments();
                recyclerView.smoothScrollToPosition(0);
                recyclerView.postDelayed(() -> sglm.scrollToPositionWithOffset(0, 0), 800);
            });
        });

        filter.setOnClickListener(v -> {
            if (ClickUtils.isFastClick(v, 400)) return;
            filter.setEnabled(false);

            Intent intent = new Intent(Intent.ACTION_PICK);
            intent.setType("image/*");
            galleryLauncher.launch(intent);

            filter.postDelayed(() -> filter.setEnabled(true), 500);
        });

        handleIntent(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        handleIntent(intent);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.frameLayout);
        if (fragment instanceof SearchMainFragment) {
            ((SearchMainFragment) fragment).onParentTouchEvent(ev);
        }
        return super.dispatchTouchEvent(ev);
    }

    private void handleIntent(Intent intent) {
        if (intent == null) return;

        String deletedId = intent.getStringExtra("DELETED_ID_FROM_DETAIL");
        if (deletedId != null) {
            if (filterAdapter != null) {
                filterAdapter.removeItemById(deletedId);
            }
            intent.removeExtra("DELETED_ID_FROM_DETAIL");
            setIntent(new Intent());
            return;
        }

        String filterId = intent.getStringExtra("filterId");
        String originalPath = intent.getStringExtra("original_image_path");
        String newImagePath = intent.getStringExtra("imgUrl");
        String brushPath = intent.getStringExtra("brush_image_path");
        String stickerPath = intent.getStringExtra("sticker_image_path");

        if (newImagePath != null) {
            String nickname = intent.getStringExtra("nickname");
            String title = intent.getStringExtra("filterTitle");
            String tags = intent.getStringExtra("tags");
            String price = intent.getStringExtra("price");
            FilterDtoCreateRequest.ColorAdjustments adj =
                    (FilterDtoCreateRequest.ColorAdjustments) intent.getSerializableExtra("color_adjustments");

            String newId = filterId;
            if (title == null) title = "New Filter";
            if (price == null) price = "0";
            if (nickname == null) nickname = "@" + "닉네임";

            FilterItem newItem = new FilterItem(
                    newId,
                    nickname,
                    originalPath,
                    newImagePath,
                    title,
                    tags,
                    price,
                    0,
                    false,
                    adj,
                    brushPath,
                    stickerPath
            );

            if (filterAdapter != null) {
                filterAdapter.prepend(newItem);
            }

            if (recyclerView != null) {
                recyclerView.smoothScrollToPosition(0);
            }

            setIntent(new Intent());
        }
    }

    private void updateRecyclerVisibility() {
        if (filterAdapter.getItemCount() == 0) {
            recyclerView.setVisibility(View.INVISIBLE);
            textView.setVisibility(View.VISIBLE);
        } else {
            recyclerView.setVisibility(View.VISIBLE);
            textView.setVisibility(View.INVISIBLE);
        }
    }

    /*private void loadNextPage() {
        if (isLoading) return;
        if (filterAdapter.getItemCount() >= MAX_ITEMS) return;
        isLoading = true;

        List<FilterItem> fetched = mockFetch(page, PAGE_SIZE);

        int remaining = MAX_ITEMS - filterAdapter.getItemCount();
        if (fetched.size() > remaining) {
            fetched = fetched.subList(0, remaining);
        }

        //filterAdapter.append(fetched);
        filterAdapter.validateAndAppend(fetched);
        page++;
        isLoading = false;

        if (filterAdapter.getItemCount() < MAX_ITEMS) {
            recyclerView.postDelayed(this::loadNextPage, 100);
        }
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
            String mockId = UUID.randomUUID().toString();
            list.add(new FilterItem(
                    mockId,
                    "@" + "닉네임" + (page * size + i + 1),
                    demoImgs[idx],
                    "필터이름" + (page * size + i + 1),
                    null,
                    "000",
                    0,
                    true
            ));
        }
        return list;
    }*/

    private int dp(int v) {
        return Math.round(getResources().getDisplayMetrics().density * v);
    }
}