package jet.opengl.postprocessing.core.outdoorLighting;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.ReadableVector2f;
import org.lwjgl.util.vector.ReadableVector3f;
import org.lwjgl.util.vector.ReadableVector4f;
import org.lwjgl.util.vector.Vector2f;

import java.io.IOException;
import java.nio.FloatBuffer;

import jet.opengl.postprocessing.core.PostProcessingFrameAttribs;
import jet.opengl.postprocessing.shader.GLSLProgram;
import jet.opengl.postprocessing.shader.Macro;
import jet.opengl.postprocessing.shader.ShaderLoader;
import jet.opengl.postprocessing.shader.ShaderSourceItem;
import jet.opengl.postprocessing.shader.ShaderType;
import jet.opengl.postprocessing.util.CacheBuffer;


final class RenderTechnique extends GLSLProgram {

//	Texture2D<float>  g_tex2DDepthBuffer            : register( t0 );
//	Texture2D<float>  g_tex2DCamSpaceZ              : register( t0 );
//	Texture2D<float4> g_tex2DSliceEndPoints         : register( t4 );
//	Texture2D<float2> g_tex2DCoordinates            : register( t1 );
//	Texture2D<float>  g_tex2DEpipolarCamSpaceZ      : register( t2 );
//	Texture2D<uint2>  g_tex2DInterpolationSource    : register( t6 );
//	Texture2DArray<float> g_tex2DLightSpaceDepthMap : register( t3 );
//	Texture2D<float4> g_tex2DSliceUVDirAndOrigin    : register( t6 );
//	Texture2D<MIN_MAX_DATA_FORMAT> g_tex2DMinMaxLightSpaceDepth  : register( t4 );
//	Texture2D<float3> g_tex2DInitialInsctrIrradiance: register( t5 );
//	Texture2D<float4> g_tex2DColorBuffer            : register( t1 );
//	Texture2D<float3> g_tex2DScatteredColor         : register( t3 );
//	Texture2D<float2> g_tex2DOccludedNetDensityToAtmTop : register( t5 );
//	Texture2D<float3> g_tex2DEpipolarExtinction     : register( t6 );
//	Texture3D<float3> g_tex3DSingleSctrLUT          : register( t7 );
//	Texture3D<float3> g_tex3DHighOrderSctrLUT       : register( t8 );
//	Texture3D<float3> g_tex3DMultipleSctrLUT        : register( t9 );
//	Texture2D<float3> g_tex2DSphereRandomSampling   : register( t1 );
//	Texture3D<float3> g_tex3DPreviousSctrOrder      : register( t0 );
//	Texture3D<float3> g_tex3DPointwiseSctrRadiance  : register( t0 );
//	Texture2D<float>  g_tex2DAverageLuminance       : register( t10 );
//	Texture2D<float>  g_tex2DLowResLuminance        : register( t0 );

	public static final int TEX2D_DEPTH = 0;
	public static final int TEX2D_CAM_SPACE = 1;
	public static final int TEX2D_SLICE_END_POINTS = 2;
	public static final int TEX2D_COORDINATES = 3;
	public static final int TEX2D_EPIPOLAR_CAM_SPACE = 4;
	public static final int TEX2D_INTERPOLATION_SOURCE = 5;
	public static final int TEX2D_LIGHT_SPACE_DEPTH = 6;
	public static final int TEX2D_SLICE_UV_ORIGIN = 7;
	public static final int TEX2D_MIN_MAX_LIGHT_DEPTH = 8;
	public static final int TEX2D_INITIAL_IRRANDIANCE = 9;
	public static final int TEX2D_COLOR = 10;
	public static final int TEX2D_SCATTERED_COLOR = 11;
	public static final int TEX2D_OCCLUDED_NET_DENSITY = 12;
	public static final int TEX2D_EPIPOLAR_EXTINCTION = 13;
	public static final int TEX3D_SINGLE_LUT = 14;
	public static final int TEX3D_HIGH_ORDER_LUT = 15;
	public static final int TEX3D_MULTIPLE_LUT = 16;
	public static final int TEX2D_SPHERE_RANDOM = 17;
	public static final int TEX3D_PREVIOUS_RADIANCE = 18;
	public static final int TEX3D_POINT_TWISE_RANDIANCE = 19;
	public static final int TEX2D_AVERAGE_LUMINACE = 20;
	public static final int TEX2D_LOW_RES_LUMINACE = 21;
	public static final int TEX2D_SHADOW_MAP = 22;

	private int angularRayleighSctrCoeffIndex;
	private int csgIndex;
	private int angularMieSctrCoeffIndex;
	private int viewInvIndex;
	private int projIndex;
	private int viewProjInvIndex;
	private int wqIndex;
	private int depthSliceIndex;
	private int farPlaneZIndex;
	private int nearPlaneZIndex;
	private int cameraPosIndex;

	private int worldToShadowMapUVDepthIndex;
	private int shadowMapTexelSizeIndex;
	private int cascadesStartEndZIndex;
	private int cascadeInd;
	private int maxStepsAlongRay;
	private int mumCascadesIndex;
	private int epipoleSDFactorIndex;
	private int srcDstMinMaxLevelOffset;
	private int isLightOnScreenIndex;
	private int lightScreenPosIndex;
	private int refinementThresholdIndex;
	private int epipoleSamplingDensityFactorIndex;
	private int firstCascadeIndex;
	private int maxShadowMapStepIndex;
	private int minMaxShadowMapResolutionIndex;
	private int numEpipolarSlicesIndex;
	
	private String debugName;
	
	private int earthRadiusIndex;
	private int dirOnLightIndex;
	private int atmTopHeightIndex;
	private int particleScaleHeightIndex;
	private int atmToRadiusIndex;
	
	private int rayleighExtinctionCoeffIndex;
	private int mieExtinctionCoeffIndex;
	private int extraterrestrialSunColorIndex;

	public RenderTechnique(String filename) { this(filename, null);}
	public RenderTechnique(String filename, Macro[] macros) {
		try {
			setSourceFromFiles("shader_libs/PostProcessingDefaultScreenSpaceVS.vert", "shader_libs/OutdoorSctr/" + filename, macros);
		} catch (IOException e) {
			e.printStackTrace();
		}

		initUniformIndices();
	}

	/** Only for the compute shader. */
	public RenderTechnique(Void unused, String filename, Macro[] macros) {
		try {
			CharSequence computeSrc = ShaderLoader.loadShaderFile("shader_libs/OutdoorSctr/" + filename, false);
			ShaderSourceItem cs_item = new ShaderSourceItem(computeSrc, ShaderType.COMPUTE);
			cs_item.macros = macros;
			setSourceFromStrings(cs_item);
		} catch (IOException e) {
			e.printStackTrace();
		}

		initUniformIndices();
	}
	

//	public void printPrograminfo(){
//		System.out.println("----------------------------"+debugName +"-----------------------------------------" );
//		ProgramProperties props = GLSLUtil.getProperties(getProgram());
//		System.out.println(props);
//	}

	public void setupUniforms(OutdoorLightScatteringFrameAttribs attribs){
		setRefinementThreshold(attribs.m_fRefinementThreshold);
		setEpipoleSamplingDensityFactor(attribs.m_uiEpipoleSamplingDensityFactor);
		setMaxShadowMapStep(attribs.m_fMaxShadowMapStep);

		setEarthRadius(attribs.fEarthRadius);
		setAtmTopHeight(attribs.fAtmTopHeight);
		setAtmToRadius(attribs.fEarthRadius + attribs.fAtmTopHeight);
		setParticleScaleHeight(attribs.f2ParticleScaleHeight);
		setExtraterrestrialSunColor(attribs.f4ExtraterrestrialSunColor);
	}

	public void setupUniforms(SMiscDynamicParams attribs) {
		setCascadeInd(attribs.fCascadeInd);
		setMaxStepsAlongRay(attribs.fMaxStepsAlongRay);
		setSrcDstMinMaxLevelOffset(attribs.ui4SrcMinMaxLevelXOffset, attribs.ui4SrcMinMaxLevelYOffset, attribs.ui4DstMinMaxLevelXOffset, attribs.ui4DstMinMaxLevelYOffset);
	}

	public void setupUniforms(OutdoorLightScatteringInitAttribs attribs){
		setNumEpipolarSlices(attribs.m_uiNumEpipolarSlices);
		setFirstCascade(attribs.m_iFirstCascade);
	}
	
	public void setupUniforms(SAirScatteringAttribs attribs){
		setRayleighExtinctionCoeff(attribs.f4RayleighExtinctionCoeff);
		setMieExtinctionCoeff(attribs.f4MieExtinctionCoeff);

		setAngularRayleighSctrCoeff(attribs.f4AngularRayleighSctrCoeff);
		setAngularMieSctrCoeff(attribs.f4AngularMieSctrCoeff);
		setCSG(attribs.f4CS_g);
	}

	public void setupUniforms(PostProcessingFrameAttribs attribs){
		setCameraPlane(attribs.cameraFar, attribs.cameraNear);
		setCameraPos(attribs.getCameraPos());
		setProjMatrix(attribs.projMat);
		setViewProjInvMatrix(attribs.getViewProjInvertMatrix());
		setMumCascadesIndex(attribs.cascadeShadowMapAttribs.numCascades);
//		setViewInvMatrix(attribs.get);  TODO

		final float texelSizeX = 1.0f/attribs.shadowMapTexture.getWidth();
		final float texelSizeY = 1.0f/attribs.shadowMapTexture.getHeight();
		setShadowMapTexelSize(texelSizeX, texelSizeY);
		setMinMaxShadowMapResolution(Math.max(attribs.shadowMapTexture.getWidth(), attribs.shadowMapTexture.getHeight()));

		setWorldToShadowMapUVDepthMatrixs(attribs.cascadeShadowMapAttribs.worldToShadowMapUVDepth);
		{
			final int MAX_CASCADES = 8;
			FloatBuffer bufs = CacheBuffer.getCachedFloatBuffer(2 * MAX_CASCADES);
			for (int i = 0; i < MAX_CASCADES; i++) {
				Vector2f startEndZ = attribs.cascadeShadowMapAttribs.startEndZ[i];
				if(startEndZ ==null){
					break;
				}
				startEndZ.store(bufs);
			}
			bufs.position(2 * MAX_CASCADES).flip();
			setCascadesStartEndZ(bufs);
		}
	}
	
	public void setupUniforms(SLightAttribs attribs){
		setDirOnLight(attribs.f4DirOnLight);

		setLightOnScreen(attribs.bIsLightOnScreen);
		setLightScreenPos(attribs.f4LightScreenPos.x, attribs.f4LightScreenPos.y, attribs.f4LightScreenPos.z, attribs.f4LightScreenPos.w);

	}
	
	public String getDebugName() { return debugName;}
	public void   setDebugName(String name) { debugName = name;}
	
	protected void initUniformIndices(){
		earthRadiusIndex = gl.glGetUniformLocation(getProgram(), "g_fEarthRadius");
		dirOnLightIndex = gl.glGetUniformLocation(getProgram(), "g_f4DirOnLight");
		atmTopHeightIndex = gl.glGetUniformLocation(getProgram(), "g_fAtmTopHeight");
		particleScaleHeightIndex = gl.glGetUniformLocation(getProgram(), "g_f2ParticleScaleHeight");
		atmToRadiusIndex = gl.glGetUniformLocation(getProgram(), "g_fAtmTopRadius");
		
		rayleighExtinctionCoeffIndex = gl.glGetUniformLocation(getProgram(), "g_f4RayleighExtinctionCoeff");
		mieExtinctionCoeffIndex = gl.glGetUniformLocation(getProgram(), "g_f4MieExtinctionCoeff");
		extraterrestrialSunColorIndex = gl.glGetUniformLocation(getProgram(), "g_f4ExtraterrestrialSunColor");

		angularRayleighSctrCoeffIndex = gl.glGetUniformLocation(getProgram(), "g_f4AngularRayleighSctrCoeff");
		csgIndex 					  = gl.glGetUniformLocation(getProgram(), "g_f4CS_g");
		angularMieSctrCoeffIndex 	  = gl.glGetUniformLocation(getProgram(), "g_f4AngularMieSctrCoeff");
		viewInvIndex 				  = gl.glGetUniformLocation(getProgram(), "g_ViewInv");
		projIndex 					  = gl.glGetUniformLocation(getProgram(), "g_Proj");
		viewProjInvIndex 			  = gl.glGetUniformLocation(getProgram(), "g_ViewProjInv");
		wqIndex						  = gl.glGetUniformLocation(getProgram(), "g_f2WQ");
		depthSliceIndex				  = gl.glGetUniformLocation(getProgram(), "g_uiDepthSlice");

		farPlaneZIndex				  = gl.glGetUniformLocation(getProgram(), "g_fFarPlaneZ");
		nearPlaneZIndex				  = gl.glGetUniformLocation(getProgram(), "g_fNearPlaneZ");
		cameraPosIndex				  = gl.glGetUniformLocation(getProgram(), "g_f4CameraPos");
		worldToShadowMapUVDepthIndex  = gl.glGetUniformLocation(getProgram(), "g_WorldToShadowMapUVDepth");
		shadowMapTexelSizeIndex		  = gl.glGetUniformLocation(getProgram(), "g_f2ShadowMapTexelSize");
		cascadesStartEndZIndex		  = gl.glGetUniformLocation(getProgram(), "g_f4ShadowAttribs_Cascades_StartEndZ");
		cascadeInd					  = gl.glGetUniformLocation(getProgram(), "g_fCascadeInd");
		maxStepsAlongRay			  = gl.glGetUniformLocation(getProgram(), "g_fMaxStepsAlongRay");
		mumCascadesIndex			  = gl.glGetUniformLocation(getProgram(), "g_iNumCascades");
		epipoleSDFactorIndex		  = gl.glGetUniformLocation(getProgram(), "g_uiEpipoleSamplingDensityFactor");
		srcDstMinMaxLevelOffset		  = gl.glGetUniformLocation(getProgram(), "g_ui4SrcDstMinMaxLevelOffset");
		isLightOnScreenIndex		  = gl.glGetUniformLocation(getProgram(), "g_bIsLightOnScreen");
		lightScreenPosIndex		  	  = gl.glGetUniformLocation(getProgram(), "g_f4LightScreenPos");
		refinementThresholdIndex	  = gl.glGetUniformLocation(getProgram(), "g_fRefinementThreshold");
		firstCascadeIndex	  		  = gl.glGetUniformLocation(getProgram(), "g_iFirstCascade");
		maxShadowMapStepIndex	  	  = gl.glGetUniformLocation(getProgram(), "g_fMaxShadowMapStep");
		minMaxShadowMapResolutionIndex= gl.glGetUniformLocation(getProgram(), "g_uiMinMaxShadowMapResolution");
		numEpipolarSlicesIndex		  = gl.glGetUniformLocation(getProgram(), "g_uiNumEpipolarSlices");
		epipoleSamplingDensityFactorIndex = gl.glGetUniformLocation(getProgram(), "g_uiEpipoleSamplingDensityFactor");
	}
	
	private void setEarthRadius(float radius) {
		if(earthRadiusIndex >= 0){
			gl.glProgramUniform1f(getProgram(), earthRadiusIndex, radius);
		}
	}

	private void setAtmToRadius(float radius) {
		if(atmToRadiusIndex >= 0){
			gl.glProgramUniform1f(getProgram(), atmToRadiusIndex, radius);
		}
	}

	private void setParticleScaleHeight(ReadableVector2f scaleHeight) {
		if(particleScaleHeightIndex >= 0){
			gl.glProgramUniform2f(getProgram(), particleScaleHeightIndex, scaleHeight.getX(), scaleHeight.getY());
		}
	}

	private void setDirOnLight(ReadableVector3f dir){
		if(dirOnLightIndex >= 0){
			gl.glProgramUniform4f(getProgram(), dirOnLightIndex, dir.getX(), dir.getY(), dir.getZ(), 0);
		}
	}

	private void setDirOnLight(float x, float y, float z){
		if(dirOnLightIndex >= 0){
			gl.glProgramUniform4f(getProgram(), dirOnLightIndex, x, y, z, 0);
		}
	}

	private void setAtmTopHeight(float height){
		if(atmTopHeightIndex >= 0){
			gl.glProgramUniform1f(getProgram(), atmTopHeightIndex, height);
		}
	}

	private void setRayleighExtinctionCoeff(ReadableVector4f coeff){
		if(rayleighExtinctionCoeffIndex >= 0){
			gl.glProgramUniform4f(getProgram(), rayleighExtinctionCoeffIndex, coeff.getX(), coeff.getY(), coeff.getZ(), coeff.getW());
		}
	}

	private void setMieExtinctionCoeff(ReadableVector4f coeff){
		if(mieExtinctionCoeffIndex >= 0){
			gl.glProgramUniform4f(getProgram(), mieExtinctionCoeffIndex, coeff.getX(), coeff.getY(), coeff.getZ(), coeff.getW());
		}
	}
	
	private void setExtraterrestrialSunColor(ReadableVector3f rgb){
		if(extraterrestrialSunColorIndex >= 0){
			gl.glProgramUniform4f(getProgram(), extraterrestrialSunColorIndex, rgb.getX(), rgb.getY(), rgb.getZ(), 1);
		}
	}

	private void setNumEpipolarSlices(int slices){
		if(numEpipolarSlicesIndex >= 0){
			gl.glProgramUniform1i(getProgram(), numEpipolarSlicesIndex, slices);
		}
	}

	private void setMinMaxShadowMapResolution(int resolution){
		if(minMaxShadowMapResolutionIndex >= 0){
			gl.glProgramUniform1i(getProgram(), minMaxShadowMapResolutionIndex, resolution);
		}
	}

	private void setMaxShadowMapStep(float f){
		if(maxShadowMapStepIndex>=0){
			gl.glProgramUniform1f(getProgram(), maxShadowMapStepIndex, f);
		}
	}

	private void setFirstCascade(int i){
		if(firstCascadeIndex >= 0){
			gl.glProgramUniform1i(getProgram(), firstCascadeIndex, i);
		}
	}

	private void setEpipoleSamplingDensityFactor(int factor){
		if(epipoleSamplingDensityFactorIndex >= 0){
			gl.glProgramUniform1i(getProgram(), epipoleSamplingDensityFactorIndex, factor);
		}
	}

	private void setRefinementThreshold(float threshold){
		if(refinementThresholdIndex >= 0){
			gl.glProgramUniform1f(getProgram(), refinementThresholdIndex, threshold);
		}
	}

	private void setLightScreenPos(float x, float y, float z, float w){
		if(lightScreenPosIndex >= 0){
			gl.glProgramUniform4f(getProgram(), lightScreenPosIndex, x, y, z, w);
		}
	}

	private void setSrcDstMinMaxLevelOffset(int x, int y, int z, int w){
		if(srcDstMinMaxLevelOffset >= 0){
			gl.glProgramUniform4i(getProgram(), srcDstMinMaxLevelOffset, x, y, z, w);
		}
	}

	private void setLightOnScreen(boolean flag){
		if(isLightOnScreenIndex >= 0){
			gl.glProgramUniform1i(getProgram(), isLightOnScreenIndex, flag ? 1 : 0);
		}
	}

	private void setEpipoleSamplingDensityFactorIndex(int i){
		if(epipoleSDFactorIndex >= 0){
			gl.glProgramUniform1i(getProgram(), epipoleSDFactorIndex, i);
		}
	}
	private void setMumCascadesIndex(int i){
		if(mumCascadesIndex >= 0){
			gl.glProgramUniform1i(getProgram(), mumCascadesIndex, i);
		}
	}

	private void setMaxStepsAlongRay(float value){
		if(maxStepsAlongRay >= 0){
			gl.glProgramUniform1f(getProgram(), maxStepsAlongRay, value);
		}
	}

	private void setCascadeInd(float index){
		if(cascadeInd >= 0){
			gl.glProgramUniform1f(getProgram(), cascadeInd, index);
		}
	}

	private void setCascadesStartEndZ(FloatBuffer values){
		if(cascadesStartEndZIndex >= 0){
			gl.glProgramUniform2fv(getProgram(), cascadesStartEndZIndex, values);
		}
	}

	private void setCameraPos(ReadableVector3f pos){
		if(cameraPosIndex >= 0){
			gl.glProgramUniform4f(getProgram(), cameraPosIndex, pos.getX(), pos.getY(), pos.getZ(), 1);
		}
	}

	private void setShadowMapTexelSize(float x, float y){
		if(shadowMapTexelSizeIndex >= 0){
			gl.glProgramUniform2f(getProgram(), shadowMapTexelSizeIndex, x, y);
		}
	}

	private void setCameraPlane(float far, float near){
		if(farPlaneZIndex >= 0){
			gl.glProgramUniform1f(getProgram(), farPlaneZIndex, far);
		}

		if(nearPlaneZIndex >= 0){
			gl.glProgramUniform1f(getProgram(), nearPlaneZIndex, near);
		}
	}

	public void setAngularRayleighSctrCoeff(ReadableVector4f coeff){
		if(angularRayleighSctrCoeffIndex >= 0){
			gl.glProgramUniform4f(getProgram(), angularRayleighSctrCoeffIndex, coeff.getX(), coeff.getY(), coeff.getZ(), coeff.getW());
		}
	}

	public void setCSG(ReadableVector4f coeff){
		if(csgIndex >= 0){
			gl.glProgramUniform4f(getProgram(), csgIndex, coeff.getX(), coeff.getY(), coeff.getZ(), coeff.getW());
		}
	}

	public void setWQ(float w, float q){
		if(wqIndex >= 0){
			gl.glProgramUniform2f(getProgram(), wqIndex, w, q);
		}
	}

	public void setDepthSlice(int slice){
		if(depthSliceIndex >= 0){
			gl.glProgramUniform1i(getProgram(), depthSliceIndex, slice);
		}
	}

	public void setAngularMieSctrCoeff(ReadableVector4f coeff){
		if(angularMieSctrCoeffIndex >= 0){
			gl.glProgramUniform4f(getProgram(), angularMieSctrCoeffIndex, coeff.getX(), coeff.getY(), coeff.getZ(), coeff.getW());
		}
	}

	void setWorldToShadowMapUVDepthMatrixs(Matrix4f[] mats){
		if(worldToShadowMapUVDepthIndex >= 0){
			FloatBuffer buf = CacheBuffer.getCachedFloatBuffer(16 * mats.length);
			for(int i = 0; i < mats.length; i++){
				mats[i].store(buf);
			}

			buf.flip();
			gl.glProgramUniformMatrix4fv(getProgram(), worldToShadowMapUVDepthIndex, false, buf);
		}
	}

	public void setProjMatrix(Matrix4f mat){
		if(projIndex >= 0){
			gl.glProgramUniformMatrix4fv(getProgram(), projIndex, false, CacheBuffer.wrap(mat));
		}
	}

	public void setViewInvMatrix(Matrix4f mat){
		if(viewInvIndex >= 0){
			gl.glProgramUniformMatrix4fv(getProgram(), viewInvIndex, false, CacheBuffer.wrap(mat));
		}
	}

	public void setViewProjInvMatrix(Matrix4f mat){
		if(viewProjInvIndex >= 0){
			gl.glProgramUniformMatrix4fv(getProgram(), viewProjInvIndex, false, CacheBuffer.wrap(mat));
		}
	}
}
