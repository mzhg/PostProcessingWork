package jet.opengl.demos.nvidia.waves.samples;

/**
 * Created by mazhen'gui on 2017/7/27.
 */

interface Constants {
    int terrain_gridpoints = 512,
    terrain_smoothsteps	= 40,
    terrain_numpatches_1d =64;

    float terrain_geometry_scale =1.0f,
    terrain_maxheight = 35.0f,
    terrain_minheight = -30.0f,
    terrain_fractalfactor = 0.51f,
    terrain_fractalinitialvalue = 100.0f,
    terrain_smoothfactor1 = 0.9f,
    terrain_smoothfactor2 = -0.03f,
    terrain_rockfactor = 0.97f,
    terrain_height_underwater_start = -100.0f,
    terrain_height_underwater_end = -8.0f,
    terrain_height_sand_start = -30.0f,
    terrain_height_sand_end = 1.7f,
    terrain_height_grass_start = 1.7f,
    terrain_height_grass_end = 30.0f,
    terrain_height_rocks_start = -2.0f,
    terrain_height_trees_start = 4.0f,
    terrain_height_trees_end = 30.0f,
    terrain_slope_grass_start = 0.96f,
    terrain_slope_rocks_start = 0.85f,

    terrain_far_range =terrain_gridpoints*terrain_geometry_scale;

    int shadowmap_resource_buffer_size_xy = 4096,
    water_normalmap_resource_buffer_size_xy	= 2048,
    terrain_layerdef_map_texture_size = 1024,
    terrain_depth_shadow_map_texture_size = 512,
    sky_gridpoints = 20;

    float sky_texture_angle	= 0.34f,
    main_buffer_size_multiplier = 1.1f,
    reflection_buffer_size_multiplier = 1.1f,
    refraction_buffer_size_multiplier = 1.1f,

    scene_z_near					=	1.0f,
    scene_z_far						=	25000.0f,
    camera_fov						=	110.0f;
}
