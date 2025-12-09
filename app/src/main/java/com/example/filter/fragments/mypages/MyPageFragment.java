package com.example.filter.fragments.mypages;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatButton;
import androidx.appcompat.widget.SwitchCompat;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;

import com.example.filter.R;
import com.example.filter.activities.mypage.EditNickNameActivity;
import com.example.filter.activities.mypage.PointChargeActivity;
import com.example.filter.activities.mypage.PointHistoryActivity;
import com.example.filter.activities.mypage.SNSidActivity;
import com.example.filter.activities.mypage.SalesManageActivity;
import com.example.filter.activities.start.StartActivity;
import com.example.filter.api_datas.response_dto.UserMypageResponse;
import com.example.filter.apis.client.AppRetrofitClient;
import com.example.filter.apis.repositories.MyPageApi;
import com.example.filter.dialogs.LogoutDialog;
import com.example.filter.etc.ClickUtils;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MyPageFragment extends Fragment {
    private TextView nickname;
    private TextView currentPoint;
    private AppCompatButton nickEditBtn, logoutBtn, pointChargeBtn, salesManageBtn;
    private ConstraintLayout pointBox, snsId, ask, appInfo, withdraw;
    private SwitchCompat pushToggle;
    private static final float DEFAULT_TEXT_SIZE_SP = 22f;
    private static final float MIN_TEXT_SIZE_SP = 12f;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.f_my_page, container, false);

        nickname = view.findViewById(R.id.nickname);
        nickEditBtn = view.findViewById(R.id.nickEditBtn);
        logoutBtn = view.findViewById(R.id.logoutBtn);
        pointBox = view.findViewById(R.id.pointBox);
        currentPoint = view.findViewById(R.id.currentPoint);
        pointChargeBtn = view.findViewById(R.id.pointChargeBtn);
        salesManageBtn = view.findViewById(R.id.salesManageBtn);
        pushToggle = view.findViewById(R.id.pushToggle);
        snsId = view.findViewById(R.id.snsId);
        ask = view.findViewById(R.id.ask);
        appInfo = view.findViewById(R.id.appInfo);
        withdraw = view.findViewById(R.id.withdraw);

        ClickUtils.clickDim(nickEditBtn);
        ClickUtils.clickDim(logoutBtn);
        ClickUtils.clickDim(pointChargeBtn);
        ClickUtils.clickDim(salesManageBtn);
        ClickUtils.clickDim(snsId);
        ClickUtils.clickDim(ask);
        ClickUtils.clickDim(appInfo);
        ClickUtils.clickDim(withdraw);

        nickEditBtn.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), EditNickNameActivity.class);
            startActivity(intent);
            requireActivity().overridePendingTransition(0, 0);
        });

        logoutBtn.setOnClickListener(v -> {
            showLogoutConfirmDialog();
        });

        pointBox.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), PointHistoryActivity.class);
            startActivity(intent);
            requireActivity().overridePendingTransition(0, 0);
        });

        pointChargeBtn.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), PointChargeActivity.class);
            startActivity(intent);
            requireActivity().overridePendingTransition(0, 0);
        });

        salesManageBtn.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), SalesManageActivity.class);
            startActivity(intent);
            requireActivity().overridePendingTransition(0, 0);
        });

        snsId.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), SNSidActivity.class);
            startActivity(intent);
            requireActivity().overridePendingTransition(0, 0);
        });

        ask.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("message/rfc822");
            intent.setPackage("com.google.android.gm");

            /// 메일 Feelem으로 바꾸기 ///
            intent.putExtra(Intent.EXTRA_EMAIL, new String[]{"usunsun38@gmail.com"});
            intent.putExtra(Intent.EXTRA_SUBJECT, "Feel'em에 문의하기");

            try {
                startActivity(Intent.createChooser(intent, "이메일 보내기"));
            } catch (Exception e) {
                Toast.makeText(requireContext(), "구글메일 앱이 없습니다.", Toast.LENGTH_SHORT).show();
            }
        });

        withdraw.setOnClickListener(v -> {
            Toast.makeText(requireContext(), "전시중이므로 계정 삭제가 불가합니다.", Toast.LENGTH_SHORT).show();
        });

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        nickname.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                adjustNicknameSize();
            }
        });

        // 1. 푸시 알림 설정 로드 (로컬 SharedPreferences)
        SharedPreferences sp = requireContext().getSharedPreferences("mypage", 0);
        boolean savedState = sp.getBoolean("push_toggle_state", true);
        pushToggle.setChecked(savedState);

        pushToggle.setOnCheckedChangeListener((buttonView, isChecked) -> {
            requireContext()
                    .getSharedPreferences("mypage", 0)
                    .edit()
                    .putBoolean("push_toggle_state", isChecked)
                    .apply();
        });

        // 2. 서버에서 마이페이지 정보 로드
        loadMyPageInfo();
    }

    @Override
    public void onResume() {
        super.onResume();
        // 화면이 다시 보일 때마다 최신 정보(포인트 등) 갱신
        loadMyPageInfo();
    }

    // ---------------------------------------------------------------
    // ✅ [서버 통신] 마이페이지 정보 조회 및 UI 갱신
    // ---------------------------------------------------------------
    private void loadMyPageInfo() {
        MyPageApi api = AppRetrofitClient.getInstance(requireContext()).create(MyPageApi.class);

        api.getMyPage().enqueue(new Callback<UserMypageResponse>() {
            @Override
            public void onResponse(Call<UserMypageResponse> call, Response<UserMypageResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    UserMypageResponse data = response.body();

                    // 1. 닉네임 설정
                    if (nickname != null) {
                        nickname.setText(data.nickname);
                    }

                    // 2. 포인트 설정 (천 단위 콤마)
                    if (currentPoint != null) {
                        currentPoint.setText(String.format("%,dP", data.pointAmount));
                    }

                    // 4. 로컬 저장소(points) 동기화
                    // (다른 액티비티에서 포인트를 사용할 때를 대비해 로컬에도 최신값 저장)
                    requireContext().getSharedPreferences("points", 0).edit()
                            .putInt("current_point", data.pointAmount)
                            .apply();

                    Log.d("MyPage", "정보 갱신 완료: " + data.nickname + ", " + data.pointAmount);

                } else {
                    Log.e("MyPage", "정보 조회 실패: " + response.code());
                    // 실패 시 로컬에 저장된 기존 포인트라도 보여주기 (선택 사항)
                    showLocalPointFallback();
                }
            }

            @Override
            public void onFailure(Call<UserMypageResponse> call, Throwable t) {
                Log.e("MyPage", "통신 오류", t);
                Toast.makeText(requireContext(), "서버 연결 실패", Toast.LENGTH_SHORT).show();
                showLocalPointFallback();
            }
        });
    }

    // 서버 통신 실패 시 로컬에 저장된 마지막 포인트를 보여주는 헬퍼 메서드
    private void showLocalPointFallback() {
        SharedPreferences sp2 = requireContext().getSharedPreferences("points", 0);
        int current = sp2.getInt("current_point", 0);
        if (currentPoint != null) {
            currentPoint.setText(String.format("%,dP", current));
        }
    }

    private void adjustNicknameSize() {
        nickname.post(() -> {
            if (nickname.getWidth() == 0) return;

            float currentSize = nickname.getTextSize() / getResources().getDisplayMetrics().scaledDensity;
            int availableWidth = nickname.getWidth();
            float textWidth = nickname.getPaint().measureText(nickname.getText().toString());

            while (textWidth > availableWidth && currentSize > MIN_TEXT_SIZE_SP) {
                currentSize -= 2;
                nickname.setTextSize(currentSize);
                textWidth = nickname.getPaint().measureText(nickname.getText().toString());
            }

            while (textWidth < availableWidth - dp(10) && currentSize < DEFAULT_TEXT_SIZE_SP) {
                currentSize += 2;
                nickname.setTextSize(currentSize);
                textWidth = nickname.getPaint().measureText(nickname.getText().toString());
            }
        });
    }

    private int dp(int v) {
        return Math.round(getResources().getDisplayMetrics().density * v);
    }

    private void showLogoutConfirmDialog() {
        new LogoutDialog(requireContext(), new LogoutDialog.LogoutDialogListener() {
            @Override
            public void onKeep() {
            }

            @Override
            public void onLogout() {
                Intent intent = new Intent(requireContext(), StartActivity.class);
                startActivity(intent);
                requireActivity().overridePendingTransition(0, 0);
                requireActivity().finish();
            }
        }).show();
    }
}