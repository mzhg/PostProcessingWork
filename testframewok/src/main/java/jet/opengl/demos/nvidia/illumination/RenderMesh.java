package jet.opengl.demos.nvidia.illumination;

import org.lwjgl.util.vector.Matrix3f;
import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector3f;

/**
 * Created by Administrator on 2017/11/13 0013.
 */

final class RenderMesh {
    private float m_translateX;
    private float m_translateY;
    private float m_translateZ;
    private float m_rotateX;
    private float m_rotateY;
    private float m_rotateZ;
    private float m_scaleX;
    private float m_scaleY;
    private float m_scaleZ;

    Mesh m_Mesh;

    final Matrix4f m_WMatrix = new Matrix4f();
    final Matrix4f m_Clip2Tex = new Matrix4f();

    boolean m_UseTexture;

    RenderMesh()
    {
//        D3DXMatrixIdentity(&m_WMatrix);
        m_Clip2Tex.set(
                0.5f,    0,    0,   0,
                0,       0.5f,  0,   0,
                0,     0,     0.5f,   0,
                0.5f,   0.5f,  0.5f,   1 );

        m_UseTexture = false;
    }

    void setWorldMatrix(float scaleX, float scaleY, float scaleZ, float rotateX, float rotateY, float rotateZ, float translateX, float translateY, float translateZ)
    {
        m_translateX = translateX;
        m_translateY = translateY;
        m_translateZ = translateZ;
        m_rotateX = rotateX;
        m_rotateY = rotateY;
        m_rotateZ = rotateZ;
        m_scaleX = scaleX;
        m_scaleY = scaleY;
        m_scaleZ = scaleZ;
        calculateWorldMatrix();
    }

    void calculateWorldMatrix()
    {
        /*D3DXMATRIX mTranslate, mRotateX, mRotateY, mRotateZ, mScale;
        D3DXMatrixTranslation( &mTranslate, m_translateX, m_translateY, m_translateZ);
        D3DXMatrixRotationX( &mRotateX, m_rotateX);
        D3DXMatrixRotationY( &mRotateY, m_rotateY);
        D3DXMatrixRotationZ( &mRotateZ, m_rotateZ);
        D3DXMatrixScaling( &mScale, m_scaleX, m_scaleY, m_scaleZ);
        m_WMatrix = mTranslate*mScale*mRotateX*mRotateY*mRotateZ;*/
        m_WMatrix.setIdentity();
        m_WMatrix.rotate(m_rotateZ, Vector3f.Z_AXIS);
        m_WMatrix.rotate(m_rotateY, Vector3f.Y_AXIS);
        m_WMatrix.rotate(m_rotateX, Vector3f.X_AXIS);
        m_WMatrix.translate(m_translateX, m_translateY, m_translateZ);

    }

    void setWorldMatrix(Matrix4f m) { m_WMatrix.load(m); }

    void setWorldMatrixTranslate(float translateX, float translateY, float translateZ)
    {
        m_translateX = translateX;
        m_translateY = translateY;
        m_translateZ = translateZ;
        calculateWorldMatrix();
    }


    void createMatrices(Matrix4f lightProjection, Matrix4f viewMatrix, Matrix4f WVMatrix, Matrix4f WVMatrixIT, Matrix4f WVPMatrix, Matrix4f ViewProjClip2Tex )
    {
        /**WVMatrix     = m_WMatrix * viewMatrix;
        D3DXMatrixInverse( WVMatrixIT, NULL, WVMatrix );
        *WVPMatrix    = (*WVMatrix)  * lightProjection;
        D3DXMatrixMultiply(ViewProjClip2Tex, WVPMatrix, &m_Clip2Tex);*/

        Matrix4f.mul(viewMatrix, m_WMatrix, WVMatrix);
        Matrix4f.invert(WVMatrix, WVMatrixIT);
        Matrix4f.mul(lightProjection, WVMatrix, WVPMatrix);
        Matrix4f.mul(m_Clip2Tex, WVPMatrix, ViewProjClip2Tex);
    }
}
