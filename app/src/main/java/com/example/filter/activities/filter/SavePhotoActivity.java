package com.example.filter.activities.filter;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.ImageView;

import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatButton;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.filter.R;
import com.example.filter.activities.BaseActivity;
import com.example.filter.activities.MainActivity;
import com.example.filter.etc.ClickUtils;
import com.example.filter.api_datas.FilterCreationData; // ✅ Import 필수
import com.example.filter.etc.ImageUtils;

import java.io.File;

public class SavePhotoActivity extends BaseActivity {
    private ImageButton backBtn;
    private ImageView photo;
    private ConstraintLayout bottomArea;
    private AppCompatButton toArchiveBtn, toRegisterBtn;

    // ✅ 데이터를 담을 객체 선언
    private FilterCreationData filterData;
    private String displayImagePath; // 화면 표시용 이미지 경로

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.a_save_photo);

        backBtn = findViewById(R.id.backBtn);
        photo = findViewById(R.id.photo);
        bottomArea = findViewById(R.id.bottomArea);
        toArchiveBtn = findViewById(R.id.toArchiveBtn);
        toRegisterBtn = findViewById(R.id.toRegisterBtn);

        // 시스템 바 인셋 처리 (기존 코드 유지)
        ViewCompat.setOnApplyWindowInsetsListener(bottomArea, (v, insets) -> {
            Insets nav = insets.getInsets(WindowInsetsCompat.Type.navigationBars());
            ConstraintLayout.LayoutParams lp = (ConstraintLayout.LayoutParams) v.getLayoutParams();
            lp.bottomMargin = 0;
            v.setLayoutParams(lp);
            v.setPadding(v.getPaddingLeft(), v.getPaddingTop(), v.getPaddingRight(), nav.bottom);
            return insets;
        });

        // ---------------------------------------------------------------
        // ✅ [핵심 수정] 1. FilterActivity에서 보낸 데이터 받기
        // ---------------------------------------------------------------
        // 개별 데이터(String, int...)를 받는 대신 객체 하나를 통째로 받습니다.
        filterData = getIntent().getParcelableExtra("filter_data");

        // 화면에 보여줄 최종 이미지 경로는 별도로 받음 (로딩 속도 최적화)
        // (만약 FilterCreationData 안에 editedImageUrl에 로컬 경로가 있다면 그걸 써도 됩니다)
        displayImagePath = getIntent().getStringExtra("display_image_path");
        if (displayImagePath == null && filterData != null) {
            // 혹시 display_image_path가 안 넘어왔다면 editedImageUrl 사용
            displayImagePath = filterData.editedImageUrl;
        }

        boolean allow = getIntent().getBooleanExtra("allowRegister", true);

        // ---------------------------------------------------------------
        // ✅ UI 설정
        // ---------------------------------------------------------------
        if (!allow) {
            toRegisterBtn.setEnabled(false);
            toRegisterBtn.getBackground().setColorFilter(Color.parseColor("#759749"), PorterDuff.Mode.SRC_ATOP);
            toRegisterBtn.setTextColor(Color.parseColor("#00499A"));
            toRegisterBtn.setClickable(false);
        }

        // 이미지 표시
        if (displayImagePath != null) {
            // 로컬 파일인지 확인 (S3 URL일 경우 Glide를 써야 하지만,
            // 여기서는 FilterActivity가 로컬 경로를 넘겨준다고 가정)
            File file = new File(displayImagePath);
            if (file.exists()) {
                Bitmap bitmap = BitmapFactory.decodeFile(displayImagePath);
                if (bitmap != null) {
                    photo.setImageBitmap(bitmap);
                }
            } else if (displayImagePath.startsWith("http")) {
                // 만약 S3 URL이라면 Glide 사용 (build.gradle에 Glide가 있어야 함)
                com.bumptech.glide.Glide.with(this).load(displayImagePath).into(photo);
            }
        }

        // ---------------------------------------------------------------
        // ✅ 버튼 리스너 설정
        // ---------------------------------------------------------------
        backBtn.setOnClickListener(v -> {
            if (ClickUtils.isFastClick(v, 400)) return;
            Intent intent = new Intent(SavePhotoActivity.this, MainActivity.class);
            startActivity(intent);
            finish();
        });

        ClickUtils.clickDim(toArchiveBtn);
        toArchiveBtn.setOnClickListener(v -> {
            if (ClickUtils.isFastClick(v, 400)) return;
            // 단순히 종료하면 Archive(갤러리/보관함)로 간 것으로 간주
            finish();
        });

        ClickUtils.clickDim(toRegisterBtn);

        // ★ [핵심 수정] 2. RegisterActivity로 데이터 넘기기
        toRegisterBtn.setOnClickListener(v -> {
            if (ClickUtils.isFastClick(v, 400)) return;
            ClickUtils.disableTemporarily(v, 800);

            if (filterData == null) return; // 데이터가 없으면 진행 불가

            Intent intent = new Intent(SavePhotoActivity.this, RegisterActivity.class);

            // 받은 객체를 그대로 다시 넘깁니다. (Toss)
            intent.putExtra("filter_data", filterData);

            // 화면 표시용 경로도 편의상 같이 넘김
            intent.putExtra("display_image_path", displayImagePath);

            startActivity(intent);
        });
    }
}