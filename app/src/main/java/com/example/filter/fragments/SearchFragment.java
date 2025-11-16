package com.example.filter.fragments;

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
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

import com.example.filter.R;
import com.example.filter.etc.ClickUtils;

public class SearchFragment extends Fragment {
    private RecyclerView recyclerView;
    private ConstraintLayout textView, dropdown;
    private static final String ARG_KEYWORD = "search_keyword";
    private String keyword;
    private ImageButton random, hot, newest;
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

        random = view.findViewById(R.id.random);
        hot = view.findViewById(R.id.hot);
        newest = view.findViewById(R.id.newest);
        txt = view.findViewById(R.id.txt);
        dropdown = view.findViewById(R.id.dropdown);
        recyclerView = view.findViewById(R.id.recyclerView);
        textView = view.findViewById(R.id.textView);

        View.OnClickListener listener = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int id = view.getId();
                switch (id) {
                    case R.id.random:
                        random.setBackgroundResource(R.drawable.btn_random_contents_yes);
                        hot.setBackgroundResource(R.drawable.btn_hot_contents_no);
                        newest.setBackgroundResource(R.drawable.btn_newest_contents_no);
                        break;
                    case R.id.hot:
                        hot.setBackgroundResource(R.drawable.btn_hot_contents_yes);
                        random.setBackgroundResource(R.drawable.btn_random_contents_no);
                        newest.setBackgroundResource(R.drawable.btn_newest_contents_no);
                        break;
                    case R.id.newest:
                        newest.setBackgroundResource(R.drawable.btn_newest_contents_yes);
                        random.setBackgroundResource(R.drawable.btn_random_contents_no);
                        hot.setBackgroundResource(R.drawable.btn_hot_contents_no);
                        break;
                    default:
                        random.setBackgroundResource(R.drawable.btn_random_contents_no);
                        hot.setBackgroundResource(R.drawable.btn_hot_contents_no);
                        newest.setBackgroundResource(R.drawable.btn_newest_contents_no);
                }
            }
        };

        random.setOnClickListener(listener);
        hot.setOnClickListener(listener);
        newest.setOnClickListener(listener);

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

        if (keyword == null || keyword.isEmpty()) {
            textView.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            textView.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        }
    }

    private View chooseOrderOn, dimBackground;
    private ConstraintLayout chooseOrder;
    private TextView recommend, sales, price, save, review;
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
            setOrderTxt(recommend);
            hideChooseOrder();
        });

        sales.setOnClickListener(v -> {
            setOrderTxt(sales);
            hideChooseOrder();
        });

        price.setOnClickListener(v -> {
            setOrderTxt(price);
            hideChooseOrder();
        });

        save.setOnClickListener(v -> {
            setOrderTxt(save);
            hideChooseOrder();
        });

        review.setOnClickListener(v -> {
            setOrderTxt(review);
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

    private void setOrderTxt(TextView select) {
        int gray = Color.parseColor("#90989F");
        recommend.setTextColor(gray);
        sales.setTextColor(gray);
        price.setTextColor(gray);
        save.setTextColor(gray);
        review.setTextColor(gray);
        select.setTextColor(Color.BLACK);
        txt.setText(select.getText().toString());
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
