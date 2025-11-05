// Sticker Store가 직접 Retrofit을 사용하지 않고 StickerUploader 인터페이스를 통해 스티커 업로드 기능을 추상화

package com.example.filter.apis.repositories;

import com.example.filter.items.StickerItem;

public interface StickerUploader {
    void uploadToServer(StickerItem item);
}
