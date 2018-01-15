
#include "GBuffer.glsl"
#include "AVSM_Gen.glsl"
#include "ListTexture.glsl"

layout(early_fragment_tests) in;

layout(location = 0) out vec4 OutColor;

//in VS_OUTPUT_HS_INPUT Input;
in _DynamicParticlePSIn
{
//    float4 Position  : SV_POSITION;
    float3 UVS		 /*: TEXCOORD0*/;
    float  Opacity	 /*: TEXCOORD1*/;
    float3 ViewPos	 /*: TEXCOORD2*/;
    float3 ObjPos    /*: TEXCOORD3*/;
    float3 ViewCenter/*: TEXCOORD4*/;
    float4 color      /*: COLOR*/;
    float2 ShadowInfo /*: TEXCOORD5*/;
}Input;

SurfaceData ConstructSurfaceData(float3 PosView, float3 Normal)
{
    SurfaceData Surface;
    Surface.positionView = PosView;
    Surface.positionViewDX = ddx(Surface.positionView);
    Surface.positionViewDY = ddy(Surface.positionView);
    Surface.normal = Normal;
    Surface.albedo = float4(0,0,0,1);
    Surface.lightSpaceZ = mul(float4(Surface.positionView.xyz, 1.0f), mCameraViewToLightProj).z;
    Surface.lightTexCoord = ProjectIntoLightTexCoord(Surface.positionView.xyz);
    Surface.lightTexCoordDX = ddx(Surface.lightTexCoord);
    Surface.lightTexCoordDY = ddy(Surface.lightTexCoord);

    return Surface;
}

void main()
{
    float3 entry, exit;
    float  shadowTerm = 1.0f;
    float  segmentTransmittance = 1.0f;
    DynamicParticlePSIn _Input;
    _Input.UVS = Input.UVS;
    _Input.Opacity = Input.Opacity;
    _Input.ViewPos = Input.ViewPos;
    _Input.ObjPos = Input.ObjPos;
    _Input.ViewCenter = Input.ViewCenter;
    _Input.color = Input.color;
    _Input.ShadowInfo = Input.ShadowInfo;

    if (IntersectDynamicParticle(_Input, entry, exit, segmentTransmittance))
    {
        float2 lightTexCoord = ProjectIntoLightTexCoord(entry);

        SurfaceData LitSurface = ConstructSurfaceData(entry, 0.0f.xxx);

        //return float4( LitSurface.positionView.xy, 1, 1 );
        //return float4( ProjectIntoAvsmLightTexCoord( LitSurface.positionView.xyz ).xy, 1, 1 );

        shadowTerm = ShadowContrib(LitSurface, _Input);
    }

    float depthDiff = 1.0f;
    float3 ambient = float3(0.01f);
    float3 diffuse = float3(0.95f);

    float3 LightContrib = float3(0.9,0.9,1.0) * diffuse;

    // soft blend with solid geometry
    {
        float depthBufferViewspacePos = LoadScreenDepthViewspace( int2(gl_FragCoord.xy) );

        depthDiff = (depthBufferViewspacePos - abs(Input.ViewPos.z)) / mSoftParticlesSaturationDepth;
        depthDiff = smoothstep(0.0f, 1.0f, depthDiff);
    }

    if(mUI.wireframe != 0u)
    {
        OutColor = Input.color;
    }
    else
    {
        float3 Color = ambient + LightContrib * shadowTerm;// * gParticleOpacityNoiseTex.Sample(gDiffuseSampler, Input.UVS.xy).xyz;
        OutColor = float4(Color, depthDiff * (1.0f - segmentTransmittance));
    }
}