package com.example.filter.etc;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Matrix;
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

public class CGLRenderer implements GLSurfaceView.Renderer {
    public interface BitmapCaptureListener {
        void onBitmapCaptured(Bitmap bitmap);
    }

    private BitmapCaptureListener captureListener;
    private boolean capturePending = false;

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
    private int ratioMode = 1;

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

    public CGLRenderer(Context context, GLSurfaceView glSurfaceView) {
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

    @Override
    public void onSurfaceCreated(GL10 unused, EGLConfig config) {
        GLES20.glClearColor(1f, 1f, 1f, 1f);
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
        GLES20.glClearColor(1f, 1f, 1f, 1f);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);

        if (bitmap == null || program == 0) {
            if (capturePending && captureListener != null) {
                captureListener.onBitmapCaptured(null);
                capturePending = false;
            }
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

            GLES20.glDisableVertexAttribArray(positionHandle);
            GLES20.glDisableVertexAttribArray(texCoordHandle);
        }

        if (capturePending) {
            capturePending = false;
            if (captureListener != null) {
                int cropW, cropH, cropX, cropY;
                if (ratioMode == 1) {
                    int size = Math.min(viewportWidth, viewportHeight);
                    cropW = size;
                    cropH = size;

                    cropX = viewportX + (viewportWidth - size) / 2;
                    cropY = viewportY + (viewportHeight - size) / 2;
                }else {
                    cropW = viewportWidth;
                    cropH = viewportHeight;
                    cropX = viewportX;
                    cropY = viewportY;
                }
                Bitmap capturedBitmap = readPixels(cropX, cropY, cropW, cropH);
                //Bitmap capturedBitmap = readPixels(viewportX, viewportY, viewportWidth, viewportHeight);
                captureListener.onBitmapCaptured(capturedBitmap);

                isCapturing = false;
            }
        }
    }

    private boolean isCapturing = false;

    @Override
    public void onSurfaceChanged(GL10 unused, int width, int height) {
        float viewAspectRatio = (float) width / height;
        int viewportX = 0;
        int viewportY = 0;
        int viewportWidth = width;
        int viewportHeight = height;

        float targetRatio;

        switch (ratioMode) {
            case 1:
                //targetRatio = 1f;
                targetRatio = 3f / 4f;
                break;
            case 2:
                targetRatio = 3f / 4f;
                break;
            case 3:
                targetRatio = 9f / 16f;
                break;
            default:
                targetRatio = viewAspectRatio;
        }

        //float finalAspect = Math.min(imageAspectRatio, targetRatio);

        if (viewAspectRatio > targetRatio) {
            viewportWidth = width;
            viewportHeight = (int) (width / targetRatio);
            viewportX = 0;
            viewportY = (height - viewportHeight) / 2;
        } else {
            viewportHeight = height;
            viewportWidth = (int) (height * targetRatio);
            viewportX = (width - viewportWidth) / 2;
            viewportY = 0;
        }

        this.viewportX = viewportX;
        this.viewportY = viewportY;
        this.viewportWidth = viewportWidth;
        this.viewportHeight = viewportHeight;

        GLES20.glViewport(viewportX, viewportY, viewportWidth, viewportHeight);
    }

    public int getViewportWidth() {
        return viewportWidth;
    }

    public int getViewportHeight() {
        return viewportHeight;
    }

    public int getViewportX() {
        return viewportX;
    }

    public int getViewportY() {
        return viewportY;
    }

    public void setRatioMode(int mode) {
        this.ratioMode = mode;
    }

    public int getRatioMode() {
        return ratioMode;
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

    private Bitmap readPixels(int x, int y, int w, int h) {
        if (w <= 0 || h <= 0) return null;

        ByteBuffer buf = ByteBuffer.allocateDirect(w * h * 4);
        buf.order(ByteOrder.nativeOrder());

        GLES20.glReadPixels(x, y, w, h, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, buf);
        buf.rewind();

        Bitmap bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        bmp.copyPixelsFromBuffer(buf);

        Matrix matrix = new Matrix();
        matrix.preScale(1.0f, -1.0f);
        return Bitmap.createBitmap(bmp, 0, 0, w, h, matrix, true);
    }

    public void captureFinalBitmap(BitmapCaptureListener listener) {
        isCapturing = true;

        this.captureListener = listener;
        this.capturePending = true;
        if (glSurfaceView != null) {
            glSurfaceView.requestRender();
        }
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
}