package jet.opengl.demos.nvidia.shadows;

import org.lwjgl.util.vector.Matrix4f;

/**
 * Created by mazhen'gui on 2017/11/3.
 */

public class ShadowMapParams {
    public final Matrix4f m_LightView = new Matrix4f();
    public final Matrix4f m_LightProj = new Matrix4f();
    public final Matrix4f m_LightViewProj = new Matrix4f();
    public float m_LightNear;
    public float m_LightFar;
    /** The value used under the perspective mode */
    public float m_LightFov;
    /** The value used under the perspective mode */
    public float m_LightRatio;

    /** The value used under the ortho mode */
    public float m_lightLeft, m_lightRight;
    /** The value used under the ortho mode */
    public float m_lightBottom, m_LightTop;
    public boolean m_perspective;
}
