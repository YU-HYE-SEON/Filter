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
    public interface OnBitmapCaptureListener {
        void onBitmapCaptured(Bitmap bitmap);
    }
    private OnBitmapCaptureListener listener;
    private boolean shouldCapture = false;
    private final Context context;
    private GLSurfaceView glSurfaceView;
    private volatile Bitmap bitmap;
    private int viewportX, viewportY, viewportWidth, viewportHeight;
    private int textureId = 0;
    private int program;
    private int positionHandle, texCoordHandle;
    private int textureHandle, brightnessHandle, exposureHandle, contrastHandle, highlightHandle, shadowHandle, temperatureHandle, hueHandle,
            saturationHandle, sharpnessHandle, blurHandle, vignetteHandle, noiseHandle, resolutionHandle;
    private final FloatBuffer vertexBuffer, texCoordBuffer;
    private float imageAspectRatio;
    private float updateBrightness = 0f, updateExposure = 0f, updateContrast = 0f, updateHighlight = 0f, updateShadow = 0f, updateTemperature = 0f,
            updateHue = 0f, updateSaturation = 1.0f, updateSharpness = 0f, updateBlur = 0f, updateVignette = 0f, updateNoise = 0f;
    private float tempBrightness = 0f, tempExposure = 0f, tempContrast = 0f, tempHighlight = 0f, tempShadow = 0f, tempTemperature = 0f,
            tempHue = 0f, tempSaturation = 1.0f, tempSharpness = 0f, tempBlur = 0f, tempVignette = 0f, tempNoise = 0f;
    private float translateX = 0f, translateY = 0f;
    private float scaleFactor = 1.0f;

    private final float[] vertices = {
            -1.0f, -1.0f,   //왼쪽 아래
            1.0f, -1.0f,    //오른쪽 아래
            -1.0f, 1.0f,    //왼쪽 위
            1.0f, 1.0f      //오른쪽 위
    };
    private final float[] texCoords = {
            0.0f, 1.0f,     //왼쪽 위 → 왼쪽 아래
            1.0f, 1.0f,     //오른쪽 위 → 오른쪽 아래
            0.0f, 0.0f,     //왼쪽 아래 → 왼쪽 위
            1.0f, 0.0f      //오른쪽 아래 → 오른쪽 위
    };

    private boolean isFilterActivity = true;    //FilterActivity면 사진 영역 밖을 검정색으로, ApplyFilterActivity면 사진 영역 밖을 흰색으로 설정하기 위한 변수

    public FGLRenderer(Context context, GLSurfaceView glSurfaceView, boolean isFilterActivity) {
        this.context = context;
        this.glSurfaceView = glSurfaceView;
        this.isFilterActivity = isFilterActivity;

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

    @Override
    public void onSurfaceCreated(GL10 unused, EGLConfig config) {
        if (isFilterActivity) {
            GLES20.glClearColor(0f, 0f, 0f, 1f);
        } else {
            GLES20.glClearColor(1f, 1f, 1f, 1f);
        }
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
        hueHandle = GLES20.glGetUniformLocation(program, "uHue");
        saturationHandle = GLES20.glGetUniformLocation(program, "uSaturation");
        sharpnessHandle = GLES20.glGetUniformLocation(program, "uSharpness");
        blurHandle = GLES20.glGetUniformLocation(program, "uBlur");
        vignetteHandle = GLES20.glGetUniformLocation(program, "uVignette");
        noiseHandle = GLES20.glGetUniformLocation(program, "uNoise");
        resolutionHandle = GLES20.glGetUniformLocation(program, "uResolution");
    }

    @Override
    public void onDrawFrame(GL10 unused) {
        if (isFilterActivity) {
            GLES20.glClearColor(0f, 0f, 0f, 1f);
        } else {
            GLES20.glClearColor(1f, 1f, 1f, 1f);
        }
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
            GLES20.glUniform1f(hueHandle, tempHue);
            GLES20.glUniform1f(saturationHandle, tempSaturation);
            GLES20.glUniform1f(sharpnessHandle, tempSharpness);
            GLES20.glUniform1f(blurHandle, tempBlur);
            GLES20.glUniform1f(vignetteHandle, tempVignette);
            GLES20.glUniform1f(noiseHandle, tempNoise);
            GLES20.glUniform2f(resolutionHandle, bitmap.getWidth(), bitmap.getHeight());

            GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);

            if (shouldCapture) {
                shouldCapture = false;

                final int w = glSurfaceView.getWidth();
                final int h = glSurfaceView.getHeight();
                Bitmap captured;

                //오프스크린 렌더링 방식을 사용하기 위함 (사용자 눈에 직접 보이지 않게 처리)
                //색감 보정 후 크롭 시 찰나에 원본 색감으로 깜빡이는 문제와
                //색감 보정 후 크롭 시 색감 보정이 이중 적용되는 문제 해결을 위함
                //오프스크린 렌더링용 세팅 및 실행
                if (captureUnfilteredNext) {
                    //오프스크린 렌더링 준비 메서드 호출
                    ensureOffscreenTarget(w, h);

                    GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, offFbo);
                    GLES20.glViewport(viewportX, viewportY, viewportWidth, viewportHeight);

                    if (isFilterActivity) {
                        GLES20.glClearColor(0f, 0f, 0f, 1f);
                    } else {
                        GLES20.glClearColor(1f, 1f, 1f, 1f);
                    }
                    GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);

                    GLES20.glUseProgram(program);

                    float[] scaledVertices2 = new float[]{
                            -1.0f * scaleFactor + translateX, -1.0f * scaleFactor + translateY,
                            1.0f * scaleFactor + translateX, -1.0f * scaleFactor + translateY,
                            -1.0f * scaleFactor + translateX, 1.0f * scaleFactor + translateY,
                            1.0f * scaleFactor + translateX, 1.0f * scaleFactor + translateY
                    };
                    vertexBuffer.put(scaledVertices2).position(0);

                    GLES20.glEnableVertexAttribArray(positionHandle);
                    GLES20.glVertexAttribPointer(positionHandle, 2, GLES20.GL_FLOAT, false, 0, vertexBuffer);
                    GLES20.glEnableVertexAttribArray(texCoordHandle);
                    GLES20.glVertexAttribPointer(texCoordHandle, 2, GLES20.GL_FLOAT, false, 0, texCoordBuffer);

                    GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
                    GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId);
                    GLES20.glUniform1i(textureHandle, 0);

                    GLES20.glUniform1f(brightnessHandle, 0f);
                    GLES20.glUniform1f(exposureHandle, 0f);
                    GLES20.glUniform1f(contrastHandle, 0f);
                    GLES20.glUniform1f(highlightHandle, 0f);
                    GLES20.glUniform1f(shadowHandle, 0f);
                    GLES20.glUniform1f(temperatureHandle, 0f);
                    GLES20.glUniform1f(hueHandle, 0f);
                    GLES20.glUniform1f(saturationHandle, 1.0f);
                    GLES20.glUniform1f(sharpnessHandle, 0f);
                    GLES20.glUniform1f(blurHandle, 0f);
                    GLES20.glUniform1f(vignetteHandle, 0f);
                    GLES20.glUniform1f(noiseHandle, 0f);
                    GLES20.glUniform2f(resolutionHandle, bitmap.getWidth(), bitmap.getHeight());

                    GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);

                    int rw = viewportWidth;
                    int rh = viewportHeight;
                    int rx = viewportX;
                    int ry = viewportY;

                    int[] rgba = new int[rw * rh];
                    int[] argb = new int[rw * rh];
                    IntBuffer ib = IntBuffer.wrap(rgba);
                    ib.position(0);

                    GLES20.glFinish();
                    GLES20.glReadPixels(rx, ry, rw, rh, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, ib);

                    for (int i = 0; i < rh; i++) {
                        for (int j = 0; j < rw; j++) {
                            int idx = i * rw + j;
                            int p = rgba[idx];
                            int r = (p) & 0xff;
                            int g = (p >> 8) & 0xff;
                            int b = (p >> 16) & 0xff;
                            int a = (p >> 24) & 0xff;
                            int flip = (rh - i - 1) * rw + j;
                            argb[flip] = (a << 24) | (r << 16) | (g << 8) | b;
                        }
                    }
                    captured = Bitmap.createBitmap(argb, rw, rh, Bitmap.Config.ARGB_8888);

                    GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
                    GLES20.glViewport(viewportX, viewportY, viewportWidth, viewportHeight);

                    captureUnfilteredNext = false;
                } else {
                    captured = createBitmapFromGLSurface(viewportX, viewportY, viewportWidth, viewportHeight);
                }

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

    /// 뷰포트 크기랑 좌표 ///
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

    /// 화면 조절 ///
    public void setTranslate(float x, float y) {
        this.translateX = (x * 2f) / (float) viewportWidth;
        this.translateY = (y * 2f) / (float) viewportHeight;
    }

    public void setScaleFactor(float factor) {
        this.scaleFactor = factor;
    }

    /// 비트맵, 캡쳐 관련 ///
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

    public void setOnBitmapCaptureListener(OnBitmapCaptureListener listener) {
        this.listener = listener;
    }

    public void captureBitmap() {
        shouldCapture = true;
        if (glSurfaceView != null) glSurfaceView.requestRender();
    }

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

    /// 필터 조정값 ///
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
                return Math.round(tempHue * 100);
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
                tempHue = value / 100f;
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
                updateHue = value / 100f;
                tempHue = updateHue;
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
                tempHue = updateHue;
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

        updateHue = 0f;
        tempHue = 0f;

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

    /// 오프스크린 ///
    private boolean captureUnfilteredNext = false;
    private int offFbo = 0;
    private int offTex = 0;
    private int offRbo = 0;
    private int offW = 0, offH = 0;

    public void captureBitmapUnfiltered() {
        captureUnfilteredNext = true;
        shouldCapture = true;
        if (glSurfaceView != null) glSurfaceView.requestRender();
    }

    private void ensureOffscreenTarget(int width, int height) {
        if (offFbo != 0 && width == offW && height == offH) return;

        releaseOffscreenTarget();

        offW = width;
        offH = height;

        int[] ids = new int[1];

        GLES20.glGenTextures(1, ids, 0);
        offTex = ids[0];
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, offTex);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, offW, offH, 0,
                GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, null);

        GLES20.glGenFramebuffers(1, ids, 0);
        offFbo = ids[0];
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, offFbo);
        GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0,
                GLES20.GL_TEXTURE_2D, offTex, 0);

        GLES20.glGenRenderbuffers(1, ids, 0);
        offRbo = ids[0];
        GLES20.glBindRenderbuffer(GLES20.GL_RENDERBUFFER, offRbo);
        GLES20.glRenderbufferStorage(GLES20.GL_RENDERBUFFER, GLES20.GL_DEPTH_COMPONENT16, offW, offH);
        GLES20.glFramebufferRenderbuffer(GLES20.GL_FRAMEBUFFER, GLES20.GL_DEPTH_ATTACHMENT,
                GLES20.GL_RENDERBUFFER, offRbo);

        int status = GLES20.glCheckFramebufferStatus(GLES20.GL_FRAMEBUFFER);
        if (status != GLES20.GL_FRAMEBUFFER_COMPLETE) {
            releaseOffscreenTarget();
        }

        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
        GLES20.glBindRenderbuffer(GLES20.GL_RENDERBUFFER, 0);
    }

    private void releaseOffscreenTarget() {
        int[] ids = new int[1];
        if (offTex != 0) {
            ids[0] = offTex;
            GLES20.glDeleteTextures(1, ids, 0);
            offTex = 0;
        }
        if (offRbo != 0) {
            ids[0] = offRbo;
            GLES20.glDeleteRenderbuffers(1, ids, 0);
            offRbo = 0;
        }
        if (offFbo != 0) {
            ids[0] = offFbo;
            GLES20.glDeleteFramebuffers(1, ids, 0);
            offFbo = 0;
        }
        offW = offH = 0;
    }
}