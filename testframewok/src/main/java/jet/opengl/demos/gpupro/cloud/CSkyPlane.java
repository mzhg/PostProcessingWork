package jet.opengl.demos.gpupro.cloud;

import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

import jet.opengl.postprocessing.common.Disposeable;
import jet.opengl.postprocessing.common.GLFuncProvider;
import jet.opengl.postprocessing.common.GLFuncProviderFactory;
import jet.opengl.postprocessing.common.GLenum;
import jet.opengl.postprocessing.util.CacheBuffer;

/**
 * A class to render sky with daylight scattering as a screen quad.<p></p>
 * Created by mazhen'gui on 2017/7/6.
 */

final class CSkyPlane implements Disposeable{
    static final short
            DIV_X = 4,
            DIV_Y = 4,
            NUM_VERTICES = (DIV_X+1) * (DIV_Y+1),
            NUM_INDICES  = 2*DIV_Y * (DIV_X+1) + (DIV_Y-1)*2,
            NUM_TRIANGLES = NUM_INDICES-2;

    private int      m_pVB;
    private int      m_pIB;
    private int      m_pDecl;

    private RenderTechnique        m_shader;
    private SSceneParamter         m_pSceneParam;
    private GLFuncProvider         gl;

    void Create(SSceneParamter pSceneParam){
        gl = GLFuncProviderFactory.getGLFuncProvider();
        dispose();

        m_pSceneParam = pSceneParam;

        // Create vertex declaraction
//        static const D3DVERTEXELEMENT9 s_elements[] = {
//                { 0,  0,  D3DDECLTYPE_FLOAT4, D3DDECLMETHOD_DEFAULT, D3DDECLUSAGE_POSITION, 0 },
//                D3DDECL_END()
//        };
//        HRESULT hr = pDev->CreateVertexDeclaration( s_elements, &m_pDecl );
//        if ( FAILED(hr) ) {
//            return FALSE;
//        }

        CreateBuffers();
        CreateShaders();

        m_pDecl = gl.glGenVertexArray();
        gl.glBindVertexArray(m_pDecl);
        gl.glBindBuffer(GLenum.GL_ARRAY_BUFFER, m_pVB);
        gl.glEnableVertexAttribArray(0);
        gl.glVertexAttribPointer(0, 4, GLenum.GL_FLOAT, false, 0,0);
        gl.glBindBuffer(GLenum.GL_ELEMENT_ARRAY_BUFFER, m_pIB);

        gl.glBindVertexArray(0);
        gl.glBindBuffer(GLenum.GL_ARRAY_BUFFER, 0);
        gl.glBindBuffer(GLenum.GL_ELEMENT_ARRAY_BUFFER, 0);
    }

    @Override
    public void dispose() {

    }

    private boolean m_printOnceprogram;
    void Draw(){
        gl.glBindVertexArray(m_pDecl);

        gl.glDisable(GLenum.GL_DEPTH_TEST);
        gl.glDisable(GLenum.GL_CULL_FACE);

        // Set shader uniforms
        m_shader.enable();
        if (m_pSceneParam != null) {

            // transform screen position to world space
//            D3DXMATRIX mC2W;
//            D3DXMatrixInverse( &mC2W, NULL, m_pSceneParam->m_pCamera->GetWorld2ProjMatrix() );
//            SetVSMatrix( pDev, VS_CONST_C2W, &mC2W );
            m_shader.setC2W(m_pSceneParam.m_viewProjInv);

            // view position
//            SetVSValue( pDev, VS_CONST_EYE, m_pSceneParam->m_pCamera->GetEyePt(), sizeof(FLOAT)*3 );
//            SetPSValue( pDev, PS_CONST_EYE, m_pSceneParam->m_pCamera->GetEyePt(), sizeof(FLOAT)*3 );
            m_shader.setEye(m_pSceneParam.m_Eye);

            // Light
//            SetVSValue( pDev, VS_CONST_LITDIR, m_pSceneParam->m_vLightDir, sizeof(FLOAT)*3 );
//            SetPSValue( pDev, PS_CONST_LITDIR, m_pSceneParam->m_vLightDir, sizeof(FLOAT)*3 );
            m_shader.setLitDir(m_pSceneParam.m_vLightDir);

            // Scattering parameter
            SScatteringShaderParameters param = new SScatteringShaderParameters();
            m_pSceneParam.getShaderParam( param );
//            SetPSValue( pDev, PS_CONST_SCATTERING, &param, sizeof(SScatteringShaderParameters) );
            m_shader.setScat(param);
        }

        gl.glDrawElements(GLenum.GL_TRIANGLE_STRIP, NUM_INDICES, GLenum.GL_UNSIGNED_SHORT, 0);
        gl.glBindVertexArray(0);

        if(!m_printOnceprogram){
            m_shader.setName("Render Sky");
            m_shader.printPrograminfo();
        }

        m_printOnceprogram = true;
    }

    private void CreateBuffers(){
        // create vertex buffer
        // This sample uses a grid for rendering sky.
//        HRESULT hr = pDev->CreateVertexBuffer( sizeof(S_VERTEX)*NUM_VERTICES,
//        D3DUSAGE_WRITEONLY, 0, D3DPOOL_DEFAULT, &m_pVB, NULL );
//        if ( FAILED(hr) ) {
//            return FALSE;
//        }
//        S_VERTEX* pV = NULL;
//        hr = m_pVB->Lock( 0, 0, (VOID**)&pV, 0 );
//        if ( FAILED(hr) ) {
//            return FALSE;
//        }

        FloatBuffer vertices = CacheBuffer.getCachedFloatBuffer(NUM_VERTICES * 4);
        for (int i = 0; i <= DIV_Y; ++i) {
            for (int j = 0; j <= DIV_X; ++j) {
                float fX = 1.0f - j/(float)(DIV_X);
                float fY = 1.0f - i/(float)(DIV_Y);
//                pV->vPos = D3DXVECTOR4( fX*2.0f-1.0f, fY*2.0f-1.0f, 1.0f, 1.0f );
//                ++pV;
                vertices.put(fX*2.0f-1.0f).put(fY*2.0f-1.0f).put(1.0f).put(1.0f);
            }
        }

        vertices.flip();
        m_pVB = gl.glGenBuffer();
        gl.glBindBuffer(GLenum.GL_ARRAY_BUFFER, m_pVB);
        gl.glBufferData(GLenum.GL_ARRAY_BUFFER, vertices, GLenum.GL_STATIC_DRAW);
        gl.glBindBuffer(GLenum.GL_ARRAY_BUFFER, 0);

        // create index buffer
//        hr = pDev->CreateIndexBuffer( sizeof(USHORT)*NUM_INDICES,
//                D3DUSAGE_WRITEONLY, D3DFMT_INDEX16, D3DPOOL_DEFAULT, &m_pIB, NULL );
//        if ( FAILED(hr) ) {
//            return FALSE;
//        }
//        USHORT* pI = NULL;
//        hr = m_pIB->Lock( 0, 0, (VOID**)&pI, 0 );
//        if ( FAILED(hr) ) {
//            return FALSE;
//        }

        ShortBuffer indices = CacheBuffer.getCachedShortBuffer(NUM_INDICES);
        for (short i = 0; i < DIV_Y; ++i) {
            for (short j = 0; j <= DIV_X; ++j) {
//                (*pI) = i*(DIV_X+1) + j;
//                ++pI;
//                (*pI) = (i+1)*(DIV_X+1) + j;
//                ++pI;
                indices.put((short) (i*(DIV_X+1) + j));
                indices.put((short) ((i+1)*(DIV_X+1) + j));
            }
            if (i+1 < DIV_Y) {
//                (*pI) = (i+1)*(DIV_X+1) + DIV_X;
//                ++pI;
//                (*pI) = (i+1)*(DIV_X+1);
//                ++pI;
                indices.put((short)((i+1)*(DIV_X+1) + DIV_X));
                indices.put((short)((i+1)*(DIV_X+1)));
            }
        }
//        m_pIB->Unlock();
        indices.flip();

        m_pIB = gl.glGenBuffer();
        gl.glBindBuffer(GLenum.GL_ELEMENT_ARRAY_BUFFER, m_pIB);
        gl.glBufferData(GLenum.GL_ELEMENT_ARRAY_BUFFER, indices, GLenum.GL_STATIC_DRAW);
        gl.glBindBuffer(GLenum.GL_ELEMENT_ARRAY_BUFFER, 0);
    }

    private void CreateShaders(){
        m_shader = new RenderTechnique("SkyVS.vert", "SkyPS.frag");
    }


}
