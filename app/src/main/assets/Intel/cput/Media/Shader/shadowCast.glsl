#include "../../../../shader_libs/PostProcessingHLSLCompatiable.glsl"

// ********************************************************************************************************
layout(binding = 0) uniform cbPerModelValues
{
    float4x4 World /*: WORLD*/;
    float4x4 WorldViewProjection /*: WORLDVIEWPROJECTION*/;
    float4x4 InverseWorld /*: INVERSEWORLD*/;
    float4   LightDirection;
    float4   EyePosition;
    float4x4 LightWorldViewProjection;
};

#if !defined(vsmain) && !defined(psmain)
#error  you must define the shader type: VSMain or PSMain
#endif

#if vsmain

layout(location = 0) in vec3 In_Position;
out gl_PerVertex
{
    vec4 gl_Position;
};

void main()
{
    gl_Position = mul( float4( In_Position, 1.0f), WorldViewProjection );
    //output.Pos.z -= 0.0001 * output.Pos.w;
    gl_Position.z += 0.00008 * gl_Position.w;
}

#elif psmain
layout(location = 0) out vec4 Out_Color;
void main()
{
    Out_Color = vec4(1);
}

#endif