package jet.opengl.renderer.Unreal4;

import jet.opengl.renderer.Unreal4.api.FMobile;

public final class UE4Engine {
    /* Maximum size of the world */
    public static final float WORLD_MAX	= 2097152.0f;

    /* Half the maximum size of the world */
    public static final float HALF_WORLD_MAX = (WORLD_MAX * 0.5f);

    /* Half the maximum size of the world minus one */
    public static final float HALF_WORLD_MAX1 = (HALF_WORLD_MAX - 1.0f);

    public static final int MAX_CASCADE = 4;

    public static final int BUF_Volatile = 1;

    public static final String SHADER_PATH = "Unreal4/";

    /** Must match global distance field shaders. */
    public static final int GMaxGlobalDistanceFieldClipmaps = 4;

    public static final int GMaxForwardShadowCascades = MAX_CASCADE;
    public static final int INDEX_NONE = -1;

    public static final boolean CHECKING = true;

    /** Maximum number of custom lighting channels */
    public static final int NUM_LIGHTING_CHANNELS = 3;

    /** The number of lights to consider for sky/atmospheric light scattering */
    public static final int NUM_ATMOSPHERE_LIGHTS = 2;

    public static final int NumCustomPrimitiveDataFloat4s = 8; // Must match NUM_CUSTOM_PRIMITIVE_DATA in SceneData.ush

    // DX11 maximum 2d texture array size is D3D11_REQ_TEXTURE2D_ARRAY_AXIS_DIMENSION = 2048, and 2048/6 = 341.33.
    public static final int GMaxNumReflectionCaptures = 341;

    /** Incremented once per frame before the scene is being rendered. In split screen mode this is incremented once for all views (not for each view). */
    public static int GFrameNumber = 1;

    public final static FMobile Mobile = new FMobile();

    public static void check(Object obj){
        if(CHECKING){
            if(obj == null){
                throw new NullPointerException();
            }
        }
    }

    public static void check(boolean condition){
        if(CHECKING){
            if(!condition){
                throw new IllegalStateException();
            }
        }
    }

    public static void ensure(boolean condition){
        if(!condition){
            throw new IllegalStateException();
        }
    }

}
