precision mediump float;
uniform sampler2D uTexture;
varying vec2 vTexCoord;

uniform float uBrightness;
uniform float uExposure;
uniform float uContrast;
uniform float uSharpness;
uniform vec2 uResolution;
uniform float uSaturation;

void main() {
    vec4 texColor = texture2D(uTexture, vTexCoord);
    vec4 color = texColor;

    float brightnessFactor = 1.0 + uBrightness * 0.8;
    color.rgb = pow(color.rgb, vec3(1.0 / brightnessFactor));
    color.rgb = clamp(color.rgb, 0.0, 1.0);

    color.rgb = (color.rgb - 0.5) * uContrast + 0.5;
    color.rgb = clamp(color.rgb, 0.0, 1.0);

    vec2 texOffset = 1.0 / uResolution;
    vec4 topLeft = texture2D(uTexture, vTexCoord + texOffset * vec2(-1, -1));
    vec4 topRight = texture2D(uTexture, vTexCoord + texOffset * vec2(1, -1));
    vec4 bottomLeft = texture2D(uTexture, vTexCoord + texOffset * vec2(-1, 1));
    vec4 bottomRight = texture2D(uTexture, vTexCoord + texOffset * vec2(1, 1));
    vec4 sharpened = (color * 5.0 - topLeft - topRight - bottomLeft - bottomRight);
    color.rgb = mix(color.rgb, sharpened.rgb, uSharpness);
    color.rgb = clamp(color.rgb, 0.0, 1.0);

    float gray = dot(color.rgb, vec3(0.2126, 0.7152, 0.0722));
    vec3 grayScale = vec3(gray);
    color.rgb = mix(grayScale, color.rgb, uSaturation);
    color.rgb = clamp(color.rgb, 0.0, 1.0);

    gl_FragColor = color;
}