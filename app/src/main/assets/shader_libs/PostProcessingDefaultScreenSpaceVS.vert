#include "PostProcessingCommon.glsl"

#if ENABLE_VERTEX_ID
const vec2 QuadVertices[4] = vec2[4]
(
    vec2(-1.0, -1.0),
    vec2( 1.0, -1.0),
    vec2(-1.0,  1.0),
    vec2( 1.0,  1.0)
);

const vec2 QuadTexCoordinates[4] = vec2[4]
(
    vec2(0.0, 1.0),
    vec2(1.0, 1.0),
    vec2(0.0, 0.0),
    vec2(1.0, 0.0)
);

#else
	attribute vec4 In_f4Postion;
#endif

#if ENABLE_IN_OUT_FEATURE
	out vec4 m_f4UVAndScreenPos;

	out gl_PerVertex
	{
		vec4 gl_Position;
	};
#else
	varying vec4 m_f4UVAndScreenPos;
#endif

    
void main()
{
#if ENABLE_VERTEX_ID
	m_f4UVAndScreenPos.xy = vec2((gl_VertexID << 1) & 2, gl_VertexID & 2);
    gl_Position = vec4(UVAndScreenPos.xy * vec2(2,2) + vec2(-1,-1), 0, 1);
    m_f4UVAndScreenPos.zw = gl_Position.xy;
#else
	gl_Position = In_f4Postion;
	m_f4UVAndScreenPos = vec4(0.5 * gl_Position.xy + 0.5, gl_Position.xy);
#endif
}

	

