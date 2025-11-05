package com.example.filter.etc;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.HashMap;
import java.util.Map;

public class FaceModeViewModel extends ViewModel {
    private final Map<String, MutableLiveData<Boolean>> stickerStates = new HashMap<>();

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
}
