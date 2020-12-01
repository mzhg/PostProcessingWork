package jet.opengl.demos.amdfx.sssr;

import java.util.ArrayList;
import java.util.List;

import jet.opengl.postprocessing.shader.GLSLProgram;

/**
 The ContextGL class encapsulates the data for a single Vulkan stochastic screen space reflections execution context.
 */
public class ContextGL {

    // If the VK_EXT_subgroup_size_control extension is available.
    boolean is_subgroup_size_control_extension_available_;
    // The compiled reflections shaders.
    final GLSLProgram[] shaders_ = new GLSLProgram[kShader_Count];
    // The Blue Noise sampler optimized for 1 sample per pixel.
    BlueNoiseSamplerVK blue_noise_sampler_1spp_;
    // The Blue Noise sampler optimized for 2 samples per pixel.
    BlueNoiseSamplerVK blue_noise_sampler_2spp_;
    // The flag for whether the samplers were populated.
    boolean samplers_were_populated_;
    // The buffer to be used for uploading memory from the CPU to the GPU.
//    UploadBufferVK upload_buffer_;
    // The array of reflection views to be resolved.
    List<ReflectionViewGL> reflection_views_;

    // The shader pass that classifies tiles.
    ShaderPass tile_classification_pass_;
    // The shader pass that prepares the indirect arguments.
    ShaderPass indirect_args_pass_;
    // The shader pass intersecting reflection rays with the depth buffer.
    ShaderPass intersection_pass_;
    // The shader pass that does spatial denoising.
    ShaderPass spatial_denoising_pass_;
    // The shader pass that does temporal denoising.
    ShaderPass temporal_denoising_pass_;
    // The shader pass that does the second spatial denoising.
    ShaderPass eaw_denoising_pass_;

    public static final int
            kShader_IndirectArguments = 0,
            kShader_TileClassification = 1,
            kShader_Intersection = 2,
            kShader_SpatialResolve = 3,
            kShader_TemporalResolve = 4,
            kShader_EAWResolve = 5,

    kShader_Count = 6;

    static final int
        BINDING_TEXTURE = 0,
            BINDING_BUFFER = 1,
            BINDING_UNIROM = 2,
            BINDING_IMAGE = 3;

    final static class BindingResource{
        int index;
        int bindingType;
        Object resource;
        int texSampler;
    }

    final static class ShaderPass{
        final ArrayList<BindingResource> mBindingResources = new ArrayList<>();
        int shaderIndex;
    }
}
