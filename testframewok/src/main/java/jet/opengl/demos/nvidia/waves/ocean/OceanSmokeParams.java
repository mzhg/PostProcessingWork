package jet.opengl.demos.nvidia.waves.ocean;

import org.lwjgl.util.vector.Matrix4f;

final class OceanSmokeParams {
    final Matrix4f g_matProj = new Matrix4f();
    final Matrix4f	g_matView = new Matrix4f();
    float3		g_LightDirection;
    float3		g_LightColor;
    float3		g_AmbientColor;
    float		g_FogExponent;
    float2		g_ParticleBeginEndScale;
    float		g_InvParticleLifeTime;
    float		g_NoiseTime;
    Buffer<float4> g_RenderInstanceData;

    float3		g_LightningPosition;
    float3		g_LightningColor;

    uint g_ParticleIndexOffset;
    uint g_ParticleCount;
    float g_TimeStep;
    float g_PreRollEndTime;

    RWBuffer<float> g_SimulationInstanceData;
    RWBuffer<float> g_SimulationVelocities;
    float4x3 g_CurrEmitterMatrix;
    float4x3 g_PrevEmitterMatrix;
    float2 g_EmitAreaScale;
    float3 g_EmitMinMaxVelocityAndSpread;
    float2 g_EmitInterpScaleAndOffset;
    float4 g_WindVectorAndNoiseMult;
    float3 g_BuoyancyParams;
    float g_WindDrag;

    float g_NoiseSpatialScale;
    float g_NoiseTimeScale;

    Buffer<float2> g_RandomUV;
    uint g_RandomOffset;

    float  g_PSMOpacityMultiplier;
    float  g_PSMFadeMargin;

    struct DepthSortEntry {
        int ParticleIndex;
        float ViewZ;
    };

    RWStructuredBuffer <DepthSortEntry> g_ParticleDepthSortUAV;
    StructuredBuffer <DepthSortEntry> g_ParticleDepthSortSRV;

    uint g_iDepthSortLevel;
    uint g_iDepthSortLevelMask;
    uint g_iDepthSortWidth;
    uint g_iDepthSortHeight;
}
