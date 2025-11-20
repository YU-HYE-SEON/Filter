package com.example.filter.api_datas;

import android.graphics.Bitmap;
import java.io.Serializable;

public class FaceStickerData implements Serializable {

    // 위치 및 회전 정보
    public float relX;
    public float relY;
    public float relW;
    public float relH;
    public float rot;

    // 로컬 식별용 그룹 ID (클론 스티커 관리용)
    public int groupId;

    // ★ [추가됨] 서버 DB에 저장된 실제 스티커 ID
    public long stickerDbId;

    // 이미지 데이터
    // (Bitmap은 직렬화가 불가능하므로 transient 키워드를 붙이거나,
    // Intent 전달 시 제외되고 파일 경로(stickerPath)를 통해 복구됩니다.)
    public transient Bitmap stickerBitmap;
    public String stickerPath;

    // 기본 생성자
    public FaceStickerData() {
    }

    // 전체 데이터를 받는 생성자
    public FaceStickerData(float relX, float relY, float relW, float relH, float rot,
                           int groupId, long stickerDbId, // ✅ stickerDbId 추가됨
                           Bitmap stickerBitmap, String stickerPath) {
        this.relX = relX;
        this.relY = relY;
        this.relW = relW;
        this.relH = relH;
        this.rot = rot;
        this.groupId = groupId;
        this.stickerDbId = stickerDbId; // ✅ 저장
        this.stickerBitmap = stickerBitmap;
        this.stickerPath = stickerPath;
    }
}