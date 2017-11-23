package jet.opengl.demos.intel.assao;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector2i;
import org.lwjgl.util.vector.Vector4i;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;

import jet.opengl.postprocessing.common.GLCheck;
import jet.opengl.postprocessing.common.GLFuncProvider;
import jet.opengl.postprocessing.common.GLFuncProviderFactory;
import jet.opengl.postprocessing.common.GLenum;
import jet.opengl.postprocessing.shader.Macro;
import jet.opengl.postprocessing.texture.SamplerDesc;
import jet.opengl.postprocessing.texture.SamplerUtils;
import jet.opengl.postprocessing.texture.Texture2D;
import jet.opengl.postprocessing.texture.Texture2DDesc;
import jet.opengl.postprocessing.texture.TextureGL;
import jet.opengl.postprocessing.texture.TextureUtils;
import jet.opengl.postprocessing.util.CacheBuffer;
import jet.opengl.postprocessing.util.DebugTools;
import jet.opengl.postprocessing.util.Numeric;

final class ASSAOGL implements ASSAO_Effect, ASSAO_Macro{

    static boolean ASSAO_DEBUG;
    static boolean g_PrintOnce = false;
    static final String DEBUG_FILE_FOLDER = "E:/textures/ASSAODX/";

	private final BufferFormats            	m_formats = new BufferFormats();

    private final Vector2i                  m_size = new Vector2i();
    private final Vector2i                  m_halfSize = new Vector2i();
    private final Vector2i                  m_quarterSize = new Vector2i();
    private final Vector4i                  m_fullResOutScissorRect = new Vector4i();
    private final Vector4i                  m_halfResOutScissorRect = new Vector4i();
    
    private final ASSAOConstants            m_ConstantsUniform = new ASSAOConstants();

    private int                             m_depthMipLevels;

//    ID3D11Device *                          m_device;

    private int                             m_allocatedVRAM;

    private int                          	m_constantsBuffer;

//    ID3D11Buffer *                          m_fullscreenVB;

    private RenderTechnique                 m_vertexShader;
//    ID3D11InputLayout *                     m_inputLayout;

//    ID3D11RasterizerState *                 m_rasterizerState;

    private int			                    m_samplerStatePointClamp;
    private int			                    m_samplerStateLinearClamp;
    private int			                    m_samplerStatePointMirror;
    private int			                    m_samplerStateViewspaceDepthTap;

//    ID3D11BlendState *                      m_blendStateMultiply;
//    ID3D11BlendState *                      m_blendStateOpaque;
//    ID3D11DepthStencilState *               m_depthStencilState;

    private RenderTechnique                 m_pixelShaderPrepareDepths;
    private RenderTechnique                 m_pixelShaderPrepareDepthsAndNormals;
    private RenderTechnique                 m_pixelShaderPrepareDepthsHalf;
    private RenderTechnique                 m_pixelShaderPrepareDepthsAndNormalsHalf;
    private RenderTechnique[]               m_pixelShaderPrepareDepthMip = new RenderTechnique[SSAO_DEPTH_MIP_LEVELS - 1];
    private RenderTechnique[]               m_pixelShaderGenerate = new RenderTechnique[5];
    private RenderTechnique                 m_pixelShaderSmartBlur;
    private RenderTechnique                 m_pixelShaderSmartBlurWide;
    private RenderTechnique                 m_pixelShaderApply;
    private RenderTechnique                 m_pixelShaderNonSmartBlur;
    private RenderTechnique                 m_pixelShaderNonSmartApply;
    private RenderTechnique                 m_pixelShaderNonSmartHalfApply;
//#ifdef INTEL_SSAO_ENABLE_ADAPTIVE_QUALITY
    private RenderTechnique                 m_pixelShaderGenerateImportanceMap;
    private RenderTechnique                 m_pixelShaderPostprocessImportanceMapA;
    private RenderTechnique                 m_pixelShaderPostprocessImportanceMapB;
//#endif

    private Texture2D[]                     m_halfDepths = new Texture2D[4];
    private Texture2D[][]                   m_halfDepthsMipViews = new Texture2D[4][SSAO_DEPTH_MIP_LEVELS];
    //D3D11Texture2D                          m_edges;
    private Texture2D                      	m_pingPongHalfResultA;
    private Texture2D                       m_pingPongHalfResultB;
    private Texture2D                       m_finalResults;
    private Texture2D[]                     m_finalResultsArrayViews = new Texture2D[4];
    private Texture2D                       m_normals;
//#ifdef INTEL_SSAO_ENABLE_ADAPTIVE_QUALITY
    // Only needed for quality level 3 (adaptive quality)
    private Texture2D                       m_importanceMap;
    private Texture2D                       m_importanceMapPong;
    private int                       		m_loadCounter;
    private int 							m_dummyVAO;
    private int 							m_framebuffer;
//    ID3D11ShaderResourceView *              m_loadCounterSRV;
//    ID3D11UnorderedAccessView *             m_loadCounterUAV;
//#endif

    private boolean                         m_requiresClear;
    private int 							m_viewportWidth, m_viewportHeight;
    private boolean 					    m_printProgramOnce = false;

    private GLFuncProvider gl;
    
    ASSAOGL( ){
    	
    }

    void InitializeGL( ){
        gl = GLFuncProviderFactory.getGLFuncProvider();
    	{// constant buffer
	    	m_constantsBuffer = gl.glGenBuffer();
            gl.glBindBuffer(GLenum.GL_UNIFORM_BUFFER, m_constantsBuffer);
            gl.glBufferData(GLenum.GL_UNIFORM_BUFFER, ASSAOConstants.SIZE, GLenum.GL_DYNAMIC_DRAW);
            gl.glBindBuffer(GLenum.GL_UNIFORM_BUFFER, 0);
    	}
    	
    	{ // fullscreen vertex buffer
    		m_dummyVAO = gl.glGenVertexArray();
    		m_framebuffer = gl.glGenFramebuffer();
    	}
    	
    	{ // samplers
    		SamplerDesc desc = new SamplerDesc();
    		
//    		desc.Filter = D3D11_FILTER_MIN_MAG_MIP_POINT;
//            desc.AddressU = desc.AddressV = desc.AddressW = D3D11_TEXTURE_ADDRESS_CLAMP;
//            hr = m_device->CreateSamplerState( &desc, &m_samplerStatePointClamp );
//            if( FAILED( hr ) ) { assert( false ); CleanupDX(); return false; }
    		desc.minFilter = GLenum.GL_NEAREST_MIPMAP_NEAREST;
    		desc.magFilter = GLenum.GL_NEAREST;
    		// defualt is clamp
    		m_samplerStatePointClamp = SamplerUtils.createSampler(desc);

//            desc.AddressU = desc.AddressV = desc.AddressW = D3D11_TEXTURE_ADDRESS_MIRROR;
//            hr = m_device->CreateSamplerState( &desc, &m_samplerStatePointMirror );
//            if( FAILED( hr ) ) { assert( false ); CleanupDX(); return false; }
    		desc.wrapR = desc.wrapS = desc.wrapT = GLenum.GL_MIRRORED_REPEAT;  // TODO ???
    		m_samplerStatePointMirror = SamplerUtils.createSampler(desc);
    		

//            desc.Filter = D3D11_FILTER_MIN_MAG_MIP_LINEAR;
//            desc.AddressU = desc.AddressV = desc.AddressW = D3D11_TEXTURE_ADDRESS_CLAMP;
//            hr = m_device->CreateSamplerState( &desc, &m_samplerStateLinearClamp );
//            if( FAILED( hr ) ) { assert( false ); CleanupDX(); return false; }
    		desc.minFilter = GLenum.GL_LINEAR_MIPMAP_LINEAR;
    		desc.magFilter = GLenum.GL_LINEAR;
    		desc.wrapR = desc.wrapS = desc.wrapT = GLenum.GL_CLAMP_TO_EDGE;
    		m_samplerStateLinearClamp = SamplerUtils.createSampler(desc);

//            desc = CD3D11_SAMPLER_DESC( CD3D11_DEFAULT( ) );
//            desc.Filter = D3D11_FILTER_MIN_MAG_MIP_POINT;
//            desc.AddressU = desc.AddressV = desc.AddressW = D3D11_TEXTURE_ADDRESS_CLAMP;
//            hr = m_device->CreateSamplerState( &desc, &m_samplerStateViewspaceDepthTap );
//            if( FAILED( hr ) ) { assert( false ); CleanupDX(); return false; }
    		desc.minFilter = GLenum.GL_NEAREST_MIPMAP_NEAREST;
    		desc.magFilter = GLenum.GL_NEAREST;
    		m_samplerStateViewspaceDepthTap = SamplerUtils.createSampler(desc);
    	}
    	
    	// shaders
    	{
//    		D3D_SHADER_MACRO shaderMacros[] = { 
//    	            { "SSAO_MAX_TAPS" ,                              SSA_STRINGIZIZER( SSAO_MAX_TAPS                              ) },
//    	            { "SSAO_MAX_REF_TAPS" ,                          SSA_STRINGIZIZER( SSAO_MAX_REF_TAPS                          ) },
//    	            { "SSAO_ADAPTIVE_TAP_BASE_COUNT" ,               SSA_STRINGIZIZER( SSAO_ADAPTIVE_TAP_BASE_COUNT               ) },
//    	            { "SSAO_ADAPTIVE_TAP_FLEXIBLE_COUNT" ,           SSA_STRINGIZIZER( SSAO_ADAPTIVE_TAP_FLEXIBLE_COUNT           ) },
//    	            { "SSAO_DEPTH_MIP_LEVELS" ,                      SSA_STRINGIZIZER( SSAO_DEPTH_MIP_LEVELS                         ) },
//    	            { "SSAO_ENABLE_NORMAL_WORLD_TO_VIEW_CONVERSION", SSA_STRINGIZIZER( SSAO_ENABLE_NORMAL_WORLD_TO_VIEW_CONVERSION   ) },
//    	            
//    	            { NULL, NULL }
//    	        };
    		Macro[] shaderMacros =
			{
				new Macro("SSAO_MAX_TAPS", SSAO_MAX_TAPS),
                new Macro("SSAO_MAX_REF_TAPS", SSAO_MAX_REF_TAPS),
                new Macro("SSAO_ADAPTIVE_TAP_BASE_COUNT", SSAO_ADAPTIVE_TAP_BASE_COUNT),
                new Macro("SSAO_ADAPTIVE_TAP_FLEXIBLE_COUNT", SSAO_ADAPTIVE_TAP_FLEXIBLE_COUNT),
                new Macro("SSAO_DEPTH_MIP_LEVELS", SSAO_DEPTH_MIP_LEVELS),
                new Macro("SSAO_ENABLE_NORMAL_WORLD_TO_VIEW_CONVERSION", SSAO_ENABLE_NORMAL_WORLD_TO_VIEW_CONVERSION),
			};
    		
    		m_pixelShaderPrepareDepths = new RenderTechnique("PrepareDepthsPS.frag", shaderMacros);
    		m_pixelShaderPrepareDepthsAndNormals = new RenderTechnique("PrepareDepthsAndNormalsPS.frag", shaderMacros);
    		m_pixelShaderPrepareDepthsHalf = new RenderTechnique("PrepareDepthsHalfPS.frag", shaderMacros);
    		m_pixelShaderPrepareDepthsAndNormalsHalf = new RenderTechnique("PrepareDepthsAndNormalsHalfPS.frag", shaderMacros);
    		for(int i = 0; i < 3; i++){
    			int length = shaderMacros.length;
    			Macro[] newMacros = Arrays.copyOf(shaderMacros, length+1);
    			newMacros[length] = new Macro("MIP_LEVEL", i);
    			m_pixelShaderPrepareDepthMip[i] = new RenderTechnique("PrepareDepthMipPS.frag", newMacros);
    		}
    		
    		for(int i = 0; i < 5; i++){
    			int length = shaderMacros.length;
    			Macro[] newMacros = Arrays.copyOf(shaderMacros, length+2);
    			
    			int qualityLevel;
    			int adapativeBase;
    			
    			if(i < 4){
    				qualityLevel = i;
    				adapativeBase = 0;
    			}else{
    				qualityLevel = 3;
    				adapativeBase = 1;
    			}
    			newMacros[length + 0] = new Macro("QUALITY_LEVEL", qualityLevel);
    			newMacros[length + 1] = new Macro("ADPATIVE_BASE", adapativeBase);
    			m_pixelShaderGenerate[i] = new RenderTechnique("GenerateQPS.frag", newMacros);
    		}
    		
    		m_pixelShaderSmartBlur = new RenderTechnique("SmartBlurPS.frag", shaderMacros);
    		m_pixelShaderSmartBlurWide = new RenderTechnique("SmartBlurWidePS.frag", shaderMacros);
    		m_pixelShaderNonSmartBlur = new RenderTechnique("NonSmartBlurPS.frag", shaderMacros);
    		m_pixelShaderApply = new RenderTechnique("ApplyPS.frag", shaderMacros);
    		m_pixelShaderNonSmartApply = new RenderTechnique("NonSmartApplyPS.frag", shaderMacros);
    		m_pixelShaderNonSmartHalfApply = new RenderTechnique("NonSmartHalfApplyPS.frag", shaderMacros);
    		
    		m_pixelShaderGenerateImportanceMap = new RenderTechnique("GenerateImportanceMapPS.frag", shaderMacros);
    		m_pixelShaderPostprocessImportanceMapA = new RenderTechnique("PostprocessImportanceMapAPS.frag", shaderMacros);
    		m_pixelShaderPostprocessImportanceMapB = new RenderTechnique("PostprocessImportanceMapBPS.frag", shaderMacros);
    	}
    	
    	// load counter stuff, only needed for adaptive quality (quality level 3)
    	{
    		m_loadCounter = gl.glGenTexture();
            gl.glBindTexture(GLenum.GL_TEXTURE_1D, m_loadCounter);
            gl.glTexImage1D(GLenum.GL_TEXTURE_1D, 0, GLenum.GL_R32UI, 1, 0, GLenum.GL_RED_INTEGER, GLenum.GL_UNSIGNED_INT, (ByteBuffer)null);
            gl.glBindTexture(GLenum.GL_TEXTURE_1D, 0);
    	}
    }
    
    private void blendStateMultiply(){
        gl.glEnable(GLenum.GL_BLEND);
        gl.glBlendFuncSeparate(GLenum.GL_ZERO, GLenum.GL_SRC_COLOR, GLenum.GL_ZERO, GLenum.GL_SRC_ALPHA);
    }
    
    private void depthStencilState(){
        gl.glDisable(GLenum.GL_DEPTH_TEST);
        gl.glDisable(GLenum.GL_STENCIL_TEST);
    }
    
    private void rasterizerState(){
        gl.glDisable(GLenum.GL_CULL_FACE);
        gl.glPolygonMode(GLenum.GL_FRONT_AND_BACK, GLenum.GL_FILL);
    }
    
    void CleanupDX( ){
    	
    }
    
    Texture2D ReCreateIfNeeded(Texture2D src, Vector2i size, int format, int mipLevels, int arraySize, boolean unused){
    	if(checkFormat(src, size, format, mipLevels, arraySize, unused)){
    		if(src != null)
    			src.dispose();
    		
    		Texture2DDesc desc = new Texture2DDesc(size.x, size.y, mipLevels, arraySize, format, 1);
    		src = TextureUtils.createTexture2D(desc, null);
    	}
    	
    	return src;
    }
    
    private static boolean checkFormat(Texture2D src, Vector2i size, int format, int mipLevels, int arraySize, boolean unused){
    	return src == null || src.getWidth() != size.x || src.getHeight() != size.y || 
    		   src.getFormat() != format || src.getMipLevels() != mipLevels || src.getArraySize() != arraySize;
    }
    
    private static Texture2D ReCreateMIPViewIfNeeded(Texture2D source, int mipLevel){
    	return TextureUtils.createTextureView(source, GLenum.GL_TEXTURE_2D, mipLevel, 1, 0, 1);
    	
    }
    
    private static Texture2D ReCreateArrayViewIfNeeded(Texture2D source, int slice){
    	return TextureUtils.createTextureView(source, GLenum.GL_TEXTURE_2D, 0, 1, slice, 1);
    }
    
    void UpdateTextures( ASSAO_InputsOpenGL inputs ){
//    	vaVector4i depthTexDims = GetTextureDimsFromSRV( inputs->DepthSRV );
        assert( inputs.DepthSRV.getWidth() >= inputs.ViewportWidth );
        assert( inputs.DepthSRV.getHeight() >= inputs.ViewportHeight );
        assert( inputs.DepthSRV.getArraySize() == 1 );  // no texture arrays supported

        if( inputs.NormalSRV != null )
        {
//            vaVector4i normTexDims = GetTextureDimsFromSRV( inputs->NormalSRV );
            assert(inputs.NormalSRV.getWidth() >= inputs.ViewportWidth );
            assert( inputs.NormalSRV.getWidth() >= inputs.ViewportHeight );
        }
        
        boolean needsUpdate = false;

        // We've got input normals? No need to keep ours then.
        if( inputs.NormalSRV != null )
        {
            if( m_normals != null )
            {   
                needsUpdate = true;
                m_normals.dispose();
            }
        }
        else
        {
            if( m_normals == null )
            {   
                needsUpdate = true;
            }
        }

        int width = inputs.ViewportWidth;
        int height = inputs.ViewportHeight;

        needsUpdate |= (m_size.x != width) || (m_size.y != height);

        m_size.x        = width;
        m_size.y        = height;
        m_halfSize.x    = ( width + 1 ) / 2;
        m_halfSize.y    = ( height + 1 ) / 2;
        m_quarterSize.x = (m_halfSize.x+1)/2;
        m_quarterSize.y = (m_halfSize.y+1)/2;

        Vector4i prevScissorRect = new Vector4i(m_fullResOutScissorRect);

        if( (inputs.ScissorRight == 0) || (inputs.ScissorBottom == 0) )
            m_fullResOutScissorRect.set( 0, 0, width, height );
        else
            m_fullResOutScissorRect.set( Math.max( 0, inputs.ScissorLeft ), Math.max( 0, inputs.ScissorTop ), Math.min( width, inputs.ScissorRight ), Math.min( height, inputs.ScissorBottom ) );

//        needsUpdate |= prevScissorRect != m_fullResOutScissorRect;
        if(!prevScissorRect.equals(m_fullResOutScissorRect)){
        	needsUpdate = true;
        }
        if( !needsUpdate )
            return;
        
        m_halfResOutScissorRect.set( m_fullResOutScissorRect.x/2, m_fullResOutScissorRect.y/2, (m_fullResOutScissorRect.z+1) / 2, (m_fullResOutScissorRect.w+1) / 2 );
        int blurEnlarge = cMaxBlurPassCount + Math.max( 0, cMaxBlurPassCount-2 );  // +1 for max normal blurs, +2 for wide blurs
        m_halfResOutScissorRect.set( Math.max( 0, m_halfResOutScissorRect.x - blurEnlarge ), Math.max( 0, m_halfResOutScissorRect.y - blurEnlarge ), 
        							 Math.min( m_halfSize.x, m_halfResOutScissorRect.z + blurEnlarge ), Math.min( m_halfSize.y, m_halfResOutScissorRect.w + blurEnlarge ) );

        float totalSizeInMB = 0.0f;

        m_depthMipLevels = SSAO_DEPTH_MIP_LEVELS;

        for( int i = 0; i < 4; i++ )
        {
            if(checkFormat( m_halfDepths[i], m_halfSize, m_formats.DepthBufferViewspaceLinear, /*totalSizeInMB,*/ m_depthMipLevels, 1, false ) )
            {
            	m_halfDepths[i] = ReCreateIfNeeded(m_halfDepths[i], m_halfSize, m_formats.DepthBufferViewspaceLinear, /*totalSizeInMB,*/ m_depthMipLevels, 1, false);
                for( int j = 0; j < m_depthMipLevels; j++ ){
                	if(m_halfDepthsMipViews[i][j] != null)
                		m_halfDepthsMipViews[i][j].dispose();
                }

                for( int j = 0; j < m_depthMipLevels; j++ )
                    m_halfDepthsMipViews[i][j] = ReCreateMIPViewIfNeeded(m_halfDepths[i], j );
            }

        }
        m_pingPongHalfResultA=ReCreateIfNeeded( m_pingPongHalfResultA, m_halfSize, m_formats.AOResult, /*totalSizeInMB,*/ 1, 1, false );
        m_pingPongHalfResultB=ReCreateIfNeeded( m_pingPongHalfResultB, m_halfSize, m_formats.AOResult, /*totalSizeInMB,*/ 1, 1, false );
        m_finalResults=ReCreateIfNeeded( m_finalResults, m_halfSize, m_formats.AOResult, /*totalSizeInMB,*/ 1, 4, false );
//    #ifdef INTEL_SSAO_ENABLE_ADAPTIVE_QUALITY
        m_importanceMap =ReCreateIfNeeded( m_importanceMap, m_quarterSize, m_formats.ImportanceMap, /*totalSizeInMB,*/ 1, 1, false );
        m_importanceMapPong=ReCreateIfNeeded( m_importanceMapPong, m_quarterSize, m_formats.ImportanceMap, /*totalSizeInMB,*/ 1, 1, false );
//    #endif
        for( int i = 0; i < 4; i++ )
            m_finalResultsArrayViews[i]=ReCreateArrayViewIfNeeded(m_finalResults, i );

        if( inputs.NormalSRV == null )
            m_normals=ReCreateIfNeeded(m_normals,m_size, m_formats.Normals, /*totalSizeInMB,*/ 1, 1, true );

        totalSizeInMB /= 1024 * 1024;
        //    m_debugInfo = vaStringTools::Format( "SSAO (approx. %.2fMB memory used) ", totalSizeInMB );

        // trigger a full buffers clear first time; only really required when using scissor rects
        m_requiresClear = true;
    }
    
    void UpdateConstants(ASSAO_Settings settings, ASSAO_InputsOpenGL inputs, int pass, String debugName ){
//    	ID3D11DeviceContext * dx11Context = inputs->DeviceContext;
        boolean generateNormals = inputs.NormalSRV == null;

        // update constants
//        D3D11_MAPPED_SUBRESOURCE mappedResource;
//        if( dx11Context->Map( m_constantsBuffer, 0, D3D11_MAP_WRITE_DISCARD, 0, &mappedResource ) != S_OK )
//        {
//            assert( false ); return;
//        }
//        else
        {
//            ASSAOConstants & consts = *((ASSAOConstants*)mappedResource.pData);
        	ASSAOConstants consts     = m_ConstantsUniform;

            final Matrix4f proj = inputs.ProjectionMatrix;

            consts.ViewportPixelSize                .set( 1.0f / (float)m_size.x, 1.0f / (float)m_size.y );
            consts.HalfViewportPixelSize            .set( 1.0f / (float)m_halfSize.x, 1.0f / (float)m_halfSize.y );

            consts.Viewport2xPixelSize              .set( consts.ViewportPixelSize.x * 2.0f, consts.ViewportPixelSize.y * 2.0f );
            consts.Viewport2xPixelSize_x_025        .set( consts.Viewport2xPixelSize.x * 0.25f, consts.Viewport2xPixelSize.y * 0.25f );

            if(ASSAO_DEBUG) {
                float depthLinearizeMul = (inputs.MatricesRowMajorOrder) ? (-proj.m32) : (-proj.m22);           // float depthLinearizeMul = ( clipFar * clipNear ) / ( clipFar - clipNear );
                float depthLinearizeAdd = (inputs.MatricesRowMajorOrder) ? (proj.m22) : (proj.m22);           // float depthLinearizeAdd = clipFar / ( clipFar - clipNear );

                // correct the handedness issue. need to make sure this below is correct, but I think it is.
                if (depthLinearizeMul * depthLinearizeAdd < 0)
                    depthLinearizeAdd = -depthLinearizeAdd;
                consts.DepthUnpackConsts.set(depthLinearizeMul, depthLinearizeAdd);
            }else {
                float depthLinearizeMul = inputs.CameraFar * inputs.CameraNear / (inputs.CameraFar - inputs.CameraNear);
                float depthLinearizeAdd = inputs.CameraFar / (inputs.CameraFar - inputs.CameraNear);
                consts.DepthUnpackConsts.set(depthLinearizeMul, depthLinearizeAdd);
            }

            float tanHalfFOVY                       = 1.0f / proj.m11;    // = tanf( drawContext.Camera.GetYFOV( ) * 0.5f );
            float tanHalfFOVX                       = 1.0F / proj.m00;    // = tanHalfFOVY * drawContext.Camera.GetAspect( );
            consts.CameraTanHalfFOV                 .set( tanHalfFOVX, tanHalfFOVY );

            if(ASSAO_DEBUG) {
                consts.NDCToViewMul.set(consts.CameraTanHalfFOV.x * 2.0f, consts.CameraTanHalfFOV.y * -2.0f);
                consts.NDCToViewAdd.set(consts.CameraTanHalfFOV.x * -1.0f, consts.CameraTanHalfFOV.y * 1.0f);
            }else {
                consts.NDCToViewMul.set(tanHalfFOVX * -2.0f, tanHalfFOVY * -2.0f);
                consts.NDCToViewAdd.set(tanHalfFOVX, tanHalfFOVY);
            }

            consts.EffectRadius                     = Numeric.clamp( settings.Radius, 0.0f, 100000.0f );
            consts.EffectShadowStrength             = Numeric.clamp( settings.ShadowMultiplier * 4.3f, 0.0f, 10.0f );
            consts.EffectShadowPow                  = Numeric.clamp( settings.ShadowPower, 0.0f, 10.0f );
            consts.EffectShadowClamp                = Numeric.clamp( settings.ShadowClamp, 0.0f, 1.0f );
            consts.EffectFadeOutMul                 = -1.0f / ( settings.FadeOutTo - settings.FadeOutFrom );
            consts.EffectFadeOutAdd                 = settings.FadeOutFrom / ( settings.FadeOutTo - settings.FadeOutFrom ) + 1.0f;
            consts.EffectHorizonAngleThreshold      = Numeric.clamp( settings.HorizonAngleThreshold, 0.0f, 1.0f );

            // 1.2 seems to be around the best trade off - 1.0 means on-screen radius will stop/slow growing when the camera is at 1.0 distance, so, depending on FOV, basically filling up most of the screen
            // This setting is viewspace-dependent and not screen size dependent intentionally, so that when you change FOV the effect stays (relatively) similar.
            float effectSamplingRadiusNearLimit     = ( settings.Radius * 1.2f );

            // if the depth precision is switched to 32bit float, this can be set to something closer to 1 (0.9999 is fine)
            consts.DepthPrecisionOffsetMod          = 0.9992f;

            // consts.RadiusDistanceScalingFunctionPow     = 1.0f - Clamp( settings.RadiusDistanceScalingFunction, 0.0f, 1.0f );

            int lastHalfDepthMipX = m_halfDepthsMipViews[0][SSAO_DEPTH_MIP_LEVELS - 1].getWidth();
            int lastHalfDepthMipY = m_halfDepthsMipViews[0][SSAO_DEPTH_MIP_LEVELS - 1].getHeight();

            // used to get average load per pixel; 9.0 is there to compensate for only doing every 9th InterlockedAdd in PSPostprocessImportanceMapB for performance reasons
            consts.LoadCounterAvgDiv                = 9.0f / (float)( m_quarterSize.x * m_quarterSize.y * 255.0 );

            // Special settings for lowest quality level - just nerf the effect a tiny bit
            if( settings.QualityLevel <= 0 )
            {
                //consts.EffectShadowStrength     *= 0.9f;
                effectSamplingRadiusNearLimit   *= 1.50f;

                if( settings.QualityLevel < 0 )
                    consts.EffectRadius             *= 0.8f;
            }
            effectSamplingRadiusNearLimit /= tanHalfFOVY; // to keep the effect same regardless of FOV

            consts.EffectSamplingRadiusNearLimitRec = 1.0f / effectSamplingRadiusNearLimit;

            consts.AdaptiveSampleCountLimit         = settings.AdaptiveQualityLimit; 

            consts.NegRecEffectRadius               = -1.0f / consts.EffectRadius;

            consts.PerPassFullResCoordOffset        .set( pass % 2, pass / 2 );
            consts.PerPassFullResUVOffset           .set( ((pass % 2) - 0.0f) / m_size.x, ((pass / 2) - 0.0f) / m_size.y );

            consts.InvSharpness                     = Numeric.clamp( 1.0f - settings.Sharpness, 0.0f, 1.0f );
            consts.PassIndex                        = pass;
            consts.QuarterResPixelSize              .set( 1.0f / (float)m_quarterSize.x, 1.0f / (float)m_quarterSize.y );

            float additionalAngleOffset = settings.TemporalSupersamplingAngleOffset;  // if using temporal supersampling approach (like "Progressive Rendering Using Multi-frame Sampling" from GPU Pro 7, etc.)
            float additionalRadiusScale = settings.TemporalSupersamplingRadiusOffset; // if using temporal supersampling approach (like "Progressive Rendering Using Multi-frame Sampling" from GPU Pro 7, etc.)
            final int subPassCount = 5;
            for( int subPass = 0; subPass < subPassCount; subPass++ )
            {
                int a = pass;
                int b = subPass;

                int spmap[] = { 0, 1, 4, 3, 2 };
                b = spmap[subPass];

                float ca, sa;
                float angle0 = ( (float)a + (float)b / (float)subPassCount ) * (3.1415926535897932384626433832795f) * 0.5f;
                angle0 += additionalAngleOffset;

                ca = (float) Math.cos( angle0 );
                sa = (float) Math.sin( angle0 );

                float scale = 1.0f + (a-1.5f + (b - (subPassCount-1.0f) * 0.5f ) / (float)subPassCount ) * 0.07f;
                scale *= additionalRadiusScale;

                consts.PatternRotScaleMatrices[subPass].set( scale * ca, scale * -sa, -scale * sa, -scale * ca );
            }

            if( !generateNormals )
            {
                consts.NormalsUnpackMul = inputs.NormalsUnpackMul;
                consts.NormalsUnpackAdd = inputs.NormalsUnpackAdd;
            }
            else
            {
                consts.NormalsUnpackMul = 2.0f;
                consts.NormalsUnpackAdd = -1.0f;
            }
            consts.DetailAOStrength = settings.DetailShadowStrength;
            consts.Dummy0 = 0.0f;

            if(!g_PrintOnce){
                String filename = String.format("%sASSAOConstants_%s.dat", DEBUG_FILE_FOLDER, debugName);
                ByteBuffer bytes = CacheBuffer.getCachedByteBuffer(ASSAOConstants.SIZE);
                consts.store(bytes).flip();
                DebugTools.saveBinary(bytes, filename);

                filename = String.format("%sASSAO_Settings_%s.dat", DEBUG_FILE_FOLDER, debugName);
                bytes = CacheBuffer.getCachedByteBuffer(ASSAO_Settings.SIZE);
                settings.store(bytes).flip();
                DebugTools.saveBinary(bytes, filename);
            }

//    #if SSAO_ENABLE_NORMAL_WORLD_TO_VIEW_CONVERSION
//            if( !generateNormals )
//            {
//                consts.NormalsWorldToViewspaceMatrix = inputs->NormalsWorldToViewspaceMatrix;
//                if( !inputs->MatricesRowMajorOrder )
//                    consts.NormalsWorldToViewspaceMatrix.Transpose();
//            }
//            else
//            {
//                consts.NormalsWorldToViewspaceMatrix.SetIdentity( );
//            }
//    #endif


//            dx11Context->Unmap( m_constantsBuffer, 0 );
            //m_constantsBuffer.Update( dx11Context, consts );
        }
    }
    void FullscreenPassDraw(RenderTechnique pixelShader, int width, int height, Runnable blendState ){
    	pixelShader.enable();
    	pixelShader.setUnfiorms(m_ConstantsUniform);
        gl.glBindVertexArray(m_dummyVAO);
    	if(m_viewportWidth != width || m_viewportHeight != height){
            gl.glViewport(0, 0, width, height);
    		m_viewportWidth = width;
    		m_viewportHeight = height;
    	}
    	
    	if(blendState != null)
    		blendState.run();
    	else{
            gl.glDisable(GLenum.GL_BLEND);
    	}

        gl.glDrawArrays(GLenum.GL_TRIANGLES, 0, 3);
        gl.glBindVertexArray(0);
    	
    	if(!m_printProgramOnce){
    		GLCheck.checkError();
    		pixelShader.printPrograminfo();
    	}
    }
    
    private void setRenderTargets(Texture2D...textures){
        gl.glBindFramebuffer(GLenum.GL_FRAMEBUFFER, m_framebuffer);
    	
    	int[] drawbuffers = new int[textures.length];
    	for(int i = 0; i < textures.length; i++){
            gl.glFramebufferTexture2D(GLenum.GL_FRAMEBUFFER, GLenum.GL_COLOR_ATTACHMENT0 + i, textures[i].getTarget(), textures[i].getTexture(), 0);
    		drawbuffers[i] = GLenum.GL_COLOR_ATTACHMENT0 + i;
    	}
        gl.glDrawBuffers(CacheBuffer.wrap(drawbuffers));
    }

    final void saveTextData(String filename, TextureGL texture){
        try {
            DebugTools.saveTextureAsText(texture.getTarget(), texture.getTexture(), 0, DEBUG_FILE_FOLDER + filename);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    final void saveTextData(String filename, int target, int buffer, int internalformat){
        try {
            DebugTools.saveBufferAsText(target, buffer, internalformat, 128, DEBUG_FILE_FOLDER + filename);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    final void saveTextData(String filename, int target, int buffer, Class<?> internalformat){
        try {
            DebugTools.saveBufferAsText(target, buffer, internalformat, 128, DEBUG_FILE_FOLDER + filename);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    private void unbindRenderTargets(int count){
    	for(int i = 1; i < count; i++){
            gl.glFramebufferTexture2D(GLenum.GL_FRAMEBUFFER, GLenum.GL_COLOR_ATTACHMENT0 + i, GLenum.GL_TEXTURE_2D, 0, 0);
    	}
    }
    
    void PrepareDepths( ASSAO_Settings settings, ASSAO_InputsOpenGL inputs ){
    	boolean generateNormals = inputs.NormalSRV == null;

//        ID3D11DeviceContext * dx11Context = inputs->DeviceContext;

//        dx11Context->PSSetShaderResources( SSAO_TEXTURE_SLOT0, 1, &inputs->DepthSRV );
    	bind(inputs.DepthSRV, RenderTechnique.TEX2D_DEPTH_BUFFER, m_samplerStatePointClamp);
    	

//        {
//            CD3D11_VIEWPORT viewport = CD3D11_VIEWPORT( 0.0f, 0.0f, (float)m_halfSize.x, (float)m_halfSize.y );
//            CD3D11_RECT rect = CD3D11_RECT( 0, 0, m_halfSize.x, m_halfSize.y );
//            dx11Context->RSSetViewports( 1, &viewport );
//            dx11Context->RSSetScissorRects( 1, &rect );  // no scissor for this
//        }
    	int viewportWidth = m_halfSize.x;
    	int viewportHeight = m_halfSize.y;

//        ID3D11RenderTargetView* fourDepths[]    = { m_halfDepths[0].RTV, m_halfDepths[1].RTV, m_halfDepths[2].RTV, m_halfDepths[3].RTV };
//        ID3D11RenderTargetView* twoDepths[]     = { m_halfDepths[0].RTV, m_halfDepths[3].RTV };
    	
    	Texture2D[] fourDepths = { m_halfDepths[0], m_halfDepths[1], m_halfDepths[2], m_halfDepths[3] };
    	Texture2D[] twoDepths  = { m_halfDepths[0], m_halfDepths[3] };
        if (!g_PrintOnce)
        {
//            DEBUG_TEXTURE2D_BINARY(dx11Context, inputs->DepthSRV, DEBUG_FILE_FOLDER"DepthSRV.dat");
        }

        if( !generateNormals )
        {
            //VA_SCOPE_CPUGPU_TIMER( PrepareDepths, drawContext.APIContext );
            if (!g_PrintOnce)
            {
//                DEBUG_TEXTURE2D_BINARY(dx11Context, inputs->NormalSRV, DEBUG_FILE_FOLDER"NormalSRV.dat");
            }

            if( settings.QualityLevel < 0 )
            {
//                dx11Context->OMSetRenderTargets( _countof(twoDepths), twoDepths, NULL );
            	setRenderTargets(twoDepths);
            	FullscreenPassDraw(m_pixelShaderPrepareDepthsHalf, viewportWidth, viewportHeight, null );
                if (!g_PrintOnce)
                {
//                    DEBUG_TEXTURE2D(dx11Context, m_halfDepths[0].RTV, DEBUG_FILE_FOLDER);
//                    DEBUG_TEXTURE2D(dx11Context, m_halfDepths[3].RTV, DEBUG_FILE_FOLDER"halfDepths[3].txt");
                    saveTextData("halfDepths[0].txt", m_halfDepths[0]);
                    saveTextData("halfDepths[3].txt", m_halfDepths[3]);
                }
            	unbindRenderTargets(twoDepths.length);
            }
            else
            {
            	setRenderTargets(fourDepths);
            	FullscreenPassDraw(m_pixelShaderPrepareDepths , viewportWidth, viewportHeight, null );
            	unbindRenderTargets(fourDepths.length);

                if (!g_PrintOnce)
                {
                    /*DEBUG_TEXTURE2D(dx11Context, m_halfDepths[0].RTV, DEBUG_FILE_FOLDER"halfDepths[0].txt");
                    DEBUG_TEXTURE2D(dx11Context, m_halfDepths[1].RTV, DEBUG_FILE_FOLDER"halfDepths[1].txt");
                    DEBUG_TEXTURE2D(dx11Context, m_halfDepths[2].RTV, DEBUG_FILE_FOLDER"halfDepths[2].txt");
                    DEBUG_TEXTURE2D(dx11Context, m_halfDepths[3].RTV, DEBUG_FILE_FOLDER"halfDepths[3].txt");*/

                    saveTextData("halfDepths[0].txt", m_halfDepths[0]);
                    saveTextData("halfDepths[1].txt", m_halfDepths[1]);
                    saveTextData("halfDepths[2].txt", m_halfDepths[2]);
                    saveTextData("halfDepths[3].txt", m_halfDepths[3]);
                }
            }
        }
        else
        {
            //VA_SCOPE_CPUGPU_TIMER( PrepareDepthsAndNormals, drawContext.APIContext );

//            ID3D11UnorderedAccessView * UAVs[] = { m_normals.UAV };
            if( settings.QualityLevel < 0 )
            {
//                dx11Context->OMSetRenderTargetsAndUnorderedAccessViews( _countof(twoDepths), twoDepths, NULL, SSAO_NORMALMAP_OUT_UAV_SLOT, 1, UAVs, NULL );
            	setRenderTargets(twoDepths);
                gl.glBindImageTexture(0, m_normals.getTexture(), 0, false, 0, GLenum.GL_WRITE_ONLY, GLenum.GL_RGBA8);
            	
                FullscreenPassDraw(m_pixelShaderPrepareDepthsAndNormalsHalf, viewportWidth, viewportHeight, null);
                if (!g_PrintOnce)
                {
                    /*DEBUG_TEXTURE2D(dx11Context, m_normals.UAV,		  DEBUG_FILE_FOLDER"GenerateNormals.txt");
                    DEBUG_TEXTURE2D(dx11Context, m_halfDepths[0].RTV, DEBUG_FILE_FOLDER"halfDepths[0].txt");
                    DEBUG_TEXTURE2D(dx11Context, m_halfDepths[3].RTV, DEBUG_FILE_FOLDER"halfDepths[3].txt");*/

                    saveTextData("GenerateNormals.txt", m_normals);
                    saveTextData("halfDepths[0].txt", m_halfDepths[0]);
                    saveTextData("halfDepths[3].txt", m_halfDepths[3]);
                }
                
                unbindRenderTargets(twoDepths.length);
                gl.glBindImageTexture(0, 0, 0, false, 0, GLenum.GL_WRITE_ONLY, GLenum.GL_RGBA8);
            }
            else
            {
//                dx11Context->OMSetRenderTargetsAndUnorderedAccessViews( _countof(fourDepths), fourDepths, NULL, SSAO_NORMALMAP_OUT_UAV_SLOT, 1, UAVs, NULL );
                setRenderTargets(fourDepths);
                gl.glBindImageTexture(0, m_normals.getTexture(), 0, false, 0, GLenum.GL_WRITE_ONLY, GLenum.GL_RGBA8);
                
                FullscreenPassDraw(m_pixelShaderPrepareDepthsAndNormals, viewportWidth, viewportHeight, null );

                if (!g_PrintOnce)
                {
                    /*DEBUG_TEXTURE2D(dx11Context, m_normals.UAV, DEBUG_FILE_FOLDER"GenerateNormals.txt");
                    DEBUG_TEXTURE2D(dx11Context, m_halfDepths[0].RTV, DEBUG_FILE_FOLDER"halfDepths[0].txt");
                    DEBUG_TEXTURE2D(dx11Context, m_halfDepths[1].RTV, DEBUG_FILE_FOLDER"halfDepths[1].txt");
                    DEBUG_TEXTURE2D(dx11Context, m_halfDepths[2].RTV, DEBUG_FILE_FOLDER"halfDepths[2].txt");
                    DEBUG_TEXTURE2D(dx11Context, m_halfDepths[3].RTV, DEBUG_FILE_FOLDER"halfDepths[3].txt");*/
                    saveTextData("GenerateNormals.txt", m_normals);
                    saveTextData("halfDepths[0].txt", m_halfDepths[0]);
                    saveTextData("halfDepths[1].txt", m_halfDepths[1]);
                    saveTextData("halfDepths[2].txt", m_halfDepths[2]);
                    saveTextData("halfDepths[3].txt", m_halfDepths[3]);
                }
                
                unbindRenderTargets(fourDepths.length);
                gl.glBindImageTexture(0, 0, 0, false, 0, GLenum.GL_WRITE_ONLY, GLenum.GL_RGBA8);
                
                if(!m_printProgramOnce){
                	saveTextureAsText("HalfDepth%d.txt", fourDepths);
                	saveTextureAsText(m_normals, "Normal.txt");
                }
            }
        }

        // only do mipmaps for higher quality levels (not beneficial on quality level 1, and detrimental on quality level 0)
        if( settings.QualityLevel > 1 )
        {
            //VA_SCOPE_CPUGPU_TIMER( PrepareDepthMips, drawContext.APIContext );

            for( int i = 1; i < m_depthMipLevels; i++ )
            {
                Texture2D fourDepthMips[] = { m_halfDepthsMipViews[0][i], m_halfDepthsMipViews[1][i], m_halfDepthsMipViews[2][i], m_halfDepthsMipViews[3][i]};

//                CD3D11_VIEWPORT viewport = CD3D11_VIEWPORT( 0.0f, 0.0f, (float)m_halfDepthsMipViews[0][i].Size.x, (float)m_halfDepthsMipViews[0][i].Size.y );
//                dx11Context->RSSetViewports( 1, &viewport );
                viewportWidth = m_halfDepthsMipViews[0][i].getWidth();
                viewportHeight = m_halfDepthsMipViews[0][i].getHeight();

                Texture2D fourSRVs[] = { m_halfDepthsMipViews[0][i - 1], m_halfDepthsMipViews[1][i - 1], m_halfDepthsMipViews[2][i - 1], m_halfDepthsMipViews[3][i - 1] };

//                dx11Context->OMSetRenderTargets( 4, fourDepthMips, NULL );
//                dx11Context->PSSetShaderResources( SSAO_TEXTURE_SLOT0, 4, fourSRVs );
                
                setRenderTargets(fourDepthMips);  // setup rendering targets
                for(int id = 0; id < fourSRVs.length; id++){  // binding texture resources.
                	bind(fourSRVs[id], RenderTechnique.TEX2D_VIEW_DEPTH + id, 0);
                }
                
                FullscreenPassDraw(m_pixelShaderPrepareDepthMip[i - 1], viewportWidth, viewportHeight, null  );
                if (!g_PrintOnce)
                {
                    for (int j = 0; j < fourDepthMips.length; j++)
                    {
                        String filename = String.format("DepthMipViews[%d][%d].txt", j, i);
//                        DEBUG_TEXTURE2D(dx11Context, fourDepthMips[j], filename);
                        saveTextureAsText(fourDepthMips[j], filename);
                    }
                }
                unbindRenderTargets(fourDepthMips.length);
            }
        }
    }
    
    static void saveTextureAsText(String filename, Texture2D...texs){
    	for(int i = 0; i < texs.length; i++){
			saveTextureAsText(texs[i], String.format(filename, i));
		}
    }
    
    static void saveTextureAsText(TextureGL texture, String filename){
    	try {
            DebugTools.saveTextureAsText(texture.getTarget(), texture.getTexture(), 0, "E:/textures/ASSAOGL/" + filename);
		} catch (IOException e) {
			e.printStackTrace();
		}
    }
    
    static final int cMaxBlurPassCount = 6;
    
    void GenerateSSAO(ASSAO_Settings  settings, ASSAO_InputsOpenGL inputs, boolean adaptiveBasePass ){
//    	ID3D11ShaderResourceView * normalmapSRV = (inputs->NormalSRV==NULL)?(m_normals.SRV):(inputs->NormalSRV);
    	Texture2D normalmapSRV = (inputs.NormalSRV == null) ? (m_normals) : (inputs.NormalSRV);

//        ID3D11DeviceContext * dx11Context = inputs->DeviceContext;

        {
//            CD3D11_VIEWPORT viewport = CD3D11_VIEWPORT( 0.0f, 0.0f, (float)m_halfSize.x, (float)m_halfSize.y );
//            CD3D11_RECT rect = CD3D11_RECT( m_halfResOutScissorRect.x, m_halfResOutScissorRect.y, m_halfResOutScissorRect.z, m_halfResOutScissorRect.w );
//            dx11Context->RSSetViewports( 1, &viewport );
//            dx11Context->RSSetScissorRects( 1, &rect );
            gl.glEnable(GLenum.GL_SCISSOR_TEST);
            gl.glScissor(m_halfResOutScissorRect.x, m_halfResOutScissorRect.y, m_halfResOutScissorRect.z, m_halfResOutScissorRect.w);
        }
        
        int viewportWidth = m_halfSize.x;
    	int viewportHeight = m_halfSize.y;

        if( adaptiveBasePass )
        {
            assert( settings.QualityLevel == 3 );
        }

        final int adaptive = adaptiveBasePass ? 1: 0;
        int passCount = 4;

//        ID3D11ShaderResourceView * zeroSRVs[] = { NULL, NULL, NULL, NULL, NULL };

        for( int pass = 0; pass < passCount; pass++ )
        {
            if( (settings.QualityLevel < 0) && ( (pass == 1) || (pass == 2) ) )
                continue;

            int blurPasses = settings.BlurPassCount;
            blurPasses = Math.min( blurPasses, cMaxBlurPassCount );

//    #ifdef INTEL_SSAO_ENABLE_ADAPTIVE_QUALITY
            if( settings.QualityLevel == 3 )
            {
                // if adaptive, at least one blur pass needed as the first pass needs to read the final texture results - kind of awkward
                if( adaptiveBasePass )
                    blurPasses = 0;
                else
                    blurPasses = Math.max( 1, blurPasses );
            } 
            else 
//    #endif
            if( settings.QualityLevel <= 0 )
            {
                // just one blur pass allowed for minimum quality 
                blurPasses = Math.min( 1, settings.BlurPassCount );
            }

            UpdateConstants( settings, inputs, pass, "GenerateSSAO_Pass" + pass + "_Adaptive" + adaptive);

            Texture2D pPingRT = m_pingPongHalfResultA;
            Texture2D pPongRT = m_pingPongHalfResultB;

            // Generate
            {
                //VA_SCOPE_CPUGPU_TIMER_NAMED( Generate, vaStringTools::Format( "Generate_pass%d", pass ), drawContext.APIContext );

                // remove textures from slots 0, 1, 2, 3 to avoid API complaints
//                dx11Context->PSSetShaderResources( SSAO_TEXTURE_SLOT0, 5, zeroSRVs );
            	// TODO unbind resources.

//                ID3D11RenderTargetView * rts[] = { pPingRT->RTV };
            	Texture2D[] rts = {pPingRT};

                // no blur?
                if( blurPasses == 0 )
                    rts[0] = m_finalResultsArrayViews[pass];

//                dx11Context->OMSetRenderTargets( _countof( rts ), rts, NULL );
                setRenderTargets(rts);

//                ID3D11ShaderResourceView * SRVs[] = { m_halfDepths[pass].SRV, normalmapSRV, NULL, NULL, NULL }; // m_loadCounterSRV used only for quality level 3
                Texture2D[] SRVs = {m_halfDepths[pass], normalmapSRV, null, null, null};
//    #ifdef INTEL_SSAO_ENABLE_ADAPTIVE_QUALITY
                if( !adaptiveBasePass && (settings.QualityLevel == 3) )
                {
//                    SRVs[2] = m_loadCounterSRV;  TODO We could use Texture2D instead it.
                    SRVs[3] = m_importanceMap;
                    SRVs[4] = m_finalResults;
                }
//    #endif
//                dx11Context->PSSetShaderResources( SSAO_TEXTURE_SLOT0, 5, SRVs );
                // TODO binding the textures, we later do this....

                int shaderIndex = Math.max( 0, (!adaptiveBasePass)?(settings.QualityLevel):(4) );
                FullscreenPassDraw(m_pixelShaderGenerate[shaderIndex], viewportWidth, viewportHeight, null );
                if (!g_PrintOnce)
                {
//                    sprintf_s(debugName, "%sGenerateSSAO_Pass%d.txt", DEBUG_FILE_FOLDER, pass);
//                    DEBUG_TEXTURE2D(dx11Context, rts[0], debugName)
                    saveTextData("GenerateSSAO_Pass" + pass + "_Adaptive" + adaptive, rts[0]);
                }

                // remove textures from slots 0, 1, 2, 3 to avoid API complaints
//                dx11Context->PSSetShaderResources( SSAO_TEXTURE_SLOT0, 5, zeroSRVs );
                // TODO unbinding textures.
            }
            
            // Blur
            if( blurPasses > 0 )
            {
                int wideBlursRemaining = Math.max( 0, blurPasses-2 );

                for( int i = 0; i < blurPasses; i++ )
                {
                    // remove textures to avoid API complaints
//                    dx11Context->PSSetShaderResources( SSAO_TEXTURE_SLOT0, _countof( zeroSRVs ), zeroSRVs );

//                    ID3D11RenderTargetView * rts[] = { pPongRT->RTV };
                	Texture2D[] rts = {pPongRT};
                    
                    // last pass?
                    if( i == (blurPasses-1) )
                        rts[0] = m_finalResultsArrayViews[pass];

//                    dx11Context->OMSetRenderTargets( _countof( rts ), rts, NULL );
                    setRenderTargets(rts);

//                    ID3D11ShaderResourceView * SRVs[] = { pPingRT->SRV };
//                    dx11Context->PSSetShaderResources( SSAO_TEXTURE_SLOT2, _countof(SRVs), SRVs );
                    bind(pPingRT, RenderTechnique.TEX2D_BLUR_INPUT, m_samplerStatePointMirror);

                    if( settings.QualityLevel > 0 )
                    {
                        if( wideBlursRemaining > 0 )
                        {
                            FullscreenPassDraw(m_pixelShaderSmartBlurWide, viewportWidth, viewportHeight, null );
                            if (!g_PrintOnce)
                            {
                                /*sprintf_s(debugName, "%sGenerateSSAO_Pass%d_Blur_Pass%d.txt", DEBUG_FILE_FOLDER, pass, i);
                                DEBUG_TEXTURE2D(dx11Context, rts[0], debugName)*/
                                saveTextData(String.format("GenerateSSAO_Pass%d_Adaptive%d_Blur_Pass%d.txt", pass,adaptive, i), rts[0]);
                            }
                            wideBlursRemaining--;
                        }
                        else
                        {
                            FullscreenPassDraw(m_pixelShaderSmartBlur, viewportWidth, viewportHeight, null );
                            if (!g_PrintOnce)
                            {
                                /*sprintf_s(debugName, "%sGenerateSSAO_Pass%d_Blur_Pass%d.txt", DEBUG_FILE_FOLDER, pass, i);
                                DEBUG_TEXTURE2D(dx11Context, rts[0], debugName)*/
                                saveTextData(String.format("GenerateSSAO_Pass%d_Adaptive%d_Blur_Pass%d.txt", pass,adaptive, i), rts[0]);
                            }
                        }
                    }
                    else
                    {
                        FullscreenPassDraw(m_pixelShaderNonSmartBlur, viewportWidth, viewportHeight, null ); // just for quality level 0 (and -1)
                        if (!g_PrintOnce)
                        {
                            /*sprintf_s(debugName, "%sGenerateSSAO_Pass%d_Blur_Pass%d.txt", DEBUG_FILE_FOLDER, pass, i);
                            DEBUG_TEXTURE2D(dx11Context, rts[0], debugName)*/
                            saveTextData(String.format("GenerateSSAO_Pass%d_Adaptive%d_Blur_Pass%d.txt", pass,adaptive, i), rts[0]);
                        }
                    }

//                    pPingRT.unbind();
                    bind(null, RenderTechnique.TEX2D_BLUR_INPUT, 0);
                    
//                    Swap( pPingRT, pPongRT );
                    Texture2D tmp = pPingRT;
                    pPingRT = pPongRT;
                    pPongRT = tmp;
                }
            }
            

            // remove textures to avoid API complaints
//            dx11Context->PSSetShaderResources( SSAO_TEXTURE_SLOT0, _countof(zeroSRVs), zeroSRVs );
        }

        gl.glDisable(GLenum.GL_SCISSOR_TEST);
    }

	@Override
	public void PreAllocateVideoMemory(ASSAO_Inputs _inputs) {
		////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
		// TODO: dynamic_cast if supported in _DEBUG to check for correct type cast below
		////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
		final ASSAO_InputsOpenGL inputs = (ASSAO_InputsOpenGL)( _inputs );
		
		UpdateTextures( inputs );
	}

	@Override
	public void DeleteAllocatedVideoMemory() {
	}

	@Override
	public int GetAllocatedVideoMemory() {
		return m_allocatedVRAM;
	}

	private void bind(TextureGL tex, int unit, int sampler){
        gl.glActiveTexture(GLenum.GL_TEXTURE0 + unit);
        if(tex != null){
            gl.glBindTexture(tex.getTarget(), tex.getTexture());
        }else{
            gl.glBindTexture(GLenum.GL_TEXTURE_2D, 0);
        }

        gl.glBindSampler(unit, sampler);
    }

	@Override
	public void Draw(ASSAO_Settings settings, ASSAO_Inputs _inputs) {
		////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
		// TODO: dynamic_cast if supported in _DEBUG to check for correct type cast below
		////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
		final ASSAO_InputsOpenGL inputs = (ASSAO_InputsOpenGL)( _inputs );
		assert( settings.QualityLevel >= -1 && settings.QualityLevel <= 3 );
		
		UpdateTextures( inputs );

	    UpdateConstants( settings, inputs, 0, "PrepareDepths" );

//	    ID3D11DeviceContext * dx11Context = inputs->DeviceContext;

	    {
	        // Backup D3D11 states (will be restored when it goes out of scope)
//	        D3D11SSAOStateBackupRAII d3d11StatesBackup( dx11Context );

	        if( m_requiresClear )
	        {
//	            float fourZeroes[/*4*/] = { 0, 0, 0, 0 };
	            float fourOnes[/*4*/]   = { 1, 1, 1, 1 };
//	            dx11Context->ClearRenderTargetView( m_halfDepths[0].RTV, fourZeroes );
//	            dx11Context->ClearRenderTargetView( m_halfDepths[1].RTV, fourZeroes );
//	            dx11Context->ClearRenderTargetView( m_halfDepths[2].RTV, fourZeroes );
//	            dx11Context->ClearRenderTargetView( m_halfDepths[3].RTV, fourZeroes );
//	            dx11Context->ClearRenderTargetView( m_pingPongHalfResultA.RTV, fourOnes );
//	            dx11Context->ClearRenderTargetView( m_pingPongHalfResultB.RTV, fourZeroes );
//	            dx11Context->ClearRenderTargetView( m_finalResultsArrayViews[0].RTV, fourOnes );
//	            dx11Context->ClearRenderTargetView( m_finalResultsArrayViews[1].RTV, fourOnes );
//	            dx11Context->ClearRenderTargetView( m_finalResultsArrayViews[2].RTV, fourOnes );
//	            dx11Context->ClearRenderTargetView( m_finalResultsArrayViews[3].RTV, fourOnes );
//	            if( m_normals.RTV != NULL ) dx11Context->ClearRenderTargetView( m_normals.RTV, fourZeroes );
//	#ifdef INTEL_SSAO_ENABLE_ADAPTIVE_QUALITY
//	            dx11Context->ClearRenderTargetView( m_importanceMap.RTV, fourZeroes );
//	            dx11Context->ClearRenderTargetView( m_importanceMapPong.RTV, fourZeroes );
//	#endif
                gl.glClearTexImage(m_halfDepths[0].getTexture(), 0, GLenum.GL_RED, GLenum.GL_FLOAT, (ByteBuffer)null);
                gl.glClearTexImage(m_halfDepths[1].getTexture(), 0, GLenum.GL_RED, GLenum.GL_FLOAT, (ByteBuffer)null);
                gl.glClearTexImage(m_halfDepths[2].getTexture(), 0, GLenum.GL_RED, GLenum.GL_FLOAT, (ByteBuffer)null);
                gl.glClearTexImage(m_halfDepths[3].getTexture(), 0, GLenum.GL_RED, GLenum.GL_FLOAT, (ByteBuffer)null);
                gl.glClearTexImage(m_pingPongHalfResultB.getTexture(), 0, GLenum.GL_RG, GLenum.GL_UNSIGNED_BYTE, (ByteBuffer)null);
                gl.glClearTexImage(m_normals.getTexture(), 0, TextureUtils.measureFormat(m_normals.getFormat()), TextureUtils.measureDataType(m_normals.getFormat()), (ByteBuffer)null);

                gl.glBindFramebuffer(GLenum.GL_FRAMEBUFFER, m_framebuffer);
                gl.glFramebufferTexture2D(GLenum.GL_FRAMEBUFFER, GLenum.GL_COLOR_ATTACHMENT0, m_pingPongHalfResultA.getTarget(), m_pingPongHalfResultA.getTexture(), 0);
                gl.glDrawBuffers(GLenum.GL_COLOR_ATTACHMENT0);
                gl.glClearBufferfv(GLenum.GL_COLOR, 0, CacheBuffer.wrap(fourOnes));
	        	
	        	for(int i = 0; i < 4; i++){
                    gl.glFramebufferTexture2D(GLenum.GL_FRAMEBUFFER, GLenum.GL_COLOR_ATTACHMENT0, m_finalResultsArrayViews[i].getTarget(), m_finalResultsArrayViews[i].getTexture(), 0);
                    gl.glClearBufferfv(GLenum.GL_COLOR, 0, CacheBuffer.wrap(fourOnes));
	        	}
	        	
	            m_requiresClear = false;
	            GLCheck.checkError();
	        }

	        // Set effect samplers
//	        ID3D11SamplerState * samplers[] =
//	        {
//	            m_samplerStatePointClamp,
//	            m_samplerStateLinearClamp,
//	            m_samplerStatePointMirror,
//	            m_samplerStateViewspaceDepthTap,
//	        };
//	        dx11Context->PSSetSamplers( 0, _countof( samplers ), samplers );

	        // Set constant buffer
//	        dx11Context->PSSetConstantBuffers( SSAO_CONSTANTS_BUFFERSLOT, 1, &m_constantsBuffer );

	        // Generate depths
	        PrepareDepths( settings, inputs );

//	#ifdef INTEL_SSAO_ENABLE_ADAPTIVE_QUALITY
	        // for adaptive quality, importance map pass
	        if( settings.QualityLevel == 3 )
	        {
	            // Generate simple quality SSAO
	            {
	                GenerateSSAO( settings, inputs, true );
	            }

	            // Generate importance map
	            {
//	                CD3D11_VIEWPORT viewport = CD3D11_VIEWPORT( 0.0f, 0.0f, (float)m_quarterSize.x, (float)m_quarterSize.y );
//	                CD3D11_RECT rect = CD3D11_RECT( 0, 0, m_quarterSize.x, m_quarterSize.y );
//	                dx11Context->RSSetViewports( 1, &viewport );
//	                dx11Context->RSSetScissorRects( 1, &rect );
	            	final int viewportWidth = m_quarterSize.x;
	            	final int viewportHeight = m_quarterSize.y; 

//	                ID3D11ShaderResourceView * zeroSRVs[] = { NULL, NULL, NULL, NULL, NULL };

	                // drawing into importanceMap
//	                dx11Context->OMSetRenderTargets( 1, &m_importanceMap.RTV, NULL );
	            	setRenderTargets(m_importanceMap);

	                // select 4 deinterleaved AO textures (texture array)
//	                dx11Context->PSSetShaderResources( SSAO_TEXTURE_SLOT4, 1, &m_finalResults.SRV );
	            	bind(m_finalResults, RenderTechnique.TEX2D_FINAL_SSAO, m_samplerStatePointClamp);
	                FullscreenPassDraw(m_pixelShaderGenerateImportanceMap, viewportWidth, viewportHeight, null );
                    if (!g_PrintOnce)
                    {
                        /*sprintf_s(debugName, "%sImportanceMap.txt", DEBUG_FILE_FOLDER);
                        DEBUG_TEXTURE2D(dx11Context, m_importanceMap.RTV, debugName)*/
                        saveTextData("ImportanceMap.txt", m_importanceMap);
                    }

	                // postprocess A
//	                dx11Context->OMSetRenderTargets( 1, &m_importanceMapPong.RTV, NULL );
	                setRenderTargets(m_importanceMapPong);
//	                dx11Context->PSSetShaderResources( SSAO_TEXTURE_SLOT3, 1, &m_importanceMap.SRV );
	                bind(m_importanceMap, RenderTechnique.TEX2D_IMPORTANCE_MAP, m_samplerStateLinearClamp);
	                FullscreenPassDraw(m_pixelShaderPostprocessImportanceMapA, viewportWidth, viewportHeight, null );
//	                dx11Context->PSSetShaderResources( SSAO_TEXTURE_SLOT3, 1, zeroSRVs );
                    if (!g_PrintOnce)
                    {
                        /*sprintf_s(debugName, "%sImportanceMapA.txt", DEBUG_FILE_FOLDER);
                        DEBUG_TEXTURE2D(dx11Context, m_importanceMapPong.RTV, debugName)*/
                        saveTextData("ImportanceMapA.txt", m_importanceMapPong);
                    }

	                // postprocess B
//	                UINT fourZeroes[4] = { 0, 0, 0, 0 };
//	                dx11Context->ClearUnorderedAccessViewUint( m_loadCounterUAV, fourZeroes );
//	                GL44.glClearTexImage(texture, level, format, type, data);  TODO clear the m_loadCounter
//	                dx11Context->OMSetRenderTargetsAndUnorderedAccessViews( 1, &m_importanceMap.RTV, NULL, SSAO_LOAD_COUNTER_UAV_SLOT, 1, &m_loadCounterUAV, NULL );
	                setRenderTargets(m_importanceMap);
	                gl.glBindImageTexture(0, m_loadCounter, 0, false, 0, GLenum.GL_READ_WRITE, GLenum.GL_R32UI);
	                
	                // select previous pass input importance map
//	                dx11Context->PSSetShaderResources( SSAO_TEXTURE_SLOT3, 1, &m_importanceMapPong.SRV );
	                bind(m_importanceMapPong, RenderTechnique.TEX2D_IMPORTANCE_MAP, m_samplerStateLinearClamp);
	                FullscreenPassDraw(m_pixelShaderPostprocessImportanceMapB, viewportWidth, viewportHeight, null );
//	                dx11Context->PSSetShaderResources( SSAO_TEXTURE_SLOT3, 1, zeroSRVs );
                    if (!g_PrintOnce)
                    {
                        /*sprintf_s(debugName, "%sImportanceMapB.txt", DEBUG_FILE_FOLDER);
                        DEBUG_TEXTURE2D(dx11Context, m_importanceMap.RTV, debugName);*/
                        saveTextData("ImportanceMapB.txt", m_importanceMap);
                    }
	            }
	        }
//	#endif
	        // Generate SSAO
	        GenerateSSAO( settings, inputs, false );

	        if( inputs.OverrideOutputRTV != null )
	        {
	            // drawing into OverrideOutputRTV
//	            dx11Context->OMSetRenderTargets( 1, &inputs->OverrideOutputRTV, NULL );
	        	setRenderTargets(inputs.OverrideOutputRTV);
	        }
	        else
	        {
	            // restore previous RTs
//	            d3d11StatesBackup.RestoreRTs( );
                gl.glBindFramebuffer(GLenum.GL_FRAMEBUFFER, 0);
                gl.glDisable(GLenum.GL_DEPTH_TEST);
//	        	GL11.glClearColor(0, 0, 0, 1);
//	        	GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);
	        }

	        // Apply
	        {
	            // select 4 deinterleaved AO textures (texture array)
//	            dx11Context->PSSetShaderResources( SSAO_TEXTURE_SLOT4, 1, &m_finalResults.SRV );
//	        	m_finalResults.bind(RenderTechnique.TEX2D_FINAL_SSAO, m_samplerStateLinearClamp);
                gl.glActiveTexture(GLenum.GL_TEXTURE0 + RenderTechnique.TEX2D_FINAL_SSAO);
                gl.glBindTexture(m_finalResults.getTarget(), m_finalResults.getTexture());
                gl.glBindSampler(RenderTechnique.TEX2D_FINAL_SSAO, m_samplerStateLinearClamp);

//	            CD3D11_VIEWPORT viewport = CD3D11_VIEWPORT( 0.0f, 0.0f, (float)m_size.x, (float)m_size.y );
//	            CD3D11_RECT rect = CD3D11_RECT( m_fullResOutScissorRect.x, m_fullResOutScissorRect.y, m_fullResOutScissorRect.z, m_fullResOutScissorRect.w );
//	            dx11Context->RSSetViewports( 1, &viewport );
//	            dx11Context->RSSetScissorRects( 1, &rect );
	            
	            final int viewportWidth = m_size.x;
            	final int viewportHeight = m_size.y;

                gl.glEnable(GLenum.GL_SCISSOR_TEST);
                gl.glScissor(m_halfResOutScissorRect.x, m_halfResOutScissorRect.y, m_halfResOutScissorRect.z, m_halfResOutScissorRect.w);

//	            ID3D11BlendState * blendState = ( inputs->DrawOpaque ) ? ( m_blendStateOpaque ) : ( m_blendStateMultiply );
            	Runnable blendState = (inputs.DrawOpaque) ? null : ()->blendStateMultiply();
	            
	            if( settings.QualityLevel < 0 )
	                FullscreenPassDraw( m_pixelShaderNonSmartHalfApply, viewportWidth, viewportHeight, blendState );
	            else if( settings.QualityLevel == 0 )
	                FullscreenPassDraw( m_pixelShaderNonSmartApply, viewportWidth, viewportHeight, blendState );
	            else
	                FullscreenPassDraw( m_pixelShaderApply, viewportWidth, viewportHeight, blendState );

                gl.glDisable(GLenum.GL_SCISSOR_TEST);
	        }

	        // restore previous RTs again (because of the viewport hack)
//	        d3d11StatesBackup.RestoreRTs( );

	    //    FullscreenPassDraw( dx11Context, m_pixelShaderDebugDraw );

	    }
	    
	    m_printProgramOnce = true;
	}
	
	private static final class BufferFormats{
		int         DepthBufferViewspaceLinear;
		int         AOResult;
		int         Normals;
		int         ImportanceMap;

        BufferFormats( )
        {
            DepthBufferViewspaceLinear  = GLenum.GL_R16F;        // increase this to DXGI_FORMAT_R32_FLOAT if using very low FOVs (for a scope effect) or similar, or in case you suspect artifacts caused by lack of precision; performance will degrade
            Normals                     = GLenum.GL_RGBA8;
            AOResult                    = GLenum.GL_RG8;
            ImportanceMap               = GLenum.GL_R8;
        }
	}

}
