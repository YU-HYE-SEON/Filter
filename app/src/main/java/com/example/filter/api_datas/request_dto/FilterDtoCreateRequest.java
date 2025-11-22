package com.example.filter.api_datas.request_dto;

import java.io.Serializable;
import java.util.List;

public class FilterDtoCreateRequest implements Serializable {
    public String name;
    public Integer price; // int -> Integer (null 처리를 위해 변경 권장, int 유지 가능)
    public List<String> tags;

    public String originalImageUrl;
    public String editedImageUrl;
    public String stickerImageNoFaceUrl; // [추가됨] 백엔드에 새로 생긴 필드

    public Integer aspectX;
    public Integer aspectY;

    // [유지] 백엔드는 Map<String, Double>이지만,
    // JSON 직렬화 시 이 객체의 필드명("brightness" 등)이 Map의 Key가 되므로 호환됩니다.
    public ColorAdjustments colorAdjustments = new ColorAdjustments();

    public List<FaceSticker> faceStickers;

    // [유지] 기존 UI 코드 수정 최소화를 위해 클래스 구조 유지
    public static class ColorAdjustments implements Serializable {
        // 백엔드는 Double을 기대하므로 정밀도를 위해 float -> double 변경 권장
        // (기존 코드를 건드리기 싫다면 float 유지해도 JSON 전송엔 문제없으나 값 오차 주의)
        public double brightness = 0.0; // 백엔드 기본값 확인 필요 (보통 0.0 or 1.0)
        public double exposure = 0.0;
        public double contrast = 0.0;
        public double highlight = 0.0;
        public double shadow = 0.0;
        public double temperature = 0.0;
        public double hue = 0.0;
        public double saturation = 0.0;
        public double sharpen = 0.0;
        public double blur = 0.0;
        public double vignette = 0.0;
        public double noise = 0.0;
    }

    // [변경 불가피] 백엔드의 FaceSticker 구조에 맞춰 필드 수정
    public static class FaceSticker implements Serializable {
        public Long stickerId; // int -> Long 변경

        // 기존 placementType, scale, anchor 등은 백엔드에서 제거됨
        // 백엔드 FaceSticker 규격에 맞춘 좌표계
        public double relX;
        public double relY;
        public double relW;
        public double relH;
        public double rot;

        // 생성자 (필요시 사용)
        public FaceSticker() {}

        public FaceSticker(Long stickerId, double relX, double relY, double relW, double relH, double rot) {
            this.stickerId = stickerId;
            this.relX = relX;
            this.relY = relY;
            this.relW = relW;
            this.relH = relH;
            this.rot = rot;
        }
    }
}