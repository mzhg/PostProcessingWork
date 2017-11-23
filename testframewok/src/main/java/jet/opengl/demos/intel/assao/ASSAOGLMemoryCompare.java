package jet.opengl.demos.intel.assao;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector2i;
import org.lwjgl.util.vector.Vector4i;

import java.io.IOException;

import jet.opengl.postprocessing.common.GLenum;
import jet.opengl.postprocessing.texture.Texture2D;
import jet.opengl.postprocessing.texture.TextureGL;
import jet.opengl.postprocessing.util.DebugTools;
import jet.opengl.postprocessing.util.Numeric;

final class ASSAOGLMemoryCompare implements ASSAO_Effect, ASSAO_Macro{

    static boolean ASSAO_DEBUG;
    static boolean g_PrintOnce = false;

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

    private boolean                         m_requiresClear;
    private int 							m_viewportWidth, m_viewportHeight;
    private boolean 					    m_printProgramOnce = false;
    private boolean                         m_adaptive;

    ASSAOGLMemoryCompare(boolean adaptive){
        m_adaptive = adaptive;
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
                testConstants(debugName);
                testSetting(debugName);
            }

        }
    }

    private void testConstants(String filename/*, boolean adaptive*/){
        boolean adaptive = m_adaptive;
        System.out.println("test ASSAOConstants: " + filename);
        final String FILE_PATH  = "E:/textures/ASSAO%s%sASSAOConstants_%s.dat";

        String gl_file = String.format(FILE_PATH, "GL", adaptive ? "_ADAPTIVE/" : "/", filename);
        String dx_file = String.format(FILE_PATH, "DX", adaptive ? "_ADAPTIVE/" : "/", filename);
        String result_file = String.format(FILE_PATH, "Result/", "", filename);

        ASSAOConstants gl_data = new ASSAOConstants();
        gl_data.load(DebugTools.loadBinary(gl_file));

        ASSAOConstants dx_data = new ASSAOConstants();
        dx_data.load(DebugTools.loadBinary(dx_file));

        String result = DebugTools.compareObjects(gl_data, dx_data).toString();
//        System.out.println(result);
        DebugTools.write(result, result_file);
    }

    private void testSetting(String filename/*, boolean adaptive*/){
        boolean adaptive = m_adaptive;
        System.out.println("test ASSAO_Settings: " + filename);
        final String FILE_PATH  = "E:/textures/ASSAO%s%sASSAO_Settings_%s.dat";

        String gl_file = String.format(FILE_PATH, "GL", adaptive ? "_ADAPTIVE/" : "/", filename);
        String dx_file = String.format(FILE_PATH, "DX", adaptive ? "_ADAPTIVE/" : "/", filename);
        String result_file = String.format(FILE_PATH, "Result/", "", filename);

        ASSAO_Settings gl_data = new ASSAO_Settings();
        gl_data.load(DebugTools.loadBinary(gl_file));

        ASSAO_Settings dx_data = new ASSAO_Settings();
        dx_data.load(DebugTools.loadBinary(dx_file));

        String result = DebugTools.compareObjects(gl_data, dx_data).toString();
//        System.out.println(result);
        DebugTools.write(result, result_file);
    }

    private void testTexture(String filename/*, boolean adaptive*/){
        boolean adaptive = m_adaptive;
        System.out.println("test: " + filename + (adaptive ? " adaptive" : ""));
        final String FILE_PATH  = "E:/textures/ASSAO%s%s%s.txt";

        String gl_file = String.format(FILE_PATH, "GL", adaptive ? "_ADAPTIVE/" : "/", filename);
        String dx_file = String.format(FILE_PATH, "DX", adaptive ? "_ADAPTIVE/" : "/", filename);
        String result_file = String.format(FILE_PATH, "Result/", filename, "");

        DebugTools.fileCompare(gl_file, dx_file, result_file);
        System.out.println();
    }

    void FullscreenPassDraw(RenderTechnique pixelShader, int width, int height, Runnable blendState ){}
    
    private void setRenderTargets(Texture2D...textures){

    }
    
    private void unbindRenderTargets(int count){
    	for(int i = 1; i < count; i++){
    	}
    }
    
    void PrepareDepths( ASSAO_Settings settings, ASSAO_InputsOpenGL inputs ){
    	boolean generateNormals = inputs.NormalSRV == null;
    	int viewportWidth = m_halfSize.x;
    	int viewportHeight = m_halfSize.y;


        if (!g_PrintOnce)
        {
//            DEBUG_TEXTURE2D_BINARY(dx11Context, inputs->DepthSRV, DEBUG_FILE_FOLDER"DepthSRV.dat");
        }

        if( !generateNormals )
        {
            //VA_SCOPE_CPUGPU_TIMER( PrepareDepths, drawContext.APIContext );

            if( settings.QualityLevel < 0 )
            {
//                dx11Context->OMSetRenderTargets( _countof(twoDepths), twoDepths, NULL );
            	/*setRenderTargets(twoDepths);
            	FullscreenPassDraw(m_pixelShaderPrepareDepthsHalf, viewportWidth, viewportHeight, null );*/
                if (!g_PrintOnce)
                {
//                    DEBUG_TEXTURE2D(dx11Context, m_halfDepths[0].RTV, DEBUG_FILE_FOLDER);
//                    DEBUG_TEXTURE2D(dx11Context, m_halfDepths[3].RTV, DEBUG_FILE_FOLDER"halfDepths[3].txt");
                    /*saveTextData("halfDepths[0].txt", m_halfDepths[0]);
                    saveTextData("halfDepths[3].txt", m_halfDepths[3]);*/
                    testTexture("halfDepths[0]");
                    testTexture("halfDepths[3]");

                }
//            	unbindRenderTargets(twoDepths.length);
            }
            else
            {
            	/*setRenderTargets(fourDepths);
            	FullscreenPassDraw(m_pixelShaderPrepareDepths , viewportWidth, viewportHeight, null );
            	unbindRenderTargets(fourDepths.length);*/

                if (!g_PrintOnce)
                {
                    /*DEBUG_TEXTURE2D(dx11Context, m_halfDepths[0].RTV, DEBUG_FILE_FOLDER"halfDepths[0].txt");
                    DEBUG_TEXTURE2D(dx11Context, m_halfDepths[1].RTV, DEBUG_FILE_FOLDER"halfDepths[1].txt");
                    DEBUG_TEXTURE2D(dx11Context, m_halfDepths[2].RTV, DEBUG_FILE_FOLDER"halfDepths[2].txt");
                    DEBUG_TEXTURE2D(dx11Context, m_halfDepths[3].RTV, DEBUG_FILE_FOLDER"halfDepths[3].txt");*/

                    /*saveTextData("halfDepths[0].txt", m_halfDepths[0]);
                    saveTextData("halfDepths[1].txt", m_halfDepths[1]);
                    saveTextData("halfDepths[2].txt", m_halfDepths[2]);
                    saveTextData("halfDepths[3].txt", m_halfDepths[3]);*/

                    testTexture("halfDepths[0]");
                    testTexture("halfDepths[1]");
                    testTexture("halfDepths[2]");
                    testTexture("halfDepths[3]");
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
            	/*setRenderTargets(twoDepths);
                gl.glBindImageTexture(0, m_normals.getTexture(), 0, false, 0, GLenum.GL_WRITE_ONLY, GLenum.GL_RGBA8);
            	
                FullscreenPassDraw(m_pixelShaderPrepareDepthsAndNormalsHalf, viewportWidth, viewportHeight, null);*/
                if (!g_PrintOnce)
                {
                    /*DEBUG_TEXTURE2D(dx11Context, m_normals.UAV,		  DEBUG_FILE_FOLDER"GenerateNormals.txt");
                    DEBUG_TEXTURE2D(dx11Context, m_halfDepths[0].RTV, DEBUG_FILE_FOLDER"halfDepths[0].txt");
                    DEBUG_TEXTURE2D(dx11Context, m_halfDepths[3].RTV, DEBUG_FILE_FOLDER"halfDepths[3].txt");*/

                    /*saveTextData("GenerateNormals.txt", m_normals);
                    saveTextData("halfDepths[0].txt", m_halfDepths[0]);
                    saveTextData("halfDepths[3].txt", m_halfDepths[3]);*/

                    testTexture("GenerateNormals");
                    testTexture("halfDepths[0]");
                    testTexture("halfDepths[3]");
                }
                
                /*unbindRenderTargets(twoDepths.length);
                gl.glBindImageTexture(0, 0, 0, false, 0, GLenum.GL_WRITE_ONLY, GLenum.GL_RGBA8);*/
            }
            else
            {
//                dx11Context->OMSetRenderTargetsAndUnorderedAccessViews( _countof(fourDepths), fourDepths, NULL, SSAO_NORMALMAP_OUT_UAV_SLOT, 1, UAVs, NULL );
                /*setRenderTargets(fourDepths);
                gl.glBindImageTexture(0, m_normals.getTexture(), 0, false, 0, GLenum.GL_WRITE_ONLY, GLenum.GL_RGBA8);
                FullscreenPassDraw(m_pixelShaderPrepareDepthsAndNormals, viewportWidth, viewportHeight, null );*/

                if (!g_PrintOnce)
                {
                    /*DEBUG_TEXTURE2D(dx11Context, m_normals.UAV, DEBUG_FILE_FOLDER"GenerateNormals.txt");
                    DEBUG_TEXTURE2D(dx11Context, m_halfDepths[0].RTV, DEBUG_FILE_FOLDER"halfDepths[0].txt");
                    DEBUG_TEXTURE2D(dx11Context, m_halfDepths[1].RTV, DEBUG_FILE_FOLDER"halfDepths[1].txt");
                    DEBUG_TEXTURE2D(dx11Context, m_halfDepths[2].RTV, DEBUG_FILE_FOLDER"halfDepths[2].txt");
                    DEBUG_TEXTURE2D(dx11Context, m_halfDepths[3].RTV, DEBUG_FILE_FOLDER"halfDepths[3].txt");*/
                    /*saveTextData("GenerateNormals.txt", m_normals);
                    saveTextData("halfDepths[0].txt", m_halfDepths[0]);
                    saveTextData("halfDepths[1].txt", m_halfDepths[1]);
                    saveTextData("halfDepths[2].txt", m_halfDepths[2]);
                    saveTextData("halfDepths[3].txt", m_halfDepths[3]);*/
                    testTexture("GenerateNormals");
                    testTexture("halfDepths[0]");
                    testTexture("halfDepths[1]");
                    testTexture("halfDepths[2]");
                    testTexture("halfDepths[3]");
                }
                
                /*unbindRenderTargets(fourDepths.length);
                gl.glBindImageTexture(0, 0, 0, false, 0, GLenum.GL_WRITE_ONLY, GLenum.GL_RGBA8);*/
            }
        }

        // only do mipmaps for higher quality levels (not beneficial on quality level 1, and detrimental on quality level 0)
        if( settings.QualityLevel > 1 )
        {
            //VA_SCOPE_CPUGPU_TIMER( PrepareDepthMips, drawContext.APIContext );

            for( int i = 1; i < m_depthMipLevels; i++ )
            {
//                Texture2D fourDepthMips[] = { m_halfDepthsMipViews[0][i], m_halfDepthsMipViews[1][i], m_halfDepthsMipViews[2][i], m_halfDepthsMipViews[3][i]};
//                viewportWidth = m_halfDepthsMipViews[0][i].getWidth();
//                viewportHeight = m_halfDepthsMipViews[0][i].getHeight();
//                Texture2D fourSRVs[] = { m_halfDepthsMipViews[0][i - 1], m_halfDepthsMipViews[1][i - 1], m_halfDepthsMipViews[2][i - 1], m_halfDepthsMipViews[3][i - 1] };
//                setRenderTargets(fourDepthMips);  // setup rendering targets

//                for(int id = 0; id < fourSRVs.length; id++){  // binding texture resources.
//                	bind(fourSRVs[id], RenderTechnique.TEX2D_VIEW_DEPTH + id, 0);
//                }
//
//                FullscreenPassDraw(m_pixelShaderPrepareDepthMip[i - 1], viewportWidth, viewportHeight, null  );
                if (!g_PrintOnce)
                {
                    for (int j = 0; j < /*fourDepthMips.length*/4; j++)
                    {
                        String filename = String.format("DepthMipViews[%d][%d].txt", j, i);
//                        DEBUG_TEXTURE2D(dx11Context, fourDepthMips[j], filename);
//                        saveTextureAsText(fourDepthMips[j], filename);
                        testTexture(filename);
                    }
                }
//                unbindRenderTargets(fourDepthMips.length);
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
//    	Texture2D normalmapSRV = (inputs.NormalSRV == null) ? (m_normals) : (inputs.NormalSRV);

//        ID3D11DeviceContext * dx11Context = inputs->DeviceContext;

        {
//            CD3D11_VIEWPORT viewport = CD3D11_VIEWPORT( 0.0f, 0.0f, (float)m_halfSize.x, (float)m_halfSize.y );
//            CD3D11_RECT rect = CD3D11_RECT( m_halfResOutScissorRect.x, m_halfResOutScissorRect.y, m_halfResOutScissorRect.z, m_halfResOutScissorRect.w );
//            dx11Context->RSSetViewports( 1, &viewport );
//            dx11Context->RSSetScissorRects( 1, &rect );
//            gl.glEnable(GLenum.GL_SCISSOR_TEST);
//            gl.glScissor(m_halfResOutScissorRect.x, m_halfResOutScissorRect.y, m_halfResOutScissorRect.z, m_halfResOutScissorRect.w);
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

//            Texture2D pPingRT = m_pingPongHalfResultA;
//            Texture2D pPongRT = m_pingPongHalfResultB;

            // Generate
            {
                //VA_SCOPE_CPUGPU_TIMER_NAMED( Generate, vaStringTools::Format( "Generate_pass%d", pass ), drawContext.APIContext );

                // remove textures from slots 0, 1, 2, 3 to avoid API complaints
//                dx11Context->PSSetShaderResources( SSAO_TEXTURE_SLOT0, 5, zeroSRVs );
            	// TODO unbind resources.

//                ID3D11RenderTargetView * rts[] = { pPingRT->RTV };
            	/*Texture2D[] rts = {pPingRT};
                if( blurPasses == 0 )
                    rts[0] = m_finalResultsArrayViews[pass];
                setRenderTargets(rts);*/

//                ID3D11ShaderResourceView * SRVs[] = { m_halfDepths[pass].SRV, normalmapSRV, NULL, NULL, NULL }; // m_loadCounterSRV used only for quality level 3
                /*Texture2D[] SRVs = {m_halfDepths[pass], normalmapSRV, null, null, null};
                if( !adaptiveBasePass && (settings.QualityLevel == 3) )
                {
//                    SRVs[2] = m_loadCounterSRV;  TODO We could use Texture2D instead it.
                    SRVs[3] = m_importanceMap;
                    SRVs[4] = m_finalResults;
                }
//                dx11Context->PSSetShaderResources( SSAO_TEXTURE_SLOT0, 5, SRVs );
                // TODO binding the textures, we later do this....

                int shaderIndex = Math.max( 0, (!adaptiveBasePass)?(settings.QualityLevel):(4) );
                FullscreenPassDraw(m_pixelShaderGenerate[shaderIndex], viewportWidth, viewportHeight, null );*/
                if (!g_PrintOnce)
                {
//                    sprintf_s(debugName, "%sGenerateSSAO_Pass%d.txt", DEBUG_FILE_FOLDER, pass);
//                    DEBUG_TEXTURE2D(dx11Context, rts[0], debugName)
//                    saveTextData("GenerateSSAO_Pass" + pass + "_Adaptive" + adaptive, rts[0]);
                    testTexture("GenerateSSAO_Pass" + pass + "_Adaptive" + adaptive);
                }

                // remove textures from slots 0, 1, 2, 3 to avoid API complaints
//                dx11Context->PSSetShaderResources( SSAO_TEXTURE_SLOT0, 5, zeroSRVs );
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
                	/*Texture2D[] rts = {pPongRT};
                    if( i == (blurPasses-1) )
                        rts[0] = m_finalResultsArrayViews[pass];
                    setRenderTargets(rts);
                    bind(pPingRT, RenderTechnique.TEX2D_BLUR_INPUT, m_samplerStatePointMirror);*/

                    if( settings.QualityLevel > 0 )
                    {
                        if( wideBlursRemaining > 0 )
                        {
//                            FullscreenPassDraw(m_pixelShaderSmartBlurWide, viewportWidth, viewportHeight, null );
                            if (!g_PrintOnce)
                            {
                                /*sprintf_s(debugName, "%sGenerateSSAO_Pass%d_Blur_Pass%d.txt", DEBUG_FILE_FOLDER, pass, i);
                                DEBUG_TEXTURE2D(dx11Context, rts[0], debugName)*/
//                                saveTextData(String.format("GenerateSSAO_Pass%d_Adaptive%d_Blur_Pass%d.txt", pass,adaptive, i), rts[0]);
                                testTexture(String.format("GenerateSSAO_Pass%d_Adaptive%d_Blur_Pass%d.txt", pass,adaptive, i));
                            }
                            wideBlursRemaining--;
                        }
                        else
                        {
//                            FullscreenPassDraw(m_pixelShaderSmartBlur, viewportWidth, viewportHeight, null );
                            if (!g_PrintOnce)
                            {
                                /*sprintf_s(debugName, "%sGenerateSSAO_Pass%d_Blur_Pass%d.txt", DEBUG_FILE_FOLDER, pass, i);
                                DEBUG_TEXTURE2D(dx11Context, rts[0], debugName)*/
//                                saveTextData(String.format("GenerateSSAO_Pass%d_Adaptive%d_Blur_Pass%d.txt", pass,adaptive, i), rts[0]);
                                testTexture(String.format("GenerateSSAO_Pass%d_Adaptive%d_Blur_Pass%d.txt", pass,adaptive, i));
                            }
                        }
                    }
                    else
                    {
//                        FullscreenPassDraw(m_pixelShaderNonSmartBlur, viewportWidth, viewportHeight, null ); // just for quality level 0 (and -1)
                        if (!g_PrintOnce)
                        {
                            /*sprintf_s(debugName, "%sGenerateSSAO_Pass%d_Blur_Pass%d.txt", DEBUG_FILE_FOLDER, pass, i);
                            DEBUG_TEXTURE2D(dx11Context, rts[0], debugName)*/
//                            saveTextData(String.format("GenerateSSAO_Pass%d_Adaptive%d_Blur_Pass%d.txt", pass,adaptive, i), rts[0]);
                            testTexture(String.format("GenerateSSAO_Pass%d_Adaptive%d_Blur_Pass%d.txt", pass,adaptive, i));
                        }
                    }

//                    pPingRT.unbind();
                    bind(null, RenderTechnique.TEX2D_BLUR_INPUT, 0);
                    
//                    Swap( pPingRT, pPongRT );
                    /*Texture2D tmp = pPingRT;
                    pPingRT = pPongRT;
                    pPongRT = tmp;*/
                }
            }
            

            // remove textures to avoid API complaints
//            dx11Context->PSSetShaderResources( SSAO_TEXTURE_SLOT0, _countof(zeroSRVs), zeroSRVs );
        }

//        gl.glDisable(GLenum.GL_SCISSOR_TEST);
    }

	@Override
	public void PreAllocateVideoMemory(ASSAO_Inputs _inputs) {
		////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
		// TODO: dynamic_cast if supported in _DEBUG to check for correct type cast below
		////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
		final ASSAO_InputsOpenGL inputs = (ASSAO_InputsOpenGL)( _inputs );
		
//		UpdateTextures( inputs );
	}

	@Override
	public void DeleteAllocatedVideoMemory() {
	}

	@Override
	public int GetAllocatedVideoMemory() {
		return m_allocatedVRAM;
	}

	private void bind(TextureGL tex, int unit, int sampler){
        /*gl.glActiveTexture(GLenum.GL_TEXTURE0 + unit);
        if(tex != null){
            gl.glBindTexture(tex.getTarget(), tex.getTexture());
        }else{
            gl.glBindTexture(GLenum.GL_TEXTURE_2D, 0);
        }

        gl.glBindSampler(unit, sampler);*/
    }

	@Override
	public void Draw(ASSAO_Settings settings, ASSAO_Inputs _inputs) {
		////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
		// TODO: dynamic_cast if supported in _DEBUG to check for correct type cast below
		////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
		final ASSAO_InputsOpenGL inputs = (ASSAO_InputsOpenGL)( _inputs );
		assert( settings.QualityLevel >= -1 && settings.QualityLevel <= 3 );
		
//		UpdateTextures( inputs );

	    UpdateConstants( settings, inputs, 0, "PrepareDepths" );

//	    ID3D11DeviceContext * dx11Context = inputs->DeviceContext;
	    {
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
//	            	setRenderTargets(m_importanceMap);

	                // select 4 deinterleaved AO textures (texture array)
//	                dx11Context->PSSetShaderResources( SSAO_TEXTURE_SLOT4, 1, &m_finalResults.SRV );
//	            	bind(m_finalResults, RenderTechnique.TEX2D_FINAL_SSAO, m_samplerStatePointClamp);
//	                FullscreenPassDraw(m_pixelShaderGenerateImportanceMap, viewportWidth, viewportHeight, null );
                    if (!g_PrintOnce)
                    {
                        /*sprintf_s(debugName, "%sImportanceMap.txt", DEBUG_FILE_FOLDER);
                        DEBUG_TEXTURE2D(dx11Context, m_importanceMap.RTV, debugName)*/
//                        saveTextData("ImportanceMap.txt", m_importanceMap);
                        testTexture("ImportanceMap.txt");
                    }

	                // postprocess A
//	                dx11Context->OMSetRenderTargets( 1, &m_importanceMapPong.RTV, NULL );
//	                setRenderTargets(m_importanceMapPong);
//	                dx11Context->PSSetShaderResources( SSAO_TEXTURE_SLOT3, 1, &m_importanceMap.SRV );
//	                bind(m_importanceMap, RenderTechnique.TEX2D_IMPORTANCE_MAP, m_samplerStateLinearClamp);
//	                FullscreenPassDraw(m_pixelShaderPostprocessImportanceMapA, viewportWidth, viewportHeight, null );
//	                dx11Context->PSSetShaderResources( SSAO_TEXTURE_SLOT3, 1, zeroSRVs );
                    if (!g_PrintOnce)
                    {
                        /*sprintf_s(debugName, "%sImportanceMapA.txt", DEBUG_FILE_FOLDER);
                        DEBUG_TEXTURE2D(dx11Context, m_importanceMapPong.RTV, debugName)*/
//                        saveTextData("ImportanceMapA.txt", m_importanceMapPong);
                        testTexture("ImportanceMapA.txt");
                    }

	                // postprocess B
//	                UINT fourZeroes[4] = { 0, 0, 0, 0 };
//	                dx11Context->ClearUnorderedAccessViewUint( m_loadCounterUAV, fourZeroes );
//	                GL44.glClearTexImage(texture, level, format, type, data);  TODO clear the m_loadCounter
//	                dx11Context->OMSetRenderTargetsAndUnorderedAccessViews( 1, &m_importanceMap.RTV, NULL, SSAO_LOAD_COUNTER_UAV_SLOT, 1, &m_loadCounterUAV, NULL );
//	                setRenderTargets(m_importanceMap);
//	                gl.glBindImageTexture(0, m_loadCounter, 0, false, 0, GLenum.GL_READ_WRITE, GLenum.GL_R32UI);
	                
	                // select previous pass input importance map
//	                dx11Context->PSSetShaderResources( SSAO_TEXTURE_SLOT3, 1, &m_importanceMapPong.SRV );
//	                bind(m_importanceMapPong, RenderTechnique.TEX2D_IMPORTANCE_MAP, m_samplerStateLinearClamp);
//	                FullscreenPassDraw(m_pixelShaderPostprocessImportanceMapB, viewportWidth, viewportHeight, null );
//	                dx11Context->PSSetShaderResources( SSAO_TEXTURE_SLOT3, 1, zeroSRVs );
                    if (!g_PrintOnce)
                    {
                        /*sprintf_s(debugName, "%sImportanceMapB.txt", DEBUG_FILE_FOLDER);
                        DEBUG_TEXTURE2D(dx11Context, m_importanceMap.RTV, debugName);*/
//                        saveTextData("ImportanceMapB.txt", m_importanceMap);
                        testTexture("ImportanceMapB.txt");
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
//                gl.glBindFramebuffer(GLenum.GL_FRAMEBUFFER, 0);
//                gl.glDisable(GLenum.GL_DEPTH_TEST);
//	        	GL11.glClearColor(0, 0, 0, 1);
//	        	GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);
	        }

	        // Apply
	        {
	            // select 4 deinterleaved AO textures (texture array)
//	            dx11Context->PSSetShaderResources( SSAO_TEXTURE_SLOT4, 1, &m_finalResults.SRV );
//	        	m_finalResults.bind(RenderTechnique.TEX2D_FINAL_SSAO, m_samplerStateLinearClamp);
//                gl.glActiveTexture(GLenum.GL_TEXTURE0 + RenderTechnique.TEX2D_FINAL_SSAO);
//                gl.glBindTexture(m_finalResults.getTarget(), m_finalResults.getTexture());
//                gl.glBindSampler(RenderTechnique.TEX2D_FINAL_SSAO, m_samplerStateLinearClamp);

//	            CD3D11_VIEWPORT viewport = CD3D11_VIEWPORT( 0.0f, 0.0f, (float)m_size.x, (float)m_size.y );
//	            CD3D11_RECT rect = CD3D11_RECT( m_fullResOutScissorRect.x, m_fullResOutScissorRect.y, m_fullResOutScissorRect.z, m_fullResOutScissorRect.w );
//	            dx11Context->RSSetViewports( 1, &viewport );
//	            dx11Context->RSSetScissorRects( 1, &rect );
	            
	            final int viewportWidth = m_size.x;
            	final int viewportHeight = m_size.y;

//                gl.glEnable(GLenum.GL_SCISSOR_TEST);
//                gl.glScissor(m_halfResOutScissorRect.x, m_halfResOutScissorRect.y, m_halfResOutScissorRect.z, m_halfResOutScissorRect.w);

//	            ID3D11BlendState * blendState = ( inputs->DrawOpaque ) ? ( m_blendStateOpaque ) : ( m_blendStateMultiply );
//            	Runnable blendState = (inputs.DrawOpaque) ? null : ()->blendStateMultiply();
	            
//	            if( settings.QualityLevel < 0 )
//	                FullscreenPassDraw( m_pixelShaderNonSmartHalfApply, viewportWidth, viewportHeight, blendState );
//	            else if( settings.QualityLevel == 0 )
//	                FullscreenPassDraw( m_pixelShaderNonSmartApply, viewportWidth, viewportHeight, blendState );
//	            else
//	                FullscreenPassDraw( m_pixelShaderApply, viewportWidth, viewportHeight, blendState );
//
//                gl.glDisable(GLenum.GL_SCISSOR_TEST);
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
