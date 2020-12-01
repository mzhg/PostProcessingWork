package jet.opengl.demos.amdfx.sssr;

import org.lwjgl.util.vector.Matrix4f;

class PassData {
    final Matrix4f inv_view_projection_ = new Matrix4f();
    final Matrix4f projection_ = new Matrix4f();
    final Matrix4f inv_projection_ = new Matrix4f();
    final Matrix4f view_ = new Matrix4f();
    final Matrix4f inv_view_ = new Matrix4f();
    final Matrix4f prev_view_projection_ = new Matrix4f();
    int frame_index_;
    int max_traversal_intersections_;
    int min_traversal_occupancy_;
    int most_detailed_mip_;
    float temporal_stability_factor_;
    float depth_buffer_thickness_;
    int samples_per_quad_;
    int temporal_variance_guided_tracing_enabled_;
    float roughness_threshold_;
    int skip_denoiser_;
}
