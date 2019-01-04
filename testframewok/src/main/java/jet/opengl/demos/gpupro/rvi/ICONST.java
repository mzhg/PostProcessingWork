package jet.opengl.demos.gpupro.rvi;

interface ICONST {
    int
    BACK_BUFFER_RT_ID=0, // back buffer
    GBUFFER_RT_ID =1, // geometry buffer
    SHADOW_MAP_RT_ID=2; // shadow map

    int
            CUSTOM_SB0_BP=9,
            CUSTOM_SB1_BP=10;

    /** number of custom textures that can be set in SURFACE */
    int NUM_CUSTOM_TEXURES = 6;
    /** number of custom structured-buffers that can be set in SURFACE */
    int NUM_CUSTOM_STRUCTURED_BUFFERS = 2;

    int NUM_PATH_POINT_LIGHTS = 12;

    int SCREEN_WIDTH =1280;
    int SCREEN_HEIGHT = 720;

    int MAIN_CAMERA_ID = 0;

    int COLOR_TEX_BP  = 0,
    NORMAL_TEX_BP = 1,
    SPECULAR_TEX_BP =2,
    CUSTOM0_TEX_BP =3,
    CUSTOM1_TEX_BP =4,
    CUSTOM2_TEX_BP =5,
    CUSTOM3_TEX_BP =6,
    CUSTOM4_TEX_BP =7,
    CUSTOM5_TEX_BP =8,

    CUSTOM0_SB_BP =9,
    CUSTOM1_SB_BP =10,

    COLOR_SAM_BP =0,
     NORMAL_SAM_BP =1,
    SPECULAR_SAM_BP =2,
    CUSTOM0_SAM_BP =3,
    CUSTOM1_SAM_BP =4,
    CUSTOM2_SAM_BP =5,
    CUSTOM3_SAM_BP =6,
    CUSTOM4_SAM_BP =7,
    CUSTOM5_SAM_BP =8;
}
