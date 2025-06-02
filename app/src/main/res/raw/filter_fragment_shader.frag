//프래그먼트 쉐이더 코드

precision mediump float;    //실수형 중간 정밀도 사용
varying vec2 vTexCoord;     //버텍스 쉐이더의 텍스쳐 좌표 저장되어있음, 버텍스 쉐이더와 프래그먼트 쉐이더를 이어주는 역할
uniform sampler2D uTexture; //2D 텍스쳐 샘플러 (텍스쳐로 불러온 이미지에 픽셀 좌표 기반으로 접근)
uniform float uBrightness;  //밝기값
uniform float uExposure;    //노출값
uniform float uContrast;    //대비값
uniform float uSharpness;   //선명하게
uniform vec2 uResolution;   //화면 해상도
uniform float uSaturation;  //채도

void main() {
    vec4 color = texture2D(uTexture, vTexCoord);    //uTexture(불러온 이미지)에서 vTexCoord 이 위치의 색상을 읽음

    //밝기값 조절
    float brightnessFactor = 1.0 + uBrightness * 0.8;
    color.rgb = pow(color.rgb, vec3(1.0 / brightnessFactor));
    color.rgb = clamp(color.rgb, 0.0, 1.0);

    //대비값 조절
    color.rgb = (color.rgb - 0.5) * uContrast + 0.5;
    color.rgb=clamp(color.rgb, 0.0, 1.0);

    //선명하게값 조절
    vec2 texOffset = 1.0 / uResolution;
    vec4 topLeft = texture2D(uTexture, vTexCoord + texOffset * vec2(-1, -1));
    vec4 topRight = texture2D(uTexture, vTexCoord + texOffset * vec2(1, -1));
    vec4 bottomLeft = texture2D(uTexture, vTexCoord + texOffset * vec2(-1, 1));
    vec4 bottomRight = texture2D(uTexture, vTexCoord + texOffset * vec2(1, 1));
    vec4 sharpened = (color * 5.0 - topLeft - topRight - bottomLeft - bottomRight);
    color.rgb = mix(color.rgb, sharpened.rgb, uSharpness);
    color.rgb=clamp(color.rgb, 0.0, 1.0);

    //채도값 조절
    float gray = dot(color.rgb, vec3(0.2126, 0.7152, 0.0722));
    vec3 grayScale = vec3(gray);
    color.rgb = mix(grayScale, color.rgb, uSaturation);
    color.rgb=clamp(color.rgb, 0.0, 1.0);

    gl_FragColor = color;   //gl_FragColor(openGL 내장 변수, 화면 색상값, 쉐이더 색상 출력)을 지정한 색상값으로 변경
}