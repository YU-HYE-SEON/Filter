package com.example.filter.etc;

import java.io.Serializable;
import java.util.List;

public class FilterDtoCreateRequest {
    public String name;
    public int price;
    public String originalImageUrl;
    public String editedImageUrl;
    public int aspectX;
    public int aspectY;
    public ColorAdjustments colorAdjustments = new ColorAdjustments();
    public List<String> tags;
    public List<Sticker> stickers;

    public static class ColorAdjustments implements Serializable {
        public float brightness = 1.0f;
        public float exposure = 1.0f;
        public float contrast = 1.0f;
        public float highlight = 1.0f;
        public float shadow = 1.0f;
        public float temperature = 1.0f;
        public float hue = 1.0f;
        public float saturation = 1.0f;
        public float sharpen = 0.0f;
        public float blur = 0.0f;
        public float vignette = 0.0f;
        public float noise = 0.0f;
    }

    public static class Sticker implements Serializable {
        public int stickerId;
        public String placementType;
        public float scale;
        public Float rotation;
        public Float x;
        public Float y;
        public String anchor;
    }
}
