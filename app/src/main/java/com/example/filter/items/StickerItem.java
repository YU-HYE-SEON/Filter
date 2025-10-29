    package com.example.filter.items;

    import androidx.annotation.Nullable;

    import org.json.JSONException;
    import org.json.JSONObject;

    public class StickerItem {
        public final @Nullable String resName;
        public final @Nullable String filePath;

        private StickerItem(@Nullable String resName, @Nullable String filePath) {
            this.resName = resName;
            this.filePath = filePath;
        }

        public static StickerItem fromRes(String resName) {
            return new StickerItem(resName, null);
        }

        public static StickerItem fromFile(String filePath) {
            return new StickerItem(null, filePath);
        }

        public boolean isFile() {
            return filePath != null;
        }

        public String key() {
            return isFile() ? filePath : resName;
        }

        public JSONObject toJson() throws JSONException {
            JSONObject o = new JSONObject();
            if (resName != null) o.put("res", resName);
            if (filePath != null) o.put("file", filePath);
            return o;
        }

        public static StickerItem fromJson(JSONObject o) {
            String r = o.optString("res", null);
            String f = o.optString("file", null);
            if (f != null && !f.isEmpty()) return fromFile(f);
            if (r != null && !r.isEmpty()) return fromRes(r);
            return null;
        }
    }