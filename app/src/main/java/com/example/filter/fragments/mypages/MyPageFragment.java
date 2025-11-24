package com.example.filter.fragments.mypages;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatButton;
import androidx.appcompat.widget.SwitchCompat;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;

import com.example.filter.R;
import com.example.filter.activities.mypage.PointChargeActivity;
import com.example.filter.activities.mypage.PointHistoryActivity;
import com.example.filter.api_datas.response_dto.UserMypageResponse;
import com.example.filter.apis.client.AppRetrofitClient;
import com.example.filter.apis.repositories.MyPageApi;
import com.example.filter.etc.ClickUtils;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MyPageFragment extends Fragment {
    private TextView nickname;
    private TextView currentPoint;
    private AppCompatButton nickEditBtn, logoutBtn, pointChargeBtn, salesManageBtn;
    private ConstraintLayout pointBox, ask, appInfo, withdraw, snsId;
    private SwitchCompat pushToggle;

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
        ask = view.findViewById(R.id.ask);
        appInfo = view.findViewById(R.id.appInfo);
        withdraw = view.findViewById(R.id.withdraw);
        snsId = view.findViewById(R.id.snsId);
        pushToggle = view.findViewById(R.id.pushToggle);

        ClickUtils.clickDim(nickEditBtn);
        ClickUtils.clickDim(logoutBtn);
        ClickUtils.clickDim(pointChargeBtn);

        // 필요한 경우 다른 버튼에도 클릭 효과 적용
        // ClickUtils.clickDim(salesManageBtn);
        // ClickUtils.clickDim(ask);

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

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

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
}