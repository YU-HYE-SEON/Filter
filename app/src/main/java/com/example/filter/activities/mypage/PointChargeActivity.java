package com.example.filter.activities.mypage;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatButton;

import com.example.filter.R;
import com.example.filter.activities.BaseActivity;
import com.example.filter.dialogs.ChargeDialog;
import com.example.filter.etc.ClickUtils;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class PointChargeActivity extends BaseActivity {
    private ImageButton backBtn;
    private ImageButton[] pointButtons;
    private ImageButton p100, p300, p500, p1000, p3000, p5000;
    private TextView[][] pointTexts;
    private TextView currentPoint, p100Txt1, p100Txt2, p300Txt1, p300Txt2, p500Txt1, p500Txt2, p1000Txt1, p1000Txt2, p3000Txt1, p3000Txt2, p5000Txt1, p5000Txt2;
    private AppCompatButton buyPointBtn;
    private boolean isSuccess = true;   // 충전 실패 경우 추가해야 함
    private int selectPoint = 0;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.a_point_charge);
        backBtn = findViewById(R.id.backBtn);
        currentPoint = findViewById(R.id.currentPoint);
        p100 = findViewById(R.id.p100);
        p300 = findViewById(R.id.p300);
        p500 = findViewById(R.id.p500);
        p1000 = findViewById(R.id.p1000);
        p3000 = findViewById(R.id.p3000);
        p5000 = findViewById(R.id.p5000);
        p100Txt1 = findViewById(R.id.p100Txt1);
        p100Txt2 = findViewById(R.id.p100Txt2);
        p300Txt1 = findViewById(R.id.p300Txt1);
        p300Txt2 = findViewById(R.id.p300Txt2);
        p500Txt1 = findViewById(R.id.p500Txt1);
        p500Txt2 = findViewById(R.id.p500Txt2);
        p1000Txt1 = findViewById(R.id.p1000Txt1);
        p1000Txt2 = findViewById(R.id.p1000Txt2);
        p3000Txt1 = findViewById(R.id.p3000Txt1);
        p3000Txt2 = findViewById(R.id.p3000Txt2);
        p5000Txt1 = findViewById(R.id.p5000Txt1);
        p5000Txt2 = findViewById(R.id.p5000Txt2);
        buyPointBtn = findViewById(R.id.buyPointBtn);

        pointButtons = new ImageButton[]{p100, p300, p500, p1000, p3000, p5000};
        pointTexts = new TextView[][]{{p100Txt1, p100Txt2}, {p300Txt1, p300Txt2}, {p500Txt1, p500Txt2}, {p1000Txt1, p1000Txt2}, {p3000Txt1, p3000Txt2}, {p5000Txt1, p5000Txt2}};

        buyPointBtn.setEnabled(false);

        backBtn.setOnClickListener(v -> {
            finish();
        });

        p100.setOnClickListener(v -> {
            selectPoint(0);
        });
        p300.setOnClickListener(v -> {
            selectPoint(1);
        });
        p500.setOnClickListener(v -> {
            selectPoint(2);
        });
        p1000.setOnClickListener(v -> {
            selectPoint(3);
        });
        p3000.setOnClickListener(v -> {
            selectPoint(4);
        });
        p5000.setOnClickListener(v -> {
            selectPoint(5);
        });

        ClickUtils.clickDim(buyPointBtn);
        buyPointBtn.setOnClickListener(v -> {
            showChargeConfirmDialog();
        });

        //포인트 0으로 초기화
        //getSharedPreferences("point_history", MODE_PRIVATE).edit().clear().apply();
        //getSharedPreferences("point_buy_history", MODE_PRIVATE).edit().clear().apply();
        //getSharedPreferences("points", MODE_PRIVATE).edit().clear().apply();
    }

    private void selectPoint(int index) {
        int points[] = {100, 300, 500, 1000, 3000, 5000};
        selectPoint = points[index];

        for (int i = 0; i < pointButtons.length; i++) {
            pointButtons[i].setBackgroundResource(R.drawable.btn_select_point_no);
            pointTexts[i][0].setTextColor(Color.parseColor("#90989F"));
            pointTexts[i][1].setTextColor(Color.parseColor("#90989F"));
        }

        pointButtons[index].setBackgroundResource(R.drawable.btn_select_point_yes);
        pointTexts[index][0].setTextColor(Color.WHITE);
        pointTexts[index][1].setTextColor(Color.WHITE);

        buyPointBtn.setEnabled(true);
        buyPointBtn.setBackgroundResource(R.drawable.btn_buy_point_yes);
    }

    private void addPoint(int point) {
        SharedPreferences sp = getSharedPreferences("points", MODE_PRIVATE);
        int current = sp.getInt("current_point", 0);
        sp.edit().putInt("before_point", current).apply();
        sp.edit().putInt("current_point", current + point).apply();
    }

    private void updateCurrentPoint() {
        SharedPreferences sp = getSharedPreferences("points", MODE_PRIVATE);
        int current = sp.getInt("current_point", 0);
        currentPoint.setText(String.format("%,dP", current));
    }

    private int getBeforePoint() {
        SharedPreferences sp = getSharedPreferences("points", MODE_PRIVATE);
        return sp.getInt("before_point", 0);
    }

    private int getIndexFromPoint(int p) {
        if (p == 100) return 0;
        if (p == 300) return 1;
        if (p == 500) return 2;
        if (p == 1000) return 3;
        if (p == 3000) return 4;
        return 5;
    }

    private String getSelectedPriceText() {
        TextView[] priceTexts = {p100Txt2, p300Txt2, p500Txt2, p1000Txt2, p3000Txt2, p5000Txt2};
        String raw = priceTexts[getIndexFromPoint(selectPoint)].getText().toString();
        return raw.replace("원", "");
    }

    private String getCurrentDate() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy.MM.dd HH:mm", Locale.getDefault());
        return sdf.format(new Date());
    }

    private void saveHistory(int pointAdded) {
        SharedPreferences sp = getSharedPreferences("point_history", MODE_PRIVATE);
        String json = sp.getString("history_list", "[]");

        try {
            JSONArray oldArray = new JSONArray(json);
            JSONArray newArray = new JSONArray();
            JSONObject obj = new JSONObject();

            obj.put("point1", pointAdded);
            obj.put("point2", getBeforePoint());
            obj.put("price", getSelectedPriceText());
            obj.put("date", getCurrentDate());

            newArray.put(obj);
            for (int i = 0; i < oldArray.length(); i++) {
                newArray.put(oldArray.getJSONObject(i));
            }

            sp.edit().putString("history_list", newArray.toString()).apply();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public void showChargeConfirmDialog() {
        ChargeDialog dialog = new ChargeDialog(this, () -> {
            if (isSuccess) {
                addPoint(selectPoint);
                saveHistory(selectPoint);
                finish();
            }
        });
        dialog.show();
        dialog.setMessage(isSuccess);
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateCurrentPoint();
    }
}
