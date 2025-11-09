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
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.filter.R;
import com.example.filter.adapters.MyStickersAdapter;
import com.example.filter.dialogs.StickerDeleteDialog;
import com.example.filter.etc.ClickUtils;
import com.example.filter.etc.Controller;
import com.example.filter.etc.StickerStore;
import com.example.filter.etc.StickerViewModel;
import com.example.filter.items.StickerItem;

import java.io.File;

public class MyStickersFragment extends Fragment {
    private ImageButton cancelBtn, checkBtn, deleteStickerIcon;
    private MyStickersAdapter adapter;
    private RecyclerView myStickers;
    private FrameLayout stickerOverlay;
    private View selectSticker = null;
    private int selectStickerId = RecyclerView.NO_POSITION;
    private LayoutInflater inflater;
    //private ImageButton undoSticker, redoSticker, originalSticker;
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

        /*undoSticker = requireActivity().findViewById(R.id.undoSticker);
        redoSticker = requireActivity().findViewById(R.id.redoSticker);
        originalSticker = requireActivity().findViewById(R.id.originalSticker);

        if (undoSticker != null) undoSticker.setVisibility(View.INVISIBLE);
        if (redoSticker != null) redoSticker.setVisibility(View.INVISIBLE);
        if (originalSticker != null) originalSticker.setVisibility(View.INVISIBLE);*/

        StickerStore.get().init(requireContext().getApplicationContext());

        deleteStickerIcon.setEnabled(false);
        deleteStickerIcon.setAlpha(0.4f);

        LinearLayoutManager lm = new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false);
        myStickers.setLayoutManager(lm);
        adapter = new MyStickersAdapter(StickerStore.get().snapshotAll());
        myStickers.setAdapter(adapter);
        myStickers.setItemAnimator(null);

        adapter.setOnStickerClickListener((position, stickerKey) -> {
            selectStickerId = position;
            deleteStickerIcon.setEnabled(true);
            deleteStickerIcon.setAlpha(1.0f);
            showStickerCentered(stickerKey);
        });

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

        return view;
    }

    private void showStickerCentered(String stickerKey) {
        Controller.clearCurrentSticker(stickerOverlay, selectSticker);

        stickerFrame = inflater.inflate(R.layout.v_sticker_edit, stickerOverlay, false);
        stickerImage = stickerFrame.findViewById(R.id.stickerImage);
        moveController = stickerFrame.findViewById(R.id.moveController);
        rotateController = stickerFrame.findViewById(R.id.rotateController);
        sizeController = stickerFrame.findViewById(R.id.sizeController);
        deleteController = stickerFrame.findViewById(R.id.deleteController);

        rotateController.setVisibility(View.INVISIBLE);
        sizeController.setVisibility(View.INVISIBLE);

        File f = new File(stickerKey);
        if (f.exists()) {
            stickerImage.setImageURI(Uri.fromFile(f));
            if (stickerImage.getDrawable() == null) {
                stickerImage.setImageBitmap(BitmapFactory.decodeFile(f.getAbsolutePath()));
            }
        } else {
            int resId = getResources().getIdentifier(stickerKey, "drawable", requireContext().getPackageName());
            if (resId != 0) stickerImage.setImageResource(resId);
        }

        int sizePx = Controller.dp(230, getResources());
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(sizePx, sizePx);
        stickerFrame.setLayoutParams(lp);

        stickerOverlay.post(() -> {
            float cx = (stickerOverlay.getWidth() - sizePx) / 2f;
            float cy = (stickerOverlay.getHeight() - sizePx) / 2f;
            stickerFrame.setX(cx);
            stickerFrame.setY(cy);

            stickerOverlay.addView(stickerFrame);

            this.selectSticker = stickerFrame;

            Controller.setStickerActive(stickerFrame, true);

            stickerFrame.setOnClickListener(v -> moveEditSticker(stickerFrame));
        });

        deleteController.setOnClickListener(x -> {
            Controller.removeStickerFrame(stickerFrame);
            Controller.setControllersVisible(stickerFrame, false);
        });

        stickerFrame.bringToFront();
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
        viewModel.setTempView(stickerFrame);
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
            public void onKeep() {

            }

            @Override
            public void onDelete() {
                if (selectStickerId != RecyclerView.NO_POSITION) {
                    StickerItem toRemove = adapter.getItem(selectStickerId);
                    String key = toRemove.getImageUrl();

                    Controller.clearCurrentSticker(stickerOverlay, selectSticker);
                    StickerStore.get().removeByKey(key);
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