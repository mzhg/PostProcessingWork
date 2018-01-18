package jet.opengl.demos.intel.cput;

import jet.opengl.postprocessing.common.Disposeable;
import jet.opengl.postprocessing.common.GLFuncProvider;
import jet.opengl.postprocessing.common.GLFuncProviderFactory;
import jet.opengl.postprocessing.common.GLenum;
import jet.opengl.postprocessing.texture.Texture2D;
import jet.opengl.postprocessing.texture.Texture2DDesc;
import jet.opengl.postprocessing.texture.TextureUtils;

/**
 * Created by mazhen'gui on 2018/1/16.
 */

public class CPUTRenderTargetDepth implements Disposeable{
    protected static int                    sCurrentWidth;
    protected static int                    sCurrentHeight;
    protected static Texture2D              spActiveDepthStencilView;

    protected String                        mName;
    protected int                           mWidth;
    protected int                           mHeight;
    protected boolean                       mRenderTargetSet;

    protected int                           mDepthFormat;
    protected int                           mMultiSampleCount;

    protected final Texture2DDesc           mDepthDesc = new Texture2DDesc();
    protected CPUTTexture                   mpDepthTexture;
    protected Texture2D                     mpDepthTextureDX;
//    ID3D11ShaderResourceView      *mpDepthResourceView;
//    ID3D11DepthStencilView        *mpDepthStencilView;

//    ID3D11Texture2D               *mpDepthTextureDXStaging;
//    eCPUTMapType                   mMappedType;

    protected int                           mSavedWidth;
    protected int                           mSavedHeight;
    protected Texture2D                     mpSavedColorRenderTargetView;
    protected Texture2D                     mpSavedDepthStencilView;

    public static Texture2D               GetActiveDepthStencilView() { return spActiveDepthStencilView; }
    public static void                    SetActiveDepthStencilView(Texture2D pView) { spActiveDepthStencilView = pView; }
    public static void                    SetActiveWidthHeight( int width, int height ) {sCurrentWidth = width; sCurrentHeight=height; }
    public static int                     GetActiveWidth()  {return sCurrentWidth; }
    public static int                     GetActiveHeight() {return sCurrentHeight; }

    public void CreateRenderTarget(
            String     textureName,
            int        width,
            int        height,
            int        depthFormat,
            int        multiSampleCount /*= 1*/,
            boolean    recreate /*= false*/
    ){
        mName             = textureName;
        mWidth            = width;
        mHeight           = height;
        mDepthFormat      = depthFormat;
        mMultiSampleCount = multiSampleCount;

        // NOTE: The following doesn't work for DX10.0 devices.
        // They don't support binding an MSAA depth texture as

        // If we have a DX 10.1 or no MSAA, then create a shader resource view, and add a CPUTTexture to the AssetLibrary
//        D3D_FEATURE_LEVEL featureLevel = gpSample->GetFeatureLevel();
        boolean supportsResourceView = /*( featureLevel >= D3D_FEATURE_LEVEL_10_1) ||*/ (mMultiSampleCount==1);

        // make sure the format is depth or stencil
        int compment = TextureUtils.measureFormat(depthFormat);
        if(compment != GLenum.GL_DEPTH_COMPONENT && compment != GLenum.GL_DEPTH_STENCIL && compment != GLenum.GL_STENCIL_COMPONENTS){
            throw new IllegalArgumentException("Invalid depth stencil format: " + TextureUtils.getFormatName(depthFormat));
        }

        Texture2DDesc depthDesc = new Texture2DDesc(
                width,
                height,
                1, // MIP Levels
                1, // Array Size
                depthFormat, //  DXGI_FORMAT_R32_TYPELESS
                mMultiSampleCount/*, 0
                D3D11_USAGE_DEFAULT,
                D3D11_BIND_DEPTH_STENCIL | (supportsResourceView ? D3D11_BIND_SHADER_RESOURCE : 0),
                0, // CPU Access flags
                0 // Misc flags*/
        );

        /*if( depthFormat == DXGI_FORMAT_D16_UNORM )
            depthDesc.Format = DXGI_FORMAT_R16_TYPELESS;*/

        // Create either a Texture2D, or Texture2DMS, depending on multisample count.
        /*D3D11_DSV_DIMENSION dsvDimension = (mMultiSampleCount>1) ? D3D11_DSV_DIMENSION_TEXTURE2DMS : D3D11_DSV_DIMENSION_TEXTURE2D;

        ID3D11Device *pD3dDevice = CPUT_DX11::GetDevice();
        result = pD3dDevice->CreateTexture2D( &depthDesc, NULL, &mpDepthTextureDX );
        ASSERT( SUCCEEDED(result), _L("Failed creating depth texture.\nAre you using MSAA with a DX10.0 GPU?\nOnly DX10.1 and above can create a shader resource view for an MSAA depth texture.") );

        D3D11_DEPTH_STENCIL_VIEW_DESC dsvd = { depthFormat, dsvDimension, 0 };
        result = pD3dDevice->CreateDepthStencilView( mpDepthTextureDX, &dsvd, &mpDepthStencilView );
        ASSERT( SUCCEEDED(result), _L("Failed creating depth stencil view") );
        CPUTSetDebugName( mpDepthStencilView, mName );*/
        mpDepthTextureDX = TextureUtils.createTexture2D(depthDesc, null);
        mpDepthTextureDX.setName(mName);

        if( supportsResourceView )
        {
            /*D3D11_SRV_DIMENSION srvDimension = (mMultiSampleCount>1) ? D3D11_SRV_DIMENSION_TEXTURE2DMS : D3D11_SRV_DIMENSION_TEXTURE2D;
            // Create the shader-resource view
            D3D11_SHADER_RESOURCE_VIEW_DESC depthRsDesc =
                    {
                            DXGI_FORMAT(depthFormat + 1),
                            srvDimension,
                            0
                    };
            // TODO: Support optionally creating MIP chain.  Then, support MIP generation (e.g., GenerateMIPS()).
            depthRsDesc.Texture2D.MipLevels = 1;

            result = pD3dDevice->CreateShaderResourceView( mpDepthTextureDX, &depthRsDesc, &mpDepthResourceView );
            ASSERT( SUCCEEDED(result), _L("Failed creating render target shader resource view") );
            CPUTSetDebugName( mpDepthResourceView, textureName + _L(" Depth") );*/

            if( !recreate )
            {
                CPUTAssetLibrary pAssetLibrary = CPUTAssetLibrary.GetAssetLibrary();

                mpDepthTexture = new CPUTTextureDX11();
                pAssetLibrary.AddTexture( mName, mpDepthTexture );
            }
            ((CPUTTextureDX11)mpDepthTexture).SetTextureAndShaderResourceView(mpDepthTextureDX, mpDepthTextureDX);
        }
    }

    public final void RecreateRenderTarget(
            int        width,
            int        height){
        RecreateRenderTarget(width, height, 0, 1);
    }

    public void RecreateRenderTarget(
            int        width,
            int        height,
            int depthFormat /*= DXGI_FORMAT_UNKNOWN*/,
            int        multiSampleCount /*= 1*/
    ){
        // We don't release these.  Instead, we release their resource views, and change them to the newly-created versions.
        //SAFE_RELEASE( mpDepthTexture );
        SAFE_RELEASE( mpDepthTextureDX );
        // Do not release saved resource views since they are not addreff'ed
        //SAFE_RELEASE( mpSavedColorRenderTargetView );
        //SAFE_RELEASE( mpSavedDepthStencilView );

        CreateRenderTarget( mName, width, height, (0 != depthFormat) ? depthFormat : mDepthFormat, multiSampleCount, true );
    }

    public void SetRenderTarget(
            CPUTRenderParameters renderParams,
            int renderTargetIndex /*= 0*/,
            float zClearVal /*= 0.0f*/,
            boolean  clear /*= false*/
    ){
        // ****************************
        // Save the current render target "state" so we can restore it later.
        // ****************************
        mSavedWidth   = CPUTRenderTargetDepth.GetActiveWidth();
        mSavedHeight  = CPUTRenderTargetDepth.GetActiveHeight();

        CPUTRenderTargetColor.SetActiveWidthHeight( mWidth, mHeight );
        CPUTRenderTargetDepth.SetActiveWidthHeight( mWidth, mHeight );

        // TODO: support multiple render target views (i.e., MRT)
//        ID3D11DeviceContext *pContext = ((CPUTRenderParametersDX*)&renderParams)->mpContext;
        CPUTRenderParametersDX pContext = (CPUTRenderParametersDX)renderParams;

        // Make sure this render target isn't currently bound as a texture.
        /*static ID3D11ShaderResourceView *pSRV[16] = {0};
        pContext->PSSetShaderResources( 0, 16, pSRV );*/
        GLFuncProvider gl = GLFuncProviderFactory.getGLFuncProvider();
        for(int i = 0; i < 16; i++){
            gl.glBindTextureUnit(i, 0);
        }

        // Save the color and depth views so we can restore them later.
        mpSavedColorRenderTargetView = CPUTRenderTargetColor.GetActiveRenderTargetView();
        mpSavedDepthStencilView      = CPUTRenderTargetDepth.GetActiveDepthStencilView();

        // ****************************
        // Set the new render target states
        // ****************************
//        ID3D11RenderTargetView *pView[1] = {NULL};
        Texture2D pView = null;
        pContext.OMSetRenderTargets( /*1,*/ pView, mpDepthTextureDX );

        CPUTRenderTargetColor.SetActiveRenderTargetView( null );
        CPUTRenderTargetDepth.SetActiveDepthStencilView( mpDepthTextureDX );

        if( clear )
        {
            pContext.ClearDepthStencilView( mpDepthTextureDX, /*D3D11_CLEAR_DEPTH | D3D11_CLEAR_STENCIL,*/
                    GLenum.GL_DEPTH_BUFFER_BIT|GLenum.GL_STENCIL_BUFFER_BIT, zClearVal, 0 );
        }
        /*D3D11_VIEWPORT viewport  = { 0.0f, 0.0f, (float)mWidth, (float)mHeight, 0.0f, 1.0f };
        ((CPUTRenderParametersDX*)&renderParams)->mpContext->RSSetViewports( 1, &viewport );*/
        pContext.RSSetViewports(0,0, mWidth, mHeight);

        mRenderTargetSet = true;
    }

    public void RestoreRenderTarget( CPUTRenderParameters renderParams ){
        assert mRenderTargetSet : ("Render target restored without calling SetRenderTarget()");

//        ID3D11DeviceContext *pContext = ((CPUTRenderParametersDX*)&renderParams)->mpContext;
        CPUTRenderParametersDX pContext = (CPUTRenderParametersDX)renderParams;

        pContext.OMSetRenderTargets(/* 1, &*/mpSavedColorRenderTargetView, mpSavedDepthStencilView );

        CPUTRenderTargetColor.SetActiveWidthHeight( mSavedWidth, mSavedHeight );
        CPUTRenderTargetDepth.SetActiveWidthHeight( mSavedWidth, mSavedHeight );
        CPUTRenderTargetColor.SetActiveRenderTargetView( mpSavedColorRenderTargetView );
        CPUTRenderTargetDepth.SetActiveDepthStencilView( mpSavedDepthStencilView );

        // TODO: save/restore original VIEWPORT settings, not assume full-screen viewport.
        /*D3D11_VIEWPORT viewport  = { 0.0f, 0.0f, (float)mSavedWidth, (float)mSavedHeight, 0.0f, 1.0f };
        ((CPUTRenderParametersDX*)&renderParams)->mpContext->RSSetViewports( 1, &viewport );*/
        pContext.RSSetViewports(0,0, mSavedWidth, mSavedHeight);

        mRenderTargetSet = false;
    }

    public Texture2D GetDepthBufferView()   { return mpDepthTextureDX; }
    public Texture2D GetDepthResourceView() { return mpDepthTextureDX; }
    public int       GetWidth()             { return mWidth; }
    public int       GetHeight()            { return mHeight; }

    /*D3D11_MAPPED_SUBRESOURCE  MapRenderTarget(   CPUTRenderParameters &params, eCPUTMapType type, bool wait=true );
    void                      UnmapRenderTarget( CPUTRenderParameters &params );*/

    @Override
    public void dispose() {
        SAFE_RELEASE( mpDepthTexture );  mpDepthTexture = null;
        SAFE_RELEASE( mpDepthTextureDX ); mpDepthTextureDX = null;
    }
}
