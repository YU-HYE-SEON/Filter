package com.example.filter.etc;

import android.graphics.Bitmap;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.HashMap;
import java.util.Map;

public class FaceModeViewModel extends ViewModel {
    private final Map<String, MutableLiveData<Boolean>> stickerStates = new HashMap<>();
    private Bitmap tempBitmap;
    private final MutableLiveData<FaceStickerData> faceStickerLiveData = new MutableLiveData<>();

    public LiveData<Boolean> getStickerState(String stickerId) {
        if (!stickerStates.containsKey(stickerId)) {
            stickerStates.put(stickerId, new MutableLiveData<>(false));
        }
        return stickerStates.get(stickerId);
    }

    public void setStickerState(String stickerId, boolean checked) {
        if (!stickerStates.containsKey(stickerId)) {
            stickerStates.put(stickerId, new MutableLiveData<>(checked));
        } else {
            stickerStates.get(stickerId).setValue(checked);
        }
    }

    public void setTempBitmap(Bitmap bitmap) {
        this.tempBitmap = bitmap;
    }

    public Bitmap getTempBitmap() {
        return this.tempBitmap;
    }

    public void clearTempBitmap() {
        this.tempBitmap = null;
    }

    public void setFaceStickerData(FaceStickerData data) {
        faceStickerLiveData.postValue(data);
    }

    public LiveData<FaceStickerData> getFaceStickerData() {
        return faceStickerLiveData;
    }
}
