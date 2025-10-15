package com.example.filter.activities;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.ImageView;

import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.filter.R;
import com.example.filter.etc.ClickUtils;

import java.io.File;

public class SavePhotoActivity extends BaseActivity {
    private ImageButton backBtn;
    private ImageView photo;
    private ConstraintLayout bottomArea;
    private ImageView backToHomeBtn, registerBtn;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.a_save_photo);
        backBtn = findViewById(R.id.backBtn);
        photo = findViewById(R.id.photo);
        bottomArea = findViewById(R.id.bottomArea);
        backToHomeBtn = findViewById(R.id.backToHomeBtn);
        registerBtn = findViewById(R.id.registerBtn);

        ViewCompat.setOnApplyWindowInsetsListener(bottomArea, (v, insets) -> {
            Insets nav = insets.getInsets(WindowInsetsCompat.Type.navigationBars());
            ConstraintLayout.LayoutParams lp = (ConstraintLayout.LayoutParams) v.getLayoutParams();
            lp.bottomMargin = 0;
            v.setLayoutParams(lp);
            v.setPadding(v.getPaddingLeft(), v.getPaddingTop(), v.getPaddingRight(), nav.bottom);
            return insets;
        });

        String imagePath = getIntent().getStringExtra("saved_image");
        if (imagePath != null) {
            File file = new File(imagePath);
            if (file.exists()) {
                Bitmap bitmap = BitmapFactory.decodeFile(imagePath);
                if (bitmap != null) {
                    photo.setImageBitmap(bitmap);
                    //사진 저장 메서드 호출
                    //ImageUtils.saveBitmapToGallery(SavePhotoActivity.this, bitmap);
                }
            }
        }

        backBtn.setOnClickListener(v -> {
            if (ClickUtils.isFastClick(500)) return;
            finish();
        });

        backToHomeBtn.setOnClickListener(v -> {
            if (ClickUtils.isFastClick(500)) return;

            Intent intent = new Intent(SavePhotoActivity.this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);

            finish();
        });

        registerBtn.setOnClickListener(v -> {
            if (ClickUtils.isFastClick(500)) return;

            Intent intent = new Intent(SavePhotoActivity.this, RegisterActivity.class);
            intent.putExtra("final_image", imagePath);
            startActivity(intent);
        });
    }
}
