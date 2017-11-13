package jet.opengl.demos.nvidia.illumination;

import org.lwjgl.util.vector.Matrix4f;

/**
 * Created by Administrator on 2017/11/13 0013.
 */

final class CB_VS_PER_OBJECT {
    final Matrix4f m_WorldViewProj = new Matrix4f();
    final Matrix4f m_WorldViewIT = new Matrix4f();
    final Matrix4f m_World = new Matrix4f();
    final Matrix4f m_LightViewProjClip2Tex = new Matrix4f();
}
