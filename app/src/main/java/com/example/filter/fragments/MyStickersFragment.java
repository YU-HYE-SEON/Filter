package com.example.filter.fragments;

import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.ViewCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.filter.R;
import com.example.filter.adapters.MyStickersAdapter;
import com.example.filter.dialogs.MyStickerDeleteDialog;
import com.example.filter.etc.ClickUtils;
import com.example.filter.etc.Controller;
import com.example.filter.items.StickerItem;
import com.example.filter.etc.StickerStore;

import java.io.File;

public class MyStickersFragment extends Fragment {
    private ImageButton cancelBtn, checkBtn;
    private ImageButton deleteStickerIcon;
    private RecyclerView myStickers;
    private FrameLayout stickerOverlay;
    private View currentStickerView = null;
    private String currentStickerName = null;
    private int currentSelectedPos = RecyclerView.NO_POSITION;
    private LayoutInflater inflater;
    private MyStickersAdapter adapter;
    private int baselineChildCount = 0;
    private boolean skipDisableOnce = false;
    private static final int TAG_TEMP_PREVIEW = R.id.tag_prev_enabled;
    //private ImageButton undoSticker, redoSticker, originalSticker;
    private long sessionId = -1L;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.f_my_stickers, container, false);

        this.inflater = inflater;
        myStickers = view.findViewById(R.id.myStickers);
        deleteStickerIcon = view.findViewById(R.id.deleteStickerIcon);
        cancelBtn = view.findViewById(R.id.cancelBtn);
        checkBtn = view.findViewById(R.id.checkBtn);

        /*undoSticker = requireActivity().findViewById(R.id.undoSticker);
        redoSticker = requireActivity().findViewById(R.id.redoSticker);
        originalSticker = requireActivity().findViewById(R.id.originalSticker);

        if (undoSticker != null) undoSticker.setVisibility(View.INVISIBLE);
        if (redoSticker != null) redoSticker.setVisibility(View.INVISIBLE);
        if (originalSticker != null) originalSticker.setVisibility(View.INVISIBLE);*/

        stickerOverlay = requireActivity().findViewById(R.id.stickerOverlay);

        /*getParentFragmentManager().setFragmentResultListener("stickerMeta", this, (key, meta) -> {
            float relX = meta.getFloat("relX");
            float relY = meta.getFloat("relY");
            float relW = meta.getFloat("relW");
            float relH = meta.getFloat("relH");
            float rotation = meta.getFloat("rot");
            Log.e("StickerMeta", String.format("마이스티커프래그먼트 :: X=%.3f, Y=%.3f, W=%.3f, H=%.3f, R=%.3f", relX, relY, relW, relH, rotation));
            //Log.d("StickerMeta", String.format("페이스프래그먼트 faceModel :: X=%.3f, Y=%.3f, W=%.3f, H=%.3f, R=%.3f", relX, relY, relW, relH,stickerR));
        });*/

        Bundle args = getArguments() != null ? getArguments() : new Bundle();
        baselineChildCount = args.getInt("sessionBaseline", (stickerOverlay != null) ? stickerOverlay.getChildCount() : 0);
        sessionId = args.getLong("sessionId", -1L);
        skipDisableOnce = args.getBoolean("skipDisableOnce", false);

        boolean cameFromEdit =
                "mystickers_check".equals(EditMyStickerFragment.sLastReturnOriginAction) ||
                        "mystickers_cancel".equals(EditMyStickerFragment.sLastReturnOriginAction);

        if (cameFromEdit) {
            skipDisableOnce = true;
            EditMyStickerFragment.sLastReturnOriginAction = null;
        }

        boolean restoreAllActive = args.getBoolean("restoreAllActive", false);

        deleteStickerIcon.setEnabled(false);
        deleteStickerIcon.setAlpha(0.4f);

        LinearLayoutManager lm = new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false);

        myStickers.setLayoutManager(lm);

        StickerStore.get().init(requireContext().getApplicationContext());

        adapter = new MyStickersAdapter(StickerStore.get().snapshotAll());
        myStickers.setAdapter(adapter);
        myStickers.setItemAnimator(null);

        if (stickerOverlay != null) {
            for (int i = 0; i < stickerOverlay.getChildCount(); i++) {
                View child = stickerOverlay.getChildAt(i);
                Controller.hideControllers(child);

                if (Boolean.TRUE.equals(child.getTag(R.id.tag_brush_layer))) {
                    child.setOnClickListener(null);
                    child.setClickable(false);
                    child.setLongClickable(false);
                    child.setEnabled(false);
                    continue;
                }

                boolean fromFaceFragment = args.getBoolean("fromFaceFragment", false);

                if (restoreAllActive) {
                    Controller.setStickerActive(child, true);
                } else if (fromFaceFragment) {
                    Object sid = child.getTag(R.id.tag_session_id);
                    if (sid instanceof Long && ((Long) sid) == sessionId) {
                        Controller.setStickerActive(child, true);
                    } else {
                        Controller.setStickerActive(child, false);
                    }
                } else if (!skipDisableOnce) {
                    Controller.setStickerActive(child, false);
                }
            }
        }

        adapter.setOnStickerClickListener((position, stickerName) -> {
            currentSelectedPos = position;
            deleteStickerIcon.setEnabled(true);
            deleteStickerIcon.setAlpha(1.0f);
            showStickerCentered(stickerName);
        });

        deleteStickerIcon.setOnClickListener(v -> {
            if (ClickUtils.isFastClick(v, 400)) return;
            if (!deleteStickerIcon.isEnabled()) return;
            confirmDeleteSelectedSticker();
        });

        cancelBtn.setOnClickListener(v -> {
            if (ClickUtils.isFastClick(v, 400)) return;
            if (stickerOverlay != null) {
                for (int i = stickerOverlay.getChildCount() - 1; i >= baselineChildCount; i--) {
                    View child = stickerOverlay.getChildAt(i);
                    Object sid = child.getTag(R.id.tag_session_id);
                    if (sid instanceof Long && ((Long) sid) == sessionId) {
                        stickerOverlay.removeViewAt(i);
                    }
                }
                for (int i = 0; i < stickerOverlay.getChildCount(); i++) {
                    View child = stickerOverlay.getChildAt(i);
                    Controller.setStickerActive(child, true);
                    Controller.hideControllers(stickerOverlay.getChildAt(i));

                    stickerOverlay.getChildAt(i).setTag(R.id.tag_prev_enabled, null);
                }
            }
            currentSelectedPos = RecyclerView.NO_POSITION;
            currentStickerName = null;
            currentStickerView = null;

            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .setCustomAnimations(R.anim.slide_up, 0)
                    .replace(R.id.bottomArea2, new StickersFragment())
                    .commit();
        });

        checkBtn.setOnClickListener(v -> {
            if (ClickUtils.isFastClick(v, 400)) return;
            if (stickerOverlay != null) {
                for (int i = 0; i < stickerOverlay.getChildCount(); i++) {
                    View child = stickerOverlay.getChildAt(i);
                    Controller.hideControllers(child);
                    Controller.setStickerActive(child, true);
                }
            }

            //FilterActivity a = (FilterActivity) requireActivity();
            //a.recordStickerPlacement(baselineChildCount);

            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .setCustomAnimations(R.anim.slide_up, 0)
                    .replace(R.id.bottomArea2, new StickersFragment())
                    .commit();
        });

        return view;
    }

    private void showStickerCentered(String key) {
        clearCurrentSticker();

        View overlayViewLayout = inflater.inflate(R.layout.v_sticker_edit, stickerOverlay, false);
        ImageView stickerImage = overlayViewLayout.findViewById(R.id.stickerImage);

        File f = new File(key);
        if (f.exists()) {
            stickerImage.setImageURI(Uri.fromFile(f));
            if (stickerImage.getDrawable() == null) {
                stickerImage.setImageBitmap(BitmapFactory.decodeFile(f.getAbsolutePath()));
            }
        } else {
            int resId = getResources().getIdentifier(key, "drawable", requireContext().getPackageName());
            if (resId != 0) stickerImage.setImageResource(resId);
        }

        int sizePx = Controller.dp(230, getResources());
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(sizePx, sizePx);
        overlayViewLayout.setLayoutParams(lp);

        stickerOverlay.post(() -> {
            float cx = (stickerOverlay.getWidth() - sizePx) / 2f;
            float cy = (stickerOverlay.getHeight() - sizePx) / 2f;
            overlayViewLayout.setX(cx);
            overlayViewLayout.setY(cy);
            overlayViewLayout.setTag("sessionAdded");

            overlayViewLayout.setTag(R.id.tag_session_id, sessionId);
            if (overlayViewLayout.getTag(R.id.tag_sticker_id) == null) {
                overlayViewLayout.setTag(R.id.tag_sticker_id, "sticker_" + System.currentTimeMillis());
            }

            stickerOverlay.addView(overlayViewLayout);

            currentStickerView = overlayViewLayout;
            currentStickerName = key;

            overlayViewLayout.setTag(TAG_TEMP_PREVIEW, Boolean.TRUE);

            Controller.setStickerActive(overlayViewLayout, true);

            overlayViewLayout.setOnClickListener(v -> openEdit(key, overlayViewLayout));
        });
    }

    private void clearCurrentSticker() {
        if (currentStickerView != null && currentStickerView.getParent() == stickerOverlay) {
            Object isTemp = currentStickerView.getTag(TAG_TEMP_PREVIEW);
            if (Boolean.TRUE.equals(isTemp)) {
                stickerOverlay.removeView(currentStickerView);
            }
        }
        currentStickerView = null;
    }

    private void openEdit(String stickerName, View stickerLayout) {
        Fragment cur = requireActivity()
                .getSupportFragmentManager()
                .findFragmentById(R.id.bottomArea2);
        if (cur instanceof EditMyStickerFragment) return;

        Bundle args = new Bundle();
        args.putString("stickerName", stickerName);
        args.putFloat("x", stickerLayout.getX());
        args.putFloat("y", stickerLayout.getY());
        args.putInt("w", stickerLayout.getLayoutParams().width);
        args.putInt("h", stickerLayout.getLayoutParams().height);
        args.putFloat("rotation", stickerLayout.getRotation());
        args.putString("origin", "mystickers");
        args.putInt("sessionBaseline", baselineChildCount);

        args.putInt("stickerW", stickerLayout.getLayoutParams().width);
        args.putInt("stickerH", stickerLayout.getLayoutParams().height);

        long childSessionId = -1L;
        Object s = stickerLayout.getTag(R.id.tag_session_id);
        if (s instanceof Long) childSessionId = (Long) s;
        else childSessionId = sessionId;
        args.putLong("sessionId", childSessionId);

        int n = stickerOverlay.getChildCount();
        boolean[] prevEnabled = new boolean[n];
        for (int i = 0; i < n; i++) {
            View child = stickerOverlay.getChildAt(i);
            prevEnabled[i] = child.isEnabled();
        }
        args.putBooleanArray("prevEnabled", prevEnabled);

        hideControllersForAll();

        for (int i = 0; i < stickerOverlay.getChildCount(); i++) {
            View child = stickerOverlay.getChildAt(i);
            child.setTag(R.id.tag_prev_enabled, child.isEnabled());
            Controller.setStickerActive(child, child == stickerLayout);
        }

        Controller.setControllersVisible(stickerLayout, true);
        stickerLayout.setTag("editingSticker");

        stickerLayout.setOnClickListener(null);
        stickerLayout.setClickable(false);

        EditMyStickerFragment edit = new EditMyStickerFragment();
        edit.setArguments(args);

        requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .setCustomAnimations(R.anim.slide_up, 0)
                .replace(R.id.bottomArea2, edit)
                .addToBackStack("edit_from_stickers")
                .commit();
    }

    /*public void consumePendingAndScrollToStart() {
        if (adapter == null || myStickers == null) return;
        StickerItem newly;
        boolean inserted = false;
        while ((newly = StickerStore.get().pollPending()) != null) {
            adapter.insertAtFront(newly);
            StickerStore.get().addToAllFront(newly);
            inserted = true;
        }
        if (inserted) {
            myStickers.scrollToPosition(0);
            if (deleteStickerIcon != null) {
                deleteStickerIcon.setEnabled(false);
                deleteStickerIcon.setAlpha(0.4f);
            }
        }
    }*/

    private void attachEditListenerForSticker(@NonNull View stickerView) {
        stickerView.setOnClickListener(null);

        if (Boolean.TRUE.equals(stickerView.getTag(R.id.tag_brush_layer))) {
            stickerView.setClickable(false);
            stickerView.setLongClickable(false);
            stickerView.setEnabled(false);
            return;
        }

        stickerView.setOnClickListener(v -> {
            if (!isAdded()) return;
            FragmentActivity act = getActivity();
            if (act == null || act.isFinishing() || act.isDestroyed()) return;
            if (!stickerView.isEnabled()) return;

            Fragment cur = act.getSupportFragmentManager().findFragmentById(R.id.bottomArea2);
            if (cur instanceof EditMyStickerFragment) return;

            if (stickerView.findViewById(R.id.stickerImage) == null ||
                    stickerView.findViewById(R.id.editFrame) == null ||
                    stickerView.findViewById(R.id.rotateController) == null ||
                    stickerView.findViewById(R.id.sizeController) == null) {
                return;
            }

            int beforeIndex = stickerOverlay.indexOfChild(stickerView);
            float beforeZ = ViewCompat.getZ(stickerView);
            int n = stickerOverlay.getChildCount();
            boolean[] prevEnabled = new boolean[n];

            Bundle args = new Bundle();
            args.putString("origin", "mystickers");
            args.putBooleanArray("prevEnabled", prevEnabled);
            args.putFloat("prevElevation", beforeZ);

            Controller.raiseStickerToAbsoluteTop(stickerView, stickerOverlay);

            int afterIndex = stickerOverlay.indexOfChild(stickerView);
            float afterZ = ViewCompat.getZ(stickerView);

            /*if (act instanceof FilterActivity) {
                ((FilterActivity) act).recordStickerZOrderChange(
                        stickerView, beforeIndex, beforeZ, afterIndex, afterZ
                );
            }*/

            stickerView.bringToFront();
            stickerOverlay.requestLayout();
            stickerOverlay.invalidate();

            for (int i = 0; i < n; i++) prevEnabled[i] = stickerOverlay.getChildAt(i).isEnabled();

            hideControllersForAll();

            for (int i = 0; i < stickerOverlay.getChildCount(); i++) {
                View child = stickerOverlay.getChildAt(i);
                child.setTag(R.id.tag_prev_enabled, child.isEnabled());
                Controller.setStickerActive(child, child == stickerView);
            }

            Controller.setControllersVisible(stickerView, true);
            stickerView.setTag("editingSticker");

            EditMyStickerFragment edit = new EditMyStickerFragment();
            edit.setArguments(args);

            act.getSupportFragmentManager()
                    .beginTransaction()
                    .setCustomAnimations(R.anim.slide_up, 0)
                    .replace(R.id.bottomArea2, edit)
                    .addToBackStack("edit_from_stickers")
                    .commit();
        });
    }

    private void hideControllersForAll() {
        for (int i = 0; i < stickerOverlay.getChildCount(); i++) {
            View child = stickerOverlay.getChildAt(i);
            Controller.hideControllers(child);

            Object t = child.getTag();
            if ("editingSticker".equals(t)) {
                child.setTag(null);
            }
        }
    }

    private void confirmDeleteSelectedSticker() {
        if (currentSelectedPos == RecyclerView.NO_POSITION) return;

        new MyStickerDeleteDialog(requireContext(), new MyStickerDeleteDialog.MyStickerDeleteDialogListener() {
            @Override
            public void onKeep() {

            }

            @Override
            public void onDelete() {
                if (currentSelectedPos != RecyclerView.NO_POSITION) {
                    StickerItem toRemove = adapter.getItem(currentSelectedPos);
                    String key = toRemove.getImageUrl();

                    clearCurrentSticker();
                    StickerStore.get().removeByKey(key);
                    adapter.removeAt(currentSelectedPos);

                    currentSelectedPos = RecyclerView.NO_POSITION;
                    currentStickerName = null;
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

        StickerItem newly;
        boolean inserted = false;
        while ((newly = StickerStore.get().pollPending()) != null) {
            adapter.insertAtFront(newly);
            StickerStore.get().addToAllFront(newly);
            inserted = true;
        }
        if (inserted) {
            myStickers.scrollToPosition(0);
            deleteStickerIcon.setEnabled(false);
            deleteStickerIcon.setAlpha(0.4f);
        }

        if (stickerOverlay != null) {
            for (int i = 0; i < stickerOverlay.getChildCount(); i++) {
                View child = stickerOverlay.getChildAt(i);

                Controller.hideControllers(child);

                if (Boolean.TRUE.equals(child.getTag(R.id.tag_brush_layer))) {
                    child.setOnClickListener(null);
                    child.setClickable(false);
                    child.setLongClickable(false);
                    child.setEnabled(false);
                    continue;
                }

                attachEditListenerForSticker(child);
            }
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

        /*if (StickersFragment.faceBox != null) {
            StickersFragment.faceBox.clearBoxes();
            StickersFragment.faceBox.setVisibility(View.GONE);
        }*/
    }
}