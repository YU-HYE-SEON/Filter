package com.example.filter.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
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
import com.example.filter.activities.filter.FilterActivity;
import com.example.filter.activities.filterinfo.FilterInfoActivity;
import com.example.filter.adapters.FilterListAdapter;
import com.example.filter.api_datas.response_dto.FilterListResponse;
import com.example.filter.api_datas.response_dto.PageResponse;
import com.example.filter.apis.FilterApi;
import com.example.filter.apis.client.AppRetrofitClient;
import com.example.filter.apis.repositories.StickerRepository;
import com.example.filter.etc.ClickUtils;
import com.example.filter.etc.GridSpaceItemDecoration;
import com.example.filter.etc.StickerStore;
import com.example.filter.fragments.mains.SearchMainFragment;
import com.example.filter.fragments.mypages.MyPageFragment;
import com.example.filter.items.FilterListItem;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends BaseActivity {
    private ConstraintLayout mainActivity;
    private ImageView logo;
    private ImageButton searchBtn, random, hot, newest;
    private ImageButton home, filter, myPage;
    private FrameLayout searchFrame, mypageFrame;

    private TextView textView;
    private RecyclerView recyclerView;

    // ✅ [수정] Adapter 이름 변경 (FilterAdapter -> FilterListAdapter)
    private FilterListAdapter filterAdapter;
    private static final int MAX_ITEMS = 50;

    private ActivityResultLauncher<Intent> detailActivityLauncher;
    public ArrayList<String> searchHistory = new ArrayList<>();

    // 갤러리 런처
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

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 1. StickerStore 초기화
        StickerStore store = StickerStore.get();
        store.init(getApplicationContext());
        store.setUploader(new StickerRepository(getApplicationContext(), null));

        // 2. UI 초기화
        setContentView(R.layout.a_main);
        mainActivity = findViewById(R.id.mainActivity);
        logo = findViewById(R.id.logo);
        searchBtn = findViewById(R.id.searchBtn);
        random = findViewById(R.id.random);
        hot = findViewById(R.id.hot);
        newest = findViewById(R.id.newest);
        textView = findViewById(R.id.textView);
        recyclerView = findViewById(R.id.recyclerView);
        home = findViewById(R.id.home);
        filter = findViewById(R.id.filter);
        myPage = findViewById(R.id.myPage);
        searchFrame = findViewById(R.id.searchFrame);
        mypageFrame = findViewById(R.id.mypageFrame);

        // 3. 필터 상세 화면 런처
        detailActivityLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        String deletedId = result.getData().getStringExtra("deleted_filter_id");
                        if (deletedId != null && filterAdapter != null) {
                            // filterAdapter에 removeItem 메서드가 있다고 가정
                            filterAdapter.removeItem(deletedId);
                        }
                    }
                });

        loadSearchHistory();

        // 검색 버튼
        searchBtn.setOnClickListener(v -> {
            searchFrame.setVisibility(View.VISIBLE);
            mainActivity.setVisibility(View.GONE);
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.searchFrame, new SearchMainFragment())
                    .addToBackStack(null)
                    .commit();
        });

        // 4. RecyclerView 설정
        StaggeredGridLayoutManager sglm = new StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL);
        sglm.setGapStrategy(StaggeredGridLayoutManager.GAP_HANDLING_MOVE_ITEMS_BETWEEN_SPANS);
        recyclerView.setLayoutManager(sglm);

        // ✅ [수정] FilterListAdapter 사용
        filterAdapter = new FilterListAdapter();
        // filterAdapter.setMaxItems(MAX_ITEMS); // Adapter 내부에 메서드가 있다면 사용
        recyclerView.setAdapter(filterAdapter);
        recyclerView.addItemDecoration(new GridSpaceItemDecoration(2, dp(12), dp(18)));

        // 데이터 변경 감지 및 뷰 갱신
        filterAdapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override
            public void onChanged() {
                updateRecyclerVisibility();
            }
            @Override
            public void onItemRangeInserted(int positionStart, int itemCount) {
                updateRecyclerVisibility();
            }
            @Override
            public void onItemRangeRemoved(int positionStart, int itemCount) {
                updateRecyclerVisibility();
            }
        });

        updateRecyclerVisibility();

        // 5. 아이템 클릭 리스너 (상세 화면 이동)
        // FilterListAdapter 내부 인터페이스에 맞게 수정 필요
        filterAdapter.setOnItemClickListener((v, item) -> {
            Intent intent = new Intent(MainActivity.this, FilterInfoActivity.class);

            // FilterListItem의 필드를 사용하여 Intent 데이터 채우기
            intent.putExtra("filterId", String.valueOf(item.id));
            intent.putExtra("nickname", item.nickname);
            intent.putExtra("imgUrl", item.thumbmailUrl);
            intent.putExtra("filterTitle", item.filterTitle);
            intent.putExtra("price", String.valueOf(item.price));

            // 주의: FilterListItem에는 태그나 상세 정보가 없을 수 있음.
            // 상세 정보는 FilterInfoActivity에서 다시 조회하거나,
            // 목록 API가 더 많은 정보를 주도록 DTO를 맞춰야 함.

            detailActivityLauncher.launch(intent);
        });

        // 로고 클릭 (맨 위로)
        logo.setOnClickListener(v -> {
            recyclerView.post(() -> {
                sglm.invalidateSpanAssignments();
                recyclerView.smoothScrollToPosition(0);
                recyclerView.postDelayed(() -> sglm.scrollToPositionWithOffset(0, 0), 800);
            });
        });

        // 6. 정렬 버튼 리스너
        View.OnClickListener listener = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int id = view.getId();
                switch (id) {
                    case R.id.random:
                        setFilterButtons(true, false, false);
                        break;
                    case R.id.hot:
                        setFilterButtons(false, true, false);
                        loadHotFilters(); // 인기순 목록 불러오기
                        break;
                    case R.id.newest:
                        setFilterButtons(false, false, true);
                        loadRecentFilters(); // 서버에서 최신 필터 목록 가져오기
                        break;
                }
            }
        };

        random.setOnClickListener(listener);
        hot.setOnClickListener(listener);
        newest.setOnClickListener(listener);

        // 하단 네비게이션 (홈)
        home.setOnClickListener(v -> {
            searchFrame.setVisibility(View.GONE);
            mypageFrame.setVisibility(View.GONE);
            mainActivity.setVisibility(View.VISIBLE);
            getSupportFragmentManager().popBackStack(null, getSupportFragmentManager().POP_BACK_STACK_INCLUSIVE);
            home.setImageResource(R.drawable.icon_home_yes);
            myPage.setImageResource(R.drawable.icon_mypage_no);
        });

        // 하단 네비게이션 (필터 제작)
        filter.setOnClickListener(v -> {
            if (ClickUtils.isFastClick(v, 400)) return;
            filter.setEnabled(false);
            Intent intent = new Intent(Intent.ACTION_PICK);
            intent.setType("image/*");
            galleryLauncher.launch(intent);
            filter.postDelayed(() -> filter.setEnabled(true), 500);
        });

        // 하단 네비게이션 (마이페이지)
        myPage.setOnClickListener(v -> {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.mypageFrame, new MyPageFragment())
                    .commitNow();
            mypageFrame.setVisibility(View.VISIBLE);
            searchFrame.setVisibility(View.GONE);
            mainActivity.setVisibility(View.GONE);
            myPage.setImageResource(R.drawable.icon_mypage_yes);
            home.setImageResource(R.drawable.icon_home_no);
        });

        // 새 필터 등록 후 돌아왔을 때 처리
        handleIntent(getIntent());

        // (선택) 앱 시작 시 최신순 자동 로드하려면 아래 주석 해제
        // newest.performClick();
    }

    // 버튼 UI 상태 변경 헬퍼
    private void setFilterButtons(boolean r, boolean h, boolean n) {
        random.setBackgroundResource(r ? R.drawable.btn_random_contents_yes : R.drawable.btn_random_contents_no);
        hot.setBackgroundResource(h ? R.drawable.btn_hot_contents_yes : R.drawable.btn_hot_contents_no);
        newest.setBackgroundResource(n ? R.drawable.btn_newest_contents_yes : R.drawable.btn_newest_contents_no);
    }

    // ---------------------------------------------------------------
    // ✅ [수정됨] 서버 API 통신: 최신 필터 목록 조회
    // ---------------------------------------------------------------
    private void loadRecentFilters() {
        FilterApi api = AppRetrofitClient.getInstance(this).create(FilterApi.class);

        api.getRecentFilters(0, 20).enqueue(new Callback<PageResponse<FilterListResponse>>() {
            @Override
            public void onResponse(Call<PageResponse<FilterListResponse>> call, Response<PageResponse<FilterListResponse>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<FilterListResponse> serverList = response.body().content;
                    List<FilterListItem> uiList = new ArrayList<>();

                    // 1. 변환하여 리스트에 담기 (순서 그대로 유지됨)
                    for (FilterListResponse dto : serverList) {
                        FilterListItem item = new FilterListItem(
                                dto.id,
                                dto.name,
                                dto.thumbnailUrl,
                                dto.creator,
                                dto.pricePoint,
                                dto.useCount != null ? dto.useCount : 0,
                                dto.usage,
                                dto.bookmark
                        );
                        uiList.add(item);
                    }

                    // 2. Adapter에 리스트 통째로 전달 (한 번에 갱신)
                    if (filterAdapter != null) {
                        filterAdapter.setItems(uiList);
                    }

                    // 3. UI 갱신
                    updateRecyclerVisibility();

                } else {
                    Log.e("MainActivity", "목록 조회 실패: " + response.code());
                    Toast.makeText(MainActivity.this, "목록을 불러오지 못했습니다.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<PageResponse<FilterListResponse>> call, Throwable t) {
                Log.e("MainActivity", "통신 오류", t);
                Toast.makeText(MainActivity.this, "서버 연결 실패", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // ---------------------------------------------------------------
    // ✅ [수정됨] 서버 API 통신: 인기 필터 목록 조회
    // ---------------------------------------------------------------
    private void loadHotFilters() {
        FilterApi api = AppRetrofitClient.getInstance(this).create(FilterApi.class);

        api.getHotFilters(0, 20).enqueue(new Callback<PageResponse<FilterListResponse>>() {
            @Override
            public void onResponse(Call<PageResponse<FilterListResponse>> call, Response<PageResponse<FilterListResponse>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<FilterListResponse> serverList = response.body().content;
                    List<FilterListItem> uiList = new ArrayList<>();

                    // 1. 변환하여 리스트에 담기
                    for (FilterListResponse dto : serverList) {
                        FilterListItem item = new FilterListItem(
                                dto.id,
                                dto.name,
                                dto.thumbnailUrl,
                                dto.creator,
                                dto.pricePoint,
                                dto.useCount != null ? dto.useCount : 0,
                                dto.usage,
                                dto.bookmark
                        );
                        uiList.add(item);
                    }

                    // 2. Adapter에 리스트 통째로 전달
                    if (filterAdapter != null) {
                        filterAdapter.setItems(uiList);
                    }

                    // 3. UI 갱신
                    updateRecyclerVisibility();

                } else {
                    Log.e("MainActivity", "인기 목록 조회 실패: " + response.code());
                    Toast.makeText(MainActivity.this, "목록을 불러오지 못했습니다.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<PageResponse<FilterListResponse>> call, Throwable t) {
                Log.e("MainActivity", "통신 오류", t);
                Toast.makeText(MainActivity.this, "서버 연결 실패", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // ---------------------------------------------------------------
    // ✅ [수정] Intent 처리 (FilterListItem 생성)
    // ---------------------------------------------------------------
    private void handleIntent(Intent intent) {
        if (intent == null) return;

        String newImagePath = intent.getStringExtra("imgUrl");

        if (newImagePath != null) {
            String filterIdStr = intent.getStringExtra("filterId");
            long newId = (filterIdStr != null) ? Long.parseLong(filterIdStr) : -1L;

            String nickname = intent.getStringExtra("nickname");
            String title = intent.getStringExtra("filterTitle");
            String priceStr = intent.getStringExtra("price");
            int price = (priceStr != null) ? Integer.parseInt(priceStr) : 0;

            if (title == null) title = "New Filter";
            if (nickname == null) nickname = "@닉네임";

            // 새로운 FilterListItem 생성
            FilterListItem newItem = new FilterListItem(
                    newId,
                    title,
                    newImagePath,
                    nickname,
                    price,
                    0L,     // useCount
                    false,  // usage
                    false   // bookmark
            );

            if (filterAdapter != null) {
                // Adapter에 containsId 메서드가 있다고 가정 (없으면 구현 필요)
                // 또는 무조건 맨 앞에 추가
                filterAdapter.addItem(newItem);

                // 스크롤 맨 위로
                recyclerView.smoothScrollToPosition(0);
            }
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

    // ... (기타 메서드들 유지: onNewIntent, dispatchTouchEvent, searchHistory 관련 등) ...

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        handleIntent(intent);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.searchFrame);
        if (fragment instanceof SearchMainFragment) {
            ((SearchMainFragment) fragment).onParentTouchEvent(ev);
        }
        return super.dispatchTouchEvent(ev);
    }

    @Override
    protected void onPause() {
        super.onPause();
        saveSearchHistory();
    }

    private void loadSearchHistory() {
        SharedPreferences sp = getSharedPreferences("search_pref", MODE_PRIVATE);
        String saved = sp.getString("search_history", "");
        searchHistory.clear();
        if (!saved.isEmpty()) {
            String[] arr = saved.split(",");
            searchHistory.addAll(Arrays.asList(arr));
        }
    }

    public void saveSearchHistory() {
        SharedPreferences sp = getSharedPreferences("search_pref", MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < searchHistory.size(); i++) {
            sb.append(searchHistory.get(i));
            if (i < searchHistory.size() - 1) sb.append(",");
        }
        editor.putString("search_history", sb.toString());
        editor.apply();
    }

    private int dp(int v) {
        return Math.round(getResources().getDisplayMetrics().density * v);
    }

    @Override
    public void onBackPressed() {
        if (mypageFrame.getVisibility() == View.VISIBLE) {
            getSupportFragmentManager().popBackStack(null, getSupportFragmentManager().POP_BACK_STACK_INCLUSIVE);
            mypageFrame.setVisibility(View.GONE);
            searchFrame.setVisibility(View.GONE);
            mainActivity.setVisibility(View.VISIBLE);
            home.setImageResource(R.drawable.icon_home_yes);
            myPage.setImageResource(R.drawable.icon_mypage_no);
            return;
        }
        super.onBackPressed();
    }
}