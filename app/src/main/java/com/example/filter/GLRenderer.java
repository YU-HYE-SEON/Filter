package com.example.filter;

import android.content.Context;
import android.graphics.Bitmap;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.GLUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

//GLSurfaceView.Renderer의 메서드를 구현 (onSurfaceCreated, onDrawFrame, onSurfaceChanged)
public class GLRenderer implements GLSurfaceView.Renderer {
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
    private float currentBrightness = 0f;
    private float currentContrast = 1.0f;
    private float currentSharpness = 0f;
    private float currentSaturation = 1.0f;

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

    //생성자
    public GLRenderer(Context context, GLSurfaceView glSurfaceView) {
        this.context = context;
        this.glSurfaceView = glSurfaceView;

        //정점 좌표 초기화, 필요한 메모리 크기 계산 후 네이티브 메모리에 바이트버퍼를 만들어 할당 → GPU에 빠른 데이터 전송 가능
        ByteBuffer bb = ByteBuffer.allocateDirect(vertices.length * 4);
        //운영체제의 바이트 순서에 맞춤 → 호환성 확보
        bb.order(ByteOrder.nativeOrder());
        //ByteBuffer를 FloatBuffer로 변환 (float배열 vertices를 GPU에 전달하기 위해)
        vertexBuffer = bb.asFloatBuffer();
        //버퍼에 vertices 데이터 추가 → 버텍스 위치 데이터를 GPU에 전달
        vertexBuffer.put(vertices);
        //버퍼 커서 위치 0부터 읽도록 설정
        vertexBuffer.position(0);

        //텍스쳐 좌표 초기화, 필요한 메모리 크기 계산 후 네이티브 메모리에 바이트버퍼를 만들어 할당 → GPU에 빠른 데이터 전송 가능
        ByteBuffer cc = ByteBuffer.allocateDirect(texCoords.length * 4);
        cc.order(ByteOrder.nativeOrder());
        //ByteBuffer를 FloatBuffer로 변환 (float배열 texCoords를 GPU에 전달하기 위해)
        texCoordBuffer = cc.asFloatBuffer();
        //버퍼에 texCoords 데이터 추가 → 텍스쳐 좌표 데이터를 GPU에 전달
        texCoordBuffer.put(texCoords);
        texCoordBuffer.position(0);
    }

    //버텍스 쉐이더 코드 파일과 프래그먼트 쉐이더 코드 파일을 읽어 문자열로 변환하는 메서드
    private String loadShaderCodeFromRawResource(int resourceId) {
        //쉐이더 코드 파일 읽기 위한 InputStream 열기
        InputStream inputStream = context.getResources().openRawResource(resourceId);
        //BufferedReader 사용해서 InputStream 효율적으로 읽기 준비
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        //쉐이더 코드 저장할 StringBuilder 객체 생성
        StringBuilder shaderCode = new StringBuilder();
        //1줄씩 읽어올 변수
        String line;
        try {
            //파일 끝까지 읽을 때까지 한 줄씩 읽어서 shaderCode에 추가 (줄 끝날 때마다 줄바꿈 추가)
            while ((line = reader.readLine()) != null) shaderCode.append(line).append("\n");
            //파일 읽기 완료 후 스트림 닫기
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        //StringBuilder에 저장된 쉐이더 코드를 문자열로 반환
        return shaderCode.toString();
    }

    //사진 이미지 설정, 렌더링 요청
    public void setBitmap(Bitmap bitmap) {
        this.bitmap = bitmap;

        //사진을 새로 불러온 경우
        //이전에 만든 텍스쳐 객체 있으면 삭제 (메모리 관리)
        if (textureId != 0) {
            GLES20.glDeleteTextures(1, new int[]{textureId}, 0);    //삭제할 텍스쳐 개수, 삭제할 텍스쳐 ID 배열, 삭제를 시작할 위치
            textureId = 0;  //다시 0(생성한 텍스쳐 없음)으로 설정
        }

        if (bitmap != null) {
            imageAspectRatio = (float) bitmap.getWidth() / bitmap.getHeight();  //사진 이미지 비율 계산

            if (glSurfaceView != null) glSurfaceView.requestRender();   //그리기 요청 메서드
        }
    }

    //openGL 환경 초기화, 쉐이더 컴파일, 쉐이더 프로그램 생성 및 링크, 쉐이더 코드 속 변수 참조 후 데이터 저장
    @Override
    public void onSurfaceCreated(GL10 unused, EGLConfig config) {
        //배경색 흰색으로 설정
        GLES20.glClearColor(1.0f, 1.0f, 1.0f, 1.0f);
        //화면 초기화, 컬러 버퍼와 깊이 버퍼 초기화
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

        //loadShaderCodeFromRawResource : 버텍스 쉐이더 코드 파일과 프래그먼트 쉐이더 코드 파일을 읽어 문자열로 변환하는 메서드
        String vertexShaderCode = loadShaderCodeFromRawResource(R.raw.filter_vertex_shader);
        String fragmentShaderCode = loadShaderCodeFromRawResource(R.raw.filter_fragment_shader);

        //버텍스 쉐이더 & 프래그먼트 쉐이더 컴파일, 문자열의 쉐이더 코드를 GPU가 이해하는 형태로 컴파일
        //loadShader : 쉐이더 객체를 생성해서 문자열로 정의한 쉐이더 코드를 가지고 쉐이더 타입별 컴파일하는 메서드
        int vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode);
        int fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode);

        //쉐이더 프로그램 객체 생성 (여러 개의 쉐이더를 하나의 프로그램으로 연결) (버텍스 쉐이더 + 프래그먼트 쉐이더)
        program = GLES20.glCreateProgram();
        GLES20.glAttachShader(program, vertexShader);
        GLES20.glAttachShader(program, fragmentShader);
        GLES20.glLinkProgram(program);  //GPU가 실행할 수 있는 최종 프로그램을 만듦

        //프로그램 링크 성공 여부 확인
        final int[] linkStatus = new int[1];
        //검사할 프로그램의 객체 ID (쉐이더 프로그램),어떤 상태를 확인할 것인지 (링크 여부), 상태 결과를 담을 배열 (linkStatus), 담을 배열의 위치 (0번)
        GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, linkStatus, 0);
        //결과가 1이면 링크 성공 / 결과가 0이면 링크 실패한 경우, 프로그램 삭제 (메모리 관리)
        if (linkStatus[0] == 0) {
            GLES20.glDeleteProgram(program);
            program = 0;
        }

        //버텍스 쉐이더 코드 속 변수 aPosition(정점 좌표)의 ID를 얻어서 positionHandle에 저장
        positionHandle = GLES20.glGetAttribLocation(program, "aPosition");
        //버텍스 쉐이더 코드 속 변수 aTexCoord(텍스쳐 좌표)의 ID를 얻어서 texCoordHandle에 저장
        texCoordHandle = GLES20.glGetAttribLocation(program, "aTexCoord");

        //프래그먼트 쉐이더 코드 속 변수 uTexture(텍스쳐 샘플러)의 ID를 얻어서 textureHandle에 저장
        textureHandle = GLES20.glGetUniformLocation(program, "uTexture");
        //프래그먼트 쉐이더 코드 속 변수(각 필터값 or 화면 해상도)의 ID를 얻어서 각 Handle에 저장
        brightnessHandle = GLES20.glGetUniformLocation(program, "uBrightness");
        contrastHandle = GLES20.glGetUniformLocation(program, "uContrast");
        sharpnessHandle = GLES20.glGetUniformLocation(program, "uSharpness");
        resolutionHandle = GLES20.glGetUniformLocation(program, "uResolution");
        saturationHandle = GLES20.glGetUniformLocation(program, "uSaturation");
    }

    //매 프레임마다 호출, 비트맵 이미지를 openGL 텍스쳐로 변환, 필터 효과 적용, 이미지 그리기
    @Override
    public void onDrawFrame(GL10 unused) {
        GLES20.glClearColor(1.0f, 1.0f, 1.0f, 1.0f);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);

        //비트맵 없거나 쉐이더 프로그램 없으면 그리지 않음
        if (bitmap == null || program == 0) {
            return;
        }

        //생성한 텍스쳐가 없으면 텍스쳐 1개 생성
        if (textureId == 0) {
            int[] textures = new int[1];
            GLES20.glGenTextures(1, textures, 0);   //생성할 텍스쳐 개수, 생성한 텍스쳐 ID를 담을 배열, 담을 위치 (0번)
            textureId = textures[0];    //생성한 텍스쳐 ID를 textureId에 저장

            //생성한 텍스쳐를 바인딩 (현재 작업할 대상으로 설정)
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId);

            //텍스쳐 필터링 방식 설정, 텍스쳐의 파라미터를 float로 설정
            //2D 텍스쳐의 최소 텍스처 필터와 최대 텍스처 필터를 GL_LINEAR로 설정
            //(주변 텍스쳐 픽셀 색상을 평균하여 화면에 그려 픽셀이 튀어 보이는 현상을 줄여줌)
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);

            //텍스쳐 (경계 처리) 랩핑 방식 설정, 텍스쳐의 파라미터를 int로 설정
            //2D 텍스쳐의 s축(수평방향)과 t축(수직방향)에 대한 랩핑 방식을 GL_CLAMP_TO_EDGE로 설정
            // (텍스쳐 경계선 바깥 부분을 텍스쳐의 가장자리 픽셀 색상으로 채워 시각적 오류를 방지함)
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);

            //비트맵을 openGL 텍스쳐로 변환!!
            //2D 텍스쳐 사용하겠다, Mipmap 레벨 0 : 원본 텍스쳐, openGL 텍스쳐로 변환될 이미지 (불러온 사진), 보더 폭 0 : 보더 사용x
            GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0);
        }

        //생성한 텍스쳐를 가지고 렌더링 진행
        if (textureId != 0) {
            //쉐이더 프로그램 활성화
            GLES20.glUseProgram(program);

            //정점 위치 배열 활성화, 정점 위치 데이터를 공급할 준비가 되었음을 GPU에게 알림
            GLES20.glEnableVertexAttribArray(positionHandle);
            //데이터가 저장된 위치, 컴포넌트 타입과 어떻게 읽는지 등에 대한 정보를 GPU에게 알림
            //데이터(정점 위치) 연결할 쉐이더 속성의 핸들, size : 2 → 컴포넌트 2개 당 정점 좌표 한 개(x,y)로 읽음,
            //GLES20.GL_FLOAT : 컴포넌트 타입, false : 정규화 안함, stride : 0 → 컴포넌트 간의 간격 0, 실제 정점 데이터가 저장되어있는 버퍼
            GLES20.glVertexAttribPointer(positionHandle, 2, GLES20.GL_FLOAT, false, 0, vertexBuffer);

            //텍스쳐 좌표 배열 활성화, 텍스쳐 좌표 데이터를 공급할 준비가 되었음을 GPU에게 알림
            GLES20.glEnableVertexAttribArray(texCoordHandle);
            GLES20.glVertexAttribPointer(texCoordHandle, 2, GLES20.GL_FLOAT, false, 0, texCoordBuffer);

            //텍스쳐 슬롯 하나 당 하나의 텍스쳐 객체 바인딩 가능
            //텍스쳐 슬롯 0 활성화 후 생성된 텍스쳐 1개를 2D 텍스쳐로 슬롯 0에 바인딩
            GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId);

            //glUniform1i : 유니폼 변수에 정수값(1개)을 넣는 메서드
            //텍스쳐 샘플러의 핸들, 0 : 텍스쳐 슬롯 0 찾아서 거기에 바인딩된 텍스쳐를 사용하겠다
            GLES20.glUniform1i(textureHandle, 0);
            //glUniform1f : 유니폼 변수에 실수값(1개)을 넣는 메서드
            //각 필터값 핸들에 조절 값 전달, 최종 적용이 아니기 때문에 실시간 미리보기용 변수를 넣음
            GLES20.glUniform1f(brightnessHandle, tempBrightness);
            GLES20.glUniform1f(contrastHandle, tempContrast);
            GLES20.glUniform1f(sharpnessHandle, tempSharpness);
            GLES20.glUniform2f(resolutionHandle, bitmap.getWidth(), bitmap.getHeight());
            GLES20.glUniform1f(saturationHandle, tempSaturation);

            //실제 화면에 그래픽 요소를 그리는 메서드
            //정점 4개를 삼각형 형태로 그리기 → 삼각형 2개의 사각형
            //삼각형으로 그려라, 시작 인덱스(가장 처음 정점부터), 정점 개수
            GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);

            //정점 위치 배열과 텍스쳐 좌표 배열 비활성화 (효율성, 오류 방지, 이전 사용 속성이 다음 객체 렌더링에 영향 미치지 않도록)
            GLES20.glDisableVertexAttribArray(positionHandle);
            GLES20.glDisableVertexAttribArray(texCoordHandle);
        }
    }

    //뷰 크기 변경 시 뷰포트 설정, 크기 변경 이벤트 발생 시 호출
    @Override
    public void onSurfaceChanged(GL10 unused, int width, int height) {
        float viewAspectRatio = (float) width / height; //현재 뷰 가로 세로 비율
        //전체 화면 크기로 뷰포트 초기 설정
        int viewportX = 0;
        int viewportY = 0;
        int viewportWidth = width;
        int viewportHeight = height;

        //setBitmap에서 계산한 사진 이미지 비율
        //0보다 크면 유효
        if (imageAspectRatio > 0) {
            //사진 비율이 뷰 비율보다 크면 (사진이 뷰보다 가로가 긴 경우)
            if (imageAspectRatio > viewAspectRatio) {
                //뷰포트 너비와 사진 너비 같게 설정
                viewportWidth = width;
                //뷰포트 높이를 사진 비율에 맞게 계산
                viewportHeight = (int) (width / imageAspectRatio);
                //사진이 가운데 오도록 y좌표 계산
                viewportY = (height - viewportHeight) / 2;
                //너비는 같게 맞춰서 가운데 정렬할 필요없음, 0으로 유지
                viewportX = 0;
            } else {    //사진 비율이 뷰 비율보다 작거나 같으면 (사진이 뷰보다 세로가 긴 경우)
                //뷰포트 높이와 사진 높이 같게 설정
                viewportHeight = height;
                //뷰포트 너비를 사진 비율에 맞게 계산
                viewportWidth = (int) (height * imageAspectRatio);
                //사진이 가운데 오도록 x좌표 계산
                viewportX = (width - viewportWidth) / 2;
                //높이는 같게 맞춰서 가운데 정렬할 필요없음, 0으로 유지
                viewportY = 0;
            }
        }
        //계산된 값으로 뷰포트 설정
        GLES20.glViewport(viewportX, viewportY, viewportWidth, viewportHeight);
    }

    //쉐이더 객체를 생성해서 문자열로 정의한 쉐이더 코드를 가지고 쉐이더 타입별 컴파일하는 메서드
    private int loadShader(int type, String shaderCode) {
        //전달받은 쉐이더 타입으로 쉐이더 객체 생성하고 ID 반환
        int shader = GLES20.glCreateShader(type);
        //쉐이더 객체에 전달받은 쉐이더 코드 연결
        GLES20.glShaderSource(shader, shaderCode);
        //쉐이더 컴파일
        GLES20.glCompileShader(shader);

        //컴파일 성공 여부 확인
        final int[] compileStatus = new int[1];
        //검사할 쉐이더 객체 ID, 어떤 상태를 확인할 것인지 (컴파일 여부), 상태 결과를 담을 배열 (compileStatus), 담을 배열의 위치 (0번)
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compileStatus, 0);
        //결과가 1이면 컴파일 성공 / 결과가 0이면 컴파일 실패한 경우, 쉐이더 삭제 (메모리 관리)
        if (compileStatus[0] == 0) {
            GLES20.glDeleteShader(shader);
            //컴파일 실패한 경우 에러 메시지 반환
            throw new RuntimeException("Error compiling shader: " + GLES20.glGetShaderInfoLog(shader));
        }
        //컴파일 성공 시 쉐이더 ID 반환
        return shader;
    }

    //실시간 미리보기 설정
    public void setTempValue(FilterActivity.Type type, float value) {
        switch (type) {
            case BRIGHTNESS:
                tempBrightness = value / 100f;
                break;
            case CONTRAST:
                tempContrast = (value / 100f) + 1.0f;
                tempContrast = Math.max(0.1f, tempContrast);    //-100일 때 완전 회색이 되는 것을 막기 위해 설정함
                break;
            case SHARPNESS:
                tempSharpness = value / 100f;
                break;
            case SATURATION:
                tempSaturation = (value / 100f) + 1.0f;
                tempSaturation = Math.max(0.0f, tempSaturation);
                break;
        }

        if (glSurfaceView != null) glSurfaceView.requestRender();
    }

    //최종 적용 설정
    public void applyValue(FilterActivity.Type type, float value) {
        switch (type) {
            case BRIGHTNESS:
                currentBrightness = value / 100f;
                tempBrightness = currentBrightness;    //최종 적용하면 실시간 미리보기용 변수 데이터 결과도 업데이트
                break;
            case CONTRAST:
                currentContrast = (value / 100f) + 1.0f;
                currentContrast = Math.max(0.1f, currentContrast);  //-100일 때 완전 회색이 되는 것을 막기 위해 설정함
                tempContrast = currentContrast;
                break;
            case SHARPNESS:
                currentSharpness = value / 100f;
                tempSharpness = currentSharpness;
                break;
            case SATURATION:
                currentSaturation = (value / 100f) + 1.0f;
                currentSaturation = Math.max(0.0f, currentSaturation);
                tempSaturation = currentSaturation;
                break;
        }

        if (glSurfaceView != null) glSurfaceView.requestRender();
    }

    //조절 취소한 경우
    public void cancelValue(FilterActivity.Type type) {
        switch (type) {
            case BRIGHTNESS:
                tempBrightness = currentBrightness;
                break;
            case CONTRAST:
                tempContrast = currentContrast;
                break;
            case SHARPNESS:
                tempSharpness = currentSharpness;
                break;
            case SATURATION:
                tempSaturation = currentSaturation;
                break;
        }

        if (glSurfaceView != null) glSurfaceView.requestRender();
    }

    //모든 필터값 초기화, 이미지를 새로 불러올 때
    public void resetAllFilter() {
        currentBrightness = 0f;
        tempBrightness = 0f;

        currentContrast = 1.0f;
        tempContrast = 1.0f;

        currentSharpness = 0f;
        tempSharpness = 0f;

        currentSaturation = 1.0f;
        tempSaturation = 1.0f;

        if (glSurfaceView != null) glSurfaceView.requestRender();
    }
}