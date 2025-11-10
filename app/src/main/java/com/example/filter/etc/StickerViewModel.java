package com.example.filter.etc;

import android.util.Log;
import android.util.SparseArray;
import android.view.View;
import android.view.ViewGroup;

import androidx.lifecycle.ViewModel;

import com.example.filter.R;
import com.example.filter.fragments.EditStickerFragment;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class StickerViewModel extends ViewModel {
    private final SparseArray<View> tempViews = new SparseArray<>();
    private final SparseArray<ArrayList<View>> cloneGroups = new SparseArray<>();

    public void setTempView(int groupId, View stickerFrame) {
        tempViews.put(groupId, stickerFrame);
    }

    public View getTempView(int groupId) {
        return tempViews.get(groupId);
    }

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

        //StringBuilder sb = new StringBuilder();
        //for (View v : list) {
        //    sb.append(v.hashCode()).append(" ");
        //}
        //int seesionId = EditStickerFragment.sessionId;
        //Log.d("스티커", String.format("[세션ID = %d] | [클론스티커ID = %d] | 개수 = %d, 구성 = %s", seesionId, groupId, list.size(), sb.toString()));
    }

    public List<View> getCloneGroup(int groupId) {
        ArrayList<View> list = cloneGroups.get(groupId);
        if (list == null) return Collections.emptyList();
        return new ArrayList<>(list);
    }

    public void removeCloneGroup(int groupId, ViewGroup parent) {
        ArrayList<View> list = cloneGroups.get(groupId);
        if (list == null) return;

        if (parent != null) {
            for (View v : list) {
                if (v != null && v.getParent() == parent) {
                    parent.removeView(v);
                }
            }
        }
        list.clear();
        cloneGroups.remove(groupId);
    }
}