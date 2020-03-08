package jet.opengl.demos.gpupro.culling;

import java.io.IOException;

import jet.opengl.postprocessing.buffer.BufferGL;
import jet.opengl.postprocessing.common.GLenum;
import jet.opengl.postprocessing.shader.GLSLProgram;
import jet.opengl.postprocessing.shader.Macro;
import jet.opengl.postprocessing.texture.RenderTargets;
import jet.opengl.postprocessing.texture.SamplerDesc;
import jet.opengl.postprocessing.texture.SamplerUtils;
import jet.opengl.postprocessing.texture.Texture2D;
import jet.opengl.postprocessing.texture.Texture2DDesc;
import jet.opengl.postprocessing.texture.TextureGL;
import jet.opengl.postprocessing.texture.TextureUtils;

final class RasterOrderOITRenderer extends TransparencyRenderer{
    private static final int MAX_SHADER_VARIATIONS = 6;
    private GLSLProgram[] m_pAOITSPResolvePS = new GLSLProgram[MAX_SHADER_VARIATIONS];
    private GLSLProgram[] m_pAOITSPClearPS = new GLSLProgram[MAX_SHADER_VARIATIONS];
    private GLSLProgram m_ROIRecord;

    private BufferGL[]          mAOITSPColorData = new BufferGL[MAX_SHADER_VARIATIONS];
    private BufferGL[]			mAOITSPDepthData = new BufferGL[MAX_SHADER_VARIATIONS];
    private BufferGL			mFragmentListNodes;
    private Texture2D           mFragmentListFirstNodeOffset;
    private boolean             mATSPClearMaskInitialized;

    private Texture2D           mATSPClearMask;

    private Runnable mAOITCompositeBlendState;
    private Runnable mAOITCompositeDepthStencilState;

    private BufferGL mpFragmentListFirstNodeOffseBuffer;
    private BufferGL mFragmentListNodesBuffer;
    private BufferGL mpAOITSPDepthDataUAVBuffer;
    private BufferGL mpAOITSPColorDataUAVBuffer;
    private BufferGL mp8AOITSPDepthDataUAVBuffer;
    private BufferGL mp8AOITSPColorDataUAVBuffer;
    private BufferGL mpIntelExt;
    private BufferGL mFragmentListConstants;
    private BufferGL mpATSPClearMaskBuffer;
    private BufferGL m_pConstBuffer;		// Buffer constants (dimensions and miplevels) for compute shaders

    private Texture2D mpClearMaskRT;
    private RenderTargets mFBO;

    private int m_pPointSampler;
    private int mLisTexNodeCount = 1 << 22;

    private RenderInput mInput = new RenderInput();
    private RenderOutput mOutput = new RenderOutput();

    @Override
    protected void onCreate() {
        super.onCreate();

        mFBO = new RenderTargets();
        final int MAX_DEFINES = 3; // Note: use MAX_DEFINES to avoid dynamic allocation.  Arbitrarily choose 3.  Not sure if there is a real limit.
        Macro[] pFinalShaderMacros = new Macro[MAX_DEFINES];

        Macro AOITMacro2 = new Macro( "AOIT_NODE_COUNT", "2" );
        Macro AOITMacro4 = new Macro( "AOIT_NODE_COUNT", "4" );
        Macro AOITMacro8 = new Macro( "AOIT_NODE_COUNT", "8" );
        Macro AOITHDRMacro = new Macro( "dohdr", "1");

//        cString ExecutableDirectory;
//        CPUTFileSystem::GetExecutableDirectory(&ExecutableDirectory);

        final String shaderPath = "Intel/OIT/shaders/";

        final String[] names = {
                "Node2", "Node4", "Node8", "Node2_HDR", "Node4_HDR", "Node8_HDR"
        };

        for (int i = 0; i < MAX_SHADER_VARIATIONS; ++i)
        {
            switch(i)
            {
                case 0:		pFinalShaderMacros[0]   = AOITMacro2; pFinalShaderMacros[1] = null; break;
                case 1:		pFinalShaderMacros[0]   = AOITMacro4; pFinalShaderMacros[1] = null; break;
                case 2:		pFinalShaderMacros[0]   = AOITMacro8; pFinalShaderMacros[1] = null; break;
                case 3:		pFinalShaderMacros[0] = AOITMacro2; pFinalShaderMacros[1] = AOITHDRMacro;  pFinalShaderMacros[2] = null; break;
                case 4:		pFinalShaderMacros[0] = AOITMacro4; pFinalShaderMacros[1] = AOITHDRMacro; pFinalShaderMacros[2] = null; break;
                case 5:		pFinalShaderMacros[0] = AOITMacro8; pFinalShaderMacros[1] = AOITHDRMacro; pFinalShaderMacros[2] = null; break;
            }

            try {
                m_pAOITSPResolvePS[i] = GLSLProgram.createFromFiles("shader_libs/PostProcessingDefaultScreenSpaceVS.vert",
                        shaderPath + "AOIT_ResolvePS.frag", pFinalShaderMacros);
                m_pAOITSPResolvePS[i].setName("AOIT_ResolvePS_"+names[i]);
                m_pAOITSPClearPS[i] = GLSLProgram.createFromFiles("shader_libs/PostProcessingDefaultScreenSpaceVS.vert",
                        shaderPath + "AOIT_ClearPS.frag", pFinalShaderMacros);
                m_pAOITSPClearPS[i].setName("AOIT_ClearPS_"+names[i]);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        final String root = "gpupro/Culling/shaders/";
        m_ROIRecord = GLSLProgram.createProgram(root + "ShadingVS.vert", root + "ROTShadingPS.frag", null);
        m_ROIRecord.setName("ROTShadingPS");

        m_pConstBuffer = new BufferGL();
        m_pConstBuffer.initlize(GLenum.GL_UNIFORM_BUFFER, 16, null, GLenum.GL_DYNAMIC_DRAW);

        SamplerDesc samplerDesc = new SamplerDesc();
        samplerDesc.wrapR = GLenum.GL_CLAMP_TO_EDGE;
        samplerDesc.wrapS = GLenum.GL_CLAMP_TO_EDGE;
        samplerDesc.wrapT = GLenum.GL_CLAMP_TO_EDGE;
        samplerDesc.magFilter = GLenum.GL_NEAREST;
        samplerDesc.minFilter = GLenum.GL_NEAREST_MIPMAP_NEAREST;  // todo mipmap
        samplerDesc.anisotropic = 16;
        m_pPointSampler = SamplerUtils.createSampler(samplerDesc);

        // Create OIT blend state
        {
            /*CD3D11_BLEND_DESC desc(D3D11_DEFAULT);
            desc.RenderTarget[0].BlendEnable = true;
            desc.RenderTarget[0].SrcBlend = D3D11_BLEND_ONE;
            desc.RenderTarget[0].DestBlend = D3D11_BLEND_SRC_ALPHA;
            desc.RenderTarget[0].BlendOp = D3D11_BLEND_OP_ADD;
            pD3DDevice->CreateBlendState(&desc, &mAOITCompositeBlendState);*/
            mAOITCompositeBlendState = new Runnable() {
                @Override
                public void run() {
                    gl.glEnable(GLenum.GL_BLEND);
                    gl.glBlendFunc(GLenum.GL_ONE, GLenum.GL_ONE_MINUS_SRC_ALPHA);
                }
            };
        }

        // Create AOIT depth stencil desc
        {
            /*D3D11_DEPTH_STENCIL_DESC DSDesc;
            DSDesc.DepthEnable                  = FALSE;
            DSDesc.DepthFunc                    = D3D11_COMPARISON_GREATER;
            DSDesc.DepthWriteMask               = D3D11_DEPTH_WRITE_MASK_ZERO;
            DSDesc.StencilEnable                = TRUE;
            DSDesc.StencilReadMask              = 0xFF;
            DSDesc.StencilWriteMask             = 0x00;
            DSDesc.FrontFace.StencilFailOp      = D3D11_STENCIL_OP_KEEP;
            DSDesc.FrontFace.StencilDepthFailOp = D3D11_STENCIL_OP_KEEP;
            DSDesc.FrontFace.StencilPassOp      = D3D11_STENCIL_OP_KEEP;
            DSDesc.FrontFace.StencilFunc        = D3D11_COMPARISON_EQUAL;
            DSDesc.BackFace.StencilFailOp       = D3D11_STENCIL_OP_KEEP;
            DSDesc.BackFace.StencilDepthFailOp  = D3D11_STENCIL_OP_KEEP;
            DSDesc.BackFace.StencilPassOp       = D3D11_STENCIL_OP_KEEP;
            DSDesc.BackFace.StencilFunc         = D3D11_COMPARISON_EQUAL;
            hr = pD3DDevice->CreateDepthStencilState(&DSDesc, &mAOITCompositeDepthStencilState );*/
            mAOITCompositeDepthStencilState = new Runnable() {
                @Override
                public void run() {
                    gl.glDisable(GLenum.GL_DEPTH_TEST);
                    gl.glEnable(GLenum.GL_STENCIL_TEST);
                    gl.glStencilFunc(GLenum.GL_EQUAL, 0x01, 0xFF);
                    gl.glStencilOp(GLenum.GL_KEEP,GLenum.GL_KEEP,GLenum.GL_KEEP);
                    gl.glStencilMask(0x00);
                }
            };
        }

        /*D3D11_BUFFER_DESC bd = {0};
        bd.ByteWidth = sizeof(FL_Constants);
        bd.BindFlags = D3D11_BIND_CONSTANT_BUFFER;
        bd.Usage = D3D11_USAGE_DYNAMIC;
        bd.CPUAccessFlags = D3D11_CPU_ACCESS_WRITE;

        ID3D11Buffer * pOurConstants;
        hr = (CPUT_DX11::GetDevice())->CreateBuffer( &bd, NULL, &pOurConstants );*/
        mFragmentListConstants = new BufferGL();
        mFragmentListConstants.initlize(GLenum.GL_UNIFORM_BUFFER, 16, null, GLenum.GL_DYNAMIC_READ);


//        final int DXGI_FORMAT_R32_UINT = GLenum.GL_R32UI;
//        mpClearMaskRT = (CPUTTextureDX11)CPUTTextureDX11.CreateTexture(("$ClearMaskRT"), DXGI_FORMAT_R32_UINT, width, height, DXGI_FORMAT_R32_UINT, 0/*D3D11_BIND_RENDER_TARGET |D3D11_BIND_UNORDERED_ACCESS*/, 1);
//        mpClearMaskRT.AddUAVView(DXGI_FORMAT_R32_UINT);
    }

    private void Resolve(/*ID3D11DeviceContext* pD3DImmediateContext,*/  Texture2D pOutput, Texture2D  pDSV,int NodeIndex){
//        pD3DImmediateContext->OMSetRenderTargets(1, &pOutput, pDSV);
        TextureGL[] RTVs = {pOutput, pDSV};
        mFBO.bind();
        mFBO.setRenderTextures(RTVs, null);

//        ID3D11ShaderResourceView* pAOITClearMaskSRV[] = {  mpClearMaskRT->GetShaderResourceView() };

        TextureGL  pAOITClearMaskSRV = mpClearMaskRT;
        int bindIndex = -1;
        int bindIndex2 = -1;
        int clearBindIndex = -1;


        if((NodeIndex==2) || (NodeIndex == 5)) // 8 node version for normal and HDR
        {
            BufferGL pSRVs = mp8AOITSPColorDataUAVBuffer;
            /*if (GetBindIndex(m_pAOITSPResolvePSReflection[NodeIndex], "g8AOITSPColorDataSRV", &bindIndex) == S_OK) {
                pD3DImmediateContext->PSSetShaderResources(bindIndex,1,  pSRVs);  TODO
            }*/

            if (NodeIndex == 5)
            {
                BufferGL pDSRVs = mp8AOITSPDepthDataUAVBuffer;
                /*if (GetBindIndex(m_pAOITSPResolvePSReflection[NodeIndex], "g8AOITSPDepthDataSRV", &bindIndex2) == S_OK) {
                    pD3DImmediateContext->PSSetShaderResources(bindIndex2, 1, pDSRVs);  TODO
                }*/
            }
        }else
        {
            BufferGL pSRVs = mpAOITSPColorDataUAVBuffer;
            /*if (GetBindIndex(m_pAOITSPResolvePSReflection[NodeIndex], "gAOITSPColorDataSRV", &bindIndex) == S_OK) {
                pD3DImmediateContext->PSSetShaderResources(bindIndex,1,  pSRVs);  TODO
            }*/
            if (NodeIndex == 4)
            {
                BufferGL pDSRVs = mpAOITSPDepthDataUAVBuffer;
                /*if (GetBindIndex(m_pAOITSPResolvePSReflection[NodeIndex], "gAOITSPDepthDataSRV", &bindIndex2) == S_OK) {
                    pD3DImmediateContext->PSSetShaderResources(bindIndex2, 1, pDSRVs);  TODO
                }*/
            }

        }

        /*if (GetBindIndex(m_pAOITSPResolvePSReflection[NodeIndex], "gAOITSPClearMaskSRV", &clearBindIndex) == S_OK) {
            pD3DImmediateContext->PSSetShaderResources(clearBindIndex,1,  &pAOITClearMaskSRV[0]);  TODO
        }*/

        m_pAOITSPResolvePS[NodeIndex].enable();

//        pD3DImmediateContext->OMSetBlendState(mAOITCompositeBlendState, 0, 0xffffffff);
        mAOITCompositeBlendState.run();

        /*ID3D11DepthStencilState * pBackupState = NULL; UINT backupStencilRef = 0;
        pD3DImmediateContext->OMGetDepthStencilState( &pBackupState, &backupStencilRef );
        pD3DImmediateContext->OMSetDepthStencilState( mAOITCompositeDepthStencilState, 0x01 );*/
        mAOITCompositeDepthStencilState.run();

        DrawFullScreenQuad();

        /*pD3DImmediateContext->OMSetDepthStencilState( pBackupState, backupStencilRef );
        SAFE_RELEASE( pBackupState );*/
        gl.glDisable(GLenum.GL_STENCIL_TEST);
        gl.glDisable(GLenum.GL_BLEND);
        m_pAOITSPResolvePS[NodeIndex].printPrograminfo();
    }

    @Override
    protected void onResize(int width, int height) {
        super.onResize(width, height);

        ReleaseResources();


        mAOITSPDepthData[0].initlize(GLenum.GL_SHADER_STORAGE_BUFFER, width*height*4 * 4, null, GLenum.GL_DYNAMIC_READ );
        mAOITSPColorData[0].initlize(GLenum.GL_SHADER_STORAGE_BUFFER,width*height*4 * 4, null, GLenum.GL_DYNAMIC_READ );
        mAOITSPDepthData[1].initlize(GLenum.GL_SHADER_STORAGE_BUFFER,width*height*4 * 8, null, GLenum.GL_DYNAMIC_READ  );
        mAOITSPColorData[1].initlize(GLenum.GL_SHADER_STORAGE_BUFFER,width*height*4 * 8, null, GLenum.GL_DYNAMIC_READ );


//        UINT bindFlags = D3D11_BIND_SHADER_RESOURCE | D3D11_BIND_RENDER_TARGET;

        int structSize = /*sizeof(FragmentNode)*/12;
        final int nodeCount = mLisTexNodeCount * 2;
        mFragmentListNodes.initlize(GLenum.GL_SHADER_STORAGE_BUFFER, 1*nodeCount* structSize,null, GLenum.GL_DYNAMIC_READ);


        mATSPClearMaskInitialized = false;
//        mFragmentListFirstNodeOffset.initlize(pD3DDevice, width, height, DXGI_FORMAT_R32_UINT, bindFlags | D3D11_BIND_UNORDERED_ACCESS, 1);
        SAFE_RELEASE(mFragmentListFirstNodeOffset);

        Texture2DDesc desc = new Texture2DDesc(width, height, GLenum.GL_R32UI);
        mFragmentListFirstNodeOffset = TextureUtils.createTexture2D(desc, null);
    }

    private void ReleaseResources(){
//create float render target
//        SAFE_RELEASE(m_pDSView);
//        SAFE_RELEASE(m_pSwapChainRTV);

        for (int i = 0; i < 2; ++i)
        {
            SAFE_RELEASE(mAOITSPDepthData[i]);
            SAFE_RELEASE(mAOITSPColorData[i]);
        }

        SAFE_RELEASE(mFragmentListNodes);
        SAFE_RELEASE(mFragmentListFirstNodeOffset);
    }

    /*
        DrawFullScreenQuad
        Helper functions for drawing a full screen quad (used for the final tonemapping and bloom composite.
        Renders the quad with and passes vertex position and texture coordinates to current pixel shader
    */
    private void DrawFullScreenQuad(){
        gl.glBindVertexArray(0);
        gl.glBindBuffer(GLenum.GL_ARRAY_BUFFER, 0);
        gl.glBindBuffer(GLenum.GL_ELEMENT_ARRAY_BUFFER_ARB, 0);

        gl.glDrawArrays(GLenum.GL_TRIANGLES, 0, 3);
    }

    @Override
    final OITType getType() {
        return OITType.ROV;
    }

    @Override
    void renderScene(Renderer sceneRender,Scene scene) {
        // todo parepare for the rendering

        m_ROIRecord.enable();

        mInput.clearFBO = false;
        mInput.transparencyRenderProg = m_ROIRecord;
        mInput.writeFBO = false;
        sceneRender.renderTransparency(scene, mInput, mOutput);
        m_ROIRecord.printPrograminfo();

        int TwoNodeIndex = 0;
        Resolve(sceneRender.mColorBuffer, sceneRender.mDepthBuffer, TwoNodeIndex);
    }

    @Override
    public void dispose() {

    }
}
