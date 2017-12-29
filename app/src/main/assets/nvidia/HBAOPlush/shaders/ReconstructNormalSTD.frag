#include "ConstantBuffers.glsl"
layout(binding = 0) uniform sampler2D u_depthTex;

vec3 NDCToViewspace( vec2 pos, float viewspaceDepth )
{
    vec3 ret;

    ret.xy = (g_f2UVToViewA * pos.xy + g_f2UVToViewB) * viewspaceDepth;
    ret.z = viewspaceDepth;

//    UV = g_f2UVToViewA * UV + g_f2UVToViewB;
//        return float3(UV * viewDepth, viewDepth);

    return ret;
}


vec4 CalculateEdges( const float centerZ, const float leftZ, const float rightZ, const float topZ, const float bottomZ )
{
    // slope-sensitive depth-based edge detection
    vec4 edgesLRTB = vec4( leftZ, rightZ, topZ, bottomZ ) - centerZ;
    vec4 edgesLRTBSlopeAdjusted = edgesLRTB + edgesLRTB.yxwz;
    edgesLRTB = min( abs( edgesLRTB ), abs( edgesLRTBSlopeAdjusted ) );
    return clamp( ( 1.3 - edgesLRTB / (centerZ * 0.040) ), vec4(0), vec4(1));
}

vec3 CalculateNormal(vec4 edgesLRTB, vec3 pixCenterPos, vec3 pixLPos, vec3 pixRPos, vec3 pixTPos, vec3 pixBPos )
{
    // Get this pixel's viewspace normal
    vec4 acceptedNormals  = vec4( edgesLRTB.x*edgesLRTB.z, edgesLRTB.z*edgesLRTB.y, edgesLRTB.y*edgesLRTB.w, edgesLRTB.w*edgesLRTB.x );

    pixLPos = normalize(pixLPos - pixCenterPos);
    pixRPos = normalize(pixRPos - pixCenterPos);
    pixTPos = normalize(pixTPos - pixCenterPos);
    pixBPos = normalize(pixBPos - pixCenterPos);

    vec3 pixelNormal = vec3( 0, 0, 0.0005 );
    pixelNormal += ( acceptedNormals.x ) * cross( pixLPos, pixTPos );
    pixelNormal += ( acceptedNormals.y ) * cross( pixTPos, pixRPos );
    pixelNormal += ( acceptedNormals.z ) * cross( pixRPos, pixBPos );
    pixelNormal += ( acceptedNormals.w ) * cross( pixBPos, pixLPos );
    pixelNormal = normalize( pixelNormal );

    return pixelNormal;
}

layout(location = 0) out float4 OutColor;
void main()
{
	ivec2 loc   = ivec2(gl_FragCoord.xy);
	float depth       = texelFetchOffset(u_depthTex, loc, 0, ivec2(0)).r;
	float depthLeft   = texelFetchOffset(u_depthTex, loc, 0, ivec2(-1,0)).r;
	float depthRight  = texelFetchOffset(u_depthTex, loc, 0, ivec2(1,0)).r;
	float depthTop    = texelFetchOffset(u_depthTex, loc, 0, ivec2(0,-1)).r;
	float depthBottom = texelFetchOffset(u_depthTex, loc, 0, ivec2(0,1)).r;

	ivec2 viewport    = textureSize(u_depthTex, 0);
    vec2 screenUV        = vec2(gl_FragCoord.xy + vec2(0)) / vec2(viewport);
    vec2 screenUVLeft    = vec2(gl_FragCoord.xy + vec2(-1,0)) / vec2(viewport);
    vec2 screenUVRight   = vec2(gl_FragCoord.xy + vec2(1,0)) / vec2(viewport);
    vec2 screenUVTop     = vec2(gl_FragCoord.xy + vec2(0,-1)) / vec2(viewport);
    vec2 screenUVBottom  = vec2(gl_FragCoord.xy + vec2(0,1)) / vec2(viewport);

//    depth = ScreenSpaceToViewSpaceDepth(depth);
//    depthLeft = ScreenSpaceToViewSpaceDepth(depthLeft);
//    depthRight = ScreenSpaceToViewSpaceDepth(depthRight);
//    depthTop = ScreenSpaceToViewSpaceDepth(depthTop);
//    depthBottom = ScreenSpaceToViewSpaceDepth(depthBottom);

    vec3 worldPos  = NDCToViewspace(screenUV, -depth);
    vec3 worldPosLeft  = NDCToViewspace(screenUVLeft, -depthLeft);
    vec3 worldPosRight  = NDCToViewspace(screenUVRight, -depthRight);
    vec3 worldPosTop  = NDCToViewspace(screenUVTop, -depthTop);
    vec3 worldPosBottom  = NDCToViewspace(screenUVBottom, -depthBottom);

    vec4 edges0 = CalculateEdges(depth, depthLeft, depthRight, depthTop, depthBottom );
	vec3 norm0 = CalculateNormal( edges0, worldPos, worldPosLeft, worldPosRight, worldPosTop, worldPosBottom );

	OutColor.rgb = norm0*0.5+0.5;
	OutColor.a = 0.0;
}