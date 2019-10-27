package jet.opengl.demos.nvidia.waves.ocean;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector2f;
import org.lwjgl.util.vector.Vector4f;

import java.nio.ByteBuffer;

import jet.opengl.postprocessing.texture.Texture2D;

final class OceanSurfaceHeightParams implements TechniqueParams {
    float g_numQuadsW;
    float g_numQuadsH;
    Vector4f g_quadScale;
    Vector2f g_quadUVDims;
    Vector4f g_srcUVToWorldScale;
    Vector4f g_srcUVToWorldRot;
    Vector4f g_srcUVToWorldOffset;
    Vector2f g_worldToClipScale;
    Vector2f g_clipToWorldRot;
    Vector2f g_clipToWorldOffset;

    final Vector2f g_worldToUVScale = new Vector2f();
    final Vector2f g_worldToUVOffset = new Vector2f();
    final Vector2f g_worldToUVRot = new Vector2f();
    Matrix4f g_matViewProj;
    Matrix4f g_matWorld;

    Texture2D g_texLookup;

    @Override
    public ByteBuffer store(ByteBuffer buf) {
        return null;
    }
}
