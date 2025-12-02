package com.example.filter.fragments.mains;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

import com.example.filter.R;
import com.example.filter.adapters.FilterListAdapter;
import com.example.filter.api_datas.response_dto.FilterListResponse;
import com.example.filter.api_datas.response_dto.FilterSortType;
import com.example.filter.api_datas.response_dto.PageResponse;
import com.example.filter.api_datas.response_dto.SearchType;
import com.example.filter.apis.FilterApi;
import com.example.filter.apis.client.AppRetrofitClient;
import com.example.filter.etc.GridSpaceItemDecoration;
import com.example.filter.items.FilterListItem;
import com.example.filter.items.PriceDisplayEnum;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SearchFragment extends Fragment {
    private RecyclerView recyclerView;
    private ConstraintLayout textView, dropdown;
    private static final String ARG_KEYWORD = "search_keyword";
    private String keyword;
    private TextView txt;
    private List<FilterListResponse> searchResults = new ArrayList<>();
    private FilterListAdapter adapter;

    public static SearchFragment newInstance(String keyword) {
        SearchFragment fragment = new SearchFragment();
        Bundle args = new Bundle();
        args.putString(ARG_KEYWORD, keyword);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            keyword = getArguments().getString(ARG_KEYWORD);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.f_search, container, false);

        txt = view.findViewById(R.id.txt);
        dropdown = view.findViewById(R.id.dropdown);
        recyclerView = view.findViewById(R.id.recyclerView);
        textView = view.findViewById(R.id.textView);

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        requireActivity().getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(), new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (ischooseOrderVisible) {
                    hideChooseOrder();
                } else {
                    setEnabled(false);
                    requireActivity().onBackPressed();
                }
            }
        });

        setupChooseOrder();

        StaggeredGridLayoutManager sglm = new StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL);
        sglm.setGapStrategy(StaggeredGridLayoutManager.GAP_HANDLING_MOVE_ITEMS_BETWEEN_SPANS);
        recyclerView.setLayoutManager(sglm);

        adapter = new FilterListAdapter();
        recyclerView.setAdapter(adapter);
        recyclerView.addItemDecoration(new GridSpaceItemDecoration(2, dp(10), dp(10)));

        if (keyword != null && !keyword.isEmpty()) {
            searchByNaturalLanguage(keyword);
        }

        Fragment parent = getParentFragment();
        if (parent instanceof SearchMainFragment) {
            ((SearchMainFragment) parent).resetSearchButton();
        }

        /*if (keyword == null || keyword.isEmpty()) {
            textView.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            textView.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        }*/
    }

    private void searchByNaturalLanguage(String query) {
        FilterApi api = AppRetrofitClient.getInstance(requireContext()).create(FilterApi.class);
        // 👈 자연어 모드FilterSortType.ACCURACY,// 👈 정확도순 (AI 랭킹 유지)0 / page20 / size
        api.searchFilters(query, SearchType.NL, currentSort, 0, 200).enqueue(new Callback<PageResponse<FilterListResponse>>() {
            @Override
            public void onResponse(Call<PageResponse<FilterListResponse>> call, Response<PageResponse<FilterListResponse>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Log.e("검색", "검색 조회 성공 | 정렬: " + currentSort.name());

                    List<FilterListResponse> results = response.body().content;

                    Log.d("검색", "리스폰스 바디 콘텐트 | "+response.body().content);

                    if (!results.isEmpty()) {
                        textView.setVisibility(View.GONE);
                        recyclerView.setVisibility(View.VISIBLE);

                        List<FilterListItem> itemsToShow = convertResponseToFilterListItems(results);
                        adapter.setItems(itemsToShow);

                        searchResults.clear();
                        searchResults.addAll(results);
                    } else {
                        adapter.clear();
                        showEmptyResult();
                    }
                } else {
                    Log.e("검색", "검색 조회 실패: " + response.code());
                    Toast.makeText(requireContext(), "정보를 불러오지 못했습니다.", Toast.LENGTH_SHORT).show();

                    adapter.clear();
                    showEmptyResult();
                }
            }

            @Override
            public void onFailure(Call<PageResponse<FilterListResponse>> call, Throwable t) {
                showEmptyResult();

                adapter.clear();
                showEmptyResult();
                Log.e("검색", "통신 오류", t);
            }
        });
    }


    private List<FilterListItem> convertResponseToFilterListItems(List<FilterListResponse> responses) {
        List<FilterListItem> items = new ArrayList<>();

        for (FilterListResponse res : responses) {
            PriceDisplayEnum type = PriceDisplayEnum.fromString(res.priceDisplayType);

            FilterListItem item = new FilterListItem(
                    res.id,
                    res.name,
                    res.thumbnailUrl,
                    res.creator,
                    res.pricePoint,
                    res.useCount,
                    type,
                    res.bookmark
            );
            items.add(item);
        }
        return items;
    }

    private void showEmptyResult() {
        textView.setVisibility(View.VISIBLE);
        recyclerView.setVisibility(View.GONE);
    }

    private FilterSortType currentSort = FilterSortType.ACCURACY;

    private void setOrder(TextView select, ImageView select2) {
        int gray = Color.parseColor("#90989F");
        recommend.setTextColor(gray);
        sales.setTextColor(gray);
        price.setTextColor(gray);
        save.setTextColor(gray);
        review.setTextColor(gray);
        select.setTextColor(Color.BLACK);
        txt.setText(select.getText().toString());

        recommendCheck.setVisibility(View.INVISIBLE);
        salesCheck.setVisibility(View.INVISIBLE);
        priceCheck.setVisibility(View.INVISIBLE);
        saveCheck.setVisibility(View.INVISIBLE);
        reviewCheck.setVisibility(View.INVISIBLE);
        select2.setVisibility(View.VISIBLE);

        FilterSortType newSort;
        if (select == price) newSort = FilterSortType.LOW_PRICE;
        else if (select == save) newSort = FilterSortType.POPULARITY;
        else if (select == review) newSort = FilterSortType.REVIEW_COUNT;
        else if (select == sales) newSort = FilterSortType.LATEST;
        else newSort = FilterSortType.ACCURACY;

        if (currentSort != newSort) {
            currentSort = newSort; // 새로운 정렬 타입 설정
            searchByNaturalLanguage(keyword); // 정렬 변경 후 검색 재실행
        }

        //searchByNaturalLanguage(keyword);
    }


    private View chooseOrderOn, dimBackground;
    private ConstraintLayout chooseOrder;
    private TextView recommend, sales, price, save, review;
    private ImageView recommendCheck, salesCheck, priceCheck, saveCheck, reviewCheck;
    private boolean ischooseOrderVisible = false;

    private void setupChooseOrder() {
        FrameLayout rootView = requireActivity().findViewById(R.id.chooseOrderOff);
        chooseOrderOn = getLayoutInflater().inflate(R.layout.m_choose_order, null);
        chooseOrder = chooseOrderOn.findViewById(R.id.chooseOrder);
        recommend = chooseOrderOn.findViewById(R.id.recommend);
        sales = chooseOrderOn.findViewById(R.id.sales);
        price = chooseOrderOn.findViewById(R.id.price);
        save = chooseOrderOn.findViewById(R.id.save);
        review = chooseOrderOn.findViewById(R.id.review);
        recommendCheck = chooseOrderOn.findViewById(R.id.recommendCheck);
        salesCheck = chooseOrderOn.findViewById(R.id.salesCheck);
        priceCheck = chooseOrderOn.findViewById(R.id.priceCheck);
        saveCheck = chooseOrderOn.findViewById(R.id.saveCheck);
        reviewCheck = chooseOrderOn.findViewById(R.id.reviewCheck);

        dimBackground = new View(requireContext());
        dimBackground.setLayoutParams(new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
        ));
        dimBackground.setBackgroundColor(Color.parseColor("#B3000000"));
        dimBackground.setVisibility(View.GONE);

        rootView.addView(dimBackground);
        rootView.addView(chooseOrderOn);
        chooseOrderOn.setVisibility(View.GONE);
        chooseOrderOn.setTranslationY(800);

        dimBackground.setOnClickListener(v -> hideChooseOrder());

        dropdown.setOnClickListener(v -> {
            if (ischooseOrderVisible) return;
            showChooseOrder();
        });

        recommend.setOnClickListener(v -> {
            setOrder(recommend, recommendCheck);
            hideChooseOrder();
        });

        sales.setOnClickListener(v -> {
            setOrder(sales, salesCheck);
            hideChooseOrder();
        });

        price.setOnClickListener(v -> {
            setOrder(price, priceCheck);
            hideChooseOrder();
        });

        save.setOnClickListener(v -> {
            setOrder(save, saveCheck);
            hideChooseOrder();
        });

        review.setOnClickListener(v -> {
            setOrder(review, reviewCheck);
            hideChooseOrder();
        });
    }

    private void showChooseOrder() {
        ischooseOrderVisible = true;
        dimBackground.setVisibility(View.VISIBLE);
        chooseOrderOn.setVisibility(View.VISIBLE);
        chooseOrderOn.animate()
                .translationY(0)
                .setDuration(300)
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .setListener(null)
                .start();
    }

    private void hideChooseOrder() {
        chooseOrderOn.animate()
                .translationY(800)
                .setDuration(250)
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        chooseOrderOn.setVisibility(View.GONE);
                        dimBackground.setVisibility(View.GONE);
                        ischooseOrderVisible = false;
                    }
                })
                .start();
    }

    private int dp(int v) {
        return Math.round(getResources().getDisplayMetrics().density * v);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (dimBackground != null && dimBackground.getVisibility() == View.VISIBLE) {
            dimBackground.setVisibility(View.GONE);
            ischooseOrderVisible = false;
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        Fragment parent = getParentFragment();
        if (parent instanceof SearchMainFragment) {
            ((SearchMainFragment) parent).resetSearchButton();
        }
    }
}
