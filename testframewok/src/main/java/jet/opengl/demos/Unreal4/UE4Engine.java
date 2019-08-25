package jet.opengl.demos.Unreal4;

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
}
