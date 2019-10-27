package jet.opengl.demos.nvidia.waves.ocean;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector2f;
import org.lwjgl.util.vector.Vector3f;
import org.lwjgl.util.vector.Vector4f;

import java.nio.ByteBuffer;

import jet.opengl.postprocessing.buffer.BufferGL;
import jet.opengl.postprocessing.texture.Texture2D;

final class OceanSprayParams implements TechniqueParams, OceanConst{
    OceanPSMParams m_pPSMParams;

    final Matrix4f g_matViewToPSM = new Matrix4f();
    Matrix4f g_matProj;
    Matrix4f	g_matView;
    final Matrix4f	g_matProjInv = new Matrix4f();
    Vector3f g_LightDirection;
    Vector3f		g_LightColor;
    Vector3f		g_AmbientColor;
    float		g_FogExponent;
    float		g_InvParticleLifeTime;

    Vector3f		g_LightningPosition;
    Vector3f		g_LightningColor;
    float		g_SimpleParticles;

    int			g_LightsNum;
    Vector4f[] g_SpotlightPosition/*[MaxNumSpotlights]*/;
    Vector4f[]		g_SpotLightAxisAndCosAngle/*[MaxNumSpotlights]*/;
    Vector4f[]		g_SpotlightColor/*[MaxNumSpotlights]*/;

//#if ENABLE_SHADOWS
Matrix4f[]    g_SpotlightMatrix/*[MaxNumSpotlights]*/;
    Texture2D[] g_SpotlightResource/*[MaxNumSpotlights]*/;
//#endif

    /*Buffer<float4>*/ BufferGL g_RenderInstanceData;
    /*Buffer<float4>*/ BufferGL g_RenderOrientationAndDecimationData;
    /*Buffer<float4>*/ BufferGL g_RenderVelocityAndTimeData;

    float  g_PSMOpacityMultiplier;

    // Data for GPU simulation
    /*struct SprayParticleData
    {
        float4 position_and_mass;
        float4 velocity_and_time;
    };*/

    int			g_ParticlesNum;
    float		g_SimulationTime;
    Vector3f		g_WindSpeed;

    // We use these for ensuring particles do not intersect ship
    final Matrix4f	g_worldToVessel = new Matrix4f();
    Matrix4f	g_vesselToWorld;

    // We use these to kill particles
    final Vector2f		g_worldToHeightLookupScale = new Vector2f();
    final Vector2f g_worldToHeightLookupOffset = new Vector2f();
    final Vector2f		g_worldToHeightLookupRot = new Vector2f();
    Texture2D   g_texHeightLookup;

    // We use these to feed particles back into foam map
    Matrix4f	g_matWorldToFoam;

    /*static const float kVesselLength = 63.f;
    static const float kVesselWidth = 9.f;
    static const float kVesselDeckHeight = 0.f;
    static const float kMaximumCollisionAcceleration = 10.f;
    static const float kCollisionAccelerationRange = 0.5f;

    static const float kSceneParticleTessFactor = 8.f;

    struct DepthSortEntry {
        int ParticleIndex;
        float ViewZ;
    };
*/
    /*StructuredBuffer <DepthSortEntry>*/BufferGL g_ParticleDepthSortSRV;
    /*StructuredBuffer<SprayParticleData>*/BufferGL g_SprayParticleDataSRV;

    /*RWStructuredBuffer <DepthSortEntry>*/BufferGL g_ParticleDepthSortUAV			/*: register(u0)*/;
    /*AppendStructuredBuffer<SprayParticleData>*/BufferGL g_SprayParticleData		/*: register(u1)*/;

    int g_iDepthSortLevel;
    int g_iDepthSortLevelMask;
    int g_iDepthSortWidth;
    int g_iDepthSortHeight;

    final Vector4f g_AudioVisualizationRect = new Vector4f(); // litterbug
    final Vector2f g_AudioVisualizationMargin = new Vector2f();
    float g_AudioVisualizationLevel;

    Texture2D g_texSplash;
    Texture2D g_texDepth;

    @Override
    public ByteBuffer store(ByteBuffer buf) {
        return null;
    }
}
