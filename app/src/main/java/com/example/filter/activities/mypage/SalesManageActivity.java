package com.example.filter.activities.mypage;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.filter.R;
import com.example.filter.activities.BaseActivity;
import com.example.filter.adapters.FilterListAdapter;
import com.example.filter.adapters.ReviewInfoAdapter;
import com.example.filter.adapters.SalesListAdapter;
import com.example.filter.api_datas.request_dto.SalesPeriod;
import com.example.filter.api_datas.request_dto.SalesSortType;
import com.example.filter.api_datas.response_dto.FilterListResponse;
import com.example.filter.api_datas.response_dto.FilterSortType;
import com.example.filter.api_datas.response_dto.PageResponse;
import com.example.filter.api_datas.response_dto.ReviewResponse;
import com.example.filter.api_datas.response_dto.SalesListResponse;
import com.example.filter.api_datas.response_dto.SalesTotalResponse;
import com.example.filter.apis.ArchiveApi;
import com.example.filter.apis.SalesApi;
import com.example.filter.apis.client.AppRetrofitClient;
import com.example.filter.items.FilterListItem;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SalesManageActivity extends BaseActivity {
    private ImageButton backBtn;
    private TextView cash, quantity, textView;
    private ConstraintLayout dateDropdown, dropdown;
    private RecyclerView recyclerView;
    private FrameLayout modalOff;
    private SalesListAdapter adapter;
    private View chooseDateOn, chooseSalesOrderOn, dimBackground;
    private ConstraintLayout chooseDate, chooseSalesOrder;
    private TextView dateTxt, oneWeek, oneMonth, oneYear;
    private ImageView oneWeekCheck, oneMonthCheck, oneYearCheck;
    private TextView txt, register, sales, salesPoint, title;
    private ImageView registerCheck, salesCheck, salesPointCheck, titleCheck;
    private boolean ischooseOrderVisible = false;

    private SalesSortType currentSort = SalesSortType.RECENT;
    private SalesPeriod currentPeriod = SalesPeriod.WEEK;
    private int nextPage = 0;
    private boolean isLoading = false;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.a_sales_manage);
        backBtn = findViewById(R.id.backBtn);
        cash = findViewById(R.id.cash);
        quantity = findViewById(R.id.quantity);
        dateDropdown = findViewById(R.id.dateDropdown);
        dropdown = findViewById(R.id.dropdown);
        dateTxt = findViewById(R.id.dateTxt);
        txt = findViewById(R.id.txt);
        recyclerView = findViewById(R.id.recyclerView);
        textView = findViewById(R.id.textView);
        modalOff = findViewById(R.id.modalOff);

        LinearLayoutManager lm = new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false);
        recyclerView.setLayoutManager(lm);
        adapter = new SalesListAdapter();
        recyclerView.setAdapter(adapter);

        backBtn.setOnClickListener(v -> {
            finish();
        });

        loadSalesList(SalesSortType.RECENT, 0);
        loadTotal(SalesPeriod.WEEK);

        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                if (!recyclerView.canScrollVertically(1)) {
                    loadSalesList(currentSort, nextPage);
                }
            }
        });

        // 모달 열려있을 때 뒤로가기 누르면 모달 닫기
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (ischooseOrderVisible) {
                    hideAllChooseOrder();
                } else {
                    setEnabled(false);
                    onBackPressed();
                }
            }
        });

        setupChooseOrder();
        setDateRangeTexts();
    }

    private void loadSalesList(SalesSortType sort, int page) {
        if (isLoading) return;
        isLoading = true;

        SalesApi api = AppRetrofitClient.getInstance(this).create(SalesApi.class);
        api.getSalesList(sort, page, 20).enqueue(new Callback<PageResponse<SalesListResponse>>() {
            @Override
            public void onResponse(Call<PageResponse<SalesListResponse>> call, Response<PageResponse<SalesListResponse>> response) {
                isLoading = false;
                if (response.isSuccessful() && response.body() != null) {

                    PageResponse<SalesListResponse> result = response.body();
                    List<SalesListResponse> list = result.getContent();

                    if (list != null && !list.isEmpty()) {
                        adapter.addItemList(list);
                        nextPage++;
                    }

                    updateRecyclerVisibility();
                }
            }

            @Override
            public void onFailure(Call<PageResponse<SalesListResponse>> call, Throwable t) {
                isLoading = false;
                Toast.makeText(SalesManageActivity.this, "불러오기 실패", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadTotal(SalesPeriod period) {
        SalesApi api = AppRetrofitClient.getInstance(this).create(SalesApi.class);
        api.getTotalSales(period).enqueue(new Callback<SalesTotalResponse>() {
            @Override
            public void onResponse(Call<SalesTotalResponse> call, Response<SalesTotalResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    SalesTotalResponse result = response.body();

                    String formattedAmount = String.format("%,d 원", result.getSettlementAmount());
                    cash.setText(formattedAmount);

                    String formattedQty = result.getTotalSales() + " 개";
                    quantity.setText(formattedQty);
                }
            }

            @Override
            public void onFailure(Call<SalesTotalResponse> call, Throwable t) {
                Toast.makeText(SalesManageActivity.this, "불러오기 실패", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateRecyclerVisibility() {
        if (adapter.getItemCount() == 0) {
            recyclerView.setVisibility(View.INVISIBLE);
            textView.setVisibility(View.VISIBLE);
        } else {
            recyclerView.setVisibility(View.VISIBLE);
            textView.setVisibility(View.INVISIBLE);
        }
    }

    private void setupChooseOrder() {
        FrameLayout rootView = findViewById(R.id.modalOff);

        // 날짜 정렬 선택 모달 셋팅
        chooseDateOn = getLayoutInflater().inflate(R.layout.m_choose_date, null);
        chooseDate = chooseDateOn.findViewById(R.id.chooseDate);
        oneWeek = chooseDateOn.findViewById(R.id.oneWeek);
        oneMonth = chooseDateOn.findViewById(R.id.oneMonth);
        oneYear = chooseDateOn.findViewById(R.id.oneYear);
        oneWeekCheck = chooseDateOn.findViewById(R.id.oneWeekCheck);
        oneMonthCheck = chooseDateOn.findViewById(R.id.oneMonthCheck);
        oneYearCheck = chooseDateOn.findViewById(R.id.oneYearCheck);

        // 판매 중인 필터 정렬 선택 모달 셋팅
        chooseSalesOrderOn = getLayoutInflater().inflate(R.layout.m_choose_sales_order, null);
        chooseSalesOrder = chooseSalesOrderOn.findViewById(R.id.chooseSalesOrder);
        register = chooseSalesOrderOn.findViewById(R.id.register);
        sales = chooseSalesOrderOn.findViewById(R.id.sales);
        salesPoint = chooseSalesOrderOn.findViewById(R.id.salesPoint);
        title = chooseSalesOrderOn.findViewById(R.id.title);
        registerCheck = chooseSalesOrderOn.findViewById(R.id.registerCheck);
        salesCheck = chooseSalesOrderOn.findViewById(R.id.salesCheck);
        salesPointCheck = chooseSalesOrderOn.findViewById(R.id.salesPointCheck);
        titleCheck = chooseSalesOrderOn.findViewById(R.id.titleCheck);

        // 모달 나올 때 딤처리
        dimBackground = new View(this);
        dimBackground.setLayoutParams(new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
        ));
        dimBackground.setBackgroundColor(Color.parseColor("#B3000000"));
        dimBackground.setVisibility(View.GONE);

        // 모달과 딤 추가 후 숨김 처리
        rootView.addView(dimBackground);
        rootView.addView(chooseDateOn);
        rootView.addView(chooseSalesOrderOn);

        chooseDateOn.setVisibility(View.GONE);
        chooseSalesOrderOn.setVisibility(View.GONE);
        chooseDateOn.setTranslationY(800);
        chooseSalesOrderOn.setTranslationY(800);

        // 모달 밖 (딤 영역) 누르면 모달 닫기
        dimBackground.setOnClickListener(v -> hideAllChooseOrder());


        // 날짜 선택 드롭다운 선택 시 모달 열기
        dateDropdown.setOnClickListener(v -> {
            if (ischooseOrderVisible) return;
            showChooseOrder(chooseDateOn);
        });

        // 최근 일주일 선택
        oneWeek.setOnClickListener(v -> {
            setDate(oneWeek, oneWeekCheck);
            hideAllChooseOrder();
        });

        // 최근 한달 선택
        oneMonth.setOnClickListener(v -> {
            setDate(oneMonth, oneMonthCheck);
            hideAllChooseOrder();
        });

        // 최근 일년 선택
        oneYear.setOnClickListener(v -> {
            setDate(oneYear, oneYearCheck);
            hideAllChooseOrder();
        });

        // 필터 정렬 선택 드롭다운 선택 시 모달 열기
        dropdown.setOnClickListener(v -> {
            if (ischooseOrderVisible) return;
            showChooseOrder(chooseSalesOrderOn);
        });

        // 등록순 선택
        register.setOnClickListener(v -> {
            setOrder(register, registerCheck);
            hideAllChooseOrder();
        });

        // 판매순 선택
        sales.setOnClickListener(v -> {
            setOrder(sales, salesCheck);
            hideAllChooseOrder();
        });

        // 판매포인트순 선택
        salesPoint.setOnClickListener(v -> {
            setOrder(salesPoint, salesPointCheck);
            hideAllChooseOrder();
        });

        // 이름순 선택
        title.setOnClickListener(v -> {
            setOrder(title, titleCheck);
            hideAllChooseOrder();
        });
    }

    private void showChooseOrder(View view) {
        ischooseOrderVisible = true;
        dimBackground.setVisibility(View.VISIBLE);
        view.setVisibility(View.VISIBLE);
        view.animate()
                .translationY(0)
                .setDuration(300)
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .setListener(null)
                .start();
    }

    private void hideAllChooseOrder() {
        chooseDateOn.animate()
                .translationY(800)
                .setDuration(250)
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        chooseDateOn.setVisibility(View.GONE);
                        dimBackground.setVisibility(View.GONE);
                        ischooseOrderVisible = false;
                    }
                })
                .start();

        chooseSalesOrderOn.animate()
                .translationY(800)
                .setDuration(250)
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        chooseSalesOrderOn.setVisibility(View.GONE);
                        dimBackground.setVisibility(View.GONE);
                        ischooseOrderVisible = false;
                    }
                })
                .start();
    }

    private void setDate(TextView select, ImageView select2) {
        int gray = Color.parseColor("#90989F");
        oneWeek.setTextColor(gray);
        oneMonth.setTextColor(gray);
        oneYear.setTextColor(gray);
        select.setTextColor(Color.BLACK);
        if (select.getText().equals(oneWeek.getText())) {
            dateTxt.setText("최근 일주일");
        } else if (select.getText().equals(oneMonth.getText())) {
            dateTxt.setText("최근 한 달");
        } else {
            dateTxt.setText("최근 일 년");
        }

        oneWeekCheck.setVisibility(View.INVISIBLE);
        oneMonthCheck.setVisibility(View.INVISIBLE);
        oneYearCheck.setVisibility(View.INVISIBLE);
        select2.setVisibility(View.VISIBLE);

        if (select == oneWeek) currentPeriod = SalesPeriod.WEEK;
        else if (select == oneMonth) currentPeriod = SalesPeriod.MONTH;
        else if (select == oneYear) currentPeriod = SalesPeriod.YEAR;


        loadTotal(currentPeriod);
    }

    private void setOrder(TextView select, ImageView select2) {
        int gray = Color.parseColor("#90989F");
        register.setTextColor(gray);
        sales.setTextColor(gray);
        salesPoint.setTextColor(gray);
        title.setTextColor(gray);
        select.setTextColor(Color.BLACK);
        txt.setText(select.getText().toString());

        registerCheck.setVisibility(View.INVISIBLE);
        salesCheck.setVisibility(View.INVISIBLE);
        salesPointCheck.setVisibility(View.INVISIBLE);
        titleCheck.setVisibility(View.INVISIBLE);
        select2.setVisibility(View.VISIBLE);

        if (select == register) currentSort = SalesSortType.RECENT;
        else if (select == sales) currentSort = SalesSortType.COUNT;
        else if (select == salesPoint) currentSort = SalesSortType.AMOUNT;
        else if (select == title) currentSort = SalesSortType.NAME;

        adapter.clear();
        nextPage = 0;
        loadSalesList(currentSort, nextPage);
    }

    // 모달 날짜 표시
    private void setDateRangeTexts() {
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy.MM.dd");

        // 오늘
        java.util.Calendar today = java.util.Calendar.getInstance();
        String todayStr = sdf.format(today.getTime());

        // 일주일 전
        java.util.Calendar weekAgo = java.util.Calendar.getInstance();
        weekAgo.add(java.util.Calendar.DAY_OF_YEAR, -7);
        String weekAgoStr = sdf.format(weekAgo.getTime());

        // 한 달 전
        java.util.Calendar monthAgo = java.util.Calendar.getInstance();
        monthAgo.add(java.util.Calendar.MONTH, -1);
        String monthAgoStr = sdf.format(monthAgo.getTime());

        // 일 년 전
        java.util.Calendar yearAgo = java.util.Calendar.getInstance();
        yearAgo.add(java.util.Calendar.YEAR, -1);
        String yearAgoStr = sdf.format(yearAgo.getTime());

        oneWeek.setText("최근 일주일 / " + weekAgoStr + " - " + todayStr);
        oneMonth.setText("최근 한 달 / " + monthAgoStr + " - " + todayStr);
        oneYear.setText("최근 일 년 / " + yearAgoStr + " - " + todayStr);
    }
}
