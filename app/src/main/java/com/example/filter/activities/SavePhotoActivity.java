package com.example.filter.activities;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.filter.R;
import com.example.filter.etc.ClickUtils;
import com.example.filter.etc.ImageUtils;

import java.io.IOException;
import java.io.OutputStream;


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
            Bitmap bitmap = BitmapFactory.decodeFile(imagePath);
            if (bitmap != null) {
                photo.setImageBitmap(bitmap);
                ImageUtils.saveBitmapToGallery(SavePhotoActivity.this, bitmap);
            }
        }

        backToHomeBtn.setOnClickListener(v -> {
            if (ClickUtils.isFastClick(500)) return;

            Intent intent = new Intent(SavePhotoActivity.this, MainActivity.class);
            startActivity(intent);
        });

        registerBtn.setOnClickListener(v -> {
            if (ClickUtils.isFastClick(500)) return;

            Intent intent=new Intent(SavePhotoActivity.this, RegisterActivity.class);
            intent.putExtra("final_image", imagePath);
            startActivity(intent);
        });
    }
}
