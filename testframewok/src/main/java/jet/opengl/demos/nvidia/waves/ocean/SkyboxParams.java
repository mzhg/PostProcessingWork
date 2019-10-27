package jet.opengl.demos.nvidia.waves.ocean;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector2f;
import org.lwjgl.util.vector.Vector3f;
import org.lwjgl.util.vector.Vector4f;

import java.nio.ByteBuffer;

import jet.opengl.postprocessing.buffer.BufferGL;
import jet.opengl.postprocessing.texture.Texture2D;
import jet.opengl.postprocessing.texture.TextureCube;

final class SkyboxParams implements TechniqueParams{

    // XForm Matrix
    Matrix4f g_matViewProj;
    Matrix4f	g_matProjInv;
    Vector3f g_FogColor;
    float		g_FogExponent;
    Vector3f		g_LightningColor;
    float		g_CloudFactor;


    Vector3f g_LightPos;												// The direction to the light source

    //-----------------------------------------------------------------------------------
// Texture & Samplers
//-----------------------------------------------------------------------------------
    TextureCube g_texSkyCube0;
    TextureCube	g_texSkyCube1;
    float g_SkyCubeBlend;
    Vector2f g_SkyCube0RotateSinCos;
    Vector2f g_SkyCube1RotateSinCos;
    Vector4f g_SkyCubeMult;

    Texture2D g_texColor;
    /*Texture2DMS<float>*/ Texture2D g_texDepthMS;
    /*Texture2D<float>*/Texture2D g_texDepth;

    @Override
    public ByteBuffer store(ByteBuffer buf) {
        return null;
    }
}
