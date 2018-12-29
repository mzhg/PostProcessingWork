package jet.opengl.demos.gpupro.rvi;

enum RenderOrder {
    BASE_RO, // fill GBuffer
    GRID_FILL_RO, // fill voxel-grid
    SHADOW_RO, // generate shadow maps
    ILLUM_RO, // direct illumination
    GRID_ILLUM_RO, // illuminate voxel-grid
    GLOBAL_ILLUM_RO, // generate global illumination
    SKY_RO, // render sky
    GUI_RO, // render GUI
    POST_PROCESS_RO // perform post-processing
}
