package jet.opengl.demos.gpupro.rvi;

enum GlobalIllumMode {
    DEFAULT_GIM,             // default combined output (direct + indirect illumination)
    DIRECT_ILLUM_ONLY_GIM,   // direct illumination only
    INDIRECT_ILLUM_ONLY_GIM, // indirect illumination only
    VISUALIZE_GIM,           // visualization of voxel-grids
}
