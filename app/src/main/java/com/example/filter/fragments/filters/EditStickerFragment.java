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

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatButton;
import androidx.constraintlayout.widget.ConstraintLayout;
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
import com.example.filter.dialogs.FaceStickerDeleteDialog;
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

public class EditStickerFragment extends Fragment {
    private StickerMeta pendingMeta;
    private Long pendingServerId = null;
    public static int sessionId = 0;
    private ConstraintLayout topArea;
    private FrameLayout photoContainer;
    private boolean isToastVisible = false;
    private FaceBoxOverlayView faceBox;
    private View editingSticker = null;
    private String stickerUrl;
    private long sticker_db_id;
    private ConstraintLayout bottomArea1;
    private LinearLayout stickerEdit;
    private CheckBox faceCheckBox;
    private AppCompatButton saveBtn;
    private ImageButton closeBtn;
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
        View view = inflater.inflate(R.layout.f_edit_sticker, container, false);
        this.inflater = inflater;

        sessionId++;

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
            sticker_db_id = item.getId();

            showStickerCentered(item.getImageUrl(), item.getId());
        });


        faceCheckBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                setCheckboxSize(28.5f, 1f);
                FaceStickerFragment faceStickerFragment = new FaceStickerFragment();
                Bundle args2 = new Bundle();
                args2.putString("stickerUrl", stickerUrl);
                args2.putLong("sticker_db_id", sticker_db_id);
                faceStickerFragment.setArguments(args2);
                faceStickerFragment.setPreviousFragment(EditStickerFragment.this);

                requireActivity().getSupportFragmentManager()
                        .beginTransaction()
                        .setCustomAnimations(R.anim.slide_up, 0)
                        .hide(EditStickerFragment.this)
                        .add(R.id.bottomArea2, faceStickerFragment)
                        .commit();

                Controller.removeStickerFrame(stickerFrame);
            } else {
                setCheckboxSize(25f, 3f);
            }
        });

        setupBottomButtons();

        // 2. 임시 스티커 업로드 시작 (조회는 콜백에서 호출됨)
        uploadPendingStickers();


        StickerViewModel vm = new ViewModelProvider(requireActivity()).get(StickerViewModel.class);

        for (int i = 0; i < stickerOverlay.getChildCount(); i++) {
            View child = stickerOverlay.getChildAt(i);

            // 이미 있었던 스티커 → original
            child.setTag(R.id.tag_sticker_original, true);
            child.setTag(R.id.tag_sticker_session, 0);

            // original 상태 저장
            StickerViewModel.StickerState s = new StickerViewModel.StickerState();
            s.x = child.getX();
            s.y = child.getY();
            s.rotation = child.getRotation();
            s.width = child.getWidth();
            s.height = child.getHeight();

            vm.saveOriginalState(child, s);
        }

        return view;
    }

    public void setFaceMeta(StickerMeta meta, String url, long serverId) {
        pendingMeta = meta;
        stickerUrl = url;
        pendingServerId = serverId;
    }

    @androidx.camera.core.ExperimentalGetImage
    public void applyPendingMeta() {
        if (pendingMeta != null) {
            FilterActivity activity = (FilterActivity) requireActivity();
            //Bundle args = getArguments();
            //boolean fromFace = args != null && args.getBoolean("IS_FACE", false);
            faceBox = new FaceBoxOverlayView(requireContext());
            photoContainer.addView(faceBox, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            activity.getPhotoPreview().queueEvent(() -> {
                Bitmap bmp = activity.getRenderer().getCurrentBitmap();
                activity.runOnUiThread(() -> FaceDetect.detectFaces(bmp, faceBox, (faces, bitmap) -> {
                    if (faces.isEmpty()) {
                        //if (fromFace) {
                        showToast("얼굴을 감지하지 못했습니다");
                        //}
                        return;
                    }

                    if (!faces.isEmpty() /*&& args != null*/) {
                        StickerViewModel viewModel = new ViewModelProvider(requireActivity()).get(StickerViewModel.class);
                        int groupId = FaceStickerFragment.stickerId;
                        //View stickerFrame = viewModel.getTempView(groupId);
                        //String stickerPath = args.getString("stickerUrl");

                        /// ⭐ Bundle로 넘겨주던 serverId 방식을 바꿨습니다 ⭐ ///
                        // ✅ [추가] 인자에서 서버 DB ID 가져오기 (없으면 -1)
                        // (이전 프래그먼트에서 "sticker_db_id"라는 키로 넘겨줘야 함)
                        //long serverId = args.getLong("sticker_db_id", -1L);
                        long serverId = (pendingServerId != null ? pendingServerId : -1L);

                        Log.e("SERVER_ID_TEST", "applyPendingMeta(): serverId=" + serverId);

                        //Log.d("얼굴스티커", String.format("에딧스티커프래그먼트 | relX = %.1f, relY = %.1f, relW = %.1f, relH = %.1f, rot = %.1f", pendingMeta.relX, pendingMeta.relY, pendingMeta.relW, pendingMeta.relH, pendingMeta.rot));

                        List<float[]> placement = StickerMeta.recalculate(faces, bitmap, stickerOverlay, pendingMeta, requireContext());
                        requireActivity().runOnUiThread(() -> {
                            //viewModel.removeCloneGroup(groupId, stickerOverlay);
                            //viewModel.setFaceStickerDataToDelete(groupId);

                            for (float[] p : placement) {
                                View faceSticker = StickerMeta.cloneSticker(stickerOverlay, stickerUrl, requireContext(), p);
                                if (faceSticker != null) {

                                    // ✅ [핵심] 뷰에 서버 DB ID 태그 저장
                                    if (serverId != -1L) {
                                        faceSticker.setTag(R.id.tag_sticker_db_id, serverId);

                                        Log.e("SERVER_ID_TEST", "Tag set on view: " + serverId);
                                    }

                                    viewModel.addCloneGroup(groupId, faceSticker);

                                    faceSticker.setTag(R.id.tag_sticker_original, false);
                                    faceSticker.setTag(R.id.tag_brush_layer, Boolean.FALSE);
                                    faceSticker.setTag(R.id.tag_sticker_url, stickerUrl);
                                    faceSticker.setTag(R.id.tag_sticker_clone, Boolean.TRUE);
                                    faceSticker.setTag(R.id.tag_sticker_group, groupId);
                                    faceSticker.setTag(R.id.tag_sticker_session, sessionId);

                                    updateCheckButtonState();
                                }

                                faceSticker.setOnClickListener(v -> {
                                    Integer gid = (Integer) v.getTag(R.id.tag_sticker_group);
                                    if (gid != null) {
                                        confirmDeleteFaceSticker(gid);
                                    }
                                });
                            }

                            showToast("얼굴 인식 성공");

                            /// ⭐ 스티커이미지를 경로로 넘겨주는 방식으로 바꿔서 비트맵부분 필요없어졌어요 ⭐ ///
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
                                    pendingMeta.relX, pendingMeta.relY, pendingMeta.relW, pendingMeta.relH, pendingMeta.rot,
                                    groupId,
                                    serverId, // ★ 추가된 DB ID
                                    null, stickerUrl
                            );
                            viewModel.setFaceStickerData(data);
                        });
                    }
                }));
            });
        }
    }

    @Override
    public void onResume() {
        // 🛑 [수정] SuperNotCalledException 방지
        super.onResume();

        saveBtn = requireActivity().findViewById(R.id.saveBtn);
        closeBtn = requireActivity().findViewById(R.id.closeBtn);
        if (saveBtn != null && closeBtn != null) {
            saveBtn.setEnabled(false);
            closeBtn.setEnabled(false);
            saveBtn.setAlpha(0.0f);
            closeBtn.setAlpha(0.0f);
        }

        if (bottomArea1 != null) {
            stickerEdit.setVisibility(View.VISIBLE);
            faceCheckBox.setChecked(false);
        }

        if (stickerOverlay != null) {
            for (int i = 0; i < stickerOverlay.getChildCount(); i++) {
                View child = stickerOverlay.getChildAt(i);

                if (Boolean.TRUE.equals(child.getTag(R.id.tag_brush_layer))) {
                    child.setOnClickListener(null);
                    child.setClickable(false);
                    child.setLongClickable(false);
                    child.setEnabled(false);
                    continue;
                }

                child.setEnabled(true);
                child.setClickable(true);
                child.setLongClickable(true);

                Boolean isClone = (Boolean) child.getTag(R.id.tag_sticker_clone);
                if (Boolean.TRUE.equals(isClone)) {
                    child.setOnClickListener(v -> {
                        Integer gid = (Integer) v.getTag(R.id.tag_sticker_group);
                        if (gid != null) {
                            if (!isAdded()) return;
                            confirmDeleteFaceSticker(gid);
                        }
                    });
                }
            }
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

            stickerFrame.setTag(R.id.tag_sticker_original, false);
            stickerFrame.setTag(R.id.tag_sticker_session, sessionId);

            this.editingSticker = stickerFrame;
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

                            stickerEdit.setAlpha(1.0f);
                            faceCheckBox.setEnabled(true);
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
                /// 이전 세션에서 배치된 기존 스티커를 삭제하고 취소버튼을 누르면 복구되도록 일단 안 보이게만 설정 ///
                if (Boolean.TRUE.equals(editingSticker.getTag(R.id.tag_sticker_original))) {
                    editingSticker.setTag(R.id.tag_sticker_delete, true);
                    editingSticker.setVisibility(View.INVISIBLE);
                } else {
                    /// 현재 세션에서 새로 배치한 스티커는 복구할 필요 없으니까 바로 삭제 ///
                    Controller.removeStickerFrame(editingSticker);
                }

                editingSticker = null;
            }

            resetSelectAdapter();

            updateCheckButtonState();
        });

        stickerFrame.bringToFront();
    }

    public void resetSelectAdapter() {
        if (adapter != null) adapter.clearSelection();

        selectStickerId = RecyclerView.NO_POSITION;

        deleteStickerIcon.setAlpha(0.4f);
        deleteStickerIcon.setEnabled(false);

        stickerEdit.setAlpha(0.4f);
        faceCheckBox.setEnabled(false);
    }

    private void setupBottomButtons() {
        deleteStickerIcon.setOnClickListener(v -> {
            if (ClickUtils.isFastClick(v, 400)) return;
            if (!deleteStickerIcon.isEnabled()) return;
            confirmDeleteSticker();
        });


        requireActivity().getOnBackPressedDispatcher().addCallback(requireActivity(), new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                cancelBtn.performClick();
            }
        });


        cancelBtn.setOnClickListener(v -> {
            if (ClickUtils.isFastClick(v, 400)) return;

            StickerViewModel vm = new ViewModelProvider(requireActivity()).get(StickerViewModel.class);

            for (int i = 0; i < stickerOverlay.getChildCount(); i++) {
                View child = stickerOverlay.getChildAt(i);

                Boolean isOriginal = (Boolean) child.getTag(R.id.tag_sticker_original);
                Object sessionTag = child.getTag(R.id.tag_sticker_session);
                int childSession = sessionTag != null ? (Integer) sessionTag : -1;
                Boolean isDelete = (Boolean) child.getTag(R.id.tag_sticker_delete);

                if (Boolean.FALSE.equals(isOriginal) && childSession == sessionId) {
                    stickerOverlay.removeView(child);
                    i--;
                    continue;
                }

                if (Boolean.TRUE.equals(isOriginal)) {
                    StickerViewModel.StickerState s = vm.getOriginalState(child);
                    if (s != null) {
                        restoreState(child, s);
                    }

                    /// 이전 세션에서 배치된 기존 스티커 복구되도록 이전 상태로 돌리고 보이게 하기 ///
                    ///  얼굴스티커 삭제도 취소 ///
                    if (Boolean.TRUE.equals(isDelete)) {
                        Controller.setControllersVisible(child, false);
                        child.setVisibility(View.VISIBLE);
                        child.setTag(R.id.tag_sticker_delete, false);
                    }
                }
            }

            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .setCustomAnimations(R.anim.slide_up, 0)
                    .replace(R.id.bottomArea2, new StickersFragment())
                    .commit();
        });

        checkBtn.setOnClickListener(v -> {
            if (ClickUtils.isFastClick(v, 400)) return;

            StickerViewModel vm = new ViewModelProvider(requireActivity()).get(StickerViewModel.class);
            List<Integer> deletedGroupId = new ArrayList<>();

            /// 이전 세션에서 배치된 기존 스티커와 얼굴스티커 실제로 삭제하기 ///
            for (int i = stickerOverlay.getChildCount() - 1; i >= 0; i--) {
                View child = stickerOverlay.getChildAt(i);
                Boolean isDelete = (Boolean) child.getTag(R.id.tag_sticker_delete);

                if (Boolean.TRUE.equals(isDelete)) {
                    Integer gid = (Integer) child.getTag(R.id.tag_sticker_group);
                    if (gid != null && !deletedGroupId.contains(gid)) {
                        deletedGroupId.add(gid);
                    }
                    stickerOverlay.removeView(child);
                }
            }

            for (Integer gid : deletedGroupId) {
                vm.setFaceStickerDataToDelete(gid);
            }

            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .setCustomAnimations(R.anim.slide_up, 0)
                    .replace(R.id.bottomArea2, new StickersFragment())
                    .commit();
        });
    }

    private void restoreState(View v, StickerViewModel.StickerState s) {
        v.setX(s.x);
        v.setY(s.y);
        v.setRotation(s.rotation);

        ViewGroup.LayoutParams lp = v.getLayoutParams();
        lp.width = s.width;
        lp.height = s.height;
        v.setLayoutParams(lp);
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

    private void confirmDeleteSticker() {
        if (selectStickerId == RecyclerView.NO_POSITION) return;

        StickerItem targetItem = adapter.getItem(selectStickerId);
        long serverId = targetItem.getId();

        new StickerDeleteDialog(requireContext(), new StickerDeleteDialog.StickerDeleteDialogListener() {
            @Override
            public void onCancel() {
            }

            @Override
            public void onDelete() {
                StickerApi api = AppRetrofitClient.getInstance(requireContext()).create(StickerApi.class);

                api.deleteSticker(serverId).enqueue(new Callback<Void>() {
                    @Override
                    public void onResponse(Call<Void> call, Response<Void> response) {
                        if (response.isSuccessful()) {
                            if (selectStickerId != RecyclerView.NO_POSITION) {
                                Log.e("목록에서스티커삭제", "삭제 성공" + serverId);

                                Controller.clearCurrentSticker(stickerOverlay, selectSticker);
                                adapter.removeAt(selectStickerId);
                                selectStickerId = RecyclerView.NO_POSITION;
                                deleteStickerIcon.setEnabled(false);
                                deleteStickerIcon.setAlpha(0.4f);

                                //loadStickersFromServer();
                            }
                        } else {
                            Log.e("목록에서스티커삭제", "서버 응답 실패 코드: " + response.code());
                        }
                    }

                    @Override
                    public void onFailure(Call<Void> call, Throwable t) {
                        Log.e("목록에서스티커삭제", "삭제 요청 실패", t);
                    }
                });
            }
        }).show();
    }

    private void confirmDeleteFaceSticker(Integer gid) {
        new FaceStickerDeleteDialog(requireContext(), new FaceStickerDeleteDialog.FaceStickerDeleteDialogListener() {
            @Override
            public void onNo() {
            }

            @Override
            public void onYes() {
                StickerViewModel viewModel = new ViewModelProvider(requireActivity()).get(StickerViewModel.class);
                //viewModel.invisibleCloneGroup(gid, stickerOverlay);
                //viewModel.removeCloneGroup(gid, stickerOverlay);
                //viewModel.setFaceStickerDataToDelete(gid);
                List<View> list = viewModel.getCloneGroup(gid);
                for (View v : list) {
                    v.setVisibility(View.INVISIBLE);
                    v.setTag(R.id.tag_sticker_delete, true);
                }
            }
        }).show();
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
    }
}