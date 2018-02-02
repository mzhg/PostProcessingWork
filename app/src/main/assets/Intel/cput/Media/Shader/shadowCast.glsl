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
    /*mat4 transform = mat4(0.48480967, 0.0, -0.87461966, -3.8146973E-6,
                          -0.6499691, 0.66913056, -0.36028382, 0.0,
                          0.58523476, 0.74314487, 0.32440096, -140.0,
                          0.0, 0.0, 0.0, 1.0
    );*/

    mat4 transform = mat4(1.170434, 0.0, -2.1115186, -9.209493E-6,
                          -1.5691642, 1.615424, -0.86980206, 0.0,
                          -0.5853518, -0.7432935, -0.32446584, 117.03904,
                          -0.58523476, -0.74314487, -0.32440096, 140.0
    );

    /*mat4 transform = mat4(4.6827307E-5, 0.0, -8.4478685E-5, -0.081664205,
                          -1.1806659E-4, 1.21547266E-4, -6.544539E-5, 0.99471265,
                          -9.049932E-5, -1.1491817E-4, -5.01646E-5, 0.8670116,
                          0.0, 0.0, 0.0, 1.0
    );*/
    transform = transpose(transform);
//    gl_Position = mul( float4( In_Position, 1.0f), transform );
    gl_Position = mul( float4( In_Position, 1.0f), WorldViewProjection );
//    gl_Position.z += 0.00008 * gl_Position.w;
}

#elif psmain
layout(location = 0) out vec4 Out_Color;
void main()
{
    Out_Color = vec4(1);
}

#endif