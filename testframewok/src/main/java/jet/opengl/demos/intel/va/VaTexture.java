package jet.opengl.demos.intel.va;

import org.lwjgl.util.vector.Vector4f;
import org.lwjgl.util.vector.Vector4i;

import java.io.IOException;

import jet.opengl.postprocessing.common.Disposeable;
import jet.opengl.postprocessing.common.GLenum;
import jet.opengl.postprocessing.texture.Texture2D;
import jet.opengl.postprocessing.texture.Texture2DDesc;
import jet.opengl.postprocessing.texture.Texture3D;
import jet.opengl.postprocessing.texture.Texture3DDesc;
import jet.opengl.postprocessing.texture.TextureDataDesc;
import jet.opengl.postprocessing.texture.TextureGL;
import jet.opengl.postprocessing.texture.TextureUtils;

/**
 * Created by mazhen'gui on 2017/11/17.
 */
public abstract class VaTexture extends VaAssetResource implements VaRenderingModule, Disposeable {
    protected static final int                    c_fileVersion       = 1;

    /*enum class vaTextureBindSupportFlags : uint32
    {*/
    public static final int
            BSF_None                        = 0,
            BSF_VertexBuffer                = (1 << 0),
            BSF_IndexBuffer                 = (1 << 1),
            BSF_ConstantBuffer              = (1 << 2),
            BSF_ShaderResource              = (1 << 3),
            BSF_RenderTarget                = (1 << 4),
            BSF_DepthStencil                = (1 << 5),
            BSF_UnorderedAccess             = (1 << 6),

            // VA-specific
            BSF_CreateAutoMipViews          = (1 << 16)/*,
    }*/;

    private String                              m_renderingModuleTypeName;

    private /*vaTextureFlags*/int                      m_flags;

    private int /*vaTextureAccessFlags*/                m_accessFlags;
    private VaTextureType                       m_type;
    private int/*vaTextureBindSupportFlags*/           m_bindSupportFlags = BSF_None;

    private int                     m_resourceFormat;
    /*vaTextureFormat                     m_srvFormat;
    vaTextureFormat                     m_rtvFormat;
    vaTextureFormat                     m_dsvFormat;
    vaTextureFormat                     m_uavFormat;*/

    private int                                 m_sizeX;            // serves as desc.ByteWidth for Buffer
    private int                                 m_sizeY;            // doubles as ArraySize in 1D texture
    private int                                 m_sizeZ;            // doubles as ArraySize in 2D texture
    private int                                 m_sampleCount;
    private int                                 m_mipLevels;

    private int                                 m_viewedMipSlice;   // if m_viewedOriginal is nullptr, this will always be 0
    private int                                 m_viewedArraySlice; // if m_viewedOriginal is nullptr, this will always be 0
    private VaTexture                           m_viewedOriginal;
    private int                                 m_viewedSliceSizeX;
    private int                                 m_viewedSliceSizeY;
    private int                                 m_viewedSliceSizeZ;

    protected VaTexture(  VaConstructorParamsBase  params  ){
        m_bindSupportFlags      = /*vaTextureBindSupportFlags::None*/0;
        m_resourceFormat        = /*vaTextureFormat::Unknown*/0;
        /*m_srvFormat             = vaTextureFormat::Unknown;
        m_rtvFormat             = vaTextureFormat::Unknown;
        m_dsvFormat             = vaTextureFormat::Unknown;
        m_uavFormat             = vaTextureFormat::Unknown;*/

        m_accessFlags           = /*vaTextureAccessFlags::None*/0;
        m_type                  = VaTextureType.Unknown;

        m_sizeX                 = 0;
        m_sizeY                 = 0;
        m_sizeZ                 = 0;
        m_sampleCount           = 0;
        m_mipLevels             = 0;

        m_viewedSliceSizeX      = 0;
        m_viewedSliceSizeY      = 0;
        m_viewedSliceSizeZ      = 0;

        m_viewedMipSlice        = -1;
        m_viewedArraySlice      = -1;

        assert( VaRenderingCore.IsInitialized( ) );
    }

    protected final void Initialize(int binds, int resourceFormat){
        Initialize(binds, resourceFormat,-1,-1);
    }

    protected void Initialize(int binds, int resourceFormat /*= vaTextureFormat::Unknown,
                              vaTextureFormat srvFormat = vaTextureFormat::Unknown, vaTextureFormat rtvFormat = vaTextureFormat::Unknown,
                              vaTextureFormat dsvFormat = vaTextureFormat::Unknown, vaTextureFormat uavFormat = vaTextureFormat::Unknown,
                              vaTextureFlags flags = vaTextureFlags::None*/, int viewedMipSlice /*= -1*/, int viewedArraySlice /*= -1*/ ){
        m_bindSupportFlags  = binds;
        m_resourceFormat    = resourceFormat;
        /*m_srvFormat         = srvFormat;
        m_rtvFormat         = rtvFormat;
        m_dsvFormat         = dsvFormat;
        m_uavFormat         = uavFormat;*/
        m_flags             = /*flags*/0;
        m_viewedMipSlice    = viewedMipSlice;
        m_viewedArraySlice  = viewedArraySlice;

        // no point having format if no bind support - bind flag maybe forgotten?
        /*if( m_srvFormat != vaTextureFormat::Unknown )
        {
            assert( ( m_bindSupportFlags & vaTextureBindSupportFlags::ShaderResource ) != 0 );
        }
        if( m_rtvFormat != vaTextureFormat::Unknown )
        {
            assert( ( m_bindSupportFlags & vaTextureBindSupportFlags::RenderTarget ) != 0 );
        }
        if( m_dsvFormat != vaTextureFormat::Unknown )
        {
            assert( ( m_bindSupportFlags & vaTextureBindSupportFlags::DepthStencil ) != 0 );
        }
        if( m_uavFormat != vaTextureFormat::Unknown )
        {
            assert( ( m_bindSupportFlags & vaTextureBindSupportFlags::UnorderedAccess ) != 0 );
        }*/
    }

    protected void                            InternalUpdateFromRenderingCounterpart( boolean notAllBindViewsNeeded /*= false*/ ){
        VaTextureDX11 dx11Texture = (VaTextureDX11)this;

        m_type = VaTextureType.Unknown;
        TextureGL resource = dx11Texture.GetResource();
        if(resource == null){
            dx11Texture.Destroy();
            return;
        }

        if(resource instanceof Texture2D){
            Texture2D tex2D = (Texture2D)resource;
            m_sizeX             = tex2D.getWidth();
            m_sizeY             = tex2D.getHeight();
            m_mipLevels         = tex2D.getMipLevels();
            m_sizeZ             = tex2D.getArraySize();
            if( m_resourceFormat != /*vaTextureFormat::Unknown*/ 0 )
            { assert( m_resourceFormat == /*(vaTextureFormat)desc.Format*/tex2D.getFormat() ); }
            m_resourceFormat = /*(vaTextureFormat)desc.Format*/tex2D.getFormat();
            m_sampleCount       = tex2D.getSampleCount();
            //                  = desc.SampleDesc.Quality;

            if( /*desc.ArraySize*/tex2D.getArraySize() > 1 )
            {
                if( /*desc.SampleDesc.Count*/tex2D.getSampleCount() > 1 )
                    m_type = VaTextureType.Texture2DMSArray;
                else
                    m_type = VaTextureType.Texture2DArray;
            }
            else
            {
                if( /*desc.SampleDesc.Count*/tex2D.getSampleCount() > 1 )
                    m_type = VaTextureType.Texture2DMS;
                else
                    m_type = VaTextureType.Texture2D;
            }

            /*Usage               = desc.Usage;
            BindFlags           = desc.BindFlags;
            CPUAccessFlags      = desc.CPUAccessFlags;
            MiscFlags           = desc.MiscFlags;*/
        }else if(resource instanceof Texture3D){
            Texture3D tex3D = (Texture3D)resource;

            m_sizeX             = tex3D.getWidth();
            m_sizeY             = tex3D.getHeight();
            m_sizeZ             = tex3D.getDepth();
            m_mipLevels         = tex3D.getMipLevels();
            if( m_resourceFormat != /*vaTextureFormat::Unknown*/tex3D.getFormat() )
            { assert( m_resourceFormat == /*(vaTextureFormat)desc.Format*/tex3D.getFormat() ); }
            m_resourceFormat = /*(vaTextureFormat)desc.Format*/tex3D.getFormat();
            m_sampleCount       = 1;
            m_type              = VaTextureType.Texture3D;

            /*Usage               = desc.Usage;
            BindFlags           = desc.BindFlags;
            CPUAccessFlags      = desc.CPUAccessFlags;
            MiscFlags           = desc.MiscFlags;*/
        }

        m_viewedSliceSizeX = m_sizeX;
        m_viewedSliceSizeY = m_sizeY;
        m_viewedSliceSizeZ = m_sizeZ;
        for( int i = 0; i < m_viewedMipSlice; i++ )
        {
            m_viewedSliceSizeX = (m_viewedSliceSizeX+1) / 2;
            m_viewedSliceSizeY = (m_viewedSliceSizeY+1) / 2;
            m_viewedSliceSizeZ = (m_viewedSliceSizeZ+1) / 2;
        }
        m_viewedSliceSizeX = Math.max( m_viewedSliceSizeX, 1 );
        m_viewedSliceSizeY = Math.max( m_viewedSliceSizeY, 1 );
        m_viewedSliceSizeZ = Math.max( m_viewedSliceSizeZ, 1 );

        /*Usage;
        m_accessFlags = vaTextureAccessFlags::None;
        if( (CPUAccessFlags & D3D11_CPU_ACCESS_WRITE ) != 0 )
            m_accessFlags = m_accessFlags | vaTextureAccessFlags::CPUWrite;
        if( ( CPUAccessFlags & D3D11_CPU_ACCESS_READ ) != 0 )
            m_accessFlags = m_accessFlags | vaTextureAccessFlags::CPURead;
        // make sure bind flags were set up correctly
        if( !notAllBindViewsNeeded )
        {
            if( (BindFlags & D3D11_BIND_VERTEX_BUFFER       ) != 0 )
                assert( ( m_bindSupportFlags & vaTextureBindSupportFlags::VertexBuffer ) != 0 );
            if( (BindFlags & D3D11_BIND_INDEX_BUFFER        ) != 0 )
                assert( ( m_bindSupportFlags & vaTextureBindSupportFlags::IndexBuffer ) != 0 );
            if( (BindFlags & D3D11_BIND_CONSTANT_BUFFER     ) != 0 )
                assert( ( m_bindSupportFlags & vaTextureBindSupportFlags::ConstantBuffer ) != 0 );
            if( (BindFlags & D3D11_BIND_SHADER_RESOURCE     ) != 0 )
                assert( ( m_bindSupportFlags & vaTextureBindSupportFlags::ShaderResource ) != 0 );
            if( (BindFlags & D3D11_BIND_RENDER_TARGET       ) != 0 )
                assert( ( m_bindSupportFlags & vaTextureBindSupportFlags::RenderTarget ) != 0 );
            if( (BindFlags & D3D11_BIND_DEPTH_STENCIL       ) != 0 )
                assert( ( m_bindSupportFlags & vaTextureBindSupportFlags::DepthStencil ) != 0 );
            if( (BindFlags & D3D11_BIND_UNORDERED_ACCESS    ) != 0 )
                assert( ( m_bindSupportFlags & vaTextureBindSupportFlags::UnorderedAccess ) != 0 );
        }
        MiscFlags;*/
    }

    public static VaTexture              Import(String storagePath, boolean assumeSourceIsInSRGB, boolean dontAutogenerateMIPs) throws IOException{
        return Import(storagePath, assumeSourceIsInSRGB, dontAutogenerateMIPs, BSF_ShaderResource);
    }
    public static VaTexture              Import(String storagePath, boolean assumeSourceIsInSRGB, boolean dontAutogenerateMIPs,
                                                int binds /*= vaTextureBindSupportFlags::ShaderResource*/ ) throws IOException{
        /*assert( vaDirectXCore::GetDevice( ) != NULL ); // none of this works without a device
        if( vaDirectXCore::GetDevice( ) == NULL )
        return NULL;*/

        VaTexture texture = //VA_RENDERING_MODULE_CREATE_PARAMS( vaTexture, vaTextureConstructorParams( vaCore::GUIDCreate( ) ) );
                VaRenderingModuleRegistrar.CreateModuleTyped/*<ModuleTypeName>*/( "vaTexture", null );
        texture.Initialize( binds ,0,-1,-1);

        VaTextureDX11 dxTexture = (VaTextureDX11)(texture);

        if( dxTexture.ImportDX11( storagePath, assumeSourceIsInSRGB, dontAutogenerateMIPs, binds ) )
        {
            //texture->SetStoragePath( storageFilePath );
            // success!
            return texture;
        }
        else
        {
            // should probably load dummy checkerbox fall back texture here
            assert( false );
//            delete texture;
            return null;
        }
    }

    public static VaTexture Create1D(int format, int width, int mipLevels, int arraySize, int bindFlags, int accessFlags /*= vaTextureAccessFlags::None*/,
                                     Object initData/*= NULL*//*, vaTextureFormat srvFormat = vaTextureFormat::Unknown, vaTextureFormat rtvFormat = vaTextureFormat::Unknown,
                                     vaTextureFormat dsvFormat = vaTextureFormat::Unknown, vaTextureFormat uavFormat = vaTextureFormat::Unknown,
                                     vaTextureFlags flags = vaTextureFlags::None */){
        throw new UnsupportedOperationException();
    }
    public static VaTexture Create2D(int format, int width, int height, int mipLevels, int arraySize, int sampleCount, int bindFlags,
                                     int accessFlags /*= vaTextureAccessFlags::None*/, Object initialData /*= NULL, int initialDataPitch = 0,
                                     vaTextureFormat srvFormat = vaTextureFormat::Unknown, vaTextureFormat rtvFormat = vaTextureFormat::Unknown,
                                     vaTextureFormat dsvFormat = vaTextureFormat::Unknown, vaTextureFormat uavFormat = vaTextureFormat::Unknown,
                                     vaTextureFlags flags = vaTextureFlags::None*/ ){
        VaTexture texture = /*VA_RENDERING_MODULE_CREATE_PARAMS( vaTexture, vaTextureConstructorParams( vaCore::GUIDCreate( ) ) );*/
                VaRenderingModuleRegistrar.CreateModuleTyped("vaTexture", null);
        texture.Initialize( bindFlags, format/*, srvFormat, rtvFormat, dsvFormat, uavFormat, flags*/ );

        VaTextureDX11 dxTexture = /*vaSaferStaticCast<vaTextureDX11*>*/texture.SafeCast();

        /*D3D11_SUBRESOURCE_DATA dxInitDataObj;
        D3D11_SUBRESOURCE_DATA * dxInitDataPtr = NULL;
        if( initialData != NULL )
        {
            dxInitDataObj.pSysMem           = initialData;
            dxInitDataObj.SysMemPitch       = initialDataPitch;
            dxInitDataObj.SysMemSlicePitch  = 0;
            dxInitDataPtr = &dxInitDataObj;
        }
        // do we need an option to make this any other usage? staging will be needed likely
        D3D11_USAGE usage = DX11UsageFromVAAccessFlags( accessFlags );
        UINT miscFlags = 0;
        ID3D11Resource * resource = vaDirectXTools::CreateTexture2D( (DXGI_FORMAT)format, width, height, dxInitDataPtr, arraySize, mipLevels, BindFlagsDXFromVA(bindFlags), usage, CPUAccessFlagsDXFromVA(accessFlags), sampleCount, 0, miscFlags );
        */
        TextureDataDesc dxInitDataObj = new TextureDataDesc(TextureUtils.measureFormat(format), TextureUtils.measureDataType(format), initialData);
        Texture2DDesc texDesc = new Texture2DDesc(width, height, format);
        texDesc.mipLevels = mipLevels;
        Texture2D resource = TextureUtils.createTexture2D(texDesc, dxInitDataObj);
        if( resource != null )
        {
            dxTexture.SetResource( resource ,false);
            //texture->SetStoragePath( L"" );
            return texture;
        }
        else
        {
            /*delete texture;
            assert( false );
            return NULL;*/
            return null;
        }
    }
    public static VaTexture Create3D(int format, int width, int height, int depth, int mipLevels, int bindFlags, int accessFlags/* = vaTextureAccessFlags::None*/,
                                     Object  initialData /*= NULL, int initialDataPitch = 0, int initialDataSlicePitch = 0, vaTextureFormat srvFormat = vaTextureFormat::Unknown,
                                     vaTextureFormat rtvFormat = vaTextureFormat::Unknown, vaTextureFormat dsvFormat = vaTextureFormat::Unknown,
                                     vaTextureFormat uavFormat = vaTextureFormat::Unknown, vaTextureFlags flags = vaTextureFlags::None*/ ){
        VaTexture texture = /*VA_RENDERING_MODULE_CREATE_PARAMS( vaTexture, vaTextureConstructorParams( vaCore::GUIDCreate( ) ) );*/
                            VaRenderingModuleRegistrar.CreateModuleTyped("vaTexture", null);

        texture.Initialize( bindFlags, format/*, srvFormat, rtvFormat, dsvFormat, uavFormat, flags*/ );

        VaTextureDX11 dxTexture = /*vaSaferStaticCast<vaTextureDX11*>*/(VaTextureDX11) ( texture );

        /*D3D11_SUBRESOURCE_DATA dxInitDataObj;
        D3D11_SUBRESOURCE_DATA * dxInitDataPtr = NULL;
        if( initialData != NULL )
        {
            dxInitDataObj.pSysMem = initialData;
            dxInitDataObj.SysMemPitch = initialDataPitch;
            dxInitDataObj.SysMemSlicePitch = 0;
            dxInitDataPtr = &dxInitDataObj;
        }
        // do we need an option to make this any other usage? staging will be needed likely
        D3D11_USAGE usage = DX11UsageFromVAAccessFlags( accessFlags );
        UINT miscFlags = 0;
        ID3D11Resource * resource = vaDirectXTools::CreateTexture3D( (DXGI_FORMAT)format, width, height, depth, dxInitDataPtr, mipLevels, BindFlagsDXFromVA( bindFlags ), usage,
                CPUAccessFlagsDXFromVA( accessFlags ), miscFlags );*/
        TextureDataDesc dxInitDataObj = new TextureDataDesc(TextureUtils.measureFormat(format), TextureUtils.measureDataType(format), initialData);
        Texture3DDesc texDesc = new Texture3DDesc(width, height, depth, mipLevels, format);
        Texture3D resource = TextureUtils.createTexture3D(texDesc, dxInitDataObj);

        if( resource != null )
        {
            dxTexture.SetResource( resource,false );
            //texture->SetStoragePath( L"" );
            return texture;
        }
        else
        {
            /*delete texture;
            assert( false );
            return NULL;*/
            return null;
        }
    }

    public static VaTexture              Create2DTestCheckerboardTexture( int format, int bindFlags, int accessFlags /*= vaTextureAccessFlags::None*/ ){
        assert( format == /*vaTextureFormat::R8G8B8A8_UNORM_SRGB*/GLenum.GL_RGBA8);
        if( format != /*vaTextureFormat::R8G8B8A8_UNORM_SRGB*/GLenum.GL_RGBA8 )
            return null;

        final int dim = 32;
        int[] buffer = new int[dim*dim];
        int bufferPitch = /*sizeof( *buffer )*/4 * 32;
        for( int y = 0; y < dim; y++ )
        {
            for( int x = 0; x < dim; x++ )
            {
//                uint32 & pixel = buffer[dim*y + x];
                int pixel = 0;
                if( x < dim / 2 )
                {
                    if( y < dim / 2 )
                    {
                        if( ( x + y ) % 2 == 0 )
                            pixel = /*vaVector4::ToRGBA( vaVector4( 1.0f, 1.0f, 1.0f, 1.0f ) )*/ 0xFFFFFFFF;
                    else
                        pixel = /*vaVector4::ToRGBA( vaVector4( 0.0f, 0.0f, 0.0f, 1.0f ) )*/0xFF000000;
                    }
                    else
                    {
                        if( ( x + y ) % 2 == 0 )
                            pixel = /*vaVector4::ToRGBA( vaVector4( 1.0f, 1.0f, 1.0f, 1.0f ) )*/0xFFFFFFFF;
                    else
                        pixel = /*vaVector4::ToRGBA( vaVector4( 0.0f, 1.0f, 0.0f, 0.0f ) )*/0x0000FF00;
                    }
                }
                else
                {
                    if( y < dim / 2 )
                    {
                        if( ( x + y ) % 2 == 0 )
                            pixel = /*vaVector4::ToRGBA( vaVector4( 1.0f, 1.0f, 1.0f, 0.0f ) )*/0xFFFFFFFF;
                    else
                        pixel = /*vaVector4::ToRGBA( vaVector4( 1.0f, 0.0f, 0.0f, 1.0f ) )*/0xFF0000FF;
                    }
                    else
                    {
                        if( ( ( x * 8 / dim ) % 2 ) == ( ( y * 8 / dim ) % 2 ) )
                            pixel = /*vaVector4::ToRGBA( vaVector4( 1.0f, 1.0f, 1.0f, 1.0f ) )*/0xFFFFFFFF;
                    else
                        pixel = /*vaVector4::ToRGBA( vaVector4( 0.0f, 0.0f, 0.0f, 1.0f ) )*/0xFF000000;
                    }
                }
            }
        }

        return Create2D( /*vaTextureFormat::R8G8B8A8_UNORM_SRGB*/ GLenum.GL_RGBA8, dim, dim, 1, 1, 1, BSF_ShaderResource,
                /*vaTextureAccessFlags::None*/0, buffer/*, bufferPitch*/ );
    }

    public static VaTexture CreateView( VaTexture texture, int bindFlags, /*int srvFormat = vaTextureFormat::Unknown,
                                        vaTextureFormat rtvFormat = vaTextureFormat::Unknown, vaTextureFormat dsvFormat = vaTextureFormat::Unknown,
                                        vaTextureFormat uavFormat = vaTextureFormat::Unknown,*/ int viewedMipSlice /*= -1*/, int viewedArraySlice /*= -1*/ ){
        VaTextureDX11 origDX11Texture = /*vaSaferStaticCast<vaTextureDX11*>( texture.get() )*/texture.SafeCast();

        TextureGL resource = origDX11Texture.GetResource( );

        if( resource == null )
        {
            assert( false );
            return null;
        }

        // Can't request additional binding flags that were not supported in the original texture
        int origFlags = origDX11Texture.GetBindSupportFlags();
        assert( ((~origFlags) & bindFlags) == 0 );

        VaTexture newTexture = /*VA_RENDERING_MODULE_CREATE_PARAMS( vaTexture, vaTextureConstructorParams( vaCore::GUIDCreate( ) ) );*/
                VaRenderingModuleRegistrar.CreateModuleTyped("vaTexture", null);
        newTexture.Initialize( bindFlags, texture.GetResourceFormat(), /*srvFormat, rtvFormat, dsvFormat, uavFormat, texture->GetFlags(),*/ viewedMipSlice, viewedArraySlice );

        // it is debatable whether this is needed since DX resources have reference counting and will stay alive, but it might be useful for DX12 or other API implementations
        // so I'll leave it in
        newTexture.m_viewedOriginal = texture;

        VaTextureDX11 newDX11Texture = /*vaSaferStaticCast<vaTextureDX11*>( newTexture )*/newTexture.SafeCast();
//        resource->AddRef( );
        newDX11Texture.SetResource( resource, true );
        return newTexture;
    }

    public VaTextureType                   GetType( )                                                { return m_type; }
    public  int                            GetBindSupportFlags( )                                     { return m_bindSupportFlags; }
    public  int                            GetFlags( )                                                { return m_flags; }


    public int                      GetResourceFormat( )                                      { return m_resourceFormat; }
    public int                      GetSRVFormat( )                                            { return m_resourceFormat;      }
    public int                      GetDSVFormat( )                                            { return m_resourceFormat;      }
    public int                      GetRTVFormat( )                                            { return m_resourceFormat;      }
    public int                      GetUAVFormat( )                                            { return m_resourceFormat;      }

    public int                             GetSizeX( )                                                { return m_sizeX;       }     // serves as desc.ByteWidth for Buffer
    public int                             GetSizeY( )                                                { return m_sizeY;       }     // doubles as ArraySize in 1D texture
    public int                             GetSizeZ( )                                                { return m_sizeZ;       }     // doubles as ArraySize in 2D texture
    public int                             GetSampleCount( )                                          { return m_sampleCount; }
    public int                             GetMipLevels( )                                            { return m_mipLevels;   }

    public int                             GetViewedMipSlice( )                                       { return m_viewedMipSlice; }
    public int                             GetViewedSliceSizeX( )                                     { return m_viewedSliceSizeX; }
    public int                             GetViewedSliceSizeY( )                                     { return m_viewedSliceSizeY; }
    public int                             GetViewedSliceSizeZ( )                                     { return m_viewedSliceSizeZ; }

    public abstract void                    ClearRTV( /*vaRenderDeviceContext & context,*/ Vector4f clearValue )                                             ;
    public abstract void                    ClearUAV( /*vaRenderDeviceContext & context,*/ Vector4i clearValue )                                             ;
    public abstract void                    ClearUAV( /*vaRenderDeviceContext & context,*/ Vector4f clearValue )                                               ;
    public abstract void                    ClearDSV( /*vaRenderDeviceContext & context,*/ boolean clearDepth, float depthValue, boolean clearStencil, int stencilValue )  ;

    public abstract boolean                    Load( VaStream inStream )                                                                                             ;
    public  boolean                    Save( VaStream outStream ){
        throw new UnsupportedOperationException();
    }

    @Override
    public String GetRenderingModuleTypeName() {
        return m_renderingModuleTypeName;
    }

    public void InternalRenderingModuleSetTypeName(String name){
        m_renderingModuleTypeName = name;
    }
}
