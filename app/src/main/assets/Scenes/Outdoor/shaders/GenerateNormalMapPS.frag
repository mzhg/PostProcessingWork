#version 430

#include "Terrain.frag"

layout(location = 0) out float4 OutColor;

in vec4 UVAndScreenPos;

uniform vec4 g_NMGenerationAttribs;

#define m_fSampleSpacingInterval g_NMGenerationAttribs.x
#define m_fMIPLevel 	  g_NMGenerationAttribs.y
#define m_fElevationScale g_NMGenerationAttribs.z
#define HEIGHT_MAP_SCALE 65535.f

float3 ComputeNormal(float2 f2ElevMapUV,
                     float fSampleSpacingInterval,
                     float fMIPLevel)
{
//#define GET_ELEV(Offset) g_tex2DElevationMap.SampleLevel( samPointClamp, f2ElevMapUV, fMIPLevel, Offset)
#define GET_ELEV(Offset) textureLodOffset(g_tex2DElevationMap, f2ElevMapUV, fMIPLevel, Offset).r;


#if 1
    float Height00 = GET_ELEV( int2( -1, -1) );
    float Height10 = GET_ELEV( int2(  0, -1) );
    float Height20 = GET_ELEV( int2( +1, -1) );

    float Height01 = GET_ELEV( int2( -1, 0) );
    //float Height11 = GET_ELEV( int2(  0, 0) );
    float Height21 = GET_ELEV( int2( +1, 0) );

    float Height02 = GET_ELEV( int2( -1, +1) );
    float Height12 = GET_ELEV( int2(  0, +1) );
    float Height22 = GET_ELEV( int2( +1, +1) );

    float3 Grad;
    Grad.x = (Height00+Height01+Height02) - (Height20+Height21+Height22);
    Grad.y = (Height00+Height10+Height20) - (Height02+Height12+Height22);
    Grad.z = fSampleSpacingInterval * 6.f;
    //Grad.x = (3*Height00+10*Height01+3*Height02) - (3*Height20+10*Height21+3*Height22);
    //Grad.y = (3*Height00+10*Height10+3*Height20) - (3*Height02+10*Height12+3*Height22);
    //Grad.z = fSampleSpacingInterval * 32.f;
#else
    float Height1 = GET_ELEV( int2( 1, 0) );
    float Height2 = GET_ELEV( int2(-1, 0) );
    float Height3 = GET_ELEV( int2( 0, 1) );
    float Height4 = GET_ELEV( int2( 0,-1) );
       
    float3 Grad;
    Grad.x = Height2 - Height1;
    Grad.y = Height4 - Height3;
    Grad.z = fSampleSpacingInterval * 2.f;
#endif
    Grad.xy *= HEIGHT_MAP_SCALE*m_fElevationScale;
    float3 Normal = normalize( Grad );

#undef GET_ELEV
    return Normal;
}

void main()
{
	float2 f2UV = UVAndScreenPos.xy; //float2(0.5,0.5) + float2(0.5,-0.5) * In.m_f2PosPS.xy;
    float3 Normal = ComputeNormal( f2UV, m_fSampleSpacingInterval*exp2(m_fMIPLevel), m_fMIPLevel );
    // Only xy components are stored. z component is calculated in the shader
    OutColor = float4(Normal.xy,0,0);
}