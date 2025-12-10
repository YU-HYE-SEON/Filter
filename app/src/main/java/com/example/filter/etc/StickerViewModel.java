package com.example.filter.etc;

import android.util.SparseArray;
import android.view.View;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.filter.R;
import com.example.filter.api_datas.FaceStickerData;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StickerViewModel extends ViewModel {
    public static class StickerState {
        public float x, y, rotation;
        public int width, height;
    }
    private final SparseArray<ArrayList<View>> cloneGroups = new SparseArray<>();
    private final MutableLiveData<FaceStickerData> faceStickerLiveData = new MutableLiveData<>();
    private final MutableLiveData<Integer> faceStickerDataToDelete = new MutableLiveData<>();
    private final Map<View, StickerState> originalStates = new HashMap<>();

    public void addCloneGroup(int groupId, View cloneSticker) {
        ArrayList<View> list = cloneGroups.get(groupId);
        if (list == null) {
            list = new ArrayList<>();
            cloneGroups.put(groupId, list);
        }
        if (!list.contains(cloneSticker)) {
            cloneSticker.setTag(R.id.tag_sticker_clone, groupId);
            list.add(cloneSticker);
        }
    }

    public List<View> getCloneGroup(int groupId) {
        ArrayList<View> list = cloneGroups.get(groupId);
        if (list == null) return Collections.emptyList();
        return new ArrayList<>(list);
    }

    public void setFaceStickerData(FaceStickerData data) {
        faceStickerLiveData.postValue(data);
    }

    public LiveData<FaceStickerData> getFaceStickerData() {
        return faceStickerLiveData;
    }

    public void setFaceStickerDataToDelete(int groupId) {
        faceStickerDataToDelete.setValue(groupId);
    }

    public LiveData<Integer> getFaceStickerDataToDelete() {
        return faceStickerDataToDelete;
    }

    public void saveOriginalState(View v, StickerState s) {
        originalStates.put(v, s);
    }

    public StickerState getOriginalState(View v) {
        return originalStates.get(v);
    }
}