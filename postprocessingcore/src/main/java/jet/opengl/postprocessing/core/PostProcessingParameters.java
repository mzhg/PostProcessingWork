package jet.opengl.postprocessing.core;

import jet.opengl.postprocessing.common.GLAPI;
import jet.opengl.postprocessing.common.GLFuncProviderFactory;

/**
 * Created by mazhen'gui on 2017/4/17.
 */

public class PostProcessingParameters {
    float radialBlurCenterX = 0.5f;
    float radialBlurCenterY = 0.5f;
    int   radialBlurSamples = 24;
    float gloablTime;

    float bloomIntensity;
    float edgeThreshold;
    float edgeThreshold2;
    float fishEyeFactor;

    float bloomThreshold;
    float exposureScale;

    PostProcessing postProcessing;

    PostProcessingParameters(PostProcessing postProcessing){
        this.postProcessing = postProcessing;
        if(GLFuncProviderFactory.getGLFuncProvider().getHostAPI() == GLAPI.ANDROID){
            radialBlurSamples = 12;
        }
    }

    public float getRadialBlurCenterX() {return radialBlurCenterX;}
    public float getRadialBlurCenterY() {return radialBlurCenterY;}
    public int getRadialBlurSamples() {return radialBlurSamples;}
    public float getGlobalTime() {return gloablTime;}
    public float getBloomIntensity() {return bloomIntensity;}

    public float getEdgeThreshold() {return edgeThreshold;}
    public float getEdgeThreshold2() {return edgeThreshold2;}
    public float getFishEyeFactor()  { return fishEyeFactor;}

    public float getBloomThreshold()	{ return bloomThreshold; }
    public float getExposureScale() { return exposureScale; }
}
