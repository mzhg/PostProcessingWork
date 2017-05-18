package jet.opengl.postprocessing.core.volumetricLighting;

final class SMiscDynamicParams {

	int ui4SrcMinMaxLevelXOffset;
    int ui4SrcMinMaxLevelYOffset;
    int ui4DstMinMaxLevelXOffset;
    int ui4DstMinMaxLevelYOffset;
    float fMaxStepsAlongRay;   // Maximum number of steps during ray tracing
//    float3 f3Dummy; // Constant buffers must be 16-byte aligned
}
