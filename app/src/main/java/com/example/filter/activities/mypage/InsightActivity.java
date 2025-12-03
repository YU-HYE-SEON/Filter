package com.example.filter.activities.mypage;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.example.filter.R;
import com.example.filter.activities.BaseActivity;
import com.example.filter.api_datas.request_dto.SalesPeriod;
import com.example.filter.api_datas.response_dto.SalesTotalResponse;
import com.example.filter.apis.SalesApi;
import com.example.filter.apis.client.AppRetrofitClient;
import com.github.mikephil.charting.charts.BarChart;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class InsightActivity extends BaseActivity {
    private ImageButton backBtn;
    private TextView title, date, point, sales, bookmark, totalPoint, quantity;
    private ConstraintLayout dateDropdown;
    private FrameLayout modalOff;
    private View chooseDateOn, dimBackground;
    private ConstraintLayout chooseDate;
    private TextView dateTxt, oneWeek, oneMonth, oneYear;
    private ImageView oneWeekCheck, oneMonthCheck, oneYearCheck;
    private boolean ischooseOrderVisible = false;
    private SalesPeriod currentPeriod = SalesPeriod.WEEK;
    private BarChart barChart;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.a_insight);
        backBtn = findViewById(R.id.backBtn);
        title = findViewById(R.id.title);
        date = findViewById(R.id.date);
        point = findViewById(R.id.point);
        sales = findViewById(R.id.sales);
        bookmark = findViewById(R.id.bookmark);
        dateDropdown = findViewById(R.id.dateDropdown);
        dateTxt = findViewById(R.id.dateTxt);
        totalPoint = findViewById(R.id.totalPoint);
        quantity = findViewById(R.id.quantity);
        barChart = findViewById(R.id.barChart);
        modalOff = findViewById(R.id.modalOff);

        backBtn.setOnClickListener(v -> {
            finish();
        });

        loadTotal(SalesPeriod.WEEK);

        // 모달 열려있을 때 뒤로가기 누르면 모달 닫기
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (ischooseOrderVisible) {
                    hideChooseOrder();
                } else {
                    setEnabled(false);
                    onBackPressed();
                }
            }
        });

        setupChooseOrder();
        setDateRangeTexts();
    }

    private void loadTotal(SalesPeriod period) {

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
        chooseDateOn.setVisibility(View.GONE);
        chooseDateOn.setTranslationY(800);

        // 모달 밖 (딤 영역) 누르면 모달 닫기
        dimBackground.setOnClickListener(v -> hideChooseOrder());

        // 날짜 선택 드롭다운 선택 시 모달 열기
        dateDropdown.setOnClickListener(v -> {
            if (ischooseOrderVisible) return;
            showChooseOrder();
        });

        // 최근 일주일 선택
        oneWeek.setOnClickListener(v -> {
            setDate(oneWeek, oneWeekCheck);
            hideChooseOrder();
        });

        // 최근 한달 선택
        oneMonth.setOnClickListener(v -> {
            setDate(oneMonth, oneMonthCheck);
            hideChooseOrder();
        });

        // 최근 일년 선택
        oneYear.setOnClickListener(v -> {
            setDate(oneYear, oneYearCheck);
            hideChooseOrder();
        });
    }

    private void showChooseOrder() {
        ischooseOrderVisible = true;
        dimBackground.setVisibility(View.VISIBLE);
        chooseDateOn.setVisibility(View.VISIBLE);
        chooseDateOn.animate()
                .translationY(0)
                .setDuration(300)
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .setListener(null)
                .start();
    }

    private void hideChooseOrder() {
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
