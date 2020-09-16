package jet.opengl.demos.labs.atmosphere;

import com.nvidia.developer.opengl.app.NvCameraMotionType;
import com.nvidia.developer.opengl.app.NvSampleApp;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector2f;
import org.lwjgl.util.vector.Vector3f;

import java.util.ArrayList;
import java.util.List;

import jet.opengl.postprocessing.common.GLFuncProvider;
import jet.opengl.postprocessing.common.GLFuncProviderFactory;
import jet.opengl.postprocessing.common.GLenum;
import jet.opengl.postprocessing.shader.GLSLProgram;
import jet.opengl.postprocessing.shader.GLSLUtil;
import jet.opengl.postprocessing.shader.Macro;
import jet.opengl.postprocessing.util.StackDouble;

public class AtmosphereDemo extends NvSampleApp {
    private static final double kPi = 3.1415926;
    private static final double kSunAngularRadius = 0.00935 / 2.0 /*Math.toRadians(0.545)*/;
    private static final double kSunSolidAngle = kPi * kSunAngularRadius * kSunAngularRadius;
    private static final double kLengthUnitInMeters = 1000.0;

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

    boolean use_constant_solar_spectrum_ = false;
    boolean use_ozone_ = true;
    final boolean use_combined_textures_ = true;
    boolean use_half_precision_ = true;
    Luminance use_luminance_ = Luminance.PRECOMPUTED;
    Luminance curr_lumi_mode = Luminance.PRECOMPUTED;
    boolean do_white_balance_ = false;
    boolean show_help_ = true;

    Model model_;
    GLSLProgram program_;
    int full_screen_quad_vao_;

    double view_distance_meters_ = 9000.0f;
    double view_zenith_angle_radians_ = 1.47f;
    double view_azimuth_angle_radians_ = -0.1f;
    float sun_zenith_angle_radians_ = 1.3f;
    float sun_azimuth_angle_radians_ = 2.9f;
    float exposure_ = 10;

    int previous_mouse_x_;
    int previous_mouse_y_;
    boolean is_ctrl_key_pressed_;

    final Matrix4f mProj = new Matrix4f();
    final Matrix4f mView = new Matrix4f();
    final Matrix4f mTemp = new Matrix4f();

    // Uniform variables
    final Vector3f camera = new Vector3f();
    final Vector3f white_point = new Vector3f();
    final Vector3f earth_center = new Vector3f();
    final Vector3f sun_direction = new Vector3f();
    final Vector2f sun_size = new Vector2f();

    private GLFuncProvider gl;

    @Override
    public void initUI() {
        mTweakBar.addValue("Sun Zenith Angle:", createControl("sun_zenith_angle_radians_"), 0.0f, (float)Math.PI/1.5f);
        mTweakBar.addValue("Sun Azimuth Angle:", createControl("sun_azimuth_angle_radians_"), 0.0f, (float)Math.PI* 2);

    }

    @Override
    protected void initRendering() {
        gl = GLFuncProviderFactory.getGLFuncProvider();
        InitModel();

        m_transformer.setMotionMode(NvCameraMotionType.FIRST_PERSON);
        m_transformer.setTranslation(0, -4, 0);

        full_screen_quad_vao_ = gl.glGenVertexArray();
    }

    /**
<p>The "real" initialization work, which is specific to our atmosphere model,
is done in the following method. It starts with the creation of an atmosphere
<code>Model</code> instance, with parameters corresponding to the Earth
atmosphere:
*/
    private void InitModel() {
        // Values from "Reference Solar Spectral Irradiance: ASTM G-173", ETR column
        // (see http://rredc.nrel.gov/solar/spectra/am1.5/ASTMG173/ASTMG173.html),
        // summed and averaged in each bin (e.g. the value for 360nm is the average
        // of the ASTM G-173 values for all wavelengths between 360 and 370nm).
        // Values in W.m^-2.
        final double kSolarIrradiance[/*48*/] = {
                1.11776, 1.14259, 1.01249, 1.14716, 1.72765, 1.73054, 1.6887, 1.61253,
                1.91198, 2.03474, 2.02042, 2.02212, 1.93377, 1.95809, 1.91686, 1.8298,
                1.8685, 1.8931, 1.85149, 1.8504, 1.8341, 1.8345, 1.8147, 1.78158, 1.7533,
                1.6965, 1.68194, 1.64654, 1.6048, 1.52143, 1.55622, 1.5113, 1.474, 1.4482,
                1.41018, 1.36775, 1.34188, 1.31429, 1.28303, 1.26758, 1.2367, 1.2082,
                1.18737, 1.14683, 1.12362, 1.1058, 1.07124, 1.04992
        };
        // Values from http://www.iup.uni-bremen.de/gruppen/molspec/databases/
        // referencespectra/o3spectra2011/index.html for 233K, summed and averaged in
        // each bin (e.g. the value for 360nm is the average of the original values
        // for all wavelengths between 360 and 370nm). Values in m^2.
        final double kOzoneCrossSection[/*48*/] = {
                1.18e-27, 2.182e-28, 2.818e-28, 6.636e-28, 1.527e-27, 2.763e-27, 5.52e-27,
                8.451e-27, 1.582e-26, 2.316e-26, 3.669e-26, 4.924e-26, 7.752e-26, 9.016e-26,
                1.48e-25, 1.602e-25, 2.139e-25, 2.755e-25, 3.091e-25, 3.5e-25, 4.266e-25,
                4.672e-25, 4.398e-25, 4.701e-25, 5.019e-25, 4.305e-25, 3.74e-25, 3.215e-25,
                2.662e-25, 2.238e-25, 1.852e-25, 1.473e-25, 1.209e-25, 9.423e-26, 7.455e-26,
                6.566e-26, 5.105e-26, 4.15e-26, 4.228e-26, 3.237e-26, 2.451e-26, 2.801e-26,
                2.534e-26, 1.624e-26, 1.465e-26, 2.078e-26, 1.383e-26, 7.105e-27
        };
        // From https://en.wikipedia.org/wiki/Dobson_unit, in molecules.m^-2.
        final double kDobsonUnit = 2.687e20;
        // Maximum number density of ozone molecules, in m^-3 (computed so at to get
        // 300 Dobson units of ozone - for this we divide 300 DU by the integral of
        // the ozone density profile defined below, which is equal to 15km).
        final double kMaxOzoneNumberDensity = 300.0 * kDobsonUnit / 15000.0;
        // Wavelength independent solar irradiance "spectrum" (not physically
        // realistic, but was used in the original implementation).
        final double kConstantSolarIrradiance = 1.5;
        final double kBottomRadius = 6360000.0;
        final double kTopRadius = 6420000.0;
        final double kRayleigh = 1.24062e-6;
        final double kRayleighScaleHeight = 8000.0;
        final double kMieScaleHeight = 1200.0;
        final double kMieAngstromAlpha = 0.0;
        final double kMieAngstromBeta = 5.328e-3;
        final double kMieSingleScatteringAlbedo = 0.9;
        final double kMiePhaseFunctionG = 0.8;
        final double kGroundAlbedo = 0.1;
        final double max_sun_zenith_angle = (use_half_precision_ ? 102.0 : 120.0) / 180.0 * Math.PI;

        DensityProfileLayer rayleigh_layer = new DensityProfileLayer(0.0, 1.0, -1.0 / kRayleighScaleHeight, 0.0, 0.0);
        DensityProfileLayer mie_layer = new DensityProfileLayer(0.0, 1.0, -1.0 / kMieScaleHeight, 0.0, 0.0);
        // Density profile increasing linearly from 0 to 1 between 10 and 25km, and
        // decreasing linearly from 1 to 0 between 25 and 40km. This is an approximate
        // profile from http://www.kln.ac.lk/science/Chemistry/Teaching_Resources/
        // Documents/Introduction%20to%20atmospheric%20chemistry.pdf (page 10).
        List<DensityProfileLayer> ozone_density = new ArrayList<>();
        ozone_density.add(new DensityProfileLayer(25000.0, 0.0, 0.0, 1.0 / 15000.0, -2.0 / 3.0));
        ozone_density.add(new DensityProfileLayer(0.0, 0.0, 0.0, -1.0 / 15000.0, 8.0 / 3.0));

        final StackDouble wavelengths = new StackDouble();
        final StackDouble solar_irradiance = new StackDouble();
        final StackDouble rayleigh_scattering = new StackDouble();
        final StackDouble mie_scattering = new StackDouble();
        final StackDouble mie_extinction = new StackDouble();
        final StackDouble absorption_extinction = new StackDouble();
        final StackDouble ground_albedo = new StackDouble();
        for (int l = Model.kLambdaMin; l <= Model.kLambdaMax; l += 10) {
            double lambda = (l) * 1e-3;  // micro-meters
            double mie = kMieAngstromBeta / kMieScaleHeight * Math.pow(lambda, -kMieAngstromAlpha);
            wavelengths.push(l);
            if (use_constant_solar_spectrum_) {
                solar_irradiance.push(kConstantSolarIrradiance);
            } else {
                solar_irradiance.push(kSolarIrradiance[(l - Model.kLambdaMin) / 10]);
            }
            rayleigh_scattering.push(kRayleigh * Math.pow(lambda, -4));
            mie_scattering.push(mie * kMieSingleScatteringAlbedo);
            mie_extinction.push(mie);
            absorption_extinction.push(use_ozone_ ? kMaxOzoneNumberDensity * kOzoneCrossSection[(l - Model.kLambdaMin) / 10] : 0.0);
            ground_albedo.push(kGroundAlbedo);
        }

        List<DensityProfileLayer> rayleighs =new ArrayList<>();
        List<DensityProfileLayer> mies =new ArrayList<>();
        rayleighs.add(new DensityProfileLayer());
        rayleighs.add(rayleigh_layer);

        mies.add(new DensityProfileLayer());
        mies.add(mie_layer);

        model_=new Model(wavelengths, solar_irradiance, kSunAngularRadius,
                kBottomRadius, kTopRadius, rayleighs, rayleigh_scattering,
                mies, mie_scattering, mie_extinction, kMiePhaseFunctionG,
                ozone_density, absorption_extinction, ground_albedo, max_sun_zenith_angle,
                kLengthUnitInMeters, use_luminance_ == Luminance.PRECOMPUTED ? 15 : 3,
                use_combined_textures_, use_half_precision_);
        model_ .Init();

        white_point.set(1,1,1);
        if (do_white_balance_) {
            Model.ConvertSpectrumToLinearSrgb (wavelengths, solar_irradiance, white_point);
            float white_point_mean = (white_point.x + white_point.y + white_point.z) / 3.0f;
            /*white_point_r /= white_point;
            white_point_g /= white_point;
            white_point_b /= white_point;*/

            white_point.scale(1.0f/white_point_mean);
        }
        /*glUniform3f(glGetUniformLocation(program_, "white_point"),
                (float) white_point_r, (float) white_point_g, (float) white_point_b);
        glUniform3f(glGetUniformLocation(program_, "earth_center"),
                0.0, 0.0, (float) (-kBottomRadius / kLengthUnitInMeters));
        glUniform2f(glGetUniformLocation(program_, "sun_size"),
                (float) tan(kSunAngularRadius),
                (float) cos(kSunAngularRadius));*/

        earth_center.set(0,0, (float) (-kBottomRadius / kLengthUnitInMeters));
        sun_size.set((float) Math.tan(kSunAngularRadius), (float) Math.cos(kSunAngularRadius));

        // This sets 'view_from_clip', which only depends on the window size.
//        HandleReshapeEvent(glutGet(GLUT_WINDOW_WIDTH), glutGet(GLUT_WINDOW_HEIGHT));
    }

    /**
<p>The scene rendering method simply sets the uniforms related to the camera
position and to the Sun direction, and then draws a full screen quad (and
optionally a help screen).
*/
    @Override
    public void display() {
        if(program_ == null || use_luminance_ != curr_lumi_mode){
            use_luminance_ = curr_lumi_mode;

            if(program_!=null)
                program_.disable();

            List<Macro> macroList = new ArrayList<>(2);
            if(curr_lumi_mode != Luminance.PRECOMPUTED)
            {
                macroList.add(new Macro("RADIANCE_API_ENABLED", 1) );
            }
            Macro[] macros= null;
            if(curr_lumi_mode != Luminance.NONE){
                macroList.add(new Macro("USE_LUMINANCE", 1) );
            }

            macros = macroList.toArray(new Macro[macroList.size()]);
            final String shaderPath = "labs/Atmosphere/shaders/";
            program_ = GLSLProgram.createProgram(shaderPath+"DemoVS.vert", shaderPath+"DemoPS.frag", macros);
        }

        program_.enable();

        gl.glBindFramebuffer(GLenum.GL_FRAMEBUFFER, 0);
        gl.glViewport(0,0, getGLContext().width(), getGLContext().height());
        gl.glClear(GLenum.GL_COLOR_BUFFER_BIT|GLenum.GL_DEPTH_BUFFER_BIT);

        m_transformer.getModelViewMat(mView);

        Matrix4f.decompseRigidMatrix(mView, camera, null, null);
        GLSLUtil.setFloat3(program_, "camera", camera);

        Matrix4f.invertRigid(mView, mTemp);
        GLSLUtil.setMat4(program_, "model_from_view", mTemp);

        Matrix4f.invert(mProj, mTemp);
        GLSLUtil.setMat4(program_, "view_from_clip", mTemp);

        sun_direction.x = (float)(Math.cos(sun_azimuth_angle_radians_) * Math.sin(sun_zenith_angle_radians_));
        sun_direction.y = (float) (Math.sin(sun_azimuth_angle_radians_) * Math.sin(sun_zenith_angle_radians_));
        sun_direction.z = (float)Math.cos(sun_zenith_angle_radians_);

        GLSLUtil.setFloat3(program_, "white_point", white_point);
        GLSLUtil.setFloat3(program_, "earth_center", earth_center);
        GLSLUtil.setFloat3(program_, "sun_direction", sun_direction);
        GLSLUtil.setFloat2(program_, "sun_size", sun_size);
        GLSLUtil.setFloat(program_, "exposure", use_luminance_ != Luminance.NONE ? exposure_ * 1e-5f: exposure_);

        model_.bindRenderingResources();

        gl.glBindVertexArray(full_screen_quad_vao_);
        gl.glDrawArrays(GLenum.GL_TRIANGLES, 0, 3);
        gl.glBindVertexArray(0);

        model_.unbindRenderingResources();

        program_.setName("Demo Program");
        program_.printOnce();
    }

    @Override
    protected void reshape(int width, int height) {
        if(width <= 0 || height <=0)
            return;

        Matrix4f.perspective(50, (float)width/height, 0.1f, 1000.f, mProj);
    }
}
