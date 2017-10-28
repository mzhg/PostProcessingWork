package jet.opengl.demos.nvidia.fire;

import com.nvidia.developer.opengl.app.NvCameraMotionType;
import com.nvidia.developer.opengl.app.NvSampleApp;
import com.nvidia.developer.opengl.models.GLVAO;
import com.nvidia.developer.opengl.models.ModelGenerator;
import com.nvidia.developer.opengl.ui.NvTweakEnumi;
import com.nvidia.developer.opengl.ui.NvTweakVarBase;
import com.nvidia.developer.opengl.utils.NvImage;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector3f;

import java.io.IOException;
import java.nio.ByteBuffer;

import jet.opengl.demos.nvidia.lightning.XMesh;
import jet.opengl.postprocessing.common.GLCheck;
import jet.opengl.postprocessing.common.GLFuncProvider;
import jet.opengl.postprocessing.common.GLFuncProviderFactory;
import jet.opengl.postprocessing.common.GLenum;
import jet.opengl.postprocessing.shader.VisualDepthTextureProgram;
import jet.opengl.postprocessing.texture.RenderTargets;
import jet.opengl.postprocessing.texture.Texture2D;
import jet.opengl.postprocessing.texture.Texture2DDesc;
import jet.opengl.postprocessing.texture.TextureDataDesc;
import jet.opengl.postprocessing.texture.TextureUtils;
import jet.opengl.postprocessing.util.CacheBuffer;
import jet.opengl.postprocessing.util.CommonUtil;
import jet.opengl.postprocessing.util.Numeric;

/**
 * Created by mazhen'gui on 2017/9/1.
 */

public final class PerlinFire extends NvSampleApp {
    // Fire parameters

    static final boolean DEFAULT_JITTER = true;
    static final int DEFAULT_SAMPLING_RATE =16;
    static final float DEFAULT_SPEED = 0.6f;
    static final float DEFAULT_NOISE_SCALE = 1.35f;
    static final float DEFAULT_ROUGHNESS = 3.20f;
    static final float DEFAULT_SHAPE_SIZE =3.0f;
    static final float DEFAULT_FREQUENCY1 =1.0f;
    static final float DEFAULT_FREQUENCY2 =0.5f;
    static final float DEFAULT_FREQUENCY3 =0.25f;
    static final float DEFAULT_FREQUENCY4 =0.125f;
    static final float DEFAULT_FREQUENCY5 =0.0625f;

    // Textures and related shader resource views
    Texture2D g_pDepthBuffer = null;
    Texture2D g_pDepthBufferSRV = null;
    Texture2D g_pDepthBufferDSV = null;

    Texture2D g_pFireTexture = null;
    Texture2D g_pFireTextureSRV = null;

    Texture2D g_pNoiseTexture = null;
    Texture2D g_pJitterTextureSRV = null;
    Texture2D g_pPermTextureSRV = null;

// Textures and views for shadow mapping

    int g_pCubeMapDepth = 0;
    Texture2D g_pCubeMapDepthViewArray[] = new Texture2D[6];


// Effect techniques
    RenderProgram g_pCurrentTechnique = null;
    RenderProgram g_pPerlinFire3D = null;
    RenderProgram g_pPerlinFire4D = null;
    RenderProgram g_pPerlinFire3DFlow = null;
    RenderProgram g_pGeometryTechniqueZOnly = null;
    RenderProgram g_pGeometryTechniqueP0 = null;
    RenderProgram g_pGeometryTechniqueAux = null;
    final RenderProgram[] m_PerlinTechniques = new RenderProgram[3];
    VisualDepthTextureProgram m_DepthTextureProgram;

    final UniformData m_uniformData = new UniformData();
    int nSamplingRate = DEFAULT_SAMPLING_RATE;
    float g_fSpeed = DEFAULT_SPEED;
    float g_fShapeSize = DEFAULT_SHAPE_SIZE;
    int g_CubeMapSize = 800;
    XMesh g_OutsideWorldMesh;
    GLVAO g_pBoxMesh;
    private GLFuncProvider gl;
    private float m_total_time;
    private final Matrix4f m_temp = new Matrix4f();
    private RenderTargets m_renderTarget;
    private boolean m_printOnce;
    private int m_shadowMapSlice;
    private int m_technique = 2;
    private int m_debugTexture = 0;

    public PerlinFire(){
        m_uniformData.bJitter = DEFAULT_JITTER;

        m_uniformData.fNoiseScale = DEFAULT_NOISE_SCALE;
        m_uniformData.fRoughness = DEFAULT_ROUGHNESS;

        m_uniformData.fFrequencyWeights[0] = DEFAULT_FREQUENCY1;
        m_uniformData.fFrequencyWeights[1] = DEFAULT_FREQUENCY2;
        m_uniformData.fFrequencyWeights[2] = DEFAULT_FREQUENCY3;
        m_uniformData.fFrequencyWeights[3] = DEFAULT_FREQUENCY4;
        m_uniformData.fFrequencyWeights[4] = DEFAULT_FREQUENCY5;
    }

    @Override
    protected void initRendering() {
        gl = GLFuncProviderFactory.getGLFuncProvider();
        m_renderTarget= new RenderTargets();
        // Obtain techniques
        g_pPerlinFire3D = new RenderProgram("PerlinFireVS.vert", "PerlinFire3DPS.frag");
        g_pPerlinFire3D.setName("PerlinFire3D");
        g_pPerlinFire3DFlow = new RenderProgram("PerlinFireVS.vert", "PerlinFire3DFlowPS.frag");
        g_pPerlinFire3DFlow.setName("PerlinFire3DFlow");
        g_pPerlinFire4D = new RenderProgram("PerlinFireVS.vert", "PerlinFire4DPS.frag");
        g_pPerlinFire4D.setName("PerlinFire4D");

        m_PerlinTechniques[0] = g_pPerlinFire3D;
        m_PerlinTechniques[1] = g_pPerlinFire3DFlow;
        m_PerlinTechniques[2] = g_pPerlinFire4D;

        g_pGeometryTechniqueZOnly = new RenderProgram("SimpleVS.vert");
        g_pGeometryTechniqueAux = new RenderProgram("ShadowVS.vert");
        g_pGeometryTechniqueP0 = new RenderProgram("SimpleVS.vert", "SimplePS.frag");

        try {
            m_DepthTextureProgram = new VisualDepthTextureProgram(false);
        } catch (IOException e) {
            e.printStackTrace();
        }

        g_pCurrentTechnique = g_pPerlinFire4D;

//        g_OutsideWorldMesh.Create();
        g_OutsideWorldMesh = new XMesh("nvidia/PerlinFire/models/bonfire_wOrcs", 185);

        g_pBoxMesh = ModelGenerator.genCube(1.0f, false, false,false).genVAO();
        String root = "nvidia\\PerlinFire\\models\\";
        try {
            NvImage.upperLeftOrigin(false);
            int fireTexture = NvImage.uploadTextureFromDDSFile(root + "Firetex.dds");
            g_pFireTextureSRV = g_pFireTexture = TextureUtils.createTexture2D(GLenum.GL_TEXTURE_2D, fireTexture);
        } catch (IOException e) {
            e.printStackTrace();
        }

        GLCheck.checkError();

        // Create noise texture
        // Fill the texture with random numbers from 0 to 256
        ByteBuffer data = CacheBuffer.getCachedByteBuffer(256*256);
        for (int i = 0; i < 256 * 256; i++)
        {
//            data[i] = (unsigned char) (rand () % 256);
            data.put((byte)(Numeric.random(0, 256)));
        }
        data.flip();
        Texture2DDesc tex_desc = new Texture2DDesc(256, 256, GLenum.GL_R8);
        TextureDataDesc data_desc = new TextureDataDesc(GLenum.GL_RED, GLenum.GL_UNSIGNED_BYTE, data);
        g_pNoiseTexture = TextureUtils.createTexture2D(tex_desc, data_desc);
        g_pJitterTextureSRV = g_pNoiseTexture;

        tex_desc.format = GLenum.GL_R8UI;
        data_desc.format = GLenum.GL_RED_INTEGER;
        g_pPermTextureSRV = TextureUtils.createTexture2D(tex_desc, data_desc);
        GLCheck.checkError();

        m_transformer.setTranslation(0.0f, -15.0f, 30.0f);
        m_transformer.setMotionMode(NvCameraMotionType.FIRST_PERSON);

        // Setup cubemap
        GLCheck.checkError();
        PrepareCubeMap( );
        getGLContext().setSwapInterval(0);
    }

    @Override
    public void initUI() {
        mTweakBar.addValue("ShapeSize", createControl("g_fShapeSize"), 0.5f, 10.0f);
        mTweakBar.addValue("SamplingRate", createControl("nSamplingRate"), 8, 64);
//        mTweakBar.addValue("Show Depth Texture", createControl("m_showDepthTexture"));
//        mTweakBar.addValue("Show Shadow Map", createControl("m_showShadowMap"));
//        mTweakBar.addValue("Shadow Map Slice", createControl("m_shadowMapSlice"), 0, 5);

        mTweakBar.addEnum("Technique", createControl("m_technique"), new NvTweakEnumi[]{
              new NvTweakEnumi("PerlinFire3D", 0),
              new NvTweakEnumi("PerlinFire3DFlow", 1),
              new NvTweakEnumi("PerlinFire4D", 2),
        }, 0);

        NvTweakVarBase var = mTweakBar.addEnum("Debug Texture", createControl("m_debugTexture"), new NvTweakEnumi[]{
                new NvTweakEnumi("None", 0),
                new NvTweakEnumi("Depth Texture", 1),
                new NvTweakEnumi("ShadowMap", 2)},0);
        mTweakBar.subgroupSwitchStart(var);
        mTweakBar.subgroupSwitchCase(2);  // shadow map
        mTweakBar.addValue("Shadow Map Slice", createControl("m_shadowMapSlice"), 0, 5);
        mTweakBar.subgroupSwitchEnd();
    }

    @Override
    public void display() {
//        ID3D10RenderTargetView * pBackBufferRTV = DXUTGetD3D10RenderTargetView();
//        ID3D10DepthStencilView * pBackBufferDSV = DXUTGetD3D10DepthStencilView();
//
//        // Clear render target and the depth stencil
//        float ClearColor[4] = { 0.0f, 0.0f, 0.0f, 1.0f };
//        pd3dDevice->ClearRenderTargetView( pBackBufferRTV, ClearColor );
//        pd3dDevice->ClearDepthStencilView( pBackBufferDSV, D3D10_CLEAR_DEPTH, 1.0, 0 );
        m_renderTarget.bind();
//        gl.glClearBufferfv(GLenum.GL_COLOR, 0, CacheBuffer.wrap(0,0,0,0f));
//        gl.glClearBufferfv(GLenum.GL_DEPTH, 1, CacheBuffer.wrap(1f));
        gl.glEnable(GLenum.GL_DEPTH_TEST);
        gl.glDepthFunc(GLenum.GL_LESS);
        gl.glViewport(0,0, getGLContext().width(), getGLContext().height());

        // Set matrices
//        D3DXMATRIX mWorld = *g_Camera.GetWorldMatrix();
//        D3DXMATRIX mView = *g_Camera.GetViewMatrix();
//        D3DXMATRIX mProj = *g_Camera.GetProjMatrix();
//
//        D3DXMATRIX mViewProj = mView * mProj;
//        D3DXMATRIX mWorldView = mWorld * mView;
//        D3DXMATRIX mWorldViewProj = mWorldView * mProj;
        m_transformer.getModelViewMat(m_uniformData.mWorldViewProj);
        Matrix4f.mul(m_uniformData.mProj, m_uniformData.mWorldViewProj, m_uniformData.mWorldViewProj);

//        g_pmWorldViewProj -> SetMatrix ((float *) & mWorldViewProj );

//        g_pvLightPos->SetFloatVector( lightPos );

        // First, render an additional z-buffer for reads in the shader
        // Unfortunately DX10 doesn't allow to set a currently bound depth buffer
        // as a shader resource even if z-writes are explicitly disabled

//        pd3dDevice->ClearDepthStencilView( g_pDepthBufferDSV, D3D10_CLEAR_DEPTH, 1.0, 0 );

//        ID3D10RenderTargetView * pNullView [] = { NULL };
//        pd3dDevice->OMSetRenderTargets( 1, pNullView, g_pDepthBufferDSV );

//        g_pGeometryTechnique->GetPassByIndex(0)->Apply(0);
//        pd3dDevice->IASetInputLayout( g_pGeometryVertexLayout );
//        g_OutsideWorldMesh.Render( pd3dDevice );
        m_renderTarget.setRenderTexture(g_pDepthBufferDSV, null);
        gl.glViewport(0,0, g_pDepthBuffer.getWidth(), g_pDepthBuffer.getHeight());
        gl.glClearDepthf(1.0f);
        gl.glClear(GLenum.GL_DEPTH_BUFFER_BIT);
        g_pGeometryTechniqueZOnly.enable();
        g_pGeometryTechniqueZOnly.setUniforms(m_uniformData);
//        gl.glColorMask(false,false, false, false);
        g_OutsideWorldMesh.draw();

        if(!m_printOnce){
            g_pGeometryTechniqueZOnly.setName("Render Depth Buffer");
            g_pGeometryTechniqueZOnly.printPrograminfo();
        }

        if(m_debugTexture == 1){
            m_DepthTextureProgram.enable();
            m_DepthTextureProgram.setUniforms(0.1f, 1000.0f, 0, 1);
            gl.glBindFramebuffer(GLenum.GL_FRAMEBUFFER, 0);
            gl.glDisable(GLenum.GL_DEPTH_TEST);
            gl.glViewport(0,0, getGLContext().width(), getGLContext().height());

            gl.glActiveTexture(GLenum.GL_TEXTURE0);
            gl.glBindTexture(g_pDepthBufferDSV.getTarget(), g_pDepthBufferDSV.getTexture());
            gl.glDrawArrays(GLenum.GL_TRIANGLES, 0, 3);

            return;
        }

        // Second, render the whole scene to a shadowmap
        // Set a new viewport for rendering to cube map
//        D3D10_VIEWPORT SMVP, prevSMVP;
//        unsigned int prevViewpotrNum = 1;
//
//        SMVP.Height		= g_CubeMapSize;
//        SMVP.Width		= g_CubeMapSize;
//        SMVP.MinDepth	= 0;
//        SMVP.MaxDepth	= 1;
//        SMVP.TopLeftX	= 0;
//        SMVP.TopLeftY	= 0;
//
//        pd3dDevice->RSGetViewports( &prevViewpotrNum, &prevSMVP );
//        pd3dDevice->RSSetViewports( 1, &SMVP );

        // TODO render the Cube shadow map.
//        pd3dDevice->IASetInputLayout( g_pGeometryVertexLayout );
//        for( int i = 0; i < 6; i ++ )
//        {
//            g_piCubeMapFace->SetInt(i);
//
//            pd3dDevice->ClearDepthStencilView( g_pCubeMapDepthViewArray[i], D3D10_CLEAR_DEPTH, 1.0f, 0 );
//            pd3dDevice->OMSetRenderTargets( 1, pNullView, g_pCubeMapDepthViewArray[i] );
//
//            g_pGeometryTechniqueAux->GetPassByIndex(0)->Apply(0);
//            g_OutsideWorldMesh.Render( pd3dDevice );
//        }

        float rnd = Numeric.random() * 0.5f + 0.5f;
        m_uniformData.vLightPos.set( 0.25f * (rnd - 0.5f), 5.7f, 1.0f * (rnd - 0.5f));
        InitCubeMatrices( m_uniformData.vLightPos );
        gl.glViewport(0,0, g_CubeMapSize, g_CubeMapSize);
        g_pGeometryTechniqueAux.enable();
        g_pGeometryTechniqueAux.setUniforms(m_uniformData);

//        Matrix4f.perspective()

        for( int i = 0; i < 6; i ++ ){
            m_renderTarget.setRenderTexture(g_pCubeMapDepthViewArray[i], null);
            gl.glClear(GLenum.GL_DEPTH_BUFFER_BIT);
            m_uniformData.iCubeMapFace = i;
            g_pGeometryTechniqueAux.setCubeFace(i);
            g_OutsideWorldMesh.draw();
        }

        if(!m_printOnce){
            g_pGeometryTechniqueAux.setName("Render Cube Shadowmap");
            g_pGeometryTechniqueAux.printPrograminfo();
        }

        if(m_debugTexture == 2){
            m_DepthTextureProgram.enable();
            m_DepthTextureProgram.setUniforms(0.2f, 200.0f, 0, 1);
            gl.glBindFramebuffer(GLenum.GL_FRAMEBUFFER, 0);
            gl.glDisable(GLenum.GL_DEPTH_TEST);
            gl.glViewport(0,0, getGLContext().width(), getGLContext().height());

            gl.glActiveTexture(GLenum.GL_TEXTURE0);
            gl.glBindTexture(g_pCubeMapDepthViewArray[m_shadowMapSlice].getTarget(), g_pCubeMapDepthViewArray[m_shadowMapSlice].getTexture());
            gl.glDrawArrays(GLenum.GL_TRIANGLES, 0, 3);

            return;
        }

        // Switch back to main RTs

//        pd3dDevice->RSSetViewports( 1, &prevSMVP );  TODO
//        pd3dDevice->OMSetRenderTargets( 1, &pBackBufferRTV, pBackBufferDSV );

//        g_pfNoiseScale->SetFloat( g_fNoiseScale );
//        g_pfRoughness->SetFloat( g_fRoughness );
//        g_pfFrequencyWeights->SetFloatArray( g_fFrequencyWeights, 0, 5 );

//        g_pfTime->SetFloat( (float)fTime * g_fSpeed );
//        g_pfStepSize->SetFloat( (float)1.0f / g_nSamplingRate );
//        g_pbJitter->SetBool( g_bJitter );

        gl.glColorMask(true, true, true, true);
        gl.glViewport(0,0, getGLContext().width(), getGLContext().height());
        m_renderTarget.unbind();
        gl.glClear(GLenum.GL_COLOR_BUFFER_BIT|GLenum.GL_DEPTH_BUFFER_BIT);

        m_uniformData.fTime = m_total_time * g_fSpeed;
        m_uniformData.fStepSize = 1.0f/nSamplingRate;

//        g_pGeometryTechnique->GetPassByIndex(1)->Apply(0);  TODO
//        pd3dDevice->IASetInputLayout( g_pGeometryVertexLayout );
//        g_OutsideWorldMesh.Render( pd3dDevice );
        g_pGeometryTechniqueP0.enable();
        g_pGeometryTechniqueP0.setUniforms(m_uniformData);
        gl.glActiveTexture(GLenum.GL_TEXTURE4);
        gl.glBindTexture(GLenum.GL_TEXTURE_CUBE_MAP, g_pCubeMapDepth);

        g_OutsideWorldMesh.draw();

        if(!m_printOnce){
            g_pGeometryTechniqueP0.setName("Render Scene");
            g_pGeometryTechniqueP0.printPrograminfo();
        }
        gl.glBindTexture(GLenum.GL_TEXTURE_CUBE_MAP, 0);

        // Render fire volume
//        D3DXMATRIX mTranslate, mScale, mWorldViewInv;
//        D3DXMatrixTranslation( &mTranslate, 0, 0.5f, 0 );
//        D3DXMatrixScaling( &mScale, 4.0f * g_fShapeSize, 8.0f * g_fShapeSize, 4.0f * g_fShapeSize);
//        mWorldView = mTranslate * mScale * mWorld * mView;
//        mWorldViewProj = mWorldView * mProj;
//        D3DXMatrixInverse( &mWorldViewInv, NULL, &mWorldView );
        Matrix4f mWorldView = m_uniformData.mWorldViewProj;
        m_transformer.getModelViewMat(mWorldView);

        mWorldView.scale(4.0f * g_fShapeSize, 8.0f * g_fShapeSize, 4.0f * g_fShapeSize);
        mWorldView.translate(0, 0.5f, 0);
        Matrix4f mWorldViewInv = Matrix4f.invert(mWorldView, m_temp);
        Matrix4f.mul(m_uniformData.mProj, mWorldView, m_uniformData.mWorldViewProj);

//        D3DXVECTOR4 vEye;
//        D3DXVECTOR4 v(0, 0, 0, 1);
//        D3DXVec4Transform( &vEye, &v, &mWorldViewInv );
        Vector3f vEye = m_uniformData.vEyePos;
        vEye.set(0,0,0);
        Matrix4f.transformVector(mWorldViewInv, vEye, vEye);

//        g_pmWorldViewProj->SetMatrix ( (float *) & mWorldViewProj );
//        g_pvEyePos->SetFloatVector ( (float *) &vEye );
//        g_pfLightIntensity->SetFloat( rnd );
        m_uniformData.fLightIntensity = rnd;

//        g_pCurrentTechnique->GetPassByIndex(0)->Apply(0);
//        g_pBoxMesh->DrawSubset( 0 );
        g_pCurrentTechnique = m_PerlinTechniques[m_technique];
        g_pCurrentTechnique.enable();
        g_pCurrentTechnique.setUniforms(m_uniformData);
        gl.glActiveTexture(GLenum.GL_TEXTURE1);
        gl.glBindTexture(g_pDepthBuffer.getTarget(), g_pDepthBuffer.getTexture());
        gl.glActiveTexture(GLenum.GL_TEXTURE2);
        gl.glBindTexture(g_pFireTextureSRV.getTarget(), g_pFireTextureSRV.getTexture());
        gl.glActiveTexture(GLenum.GL_TEXTURE3);
        gl.glBindTexture(g_pJitterTextureSRV.getTarget(), g_pJitterTextureSRV.getTexture());
        gl.glTexParameteri(g_pJitterTextureSRV.getTarget(), GLenum.GL_TEXTURE_WRAP_S, GLenum.GL_REPEAT);
        gl.glTexParameteri(g_pJitterTextureSRV.getTarget(), GLenum.GL_TEXTURE_WRAP_T, GLenum.GL_REPEAT);
        gl.glTexParameteri(g_pJitterTextureSRV.getTarget(), GLenum.GL_TEXTURE_WRAP_R, GLenum.GL_REPEAT);
        gl.glActiveTexture(GLenum.GL_TEXTURE5);
        gl.glBindTexture(g_pPermTextureSRV.getTarget(), g_pPermTextureSRV.getTexture());

        gl.glEnable(GLenum.GL_BLEND);
        gl.glBlendFuncSeparate(GLenum.GL_ONE_MINUS_DST_COLOR, GLenum.GL_ONE, GLenum.GL_ONE, GLenum.GL_ONE);
        gl.glEnable(GLenum.GL_DEPTH_TEST);
        gl.glDepthMask(false);
        gl.glDisable(GLenum.GL_CULL_FACE);

//        gl.glEnable(GLenum.GL_CULL_FACE);
//        gl.glCullFace(GLenum.GL_BACK);
//        gl.glFrontFace(GLenum.GL_CCW);

        g_pBoxMesh.bind();
        g_pBoxMesh.draw(GLenum.GL_TRIANGLES);
        g_pBoxMesh.unbind();
        if(!m_printOnce){
            g_pCurrentTechnique.printPrograminfo();
        }

        gl.glDisable(GLenum.GL_BLEND);
        gl.glDepthMask(true);

        for(int i = 5; i>=0; i--){
            gl.glActiveTexture(GLenum.GL_TEXTURE0+i);
            gl.glBindTexture(g_pPermTextureSRV.getTarget(),0);
        }

        m_printOnce = true;

//        RenderText();

        // Render GUI

//        DXUT_BeginPerfEvent( DXUT_PERFEVENTCOLOR, L"HUD / Stats" );
//        RenderText();
//        g_HUD.OnRender( fElapsedTime );
//        g_SampleUI.OnRender( fElapsedTime );
//        g_SliderBar.OnRender( fElapsedTime );
//        DXUT_EndPerfEvent();

        m_total_time+= getFrameDeltaTime();
    }

    @Override
    protected void reshape(int width, int height) {
        if(width <=0 || height <= 0){
            return;
        }

        if(g_pDepthBuffer == null || g_pDepthBuffer.getWidth() != width || g_pDepthBuffer.getHeight() != height){
            CommonUtil.safeRelease(g_pDepthBuffer);

            Texture2DDesc tex_desc = new Texture2DDesc(width, height, GLenum.GL_DEPTH_COMPONENT24);
            g_pDepthBuffer = TextureUtils.createTexture2D(tex_desc, null);
            g_pDepthBufferDSV = g_pDepthBufferSRV = g_pDepthBuffer;
        }
        Matrix4f.perspective(60, (float)width/height, 0.1f, 1000.0f, m_uniformData.mProj);
    }

    // Prepare cube map texture array

    boolean PrepareCubeMap(/*ID3D10Device * pd3dDevice*/)
    {
        // Create cubic depth stencil texture.

        /*D3D10_TEXTURE2D_DESC dstex;
        dstex.Width = g_CubeMapSize;
        dstex.Height = g_CubeMapSize;
        dstex.MipLevels = 1;
        dstex.ArraySize = 6;
        dstex.SampleDesc.Count = 1;
        dstex.SampleDesc.Quality = 0;
        dstex.Format = DXGI_FORMAT_R24G8_TYPELESS;
        dstex.Usage = D3D10_USAGE_DEFAULT;
        dstex.BindFlags = D3D10_BIND_DEPTH_STENCIL | D3D10_BIND_SHADER_RESOURCE;
        dstex.CPUAccessFlags = 0;
        dstex.MiscFlags = D3D10_RESOURCE_MISC_TEXTURECUBE;
        if( FAILED( pd3dDevice->CreateTexture2D( &dstex, NULL, &g_pCubeMapDepth ) ) )
        {
            DXUTTRACE( L"Failed to create depth stencil texture\n" );
            return false;
        }*/
        g_pCubeMapDepth =gl.glGenTexture();
        gl.glBindTexture(GLenum.GL_TEXTURE_CUBE_MAP, g_pCubeMapDepth);
        gl.glTexStorage2D(GLenum.GL_TEXTURE_CUBE_MAP,1, GLenum.GL_DEPTH24_STENCIL8, g_CubeMapSize, g_CubeMapSize);
        gl.glTexParameteri(GLenum.GL_TEXTURE_CUBE_MAP, GLenum.GL_TEXTURE_MAG_FILTER, GLenum.GL_NEAREST);
        gl.glTexParameteri(GLenum.GL_TEXTURE_CUBE_MAP, GLenum.GL_TEXTURE_MIN_FILTER, GLenum.GL_NEAREST);
        gl.glTexParameteri(GLenum.GL_TEXTURE_CUBE_MAP, GLenum.GL_TEXTURE_WRAP_S, GLenum.GL_CLAMP_TO_BORDER);
        gl.glTexParameteri(GLenum.GL_TEXTURE_CUBE_MAP, GLenum.GL_TEXTURE_WRAP_T, GLenum.GL_CLAMP_TO_BORDER);
        gl.glTexParameteri(GLenum.GL_TEXTURE_CUBE_MAP, GLenum.GL_TEXTURE_WRAP_R, GLenum.GL_CLAMP_TO_BORDER);
        gl.glTexParameterfv(GLenum.GL_TEXTURE_CUBE_MAP, GLenum.GL_TEXTURE_BORDER_COLOR, CacheBuffer.wrap(1.0f, 1.0f, 1.0f,1.0f));
        gl.glBindTexture(GLenum.GL_TEXTURE_CUBE_MAP, 0);
        GLCheck.checkError();

        // Create the depth stencil view for the entire cube

//        D3D10_DEPTH_STENCIL_VIEW_DESC DescDS;

        for( int i = 0; i < 6; i ++ )
        {
            /*DescDS.Format = DXGI_FORMAT_D24_UNORM_S8_UINT;
            DescDS.ViewDimension = D3D10_DSV_DIMENSION_TEXTURE2DARRAY;
            DescDS.Texture2DArray.FirstArraySlice = i;
            DescDS.Texture2DArray.ArraySize = (unsigned) 1;
            DescDS.Texture2DArray.MipSlice = 0;

            if( FAILED( pd3dDevice->CreateDepthStencilView( g_pCubeMapDepth, &DescDS, &(g_pCubeMapDepthViewArray[i]) ) ) )
            {
                DXUTTRACE( L"Failed to create depth stencil view for a depth cube map\n" );
                return false;
            }*/

            int cubeMapSlice = gl.glGenTexture();
            gl.glTextureView(cubeMapSlice, GLenum.GL_TEXTURE_2D, g_pCubeMapDepth, GLenum.GL_DEPTH24_STENCIL8, 0,1, i, 1);
            g_pCubeMapDepthViewArray[i] = TextureUtils.createTexture2D(GLenum.GL_TEXTURE_2D, cubeMapSlice);
            GLCheck.checkError();
        }

        // Create the shader resource view for the shadow map

        /*D3D10_SHADER_RESOURCE_VIEW_DESC SRVDesc;
        ZeroMemory( &SRVDesc, sizeof( SRVDesc ) );
        SRVDesc.Format = DXGI_FORMAT_R24_UNORM_X8_TYPELESS;
        SRVDesc.ViewDimension = D3D10_SRV_DIMENSION_TEXTURECUBE;
        SRVDesc.Texture2DArray.MipLevels = 1;
        SRVDesc.Texture2DArray.MostDetailedMip = 0;
        SRVDesc.Texture2DArray.FirstArraySlice = 0;
        SRVDesc.Texture2DArray.ArraySize = 6;
        if( FAILED( pd3dDevice->CreateShaderResourceView( g_pCubeMapDepth, &SRVDesc, &g_pCubeMapTextureRV ) ) )
        {
            DXUTTRACE( L"Failed to create shader resource view for a depth stencil\n" );
            return false;
        }
        return true;*/
        return true;
    }

// Set matrices for cube mapping

    void InitCubeMatricesGL( Vector3f cubeCenter )
    {
        final Vector3f vLookDir = new Vector3f();
        final Vector3f vUpDir = new Vector3f();
        final Matrix4f[] cubeViewMatrices = m_uniformData.mCubeViewMatrixs;
        final Matrix4f cubeProjMatrix = m_uniformData.mCubeProjMatrix;

//        vLookDir = D3DXVECTOR3( 1.0f, 0.0f, 0.0f ) + (* (D3DXVECTOR3 *) cubeCenter);
//        vUpDir = D3DXVECTOR3( 0.0f, 1.0f, 0.0f );
//        D3DXMatrixLookAtLH( &cubeViewMatrices[0], (D3DXVECTOR3 *) cubeCenter, &vLookDir, &vUpDir );
        Vector3f.add(cubeCenter, Vector3f.X_AXIS_NEG, vLookDir);
        vUpDir.set(0.0f, 1.0f, 0.0f);
        Matrix4f.lookAt(cubeCenter, vLookDir, vUpDir, cubeViewMatrices[0]);

//        vLookDir = D3DXVECTOR3( -1.0f, 0.0f, 0.0f ) + (* (D3DXVECTOR3 *) cubeCenter);
//        vUpDir = D3DXVECTOR3( 0.0f, 1.0f, 0.0f );
//        D3DXMatrixLookAtLH( &cubeViewMatrices[1], (D3DXVECTOR3 *) cubeCenter, &vLookDir, &vUpDir );
        Vector3f.add(cubeCenter, Vector3f.X_AXIS, vLookDir);
        vUpDir.set(0.0f, 1.0f, 0.0f);
        Matrix4f.lookAt(cubeCenter, vLookDir, vUpDir, cubeViewMatrices[1]);

//        vLookDir = D3DXVECTOR3( 0.0f, 1.0f,  0.0f ) + (* (D3DXVECTOR3 *) cubeCenter);
//        vUpDir = D3DXVECTOR3( 0.0f, 0.0f, -1.0f );
//        D3DXMatrixLookAtLH( &cubeViewMatrices[2], (D3DXVECTOR3 *) cubeCenter, &vLookDir, &vUpDir );
        Vector3f.add(cubeCenter, Vector3f.Y_AXIS_NEG, vLookDir);
        vUpDir.set(0.0f, 0.0f, -1.0f);
        Matrix4f.lookAt(cubeCenter, vLookDir, vUpDir, cubeViewMatrices[2]);

//        vLookDir = D3DXVECTOR3( 0.0f, -1.0f, 0.0f ) + (* (D3DXVECTOR3 *) cubeCenter);
//        vUpDir = D3DXVECTOR3( 0.0f,  0.0f, 1.0f );
//        D3DXMatrixLookAtLH( &cubeViewMatrices[3], (D3DXVECTOR3 *) cubeCenter, &vLookDir, &vUpDir );
        Vector3f.add(cubeCenter, Vector3f.Y_AXIS, vLookDir);
        vUpDir.set(0.0f, 0.0f, 1.0f);
        Matrix4f.lookAt(cubeCenter, vLookDir, vUpDir, cubeViewMatrices[3]);

//        vLookDir = D3DXVECTOR3( 0.0f, 0.0f, 1.0f ) + (* (D3DXVECTOR3 *) cubeCenter);
//        vUpDir = D3DXVECTOR3( 0.0f, 1.0f, 0.0f );
//        D3DXMatrixLookAtLH( &cubeViewMatrices[4], (D3DXVECTOR3 *) cubeCenter, &vLookDir, &vUpDir );
        Vector3f.add(cubeCenter, Vector3f.Z_AXIS_NEG, vLookDir);
        vUpDir.set(0.0f, 1.0f, 0.0f);
        Matrix4f.lookAt(cubeCenter, vLookDir, vUpDir, cubeViewMatrices[4]);

//        vLookDir = D3DXVECTOR3( 0.0f, 0.0f, -1.0f ) + (* (D3DXVECTOR3 *) cubeCenter);
//        vUpDir = D3DXVECTOR3( 0.0f, 1.0f,  0.0f );
//        D3DXMatrixLookAtLH( &cubeViewMatrices[5], (D3DXVECTOR3 *) cubeCenter, &vLookDir, &vUpDir );
        Vector3f.add(cubeCenter, Vector3f.Z_AXIS, vLookDir);
        vUpDir.set(0.0f, 1.0f, 0.0f);
        Matrix4f.lookAt(cubeCenter, vLookDir, vUpDir, cubeViewMatrices[5]);

//        D3DXMatrixPerspectiveFovLH( &cubeProjMatrix, (float)D3DX_PI * 0.5f, 1.0f, 0.2f, 200.0f );
        Matrix4f.perspective(90, 1.0f, 0.2f, 200.0f, cubeProjMatrix);

//        g_pmCubeViewMatrixVariable->SetMatrixArray( (float *)cubeViewMatrices, 0, 6 );
//        g_pmCubeProjMatrixVariable->SetMatrix( cubeProjMatrix );
    }

    void InitCubeMatrices( Vector3f cubeCenter )
    {
        final Vector3f vLookDir = new Vector3f();
        final Vector3f vUpDir = new Vector3f();
        final Matrix4f[] cubeViewMatrices = m_uniformData.mCubeViewMatrixs;
        final Matrix4f cubeProjMatrix = m_uniformData.mCubeProjMatrix;

        Vector3f.add(cubeCenter, Vector3f.X_AXIS, vLookDir);
        vUpDir.set(0.0f, 1.0f, 0.0f);
        Matrix4f.lookAt(cubeCenter, vLookDir, vUpDir, cubeViewMatrices[0]);

//        vLookDir = D3DXVECTOR3( -1.0f, 0.0f, 0.0f ) + (* (D3DXVECTOR3 *) cubeCenter);
//        vUpDir = D3DXVECTOR3( 0.0f, 1.0f, 0.0f );
//        D3DXMatrixLookAtLH( &cubeViewMatrices[1], (D3DXVECTOR3 *) cubeCenter, &vLookDir, &vUpDir );
        Vector3f.add(cubeCenter, Vector3f.X_AXIS_NEG, vLookDir);
        vUpDir.set(0.0f, 1.0f, 0.0f);
        Matrix4f.lookAt(cubeCenter, vLookDir, vUpDir, cubeViewMatrices[1]);

//        vLookDir = D3DXVECTOR3( 0.0f, 1.0f,  0.0f ) + (* (D3DXVECTOR3 *) cubeCenter);
//        vUpDir = D3DXVECTOR3( 0.0f, 0.0f, -1.0f );
//        D3DXMatrixLookAtLH( &cubeViewMatrices[2], (D3DXVECTOR3 *) cubeCenter, &vLookDir, &vUpDir );
        Vector3f.add(cubeCenter, Vector3f.Y_AXIS, vLookDir);
        vUpDir.set(0.0f, 0.0f, -1.0f);
        Matrix4f.lookAt(cubeCenter, vLookDir, vUpDir, cubeViewMatrices[2]);

//        vLookDir = D3DXVECTOR3( 0.0f, -1.0f, 0.0f ) + (* (D3DXVECTOR3 *) cubeCenter);
//        vUpDir = D3DXVECTOR3( 0.0f,  0.0f, 1.0f );
//        D3DXMatrixLookAtLH( &cubeViewMatrices[3], (D3DXVECTOR3 *) cubeCenter, &vLookDir, &vUpDir );
        Vector3f.add(cubeCenter, Vector3f.Y_AXIS_NEG, vLookDir);
        vUpDir.set(0.0f, 0.0f, 1.0f);
        Matrix4f.lookAt(cubeCenter, vLookDir, vUpDir, cubeViewMatrices[3]);

//        vLookDir = D3DXVECTOR3( 0.0f, 0.0f, 1.0f ) + (* (D3DXVECTOR3 *) cubeCenter);
//        vUpDir = D3DXVECTOR3( 0.0f, 1.0f, 0.0f );
//        D3DXMatrixLookAtLH( &cubeViewMatrices[4], (D3DXVECTOR3 *) cubeCenter, &vLookDir, &vUpDir );
        Vector3f.sub(cubeCenter, Vector3f.Z_AXIS, vLookDir);
        vUpDir.set(0.0f, 1.0f, 0.0f);
        Matrix4f.lookAt(cubeCenter, vLookDir, vUpDir, cubeViewMatrices[4]);

//        vLookDir = D3DXVECTOR3( 0.0f, 0.0f, -1.0f ) + (* (D3DXVECTOR3 *) cubeCenter);
//        vUpDir = D3DXVECTOR3( 0.0f, 1.0f,  0.0f );
//        D3DXMatrixLookAtLH( &cubeViewMatrices[5], (D3DXVECTOR3 *) cubeCenter, &vLookDir, &vUpDir );
        Vector3f.add(cubeCenter, Vector3f.Z_AXIS_NEG, vLookDir);
        vUpDir.set(0.0f, 1.0f, 0.0f);
        Matrix4f.lookAt(cubeCenter, vLookDir, vUpDir, cubeViewMatrices[5]);

//        D3DXMatrixPerspectiveFovLH( &cubeProjMatrix, (float)D3DX_PI * 0.5f, 1.0f, 0.2f, 200.0f );
        Matrix4f.perspective(90, 1.0f, 0.2f, 200.0f, cubeProjMatrix);

//        g_pmCubeViewMatrixVariable->SetMatrixArray( (float *)cubeViewMatrices, 0, 6 );
//        g_pmCubeProjMatrixVariable->SetMatrix( cubeProjMatrix );
    }
}
