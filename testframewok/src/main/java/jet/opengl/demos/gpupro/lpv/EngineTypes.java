package jet.opengl.demos.gpupro.lpv;

interface EngineTypes {
    static final int
            EFLAG              = 0x00,
            EFLAG_SHOW_CONSOLE = 0x01,
            EFLAG_MAXIMIZED    = 0x02,
            EFLAG_FULLSCREEN   = 0x04;

    int REDRAW_TIMER_MS = 17;
    float FPS_MS = 0.001f;
    int FPS_COUNTER_MAX = 10;

    int INIT_LOAD_TIMER_MS = 100;

    int DEFAULT_SCREEN_WIDTH = 1024;
    int DEFAULT_SCREEN_HEIGHT = 600;
    int DEFAULT_MULTISAMPLING = 4;
    int MAX_TEXTURE_SIZE = 0;

    int SHADOW_TEXTURE_SIZE = 1024;
    int SHADOW_CASCADES_COUNT = 6;
    int SHADOW_TILES_X = 3;
    int SHADOW_TILES_Y = 2;
    float SHADOW_JITTERING = 2.0f;

    int GEOMETRY_TEXTURE_SIZE = 64;

    int SUN_RAYS_TEXTURE_SIZE = 256;

    float LPV_TEXTURE_SIZE_X = 32.0f;
    float LPV_TEXTURE_SIZE_Y = 32.0f;
    float LPV_TEXTURE_SIZE_Z = 32.0f;
    int LPV_PROPAGATION_STEPS = 5;
    float LPV_INTENSITY = 1.0f;
    float LPV_REFL_INTENSITY = 1.0f;
    int LPV_MODES_COUNT = 3;
    int LPV_TECHNIQUES_COUNT = 2;
    int LPV_SH_COUNT = 4;
    int LPV_CASCADES_COUNT = 4;
    int LPV_SPECIAL_DIRS_COUNT = 2; // camp proj
    int LPV_SUN_SKY_DIRS_COUNT = 2; // sun + sky
    int LPV_SKY_DIRS_COUNT = 1;
    int LPV_DIRS_RESERVED_COUNT = 1;
}
