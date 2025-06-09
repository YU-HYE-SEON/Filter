attribute vec4 aPosition;
attribute vec2 aTexCoord;
uniform mat4 uMVPMatrix; //스티커의 위치, 크기, 회전 정보를 담을 행렬
varying vec2 vTexCoord;

void main() {
    gl_Position = uMVPMatrix * aPosition;
    vTexCoord = aTexCoord;
}