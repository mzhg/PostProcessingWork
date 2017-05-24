
#version 410

#define FACE_NEAR  0
#define FACE_FAR   1
#define FACE_TOP   2
#define FACE_BOTTOM 3
#define FACE_LEFT  4
#define FACE_RIGHT 5

// noperspective
uniform vec4 f4FaceColors[6];
flat in int m_FaceID;

layout(location = 0) out vec4 OutColor;

void main()
{
	OutColor = f4FaceColors[m_FaceID];
}