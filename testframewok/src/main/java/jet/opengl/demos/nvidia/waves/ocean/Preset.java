package jet.opengl.demos.nvidia.waves.ocean;

import org.lwjgl.util.vector.Vector3f;

final class Preset {
    String preset_name;
    float wind_speed;				// In Beaufort
    float smoke_speed;				// In m/s
    float smoke_emit_rate_scale;
    float ship_lighting;
    float visibility_distance;
    int sky_map;
    final Vector3f sky_color_mult;
    final Vector3f dir_light_color;
    boolean  lightnings_enabled;
    float lightning_avg_time_to_next_strike;
    float lightning_average_number_of_cstrikes;
    float cloud_factor;			// 0.0 - clear sky, 1.0 - overcast
    float time_of_day;			// in hours
    float upward_transition_time;
    float foam_spray_fade;

    public Preset(String preset_name, float wind_speed, float smoke_speed, float smoke_emit_rate_scale, float ship_lighting, float visibility_distance, int sky_map, Vector3f sky_color_mult, Vector3f dir_light_color, boolean lightnings_enabled, float lightning_avg_time_to_next_strike, float lightning_average_number_of_cstrikes, float cloud_factor, float time_of_day, float upward_transition_time, float foam_spray_fade) {
        this.preset_name = preset_name;
        this.wind_speed = wind_speed;
        this.smoke_speed = smoke_speed;
        this.smoke_emit_rate_scale = smoke_emit_rate_scale;
        this.ship_lighting = ship_lighting;
        this.visibility_distance = visibility_distance;
        this.sky_map = sky_map;
        this.sky_color_mult = sky_color_mult;
        this.dir_light_color = dir_light_color;
        this.lightnings_enabled = lightnings_enabled;
        this.lightning_avg_time_to_next_strike = lightning_avg_time_to_next_strike;
        this.lightning_average_number_of_cstrikes = lightning_average_number_of_cstrikes;
        this.cloud_factor = cloud_factor;
        this.time_of_day = time_of_day;
        this.upward_transition_time = upward_transition_time;
        this.foam_spray_fade = foam_spray_fade;
    }
}
