package jet.opengl.demos.nvidia.waves;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector2f;
import org.lwjgl.util.vector.Vector4f;

import jet.opengl.postprocessing.common.GLAPI;
import jet.opengl.postprocessing.common.GLFuncProvider;
import jet.opengl.postprocessing.common.GLFuncProviderFactory;
import jet.opengl.postprocessing.texture.SamplerUtils;
import jet.opengl.postprocessing.util.LogUtil;

/**
 * Created by Administrator on 2017/7/23 0023.
 */

public final class GFSDK_WaveWorks {

    private static final GFSDK_WaveWorks_API_GUID GFSDK_WAVEWORKS_API_GUID = new GFSDK_WaveWorks_API_GUID(0x40C55E2B, 0x2AE34be0, 0xA4849F24, 0x585EB898);
    private static nv_water_d3d_api g_InitialisedAPI= nv_water_d3d_api.nv_water_d3d_api_undefined;
    private static boolean g_CanUseCUDA=false;

    static final float kCascadeScale = 5.23f; // Cascade - to - cascade ratio should be not integer, so repeats are less visible
    static final float kLODCascadeMaxWaveNumber = kCascadeScale * 10.f;
    static final int kLODCascadeResolution = 256; // Chosen to satisfy: kLODCascadeResolution >= (4*kLODCascadeMaxWaveNumber), for reasons of symmetry and Nyquist
    static final int MaxNumGPUs=4;
    /** Flags used to specify what state to preserve during rendering */
    public static final int
    GFSDK_WaveWorks_StatePreserve_None = 0,
    GFSDK_WaveWorks_StatePreserve_Shaders = 1,
    GFSDK_WaveWorks_StatePreserve_ShaderConstants = 2,
    GFSDK_WaveWorks_StatePreserve_Samplers = 4,			// Includes textures/shader-resources
    GFSDK_WaveWorks_StatePreserve_RenderTargets = 8,
    GFSDK_WaveWorks_StatePreserve_Viewports = 16,
    GFSDK_WaveWorks_StatePreserve_Streams = 32,			// Includes vertex/index-buffers, decls/input-layouts
    GFSDK_WaveWorks_StatePreserve_UnorderedAccessViews = 64,
    GFSDK_WaveWorks_StatePreserve_Other = 128,
    GFSDK_WaveWorks_StatePreserve_All = 0xFFFFFFFF,
    GFSDK_WaveWorks_StatePreserve_ForceDWORD = 0xFFFFFFFF;

    // Beaufort presets for water
    // Wave amplitude scaler in meters
	final static float BeaufortAmplitude[/*13*/] = {
        0.7f, // for Beaufort scale value 0
                0.7f, // for Beaufort scale value 1
                0.7f, // for Beaufort scale value 2
                0.7f, // for Beaufort scale value 3
                0.7f, // for Beaufort scale value 4
                0.7f, // for Beaufort scale value 5
                0.7f, // for Beaufort scale value 6
                0.7f, // for Beaufort scale value 7
                0.7f, // for Beaufort scale value 8
                0.7f, // for Beaufort scale value 9
                0.7f, // for Beaufort scale value 10
                0.7f, // for Beaufort scale value 11
                0.7f  // for Beaufort scale value 12 and above
    };
    // Wind speed in meters per second
    final static float BeaufortWindSpeed[/*13*/] = {
        0.0f, // for Beaufort scale value 0
                0.6f, // for Beaufort scale value 1
                2.0f, // for Beaufort scale value 2
                3.0f, // for Beaufort scale value 3
                6.0f, // for Beaufort scale value 4
                8.1f, // for Beaufort scale value 5
                10.8f,// for Beaufort scale value 6
                13.9f,// for Beaufort scale value 7
                17.2f,// for Beaufort scale value 8
                20.8f,// for Beaufort scale value 9
                24.7f,// for Beaufort scale value 10
                28.6f,// for Beaufort scale value 11
                32.8f // for Beaufort scale value 12 and above
    };
    // Choppy scale factor (unitless)
    final static float BeaufortChoppiness[/*13*/] = {
        1.0f, // for Beaufort scale value 0
                1.0f, // for Beaufort scale value 1
                1.0f, // for Beaufort scale value 2
                1.0f, // for Beaufort scale value 3
                1.0f, // for Beaufort scale value 4
                1.0f, // for Beaufort scale value 5
                1.0f, // for Beaufort scale value 6
                1.0f, // for Beaufort scale value 7
                1.0f, // for Beaufort scale value 8
                1.0f, // for Beaufort scale value 9
                1.0f, // for Beaufort scale value 10
                1.0f, // for Beaufort scale value 11
                1.0f  // for Beaufort scale value 12 and above
    };

    // Foam generation threshold (unitless)
    final static float BeaufortFoamGenerationThreshold[/*13*/] = {
        0.3f, // for Beaufort scale value 0
                0.3f, // for Beaufort scale value 1
                0.3f, // for Beaufort scale value 2
                0.3f, // for Beaufort scale value 3
                0.24f,// for Beaufort scale value 4
                0.27f,// for Beaufort scale value 5
                0.27f, // for Beaufort scale value 6
                0.30f, // for Beaufort scale value 7
                0.30f, // for Beaufort scale value 8
                0.30f, // for Beaufort scale value 9
                0.30f, // for Beaufort scale value 10
                0.30f, // for Beaufort scale value 11
                0.30f  // for Beaufort scale value 12 and above
    };

    // Foam generation amount (unitless)
    final static float BeaufortFoamGenerationAmount[/*13*/] = {
        0.0f, // for Beaufort scale value 0
                0.0f, // for Beaufort scale value 1
                0.0f, // for Beaufort scale value 2
                0.0f, // for Beaufort scale value 3
                0.13f,// for Beaufort scale value 4
                0.13f,// for Beaufort scale value 5
                0.13f,// for Beaufort scale value 6
                0.13f,// for Beaufort scale value 7
                0.13f,// for Beaufort scale value 8
                0.13f,// for Beaufort scale value 9
                0.13f,// for Beaufort scale value 10
                0.13f,// for Beaufort scale value 11
                0.13f // for Beaufort scale value 12 and above
    };

    // Foam dissipation speed (unitless)
    final static float BeaufortFoamDissipationSpeed[/*13*/] = {
        1.0f, // for Beaufort scale value 0
                1.0f, // for Beaufort scale value 1
                1.0f, // for Beaufort scale value 2
                0.8f, // for Beaufort scale value 3
                0.7f,// for Beaufort scale value 4
                0.6f,// for Beaufort scale value 5
                0.6f,// for Beaufort scale value 6
                0.6f,// for Beaufort scale value 7
                0.7f,// for Beaufort scale value 8
                0.8f,// for Beaufort scale value 9
                0.9f,// for Beaufort scale value 10
                1.0f,// for Beaufort scale value 11
                1.1f // for Beaufort scale value 12 and above
    };

    // Foam falloff speed (unitless)
    final static float BeaufortFoamFalloffSpeed[/*13*/] = {
        0.985f, // for Beaufort scale value 0
                0.985f, // for Beaufort scale value 1
                0.985f, // for Beaufort scale value 2
                0.985f, // for Beaufort scale value 3
                0.985f, // for Beaufort scale value 4
                0.985f, // for Beaufort scale value 5
                0.985f, // for Beaufort scale value 6
                0.988f, // for Beaufort scale value 7
                0.985f, // for Beaufort scale value 8
                0.985f, // for Beaufort scale value 9
                0.986f, // for Beaufort scale value 10
                0.988f, // for Beaufort scale value 11
                0.988f  // for Beaufort scale value 12 and above
    };

    private GFSDK_WaveWorks(){
        throw new IllegalStateException();
    }
    /*===========================================================================
      Globals/init
      ===========================================================================*/

    public static String GFSDK_WaveWorks_GetBuildString(){
        GLFuncProvider gl= GLFuncProviderFactory.getGLFuncProvider();
        if(gl.getHostAPI()== GLAPI.ANDROID){
            return "ANDROID_TEST";
        }else if(gl.getHostAPI()==GLAPI.JOGL){
            return "JOGL_TEST";
        }else{
            return "LWJGL_TEST";
        }
    }

//    public static GFSDK_WaveWorks_Result GFSDK_WaveWorks_SetUserLogger(nv::ILogger* userLogger);
//
//
//    // Use these calls to globally initialize/release on D3D device create/destroy.
    public static GFSDK_WaveWorks_Result GFSDK_WaveWorks_InitNoGraphics(/*const GFSDK_WaveWorks_Malloc_Hooks* pOptionalMallocHooks, const */GFSDK_WaveWorks_API_GUID apiGUID){
        if(g_InitialisedAPI == nv_water_d3d_api.nv_water_d3d_api_undefined) {
            LogUtil.e(LogUtil.LogType.DEFAULT, "ERROR: 'GFSDK_WaveWorks_InitNoGraphics' was called but the library was not initialised");
            return GFSDK_WaveWorks_Result.FAIL;
        }


        if(!apiGUID.equals(GFSDK_WAVEWORKS_API_GUID)) {
            LogUtil.e(LogUtil.LogType.DEFAULT, "ERROR: 'GFSDK_WaveWorks_InitNoGraphics' was called with an invalid API GUID");
            return GFSDK_WaveWorks_Result.FAIL;
        }

//        if(pOptionalMallocHooks) {
//		const gfsdk_waveworks_result smmcResult = SetMemoryManagementCallbacks(*pOptionalMallocHooks);
//            if(smmcResult != gfsdk_waveworks_result_OK)
//                return smmcResult;
//        }
        g_InitialisedAPI = nv_water_d3d_api.nv_water_d3d_api_none;
        return GFSDK_WaveWorks_Result.OK;
    }

    public static GFSDK_WaveWorks_Result GFSDK_WaveWorks_ReleaseNoGraphics(){
        if(g_InitialisedAPI != nv_water_d3d_api.nv_water_d3d_api_none) {
            LogUtil.e(LogUtil.LogType.DEFAULT, "ERROR: 'GFSDK_WaveWorks_ReleaseNoGraphics' was called but the library was not initialised for no graphics");
            return GFSDK_WaveWorks_Result.FAIL;
        }

//        resetMemoryManagementCallbacksToDefaults();

        g_InitialisedAPI = nv_water_d3d_api.nv_water_d3d_api_undefined;
        return GFSDK_WaveWorks_Result.OK;
    }

    public static GFSDK_WaveWorks_Result GFSDK_WaveWorks_InitD3D11(/*ID3D11Device* pD3DDevice, const GFSDK_WaveWorks_Malloc_Hooks* pOptionalMallocHooks, const*/ GFSDK_WaveWorks_API_GUID apiGUID){
        LogUtil.i(LogUtil.LogType.DEFAULT, "Initing D3D11");

        if(g_InitialisedAPI != nv_water_d3d_api.nv_water_d3d_api_undefined) {
            LogUtil.e(LogUtil.LogType.DEFAULT, "ERROR: 'GFSDK_WaveWorks_InitD3D11' was called with the library already in an initialised state");
            return GFSDK_WaveWorks_Result.FAIL;
        }

        if(!apiGUID.equals(GFSDK_WAVEWORKS_API_GUID)) {
            LogUtil.e(LogUtil.LogType.DEFAULT, "ERROR: 'GFSDK_WaveWorks_InitD3D11' was called with an invalid API GUID");
            return GFSDK_WaveWorks_Result.FAIL;
        }

//        if(pRequiredMallocHooks) {
//		const gfsdk_waveworks_result smmcResult = SetMemoryManagementCallbacks(*pRequiredMallocHooks);
//            if(smmcResult != gfsdk_waveworks_result_OK)
//                return smmcResult;
//        }

        g_InitialisedAPI = nv_water_d3d_api.nv_water_d3d_api_d3d11;
        return GFSDK_WaveWorks_Result.OK;
    }

    public static GFSDK_WaveWorks_Result GFSDK_WaveWorks_ReleaseD3D11(){
        if(g_InitialisedAPI != nv_water_d3d_api.nv_water_d3d_api_d3d11) {
            LogUtil.e(LogUtil.LogType.DEFAULT, "ERROR: 'GFSDK_WaveWorks_ReleaseD3D11' was called but the library was not initialised for d3d11");
            return GFSDK_WaveWorks_Result.FAIL;
        }

//        resetMemoryManagementCallbacksToDefaults();

        g_InitialisedAPI = nv_water_d3d_api.nv_water_d3d_api_undefined;
        return GFSDK_WaveWorks_Result.OK;
    }

    public static GFSDK_WaveWorks_Result GFSDK_WaveWorks_InitGL2(/*const GFSDK_WAVEWORKS_GLFunctions* pGLFuncs, const GFSDK_WaveWorks_Malloc_Hooks* pOptionalMallocHooks, const*/ GFSDK_WaveWorks_API_GUID apiGUID){
        LogUtil.i(LogUtil.LogType.DEFAULT, "Initing GL2");

        if(g_InitialisedAPI != nv_water_d3d_api.nv_water_d3d_api_undefined) {
            LogUtil.e(LogUtil.LogType.DEFAULT, "ERROR: 'GFSDK_WaveWorks_InitGL2' was called with the library already in an initialised state");
            return GFSDK_WaveWorks_Result.FAIL;
        }

        if(!apiGUID.equals(GFSDK_WAVEWORKS_API_GUID)) {
            LogUtil.e(LogUtil.LogType.DEFAULT, "ERROR: 'GFSDK_WaveWorks_InitGL2' was called with an invalid API GUID");
            return GFSDK_WaveWorks_Result.FAIL;
        }

//        if(pRequiredMallocHooks) {
//		const gfsdk_waveworks_result smmcResult = SetMemoryManagementCallbacks(*pRequiredMallocHooks);
//            if(smmcResult != gfsdk_waveworks_result_OK)
//                return smmcResult;
//        }

        g_InitialisedAPI = nv_water_d3d_api.nv_water_d3d_api_gl2;
        return GFSDK_WaveWorks_Result.OK;
    }

    public static GFSDK_WaveWorks_Result GFSDK_WaveWorks_ReleaseGL2(){
        if(g_InitialisedAPI != nv_water_d3d_api.nv_water_d3d_api_gl2) {
            LogUtil.e(LogUtil.LogType.DEFAULT, "ERROR: 'GFSDK_WaveWorks_ReleaseGL2' was called but the library was not initialised for GL2");
            return GFSDK_WaveWorks_Result.FAIL;
        }

//        resetMemoryManagementCallbacksToDefaults();

        g_InitialisedAPI = nv_water_d3d_api.nv_water_d3d_api_undefined;
        return GFSDK_WaveWorks_Result.OK;
    }
//
    /** Utility function to test whether a given GL attrib is a match for a given shader input, based on name */
    public static boolean GFSDK_WaveWorks_GLAttribIsShaderInput(String attribName, GFSDK_WaveWorks_ShaderInput_Desc inputDesc){
        // We have a match if the input desc name is the end fo the attrib name string
        // This is because we would like to support clients who embed the vertex attributes in their own GLSL structs, so any of
        // the following is considered a match for an attrib input named 'foo'...
        //    foo
        //    waveworks_struct.foo
        //    client_struct.foo
        //    client_struct.waveworks_struct.foo
        // ...etc, etc
        final int inputNameLen = inputDesc.Name.length();
        final int attribNameLen = attribName.length();
        if(attribNameLen < inputNameLen)
        {
            // Can't possibly match
            return false;
        }

    //        return 0 == strcmp(attribName + (attribNameLen - inputNameLen), inputDesc.Name);
        return attribName.substring(attribNameLen - inputNameLen).equals(inputDesc.Name);
    }

    static int ToAPI(GFSDK_WaveWorks_Simulation_DetailLevel dl)
    {
        switch(dl)
        {
            case Normal:
//#if defined(SUPPORT_FFTCPU)
//                return nv_water_simulation_api_cpu;
//#else
            return nv_water_simulation_api.nv_water_simulation_api_gpu_preferred;
//#endif
            case High: return nv_water_simulation_api.nv_water_simulation_api_gpu_preferred;
            case Extreme: return nv_water_simulation_api.nv_water_simulation_api_gpu_preferred;
            default: return nv_water_simulation_api.nv_water_simulation_api_gpu_preferred;
        }
    }
//
//
///*===========================================================================
//  Save/restore of graphics device state
//  ===========================================================================*/
//
//    // In order to preserve D3D state across certain calls, create a save-state object, pass it to the call
//// and then once the call is done, use it to restore the previous D3D state
//    public static GFSDK_WaveWorks_Result GFSDK_WaveWorks_Savestate_CreateD3D11(GFSDK_WaveWorks_StatePreserveFlags PreserveFlags, ID3D11Device* pD3DDevice, GFSDK_WaveWorks_SavestateHandle* pResult);
//    public static GFSDK_WaveWorks_Result GFSDK_WaveWorks_Savestate_RestoreD3D11(GFSDK_WaveWorks_SavestateHandle hSavestate, ID3D11DeviceContext* pDC);
//    public static GFSDK_WaveWorks_Result GFSDK_WaveWorks_Savestate_Destroy(GFSDK_WaveWorks_SavestateHandle hSavestate);

    // These functions can be used to check whether a particular graphics device supports a particular detail level,
// *before* initialising the graphics device
    public static boolean GFSDK_WaveWorks_Simulation_DetailLevelIsSupported_NoGraphics(GFSDK_WaveWorks_Simulation_DetailLevel detailLevel){
        final int simulationAPI = ToAPI(detailLevel);
        switch(simulationAPI) {
            case nv_water_simulation_api.nv_water_simulation_api_cuda:
            {
//			#ifdef SUPPORT_CUDA
//                int cuda_device;
//                cudaError cu_err = cudaGetDevice(&cuda_device);
//                if (cu_err != cudaSuccess)
//                    return false;
//                else
//                    return cudaDeviceSupportsDoublePrecision(cuda_device);
//			#else
                return false;
//			#endif
            }
            case nv_water_simulation_api.nv_water_simulation_api_cpu:
            {
//			#ifdef SUPPORT_FFTCPU
//                return true;
//			#else
                return false;
//			#endif
            }
            default:
                return false;
        }
    }
    public static boolean GFSDK_WaveWorks_Simulation_DetailLevelIsSupported_D3D11(/*IDXGIAdapter* adapter,*/ GFSDK_WaveWorks_Simulation_DetailLevel detailLevel){

	    final int simulationAPI = ToAPI(detailLevel);
        switch(simulationAPI) {
            case nv_water_simulation_api.nv_water_simulation_api_cuda:
            {
//			#ifdef SUPPORT_CUDA
//                int device;
//                cudaD3D11GetDevice(&device, adapter);
//                if (cudaGetLastError() != cudaSuccess)
//                    return false;
//                else
//                    return cudaDeviceSupportsDoublePrecision(device);
//			#else
                return false;
//			#endif
            }
            case nv_water_simulation_api.nv_water_simulation_api_direct_compute:
            {
//#ifdef SUPPORT_DIRECTCOMPUTE
                // todo: check D3D11 support
                return true;
//#else
//                return false;
//#endif
            }
            case nv_water_simulation_api.nv_water_simulation_api_cpu:
            {
//			#ifdef SUPPORT_FFTCPU
//                return true;
//			#else
                return false;
//			#endif
            }
            default:
                return false;
        }
    }

    public static boolean GFSDK_WaveWorks_Simulation_DetailLevelIsSupported_GL2(GFSDK_WaveWorks_Simulation_DetailLevel detailLevel){
        final int simulationAPI = ToAPI(detailLevel);
        switch(simulationAPI) {
            case nv_water_simulation_api.nv_water_simulation_api_cuda:
            {
//			#ifdef SUPPORT_CUDA
//                unsigned int num_devices;
//                int cuda_device;
//                cudaError cu_err = cudaGLGetDevices(&num_devices,&cuda_device,1,cudaGLDeviceListCurrentFrame);
//                if (cu_err != cudaSuccess)
//                    return false;
//                else
//                    return cudaDeviceSupportsDoublePrecision(cuda_device);
//			#else
                return false;
//			#endif
            }
            case nv_water_simulation_api.nv_water_simulation_api_cpu:
            {
//			#ifdef SUPPORT_FFTCPU
//                return true;
//			#else
                return false;
//			#endif
            }
            default:
                return false;
        }
    }

    static GFSDK_WaveWorks_Result CheckDetailLevelSupport(GFSDK_WaveWorks_Simulation_DetailLevel dl, String entrypointFnName)
    {
		final int simulationAPI = ToAPI(dl);
        switch(simulationAPI) {
            case nv_water_simulation_api.nv_water_simulation_api_cuda:
            {
//				#ifdef SUPPORT_CUDA
//
//                if(g_CanUseCUDA)
//                    break;			// We detected CUDA, keep going
//
//                NV_ERROR(TEXT("ERROR: %s failed because the hardware does not support the detail_level specified in the simulation settings\n"), szEntrypointFnName);
//                return gfsdk_waveworks_result_FAIL;
//
//				#else
                return GFSDK_WaveWorks_Result.FAIL;
//				#endif
            }
            case nv_water_simulation_api.nv_water_simulation_api_cpu:
            {
//				#ifdef SUPPORT_FFTCPU
//                break;
//				#else
                return GFSDK_WaveWorks_Result.FAIL;
//				#endif
            }
            case nv_water_simulation_api.nv_water_simulation_api_direct_compute:
            {
//				#ifdef SUPPORT_DIRECTCOMPUTE
                break;
//				#else
//                return GFSDK_WaveWorks_Result.FAIL;
//				#endif
            }
            default:
                return GFSDK_WaveWorks_Result.FAIL;
        }

        return GFSDK_WaveWorks_Result.OK;
    }

    // Convenience functions for resolving detail levels
    private static int ToInt(GFSDK_WaveWorks_Simulation_DetailLevel dl)
    {
        switch(dl)
        {
            case Normal: return Simulation_Util.MAX_FFT_RESOLUTION/4;
            case High: return Simulation_Util.MAX_FFT_RESOLUTION/2;
            case Extreme: return Simulation_Util.MAX_FFT_RESOLUTION;
            default: return Simulation_Util.MAX_FFT_RESOLUTION;
        }
    }

    private static void Init_Detailed_Water_Simulation_Params(GFSDK_WaveWorks_Simulation_Settings global_settings, GFSDK_WaveWorks_Simulation_Params global_params, GFSDK_WaveWorks_Detailed_Simulation_Params detailed_params)
    {
        int BeaufortInteger=(int)(Math.floor(global_params.wind_speed));
        float BeaufortFractional = (float) (global_params.wind_speed - Math.floor(global_params.wind_speed));
	    final int fft_resolution = ToInt(global_settings.detail_level);

        // Clamping GPU count to 1..4 range internally
        int num_GPUs = global_settings.num_GPUs;
        if(num_GPUs < 1) num_GPUs = 1;
        if(num_GPUs > MaxNumGPUs) num_GPUs = MaxNumGPUs;

        // doing piece-wise linear interpolation between predefined values
        // and extrapolating last linear segment to higher Beaufort values
        if(BeaufortInteger>11)
        {
            BeaufortInteger=11;
            BeaufortFractional = global_params.wind_speed - 11;
        }

        detailed_params.num_cascades					= GFSDK_WaveWorks_Detailed_Simulation_Params.MaxNumCascades;
        detailed_params.aniso_level					    = Math.max(1,Math.min(16,global_settings.aniso_level));
        detailed_params.simulation_api					= ToAPI(global_settings.detail_level);
        detailed_params.CPU_simulation_threading_model	= global_settings.CPU_simulation_threading_model;
        detailed_params.time_scale						= global_params.time_scale;
        detailed_params.num_GPUs						= num_GPUs;
        detailed_params.use_texture_arrays				= global_settings.use_texture_arrays;
        detailed_params.enable_gfx_timers				= global_settings.enable_gfx_timers;
        detailed_params.enable_CPU_timers				= global_settings.enable_CPU_timers;

        for(int i=0;i<GFSDK_WaveWorks_Detailed_Simulation_Params.MaxNumCascades;i++)
        {
		    final boolean is_most_detailed_cascade_level = (i < (GFSDK_WaveWorks_Detailed_Simulation_Params.MaxNumCascades-1)) ? false : true;

            detailed_params.cascades[i].fft_period						= (float) (global_settings.fft_period / Math.pow(kCascadeScale,(float)i));
            detailed_params.cascades[i].readback_displacements			= global_settings.readback_displacements;
            detailed_params.cascades[i].num_readback_FIFO_entries		= global_settings.num_readback_FIFO_entries;
            detailed_params.cascades[i].fft_resolution					= is_most_detailed_cascade_level ? fft_resolution : Math.min(fft_resolution,kLODCascadeResolution);
            detailed_params.cascades[i].small_wave_fraction			    = global_params.small_wave_fraction;
            detailed_params.cascades[i].time_scale						= 1.0f;
            detailed_params.cascades[i].wind_dir						.set(global_params.wind_dir);
            detailed_params.cascades[i].wind_dependency				    = global_params.wind_dependency;
            detailed_params.cascades[i].enable_CUDA_timers				= global_settings.enable_CUDA_timers;

            if(global_settings.use_Beaufort_scale)
            {
                // doing piece-wise linear interpolation between values predefined by Beaufort scale
                detailed_params.cascades[i].choppy_scale					= BeaufortChoppiness[BeaufortInteger] + BeaufortFractional*(BeaufortChoppiness[BeaufortInteger + 1] - BeaufortChoppiness[BeaufortInteger]);
                detailed_params.cascades[i].wave_amplitude					= BeaufortAmplitude[BeaufortInteger] + BeaufortFractional*(BeaufortAmplitude[BeaufortInteger + 1] - BeaufortAmplitude[BeaufortInteger]);
                detailed_params.cascades[i].wind_speed						= BeaufortWindSpeed[BeaufortInteger] + BeaufortFractional*(BeaufortWindSpeed[BeaufortInteger + 1] - BeaufortWindSpeed[BeaufortInteger]);
                detailed_params.cascades[i].foam_generation_threshold		= BeaufortFoamGenerationThreshold[BeaufortInteger] + BeaufortFractional*(BeaufortFoamGenerationThreshold[BeaufortInteger + 1] - BeaufortFoamGenerationThreshold[BeaufortInteger]);
                detailed_params.cascades[i].foam_generation_amount			= BeaufortFoamGenerationAmount[BeaufortInteger] + BeaufortFractional*(BeaufortFoamGenerationAmount[BeaufortInteger + 1] - BeaufortFoamGenerationAmount[BeaufortInteger]);
                detailed_params.cascades[i].foam_dissipation_speed			= BeaufortFoamDissipationSpeed[BeaufortInteger] + BeaufortFractional*(BeaufortFoamDissipationSpeed[BeaufortInteger + 1] - BeaufortFoamDissipationSpeed[BeaufortInteger]);
                detailed_params.cascades[i].foam_falloff_speed				= BeaufortFoamFalloffSpeed[BeaufortInteger] + BeaufortFractional*(BeaufortFoamFalloffSpeed[BeaufortInteger + 1] - BeaufortFoamFalloffSpeed[BeaufortInteger]);

            }
            else
            {
                // using values defined in global params
                detailed_params.cascades[i].choppy_scale					= global_params.choppy_scale;
                detailed_params.cascades[i].wave_amplitude					= global_params.wave_amplitude;
                detailed_params.cascades[i].wind_speed						= global_params.wind_speed;
                detailed_params.cascades[i].foam_generation_threshold		= global_params.foam_generation_threshold;
                detailed_params.cascades[i].foam_generation_amount			= global_params.foam_generation_amount;
                detailed_params.cascades[i].foam_dissipation_speed			= global_params.foam_dissipation_speed;
                detailed_params.cascades[i].foam_falloff_speed				= global_params.foam_falloff_speed;
            }

            // Windowing params to ensure we do not overlap wavelengths in different cascade levels
            if(is_most_detailed_cascade_level)
            {
                // Allow all high frequencies in most detailed level
                detailed_params.cascades[i].window_out = (float)(detailed_params.cascades[i].fft_resolution);
            }
            else
            {
                detailed_params.cascades[i].window_out = kLODCascadeMaxWaveNumber;
            }

            if(i > 0)
            {
                // Match the 'in' on this cascade to the 'out' on the previous
                detailed_params.cascades[i].window_in = detailed_params.cascades[i-1].window_out * detailed_params.cascades[i].fft_period/detailed_params.cascades[i-1].fft_period;
            }
            else
            {
                // This is the biggest cascade in world space, so we cover all the frequencies at the low end
                detailed_params.cascades[i].window_in= 0.f;
            }

        }
    }

    private static GFSDK_WaveWorks_Simulation  Simulation_CreateD3D11_Generic(   GFSDK_WaveWorks_Simulation_Settings global_settings,
															 GFSDK_WaveWorks_Simulation_Params global_params/*,
                                                            GFSDK_WaveWorks_CPU_Scheduler_Interface* pOptionalScheduler,
                                                            ID3D11Device* pD3DDevice,
                                                            GFSDK_WaveWorks_Simulation* pResult*/
    )
    {
//		#if WAVEWORKS_ENABLE_D3D11
        // Don't assume the user checked GFSDK_WaveWorks_Simulation_DetailLevelIsSupported_XXXX()...
        if(GFSDK_WaveWorks_Result.OK != CheckDetailLevelSupport(global_settings.detail_level,"Simulation_CreateD3D11_Generic"))
        {
            return  null;
//            return GFSDK_WaveWorks_Result.FAIL;
        }

        GFSDK_WaveWorks_Simulation pImpl = new GFSDK_WaveWorks_Simulation();
        GFSDK_WaveWorks_Detailed_Simulation_Params detailed_params;
        Init_Detailed_Water_Simulation_Params(global_settings, global_params, detailed_params);
        HRESULT hr = pImpl.initD3D11(detailed_params/*, pOptionalScheduler, pD3DDevice*/);
        if(hr==HRESULT.E_FAIL)
        {
//            delete pImpl;
//            return ToAPIResult(hr);
            return null;
        }
//			*pResult = ToHandle(pImpl);
//        return gfsdk_waveworks_result_OK;
        return pImpl;
//		#else // WAVEWORKS_ENABLE_D3D11
//        return gfsdk_waveworks_result_FAIL;
//		#endif // WAVEWORKS_ENABLE_D3D11
    }

    // Simulation lifetime management
    public static GFSDK_WaveWorks_Simulation GFSDK_WaveWorks_Simulation_CreateNoGraphics(GFSDK_WaveWorks_Simulation_Settings settings,  GFSDK_WaveWorks_Simulation_Params params){
        if(g_InitialisedAPI != nv_water_d3d_api.nv_water_d3d_api_none) {
            LogUtil.e(LogUtil.LogType.DEFAULT, "ERROR: 'GFSDK_WaveWorks_Simulation_CreateNoGraphics' was called but the library was not initialised for none!");
            return null;
        }

        // Don't assume the user checked GFSDK_WaveWorks_Simulation_DetailLevelIsSupported_XXXX()...
        if(GFSDK_WaveWorks_Result.OK != CheckDetailLevelSupport(settings.detail_level,"GFSDK_WaveWorks_Simulation_CreateNoGraphics"))
        {
            return null;
        }

        GFSDK_WaveWorks_Simulation pImpl = new GFSDK_WaveWorks_Simulation();
        GFSDK_WaveWorks_Detailed_Simulation_Params detailed_params=new GFSDK_WaveWorks_Detailed_Simulation_Params();
        Init_Detailed_Water_Simulation_Params(settings, params, detailed_params);
        HRESULT hr = pImpl.initNoGraphics(detailed_params);
        if(hr==HRESULT.E_FAIL)
        {
//            delete pImpl;
            return null;// ToAPIResult(hr);
        }

        return pImpl;
    }


    public static GFSDK_WaveWorks_Simulation GFSDK_WaveWorks_Simulation_CreateD3D11(GFSDK_WaveWorks_Simulation_Settings settings,  GFSDK_WaveWorks_Simulation_Params params/*, ID3D11Device* pD3DDevice, GFSDK_WaveWorks_Simulation* pResult*/){
        if(g_InitialisedAPI != nv_water_d3d_api.nv_water_d3d_api_d3d11) {
            LogUtil.e(LogUtil.LogType.DEFAULT, "ERROR: 'GFSDK_WaveWorks_Simulation_CreateD3D11' was called but the library was not initialised for d3d11!");
            return null;
        }

        return Simulation_CreateD3D11_Generic(settings, params/*, NULL, pD3DDevice, pResult*/);
    }
    public static GFSDK_WaveWorks_Simulation GFSDK_WaveWorks_Simulation_CreateGL2(GFSDK_WaveWorks_Simulation_Settings settings,  GFSDK_WaveWorks_Simulation_Params params/*, void *pGLContext, GFSDK_WaveWorks_Simulation* pResult*/){
        if(g_InitialisedAPI != nv_water_d3d_api.nv_water_d3d_api_gl2) {
            LogUtil.e(LogUtil.LogType.DEFAULT, "ERROR: 'GFSDK_WaveWorks_Simulation_CreateGL2' was called but the library was not initialised for gl2!");
            return null;
        }

        // Don't assume the user checked GFSDK_WaveWorks_Simulation_DetailLevelIsSupported_XXXX()...
	    final int simulationAPI = ToAPI(settings.detail_level);
        switch(simulationAPI) {
            case nv_water_simulation_api.nv_water_simulation_api_cuda:
            {
                if(g_CanUseCUDA)
                    break;			// We detected CUDA, keep going
            }
            case nv_water_simulation_api.nv_water_simulation_api_cpu:
            {
//			#ifdef SUPPORT_FFTCPU
//                break;
//			#else
//                return gfsdk_waveworks_result_FAIL;
                return null;
//			#endif
            }
        }

        GFSDK_WaveWorks_Simulation pImpl = new GFSDK_WaveWorks_Simulation();
        GFSDK_WaveWorks_Detailed_Simulation_Params detailed_params=new GFSDK_WaveWorks_Detailed_Simulation_Params();
        Init_Detailed_Water_Simulation_Params(settings, params, detailed_params);
        HRESULT hr = pImpl.initGL2(detailed_params/*, pGLContext*/);
        if(hr != HRESULT.S_OK)
        {
//            delete pImpl;
//            return ToAPIResult(hr);
            return null;
        }
//	*pResult = ToHandle(pImpl);
//        return gfsdk_waveworks_result_OK;
        return pImpl;
    }
    public static GFSDK_WaveWorks_Result GFSDK_WaveWorks_Simulation_Destroy(GFSDK_WaveWorks_Simulation hSim){
        hSim.dispose();
        return GFSDK_WaveWorks_Result.OK;
    }

    private static GFSDK_WaveWorks_Result ToAPIResult(HRESULT hr) {
        if(hr == HRESULT.S_OK) {
            return GFSDK_WaveWorks_Result.OK;
        }
        else {
            return GFSDK_WaveWorks_Result.FAIL;
        }
    }

    /**
     * A simulation can be 'updated' with new settings and properties - this is universally preferable to recreating
     * a simulation from scratch, since WaveWorks will only do as much reinitialization work as is necessary to implement
     * the changes in the setup. For instance, simple changes of wind speed require no reallocations and no interruptions
     * to the simulation and rendering pipeline
     * @param hSim
     * @param settings
     * @param params
     * @return
     */
    public static GFSDK_WaveWorks_Result GFSDK_WaveWorks_Simulation_UpdateProperties(GFSDK_WaveWorks_Simulation hSim, GFSDK_WaveWorks_Simulation_Settings settings, GFSDK_WaveWorks_Simulation_Params params){
        // Don't assume the user checked GFSDK_WaveWorks_Simulation_DetailLevelIsSupported_XXXX()...
        if(GFSDK_WaveWorks_Result.OK != CheckDetailLevelSupport(settings.detail_level,"GFSDK_WaveWorks_Simulation_UpdateProperties"))
        {
            return GFSDK_WaveWorks_Result.FAIL;
        }

        GFSDK_WaveWorks_Detailed_Simulation_Params detailed_params = new GFSDK_WaveWorks_Detailed_Simulation_Params();
        GFSDK_WaveWorks_Simulation pImpl = hSim;
        Init_Detailed_Water_Simulation_Params(settings, params, detailed_params);
        return ToAPIResult(pImpl.reinit(detailed_params));
    }

    // Sets the absolute simulation time for the next kick. WaveWorks guarantees that the same displacements will be
// generated for the same settings and input times, even across different platforms (e.g. to enable network-
// synchronized implementations)
    public static GFSDK_WaveWorks_Result GFSDK_WaveWorks_Simulation_SetTime(GFSDK_WaveWorks_Simulation hSim, double dAppTime){
        hSim.setSimulationTime(dAppTime);
        return GFSDK_WaveWorks_Result.OK;
    }

    /**
     * Retrieve information about the WaveWorks shader inputs for a given platform. This information can be used to
     * query compiled shaders via a reflection interface to obtain register or constant buffer indices for subsequent
     * calls to SetRenderState
     * @return
     */
    public static int GFSDK_WaveWorks_Simulation_GetShaderInputCountD3D11(){
        if(g_InitialisedAPI != nv_water_d3d_api.nv_water_d3d_api_d3d11) {
            LogUtil.e(LogUtil.LogType.DEFAULT, "ERROR: 'GFSDK_WaveWorks_Simulation_GetShaderInputCountD3D11' was called but the library was not initialised for d3d11!");
            return 0;
        }

        return GFSDK_WaveWorks_Simulation.getShaderInputCountD3D11();
    }

    public static GFSDK_WaveWorks_Result GFSDK_WaveWorks_Simulation_GetShaderInputDescD3D11(int inputIndex, GFSDK_WaveWorks_ShaderInput_Desc pDesc){
        if(g_InitialisedAPI != nv_water_d3d_api.nv_water_d3d_api_d3d11) {
            LogUtil.e(LogUtil.LogType.DEFAULT, "ERROR: 'GFSDK_WaveWorks_Simulation_GetShaderInputDescD3D11' was called but the library was not initialised for d3d11!");
            return GFSDK_WaveWorks_Result.FAIL;
        }

        return ToAPIResult(GFSDK_WaveWorks_Simulation.getShaderInputDescD3D11(inputIndex, pDesc));
    }

    public static int GFSDK_WaveWorks_Simulation_GetShaderInputCountGL2(){
        if(g_InitialisedAPI != nv_water_d3d_api.nv_water_d3d_api_gl2) {
            LogUtil.e(LogUtil.LogType.DEFAULT, "ERROR: 'GFSDK_WaveWorks_Simulation_SetRenderStateD3D11' was called but the library was not initialised for gl2!");
            return 0;
        }

        return GFSDK_WaveWorks_Simulation.getShaderInputCountGL2();
    }

    public static GFSDK_WaveWorks_Result GFSDK_WaveWorks_Simulation_GetShaderInputDescGL2(int inputIndex, GFSDK_WaveWorks_ShaderInput_Desc pDesc){
        if(g_InitialisedAPI != nv_water_d3d_api.nv_water_d3d_api_gl2) {
            LogUtil.e(LogUtil.LogType.DEFAULT, "ERROR: 'GFSDK_WaveWorks_Simulation_SetRenderStateD3D11' was called but the library was not initialised for gl2!");
            return GFSDK_WaveWorks_Result.FAIL;
        }

        return ToAPIResult(GFSDK_WaveWorks_Simulation.getShaderInputDescGL2(inputIndex, pDesc));
    }

    // For GL only, get the number of texture units that need to be reserved for WaveWorks in GFSDK_WaveWorks_Simulation_GL_Pool
    public static int GFSDK_WaveWorks_Simulation_GetTextureUnitCountGL2(boolean useTextureArrays){
        if(g_InitialisedAPI != nv_water_d3d_api.nv_water_d3d_api_gl2) {
            LogUtil.e(LogUtil.LogType.DEFAULT, "ERROR: 'GFSDK_WaveWorks_Simulation_GetTextureUnitCountGL2' was called but the library was not initialised for gl2!");
            return 0;
        }

        return GFSDK_WaveWorks_Simulation.getTextureUnitCountGL2(useTextureArrays);
    }

    private static GFSDK_WaveWorks_Result Simulation_SetRenderState_Generic(GFSDK_WaveWorks_Simulation hSim, Matrix4f matView, int[] pShaderInputRegisterMappings,
                                                                            GFSDK_WaveWorks_Savestate hSavestate, GFSDK_WaveWorks_Simulation_GL_Pool pGlPool)
    {
        GFSDK_WaveWorks_Savestate pImpl = hSavestate;
//        if(hSavestate)
//        {
//            pImpl = FromHandle(hSavestate);
//        }

        return ToAPIResult(hSim.setRenderState(matView, pShaderInputRegisterMappings, pImpl, pGlPool));
    }

    // Set WaveWorks shader inputs ready for rendering - use GetStagingCursor() to identify the kick which produced the simulation
// results that are about to be set
    public static GFSDK_WaveWorks_Result GFSDK_WaveWorks_Simulation_SetRenderStateD3D11(GFSDK_WaveWorks_Simulation hSim, /*ID3D11DeviceContext* pDC, */Matrix4f matView, int[] pShaderInputRegisterMappings, GFSDK_WaveWorks_Savestate hSavestate){
        if(g_InitialisedAPI != nv_water_d3d_api.nv_water_d3d_api_d3d11) {
            LogUtil.e(LogUtil.LogType.DEFAULT, "ERROR: 'GFSDK_WaveWorks_Simulation_SetRenderStateD3D11' was called but the library was not initialised for d3d11!");
            return GFSDK_WaveWorks_Result.FAIL;
        }

        return Simulation_SetRenderState_Generic(hSim,matView,pShaderInputRegisterMappings,hSavestate,null);
    }

    public static GFSDK_WaveWorks_Result GFSDK_WaveWorks_Simulation_SetRenderStateGL2(GFSDK_WaveWorks_Simulation hSim, Matrix4f matView, int[] pShaderInputRegisterMappings, GFSDK_WaveWorks_Simulation_GL_Pool glPool){
        if(g_InitialisedAPI != nv_water_d3d_api.nv_water_d3d_api_gl2) {
            LogUtil.e(LogUtil.LogType.DEFAULT, "ERROR: 'GFSDK_WaveWorks_Simulation_GetTextureUnitCountGL2' was called but the library was not initialised for gl2!");
            return GFSDK_WaveWorks_Result.FAIL;
        }

        return Simulation_SetRenderState_Generic(hSim,matView,pShaderInputRegisterMappings,null,glPool);
    }

    // Retrieve an array of simulated displacements for some given array of x-y locations - use GetReadbackCursor() to identify the
// kick which produced the simulation results that are about to be retrieved
    public static GFSDK_WaveWorks_Result GFSDK_WaveWorks_Simulation_GetDisplacements(GFSDK_WaveWorks_Simulation hSim, Vector2f[] inSamplePoints, Vector4f[] outDisplacements, int numSamples){
        hSim.getDisplacements(inSamplePoints, outDisplacements, numSamples);
        return GFSDK_WaveWorks_Result.OK;
    }

    // Get the most recent simulation statistics
    public static GFSDK_WaveWorks_Result GFSDK_WaveWorks_Simulation_GetStats(GFSDK_WaveWorks_Simulation hSim, GFSDK_WaveWorks_Simulation_Stats& stats);

    // For the current simulation settings and params, calculate an estimate of the maximum displacement that can be generated by the simulation.
// This can be used to conservatively inflate camera frusta for culling purposes (e.g. as a suitable value for Quadtree_SetFrustumCullMargin)
    GFSDK_WAVEWORKS_DECL(gfsdk_F32                 ) GFSDK_WaveWorks_Simulation_GetConservativeMaxDisplacementEstimate(GFSDK_WaveWorks_Simulation hSim);

    // Kicks off the work to update the simulation to the most recent time specified by SetTime
// The top of the simulation pipeline is always run on the CPU, whereas the bottom may be run on either the CPU or GPU, depending on whether the simulation
// is using the CPU or GPU path internally, and whether graphics interop is required for rendering.
// If necessary, this call will block until the CPU part of the pipeline is able to accept further in-flight work. If the CPU part of the pipeline
// is already completely full, this means waiting for an in-flight kick to exit the CPU pipeline (kicks are processed in FIFO order)
    public static GFSDK_WaveWorks_Result GFSDK_WaveWorks_Simulation_KickNoGraphics(GFSDK_WaveWorks_Simulation hSim, gfsdk_U64* pKickID);
    public static GFSDK_WaveWorks_Result GFSDK_WaveWorks_Simulation_KickD3D11(GFSDK_WaveWorks_Simulation hSim, gfsdk_U64* pKickID, ID3D11DeviceContext* pDC, GFSDK_WaveWorks_SavestateHandle hSavestate);
    public static GFSDK_WaveWorks_Result GFSDK_WaveWorks_Simulation_KickGL2(GFSDK_WaveWorks_Simulation hSim, gfsdk_U64* pKickID);

    // The staging cursor points to the most recent kick to exit the CPU part of the simulation pipeline (and therefore the kick whose state would be set by a
// subsequent call to SetRenderState)
// Returns gfsdk_waveworks_result_NONE if no simulation results are staged
// The staging cursor will only ever change during an API call, and is guaranteed to advance by a maximum of one kick in any one call
    public static GFSDK_WaveWorks_Result GFSDK_WaveWorks_Simulation_GetStagingCursor(GFSDK_WaveWorks_Simulation hSim, gfsdk_U64* pKickID);

    // Advances the staging cursor
// Use block to specify behaviour in the case where there is an in-flight kick in the CPU part of the simulation pipeline
// Returns gfsdk_waveworks_result_NONE if there are no in-flight kicks in the CPU part of the simulation pipeline
// Returns gfsdk_waveworks_result_WOULD_BLOCK if there are in-flight kicks in the CPU part of the pipeline, but they're not ready for staging
    public static GFSDK_WaveWorks_Result GFSDK_WaveWorks_Simulation_AdvanceStagingCursorNoGraphics(GFSDK_WaveWorks_Simulation hSim, bool block);
    public static GFSDK_WaveWorks_Result GFSDK_WaveWorks_Simulation_AdvanceStagingCursorD3D11(GFSDK_WaveWorks_Simulation hSim, bool block, ID3D11DeviceContext* pDC, GFSDK_WaveWorks_SavestateHandle hSavestate);
    public static GFSDK_WaveWorks_Result GFSDK_WaveWorks_Simulation_AdvanceStagingCursorGL2(GFSDK_WaveWorks_Simulation hSim, bool block);

    // Waits until the staging cursor is ready to advance (i.e. waits until a non-blocking call to AdvanceStagingCursor would succeed)
// Returns gfsdk_waveworks_result_NONE if there are no in-flight kicks in the CPU part of the simulation pipeline
    public static GFSDK_WaveWorks_Result GFSDK_WaveWorks_Simulation_WaitStagingCursor(GFSDK_WaveWorks_Simulation hSim);

    // The readback cursor points to the kick whose results would be fetched by a call to GetDisplacements
// Returns gfsdk_waveworks_result_NONE if no results are available for readback
// The readback cursor will only ever change during an API call, and is guaranteed to advance by a maximum of one kick in any one call
    public static GFSDK_WaveWorks_Result GFSDK_WaveWorks_Simulation_GetReadbackCursor(GFSDK_WaveWorks_Simulation hSim, gfsdk_U64* pKickID);

    // Advances the readback cursor
// Use block to specify behaviour in the case where there is an in-flight readback
// Returns gfsdk_waveworks_result_NONE if there are no readbacks in-flight beyond staging
// Returns gfsdk_waveworks_result_WOULD_BLOCK if there are readbacks in-flight  beyond staging, but they're not yet ready
    public static GFSDK_WaveWorks_Result GFSDK_WaveWorks_Simulation_AdvanceReadbackCursor(GFSDK_WaveWorks_Simulation hSim, bool block);

    // Archives the current readback results in the readback FIFO, evicting the oldest FIFO entry if necessary
// Returns gfsdk_waveworks_result_FAIL if no results are available for readback
    public static GFSDK_WaveWorks_Result GFSDK_WaveWorks_Simulation_ArchiveDisplacements(GFSDK_WaveWorks_Simulation hSim);

    // Identical to GFSDK_WaveWorks_Simulation_GetDisplacements, except values are retrieved from the readback FIFO
// The readback entries to use are specified using the 'coord' parameter, as follows:
//    - specify 0.f to read from the most recent entry in the FIFO
//    - specify (num_readback_FIFO_entries-1) to read from the oldest entry in the FIFO
//    - intervening entries may be accessed the same way, using a zero-based index
//    - if 'coord' is fractional, the nearest pair of entries will be lerp'd accordingly (fractional lookups are therefore more CPU-intensive)
//
    public static GFSDK_WaveWorks_Result GFSDK_WaveWorks_Simulation_GetArchivedDisplacements(GFSDK_WaveWorks_Simulation hSim, float coord, Vector2f[] inSamplePoints, Vector4f[] outDisplacements, int numSamples){
        hSim.getArchivedDisplacements(coord, inSamplePoints, outDisplacements, numSamples);
        return GFSDK_WaveWorks_Result.OK;
    }

    // Quadtree lifetime management
    public static GFSDK_WaveWorks_Result GFSDK_WaveWorks_Quadtree_CreateD3D11(GFSDK_WaveWorks_Quadtree_Params params, /*ID3D11Device* pD3DDevice,*/ GFSDK_WaveWorks_QuadtreeHandle* pResult);
    public static GFSDK_WaveWorks_Result GFSDK_WaveWorks_Quadtree_CreateGL2(GFSDK_WaveWorks_Quadtree_Params params, unsigned int Program, GFSDK_WaveWorks_QuadtreeHandle* pResult);
    public static GFSDK_WaveWorks_Result GFSDK_WaveWorks_Quadtree_Destroy(GFSDK_WaveWorks_QuadtreeHandle hQuadtree);

    // A quadtree can be udpated with new parameters. This is something of a corner-case for quadtrees, but is provided for completeness
    public static GFSDK_WaveWorks_Result GFSDK_WaveWorks_Quadtree_UpdateParams(GFSDK_WaveWorks_QuadtreeHandle hQuadtree, const GFSDK_WaveWorks_Quadtree_Params& params);

    // Retrieve information about the WaveWorks shader inputs for a given platform. This information can be used to
// query compiled shaders via a reflection interface to obtain register or constant buffer indices for subsequent
// calls to Draw
    GFSDK_WAVEWORKS_DECL(gfsdk_U32                ) GFSDK_WaveWorks_Quadtree_GetShaderInputCountD3D11();
    public static GFSDK_WaveWorks_Result GFSDK_WaveWorks_Quadtree_GetShaderInputDescD3D11(gfsdk_U32 inputIndex, GFSDK_WaveWorks_ShaderInput_Desc* pDesc);
    GFSDK_WAVEWORKS_DECL(gfsdk_U32                ) GFSDK_WaveWorks_Quadtree_GetShaderInputCountGL2();
    public static GFSDK_WaveWorks_Result GFSDK_WaveWorks_Quadtree_GetShaderInputDescGL2(gfsdk_U32 inputIndex, GFSDK_WaveWorks_ShaderInput_Desc* pDesc);

    // Explicit control over quadtree tiles, primarily for removing tiles that are known to be entirey hidden by terrain.
// If AllocPatch is never called, the quadtree runs in automatic mode and is assumed to be rooted on a eye-centred
// patch whose dimension is determined by GFSDK_WaveWorks_Quadtree_Params.auto_root_lod
// Otherwise, WaveWorks starts frustum culling from the list of patches implied by calls to AllocPatch/FreePatch
    public static GFSDK_WaveWorks_Result GFSDK_WaveWorks_Quadtree_AllocPatch(GFSDK_WaveWorks_QuadtreeHandle hQuadtree, gfsdk_S32 x, gfsdk_S32 y, gfsdk_U32 lod, gfsdk_bool enabled);
    public static GFSDK_WaveWorks_Result GFSDK_WaveWorks_Quadtree_FreePatch(GFSDK_WaveWorks_QuadtreeHandle hQuadtree, gfsdk_S32 x, gfsdk_S32 y, gfsdk_U32 lod);

    // Draw the water surface using the specified quadtree with the specified view and projection matrices
    public static GFSDK_WaveWorks_Result GFSDK_WaveWorks_Quadtree_DrawD3D11(GFSDK_WaveWorks_QuadtreeHandle hQuadtree, ID3D11DeviceContext* pDC, const gfsdk_float4x4& matView, const gfsdk_float4x4& matProj, const gfsdk_U32 * pShaderInputRegisterMappings, GFSDK_WaveWorks_SavestateHandle hSavestate);
    public static GFSDK_WaveWorks_Result GFSDK_WaveWorks_Quadtree_DrawGL2(GFSDK_WaveWorks_QuadtreeHandle hQuadtree, const gfsdk_float4x4& matView, const gfsdk_float4x4& matProj, const gfsdk_U32 * pShaderInputRegisterMappings);

    // Get the most recent quadtree rendering statistics
    public static GFSDK_WaveWorks_Result GFSDK_WaveWorks_Quadtree_GetStats(GFSDK_WaveWorks_QuadtreeHandle hQuadtree, GFSDK_WaveWorks_Quadtree_Stats& stats);

    // Patches are culled based on their undisplaced footrpint plus an additional user-supplied margin to take account
// of simulated displacements. Simulation_GetConservativeMaxDisplacementEstimate() can be used for this directly
// when WaveWorks is the only source of displacement on the water surface, otherwise it can be added to estimates
// from any other sources as necessary (e.g. wakes, explosions etc.)
    public static GFSDK_WaveWorks_Result GFSDK_WaveWorks_Quadtree_SetFrustumCullMargin(GFSDK_WaveWorks_QuadtreeHandle hQuadtree, gfsdk_F32 margin);

}
