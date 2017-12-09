package jet.opengl.demos.nvidia.hbaoplus;

final class RenderOptions {
	GFSDK_SSAO_DepthStorage depthStorage;
    GFSDK_SSAO_DepthClampMode depthClampMode;
    final GFSDK_SSAO_BlurParameters blur = new GFSDK_SSAO_BlurParameters();
    boolean enableForegroundAO;
    boolean enableBackgroundAO;
    boolean enableDepthThreshold;
    
    void setRenderOptions(GFSDK_SSAO_Parameters params){
    	depthStorage = params.depthStorage;
        depthClampMode = params.depthClampMode;
        blur.set(params.blur);
        enableForegroundAO = params.foregroundAO.enable;
        enableBackgroundAO = params.backgroundAO.enable;
        enableDepthThreshold = params.depthThreshold.enable;
    }
}
