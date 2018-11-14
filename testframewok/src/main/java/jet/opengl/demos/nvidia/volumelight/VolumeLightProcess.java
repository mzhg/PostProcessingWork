package jet.opengl.demos.nvidia.volumelight;

import java.io.IOException;

import jet.opengl.postprocessing.common.Disposeable;
import jet.opengl.postprocessing.common.GLFuncProvider;
import jet.opengl.postprocessing.common.GLFuncProviderFactory;
import jet.opengl.postprocessing.common.GLenum;
import jet.opengl.postprocessing.core.PostProcessingReconstructCameraZProgram;
import jet.opengl.postprocessing.shader.GLSLProgram;
import jet.opengl.postprocessing.texture.RenderTargets;
import jet.opengl.postprocessing.texture.SamplerDesc;
import jet.opengl.postprocessing.texture.SamplerUtils;
import jet.opengl.postprocessing.texture.Texture2D;
import jet.opengl.postprocessing.texture.Texture2DDesc;
import jet.opengl.postprocessing.texture.TextureDataDesc;
import jet.opengl.postprocessing.texture.TextureDesc;
import jet.opengl.postprocessing.texture.TextureUtils;
import jet.opengl.postprocessing.util.Numeric;
import sun.swing.plaf.GTKKeybindings;

public class VolumeLightProcess implements Disposeable {
    public static final int SHADOW_MIPS	= 4;
    public static final int SHADOW_HOLE_MIP = 3; // SHADOW_HOLE_MIP < SHADOW_MIPS !!!

    int		g_ScaleRatio = 4;

    private Texture2D           g_pShadowMapTextureWorld = null;
//    ID3D10ShaderResourceView*   g_pShadowMapTextureWorldSRV = null;
//    ID3D10RenderTargetView*     g_pShadowMapTextureWorldRTV = null;

    private final Texture2D[]   g_pShadowMapTextureScaled = new Texture2D[SHADOW_MIPS];
//    ID3D10ShaderResourceView*   g_pShadowMapTextureScaledSRV[SHADOW_MIPS];
//    ID3D10RenderTargetView*		g_pShadowMapTextureScaledRTV[SHADOW_MIPS];

    private Texture2D           g_pShadowMapTextureScaledOpt = null;
//    ID3D10ShaderResourceView*   g_pShadowMapTextureScaledOptSRV = null;
//    ID3D10RenderTargetView*		g_pShadowMapTextureScaledOptRTV = null;

//    ID3D10Texture2D*            g_pShadowMapTextureHole[2];
//    ID3D10ShaderResourceView*   g_pShadowMapTextureHoleSRV[2];
//    ID3D10RenderTargetView*     g_pShadowMapTextureHoleRTV[2];

    private Texture2D           g_pDepthBufferTexture = null;
//    ID3D10ShaderResourceView*   g_pDepthBufferTextureSRV = null;
//    ID3D10DepthStencilView*     g_pDepthBufferTextureDSV = null;

    private Texture2D           g_pDepthBufferTextureWS = null;
//    ID3D10ShaderResourceView*   g_pDepthBufferTextureWSSRV = null;
//    ID3D10RenderTargetView*     g_pDepthBufferTextureWSRTV = null;

    private Texture2D           g_pNoiseTexture = null;

    private final Texture2D[] g_pShadowMapTextureHole = new Texture2D[2];

    public static final int NUM_TONEMAP_TEXTURES = 5;       // Number of stages in the 3x3 down-scaling
            // of average luminance textures
    public static final int NUM_BLOOM_TEXTURES = 2;

    private Texture2D           g_pHDRTexture;
//    ID3D10ShaderResourceView*	g_pHDRTextureSRV;
//    ID3D10RenderTargetView*		g_pHDRTextureRTV;

    private Texture2D           g_pEdgeTextureFS = null;
//    ID3D10ShaderResourceView*	g_pEdgeTextureFSSRV = NULL;
//    ID3D10RenderTargetView*		g_pEdgeTextureFSRTV = NULL;

    private Texture2D           g_pHDRTextureScaled;
//    ID3D10ShaderResourceView*	g_pHDRTextureScaledSRV;
//    ID3D10RenderTargetView*		g_pHDRTextureScaledRTV;

    private Texture2D           g_pHDRTextureScaled2;
//    ID3D10ShaderResourceView*	g_pHDRTextureScaled2SRV;
//    ID3D10RenderTargetView*		g_pHDRTextureScaled2RTV;

    private Texture2D           g_pHDRTextureScaled3;
//    ID3D10ShaderResourceView*	g_pHDRTextureScaled3SRV;
//    ID3D10RenderTargetView*		g_pHDRTextureScaled3RTV;

    private final Texture2D[]   g_apTexToneMap10 = new Texture2D[NUM_TONEMAP_TEXTURES]; // Tone mapping calculation textures
//    ID3D10ShaderResourceView*	g_apTexToneMapRV10[NUM_TONEMAP_TEXTURES];
//    ID3D10RenderTargetView*		g_apTexToneMapRTV10[NUM_TONEMAP_TEXTURES];
//    ID3D10Texture2D*			g_apTexBloom10[NUM_BLOOM_TEXTURES]; // Blooming effect intermediate texture
//    ID3D10ShaderResourceView*	g_apTexBloomRV10[NUM_BLOOM_TEXTURES];
//    ID3D10RenderTargetView*		g_apTexBloomRTV10[NUM_BLOOM_TEXTURES];

    /*ID3D10EffectShaderResourceVariable* g_pS0;
    ID3D10EffectShaderResourceVariable* g_pS1;
    ID3D10EffectShaderResourceVariable* g_pS2;
    ID3D10EffectVectorVariable* g_pavSampleOffsetsHorizontal;
    ID3D10EffectVectorVariable* g_pavSampleOffsetsVertical;
    ID3D10EffectVectorVariable* g_pavSampleWeights;*/

    private Texture2D          g_pTexBrightPass10 = null; // Bright pass filter
//    ID3D10ShaderResourceView* g_pTexBrightPassRV10 = NULL;
//    ID3D10RenderTargetView*   g_pTexBrightPassRTV10 = NULL;

    // samplers
    private int m_samplerPoint;

    private PostProcessingReconstructCameraZProgram  m_LinearDepthProgram;
    private GLSLProgram m_MinMax2x2_1;
    private GLSLProgram m_MinMax2x2_2;
    private GLSLProgram m_MinMax3x3;
    private GLSLProgram m_PropagateMaxDepth;
    private GLSLProgram m_PropagateMinDepth_0;
    private GLSLProgram m_PropagateMinDepth_1;

    private GLFuncProvider gl;
    private RenderTargets m_fbo;
    private int  m_dummyVAO;

    public void initlizeGL(int shadowMapSize){
        gl = GLFuncProviderFactory.getGLFuncProvider();
        m_dummyVAO = gl.glGenVertexArray();
        m_fbo = new RenderTargets();

        Texture2DDesc desc = new Texture2DDesc(shadowMapSize, shadowMapSize, GLenum.GL_R32F);
        g_pShadowMapTextureWorld = TextureUtils.createTexture2D(desc, null);

        for( int i = 0; i < SHADOW_MIPS; i++ )
        {
            desc.format = GLenum.GL_RG32F;
//            SRVdesc.Format = DXGI_FORMAT_R32G32_FLOAT;
//            RTVdesc.Format = DXGI_FORMAT_R32G32_FLOAT;

            desc.width /= 2;
            desc.height /= 2;

//            V_RETURN( pd3dDevice->CreateTexture2D( &desc, NULL, &g_pShadowMapTextureScaled[i] ) );
//            V_RETURN( pd3dDevice->CreateShaderResourceView( g_pShadowMapTextureScaled[i], &SRVdesc, &g_pShadowMapTextureScaledSRV[i] ) );
//            V_RETURN( pd3dDevice->CreateRenderTargetView( g_pShadowMapTextureScaled[i], &RTVdesc, &g_pShadowMapTextureScaledRTV[i] ) );
            g_pShadowMapTextureScaled[i] = TextureUtils.createTexture2D(desc, null);

            if( i == SHADOW_HOLE_MIP )
            {
                desc.format = GLenum.GL_R32F;
//                SRVdesc.Format = DXGI_FORMAT_R32_FLOAT;
//                RTVdesc.Format = DXGI_FORMAT_R32_FLOAT;

                for( int j = 0; j < 2; j++ )
                {
//                    V_RETURN( pd3dDevice->CreateTexture2D( &desc, NULL, &g_pShadowMapTextureHole[j] ) );
//                    V_RETURN( pd3dDevice->CreateShaderResourceView( g_pShadowMapTextureHole[j], &SRVdesc, &g_pShadowMapTextureHoleSRV[j] ) );
//                    V_RETURN( pd3dDevice->CreateRenderTargetView( g_pShadowMapTextureHole[j], &RTVdesc, &g_pShadowMapTextureHoleRTV[j] ) );
                    g_pShadowMapTextureHole[j] = TextureUtils.createTexture2D(desc, null);
                }
            }
        }

        // Currently use a single optimized texture
        desc.format = GLenum.GL_RG32F;
//        SRVdesc.Format = DXGI_FORMAT_R32G32_FLOAT;
//        RTVdesc.Format = DXGI_FORMAT_R32G32_FLOAT;

        // Create texture for optimized steps detection, used during main shading pass
//        V_RETURN( pd3dDevice->CreateTexture2D( &desc, NULL, &g_pShadowMapTextureScaledOpt ) );
        g_pShadowMapTextureScaledOpt = TextureUtils.createTexture2D(desc, null);

        createSamplers();
        createPrograms();
    }

    private void CreateDepthBufferTexture(int width, int height )
    {
//        HRESULT hr = S_OK;
        DeleteDepthBufferTexture();

        Texture2DDesc desc = new Texture2DDesc(width, height, GLenum.GL_R32F);
        /*desc.Width = width;
        desc.Height = height;
        desc.Format = DXGI_FORMAT_R32_TYPELESS; // Format of the main depth buffer
        desc.MipLevels = 1;
        desc.ArraySize = 1;
        desc.Usage = D3D10_USAGE_DEFAULT;
        desc.BindFlags = D3D10_BIND_SHADER_RESOURCE | D3D10_BIND_DEPTH_STENCIL;
        desc.MiscFlags = 0;
        desc.CPUAccessFlags = 0;
        desc.SampleDesc.Count = 1;
        desc.SampleDesc.Quality = 0;*/
//        V_RETURN( pd3dDevice->CreateTexture2D( &desc, NULL, &g_pDepthBufferTexture ) );
        g_pDepthBufferTexture = TextureUtils.createTexture2D(desc, null);

        /*desc.BindFlags = D3D10_BIND_SHADER_RESOURCE | D3D10_BIND_RENDER_TARGET;
        V_RETURN( pd3dDevice->CreateTexture2D( &desc, NULL, &g_pDepthBufferTextureWS ) );*/
        g_pDepthBufferTextureWS = TextureUtils.createTexture2D(desc, null);

        /*D3D10_SHADER_RESOURCE_VIEW_DESC SRVdesc;
        SRVdesc.Format = DXGI_FORMAT_R32_FLOAT;
        SRVdesc.ViewDimension = D3D10_SRV_DIMENSION_TEXTURE2D;
        SRVdesc.Texture2D.MipLevels = 1;
        SRVdesc.Texture2D.MostDetailedMip = 0;
        V_RETURN( pd3dDevice->CreateShaderResourceView( g_pDepthBufferTexture, &SRVdesc, &g_pDepthBufferTextureSRV ) );
        V_RETURN( pd3dDevice->CreateShaderResourceView( g_pDepthBufferTexture, &SRVdesc, &g_pDepthBufferTextureWSSRV ) );

        D3D10_DEPTH_STENCIL_VIEW_DESC DSVdesc;
        DSVdesc.Format = DXGI_FORMAT_D32_FLOAT;
        DSVdesc.ViewDimension = D3D10_DSV_DIMENSION_TEXTURE2D;
        DSVdesc.Texture2D.MipSlice = 0;
        V_RETURN( pd3dDevice->CreateDepthStencilView( g_pDepthBufferTexture, &DSVdesc, &g_pDepthBufferTextureDSV ) );

        D3D10_RENDER_TARGET_VIEW_DESC RTVdesc;
        RTVdesc.Format = DXGI_FORMAT_R32_FLOAT;
        RTVdesc.ViewDimension = D3D10_RTV_DIMENSION_TEXTURE2D;
        RTVdesc.Texture2D.MipSlice = 0;
        V_RETURN( pd3dDevice->CreateRenderTargetView( g_pDepthBufferTextureWS, &RTVdesc, &g_pDepthBufferTextureWSRTV ) );

        return hr;*/
    }

    public void renderVolumeLight(Texture2D shadowMap, float lightNear, float lightFar){
        m_fbo.bind();
        gl.glBindVertexArray(m_dummyVAO);
        gl.glDisable(GLenum.GL_DEPTH_TEST);
        gl.glDisable(GLenum.GL_CULL_FACE);
        gl.glDisable(GLenum.GL_BLEND);

        // 1, Linearlize the shadow map depth value.
        m_fbo.setRenderTexture(g_pShadowMapTextureWorld, null);
        gl.glViewport(0,0, g_pShadowMapTextureWorld.getWidth(), g_pShadowMapTextureWorld.getHeight());
        m_LinearDepthProgram.enable();
        m_LinearDepthProgram.setCameraRange(lightNear, lightFar);
        m_LinearDepthProgram.setSampleIndex(0);
        gl.glBindTextureUnit(0, shadowMap.getTexture());
        gl.glBindSampler(0, m_samplerPoint);

        gl.glDrawArrays(GLenum.GL_TRIANGLES, 0, 3);

        // 2, Downscale Depth, produce Min and Max mips for optimized tracing
        m_fbo.setRenderTexture(g_pShadowMapTextureScaled[0], null);
        gl.glViewport(0,0,g_pShadowMapTextureScaled[0].getWidth(), g_pShadowMapTextureScaled[0].getHeight());

        m_MinMax2x2_1.enable();
        setBufferSizeInv(m_MinMax2x2_1, g_pShadowMapTextureScaled[0].getWidth(), g_pShadowMapTextureScaled[0].getHeight());
        gl.glBindTextureUnit(0, g_pShadowMapTextureWorld.getTexture());
        gl.glDrawArrays(GLenum.GL_TRIANGLES, 0, 3);

        for( int i = 0; i < SHADOW_MIPS - 1; i++ ){
            m_fbo.setRenderTexture(g_pShadowMapTextureScaled[i + 1], null);
            gl.glViewport(0,0,g_pShadowMapTextureScaled[i + 1].getWidth(), g_pShadowMapTextureScaled[i + 1].getHeight());

            m_MinMax2x2_2.enable();
            setBufferSizeInv(m_MinMax2x2_2, g_pShadowMapTextureScaled[i + 1].getWidth(), g_pShadowMapTextureScaled[i + 1].getHeight());
            gl.glBindTextureUnit(0, g_pShadowMapTextureScaled[i].getTexture());
            gl.glDrawArrays(GLenum.GL_TRIANGLES, 0, 3);
        }

        m_fbo.setRenderTexture(g_pShadowMapTextureScaledOpt, null);
        gl.glViewport(0,0,g_pShadowMapTextureScaledOpt.getWidth(), g_pShadowMapTextureScaledOpt.getHeight());

        m_MinMax3x3.enable();
        setBufferSizeInv(m_MinMax3x3, g_pShadowMapTextureScaledOpt.getWidth(), g_pShadowMapTextureScaledOpt.getHeight());
        gl.glBindTextureUnit(0, g_pShadowMapTextureScaled[SHADOW_MIPS - 1].getTexture());
        gl.glDrawArrays(GLenum.GL_TRIANGLES, 0, 3);

        ////////////////////////////////////////////////////////////////////////////////////////////////////////////
        //
        //		Use a number of passes to fill the "holes" in the depth map
        //
        //		The idea is to increase light density in shadowmap "hole" areas
        //		(small) areas with big depth discontinuetes.
        //
        //		DEPTH MAP PROCESSING
        //
        //		1) BEFORE:
        //		____    ___
        //				   \____     _______
        //
        //
        //		2) AFTER:
        //		___________
        //				   \_________________
        //
        //		If, after the final step, the area is occluded, but was initially lit,
        //		we increase light density for those.
        //
        ////////////////////////////////////////////////////////////////////////////////////////////////////////////
        m_fbo.setRenderTexture(g_pShadowMapTextureHole[0], null);
        gl.glViewport(0, 0, g_pShadowMapTextureHole[0].getWidth(), g_pShadowMapTextureHole[0].getHeight());
        m_PropagateMinDepth_0.enable();
        gl.glBindTextureUnit(0, g_pShadowMapTextureScaled[SHADOW_HOLE_MIP].getTexture());
        gl.glDrawArrays(GLenum.GL_TRIANGLES, 0, 3);

        int oddPass = 0;

        for( int i = 0; i < 2; i++, oddPass = 1 - oddPass )
        {
//            pd3dDevice->OMSetRenderTargets( 1, &g_pShadowMapTextureHoleRTV[1 - oddPass], NULL );
            m_fbo.setRenderTexture(g_pShadowMapTextureHole[1 - oddPass], null);
//            g_pS0->SetResource( g_pShadowMapTextureHoleSRV[oddPass] );
            gl.glBindTextureUnit(0, g_pShadowMapTextureHole[oddPass].getTexture());
            /*g_pEffect->GetTechniqueByName( "DepthProcessing" )->GetPassByName( "pPropagateMinDepth_1" )->Apply( 0 );
            DrawFullScreenQuad10( pd3dDevice );*/
            m_PropagateMinDepth_1.enable();
            gl.glDrawArrays(GLenum.GL_TRIANGLES, 0, 3);
        }

        for( int i = 0; i < 5; i++, oddPass = 1 - oddPass )
        {
//            pd3dDevice->OMSetRenderTargets( 1, &g_pShadowMapTextureHoleRTV[1 - oddPass], NULL );
            m_fbo.setRenderTexture(g_pShadowMapTextureHole[1 - oddPass], null);
//            g_pS0->SetResource( g_pShadowMapTextureHoleSRV[oddPass] );
            gl.glBindTextureUnit(0, g_pShadowMapTextureHole[oddPass].getTexture());
            /*g_pEffect->GetTechniqueByName( "DepthProcessing" )->GetPassByName( "pPropagateMaxDepth" )->Apply( 0 );
            DrawFullScreenQuad10( pd3dDevice );*/
            m_PropagateMaxDepth.enable();
            gl.glDrawArrays(GLenum.GL_TRIANGLES, 0, 3);
        }


    }

    private void setBufferSizeInv(GLSLProgram program, float width, float height){
        int index = program.getUniformLocation("g_BufferWidthInv");
        if(index >= 0){
            gl.glUniform1f(index, 1f/width);
        }

        index = program.getUniformLocation("g_BufferHeightInv");
        if(index >= 0){
            gl.glUniform1f(index, 1f/height);
        }
    }

    private void createPrograms(){
        try {
            final String quadVertex = "shader_libs/PostProcessingDefaultScreenSpaceVS.vert";
            final String root = "VolumeLight/shaders/";
            m_LinearDepthProgram = new PostProcessingReconstructCameraZProgram(false);
            m_MinMax2x2_1 = GLSLProgram.createFromFiles(quadVertex, root+"MinMax2x2_1PS.frag");
            m_MinMax2x2_2 = GLSLProgram.createFromFiles(quadVertex, root+"MinMax2x2_2PS.frag");
            m_MinMax3x3 = GLSLProgram.createFromFiles(quadVertex, root+"MinMax3x3PS.frag");
            m_PropagateMaxDepth = GLSLProgram.createFromFiles(quadVertex, root+"PropagateMaxDepthPS.frag");
            m_PropagateMinDepth_0 = GLSLProgram.createFromFiles(quadVertex, root+"PropagateMinDepth_0PS.frag");
            m_PropagateMinDepth_1 = GLSLProgram.createFromFiles(quadVertex, root+"PropagateMinDepth_1PS.frag");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void createSamplers(){
        SamplerDesc desc = new SamplerDesc();
        desc.minFilter = GLenum.GL_NEAREST;
        desc.magFilter = GLenum.GL_NEAREST;

        m_samplerPoint = SamplerUtils.createSampler(desc);
    }

    public void onResize(int width, int height){
        CreateDepthBufferTexture( width, height);

        Texture2DDesc desc = new Texture2DDesc(width/8, height/8, GLenum.GL_R8);
        /*desc.Width = pBackBufferSurfaceDesc->Width / 8; // Noise texture scale
        desc.Height = pBackBufferSurfaceDesc->Height / 8;
        desc.Format = DXGI_FORMAT_R8_UNORM;
        desc.MipLevels = 1;
        desc.ArraySize = 1;
        desc.Usage = D3D10_USAGE_DEFAULT;
        desc.BindFlags = D3D10_BIND_SHADER_RESOURCE;
        desc.MiscFlags = 0;
        desc.CPUAccessFlags = 0;
        desc.SampleDesc.Count = 1;
        desc.SampleDesc.Quality = 0;*/

        TextureDataDesc data = new TextureDataDesc();
        byte[] pNoiseData;
        pNoiseData = new byte[desc.width * desc.height];
        for( int i = 0; i < desc.width * desc.height; i++ )
            pNoiseData[i] = (byte) Numeric.random(0.f, 255.9f); /*( ( (float)rand() / (float)RAND_MAX ) * 256 )*/;

        data.data = pNoiseData;
        data.format = GLenum.GL_RED;
        data.type = GLenum.GL_UNSIGNED_BYTE;
//        V_RETURN( pd3dDevice->CreateTexture2D( &desc, &data, &g_pNoiseTexture ) );
        g_pNoiseTexture = TextureUtils.createTexture2D(desc, data);

//        delete [] pNoiseData;

        /*D3D10_SHADER_RESOURCE_VIEW_DESC SRVdesc;
        SRVdesc.Format = DXGI_FORMAT_R8_UNORM;
        SRVdesc.ViewDimension = D3D10_SRV_DIMENSION_TEXTURE2D;
        SRVdesc.Texture2D.MipLevels = 1;
        SRVdesc.Texture2D.MostDetailedMip = 0;
        V_RETURN( pd3dDevice->CreateShaderResourceView( g_pNoiseTexture, &SRVdesc, &g_pNoiseTextureSRV ) );*/

        desc.width = width;
        desc.height = height;
        desc.format = GLenum.GL_RGBA16F;
        /*desc.MipLevels = 1;
        desc.ArraySize = 1;
        desc.Usage = D3D10_USAGE_DEFAULT;
        desc.BindFlags = D3D10_BIND_SHADER_RESOURCE | D3D10_BIND_RENDER_TARGET;
        desc.MiscFlags = 0;
        desc.CPUAccessFlags = 0;
        desc.SampleDesc.Count = 1;
        desc.SampleDesc.Quality = 0;
        V_RETURN( pd3dDevice->CreateTexture2D( &desc, NULL, &g_pHDRTexture ) );*/
        g_pHDRTexture = TextureUtils.createTexture2D(desc, null);

        desc.format = GLenum.GL_RG16F;
        desc.width /= g_ScaleRatio;
        desc.height /= g_ScaleRatio;
//        V_RETURN( pd3dDevice->CreateTexture2D( &desc, NULL, &g_pEdgeTextureFS ) );
        g_pEdgeTextureFS= TextureUtils.createTexture2D(desc, null);

        /*SRVdesc.Format = DXGI_FORMAT_R16G16B16A16_FLOAT;
        SRVdesc.ViewDimension = D3D10_SRV_DIMENSION_TEXTURE2D;
        SRVdesc.Texture2D.MipLevels = 1;
        SRVdesc.Texture2D.MostDetailedMip = 0;
        V_RETURN( pd3dDevice->CreateShaderResourceView( g_pHDRTexture, &SRVdesc, &g_pHDRTextureSRV ) );

        SRVdesc.Format = DXGI_FORMAT_R16G16_FLOAT;
        V_RETURN( pd3dDevice->CreateShaderResourceView( g_pEdgeTextureFS, &SRVdesc, &g_pEdgeTextureFSSRV ) );

        D3D10_RENDER_TARGET_VIEW_DESC RTVdesc;
        RTVdesc.Format = DXGI_FORMAT_R16G16B16A16_FLOAT;
        RTVdesc.ViewDimension = D3D10_RTV_DIMENSION_TEXTURE2D;
        RTVdesc.Texture2D.MipSlice = 0;
        V_RETURN( pd3dDevice->CreateRenderTargetView( g_pHDRTexture, &RTVdesc, &g_pHDRTextureRTV ) );

        RTVdesc.Format = DXGI_FORMAT_R16G16_FLOAT;
        V_RETURN( pd3dDevice->CreateRenderTargetView( g_pEdgeTextureFS, &RTVdesc, &g_pEdgeTextureFSRTV ) );*/

        CreateScaledTextures( /*pd3dDevice*/ );

        int nSampleLen = 1;
        for( int i=0; i < NUM_TONEMAP_TEXTURES; i++ )
        {
            Texture2DDesc tmdesc = new Texture2DDesc(nSampleLen, nSampleLen, GLenum.GL_R16F);
//            ZeroMemory( &tmdesc, sizeof( D3D10_TEXTURE2D_DESC ) );
            /*tmdesc.ArraySize = 1;
            tmdesc.BindFlags = D3D10_BIND_RENDER_TARGET | D3D10_BIND_SHADER_RESOURCE;
            tmdesc.Usage = D3D10_USAGE_DEFAULT;
            tmdesc.Format = DXGI_FORMAT_R16_FLOAT;
            tmdesc.Width = nSampleLen;
            tmdesc.Height = nSampleLen;
            tmdesc.MipLevels = 1;
            tmdesc.SampleDesc.Count = 1;*/

//            V_RETURN( pd3dDevice->CreateTexture2D( &tmdesc, NULL, &g_apTexToneMap10[i] ) );
            g_apTexToneMap10[i] = TextureUtils.createTexture2D(tmdesc, null);

            /*D3D10_RENDER_TARGET_VIEW_DESC DescRT;
            DescRT.Format = tmdesc.Format;
            DescRT.ViewDimension = D3D10_RTV_DIMENSION_TEXTURE2D;
            DescRT.Texture2D.MipSlice = 0;
            V_RETURN(pd3dDevice->CreateRenderTargetView( g_apTexToneMap10[i], &DescRT, &g_apTexToneMapRTV10[i] ));

            D3D10_SHADER_RESOURCE_VIEW_DESC DescRV;
            DescRV.Format = tmdesc.Format;
            DescRV.ViewDimension = D3D10_SRV_DIMENSION_TEXTURE2D;
            DescRV.Texture2D.MipLevels = 1;
            DescRV.Texture2D.MostDetailedMip = 0;
            V_RETURN(pd3dDevice->CreateShaderResourceView( g_apTexToneMap10[i], &DescRV, &g_apTexToneMapRV10[i] ));*/

            nSampleLen *= 3;
        }

        Texture2DDesc Desc = new Texture2DDesc(width, height, GLenum.GL_RGBA16F);
        /*ZeroMemory( &Desc, sizeof( D3D10_TEXTURE2D_DESC ) );
        Desc.ArraySize = 1;
        Desc.BindFlags = D3D10_BIND_RENDER_TARGET | D3D10_BIND_SHADER_RESOURCE;
        Desc.Usage = D3D10_USAGE_DEFAULT;
        Desc.Format = DXGI_FORMAT_R16G16B16A16_FLOAT;
        Desc.Width = pBackBufferSurfaceDesc->Width;
        Desc.Height = pBackBufferSurfaceDesc->Height;
        Desc.MipLevels = 1;
        Desc.SampleDesc.Count = 1;*/

        // Create the bright pass texture
        Desc.width /= 8;
        Desc.height /= 8;
        Desc.format = GLenum.GL_RGBA8;
//        V_RETURN( pd3dDevice->CreateTexture2D( &Desc, NULL, &g_pTexBrightPass10 ) );
        g_pTexBrightPass10 = TextureUtils.createTexture2D(Desc, null);

        /*D3D10_RENDER_TARGET_VIEW_DESC DescRT;
        DescRT.Format = Desc.Format;
        DescRT.ViewDimension = D3D10_RTV_DIMENSION_TEXTURE2D;
        DescRT.Texture2D.MipSlice = 0;
        V_RETURN(pd3dDevice->CreateRenderTargetView( g_pTexBrightPass10, &DescRT, &g_pTexBrightPassRTV10 ));

        D3D10_SHADER_RESOURCE_VIEW_DESC DescRV;
        DescRV.Format = Desc.Format;
        DescRV.ViewDimension = D3D10_SRV_DIMENSION_TEXTURE2D;
        DescRV.Texture2D.MipLevels = 1;
        DescRV.Texture2D.MostDetailedMip = 0;
        V_RETURN(pd3dDevice->CreateShaderResourceView( g_pTexBrightPass10, &DescRV, &g_pTexBrightPassRV10 ));*/

        // Create the temporary blooming effect textures
        for( int i=0; i < NUM_BLOOM_TEXTURES; i++ )
        {
            /*D3D10_TEXTURE2D_DESC bmdesc;
            ZeroMemory( &bmdesc, sizeof( D3D10_TEXTURE2D_DESC ) );
            bmdesc.ArraySize = 1;
            bmdesc.BindFlags = D3D10_BIND_RENDER_TARGET | D3D10_BIND_SHADER_RESOURCE;
            bmdesc.Usage = D3D10_USAGE_DEFAULT;
            bmdesc.Format = DXGI_FORMAT_R8G8B8A8_UNORM;
            bmdesc.Width = pBackBufferSurfaceDesc->Width / 8;
            bmdesc.Height = pBackBufferSurfaceDesc->Height / 8;
            bmdesc.MipLevels = 1;
            bmdesc.SampleDesc.Count = 1;

            V_RETURN( pd3dDevice->CreateTexture2D( &bmdesc, NULL, &g_apTexBloom10[i] ) );

            // Create the render target view
            D3D10_RENDER_TARGET_VIEW_DESC DescRT;
            DescRT.Format = bmdesc.Format;
            DescRT.ViewDimension = D3D10_RTV_DIMENSION_TEXTURE2D;
            DescRT.Texture2D.MipSlice = 0;
            V_RETURN(pd3dDevice->CreateRenderTargetView( g_apTexBloom10[i], &DescRT, &g_apTexBloomRTV10[i] ));

            // Create the shader resource view
            D3D10_SHADER_RESOURCE_VIEW_DESC DescRV;
            DescRV.Format = bmdesc.Format;
            DescRV.ViewDimension = D3D10_SRV_DIMENSION_TEXTURE2D;
            DescRV.Texture2D.MipLevels = 1;
            DescRV.Texture2D.MostDetailedMip = 0;
            V_RETURN(pd3dDevice->CreateShaderResourceView( g_apTexBloom10[i], &DescRV, &g_apTexBloomRV10[i] ));*/
        }
    }


    @Override
    public void dispose() {

    }

    void CreateScaledTextures(){
        ReleaseScaledTextures();

        Texture2DDesc desc = new Texture2DDesc();
//        g_pHDRTexture->GetDesc( &desc );
        g_pHDRTexture.getDesc(desc);
        desc.format = GLenum.GL_R16F;
        desc.width /= g_ScaleRatio;
        desc.height /= g_ScaleRatio;
        g_pHDRTextureScaled = TextureUtils.createTexture2D(desc, null);
        g_pHDRTextureScaled2 = TextureUtils.createTexture2D(desc, null);
        g_pHDRTextureScaled3 = TextureUtils.createTexture2D(desc, null);

        /*D3D10_SHADER_RESOURCE_VIEW_DESC SRVdesc;
        SRVdesc.Format = DXGI_FORMAT_R16_FLOAT;
        SRVdesc.ViewDimension = D3D10_SRV_DIMENSION_TEXTURE2D;
        SRVdesc.Texture2D.MipLevels = 1;
        SRVdesc.Texture2D.MostDetailedMip = 0;
        V_RETURN( pd3dDevice->CreateShaderResourceView( g_pHDRTextureScaled, &SRVdesc, &g_pHDRTextureScaledSRV ) );
        V_RETURN( pd3dDevice->CreateShaderResourceView( g_pHDRTextureScaled2, &SRVdesc, &g_pHDRTextureScaled2SRV ) );
        V_RETURN( pd3dDevice->CreateShaderResourceView( g_pHDRTextureScaled3, &SRVdesc, &g_pHDRTextureScaled3SRV ) );

        D3D10_RENDER_TARGET_VIEW_DESC RTVdesc;
        RTVdesc.Format = DXGI_FORMAT_R16_FLOAT;
        RTVdesc.ViewDimension = D3D10_RTV_DIMENSION_TEXTURE2D;
        RTVdesc.Texture2D.MipSlice = 0;
        V_RETURN( pd3dDevice->CreateRenderTargetView( g_pHDRTextureScaled, &RTVdesc, &g_pHDRTextureScaledRTV ) );
        V_RETURN( pd3dDevice->CreateRenderTargetView( g_pHDRTextureScaled2, &RTVdesc, &g_pHDRTextureScaled2RTV ) );
        V_RETURN( pd3dDevice->CreateRenderTargetView( g_pHDRTextureScaled3, &RTVdesc, &g_pHDRTextureScaled3RTV ) );*/
    }

    void DeleteDepthBufferTexture()
    {
        SAFE_RELEASE( g_pDepthBufferTexture );
//        SAFE_RELEASE( g_pDepthBufferTextureSRV );
//        SAFE_RELEASE( g_pDepthBufferTextureDSV );

        SAFE_RELEASE( g_pDepthBufferTextureWS );
//        SAFE_RELEASE( g_pDepthBufferTextureWSSRV );
//        SAFE_RELEASE( g_pDepthBufferTextureWSRTV );
    }

    void ReleaseResolutionDependentResources()
    {
        ReleaseScaledTextures();

        SAFE_RELEASE( g_pNoiseTexture );
//        SAFE_RELEASE( g_pNoiseTextureSRV );

        SAFE_RELEASE( g_pHDRTexture );
//        SAFE_RELEASE( g_pHDRTextureSRV );
//        SAFE_RELEASE( g_pHDRTextureRTV );

        SAFE_RELEASE( g_pEdgeTextureFS );
//        SAFE_RELEASE( g_pEdgeTextureFSSRV );
//        SAFE_RELEASE( g_pEdgeTextureFSRTV );

        for( int i = 0; i < NUM_TONEMAP_TEXTURES; i++ )
        {
            SAFE_RELEASE( g_apTexToneMap10[i] );
//            SAFE_RELEASE( g_apTexToneMapRTV10[i] );
//            SAFE_RELEASE( g_apTexToneMapRV10[i] );
        }

        for( int i = 0; i < NUM_BLOOM_TEXTURES; i++ )
        {
//            SAFE_RELEASE( g_apTexBloom10[i] );
//            SAFE_RELEASE( g_apTexBloomRTV10[i] );
//            SAFE_RELEASE( g_apTexBloomRV10[i] );
        }

        SAFE_RELEASE( g_pTexBrightPass10 );
//        SAFE_RELEASE( g_pTexBrightPassRV10 );
//        SAFE_RELEASE( g_pTexBrightPassRTV10 );
    }

    void ReleaseScaledTextures()
    {
        SAFE_RELEASE( g_pHDRTextureScaled );
//        SAFE_RELEASE( g_pHDRTextureScaledSRV );
//        SAFE_RELEASE( g_pHDRTextureScaledRTV );

        SAFE_RELEASE( g_pHDRTextureScaled2 );
//        SAFE_RELEASE( g_pHDRTextureScaled2SRV );
//        SAFE_RELEASE( g_pHDRTextureScaled2RTV );

        SAFE_RELEASE( g_pHDRTextureScaled3 );
//        SAFE_RELEASE( g_pHDRTextureScaled3SRV );
//        SAFE_RELEASE( g_pHDRTextureScaled3RTV );
    }
}
