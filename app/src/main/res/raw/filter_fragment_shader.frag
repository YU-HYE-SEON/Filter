//프래그먼트 쉐이더 코드

precision mediump float;    //실수형 중간 정밀도 사용
varying vec2 vTexCoord;     //버텍스 쉐이더의 텍스쳐 좌표 저장되어있음, 버텍스 쉐이더와 프래그먼트 쉐이더를 이어주는 역할
uniform sampler2D uTexture; //2D 텍스쳐 샘플러 (텍스쳐로 불러온 이미지에 픽셀 좌표 기반으로 접근)
uniform float uBrightness;  //밝기값
uniform float uExposure;    //노출값
uniform float uContrast;    //대비값
uniform float uSharpness;   //선명하게
uniform float uSaturation;  //채도

void main() {
    vec4 color = texture2D(uTexture, vTexCoord);    //uTexture(불러온 이미지)에서 vTexCoord 이 위치의 색상을 읽음
    color.rgb += uBrightness;   //rgb ± 밝기값
    gl_FragColor = color;       //gl_FragColor(?)에 색상 저장
}