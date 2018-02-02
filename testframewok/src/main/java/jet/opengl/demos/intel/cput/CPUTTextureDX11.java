package jet.opengl.demos.intel.cput;

import com.nvidia.developer.opengl.utils.NvImage;

import java.io.IOException;
import java.nio.ByteBuffer;

import jet.opengl.postprocessing.buffer.BufferGL;
import jet.opengl.postprocessing.common.GLFuncProvider;
import jet.opengl.postprocessing.common.GLFuncProviderFactory;
import jet.opengl.postprocessing.common.GLenum;
import jet.opengl.postprocessing.texture.Texture2D;
import jet.opengl.postprocessing.texture.TextureGL;
import jet.opengl.postprocessing.texture.TextureUtils;
import jet.opengl.postprocessing.util.FileUtils;

/**
 * Created by mazhen'gui on 2017/11/14.
 */

public final class CPUTTextureDX11 extends CPUTTexture{
    // resource view pointer
    /*ID3D11ShaderResourceView *mpShaderResourceView;
    ID3D11Resource           *mpTexture;
    ID3D11Resource           *mpTextureStaging;*/
    private TextureGL mpShaderResourceView;
    private TextureGL mpTexture;
    private BufferGL mpTextureStaging;

    @Override
    public void dispose() {
        SAFE_RELEASE( mpShaderResourceView );
        SAFE_RELEASE( mpTexture );
        SAFE_RELEASE( mpTextureStaging );
    }

    public CPUTTextureDX11(){}
    public CPUTTextureDX11(String name, int numArraySlices){
        super(name, numArraySlices);
    }

    public CPUTTextureDX11(String name, TextureGL pTextureResource, TextureGL pSrv, int numArraySlices/*=1*/ ){
        super(name, numArraySlices);
        mpShaderResourceView = pTextureResource;
        mpTexture = pSrv;
    }

    @Override
    public ByteBuffer MapTexture(CPUTRenderParameters params, CPUTMapType type, boolean wait) {
        return null;
    }

    @Override
    public void UnmapTexture(CPUTRenderParameters params) {

    }

    static String GetDXGIFormatString(int Format) { return TextureUtils.getFormatName(Format);}
    /*static CPUTResult     GetSRGBEquivalent(DXGI_FORMAT inFormat, DXGI_FORMAT& sRGBFormat);*/
    static boolean           DoesExistEquivalentSRGBFormat(int inFormat){
        String formatName = TextureUtils.getFormatName(inFormat);
        return formatName.contains("SNORM");
    }
    static CPUTTexture    CreateTextureDX11(String name, String absolutePathAndFilename, boolean loadAsSRGB ) throws IOException{
        // TODO:  Delegate to derived class.  We don't currently have CPUTTextureDX11
        /*ID3D11ShaderResourceView *pShaderResourceView = NULL;
        ID3D11Resource *pTexture = NULL;
        ID3D11Device *pD3dDevice= CPUT_DX11::GetDevice();
        CPUTResult result = CreateNativeTextureFromFile( *//*pD3dDevice,*//* absolutePathAndFilename, &pShaderResourceView, &pTexture, loadAsSRGB );
        ASSERT( CPUTSUCCESS(result), _L("Error loading texture: '")+absolutePathAndFilename );*/
        Texture2D pTexture = CreateNativeTextureFromFile(absolutePathAndFilename, loadAsSRGB);

        CPUTTextureDX11 pNewTexture = new CPUTTextureDX11();
        pNewTexture.mName = name;
        pNewTexture.SetTextureAndShaderResourceView( pTexture, pTexture );
        /*pTexture->Release();
        pShaderResourceView->Release();*/

        CPUTAssetLibrary.GetAssetLibrary().AddTexture( absolutePathAndFilename, pNewTexture);

        return pNewTexture;
    }

    static CPUTTexture   CreateTextureArrayFromFilenameListDX11( CPUTRenderParameters renderParams, String absolutePathAndFilename, String []pFilenameList, boolean loadAsSRGB ) throws IOException{
        CPUTAssetLibraryDX11 pAssetLibrary = (CPUTAssetLibraryDX11)CPUTAssetLibrary.GetAssetLibrary();

        // TODO:  Delegate to derived class.  We don't currently have CPUTTextureDX11
        /*ID3D11ShaderResourceView *pShaderResourceView = NULL;
        ID3D11Resource *pTexture = NULL;
        ID3D11Device *pD3dDevice = CPUT_DX11::GetDevice();*/
        Texture2D pShaderResourceView = null;
        Texture2D pTexture = null;

        int  indexFirstRealTexture = -1;
        int numArraySlices;
        for( numArraySlices=0; /*pFilenameList[numArraySlices]*/ numArraySlices < pFilenameList.length; numArraySlices++ )
        {
            // This loop looks for the first NULL entry in the filename list.
            // When complete, numArraySlices contains the index of that first null entry
            if( indexFirstRealTexture == -1 && pFilenameList[numArraySlices].compareTo("$Undefined") !=0)
            {
                indexFirstRealTexture = numArraySlices;
            }
        }
        if( indexFirstRealTexture == -1 )
        {
            // No real textures specified (i.e., all == Undefined)
            return null;
        }

        // Get information we need from first slice texture.  We need information like the texture format.
        // Note that all slices must have the same dimensions, type, format, etc.
        /*D3DX11_IMAGE_INFO srcInfo;
        HRESULT hr = D3DX11GetImageInfoFromFile( pFilenameList[indexFirstRealTexture]->c_str(), NULL, &srcInfo, NULL);
        ASSERT( SUCCEEDED(hr), _L(" - Error loading texture '") + *pFilenameList[indexFirstRealTexture] + _L("'.") );*/


        CPUTTextureDX11 pNewTexture = new CPUTTextureDX11( absolutePathAndFilename, numArraySlices ); // TODO: Move numArraySlices to Create* call
        /*CPUTResult result = pNewTexture.CreateNativeTexture( pD3dDevice, absolutePathAndFilename, srcInfo, &pShaderResourceView, &pTexture, loadAsSRGB );
        ASSERT( CPUTSUCCESS(result), _L("Error creating texture array: '") + absolutePathAndFilename );*/
        pShaderResourceView = pTexture = pNewTexture.CreateNativeTexture(pFilenameList[indexFirstRealTexture], /*&pShaderResourceView, &pTexture, */loadAsSRGB );

        pNewTexture.SetTextureAndShaderResourceView( pTexture, pShaderResourceView );
        /*pTexture->Release();
        pShaderResourceView->Release();*/

        GLFuncProvider gl = GLFuncProviderFactory.getGLFuncProvider();
        int numMipLevels = /*srcInfo.MipLevels*/pTexture.getMipLevels();
        for( int ii=0; ii<numArraySlices; ii++ )
        {
            if(pFilenameList[ii].compareTo("$Undefined") == 0 )
            {
                // No texture for this slice.
                // TODO: use indirection table to avoid this, and to avoid duplicating slices
                continue;
            }

            for( int mip=0; mip</*srcInfo.MipLevels*/pTexture.getMipLevels(); mip++ )
            {
                // Get the texture by name
                CPUTTextureDX11 pTexture11 = (CPUTTextureDX11)pAssetLibrary.GetTexture( pFilenameList[ii], true, loadAsSRGB );
                /*ASSERT( pTexture, _L("Error getting texture ") + *pFilenameList[ii] );*/
                if(pTexture11 == null)
                    throw new RuntimeException("Error getting texture " + pFilenameList[ii]);

/*#ifdef _DEBUG
                // TODO: Add GetNumMipLevels() to CPUTTexture
                D3DX11GetImageInfoFromFile( pFilenameList[ii]->c_str(), NULL, &srcInfo, NULL);
                ASSERT( numMipLevels == srcInfo.MipLevels, _L("Array textures must have the same MIP count") );
#endif*/
                // Set this texture to our array texture at slice ii
                int srcSubresource  = D3D11CalcSubresource( mip, 0,  numMipLevels );
                int destSubresource = D3D11CalcSubresource( mip, ii, numMipLevels );
                /*renderParams.mpContext->CopySubresourceRegion(
                    pNewTexture->mpTexture,
                    destSubresource,
                    0, 0, 0,
                    pTexture->mpTexture,
                    srcSubresource,
                    NULL
                );*/
                gl.glCopyImageSubData(pTexture11.mpTexture.getTexture(), pTexture11.mpTexture.getTarget(), srcSubresource,0,0,0,
                        pNewTexture.mpTexture.getTexture(), pNewTexture.mpTexture.getTarget(), destSubresource,0,0,0,
                        pTexture11.mpTexture.getWidth(), pTexture11.mpTexture.getHeight(), 1);
            }
        }
        return pNewTexture;
    }

    static int D3D11CalcSubresource( int MipSlice, int ArraySlice, int MipLevels )
    { return MipSlice + ArraySlice * MipLevels; }

    //-----------------------------------------------------------------------------
    static int GetSRGBEquivalent(int inFormat/*, DXGI_FORMAT& sRGBFormat*/)
    {
        switch( inFormat )
        {
            case GLenum.GL_RGBA8:
            case GLenum.GL_RGBA8_SNORM:
                return GLenum.GL_RGBA8_SNORM;
            /*case DXGI_FORMAT_B8G8R8X8_UNORM:
            case DXGI_FORMAT_B8G8R8X8_UNORM_SRGB:
                sRGBFormat = DXGI_FORMAT_B8G8R8X8_UNORM_SRGB;
                return CPUT_SUCCESS;
            case DXGI_FORMAT_BC1_UNORM:
            case DXGI_FORMAT_BC1_UNORM_SRGB:
                sRGBFormat = DXGI_FORMAT_BC1_UNORM_SRGB;
                return CPUT_SUCCESS;
            case DXGI_FORMAT_BC2_UNORM:
            case DXGI_FORMAT_BC2_UNORM_SRGB:
                sRGBFormat = DXGI_FORMAT_BC2_UNORM_SRGB;
                return CPUT_SUCCESS;
            case DXGI_FORMAT_BC3_UNORM:
            case DXGI_FORMAT_BC3_UNORM_SRGB:
                sRGBFormat = DXGI_FORMAT_BC3_UNORM_SRGB;
                return CPUT_SUCCESS;
            case DXGI_FORMAT_BC7_UNORM:
            case DXGI_FORMAT_BC7_UNORM_SRGB:
                sRGBFormat = DXGI_FORMAT_BC7_UNORM_SRGB;
                return CPUT_SUCCESS;*/
        };
        /*return CPUT_ERROR_UNSUPPORTED_SRGB_IMAGE_FORMAT;*/
        throw new IllegalArgumentException();
    }

    /*static CPUTTexture   *CreateTextureArrayFromTxaFile( CPUTRenderParametersDX &renderParams, cString &absolutePathAndFilename, bool loadAsSRGB );*/
    static Texture2D     CreateNativeTextureFromFile(
//            ID3D11Device *pD3dDevice,
                              String fileName,
                              /*ID3D11ShaderResourceView **ppShaderResourceView,
                              ID3D11Resource **ppTexture,*/
            boolean forceLoadAsSRGB
    )throws IOException{
        if(fileName.endsWith("dds") || fileName.endsWith("DDS")){
            NvImage image = new NvImage();
            image.loadImageFromFile(fileName);
            int textureID = image.updaloadTexture();
            int target = image.isCubeMap() ? GLenum.GL_TEXTURE_CUBE_MAP : image.isVolume() ? GLenum.GL_TEXTURE_3D : GLenum.GL_TEXTURE_2D;
            Texture2D texture = TextureUtils.createTexture2D(target, textureID);
            texture.setName(FileUtils.getFile(fileName));
            return texture;
        }else{
            return TextureUtils.createTexture2DFromFile(fileName, true);
        }
    }

    void ReleaseTexture()
    {
        SAFE_RELEASE(mpShaderResourceView);
        SAFE_RELEASE(mpTexture);
    }
    void SetTexture(TextureGL pTextureResource, TextureGL pSrv )
    {
        mpShaderResourceView = pSrv;
//        if(mpShaderResourceView) pSrv->AddRef();

        mpTexture = pTextureResource;
//        if(mpTexture) mpTexture->AddRef();
    }

    TextureGL GetShaderResourceView()
    {
        return mpShaderResourceView;
    }

    public void SetTextureAndShaderResourceView(TextureGL pTexture, TextureGL pShaderResourceView)
    {
        // release any resources we might already be pointing too
        SAFE_RELEASE( mpTexture );
        SAFE_RELEASE( mpTextureStaging ); // Now out-of sync.  Will be recreated on next Map().
        SAFE_RELEASE( mpShaderResourceView );
        mpTexture = pTexture;
//        if( mpTexture ) mpTexture->AddRef();
        mpShaderResourceView = pShaderResourceView;
//        mpShaderResourceView->AddRef();
    }
    Texture2D CreateNativeTexture(
//            ID3D11Device *pD3dDevice,
                    String fileName,
//            D3DX11_IMAGE_INFO &info,
//            ID3D11ShaderResourceView **ppShaderResourceView,
//            ID3D11Resource **ppTexture,
            boolean forceLoadAsSRGB
    ){
        int format = /*srcInfo.Format*/ GLenum.GL_RGBA8;
        if( forceLoadAsSRGB )
        {
            format = GetSRGBEquivalent(/*srcInfo.Format, */format);
        }
//        DXGI_SAMPLE_DESC sampleDesc = {1,0};
        /*D3D11_TEXTURE2D_DESC desc;  TODO
        desc.Width          = srcInfo.Width;
        desc.Height         = srcInfo.Height;
        desc.MipLevels      = srcInfo.MipLevels;
        desc.ArraySize      = mNumArraySlices;
        desc.Format         = format;
        desc.SampleDesc     = sampleDesc;
        desc.Usage          = D3D11_USAGE_DEFAULT;
        desc.BindFlags      = D3D11_BIND_SHADER_RESOURCE; // TODO: Support UAV, etc.  Accept flags as argument?
        desc.CPUAccessFlags = 0;
        desc.MiscFlags      = srcInfo.MiscFlags;

        hr = pD3dDevice->CreateTexture2D( &desc, NULL, (ID3D11Texture2D**)ppTexture );
        ASSERT( SUCCEEDED(hr), _L("Failed to create texture array: ") + fileName );
        CPUTSetDebugName( *ppTexture, fileName );

        hr = pD3dDevice->CreateShaderResourceView( *ppTexture, NULL, ppShaderResourceView );
        ASSERT( SUCCEEDED(hr), _L("Failed to create texture shader resource view.") );
        CPUTSetDebugName( *ppShaderResourceView, fileName );*/

        throw new UnsupportedOperationException();
    }
}
