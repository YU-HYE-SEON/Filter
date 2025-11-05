package com.example.filter.etc;

import android.graphics.Bitmap;

import java.io.Serializable;

public class FaceStickerData implements Serializable {
    public float relX;
    public float relY;
    public float relW;
    public float relH;
    public float stickerR;
    public String batchId;
    public transient Bitmap stickerBitmap;
    public String stickerPath;

    public FaceStickerData(float relX, float relY, float relW, float relH, float stickerR, String batchId, Bitmap stickerBitmap, String stickerPath) {
        this.relX = relX;
        this.relY = relY;
        this.relW = relW;
        this.relH = relH;
        this.stickerR = stickerR;
        this.batchId = batchId;
        this.stickerBitmap = stickerBitmap;
        this.stickerPath = stickerPath;
    }
}