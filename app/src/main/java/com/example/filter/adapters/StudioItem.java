package com.example.filter.adapters;

import java.util.List;

public class StudioItem {
    private int studioImageResId;
    private String studioTitle;
    private String studioIntro;
    private List<Integer> previewImageCountList;

    public StudioItem(int studioImageResId, String studioTitle, String studioIntro, List<Integer> previewImageCountList) {
        this.studioImageResId = studioImageResId;
        this.studioTitle = studioTitle;
        this.studioIntro = studioIntro;
        this.previewImageCountList = previewImageCountList;
    }

    public int getStudioImageResId() {
        return studioImageResId;
    }

    public String getStudioTitle() {
        return studioTitle;
    }

    public String getStudioIntro() {
        return studioIntro;
    }

    public List<Integer> getPreviewImageCountList() {
        return previewImageCountList;
    }
}
