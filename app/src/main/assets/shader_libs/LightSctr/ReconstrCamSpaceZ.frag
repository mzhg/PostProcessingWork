
#include "PostProcessingLightScatteringCommon.frag"

layout(location = 0) out float OutColor;

void main()
{

#if TEST_STATIC_SCENE == 0
	float dDepth = texelFetch(g_tex2DDepthBuffer, int2(gl_FragCoord.xy), 0).r;
	
	/*
	if(dDepth == 1.0)
	{
		OutColor = g_fFarPlaneZ;
		return;
	}*/

	float mZFar =g_fFarPlaneZ;
	float mZNear = g_fNearPlaneZ;
	float fCamSpaceZ = mZFar*mZNear/(mZFar-dDepth*(mZFar-mZNear));
	OutColor = fCamSpaceZ;
#else
	float fDepth = texelFetch(g_tex2DDepthBuffer, int2(gl_FragCoord.xy), 0).r;
//	float fDepth = textureLod(g_tex2DDepthBuffer, UVAndScreenPos.xy, 0.0).r;
    float fCamSpaceZ = g_Proj[3][2]/(fDepth - g_Proj[2][2]);
    OutColor = fCamSpaceZ;
#endif
}
