package com.example.filter.api_datas.dto;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

public class FilterResponse implements Serializable {
    public Boolean isMine;

    public Long id;
    public String name;
    public String creator;
    public Integer price;
    public List<String> tags;

    public String originalImageUrl;
    public String editedImageUrl;
    public String stickerImageNoFaceUrl;

    public Integer aspectX;
    public Integer aspectY;

    public Map<String, Double> colorAdjustments;

    // 내부 클래스 리스트 사용
    public List<FaceStickerResponse> stickers;

    public Boolean isDeleted;
    public Long saveCount;
    public Long useCount;

    // ✅ [내부 클래스] 스티커 응답 정보
    public static class FaceStickerResponse implements Serializable {
        public Long stickerId;
        public String stickerImageUrl;
        public double relX;
        public double relY;
        public double relW;
        public double relH;
        public double rot;
    }
}