package com.example.filter.etc;

import android.graphics.Bitmap;
import android.graphics.Rect;
import android.view.View;

import com.example.filter.overlayviews.FaceBoxOverlayView;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;

import java.util.ArrayList;
import java.util.List;

public class FaceDetect {
    public interface FaceDetectionCallback {
        void onFacesDetected(List<Face> faces, Bitmap originalBitmap);
    }

    public static void detectFaces(Bitmap bitmap, FaceBoxOverlayView faceBox, FaceDetectionCallback callback) {
        if (bitmap == null) {
            if (callback != null) {
                callback.onFacesDetected(new ArrayList<>(), null);
            }
            return;
        }

        InputImage image = InputImage.fromBitmap(bitmap, 0);

        FaceDetectorOptions options = new FaceDetectorOptions.Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
                .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
                .setContourMode(FaceDetectorOptions.CONTOUR_MODE_ALL)
                .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
                .build();

        FaceDetector detector = FaceDetection.getClient(options);

        detector.process(image)
                .addOnSuccessListener(faces -> {
                    List<Rect> rects = new ArrayList<>();

                    for (int i = 0; i < faces.size(); i++) {
                        Face face = faces.get(i);
                        rects.add(face.getBoundingBox());
                    }

                    if (faceBox != null) {
                        if (!faces.isEmpty()) {
                            faceBox.setVisibility(View.VISIBLE);
                            faceBox.setFaceBoxes(rects, bitmap.getWidth(), bitmap.getHeight());
                        } else {
                            faceBox.clearBoxes();
                            faceBox.setVisibility(View.GONE);
                        }
                    }

                    if (callback != null) {
                        callback.onFacesDetected(faces, bitmap);
                    }

                    detector.close();
                })
                .addOnFailureListener(e -> {
                    if (faceBox != null) {
                        faceBox.clearBoxes();
                        faceBox.setVisibility(View.GONE);
                    }
                    if (callback != null) {
                        callback.onFacesDetected(new ArrayList<>(), bitmap);
                    }
                    detector.close();
                });
    }
}
