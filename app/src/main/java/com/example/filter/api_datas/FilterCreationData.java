package com.example.filter.api_datas;

import android.os.Parcel;
import android.os.Parcelable;

import com.example.filter.api_datas.request_dto.FilterDtoCreateRequest;
// DTO 내부 클래스 import
import com.example.filter.api_datas.request_dto.FilterDtoCreateRequest.ColorAdjustments;
import com.example.filter.api_datas.request_dto.FilterDtoCreateRequest.FaceSticker;

import java.util.ArrayList;
import java.util.List;

public class FilterCreationData implements Parcelable {

    // --- 기본 정보 ---
    public String name;
    public Integer price;
    public List<String> tags;

    // --- 이미지 경로 (앱 내에서는 로컬 경로, 전송 직전 URL로 교체됨) ---
    public String originalImageUrl;
    public String editedImageUrl;
    public String stickerImageNoFaceUrl;

    public Integer aspectX;
    public Integer aspectY;

    // --- DTO 내부 클래스 재사용 (Serializable) ---
    public ColorAdjustments colorAdjustments;

    // [수정됨] Sticker -> FaceSticker
    public List<FaceSticker> stickers;


    // --- 생성자 ---
    public FilterCreationData() {
        tags = new ArrayList<>();
        stickers = new ArrayList<>();
        colorAdjustments = new ColorAdjustments();
    }

    // --- 서버 전송용 DTO 변환 헬퍼 메서드 ---
    public FilterDtoCreateRequest toDto() {
        FilterDtoCreateRequest request = new FilterDtoCreateRequest();

        request.name = this.name;
        request.price = this.price;
        request.tags = this.tags;

        request.originalImageUrl = this.originalImageUrl;
        request.editedImageUrl = this.editedImageUrl;
        request.stickerImageNoFaceUrl = this.stickerImageNoFaceUrl;

        request.aspectX = this.aspectX;
        request.aspectY = this.aspectY;

        request.colorAdjustments = this.colorAdjustments;

        // ✅ [수정] stickers가 null이면 빈 리스트로 초기화하여 NullPointerException 방지
        if (this.stickers != null) {
            request.stickers = this.stickers;
        } else {
            request.stickers = new ArrayList<>(); // 빈 리스트 할당
        }

        return request;
    }


    // ---------------------------------------------------------
    // Parcelable 구현부
    // ---------------------------------------------------------
    protected FilterCreationData(Parcel in) {
        name = in.readString();

        // Integer (Nullable) 읽기
        if (in.readByte() == 0) {
            price = null;
        } else {
            price = in.readInt();
        }

        tags = in.createStringArrayList();
        originalImageUrl = in.readString();
        editedImageUrl = in.readString();
        stickerImageNoFaceUrl = in.readString();

        if (in.readByte() == 0) {
            aspectX = null;
        } else {
            aspectX = in.readInt();
        }

        if (in.readByte() == 0) {
            aspectY = null;
        } else {
            aspectY = in.readInt();
        }

        // Serializable 객체 읽기
        colorAdjustments = (ColorAdjustments) in.readSerializable();

        // [수정됨] FaceSticker로 캐스팅 (명시적 경로 사용 권장)
        stickers = (ArrayList<FaceSticker>) in.readSerializable();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(name);

        // Integer (Nullable) 쓰기
        if (price == null) {
            dest.writeByte((byte) 0);
        } else {
            dest.writeByte((byte) 1);
            dest.writeInt(price);
        }

        dest.writeStringList(tags);
        dest.writeString(originalImageUrl);
        dest.writeString(editedImageUrl);
        dest.writeString(stickerImageNoFaceUrl);

        if (aspectX == null) {
            dest.writeByte((byte) 0);
        } else {
            dest.writeByte((byte) 1);
            dest.writeInt(aspectX);
        }

        if (aspectY == null) {
            dest.writeByte((byte) 0);
        } else {
            dest.writeByte((byte) 1);
            dest.writeInt(aspectY);
        }

        // Serializable 객체 쓰기
        dest.writeSerializable(colorAdjustments);

        // List<FaceSticker> 쓰기 (ArrayList 캐스팅)
        dest.writeSerializable((ArrayList<FaceSticker>) stickers);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<FilterCreationData> CREATOR = new Creator<FilterCreationData>() {
        @Override
        public FilterCreationData createFromParcel(Parcel in) {
            return new FilterCreationData(in);
        }

        @Override
        public FilterCreationData[] newArray(int size) {
            return new FilterCreationData[size];
        }
    };
}