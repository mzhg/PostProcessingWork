package jet.opengl.demos.gpupro.cloud;

import org.lwjgl.util.vector.Vector2f;
import org.lwjgl.util.vector.Vector4f;

import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

import jet.opengl.postprocessing.common.GLFuncProvider;
import jet.opengl.postprocessing.common.GLFuncProviderFactory;
import jet.opengl.postprocessing.common.GLenum;
import jet.opengl.postprocessing.texture.Texture2D;
import jet.opengl.postprocessing.util.CacheBuffer;

/**
 * render final cloud as a screen quad.<p></p>
 * Created by mazhen'gui on 2017/7/6.
 */

final class CCloudPlane {
    static final int
            DIV_X = 4,
            DIV_Y = 4,
            NUM_VERTICES = (DIV_X+1) * (DIV_Y+1),
            NUM_INDICES  = 2*DIV_Y * (DIV_X+1) + (DIV_Y-1)*2,
            NUM_TRIANGLES = NUM_INDICES-2;

    private int      m_pVB;            // vertex buffer
    private int      m_pIB;            // index buffer
    private int      m_pDecl;          // vertex declaration
    private SSceneParamter m_pSceneParam;    // scene parameter

    private Texture2D m_pDensityMap;    // density map
    private Texture2D m_pBlurredMap;    // blurred density map
    private RenderTechnique m_shader;
    private GLFuncProvider gl;

    void Create( SSceneParamter pSceneParam, Texture2D pDensityMap, Texture2D pBlurredMap){
        gl = GLFuncProviderFactory.getGLFuncProvider();

        Delete();

        m_pSceneParam = pSceneParam;

        // create index and vertex buffer.
        CreateBuffers();

        // Create vertex declaration
//        static const D3DVERTEXELEMENT9 s_elements[] = {
//                { 0,  0,  D3DDECLTYPE_FLOAT4, D3DDECLMETHOD_DEFAULT, D3DDECLUSAGE_POSITION, 0 },
//                { 0, 16,  D3DDECLTYPE_FLOAT2, D3DDECLMETHOD_DEFAULT, D3DDECLUSAGE_TEXCOORD, 0 },
//                D3DDECL_END()
//        };
//        HRESULT hr = pDev->CreateVertexDeclaration( s_elements, &m_pDecl );
//        if ( FAILED(hr) ) {
//            return FALSE;
//        }

        m_pDecl = gl.glGenVertexArray();
        gl.glBindVertexArray(m_pDecl);
        gl.glBindBuffer(GLenum.GL_ARRAY_BUFFER, m_pVB);
        gl.glEnableVertexAttribArray(0);
        gl.glVertexAttribPointer(0, 4, GLenum.GL_FLOAT, false, 24,0);
        gl.glEnableVertexAttribArray(1);
        gl.glVertexAttribPointer(1, 2, GLenum.GL_FLOAT, false, 24,16);
        gl.glBindBuffer(GLenum.GL_ELEMENT_ARRAY_BUFFER, m_pIB);

        gl.glBindVertexArray(0);
        gl.glBindBuffer(GLenum.GL_ARRAY_BUFFER, 0);
        gl.glBindBuffer(GLenum.GL_ELEMENT_ARRAY_BUFFER, 0);

        // Create shaders
        CreateShaders();

        // Set textures
        m_pDensityMap = pDensityMap;

        m_pBlurredMap = pBlurredMap;
    }
    void Delete(){}

    private boolean m_printOnceprogram;
    void Draw(){
        gl.glBindVertexArray(m_pDecl);
        SetShaderConstant();

        // set textures.
        gl.glActiveTexture(GLenum.GL_TEXTURE0);
        gl.glBindTexture(m_pDensityMap.getTarget(), m_pDensityMap.getTexture());
        gl.glActiveTexture(GLenum.GL_TEXTURE1);
        gl.glBindTexture(m_pBlurredMap.getTarget(), m_pBlurredMap.getTexture());

        gl.glDrawElements(GLenum.GL_TRIANGLE_STRIP, NUM_INDICES, GLenum.GL_UNSIGNED_SHORT, 0);
        gl.glBindVertexArray(0);

        if(!m_printOnceprogram){
            m_shader.setName("Render Cloud");
            m_shader.printPrograminfo();
        }

        m_printOnceprogram = true;
    }

    private void CreateBuffers(){
        // create vertex buffer
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
        class S_VERTEX{
            final Vector4f vPos = new Vector4f();
            final float[] vTex  = new float[2];

            void store(FloatBuffer buffer){
                vPos.store(buffer);
                buffer.put(vTex);
            }
        };

        final S_VERTEX pV= new S_VERTEX();
        FloatBuffer buffer = CacheBuffer.getCachedFloatBuffer(NUM_VERTICES * 6);
        float fDepth = 0.99999f;
        for (int i = 0; i <= DIV_Y; ++i) {
            for (int j = 0; j <= DIV_X; ++j) {
                float fX = 1.0f - j/(float)(DIV_X);
                float fY = i/(float)(DIV_Y);
                pV.vPos.set( fX*2.0f-1.0f, -(fY*2.0f-1.0f), fDepth, 1.0f );
                pV.vTex[0] = fX;
                pV.vTex[1] = fY;

                pV.store(buffer);
            }
        }
        buffer.flip();

        // create index buffer
//        hr = pDev->CreateIndexBuffer( sizeof(USHORT)*NUM_INDICES,
//                D3DUSAGE_WRITEONLY, D3DFMT_INDEX16, D3DPOOL_DEFAULT, &m_pIB, NULL );
//        if ( FAILED(hr) ) {
//            return FALSE;
//        }
        m_pVB = gl.glGenBuffer();
        gl.glBindBuffer(GLenum.GL_ARRAY_BUFFER, m_pVB);
        gl.glBufferData(GLenum.GL_ARRAY_BUFFER, buffer, GLenum.GL_STATIC_DRAW);
        gl.glBindBuffer(GLenum.GL_ARRAY_BUFFER, 0);

//        USHORT* pI = NULL;
//        hr = m_pIB->Lock( 0, 0, (VOID**)&pI, 0 );
//        if ( FAILED(hr) ) {
//            return FALSE;
//        }
        ShortBuffer indices = CacheBuffer.getCachedShortBuffer(NUM_INDICES);
        for (int i = 0; i < DIV_Y; ++i) {
            for (int j = 0; j <= DIV_X; ++j) {
//                (*pI) = i*(DIV_X+1) + j;
//                ++pI;
//                (*pI) = (i+1)*(DIV_X+1) + j;
//                ++pI;
                indices.put((short)(i*(DIV_X+1) + j));
                indices.put((short)((i+1)*(DIV_X+1) + j));
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
    private void CreateShaders(){ m_shader = new RenderTechnique("CloudPlaneVS.vert", "CloudPlanePS.frag");}
    private void SetShaderConstant(){
        m_shader.enable();
        if (m_pSceneParam != null) {
            // Camera
            // transform screen position to world
//            D3DXMATRIX mC2W;
//            D3DXMatrixInverse( &mC2W, NULL, m_pSceneParam->m_pCamera->GetWorld2ProjMatrix() );
//            SetVSMatrix( pDev, VS_CONST_C2W, &mC2W );
            m_shader.setC2W(m_pSceneParam.m_viewProj);

            // view position
//            SetVSValue( pDev, VS_CONST_EYE, m_pSceneParam->m_pCamera->GetEyePt(), sizeof(FLOAT)*3 );
//            SetPSValue( pDev, PS_CONST_EYE, m_pSceneParam->m_pCamera->GetEyePt(), sizeof(FLOAT)*3 );
            m_shader.setEye(m_pSceneParam.m_Eye);

            // Light
//            SetVSValue( pDev, VS_CONST_LITDIR, m_pSceneParam->m_vLightDir, sizeof(FLOAT)*3 );
//            SetPSValue( pDev, PS_CONST_LITDIR, m_pSceneParam->m_vLightDir, sizeof(FLOAT)*3 );
//            SetPSValue( pDev, PS_CONST_LIGHT, &m_pSceneParam->m_vLightColor, sizeof(FLOAT)*3 );
//            SetPSValue( pDev, PS_CONST_AMBIENT, &m_pSceneParam->m_vAmbientLight, sizeof(FLOAT)*3 );
            m_shader.setLitDir(m_pSceneParam.m_vLightDir);
            m_shader.setLitCol(m_pSceneParam.m_vLightColor);
            m_shader.setAmb(m_pSceneParam.m_vAmbientLight);

            // scattering parameter
            SScatteringShaderParameters param = new SScatteringShaderParameters();
            m_pSceneParam.getShaderParam( param );
//            SetPSValue( pDev, PS_CONST_SCATTERING, &param, sizeof(SScatteringShaderParameters) );
            m_shader.setScat(param);

            // parameter to compute distance of cloud.
            Vector2f v = new Vector2f();
            m_pSceneParam.getCloudDistance( v );
//            SetPSValue( pDev, PS_CONST_DISTANCE, &v, sizeof(FLOAT)*2 );
            m_shader.setDistance(v);
        }

    }

}
