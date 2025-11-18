package com.example.filter.fragments.mypages;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatButton;
import androidx.appcompat.widget.SwitchCompat;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;

import com.example.filter.R;
import com.example.filter.activities.mypage.PointChargeActivity;
import com.example.filter.activities.mypage.PointHistoryActivity;
import com.example.filter.etc.ClickUtils;

public class MyPageFragment extends Fragment {
    private TextView nickname, id, currentPoint;
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
        id = view.findViewById(R.id.id);
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
        ClickUtils.clickDim(salesManageBtn);
        ClickUtils.clickDim(ask);
        ClickUtils.clickDim(appInfo);
        ClickUtils.clickDim(withdraw);
        ClickUtils.clickDim(snsId);

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

        SharedPreferences sp = requireContext().getSharedPreferences("mypage", 0);
        boolean savedState = sp.getBoolean("push_toggle_state", true);
        pushToggle.setChecked(savedState);

        SharedPreferences sp2 = requireContext().getSharedPreferences("points", 0);
        int current = sp2.getInt("current_point", 0);
        currentPoint.setText(current + "P");

        pushToggle.setOnCheckedChangeListener((buttonView, isChecked) -> {
            requireContext()
                    .getSharedPreferences("mypage", 0)
                    .edit()
                    .putBoolean("push_toggle_state", isChecked)
                    .apply();
        });
    }

    @Override
    public void onResume() {
        super.onResume();

        SharedPreferences sp = requireContext().getSharedPreferences("points", 0);
        int current = sp.getInt("current_point", 0);
        currentPoint.setText(current + "P");
    }
}
