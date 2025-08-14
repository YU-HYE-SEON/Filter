package com.example.filter.activities;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Shader;
import android.net.Uri;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.filter.R;
import com.example.filter.adapters.RecommendFilterAdapter;
import com.example.filter.adapters.RecommendStudioAdapter;
import com.example.filter.etc.ClickUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MainActivity extends BaseActivity {
    private ActivityResultLauncher<Intent> galleryLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri photoUri = result.getData().getData();

                    if (photoUri != null) {
                        Intent intent = new Intent(MainActivity.this, FilterActivity.class);
                        intent.setData(photoUri);
                        startActivity(intent);
                    } else {
                        Toast.makeText(this, "사진 선택 실패: URI 없음", Toast.LENGTH_SHORT).show();
                    }
                }
            });
    private ImageButton filter;
    private RecyclerView recommendFilterRecyclerView;
    private RecyclerView recommendStudioRecyclerView;
    private RecommendStudioAdapter studioAdapter;

    private RecyclerView hotFilterRecyclerView;
    private RecyclerView hotStudioRecyclerView;
    private RecommendStudioAdapter studioAdapter2;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        TextView txt2 = findViewById(R.id.txt2);
        txt2.post(() -> {
            float width = txt2.getWidth();
            Shader shader = new LinearGradient(
                    0, 0, width, 0,
                    new int[]{Color.parseColor("#007AFF"), Color.parseColor("#0062CC"), Color.BLACK},
                    new float[]{0f, 0.5f, 1.0f},
                    Shader.TileMode.CLAMP);
            txt2.getPaint().setShader(shader);
            txt2.invalidate();
        });

        TextView txt4 = findViewById(R.id.txt4);
        String fullTxt = "최근 HOT한 스튜디오";
        SpannableString spannable = new SpannableString(fullTxt);
        int start = fullTxt.indexOf("HOT");
        int end = start + "HOT".length();
        spannable.setSpan(new ForegroundColorSpan(Color.parseColor("#FF5C8A")),
                start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        txt4.setText(spannable);

        filter = findViewById(R.id.filter);

        filter.setOnClickListener(v -> {
            if (ClickUtils.isFastClick(500)) return;

            Intent intent = new Intent(Intent.ACTION_PICK);
            intent.setType("image/*");
            galleryLauncher.launch(intent);
        });

        recommendFilterRecyclerView = findViewById(R.id.recommendFilter);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
        recommendFilterRecyclerView.setLayoutManager(layoutManager);
        List<String> filterList = new ArrayList<>();
        filterList.add("Cloud POP");
        filterList.add("Bloom");
        filterList.add("별로");
        filterList.add("푸른 필름 느낌");
        filterList.add("청(淸靑)");
        filterList.add("Zandi");
        RecommendFilterAdapter adapter = new RecommendFilterAdapter(filterList);
        recommendFilterRecyclerView.setAdapter(adapter);

        recommendStudioRecyclerView = findViewById(R.id.recommendStudio);
        GridLayoutManager studioLayoutManager = new GridLayoutManager(this, 3, GridLayoutManager.HORIZONTAL, false);
        recommendStudioRecyclerView.setLayoutManager(studioLayoutManager);
        List<String> studioTitles = Arrays.asList(
                "취향저격 상점", "파랑주의보", "오늘은 이 색깔", "Zan#", "별사탕", "Sticky"
        );
        studioAdapter = new RecommendStudioAdapter(studioTitles);
        recommendStudioRecyclerView.setAdapter(studioAdapter);


        hotFilterRecyclerView = findViewById(R.id.HOTFilter);
        LinearLayoutManager layoutManager2 = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
        hotFilterRecyclerView.setLayoutManager(layoutManager2);
        List<String> filterList2 = new ArrayList<>();
        filterList2.add("햇살 가득 공원");
        filterList2.add("새콤달콤 과일 샐러드");
        filterList2.add("Warm Vintage");
        filterList2.add("오늘의 하늘");
        filterList2.add("고요한 횡단보도");
        filterList2.add("노을 가득 하늘");
        RecommendFilterAdapter adapter2 = new RecommendFilterAdapter(filterList2);
        hotFilterRecyclerView.setAdapter(adapter2);

        hotStudioRecyclerView = findViewById(R.id.HOTStudio);
        GridLayoutManager studioLayoutManager2 = new GridLayoutManager(this, 3, GridLayoutManager.HORIZONTAL, false);
        hotStudioRecyclerView.setLayoutManager(studioLayoutManager2);
        List<String> studioTitles2 = Arrays.asList(
                "별사탕", "코랄선셋", "오늘의 감정관", "슈가코팅", "SOON", "레트로콜라"
        );
        studioAdapter2 = new RecommendStudioAdapter(studioTitles2);
        hotStudioRecyclerView.setAdapter(studioAdapter2);
    }
}
