package jet.opengl.demos.nvidia.face.sample;

import org.lwjgl.util.vector.Matrix3f;
import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector3f;

import jet.opengl.postprocessing.common.Disposeable;
import jet.opengl.postprocessing.common.GLFuncProvider;
import jet.opengl.postprocessing.common.GLFuncProviderFactory;
import jet.opengl.postprocessing.common.GLenum;
import jet.opengl.postprocessing.texture.Texture2D;
import jet.opengl.postprocessing.texture.Texture2DDesc;
import jet.opengl.postprocessing.texture.TextureUtils;
import jet.opengl.postprocessing.util.LogUtil;

/**
 * Very simple shadow map class, fits an orthogonal shadow map around a scene bounding box<p></p>
 * Created by mazhen'gui on 2017/9/5.
 */

final class CShadowMap implements Disposeable{
    Texture2D   m_pDsv;
    Texture2D		m_pSrv;
    /** Shadow map resolution */
    int								m_size;
    /** Unit vector toward directional light */
    final Vector3f m_vecLight = new Vector3f();
    /** AABB of scene in world space */
    final Vector3f m_posMinScene = new Vector3f(Float.MAX_VALUE,Float.MAX_VALUE, Float.MAX_VALUE);
    final Vector3f m_posMaxScene = new Vector3f(-Float.MAX_VALUE, -Float.MAX_VALUE, -Float.MAX_VALUE);
    /** Projection matrix */
    final Matrix4f m_matProj = new Matrix4f();
    /** View matrix */
    final Matrix4f m_matView = new Matrix4f();
    /** Matrix for rendering shadow map */
    final Matrix4f m_matWorldToClip = new Matrix4f();
    /** Matrix for sampling shadow map */
    final Matrix4f m_matWorldToUvzw = new Matrix4f();
    /** Matrix for transforming normals to shadow map space */
    final Matrix3f m_matWorldToUvzNormal = new Matrix3f();
    /** Diameter in world units along shadow XYZ axes */
    final Vector3f m_vecDiam = new Vector3f();

    private GLFuncProvider gl;
    private int m_shadow_fbo;

    CShadowMap(){
        gl = GLFuncProviderFactory.getGLFuncProvider();
    }

    void Init(int size){

        // Create 2D texture for shadow map
        /*D3D11_TEXTURE2D_DESC texDesc =
                {
                        size, size,						// width, height
                        1, 1,							// mip levels, array size
                        DXGI_FORMAT_R32_TYPELESS,
                        { 1, 0 },						// multisample count, quality
                        D3D11_USAGE_DEFAULT,
                        D3D11_BIND_SHADER_RESOURCE | D3D11_BIND_DEPTH_STENCIL,
                        0,								// no cpu access
                        0,								// no misc flags
                };
        ID3D11Texture2D * pTex = nullptr;
        V_RETURN(pDevice->CreateTexture2D(&texDesc, nullptr, &pTex));

        // Create depth-stencil view
        D3D11_DEPTH_STENCIL_VIEW_DESC dsvDesc =
                {
                        DXGI_FORMAT_D32_FLOAT,
                        D3D11_DSV_DIMENSION_TEXTURE2D,
                };
        V_RETURN(pDevice->CreateDepthStencilView(pTex, &dsvDesc, &m_pDsv));

        // Create shader resource view
        D3D11_SHADER_RESOURCE_VIEW_DESC srvDesc =
                {
                        DXGI_FORMAT_R32_FLOAT,
                        D3D11_SRV_DIMENSION_TEXTURE2D,
                };
        srvDesc.Texture2D.MipLevels = 1;
        V_RETURN(pDevice->CreateShaderResourceView(pTex, &srvDesc, &m_pSrv));*/
        Texture2DDesc texDesc = new Texture2DDesc(size, size, GLenum.GL_DEPTH_COMPONENT32F);
        m_pDsv = m_pSrv = TextureUtils.createTexture2D(texDesc, null);

        m_shadow_fbo = gl.glGenFramebuffer();
        gl.glBindFramebuffer(GLenum.GL_FRAMEBUFFER, m_shadow_fbo);
        gl.glFramebufferTexture2D(GLenum.GL_FRAMEBUFFER, GLenum.GL_DEPTH_ATTACHMENT, GLenum.GL_TEXTURE_2D, m_pDsv.getTexture(), 0);
        gl.glBindFramebuffer(GLenum.GL_FRAMEBUFFER, 0);

        // The texture isn't needed any more
//        SAFE_RELEASE(pTex);

        m_size = size;

        m_pDsv.setName("Shadow map - DSV");
        m_pSrv.setName("Shadow map - SRV");
        LogUtil.i(LogUtil.LogType.DEFAULT, String.format("Created shadow map, format D32_FLOAT, %d x %d", size, size));
    }
    void UpdateMatrix(){
        // Calculate view matrix based on light direction

        float posEyeX = 0;
        float posEyeY = 0;
        float posEyeZ = 0;
        float posLookAtX = -m_vecLight.x;
        float posLookAtY = -m_vecLight.y;
        float posLookAtZ = -m_vecLight.z;
//        XMVECTOR vecUp = XMVectorSet(0.0f, 1.0f, 0.0f, 0.0f);
        float vecUpX =0;
        float vecUpY =1;
        float vecUpZ =0;

        // Handle light vector being straight up or down
        if (Math.abs(m_vecLight.x) < 1e-4f && Math.abs(m_vecLight.z) < 1e-4f) {
//            vecUp = XMVectorSet(1.0f, 0.0f, 0.0f, 0.0f);
            vecUpX = 1;
            vecUpY = 0;
            vecUpZ = 0;
        }

//        XMMATRIX matView = XMMatrixLookAtRH(posEye, posLookAt, vecUp);
        Matrix4f matView = Matrix4f.lookAt(posEyeX, posEyeY,posEyeZ, posLookAtX,posLookAtY, posLookAtZ, vecUpX,vecUpY,vecUpZ, m_matView);

        // Transform scene AABB into view space and recalculate bounds

//        XMVECTOR posMinView = XMVectorReplicate(FLT_MAX);
//        XMVECTOR posMaxView = XMVectorReplicate(-FLT_MAX);
        float posMinViewX = Float.MIN_VALUE;
        float posMinViewY = Float.MIN_VALUE;
        float posMinViewZ = Float.MIN_VALUE;
        float posMaxViewX = -Float.MIN_VALUE;
        float posMaxViewY = -Float.MIN_VALUE;
        float posMaxViewZ = -Float.MIN_VALUE;

        Vector3f posWorld = m_vecDiam;
        for (int i = 0; i < 8; ++i)
        {
            posWorld.set(
                    (i & 1)!=0 ? m_posMaxScene.x : m_posMinScene.x,
                    (i & 2)!=0 ? m_posMaxScene.y : m_posMinScene.y,
                    (i & 4)!=0 ? m_posMaxScene.z : m_posMinScene.z);

//            XMVECTOR posView = XMVector3TransformCoord(posWorld, matView);
            Vector3f posView =  Matrix4f.transformVector(matView, posWorld, posWorld);

//            posMinView = XMVectorMin(posMinView, posView);
//            posMaxView = XMVectorMax(posMaxView, posView);
            posMinViewX = Math.min(posMinViewX, posView.x);
            posMinViewY = Math.min(posMinViewY, posView.y);
            posMinViewZ = Math.min(posMinViewZ, posView.z);
            posMaxViewX = Math.max(posMaxViewX, posView.x);
            posMaxViewY = Math.max(posMaxViewY, posView.y);
            posMaxViewZ = Math.max(posMaxViewZ, posView.z);
        }

//        XMStoreFloat3(&m_vecDiam, posMaxView - posMinView);
        m_vecDiam.set(posMaxViewX - posMinViewX,posMaxViewY - posMinViewY, posMaxViewZ - posMinViewZ);

        // Calculate orthogonal projection matrix to fit the scene bounds
        Matrix4f.ortho(posMinViewX, posMaxViewX, posMinViewY, posMaxViewY, -posMaxViewZ, -posMinViewZ, m_matProj);
        Matrix4f.mul(m_matProj, m_matView, m_matWorldToClip);

        // Calculate matrix that maps to [0, 1] UV space instead of [-1, 1] clip space
        m_matWorldToUvzw.set(
                0.5f,  0,    0, 0,
                0,    0.5f, 0, 0,
                0,     0,    0.5f, 0,
                0.5f,  0.5f, 0.5f, 1);
        Matrix4f.mul(m_matWorldToUvzw, m_matWorldToClip, m_matWorldToUvzw);

        // Calculate inverse transpose matrix for transforming normals
        Matrix4f.getNormalMatrix(m_matWorldToUvzw, m_matWorldToUvzNormal);  // TODO need check
    }
    void BindRenderTarget(){
        gl.glBindFramebuffer(GLenum.GL_FRAMEBUFFER, m_shadow_fbo);
        gl.glViewport(0,0, m_size, m_size);
    }

    void CalcFilterUVZScale(float filterRadius, Vector3f result) {
        // Expand the filter in the Z direction (this controls how far
        // the filter can tilt before it starts contracting).
        // Tuned empirically.
        float zScale = 4.0f;

        result.set(
                filterRadius / m_vecDiam.x,
                filterRadius / m_vecDiam.y,
                zScale * filterRadius / m_vecDiam.z);
    }

    @Override
    public void dispose() {
        gl.glDeleteFramebuffer(m_shadow_fbo);
        m_pDsv.dispose();
    }
}
