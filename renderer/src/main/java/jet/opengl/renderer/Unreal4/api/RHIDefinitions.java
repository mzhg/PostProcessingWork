package jet.opengl.renderer.Unreal4.api;

public interface RHIDefinitions {
    /** Maximum number of miplevels in a texture. */
    int MAX_TEXTURE_MIP_COUNT = 14;
    /** The maximum number of vertex elements which can be used by a vertex declaration. */
    int MaxVertexElementCount = 16;

    /** The alignment in bytes between elements of array shader parameters. */
    int ShaderArrayElementAlignBytes = 16;

    /** The number of render-targets that may be simultaneously written to. */
    int MaxSimultaneousRenderTargets = 8;

    /** The number of UAVs that may be simultaneously bound to a shader. */
    int MaxSimultaneousUAVs = 8;

    /** Whether the shader platform corresponds to the ES2 feature level. */
    static boolean IsES2Platform(EShaderPlatform Platform)
    {
        return Platform == EShaderPlatform.SP_PCD3D_ES2 || Platform == EShaderPlatform.SP_OPENGL_PCES2 || Platform == EShaderPlatform.SP_OPENGL_ES2_ANDROID || Platform == EShaderPlatform.SP_OPENGL_ES2_WEBGL || Platform == EShaderPlatform.SP_OPENGL_ES2_IOS || Platform == EShaderPlatform.SP_METAL_MACES2;
    }

    /** Whether the shader platform corresponds to the ES2/ES3.1 feature level. */
    static boolean IsMobilePlatform(EShaderPlatform Platform)
    {
        return IsES2Platform(Platform)
                || Platform == EShaderPlatform.SP_METAL || Platform == EShaderPlatform.SP_PCD3D_ES3_1 || Platform == EShaderPlatform.SP_OPENGL_PCES3_1 || Platform == EShaderPlatform.SP_VULKAN_ES3_1_ANDROID
                || Platform == EShaderPlatform.SP_VULKAN_PCES3_1 || Platform == EShaderPlatform.SP_METAL_MACES3_1 || Platform == EShaderPlatform.SP_OPENGL_ES3_1_ANDROID || Platform == EShaderPlatform.SP_WOLF_FORWARD;
    }
}
