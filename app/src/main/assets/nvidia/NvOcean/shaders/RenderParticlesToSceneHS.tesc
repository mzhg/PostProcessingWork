#include "ocean_spray.glsl"

layout (vertices = 4) out;

in _VS_SCENE_PARTICLE_OUTPUT {
    float4 position_and_mass			/*: PosMass*/;
    float3 orientation_and_decimation	/*: OriDec;*/;
    float3 velocity						/*: Vel*/;
    float time							/*: T*/;
    float FogFactor               /*: FogFactor*/;
}_inputs[];

out HS_SCENE_PARTICLE_OUTPUT {
    float3 ViewPos                /*: ViewPos*/;
    float3 TextureUVAndOpacity    /*: TEXCOORD0*/;
// NOT USED float3 PSMCoords              : PSMCoords;
    float FogFactor               /*: FogFactor*/;
}_output[];

void main()
{
//    _output[gl_InvocationID].worldspace_position = _inputs[gl_InvocationID].worldspace_position;

    VS_SCENE_PARTICLE_OUTPUT InstanceData;
    InstanceData.position_and_mass = _inputs[0].position_and_mass;
    InstanceData.orientation_and_decimation = _inputs[0].orientation_and_decimation;
    InstanceData.velocity = _inputs[0].velocity;
    InstanceData.time = _inputs[0].time;
    InstanceData.FogFactor = _inputs[0].FogFactor;

    HS_PARTICLE_COORDS particleCoords = CalcParticleCoords(/*I[0].*/InstanceData,gl_InvocationID);

    // NOT USED float4 PSMCoords = mul(float4(particleCoords.ViewPos,1.f), g_matViewToPSM);

    _output[gl_InvocationID].TextureUVAndOpacity = particleCoords.TextureUVAndOpacity;
    _output[gl_InvocationID].ViewPos = particleCoords.ViewPos;
    // NOT USED outvert.PSMCoords = float3(PSMCoords.xyz);
    _output[gl_InvocationID].FogFactor = _inputs[0].FogFactor;

    gl_TessLevelOuter[0] = kSceneParticleTessFactor;
    gl_TessLevelOuter[1] = kSceneParticleTessFactor;
    gl_TessLevelOuter[2] = kSceneParticleTessFactor;
    gl_TessLevelOuter[3] = kSceneParticleTessFactor;
    gl_TessLevelInner[0] = kSceneParticleTessFactor;
    gl_TessLevelInner[1] = kSceneParticleTessFactor;
}