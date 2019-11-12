#include "ocean_spray.glsl"

out _VS_SCENE_PARTICLE_OUTPUT {
    float4 position_and_mass			/*: PosMass*/;
    float3 orientation_and_decimation	/*: OriDec;*/;
    float3 velocity						/*: Vel*/;
    float time							/*: T*/;
    float FogFactor               /*: FogFactor*/;
}Output;

void main()
{
    PARTICLE_INSTANCE_DATA InstanceData = GetParticleInstanceData(gl_VertexID);

    Output.position_and_mass = InstanceData.position_and_mass;
    Output.orientation_and_decimation = InstanceData.orientation_and_decimation;
    Output.velocity = InstanceData.velocity;
    Output.time = InstanceData.time;

    float3 CentreViewPos = mul(float4(InstanceData.position_and_mass.xyz,1), g_matView).xyz;
    Output.FogFactor = exp(dot(CentreViewPos,CentreViewPos)*g_FogExponent);
}