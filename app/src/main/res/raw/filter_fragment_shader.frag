//프래그먼트 쉐이더 코드

precision mediump float;//실수형 중간 정밀도 사용
varying vec2 vTexCoord;//버텍스 쉐이더의 텍스쳐 좌표 저장되어있음, 버텍스 쉐이더와 프래그먼트 쉐이더를 이어주는 역할
uniform sampler2D uTexture;//2D 텍스쳐 샘플러 (텍스쳐로 불러온 이미지에 픽셀 좌표 기반으로 접근)
uniform float uBrightness;//밝기값
uniform float uExposure;//노출값
uniform float uContrast;//대비값
uniform float uSharpness;//선명하게
uniform vec2 uResolution;//화면 해상도
uniform float uSaturation;//채도

void main() {
    vec4 baseColor = texture2D(uTexture, vTexCoord);
    vec3 color = baseColor.rgb;

    color += uBrightness * 0.4;
    color = clamp(color, 0.0, 1.0);

    color = (color - 0.5) * (1.0 + uContrast) + 0.5;
    color = clamp(color, 0.0, 1.0);

    vec2 texOffset = 1.0 / uResolution;
    vec3 topLeft = texture2D(uTexture, vTexCoord + texOffset * vec2(-1, -1)).rgb;
    vec3 topRight = texture2D(uTexture, vTexCoord + texOffset * vec2(1, -1)).rgb;
    vec3 bottomLeft = texture2D(uTexture, vTexCoord + texOffset * vec2(-1, 1)).rgb;
    vec3 bottomRight = texture2D(uTexture, vTexCoord + texOffset * vec2(1, 1)).rgb;
    vec3 avgBlur = (topLeft + topRight + bottomLeft + bottomRight) / 4.0;
    vec3 sharpened = color + (color - avgBlur) * 1.5;
    color = mix(color, sharpened, clamp(uSharpness, 0.0, 1.0));
    color = clamp(color, 0.0, 1.0);

    float gray = dot(color, vec3(0.2126, 0.7152, 0.0722));
    vec3 grayScale = vec3(gray);
    color = mix(grayScale, color, clamp(uSaturation, 0.0, 2.0));
    color = clamp(color, 0.0, 1.0);

    gl_FragColor = vec4(color, baseColor.a);
}