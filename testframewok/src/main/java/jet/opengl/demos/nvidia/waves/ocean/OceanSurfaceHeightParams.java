package jet.opengl.demos.nvidia.waves.ocean;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector2f;
import org.lwjgl.util.vector.Vector4f;

import java.nio.ByteBuffer;

import jet.opengl.postprocessing.texture.Texture2D;

final class OceanSurfaceHeightParams implements TechniqueParams {
    float g_numQuadsW;
    float g_numQuadsH;
    final Vector4f g_quadScale = new Vector4f();
    final Vector2f g_quadUVDims = new Vector2f();
    final Vector4f g_srcUVToWorldScale = new Vector4f();
    final Vector4f g_srcUVToWorldRot = new Vector4f();
    final Vector4f g_srcUVToWorldOffset = new Vector4f();
    final Vector2f g_worldToClipScale = new Vector2f();
    final Vector2f g_clipToWorldRot = new Vector2f();
    final Vector2f g_clipToWorldOffset = new Vector2f();

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
