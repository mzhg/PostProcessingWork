package jet.opengl.demos.nvidia.face.sample;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector4f;

/**
 * Created by mazhen'gui on 2017/9/5.
 */

final class CbufFrame {
    final Matrix4f m_matWorldToClip = new Matrix4f();
    final Vector4f m_posCamera = new Vector4f();

    final Vector4f	m_vecDirectionalLight = new Vector4f();
    final Vector4f	m_rgbDirectionalLight = new Vector4f();

    final Matrix4f	m_matWorldToUvzwShadow = new Matrix4f();
    /** Matrix for transforming normals to shadow map space */
    final Matrix4f[]	m_matWorldToUvzShadowNormal = new Matrix4f[3];

    /** Minimum variance for variance shadow maps */
    float				m_vsmMinVariance;			//
    float				m_shadowSharpening;
    /** Scale of adaptive tessellation */
    float				m_tessScale;				//

    /** Multiplier on whole deep scattering result */
    float				m_deepScatterIntensity;		//
    /** Reciprocal of one-sigma radius of deep scatter Gaussian, in cm */
    float				m_deepScatterFalloff;		//
    /** Normal offset for shadow lookup to calculate thickness */
    float				m_deepScatterNormalOffset;	//
    /** Exposure multiplier */
    float				m_exposure;					//
}
