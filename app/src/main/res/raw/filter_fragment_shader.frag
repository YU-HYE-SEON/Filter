//프래그먼트 쉐이더 코드

precision mediump float;//실수형 중간 정밀도 사용
varying vec2 vTexCoord;//버텍스 쉐이더의 텍스쳐 좌표 저장되어있음, 버텍스 쉐이더와 프래그먼트 쉐이더를 이어주는 역할
uniform sampler2D uTexture;//2D 텍스쳐 샘플러 (텍스쳐로 불러온 이미지에 픽셀 좌표 기반으로 접근)
uniform float uBrightness;  //밝기
uniform float uExposure;    //노출
uniform float uContrast;    //대비
uniform float uHighlight;  //하이라이트
uniform float uShadow;     //그림자
uniform float uTemperature; //온도
uniform float uTint;        //색조
uniform float uSaturation;  //채도
uniform float uSharpness;   //선명하게
uniform float uBlur;        //흐리게
uniform float uVignette;    //비네트
uniform float uNoise;       //노이즈
uniform vec2 uResolution;   //화면 해상도

void main() {
    vec4 baseColor = texture2D(uTexture, vTexCoord);
    vec3 color = baseColor.rgb;

    //밝기
    color += uBrightness * 0.4;

    //노출
    color *= pow(2.0, uExposure);

    //그림자 & 하이라이트
    float luminance = dot(color, vec3(0.299, 0.587, 0.114));
    if (luminance < 0.4) {
        float shadowStrength = (0.4 - luminance) / 0.4;
        color = mix(color, vec3(0.0), -uShadow * shadowStrength);
    }
    if (luminance > 0.6) {
        float highlightStrength = (luminance - 0.6) / 0.4;
        color = mix(color, vec3(1.0), uHighlight * highlightStrength);
    }

    //대비
    color = (color - 0.5) * (1.0 + uContrast) + 0.5;

    //온도 & 색조
    color.r += uTemperature * 0.1;
    color.b -= uTemperature * 0.1;
    color.g -= uTint * 0.1;

    //흐리게 & 선명하게
    vec2 offset = 1.0 / uResolution;
    vec3 blurColor = (
        texture2D(uTexture, vTexCoord + offset * vec2(-1, -1)).rgb +
        texture2D(uTexture, vTexCoord + offset * vec2(1, -1)).rgb +
        texture2D(uTexture, vTexCoord + offset * vec2(-1, 1)).rgb +
        texture2D(uTexture, vTexCoord + offset * vec2(1, 1)).rgb
    ) / 4.0;
    color = mix(color, blurColor, clamp(uBlur, 0.0, 1.0));

    vec3 sharpened = color + (color - blurColor) * 1.5;
    color = mix(color, sharpened, clamp(uSharpness, 0.0, 1.0));

    //채도
    float gray = dot(color, vec3(0.2126, 0.7152, 0.0722));
    color = mix(vec3(gray), color, clamp(uSaturation, 0.0, 2.0));

    //비네트
    vec2 pos = vTexCoord - vec2(0.5);
    float dist = dot(pos, pos);
    float vignette = 1.0 - clamp(uVignette * dist * 2.0, 0.0, 1.0);
    color *= vignette;

    //노이즈

    color = clamp(color, 0.0, 1.0);

    gl_FragColor = vec4(color, baseColor.a);
}