package jet.opengl.demos.nvidia.waves.packets;

interface GlobalDefs {
    // Global definitions needed for packet simulation and rendering

    // scene parameters
    float SCENE_EXTENT = 100.0f;					// extent of the entire scene (packets traveling outside are removed)
    float MIN_WATER_DEPTH = 0.1f;				    // minimum water depth (meters)
    float MAX_WATER_DEPTH = 5.0f;				    // maximum water depth (meters)
    String WATER_TERRAIN_FILE = "TestIsland.bmp";   // Contains water depth and land height in different channels


    // rendering parameters
    int PACKET_GPU_BUFFER_SIZE = 1000000;   		// maximum number of wave packets to be displayed in one draw call


/*
// Fast rendering setup
#define WAVETEX_WIDTH_FACTOR 0.5	// the wavemesh texture compared to screen resolution
#define WAVETEX_HEIGHT_FACTOR 1		// the wavemesh texture compared to screen resolution
#define WAVEMESH_WIDTH_FACTOR 0.1	// the fine wave mesh compared to screen resolution
#define WAVEMESH_HEIGHT_FACTOR 0.25	// the fine wave mesh compared to screen resolution
#define AA_OVERSAMPLE_FACTOR 2		// anti aliasing applied in BOTH X and Y directions  {1,2,4,8}
*/

/*
// Balanced rendering  setup
#define WAVETEX_WIDTH_FACTOR 1	    // the wavemesh texture compared to screen resolution
#define WAVETEX_HEIGHT_FACTOR 2	    // the wavemesh texture compared to screen resolution
#define WAVEMESH_WIDTH_FACTOR 1		// the fine wave mesh compared to screen resolution
#define WAVEMESH_HEIGHT_FACTOR 2	// the fine wave mesh compared to screen resolution
#define AA_OVERSAMPLE_FACTOR 2		// anti aliasing applied in BOTH X and Y directions  {1,2,4,8}
*/


// High quality rendering  setup
    int WAVETEX_WIDTH_FACTOR = 2;		// the wavemesh texture compared to screen resolution
    int WAVETEX_HEIGHT_FACTOR = 4;		// the wavemesh texture compared to screen resolution
    int WAVEMESH_WIDTH_FACTOR = 2;		// the fine wave mesh compared to screen resolution
    int WAVEMESH_HEIGHT_FACTOR = 4;	// the fine wave mesh compared to screen resolution
    int AA_OVERSAMPLE_FACTOR = 4;		// anti aliasing applied in BOTH X and Y directions  {1,2,4,8}


    // simulation parameters
    float PACKET_SPLIT_ANGLE =0.95105f;			// direction angle variation threshold: 0.95105=18 degree
    float PACKET_SPLIT_DISPERSION = 0.3f;		// if the fastest wave in a packet traveled PACKET_SPLIT_DISPERSION*Envelopesize ahead, or the slowest by the same amount behind, subdivide this packet into two wavelength intervals
    float PACKET_KILL_AMPLITUDE_DERIV = 0.0001f;	// waves below this maximum amplitude derivative gets killed
    float PACKET_BLEND_TRAVEL_FACTOR =1.0f;		// in order to be fully blended (appear or disappear), any wave must travel PACKET_BLEND_TRAVEL_FACTOR times "envelope size" in space (1.0 is standard)
    float PACKET_ENVELOPE_SIZE_FACTOR =3.0f;	// size of the envelope relative to wavelength (determines how many "bumps" appear)
    float PACKET_ENVELOPE_MINSIZE = 0.02f;		// minimum envelope size in meters (smallest expected feature)
    float PACKET_ENVELOPE_MAXSIZE = 10.0f;		// maximum envelope size in meters (largest expected feature)
    boolean PACKET_BOUNCE_FREQSPLIT = true; 		// (boolean) should a wave packet produce smaller waves at a bounce/reflection (->widen the wavelength interval of this packet)?
    float PACKET_BOUNCE_FREQSPLIT_K = 31.4f;		// if k_L is smaller than this value (lambda = 20cm), the wave is (potentially) split after a bounce
    float MAX_SPEEDNESS = 0.07f;					// all wave amplitudes a are limited to a <= MAX_SPEEDNESS*2.0*M_PI/k

// physical parameters
    float SIGMA = 0.074f;						// surface tension N/m at 20 grad celsius
    float GRAVITY =9.81f;						// GRAVITY m/s^2
    float DENSITY = 998.2071f;					// water density at 20 degree celsius
    float KINEMATIC_VISCOSITY = 0.0000089f;		// kinematic viscosity
    float PACKET_SLOWAVE_K = 143.1405792f;		// k of the slowest possible wave packet
    float PACKET_SLOWAVE_W0 = 40.2646141f;		// w_0 of the slowest possible wave packet

// memory management
    int PACKET_BUFFER_DELTA = 500000;			// initial number of vertices, packet memory will be increased on demand by this stepsize
}
