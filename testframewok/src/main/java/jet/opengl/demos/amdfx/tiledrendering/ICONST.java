package jet.opengl.demos.amdfx.tiledrendering;

interface ICONST {
    int MAX_NUM_LIGHTS = 2*1024;
    int MAX_NUM_GRID_OBJECTS = 280;

    // These must match their counterparts in CommonHeader.h
    int MAX_NUM_SHADOWCASTING_POINTS = 12;
    int MAX_NUM_SHADOWCASTING_SPOTS = 12;

    // no MSAA, 2x MSAA, and 4x MSAA
    int
        MSAA_SETTING_NO_MSAA = 0,
        MSAA_SETTING_2X_MSAA = 1,
        MSAA_SETTING_4X_MSAA = 2,
        NUM_MSAA_SETTINGS = 3;
    int g_nMSAASampleCount[] = {1,2,4};

    String SHADER_PATH = "amdfx/TiledLighting11/shaders/";
}
