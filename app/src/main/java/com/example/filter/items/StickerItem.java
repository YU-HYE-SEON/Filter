package com.example.filter.items;

import androidx.annotation.Nullable;
import org.json.JSONException;
import org.json.JSONObject;

public class StickerItem {
    private final @Nullable String resName;
    private final @Nullable String filePath;
    private final StickerType type; // 서버/로직에서 쓰는 타입 (IMAGE / BRUSH / AI 등)

    private StickerItem(@Nullable String resName, @Nullable String filePath, StickerType type) {
        this.resName = resName;
        this.filePath = filePath;
        this.type = (type != null) ? type : StickerType.IMAGE;
    }

    // === 팩토리 ===
    public static StickerItem fromFile(String filePath, StickerType type) {
        return new StickerItem(null, filePath, type);
    }

    // === Getter ===
    @Nullable
    public String getResName() {
        return resName;
    }

    @Nullable
    public String getImageUrl() {
        return filePath;
    }

    public String getType() {
        return type.name();
    }

    public boolean isFile() {
        return filePath != null;
    }

    public String key() {
        return isFile() ? filePath : resName;
    }

    // === JSON 직렬화 ===
    public JSONObject toJson() throws JSONException {
        JSONObject o = new JSONObject();
        if (resName != null) o.put("res", resName);
        if (filePath != null) o.put("file", filePath);
        o.put("type", type.name()); // Enum을 문자열로 저장
        return o;
    }

    // === JSON 역직렬화 ===
    public static StickerItem fromJson(JSONObject o) {
        String r = o.optString("res", null);
        String f = o.optString("file", null);
        String t = o.optString("type", "IMAGE"); // 기본값 IMAGE

        StickerType type;
        try {
            type = StickerType.valueOf(t.toUpperCase());
        } catch (IllegalArgumentException e) {
            type = StickerType.IMAGE; // 안전한 fallback
        }

        if (f != null && !f.isEmpty()) return new StickerItem(null, f, type);
        if (r != null && !r.isEmpty()) return new StickerItem(r, null, type);
        return null;
    }
}
