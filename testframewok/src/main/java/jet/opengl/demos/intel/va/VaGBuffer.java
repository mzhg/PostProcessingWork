package jet.opengl.demos.intel.va;

import com.nvidia.developer.opengl.utils.Holder;

import org.lwjgl.util.vector.Vector2i;

/**
 * A helper for GBuffer rendering - does creation and updating of render target textures and provides debug views of them.<p></p>
 * Created by mazhen'gui on 2017/11/18.
 */

public abstract class VaGBuffer extends VaImguiHierarchyObject implements VaRenderingModule {
    private String m_renderingModuleTypeName;

    protected String                                m_debugInfo;
    protected int                                   m_debugSelectedTexture;

    protected final BufferFormats                   m_formats = new BufferFormats();

    protected final Holder<VaTexture>               m_depthBuffer = new Holder<>();                      // just a regular depth buffer
    protected final Holder<VaTexture>               m_depthBufferViewspaceLinear = new Holder<>();       // regular depth buffer converted to viewspace
    protected final Holder<VaTexture>               m_normalMap = new Holder<>();                        // screen space normal map
    protected final Holder<VaTexture>               m_albedo = new Holder<>();                           // material color plus whatever else
    protected final Holder<VaTexture>               m_radiance = new Holder<>();                         // a.k.a. light accumulation, a.k.a. screen color - final lighting output goes here as well as emissive materials
    protected final Holder<VaTexture>               m_outputColor = new Holder<>();                      // final output color

    // Light Pre-Pass
    // protected VaTexture                             m_diffuseIrradiance;        // placeholder for Light Pre-Pass
    // protected VaTexture                             m_specularIrradiance;       // placeholder for Light Pre-Pass

    protected final Vector2i                        m_resolution = new Vector2i();

    protected VaGBuffer( ){
        m_debugInfo = "GBuffer (uninitialized - forgot to call RenderTick?)";
        m_debugSelectedTexture = -1;
    }
    /*public:
    virtual ~vaGBuffer( );

    private:
    friend class vaGBufferDX11;*/

    public void                                         SetFormats(BufferFormats newFormats ){
        m_formats.set(newFormats);
    }
    public BufferFormats                                GetFormats( )                      { return m_formats; }

    public VaTexture GetDepthBuffer( )                  { return m_depthBuffer.get();              }
    public VaTexture GetDepthBufferViewspaceLinear( )   { return m_depthBufferViewspaceLinear.get();}
    public VaTexture GetNormalMap( )                    { return m_normalMap.get();                }
    public VaTexture GetAlbedo( )                       { return m_albedo.get();                   }
    public VaTexture GetRadiance( )                     { return m_radiance.get();                 }
    public VaTexture GetOutputColor( )                  { return m_outputColor.get();              }
    public Vector2i  GetResolution( )                   { return m_resolution; }

    /**
     * if viewportWidth/Height are -1, take them from drawContext.Canvas.GetViewport
     * @param width
     * @param height
     */
    public abstract void                            UpdateResources( VaDrawContext drawContext, int width /*= -1*/, int height /*= -1*/ );
    public final void UpdateResources( VaDrawContext drawContext) { UpdateResources(drawContext, -1,-1);}

    public abstract void RenderDebugDraw( VaDrawContext drawContext );

    // draws provided depthTexture (can be the one obtained using GetDepthBuffer( )) into currently selected RT; relies on settings set in vaRenderingGlobals and will assert and return without doing anything if those are not present
    public abstract void DepthToViewspaceLinear( VaDrawContext drawContext, VaTexture depthTexture );

    protected String IHO_GetInstanceInfo( ) { return m_debugInfo; }
    protected void                                    IHO_Draw( ){
        /*struct TextureInfo
        {
            string                  Name;
            shared_ptr<vaTexture>   Texture;
        };

        std::vector< TextureInfo > textures;

        textures.push_back( { "Depth Buffer", m_depthBuffer } );
        textures.push_back( { "Depth Buffer Viewspace Linear", m_depthBufferViewspaceLinear } );
        textures.push_back( { "Normal Map", m_normalMap } );
        textures.push_back( { "Albedo", m_albedo } );
        textures.push_back( { "Radiance", m_radiance} );
        textures.push_back( { "OutputColor", m_outputColor} );

        for( size_t i = 0; i < textures.size(); i++ )
        {
            if( ImGui::Selectable( textures[i].Name.c_str(), m_debugSelectedTexture == i ) )
            {
                if( m_debugSelectedTexture == i )
                    m_debugSelectedTexture = -1;
                else
                    m_debugSelectedTexture = (int)i;
            }
        }*/
    }

    @Override
    public String GetRenderingModuleTypeName() {
        return m_renderingModuleTypeName;
    }

    @Override
    public void InternalRenderingModuleSetTypeName(String name) {
        m_renderingModuleTypeName = name;
    }

    // to disable a texture, use VaTexture.Unknown
    public static final class BufferFormats
    {
        public int         DepthBuffer;
        public int         DepthBufferViewspaceLinear;
        public int         Albedo;
        public int         NormalMap;
        public int         Radiance;
        public int         OutputColor;

        public BufferFormats( )
        {
            DepthBuffer                 = VaTexture.D32_FLOAT;
            DepthBufferViewspaceLinear  = VaTexture.R16_FLOAT;
            Albedo                      = VaTexture.R8G8B8A8_UNORM_SRGB;
            NormalMap                   = VaTexture.R8G8B8A8_UNORM;          // improve encoding, drop to R8G8?
            Radiance                    = VaTexture.R11G11B10_FLOAT;         // is this enough? R16G16B16A16_FLOAT as alternative
            OutputColor                 = VaTexture.R8G8B8A8_UNORM_SRGB;     // for HDR displays use something higher
        }

        public void set(BufferFormats ohs){
            DepthBuffer = ohs.DepthBuffer;
            DepthBufferViewspaceLinear = ohs.DepthBufferViewspaceLinear;
            Albedo = ohs.Albedo;
            NormalMap = ohs.NormalMap;
            Radiance = ohs.Radiance;
            OutputColor = ohs.OutputColor;
        }
    };
}
