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
}
