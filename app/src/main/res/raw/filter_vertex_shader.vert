//버텍스 쉐이더 코드

attribute vec4 aPosition;   //정점 좌표
attribute vec2 aTexCoord;   //텍스쳐 좌표
varying vec2 vTexCoord;     //프래그먼트 쉐이더와 연결 역할

void main() {
    gl_Position = aPosition;
    vTexCoord = aTexCoord;
}