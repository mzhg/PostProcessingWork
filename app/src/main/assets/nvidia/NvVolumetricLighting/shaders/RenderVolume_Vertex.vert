#include "ShaderCommon.frag"

#define COARSE_CASCADE (CASCADECOUNT-1)

layout(location = 0) in float3 In_Position;

#if (SHADOWMAPTYPE == SHADOWMAPTYPE_ATLAS)
// Texture2D<float> tShadowMap : register(t1);
   uniform sampler2D  tShadowMap;
#elif (SHADOWMAPTYPE == SHADOWMAPTYPE_ARRAY)
// Texture2DArray<float> tShadowMap : register(t1);
   uniform sampler2DArray tShadowMap;
#endif

float SampleShadowMap(float2 tex_coord, int cascade)
{
	float depth_value = 1.0f;
#if 1  // TODO
	float2 lookup_coord = g_vElementOffsetAndScale[cascade].zw * tex_coord + g_vElementOffsetAndScale[cascade].xy;
#else
	float2 lookup_coord = tex_coord;
#endif

#if (SHADOWMAPTYPE == SHADOWMAPTYPE_ATLAS)
//	depth_value = tShadowMap.SampleLevel( sBilinear, lookup_coord, 0).x;
	depth_value = textureLod(tShadowMap, lookup_coord, 0.0).x;
#elif (SHADOWMAPTYPE == SHADOWMAPTYPE_ARRAY)
//	depth_value = tShadowMap.SampleLevel( sBilinear, float3( lookup_coord, (float)g_uElementIndex[cascade] ), 0).x;
	depth_value = textureLod(tShadowMap, float3( lookup_coord, float(g_uElementIndex[cascade]) ), 0.0).x;
#endif
	return depth_value;
}

float3 ParaboloidProject(float3 P, float zNear, float zFar)
{
	float3 outP;
	float lenP = length(P.xyz);
	outP.xyz = P.xyz/lenP;
	outP.x = outP.x / (outP.z + 1.0);
	outP.y = outP.y / (outP.z + 1.0);
	outP.z = (lenP - zNear) / (zFar - zNear);
	return outP;
}

float3 ParaboloidUnproject(float3 P, float zNear, float zFar)
{
	// Use a quadratic to find the Z component
	// then reverse the projection to find the unit vector, and scale
	float L = P.z*(zFar-zNear) + zNear;

	float qa = P.x*P.x + P.y*P.y + 1;
	float qb = 2*(P.x*P.x + P.y*P.y);
	float qc = P.x*P.x + P.y*P.y - 1;
	float z = (-qb + sqrt(qb*qb - 4*qa*qc)) / (2*qa);

	float3 outP;
	outP.x = P.x * (z + 1);
	outP.y = P.y * (z + 1);
	outP.z = z;
	return outP*L;
}

out float4 vWorldPos;

void main()
{
    float3 vClipIn = In_Position;

    vWorldPos = mul(g_mLightToWorld, vec4(vClipIn.xy,1, 1));
    vWorldPos /= vWorldPos.w;

    if (VOLUMETYPE == VOLUMETYPE_FRUSTUM)
    {
        if (all(lessThan(abs(vClipIn.xy), float2(EDGE_FACTOR))))
        {
            int iCascade = -1;
            float4 vClipPos = float4(0,0,0,1);

            for (int i = COARSE_CASCADE;i >= 0; --i)
            {
                // Try to refetch from finer cascade
                float4 vClipPosCascade = mul( g_mLightProj[i], vWorldPos );
                vClipPosCascade *= 1.f / vClipPosCascade.w;
                if (all(lessThan(abs(vClipPosCascade.xy), float2(1.0f))))
                {

                    float2 vTex = float2(0.5*vClipPosCascade.x + 0.5, 0.5*vClipPosCascade.y + 0.5);  // TODO
                    float depthSample = SampleShadowMap(vTex, i);
                    if (depthSample < 1.0f)
                    {
                        vClipPos.xy = vClipPosCascade.xy;
                        vClipPos.z = depthSample;
                        iCascade = i;
                    }
                }
            }

            if (iCascade >= 0)
            {
                vClipPos.z = 2.0 * vClipPos.z - 1.0;
                vWorldPos = mul( g_mLightProjInv[iCascade], float4(vClipPos.xyz, 1) );
                vWorldPos *= 1.0 / vWorldPos.w;
                vWorldPos.xyz = g_vEyePosition + (1.0-g_fGodrayBias)*(vWorldPos.xyz-g_vEyePosition);
            }
        }
        else
        {
            vWorldPos = mul(g_mLightToWorld, float4(vClipIn.xy, 1, 1));
            vWorldPos *= 1.0 / vWorldPos.w;
        }
    }
    else if (VOLUMETYPE == VOLUMETYPE_PARABOLOID)
    {
        vClipIn.xyz = normalize(vClipIn.xyz);
        float4 shadowPos = mul(g_mLightProj[0], vWorldPos);
        shadowPos.xyz = shadowPos.xyz/shadowPos.w;
        int hemisphereID = (shadowPos.z > -1.0) ? 0 : 1; // TODO
        shadowPos.z = abs(shadowPos.z);
        shadowPos.xyz = ParaboloidProject(shadowPos.xyz, g_fLightZNear, g_fLightZFar);
        float2 shadowTC = 0.5f * shadowPos.xy + 0.5f;
        float depthSample = SampleShadowMap(shadowTC, hemisphereID);
//        depthSample = max(1.0, depthSample);
        float sceneDepth = depthSample*(g_fLightZFar-g_fLightZNear)+g_fLightZNear;
        vWorldPos = mul( g_mLightProjInv[0], float4(vClipIn.xyz * sceneDepth, 1));
        vWorldPos *= 1.0f / vWorldPos.w;
    }

    // Transform world position with viewprojection matrix
//	output.vWorldPos = vWorldPos;
//    output.vPos = mul( g_mViewProj, output.vWorldPos );
//    return output;
    gl_Position = mul( g_mViewProj, vWorldPos );
}