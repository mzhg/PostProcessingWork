
#include "PostProcessingLightScatteringCommon.frag"

in float4 UVAndScreenPos;

layout(location = 0) out float4 OutColor;

void main()
{
	int uiSliceInd = int(gl_FragCoord.x);
    // Load epipolar slice endpoints
    float4 f4SliceEndpoints = // g_tex2DSliceEndPoints.Load(  uint3(uiSliceInd,0,0) );
    						     texelFetch(g_tex2DSliceEndPoints, int2(uiSliceInd, 0), 0);
    // All correct entry points are completely inside the [-1,1]x[-1,1] area
    if( any( greaterThan(abs(f4SliceEndpoints.xy), float2(1)) ) )
    {
    	OutColor =  g_f4IncorrectSliceUVDirAndStart;
    	return;
    }

    // Reconstruct slice exit point position in world space
    float3 f3SliceExitWS = ProjSpaceXYToWorldSpace(f4SliceEndpoints.zw);
    float3 f3DirToSliceExitFromCamera = normalize(f3SliceExitWS - g_f4CameraPos.xyz);
    // Compute epipolar slice normal. If light source is outside the screen, the vectors could be collinear
    float3 f3SliceNormal = cross(f3DirToSliceExitFromCamera, g_f4DirOnLight.xyz);
    if( length(f3SliceNormal) < 1e-5 )
    {
    	OutColor = g_f4IncorrectSliceUVDirAndStart;
    	return;
    }
    f3SliceNormal = normalize(f3SliceNormal);

    // Intersect epipolar slice plane with the light projection plane.
    float3 f3IntersecOrig, f3IntersecDir;

#if LIGHT_TYPE == LIGHT_TYPE_POINT || LIGHT_TYPE == LIGHT_TYPE_SPOT
    // We can use any plane parallel to the light furstum near clipping plane. The exact distance from the plane
    // to light source does not matter since the projection will always be the same:
    float3 f3LightProjPlaneCenter = g_f4LightWorldPos.xyz + g_f4SpotLightAxisAndCosAngle.xyz;
#endif
    
    if( !PlanePlaneIntersect( 
#if LIGHT_TYPE == LIGHT_TYPE_DIRECTIONAL
                             // In case light is directional, the matrix is not perspective, so location
                             // of the light projection plane in space as well as camera position do not matter at all
                             f3SliceNormal, float3(0),
                             -g_f4DirOnLight.xyz, float3(0),
#elif LIGHT_TYPE == LIGHT_TYPE_POINT || LIGHT_TYPE == LIGHT_TYPE_SPOT
                             f3SliceNormal, g_f4CameraPos.xyz,
                             g_f4SpotLightAxisAndCosAngle.xyz, f3LightProjPlaneCenter,
#endif
                             f3IntersecOrig, f3IntersecDir ) )
    {
        // There is no correct intersection between planes in barelly possible case which
        // requires that:
        // 1. DirOnLight is exacatly parallel to light projection plane
        // 2. The slice is parallel to light projection plane
    	OutColor = g_f4IncorrectSliceUVDirAndStart;
    	return;
    }
    // Important: ray direction f3IntersecDir is computed as a cross product of 
    // slice normal and light direction (or spot light axis). As a result, the ray
    // direction is always correct for valid slices. 

    // Now project the line onto the light space UV coordinates. 
    // Get two points on the line:
    float4 f4P0 = float4( f3IntersecOrig, 1 );
    float4 f4P1 = float4( f3IntersecOrig + f3IntersecDir * max(1, length(f3IntersecOrig)), 1 );
    // Transform the points into the shadow map UV:
    f4P0 = mul( f4P0, g_WorldToLightProjSpace); 
    f4P0 /= f4P0.w;
    f4P1 = mul( f4P1, g_WorldToLightProjSpace); 
    f4P1 /= f4P1.w;
    // Note that division by w is not really necessary because both points lie in the plane 
    // parallel to light projection and thus have the same w value.
    float2 f2SliceDir = ProjToUV(f4P1.xy) - ProjToUV(f4P0.xy);
    
    // The following method also works:
    // Since we need direction only, we can use any origin. The most convinient is
    // f3LightProjPlaneCenter which projects into (0.5,0.5):
    //float4 f4SliceUVDir = mul( float4(f3LightProjPlaneCenter + f3IntersecDir, 1), g_LightAttribs.mWorldToLightProjSpace);
    //f4SliceUVDir /= f4SliceUVDir.w;
    //float2 f2SliceDir = ProjToUV(f4SliceUVDir.xy) - 0.5;

    f2SliceDir /= max(abs(f2SliceDir.x), abs(f2SliceDir.y));

    float2 f2SliceOriginUV = g_f4CameraUVAndDepthInShadowMap.xy;
    
#if LIGHT_TYPE == LIGHT_TYPE_POINT || LIGHT_TYPE == LIGHT_TYPE_SPOT
    bool bIsCamInsideCone = dot( -g_f4DirOnLight.xyz, g_f4SpotLightAxisAndCosAngle.xyz) > g_f4SpotLightAxisAndCosAngle.w;
    if( !bIsCamInsideCone )
    {
        // If camera is outside the cone, all the rays in slice hit the same cone side, which means that they
        // all start from projection of this rib onto the shadow map

        // Intesect the ray with the light cone:
        float2 f2ConeIsecs = 
            RayConeIntersect(g_f4LightWorldPos.xyz, g_f4SpotLightAxisAndCosAngle.xyz, g_f4SpotLightAxisAndCosAngle.w,
                             f3IntersecOrig, f3IntersecDir);
        
//        if( any(f2ConeIsecs == -FLT_MAX) )
		if(f2ConeIsecs.x == -FLT_MAX||f2ConeIsecs.y == -FLT_MAX)
        {
        	OutColor = g_f4IncorrectSliceUVDirAndStart;
        	return;
        }
        // Now select the first intersection with the cone along the ray
        float4 f4RayConeIsec = float4( f3IntersecOrig + min(f2ConeIsecs.x, f2ConeIsecs.y) * f3IntersecDir, 1 );
        // Project this intersection:
        f4RayConeIsec = mul( f4RayConeIsec, g_WorldToLightProjSpace);
        f4RayConeIsec /= f4RayConeIsec.w;

        f2SliceOriginUV = ProjToUV(f4RayConeIsec.xy);
    }
#endif

	OutColor = float4(f2SliceDir, f2SliceOriginUV);
}