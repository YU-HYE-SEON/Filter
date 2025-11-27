package com.example.filter.fragments.mains;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

import com.example.filter.R;
import com.example.filter.adapters.SearchKeywordAdapter;
import com.example.filter.api_datas.response_dto.FilterListResponse;
import com.example.filter.api_datas.response_dto.FilterSortType;
import com.example.filter.api_datas.response_dto.PageResponse;
import com.example.filter.api_datas.response_dto.SearchType;
import com.example.filter.apis.FilterApi;
import com.example.filter.apis.client.AppRetrofitClient;

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

        setupChooseOrder();

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

    private List<FilterListResponse> searchResults = new ArrayList<>();
    private SearchKeywordAdapter resultAdapter;
    private void searchByNaturalLanguage(String query) {
        FilterApi api = AppRetrofitClient.getInstance(requireContext()).create(FilterApi.class);
        // 👈 자연어 모드FilterSortType.ACCURACY,// 👈 정확도순 (AI 랭킹 유지)0 / page20 / size
        api.searchFilters(query, SearchType.NL, FilterSortType.ACCURACY, 0, 20 ).enqueue(new Callback<PageResponse<FilterListResponse>>() {
            @Override
            public void onResponse(Call<PageResponse<FilterListResponse>> call, Response<PageResponse<FilterListResponse>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<FilterListResponse> results = response.body().content;

                    if (results.isEmpty()) {
                        textView.setVisibility(View.VISIBLE);
                        recyclerView.setVisibility(View.GONE);

                        searchResults.clear();
                        //searchResults.addAll(response.body().getContent());
                        resultAdapter.notifyDataSetChanged();
                    } else {
                        textView.setVisibility(View.GONE);
                        recyclerView.setVisibility(View.VISIBLE);
                    }
                }
            }

            @Override
            public void onFailure(Call<PageResponse<FilterListResponse>> call, Throwable t) {
                // 에러 처리
            }
        });
    }


    private View chooseOrderOn, dimBackground;
    private ConstraintLayout chooseOrder;
    private TextView recommend, sales, price, save, review;
    private ImageView recommendCheck, salesCheck, priceCheck, saveCheck, reviewCheck;
    private boolean ischooseOrderVisible = false;

    private void setupChooseOrder() {
        FrameLayout rootView = requireActivity().findViewById(R.id.chooseOrderOff);
        chooseOrderOn = getLayoutInflater().inflate(R.layout.f_choose_order, null);
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
