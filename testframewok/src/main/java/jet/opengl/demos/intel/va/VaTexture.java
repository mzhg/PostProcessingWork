package jet.opengl.demos.intel.va;

import org.lwjgl.util.vector.Vector4f;
import org.lwjgl.util.vector.Vector4i;

import java.io.IOException;

import jet.opengl.postprocessing.common.Disposeable;
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
    public static final int
            Unknown	                    = 0,
            R32G32B32A32_TYPELESS       = 1,
            R32G32B32A32_FLOAT          = 2,
            R32G32B32A32_UINT           = 3,
            R32G32B32A32_SINT           = 4,
            R32G32B32_TYPELESS          = 5,
            R32G32B32_FLOAT             = 6,
            R32G32B32_UINT              = 7,
            R32G32B32_SINT              = 8,
            R16G16B16A16_TYPELESS       = 9,
            R16G16B16A16_FLOAT          = 10,
            R16G16B16A16_UNORM          = 11,
            R16G16B16A16_UINT           = 12,
            R16G16B16A16_SNORM          = 13,
            R16G16B16A16_SINT           = 14,
            R32G32_TYPELESS             = 15,
            R32G32_FLOAT                = 16,
            R32G32_UINT                 = 17,
            R32G32_SINT                 = 18,
            R32G8X24_TYPELESS           = 19,
            D32_FLOAT_S8X24_UINT        = 20,
            R32_FLOAT_X8X24_TYPELESS    = 21,
            X32_TYPELESS_G8X24_UINT     = 22,
            R10G10B10A2_TYPELESS        = 23,
            R10G10B10A2_UNORM           = 24,
            R10G10B10A2_UINT            = 25,
            R11G11B10_FLOAT             = 26,
            R8G8B8A8_TYPELESS           = 27,
            R8G8B8A8_UNORM              = 28,
            R8G8B8A8_UNORM_SRGB         = 29,
            R8G8B8A8_UINT               = 30,
            R8G8B8A8_SNORM              = 31,
            R8G8B8A8_SINT               = 32,
            R16G16_TYPELESS             = 33,
            R16G16_FLOAT                = 34,
            R16G16_UNORM                = 35,
            R16G16_UINT                 = 36,
            R16G16_SNORM                = 37,
            R16G16_SINT                 = 38,
            R32_TYPELESS                = 39,
            D32_FLOAT                   = 40,
            R32_FLOAT                   = 41,
            R32_UINT                    = 42,
            R32_SINT                    = 43,
            R24G8_TYPELESS              = 44,
            D24_UNORM_S8_UINT           = 45,
            R24_UNORM_X8_TYPELESS       = 46,
            X24_TYPELESS_G8_UINT        = 47,
            R8G8_TYPELESS               = 48,
            R8G8_UNORM                  = 49,
            R8G8_UINT                   = 50,
            R8G8_SNORM                  = 51,
            R8G8_SINT                   = 52,
            R16_TYPELESS                = 53,
            R16_FLOAT                   = 54,
            D16_UNORM                   = 55,
            R16_UNORM                   = 56,
            R16_UINT                    = 57,
            R16_SNORM                   = 58,
            R16_SINT                    = 59,
            R8_TYPELESS                 = 60,
            R8_UNORM                    = 61,
            R8_UINT                     = 62,
            R8_SNORM                    = 63,
            R8_SINT                     = 64,
            A8_UNORM                    = 65,
            R1_UNORM                    = 66,
            R9G9B9E5_SHAREDEXP          = 67,
            R8G8_B8G8_UNORM             = 68,
            G8R8_G8B8_UNORM             = 69,
            BC1_TYPELESS                = 70,
            BC1_UNORM                   = 71,
            BC1_UNORM_SRGB              = 72,
            BC2_TYPELESS                = 73,
            BC2_UNORM                   = 74,
            BC2_UNORM_SRGB              = 75,
            BC3_TYPELESS                = 76,
            BC3_UNORM                   = 77,
            BC3_UNORM_SRGB              = 78,
            BC4_TYPELESS                = 79,
            BC4_UNORM                   = 80,
            BC4_SNORM                   = 81,
            BC5_TYPELESS                = 82,
            BC5_UNORM                   = 83,
            BC5_SNORM                   = 84,
            B5G6R5_UNORM                = 85,
            B5G5R5A1_UNORM              = 86,
            B8G8R8A8_UNORM              = 87,
            B8G8R8X8_UNORM              = 88,
            R10G10B10_XR_BIAS_A2_UNORM  = 89,
            B8G8R8A8_TYPELESS           = 90,
            B8G8R8A8_UNORM_SRGB         = 91,
            B8G8R8X8_TYPELESS           = 92,
            B8G8R8X8_UNORM_SRGB         = 93,
            BC6H_TYPELESS               = 94,
            BC6H_UF16                   = 95,
            BC6H_SF16                   = 96,
            BC7_TYPELESS                = 97,
            BC7_UNORM                   = 98,
            BC7_UNORM_SRGB              = 99,
            AYUV                        = 100,
            Y410                        = 101,
            Y416                        = 102,
            NV12                        = 103,
            P010                        = 104,
            P016                        = 105,
            F420_OPAQUE                 = 106,
            YUY2                        = 107,
            Y210                        = 108,
            Y216                        = 109,
            NV11                        = 110,
            AI44                        = 111,
            IA44                        = 112,
            P8                          = 113,
            A8P8                        = 114,
            B4G4R4A4_UNORM              = 115,
    //        FORCE_UINT                  = 0xffffffff
            MaxVal                      = 116;

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
    private int                     m_srvFormat;
    private int                     m_rtvFormat;
    private int                     m_dsvFormat;
    private int                     m_uavFormat;

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
        super(((VaTextureConstructorParams)params).UID);
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
        Initialize(binds, resourceFormat,0,0,0,0,0,-1,-1);
    }

    protected void Initialize(int binds, int resourceFormat /*= vaTextureFormat::Unknown*/,
                              int srvFormat /*= vaTextureFormat::Unknown*/, int rtvFormat /*= vaTextureFormat::Unknown*/,
                              int dsvFormat /*= vaTextureFormat::Unknown*/, int uavFormat /*= vaTextureFormat::Unknown*/,
                              int flags /*= vaTextureFlags::None*/, int viewedMipSlice /*= -1*/, int viewedArraySlice /*= -1*/ ){
        m_bindSupportFlags  = binds;
        m_resourceFormat    = resourceFormat;
        m_srvFormat         = srvFormat;
        m_rtvFormat         = rtvFormat;
        m_dsvFormat         = dsvFormat;
        m_uavFormat         = uavFormat;
        m_flags             = flags;
        m_viewedMipSlice    = viewedMipSlice;
        m_viewedArraySlice  = viewedArraySlice;

        // no point having format if no bind support - bind flag maybe forgotten?
        if( m_srvFormat != Unknown )
        {
            assert( ( m_bindSupportFlags & BSF_ShaderResource ) != 0 );
        }
        if( m_rtvFormat != Unknown )
        {
            assert( ( m_bindSupportFlags & BSF_RenderTarget ) != 0 );
        }
        if( m_dsvFormat != Unknown )
        {
            assert( ( m_bindSupportFlags & BSF_DepthStencil ) != 0 );
        }
        if( m_uavFormat != Unknown )
        {
            assert( ( m_bindSupportFlags & BSF_UnorderedAccess ) != 0 );
        }
    }

    protected void InternalUpdateFromRenderingCounterpart( boolean notAllBindViewsNeeded /*= false*/ ){
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
            m_resourceFormat = /*(vaTextureFormat)desc.Format*/tex2D.getFormat(); // TODO Need conver the OpenGL format to vaTextureFormat
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
            m_resourceFormat = /*(vaTextureFormat)desc.Format*/tex3D.getFormat();  // TODO Need conver the OpenGL format to vaTextureFormat
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
            m_accessFlags = m_accessFlags | vaTextureAccessFlags::CPURead;*/
        // make sure bind flags were set up correctly
        /*if( !notAllBindViewsNeeded )
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
        }*/
//        MiscFlags;
    }

    public static VaTexture Import(String storagePath, boolean assumeSourceIsInSRGB, boolean dontAutogenerateMIPs) throws IOException{
        return Import(storagePath, assumeSourceIsInSRGB, dontAutogenerateMIPs, BSF_ShaderResource);
    }
    public static VaTexture Import(String storagePath, boolean assumeSourceIsInSRGB, boolean dontAutogenerateMIPs,
                                                int binds /*= vaTextureBindSupportFlags::ShaderResource*/ ) throws IOException{
        /*assert( vaDirectXCore::GetDevice( ) != NULL ); // none of this works without a device
        if( vaDirectXCore::GetDevice( ) == NULL )
        return NULL;*/

        VaTexture texture = //VA_RENDERING_MODULE_CREATE_PARAMS( vaTexture, vaTextureConstructorParams( vaCore::GUIDCreate( ) ) );
                VaRenderingModuleRegistrar.CreateModuleTyped/*<ModuleTypeName>*/( "vaTexture", null );
        texture.Initialize( binds ,0);

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

    public static VaTexture Create2D(int format, int width, int height, int mipLevels, int arraySize, int sampleCount, int bindFlags){
        return Create2D(format, width, height, mipLevels, arraySize, sampleCount, bindFlags, 0, null, 0, Unknown, Unknown, Unknown, Unknown, BSF_None);
    }

    public static VaTexture Create2D(int format, int width, int height, int mipLevels, int arraySize, int sampleCount, int bindFlags,
                                     int accessFlags /*= vaTextureAccessFlags::None*/, Object initialData /*= NULL*/, int initialDataPitch /*= 0*/,
                                     int srvFormat /*= vaTextureFormat::Unknown*/, int rtvFormat /*= vaTextureFormat::Unknown*/,
                                     int dsvFormat /*= vaTextureFormat::Unknown*/, int uavFormat /*= vaTextureFormat::Unknown*/,
                                     int flags /*= vaTextureFlags::None*/ ){
        VaTexture texture = /*VA_RENDERING_MODULE_CREATE_PARAMS( vaTexture, vaTextureConstructorParams( vaCore::GUIDCreate( ) ) );*/
                VaRenderingModuleRegistrar.CreateModuleTyped("vaTexture", new VaTextureConstructorParams());
        texture.Initialize( bindFlags, format, srvFormat, rtvFormat, dsvFormat, uavFormat, flags,-1,-1 );

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
        TextureDataDesc dxInitDataObj = null;
        if(initialData != null){
            dxInitDataObj = new TextureDataDesc(TextureUtils.measureFormat(format), TextureUtils.measureDataType(format), initialData);  // TODO Need convert the format to the other that the Opengl can accept.
        }

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
                                     Object  initialData /*= NULL*/, int initialDataPitch /*= 0*/, int initialDataSlicePitch /*= 0*/,
                                     int srvFormat /*= vaTextureFormat::Unknown*/,
                                     int rtvFormat /*= vaTextureFormat::Unknown*/, int dsvFormat /*= vaTextureFormat::Unknown*/,
                                     int uavFormat /*= vaTextureFormat::Unknown*/, int flags /*= vaTextureFlags::None*/ ){
        VaTexture texture = /*VA_RENDERING_MODULE_CREATE_PARAMS( vaTexture, vaTextureConstructorParams( vaCore::GUIDCreate( ) ) );*/
                            VaRenderingModuleRegistrar.CreateModuleTyped("vaTexture", new VaTextureConstructorParams());

        texture.Initialize( bindFlags, format, srvFormat, rtvFormat, dsvFormat, uavFormat, flags, -1, -1 );

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
        TextureDataDesc dxInitDataObj = null;
        if(initialData != null)
            dxInitDataObj = new TextureDataDesc(TextureUtils.measureFormat(format), TextureUtils.measureDataType(format), initialData);  // TODO Need convert the format to the other that the Opengl can accept.
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
        assert( format == R8G8B8A8_UNORM_SRGB);
        if( format != R8G8B8A8_UNORM_SRGB )
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

                buffer[dim*y + x] = pixel;
            }
        }

        return Create2D( R8G8B8A8_UNORM_SRGB, dim, dim, 1, 1, 1, BSF_ShaderResource,
                /*vaTextureAccessFlags::None*/0, buffer/*, bufferPitch*/, 0,0,0,0,0,0);
    }

    public static VaTexture CreateView( VaTexture texture, int bindFlags, int srvFormat /*= vaTextureFormat::Unknown*/,
                                        int rtvFormat /*= vaTextureFormat::Unknown*/, int dsvFormat /*= vaTextureFormat::Unknown*/,
                                        int uavFormat /*= vaTextureFormat::Unknown*/, int viewedMipSlice /*= -1*/, int viewedArraySlice /*= -1*/ ){
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
        newTexture.Initialize( bindFlags, texture.GetResourceFormat(), srvFormat, rtvFormat, dsvFormat, uavFormat, texture.GetFlags(), viewedMipSlice, viewedArraySlice );

        // it is debatable whether this is needed since DX resources have reference counting and will stay alive, but it might be useful for DX12 or other API implementations
        // so I'll leave it in
        newTexture.m_viewedOriginal = texture;

        VaTextureDX11 newDX11Texture = /*vaSaferStaticCast<vaTextureDX11*>( newTexture )*/newTexture.SafeCast();
//        resource->AddRef( );
        newDX11Texture.SetResource( resource, true );
        return newTexture;
    }

    public VaTextureType            GetType( )                                                { return m_type; }
    public  int                     GetBindSupportFlags( )                                     { return m_bindSupportFlags; }
    public  int                     GetFlags( )                                                { return m_flags; }


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

    public abstract void                    ClearRTV( /*vaRenderDeviceContext & context,*/ Vector4f clearValue );
    public abstract void                    ClearUAV( /*vaRenderDeviceContext & context,*/ Vector4i clearValue );
    public abstract void                    ClearUAV( /*vaRenderDeviceContext & context,*/ Vector4f clearValue );
    public abstract void                    ClearDSV( /*vaRenderDeviceContext & context,*/ boolean clearDepth, float depthValue, boolean clearStencil, int stencilValue );

    public abstract boolean  Load( VaStream inStream ) throws IOException;
    public  boolean          Save( VaStream outStream )  throws IOException{
        throw new UnsupportedOperationException();
    }

    @Override
    public String GetRenderingModuleTypeName() {
        return m_renderingModuleTypeName;
    }

    public void InternalRenderingModuleSetTypeName(String name){
        m_renderingModuleTypeName = name;
    }

    public final static int GetPixelSizeInBytes( int val )
    {
        switch( val )
        {
            case Unknown:                    return 0;
            case R32G32B32A32_TYPELESS:      return 4*4;
            case R32G32B32A32_FLOAT:         return 4*4;
            case R32G32B32A32_UINT:          return 4*4;
            case R32G32B32A32_SINT:          return 4*4;
            case R32G32B32_TYPELESS:         return 3*4;
            case R32G32B32_FLOAT:            return 3*4;
            case R32G32B32_UINT:             return 3*4;
            case R32G32B32_SINT:             return 3*4;
            case R16G16B16A16_TYPELESS:      return 4*2;
            case R16G16B16A16_FLOAT:         return 4*2;
            case R16G16B16A16_UNORM:         return 4*2;
            case R16G16B16A16_UINT:          return 4*2;
            case R16G16B16A16_SNORM:         return 4*2;
            case R16G16B16A16_SINT:          return 4*2;
            case R32G32_TYPELESS:            return 2*4;
            case R32G32_FLOAT:               return 2*4;
            case R32G32_UINT:                return 2*4;
            case R32G32_SINT:                return 2*4;
            case R32G8X24_TYPELESS:          return 4+1;
            case D32_FLOAT_S8X24_UINT:       return 4+1;
            case R32_FLOAT_X8X24_TYPELESS:   return 4+1;
            case X32_TYPELESS_G8X24_UINT:    return 4+1;
            case R10G10B10A2_TYPELESS:       return 4;
            case R10G10B10A2_UNORM:          return 4;
            case R10G10B10A2_UINT:           return 4;
            case R11G11B10_FLOAT:            return 4;
            case R8G8B8A8_TYPELESS:          return 4;
            case R8G8B8A8_UNORM:             return 4;
            case R8G8B8A8_UNORM_SRGB:        return 4;
            case R8G8B8A8_UINT:              return 4;
            case R8G8B8A8_SNORM:             return 4;
            case R8G8B8A8_SINT:              return 4;
            case R16G16_TYPELESS:            return 4;
            case R16G16_FLOAT:               return 4;
            case R16G16_UNORM:               return 4;
            case R16G16_UINT:                return 4;
            case R16G16_SNORM:               return 4;
            case R16G16_SINT:                return 4;
            case R32_TYPELESS:               return 4;
            case D32_FLOAT:                  return 4;
            case R32_FLOAT:                  return 4;
            case R32_UINT:                   return 4;
            case R32_SINT:                   return 4;
            case R24G8_TYPELESS:             return 4;
            case D24_UNORM_S8_UINT:          return 4;
            case R24_UNORM_X8_TYPELESS:      return 4;
            case X24_TYPELESS_G8_UINT:       return 4;
            case R8G8_TYPELESS:              return 2;
            case R8G8_UNORM:                 return 2;
            case R8G8_UINT:                  return 2;
            case R8G8_SNORM:                 return 2;
            case R8G8_SINT:                  return 2;
            case R16_TYPELESS:               return 2;
            case R16_FLOAT:                  return 2;
            case D16_UNORM:                  return 2;
            case R16_UNORM:                  return 2;
            case R16_UINT:                   return 2;
            case R16_SNORM:                  return 2;
            case R16_SINT:                   return 2;
            case R8_TYPELESS:                return 1;
            case R8_UNORM:                   return 1;
            case R8_UINT:                    return 1;
            case R8_SNORM:                   return 1;
            case R8_SINT:                    return 1;
            case A8_UNORM:                   return 1;
            case R1_UNORM:                   return 1;
            case R9G9B9E5_SHAREDEXP:         return 4;
            case R8G8_B8G8_UNORM:            return 4;
            case G8R8_G8B8_UNORM:            return 4;
            case BC1_TYPELESS:               assert( false ); return 0; // not supported for compressed formats
            case BC1_UNORM:                  assert( false ); return 0; // not supported for compressed formats
            case BC1_UNORM_SRGB:             assert( false ); return 0; // not supported for compressed formats
            case BC2_TYPELESS:               assert( false ); return 0; // not supported for compressed formats
            case BC2_UNORM:                  assert( false ); return 0; // not supported for compressed formats
            case BC2_UNORM_SRGB:             assert( false ); return 0; // not supported for compressed formats
            case BC3_TYPELESS:               assert( false ); return 0; // not supported for compressed formats
            case BC3_UNORM:                  assert( false ); return 0; // not supported for compressed formats
            case BC3_UNORM_SRGB:             assert( false ); return 0; // not supported for compressed formats
            case BC4_TYPELESS:               assert( false ); return 0; // not supported for compressed formats
            case BC4_UNORM:                  assert( false ); return 0; // not supported for compressed formats
            case BC4_SNORM:                  assert( false ); return 0; // not supported for compressed formats
            case BC5_TYPELESS:               assert( false ); return 0; // not supported for compressed formats
            case BC5_UNORM:                  assert( false ); return 0; // not supported for compressed formats
            case BC5_SNORM:                  assert( false ); return 0; // not supported for compressed formats
            case B5G6R5_UNORM:               return 2;
            case B5G5R5A1_UNORM:             return 2;
            case B8G8R8A8_UNORM:             return 4;
            case B8G8R8X8_UNORM:             return 4;
            case R10G10B10_XR_BIAS_A2_UNORM: return 4;
            case B8G8R8A8_TYPELESS:          return 4;
            case B8G8R8A8_UNORM_SRGB:        return 4;
            case B8G8R8X8_TYPELESS:          return 4;
            case B8G8R8X8_UNORM_SRGB:        return 4;
            case BC6H_TYPELESS:              assert( false ); return 0; // not supported for compressed formats
            case BC6H_UF16:                  assert( false ); return 0; // not supported for compressed formats
            case BC6H_SF16:                  assert( false ); return 0; // not supported for compressed formats
            case BC7_TYPELESS:               assert( false ); return 0; // not supported for compressed formats
            case BC7_UNORM:                  assert( false ); return 0; // not supported for compressed formats
            case BC7_UNORM_SRGB:             assert( false ); return 0; // not supported for compressed formats
            case AYUV:                       assert( false ); return 0; // not yet implemented
            case Y410:                       assert( false ); return 0; // not yet implemented
            case Y416:                       assert( false ); return 0; // not yet implemented
            case NV12:                       assert( false ); return 0; // not yet implemented
            case P010:                       assert( false ); return 0; // not yet implemented
            case P016:                       assert( false ); return 0; // not yet implemented
            case F420_OPAQUE:                assert( false ); return 0; // not yet implemented
            case YUY2:                       assert( false ); return 0; // not yet implemented
            case Y210:                       assert( false ); return 0; // not yet implemented
            case Y216:                       assert( false ); return 0; // not yet implemented
            case NV11:                       assert( false ); return 0; // not yet implemented
            case AI44:                       assert( false ); return 0; // not yet implemented
            case IA44:                       assert( false ); return 0; // not yet implemented
            case P8:                         return 1;
            case A8P8:                       return 2;
            case B4G4R4A4_UNORM:             return 2;
            default: break;
        }
        assert( false );
        return 0;
    }
}
