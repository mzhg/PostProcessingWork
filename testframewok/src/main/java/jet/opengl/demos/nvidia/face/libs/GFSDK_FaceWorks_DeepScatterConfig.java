package jet.opengl.demos.nvidia.face.libs;

import org.lwjgl.util.vector.Matrix4f;

/**
 * Runtime config struct for deep scatter.<p></p>
 * Parameters m_shadow* are for shadow map thickness estimate: you only need to fill these in if
 * you're using the FaceWorks helper functions forgetting the thickness. Set m_shadowProjType to
 * GFSDK_FaceWorks_NoProjection if not using the helpers.<p></p>
 * Created by mazhen'gui on 2017/9/4.
 */

public class GFSDK_FaceWorks_DeepScatterConfig {
    /** Deep scatter radius, in world units */
    public float			m_radius;
    /** What type of projection this is */
    public GFSDK_FaceWorks_ProjectionType m_shadowProjType;
    /** Shadow map projection matrix (row-vector convention) */
    public final Matrix4f m_shadowProjMatrix = new Matrix4f();
    /** Desired filter radius, in shadow texture UV space */
    public float			m_shadowFilterRadius;

    public float            m_shadowNear;
    public float            m_shadowFar;

    public void set(GFSDK_FaceWorks_DeepScatterConfig o){
        m_radius = o.m_radius;
        m_shadowProjType = o.m_shadowProjType;
        m_shadowProjMatrix.load(o.m_shadowProjMatrix);
        m_shadowFilterRadius = o.m_shadowFilterRadius;

        m_shadowNear = o.m_shadowNear;
        m_shadowFar = o.m_shadowFar;
    }
}
