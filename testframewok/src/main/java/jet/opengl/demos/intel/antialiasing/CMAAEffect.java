package jet.opengl.demos.intel.antialiasing;

import org.lwjgl.util.vector.Readable;
import org.lwjgl.util.vector.Vector4f;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import jet.opengl.postprocessing.buffer.BufferGL;
import jet.opengl.postprocessing.common.Disposeable;
import jet.opengl.postprocessing.common.GLFuncProvider;
import jet.opengl.postprocessing.common.GLFuncProviderFactory;
import jet.opengl.postprocessing.common.GLenum;
import jet.opengl.postprocessing.shader.GLSLProgram;
import jet.opengl.postprocessing.shader.Macro;
import jet.opengl.postprocessing.texture.RenderTargets;
import jet.opengl.postprocessing.texture.Texture2D;
import jet.opengl.postprocessing.texture.Texture2DDesc;
import jet.opengl.postprocessing.texture.TextureGL;
import jet.opengl.postprocessing.texture.TextureUtils;
import jet.opengl.postprocessing.util.CacheBuffer;
import jet.opengl.postprocessing.util.CommonUtil;
import jet.opengl.postprocessing.util.Numeric;

/**
 * Created by mazhen'gui on 2017/9/23.
 */

final class CMAAEffect implements Disposeable{
    public static final int DT_Normal                     = 0;
    public static final int DT_ShowEdges                  = 1;

    private GLSLProgram m_edges0PS;
    private GLSLProgram                 m_edges1PS;
    private GLSLProgram                m_edgesCombinePS;
    private GLSLProgram                 m_processAndApplyPS;

    private GLSLProgram                 m_dbgDisplayEdgesPS;
    private GLSLProgram                 m_dbgProcessAndApplyPS;

    private BufferGL m_constantsBuffer;

    private Texture2D m_depthStencilTex;
    private Texture2D            m_depthStencilTexDSV;
    private Texture2D            m_depthStencilTexReadOnlyDSV;
    private Texture2D          m_depthStencilTexSRV;

    private Texture2D                   m_edgesTexture;
    private Texture2D          m_edgesTextureSRV;
    private Texture2D         m_edgesTextureUAV;
    private Texture2D                   m_edgesTexture2;
    private Texture2D          m_edgesTexture2SRV;
    private Texture2D         m_edgesTexture2UAV;

    private Texture2D                   m_mini4edgeTexture;
    private Texture2D            m_mini4edgeTextureUintRTV;
    private Texture2D          m_mini4edgeTextureUintSRV;

    private Texture2D                   m_workingColorTexture;
    private Texture2D          m_workingColorTextureSRV;
    private Texture2D         m_workingColorTextureUAV;

    private Runnable m_renderAlwaysDSS;
    private Runnable           m_renderCreateMaskDSS;
    private Runnable           m_renderUseMaskDSS;

    private int                                 m_frameID;
    private float								m_Width;
    private float								m_Height;

    private boolean                                m_isInGammaCorrectMode;
    private GLFuncProvider gl;
    private RenderTargets m_renderTargets;
    private final CMAAConstants     m_Constants = new CMAAConstants();
    private boolean m_prinOnce = false;

//    ID3D11VertexShader*                 m_pFullScreenQuadVS;

    CMAAEffect(){
        // Set this to false if the input/output isn't a SRGB DXGI format
        m_isInGammaCorrectMode      = false;
    }

    private static GLSLProgram createProgram(String fragShaderName, Macro[] macros){
        try {
            GLSLProgram program =  GLSLProgram.createFromFiles("shader_libs/PostProcessingDefaultScreenSpaceVS.vert",
                    String.format("shader_libs/AntiAliasing/PostProcessingCMAA_%s.frag", fragShaderName), macros);
            program.setName(fragShaderName);
            return program;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    void			OnCreate(/*ID3D11Device* pD3dDevice, ID3D11DeviceContext* pContext, IDXGISwapChain* pSwapChain*/){
        gl = GLFuncProviderFactory.getGLFuncProvider();
        m_renderTargets = new RenderTargets();
//        HRESULT hr = S_OK;
//        ID3DBlob* pShaderBlob;
//        CPUTResult result;
//        cString FullPath, FinalPath;

//        CPUTAssetLibraryDX11 *pAssetLibrary = (CPUTAssetLibraryDX11*)CPUTAssetLibrary::GetAssetLibrary();
//        cString OriginalAssetLibraryDirectory = pAssetLibrary->GetShaderDirectoryName();


        // Create constants buffer
        {
            /*D3D11_BUFFER_DESC BufferDesc;
            ZeroMemory(&BufferDesc, sizeof(BufferDesc));
            BufferDesc.Usage          = D3D11_USAGE_DYNAMIC;
            BufferDesc.BindFlags      = D3D11_BIND_CONSTANT_BUFFER;
            BufferDesc.CPUAccessFlags = D3D11_CPU_ACCESS_WRITE;
            BufferDesc.ByteWidth      = sizeof( CMAAConstants );
            hr = pD3DDevice->CreateBuffer( &BufferDesc, NULL, &m_constantsBuffer );*/
            m_constantsBuffer = new BufferGL();
            m_constantsBuffer.initlize(GLenum.GL_UNIFORM_BUFFER, CMAAConstants.SIZE, null, GLenum.GL_DYNAMIC_COPY);
            m_constantsBuffer.unbind();
        }

        /*int macroCount = 0;
        D3D_SHADER_MACRO macros[16] = { NULL };

#if defined(DEBUG) || defined(_DEBUG)
        macros[macroCount].Name       = "_DEBUG";
        macros[macroCount].Definition = "";
        macroCount++;
#endif

        if( m_isInGammaCorrectMode )
        {
            macros[macroCount].Name       = "IN_GAMMA_CORRECT_MODE";
            macros[macroCount].Definition = "";
            macroCount++;
        }

        macros[macroCount].Name       = NULL;
        macros[macroCount].Definition = NULL;*/
        Macro[] macros = {
                new Macro("_DEBUG", ""),
                m_isInGammaCorrectMode ? new Macro("IN_GAMMA_CORRECT_MODE", ""): null,
                null
        };

        // Load the shaders
        /*FullPath = OriginalAssetLibraryDirectory + L"CMAA.hlsl";
        CPUTFileSystem::ResolveAbsolutePathAndFilename(FullPath, &FinalPath);*/

        /*result = pAssetLibrary->CompileShaderFromFile(FinalPath, L"DbgDisplayEdgesPS", L"ps_5_0", &pShaderBlob, macros );
        ASSERT( CPUTSUCCESS(result), _L("Error compiling Pixel shader:\n\n") );
        UNREFERENCED_PARAMETER(result);
        hr = pD3DDevice->CreatePixelShader( pShaderBlob->GetBufferPointer(), pShaderBlob->GetBufferSize(),NULL, &m_dbgDisplayEdgesPS);*/
        m_dbgDisplayEdgesPS =createProgram("DbgDisplayEdgesPS", macros);

        /*result = pAssetLibrary->CompileShaderFromFile(FinalPath, L"Edges0PS", L"ps_5_0", &pShaderBlob, macros );
        ASSERT( CPUTSUCCESS(result), _L("Error compiling Pixel shader:\n\n") );
        hr = pD3DDevice->CreatePixelShader( pShaderBlob->GetBufferPointer(), pShaderBlob->GetBufferSize(),NULL, &m_edges0PS);*/
        m_edges0PS = createProgram("Edges0PS", macros);

        /*result = pAssetLibrary->CompileShaderFromFile(FinalPath, L"Edges1PS", L"ps_5_0", &pShaderBlob, macros );
        ASSERT( CPUTSUCCESS(result), _L("Error compiling Pixel shader:\n\n") );
        hr = pD3DDevice->CreatePixelShader( pShaderBlob->GetBufferPointer(), pShaderBlob->GetBufferSize(),NULL, &m_edges1PS);*/
        m_edges1PS = createProgram("Edges1PS", macros);

        /*result = pAssetLibrary->CompileShaderFromFile(FinalPath, L"EdgesCombinePS", L"ps_5_0", &pShaderBlob, macros );
        ASSERT( CPUTSUCCESS(result), _L("Error compiling Pixel shader:\n\n") );
        hr = pD3DDevice->CreatePixelShader( pShaderBlob->GetBufferPointer(), pShaderBlob->GetBufferSize(),NULL, &m_edgesCombinePS);*/
        m_edgesCombinePS = createProgram("EdgesCombinePS", macros);

        /*result = pAssetLibrary->CompileShaderFromFile(FinalPath, L"ProcessAndApplyPS", L"ps_5_0", &pShaderBlob, macros );
        ASSERT( CPUTSUCCESS(result), _L("Error compiling Pixel shader:\n\n") );
        hr = pD3DDevice->CreatePixelShader( pShaderBlob->GetBufferPointer(), pShaderBlob->GetBufferSize(),NULL, &m_processAndApplyPS);*/
        m_processAndApplyPS = createProgram("ProcessAndApplyPS", macros);

        /*macros[macroCount].Name       = "DEBUG_OUTPUT_AAINFO";
        macros[macroCount].Definition = "";
        macroCount++;
        macros[macroCount].Name       = NULL;
        macros[macroCount].Definition = NULL;*/
        macros[macros.length - 1] = new Macro("DEBUG_OUTPUT_AAINFO", 1);

        /*result = pAssetLibrary->CompileShaderFromFile(FinalPath, L"ProcessAndApplyPS", L"ps_5_0", &pShaderBlob, macros );
        ASSERT( CPUTSUCCESS(result), _L("Error compiling Pixel shader:\n\n") );
        hr = pD3DDevice->CreatePixelShader( pShaderBlob->GetBufferPointer(), pShaderBlob->GetBufferSize(),NULL, &m_dbgProcessAndApplyPS);*/
        m_dbgProcessAndApplyPS = createProgram("ProcessAndApplyPS", macros);


        // Create depth-stencil states for masking out pixels to avoid.
        // Stencil based masking seems to be a wee-bit slower than depth buffer based one on Intel hardware, and also depth buffer takes
        // less memory, so use depth buffer only.
        {
            /*CD3D11_DEPTH_STENCIL_DESC desc = CD3D11_DEPTH_STENCIL_DESC( CD3D11_DEFAULT() );

            desc.DepthEnable     = FALSE;
            desc.DepthFunc       = D3D11_COMPARISON_ALWAYS;
            desc.StencilEnable   = FALSE;

            hr = pD3DDevice->CreateDepthStencilState( &desc, &m_renderAlwaysDSS );*/
            m_renderAlwaysDSS = ()->
            {
                gl.glDisable(GLenum.GL_DEPTH_TEST);
                gl.glDisable(GLenum.GL_STENCIL_TEST);
            };
        }

        {
            /*CD3D11_DEPTH_STENCIL_DESC desc = CD3D11_DEPTH_STENCIL_DESC( CD3D11_DEFAULT() );

            desc.DepthEnable     = FALSE;
            if( c_useDepthTest )
            {
                desc.DepthEnable     = TRUE;
                desc.DepthWriteMask  = D3D11_DEPTH_WRITE_MASK_ALL;
                desc.DepthFunc       = D3D11_COMPARISON_ALWAYS;
            }
            desc.StencilEnable      = FALSE;

            hr = pD3DDevice->CreateDepthStencilState( &desc, &m_renderCreateMaskDSS );*/
            m_renderCreateMaskDSS = ()->
            {
                gl.glEnable(GLenum.GL_DEPTH_TEST);
                gl.glDisable(GLenum.GL_STENCIL_TEST);
                gl.glDepthMask(true);
                gl.glDepthFunc(GLenum.GL_ALWAYS);
            };
        }

        {
            /*CD3D11_DEPTH_STENCIL_DESC desc = CD3D11_DEPTH_STENCIL_DESC( CD3D11_DEFAULT() );

            desc.DepthEnable     = FALSE;
            if( c_useDepthTest )
            {
                desc.DepthEnable     = TRUE;
                desc.DepthWriteMask  = D3D11_DEPTH_WRITE_MASK_ZERO;
                desc.DepthFunc       = D3D11_COMPARISON_LESS;
            }
            desc.StencilEnable      = FALSE;

            hr = pD3DDevice->CreateDepthStencilState( &desc, &m_renderUseMaskDSS );*/
            m_renderUseMaskDSS = ()->
            {
                gl.glEnable(GLenum.GL_DEPTH_TEST);
                gl.glDisable(GLenum.GL_STENCIL_TEST);
                gl.glDepthMask(false);
                gl.glDepthFunc(GLenum.GL_LESS);
            };
        }
    }

    void			OnShutdown(){
        ReleaseTextures();

        CommonUtil.safeRelease( m_constantsBuffer );
//        CommonUtil.safeRelease( m_renderAlwaysDSS );
//        CommonUtil.safeRelease( m_renderCreateMaskDSS );
//        CommonUtil.safeRelease( m_renderUseMaskDSS );

        CommonUtil.safeRelease( m_edges0PS );
        CommonUtil.safeRelease( m_edges1PS );
        CommonUtil.safeRelease( m_edgesCombinePS );
        CommonUtil.safeRelease( m_processAndApplyPS );

        CommonUtil.safeRelease( m_dbgProcessAndApplyPS );
        CommonUtil.safeRelease( m_dbgDisplayEdgesPS  );
        CommonUtil.safeRelease( m_renderTargets);
    }

    void				OnSize(/*ID3D11Device* pD3DDevice,*/ int width, int height){
        m_Width = (float)width;
        m_Height = (float)height;

        ReleaseTextures();

        //edges texture
        {
            /*D3D11_TEXTURE2D_DESC td;
            memset(&td, 0, sizeof(td));
            td.ArraySize = 1;
            td.Format = DXGI_FORMAT_R8_UNORM;
            td.Height = height;
            td.Width = width;
            td.CPUAccessFlags = 0;
            td.MipLevels = 1;
            td.MiscFlags = 0;
            td.SampleDesc.Count = 1;
            td.SampleDesc.Quality = 0;
            td.Usage = D3D11_USAGE_DEFAULT;
            td.BindFlags = D3D11_BIND_SHADER_RESOURCE | D3D11_BIND_UNORDERED_ACCESS; // | D3D11_BIND_RENDER_TARGET;
            hr = pD3DDevice->CreateTexture2D( &td, NULL, &m_edgesTexture ) ;
            hr = pD3DDevice->CreateTexture2D( &td, NULL, &m_edgesTexture2 ) ;
            {
                CD3D11_SHADER_RESOURCE_VIEW_DESC srvd( m_edgesTexture, D3D11_SRV_DIMENSION_TEXTURE2D, DXGI_FORMAT_R8_UNORM );
                hr =  pD3DDevice->CreateShaderResourceView( m_edgesTexture, &srvd, &m_edgesTextureSRV );
                hr =  pD3DDevice->CreateShaderResourceView( m_edgesTexture2, &srvd, &m_edgesTexture2SRV );
                CD3D11_UNORDERED_ACCESS_VIEW_DESC uavDesc( m_edgesTexture, D3D11_UAV_DIMENSION_TEXTURE2D, DXGI_FORMAT_R8_UNORM );
                hr =  pD3DDevice->CreateUnorderedAccessView( m_edgesTexture, &uavDesc, &m_edgesTextureUAV );
                hr =  pD3DDevice->CreateUnorderedAccessView( m_edgesTexture2, &uavDesc, &m_edgesTexture2UAV );
            }*/

            Texture2DDesc td = new Texture2DDesc(width, height, GLenum.GL_R8);
            m_edgesTextureUAV = m_edgesTextureSRV = m_edgesTexture = TextureUtils.createTexture2D(td, null);
            m_edgesTexture2UAV = m_edgesTexture2SRV = m_edgesTexture2 = TextureUtils.createTexture2D(td, null);

        }

        // Half*half compressed 4-edge-per-pixel texture
        {
            /*D3D11_TEXTURE2D_DESC td;
            memset(&td, 0, sizeof(td));
            td.ArraySize = 1;
            td.Format = DXGI_FORMAT_R8G8B8A8_TYPELESS;
            td.Height = (height+1)/2;
            td.Width = (width+1)/2;
            td.CPUAccessFlags = 0;
            td.MipLevels = 1;
            td.MiscFlags = 0;
            td.SampleDesc.Count = 1;
            td.SampleDesc.Quality = 0;
            td.Usage = D3D11_USAGE_DEFAULT;
            td.BindFlags = D3D11_BIND_RENDER_TARGET | D3D11_BIND_SHADER_RESOURCE | D3D11_BIND_UNORDERED_ACCESS;
            hr = pD3DDevice->CreateTexture2D( &td, NULL, &m_mini4edgeTexture );
            {
                CD3D11_SHADER_RESOURCE_VIEW_DESC srvdUint( m_mini4edgeTexture, D3D11_SRV_DIMENSION_TEXTURE2D, DXGI_FORMAT_R8G8B8A8_UINT );
                hr = pD3DDevice->CreateShaderResourceView( m_mini4edgeTexture, &srvdUint, &m_mini4edgeTextureUintSRV );
                CD3D11_RENDER_TARGET_VIEW_DESC dsvDescUint( m_mini4edgeTexture, D3D11_RTV_DIMENSION_TEXTURE2D, DXGI_FORMAT_R8G8B8A8_UINT );
                hr = pD3DDevice->CreateRenderTargetView( m_mini4edgeTexture, &dsvDescUint, &m_mini4edgeTextureUintRTV );
            }*/
            Texture2DDesc td = new Texture2DDesc((width+1)/2, (height+1)/2, GLenum.GL_RGBA8UI);
            m_mini4edgeTextureUintSRV = m_mini4edgeTextureUintRTV = m_mini4edgeTexture = TextureUtils.createTexture2D(td, null);
        }

        // Color working texture
        {
            // use the framebuffer format here; if it's incompatible with UAVs, try
            // http://msdn.microsoft.com/en-gb/library/windows/desktop/ff728749(v=vs.85).aspx

            /*D3D11_TEXTURE2D_DESC td;
            memset(&td, 0, sizeof(td));
            td.ArraySize = 1;
            td.Format = DXGI_FORMAT_R8G8B8A8_TYPELESS; // DXGI_FORMAT_R10G10B10A2_TYPELESS;
            td.Height = height;
            td.Width = width;
            td.CPUAccessFlags = 0;
            td.MipLevels = 1;
            td.MiscFlags = 0;
            td.SampleDesc.Count = 1;
            td.SampleDesc.Quality = 0;
            td.Usage = D3D11_USAGE_DEFAULT;
            td.BindFlags = *//*D3D11_BIND_RENDER_TARGET |*//* D3D11_BIND_SHADER_RESOURCE | D3D11_BIND_UNORDERED_ACCESS;
            hr = pD3DDevice->CreateTexture2D( &td, NULL, &m_workingColorTexture );
            {
                CD3D11_SHADER_RESOURCE_VIEW_DESC srvd( m_workingColorTexture, D3D11_SRV_DIMENSION_TEXTURE2D, DXGI_FORMAT_R8G8B8A8_UNORM_SRGB );
                hr = pD3DDevice->CreateShaderResourceView( m_workingColorTexture, &srvd, &m_workingColorTextureSRV );
                CD3D11_UNORDERED_ACCESS_VIEW_DESC uavDesc( m_workingColorTexture, D3D11_UAV_DIMENSION_TEXTURE2D, DXGI_FORMAT_R8G8B8A8_UNORM );
                hr = pD3DDevice->CreateUnorderedAccessView( m_workingColorTexture, &uavDesc, &m_workingColorTextureUAV );
            }*/
            Texture2DDesc td = new Texture2DDesc(width, height, GLenum.GL_RGBA8);
            m_workingColorTextureSRV = m_workingColorTextureUAV = m_workingColorTexture = TextureUtils.createTexture2D(td, null);
        }

        // Mask depth stencil (1/2 x 1/2) texture
        {
            /*D3D11_TEXTURE2D_DESC td;
            memset(&td, 0, sizeof(td));
            td.ArraySize = 1;
            td.Height = (height+1)/2;
            td.Width = (width+1)/2;
            td.CPUAccessFlags = 0;
            td.MipLevels = 1;
            td.MiscFlags = 0;
            td.SampleDesc.Count = 1;
            td.SampleDesc.Quality = 0;
            td.Usage = D3D11_USAGE_DEFAULT;
            td.BindFlags = D3D11_BIND_RENDER_TARGET;
            td.Format       = DXGI_FORMAT_R16_TYPELESS;
            td.BindFlags    = D3D11_BIND_DEPTH_STENCIL | D3D11_BIND_SHADER_RESOURCE;
            hr = pD3DDevice->CreateTexture2D( &td, NULL, &m_depthStencilTex );
            CD3D11_DEPTH_STENCIL_VIEW_DESC dsvd( m_depthStencilTex, D3D11_DSV_DIMENSION_TEXTURE2D, DXGI_FORMAT_D16_UNORM );
            hr = pD3DDevice->CreateDepthStencilView( m_depthStencilTex, &dsvd, &m_depthStencilTexDSV );
            dsvd.Flags = D3D11_DSV_READ_ONLY_DEPTH;
            hr = pD3DDevice->CreateDepthStencilView( m_depthStencilTex, &dsvd, &m_depthStencilTexReadOnlyDSV );
            CD3D11_SHADER_RESOURCE_VIEW_DESC srvd( m_depthStencilTex, D3D11_SRV_DIMENSION_TEXTURE2D, DXGI_FORMAT_R16_UNORM );
            hr = pD3DDevice->CreateShaderResourceView( m_depthStencilTex, &srvd, &m_depthStencilTexSRV );*/
            Texture2DDesc td = new Texture2DDesc((width+1)/2, (height+1)/2, GLenum.GL_DEPTH_COMPONENT16);
            m_depthStencilTexReadOnlyDSV = m_depthStencilTexSRV = m_depthStencilTexDSV = m_depthStencilTex = TextureUtils.createTexture2D(td, null);
        }
    }
    void			ReleaseTextures(){
        CommonUtil.safeRelease( m_edgesTexture );
        CommonUtil.safeRelease( m_edgesTextureSRV );
        CommonUtil.safeRelease( m_edgesTextureUAV );

        CommonUtil.safeRelease( m_edgesTexture2 );
        CommonUtil.safeRelease( m_edgesTexture2SRV );
        CommonUtil.safeRelease( m_edgesTexture2UAV );

        CommonUtil.safeRelease( m_mini4edgeTexture );
        CommonUtil.safeRelease( m_mini4edgeTextureUintRTV );
        CommonUtil.safeRelease( m_mini4edgeTextureUintSRV );

        CommonUtil.safeRelease( m_workingColorTexture      );
        CommonUtil.safeRelease( m_workingColorTextureSRV   );
        CommonUtil.safeRelease( m_workingColorTextureUAV   );

        CommonUtil.safeRelease( m_depthStencilTex );
        CommonUtil.safeRelease( m_depthStencilTexDSV );
        CommonUtil.safeRelease( m_depthStencilTexReadOnlyDSV );
        CommonUtil.safeRelease( m_depthStencilTexSRV );
    }

    private boolean DoFirstTime = true;

    void                Draw(double timeElapsed, float PPAADEMO_gEdgeDetectionThreshold, float PPAADEMO_gNonDominantEdgeRemovalAmount,
                             Texture2D sourceColorSRV_SRGB, Texture2D sourceColorSRV_UNORM, Texture2D destColorUAV, Texture2D depthSRV,
                             int displayType /*= DT_Normal*/, Texture2D exportZoomColourTexture/* = NULL*/, Texture2D exportEdgesInfoTexture /*= NULL*/ ){
//        static bool DoFirstTime = true;
        //float    f4zero[4] = { 0, 0, 0, 0 };
//        UINT     u4zero[4] = { 0, 0, 0, 0 };
//        ID3D11ShaderResourceView* nullSRVs[12] = { NULL };

        if(DoFirstTime)
        {
            DoFirstTime = false;
            // first time only: we need to clear textures.
            // the algorithm self-clears them later
//            context->ClearUnorderedAccessViewUint( m_edgesTextureUAV, u4zero );
//            context->ClearUnorderedAccessViewUint( m_edgesTexture2UAV, u4zero );
            gl.glClearTexImage(m_edgesTextureUAV.getTexture(), 0, GLenum.GL_RGBA, GLenum.GL_UNSIGNED_BYTE, (ByteBuffer) null);
            gl.glClearTexImage(m_edgesTexture2UAV.getTexture(), 0, GLenum.GL_RGBA, GLenum.GL_UNSIGNED_BYTE, (ByteBuffer) null);
        }

/*#ifdef _DEBUG
        if( m_isInGammaCorrectMode )
            assert( IsSRGB(sourceColorSRV_SRGB) );
        else
            assert( !IsSRGB(sourceColorSRV_SRGB) );
        assert( !IsSRGB(sourceColorSRV_UNORM) );
#endif*/

        /*D3D11_VIEWPORT vpOld;
        // backup original viewport
        UINT numViewports = 1;
        context->RSGetViewports( &numViewports, &vpOld );*/
        int vpOldx, vpOldy, vpOldw, vpOldh;
        IntBuffer viewport = CacheBuffer.getCachedIntBuffer(4);
        gl.glGetIntegerv(GLenum.GL_VIEWPORT, viewport);
        vpOldx = viewport.get();
        vpOldy = viewport.get();
        vpOldw = viewport.get();
        vpOldh = viewport.get();

        // Backup current render/stencil
        /*ID3D11RenderTargetView * oldRenderTargetViews[4] = { NULL };
        ID3D11DepthStencilView * oldDepthStencilView = NULL;
        context->OMGetRenderTargets( _countof(oldRenderTargetViews), oldRenderTargetViews, &oldDepthStencilView );*/

        /*ID3D11RenderTargetView *pView[4] = {NULL,NULL,NULL,NULL};
        context->OMSetRenderTargets( 4, pView, NULL );
        // Clear the shader resources to avoid a hazard warning
        ID3D11ShaderResourceView *pNullResources[16] = {0};
        context->PSSetShaderResources(0, 16, pNullResources );
        context->VSSetShaderResources(0, 16, pNullResources );*/

        m_frameID++;

        Texture2D edgesTextureA_SRV;
        Texture2D edgesTextureA_UAV;
        Texture2D edgesTextureA;
        Texture2D edgesTextureB_SRV;
        Texture2D edgesTextureB_UAV;
        Texture2D edgesTextureB;
        // flip flop - one pass clears the texture that needs clearing for the other one (actually it's only important that it clears the highest bit)
        if( (m_frameID % 2) == 0 )
        {
            edgesTextureA       = m_edgesTexture;
            edgesTextureA_SRV   = m_edgesTextureSRV;
            edgesTextureA_UAV   = m_edgesTextureUAV;
            edgesTextureB       = m_edgesTexture2;
            edgesTextureB_SRV   = m_edgesTexture2SRV;
            edgesTextureB_UAV   = m_edgesTexture2UAV;
        }
        else
        {
            edgesTextureA       = m_edgesTexture2;
            edgesTextureA_SRV   = m_edgesTexture2SRV;
            edgesTextureA_UAV   = m_edgesTexture2UAV;
            edgesTextureB       = m_edgesTexture;
            edgesTextureB_SRV   = m_edgesTextureSRV;
            edgesTextureB_UAV   = m_edgesTextureUAV;
        }

        /*D3D11_VIEWPORT viewport, viewportHalfHalf;

        {
            // Setup the viewport to match the backbuffer
            viewport.TopLeftX = 0.0f;
            viewport.TopLeftY = 0.0f;
            viewport.Width    = (float)m_Width;
            viewport.Height   = (float)m_Height;
            viewport.MinDepth = 0;
            viewport.MaxDepth = 1;

            viewportHalfHalf = viewport;
            assert( ( (((int)viewport.TopLeftX)%2)==0 ) && ( (((int)viewport.TopLeftY)%2)==0 ) );
            viewportHalfHalf.TopLeftX = viewport.TopLeftX/2;
            viewportHalfHalf.TopLeftY = viewport.TopLeftY/2;
            viewportHalfHalf.Width    = (float)((int(viewport.Width)+1)/2);
            viewportHalfHalf.Height   = (float)((int(viewport.Height)+1)/2);
        }*/

        final boolean dbgExportAAInfo = displayType == DT_ShowEdges;

        // Setup constants
        {
            /*HRESULT hr;
            D3D11_MAPPED_SUBRESOURCE subResMap;
            hr = context->Map( m_constantsBuffer, 0, D3D11_MAP_WRITE_DISCARD, 0, &subResMap );*/

//            CMAAConstants & consts = *((CMAAConstants*)subResMap.pData);
            CMAAConstants consts = m_Constants;

            consts.LumWeights[0]                = 0.2126f;
            consts.LumWeights[1]                = 0.7152f;
            consts.LumWeights[2]                = 0.0722f;
            consts.LumWeights[3]                = 0.0000f;

            consts.ColorThreshold               = PPAADEMO_gEdgeDetectionThreshold;             // 1.0/13.0
            consts.DepthThreshold               = 0.07f;
            consts.NonDominantEdgeRemovalAmount = Numeric.clamp( PPAADEMO_gNonDominantEdgeRemovalAmount, 0.05f, 0.95f );       // 0.35
            consts.Dummy0                       = 0.0f;

            consts.OneOverScreenSize[0]         = 1.0f / m_Width;
            consts.OneOverScreenSize[1]         = 1.0f / m_Height;
            consts.ScreenWidth                  = (int)m_Width;
            consts.ScreenHeight                 = (int)m_Height;

            consts.DebugZoomTool[0]             = 0.0f;
            consts.DebugZoomTool[1]             = 0.0f;
            consts.DebugZoomTool[2]             = 0.0f;
            consts.DebugZoomTool[3]             = 0.0f;

            /*context->Unmap( m_constantsBuffer, 0 );
            context->PSSetConstantBuffers( 4, 1, &m_constantsBuffer );
            context->CSSetConstantBuffers( 4, 1, &m_constantsBuffer );*/
            ByteBuffer bytes = CacheBuffer.getCachedByteBuffer(CMAAConstants.SIZE);
            consts.store(bytes).flip();
            m_constantsBuffer.update(0, bytes);
            gl.glBindBufferBase(m_constantsBuffer.getTarget(), 0, m_constantsBuffer.getBuffer());
        }

        // common samplers
        /*ID3D11SamplerState * samplerStates[2];
        samplerStates[0] = MySample::GetSS_PointClamp();
        samplerStates[1] = MySample::GetSS_LinearClamp();
        context->PSSetSamplers( 0, 2, samplerStates ); TODO*/

        // set adequate viewport
//        context->RSSetViewports( 1, &viewportHalfHalf );
        m_renderTargets.bind();
        gl.glViewport(0,0, Numeric.divideAndRoundUp((int)m_Width, 2), Numeric.divideAndRoundUp((int)m_Height, 2));

//        UINT UAVInitialCounts[] = { (UINT)-1, (UINT)-1, (UINT)-1, (UINT)-1 };

        // no need to clear DepthStencil; cleared by the next fullscreen pass - not sure if this is correct for Hi-Z but seems to work well
        // on all tested hardware (AMD 5xxx, 7xxx; NVidia 4XX, 6XX; Intel IvyBridge/Haswell)
        //context->ClearDepthStencilView( m_depthStencilTexDSV, D3D11_CLEAR_DEPTH, 1.0f, 0 );

        // Detect edges Pass 0
        //   - for every pixel detect edges to the right and down and output depth mask where edges detected (1 - far, for detected, 0-near for empty pixels)
        {
            //context->OMSetRenderTargets( 1, &m_mini4edgeTextureUintRTV, m_depthStencilTexDSV );
//            ID3D11UnorderedAccessView * UAVs[] = { m_workingColorTextureUAV };
//            context->OMSetRenderTargetsAndUnorderedAccessViews( 1, &m_mini4edgeTextureUintRTV, m_depthStencilTexDSV, 1, _countof(UAVs), UAVs, UAVInitialCounts );
            TextureGL[] RTVs = {m_mini4edgeTextureUintRTV, m_depthStencilTexDSV};
            m_renderTargets.setRenderTextures(RTVs, null);
            gl.glBindImageTexture(0, m_workingColorTextureUAV.getTexture(), 0, false, 0, GLenum.GL_WRITE_ONLY, m_workingColorTextureUAV.getFormat());

            /*context->PSSetShaderResources( 0, 1, &sourceColorSRV_UNORM );
            MySample::FullscreenPassDraw( context, m_edges0PS, NULL, MySample::GetBS_Opaque(), m_renderCreateMaskDSS, 0, 1.0f );
            context->PSSetShaderResources( 0, 1, nullSRVs );*/
            gl.glActiveTexture(GLenum.GL_TEXTURE0);
            gl.glBindTexture(sourceColorSRV_UNORM.getTarget(), sourceColorSRV_UNORM.getTexture());
            m_renderCreateMaskDSS.run();
            m_edges0PS.enable();
            gl.glDrawArrays(GLenum.GL_TRIANGLES, 0, 3);

            if(!m_prinOnce){
                m_edges0PS.printPrograminfo();
            }
        }

        // Detect edges Pass 1 (finish the previous pass edge processing).
        // Do the culling of non-dominant local edges (leave mainly locally dominant edges) and merge Right and Bottom edges into TopRightBottomLeft
        {
            /*ID3D11UnorderedAccessView * UAVs[] = { edgesTextureB_UAV };
            context->OMSetRenderTargetsAndUnorderedAccessViews( 0, NULL, m_depthStencilTexReadOnlyDSV, 0, _countof(UAVs), UAVs, UAVInitialCounts );
            context->PSSetShaderResources( 3, 1, &m_mini4edgeTextureUintSRV );*/
            m_renderTargets.setRenderTexture(m_depthStencilTexReadOnlyDSV, null);
            gl.glBindImageTexture(0, edgesTextureB_UAV.getTexture(), 0, false, 0, GLenum.GL_WRITE_ONLY, edgesTextureB_UAV.getFormat());
            gl.glActiveTexture(GLenum.GL_TEXTURE0);
            gl.glBindTexture(m_mini4edgeTextureUintSRV.getTarget(), m_mini4edgeTextureUintSRV.getTexture());

            /*MySample::FullscreenPassDraw( context, m_edges1PS, NULL, MySample::GetBS_Opaque(), m_renderUseMaskDSS, 0, 0.0f );
            context->PSSetShaderResources( 3, 1, nullSRVs );
            context->PSSetShaderResources( 0, 1, nullSRVs );*/
            m_renderUseMaskDSS.run();
            m_edges1PS.enable();
            gl.glDrawArrays(GLenum.GL_TRIANGLES, 0, 3);

            if(!m_prinOnce){
                m_edges1PS.printPrograminfo();
            }
        }

        //  - Combine RightBottom (.xy) edges from previous pass into RightBottomLeftTop (.xyzw) edges and output it into the mask (have to fill in the whole buffer
        //    including empty ones for the line length detection to work correctly).
        //  - On all pixels with any edge, input buffer into a temporary color buffer needed for correct blending in the next pass (other pixels not needed
        //    so not copied to avoid bandwidth use)
        //  - On all pixels with 2 or more edges output positive depth mask for the next pass
        {
            // Combine edges: each pixel will now contain info on all (top, right, bottom, left) edges; also create depth mask as above depth and mark potential Zs
            // AND also copy source color data but only on edge pixels
            /*ID3D11UnorderedAccessView * UAVs[] = { destColorUAV, edgesTextureA_UAV };
            context->OMSetRenderTargetsAndUnorderedAccessViews( 0, NULL, m_depthStencilTexDSV, 1, _countof(UAVs), UAVs, UAVInitialCounts );*/
            m_renderTargets.setRenderTexture(m_depthStencilTexDSV, null);
            gl.glBindImageTexture(0, destColorUAV.getTexture(), 0, false, 0, GLenum.GL_WRITE_ONLY, destColorUAV.getFormat());
            gl.glBindImageTexture(1, edgesTextureA_UAV.getTexture(), 0, false, 0, GLenum.GL_WRITE_ONLY, edgesTextureA_UAV.getFormat());

            /*context->PSSetShaderResources( 0, 1, &m_workingColorTextureSRV );
            context->PSSetShaderResources( 3, 1, &edgesTextureB_SRV );
            MySample::FullscreenPassDraw( context, m_edgesCombinePS, NULL, MySample::GetBS_Opaque(), m_renderCreateMaskDSS, 0, 1.0f );
            context->PSSetShaderResources( 0, 1, nullSRVs );
            context->PSSetShaderResources( 3, 1, nullSRVs );*/
            gl.glActiveTexture(GLenum.GL_TEXTURE0);
            gl.glBindTexture(m_workingColorTextureSRV.getTarget(), m_workingColorTextureSRV.getTexture());
            gl.glActiveTexture(GLenum.GL_TEXTURE1);
            gl.glBindTexture(edgesTextureB_SRV.getTarget(), edgesTextureB_SRV.getTexture());
            m_renderCreateMaskDSS.run();
            m_edgesCombinePS.enable();
            gl.glDrawArrays(GLenum.GL_TRIANGLES, 0, 3);

            if(!m_prinOnce){
                m_edgesCombinePS.printPrograminfo();
            }
        }

        // Using depth mask and [earlydepthstencil] to work on pixels with 2, 3, 4 edges:
        //    - First blend simple blur map for 2,3,4 edge pixels
        //    - Then do the lines (line length counter -should- guarantee no overlap with other pixels - pixels with 1 edge are excluded in the previous
        //      pass and the pixels with 2 parallel edges are excluded in the simple blur)
        {
            if( dbgExportAAInfo )
            {
//                context->CopyResource( edgesTextureB, edgesTextureA );
//                ID3D11UnorderedAccessView * UAVs[] = { destColorUAV, edgesTextureB_UAV };

                gl.glCopyImageSubData(edgesTextureA.getTexture(), edgesTextureA.getTarget(), 0, 0,0,0,
                        edgesTextureB.getTexture(), edgesTextureB.getTarget(), 0,0,0,0,
                        edgesTextureA.getWidth(), edgesTextureA.getHeight(), 1);
                Texture2D[] UAVs = {destColorUAV, edgesTextureA_UAV};
                // 2 seconds show blur, 2 seconds not
                if( timeElapsed % 4.0 > 2.0 )
                    UAVs[0] = null;

//                context->OMSetRenderTargetsAndUnorderedAccessViews( 0, NULL, m_depthStencilTexReadOnlyDSV, 1, _countof(UAVs), UAVs, UAVInitialCounts );
                m_renderTargets.setRenderTexture(m_depthStencilTexReadOnlyDSV, null);
                for(int i = 0; i < UAVs.length; i++){
                    if(UAVs[i] != null){
                        gl.glBindImageTexture(i, UAVs[i].getTexture(), 0, false, 0, GLenum.GL_WRITE_ONLY, UAVs[i].getFormat());
                    }else{
                        gl.glBindImageTexture(i, 0, 0, false, 0, GLenum.GL_WRITE_ONLY,GLenum.GL_RGBA8);
                    }
                }
            }
            else
            {
                /*ID3D11UnorderedAccessView * UAVs[] = { destColorUAV }; //m_blurmapTextureUAV };
                context->OMSetRenderTargetsAndUnorderedAccessViews( 0, NULL, m_depthStencilTexReadOnlyDSV, 1, _countof(UAVs), UAVs, UAVInitialCounts );*/
                m_renderTargets.setRenderTexture(m_depthStencilTexReadOnlyDSV, null);
                gl.glBindImageTexture(0, destColorUAV.getTexture(), 0, false, 0, GLenum.GL_WRITE_ONLY, destColorUAV.getFormat());
            }

            /*context->PSSetShaderResources( 0, 1, &m_workingColorTextureSRV );
            context->PSSetShaderResources( 3, 1, &edgesTextureA_SRV );
            context->PSSetShaderResources( 4, 1, &m_depthStencilTexSRV );*/
            gl.glActiveTexture(GLenum.GL_TEXTURE0);
            gl.glBindTexture(m_workingColorTextureSRV.getTarget(), m_workingColorTextureSRV.getTexture());
            gl.glActiveTexture(GLenum.GL_TEXTURE1);
            gl.glBindTexture(edgesTextureA_SRV.getTarget(), edgesTextureA_SRV.getTexture());
            gl.glActiveTexture(GLenum.GL_TEXTURE2);
            gl.glBindTexture(m_depthStencilTexSRV.getTarget(), m_depthStencilTexSRV.getTexture());

//            MySample::FullscreenPassDraw( context, (dbgExportAAInfo)?(m_dbgProcessAndApplyPS):(m_processAndApplyPS), NULL, MySample::GetBS_Opaque(), m_renderUseMaskDSS, 0, 0.0f );
            GLSLProgram program = (dbgExportAAInfo)?(m_dbgProcessAndApplyPS):(m_processAndApplyPS);
            program.enable();
            m_renderUseMaskDSS.run();
            gl.glDrawArrays(GLenum.GL_TRIANGLES, 0, 3);

//            context->PSSetShaderResources( 0, _countof(nullSRVs), nullSRVs );
        }

        // set full res viewport
//        context->RSSetViewports( 1, &viewport );
        gl.glViewport(0,0, (int)m_Width, (int)m_Height);

        // Restore original render/depthstencil
        /*context->OMSetRenderTargets( _countof(oldRenderTargetViews), oldRenderTargetViews, oldDepthStencilView );
        for( int i = 0; i < _countof(oldRenderTargetViews); i++ )
            SAFE_RELEASE( oldRenderTargetViews[i] );
        SAFE_RELEASE( oldDepthStencilView );*/

        // Restore original viewport
//        context->RSSetViewports( 1, &vpOld );
        gl.glViewport(vpOldx, vpOldy, vpOldw, vpOldh);

        // For debugging purposes only: be sure you're not doing it otherwise! :)
        if( (exportEdgesInfoTexture != null) && dbgExportAAInfo )
        {
//            context->CopyResource( exportEdgesInfoTexture, edgesTextureB );
            gl.glCopyImageSubData(edgesTextureB.getTexture(), edgesTextureB.getTarget(), 0, 0,0,0,
                    exportEdgesInfoTexture.getTexture(), exportEdgesInfoTexture.getTarget(), 0,0,0,0,
                    edgesTextureB.getWidth(), edgesTextureB.getHeight(), 1);
        }

        // For debugging purposes only: be sure you're not doing it otherwise! :)
        if( exportZoomColourTexture != null )
        {
            /*ID3D11Resource*         pRT = NULL;
            destColorUAV->GetResource( &pRT );
            context->CopyResource( exportZoomColourTexture, pRT );
            pRT->Release();*/
            gl.glCopyImageSubData(destColorUAV.getTexture(), destColorUAV.getTarget(), 0, 0,0,0,
                    exportZoomColourTexture.getTexture(), exportZoomColourTexture.getTarget(), 0,0,0,0,
                    destColorUAV.getWidth(), destColorUAV.getHeight(), 1);
        }

        if( displayType == DT_ShowEdges )
        {

            // Copy the whole working color texture
            /*ID3D11Resource*         pRT = NULL;
            destColorUAV->GetResource( &pRT );
            context->CopyResource( m_workingColorTexture, pRT );
            pRT->Release();*/
            gl.glCopyImageSubData(destColorUAV.getTexture(), destColorUAV.getTarget(), 0, 0,0,0,
                    m_workingColorTexture.getTexture(), m_workingColorTexture.getTarget(), 0,0,0,0,
                    destColorUAV.getWidth(), destColorUAV.getHeight(), 1);

            /*context->PSSetShaderResources( 0, 1, &m_workingColorTextureSRV );
            context->PSSetShaderResources( 3, 1, &edgesTextureB_SRV );*/
            gl.glActiveTexture(GLenum.GL_TEXTURE0);
            gl.glBindTexture(m_workingColorTextureSRV.getTarget(), m_workingColorTextureSRV.getTexture());
            gl.glActiveTexture(GLenum.GL_TEXTURE1);
            gl.glBindTexture(edgesTextureB_SRV.getTarget(), edgesTextureB_SRV.getTexture());

            // Debug display edges!
            if( displayType == DT_ShowEdges )
            {
//                MySample::FullscreenPassDraw( context, m_dbgDisplayEdgesPS, NULL, MySample::GetBS_AlphaBlend(), m_renderAlwaysDSS, 0, 0.0f );
                m_dbgDisplayEdgesPS.enable();
                m_renderAlwaysDSS.run();
                // TODO BS_AlphaBlend()
                gl.glDrawArrays(GLenum.GL_TRIANGLES, 0, 3);
            }

//            context->PSSetShaderResources( 0, _countof(nullSRVs), nullSRVs );
        }
//        context->PSSetShaderResources( 0, _countof(nullSRVs), nullSRVs );

        m_prinOnce = true;
    }

    @Override
    public void dispose() {
        OnShutdown();
    }

    public static class CMAAConstants implements Readable
    {
        static final int SIZE = Vector4f.SIZE * 4;
        // .rgb - luminance weight for each colour channel; .w unused for now (maybe will be used for gamma correction before edge detect)
        final float[]           LumWeights = new float[4];

        float           ColorThreshold;                     // for simple edge detection
        float           DepthThreshold;                     // for depth (unused at the moment)
        float           NonDominantEdgeRemovalAmount;       // how much non-dominant edges to remove
        float           Dummy0;

        final float[]           OneOverScreenSize = new float[2];
        int             ScreenWidth;
        int             ScreenHeight;

        final float[]           DebugZoomTool  = new float[4];

        @Override
        public ByteBuffer store(ByteBuffer buf) {
            CacheBuffer.put(buf, LumWeights);
            buf.putFloat(ColorThreshold);
            buf.putFloat(DepthThreshold);
            buf.putFloat(NonDominantEdgeRemovalAmount);
            buf.putFloat(Dummy0);

            CacheBuffer.put(buf, OneOverScreenSize);
            buf.putFloat(ScreenWidth);
            buf.putFloat(ScreenHeight);

            CacheBuffer.put(buf, DebugZoomTool);
            return buf;
        }
    };
}
