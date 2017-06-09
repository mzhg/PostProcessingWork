#include "PostProcessingCommonPS.frag"

#ifndef DEPTHLINEARIZE_USEMSAA
#define DEPTHLINEARIZE_USEMSAA 0
#endif

#if DEPTHLINEARIZE_USEMSAA
uniform int g_SampleIndex;
uniform sampler2DMS g_DepthTexture;
#else
uniform sampler2D g_DepthTexture;
#endif

uniform float2 g_Uniforms;

#define g_fFarPlaneZ g_Uniforms.x
#define g_fNearPlaneZ g_Uniforms.y

void main()
{
#if DEPTHLINEARIZE_USEMSAA
    float dDepth = texelFetch(g_DepthTexture, ivec2(gl_FragCoord.xy), g_SampleIndex).r;
#else
    float dDepth = textureLod(g_DepthTexture, m_f4UVAndScreenPos.xy, 0.0).r;
#endif

#if 0
    if(dDepth == 1.0)
    {
        Out_f4Color = vec4(g_fFarPlaneZ + 1000.0);
        return;
    }
#endif

    float mZFar =g_fFarPlaneZ;
    float mZNear = g_fNearPlaneZ;
    float fCamSpaceZ = mZFar*mZNear/(mZFar-dDepth*(mZFar-mZNear));
    Out_f4Color = vec4(fCamSpaceZ);
}