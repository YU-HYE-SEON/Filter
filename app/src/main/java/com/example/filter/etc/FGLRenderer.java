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

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class FGLRenderer implements GLSurfaceView.Renderer {
    private final Context context;  //리소스 접근 위해 사용
    private GLSurfaceView glSurfaceView;    //화면에 그려지는 OpenGL의 뷰 (현재 불러온 사진 이미지 + 필터 적용)
    private Bitmap bitmap;  //렌더링할 이미지, openGL의 텍스쳐로 변환
    private int textureId = 0;  //텍스쳐 객체 고유 ID 참조용, 0:생성한 텍스쳐 없음
    private int program;    //쉐이더 프로그램 ID, 쉐이더 프로그램 = (버텍스 쉐이더(정점 위치 계산) + 프래그먼트 쉐이더(픽셀 색상 계산))
    private int positionHandle; //버텍스 쉐이더(vertexShaderCode)의 변수 aPosition ID 참조용 핸들
    private int texCoordHandle; //버텍스 쉐이더(vertexShaderCode)의 변수 aTexCoord ID 참조용 핸들
    private int textureHandle;  //프래그먼트 쉐이더(fragmentShaderCode)의 변수 uTexture ID 참조용 핸들
    private int brightnessHandle;   //프래그먼트 쉐이더(fragmentShaderCode)의 변수 uBrightness ID 참조용 핸들
    private int contrastHandle;  //프래그먼트 쉐이더(fragmentShaderCode)의 변수 uContrast ID 참조용 핸들
    private int sharpnessHandle;  //프래그먼트 쉐이더(fragmentShaderCode)의 변수 uSharpness ID 참조용 핸들
    private int resolutionHandle; //프래그먼트 쉐이더(fragmentShaderCode)의 변수 uResolution ID 참조용 핸들
    private int saturationHandle;   //프래그먼트 쉐이더(fragmentShaderCode)의 변수 uSaturation ID 참조용 핸들
    private final FloatBuffer vertexBuffer; //버텍스 위치 데이터를 GPU에 전달할 때 사용하는 버퍼
    private final FloatBuffer texCoordBuffer;   //텍스쳐 좌표 데이터를 GPU에 전달할 때 사용하는 버퍼
    private float imageAspectRatio; //사진 이미지 비율

    //최종 적용 조절값
    private float updateBrightness = 0f;
    private float updateContrast = 1.0f;
    private float updateSharpness = 0f;
    private float updateSaturation = 1.0f;

    //최종 적용 전 실시간 미리보기용 조절값 (onDrawFrame에서 사용)
    private float tempBrightness = 0f;
    private float tempContrast = 1.0f;
    private float tempSharpness = 0f;
    private float tempSaturation = 1.0f;

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

            if (glSurfaceView != null) glSurfaceView.requestRender();
        }
    }

    @Override
    public void onSurfaceCreated(GL10 unused, EGLConfig config) {
        GLES20.glClearColor(1.0f, 1.0f, 1.0f, 1.0f);
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
        contrastHandle = GLES20.glGetUniformLocation(program, "uContrast");
        sharpnessHandle = GLES20.glGetUniformLocation(program, "uSharpness");
        resolutionHandle = GLES20.glGetUniformLocation(program, "uResolution");
        saturationHandle = GLES20.glGetUniformLocation(program, "uSaturation");
    }

    @Override
    public void onDrawFrame(GL10 unused) {
        GLES20.glClearColor(1.0f, 1.0f, 1.0f, 1.0f);
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

            GLES20.glEnableVertexAttribArray(positionHandle);
            GLES20.glVertexAttribPointer(positionHandle, 2, GLES20.GL_FLOAT, false, 0, vertexBuffer);
            GLES20.glEnableVertexAttribArray(texCoordHandle);
            GLES20.glVertexAttribPointer(texCoordHandle, 2, GLES20.GL_FLOAT, false, 0, texCoordBuffer);

            GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId);
            GLES20.glUniform1i(textureHandle, 0);

            GLES20.glUniform1f(brightnessHandle, tempBrightness);
            GLES20.glUniform1f(contrastHandle, tempContrast);
            GLES20.glUniform1f(sharpnessHandle, tempSharpness);
            GLES20.glUniform2f(resolutionHandle, bitmap.getWidth(), bitmap.getHeight());
            GLES20.glUniform1f(saturationHandle, tempSaturation);

            GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);

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

        GLES20.glViewport(viewportX, viewportY, viewportWidth, viewportHeight);

        if (glSurfaceView != null) glSurfaceView.requestRender();
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
            case "대비":
                return Math.round(tempContrast * 100);
            case "선명하게":
                return Math.round(tempSharpness * 100);
            case "채도":
                return Math.round((tempSaturation - 1.0f) * 100);
        }
        return 0;
    }

    public void setTempValue(String filterType, float value) {
        switch (filterType) {
            case "밝기":
                tempBrightness = value / 100f;
                break;
            case "대비":
                tempContrast = (value / 100f);
                tempContrast = Math.max(-0.8f, tempContrast);
                break;
            case "선명하게":
                tempSharpness = value / 100f;
                break;
            case "채도":
                tempSaturation = (value / 100f) + 1.0f;
                tempSaturation = Math.max(0.0f, tempSaturation);
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
            case "대비":
                updateContrast = (value / 100f);
                updateContrast = Math.max(-0.8f, updateContrast);
                tempContrast = updateContrast;
                break;
            case "선명하게":
                updateSharpness = value / 100f;
                tempSharpness = updateSharpness;
                break;
            case "채도":
                updateSaturation = (value / 100f) + 1.0f;
                updateSaturation = Math.max(0.0f, updateSaturation);
                tempSaturation = updateSaturation;
                break;
        }

        if (glSurfaceView != null) glSurfaceView.requestRender();
    }

    public void cancelValue(String filterType) {
        switch (filterType) {
            case "밝기":
                tempBrightness = updateBrightness;
                break;
            case "대비":
                tempContrast = updateContrast;
                break;
            case "선명하게":
                tempSharpness = updateSharpness;
                break;
            case "채도":
                tempSaturation = updateSaturation;
                break;
        }

        if (glSurfaceView != null) glSurfaceView.requestRender();
    }

    public void resetAllFilter() {
        updateBrightness = 0f;
        tempBrightness = 0f;

        updateContrast = 0f;
        tempContrast = 0f;

        updateSharpness = 0f;
        tempSharpness = 0f;

        updateSaturation = 1.0f;
        tempSaturation = 1.0f;

        if (glSurfaceView != null) glSurfaceView.requestRender();
    }
}