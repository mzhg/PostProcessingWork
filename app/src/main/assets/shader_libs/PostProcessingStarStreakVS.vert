#include "PostProcessingCommon.glsl"

#if ENABLE_VERTEX_ID

// Triangle
const vec2 TriangleVertices[3] = vec2[3]
(
    vec2(-1.0, -1.0),
    vec2( 3.0, -1.0),
    vec2(-1.0,  3.0)
);

const vec2 QuadVertices[4] = vec2[4]
(
    vec2(-1.0, -1.0),
    vec2( 1.0, -1.0),
    vec2(-1.0,  1.0),
    vec2( 1.0,  1.0)
);
#else
	attribute vec4 In_f4Postion;
#endif

#if ENABLE_IN_OUT_FEATURE
	out vec4 m_f4UVAndScreenPos;

	out vec2 m_TexCoord1;
	out vec2 m_TexCoord2;
	out vec2 m_TexCoord3;
	out vec2 m_TexCoord4;

	out gl_PerVertex
	{
		vec4 gl_Position;
	};
#else
	varying vec4 m_f4UVAndScreenPos;

	varying vec2 m_TexCoord1;
    varying vec2 m_TexCoord2;
    varying vec2 m_TexCoord3;
    varying vec2 m_TexCoord4;
#endif

#if ENABLE_POS_TRANSFORM
uniform mat4 g_PosTransform;
#endif

uniform vec4 g_Uniforms;
#define stepSize g_Uniforms.xy
#define Stride   g_Uniforms.z

void main()
{
#if ENABLE_VERTEX_ID
    #if ENABLE_POS_TRANSFORM
        gl_Position = vec4(QuadVertices[gl_VertexID], 0, 1);
        m_f4UVAndScreenPos = vec4(0.5 * gl_Position.xy + 0.5, gl_Position.xy);
    #else
        m_f4UVAndScreenPos.xy = vec2((gl_VertexID << 1) & 2, gl_VertexID & 2);
        gl_Position = vec4(m_f4UVAndScreenPos.xy * 2.0 - 1.0, 0, 1);
        m_f4UVAndScreenPos.zw = gl_Position.xy;
    #endif
#else
	gl_Position = In_f4Postion;
	m_f4UVAndScreenPos = vec4(0.5 * In_f4Postion.xy + 0.5, In_f4Postion.xy);
#endif

    m_TexCoord1 = m_f4UVAndScreenPos.xy;
    m_TexCoord2 = m_f4UVAndScreenPos.xy + stepSize*Stride;
    m_TexCoord3 = m_f4UVAndScreenPos.xy + stepSize*2.0*Stride;
    m_TexCoord4 = m_f4UVAndScreenPos.xy + stepSize*3.0*Stride;

#if ENABLE_POS_TRANSFORM
//    gl_Position.xy = clamp(gl_Position.xy, vec2(-1), vec2(1));
    gl_Position = g_PosTransform * gl_Position;
#endif
}



