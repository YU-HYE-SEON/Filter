package com.example.filter.items;

public class OnBoardingItem {
    private boolean isSelected;
    private int imageResId;

    public OnBoardingItem(int imageResId) {
        this.imageResId = imageResId;
        this.isSelected = false;
    }

    public boolean isSelected() {
        return isSelected;
    }

    public void setSelected(boolean selected) {
        isSelected = selected;
    }

    public int getImageResId() {
        return imageResId;
    }
}