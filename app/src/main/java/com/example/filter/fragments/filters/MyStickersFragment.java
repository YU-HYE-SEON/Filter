package com.example.filter.fragments.filters;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatButton;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.view.ViewCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.filter.R;
import com.example.filter.activities.filter.FilterActivity;
import com.example.filter.adapters.MyStickersAdapter;
import com.example.filter.api_datas.FaceStickerData;
import com.example.filter.apis.client.AppRetrofitClient;
import com.example.filter.api_datas.response_dto.StickerResponseDto;
import com.example.filter.apis.repositories.StickerRepository;
import com.example.filter.apis.StickerApi;
import com.example.filter.dialogs.StickerDeleteDialog;
import com.example.filter.etc.ClickUtils;
import com.example.filter.etc.Controller;
import com.example.filter.etc.FaceDetect;
import com.example.filter.etc.StickerMeta;
import com.example.filter.etc.StickerStore;
import com.example.filter.etc.StickerViewModel;
import com.example.filter.items.StickerItem;
import com.example.filter.overlayviews.FaceBoxOverlayView;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MyStickersFragment extends Fragment {
    private ConstraintLayout topArea;
    private FrameLayout photoContainer;
    private boolean isToastVisible = false;
    private FaceBoxOverlayView faceBox;
    private View editingSticker = null;
    private String stickerUrl;
    private Float prevElevation = null;

    //private float prevElevation;
    private ConstraintLayout bottomArea1;
    private LinearLayout stickerEdit;
    private CheckBox faceCheckBox;
    private AppCompatButton saveBtn;
    private ImageButton cancelBtn, checkBtn, deleteStickerIcon;
    private MyStickersAdapter adapter;
    private RecyclerView myStickers;
    private FrameLayout stickerOverlay;
    private View selectSticker = null;
    private int selectStickerId = RecyclerView.NO_POSITION;
    private LayoutInflater inflater;

    private View stickerFrame;
    private ImageView stickerImage, deleteController;

    private int pendingUploadCount = 0;

    // ✅ 업로드 완료 리스너 정의 (콜백 구현체)
    public interface StickerUploadListener {
        void onUploadFinished();
    }

    // 업로드 완료 시 호출될 리스너 구현
    private final StickerUploadListener uploadListener = () -> {
        synchronized (this) {
            pendingUploadCount--;
            Log.d("StickerUpload", "업로드 완료 카운트: " + pendingUploadCount);

            // 모든 업로드가 완료되면 목록 조회 시작
            if (pendingUploadCount <= 0) {
                Log.d("StickerUpload", "✅ 모든 업로드 완료. 서버 목록 조회 시작.");
                requireActivity().runOnUiThread(this::loadStickersFromServer);
            }
        }
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.f_my_stickers, container, false);
        this.inflater = inflater;

        myStickers = view.findViewById(R.id.myStickers);
        deleteStickerIcon = view.findViewById(R.id.deleteStickerIcon);
        cancelBtn = view.findViewById(R.id.cancelBtn);
        checkBtn = view.findViewById(R.id.checkBtn);

        FilterActivity activity = (FilterActivity) requireActivity();

        topArea = activity.findViewById(R.id.topArea);
        photoContainer = activity.findViewById(R.id.photoContainer);
        stickerOverlay = activity.findViewById(R.id.stickerOverlay);
        faceCheckBox = activity.findViewById(R.id.faceCheckBox);
        stickerEdit = activity.findViewById(R.id.stickerEdit);
        bottomArea1 = activity.findViewById(R.id.bottomArea1);

        if (bottomArea1 != null) {
            stickerEdit.setVisibility(View.VISIBLE);
            stickerEdit.setAlpha(0.4f);
            faceCheckBox.setEnabled(false);
            setCheckboxSize(25f, 3f);
        }

        Bundle args = getArguments();
        boolean fromFace = args != null && args.getBoolean("IS_FACE", false);
        faceBox = new FaceBoxOverlayView(requireContext());
        photoContainer.addView(faceBox, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        activity.getPhotoPreview().queueEvent(() -> {
            Bitmap bmp = activity.getRenderer().getCurrentBitmap();
            activity.runOnUiThread(() -> FaceDetect.detectFaces(bmp, faceBox, (faces, bitmap) -> {
                if (faces.isEmpty()) {
                    if (fromFace) {
                        showToast("얼굴을 감지하지 못했습니다");
                    }
                    return;
                }

                if (!faces.isEmpty() && args != null) {
                    StickerViewModel viewModel = new ViewModelProvider(requireActivity()).get(StickerViewModel.class);
                    int groupId = EditStickerFragment.stickerId;
                    //View stickerFrame = viewModel.getTempView(groupId);
                    String stickerPath = args.getString("stickerUrl");

                    // ✅ [추가] 인자에서 서버 DB ID 가져오기 (없으면 -1)
                    // (이전 프래그먼트에서 "sticker_db_id"라는 키로 넘겨줘야 함)
                    long serverId = args.getLong("sticker_db_id", -1L);

                    StickerMeta meta = new StickerMeta(
                            args.getFloat("relX"),
                            args.getFloat("relY"),
                            args.getFloat("relW"),
                            args.getFloat("relH"),
                            args.getFloat("rot")
                    );

                    List<float[]> placement = StickerMeta.recalculate(faces, bitmap, stickerOverlay, meta, requireContext());
                    requireActivity().runOnUiThread(() -> {
                        //viewModel.removeCloneGroup(groupId, stickerOverlay);
                        //viewModel.setFaceStickerDataToDelete(groupId);

                        for (float[] p : placement) {
                            View cloneSticker = StickerMeta.faceSticker(stickerOverlay, stickerPath, requireContext(), p);
                            if (cloneSticker != null) {

                                // ✅ [핵심] 뷰에 서버 DB ID 태그 저장
                                if (serverId != -1L) {
                                    cloneSticker.setTag(R.id.tag_sticker_db_id, serverId);
                                }

                                viewModel.addCloneGroup(groupId, cloneSticker);
                                updateCheckButtonState();
                                //moveEditSticker(cloneSticker);
                                ((FilterActivity) getActivity()).updateSaveButtonState();
                            }
                        }

                        showToast("얼굴 인식 성공");

                        /*ImageView stickerImage = stickerFrame.findViewById(R.id.stickerImage);
                        Bitmap stickerBitmap = null;
                        if (stickerImage != null && stickerImage.getDrawable() != null) {
                            stickerImage.setDrawingCacheEnabled(true);
                            stickerBitmap = Bitmap.createBitmap(stickerImage.getDrawingCache());
                            stickerImage.setDrawingCacheEnabled(false);
                        }
                        String stickerPath = null;
                        if (stickerBitmap != null) {
                            try {
                                File file = new File(requireContext().getCacheDir(),
                                        "face_sticker_" + System.currentTimeMillis() + ".png");
                                FileOutputStream out = new FileOutputStream(file);
                                stickerBitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
                                out.close();
                                stickerPath = file.getAbsolutePath();
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }*/

                        // ✅ [수정] FaceStickerData에 serverId 포함하여 생성
                        // (FaceStickerData 생성자를 수정하지 않았다면 serverId 부분만 지우세요)
                        FaceStickerData data = new FaceStickerData(
                                meta.relX, meta.relY, meta.relW, meta.relH, meta.rot,
                                groupId,
                                serverId, // ★ 추가된 DB ID
                                null, stickerPath
                        );
                        viewModel.setFaceStickerData(data);
                    });
                }
            }));
        });

        checkBtn.setEnabled(false);
        checkBtn.setAlpha(0.4f);
        deleteStickerIcon.setEnabled(false);
        deleteStickerIcon.setAlpha(0.4f);

        // 1. 로컬 스토어 초기화 및 업로더 연결 (리스너 주입)
        StickerStore.get().init(requireContext().getApplicationContext());
        StickerStore.get().setUploader(new StickerRepository(requireContext(), uploadListener));

        LinearLayoutManager lm = new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false);
        myStickers.setLayoutManager(lm);
        adapter = new MyStickersAdapter(new ArrayList<>());
        myStickers.setAdapter(adapter);
        myStickers.setItemAnimator(null);

        adapter.setOnStickerClickListener((position, item) -> {
            selectStickerId = position;
            deleteStickerIcon.setEnabled(true);
            deleteStickerIcon.setAlpha(1.0f);

            stickerUrl = item.getImageUrl();

            showStickerCentered(item.getImageUrl(), item.getId());
        });


        faceCheckBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                setCheckboxSize(28.5f, 1f);
                EditStickerFragment editStickerFragment = new EditStickerFragment();
                Bundle args2 = new Bundle();
                args2.putString("stickerUrl", stickerUrl);
                editStickerFragment.setArguments(args2);
                requireActivity().getSupportFragmentManager()
                        .beginTransaction()
                        .setCustomAnimations(R.anim.slide_up, 0)
                        .replace(R.id.bottomArea2, editStickerFragment)
                        .commit();

                Controller.removeStickerFrame(stickerFrame);
            }else{
                setCheckboxSize(25f, 3f);
            }
        });

        setupBottomButtons();

        // 2. 임시 스티커 업로드 시작 (조회는 콜백에서 호출됨)
        uploadPendingStickers();

        return view;
    }

    @Override
    public void onResume() {
        // 🛑 [수정] SuperNotCalledException 방지
        super.onResume();

        saveBtn = requireActivity().findViewById(R.id.saveBtn);
        if (saveBtn != null) {
            saveBtn.setEnabled(false);
            saveBtn.setAlpha(0.4f);
        }

        if (bottomArea1 != null) {
            stickerEdit.setVisibility(View.VISIBLE);
            faceCheckBox.setChecked(false);
        }

        updateCheckButtonState();
    }

    // ---------------------------------------------------------------
// ✅ Pending Sticker 업로드 (순서 보장 로직)
// ---------------------------------------------------------------
    private void uploadPendingStickers() {
        List<StickerItem> itemsToUpload = new ArrayList<>();
        StickerItem pendingItem;

        // 1. 업로드할 항목들을 큐에서 꺼내 카운트 설정
        while ((pendingItem = StickerStore.get().pollPending()) != null) {
            itemsToUpload.add(pendingItem);
        }

        pendingUploadCount = itemsToUpload.size();
        Log.d("StickerUpload", "업로드할 스티커 개수: " + pendingUploadCount);

        if (pendingUploadCount == 0) {
            // 업로드할 스티커가 없으면 바로 목록 조회
            loadStickersFromServer();
            return;
        }

        // 2. 각 항목을 로컬 스토어에 추가하고 (이 과정에서 uploader.uploadToServer가 호출됨)
        //    UI에 즉시 반영
        for (StickerItem item : itemsToUpload) {
            StickerStore.get().addToAllFront(item);
            adapter.insertAtFront(item);
        }

        if (adapter.getItemCount() > 0) {
            myStickers.scrollToPosition(0);
        }
    }

    // ---------------------------------------------------------------
// ✅ 서버 API 호출: 내 스티커 목록 가져오기 (콜백 완료 후 실행)
// ---------------------------------------------------------------
    private void loadStickersFromServer() {
        StickerApi api = AppRetrofitClient.getInstance(requireContext()).create(StickerApi.class);

        // ★ 토큰을 가져와 API 호출에 사용해야 합니다. (이 부분은 AppRetrofitClient가 처리하므로, 여기서는 호출만 합니다.)
        api.getMyStickers().enqueue(new Callback<List<StickerResponseDto>>() {
            @Override
            public void onResponse(Call<List<StickerResponseDto>> call, Response<List<StickerResponseDto>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<StickerResponseDto> dtos = response.body();
                    List<StickerItem> items = new ArrayList<>();

                    for (StickerResponseDto dto : dtos) {
                        if (dto.getImageUrl() != null) {
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

    // ---------------------------------------------------------------
// ✅ 스티커 화면 배치 (Glide + ID 태그 저장)
// ---------------------------------------------------------------
    @SuppressLint("ClickableViewAccessibility")
    private void showStickerCentered(String stickerUrl, long stickerId) {
        stickerFrame = inflater.inflate(R.layout.v_sticker_edit, stickerOverlay, false);
        stickerImage = stickerFrame.findViewById(R.id.stickerImage);
        deleteController = stickerFrame.findViewById(R.id.deleteController);

        stickerEdit.setAlpha(1.0f);
        faceCheckBox.setEnabled(true);

        // Glide로 이미지 로드 (URL 처리)
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

            this.editingSticker = stickerFrame;
           // stickerFrame.setTag(R.id.tag_from_mysticker, true);
            stickerFrame.setTag(R.id.tag_sticker_url, stickerUrl);

            updateCheckButtonState();

            this.selectSticker = stickerFrame;
            Controller.setStickerActive(stickerFrame, true);

            for (int i = 0; i < stickerOverlay.getChildCount(); i++) {
                View child = stickerOverlay.getChildAt(i);
                if (child == stickerFrame) {
                    Controller.setControllersVisible(child, true);
                } else {
                    Controller.setControllersVisible(child, false);
                }
            }

            stickerFrame.setOnTouchListener((v, event) -> {
                switch (event.getActionMasked()) {
                    case MotionEvent.ACTION_DOWN: {
                        editingSticker = v;

                        for (int i = 0; i < stickerOverlay.getChildCount(); i++) {
                            View child = stickerOverlay.getChildAt(i);
                            Controller.setControllersVisible(child, child == v);
                        }
                        v.bringToFront();

                        return true;
                    }
                }
                return false;
            });

            Controller.enableStickerControl(null, null, stickerFrame, stickerOverlay, getResources());
            Controller.updateControllersSizeAndAngle(stickerFrame, getResources());
        });

        deleteController.setOnClickListener(x -> {
            if (editingSticker != null) {
                Controller.removeStickerFrame(editingSticker);
                editingSticker = null;
            }

            adapter.clearSelection();
            deleteStickerIcon.setAlpha(0.4f);
            deleteStickerIcon.setEnabled(false);

            stickerEdit.setAlpha(0.4f);
            faceCheckBox.setEnabled(false);

            updateCheckButtonState();
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

            restoreElevation();

            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .setCustomAnimations(R.anim.slide_up, 0)
                    .replace(R.id.bottomArea2, new StickersFragment())
                    .commit();
        });

        checkBtn.setOnClickListener(v -> {
            if (ClickUtils.isFastClick(v, 400)) return;

            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .setCustomAnimations(R.anim.slide_up, 0)
                    .replace(R.id.bottomArea2, new StickersFragment())
                    .commit();
        });
    }

    private boolean hasAnySticker() {
        for (int i = 0; i < stickerOverlay.getChildCount(); i++) {
            View child = stickerOverlay.getChildAt(i);

            Boolean isBrush = (Boolean) child.getTag(R.id.tag_brush_layer);
            if (Boolean.TRUE.equals(isBrush)) continue;

            String url = (String) child.getTag(R.id.tag_sticker_url);
            if (url != null && child.getVisibility() == View.VISIBLE) {
                return true;
            }

            Boolean isClone = (Boolean) child.getTag(R.id.tag_sticker_clone);
            if (Boolean.TRUE.equals(isClone) && child.getVisibility() == View.VISIBLE) {
                return true;
            }
        }
        return false;
    }

    private void updateCheckButtonState() {
        boolean hasSticker = hasAnySticker();

        if (hasSticker) {
            checkBtn.setEnabled(true);
            checkBtn.setAlpha(1.0f);
        } else {
            checkBtn.setEnabled(false);
            checkBtn.setAlpha(0.4f);
        }
    }

    private void restoreElevation() {
        if (stickerFrame != null && prevElevation != null) {
            ViewCompat.setZ(stickerFrame, prevElevation);
            stickerFrame.invalidate();
        }
    }

    private void raiseStickerTop(@NonNull View sticker, @NonNull ViewGroup parent) {
        float maxZ = 0f;
        for (int i = 0; i < parent.getChildCount(); i++) {
            maxZ = Math.max(maxZ, ViewCompat.getZ(parent.getChildAt(i)));
        }
        ViewCompat.setZ(sticker, maxZ + 1000f);
        sticker.bringToFront();
        parent.invalidate();
    }

    private void confirmDeleteSticker() {
        if (selectStickerId == RecyclerView.NO_POSITION) return;

        new StickerDeleteDialog(requireContext(), new StickerDeleteDialog.StickerDeleteDialogListener() {
            @Override
            public void onKeep() {
            }

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
        }).withMessage("내 스티커에서 정말로 삭제하시겠습니까?")
                .withButton1Text("예")
                .withButton2Text("아니오")
                .show();
    }

    public void showToast(String message) {
        isToastVisible = true;

        View old = topArea.findViewWithTag("inline_banner");
        if (old != null) topArea.removeView(old);

        TextView tv = new TextView(requireContext());
        tv.setTag("inline_banner");
        tv.setText(message);
        tv.setTextColor(0XFFFFFFFF);
        tv.setTextSize(16);
        tv.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        tv.setPadding(Controller.dp(14, getResources()), Controller.dp(10, getResources()), Controller.dp(14, getResources()), Controller.dp(10, getResources()));
        tv.setElevation(Controller.dp(4, getResources()));

        GradientDrawable bg = new GradientDrawable();
        bg.setColor(0xCC222222);
        bg.setCornerRadius(Controller.dp(16, getResources()));
        tv.setBackground(bg);

        ConstraintLayout.LayoutParams lp =
                new ConstraintLayout.LayoutParams(ConstraintLayout.LayoutParams.WRAP_CONTENT,
                        ConstraintLayout.LayoutParams.WRAP_CONTENT);
        lp.startToStart = topArea.getId();
        lp.endToEnd = topArea.getId();
        lp.topToTop = topArea.getId();
        lp.bottomToBottom = topArea.getId();
        tv.setLayoutParams(lp);

        tv.setAlpha(0f);
        topArea.addView(tv);
        tv.animate().alpha(1f).setDuration(150).start();

        tv.postDelayed(() -> tv.animate()
                .alpha(0f)
                .setDuration(200)
                .withEndAction(() -> {
                    if (tv.getParent() == topArea) topArea.removeView(tv);
                    isToastVisible = false;
                })
                .start(), 2000);
    }

    private void setCheckboxSize(float dp1, float dp2) {
        int px = (int) dp(dp1);

        ViewGroup.LayoutParams lp = faceCheckBox.getLayoutParams();
        lp.width = px;
        lp.height = px;
        faceCheckBox.setLayoutParams(lp);

        faceCheckBox.requestLayout();

        ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) faceCheckBox.getLayoutParams();
        params.topMargin = (int) dp(dp2);
        faceCheckBox.setLayoutParams(params);
    }

    private float dp(float dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        if (faceBox != null) {
            faceBox.clearBoxes();
            faceBox.setVisibility(View.GONE);
        }

        if (stickerOverlay != null) {
            for (int i = 0; i < stickerOverlay.getChildCount(); i++) {
                View child = stickerOverlay.getChildAt(i);
                child.setOnClickListener(null);
                child.setOnTouchListener(null);
                Controller.setControllersVisible(child, false);
                Controller.setStickerActive(child, false);
            }
        }
    }
}