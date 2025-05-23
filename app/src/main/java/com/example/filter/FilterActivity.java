package com.example.filter;

import android.os.Bundle;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.HorizontalScrollView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;

public class FilterActivity extends AppCompatActivity {
    private CustomSeekbar customSeekBar;    //custom seekbar 자체
    private HorizontalScrollView scrollView;    //자르기, 밝기 등 아이콘 스크롤 뷰
    private LinearLayout seekbar;   //custom seekbar가 있는 레이아웃 전체
    private ImageView brightness;   //밝기 아이콘
    private Animation slideDownAnim;    //레이아웃 내려가는 애니메이션
    private Animation slideUpAnim;  //레이아웃 올라오는 애니메이션
    private ImageButton closeButton2;   //custom seekbar 닫기 버튼

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        scrollView = findViewById(R.id.scrollView);
        seekbar = findViewById(R.id.seekbar);
        brightness = findViewById(R.id.brightness);
        closeButton2 = findViewById(R.id.closeButton2);

        slideDownAnim = AnimationUtils.loadAnimation(this, R.anim.slide_down);
        slideUpAnim = AnimationUtils.loadAnimation(this, R.anim.slide_up);

        brightness.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //자르기, 밝기 등 아이콘 스크롤 뷰 보이는 상태라면
                if (scrollView.getVisibility() == View.VISIBLE) {
                    //스크롤 뷰 내려가는 애니메이션 + 숨김 처리
                    scrollView.startAnimation(slideDownAnim);
                    scrollView.setVisibility(View.GONE);

                    //custom seekbar 레이아웃 보이도록 설정 + 올라오는 애니메이션
                    seekbar.setVisibility(View.VISIBLE);
                    seekbar.startAnimation(slideUpAnim);
                }
            }
        });


        closeButton2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //자르기, 밝기 등 아이콘 스크롤 뷰 보이는 상태라면
                if (seekbar.getVisibility() == View.VISIBLE) {
                    //스크롤 뷰 내려가는 애니메이션 + 숨김 처리
                    seekbar.startAnimation(slideDownAnim);
                    seekbar.setVisibility(View.GONE);

                    //custom seekbar 레이아웃 보이도록 설정 + 올라오는 애니메이션
                    scrollView.setVisibility(View.VISIBLE);
                    scrollView.startAnimation(slideUpAnim);
                }
            }
        });

        customSeekBar = findViewById(R.id.customSeekbar);
        customSeekBar.setProgress(0);   //custom seekbar 현재값 0으로 설정

    }
}