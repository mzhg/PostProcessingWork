
#version 400

#define FACE_NEAR  0
#define FACE_FAR   1
#define FACE_TOP   2
#define FACE_BOTTOM 3
#define FACE_LEFT  4
#define FACE_RIGHT 5
#define FACE_ALL   6

uniform mat4 g_LightToWorld;
uniform mat4 g_ViewProj;
uniform int iFaceID;

in int gl_VertexID;
in int gl_InstanceID;

flat out int m_FaceID;

vec2 getVertex(int idx)
{
	vec2 f2Pos = vec2(0);
	if(idx == 0)
	{
		f2Pos.xy = vec2(-1);
	}
	else if(idx == 1)
	{
		f2Pos.xy = vec2(1, -1);	
	}
	else if(idx == 2)
	{
		f2Pos.xy = vec2(-1, 1);
	}
	else // if(idx == 3)
	{
		f2Pos.xy = vec2(1);
	}
	
	return f2Pos;
}

void main()
{
	int idx = gl_VertexID;
	vec3 f3Pos = vec3(0);
	int _iFaceID = gl_InstanceID;
	if(iFaceID != FACE_ALL)
	{
		_iFaceID = iFaceID;
	}
	
	
	if(_iFaceID == FACE_NEAR)
	{
		f3Pos.xy = getVertex(idx);
		f3Pos.z = -1.0;
	}
	else if(_iFaceID == FACE_FAR)
	{
		f3Pos.xy = getVertex(idx);
		f3Pos.z = 1.0;
	}
	else if(_iFaceID == FACE_TOP)
	{
		f3Pos.xz = getVertex(idx);
		f3Pos.y = 1.0;
	}
	else if(_iFaceID == FACE_BOTTOM)
	{
		f3Pos.xz = getVertex(idx);
		f3Pos.y = -1.0;
	}
	else if(_iFaceID == FACE_LEFT)
	{
		f3Pos.yz = getVertex(idx);
		f3Pos.x = -1.0;
	}
	else if(_iFaceID == FACE_RIGHT)
	{
		f3Pos.yz = getVertex(idx);
		f3Pos.x = 1.0;
	}
	
	m_FaceID = _iFaceID;
	
	vec4 worldPos = g_LightToWorld * vec4(f3Pos, 1);
	worldPos /= worldPos.w;
	
	gl_Position = g_ViewProj * worldPos;
}