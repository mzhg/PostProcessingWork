package jet.opengl.demos.nvidia.waves.ocean;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector3f;
import org.lwjgl.util.vector.Vector4f;

import java.nio.ByteBuffer;

import jet.opengl.postprocessing.texture.Texture2D;

final class OceanVesselParams implements TechniqueParams {
    Matrix4f    g_matWorldViewProj;
    Matrix4f	g_matWorldView;
    Matrix4f	g_matWorld;
    Vector4f    g_DiffuseColor;
    Vector3f    g_LightDirection;
    Vector3f	g_LightColor;
    Vector3f	g_AmbientColor;
    Vector3f	g_LightningPosition;
    Vector3f	g_LightningColor;

    float		g_FogExponent;

    int			g_LightsNum;
    Vector4f[]		g_SpotlightPosition/*[MaxNumSpotlights]*/;
    Vector4f[]		g_SpotLightAxisAndCosAngle/*[MaxNumSpotlights]*/;
    Vector4f[]		g_SpotlightColor/*[MaxNumSpotlights]*/;

//#if ENABLE_SHADOWS
    Matrix4f[]    g_SpotlightMatrix/*[MaxNumSpotlights]*/;
    Texture2D[]   g_SpotlightResource/*[MaxNumSpotlights]*/;
//#endif

    //-----------------------------------------------------------------------------------
// Texture & Samplers
//-----------------------------------------------------------------------------------
    int g_texDiffuse;
    Texture2D g_texRustMap;
    Texture2D g_texRust;
    Texture2D g_texBump;

    @Override
    public ByteBuffer store(ByteBuffer buf) {
        return null;
    }
}
