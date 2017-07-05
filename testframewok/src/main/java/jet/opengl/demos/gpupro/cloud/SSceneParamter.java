package jet.opengl.demos.gpupro.cloud;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Quaternion;
import org.lwjgl.util.vector.Vector2f;
import org.lwjgl.util.vector.Vector3f;

import jet.opengl.postprocessing.util.Numeric;

/**
 * Created by mazhen'gui on 2017/7/4.
 */

final class SSceneParamter {
    // light
    /** sun light direction */
    final Vector3f m_vLightDir = new Vector3f();
    /** sun light color */
    final Vector3f m_vLightColor = new Vector3f();
    final Vector3f m_vAmbientLight = new Vector3f();// ambient light
    // scattering
    final Vector3f m_vRayleigh = new Vector3f();     // rayleigh scattering
    final Vector3f m_vMie = new Vector3f();          // mie scattering

    float       m_fG;            // eccentricity of mie scattering
    float       m_fLightScale;   // scaling parameter of light
    float       m_fAmbientScale; // scaline parameter of ambient term
    // distance for scattering
    float       m_fEarthRadius;  // radius of the earth
    float       m_fAtomosHeight; // height of the atomosphere
    float       m_fCloudHeight;  // height of cloud

    float       m_far;
    final Matrix4f m_viewMat = new Matrix4f();
    final Matrix4f m_projMat = new Matrix4f();
    final Vector3f m_Eye     = new Vector3f();


    void getShaderParam(SScatteringShaderParameters param){
        // vRayleigh : rgb : 3/(16*PI) * Br           w : -2*g
//        D3DXVec3Scale( (D3DXVECTOR3*)&param.vRayleigh, &m_vRayleigh, 3.0f/(16.0f*PI) );
        Vector3f.scale(m_vRayleigh, 3.0f/(16.0f* Numeric.PI), param.vRayleigh);
        param.vRayleigh.w = -2.0f * m_fG;

        // vMie : rgb : 1/(4*PI) * Bm * (1-g)^2  w : (1+g^2)
        float       fG = 1.0f -m_fG;
//        D3DXVec3Scale( (D3DXVECTOR3*)&param.vMie, &m_vMie, fG*fG/(4.0f*PI) );
        Vector3f.scale(m_vMie, fG*fG/(4.0f*Numeric.PI), param.vMie);
        param.vMie.w = 1.0f + m_fG * m_fG;

//        D3DXVECTOR3 vSum;
//        D3DXVec3Add( (D3DXVECTOR3*)&vSum, &m_vRayleigh, &m_vMie );
        Vector3f vSum = Vector3f.add(m_vRayleigh, m_vMie, null);
        // vESun : rgb : Esun/(Br+Bm)             w : R
        param.vESun.x = m_fLightScale * m_vLightColor.x/vSum.x;
        param.vESun.y = m_fLightScale * m_vLightColor.y/vSum.y;
        param.vESun.z = m_fLightScale * m_vLightColor.z/vSum.z;
        param.vESun.w = m_fEarthRadius;

        // vSum  : rgb : (Br+Bm)                  w : h(2R+h)
        // scale by inverse of farclip to apply constant scattering effect in case farclip is changed.
//        D3DXVec3Scale( (D3DXVECTOR3*)&param.vSum, (D3DXVECTOR3*)&vSum, 1.0f/m_pCamera->GetFarClip() );
        Vector3f.scale(vSum, 1.0f/m_far, param.vSum);
        param.vSum.w = m_fAtomosHeight * (2.0f*m_fEarthRadius + m_fAtomosHeight);

        // ambient term of scattering
//        D3DXVec3Scale( (D3DXVECTOR3*)&param.vAmbient, &m_vAmbientLight, m_fAmbientScale );
        Vector3f.scale(m_vAmbientLight, m_fAmbientScale, param.vAmbient);
        param.vAmbient.w = (float) (1.0/Math.sqrt( param.vSum.w ));
    }

    void setTime(float fTimeOfADay){
        float fAngle = ( 45.0f ) * Numeric.PI / 180.0f;
//        Vector3f vRotAxis = new Vector3f( 0.0f, (float)Math.sin( fAngle ), (float)Math.cos( fAngle ) );

//        D3DXMATRIX matRot;
//        D3DXMatrixRotationAxis( &matRot, &vRotAxis, fTimeOfADay * (1.0f*PI) );
        Quaternion matRot = new Quaternion();
        matRot.setFromAxisAngle(0.0f, (float)Math.sin( fAngle ), (float)Math.cos( fAngle ), fTimeOfADay * (1.0f* Numeric.PI));

//        D3DXVECTOR3 v( -1.0f, 0.0f, 0.0f );
//        D3DXVec3TransformNormal( &m_vLightDir, &v, &matRot );
        m_vLightDir.set(-1, 0,0);
        Quaternion.transform(matRot, m_vLightDir, m_vLightDir);
    }
//
    void getCloudDistance(Vector2f v){
        v.x = m_fEarthRadius;
        v.y = m_fCloudHeight * (2.0f*m_fEarthRadius + m_fCloudHeight);
    }
}
