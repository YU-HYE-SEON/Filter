package com.example.filter.fragments.archives;

import static android.app.Activity.RESULT_OK;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

import com.example.filter.R;
import com.example.filter.activities.filterinfo.FilterInfoActivity;
import com.example.filter.adapters.FilterListAdapter;
import com.example.filter.api_datas.response_dto.FilterListResponse;
import com.example.filter.api_datas.response_dto.MyReviewResponse;
import com.example.filter.api_datas.response_dto.PageResponse;
import com.example.filter.apis.ArchiveApi;
import com.example.filter.apis.FilterApi;
import com.example.filter.apis.client.AppRetrofitClient;
import com.example.filter.etc.ClickUtils;
import com.example.filter.etc.GridSpaceItemDecoration;
import com.example.filter.items.FilterListItem;
import com.example.filter.items.PriceDisplayEnum;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ArchiveFragment extends Fragment {
    private enum Type {BOOKMARK, BUY, CREATE, REVIEW}

    private Type currentType = Type.BOOKMARK;
    private ImageButton bookmark, buy, create, review;
    private RecyclerView recyclerView;
    private TextView textView;
    private FilterListAdapter filterAdapter;
    private ActivityResultLauncher<Intent> detailActivityLauncher;
    private int nextPage = 0;
    private boolean isLoading = false;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.f_archive, container, false);

        bookmark = view.findViewById(R.id.bookmark);
        buy = view.findViewById(R.id.buy);
        create = view.findViewById(R.id.create);
        review = view.findViewById(R.id.review);
        recyclerView = view.findViewById(R.id.recyclerView);
        textView = view.findViewById(R.id.textView);

        detailActivityLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        String deletedId = result.getData().getStringExtra("deleted_filter_id");
                        if (deletedId != null && filterAdapter != null) {
                            filterAdapter.removeItem(deletedId);
                        }

                        String changedFilterId = result.getData().getStringExtra("filter_id_changed");
                        boolean isBookmarkedNewState = result.getData().getBooleanExtra("is_bookmarked_new_state", false);

                        if (changedFilterId != null && filterAdapter != null) {
                            filterAdapter.updateBookmarkState(changedFilterId, isBookmarkedNewState);
                        }
                    }

                    if (currentType == Type.CREATE) {
                        loadCreate();
                    }
                });

        StaggeredGridLayoutManager sglm = new StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL);
        sglm.setGapStrategy(StaggeredGridLayoutManager.GAP_HANDLING_MOVE_ITEMS_BETWEEN_SPANS);
        recyclerView.setLayoutManager(sglm);

        filterAdapter = new FilterListAdapter();
        recyclerView.setAdapter(filterAdapter);
        recyclerView.addItemDecoration(new GridSpaceItemDecoration(2, dp(12), dp(18)));

        setArchiveButtons(true, false, false, false);
        loadBookmark();

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

        filterAdapter.setOnItemClickListener((v, item) -> {
            Intent intent = new Intent(requireActivity(), FilterInfoActivity.class);

            intent.putExtra("filterId", String.valueOf(item.id));
            intent.putExtra("nickname", item.nickname);
            intent.putExtra("imgUrl", item.thumbmailUrl);
            intent.putExtra("filterTitle", item.filterTitle);
            intent.putExtra("price", String.valueOf(item.price));

            detailActivityLauncher.launch(intent);
        });

        filterAdapter.setOnBookmarkClickListener((v, item, position) -> {
            if (ClickUtils.isFastClick(v, 500)) return;
            requestToggleBookmark(item.id, position, item);
        });

        View.OnClickListener listener = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int id = view.getId();
                switch (id) {
                    case R.id.bookmark:
                        currentType = Type.BOOKMARK;
                        setArchiveButtons(true, false, false, false);
                        loadBookmark();
                        break;
                    case R.id.buy:
                        currentType = Type.BUY;
                        setArchiveButtons(false, true, false, false);
                        loadBuy();
                        break;
                    case R.id.create:
                        currentType = Type.CREATE;
                        setArchiveButtons(false, false, true, false);
                        loadCreate();
                        break;
                    case R.id.review:
                        currentType = Type.REVIEW;
                        setArchiveButtons(false, false, false, true);
                        loadReview();
                        break;
                }
            }
        };

        bookmark.setOnClickListener(listener);
        buy.setOnClickListener(listener);
        create.setOnClickListener(listener);
        review.setOnClickListener(listener);

        return view;
    }

    private void requestToggleBookmark(long filterId, int position, FilterListItem oldItem) {
        FilterApi api = AppRetrofitClient.getInstance(requireActivity()).create(FilterApi.class);

        api.toggleBookmark(filterId).enqueue(new Callback<Boolean>() {
            @Override
            public void onResponse(Call<Boolean> call, Response<Boolean> response) {
                if (response.isSuccessful() && response.body() != null) {
                    boolean newState = response.body();

                    FilterListItem newItem = new FilterListItem(
                            oldItem.id,
                            oldItem.filterTitle,
                            oldItem.thumbmailUrl,
                            oldItem.nickname,
                            oldItem.price,
                            oldItem.useCount,
                            oldItem.type,
                            newState
                    );

                    filterAdapter.updateItem(position, newItem);

                    if (currentType == Type.BOOKMARK) {
                        loadBookmark();
                    }

                    String msg = newState ? "북마크 저장됨" : "북마크 해제됨";
                    Toast.makeText(requireActivity(), msg, Toast.LENGTH_SHORT).show();

                } else {
                    Log.e("Bookmark", "실패: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<Boolean> call, Throwable t) {
                Log.e("Bookmark", "통신 오류", t);
            }
        });
    }

    private void loadBookmark() {
        filterAdapter.setReviewMode(false);

        ArchiveApi api = AppRetrofitClient.getInstance(requireActivity()).create(ArchiveApi.class);
        api.getBookmarks(0, 200).enqueue(new Callback<PageResponse<FilterListResponse>>() {

            @Override
            public void onResponse(Call<PageResponse<FilterListResponse>> call, Response<PageResponse<FilterListResponse>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<FilterListResponse> serverList = response.body().content;
                    List<FilterListItem> uiList = new ArrayList<>();

                    for (FilterListResponse dto : serverList) {
                        FilterListItem item = FilterListItem.convertFromDto(dto);
                        uiList.add(item);
                    }

                    if (filterAdapter != null) {
                        filterAdapter.setItems(uiList);
                    }

                    updateRecyclerVisibility();

                } else {
                    Log.e("아카이브", "목록 조회 실패: " + response.code());
                    Toast.makeText(requireActivity(), "목록을 불러오지 못했습니다.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<PageResponse<FilterListResponse>> call, Throwable t) {
                Log.e("아카이브", "통신 오류", t);
                Toast.makeText(requireActivity(), "서버 연결 실패", Toast.LENGTH_SHORT).show();
            }

        });
    }

    private void loadBuy() {
        filterAdapter.setReviewMode(false);

        ArchiveApi api = AppRetrofitClient.getInstance(requireActivity()).create(ArchiveApi.class);
        api.getUsage(0, 200).enqueue(new Callback<PageResponse<FilterListResponse>>() {

            @Override
            public void onResponse(Call<PageResponse<FilterListResponse>> call, Response<PageResponse<FilterListResponse>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<FilterListResponse> serverList = response.body().content;
                    List<FilterListItem> uiList = new ArrayList<>();

                    for (FilterListResponse dto : serverList) {
                        FilterListItem item = FilterListItem.convertFromDto(dto);
                        uiList.add(item);
                    }

                    if (filterAdapter != null) {
                        filterAdapter.setItems(uiList);
                    }

                    updateRecyclerVisibility();

                } else {
                    Log.e("아카이브", "목록 조회 실패: " + response.code());
                    Toast.makeText(requireActivity(), "목록을 불러오지 못했습니다.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<PageResponse<FilterListResponse>> call, Throwable t) {
                Log.e("아카이브", "통신 오류", t);
                Toast.makeText(requireActivity(), "서버 연결 실패", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadCreate() {
        filterAdapter.setReviewMode(false);

        ArchiveApi api = AppRetrofitClient.getInstance(requireActivity()).create(ArchiveApi.class);
        api.getMyFilters(0, 200).enqueue(new Callback<PageResponse<FilterListResponse>>() {

            @Override
            public void onResponse(Call<PageResponse<FilterListResponse>> call, Response<PageResponse<FilterListResponse>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<FilterListResponse> serverList = response.body().content;
                    List<FilterListItem> uiList = new ArrayList<>();

                    for (FilterListResponse dto : serverList) {
                        FilterListItem item = FilterListItem.convertFromDto(dto);
                        uiList.add(item);
                    }

                    if (filterAdapter != null) {
                        filterAdapter.setItems(uiList);
                    }

                    updateRecyclerVisibility();

                } else {
                    Log.e("아카이브", "목록 조회 실패: " + response.code());
                    Toast.makeText(requireActivity(), "목록을 불러오지 못했습니다.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<PageResponse<FilterListResponse>> call, Throwable t) {
                Log.e("아카이브", "통신 오류", t);
                Toast.makeText(requireActivity(), "서버 연결 실패", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadReview() {
        filterAdapter.setReviewMode(true);

        ArchiveApi api = AppRetrofitClient.getInstance(requireActivity()).create(ArchiveApi.class);
        api.getMyReviews(0, 200).enqueue(new Callback<PageResponse<MyReviewResponse>>() {

            @Override
            public void onResponse(Call<PageResponse<MyReviewResponse>> call, Response<PageResponse<MyReviewResponse>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<MyReviewResponse> serverList = response.body().content;
                    List<FilterListItem> uiList = new ArrayList<>();

                    for (MyReviewResponse dto : serverList) {
                        PriceDisplayEnum displayType = PriceDisplayEnum.fromString(dto.priceDisplayType);

                        FilterListItem item = new FilterListItem(
                                dto.filterId,
                                dto.filterName,
                                dto.imageUrl,
                                null,
                                dto.pricePoint,
                                null,
                                displayType,
                                false
                        );

                        uiList.add(item);
                    }

                    if (filterAdapter != null) {
                        filterAdapter.setItems(uiList);
                    }

                    updateRecyclerVisibility();

                } else {
                    Log.e("아카이브", "목록 조회 실패: " + response.code());
                    Toast.makeText(requireActivity(), "목록을 불러오지 못했습니다.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<PageResponse<MyReviewResponse>> call, Throwable t) {
                Log.e("아카이브", "통신 오류", t);
                Toast.makeText(requireActivity(), "서버 연결 실패", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setArchiveButtons(boolean bm, boolean b, boolean c, boolean r) {
        bookmark.setBackgroundResource(bm ? R.drawable.btn_bookmark_yes : R.drawable.btn_bookmark_no);
        buy.setBackgroundResource(b ? R.drawable.btn_buy_yes : R.drawable.btn_buy_no);
        create.setBackgroundResource(c ? R.drawable.btn_create_yes : R.drawable.btn_create_no);
        review.setBackgroundResource(r ? R.drawable.btn_review_yes : R.drawable.btn_review_no);
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

    private int dp(int v) {
        return Math.round(getResources().getDisplayMetrics().density * v);
    }

    @Override
    public void onResume() {
        super.onResume();

        switch (currentType) {
            case BOOKMARK:
                loadBookmark();
                break;
            case BUY:
                loadBuy();
                break;
            case REVIEW:
                loadReview();
                break;
        }
    }
}