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
                        //Log.d("얼굴인식", "========================= photoPreview 속 Face [" + (i + 1) + "] =========================");
                        //getFaceLandmarks(face);
                        //getFaceContours(face);
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

    /*private static void logFaceLandmark(Face face, String label, int type) {
        FaceLandmark lm = face.getLandmark(type);
        if (lm == null) {
            Log.d("얼굴인식", label + " : 인식 실패");
            return;
        }
        PointF p = lm.getPosition();
        Log.d("얼굴인식", label + " : (" + p.x + ", " + p.y + ")");
    }

    private static void getFaceLandmarks(Face face) {
        logFaceLandmark(face, "LEFT_EYE : ", FaceLandmark.LEFT_EYE);
        logFaceLandmark(face, "RIGHT_EYE : ", FaceLandmark.RIGHT_EYE);
        logFaceLandmark(face, "NOSE_BASE : ", FaceLandmark.NOSE_BASE);
        logFaceLandmark(face, "MOUTH_BOTTOM : ", FaceLandmark.MOUTH_BOTTOM);
        logFaceLandmark(face, "LEFT_CHEEK : ", FaceLandmark.LEFT_CHEEK);
        logFaceLandmark(face, "RIGHT_CHEEK : ", FaceLandmark.RIGHT_CHEEK);
    }

    private static void logFaceContour(Face face, String label, int type) {
        FaceContour contour = face.getContour(type);
        if (contour == null || contour.getPoints() == null || contour.getPoints().isEmpty()) {
            Log.d("얼굴인식", label + " : 인식 실패");
            return;
        }
        List<PointF> points = contour.getPoints();
        StringBuilder sb = new StringBuilder();
        sb.append(label).append("[");
        for (int i = 0; i < points.size(); i++) {
            PointF p = points.get(i);
            sb.append(String.format("(%.1f, %.1f)", p.x, p.y));
            if (i < points.size() - 1) sb.append(", ");
        }
        sb.append("]");
        Log.d("얼굴인식", sb.toString());
    }

    private static void getFaceContours(Face face) {
        logFaceContour(face, "FaceContour : ", FaceContour.FACE);
    }*/
}
