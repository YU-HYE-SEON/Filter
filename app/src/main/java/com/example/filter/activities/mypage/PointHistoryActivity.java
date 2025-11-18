package com.example.filter.activities.mypage;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.ImageButton;

import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatButton;
import androidx.recyclerview.widget.RecyclerView;

import com.example.filter.R;
import com.example.filter.activities.BaseActivity;
import com.example.filter.adapters.PointHistoryAdapter;
import com.example.filter.etc.ClickUtils;
import com.example.filter.items.PointHistoryItem;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;

public class PointHistoryActivity extends BaseActivity {
    private ImageButton backBtn;
    private AppCompatButton chargeHistoryBtn, buyHistoryBtn;
    private PointHistoryAdapter adapter1;   //충전 내역용
    private PointHistoryAdapter adapter2;   //사용 내역용
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
        history.setAdapter(adapter1);

        backBtn.setOnClickListener(v -> {
            finish();
        });

        chargeHistoryBtn.setOnClickListener(v -> {
            chargeHistoryBtn.setBackgroundResource(R.drawable.btn_select_history_yes);
            chargeHistoryBtn.setTextColor(Color.WHITE);
            buyHistoryBtn.setBackgroundResource(R.drawable.btn_select_history_no);
            buyHistoryBtn.setTextColor(Color.parseColor("#90989F"));

            history.setAdapter(adapter1);
            loadChargeHistory();
        });

        buyHistoryBtn.setOnClickListener(v -> {
            buyHistoryBtn.setBackgroundResource(R.drawable.btn_select_history_yes);
            buyHistoryBtn.setTextColor(Color.WHITE);
            chargeHistoryBtn.setBackgroundResource(R.drawable.btn_select_history_no);
            chargeHistoryBtn.setTextColor(Color.parseColor("#90989F"));

            history.setAdapter(adapter2);
            loadBuyHistory();
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (history.getAdapter() == adapter1) {
            loadChargeHistory();
        } else {
            loadBuyHistory();
        }
    }

    private void loadChargeHistory() {
        SharedPreferences sp = getSharedPreferences("point_history", MODE_PRIVATE);
        String json = sp.getString("history_list", "[]");

        adapter1.items.clear();

        try {
            JSONArray arr = new JSONArray(json);

            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.getJSONObject(i);
                PointHistoryItem item = new PointHistoryItem();

                item.point1 = o.getInt("point1");
                item.point2 = o.getInt("point2");
                item.price = o.getString("price");
                item.date = o.getString("date");
                item.isBuyHistory = false;

                adapter1.items.add(item);
            }
            adapter1.notifyDataSetChanged();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadBuyHistory() {
        SharedPreferences sp = getSharedPreferences("point_buy_history", MODE_PRIVATE);
        String json = sp.getString("buy_history_list", "[]");

        adapter2.items.clear();

        try {
            JSONArray arr = new JSONArray(json);

            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.getJSONObject(i);
                PointHistoryItem item = new PointHistoryItem();

                item.point1 = o.getInt("point1");
                item.point2 = o.getInt("point2");
                item.filterTitle = o.getString("txt");
                item.date = o.getString("date");
                item.isBuyHistory = true;

                adapter2.items.add(item);
            }
            adapter2.notifyDataSetChanged();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
