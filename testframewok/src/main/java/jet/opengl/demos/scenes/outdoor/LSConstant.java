package jet.opengl.demos.scenes.outdoor;

interface LSConstant {

	int MAX_CASCADES = 8;
	
	int LIGHT_SCTR_TECHNIQUE_EPIPOLAR_SAMPLING = 0;
	int LIGHT_SCTR_TECHNIQUE_BRUTE_FORCE = 1;

	int CASCADE_PROCESSING_MODE_SINGLE_PASS = 0;
	int CASCADE_PROCESSING_MODE_MULTI_PASS = 1;
	int CASCADE_PROCESSING_MODE_MULTI_PASS_INST = 2;

	int REFINEMENT_CRITERION_DEPTH_DIFF = 0;
	int REFINEMENT_CRITERION_INSCTR_DIFF = 1;

	// Extinction evaluation mode used when attenuating background
	int EXTINCTION_EVAL_MODE_PER_PIXEL = 0;// Evaluate extinction for each pixel using analytic formula 
	                                        // by Eric Bruneton
	int EXTINCTION_EVAL_MODE_EPIPOLAR = 1; // Render extinction in epipolar space and perform
	                                        // bilateral filtering in the same manner as for
	                                        // inscattering

	int SINGLE_SCTR_MODE_NONE = 0;
	int SINGLE_SCTR_MODE_INTEGRATION = 1;
	int SINGLE_SCTR_MODE_LUT = 2;

	int MULTIPLE_SCTR_MODE_NONE = 0;
	int MULTIPLE_SCTR_MODE_UNOCCLUDED = 1;
	int MULTIPLE_SCTR_MODE_OCCLUDED = 2;

	int TONE_MAPPING_MODE_EXP = 0;
	int TONE_MAPPING_MODE_REINHARD = 1;
	int TONE_MAPPING_MODE_REINHARD_MOD = 2;
	int TONE_MAPPING_MODE_UNCHARTED2 = 3;
	int TONE_MAPPING_FILMIC_ALU = 4;
	int TONE_MAPPING_LOGARITHMIC = 5;
	int TONE_MAPPING_ADAPTIVE_LOG = 6;
	
	int TERRAIN_BINDING = 0;
	int CAMERA_BINDING = 1;
	int LIGHT_BINDING = 2;
	int AIRSCATTERING_BINDING = 3;
	int POSTPROCESS_BINDING = 0;
	int MISC_BINDING = 4;
	
	int sm_iNumPrecomputedHeights = 256;
	int sm_iNumPrecomputedAngles = 256;
	
	int sm_iPrecomputedSctrUDim = 32;
	int sm_iPrecomputedSctrVDim = 128;
	int sm_iPrecomputedSctrWDim = 64;
	int sm_iPrecomputedSctrQDim = 16;
	
	int sm_iNumRandomSamplesOnSphere = 128;
	int sm_iAmbientSkyLightTexDim = 1024;
	
	int sm_iLowResLuminanceMips = 7; // 64x64
	
	String[] textureNames = {
			"g_tex2DDepthBuffer", 
			"g_tex2DCamSpaceZ", 
			"g_tex2DSliceEndPoints",
			"g_tex2DCoordinates",
			"g_tex2DEpipolarCamSpaceZ",
			"g_tex2DInterpolationSource",
			"g_tex2DLightSpaceDepthMap",
			"g_tex2DSliceUVDirAndOrigin",
			"g_tex2DMinMaxLightSpaceDepth",
			"g_tex2DInitialInsctrIrradiance",
			"g_tex2DColorBuffer",
			"g_tex2DScatteredColor",
			"g_tex2DOccludedNetDensityToAtmTop",
			"g_tex2DEpipolarExtinction",
			"g_tex3DSingleSctrLUT",
			"g_tex3DHighOrderSctrLUT",
			"g_tex3DMultipleSctrLUT",
			"g_tex2DSphereRandomSampling",
			"g_tex3DPreviousSctrOrder",
			"g_tex3DPointwiseSctrRadiance",
			"g_tex2DAverageLuminance",
			"g_tex2DLowResLuminance"
		};
}
