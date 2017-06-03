
#include "Scattering.frag"

in float4 m_f4UVAndScreenPos;

layout(location = 0) out float2 f2XY;
layout(location = 1) out float fCamSpaceZ;

void main()
{
	float4 f4SliceEndPoints = 
//					g_tex2DSliceEndPoints.Load( int3(In.m_f4Pos.y,0,0) );
					texelFetch(g_tex2DSliceEndPoints, int2(gl_FragCoord.y,0), 0);
    
    // If slice entry point is outside [-1,1]x[-1,1] area, the slice is completely invisible
    // and we can skip it from further processing.
    // Note that slice exit point can lie outside the screen, if sample locations are optimized
    if( !IsValidScreenLocation(f4SliceEndPoints.xy) )
    {
        // Discard invalid slices
        // Such slices will not be marked in the stencil and as a result will always be skipped
        discard;
    }

    float2 f2UV = ProjToUV(m_f4UVAndScreenPos.zw);

    // Note that due to the rasterization rules, UV coordinates are biased by 0.5 texel size.
    //
    //      0.5     1.5     2.5     3.5
    //   |   X   |   X   |   X   |   X   |     ....       
    //   0       1       2       3       4   f2UV * f2TexDim
    //   X - locations where rasterization happens
    //
    // We need remove this offset:
    float fSamplePosOnEpipolarLine = f2UV.x - 0.5f / float(MAX_SAMPLES_IN_SLICE);
    // fSamplePosOnEpipolarLine is now in the range [0, 1 - 1/MAX_SAMPLES_IN_SLICE]
    // We need to rescale it to be in [0, 1]
    fSamplePosOnEpipolarLine *= float(MAX_SAMPLES_IN_SLICE) / (float(MAX_SAMPLES_IN_SLICE)-1.f);
    fSamplePosOnEpipolarLine = saturate(fSamplePosOnEpipolarLine);

    // Compute interpolated position between entry and exit points:
    f2XY = lerp(f4SliceEndPoints.xy, f4SliceEndPoints.zw, fSamplePosOnEpipolarLine);
    if( !IsValidScreenLocation(f2XY) )
    {
        // Discard pixels that fall behind the screen
        // This can happen if slice exit point was optimized
        discard;
    }

    // Compute camera space z for current location
    fCamSpaceZ = GetCamSpaceZ( ProjToUV(f2XY) );
}