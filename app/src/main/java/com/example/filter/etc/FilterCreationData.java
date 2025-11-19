package com.example.filter.etc;

import android.os.Parcel;
import android.os.Parcelable; // intent 전달을 위한 직렬화 방식

import com.example.filter.apis.dto.FilterDtoCreateRequest;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class FilterCreationData implements Parcelable {

    // --- 이미지 정보 ---
    public String originalImagePath;
    public String finalImagePath;

    // --- 필터 메타 정보 ---
    public String filterName;
    public boolean isFree;
    public int price;
    public List<String> tags;

    // --- 이미지 편집 정보 ---
    public float cropL, cropT, cropR, cropB;
    public int aspectX;
    public int aspectY;

    public int rotationDeg;
    public boolean flipH, flipV;

    // --- 스티커 이미지 경로 / 여러 개 지원 ---
    public ArrayList<String> brushImagePaths;   // 여러 개 가능
    public ArrayList<String> freeStickerImagePaths;

    public String stickerImageNoFacePath;
    public String stickerRawImagePath; // 얼굴 인식 전 원본 스티커

    // --- 실제 서버에 전달할 스티커 정보 ---
    public ArrayList<StickerPlacementData> stickerPlacements;

    // --- 색보정 값 ---
    public FilterDtoCreateRequest.ColorAdjustments colorAdjustments;

    // --- 얼굴 인식 스티커 ---
    public ArrayList<FaceStickerData> faceStickers;

    // --- 기본 생성자 ---
    public FilterCreationData() {
        brushImagePaths = new ArrayList<>();
        freeStickerImagePaths = new ArrayList<>();
        stickerPlacements = new ArrayList<>();
        faceStickers = new ArrayList<>();
        tags = new ArrayList<>();
    }

    // --- Parcel → 객체 복원 ---
    protected FilterCreationData(Parcel in) {
        originalImagePath = in.readString();
        finalImagePath = in.readString();

        filterName = in.readString();
        isFree = in.readByte() != 0;
        price = in.readInt();
        tags = in.createStringArrayList();

        cropL = in.readFloat();
        cropT = in.readFloat();
        cropR = in.readFloat();
        cropB = in.readFloat();

        aspectX = in.readInt();
        aspectY = in.readInt();

        rotationDeg = in.readInt();
        flipH = in.readByte() != 0;
        flipV = in.readByte() != 0;

        brushImagePaths = in.createStringArrayList();
        freeStickerImagePaths = in.createStringArrayList();

        stickerImageNoFacePath = in.readString();
        stickerRawImagePath = in.readString();

        colorAdjustments = (FilterDtoCreateRequest.ColorAdjustments) in.readSerializable();
        faceStickers = (ArrayList<FaceStickerData>) in.readSerializable();
        stickerPlacements = (ArrayList<StickerPlacementData>) in.readSerializable();
    }


    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(originalImagePath);
        dest.writeString(finalImagePath);

        dest.writeString(filterName);
        dest.writeByte((byte) (isFree ? 1 : 0));
        dest.writeInt(price);
        dest.writeStringList(tags);

        dest.writeFloat(cropL);
        dest.writeFloat(cropT);
        dest.writeFloat(cropR);
        dest.writeFloat(cropB);

        dest.writeInt(aspectX);
        dest.writeInt(aspectY);

        dest.writeInt(rotationDeg);
        dest.writeByte((byte) (flipH ? 1 : 0));
        dest.writeByte((byte) (flipV ? 1 : 0));

        dest.writeStringList(brushImagePaths);
        dest.writeStringList(freeStickerImagePaths);

        dest.writeString(stickerImageNoFacePath);
        dest.writeString(stickerRawImagePath);

        dest.writeSerializable(colorAdjustments);
        dest.writeSerializable(faceStickers);
        dest.writeSerializable(stickerPlacements);
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

    // --- 스티커 placement 구조 ---
    public static class StickerPlacementData implements Serializable {
        public long stickerId;
        public String placementType;
        public float x, y, scale, rotation;
        public String anchor;
    }
}


