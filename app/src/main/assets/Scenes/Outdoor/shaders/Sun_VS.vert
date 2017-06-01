
#include "../../../shader_libs/OutdoorSctr/Base.frag"

out vec2 m_f2PosPS;  // Position in projection space [-1,1]x[-1,1]

const float fSunAngularRadius =  32./2. / 60. * ((2. * 3.1415926)/180.0); // Sun angular DIAMETER is 32 arc minutes
const float fTanSunAngularRadius = tan(fSunAngularRadius); 

// xy: g_CameraAttribs.mProj[0][0], g_CameraAttribs.mProj[1][1], zw: g_LightAttribs.f4LightScreenPos
uniform float4 uniformData;

void main()
{
	float2 fCotanHalfFOV = uniformData.xy; //float2( g_CameraAttribs.mProj[0][0], g_CameraAttribs.mProj[1][1] );
    float2 f2SunScreenPos = uniformData.zw; // g_LightAttribs.f4LightScreenPos.xy;
    float2 f2SunScreenSize = fTanSunAngularRadius * fCotanHalfFOV;
    float4 MinMaxUV = f2SunScreenPos.xyxy + float4(-1,-1,1,1) * f2SunScreenSize.xyxy;
 
 	/*
    SSunVSOutput Verts[4] = 
    {
        {float4(MinMaxUV.xy, 0.0, 1.0), MinMaxUV.xy}, 
        {float4(MinMaxUV.xw, 0.0, 1.0), MinMaxUV.xw},
        {float4(MinMaxUV.zy, 0.0, 1.0), MinMaxUV.zy},
        {float4(MinMaxUV.zw, 0.0, 1.0), MinMaxUV.zw}
    };
    */
    
    float4 Verts[4] = float4[4]
    (
    	float4(MinMaxUV.xy, 1.0, 1.0),
    	float4(MinMaxUV.zy, 1.0, 1.0),
    	float4(MinMaxUV.xw, 1.0, 1.0),
    	float4(MinMaxUV.zw, 1.0, 1.0)
    );

    gl_Position = Verts[gl_VertexID];
    m_f2PosPS   = gl_Position.xy;
}