package jet.opengl.renderer.Unreal4;

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

    public static final int GMaxNumReflectionCaptures = 341;

    /** Must match global distance field shaders. */
    public static final int GMaxGlobalDistanceFieldClipmaps = 4;

    public static final int GMaxForwardShadowCascades = MAX_CASCADE;
    public static final int INDEX_NONE = -1;

    public static final boolean CHECKING = true;

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
