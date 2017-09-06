package jet.opengl.demos.nvidia.face.sample;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector4f;

import java.nio.ByteBuffer;

/**
 * Created by mazhen'gui on 2017/9/5.
 */

final class CbufFrame {
    static final int SIZE = Matrix4f.SIZE * 5 + Vector4f.SIZE * 5;
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

    CbufFrame(){
        for(int i = 0; i < m_matWorldToUvzShadowNormal.length; i++){
            m_matWorldToUvzShadowNormal[i] = new Matrix4f();
        }
    }

    ByteBuffer store(ByteBuffer buffer){
        m_matWorldToClip.store(buffer);
        m_posCamera.store(buffer);
        m_vecDirectionalLight.store(buffer);
        m_rgbDirectionalLight.store(buffer);
        m_matWorldToUvzwShadow.store(buffer);

        for(int i = 0; i < m_matWorldToUvzShadowNormal.length; i++){
            m_matWorldToUvzShadowNormal[i].store(buffer);
        }

        buffer.putFloat(m_vsmMinVariance);
        buffer.putFloat(m_shadowSharpening);
        buffer.putFloat(m_tessScale);
        buffer.putFloat(m_deepScatterIntensity);
        buffer.putFloat(m_deepScatterFalloff);
        buffer.putFloat(m_deepScatterNormalOffset);
        buffer.putFloat(m_exposure);
        buffer.putFloat(0);
        return buffer;
    }
}
