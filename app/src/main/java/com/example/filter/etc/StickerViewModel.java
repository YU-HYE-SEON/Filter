package com.example.filter.etc;

import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.lifecycle.ViewModel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StickerViewModel extends ViewModel {
    private View stickerFrame = null;
    private final List<View> stickerList = new ArrayList<>();
    private final Map<Integer, Boolean> faceModeMap = new HashMap<>();
    private final Map<Integer, List<View>> cloneGroups = new HashMap<>();

    public void setTempView(View stickerFrame) {
        this.stickerFrame = stickerFrame;
    }

    public void addSticker(View view) {
        if (view != null) stickerList.add(view);
    }

    public void addGroupSticker(int stickerId, View stickerFrame) {
        cloneGroups.computeIfAbsent(stickerId, k -> new ArrayList<>()).add(stickerFrame);
    }

    public View getTempView() {
        return stickerFrame;
    }


    public List<View> getStickers() {
        return stickerList;
    }

    public List<View> getGroup(int stickerId) {
        return cloneGroups.getOrDefault(stickerId, new ArrayList<>());
    }

    public void removeSticker(View view) {
        stickerList.remove(view);
    }

    public void removeGroup(int stickerId, FrameLayout stickerOverlay) {
        List<View> group = cloneGroups.get(stickerId);
        if (group != null) {
            for (View v : group) {
                if (v.getParent() == stickerOverlay) {
                    ((ViewGroup) v.getParent()).removeView(v);
                }
                stickerList.remove(v);
            }
            cloneGroups.remove(stickerId);
        }
    }

    public void clearAll() {
        stickerList.clear();
        cloneGroups.clear();
    }

    // ✅ 스티커별 얼굴모드 상태 저장/조회
    public void setFaceMode(int stickerId, boolean isFaceMode) {
        faceModeMap.put(stickerId, isFaceMode);
    }

    public boolean isFaceMode(int stickerId) {
        Boolean val = faceModeMap.get(stickerId);
        return val != null && val;
    }
}
