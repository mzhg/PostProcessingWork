
#include "Scattering.frag"

in float4 m_f4UVAndScreenPos;

layout(location = 0) out float4 OutColor;

void main()
{
	const float4 g_f4IncorrectSliceUVDirAndStart = float4(-10000, -10000, 0, 0);
	uint uiSliceInd = uint(gl_FragCoord.x);
    // Load epipolar slice endpoints
    float4 f4SliceEndpoints = 
//    				g_tex2DSliceEndPoints.Load(  uint3(uiSliceInd,0,0) );
    				texelFetch(g_tex2DSliceEndPoints, int2(uiSliceInd, 0), 0);
    // All correct entry points are completely inside the [-1+1/W,1-1/W]x[-1+1/H,1-1/H] area
    if( !IsValidScreenLocation(f4SliceEndpoints.xy) )
    {
        OutColor= g_f4IncorrectSliceUVDirAndStart;
        return;
	}
//    uint uiCascadeInd = In.m_f4Pos.y;
	uint uiCascadeInd = uint(gl_FragCoord.y);
    mat4 mWorldToShadowMapUVDepth = g_WorldToShadowMapUVDepth[uiCascadeInd];

    // Reconstruct slice exit point position in world space
    float3 f3SliceExitWS = ProjSpaceXYZToWorldSpace( float3(f4SliceEndpoints.zw, g_f4ShadowAttribs_Cascades_StartEndZ[uiCascadeInd].y) );
    // Transform it to the shadow map UV
    float2 f2SliceExitUV = WorldSpaceToShadowMapUV(f3SliceExitWS, mWorldToShadowMapUVDepth).xy;
    
    // Compute camera position in shadow map UV space
    float2 f2SliceOriginUV = WorldSpaceToShadowMapUV(g_f4CameraPos.xyz, mWorldToShadowMapUVDepth).xy;

    // Compute slice direction in shadow map UV space
    float2 f2SliceDir = f2SliceExitUV - f2SliceOriginUV;
    f2SliceDir /= max(abs(f2SliceDir.x), abs(f2SliceDir.y));
    
    float4 f4BoundaryMinMaxXYXY = float4(0,0,1,1) + float4(0.5, 0.5, -0.5, -0.5)*g_f2ShadowMapTexelSize.xyxy;
    if( any( lessThan((f2SliceOriginUV.xyxy - f4BoundaryMinMaxXYXY) * float4( 1, 1, -1, -1), float4(0)) ) )
    {
        // If slice origin in UV coordinates falls beyond [0,1]x[0,1] region, we have
        // to continue the ray and intersect it with this rectangle
        //                  
        //    f2SliceOriginUV
        //       *
        //        \
        //         \  New f2SliceOriginUV
        //    1   __\/___
        //       |       |
        //       |       |
        //    0  |_______|
        //       0       1
        //           
        
        // First, compute signed distances from the slice origin to all four boundaries
        bool4 b4IsValidIsecFlag = greaterThan(abs(f2SliceDir.xyxy), float4( 1e-6));
        float4 f4DistToBoundaries = (f4BoundaryMinMaxXYXY - f2SliceOriginUV.xyxy) / (f2SliceDir.xyxy + float4(!b4IsValidIsecFlag));

        //We consider only intersections in the direction of the ray
        b4IsValidIsecFlag = b4IsValidIsecFlag && greaterThan(f4DistToBoundaries, float4(0));
        // Compute the second intersection coordinate
        float4 f4IsecYXYX = f2SliceOriginUV.yxyx + f4DistToBoundaries * f2SliceDir.yxyx;
        
        // Select only these coordinates that fall onto the boundary
        b4IsValidIsecFlag = b4IsValidIsecFlag && greaterThanEqual(f4IsecYXYX, f4BoundaryMinMaxXYXY.yxyx) && lessThanEqual(f4IsecYXYX, f4BoundaryMinMaxXYXY.wzwz);
        // Replace distances to all incorrect boundaries with the large value
        f4DistToBoundaries = float4(b4IsValidIsecFlag) * f4DistToBoundaries + 
                             float4(!b4IsValidIsecFlag) * float4(+FLT_MAX, +FLT_MAX, +FLT_MAX, +FLT_MAX);
        // Select the closest valid intersection
        float2 f2MinDist = min(f4DistToBoundaries.xy, f4DistToBoundaries.zw);
        float fMinDist = min(f2MinDist.x, f2MinDist.y);
        
        // Update origin
        f2SliceOriginUV = f2SliceOriginUV + fMinDist * f2SliceDir;
    }
    
    f2SliceDir *= g_f2ShadowMapTexelSize;

    OutColor = float4(f2SliceDir, f2SliceOriginUV);
}

