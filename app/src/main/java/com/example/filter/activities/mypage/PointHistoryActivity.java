package com.example.filter.activities.mypage;

import android.os.Bundle;
import android.util.Log;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatButton;
import androidx.recyclerview.widget.RecyclerView;

import com.example.filter.R;
import com.example.filter.activities.BaseActivity;
import com.example.filter.adapters.PointHistoryAdapter;
import com.example.filter.api_datas.response_dto.CashTransactionListResponse;
import com.example.filter.api_datas.response_dto.FilterTransactionListResponse;
import com.example.filter.api_datas.response_dto.PageResponse;
import com.example.filter.apis.PointApi;
import com.example.filter.apis.client.AppRetrofitClient;
import com.example.filter.items.PointHistoryItem;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PointHistoryActivity extends BaseActivity {
    private ImageButton backBtn;
    private AppCompatButton chargeHistoryBtn, buyHistoryBtn;
    private PointHistoryAdapter adapter1;   // 충전 내역용
    private PointHistoryAdapter adapter2;   // 사용 내역용
    private RecyclerView history;
    private int chargeNextPage = 0;
    private int buyNextPage = 0;
    private boolean isChargeLoading = false;
    private boolean isChargeLastPage = false;
    private boolean isBuyLoading = false;
    private boolean isBuyLastPage = false;
    private int chargeTotalPages = 1;
    private int buyTotalPages = 1;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.a_point_history);

        backBtn = findViewById(R.id.backBtn);
        chargeHistoryBtn = findViewById(R.id.chargeHistoryBtn);
        buyHistoryBtn = findViewById(R.id.buyHistoryBtn);
        history = findViewById(R.id.history);

        adapter1 = new PointHistoryAdapter(new ArrayList<>());
        adapter2 = new PointHistoryAdapter(new ArrayList<>());

        // 기본값: 충전 내역
        history.setAdapter(adapter1);
        loadChargeHistory(chargeNextPage, true);

        history.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                if (dy <= 0) return;
                if (!recyclerView.canScrollVertically(1)) {
                    if (history.getAdapter() == adapter1) {
                        if (!isChargeLoading && chargeNextPage < chargeTotalPages) {
                            loadChargeHistory(chargeNextPage, false);
                        }
                    } else if (history.getAdapter() == adapter2) {
                        if (!isBuyLoading && buyNextPage < buyTotalPages) {
                            loadBuyHistory(buyNextPage, false);
                        }
                    }
                }
            }
        });

        backBtn.setOnClickListener(v -> finish());

        // [충전 내역] 버튼
        chargeHistoryBtn.setOnClickListener(v -> {
            chargeHistoryBtn.setBackgroundResource(R.drawable.btn_select_history_yes);
            chargeHistoryBtn.setTextColor(android.graphics.Color.WHITE);
            buyHistoryBtn.setBackgroundResource(R.drawable.btn_select_history_no);
            buyHistoryBtn.setTextColor(android.graphics.Color.parseColor("#90989F"));

            history.setAdapter(adapter1);
            chargeNextPage = 0;
            isChargeLastPage = false;
            loadChargeHistory(chargeNextPage, true);
        });

        // [사용 내역] 버튼
        buyHistoryBtn.setOnClickListener(v -> {
            buyHistoryBtn.setBackgroundResource(R.drawable.btn_select_history_yes);
            buyHistoryBtn.setTextColor(android.graphics.Color.WHITE);
            chargeHistoryBtn.setBackgroundResource(R.drawable.btn_select_history_no);
            chargeHistoryBtn.setTextColor(android.graphics.Color.parseColor("#90989F"));

            history.setAdapter(adapter2);
            buyNextPage = 0;
            isBuyLastPage = false;
            loadBuyHistory(buyNextPage, true);
        });
    }

    // ---------------------------------------------------------------
    // ✅ [서버 통신] 충전 내역 조회
    // ---------------------------------------------------------------
    private void loadChargeHistory(int page,boolean isFirstLoadisFirstLoad) {
        if (isChargeLoading || isChargeLastPage) return;
        isChargeLoading = true;

        PointApi api = AppRetrofitClient.getInstance(this).create(PointApi.class);
        api.getChargeHistory(page, 20).enqueue(new Callback<PageResponse<CashTransactionListResponse>>() {
            @Override
            public void onResponse(Call<PageResponse<CashTransactionListResponse>> call, Response<PageResponse<CashTransactionListResponse>> response) {
                isChargeLoading = false;
                if (response.isSuccessful() && response.body() != null) {
                    PageResponse<CashTransactionListResponse> body = response.body();
                    List<CashTransactionListResponse> list = body.content;

                    if (isFirstLoadisFirstLoad) {
                        adapter1.items.clear();
                    }

                    if (list == null || list.isEmpty()) {
                        isChargeLastPage = true;
                        return;
                    }

                    //chargeTotalPages = body.totalPages;

                    for (CashTransactionListResponse dto : list) {
                        PointHistoryItem item = new PointHistoryItem();
                        item.point1 = dto.point;         // 충전된 포인트
                        item.point2 = dto.balance;       // 잔여 포인트
                        item.price = String.valueOf((int) dto.cash); // 결제 금액
                        item.date = formatDate(dto.createdAt); // 날짜 변환
                        item.isBuyHistory = false;

                        adapter1.items.add(item);
                    }
                    adapter1.notifyDataSetChanged();

                    chargeNextPage++;

                    if (list.size() < 20) {
                        isChargeLastPage = true;
                    }

                } else {
                    isChargeLoading = false;
                    Log.e("PointHistory", "충전 내역 실패: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<PageResponse<CashTransactionListResponse>> call, Throwable t) {
                isChargeLoading = false;
                Log.e("PointHistory", "통신 오류", t);
            }
        });
    }

    // ---------------------------------------------------------------
    // ✅ [서버 통신] 사용 내역 조회
    // ---------------------------------------------------------------
    private void loadBuyHistory(int page, boolean isFirstLoad) {
        if (isBuyLoading || isBuyLastPage) return;
        isBuyLoading = true;

        PointApi api = AppRetrofitClient.getInstance(this).create(PointApi.class);
        api.getUsageHistory(page, 20).enqueue(new Callback<PageResponse<FilterTransactionListResponse>>() {
            @Override
            public void onResponse(Call<PageResponse<FilterTransactionListResponse>> call, Response<PageResponse<FilterTransactionListResponse>> response) {
                isBuyLoading = false;
                if (response.isSuccessful() && response.body() != null) {
                    PageResponse<FilterTransactionListResponse> body = response.body();
                    List<FilterTransactionListResponse> list = body.content;

                    if (list == null || list.isEmpty()) {
                        isBuyLastPage = true;
                        return;
                    }

                    if (isFirstLoad) {
                        adapter2.items.clear();
                    }
                    //buyTotalPages = body.totalPages;

                    for (FilterTransactionListResponse dto : list) {
                        PointHistoryItem item = new PointHistoryItem();
                        item.point1 = dto.amount;        // 사용한 포인트
                        item.point2 = dto.balance;       // 잔여 포인트
                        item.filterTitle = dto.filterName; // 구매한 필터명
                        item.date = formatDate(dto.createdAt);
                        item.isBuyHistory = true;

                        adapter2.items.add(item);
                    }
                    adapter2.notifyDataSetChanged();

                    buyNextPage++;

                    if (list.size() < 20) {
                        isBuyLastPage = true;
                    }

                } else {
                    isBuyLoading = false;
                    Log.e("PointHistory", "사용 내역 실패: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<PageResponse<FilterTransactionListResponse>> call, Throwable t) {
                isBuyLoading = false;
                Log.e("PointHistory", "통신 오류", t);
            }
        });
    }

    // ✅ 날짜 포맷 변환 (2025-11-22T08:08:41... -> 2025.11.22 08:08)
    private String formatDate(String isoDateString) {
        try {
            // 서버에서 오는 형식 (ISO 8601)
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
            // 앱에 표시할 형식
            SimpleDateFormat outputFormat = new SimpleDateFormat("yyyy.MM.dd HH:mm", Locale.getDefault());

            Date date = inputFormat.parse(isoDateString);
            return outputFormat.format(date);
        } catch (Exception e) {
            return isoDateString; // 변환 실패 시 원본 반환
        }
    }
}