package jet.opengl.demos.gpupro.cloud;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector3f;
import org.lwjgl.util.vector.Vector4f;

import jet.opengl.postprocessing.common.GLFuncProvider;
import jet.opengl.postprocessing.common.GLFuncProviderFactory;
import jet.opengl.postprocessing.common.GLenum;
import jet.opengl.postprocessing.texture.Texture2D;
import jet.opengl.postprocessing.util.CacheBuffer;

/**
 * Shader for blur density.<p></p>
 * Created by mazhen'gui on 2017/7/6.
 */

final class CCloudBlur {
    private int      m_pVB;
    private int      m_pDecl;
    private RenderTechnique m_shader;
    private SSceneParamter        m_pSceneParam;
    private GLFuncProvider gl;

    void Create(SSceneParamter pSceneParam){
        gl = GLFuncProviderFactory.getGLFuncProvider();
        m_pSceneParam = pSceneParam;

        Delete();

        final float[] s_vertices = {
                1.0f,  1.0f, 1.0f, 1.0f, 1.0f, 0.0f ,
                1.0f, -1.0f, 1.0f, 1.0f, 1.0f, 1.0f ,
                -1.0f,  1.0f, 1.0f, 1.0f, 0.0f, 0.0f,
                -1.0f, -1.0f, 1.0f, 1.0f, 0.0f, 1.0f,
        };

        m_pDecl = gl.glGenVertexArray();
        gl.glBindVertexArray(m_pDecl);
        {
            m_pVB = gl.glGenBuffer();
            gl.glBindBuffer(GLenum.GL_ARRAY_BUFFER, m_pVB);
            gl.glBufferData(GLenum.GL_ARRAY_BUFFER, CacheBuffer.wrap(s_vertices), GLenum.GL_STATIC_DRAW);
            gl.glEnableVertexAttribArray(0);
            gl.glVertexAttribPointer(0, 4, GLenum.GL_FLOAT, false, 24, 0);
            gl.glEnableVertexAttribArray(1);
            gl.glVertexAttribPointer(1, 2, GLenum.GL_FLOAT, false, 24, 16);
        }
        gl.glBindVertexArray(0);
        gl.glBindBuffer(GLenum.GL_ARRAY_BUFFER, 0);

        CreateShaders();
    }

    void Delete(){

    }

    void Blur(Texture2D pTex){
        gl.glBindVertexArray(m_pDecl);
        gl.glDisable(GLenum.GL_DEPTH_TEST);
        gl.glDisable(GLenum.GL_CULL_FACE);

        m_shader.enable();
        gl.glActiveTexture(GLenum.GL_TEXTURE0);
        gl.glBindTexture(pTex.getTarget(), pTex.getTexture());

        gl.glDrawArrays(GLenum.GL_TRIANGLE_STRIP, 0, 4);
        gl.glBindVertexArray(0);
    }

    void CreateShaders(){ m_shader = new RenderTechnique("CloudBlurVS.vert", "CloudBlurPS.frag");}
    void SetShaderConstant( Texture2D pTex){
        if ( pTex != null ) {
            // offset parameter to sample center of texels.
//            D3DSURFACE_DESC desc;
//            pTex->GetLevelDesc( 0, &desc );
//            D3DXVECTOR2 v( 0.5f / (FLOAT)desc.Width, 0.5f / (FLOAT)desc.Height );
//            SetVSValue( pDev, VS_CONST_PIXELSIZE, &v, sizeof(D3DXVECTOR2) );
            m_shader.setPix(0.5f/pTex.getWidth(), 0.5f/pTex.getHeight());
        }

        if ( m_pSceneParam != null /*&& m_pSceneParam->m_pCamera != NULL*/ ) {
            // view position
//            SetPSValue( pDev, PS_CONST_EYE, m_pSceneParam->m_pCamera->GetEyePt(), sizeof(FLOAT)*3 );
            m_shader.setEye(m_pSceneParam.m_Eye);

            // transform screen position to world space
//            D3DXMATRIX mC2W;
//            D3DXMatrixInverse( &mC2W, NULL, m_pSceneParam->m_pCamera->GetWorld2ProjMatrix() );
//            SetVSMatrix( pDev, VS_CONST_C2W, &mC2W );
            m_shader.setC2W(m_pSceneParam.m_viewProj);

            // Directional Light in projection space.
            Vector4f vLit = new Vector4f(m_pSceneParam.m_vLightDir.x, m_pSceneParam.m_vLightDir.y, m_pSceneParam.m_vLightDir.z, 0.0f );
            if ( vLit.y > 0.0f ) {
                // assuming light direction is horizontal when sunset or sunrise.
                // otherwise, shadow of clouds converges at a point on the screen opposite to the light.
                vLit.y = 0.0f;
//                D3DXVec4Normalize( &vLit, &vLit );
                vLit.normalise();
            }
//            D3DXVECTOR4 vProjPos;
//            D3DXVec4Transform( &vProjPos, &vLit, m_pSceneParam->m_pCamera->GetWorld2ProjMatrix() );
            Vector4f vProjPos = Matrix4f.transform(m_pSceneParam.m_viewProj, vLit, vLit);

            // blur vector = vBlurDir.xy * uv + vBlurDir.zw
            Vector4f vBlurDir = new Vector4f();
            final float EPSIRON = 0.000001f;
            if ( ( Math.abs(vProjPos.w) < EPSIRON )|| ( Math.abs(vProjPos.z) < EPSIRON ) ) {
                // if dot( litdir, ray ) == 0.0f : directional.
                // light is stil directional in projection space.
                vProjPos.w = vProjPos.z = 0.0f;
//                D3DXVec4Normalize( &vProjPos, &vProjPos );
                vProjPos.normalise();
                vProjPos.y = -vProjPos.y;
                // directional blur
                vBlurDir.set( 0.0f, 0.0f, -vProjPos.x, -vProjPos.y );
            }
            else {
                // otherwise : point blur.
                // light direction is a position in projection space.
                if ( 0.0f < vProjPos.w ) {
                    // transform screen position to texture coordinate
//                    D3DXVec4Scale( &vProjPos, &vProjPos, 1.0f/vProjPos.w );
                    vProjPos.x /= vProjPos.w;
                    vProjPos.y /= vProjPos.w;

                    vProjPos.x =  0.5f * vProjPos.x + 0.5f; //
                    vProjPos.y =  0.5f * vProjPos.y + 0.5f; //
                    vBlurDir.set( 1.0f, 1.0f, -vProjPos.x, -vProjPos.y );
                }
                else {
                    // transform screen position to texture coordinate
//                    D3DXVec4Scale( &vProjPos, &vProjPos, 1.0f/vProjPos.w );
                    vProjPos.x /= vProjPos.w;
                    vProjPos.y /= vProjPos.w;

                    vProjPos.x = 0.5f * vProjPos.x + 0.5f; //
                    vProjPos.y = 0.5f * vProjPos.y + 0.5f; // upside down
                    // invert vector if light comes from behind the camera.
                    vBlurDir.set( -1.0f, -1.0f, vProjPos.x, vProjPos.y );
                }
            }
//            SetVSValue( pDev, VS_CONST_OFFSET, &vBlurDir, sizeof(D3DXVECTOR4) );
            m_shader.setOff(vBlurDir);
        }

        if (m_pSceneParam != null) {
            // parameter to scale down blur vector acoording to the distance from the view position.
            SScatteringShaderParameters param = new SScatteringShaderParameters();
            m_pSceneParam.getShaderParam( param );

            Vector3f v = new Vector3f(param.vESun.w, param.vSum.w, m_pSceneParam.m_fAtomosHeight );
//            SetPSValue( pDev, PS_CONST_DISTANCE, &v, sizeof(D3DXVECTOR3) );
            m_shader.setParam(v);
        }

        // maximum length of blur vector in texture space.
        float fMaxMove = 0.1f/16;
//        D3DXVECTOR2 vInvMax( 1.0f/fMaxMove, 1.0f/fMaxMove );
//        SetPSValue( pDev, PS_CONST_MAX, &vInvMax, sizeof(D3DXVECTOR2) );
        m_shader.setInvMax(1.0f/fMaxMove, 1.0f/fMaxMove);

        // fall off parameter of weights.
//        D3DXVECTOR4 vFallOff( -5000.0f, -1.5f, -1.5f, -1000.0f );
//        SetPSValue( pDev, PS_CONST_FALLOFF, &vFallOff, sizeof(D3DXVECTOR4) );
        m_shader.setFallOff(-5000.0f, -1.5f, -1.5f, -1000.0f);
    }
}
