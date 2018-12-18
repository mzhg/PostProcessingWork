package jet.opengl.demos.nvidia.volumelight;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector2f;
import org.lwjgl.util.vector.Vector3f;
import org.lwjgl.util.vector.Vector4f;

import java.io.IOException;

import jet.opengl.postprocessing.common.Disposeable;
import jet.opengl.postprocessing.common.GLAPIVersion;
import jet.opengl.postprocessing.common.GLCheck;
import jet.opengl.postprocessing.common.GLFuncProviderFactory;
import jet.opengl.postprocessing.common.GLenum;
import jet.opengl.postprocessing.core.OpenGLProgram;
import jet.opengl.postprocessing.texture.Texture2D;
import jet.opengl.postprocessing.texture.Texture2DDesc;
import jet.opengl.postprocessing.texture.TextureGL;
import jet.opengl.postprocessing.texture.TextureUtils;
import jet.opengl.postprocessing.util.DebugTools;


public abstract class ContextImp_Common implements Disposeable {

    protected static final boolean USE_UNIFORM_BLOCK = true ;
	// Miscellaneous internal state
    boolean isInitialized_;
    DebugFlags debugFlags_;
    final ContextDesc contextDesc_ = new ContextDesc();  // TODO could be reference copy
    final ViewerDesc viewerDesc_ = new ViewerDesc();	 // TODO could be reference copy
    int jitterIndex_;
    int lastFrameIndex_;
    int nextFrameIndex_;
    final Matrix4f lastViewProj_ = new Matrix4f();
    final Matrix4f nextViewProj_ = new Matrix4f();
    
    // ------  Debug variables ---------------
    protected int faceIdx;
    protected boolean showFace;
    protected boolean mbSavedDepthBuffer = false;
    
    //--------------------------------------------------------------------------
    // Constructors
    // Protected constructor - Should only ever instantiate children
    protected ContextImp_Common() {};
    
    public void setFaceIndex(int faceIdx) {this.faceIdx = faceIdx;}
    public void nextFace() {faceIdx = (++faceIdx) % 7;}
    public void prevFace() {
    	faceIdx --;
    	if(faceIdx < 0 )
    		faceIdx = 6;
    }
    public void toggleFaceVisiable() {showFace = !showFace;}
    public void saveDepthBuffer()    {mbSavedDepthBuffer = true;}
    
 // Creates the context and resources
    public static ContextImp_Common create(ContextDesc pContextDesc){
        GLAPIVersion version = GLFuncProviderFactory.getGLFuncProvider().getGLAPIVersion();
        boolean supportComputeShader =version.ES ?  (version.major >= 3 && version.major >= 1) : (version.major >= 4 && version.minor >= 3);

    	if(!supportComputeShader){
    		System.err.println("The hardware doesn't support! Please update your videocard driver!");
    		return null;
    	}
    	
    	ContextImp_OpenGL out_ctx = new ContextImp_OpenGL(pContextDesc);
    	Status status = out_ctx.createResources();
    	
    	if(status != Status.OK){
    		System.err.println("Initlize resource error!");
    		return null;
    	}
    	
    	GLCheck.checkError();
    	return out_ctx;
    }

    // Call this from base-class for actual initialization
    protected ContextImp_Common( ContextDesc pContextDesc){
    	isInitialized_ = false;
        jitterIndex_ = 0;
        lastFrameIndex_ = -1;
        nextFrameIndex_ = -0;
        
        contextDesc_.set(pContextDesc);
    }
    
    public Status beginAccumulation( int sceneDepth, ViewerDesc pViewerDesc, MediumDesc pMediumDesc, DebugFlags debugFlags){
    	debugFlags_ = debugFlags;
    	viewerDesc_.set(pViewerDesc);
    	Status code = beginAccumulation_Start(sceneDepth, pViewerDesc, pMediumDesc);
    	if(code != Status.OK)  return code;
    		
    	code = beginAccumulation_UpdateMediumLUT();
    	if(code != Status.OK)  return code;
    	
    	code = beginAccumulation_CopyDepth(sceneDepth);
    	if(code != Status.OK)  return code;
    	
    	return beginAccumulation_End(sceneDepth, pViewerDesc, pMediumDesc);
    }
    
    public Status renderVolume( int shadowMap, ShadowMapDesc pShadowMapDesc, LightDesc pLightDesc, VolumeDesc pVolumeDesc){
    	Status code = renderVolume_Start(shadowMap, pShadowMapDesc, pLightDesc, pVolumeDesc);
    	if(code != Status.OK)  return code;
    	
    	switch (pLightDesc.eType) {
		case DIRECTIONAL:
			code = renderVolume_DoVolume_Directional(shadowMap, pShadowMapDesc, pLightDesc, pVolumeDesc);
	    	if(code != Status.OK)  return code;
			break;
		case SPOT:
			code = renderVolume_DoVolume_Spotlight(shadowMap, pShadowMapDesc, pLightDesc, pVolumeDesc);
	    	if(code != Status.OK)  return code;
			break;
        case POINT:
			code = renderVolume_DoVolume_Omni(shadowMap, pShadowMapDesc, pLightDesc, pVolumeDesc);
	    	if(code != Status.OK)  return code;
			break;
		default:
			// Error -- unsupported light type
			return Status.INVALID_PARAMETER;
		}
    	
    	return renderVolume_End(shadowMap, pShadowMapDesc, pLightDesc, pVolumeDesc);
    }

    public Status endAccumulation(){
    	return endAccumulation_Imp();
    }
    
    public Status applyLighting( int sceneTarget, int sceneDepth, PostprocessDesc pPostprocessDesc){
    	Status code = applyLighting_Start(sceneTarget, sceneDepth, pPostprocessDesc);
    	if(code != Status.OK)  return code;
    	
    	if(getFilterMode() == FilterMode.TEMPORAL){
    		code = applyLighting_Resolve(pPostprocessDesc);
    		if(code != Status.OK)  return code;
    		
    		code = applyLighting_TemporalFilter(sceneDepth, pPostprocessDesc);
    		if(code != Status.OK)  return code;
    	}else if(isInternalMSAA()){
    		code = applyLighting_Resolve(pPostprocessDesc);
    		if(code != Status.OK)  return code;
    	}
    	
    	code = applyLighting_Composite(sceneTarget, sceneDepth, pPostprocessDesc);
		if(code != Status.OK)  return code;
		code = applyLighting_End(sceneTarget, sceneDepth, pPostprocessDesc);
		if(code != Status.OK)  return code;
		
		// Update frame counters as needed
	    jitterIndex_ = (jitterIndex_ + 1) % VLConstant.MAX_JITTER_STEPS;
	    lastFrameIndex_ = nextFrameIndex_;
	    nextFrameIndex_ = (nextFrameIndex_ + 1) % 2;
		return Status.OK;
    }
    
  //--------------------------------------------------------------------------
    // Implementation Stubs

    // BeginAccumulation
    protected abstract Status beginAccumulation_Start(int sceneDepth, ViewerDesc pViewerDesc, MediumDesc pMediumDesc);
    protected abstract Status beginAccumulation_UpdateMediumLUT();
    protected abstract Status beginAccumulation_CopyDepth(int sceneDepth);
    protected abstract Status beginAccumulation_End(int sceneDepth, ViewerDesc pViewerDesc, MediumDesc pMediumDesc);

    // RenderVolume
    protected abstract Status renderVolume_Start(int shadowMap, ShadowMapDesc pShadowMapDesc, LightDesc pLightDesc, VolumeDesc pVolumeDesc);
    protected abstract Status renderVolume_DoVolume_Directional(int shadowMap, ShadowMapDesc pShadowMapDesc, LightDesc pLightDesc, VolumeDesc pVolumeDesc);
    protected abstract Status renderVolume_DoVolume_Spotlight(int shadowMap, ShadowMapDesc pShadowMapDesc, LightDesc pLightDesc, VolumeDesc pVolumeDesc);
    protected abstract Status renderVolume_DoVolume_Omni(int shadowMap, ShadowMapDesc pShadowMapDesc, LightDesc pLightDesc, VolumeDesc pVolumeDesc);
    protected abstract Status renderVolume_End(int shadowMap, ShadowMapDesc pShadowMapDesc, LightDesc pLightDesc, VolumeDesc pVolumeDesc);

    // EndAccumulation
    protected abstract Status endAccumulation_Imp();

    // ApplyLighting
    protected abstract Status applyLighting_Start(int sceneTarget, int sceneDepth, PostprocessDesc pPostprocessDesc);
    protected abstract Status applyLighting_Resolve(PostprocessDesc pPostprocessDesc);
    protected abstract Status applyLighting_TemporalFilter(int sceneDepth, PostprocessDesc pPostprocessDesc);
    protected abstract Status applyLighting_Composite(int sceneTarget, int sceneDepth, PostprocessDesc pPostprocessDesc);
    protected abstract Status applyLighting_End(int sceneTarget, int sceneDepth, PostprocessDesc pPostprocessDesc);
    
    private static float haltonSequence(int index, int base){
    	float result = 0;
        float f = 1;
        int i = index + 1;
        while (i > 0)
        {
            f = f / base;
            result += f * (i % base);
            i = i / base;
        }
        return result;
    }
    
    //--------------------------------------------------------------------------
    // Helper functions
    protected void getJitter(Vector2f dest) {
    	if (getFilterMode() == FilterMode.TEMPORAL)
        {

//            return NvVec2((HaltonSequence(jitterIndex_, 2) - 0.5f), (HaltonSequence(jitterIndex_, 3) - 0.5f));
    		dest.set((haltonSequence(jitterIndex_, 2) - 0.5f), (haltonSequence(jitterIndex_, 3) - 0.5f));
        }
        else
        {
            dest.set(0, 0);
        }
    }
    protected int getOutputBufferWidth()  { return contextDesc_.uWidth;}
    protected int getOutputBufferHeight() { return contextDesc_.uHeight;}
    protected int getOutputViewportWidth() { return viewerDesc_.uViewportWidth;}
    protected int getOutputViewportHeight() { return viewerDesc_.uViewportHeight;}
    protected int getOutputSampleCount()  { return contextDesc_.uSamples;}
    protected float getInternalScale() { 
    	switch (contextDesc_.eDownsampleMode) {
    	default:
    	case FULL: return 1.00f;
		case HALF: return 0.5f;
		case QUARTER: return 0.25f;
		}
    }
    
    protected int getInternalBufferWidth(){
    	switch (contextDesc_.eDownsampleMode) {
    	default:
    	case FULL: return contextDesc_.uWidth;
		case HALF: return contextDesc_.uWidth >> 1;
		case QUARTER: return contextDesc_.uWidth >> 2;
		}
    }
    
    protected int getInternalBufferHeight() {
    	switch (contextDesc_.eDownsampleMode) {
    	default:
    	case FULL: return contextDesc_.uHeight;
		case HALF: return contextDesc_.uHeight >> 1;
		case QUARTER: return contextDesc_.uHeight >> 2;
		}
    }
    protected int getInternalViewportWidth(){
    	switch (contextDesc_.eDownsampleMode) {
    	default:
    	case FULL: return viewerDesc_.uViewportWidth;
		case HALF: return viewerDesc_.uViewportWidth >> 1;
		case QUARTER: return viewerDesc_.uViewportWidth >> 2;
		}
    }
    
    protected int getInternalViewportHeight() {
    	switch (contextDesc_.eDownsampleMode) {
    	default:
    	case FULL: return viewerDesc_.uViewportHeight;
		case HALF: return viewerDesc_.uViewportHeight >> 1;
		case QUARTER: return viewerDesc_.uViewportHeight >> 2;
		}
    }
    
    protected int getInternalSampleCount(){
    	switch (contextDesc_.eInternalSampleMode)
        {
        default:
        case SINGLE:
            return 1;

        case MSAA2:
            return 2;

        case MSAA4:
            return 4;
        }
    }
    
    protected boolean isOutputMSAA(){
    	return (getOutputSampleCount() > 1);
    }
    
    protected boolean isInternalMSAA() {
    	return (getInternalSampleCount() > 1);
    }
    
    protected FilterMode getFilterMode() {
    	return contextDesc_.eFilterMode;
    }
    
    protected int getCoarseResolution(VolumeDesc pVolumeDesc) {
    	switch (pVolumeDesc.eTessQuality)
        {
        default:
        case HIGH:   return pVolumeDesc.uMaxMeshResolution / 64;
        case MEDIUM: return pVolumeDesc.uMaxMeshResolution / 32;
        case LOW:    return pVolumeDesc.uMaxMeshResolution / 16;
        }
    }
    
    protected void setupCB_PerContext(PerContextCB cb){
    	cb.vOutputSize.x = getOutputBufferWidth();
    	cb.vOutputSize.y = getOutputBufferHeight();
    	cb.vOutputSize_Inv.x = 1.0f/cb.vOutputSize.x;
    	cb.vOutputSize_Inv.y = 1.0f/cb.vOutputSize.y;
    	
    	cb.vBufferSize.x = getInternalBufferWidth();
    	cb.vBufferSize.y = getInternalBufferHeight();
    	cb.vBufferSize_Inv.x = 1.0f/cb.vBufferSize.x;
    	cb.vBufferSize_Inv.y = 1.0f/cb.vBufferSize.y;
    	
    	cb.fResMultiplier = 1.0f/getInternalScale();
    	cb.uSampleCount = getInternalSampleCount();
    }
    
    protected void setupCB_PerFrame(ViewerDesc pViewerDesc, MediumDesc pMediumDesc, PerFrameCB cb){
    	cb.mProj.load(pViewerDesc.mProj);
    	cb.mViewProj.load(pViewerDesc.mViewProj);
    	Matrix4f.invert(cb.mViewProj, cb.mViewProj_Inv);
    	
    	cb.vOutputViewportSize.x = getOutputViewportWidth();
    	cb.vOutputViewportSize.y = getOutputViewportHeight();
    	cb.vOutputViewportSize_Inv.x = 1.0f/cb.vOutputViewportSize.x;
    	cb.vOutputViewportSize_Inv.y = 1.0f/cb.vOutputViewportSize.y;
    	
    	cb.vViewportSize.x = getInternalViewportWidth();
    	cb.vViewportSize.y = getInternalViewportHeight();
    	cb.vViewportSize_Inv.x = 1.0f/cb.vViewportSize.x;
    	cb.vViewportSize_Inv.y = 1.0f/cb.vViewportSize.y;
    	
    	cb.vEyePosition.set(pViewerDesc.vEyePosition);
    	getJitter(cb.vJitterOffset);
    	cb.fZNear = pViewerDesc.fZNear;
    	cb.fZFar  = pViewerDesc.fZFar;
    	
    	final float SCATTER_EPSILON = 0.000001f;
    	
    	float total_scatter_x = SCATTER_EPSILON;
    	float total_scatter_y = SCATTER_EPSILON;
    	float total_scatter_z = SCATTER_EPSILON;
    	cb.uNumPhaseTerms = pMediumDesc.uNumPhaseTerms;
    	for(int p = 0; p < pMediumDesc.uNumPhaseTerms; p++){
    		cb.uPhaseFunc[p][0] =  pMediumDesc.phaseTerms[p].ePhaseFunc.ordinal() - 1;
    		Vector3f density = pMediumDesc.phaseTerms[p].vDensity;
    		cb.vPhaseParams[p].set(density.x, density.y, density.z, pMediumDesc.phaseTerms[p].fEccentricity);
    		
    		total_scatter_x += density.x;
    		total_scatter_y += density.y;
    		total_scatter_z += density.z;
    	}
    	
    	Vector3f absorption = pMediumDesc.vAbsorption;
    	cb.vScatterPower.x = (float) (1-Math.exp(-total_scatter_x));
        cb.vScatterPower.y = (float) (1-Math.exp(-total_scatter_y));
        cb.vScatterPower.z = (float) (1-Math.exp(-total_scatter_z));
        cb.vSigmaExtinction.x = total_scatter_x + absorption.x;
        cb.vSigmaExtinction.y = total_scatter_x + absorption.y;
        cb.vSigmaExtinction.z = total_scatter_x + absorption.z;
    }
    
    protected void setupCB_PerVolume(ShadowMapDesc pShadowMapDesc, LightDesc pLightDesc, VolumeDesc pVolumeDesc, PerVolumeCB cb)
    {
        cb.mLightToWorld.load(pLightDesc.mLightToWorld);
        cb.vLightIntensity.set(pLightDesc.vIntensity);
        switch (pLightDesc.eType)
        {
        case DIRECTIONAL:
            cb.vLightDir.set(pLightDesc.vDirection);
            break;

        case SPOT:
            cb.vLightDir.set(pLightDesc.vDirection);
            cb.vLightPos.set(pLightDesc.vPosition);
            cb.fLightZNear = pLightDesc.fZNear;
            cb.fLightZFar = pLightDesc.fZFar;
            cb.fLightFalloffAngle = pLightDesc.fFalloff_CosTheta;
            cb.fLightFalloffPower = pLightDesc.fFalloff_Power;
//            cb.vAttenuationFactors = *reinterpret_cast<const NvVec4 *>(pLightDesc.fAttenuationFactors);
            cb.vAttenuationFactors.load(pLightDesc.fAttenuationFactors, 0);
            break;

        case POINT:
            cb.vLightPos.set(pLightDesc.vPosition);
            cb.fLightZNear = pLightDesc.fZNear;
            cb.fLightZFar = pLightDesc.fZFar;
//            cb.vAttenuationFactors = *reinterpret_cast<const NvVec4 *>(pLightDesc.fAttenuationFactors);
            cb.vAttenuationFactors.load(pLightDesc.fAttenuationFactors, 0);
        default:
            break;
        };
        cb.fDepthBias = pVolumeDesc.fDepthBias;

        cb.uMeshResolution = getCoarseResolution(pVolumeDesc);

//        NvVec4 vw1 = cb.mLightToWorld.transform(NvVec4(-1, -1,  1, 1));
//        NvVec4 vw2 = cb.mLightToWorld.transform(NvVec4( 1,  1,  1, 1));
        Vector4f vw1 = Matrix4f.transform(cb.mLightToWorld, new Vector4f(-1, -1, 1, 1), null);  // TODO Need Valide
        Vector4f vw2 = Matrix4f.transform(cb.mLightToWorld, new Vector4f( 1,  1, 1, 1), null);
//        vw1 = vw1 / vw1.w;
//        vw2 = vw2 / vw2.w;
        vw1.scale(1.0f/vw1.w);
        vw2.scale(1.0f/vw2.w);
//        float fCrossLength = ((vw1).getXYZ() - (vw2).getXYZ()).magnitude();
        float fCrossLength = Vector3f.distance(vw1, vw2);
        float fSideLength = (float) Math.sqrt(0.5f*fCrossLength*fCrossLength);
        cb.fGridSectionSize = fSideLength / cb.uMeshResolution;
        cb.fTargetRaySize = pVolumeDesc.fTargetRayResolution;

        for (int i=0;i<VLConstant.MAX_SHADOWMAP_ELEMENTS;++i)
        {
            cb.vElementOffsetAndScale[i].x = (float)pShadowMapDesc.elements[i].uOffsetX / pShadowMapDesc.uWidth;
            cb.vElementOffsetAndScale[i].y = (float)pShadowMapDesc.elements[i].uOffsetY / pShadowMapDesc.uHeight;
            cb.vElementOffsetAndScale[i].z = (float)pShadowMapDesc.elements[i].uWidth   / pShadowMapDesc.uWidth;
            cb.vElementOffsetAndScale[i].w = (float)pShadowMapDesc.elements[i].uHeight  / pShadowMapDesc.uHeight;
//            cb.mLightProj[i] = NVCtoNV(pShadowMapDesc.Elements[i].mViewProj);
            cb.mLightProj[i].load(pShadowMapDesc.elements[i].mViewProj);
//            cb.mLightProj_Inv[i] = Inverse(cb.mLightProj[i]);
            Matrix4f.invert(cb.mLightProj[i], cb.mLightProj_Inv[i]);
//            cb.uElementIndex[i][0] = pShadowMapDesc.Elements[i].mArrayIndex;
            cb.uElementIndex[i * 4] = pShadowMapDesc.elements[i].mArrayIndex;
        }

        cb.vShadowMapDim.x = pShadowMapDesc.uWidth;
        cb.vShadowMapDim.y = pShadowMapDesc.uHeight;
    }
    
    private final Matrix4f tmpVar = new Matrix4f();
    protected void setupCB_PerApply(PostprocessDesc pPostprocessDesc, PerApplyCB cb){
    	if (getFilterMode() == FilterMode.TEMPORAL)
        {
            cb.fHistoryFactor = (lastFrameIndex_ == -1) ? 0.0f : pPostprocessDesc.fTemporalFactor;
            cb.fFilterThreshold = pPostprocessDesc.fFilterThreshold;
            if (lastFrameIndex_ == -1)
            {
                lastViewProj_.load(pPostprocessDesc.mUnjitteredViewProj);
                lastFrameIndex_ = (nextFrameIndex_+1)%2;
            }
            else
            {
                lastViewProj_.load(nextViewProj_);
            }
            nextViewProj_.load(pPostprocessDesc.mUnjitteredViewProj);
//            cb.mHistoryXform = lastViewProj_ * Inverse(nextViewProj_);
            Matrix4f nextViewProj_Inv = Matrix4f.invert(nextViewProj_, tmpVar);
            Matrix4f.mul(lastViewProj_, nextViewProj_Inv, cb.mHistoryXform);
        }
        else
        {
            cb.mHistoryXform.setIdentity(); // = NvMat44(NvIdentity);
            cb.fFilterThreshold = 0.0f;
            cb.fHistoryFactor = 0.0f;
        }
        cb.vFogLight.set(pPostprocessDesc.vFogLight);
        cb.fMultiScattering = pPostprocessDesc.fMultiscatter;
    }

    //-------------------------- Helper function declared here for the subclass conversine using.-----------------------
    protected static Texture2D create(int width, int height, int samples, int format, String debug_name){
        Texture2DDesc desc = new Texture2DDesc();
        desc.width = width;
        desc.height = height;
        desc.sampleCount = samples;
        desc.format = format;
        desc.mipLevels = 1;
        desc.arraySize = 1;

        Texture2D result = TextureUtils.createTexture2D(desc, null);
        result.setName(debug_name);
        return result;
    }

    protected static void printProgram(OpenGLProgram program, String name){
        program.setName(name);
        program.printPrograminfo();
    }

    static void saveTextureAsText(TextureGL texture, String filename){
        try {
            DebugTools.saveTextureAsText(texture.getTarget(), texture.getTexture(), 0, "E:/textures/VolumetricLighting/" + filename);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    static void saveTextureAsText(int texture, String filename){
        try {
            DebugTools.saveTextureAsText(GLenum.GL_TEXTURE_2D, texture, 0, "E:/textures/VolumetricLighting/" + filename);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
