package jet.opengl.demos.nvidia.waves.ocean;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector2f;
import org.lwjgl.util.vector.Vector3f;
import org.lwjgl.util.vector.Vector4f;

import java.nio.ByteBuffer;

import jet.opengl.postprocessing.buffer.BufferGL;
import jet.opengl.postprocessing.texture.Texture2D;

final class OceanSmokeParams implements TechniqueParams{

    // Galobal variables for the ocean_psm.fxh
    final Matrix4f g_matViewToPSM = new Matrix4f();
    OceanPSMParams m_pPSMParams;

    Matrix4f g_matProj ;
    Matrix4f	g_matView ;
    Vector3f g_LightDirection;
    Vector3f		g_LightColor;
    Vector3f		g_AmbientColor;
    float		g_FogExponent;
    final Vector2f g_ParticleBeginEndScale = new Vector2f();
    float		g_InvParticleLifeTime;
    float		g_NoiseTime;
    BufferGL g_RenderInstanceData;

    Vector3f		g_LightningPosition;
    Vector3f		g_LightningColor;

    int g_ParticleIndexOffset;
    int g_ParticleCount;
    float g_TimeStep;
    float g_PreRollEndTime;

    BufferGL g_SimulationInstanceData;
    BufferGL g_SimulationVelocities;
    Matrix4f g_CurrEmitterMatrix;
    Matrix4f g_PrevEmitterMatrix;
    final Vector2f g_EmitAreaScale = new Vector2f();
    final Vector3f g_EmitMinMaxVelocityAndSpread = new Vector3f();
    final Vector2f g_EmitInterpScaleAndOffset = new Vector2f();
    final Vector4f g_WindVectorAndNoiseMult = new Vector4f();
    final Vector3f g_BuoyancyParams = new Vector3f();
    float g_WindDrag;

    float g_NoiseSpatialScale;
    float g_NoiseTimeScale;

    BufferGL g_RandomUV;
    int g_RandomOffset;

    float  g_PSMOpacityMultiplier;
    float  g_PSMFadeMargin;

    BufferGL g_ParticleDepthSortUAV;
    BufferGL g_ParticleDepthSortSRV;

    int g_iDepthSortLevel;
    int g_iDepthSortLevelMask;
    int g_iDepthSortWidth;
    int g_iDepthSortHeight;

    Texture2D g_texDiffuse;
    Texture2D permTexture;    // noise
    Texture2D gradTexture4d ;

    @Override
    public ByteBuffer store(ByteBuffer buf) {
        return null;
    }
}
