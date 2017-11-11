package jet.opengl.demos.amdfx.shadows;

interface ShadowFX_Constants {

	public static final int AMD_SHADOWFX_VERSION_MAJOR = 2;
	public static final int AMD_SHADOWFX_VERSION_MINOR = 0;
	public static final int AMD_SHADOWFX_VERSION_PATCH = 0;
	
	public static final int
//	typedef enum SHADOWFX_IMPLEMENTATION_t
//	{
	    SHADOWFX_IMPLEMENTATION_PS                   = 0,
	    SHADOWFX_IMPLEMENTATION_CS                   = 1, // not available yet
	    SHADOWFX_IMPLEMENTATION_COUNT                = 2;
//	} SHADOWFX_IMPLEMENTATION;

	public static final int
//	typedef enum SHADOWFX_EXECUTION_t
//	{
	    SHADOWFX_EXECUTION_UNION                     = 0,
	    SHADOWFX_EXECUTION_CASCADE                   = 1,
	    SHADOWFX_EXECUTION_CUBE                      = 2,
	    SHADOWFX_EXECUTION_WEIGHTED_AVG              = 3,

	    SHADOWFX_EXECUTION_COUNT                     = 4;
//	} SHADOWFX_EXECUTION;


	public static final int
//	typedef enum SHADOWFX_FILTERING_t
//	{
	    SHADOWFX_FILTERING_UNIFORM                   = 0,
	    SHADOWFX_FILTERING_CONTACT                   = 1,
	    SHADOWFX_FILTERING_COUNT                     = 2,

	    SHADOWFX_FILTERING_DEBUG_POINT               = 10,
	    SHADOWFX_FILTERING_DEBUG_COUNT               = 1;
//	} SHADOWFX_FILTERING;

	public static final int
//	typedef enum SHADOWFX_TEXTURE_FETCH_t
//	{
	    SHADOWFX_TEXTURE_FETCH_GATHER4               = 0,
	    SHADOWFX_TEXTURE_FETCH_PCF                   = 1,
	    SHADOWFX_TEXTURE_FETCH_COUNT                 = 2;
//	} SHADOWFX_TEXTURE_FETCH;

	public static final int
//	typedef enum SHADOWFX_TEXTURE_TYPE_t
//	{
	    SHADOWFX_TEXTURE_2D                          = 0,
	    SHADOWFX_TEXTURE_2D_ARRAY                    = 1,
	    SHADOWFX_TEXTURE_TYPE_COUNT                  = 2;
//	} SHADOWFX_TEXTURE_TYPE;

	// The TAP types
	public static final int
//	typedef enum SHADOWFX_TAP_TYPE_t
//	{
	    SHADOWFX_TAP_TYPE_FIXED                      = 0,
	    SHADOWFX_TAP_TYPE_POISSON                    = 1,
	    SHADOWFX_TAP_TYPE_COUNT                      = 2;
//	} SHADOWFX_TAP_TYPE;

	public static final int
//	typedef enum SHADOWFX_NORMAL_OPTION_t
//	{
	    SHADOWFX_NORMAL_OPTION_NONE                  = 0,
	    SHADOWFX_NORMAL_OPTION_CALC_FROM_DEPTH       = 1,
	    SHADOWFX_NORMAL_OPTION_READ_FROM_SRV         = 2,
	    SHADOWFX_NORMAL_OPTION_COUNT                 = 3;
//	} SHADOWFX_NORMAL_OPTION;

	public static final int
//	typedef enum SHADOWFX_FILTER_SIZE_t
//	{
	    SHADOWFX_FILTER_SIZE_7                       = 7,
	    SHADOWFX_FILTER_SIZE_9                       = 9,
	    SHADOWFX_FILTER_SIZE_11                      = 11,
	    SHADOWFX_FILTER_SIZE_13                      = 13,
	    SHADOWFX_FILTER_SIZE_15                      = 15,
	    SHADOWFX_FILTER_SIZE_COUNT                   = 5;
//	} SHADOWFX_FILTER_SIZE;

	public static final int
//	typedef enum SHADOWFX_OUTPUT_CHANNEL_t
//	{
	    SHADOWFX_OUTPUT_CHANNEL_R                    = 1,
	    SHADOWFX_OUTPUT_CHANNEL_G                    = 2,
	    SHADOWFX_OUTPUT_CHANNEL_B                    = 4,
	    SHADOWFX_OUTPUT_CHANNEL_A                    = 8,
	    SHADOWFX_OUTPUT_CHANNEL_COUNT                = 16;
//	} SHADOWFX_OUTPUT_CHANNEL;
}
