package jet.opengl.demos.labs.atmosphere;

import com.nvidia.developer.opengl.app.NvSampleApp;

import org.lwjgl.util.vector.Matrix4f;

import jet.opengl.postprocessing.shader.GLSLProgram;

public class AtmosphereDemo extends NvSampleApp {
    enum Luminance {
        // Render the spectral radiance at kLambdaR, kLambdaG, kLambdaB.
        NONE,
        // Render the sRGB luminance, using an approximate (on the fly) conversion
        // from 3 spectral radiance values only (see section 14.3 in <a href=
        // "https://arxiv.org/pdf/1612.04336.pdf">A Qualitative and Quantitative
        //  Evaluation of 8 Clear Sky Models</a>).
        APPROXIMATE,
        // Render the sRGB luminance, precomputed from 15 spectral radiance values
        // (see section 4.4 in <a href=
        // "http://www.oskee.wz.cz/stranka/uploads/SCCG10ElekKmoch.pdf">Real-time
        //  Spectral Scattering in Large-scale Natural Participating Media</a>).
        PRECOMPUTED
    };

    boolean use_constant_solar_spectrum_;
    boolean use_ozone_;
    boolean use_combined_textures_;
    boolean use_half_precision_;
    Luminance use_luminance_;
    boolean do_white_balance_;
    boolean show_help_;

    Model model_;
    GLSLProgram program_;
    int full_screen_quad_vao_;

    double view_distance_meters_;
    double view_zenith_angle_radians_;
    double view_azimuth_angle_radians_;
    double sun_zenith_angle_radians_;
    double sun_azimuth_angle_radians_;
    double exposure_;

    int previous_mouse_x_;
    int previous_mouse_y_;
    boolean is_ctrl_key_pressed_;

    final Matrix4f mProj = new Matrix4f();

    @Override
    protected void initRendering() {

    }

    @Override
    public void display() {

    }

    @Override
    protected void reshape(int width, int height) {
        if(width <= 0 || height <=0)
            return;

        Matrix4f.perspective(60, (float)width/height, 0.1f, 1000.f, mProj);
    }
}
