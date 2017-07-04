package jet.opengl.demos.scenes.outdoor;

import com.nvidia.developer.opengl.utils.NvImage;
import com.nvidia.developer.opengl.utils.StackFloat;
import com.nvidia.developer.opengl.utils.StackInt;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector2f;
import org.lwjgl.util.vector.Vector3f;

import java.io.IOException;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;

import jet.opengl.postprocessing.common.GLCheck;
import jet.opengl.postprocessing.common.GLFuncProvider;
import jet.opengl.postprocessing.common.GLFuncProviderFactory;
import jet.opengl.postprocessing.common.GLenum;
import jet.opengl.postprocessing.shader.GLSLProgramPipeline;
import jet.opengl.postprocessing.shader.Macro;
import jet.opengl.postprocessing.texture.RenderTargets;
import jet.opengl.postprocessing.texture.SamplerDesc;
import jet.opengl.postprocessing.texture.SamplerUtils;
import jet.opengl.postprocessing.texture.Texture2D;
import jet.opengl.postprocessing.texture.Texture2DDesc;
import jet.opengl.postprocessing.texture.TextureDataDesc;
import jet.opengl.postprocessing.texture.TextureGL;
import jet.opengl.postprocessing.texture.TextureUtils;
import jet.opengl.postprocessing.util.CacheBuffer;

final class CEarthHemsiphere {

	// // One base material + 4 masked materials
	static final int NUM_TILE_TEXTURES = 1 + 4;
	
	SRenderingParams m_Params;

	private TerrainRenderTechnique m_RenderEarthHemisphereVS;
    private TerrainRenderTechnique m_RenderEarthHemispherePS;
    
    private HemisphereZOnlyProgram m_RenderEarthHemisphereZOnlyTech;

    private int m_pVertBuff;
    private int m_pInputLayout;
    private Texture2D m_ptex2DNormalMapSRV, m_ptex2DMtrlMaskSRV;

	private Texture2D[] m_ptex2DTilesSRV = new Texture2D[NUM_TILE_TEXTURES];
	private Texture2D[] m_ptex2DTilNormalMapsSRV = new Texture2D[NUM_TILE_TEXTURES];
	
	private int m_psamComaprison;

    private final List<SRingSectorMesh> m_SphereMeshes = new ArrayList<SRingSectorMesh>();
    
    private int m_pStitchIndBuff;
    private int m_uiNumStitchIndices;
    private GLSLProgramPipeline m_pipeline;
    
    private RenderTargets rtManager;
    private String m_ShaderPath;
    private final SViewFrustum m_viewFrustum = new SViewFrustum();
    private boolean m_PrintOnce= false;
	private GLFuncProvider gl;
    
    public void onD3D11CreateDevice(CElevationDataSource pDataSource,
						            SRenderingParams Params,
//						            ID3D11Device* pd3dDevice,
//						            ID3D11DeviceContext* pd3dImmediateContext,
						            String HeightMapPath,
						            String MaterialMaskPath,
									String[] TileTexturePath,
						            String[] TileNormalMapPath,
						            String ShaderPath){
		gl = GLFuncProviderFactory.getGLFuncProvider();
    	m_ShaderPath = ShaderPath;
    	m_Params = Params;
    	
    	short[] pHeightMap = pDataSource.getDataPtr();
    	int HeightMapPitch = pDataSource.getPitch();
    	int iHeightMapDim = pDataSource.getNumCols();
    	assert(iHeightMapDim == pDataSource.getNumRows() );
    	GLCheck.checkError();
    	StackFloat VB = new StackFloat();
    	StackInt StitchIB = new StackInt();
    	generateSphereGeometry(/*pd3dDevice,*/ 6360000f, m_Params.m_iRingDimension, m_Params.m_iNumRings, pDataSource, 
    						m_Params.m_TerrainAttribs.m_fElevationSamplingInterval, m_Params.m_TerrainAttribs.m_fElevationScale, 
    						VB, StitchIB, m_SphereMeshes);
    	
    	for(int i = 0; i < m_SphereMeshes.size(); i++){
    		System.out.println(i + ": " + m_SphereMeshes.get(i));
    	}

		GLCheck.checkError();
    	m_pVertBuff = gl.glGenBuffer();
		gl.glBindBuffer(GLenum.GL_ARRAY_BUFFER, m_pVertBuff);
    	FloatBuffer vertsBuf = CacheBuffer.wrap(VB.getData(), 0, VB.size());
		gl.glBufferData(GLenum.GL_ARRAY_BUFFER, vertsBuf, GLenum.GL_STATIC_DRAW);
		GLCheck.checkError();
    	m_uiNumStitchIndices = StitchIB.size();
    	m_pStitchIndBuff = gl.glGenBuffer();
    	gl.glBindBuffer(GLenum.GL_ARRAY_BUFFER, m_pStitchIndBuff);
    	IntBuffer indicesBuf = CacheBuffer.wrap(StitchIB.getData(), 0, StitchIB.size());
		gl.glBufferData(GLenum.GL_ARRAY_BUFFER, indicesBuf, GLenum.GL_STATIC_DRAW);
		gl.glBindBuffer(GLenum.GL_ARRAY_BUFFER, 0);

		GLCheck.checkError();
    	createRenderStates();

		GLCheck.checkError();
    	m_RenderEarthHemisphereZOnlyTech = new HemisphereZOnlyProgram(m_ShaderPath);
    	
    	RenderNormalMap(/*pd3dDevice, pd3dImmediateContext,*/ pHeightMap, HeightMapPitch, iHeightMapDim);
		GLCheck.checkError();
    	try {
			m_ptex2DMtrlMaskSRV = TextureUtils.createTexture2DFromFile(MaterialMaskPath, true);
			m_ptex2DMtrlMaskSRV.setMagFilter(GLenum.GL_LINEAR);
			m_ptex2DMtrlMaskSRV.setMinFilter(m_ptex2DMtrlMaskSRV.getMipLevels() > 1 ? GLenum.GL_LINEAR_MIPMAP_LINEAR : GLenum.GL_LINEAR);
			m_ptex2DMtrlMaskSRV.setWrapS(GLenum.GL_MIRRORED_REPEAT);
			m_ptex2DMtrlMaskSRV.setWrapT(GLenum.GL_MIRRORED_REPEAT);
			
//			System.err.println(m_ptex2DMtrlMaskSRV.toString("m_ptex2DMtrlMaskSRV"));
			// Load tiles
			for(int iTileTex = 0; iTileTex < NUM_TILE_TEXTURES; iTileTex++){
				NvImage.upperLeftOrigin(false);  // TODO
				int textureID = NvImage.uploadTextureFromDDSFile(TileTexturePath[iTileTex]);
				m_ptex2DTilesSRV[iTileTex] = TextureUtils.createTexture2D(GLenum.GL_TEXTURE_2D, textureID);

				m_ptex2DTilesSRV[iTileTex].setMagFilter(GLenum.GL_LINEAR);
				m_ptex2DTilesSRV[iTileTex].setMinFilter(m_ptex2DTilesSRV[iTileTex].getMipLevels() > 1 ? GLenum.GL_LINEAR_MIPMAP_LINEAR : GLenum.GL_LINEAR);
				m_ptex2DTilesSRV[iTileTex].setWrapS(GLenum.GL_REPEAT);
				m_ptex2DTilesSRV[iTileTex].setWrapT(GLenum.GL_REPEAT);

				if(TileNormalMapPath[iTileTex].endsWith(".dds")) {
					textureID = NvImage.uploadTextureFromDDSFile(TileNormalMapPath[iTileTex]);
					m_ptex2DTilNormalMapsSRV[iTileTex] = TextureUtils.createTexture2D(GLenum.GL_TEXTURE_2D, textureID);
				}else{
					m_ptex2DTilNormalMapsSRV[iTileTex] = TextureUtils.createTexture2DFromFile(TileNormalMapPath[iTileTex], true);
				}
				
				m_ptex2DTilNormalMapsSRV[iTileTex].setMagFilter(GLenum.GL_LINEAR);
				m_ptex2DTilNormalMapsSRV[iTileTex].setMinFilter(m_ptex2DTilNormalMapsSRV[iTileTex].getMipLevels() > 1 ? GLenum.GL_LINEAR_MIPMAP_LINEAR : GLenum.GL_LINEAR);
				m_ptex2DTilNormalMapsSRV[iTileTex].setWrapS(GLenum.GL_REPEAT);
				m_ptex2DTilNormalMapsSRV[iTileTex].setWrapT(GLenum.GL_REPEAT);
			}
    	} catch (IOException e) {
			e.printStackTrace();
		}
    	
    	m_pipeline = new GLSLProgramPipeline();
    	m_RenderEarthHemisphereVS = new TerrainRenderTechnique(ShaderPath + "HemisphereVS.vert", null, GLenum.GL_VERTEX_SHADER);
    	m_RenderEarthHemisphereVS.setDebugName("HemisphereVS");
    	
    	GLCheck.checkError();
    }
    
    public void render( //ID3D11DeviceContext* pd3dImmediateContext,
			            final Vector3f vCameraPosition, 
			            final Matrix4f CameraViewProjMatrix,
			            SLightAttribs lightAttribs,
			            SAirScatteringAttribs pcMediaScatteringParams,
			            float near, float far,
			            Texture2D pShadowMapSRV,
			            Texture2D pPrecomputedNetDensitySRV,
			            Texture2D pAmbientSkylightSRV,
			            boolean bZOnlyPass){
//    	if( GetAsyncKeyState(VK_F9) )
//        {
//            m_RenderEarthHemisphereTech.Release();
//        }
    	GLCheck.checkError();
    	if(!bZOnlyPass && m_RenderEarthHemispherePS == null){
			Macro[] macros= {
    			new Macro("TEXTURING_MODE", m_Params.m_TexturingMode),
				new Macro("NUM_TILE_TEXTURES", NUM_TILE_TEXTURES),
				new Macro("NUM_SHADOW_CASCADES", m_Params.m_iNumShadowCascades),
				new Macro("BEST_CASCADE_SEARCH", m_Params.m_bBestCascadeSearch),
				new Macro("SMOOTH_SHADOWS", m_Params.m_bSmoothShadows),
    		};
    		m_RenderEarthHemispherePS = new TerrainRenderTechnique(m_ShaderPath+ "HemispherePS.frag", macros);
    		m_RenderEarthHemispherePS.setDebugName("HemispherePS");
    	}
    	
    	if(m_pInputLayout == 0){
    		m_pInputLayout = gl.glGenVertexArray();
			gl.glBindVertexArray(m_pInputLayout);
    		{
				gl.glBindBuffer(GLenum.GL_ARRAY_BUFFER, m_pVertBuff);
				gl.glVertexAttribPointer(0, 3, GLenum.GL_FLOAT, false, 20, 0);
				gl.glVertexAttribPointer(1, 2, GLenum.GL_FLOAT, false, 20, 12);

				gl.glEnableVertexAttribArray(0);
				gl.glEnableVertexAttribArray(1);
    		}
			gl.glBindVertexArray(0);
			gl.glBindBuffer(GLenum.GL_ARRAY_BUFFER, 0);
//    		GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, 0);
    	}
    	
    	if( bZOnlyPass ){
            m_RenderEarthHemisphereZOnlyTech.enable();
            m_RenderEarthHemisphereZOnlyTech.setWorldViewProjMatrix(CameraViewProjMatrix);
    	}else{
    		// TODO Bind textures here
//            m_RenderEarthHemisphereTech.enable();
			gl.glUseProgram(0);
    		m_pipeline.enable();
    		m_pipeline.setVS(m_RenderEarthHemisphereVS);
    		m_pipeline.setPS(m_RenderEarthHemispherePS);
    		
    		setupUniforms(m_RenderEarthHemisphereVS, CameraViewProjMatrix, lightAttribs, pcMediaScatteringParams, near, far);
    		setupUniforms(m_RenderEarthHemispherePS, CameraViewProjMatrix, lightAttribs, pcMediaScatteringParams, near, far);
    		
    		bindTextures(pShadowMapSRV, pPrecomputedNetDensitySRV, pAmbientSkylightSRV);
    	}
    	
    	drawMesh(CameraViewProjMatrix);
    	
		gl.glUseProgram(0);
    	if(!bZOnlyPass){
    		unbindTextures(pShadowMapSRV, pPrecomputedNetDensitySRV, pAmbientSkylightSRV);
    		
    		if(!m_PrintOnce){
    			m_PrintOnce = true;
    			
    			m_RenderEarthHemisphereVS.printPrograminfo();
    			m_RenderEarthHemispherePS.printPrograminfo();
    		}
    	}
    	
    	m_pipeline.disable();
    	gl.glUseProgram(0);
    }
    
    private void setupUniforms(TerrainRenderTechnique shader, Matrix4f worldViewProj, SLightAttribs lightAttribs,
            SAirScatteringAttribs pcMediaScatteringParams,  float near, float far){
    	shader.setWorldViewProjMatrix(worldViewProj);
    	shader.setupUniforms(pcMediaScatteringParams);
    	shader.setupUniforms(lightAttribs);
//    	shader.setupUniforms(cameraAttribs);
		shader.setCameraPlane(far, near);
    	shader.setBaseMtrlTilingScale(m_Params.m_TerrainAttribs.m_fBaseMtrlTilingScale);
    	shader.setCascadeColors(m_Params.m_TerrainAttribs.f4CascadeColors);
    }

	private final void bind(TextureGL texture, int unit, int sampler){
		gl.glActiveTexture(GLenum.GL_TEXTURE0 + unit);
		if(texture != null)
			gl.glBindTexture(texture.getTarget(), texture.getTexture());
		else
			gl.glBindTexture(GLenum.GL_TEXTURE_2D, 0);
		gl.glBindSampler(unit, sampler);
	}
    
    private void bindTextures(Texture2D pShadowMapSRV, Texture2D pPrecomputedNetDensitySRV, Texture2D pAmbientSkylightSRV){
    	bind(pAmbientSkylightSRV, TerrainRenderTechnique.TEX2D_AMBIENT_SKY_LIGHT, 0);
    	bind(pPrecomputedNetDensitySRV, TerrainRenderTechnique.TEX2D_OCCLUDED_NET_DENSITY_TO_ATMTOP, 0);
    	bind(m_ptex2DNormalMapSRV, TerrainRenderTechnique.TEX2D_NORMAL_MAP, 0);
    	bind(m_ptex2DMtrlMaskSRV, TerrainRenderTechnique.TEX2D_MTRLMAP, 0);
    	bind(pShadowMapSRV, TerrainRenderTechnique.TEX2D_SHADOWMAP, m_psamComaprison);
    	
	    for(int iTileTex = 0; iTileTex < NUM_TILE_TEXTURES; iTileTex++){
	    	bind(m_ptex2DTilesSRV[iTileTex], TerrainRenderTechnique.TEX2D_TILE_TEXTURES+iTileTex, 0);
	    	bind(m_ptex2DTilNormalMapsSRV[iTileTex], TerrainRenderTechnique.TEX2D_TILE_NORMAL_MAPS+iTileTex, 0);
	    }
    }
    
    private void unbindTextures(Texture2D pShadowMapSRV, Texture2D pPrecomputedNetDensitySRV, Texture2D pAmbientSkylightSRV){
//    	pAmbientSkylightSRV.unbind();
//    	pPrecomputedNetDensitySRV.unbind();
//    	m_ptex2DNormalMapSRV.unbind();
//    	m_ptex2DMtrlMaskSRV.unbind();
//    	pShadowMapSRV.unbind();
//
//	    for(int iTileTex = 0; iTileTex < NUM_TILE_TEXTURES; iTileTex++){
//	    	m_ptex2DTilesSRV[iTileTex].unbind();
//	    	m_ptex2DTilNormalMapsSRV[iTileTex].unbind();
//	    }

		gl.glBindSampler(TerrainRenderTechnique.TEX2D_SHADOWMAP, 0);
    }
    
    public void drawMesh(Matrix4f mvp){
    	m_viewFrustum.extractViewFrustumPlanesFromMatrix(mvp);
    	gl.glBindVertexArray(m_pInputLayout);
    	for(SRingSectorMesh meshIt : m_SphereMeshes){
    		if(m_viewFrustum.isBoxVisible(meshIt.boundBox))
            {
				gl.glBindBuffer(GLenum.GL_ELEMENT_ARRAY_BUFFER, meshIt.pIndBuff);
				gl.glDrawElements(GLenum.GL_TRIANGLE_STRIP, meshIt.uiNumIndices, GLenum.GL_UNSIGNED_INT, 0);
            }
    	}

		gl.glBindVertexArray(0);
		gl.glBindBuffer(GLenum.GL_ELEMENT_ARRAY_BUFFER, 0);
    }
    
    public void onD3D11DestroyDevice(){
    	if(m_RenderEarthHemisphereVS != null){
    		m_RenderEarthHemisphereVS.dispose();
    		m_RenderEarthHemisphereVS = null;
    	}
    	
    	if(m_RenderEarthHemispherePS != null){
    		m_RenderEarthHemispherePS.dispose();
    		m_RenderEarthHemispherePS = null;
    	}

    	if(m_RenderEarthHemisphereZOnlyTech != null){
    		m_RenderEarthHemisphereZOnlyTech.dispose();
    		m_RenderEarthHemisphereZOnlyTech = null;
    	}

    	if(m_pVertBuff != 0){
    		gl.glDeleteBuffer(m_pVertBuff);
    		m_pVertBuff = 0;
    	}

    	if(m_ptex2DNormalMapSRV != null){
    		m_ptex2DNormalMapSRV.dispose();
    		m_ptex2DNormalMapSRV = null;
    	}

    	if(m_ptex2DMtrlMaskSRV != null){
    		m_ptex2DMtrlMaskSRV.dispose();
    		m_ptex2DMtrlMaskSRV = null;
    	}

//    	if(m_pcbTerrainAttribs != 0){
//    		GL15.glDeleteBuffers(m_pcbTerrainAttribs);
//    	m_pcbTerrainAttribs = 0;
//    	}
//
//    	if(m_pcbCameraAttribs != 0){
//    		GL15.glDeleteBuffers(m_pcbCameraAttribs);
//    	m_pcbCameraAttribs = 0;
//    	}

    	for(int i = 0; i < m_ptex2DTilesSRV.length; i++){
    		if(m_ptex2DTilesSRV[i] != null){
    			m_ptex2DTilesSRV[i].dispose();
    			m_ptex2DTilesSRV[i] = null;
    		}
    	}

    	for(int i = 0; i < m_ptex2DTilNormalMapsSRV.length; i++){
    		if(m_ptex2DTilNormalMapsSRV[i] != null){
    			m_ptex2DTilNormalMapsSRV[i].dispose();
    			m_ptex2DTilNormalMapsSRV[i] = null;
    		}
    	}

    	if(m_pStitchIndBuff != 0){
    		gl.glDeleteBuffer(m_pStitchIndBuff);
    		m_pStitchIndBuff = 0;
    	}

    	if(rtManager != null){
    		rtManager.dispose();
    		rtManager = null;
    	}
    }
    
    private void createRenderStates(/*ID3D11Device* pd3dDevice*/){
//    	SamplerDesc SamLinearMirrorDesc = new SamplerDesc
//    	(
//    			GL11.GL_LINEAR, 
//    			GL11.GL_LINEAR,
//    			GL14.GL_MIRRORED_REPEAT,
//    			GL14.GL_MIRRORED_REPEAT,
//    			GL14.GL_MIRRORED_REPEAT
//    	);
//    	
//    	m_psamLinearMirror = SamplerUtils.createSampler(SamLinearMirrorDesc);
    	
//    	SamplerDesc SamPointClamp = new SamplerDesc
//    	(
//    			GL11.GL_NEAREST,
//    			GL11.GL_NEAREST,
//    			GL12.GL_CLAMP_TO_EDGE,
//    			GL12.GL_CLAMP_TO_EDGE,
//    			GL12.GL_CLAMP_TO_EDGE
//    	);
//    	m_psamPointClamp = SamplerUtils.createSampler(SamPointClamp);
    	
//    	SamplerDesc SamLinearClamp = new SamplerDesc
//    	(
//    			GL11.GL_LINEAR,
//    			GL11.GL_LINEAR,
//    			GL12.GL_CLAMP_TO_EDGE,
//    			GL12.GL_CLAMP_TO_EDGE,
//    			GL12.GL_CLAMP_TO_EDGE
//    	);
//    	m_psamLinearClamp = SamplerUtils.createSampler(SamLinearClamp);
    	
    	SamplerDesc SamComparison = new SamplerDesc
    	(
    			GLenum.GL_NEAREST,
				GLenum.GL_NEAREST,
				GLenum.GL_CLAMP_TO_EDGE,
				GLenum.GL_CLAMP_TO_EDGE,
				GLenum.GL_CLAMP_TO_EDGE,
    			0,  // border Color
    			0,  // MaxAnisotropy
				GLenum.GL_LESS,
				GLenum.GL_COMPARE_REF_TO_TEXTURE
    	);
    	m_psamComaprison = SamplerUtils.createSampler(SamComparison);
    	
//    	SamplerDesc SamLinearWrapDesc = new SamplerDesc
//    	(
//    			GL11.GL_LINEAR,
//    			GL11.GL_LINEAR,
//    			GL11.GL_REPEAT,
//    			GL11.GL_REPEAT,
//    			GL11.GL_REPEAT
//    	);
//    	m_psamLinearWrap = SamplerUtils.createSampler(SamLinearWrapDesc);
    }
    
    private void RenderNormalMap(/*ID3D11Device* pd3dDevice,
                         ID3D11DeviceContext* pd3dImmediateContext,*/
                         short[] pHeightMap,
                         int HeightMapPitch,
                         int iHeightMapDim){
    	Texture2DDesc HeightMapDesc = new Texture2DDesc
    	(
    			iHeightMapDim-1, // TODO
    	        iHeightMapDim-1, // TODO
    			1,
    			1,
				GLenum.GL_R16F,
    	        1
    	);
    	
    	while( (iHeightMapDim >> HeightMapDesc.mipLevels) > 1 )
            ++HeightMapDesc.mipLevels;
//    	ShortBuffer CoarseMipLevels = GLUtil.getCachedShortBuffer(iHeightMapDim/2 * iHeightMapDim);
    	List<Object> InitData = new ArrayList<>();
    	InitData.add(pHeightMap);
    	short[] pFinerMipLevel = pHeightMap;
    	int FinerMipPitch = HeightMapPitch;
        int CurrMipPitch = iHeightMapDim/2;
        for(int uiMipLevel = 1; uiMipLevel < HeightMapDesc.mipLevels; ++uiMipLevel)
        {
            int MipWidth  = HeightMapDesc.width >> uiMipLevel;
            int MipHeight = HeightMapDesc.height >> uiMipLevel;
            short[] pCurrMipLevel = new short[MipHeight*CurrMipPitch];
            for(int uiRow=0; uiRow < MipHeight; ++uiRow)
                for(int uiCol=0; uiCol < MipWidth; ++uiCol)
                {
                    int iAverageHeight = 0;
                    for(int i=0; i<2; ++i)
                        for(int j=0; j<2; ++j)
                            iAverageHeight += pFinerMipLevel[ (uiCol*2+i) + (uiRow*2+j)*FinerMipPitch] & 0xFFFF;
//                        	iAverageHeight += pHeightMap[pFinerMipLevel + (uiCol*2+i) + (uiRow*2+j)*FinerMipPitch] & 0xFFFF;
                    pCurrMipLevel[uiCol + uiRow*CurrMipPitch] = (short)(iAverageHeight>>2);
                }

//            InitData[uiMipLevel].pSysMem = pCurrMipLevel;
//            InitData[uiMipLevel].SysMemPitch = (UINT)CurrMipPitch*sizeof(*pCurrMipLevel);
            InitData.add(pCurrMipLevel);
            pFinerMipLevel = pCurrMipLevel;
            FinerMipPitch = CurrMipPitch;
//            pCurrMipLevel += MipHeight*CurrMipPitch;
            CurrMipPitch = iHeightMapDim/2;
        }
        
//        System.out.println(HeightMapDesc);
        TextureDataDesc texData = new TextureDataDesc(GLenum.GL_RED, GLenum.GL_UNSIGNED_SHORT, InitData);
        Texture2D ptex2DHeightMapSRV = TextureUtils.createTexture2D(HeightMapDesc, texData);
        ptex2DHeightMapSRV.setMagFilter(GLenum.GL_NEAREST);
        ptex2DHeightMapSRV.setMinFilter(GLenum.GL_NEAREST_MIPMAP_NEAREST);
        ptex2DHeightMapSRV.setWrapS(GLenum.GL_CLAMP_TO_EDGE);
        ptex2DHeightMapSRV.setWrapT(GLenum.GL_CLAMP_TO_EDGE);
        
        GLCheck.checkError();
        
        System.err.println("Load Done");
        Texture2DDesc NormalMapDesc = HeightMapDesc;
        NormalMapDesc.format = GLenum.GL_RG8_SNORM;
        m_ptex2DNormalMapSRV = TextureUtils.createTexture2D(NormalMapDesc, null);
        m_ptex2DNormalMapSRV.setMagFilter(GLenum.GL_LINEAR);
        m_ptex2DNormalMapSRV.setMinFilter(GLenum.GL_LINEAR_MIPMAP_LINEAR);
        m_ptex2DNormalMapSRV.setWrapS(GLenum.GL_MIRRORED_REPEAT);
        m_ptex2DNormalMapSRV.setWrapT(GLenum.GL_MIRRORED_REPEAT);
        
        int[] OrigViewPort = new int[4];
		IntBuffer buffer = CacheBuffer.getCachedIntBuffer(4);
        gl.glGetIntegerv(GLenum.GL_VIEWPORT, buffer);
		buffer.get(OrigViewPort);
        int origFramebuffer = gl.glGetInteger(GLenum.GL_FRAMEBUFFER_BINDING);
        
        GenerateNormalMapProgram RenderNormalMapTech = new GenerateNormalMapProgram(m_ShaderPath);
        RenderNormalMapTech.enable();
        bind(ptex2DHeightMapSRV, TerrainRenderTechnique.TEX2D_ELEVATION_MAP, 0);
        
        int VAO = gl.glGenVertexArray();
		gl.glBindVertexArray(VAO);
        int framebuffer = gl.glGenFramebuffer();
		gl.glBindFramebuffer(GLenum.GL_FRAMEBUFFER, framebuffer);
        for(int uiMipLevel = 0; uiMipLevel < NormalMapDesc.mipLevels; ++uiMipLevel){
			gl.glFramebufferTexture2D(GLenum.GL_FRAMEBUFFER, GLenum.GL_COLOR_ATTACHMENT0, GLenum.GL_TEXTURE_2D, m_ptex2DNormalMapSRV.getTexture(), uiMipLevel);
        	int width  = (NormalMapDesc.width  >> uiMipLevel);
            int height = (NormalMapDesc.height >> uiMipLevel);

			gl.glViewport(0, 0, width, height);
            
            float m_fElevationScale = m_Params.m_TerrainAttribs.m_fElevationScale;
            float m_fSampleSpacingInterval = m_Params.m_TerrainAttribs.m_fElevationSamplingInterval;
            float m_fMIPLevel = (float)uiMipLevel;
            
//            #define m_fSampleSpacingInterval g_NMGenerationAttribs.x
//            #define m_fMIPLevel 	  g_NMGenerationAttribs.y
//            #define m_fElevationScale g_NMGenerationAttribs.z
			gl.glUniform4f(RenderNormalMapTech.getNMGenerationAttribs(), m_fSampleSpacingInterval, m_fMIPLevel, m_fElevationScale, 0);

			gl.glDrawArrays(GLenum.GL_TRIANGLES, 0, 3);
        }

		gl.glBindVertexArray(0);
        RenderNormalMapTech.disable();
        
        // Reset the states
		gl.glBindFramebuffer(GLenum.GL_FRAMEBUFFER, origFramebuffer);
		gl.glViewport(OrigViewPort[0], OrigViewPort[1], OrigViewPort[2], OrigViewPort[3]);
//        ptex2DHeightMapSRV.unbind();
        
        // Release the resources
        RenderNormalMapTech.dispose();
		gl.glDeleteFramebuffer(framebuffer);
		gl.glDeleteVertexArray(VAO);
        ptex2DHeightMapSRV.dispose();
    }
    
    private static void computeVertexHeight(SHemisphereVertex Vertex, 
            CElevationDataSource pDataSource,
            float fSamplingStep,
            float fSampleScale)
	{
		Vector3f f3PosWS = Vertex.f3WorldPos;
		
		float fCol = f3PosWS.x / fSamplingStep;
		float fRow = f3PosWS.z / fSamplingStep;
		float fDispl = pDataSource.getInterpolatedHeight(fCol, fRow);
		int iColOffset, iRowOffset;
//		pDataSource.getOffsets(iColOffset, iRowOffset);
		iColOffset = pDataSource.getColOffset();
		iRowOffset = pDataSource.getRowOffset();
		Vertex.f2MaskUV0.x = (fCol + (float)iColOffset + 0.5f)/(float)pDataSource.getNumCols();
		Vertex.f2MaskUV0.y = (fRow + (float)iRowOffset + 0.5f)/(float)pDataSource.getNumRows();
		
//		D3DXVECTOR3 f3SphereNormal;
//		D3DXVec3Normalize(&f3SphereNormal, &f3PosWS);
//		f3PosWS += f3SphereNormal * fDispl * fSampleScale;
		
		float x = f3PosWS.x;
		float y = f3PosWS.y;
		float z = f3PosWS.z;
		Vector3f f3SphereNormal = f3PosWS;
		f3SphereNormal.normalise();
		
		f3PosWS.x = x + f3SphereNormal.x * fDispl * fSampleScale;
		f3PosWS.y = y + f3SphereNormal.y * fDispl * fSampleScale;
		f3PosWS.z = z + f3SphereNormal.z * fDispl * fSampleScale;
	}
    
    private static void generateSphereGeometry(//ID3D11Device *pDevice,
            final float fEarthRadius,
            int iGridDimension, 
            final int iNumRings,
            CElevationDataSource pDataSource,
            float fSamplingStep,
            float fSampleScale,
            StackFloat VB,
            StackInt StitchIB,
            List<SRingSectorMesh> SphereMeshes){
    	if( (iGridDimension - 1) % 4 != 0 )
        {
            assert(false);
            iGridDimension = /*new SRenderingParams().m_iRingDimension*/65;
        }
        final int iGridMidst = (iGridDimension-1)/2;
        final int iGridQuart = (iGridDimension-1)/4;

        @SuppressWarnings("unused")
		final int iLargestGridScale = iGridDimension << (iNumRings-1);
        
        CRingMeshBuilder RingMeshBuilder = new  CRingMeshBuilder(/*pDevice,*/ VB, iGridDimension, SphereMeshes);

        final SHemisphereVertex CurrVert = new SHemisphereVertex();
        int iStartRing = 0;
        VB.reserve( (iNumRings-iStartRing) * iGridDimension * iGridDimension * 5);
        for(int iRing = iStartRing; iRing < iNumRings; ++iRing)
        {
            final int iCurrGridStart = VB.size()/5;
            int bufferSize = VB.size() + (iGridDimension * iGridDimension) * 5;
            VB.resize(bufferSize);
            final float[] verts = VB.getData();
            float fGridScale = 1.f / (float)(1<<(iNumRings-1 - iRing));
            // Fill vertex buffer
            for(int iRow = 0; iRow < iGridDimension; ++iRow)
                for(int iCol = 0; iCol < iGridDimension; ++iCol)
                {
//                    auto &CurrVert = VB[iCurrGridStart + iCol + iRow*iGridDimension];
                	final int index = (iCurrGridStart + iCol + iRow*iGridDimension) * 5;
//                    auto &f3Pos = CurrVert.f3WorldPos;
                	final Vector3f f3Pos = CurrVert.f3WorldPos;
                    f3Pos.x = (float)(iCol) / (float)(iGridDimension-1);
                    f3Pos.z = (float)(iRow) / (float)(iGridDimension-1);
                    f3Pos.x = f3Pos.x*2 - 1;  // remap to [-1, 1]
                    f3Pos.z = f3Pos.z*2 - 1;
                    f3Pos.y = 0;
                    float fDirectionScale = 1;
                    if( f3Pos.x != 0 || f3Pos.z != 0 )
                    {
                        float fDX = Math.abs(f3Pos.x);
                        float fDZ = Math.abs(f3Pos.z);
                        float fMaxD = Math.max(fDX, fDZ);
                        float fMinD = Math.min(fDX, fDZ);
                        float fTan = fMinD/fMaxD;
                        fDirectionScale = (float) (1.0 / Math.sqrt(1 + fTan*fTan));
                    }
                
                    f3Pos.x *= fDirectionScale*fGridScale;
                    f3Pos.z *= fDirectionScale*fGridScale;
                    f3Pos.y = (float) Math.sqrt( Math.max(0, 1 - (f3Pos.x*f3Pos.x + f3Pos.z*f3Pos.z)) );

                    f3Pos.x *= fEarthRadius;
                    f3Pos.z *= fEarthRadius;
                    f3Pos.y *= fEarthRadius;

                    computeVertexHeight(CurrVert, pDataSource, fSamplingStep, fSampleScale);
                    f3Pos.y -= fEarthRadius;
                    
                    verts[index + 0] = f3Pos.x;
                    verts[index + 1] = f3Pos.y;
                    verts[index + 2] = f3Pos.z;
                    verts[index + 3] = CurrVert.f2MaskUV0.x;
                    verts[index + 4] = CurrVert.f2MaskUV0.y;
                }

            // Align vertices on the outer boundary
            if( iRing < iNumRings-1 )
            {
                for(int i=1; i < iGridDimension-1; i+=2)
                {
                    // Top & bottom boundaries
                    for(int iRow=0; iRow < iGridDimension; iRow += iGridDimension-1)
                    {
//                        const auto &V0 = VB[iCurrGridStart + i - 1 + iRow*iGridDimension].f3WorldPos;
//                              auto &V1 = VB[iCurrGridStart + i + 0 + iRow*iGridDimension].f3WorldPos;
//                        const auto &V2 = VB[iCurrGridStart + i + 1 + iRow*iGridDimension].f3WorldPos;
//                        V1 = (V0+V2)/2.f;
                    	
                    	final int idx0 = (iCurrGridStart + i - 1 + iRow*iGridDimension) * 5;
                    	final int idx1 = (iCurrGridStart + i + 0 + iRow*iGridDimension) * 5;
                    	final int idx2 = (iCurrGridStart + i + 1 + iRow*iGridDimension) * 5;
                    	
                    	for(int m = 0; m < 3; m++){
                    		verts[idx1 + m] = (verts[idx0 + m] + verts[idx2 + m]) * 0.5f;
                    	}
                    }

                    // Left & right boundaries
                    for(int iCol=0; iCol < iGridDimension; iCol += iGridDimension-1)
                    {
//                        const auto &V0 = VB[iCurrGridStart + iCol + (i - 1)*iGridDimension].f3WorldPos;
//                              auto &V1 = VB[iCurrGridStart + iCol + (i + 0)*iGridDimension].f3WorldPos;
//                        const auto &V2 = VB[iCurrGridStart + iCol + (i + 1)*iGridDimension].f3WorldPos;
//                        V1 = (V0+V2)/2.f;
                    	
                    	final int idx0 = (iCurrGridStart + iCol + (i - 1)*iGridDimension) * 5;
                    	final int idx1 = (iCurrGridStart + iCol + (i + 0)*iGridDimension) * 5;
                    	final int idx2 = (iCurrGridStart + iCol + (i + 1)*iGridDimension) * 5;
                    	
                    	for(int m = 0; m < 3; m++){
                    		verts[idx1 + m] = (verts[idx0 + m] + verts[idx2 + m]) * 0.5f;
                    	}
                    }
                }


                // Add triangles stitching this ring with the next one
                int iNextGridStart = (int)VB.size();
                assert( iNextGridStart == iCurrGridStart + iGridDimension*iGridDimension);

                // Bottom boundary
                for(int iCol=0; iCol < iGridDimension-1; iCol += 2)
                {
                    StitchIB.push(iNextGridStart + (iGridQuart + iCol/2) + iGridQuart * iGridDimension); 
                    StitchIB.push(iCurrGridStart + (iCol+1) + 0 * iGridDimension); 
                    StitchIB.push(iCurrGridStart + (iCol+0) + 0 * iGridDimension); 

                    StitchIB.push(iNextGridStart + (iGridQuart + iCol/2) + iGridQuart * iGridDimension); 
                    StitchIB.push(iCurrGridStart + (iCol+2) + 0 * iGridDimension); 
                    StitchIB.push(iCurrGridStart + (iCol+1) + 0 * iGridDimension); 

                    StitchIB.push(iNextGridStart + (iGridQuart + iCol/2)   + iGridQuart * iGridDimension); 
                    StitchIB.push(iNextGridStart + (iGridQuart + iCol/2+1) + iGridQuart * iGridDimension); 
                    StitchIB.push(iCurrGridStart + (iCol+2) + 0 * iGridDimension); 
                }

                // Top boundary
                for(int iCol=0; iCol < iGridDimension-1; iCol += 2)
                {
                    StitchIB.push(iCurrGridStart + (iCol+0) + (iGridDimension-1) * iGridDimension); 
                    StitchIB.push(iCurrGridStart + (iCol+1) + (iGridDimension-1) * iGridDimension); 
                    StitchIB.push(iNextGridStart + (iGridQuart + iCol/2) + iGridQuart* 3 * iGridDimension); 

                    StitchIB.push(iCurrGridStart + (iCol+1) + (iGridDimension-1) * iGridDimension); 
                    StitchIB.push(iCurrGridStart + (iCol+2) + (iGridDimension-1) * iGridDimension); 
                    StitchIB.push(iNextGridStart + (iGridQuart + iCol/2) + iGridQuart* 3 * iGridDimension); 

                    StitchIB.push(iCurrGridStart + (iCol+2) + (iGridDimension-1) * iGridDimension); 
                    StitchIB.push(iNextGridStart + (iGridQuart + iCol/2 + 1) + iGridQuart* 3 * iGridDimension); 
                    StitchIB.push(iNextGridStart + (iGridQuart + iCol/2)     + iGridQuart* 3 * iGridDimension); 
                }

                // Left boundary
                for(int iRow=0; iRow < iGridDimension-1; iRow += 2)
                {
                    StitchIB.push(iNextGridStart + iGridQuart + (iGridQuart+ iRow/2) * iGridDimension); 
                    StitchIB.push(iCurrGridStart + 0 + (iRow+0) * iGridDimension); 
                    StitchIB.push(iCurrGridStart + 0 + (iRow+1) * iGridDimension); 

                    StitchIB.push(iNextGridStart + iGridQuart + (iGridQuart+ iRow/2) * iGridDimension);  
                    StitchIB.push(iCurrGridStart + 0 + (iRow+1) * iGridDimension); 
                    StitchIB.push(iCurrGridStart + 0 + (iRow+2) * iGridDimension); 

                    StitchIB.push(iNextGridStart + iGridQuart + (iGridQuart + iRow/2 + 1) * iGridDimension); 
                    StitchIB.push(iNextGridStart + iGridQuart + (iGridQuart + iRow/2)     * iGridDimension); 
                    StitchIB.push(iCurrGridStart + 0 + (iRow+2) * iGridDimension); 
                }

                // Right boundary
                for(int iRow=0; iRow < iGridDimension-1; iRow += 2)
                {
                    StitchIB.push(iCurrGridStart + (iGridDimension-1) + (iRow+1) * iGridDimension); 
                    StitchIB.push(iCurrGridStart + (iGridDimension-1) + (iRow+0) * iGridDimension); 
                    StitchIB.push(iNextGridStart + iGridQuart*3 + (iGridQuart+ iRow/2) * iGridDimension); 

                    StitchIB.push(iCurrGridStart + (iGridDimension-1) + (iRow+2) * iGridDimension); 
                    StitchIB.push(iCurrGridStart + (iGridDimension-1) + (iRow+1) * iGridDimension); 
                    StitchIB.push(iNextGridStart + iGridQuart*3 + (iGridQuart+ iRow/2) * iGridDimension); 

                    StitchIB.push(iCurrGridStart + (iGridDimension-1) + (iRow+2) * iGridDimension); 
                    StitchIB.push(iNextGridStart + iGridQuart*3 + (iGridQuart+ iRow/2)     * iGridDimension); 
                    StitchIB.push(iNextGridStart + iGridQuart*3 + (iGridQuart+ iRow/2 + 1) * iGridDimension); 
                }
            }


            // Generate indices for the current ring
            if( iRing == 0 )
            {
                RingMeshBuilder.createMesh( iCurrGridStart, 0,                   0, iGridMidst+1, iGridMidst+1, CTriStrip.QUAD_TRIANG_TYPE_00_TO_11);
                RingMeshBuilder.createMesh( iCurrGridStart, iGridMidst,          0, iGridMidst+1, iGridMidst+1, CTriStrip.QUAD_TRIANG_TYPE_01_TO_10);
                RingMeshBuilder.createMesh( iCurrGridStart, 0,          iGridMidst, iGridMidst+1, iGridMidst+1, CTriStrip.QUAD_TRIANG_TYPE_01_TO_10);
                RingMeshBuilder.createMesh( iCurrGridStart, iGridMidst, iGridMidst, iGridMidst+1, iGridMidst+1, CTriStrip.QUAD_TRIANG_TYPE_00_TO_11);
            }
            else
            {
                RingMeshBuilder.createMesh( iCurrGridStart,            0,            0,   iGridQuart+1, iGridQuart+1, CTriStrip.QUAD_TRIANG_TYPE_00_TO_11);
                RingMeshBuilder.createMesh( iCurrGridStart,   iGridQuart,            0,   iGridQuart+1, iGridQuart+1, CTriStrip.QUAD_TRIANG_TYPE_00_TO_11);

                RingMeshBuilder.createMesh( iCurrGridStart,   iGridMidst,            0,   iGridQuart+1, iGridQuart+1, CTriStrip.QUAD_TRIANG_TYPE_01_TO_10);
                RingMeshBuilder.createMesh( iCurrGridStart, iGridQuart*3,            0,   iGridQuart+1, iGridQuart+1, CTriStrip.QUAD_TRIANG_TYPE_01_TO_10);
                                            
                RingMeshBuilder.createMesh( iCurrGridStart,            0,   iGridQuart,   iGridQuart+1, iGridQuart+1, CTriStrip.QUAD_TRIANG_TYPE_00_TO_11);
                RingMeshBuilder.createMesh( iCurrGridStart,            0,   iGridMidst,   iGridQuart+1, iGridQuart+1, CTriStrip.QUAD_TRIANG_TYPE_01_TO_10);
                                            
                RingMeshBuilder.createMesh( iCurrGridStart, iGridQuart*3,   iGridQuart,   iGridQuart+1, iGridQuart+1, CTriStrip.QUAD_TRIANG_TYPE_01_TO_10);
                RingMeshBuilder.createMesh( iCurrGridStart, iGridQuart*3,   iGridMidst,   iGridQuart+1, iGridQuart+1, CTriStrip.QUAD_TRIANG_TYPE_00_TO_11);

                RingMeshBuilder.createMesh( iCurrGridStart,            0, iGridQuart*3,   iGridQuart+1, iGridQuart+1, CTriStrip.QUAD_TRIANG_TYPE_01_TO_10);
                RingMeshBuilder.createMesh( iCurrGridStart,   iGridQuart, iGridQuart*3,   iGridQuart+1, iGridQuart+1, CTriStrip.QUAD_TRIANG_TYPE_01_TO_10);

                RingMeshBuilder.createMesh( iCurrGridStart,   iGridMidst, iGridQuart*3,   iGridQuart+1, iGridQuart+1, CTriStrip.QUAD_TRIANG_TYPE_00_TO_11);
                RingMeshBuilder.createMesh( iCurrGridStart, iGridQuart*3, iGridQuart*3,   iGridQuart+1, iGridQuart+1, CTriStrip.QUAD_TRIANG_TYPE_00_TO_11);
            }
        }
        
        // We do not need per-vertex normals as we use normal map to shade terrain
        // Sphere tangent vertex are computed in the shader
    }
    
    public void UpdateParams(SRenderingParams NewParams){
    	if( m_Params.m_iNumShadowCascades    != NewParams.m_iNumShadowCascades    ||
    	        m_Params.m_bBestCascadeSearch    != NewParams.m_bBestCascadeSearch    || 
    	        m_Params.m_bSmoothShadows        != NewParams.m_bSmoothShadows )
    	    {	
	        	if(m_RenderEarthHemispherePS != null){
	        		m_RenderEarthHemispherePS.dispose();
	        		m_RenderEarthHemispherePS = null;
	        	}    	    
	        }

    	    m_Params = NewParams;  // TODO
    }
    
    private static final class SHemisphereVertex{
    	final Vector3f f3WorldPos = new Vector3f();
    	final Vector2f f2MaskUV0 = new Vector2f();
    }
}
