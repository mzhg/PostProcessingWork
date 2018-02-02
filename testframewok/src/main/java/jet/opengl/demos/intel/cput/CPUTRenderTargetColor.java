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

public class CPUTRenderTargetColor implements Disposeable{

    public static Texture2D  GetActiveRenderTargetView() { return spActiveRenderTargetView; }
    public static void       SetActiveRenderTargetView(Texture2D pView) { spActiveRenderTargetView = pView; }
    public static void       SetActiveWidthHeight( int width, int height ) {sCurrentWidth = width; sCurrentHeight=height; }
    public static int        GetActiveWidth()  {return sCurrentWidth; }
    public static int        GetActiveHeight() {return sCurrentHeight; }

    protected static int                    sCurrentWidth;
    protected static int                    sCurrentHeight;
    protected static Texture2D              spActiveRenderTargetView;

    protected String                        mName;
    protected int                           mWidth;
    protected int                           mHeight;
    protected boolean                       mRenderTargetSet;
    protected boolean                       mHasUav;

    protected int                           mColorFormat;
    protected int                           mMultiSampleCount;

    protected final Texture2DDesc           mColorDesc = new Texture2DDesc();
    protected CPUTTexture                   mpColorTexture;
    protected CPUTBuffer                    mpColorBuffer;
    protected Texture2D                     mpColorTextureDX;
//    ID3D11ShaderResourceView      *mpColorSRV;
//    ID3D11UnorderedAccessView     *mpColorUAV;
    protected Texture2D                     mpColorRenderTargetView;

    protected CPUTTexture                   mpColorTextureMSAA;
    protected Texture2D                     mpColorTextureDXMSAA;
//    ID3D11ShaderResourceView      *mpColorSRVMSAA;

//    ID3D11Texture2D               *mpColorTextureDXStaging;
//    eCPUTMapType                   mMappedType;

    protected int                           mSavedWidth;
    protected int                           mSavedHeight;
    protected Texture2D                     mpSavedColorRenderTargetView;
    protected Texture2D                     mpSavedDepthStencilView;

    public void CreateRenderTarget(
            String      textureName,
            int         width,
            int         height,
            int         colorFormat,
            int         multiSampleCount /*= 1*/,
            boolean     createUAV /*= false*/,
            boolean     recreate /*= false*/
    ){
        mName             = textureName;
        mWidth            = width;
        mHeight           = height;
        mColorFormat      = colorFormat;
        mMultiSampleCount = multiSampleCount;

        CPUTAssetLibrary pAssetLibrary = CPUTAssetLibrary.GetAssetLibrary();
//        CPUTOSServices   pServices     = CPUTOSServices::GetOSServices();

        // Create the color texture
        /*mColorDesc = CD3D11_TEXTURE2D_DESC(
                colorFormat,
                width,
                height,
                1, // Array Size
                1, // MIP Levels
                D3D11_BIND_RENDER_TARGET | D3D11_BIND_SHADER_RESOURCE | D3D11_BIND_UNORDERED_ACCESS,
                D3D11_USAGE_DEFAULT,
                0,
                mMultiSampleCount, 0
        );*/
        mColorDesc.format = colorFormat;
        mColorDesc.width = width;
        mColorDesc.height = height;
        mColorDesc.arraySize = 1;
        mColorDesc.mipLevels = 1;
        mColorDesc.sampleCount = mMultiSampleCount;

//        ID3D11Device *pD3dDevice = CPUT_DX11::GetDevice();

        // If MSAA enabled, then create MSAA texture
        if( mMultiSampleCount>1 )
        {
            /*result = pD3dDevice->CreateTexture2D( &mColorDesc, NULL, &mpColorTextureDXMSAA );
            ASSERT( SUCCEEDED(result), _L("Failed creating MSAA render target texture") );*/
            mpColorTextureDXMSAA = TextureUtils.createTexture2D(mColorDesc, null);

            /*D3D11_SHADER_RESOURCE_VIEW_DESC srDesc = { colorFormat, D3D11_SRV_DIMENSION_TEXTURE2DMS, 0 };
            srDesc.Texture2D.MipLevels = 1;
            result = pD3dDevice->CreateShaderResourceView( mpColorTextureDXMSAA, &srDesc, &mpColorSRVMSAA );
            ASSERT( SUCCEEDED(result), _L("Failed creating MSAA render target shader resource view") );
            CPUTSetDebugName( mpColorSRVMSAA, textureName + _L(" ColorMSAA") );*/
            mpColorTextureDXMSAA.setName(textureName + " ColorMSAA");

            if( !recreate )
            {
                String msaaName = mName + "_MSAA";
                mpColorTextureMSAA = new CPUTTextureDX11( /*msaaName*/ );

                String finalName;
                // If the name starts with a '$', then its an internal texture (has no filesystem path).
                // Otherwise, its a file, so prepend filesystem path
                if( mName.charAt(0) == '$' )
                {
                    finalName = msaaName;
                }else
                {
//                    pServices->ResolveAbsolutePathAndFilename( (pAssetLibrary->GetTextureDirectory() + msaaName), &finalName);
                    finalName = pAssetLibrary.GetTextureDirectory() + msaaName;
                }
                pAssetLibrary.AddTexture( finalName, mpColorTextureMSAA );
            }
            ((CPUTTextureDX11)mpColorTextureMSAA).SetTextureAndShaderResourceView( mpColorTextureDXMSAA, mpColorTextureDXMSAA );
        }

        // Create non-MSAA texture.  If we're MSAA, then we'll resolve into this.  If not, then we'll render directly to this one.
        mColorDesc.sampleCount = 1;
        /*result = pD3dDevice->CreateTexture2D( &mColorDesc, NULL, &mpColorTextureDX );
        ASSERT( SUCCEEDED(result), _L("Failed creating render target texture") );*/
        mpColorTextureDX = TextureUtils.createTexture2D(mColorDesc, null);
        mpColorTextureDX.setName(textureName + (" Color"));

        // Create the shader-resource view from the non-MSAA texture
        /*D3D11_SHADER_RESOURCE_VIEW_DESC srDesc = { colorFormat, D3D11_SRV_DIMENSION_TEXTURE2D, 0 };
        srDesc.Texture2D.MipLevels = 1;
        result = pD3dDevice->CreateShaderResourceView( mpColorTextureDX, &srDesc, &mpColorSRV );
        ASSERT( SUCCEEDED(result), _L("Failed creating render target shader resource view") );
        CPUTSetDebugName( mpColorSRV, textureName + _L(" Color") );*/

        mHasUav = createUAV; // Remember, so we know to recreate it (or not) on RecreateRenderTarget()
        if( createUAV )
        {
            // D3D11_SHADER_RESOURCE_VIEW_DESC srDesc = { colorFormat, D3D_SRV_DIMENSION_BUFFER, 0 };
            /*D3D11_UNORDERED_ACCESS_VIEW_DESC uavDesc;
            memset( &uavDesc, 0, sizeof(uavDesc) );
            uavDesc.Format = colorFormat;
            uavDesc.ViewDimension = D3D11_UAV_DIMENSION_TEXTURE2D;
            uavDesc.Texture2D.MipSlice = 0;
            result = pD3dDevice->CreateUnorderedAccessView( mpColorTextureDX, &uavDesc, &mpColorUAV );
            ASSERT( SUCCEEDED(result), _L("Failed creating render target buffer shader resource view") );
            CPUTSetDebugName( mpColorUAV, textureName + _L(" Color Buffer") );*/
        }
        if( !recreate )
        {
            mpColorTexture = new CPUTTextureDX11(/*mName*/);
            pAssetLibrary.AddTexture( mName, mpColorTexture );

            mpColorBuffer = new CPUTBufferDX11(mName, /*NULL,*/ mpColorTextureDX); // We don't have an ID3D11Buffer, but we want to track this UAV as if the texture was a buffer.
            pAssetLibrary.AddBuffer( mName, mpColorBuffer );
        }
        ((CPUTTextureDX11)mpColorTexture).SetTextureAndShaderResourceView( mpColorTextureDX, mpColorTextureDX);

        // Choose our render target.  If MSAA, then use the MSAA texture, and use resolve to fill the non-MSAA texture.
        Texture2D pColorTexture = (mMultiSampleCount>1) ? mpColorTextureDXMSAA : mpColorTextureDX;
        mpColorRenderTargetView = pColorTexture;
        /*result = pD3dDevice->CreateRenderTargetView( pColorTexture, NULL, &mpColorRenderTargetView );
        ASSERT( SUCCEEDED(result), _L("Failed creating render target view") );
        CPUTSetDebugName( mpColorRenderTargetView, mName );*/
    }

    public void RecreateRenderTarget(
            int         width,
            int         height,
            int         colorFormat /*= DXGI_FORMAT_UNKNOWN*/,
            int         multiSampleCount /*= 1*/
    ){
        // We don't release these.  Instead, we release their resource views, and change them to the newly-created versions.
        // SAFE_RELEASE( mpColorTexture );
        SAFE_RELEASE( mpColorTextureDX );     mpColorTextureDX = null;
        SAFE_RELEASE( mpColorTextureDXMSAA ); mpColorTextureDXMSAA = null;

        // TODO: Complete buffer changes by including them here.
        // Do not release saved resource views since they are not addreff'ed
        //SAFE_RELEASE( mpSavedColorRenderTargetView );
        //SAFE_RELEASE( mpSavedDepthStencilView );
        CreateRenderTarget( mName, width, height, (0 != colorFormat) ? colorFormat : mColorFormat, multiSampleCount, mHasUav, true );
    }

    public void SetRenderTarget(
            CPUTRenderParameters renderParams,
            CPUTRenderTargetDepth pDepthBuffer /*= null*/,
            int renderTargetIndex/*=0*/,
            float[] pClearColor/*=NULL*/,
            boolean  clear /*= false*/,
            float zClearVal /*= 1.0f*/
    ){
        // ****************************
        // Save the current render target "state" so we can restore it later.
        // ****************************
        mSavedWidth   = CPUTRenderTargetColor.sCurrentWidth;
        mSavedHeight  = CPUTRenderTargetColor.sCurrentHeight;

        // Save the render target view so we can restore it later.
        mpSavedColorRenderTargetView = CPUTRenderTargetColor.GetActiveRenderTargetView();
        mpSavedDepthStencilView      = CPUTRenderTargetDepth.GetActiveDepthStencilView();

        CPUTRenderTargetColor.SetActiveWidthHeight( mWidth, mHeight );
        CPUTRenderTargetDepth.SetActiveWidthHeight( mWidth, mHeight );

        // TODO: support multiple render target views (i.e., MRT)
        CPUTRenderParametersDX pContext = ((CPUTRenderParametersDX)renderParams);

        // Make sure this render target isn't currently bound as a texture.
        /*static ID3D11ShaderResourceView *pSRV[16] = {0};
        pContext->PSSetShaderResources( 0, 16, pSRV );*/
        GLFuncProvider gl = GLFuncProviderFactory.getGLFuncProvider();
        for(int i = 0; i < 16; i++){
            gl.glBindTextureUnit(i, 0);
        }

        // Clear the shader resources to avoid a hazard warning
        /*ID3D11ShaderResourceView *pNullResources[16] = {0};
        pContext->PSSetShaderResources(0, 16, pNullResources );
        pContext->VSSetShaderResources(0, 16, pNullResources );*/

        // ****************************
        // Set the new render target states
        // ****************************
        Texture2D pDepthStencilView = pDepthBuffer != null? pDepthBuffer.GetDepthBufferView() : null;
        pContext.OMSetRenderTargets( /*1,*/ mpColorRenderTargetView, pDepthStencilView );

        CPUTRenderTargetColor.SetActiveRenderTargetView(mpColorRenderTargetView);
        CPUTRenderTargetDepth.SetActiveDepthStencilView(pDepthStencilView);

        if( clear )
        {
            pContext.ClearRenderTargetView( mpColorRenderTargetView, pClearColor );
            if( pDepthStencilView != null)
            {
                pContext.ClearDepthStencilView( pDepthStencilView, /*D3D11_CLEAR_DEPTH | D3D11_CLEAR_STENCIL*/
                        GLenum.GL_DEPTH_BUFFER_BIT|GLenum.GL_STENCIL_BUFFER_BIT, zClearVal, 0 );
            }
        }

        /*D3D11_VIEWPORT viewport  = { 0.0f, 0.0f, (float)mWidth, (float)mHeight, 0.0f, 1.0f };
        ((CPUTRenderParametersDX*)&renderParams)->mpContext->RSSetViewports( 1, &viewport );*/
        pContext.RSSetViewports(0,0, mWidth, mHeight);
        mRenderTargetSet = true;
    }

    public void RestoreRenderTarget( CPUTRenderParameters renderParams ){
        assert  mRenderTargetSet: "Render target restored without calling SetRenderTarget()";

        if( mMultiSampleCount>1 )
        {
            Resolve( renderParams );
        }

//        ID3D11DeviceContext *pContext = ((CPUTRenderParametersDX*)&renderParams)->mpContext;
        CPUTRenderParametersDX pContext = ((CPUTRenderParametersDX)renderParams);

        pContext.OMSetRenderTargets( /*1, &*/mpSavedColorRenderTargetView, mpSavedDepthStencilView );

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

    void Resolve( CPUTRenderParameters renderParams ){
        /*ID3D11DeviceContext *pContext = ((CPUTRenderParametersDX*)&renderParams)->mpContext;
        pContext->ResolveSubresource( mpColorTextureDX,  0, mpColorTextureDXMSAA, 0, mColorFormat);*/
        throw new UnsupportedOperationException();  // Later we will implement it.
    }

    public CPUTTexture          GetColorTexture()            { return mpColorTexture; }
    public CPUTBuffer           GetColorBuffer()             { return mpColorBuffer; }
    public Texture2D            GetColorResourceView()       { return mpColorTextureDX; }
    public Texture2D            GetColorUAV()                { return mpColorTextureDX; }
    public int                  GetWidth()                   { return mWidth; }
    public int                  GetHeight()                  { return mHeight; }
    public int                  GetColorFormat()             { return mColorFormat; }

    /*D3D11_MAPPED_SUBRESOURCE  MapRenderTarget(   CPUTRenderParameters &params, eCPUTMapType type, bool wait=true );
    void                      UnmapRenderTarget( CPUTRenderParameters &params );*/

    @Override
    public void dispose() {
        SAFE_RELEASE( mpColorTexture );   mpColorTexture = null;
        SAFE_RELEASE( mpColorBuffer );    mpColorBuffer  = null;
        SAFE_RELEASE( mpColorTextureDX ); mpColorTextureDX = null;
        SAFE_RELEASE( mpColorTextureDXMSAA );mpColorTextureDXMSAA = null;

        SAFE_RELEASE( mpColorTextureMSAA );  mpColorTextureMSAA = null;
        SAFE_RELEASE( mpColorTextureDXMSAA );  mpColorTextureDXMSAA = null;
    }
}
