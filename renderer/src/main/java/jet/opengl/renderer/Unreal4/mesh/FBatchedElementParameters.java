package jet.opengl.renderer.Unreal4.mesh;

import org.lwjgl.util.vector.Matrix4f;

import jet.opengl.postprocessing.texture.TextureGL;

/** Custom parameters for batched element shaders.  Derive from this class to implement your shader bindings. */
public interface FBatchedElementParameters {
    /** Binds vertex and pixel shaders for this element */
    void BindShaders(/*FRHICommandList& RHICmdList, FGraphicsPipelineStateInitializer& GraphicsPSOInit,*/int InFeatureLevel, Matrix4f InTransform,
                                 float InGamma, Matrix4f ColorWeights, TextureGL Texture);

}
