package jet.opengl.demos.nvidia.waves.ocean;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector3f;
import org.lwjgl.util.vector.Vector4f;

import jet.opengl.postprocessing.texture.Texture2D;
import jet.opengl.postprocessing.texture.TextureGL;

final class OceanEnvironment {
    static final int MaxNumSpotlights = 11;

    final Vector3f main_light_direction = new Vector3f();
    final Vector3f main_light_color = new Vector3f();
    final Vector3f ambient_light_color = new Vector3f();
    final Vector3f sky_color = new Vector3f();
    final Vector3f sky_map_color_mult = new Vector3f();
    OceanSkyMapInfo pSky0;
    OceanSkyMapInfo pSky1;
    float sky_interp;
    float fog_exponent;

    final Vector4f[] spotlight_position = new Vector4f[MaxNumSpotlights];
    final Vector4f[] spotlight_axis_and_cos_angle = new Vector4f[MaxNumSpotlights];
    final Vector4f[] spotlight_color = new Vector4f[MaxNumSpotlights];

    final Matrix4f spotlights_to_world_matrix = new Matrix4f();
    final Matrix4f[] spotlight_shadow_matrix = new Matrix4f[MaxNumSpotlights];
    final Texture2D[] spotlight_shadow_resource = new Texture2D[MaxNumSpotlights];
    Texture2D pPlanarReflectionSRV;
    Texture2D pPlanarReflectionDepthSRV;
    Texture2D pPlanarReflectionPosSRV;

    int activeLightsNum;
    final int[] objectID = new int[MaxNumSpotlights];
    int lightFilter;

    TextureGL pSceneShadowmapSRV;
    final Vector3f lightning_light_position = new Vector3f();
    final Vector3f lightning_light_intensity = new Vector3f();;
    float lightning_time_to_next_strike;
    float lightning_time_to_next_cstrike;
    int   lightning_num_of_cstrikes;
    int   lightning_current_cstrike;

    float cloud_factor;

    final Vector4f gust_UV = new Vector4f();
}
