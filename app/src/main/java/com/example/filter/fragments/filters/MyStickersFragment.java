package com.example.filter.fragments.filters;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatButton;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.filter.R;
import com.example.filter.adapters.MyStickersAdapter;
import com.example.filter.apis.client.AppRetrofitClient; // ✅ 내 클라이언트 Import
import com.example.filter.apis.dto.StickerResponseDto;
import com.example.filter.apis.service.StickerApi;
import com.example.filter.dialogs.StickerDeleteDialog;
import com.example.filter.etc.ClickUtils;
import com.example.filter.etc.Controller;
import com.example.filter.etc.StickerStore;
import com.example.filter.etc.StickerViewModel;
import com.example.filter.items.StickerItem;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MyStickersFragment extends Fragment {
    private AppCompatButton saveBtn;
    private ImageButton cancelBtn, checkBtn, deleteStickerIcon;
    private MyStickersAdapter adapter;
    private RecyclerView myStickers;
    private FrameLayout stickerOverlay;
    private View selectSticker = null;
    private int selectStickerId = RecyclerView.NO_POSITION;
    private LayoutInflater inflater;

    private View stickerFrame;
    private ImageView stickerImage, moveController, rotateController, sizeController, deleteController;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.f_my_stickers, container, false);
        this.inflater = inflater;

        stickerOverlay = requireActivity().findViewById(R.id.stickerOverlay);
        myStickers = view.findViewById(R.id.myStickers);
        deleteStickerIcon = view.findViewById(R.id.deleteStickerIcon);
        cancelBtn = view.findViewById(R.id.cancelBtn);
        checkBtn = view.findViewById(R.id.checkBtn);

        checkBtn.setEnabled(false);
        checkBtn.setAlpha(0.4f);

        deleteStickerIcon.setEnabled(false);
        deleteStickerIcon.setAlpha(0.4f);

        StickerStore.get().init(requireContext().getApplicationContext());

        LinearLayoutManager lm = new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false);
        myStickers.setLayoutManager(lm);
        adapter = new MyStickersAdapter(new ArrayList<>());
        myStickers.setAdapter(adapter);
        myStickers.setItemAnimator(null);

        adapter.setOnStickerClickListener((position, item) -> {
            selectStickerId = position;
            deleteStickerIcon.setEnabled(true);
            deleteStickerIcon.setAlpha(1.0f);
            showStickerCentered(item.getImageUrl(), item.getId());
        });

        for (int i = 0; i < stickerOverlay.getChildCount(); i++) {
            View child = stickerOverlay.getChildAt(i);
            Controller.setStickerActive(child, false);
        }

        setupBottomButtons();

        // 서버에서 로드
        loadStickersFromServer();

        return view;
    }

    // ---------------------------------------------------------------
    // ✅ [수정됨] AppRetrofitClient를 사용하여 서버 호출
    // ---------------------------------------------------------------
    private void loadStickersFromServer() {
        // 1. 내가 만든 클라이언트로 API 생성 (토큰 처리는 내부에서 자동 해결)
        StickerApi api = AppRetrofitClient.getInstance(requireContext()).create(StickerApi.class);

        // 2. 호출 (파라미터 없이 호출 가능!)
        api.getMyStickers().enqueue(new Callback<List<StickerResponseDto>>() {
            @Override
            public void onResponse(Call<List<StickerResponseDto>> call, Response<List<StickerResponseDto>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<StickerResponseDto> dtos = response.body();
                    List<StickerItem> items = new ArrayList<>();

                    for (StickerResponseDto dto : dtos) {
                        if (dto.getImageUrl() != null) {
                            // DTO -> StickerItem 변환
                            items.add(StickerItem.fromServer(
                                    dto.getId(),
                                    dto.getImageUrl(),
                                    dto.getType()
                            ));
                        }
                    }
                    adapter.updateData(items);
                } else {
                    Log.e("StickerAPI", "목록 조회 실패: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<List<StickerResponseDto>> call, Throwable t) {
                Log.e("StickerAPI", "통신 오류", t);
            }
        });
    }

    private void showStickerCentered(String stickerUrl, long stickerId) {
        Controller.clearCurrentSticker(stickerOverlay, selectSticker);

        stickerFrame = inflater.inflate(R.layout.v_sticker_edit, stickerOverlay, false);
        stickerImage = stickerFrame.findViewById(R.id.stickerImage);
        moveController = stickerFrame.findViewById(R.id.moveController);
        rotateController = stickerFrame.findViewById(R.id.rotateController);
        sizeController = stickerFrame.findViewById(R.id.sizeController);
        deleteController = stickerFrame.findViewById(R.id.deleteController);

        rotateController.setVisibility(View.INVISIBLE);
        sizeController.setVisibility(View.INVISIBLE);

        // Glide 로드 (로딩/에러 이미지 제거하여 깔끔하게)
        Glide.with(this)
                .load(stickerUrl)
                .into(stickerImage);

        int sizePx = Controller.dp(230, getResources());
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(sizePx, sizePx);
        stickerFrame.setLayoutParams(lp);

        stickerOverlay.post(() -> {
            stickerFrame.setPivotX(sizePx / 2f);
            stickerFrame.setPivotY(sizePx / 2f);

            float cx = (stickerOverlay.getWidth() - sizePx) / 2f;
            float cy = (stickerOverlay.getHeight() - sizePx) / 2f;

            stickerFrame.setX(cx);
            stickerFrame.setY(cy);

            // DB ID 저장
            stickerFrame.setTag(R.id.tag_sticker_db_id, stickerId);

            stickerOverlay.addView(stickerFrame);

            this.selectSticker = stickerFrame;
            Controller.setStickerActive(stickerFrame, true);
            stickerFrame.setOnClickListener(v -> moveEditSticker(stickerFrame));
        });

        deleteController.setOnClickListener(x -> {
            Controller.removeStickerFrame(stickerFrame);
            Controller.setControllersVisible(stickerFrame, false);
            adapter.clearSelection();
            deleteStickerIcon.setEnabled(false);
            deleteStickerIcon.setAlpha(0.4f);
        });

        stickerFrame.bringToFront();
    }

    private void setupBottomButtons() {
        deleteStickerIcon.setOnClickListener(v -> {
            if (ClickUtils.isFastClick(v, 400)) return;
            if (!deleteStickerIcon.isEnabled()) return;
            confirmDeleteSticker();
        });

        cancelBtn.setOnClickListener(v -> {
            if (ClickUtils.isFastClick(v, 400)) return;
            Controller.removeStickerFrame(stickerFrame);
            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .setCustomAnimations(R.anim.slide_up, 0)
                    .replace(R.id.bottomArea2, new StickersFragment())
                    .commit();
        });

        checkBtn.setOnClickListener(v -> {
            if (ClickUtils.isFastClick(v, 400)) return;
            Controller.removeStickerFrame(stickerFrame);
            Controller.setControllersVisible(stickerFrame, false);
            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .setCustomAnimations(R.anim.slide_up, 0)
                    .replace(R.id.bottomArea2, new StickersFragment())
                    .commit();
        });
    }

    private void moveEditSticker(View stickerFrame) {
        if (rotateController != null) rotateController.setVisibility(View.VISIBLE);
        if (sizeController != null) sizeController.setVisibility(View.VISIBLE);

        for (int i = 0; i < stickerOverlay.getChildCount(); i++) {
            View child = stickerOverlay.getChildAt(i);
            Controller.setStickerActive(child, child == stickerFrame);
        }

        EditStickerFragment editStickerFragment = new EditStickerFragment();
        StickerViewModel viewModel = new ViewModelProvider(requireActivity()).get(StickerViewModel.class);

        int currentId = EditStickerFragment.stickerId + 1;
        viewModel.setTempView(currentId, stickerFrame);

        Bundle args = new Bundle();
        args.putString("prev_frag", "myStickerF");
        editStickerFragment.setArguments(args);

        requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .setCustomAnimations(R.anim.slide_up, 0)
                .replace(R.id.bottomArea2, editStickerFragment)
                .commit();
    }

    private void confirmDeleteSticker() {
        if (selectStickerId == RecyclerView.NO_POSITION) return;

        new StickerDeleteDialog(requireContext(), new StickerDeleteDialog.StickerDeleteDialogListener() {
            @Override
            public void onKeep() { }

            @Override
            public void onDelete() {
                if (selectStickerId != RecyclerView.NO_POSITION) {
                    Controller.clearCurrentSticker(stickerOverlay, selectSticker);
                    adapter.removeAt(selectStickerId);
                    selectStickerId = RecyclerView.NO_POSITION;
                    deleteStickerIcon.setEnabled(false);
                    deleteStickerIcon.setAlpha(0.4f);
                }
            }
        })
                .withMessage("내 스티커에서 정말로 삭제하시겠습니까?")
                .withButton1Text("예")
                .withButton2Text("아니오")
                .show();
    }

    @Override
    public void onResume() {
        super.onResume();
        saveBtn = requireActivity().findViewById(R.id.saveBtn);
        if (saveBtn != null) {
            saveBtn.setEnabled(false);
            saveBtn.setAlpha(0.4f);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (stickerOverlay != null) {
            for (int i = 0; i < stickerOverlay.getChildCount(); i++) {
                View child = stickerOverlay.getChildAt(i);
                child.setOnClickListener(null);
            }
        }
    }
}