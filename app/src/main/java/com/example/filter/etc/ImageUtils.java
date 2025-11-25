package com.example.filter.etc;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class ImageUtils {
    public static void saveBitmapToGallery(Context context, Bitmap bitmap) {
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.DISPLAY_NAME, "my_filtered_image_" + System.currentTimeMillis() + ".png");
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/png");
        values.put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/Feel'em");

        ContentResolver resolver = context.getContentResolver();
        Uri uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);

        try {
            if (uri != null) {
                OutputStream outputStream = resolver.openOutputStream(uri);
                if (outputStream != null) {
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream);
                    outputStream.flush();
                    outputStream.close();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(context, "저장 실패", Toast.LENGTH_SHORT).show();
        }
    }

    public static String saveBitmapToCache(Context ctx, Bitmap bitmap) {
        File cacheDir = new File(ctx.getCacheDir(), "reviews");
        if (!cacheDir.exists()) cacheDir.mkdirs();

        File file = new File(cacheDir, "review_" + System.currentTimeMillis() + ".png");
        try (FileOutputStream out = new FileOutputStream(file)) {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
            out.flush();
            out.close();
            return file.getAbsolutePath();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
