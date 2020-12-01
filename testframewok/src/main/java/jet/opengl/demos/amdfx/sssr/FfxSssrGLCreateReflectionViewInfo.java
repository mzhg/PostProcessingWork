package jet.opengl.demos.amdfx.sssr;

import jet.opengl.postprocessing.texture.Texture2D;

/**
 The parameters for creating a Vulkan reflection view.
 */
public class FfxSssrGLCreateReflectionViewInfo {
    /** The format of the sceneSRV to allow creating matching internal resources. */
    public int sceneFormat;
    /** The rendered scene without reflections.*/
    public Texture2D sceneSRV;
    public Texture2D depthBufferHierarchySRV; ///< Full downsampled depth buffer. Each lower detail mip containing the minimum values of the higher detailed mip.
    public Texture2D motionBufferSRV; ///< The per pixel motion vectors.
    public Texture2D normalBufferSRV; ///< The surface normals in world space. Each channel mapped to [0, 1].
    public Texture2D roughnessBufferSRV; ///< Perceptual roughness squared per pixel.
    public Texture2D normalHistoryBufferSRV; ///< Last frames normalBufferSRV.
    public Texture2D roughnessHistoryBufferSRV; ///< Last frames roughnessHistoryBufferSRV.
    public Texture2D environmentMapSampler; ///< Environment map sampler used when looking up the fallback for ray misses.
    public Texture2D environmentMapSRV; ///< Environment map serving as a fallback for ray misses.
    public Texture2D reflectionViewUAV; ///< The fully resolved reflection view. Make sure to synchronize for UAV writes.
//    VkCommandBuffer uploadCommandBuffer; ///< Vulkan command buffer to upload static resources. The application has to begin the command buffer and has to handle synchronization to make sure the uploads are done.
}
