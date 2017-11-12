package jet.opengl.demos.nvidia.illumination;

/**
 * Created by Administrator on 2017/11/12 0012.
 */

interface Defines {
    int DOWNSAMPLE_MAX = 0;
    int DOWNSAMPLE_MIN = 1;
    int DOWNSAMPLE_AVERAGE = 2;

    int UPSAMPLE_DUPLICATE = 0,
            UPSAMPLE_BILINEAR = 1;

    int SAMPLE_REPLACE = 0,
            SAMPLE_ACCUMULATE = 1;

    //block size for the LPV propagation code
    int X_BLOCK_SIZE =8,
             Y_BLOCK_SIZE =8,
             Z_BLOCK_SIZE =4,
             TOTAL_BLOCK_SIZE =405, //(8+1)*(8+1)*(4+1)

             SM_SIZE =1024, //the resolution of the shadow map for the scene

             RSM_RES =512, //the resolution of the RSMs (this has to be a power of two, see note below)
             RSM_RES_M_1 =RSM_RES-1, //one RSM_RES-1 (to be used for doing modulus with RSM_RES - note that any code using this assumes RSM_RES is a power of two)
             RSM_RES_SQR =RSM_RES*RSM_RES;
 int RSM_RES_SQR_M_1 =RSM_RES_SQR-1,

             g_LPVWIDTH =32, //resolution of the 3D LPV
             g_LPVHEIGHT =32, //resolution of the 3D LPV
             g_LPVDEPTH =32, //resolution of the 3D LPV

             USE_MULTIPLE_BOUNCES=33;

 int HIERARCHICAL_INIT_LEVEL =0, //if we are using the hiearchy model we always initialize the top level, and downsample down to the other levels

//#define USE_SINGLE_CHANNELS

             MAX_P_SAMPLES =32;
}
