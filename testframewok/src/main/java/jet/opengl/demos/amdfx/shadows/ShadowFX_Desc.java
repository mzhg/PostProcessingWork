package jet.opengl.demos.amdfx.shadows;

import org.lwjgl.util.vector.Vector2f;
import org.lwjgl.util.vector.Vector4f;

import jet.opengl.postprocessing.texture.Texture2D;
final class ShadowFX_Desc {

	/** this has to be at least 6 to allow cube map shadow maps to work */
	static final int m_MaxLightCount = 6;
	
	// [optional]
	boolean                                      m_EnableCapture;

	// [required] Optional at initialization
    final Camera                                 m_Viewer = new Camera();
    // [required] Viewer Depth Buffer Size. Optional at initialization
    final Vector2f                               m_DepthSize = new Vector2f(); 

    // [required] Optional at initialization
    final Camera[]                               m_Light = new Camera[m_MaxLightCount];
    // [required] Optional at initialization
    final Vector2f[]                             m_ShadowSize = new Vector2f[m_MaxLightCount];
    // [required] Optional for shadow stored in texture arrays
    final Vector4f[]                             m_ShadowRegion = new Vector4f[m_MaxLightCount];
    // [required] Optional if contact hardening is not used
    final float[]                                m_SunArea = new float[m_MaxLightCount]; 
    // [required] Optional at initialization
    final float[]                                m_DepthTestOffset = new float[m_MaxLightCount]; 
    // [required] Optional at initialization
    final float[]                                m_NormalOffsetScale = new float[m_MaxLightCount]; 
    // [required] Optional at initialization. Only used with SHADOWFX_EXECUTION_WEIGHTED_AVG
    final float[]                                m_Weight = new float[m_MaxLightCount]; 
    // [required]Optional at initialization
    final int[]                                  m_ArraySlice = new int[m_MaxLightCount]; 
    int		                                     m_ActiveLightCount; // [required]

    int                           				 m_Execution; // [required]
    int                      					 m_Implementation; // [required]
    int                        					 m_TextureType; // [required]
    int                       					 m_TextureFetch; // [required]
    int                           				 m_Filtering; // [required]
    int                            				 m_TapType; // [required]
    int                         				 m_FilterSize; // [required]
    int                       					 m_NormalOption; // [required]
    
    Texture2D m_pDepthSRV;  // [required] input main viewer zbuffer
    Texture2D                   				 m_pShadowSRV; // [required] input shadow map
    Texture2D                   				 m_pNormalSRV; // [optional] input main viewer normal data (for Deferred Renderers)
    Texture2D                     				 m_pOutputRTV; // [required] output shadow mask 
    
    int 										 m_OutputFormat;
    
    Texture2D                    				 m_pOutputDSS; // [optional] output dss can specify stencil test 
    Texture2D                     				 m_pOutputDSV; // [optional] output depth stencil view (used if dss != null) it should have stencil data
    int                                			 m_ReferenceDSS; // [optional] stencil reference value (used if dss != null)

//    ID3D11BlendState*                            m_pOutputBS;      // [optional] output bs can specify how to write to rtv
    int                                			 m_OutputChannels; // [optional] output channels flags (not used if bs != null)

    /**
    All ShadowFX_Desc objects have a pointer to a single instance of a ShadowFX_OpaqueDesc.
    */
    ShadowFX_OpaqueDesc                          m_pOpaque;

	public static void ShadowFX_Initialize(ShadowFX_Desc desc) {
		
	}

	/**
    Execute ShadowFX rendering for a given ShadowFX_Desc parameters descriptior
    Calling this function requires setting up:<ul>
    * <li>m_pDeviceContext must be set to a valid immediate context. Only used in DX11
    * <li>m_CommandList must be set to a valid command list. Only used in DX12
    * <li>m_pDepthSRV must be set to a valid shader resource view pointing to a depth buffer resource. Only used in DX11
    * <li>m_DepthSize must be set to the correct size of m_pDepthSRV resource
    * <li>m_pShadowSRV must be set to a valid shader resource view pointing to a depth buffer resource. Only used in DX11
    * <li>m_pOutputRTV must be set to a valid render target view pointint to a resource of m_InputSize size. Only used in DX11
    * <li>m_Viewer - viewer camera parameters
    * <li>m_ActiveLightCount must indicate a valid active light count (0, 6]<p>
      for each active light application must specify values in:<ul>
        <li> m_Light[] - light camera parameters
        <li> m_ShadowSize[] - size of the shadow map
        <li> m_ShadowRegion[] - dimensions of a shadow region in uv space [0, 1]x[0, 1] .xy - offset .zw - size
        <li> m_SunArea[] - a scalar that controls the simulated light size (affects contact hardening algorithms)
        <li> m_DepthTestOffset[] - a scalar that is used in the shader to slightly adjust z test
        <li> m_NormalOffsetScale[] - a scalar that is used to displace tested position in world space before projecting to light space.
                                   It used when Normal option is set to either CALC_FROM_DEPTH or READ_FROM_SRV
        <li> m_ArraySlice[] - texture array index. It is used when m_TextureType is equal to ARRAY</ul>
    <li>Optionally application can change:
    * <li>m_Execution - lights can be arranged either:<ul>
        <li> as a union of shadow casters (each shadowed pixel is tested against each shadow caster)
        <li> as cascades for a single directional light (each shadowed pixel is tested until it falls into a single cascade)
        <li> as a cube map light (each shadowed pixel selects the correct shadow cube face and only uses that for filtering)</ul>
    * <li>m_Implementation - alternate between compute shader and pixel shader implementations (CS is under development)
    * <li>m_TextureType - shadow map(s) can be stored in either a texture 2d (atlas) or a texture 2d array
    * <li>m_TextureFetch - alternate between shader permutations that use Gather 4 or PCF instructions
    * <li>m_Filtering - alternate between uniform and contact hardening shadows;
    * <li>m_TapType - alternate between sampling all texels inside the filtering kernel or fetch poisson distributed samples (less samples)
    * <li>m_FilterSize - select filter size from 7x7 to 15x15
    * <li>m_NormalOption - each visible pixel on the screen is first reprojected in World Space. At this point it can be displaced along the normal
                       to help reduce incorrect self shadowing. Normal can either be calculated from depth buffer or fetched from SRV
    * <li>m_pNormalSRV - set to a valid SRV with normal gbuffer layer to use normal option READ_FROM_SRV . Only used in DX11
    
    * <li>m_pOutputDSS - set to a valid depth stencil state to enable performance optimizations.
                     For example this can be used to enable stencil testing to reduce the number of filtered pixels on the screen
    * <li>m_pOutputDSV - set to a valid depth stencil view to use in conjunction with m_pOutputDSS
    * <li>m_ReferenceDSS - stencil reference value (used if m_pOutputDSS != NULL)
    * <li>m_OutputChannels - set output write mask
    * <li>m_pOutputBS - set the whole blend state (will override m_OutputChannels)
    * <li>m_pDepth must be set to a valid depth buffer resource. Only used in DX12
    * <li>m_DepthSRV must be set to a valid shader resource view associated with m_pDepth. Only used in DX12
    * <li>m_pShadow must be set to a valid depth buffer resource. Only used in DX12
    * <li>m_ShadowSRV must be set to a valid shader resource view associated with m_pShadow. Only used in DX12
    * <li>m_pNormal set to a valid normal gbuffer layer resource. Only used in DX12
    * <li>m_NormalSRV set to a valid shader resource view associated with m_pNormal. Only used in DX12
    * <li>m_MaxInstance maximum number of instances: Up to m_MaxInstance shadow masks can be created in parallel. Only used in DX12
    * <li>m_InstanceID instance id must be less than m_MaxInstance. Only used in DX12
    * <li>m_PreserveViewport the library will not change the viewport and scissor if set to true. The default is false and the library sets viewport and scissor
    */
	public static void ShadowFX_Render(ShadowFX_Desc desc) {
		
	}
}
