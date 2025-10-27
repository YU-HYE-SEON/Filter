//프래그먼트 쉐이더 코드

precision mediump float;//실수형 중간 정밀도 사용
varying vec2 vTexCoord;//버텍스 쉐이더의 텍스쳐 좌표 저장되어있음, 버텍스 쉐이더와 프래그먼트 쉐이더를 이어주는 역할
uniform sampler2D uTexture;//2D 텍스쳐 샘플러 (텍스쳐로 불러온 이미지에 픽셀 좌표 기반으로 접근)
uniform float uBrightness;//밝기
uniform float uExposure;//노출
uniform float uContrast;//대비
uniform float uHighlight;//하이라이트
uniform float uShadow;//그림자
uniform float uTemperature;//온도
uniform float uHue;//색조
uniform float uSaturation;//채도
uniform float uSharpness;//선명하게
uniform float uBlur;//흐리게
uniform float uVignette;//비네트
uniform float uNoise;//노이즈
uniform vec2 uResolution;//화면 해상도

vec3 applyToneOps(vec3 c) {
    //밝기
    c += uBrightness * 0.4;

    //노출
    c *= pow(2.0, uExposure);

    //그림자 & 하이라이트
    float lum = dot(c, vec3(0.299, 0.587, 0.114));
    if (lum < 0.4) {
        float s = (0.4 - lum) / 0.4;
        c = mix(c, vec3(0.0), -uShadow * s);
    }
    if (lum > 0.6) {
        float h = (lum - 0.6) / 0.4;
        c = mix(c, vec3(1.0), uHighlight * h);
    }

    //대비
    c = (c - 0.5) * (1.0 + uContrast) + 0.5;

    //온도 & 색조
    c.r += uTemperature * 0.1;
    c.b -= uTemperature * 0.1;
    c.g -= uHue * 0.1;

    return c;
}

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
    color.g -= uHue * 0.1;

    //흐리게 & 선명하게
    vec2 offset = 1.0 / uResolution;
    float BLUR_RADIUS_SCALE = 4.0;
    float BLUR_STRENGTH = 1.3;
    float SHARP_AMOUNT = 3.0;
    float SHARP_STRENGTH = 1.3;
    vec2 r = offset * BLUR_RADIUS_SCALE;

    vec3 blurColor = (
    applyToneOps(texture2D(uTexture, vTexCoord + r * vec2(-1.0, -1.0)).rgb) +
    applyToneOps(texture2D(uTexture, vTexCoord + r * vec2(1.0, -1.0)).rgb) +
    applyToneOps(texture2D(uTexture, vTexCoord + r * vec2(-1.0, 1.0)).rgb) +
    applyToneOps(texture2D(uTexture, vTexCoord + r * vec2(1.0, 1.0)).rgb)
    ) / 4.0;
    color = mix(color, blurColor, clamp(uBlur * BLUR_STRENGTH, 0.0, 1.0));

    vec3 sharpened = color + (color - blurColor) * SHARP_AMOUNT;
    color = mix(color, sharpened, clamp(uSharpness * SHARP_STRENGTH, 0.0, 1.0));

    //채도
    float gray = dot(color, vec3(0.2126, 0.7152, 0.0722));
    color = mix(vec3(gray), color, clamp(uSaturation, 0.0, 2.0));

    //비네트
    vec2 pos = vTexCoord - vec2(0.5);
    float dist = dot(pos, pos);
    float vignette = 1.0 - clamp(uVignette * dist * 2.0, 0.0, 1.0);
    color *= vignette;

    //노이즈
    float nR = fract(sin(dot(gl_FragCoord.xy, vec2(12.9898, 78.233))) * 43758.5453123);
    float nG = fract(sin(dot(gl_FragCoord.xy + 17.0, vec2(12.9898, 78.233))) * 43758.5453123);
    float nB = fract(sin(dot(gl_FragCoord.xy + 31.0, vec2(12.9898, 78.233))) * 43758.5453123);
    vec3 nRGB = vec3(nR, nG, nB) * 2.0 - 1.0;
    float nMono = (nR + nG + nB) / 3.0 * 2.0 - 1.0;
    vec3 grain = mix(vec3(nMono), nRGB, 0.5);
    float lumaNow = dot(color, vec3(0.299, 0.587, 0.114));
    float shadowBoost = 0.6 + 0.4 * (1.0 - lumaNow);
    float noiseStrength = clamp(uNoise, 0.0, 1.0) * shadowBoost * 0.5;
    color += grain * noiseStrength;

    color = clamp(color, 0.0, 1.0);

    gl_FragColor = vec4(color, baseColor.a);
}