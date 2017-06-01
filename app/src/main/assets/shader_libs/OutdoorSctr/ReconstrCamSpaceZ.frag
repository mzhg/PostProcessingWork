
#include "Scattering.frag"

in float4 UVAndScreenPos;

layout(location = 0) out float OutColor;

void main()
{
#if TEST_STATIC_SCENE == 1
	float fDepth = texelFetch(g_tex2DDepthBuffer, int2(gl_FragCoord.xy), 0).r;
	//	float fDepth = textureLod(g_tex2DDepthBuffer, UVAndScreenPos.xy, 0.0).r;
    float fCamSpaceZ = g_Proj[3][2]/(fDepth - g_Proj[2][2]);
    OutColor = fCamSpaceZ;
#else

	float dDepth = textureLod(g_tex2DDepthBuffer, UVAndScreenPos.xy, 0.0).r;

	if(dDepth == 1.0)
	{
		OutColor = g_fFarPlaneZ + 1000.0;
		return;
	}

	float mZFar =g_fFarPlaneZ;
	float mZNear = g_fNearPlaneZ;
	float fCamSpaceZ = mZFar*mZNear/(mZFar-dDepth*(mZFar-mZNear));
	OutColor = fCamSpaceZ;
 #endif
}
