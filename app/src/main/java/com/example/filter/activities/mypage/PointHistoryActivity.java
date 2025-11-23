package com.example.filter.activities.mypage;

import android.os.Bundle;
import android.util.Log;
import android.widget.ImageButton;
import android.widget.Toast;

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

        backBtn.setOnClickListener(v -> finish());

        // [충전 내역] 버튼
        chargeHistoryBtn.setOnClickListener(v -> {
            chargeHistoryBtn.setBackgroundResource(R.drawable.btn_select_history_yes);
            chargeHistoryBtn.setTextColor(android.graphics.Color.WHITE);
            buyHistoryBtn.setBackgroundResource(R.drawable.btn_select_history_no);
            buyHistoryBtn.setTextColor(android.graphics.Color.parseColor("#90989F"));

            history.setAdapter(adapter1);
            loadChargeHistory();
        });

        // [사용 내역] 버튼
        buyHistoryBtn.setOnClickListener(v -> {
            buyHistoryBtn.setBackgroundResource(R.drawable.btn_select_history_yes);
            buyHistoryBtn.setTextColor(android.graphics.Color.WHITE);
            chargeHistoryBtn.setBackgroundResource(R.drawable.btn_select_history_no);
            chargeHistoryBtn.setTextColor(android.graphics.Color.parseColor("#90989F"));

            history.setAdapter(adapter2);
            loadBuyHistory();
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // 현재 탭에 맞는 데이터 새로고침
        if (history.getAdapter() == adapter1) {
            loadChargeHistory();
        } else {
            loadBuyHistory();
        }
    }

    // ---------------------------------------------------------------
    // ✅ [서버 통신] 충전 내역 조회
    // ---------------------------------------------------------------
    private void loadChargeHistory() {
        PointApi api = AppRetrofitClient.getInstance(this).create(PointApi.class);

        api.getChargeHistory(0, 50).enqueue(new Callback<PageResponse<CashTransactionListResponse>>() {
            @Override
            public void onResponse(Call<PageResponse<CashTransactionListResponse>> call, Response<PageResponse<CashTransactionListResponse>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<CashTransactionListResponse> list = response.body().content;
                    adapter1.items.clear();

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
                } else {
                    Log.e("PointHistory", "충전 내역 실패: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<PageResponse<CashTransactionListResponse>> call, Throwable t) {
                Log.e("PointHistory", "통신 오류", t);
            }
        });
    }

    // ---------------------------------------------------------------
    // ✅ [서버 통신] 사용 내역 조회
    // ---------------------------------------------------------------
    private void loadBuyHistory() {
        PointApi api = AppRetrofitClient.getInstance(this).create(PointApi.class);

        api.getUsageHistory(0, 50).enqueue(new Callback<PageResponse<FilterTransactionListResponse>>() {
            @Override
            public void onResponse(Call<PageResponse<FilterTransactionListResponse>> call, Response<PageResponse<FilterTransactionListResponse>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<FilterTransactionListResponse> list = response.body().content;
                    adapter2.items.clear();

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
                } else {
                    Log.e("PointHistory", "사용 내역 실패: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<PageResponse<FilterTransactionListResponse>> call, Throwable t) {
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