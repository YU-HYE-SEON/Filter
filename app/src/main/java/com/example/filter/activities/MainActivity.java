package com.example.filter.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;

import com.example.filter.R;
import com.example.filter.etc.ClickUtils;

public class MainActivity extends BaseActivity {
    private ImageButton filter;
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

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.a_main);
        filter = findViewById(R.id.filter);

        filter.setOnClickListener(v -> {
            if (ClickUtils.isFastClick(500)) return;

            Intent intent = new Intent(Intent.ACTION_PICK);
            intent.setType("image/*");
            galleryLauncher.launch(intent);
        });
    }
}
