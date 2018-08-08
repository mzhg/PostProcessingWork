package jet.opengl.demos.intel.oit;

import org.lwjgl.util.vector.Readable;
import org.lwjgl.util.vector.ReadableVector2f;
import org.lwjgl.util.vector.Vector2f;
import org.lwjgl.util.vector.Vector4f;

import java.io.IOException;
import java.nio.ByteBuffer;

import jet.opengl.demos.intel.cput.CPUTAssetLibrary;
import jet.opengl.demos.intel.cput.CPUTBufferDX11;
import jet.opengl.demos.intel.cput.CPUTMaterial;
import jet.opengl.demos.intel.cput.CPUTRenderParametersDX;
import jet.opengl.demos.intel.cput.CPUTTextureDX11;
import jet.opengl.postprocessing.buffer.BufferGL;
import jet.opengl.postprocessing.common.Disposeable;
import jet.opengl.postprocessing.common.GLenum;
import jet.opengl.postprocessing.texture.Texture2D;
import jet.opengl.postprocessing.texture.Texture2DDesc;
import jet.opengl.postprocessing.texture.TextureUtils;
import jet.opengl.postprocessing.util.CacheBuffer;
import jet.opengl.postprocessing.util.Numeric;

final class CPUTZoomBox implements Disposeable{
    private final Vector2f mvZoomCenterPos = new Vector2f(0.5f, 0.5f);
    private final Vector2f mvZoomSourceSize = new Vector2f(0.04f, 0.04f);
    private int            mZoomFactor = 8;
    private CPUTGeometrySprite					mpBorderSprite;
    private CPUTMaterial				mpBorderMaterial;
    private CPUTMaterial				mpZoomMaterial;
    private CPUTBufferDX11        mpZoomBoxConstants;
    private Texture2D       m_pZoomT;
    private Texture2D       m_pZoomSRV;
    private CPUTTextureDX11 mZoomTxT;

    private int             mScreenWidth;
    private int             mScreenHeight;

    private final ZoomBoxConstants pCB = new ZoomBoxConstants();

    void SetZoomCenterPosition(float x, float y){
        mvZoomCenterPos.x = x;
        mvZoomCenterPos.y = y;
    }

    void OnCreate(){
        CPUTAssetLibrary pAssetLibrary = CPUTAssetLibrary.GetAssetLibrary();

        /*D3D11_BUFFER_DESC bd = {0};
        bd.ByteWidth = sizeof(ZoomBoxConstants);
        bd.BindFlags = D3D11_BIND_CONSTANT_BUFFER;
        bd.Usage = D3D11_USAGE_DYNAMIC;
        bd.CPUAccessFlags = D3D11_CPU_ACCESS_WRITE;

        ID3D11Buffer * pOurConstants;
        hr = (CPUT_DX11::GetDevice())->CreateBuffer( &bd, NULL, &pOurConstants );*/
        BufferGL pOurConstants = new BufferGL();
        pOurConstants.initlize(GLenum.GL_ARRAY_BUFFER, ZoomBoxConstants.SIZE, null, GLenum.GL_DYNAMIC_DRAW);

        String name = "$cbZoomBoxConstants";
        mpZoomBoxConstants = new CPUTBufferDX11( name, pOurConstants );

        CPUTAssetLibrary.GetAssetLibrary().AddConstantBuffer( name, /*_L(""), _L(""),*/ mpZoomBoxConstants );

        CPUTMaterial.mGlobalProperties.AddValue( "cbZoomBoxConstants", "$cbZoomBoxConstants" );
        CPUTMaterial.mGlobalProperties.AddValue( "g_txZoomSrcColor", "$g_txZoomSrcColor" );
        CPUTMaterial.mGlobalProperties.AddValue( "g_txZoomSrcEdges", "$g_txZoomSrcEdges" );


        mpBorderSprite = new CPUTGeometrySprite();

        try {
            mpBorderSprite.CreateSprite( -1.0f, -1.0f, 2.0f, 2.0f, "ZoomBorder");
            mpBorderMaterial = pAssetLibrary.GetMaterial( "ZoomBorder", false );
            mpZoomMaterial = pAssetLibrary.GetMaterial( "Zoom" ,false);
        }catch (IOException e){
            e.printStackTrace();
        }
    }
    void    OnShutdown(){
        dispose();
    }
    void	OnFrameRender(CPUTRenderParametersDX renderParams , boolean showEdgeInfo ){
        BufferGL pBuffer = mpZoomBoxConstants.GetNativeBuffer();

//        D3D11_MAPPED_SUBRESOURCE MappedCB;
//        if( SUCCEEDED( renderParams.mpContext->Map(pBuffer, 0, D3D11_MAP_WRITE_DISCARD, 0, &MappedCB) ) )
        {
//            ZoomBoxConstants *pCB = (ZoomBoxConstants *) MappedCB.pData;
            pCB.ZoomCenterPos.x        = mvZoomCenterPos.x;
            pCB.ZoomCenterPos.y        = mvZoomCenterPos.y;

            int centerX                 = (int)( 0.5 + mvZoomCenterPos.x * mScreenWidth  );
            int centerY                 = (int)( 0.5 + mvZoomCenterPos.y * mScreenHeight );
            int halfSrcSizeX            = (int)( 0.5 + 0.5 * mvZoomSourceSize.x * mScreenWidth  );
            int halfSrcSizeY            = (int)( 0.5 + 0.5 * mvZoomSourceSize.y * mScreenHeight );

            pCB.ZoomSrcRectScreen.x    = (float)( centerX - halfSrcSizeX );
            pCB.ZoomSrcRectScreen.y    = (float)( centerY - halfSrcSizeY );
            pCB.ZoomSrcRectScreen.z    = (float)( centerX + halfSrcSizeX );
            pCB.ZoomSrcRectScreen.w    = (float)( centerY + halfSrcSizeY );

            Vector2f screenCenter     = new Vector2f( mScreenWidth * 0.5f, mScreenHeight * 0.5f );
            Vector2f screenSize       = new Vector2f( (float)mScreenWidth, (float)mScreenHeight );
            Vector2f srcRectSize      = new Vector2f( halfSrcSizeX*2.0f, halfSrcSizeY*2.0f );
            Vector2f srcRectCenter    = new Vector2f( (float)centerX, (float)centerY );
            Vector2f destRectSize  = new Vector2f( halfSrcSizeX*2.0f * mZoomFactor, halfSrcSizeY*2.0f  * mZoomFactor );
            Vector2f destRectCenter = new Vector2f();
            int srcDestDist = 100;
            destRectCenter.x = (srcRectCenter.x > screenCenter.x)?(srcRectCenter.x - srcRectSize.x * 0.5f - destRectSize.x * 0.5f - srcDestDist):(srcRectCenter.x + srcRectSize.x * 0.5f + destRectSize.x * 0.5f + srcDestDist);
            destRectCenter.y = Numeric.mix( destRectSize.y/2, screenSize.y - destRectSize.y/2, srcRectCenter.y / screenSize.y );

            pCB.ZoomDestRectScreen.x   = destRectCenter.x - destRectSize.x * 0.5f;
            pCB.ZoomDestRectScreen.y   = destRectCenter.y - destRectSize.y * 0.5f;
            pCB.ZoomDestRectScreen.z   = destRectCenter.x + destRectSize.x * 0.5f;
            pCB.ZoomDestRectScreen.w   = destRectCenter.y + destRectSize.y * 0.5f;

            pCB.ZoomSrcRectUV.x        = (pCB.ZoomSrcRectScreen.x + 0.5f) / (float)mScreenWidth;
            pCB.ZoomSrcRectUV.y        = (pCB.ZoomSrcRectScreen.y + 0.5f) / (float)mScreenHeight;
            pCB.ZoomSrcRectUV.z        = (pCB.ZoomSrcRectScreen.z + 0.5f) / (float)mScreenWidth;
            pCB.ZoomSrcRectUV.w        = (pCB.ZoomSrcRectScreen.w + 0.5f) / (float)mScreenHeight;

            pCB.ZoomDestRectUV.x       = (pCB.ZoomDestRectScreen.x + 0.5f) / (float)mScreenWidth;
            pCB.ZoomDestRectUV.y       = (pCB.ZoomDestRectScreen.y + 0.5f) / (float)mScreenHeight;
            pCB.ZoomDestRectUV.z       = (pCB.ZoomDestRectScreen.z + 0.5f) / (float)mScreenWidth;
            pCB.ZoomDestRectUV.w       = (pCB.ZoomDestRectScreen.w + 0.5f) / (float)mScreenHeight;

            pCB.ZoomShowEdges          = (showEdgeInfo)?(1.0f):(0.0f);

//            renderParams.mpContext->Unmap(pBuffer, 0);
            ByteBuffer buffer = CacheBuffer.getCachedByteBuffer(ZoomBoxConstants.SIZE);
            pCB.store(buffer).flip();
            pBuffer.update(0, buffer);
        }

        mpBorderSprite.DrawSprite(renderParams,mpZoomMaterial);
        mpBorderSprite.DrawSprite(renderParams,mpBorderMaterial);
    }

    void OnSize(int width, int height){
        CPUTAssetLibrary pAssetLibrary = CPUTAssetLibrary.GetAssetLibrary();

        mScreenWidth    = width;
        mScreenHeight   = height;

        /*D3D11_TEXTURE2D_DESC ZoomTDesc;
        ZeroMemory(&ZoomTDesc, sizeof(ZoomTDesc));
        ZoomTDesc.Width                = width;
        ZoomTDesc.Height               = height;
        ZoomTDesc.MipLevels            = 1;
        ZoomTDesc.ArraySize            = 1;
        ZoomTDesc.Format               = DXGI_FORMAT_R8G8B8A8_UNORM_SRGB;
        ZoomTDesc.SampleDesc.Count     = 1;
        ZoomTDesc.Usage                = D3D11_USAGE_DEFAULT;
        ZoomTDesc.CPUAccessFlags       = 0;
        ZoomTDesc.BindFlags            = D3D11_BIND_SHADER_RESOURCE;

        D3D11_SHADER_RESOURCE_VIEW_DESC RSVDesc;
        ZeroMemory(&RSVDesc, sizeof(RSVDesc));
        RSVDesc.ViewDimension             = D3D11_SRV_DIMENSION_TEXTURE2D;
        RSVDesc.Format                    = DXGI_FORMAT_R8G8B8A8_UNORM_SRGB;
        RSVDesc.Texture2D.MostDetailedMip = 0;
        RSVDesc.Texture2D.MipLevels       = 1;

        HRESULT hr;
        hr = pD3DDevice->CreateTexture2D(&ZoomTDesc, NULL, &m_pZoomT);
        hr = pD3DDevice->CreateShaderResourceView(m_pZoomT, &RSVDesc, &m_pZoomSRV);*/
        SAFE_RELEASE(m_pZoomT);

        Texture2DDesc zoomTDesc = new Texture2DDesc(width, height, GLenum.GL_RGBA8);
        m_pZoomSRV = m_pZoomT = TextureUtils.createTexture2D(zoomTDesc, null);

        if( mZoomTxT != null)
        {
            mZoomTxT.SetTextureAndShaderResourceView(m_pZoomT, m_pZoomSRV);
        }
        else
        {
            // make an internal/system-generated texture
            String name = ("$g_txZoomSrcColor");                                         // $ name indicates not loaded from file
            mZoomTxT = new CPUTTextureDX11(name,1);
            pAssetLibrary.AddTexture( name, mZoomTxT );

            // wrap the previously created objects
            mZoomTxT.SetTextureAndShaderResourceView(m_pZoomT, m_pZoomSRV);
        }


        /*ZeroMemory(&ZoomTDesc, sizeof(ZoomTDesc));
        ZoomTDesc.Width                = width;
        ZoomTDesc.Height               = height;
        ZoomTDesc.MipLevels            = 1;
        ZoomTDesc.ArraySize            = 1;
        ZoomTDesc.Format               = DXGI_FORMAT_R8_UNORM;
        ZoomTDesc.SampleDesc.Count     = 1;
        ZoomTDesc.Usage                = D3D11_USAGE_DEFAULT;
        ZoomTDesc.CPUAccessFlags       = 0;
        ZoomTDesc.BindFlags            = D3D11_BIND_SHADER_RESOURCE;

        ZeroMemory(&RSVDesc, sizeof(RSVDesc));
        RSVDesc.ViewDimension             = D3D11_SRV_DIMENSION_TEXTURE2D;
        RSVDesc.Format                    = DXGI_FORMAT_R8_UNORM;
        RSVDesc.Texture2D.MostDetailedMip = 0;
        RSVDesc.Texture2D.MipLevels       = 1;*/
    }

    ReadableVector2f GetCenterPos( )     { return mvZoomCenterPos; }

    Texture2D GetZoomColourTxtNoAddRef()    { return m_pZoomT; }

    @Override
    public void dispose() {
        SAFE_RELEASE( mpZoomBoxConstants );
        SAFE_RELEASE( mpBorderMaterial);
        SAFE_RELEASE( mpZoomMaterial);
        SAFE_RELEASE( mpBorderSprite );
        SAFE_RELEASE(m_pZoomT);
        SAFE_RELEASE(m_pZoomSRV);
        SAFE_RELEASE(mZoomTxT);
    }

    private static final class  ZoomBoxConstants implements Readable
    {
        static final int SIZE = 6 * Vector4f.SIZE;
        final Vector4f ZoomCenterPos = new Vector4f();

        final Vector4f ZoomSrcRectUV = new Vector4f();
        final Vector4f ZoomDestRectUV = new Vector4f();

        final Vector4f ZoomSrcRectScreen = new Vector4f();
        final Vector4f ZoomDestRectScreen = new Vector4f();

        float  ZoomScale;
        float  ZoomShowEdges;

        @Override
        public ByteBuffer store(ByteBuffer buf) {
            ZoomCenterPos.store(buf);

            ZoomSrcRectUV.store(buf);
            ZoomDestRectUV.store(buf);

            ZoomSrcRectScreen.store(buf);
            ZoomDestRectScreen.store(buf);

            buf.putFloat(ZoomScale);
            buf.putFloat(ZoomShowEdges);
            buf.putLong(0);  // dummy1
            return buf;
        }
//        final Vector2f dummy1 = new Vector4f();
    };
}
