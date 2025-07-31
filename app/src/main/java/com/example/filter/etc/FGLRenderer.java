package com.example.filter.etc;

import android.content.Context;
import android.graphics.Bitmap;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.GLUtils;

import com.example.filter.R;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class FGLRenderer implements GLSurfaceView.Renderer {
    private final Context context;  //리소스 접근 위해 사용
    private GLSurfaceView glSurfaceView;    //화면에 그려지는 OpenGL의 뷰 (현재 불러온 사진 이미지 + 필터 적용)
    private Bitmap bitmap;  //렌더링할 이미지, openGL의 텍스쳐로 변환
    private int viewportX, viewportY, viewportWidth, viewportHeight;
    private int textureId = 0;  //텍스쳐 객체 고유 ID 참조용, 0:생성한 텍스쳐 없음
    private int program;    //쉐이더 프로그램 ID, 쉐이더 프로그램 = (버텍스 쉐이더(정점 위치 계산) + 프래그먼트 쉐이더(픽셀 색상 계산))
    private int positionHandle, texCoordHandle; //버텍스 쉐이더의 변수들 ID 참조용 핸들

    //프래그먼트 쉐이더의 변수들 ID 참조용 핸들
    private int textureHandle, brightnessHandle, exposureHandle, contrastHandle,
            highlightHandle, shadowHandle, temperatureHandle, tintHandle,
            saturationHandle, sharpnessHandle, blurHandle,
            vignetteHandle, noiseHandle, resolutionHandle;
    private final FloatBuffer vertexBuffer, texCoordBuffer; //버텍스 위치/텍스쳐 좌표 데이터를 GPU에 전달할 때 사용하는 버퍼
    private float imageAspectRatio; //사진 이미지 비율

    //최종 적용 조절값
    private float updateBrightness = 0f, updateExposure = 0f, updateContrast = 1.0f, updateHighlight = 0f,
            updateShadow = 0f, updateTemperature = 0f, updateTint = 0f, updateSaturation = 1.0f,
            updateSharpness = 0f, updateBlur = 0f, updateVignette = 0f, updateNoise = 0f;

    //최종 적용 전 실시간 미리보기용 조절값 (onDrawFrame에서 사용)
    private float tempBrightness = 0f, tempExposure = 0f, tempContrast = 1.0f, tempHighlight = 0f,
            tempShadow = 0f, tempTemperature = 0f, tempTint = 0f, tempSaturation = 1.0f,
            tempSharpness = 0f, tempBlur = 0f, tempVignette = 0f, tempNoise = 0f;

    //화면(사각형)을 그릴 정점 좌표 (x,y)
    //openGL의 정점 좌표는 가운데가 (0,0) / 왼쪽 아래가 (-1,-1) / 오른쪽 위가 (1,1)
    private final float[] vertices = {
            -1.0f, -1.0f,   //왼쪽 아래
            1.0f, -1.0f,    //오른쪽 아래
            -1.0f, 1.0f,    //왼쪽 위
            1.0f, 1.0f      //오른쪽 위
    };

    //텍스쳐(이미지)의 어느 부분을 정점에 매핑할지에 대한 좌표 (s,t)
    //openGL의 텍스쳐 좌표는 왼쪽 아래가 (0,0) / 오른쪽 위가 (1,1)
    //안드로이드 비트맵 좌표는 왼쪽 위가 (0,0) / 오른쪽 아래가 (1,1)
    //안드로이드 비트맵 좌표와 텍스쳐 좌표는 상하가 반대
    //→ 텍스쳐 t값 반대로 생각해야 함
    private final float[] texCoords = {
            0.0f, 1.0f,     //왼쪽 위 → 왼쪽 아래
            1.0f, 1.0f,     //오른쪽 위 → 오른쪽 아래
            0.0f, 0.0f,     //왼쪽 아래 → 왼쪽 위
            1.0f, 0.0f      //오른쪽 아래 → 오른쪽 위
    };

    private float translateX = 0f, translateY = 0f;  //사진 드래그 좌표

    //사진 터치 드래그 좌표 계산
    public void setTranslate(float x, float y) {
        this.translateX = (x * 2f) / (float) viewportWidth;
        this.translateY = (y * 2f) / (float) viewportHeight;
    }

    private float scaleFactor = 1.0f;   //사진 확대/축소 비율

    //사진 확대/축소 비율 설정
    public void setScaleFactor(float factor) {
        this.scaleFactor = factor;
    }

    //사진 캡쳐를 위한 인터페이스
    public interface OnBitmapCaptureListener {
        void onBitmapCaptured(Bitmap bitmap);
    }

    //사진 캡쳐 리스너
    private OnBitmapCaptureListener listener;
    //사진 캡쳐 여부
    private boolean shouldCapture = false;

    public FGLRenderer(Context context, GLSurfaceView glSurfaceView) {
        this.context = context;
        this.glSurfaceView = glSurfaceView;

        ByteBuffer bb = ByteBuffer.allocateDirect(vertices.length * 4);
        bb.order(ByteOrder.nativeOrder());
        vertexBuffer = bb.asFloatBuffer();
        vertexBuffer.put(vertices);
        vertexBuffer.position(0);

        ByteBuffer cc = ByteBuffer.allocateDirect(texCoords.length * 4);
        cc.order(ByteOrder.nativeOrder());
        texCoordBuffer = cc.asFloatBuffer();
        texCoordBuffer.put(texCoords);
        texCoordBuffer.position(0);
    }

    private String loadShaderCodeFromRawResource(int resourceId) {
        InputStream inputStream = context.getResources().openRawResource(resourceId);
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        StringBuilder shaderCode = new StringBuilder();
        String line;
        try {
            while ((line = reader.readLine()) != null) shaderCode.append(line).append("\n");
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return shaderCode.toString();
    }

    public void setBitmap(Bitmap bitmap) {
        this.bitmap = bitmap;

        if (textureId != 0) {
            GLES20.glDeleteTextures(1, new int[]{textureId}, 0);
            textureId = 0;
        }

        if (bitmap != null) {
            imageAspectRatio = (float) bitmap.getWidth() / bitmap.getHeight();

            if (glSurfaceView != null) {
                glSurfaceView.queueEvent(() -> {
                    int w = glSurfaceView.getWidth();
                    int h = glSurfaceView.getHeight();
                    onSurfaceChanged(null, w, h);
                });

                glSurfaceView.requestRender();
            }
        }
    }

    public Bitmap getCurrentBitmap() {
        return this.bitmap;
    }

    @Override
    public void onSurfaceCreated(GL10 unused, EGLConfig config) {
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

        String vertexShaderCode = loadShaderCodeFromRawResource(R.raw.filter_vertex_shader);
        String fragmentShaderCode = loadShaderCodeFromRawResource(R.raw.filter_fragment_shader);

        int vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode);
        int fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode);

        program = GLES20.glCreateProgram();
        GLES20.glAttachShader(program, vertexShader);
        GLES20.glAttachShader(program, fragmentShader);
        GLES20.glLinkProgram(program);

        final int[] linkStatus = new int[1];
        GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, linkStatus, 0);
        if (linkStatus[0] == 0) {
            GLES20.glDeleteProgram(program);
            program = 0;
        }

        positionHandle = GLES20.glGetAttribLocation(program, "aPosition");
        texCoordHandle = GLES20.glGetAttribLocation(program, "aTexCoord");
        textureHandle = GLES20.glGetUniformLocation(program, "uTexture");
        brightnessHandle = GLES20.glGetUniformLocation(program, "uBrightness");
        exposureHandle = GLES20.glGetUniformLocation(program, "uExposure");
        contrastHandle = GLES20.glGetUniformLocation(program, "uContrast");
        highlightHandle = GLES20.glGetUniformLocation(program, "uHighlight");
        shadowHandle = GLES20.glGetUniformLocation(program, "uShadow");
        temperatureHandle = GLES20.glGetUniformLocation(program, "uTemperature");
        tintHandle = GLES20.glGetUniformLocation(program, "uTint");
        saturationHandle = GLES20.glGetUniformLocation(program, "uSaturation");
        sharpnessHandle = GLES20.glGetUniformLocation(program, "uSharpness");
        blurHandle = GLES20.glGetUniformLocation(program, "uBlur");
        vignetteHandle = GLES20.glGetUniformLocation(program, "uVignette");
        noiseHandle = GLES20.glGetUniformLocation(program, "uNoise");
        resolutionHandle = GLES20.glGetUniformLocation(program, "uResolution");
    }

    @Override
    public void onDrawFrame(GL10 unused) {
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);

        if (bitmap == null || program == 0) {
            return;
        }

        if (textureId == 0) {
            int[] textures = new int[1];
            GLES20.glGenTextures(1, textures, 0);
            textureId = textures[0];
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId);

            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);

            GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0);
        }

        if (textureId != 0) {
            GLES20.glUseProgram(program);

            float tx = translateX;
            float ty = translateY;

            float[] scaledVertices = new float[]{
                    -1.0f * scaleFactor + tx, -1.0f * scaleFactor + ty,
                    1.0f * scaleFactor + tx, -1.0f * scaleFactor + ty,
                    -1.0f * scaleFactor + tx, 1.0f * scaleFactor + ty,
                    1.0f * scaleFactor + tx, 1.0f * scaleFactor + ty
            };
            vertexBuffer.put(scaledVertices).position(0);

            GLES20.glEnableVertexAttribArray(positionHandle);
            GLES20.glVertexAttribPointer(positionHandle, 2, GLES20.GL_FLOAT, false, 0, vertexBuffer);
            GLES20.glEnableVertexAttribArray(texCoordHandle);
            GLES20.glVertexAttribPointer(texCoordHandle, 2, GLES20.GL_FLOAT, false, 0, texCoordBuffer);

            GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId);
            GLES20.glUniform1i(textureHandle, 0);

            GLES20.glUniform1f(brightnessHandle, tempBrightness);
            GLES20.glUniform1f(exposureHandle, tempExposure);
            GLES20.glUniform1f(contrastHandle, tempContrast);
            GLES20.glUniform1f(highlightHandle, tempHighlight);
            GLES20.glUniform1f(shadowHandle, tempShadow);
            GLES20.glUniform1f(temperatureHandle, tempTemperature);
            GLES20.glUniform1f(tintHandle, tempTint);
            GLES20.glUniform1f(saturationHandle, tempSaturation);
            GLES20.glUniform1f(sharpnessHandle, tempSharpness);
            GLES20.glUniform1f(blurHandle, tempBlur);
            GLES20.glUniform1f(vignetteHandle, tempVignette);
            GLES20.glUniform1f(noiseHandle, tempNoise);
            GLES20.glUniform2f(resolutionHandle, bitmap.getWidth(), bitmap.getHeight());

            GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);

            //사진 저장하고자 할 때 현재 이미지를 비트맵으로 변환시키는 메서드 호출 및 전달
            if (shouldCapture) {
                shouldCapture = false;
                Bitmap captured = createBitmapFromGLSurface(0, 0, glSurfaceView.getWidth(), glSurfaceView.getHeight());
                if (listener != null) {
                    glSurfaceView.post(() -> listener.onBitmapCaptured(captured));
                }
            }

            GLES20.glDisableVertexAttribArray(positionHandle);
            GLES20.glDisableVertexAttribArray(texCoordHandle);
        }
    }

    @Override
    public void onSurfaceChanged(GL10 unused, int width, int height) {
        float viewAspectRatio = (float) width / height;
        int viewportX = 0;
        int viewportY = 0;
        int viewportWidth = width;
        int viewportHeight = height;

        if (imageAspectRatio > 0) {
            if (imageAspectRatio > viewAspectRatio) {
                viewportWidth = width;
                viewportHeight = (int) (width / imageAspectRatio);
                viewportY = (height - viewportHeight) / 2;
                viewportX = 0;
            } else {
                viewportHeight = height;
                viewportWidth = (int) (height * imageAspectRatio);
                viewportX = (width - viewportWidth) / 2;
                viewportY = 0;
            }
        }

        this.viewportX = viewportX;
        this.viewportY = viewportY;
        this.viewportWidth = viewportWidth;
        this.viewportHeight = viewportHeight;

        GLES20.glViewport(viewportX, viewportY, viewportWidth, viewportHeight);

        if (glSurfaceView != null) glSurfaceView.requestRender();
    }

    public int getViewportX() {
        return viewportX;
    }

    public int getViewportY() {
        return viewportY;
    }

    public int getViewportWidth() {
        return viewportWidth;
    }

    public int getViewportHeight() {
        return viewportHeight;
    }

    private int loadShader(int type, String shaderCode) {
        int shader = GLES20.glCreateShader(type);
        GLES20.glShaderSource(shader, shaderCode);
        GLES20.glCompileShader(shader);

        final int[] compileStatus = new int[1];
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compileStatus, 0);

        if (compileStatus[0] == 0) {
            GLES20.glDeleteShader(shader);
            throw new RuntimeException("Error compiling shader: " + GLES20.glGetShaderInfoLog(shader));
        }
        return shader;
    }

    public int getCurrentValue(String filterType) {
        switch (filterType) {
            case "밝기":
                return Math.round(tempBrightness * 100);
            case "노출":
                return Math.round(tempExposure * 100);
            case "대비":
                return Math.round(tempContrast * 100);
            case "하이라이트":
                return Math.round(tempHighlight * 100);
            case "그림자":
                return Math.round(tempShadow * 100);
            case "온도":
                return Math.round(tempTemperature * 100);
            case "색조":
                return Math.round(tempTint * 100);
            case "채도":
                return Math.round((tempSaturation - 1.0f) * 100);
            case "선명하게":
                return Math.round(tempSharpness * 100);
            case "흐리게":
                return Math.round(tempBlur * 100);
            case "비네트":
                return Math.round(tempVignette * 100);
            case "노이즈":
                return Math.round(tempNoise * 100);
        }
        return 0;
    }

    public void setTempValue(String filterType, float value) {
        switch (filterType) {
            case "밝기":
                tempBrightness = value / 100f;
                break;
            case "노출":
                tempExposure = value / 100f;
                break;
            case "대비":
                tempContrast = (value / 100f);
                tempContrast = Math.max(-0.8f, tempContrast);
                break;
            case "하이라이트":
                tempHighlight = value / 100f;
                break;
            case "그림자":
                tempShadow = value / 100f;
                break;
            case "온도":
                tempTemperature = value / 100f;
                break;
            case "색조":
                tempTint = value / 100f;
                break;
            case "채도":
                tempSaturation = (value / 100f) + 1.0f;
                tempSaturation = Math.max(0.0f, tempSaturation);
                break;
            case "선명하게":
                tempSharpness = value / 100f;
                break;
            case "흐리게":
                tempBlur = value / 100f;
                break;
            case "비네트":
                tempVignette = value / 100f;
                break;
            case "노이즈":
                tempNoise = value / 100f;
                break;
        }

        if (glSurfaceView != null) glSurfaceView.requestRender();
    }

    public void updateValue(String filterType, float value) {
        switch (filterType) {
            case "밝기":
                updateBrightness = value / 100f;
                tempBrightness = updateBrightness;
                break;
            case "노출":
                updateExposure = value / 100f;
                tempExposure = updateExposure;
                break;
            case "대비":
                updateContrast = (value / 100f);
                updateContrast = Math.max(-0.8f, updateContrast);
                tempContrast = updateContrast;
                break;
            case "하이라이트":
                updateHighlight = value / 100f;
                tempHighlight = updateHighlight;
                break;
            case "그림자":
                updateShadow = value / 100f;
                tempShadow = updateShadow;
                break;
            case "온도":
                updateTemperature = value / 100f;
                tempTemperature = updateTemperature;
                break;
            case "색조":
                updateTint = value / 100f;
                tempTint = updateTint;
                break;
            case "채도":
                updateSaturation = (value / 100f) + 1.0f;
                updateSaturation = Math.max(0.0f, updateSaturation);
                tempSaturation = updateSaturation;
                break;
            case "선명하게":
                updateSharpness = value / 100f;
                tempSharpness = updateSharpness;
                break;
            case "흐리게":
                updateBlur = value / 100f;
                tempBlur = updateBlur;
                break;
            case "비네트":
                updateVignette = value / 100f;
                tempVignette = updateVignette;
                break;
            case "노이즈":
                updateNoise = value / 100f;
                tempNoise = updateNoise;
                break;
        }

        if (glSurfaceView != null) glSurfaceView.requestRender();
    }

    public void cancelValue(String filterType) {
        switch (filterType) {
            case "밝기":
                tempBrightness = updateBrightness;
                break;
            case "노출":
                tempExposure = updateExposure;
                break;
            case "대비":
                tempContrast = updateContrast;
                break;
            case "하이라이트":
                tempHighlight = updateHighlight;
                break;
            case "그림자":
                tempShadow = updateShadow;
                break;
            case "온도":
                tempTemperature = updateTemperature;
                break;
            case "색조":
                tempTint = updateTint;
                break;
            case "채도":
                tempSaturation = updateSaturation;
                break;
            case "선명하게":
                tempSharpness = updateSharpness;
                break;
            case "흐리게":
                tempBlur = updateBlur;
                break;
            case "비네트":
                tempVignette = updateVignette;
                break;
            case "노이즈":
                tempNoise = updateNoise;
                break;
        }

        if (glSurfaceView != null) glSurfaceView.requestRender();
    }

    public void resetAllFilter() {
        updateBrightness = 0f;
        tempBrightness = 0f;

        updateExposure = 0f;
        tempExposure = 0f;

        updateContrast = 0f;
        tempContrast = 0f;

        updateHighlight = 0f;
        tempHighlight = 0f;

        updateShadow = 0f;
        tempShadow = 0f;

        updateTemperature = 0f;
        tempTemperature = 0f;

        updateTint = 0f;
        tempTint = 0f;

        updateSaturation = 1.0f;
        tempSaturation = 1.0f;

        updateSharpness = 0f;
        tempSharpness = 0f;

        updateBlur = 0f;
        tempBlur = 0f;

        updateVignette = 0f;
        tempVignette = 0f;

        updateNoise = 0f;
        tempNoise = 0f;

        if (glSurfaceView != null) glSurfaceView.requestRender();
    }

    //비트맵 캡쳐 후 리스너에 등록
    public void setOnBitmapCaptureListener(OnBitmapCaptureListener listener) {
        this.listener = listener;
    }

    //비트맵 이미지 캡쳐 메서드, 캡쳐 여부 true로 -> onDraw에서 캡쳐되도록
    public void captureBitmap() {
        shouldCapture = true;
        if (glSurfaceView != null) glSurfaceView.requestRender();
    }

    //openGL 픽셀 데이터를 Bitmap으로 변환하는 메서드
    private Bitmap createBitmapFromGLSurface(int x, int y, int width, int height) {
        int rgbaBuffer[] = new int[width * height]; //openGL RGBA 픽셀 배열
        int argbBuffer[] = new int[width * height]; //Bitmap ARGB 배열
        IntBuffer ib = IntBuffer.wrap(rgbaBuffer);
        ib.position(0);

        GLES20.glReadPixels(x, y, width, height, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, ib);

        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                int index = i * width + j;
                int pixel = rgbaBuffer[index];
                int r = (pixel >> 0) & 0xff;
                int g = (pixel >> 8) & 0xff;
                int b = (pixel >> 16) & 0xff;
                int a = (pixel >> 24) & 0xff;
                int flippedIndex = (height - i - 1) * width + j;
                argbBuffer[flippedIndex] = (a << 24) | (r << 16) | (g << 8) | b;
            }
        }
        return Bitmap.createBitmap(argbBuffer, width, height, Bitmap.Config.ARGB_8888);
    }

    //이미지 캡쳐할 때 뷰포트 여백 (가로사진->위아래 여백 / 세로사진->왼오 여백) 자르기
    public Bitmap cropCenterRegion(Bitmap fullBitmap, int viewportX, int viewportY, int viewportWidth, int viewportHeight) {
        int correctedY = fullBitmap.getHeight() - viewportY - viewportHeight;

        return Bitmap.createBitmap(fullBitmap, viewportX, correctedY, viewportWidth, viewportHeight);
    }
}