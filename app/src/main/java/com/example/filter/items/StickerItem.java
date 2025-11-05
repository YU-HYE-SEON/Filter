package com.example.filter.items;

import androidx.annotation.Nullable;
import org.json.JSONException;
import org.json.JSONObject;

public class StickerItem {
    private final long id; // 서버 ID (-1이면 로컬 전용)
    private final @Nullable String resName;
    private final @Nullable String filePath;
    private final StickerType type;

    public StickerItem(long id, @Nullable String resName, @Nullable String filePath, StickerType type) {
        this.id = id;
        this.resName = resName;
        this.filePath = filePath;
        this.type = type;
    }

    // ✅ 로컬 파일에서 새 스티커 생성 (서버 업로드 전)
    public static StickerItem fromFile(String filePath, StickerType type) {
        return new StickerItem(-1, null, filePath, type);
    }

    // ✅ 리소스에서 불러오는 스티커 (앱 내 기본 스티커)
    public static StickerItem fromRes(String resName, StickerType type) {
        return new StickerItem(-1, resName, null, type);
    }

    // ✅ Getter
    public long getId() { return id; }

    @Nullable
    public String getResName() { return resName; }

    @Nullable
    public String getImageUrl() { return filePath; }

    public String getType() { return type.name(); }

    // ✅ isFile() 복원: 파일 기반 스티커인지 구분
    public boolean isFile() {
        return filePath != null && !filePath.isEmpty();
    }

    public boolean isResource() {
        return resName != null && !resName.isEmpty();
    }

    public String key() {
        return isFile() ? filePath : resName;
    }

    // ✅ JSON 직렬화
    public JSONObject toJson() throws JSONException {
        JSONObject o = new JSONObject();
        o.put("id", id);
        if (resName != null) o.put("res", resName);
        if (filePath != null) o.put("file", filePath);
        o.put("type", type.name());
        return o;
    }

    // ✅ JSON 역직렬화
    public static StickerItem fromJson(JSONObject o) {
        long id = o.optLong("id", -1);
        String r = o.optString("res", null);
        String f = o.optString("file", null);
        String t = o.optString("type", "IMAGE");

        StickerType typeEnum;
        try {
            typeEnum = StickerType.valueOf(t);
        } catch (IllegalArgumentException e) {
            typeEnum = StickerType.IMAGE;
        }

        if (f != null && !f.isEmpty()) return new StickerItem(id, null, f, typeEnum);
        if (r != null && !r.isEmpty()) return new StickerItem(id, r, null, typeEnum);
        return null;
    }
}
