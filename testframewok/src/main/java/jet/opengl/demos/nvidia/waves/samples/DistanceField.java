package jet.opengl.demos.nvidia.waves.samples;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector2f;
import org.lwjgl.util.vector.Vector3f;
import org.lwjgl.util.vector.Vector4f;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import jet.opengl.postprocessing.common.Disposeable;
import jet.opengl.postprocessing.common.GLCheck;
import jet.opengl.postprocessing.common.GLFuncProvider;
import jet.opengl.postprocessing.common.GLFuncProviderFactory;
import jet.opengl.postprocessing.common.GLenum;
import jet.opengl.postprocessing.texture.AttachType;
import jet.opengl.postprocessing.texture.RenderTargets;
import jet.opengl.postprocessing.texture.Texture2D;
import jet.opengl.postprocessing.texture.Texture2DDesc;
import jet.opengl.postprocessing.texture.TextureAttachDesc;
import jet.opengl.postprocessing.texture.TextureUtils;
import jet.opengl.postprocessing.util.CacheBuffer;
import jet.opengl.postprocessing.util.CommonUtil;

/**
 * Created by mazhen'gui on 2017/7/27.
 */

final class DistanceField implements Disposeable, Constants{
    private static final int kTopDownDataResolution = 256;

// ---------------------------------- Not owned refrences ------------------------------------
    private CTerrainOcean m_pTerrainRenderer;	// Not owned.

    // ---------------------------------- GPU shading data ------------------------------------
    private Texture2D m_pTopDownDataSRV;
    private Texture2D		m_pTopDownDataRTV;
    private Texture2D			m_pTopDownDataTexture;
    private Texture2D			m_pStagingTexture;
    private RenderTargets m_RenderTarget;
    private TextureAttachDesc m_AttachDesc = new TextureAttachDesc();

    // ---------------------------------- Top down camera data ------------------------------------
    private final Vector4f m_topDownViewPositionWS = new Vector4f();
    private final Vector4f	m_viewDirectionWS = new Vector4f();
    private final Matrix4f	m_worldToViewMatrix = new Matrix4f();
    private final Matrix4f	m_viewToProjectionMatrix = new Matrix4f();

    private boolean m_shouldGenerateDataTexture = true;
    private GLFuncProvider gl;
    private IsParameters m_params;

    DistanceField( CTerrainOcean pTerrainRenderer, IsParameters params ){
        m_pTerrainRenderer = pTerrainRenderer;
        m_params = params;
    }

    void Init( /*ID3D11Device* const pDevice*/ ){
        gl = GLFuncProviderFactory.getGLFuncProvider();
        m_RenderTarget = new RenderTargets();

        if( null == m_pTopDownDataTexture )
        {
            Texture2DDesc textureDesc = new Texture2DDesc();
//            ZeroMemory(&textureDesc, sizeof(textureDesc));

            textureDesc.arraySize = 1;
//            textureDesc.BindFlags = D3D11_BIND_RENDER_TARGET | D3D11_BIND_SHADER_RESOURCE;
//            textureDesc.CPUAccessFlags = 0;
            textureDesc.format = GLenum.GL_RGBA32F; // DXGI_FORMAT_R32G32B32A32_FLOAT;
            textureDesc.height = kTopDownDataResolution;
            textureDesc.width = kTopDownDataResolution;
            textureDesc.mipLevels = 1;
//            textureDesc.MiscFlags = 0;
//            textureDesc.SampleDesc.Count = 1;
//            textureDesc.SampleDesc.Quality = 0;
//            textureDesc.Usage = D3D11_USAGE_DEFAULT;

//            V_RETURN( pDevice->CreateTexture2D( &textureDesc, nullptr, &m_pTopDownDataTexture ) );
            m_pTopDownDataRTV = m_pTopDownDataSRV = m_pTopDownDataTexture = TextureUtils.createTexture2D(textureDesc, null);

            textureDesc.arraySize = 1;
//            textureDesc.BindFlags = 0;
//            textureDesc.CPUAccessFlags = D3D11_CPU_ACCESS_WRITE | D3D11_CPU_ACCESS_READ;
            textureDesc.format = GLenum.GL_RGBA32F; //DXGI_FORMAT_R32G32B32A32_FLOAT;
            textureDesc.height = kTopDownDataResolution;
            textureDesc.width = kTopDownDataResolution;
            textureDesc.mipLevels = 1;
//            textureDesc.MiscFlags = 0;
//            textureDesc.SampleDesc.Count = 1;
//            textureDesc.SampleDesc.Quality = 0;
//            textureDesc.Usage = D3D11_USAGE_STAGING;

//            V_RETURN( pDevice->CreateTexture2D( &textureDesc, nullptr, &m_pStagingTexture ) );
            m_pStagingTexture = TextureUtils.createTexture2D(textureDesc, null);

//            D3D11_SHADER_RESOURCE_VIEW_DESC srvDesc;
//            ZeroMemory( &srvDesc, sizeof( srvDesc ) );
//            srvDesc.Format = textureDesc.Format;
//            srvDesc.Texture2D.MipLevels = 1;
//            srvDesc.ViewDimension = D3D11_SRV_DIMENSION_TEXTURE2D;
//
//            V_RETURN( pDevice->CreateShaderResourceView( m_pTopDownDataTexture, &srvDesc, &m_pTopDownDataSRV ) );
//
//            D3D11_RENDER_TARGET_VIEW_DESC rtvDesc;
//            ZeroMemory( &rtvDesc, sizeof( rtvDesc ) );
//            rtvDesc.Format = textureDesc.Format;
//            rtvDesc.ViewDimension = D3D11_RTV_DIMENSION_TEXTURE2D;
//
//            V_RETURN( pDevice->CreateRenderTargetView( m_pTopDownDataTexture, &rtvDesc, &m_pTopDownDataRTV ) );
        }
//        return S_OK;
    }

    // --------------------------------- Accessors -----------------------------------
    Texture2D	GetDataTextureSRV() { return m_pTopDownDataSRV; }

    void GetWorldToTopDownTextureMatrix(Matrix4f worldToTopDownMatrix){
        Matrix4f.mul(m_viewToProjectionMatrix, m_worldToViewMatrix, worldToTopDownMatrix);
    }

    // --------------------------------- Rendering routines -----------------------------------
    void GenerateDataTexture(/*ID3D11DeviceContext* pDC */){
        if( !m_shouldGenerateDataTexture ) return;

        renderTopDownData(/* pDC,*/ new Vector4f( 250, 0, 250, 0 ) );
        generateDistanceField( /*pDC*/ );

        m_shouldGenerateDataTexture = false;
    }

    void renderTopDownData(/*ID3D11DeviceContext* pDC,*/ Vector4f eyePositionWS){
        final float kHeightAboveSeaLevel = 300;
        final float kMinHeightBelowSeaLevel = 20;

//        D3D11_VIEWPORT vp;
//        UINT NumViewports = 1;
//        pDC->RSGetViewports(&NumViewports,&vp);
        int vx, vy, vw, vh;
        IntBuffer vp = CacheBuffer.getCachedIntBuffer(4);
        gl.glGetIntegerv(GLenum.GL_VIEWPORT, vp);
        vx = vp.get();
        vy = vp.get();
        vw = vp.get();
        vh = vp.get();

//        ID3D11RenderTargetView* pRenderTarget;
//        ID3D11DepthStencilView* pDepthBuffer;
//        pDC->OMGetRenderTargets( 1, &pRenderTarget, &pDepthBuffer );

        // Set the viewport
//        D3D11_VIEWPORT viewport;
//        ZeroMemory(&viewport, sizeof(D3D11_VIEWPORT));

//        viewport.TopLeftX = 0;
//        viewport.TopLeftY = 0;
//        viewport.Height = kTopDownDataResolution;
//        viewport.Width = kTopDownDataResolution;

//        float ClearColor[4] = { 0.0f, -kMinHeightBelowSeaLevel, 0.0f, 0.0f };
//        pDC->RSSetViewports(1, &viewport);
//        pDC->ClearRenderTargetView( m_pTopDownDataRTV, ClearColor );
//        pDC->OMSetRenderTargetsAndUnorderedAccessViews( 1, &m_pTopDownDataRTV, NULL, 0, 0, NULL, NULL );
        gl.glViewport(0,0, kTopDownDataResolution, kTopDownDataResolution);
        m_RenderTarget.bind();

        m_AttachDesc.index = 0;
        m_AttachDesc.layer = 0;
        m_AttachDesc.type = AttachType.TEXTURE_2D;
        m_AttachDesc.level = 0;
        m_RenderTarget.setRenderTexture(m_pTopDownDataRTV, m_AttachDesc);
        gl.glClearBufferfv(GLenum.GL_COLOR, 0, CacheBuffer.wrap(0.0f, -kMinHeightBelowSeaLevel, 0.0f, 0.0f));

        m_topDownViewPositionWS.set(eyePositionWS.x, kHeightAboveSeaLevel, eyePositionWS.z, 0);

        final float kOrthoSize = 700;
//        XMStoreFloat4x4(&m_viewToProjectionMatrix, XMMatrixOrthographicLH(kOrthoSize, kOrthoSize, 0.3f, kHeightAboveSeaLevel + kMinHeightBelowSeaLevel));
        Matrix4f.ortho(0, kOrthoSize, 0, kOrthoSize, 0.3f, kHeightAboveSeaLevel + kMinHeightBelowSeaLevel, m_viewToProjectionMatrix);

//        const XMVECTOR up = XMVectorSet( 0, 0, 1, 0 );
//        XMStoreFloat4x4(&m_worldToViewMatrix, XMMatrixLookAtLH(XMLoadFloat4(&m_topDownViewPositionWS), eyePositionWS, up));
        Matrix4f.lookAt(m_topDownViewPositionWS, eyePositionWS, Vector3f.Y_AXIS, m_worldToViewMatrix);

//        m_pTerrainRenderer.renderTerrainToHeightField( /*pDC,*/ m_worldToViewMatrix, m_viewToProjectionMatrix, m_topDownViewPositionWS, m_viewDirectionWS );
        // TODO setup the parameters.
        m_pTerrainRenderer.renderTerrainToHeightField(m_params);

//        pDC->RSSetViewports(NumViewports, &vp);
//        pDC->OMSetRenderTargetsAndUnorderedAccessViews( 1, &pRenderTarget, pDepthBuffer, 0, 0, NULL, NULL );
//        SAFE_RELEASE( pRenderTarget );
//        SAFE_RELEASE( pDepthBuffer );
        gl.glViewport(vx,vy,vw,vh);
        m_RenderTarget.unbind();
    }

    void generateDistanceField(/*ID3D11DeviceContext* pDC*/){
        float[] pTextureReadData = new float[kTopDownDataResolution * kTopDownDataResolution * 4];//sizeof(float));

//        pDC->CopyResource( m_pStagingTexture, m_pTopDownDataTexture );
//        gl.glCopyImageSubData(m_pTopDownDataTexture.getTexture(), m_pTopDownDataTexture.getTarget(), 0, 0,0,0,
//                m_pStagingTexture.getTexture(), m_pStagingTexture.getTarget(), 0,0,0,0,
//                m_pTopDownDataTexture.getWidth(), m_pTopDownDataTexture.getHeight(), 1);

        ByteBuffer mappedResource = TextureUtils.getTextureData(m_pTopDownDataTexture.getTarget(), m_pTopDownDataTexture.getTexture(), 0, true);
        mappedResource.asFloatBuffer().get(pTextureReadData);

//        D3D11_MAPPED_SUBRESOURCE mappedResource;
//        pDC->Map( m_pStagingTexture, 0, D3D11_MAP_READ_WRITE, 0, &mappedResource );
        {
//            memcpy( pTextureReadData, mappedResource.pData, kTopDownDataResolution * kTopDownDataResolution * 4*sizeof(float));

//            float* pTextureWriteData = reinterpret_cast<float*>( mappedResource.pData );
            float[] pTextureWriteData = new float[pTextureReadData.length];
            Vector2f gradient = new Vector2f();
            // Calculating the distance field to be stored in R channel
            // Seabed level is stored in G channel, leaving it intact
            for( int x=0 ; x<kTopDownDataResolution ; x++ )
            {
                for(int y=0 ; y<kTopDownDataResolution ; y++ )
                {
                    float gradientX, gradientY;
                    float distanceToNearestPixel = FindNearestPixel( pTextureReadData, x, y , gradient);
                    gradientX = gradient.x;
                    gradientY = gradient.y;
                    pTextureWriteData[ (x * kTopDownDataResolution + y) * 4 + 0 ] = distanceToNearestPixel;
                    pTextureWriteData[ (x * kTopDownDataResolution + y) * 4 + 2] = gradientY;
                    pTextureWriteData[ (x * kTopDownDataResolution + y) * 4 + 3] = gradientX;
                }
            }


            // now blurring the distance field a bit to smoothen the harsh edges, using channel B as temporaty storage,
            for( int x = 1 ; x < kTopDownDataResolution - 1 ; x++ )
            {
                for( int y = 1 ; y < kTopDownDataResolution - 1; y++ )
                {
                    pTextureWriteData[ (x * kTopDownDataResolution + y) * 4 + 2] = (pTextureWriteData[ ((x + 1)* kTopDownDataResolution + y + 0) * 4 + 0] +
                            pTextureWriteData[ ((x - 1)* kTopDownDataResolution + y + 0) * 4 + 0] +
                            pTextureWriteData[ ((x + 0)* kTopDownDataResolution + y - 1) * 4 + 0] +
                            pTextureWriteData[ ((x + 0)* kTopDownDataResolution + y + 1) * 4 + 0] +
                            pTextureWriteData[ ((x + 0)* kTopDownDataResolution + y + 1) * 4 + 0])*0.2f;
                }
            }
            for( int x = 1 ; x < kTopDownDataResolution - 1; x++ )
            {
                for( int y = 1 ; y < kTopDownDataResolution - 1; y++ )
                {
                    pTextureWriteData[ (x * kTopDownDataResolution + y) * 4 + 0] = (pTextureWriteData[ ((x + 1)* kTopDownDataResolution + y + 0) * 4 + 2] +
                            pTextureWriteData[ ((x - 1)* kTopDownDataResolution + y + 0) * 4 + 2] +
                            pTextureWriteData[ ((x + 0)* kTopDownDataResolution + y - 1) * 4 + 2] +
                            pTextureWriteData[ ((x + 0)* kTopDownDataResolution + y + 1) * 4 + 2] +
                            pTextureWriteData[ ((x + 0)* kTopDownDataResolution + y + 0) * 4 + 2])*0.2f;
                }
            }

            // calculating SDF gradients to be stored in B, A channels of the SDF texture

            for( int x = 1 ; x < kTopDownDataResolution - 1; x++ )
            {
                for( int y = 1 ; y < kTopDownDataResolution - 1; y++ )
                {
                    float value_center = pTextureWriteData[ ((x + 0)* kTopDownDataResolution + y + 0) * 4 + 0];
                    float value_left = pTextureWriteData[ ((x - 1)* kTopDownDataResolution + y + 0) * 4 + 0];
                    float value_right = pTextureWriteData[ ((x + 1)* kTopDownDataResolution + y + 0) * 4 + 0];
                    float value_bottom = pTextureWriteData[ ((x + 0)* kTopDownDataResolution + y - 1) * 4 + 0];
                    float value_top = pTextureWriteData[ ((x + 0)* kTopDownDataResolution + y + 1) * 4 + 0];
                    float gdx = value_right - value_left;
                    float gdy = value_top - value_bottom;
                    float length = (float) Math.sqrt(gdx*gdx + gdy*gdy + 0.001f);
                    gdx /= length;
                    gdy /= length;
                    pTextureWriteData[ (x * kTopDownDataResolution + y) * 4 + 2] = -gdy;
                    pTextureWriteData[ (x * kTopDownDataResolution + y) * 4 + 3] = gdx;
                }
            }

            FloatBuffer newPixels = CacheBuffer.wrap(pTextureWriteData);

//            int target, int level, int xoffset, int yoffset, int width, int height, int format, int type, Buffer pixels
            gl.glTexSubImage2D(m_pTopDownDataTexture.getTarget(), 0, 0,0, m_pTopDownDataTexture.getWidth(), m_pTopDownDataTexture.getHeight(),
                    GLenum.GL_RGBA, GLenum.GL_FLOAT, newPixels);
            GLCheck.checkError();

            gl.glBindTexture(GLenum.GL_TEXTURE_2D, 0);
        }

//        pDC->Unmap( m_pStagingTexture, 0 );
//        pDC->CopyResource( m_pTopDownDataTexture, m_pStagingTexture );
//        free(pTextureReadData);
    }

    boolean checkPixel( float[] pTextureData, final int cx, final int cy, final int dx, final int dy) {
        final int x = (cx+dx) < 0 ? 0 : (cx+dx) >= kTopDownDataResolution ? (kTopDownDataResolution-1) : (cx+dx);
        final int y = (cy+dy) < 0 ? 0 : (cy+dy) >= kTopDownDataResolution ? (kTopDownDataResolution-1) : (cy+dy);

        final int idx = (x * kTopDownDataResolution + y) * 4 + 0; // Red channel

        return pTextureData[ idx ] > 0.0f;
    }

    float FindNearestPixel( float[] pTextureData, final int cx, final int cy, Vector2f gradient){
        float gradientX = 0, gradientY = 0;
        final int kMaxDistance = 20;
        float minDistance = kMaxDistance;
        boolean originPositive = checkPixel( pTextureData, cx, cy, 0, 0);
        boolean resultPositive;
        for( int dx = -kMaxDistance ; dx <= kMaxDistance ; dx++ )
        {
            for( int dy = -kMaxDistance + 1 ; dy < kMaxDistance ; dy++ )
            {
                resultPositive = checkPixel( pTextureData, cx, cy, dx, dy);
                float pixelDistance = (float) Math.sqrt((float)(dx * dx + dy * dy));
                if((originPositive != resultPositive) && (pixelDistance < minDistance))
                {
                    minDistance = pixelDistance;
                    gradientX = dx / (pixelDistance+0.001f);
                    gradientY = dy/ (pixelDistance+0.001f);
                    if(!originPositive)
                    {
                        gradientX=-gradientX;
                        gradientY=-gradientY;
                    }
                }
            }
        }

        gradient.set(gradientX, gradientY);
        return originPositive ? -minDistance/kMaxDistance : minDistance/kMaxDistance;
    }

    @Override
    public void dispose() {
        CommonUtil.safeRelease( m_pTopDownDataSRV );
        CommonUtil.safeRelease( m_pTopDownDataRTV );
        CommonUtil.safeRelease( m_pTopDownDataTexture );
        CommonUtil.safeRelease( m_pStagingTexture );
    }
}
