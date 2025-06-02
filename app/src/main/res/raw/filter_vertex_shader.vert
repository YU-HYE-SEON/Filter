//버텍스 쉐이더 코드

attribute vec4 aPosition;   //정점 좌표
attribute vec2 aTexCoord;   //텍스쳐 좌표
varying vec2 vTexCoord;     //프래그먼트 쉐이더와 연결 역할

void main() {
    gl_Position = aPosition;    //gl_Position(openGL 내장 변수, 화면 공간 좌표, 쉐이더 렌더링 위치)를 지정한 좌표로 설정
    vTexCoord = aTexCoord;      //프래그먼트 쉐이더와 연결 역할하는 변수에 텍스쳐 좌표 저장
}