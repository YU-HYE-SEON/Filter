package com.example.filter.activities;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.widget.ImageView;

import androidx.annotation.Nullable;

import com.example.filter.R;
import com.example.filter.etc.ClickUtils;

import java.io.File;

public class SavePhotoActivity extends BaseActivity {
    private ImageView photo;
    private ImageView backToHomeBtn;
    private ImageView registerBtn;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_save_photo);
        photo = findViewById(R.id.photo);
        backToHomeBtn = findViewById(R.id.backToHomeBtn);
        registerBtn = findViewById(R.id.registerBtn);

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

        backToHomeBtn.setOnClickListener(v -> {
            if (ClickUtils.isFastClick(500)) return;

            Intent intent = new Intent(SavePhotoActivity.this, MainActivity.class);
            startActivity(intent);
        });

        registerBtn.setOnClickListener(v -> {
            if (ClickUtils.isFastClick(500)) return;

            Intent intent = new Intent(SavePhotoActivity.this, RegisterActivity.class);
            intent.putExtra("final_image", imagePath);
            startActivity(intent);
        });
    }
}
