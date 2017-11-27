#include "LPV.glsl"

out initLPV_VSOUT
{
    float4 pos /*: POSITION*/; // 2D slice vertex coordinates in homogenous clip space
    float3 normal/* : NORMAL*/;
    float3 color /*: COLOR*/;
    flat int BiLinearOffset /*: BILINEAROFFSET*/;
    float fluxWeight /*:FW*/ ;
}_output;

void main()
{
    int vertexID = gl_VertexID;

    bool outside = false;

    _output.BiLinearOffset = floor(float(vertexIDin) / (RSM_RES_SQR));
    int vertexID = vertexIDin & (RSM_RES_SQR_M_1);

    //read the attributres for this virtual point light (VPL)
    int x = (vertexID & RSM_RES_M_1);
    int y = int( float(vertexID) / RSMWidth);
    if(y>=RSMHeight) outside = true;
    int3 uvw = int3(x,y,0);

    float3 normal = /*g_txRSMNormal.Load(uvw).rgb*/ texelFetch(g_txRSMNormal, uvw.xy, uvw.z).rgb;
    //decode the normal:
    normal = normal*float3(2.0f,2.0f,2.0f) - 1.0f;
    normal = normalize(normal);

    float4 color = /*g_txRSMColor.Load(uvw)*/texelFetch(g_txRSMColor, uvw.xy, uvw.z);

    //unproject the depth to get the view space position of the texel
    float2 normalizedInputPos = float2(float(x)/RSMWidth,float(y)/RSMHeight);
    float depthSample = /*g_txRSMDepth.Load(uvw).x*/texelFetch(g_txRSMDepth, uvw.xy, uvw.z).x;
    depthSample = 2.0 * depthSample - 1.0;
    float2 inputPos = float2((normalizedInputPos.x*2.0)-1.0,((1.0f-normalizedInputPos.y)*2.0)-1.0);

    float4 vProjectedPos = float4(inputPos.x, inputPos.y, depthSample, 1.0f);
    float4 viewSpacePos = mul(vProjectedPos, g_InverseProjection);
    viewSpacePos.xyz = viewSpacePos.xyz / viewSpacePos.w;
    if(g_useFluxWeight)
        _output.fluxWeight = viewSpacePos.z * viewSpacePos.z * g_fluxWeight; // g_fluxWeight is ((2 * tan_Fov_X_half)/RSMWidth) * ((2 * tan_Fov_Y_half)/RSMHeight)
    else
        _output.fluxWeight = 1.0f;

    if(-viewSpacePos.z >= farClip) outside=true;  // TODO

    float3 LPVSpacePos = mul( float4(viewSpacePos.xyz,1), g_ViewToLPV ).xyz;

    //displace the position half a cell size along its normal
    LPVSpacePos += normal / float3(LPV3DWidth, LPV3DHeight, LPV3DDepth) * displacement;

    if(LPVSpacePos.x<0.0f || LPVSpacePos.x>=1.0f) outside = true;
    if(LPVSpacePos.y<0.0f || LPVSpacePos.y>=1.0f) outside = true;
    if(LPVSpacePos.z<0.0f || LPVSpacePos.z>=1.0f) outside = true;

    _output.pos =float4(LPVSpacePos.x,LPVSpacePos.y, LPVSpacePos.z, 1.0f);

    _output.color = color.rgb;
    _output.normal = normal;

    //if(outside) kill the vertex
    if(outside) _output.pos.x = LPV3DWidth*2.0f;

//    return output;
}