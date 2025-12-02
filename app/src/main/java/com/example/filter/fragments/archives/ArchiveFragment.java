package com.example.filter.fragments.archives;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

import com.example.filter.R;
import com.example.filter.activities.MainActivity;
import com.example.filter.adapters.FilterListAdapter;
import com.example.filter.api_datas.response_dto.CashTransactionListResponse;
import com.example.filter.api_datas.response_dto.FilterListResponse;
import com.example.filter.api_datas.response_dto.FilterTransactionListResponse;
import com.example.filter.api_datas.response_dto.PageResponse;
import com.example.filter.apis.ArchiveApi;
import com.example.filter.apis.PointApi;
import com.example.filter.apis.client.AppRetrofitClient;
import com.example.filter.etc.GridSpaceItemDecoration;
import com.example.filter.items.FilterListItem;
import com.example.filter.items.PointHistoryItem;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ArchiveFragment extends Fragment {
    private ImageButton bookmark, buy, create, review;
    private RecyclerView recyclerView;
    private FilterListAdapter adapter1; //뷱마크용
    private FilterListAdapter adapter2; //구매용
    private FilterListAdapter adapter3; //제작용
    private FilterListAdapter adapter4; //리뷰용

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.f_archive, container, false);

        bookmark = view.findViewById(R.id.bookmark);
        buy = view.findViewById(R.id.buy);
        create = view.findViewById(R.id.create);
        review = view.findViewById(R.id.review);
        recyclerView = view.findViewById(R.id.recyclerView);

        View.OnClickListener listener = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int id = view.getId();
                switch (id) {
                    case R.id.bookmark:
                        setArchiveButtons(true, false, false, false);
                        recyclerView.setAdapter(adapter1);
                        loadBookmark();
                        break;
                    case R.id.buy:
                        setArchiveButtons(false, true, false, false);
                        recyclerView.setAdapter(adapter2);
                        loadBuy();
                        break;
                    case R.id.create:
                        setArchiveButtons(false, false, true, false);
                        recyclerView.setAdapter(adapter3);
                        loadCreate();
                        break;
                    case R.id.review:
                        setArchiveButtons(false, false, false, true);
                        recyclerView.setAdapter(adapter4);
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

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        StaggeredGridLayoutManager sglm = new StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL);
        sglm.setGapStrategy(StaggeredGridLayoutManager.GAP_HANDLING_MOVE_ITEMS_BETWEEN_SPANS);
        recyclerView.setLayoutManager(sglm);

        adapter1 = new FilterListAdapter();
        adapter2 = new FilterListAdapter();
        adapter3 = new FilterListAdapter();
        adapter4 = new FilterListAdapter();

        recyclerView.setAdapter(adapter1);

        recyclerView.addItemDecoration(new GridSpaceItemDecoration(2, dp(12), dp(18)));

        MainActivity activity = (MainActivity) requireActivity();

        requireActivity().getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(), new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (getChildFragmentManager().getBackStackEntryCount() > 0) {
                    getChildFragmentManager().popBackStack();
                } else {
                    setEnabled(false);
                    requireActivity().onBackPressed();
                }
            }
        });
    }


    private void loadBookmark() {
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

                    if (adapter1 != null) {
                        adapter1.setItems(uiList);
                    }

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

                    if (adapter2 != null) {
                        adapter2.setItems(uiList);
                    }

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

                    if (adapter3 != null) {
                        adapter3.setItems(uiList);
                    }

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

    private void setArchiveButtons(boolean bm, boolean b, boolean c, boolean r) {
        bookmark.setBackgroundResource(bm ? R.drawable.btn_bookmark_yes : R.drawable.btn_bookmark_no);
        buy.setBackgroundResource(b ? R.drawable.btn_buy_yes : R.drawable.btn_buy_no);
        create.setBackgroundResource(c ? R.drawable.btn_create_yes : R.drawable.btn_create_no);
        review.setBackgroundResource(r ? R.drawable.btn_review_yes : R.drawable.btn_review_no);
    }

    private int dp(int v) {
        return Math.round(getResources().getDisplayMetrics().density * v);
    }

    @Override
    public void onResume() {
        super.onResume();

        if (recyclerView.getAdapter() == adapter1) {
            loadBookmark();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        requireActivity().findViewById(R.id.archiveFrame).setVisibility(View.GONE);
        requireActivity().findViewById(R.id.mainActivity).setVisibility(View.VISIBLE);
    }
}