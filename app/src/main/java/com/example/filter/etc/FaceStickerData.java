package com.example.filter.etc;

import android.graphics.Bitmap;

import java.io.Serializable;

public class FaceStickerData implements Serializable {
    public float relX;
    public float relY;
    public float relW;
    public float relH;
    public float rot;
    public int groupId;
    public transient Bitmap stickerBitmap;
    public String stickerPath;

    public FaceStickerData(float relX, float relY, float relW, float relH, float rot, int groupId, Bitmap stickerBitmap, String stickerPath) {
        this.relX = relX;
        this.relY = relY;
        this.relW = relW;
        this.relH = relH;
        this.rot = rot;
        this.groupId = groupId;
        this.stickerBitmap = stickerBitmap;
        this.stickerPath = stickerPath;
    }
}