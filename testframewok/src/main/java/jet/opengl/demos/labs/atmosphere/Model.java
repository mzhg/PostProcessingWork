/**
 * Copyright (c) 2017 Eric Bruneton
 * All rights reserved.
 * <p>
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 * 3. Neither the name of the copyright holders nor the names of its
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 * <p>
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF
 * THE POSSIBILITY OF SUCH DAMAGE.
 */

package jet.opengl.demos.labs.atmosphere;

import org.lwjgl.util.vector.Vector3f;

import java.util.List;

import jet.opengl.postprocessing.common.GLFuncProvider;
import jet.opengl.postprocessing.common.GLFuncProviderFactory;
import jet.opengl.postprocessing.common.GLenum;
import jet.opengl.postprocessing.shader.GLSLProgram;
import jet.opengl.postprocessing.texture.RenderTargets;
import jet.opengl.postprocessing.texture.SamplerDesc;
import jet.opengl.postprocessing.texture.SamplerUtils;
import jet.opengl.postprocessing.texture.Texture2D;
import jet.opengl.postprocessing.texture.Texture2DDesc;
import jet.opengl.postprocessing.texture.Texture3D;
import jet.opengl.postprocessing.texture.Texture3DDesc;
import jet.opengl.postprocessing.texture.TextureGL;
import jet.opengl.postprocessing.texture.TextureUtils;
import jet.opengl.postprocessing.util.Numeric;
import jet.opengl.postprocessing.util.StackDouble;

/**<h2>atmosphere/model.h</h2>

 <p>This file defines the API to use our atmosphere model in OpenGL applications.
 To use it:
 <ul>
 <li>create a <code>Model</code> instance with the desired atmosphere
 parameters.</li>
 <li>call <code>Init</code> to precompute the atmosphere textures,</li>
 <li>link <code>GetShader</code> with your shaders that need access to the
 atmosphere shading functions.</li>
 <li>for each GLSL program linked with <code>GetShader</code>, call
 <code>SetProgramUniforms</code> to bind the precomputed textures to this
 program (usually at each frame).</li>
 <li>delete your <code>Model</code> when you no longer need its shader and
 precomputed textures (the destructor deletes these resources).</li>
 </ul>

 <p>The shader returned by <code>GetShader</code> provides the following
 functions (that you need to forward declare in your own shaders to be able to
 compile them separately):

 <pre class="prettyprint">
 // Returns the radiance of the Sun, outside the atmosphere.
 vec3 GetSolarRadiance();

 // Returns the sky radiance along the segment from 'camera' to the nearest
 // atmosphere boundary in direction 'view_ray', as well as the transmittance
 // along this segment.
 vec3 GetSkyRadiance(vec3 camera, vec3 view_ray, double shadow_length,
 vec3 sun_direction, out vec3 transmittance);

 // Returns the sky radiance along the segment from 'camera' to 'p', as well as
 // the transmittance along this segment.
 vec3 GetSkyRadianceToPoint(vec3 camera, vec3 p, double shadow_length,
 vec3 sun_direction, out vec3 transmittance);

 // Returns the sun and sky irradiance received on a surface patch located at 'p'
 // and whose normal vector is 'normal'.
 vec3 GetSunAndSkyIrradiance(vec3 p, vec3 normal, vec3 sun_direction,
 out vec3 sky_irradiance);

 // Returns the luminance of the Sun, outside the atmosphere.
 vec3 GetSolarLuminance();

 // Returns the sky luminance along the segment from 'camera' to the nearest
 // atmosphere boundary in direction 'view_ray', as well as the transmittance
 // along this segment.
 vec3 GetSkyLuminance(vec3 camera, vec3 view_ray, double shadow_length,
 vec3 sun_direction, out vec3 transmittance);

 // Returns the sky luminance along the segment from 'camera' to 'p', as well as
 // the transmittance along this segment.
 vec3 GetSkyLuminanceToPoint(vec3 camera, vec3 p, double shadow_length,
 vec3 sun_direction, out vec3 transmittance);

 // Returns the sun and sky illuminance received on a surface patch located at
 // 'p' and whose normal vector is 'normal'.
 vec3 GetSunAndSkyIlluminance(vec3 p, vec3 normal, vec3 sun_direction,
 out vec3 sky_illuminance);
 </pre>

 <p>where
 <ul>
 <li><code>camera</code> and <code>p</code> must be expressed in a reference
 frame where the planet center is at the origin, and measured in the unit passed
 to the constructor's <code>length_unit_in_meters</code> argument.
 <code>camera</code> can be in space, but <code>p</code> must be inside the
 atmosphere,</li>
 <li><code>view_ray</code>, <code>sun_direction</code> and <code>normal</code>
 are unit direction vectors expressed in the same reference frame (with
 <code>sun_direction</code> pointing <i>towards</i> the Sun),</li>
 <li><code>shadow_length</code> is the length along the segment which is in
 shadow, measured in the unit passed to the constructor's
 <code>length_unit_in_meters</code> argument.</li>
 </ul>

 <p>and where
 <ul>
 <li>the first 4 functions return spectral radiance and irradiance values
 (in $W.m^{-2}.sr^{-1}.nm^{-1}$ and $W.m^{-2}.nm^{-1}$), at the 3 wavelengths
 <code>kLambdaR</code>, <code>kLambdaG</code>, <code>kLambdaB</code> (in this
 order),</li>
 <li>the other functions return luminance and illuminance values (in
 $cd.m^{-2}$ and $lx$) in linear <a href="https://en.wikipedia.org/wiki/SRGB">
 sRGB</a> space (i.e. before adjustements for gamma correction),</li>
 <li>all the functions return the (unitless) transmittance of the atmosphere
 along the specified segment at the 3 wavelengths <code>kLambdaR</code>,
 <code>kLambdaG</code>, <code>kLambdaB</code> (in this order).</li>
 </ul>

 <p><b>Note</b> The precomputed atmosphere textures can store either irradiance
 or illuminance values (see the <code>num_precomputed_wavelengths</code>
 parameter):
 <ul>
 <li>when using irradiance values, the RGB channels of these textures contain
 spectral irradiance values, in $W.m^{-2}.nm^{-1}$, at the 3 wavelengths
 <code>kLambdaR</code>, <code>kLambdaG</code>, <code>kLambdaB</code> (in this
 order). The API functions returning radiance values return these precomputed
 values (times the phase functions), while the API functions returning
 luminance values use the approximation described in
 <a href="https://arxiv.org/pdf/1612.04336.pdf">A Qualitative and Quantitative
 Evaluation of 8 Clear Sky Models</a>, section 14.3, to convert 3 radiance
 values to linear sRGB luminance values.</li>
 <li>when using illuminance values, the RGB channels of these textures contain
 illuminance values, in $lx$, in linear sRGB space. These illuminance values
 are precomputed as described in
 <a href="http://www.oskee.wz.cz/stranka/uploads/SCCG10ElekKmoch.pdf">Real-time
 Spectral Scattering in Large-scale Natural Participating Media</a>, section
 4.4 (i.e. <code>num_precomputed_wavelengths</code> irradiance values are
 precomputed, and then converted to sRGB via a numerical integration of this
 spectrum with the CIE color matching functions). The API functions returning
 luminance values return these precomputed values (times the phase functions),
 while <i>the API functions returning radiance values are not provided</i>.
 </li>
 </ul>

 <p>The concrete API definition is the following:
 */
final class Model implements Constant{

    static final float kLambdaR = 680.0f;
    static final float kLambdaG = 550.0f;
    static final float kLambdaB = 440.0f;

    private int num_precomputed_wavelengths_;
    private boolean half_precision_;
    private boolean rgb_format_supported_;
    //    std::function<std::string(const vec3&)> glsl_header_factory_;
    private Texture2D transmittance_texture_;
    private Texture3D scattering_texture_;
    private Texture3D optional_single_mie_scattering_texture_;
    private Texture2D irradiance_texture_;

    private GLSLProgram atmosphere_shader_;
    private int full_screen_quad_vao_;
    private int linearSampler;

    private GLFuncProvider gl;

    /**
     * Constructor for Atmosphere Model
     * @param wavelengths The wavelength values, in nanometers, and sorted in increasing order, for which the solar_irradiance, rayleigh_scattering, mie_scattering,
     *                mie_extinction and ground_albedo samples are provided. If your shaders use luminance values (as opposed to radiance values, see above), use a
     *                large number of wavelengths (e.g. between 15 and 50) to get accurate results (this number of wavelengths has absolutely no impact on the
     *                shader performance).
     * @param solar_irradiance The solar irradiance at the top of the atmosphere, in W/m^2/nm. This array must have the same size as the wavelengths parameter.
     * @param sun_angular_radius The sun's angular radius, in radians. Warning: the implementation uses approximations that are valid only if this value is smaller than 0.1.
     * @param bottom_radius The distance between the planet center and the bottom of the atmosphere,in m.
     * @param top_radius The distance between the planet center and the top of the atmosphere,in m.
     * @param rayleigh_density   The density profile of air molecules, i.e. a function from altitude to
     *             dimensionless values between 0 (null density) and 1 (maximum density).
     *             Layers must be sorted from bottom to top. The width of the last layer is
     *             ignored, i.e. it always extend to the top atmosphere boundary. At most 2
     *             layers can be specified.
     * @param rayleigh_scattering  The scattering coefficient of air molecules at the altitude where their
     *             density is maximum (usually the bottom of the atmosphere), as a function
     *             of wavelength, in m^-1. The scattering coefficient at altitude h is equal
     *             to 'rayleigh_scattering' times 'rayleigh_density' at this altitude. This
     *             vector must have the same size as the wavelengths parameter.
     * @param mie_density  The density profile of aerosols, i.e. a function from altitude to
     *             dimensionless values between 0 (null density) and 1 (maximum density).
     *             Layers must be sorted from bottom to top. The width of the last layer is
     *             ignored, i.e. it always extend to the top atmosphere boundary. At most 2
     *             layers can be specified.
     * @param mie_scattering  The scattering coefficient of aerosols at the altitude where their
     *             density is maximum (usually the bottom of the atmosphere), as a function
     *             of wavelength, in m^-1. The scattering coefficient at altitude h is equal
     *             to 'mie_scattering' times 'mie_density' at this altitude. This vector
     *             must have the same size as the wavelengths parameter.
     * @param mie_extinction   The extinction coefficient of aerosols at the altitude where their
     *             density is maximum (usually the bottom of the atmosphere), as a function
     *             of wavelength, in m^-1. The extinction coefficient at altitude h is equal
     *             to 'mie_extinction' times 'mie_density' at this altitude. This vector
     *             must have the same size as the wavelengths parameter.
     * @param mie_phase_function_g  The asymetry parameter for the Cornette-Shanks phase function for the aerosols.
     * @param absorption_density   The density profile of air molecules that absorb light (e.g. ozone), i.e.
     *             a function from altitude to dimensionless values between 0 (null density)
     *             and 1 (maximum density). Layers must be sorted from bottom to top. The
     *             width of the last layer is ignored, i.e. it always extend to the top
     *             atmosphere boundary. At most 2 layers can be specified.
     * @param absorption_extinction  The extinction coefficient of molecules that absorb light (e.g. ozone) at
     *             the altitude where their density is maximum, as a function of wavelength,
     *             in m^-1. The extinction coefficient at altitude h is equal to
     *             'absorption_extinction' times 'absorption_density' at this altitude. This
     *             vector must have the same size as the wavelengths parameter.
     * @param ground_albedo  The average albedo of the ground, as a function of wavelength. This vector must have the same size as the wavelengths parameter.
     * @param max_sun_zenith_angle  The maximum Sun zenith angle for which atmospheric scattering must be
     *             precomputed, in radians (for maximum precision, use the smallest Sun
     *             zenith angle yielding negligible sky light radiance values. For instance,
     *             for the Earth case, 102 degrees is a good choice for most cases (120
     *             degrees is necessary for very high exposure values).
     * @param length_unit_in_meters   The length unit used in your shaders and meshes. This is the length unit which must be used when calling the atmosphere model shader functions.
     * @param num_precomputed_wavelengths The number of wavelengths for which atmospheric scattering must be
     *             precomputed (the temporary GPU memory used during precomputations, and
     *             the GPU memory used by the precomputed results, is independent of this
     *             number, but the <i>precomputation time is directly proportional to this
     *             number</i>):
     *             - if this number is less than or equal to 3, scattering is precomputed
     *             for 3 wavelengths, and stored as irradiance values. Then both the
     *             radiance-based and the luminance-based API functions are provided (see
     *             the above note).
     *             - otherwise, scattering is precomputed for this number of wavelengths
     *             (rounded up to a multiple of 3), integrated with the CIE color matching
     *             functions, and stored as illuminance values. Then only the
     *             luminance-based API functions are provided (see the above note).
     * @param combine_scattering_textures  Whether to pack the (red component of the) single Mie scattering with the
     *             Rayleigh and multiple scattering in a single texture, or to store the
     *             (3 components of the) single Mie scattering in a separate texture.
     * @param half_precision  Whether to use half precision floats (16 bits) or single precision floats
     *             (32 bits) for the precomputed textures. Half precision is sufficient for
     *             most cases, except for very high exposure values.
     */
    Model(
            StackDouble wavelengths,
            StackDouble solar_irradiance,
            double sun_angular_radius,
            double bottom_radius,
            double top_radius,
            List<DensityProfileLayer> rayleigh_density,
            StackDouble rayleigh_scattering,
            List<DensityProfileLayer> mie_density,
            StackDouble mie_scattering,
            StackDouble mie_extinction,
            double mie_phase_function_g,
            List<DensityProfileLayer> absorption_density,
            StackDouble absorption_extinction,
            StackDouble ground_albedo,
            double max_sun_zenith_angle,
            double length_unit_in_meters,
            int num_precomputed_wavelengths,
            boolean combine_scattering_textures,
            boolean half_precision) {
        num_precomputed_wavelengths_ = num_precomputed_wavelengths;
        half_precision_ = (half_precision);
        rgb_format_supported_ = true;

        // Allocate the precomputed textures, but don't precompute them yet.
        SamplerDesc samplerDesc = new SamplerDesc();
        linearSampler = SamplerUtils.createSampler(samplerDesc);

        transmittance_texture_ = NewTexture2d(
                TRANSMITTANCE_TEXTURE_WIDTH, TRANSMITTANCE_TEXTURE_HEIGHT);
        scattering_texture_ = NewTexture3d(
                SCATTERING_TEXTURE_WIDTH,
                SCATTERING_TEXTURE_HEIGHT,
                SCATTERING_TEXTURE_DEPTH);
        if (combine_scattering_textures) {
            optional_single_mie_scattering_texture_ = null;
        } else {
            optional_single_mie_scattering_texture_ = NewTexture3d(
                    SCATTERING_TEXTURE_WIDTH,
                    SCATTERING_TEXTURE_HEIGHT,
                    SCATTERING_TEXTURE_DEPTH);
        }
        irradiance_texture_ = NewTexture2d(
                IRRADIANCE_TEXTURE_WIDTH, IRRADIANCE_TEXTURE_HEIGHT);
    }

    final void Init() {
        Init(4);
    }

    /**
<p>The Init method precomputes the atmosphere textures. It first allocates the
temporary resources it needs, then calls <code>Precompute</code> to do the
actual precomputations, and finally destroys the temporary resources.

<p>Note that there are two precomputation modes here, depending on whether we
want to store precomputed irradiance or illuminance values:
<ul>
  <li>In precomputed irradiance mode, we simply need to call
  <code>Precompute</code> with the 3 wavelengths for which we want to precompute
  irradiance, namely <code>kLambdaR</code>, <code>kLambdaG</code>,
  <code>kLambdaB</code> (with the identity matrix for
  <code>luminance_from_radiance</code>, since we don't want any conversion from
  radiance to luminance)</li>
  <li>In precomputed illuminance mode, we need to precompute irradiance for
  <code>num_precomputed_wavelengths_</code>, and then integrate the results,
  multiplied with the 3 CIE xyz color matching functions and the XYZ to sRGB
  matrix to get sRGB illuminance values.
  <p>A naive solution would be to allocate temporary textures for the
  intermediate irradiance results, then perform the integration from irradiance
  to illuminance and store the result in the final precomputed texture. In
  pseudo-code (and assuming one wavelength per texture instead of 3):
  <pre>
    create n temporary irradiance textures
    for each wavelength lambda in the n wavelengths:
       precompute irradiance at lambda into one of the temporary textures
    initializes the final illuminance texture with zeros
    for each wavelength lambda in the n wavelengths:
      accumulate in the final illuminance texture the product of the
      precomputed irradiance at lambda (read from the temporary textures)
      with the value of the 3 sRGB color matching functions at lambda (i.e.
      the product of the XYZ to sRGB matrix with the CIE xyz color matching
      functions).
  </pre>
  <p>However, this be would waste GPU memory. Instead, we can avoid allocating
  temporary irradiance textures, by merging the two above loops:
  <pre>
    for each wavelength lambda in the n wavelengths:
      accumulate in the final illuminance texture (or, for the first
      iteration, set this texture to) the product of the precomputed
      irradiance at lambda (computed on the fly) with the value of the 3
      sRGB color matching functions at lambda.
  </pre>
  <p>This is the method we use below, with 3 wavelengths per iteration instead
  of 1, using <code>Precompute</code> to compute 3 irradiances values per
  iteration, and <code>luminance_from_radiance</code> to multiply 3 irradiances
  with the values of the 3 sRGB color matching functions at 3 different
  wavelengths (yielding a 3x3 matrix).</li>
</ul>

<p>This yields the following implementation:
*/
    void Init( int num_scattering_orders) {
        gl = GLFuncProviderFactory.getGLFuncProvider();

        // The precomputations require temporary textures, in particular to store the
        // contribution of one scattering order, which is needed to compute the next
        // order of scattering (the final precomputed textures store the sum of all
        // the scattering orders). We allocate them here, and destroy them at the end
        // of this method.
        Texture2D delta_irradiance_texture = NewTexture2d(
                IRRADIANCE_TEXTURE_WIDTH, IRRADIANCE_TEXTURE_HEIGHT);
        Texture3D delta_rayleigh_scattering_texture = NewTexture3d(
                SCATTERING_TEXTURE_WIDTH,
                SCATTERING_TEXTURE_HEIGHT,
                SCATTERING_TEXTURE_DEPTH);
        Texture3D delta_mie_scattering_texture = NewTexture3d(
                SCATTERING_TEXTURE_WIDTH,
                SCATTERING_TEXTURE_HEIGHT,
                SCATTERING_TEXTURE_DEPTH);
        Texture3D delta_scattering_density_texture = NewTexture3d(
                SCATTERING_TEXTURE_WIDTH,
                SCATTERING_TEXTURE_HEIGHT,
                SCATTERING_TEXTURE_DEPTH);
        // delta_multiple_scattering_texture is only needed to compute scattering
        // order 3 or more, while delta_rayleigh_scattering_texture and
        // delta_mie_scattering_texture are only needed to compute double scattering.
        // Therefore, to save memory, we can store delta_rayleigh_scattering_texture
        // and delta_multiple_scattering_texture in the same GPU texture.
        Texture3D delta_multiple_scattering_texture = delta_rayleigh_scattering_texture;

        full_screen_quad_vao_ = gl.glGenVertexArray();

        // The precomputations also require a temporary framebuffer object, created
        // here (and destroyed at the end of this method).
        RenderTargets fbo = new RenderTargets();
        // The actual precomputations depend on whether we want to store precomputed
        // irradiance or illuminance values.
        if (num_precomputed_wavelengths_ <= 3) {
            float[] lambdas = {kLambdaR, kLambdaG, kLambdaB};
            float[] luminance_from_radiance = {1.0f, 0.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 0.0f, 1.0f};
            Precompute(fbo, delta_irradiance_texture, delta_rayleigh_scattering_texture,
                    delta_mie_scattering_texture, delta_scattering_density_texture,
                    delta_multiple_scattering_texture, lambdas, luminance_from_radiance,
                    false /* blend */, num_scattering_orders);
        } else {
            final float kLambdaMin = 360;
            final float kLambdaMax = 830;
            int num_iterations = (num_precomputed_wavelengths_ + 2) / 3;
            float dlambda = (kLambdaMax - kLambdaMin) / (3 * num_iterations);
            for (int i = 0; i < num_iterations; ++i) {
                float[] lambdas= {
                        kLambdaMin + (3 * i + 0.5f) * dlambda,
                        kLambdaMin + (3 * i + 1.5f) * dlambda,
                        kLambdaMin + (3 * i + 2.5f) * dlambda
                };

                float[] luminance_from_radiance = {
                        coeff(dlambda, lambdas[0], 0), coeff(dlambda, lambdas[1], 0), coeff(dlambda, lambdas[2], 0),
                        coeff(dlambda, lambdas[0], 1), coeff(dlambda, lambdas[1], 1), coeff(dlambda, lambdas[2], 1),
                        coeff(dlambda, lambdas[0], 2), coeff(dlambda, lambdas[1], 2), coeff(dlambda, lambdas[2], 2)
                };
                Precompute(fbo, delta_irradiance_texture,
                        delta_rayleigh_scattering_texture, delta_mie_scattering_texture,
                        delta_scattering_density_texture, delta_multiple_scattering_texture,
                        lambdas, luminance_from_radiance, i > 0 /* blend */,
                        num_scattering_orders);
            }

            // After the above iterations, the transmittance texture contains the
            // transmittance for the 3 wavelengths used at the last iteration. But we
            // want the transmittance at kLambdaR, kLambdaG, kLambdaB instead, so we
            // must recompute it here for these 3 wavelengths:
            /*std::string header = glsl_header_factory_({kLambdaR, kLambdaG, kLambdaB});  todo
            Program compute_transmittance(
                    kVertexShader, header + kComputeTransmittanceShader);
            glFramebufferTexture(
                    GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, transmittance_texture_, 0);
            glDrawBuffer(GL_COLOR_ATTACHMENT0);
            glViewport(0, 0, TRANSMITTANCE_TEXTURE_WIDTH, TRANSMITTANCE_TEXTURE_HEIGHT);
            compute_transmittance.Use();
            DrawQuad({}, full_screen_quad_vao_);*/
        }

        // Delete the temporary resources allocated at the begining of this method.
       /* glUseProgram(0);
        glBindFramebuffer(GL_FRAMEBUFFER, 0);
        glDeleteFramebuffers(1, &fbo);
        glDeleteTextures(1, &delta_scattering_density_texture);
        glDeleteTextures(1, &delta_mie_scattering_texture);
        glDeleteTextures(1, &delta_rayleigh_scattering_texture);
        glDeleteTextures(1, &delta_irradiance_texture);
        assert(glGetError() == 0);*/

        fbo.dispose();
        delta_scattering_density_texture.dispose();
        delta_mie_scattering_texture.dispose();
        delta_rayleigh_scattering_texture.dispose();
        delta_irradiance_texture.dispose();
    }

    GLSLProgram shader() {
        return atmosphere_shader_;
    }

    float coeff(double dlambda, double lambda, int component) {
        // Note that we don't include MAX_LUMINOUS_EFFICACY here, to avoid
        // artefacts due to too large values when using half precision on GPU.
        // We add this term back in kAtmosphereShader, via
        // SKY_SPECTRAL_RADIANCE_TO_LUMINANCE (see also the comments in the
        // Model constructor).
        double x = CIEUtils.CieColorMatchingFunctionTableValue(lambda, 1);
        double y = CIEUtils.CieColorMatchingFunctionTableValue(lambda, 2);
        double z = CIEUtils.CieColorMatchingFunctionTableValue(lambda, 3);
        return (float)((
                XYZ_TO_SRGB[component * 3] * x +
                        XYZ_TO_SRGB[component * 3 + 1] * y +
                        XYZ_TO_SRGB[component * 3 + 2] * z) * dlambda);
    };

    /**
<p>The <code>SetProgramUniforms</code> method is straightforward: it simply
binds the precomputed textures to the specified texture units, and then sets
the corresponding uniforms in the user provided program to the index of these
texture units.
*/
    void SetProgramUniforms(
            GLSLProgram program,
            int transmittance_texture_unit,
            int scattering_texture_unit,
            int irradiance_texture_unit,
            int single_mie_scattering_texture_unit) {
        gl.glActiveTexture(GLenum.GL_TEXTURE0 + transmittance_texture_unit);
        gl.glBindTexture(GLenum.GL_TEXTURE_2D, transmittance_texture_.getTexture());
        gl.glUniform1i(gl.glGetUniformLocation(program.getProgram(), "transmittance_texture"),
                transmittance_texture_unit);

        gl.glActiveTexture(GLenum.GL_TEXTURE0 + scattering_texture_unit);
        gl.glBindTexture(GLenum.GL_TEXTURE_3D, scattering_texture_.getTexture());
        gl.glUniform1i(gl.glGetUniformLocation(program.getProgram(), "scattering_texture"),
                scattering_texture_unit);

        gl.glActiveTexture(GLenum.GL_TEXTURE0 + irradiance_texture_unit);
        gl.glBindTexture(GLenum.GL_TEXTURE_2D, irradiance_texture_.getTexture());
        gl.glUniform1i(gl.glGetUniformLocation(program.getProgram(), "irradiance_texture"),
                irradiance_texture_unit);

        if (optional_single_mie_scattering_texture_ != null) {
            gl.glActiveTexture(GLenum.GL_TEXTURE0 + single_mie_scattering_texture_unit);
            gl.glBindTexture(GLenum.GL_TEXTURE_3D, optional_single_mie_scattering_texture_.getTexture());
            gl.glUniform1i(gl.glGetUniformLocation(program.getProgram(), "single_mie_scattering_texture"),
                    single_mie_scattering_texture_unit);
        }
    }

    // Utility method to convert a function of the wavelength to linear sRGB.
    // 'wavelengths' and 'spectrum' must have the same size. The integral of
    // 'spectrum' times each CIE_2_DEG_COLOR_MATCHING_FUNCTIONS (and times
    // MAX_LUMINOUS_EFFICACY) is computed to get XYZ values, which are then
    // converted to linear sRGB with the XYZ_TO_SRGB matrix.
    static void ConvertSpectrumToLinearSrgb(
            StackDouble wavelengths,
            StackDouble spectrum,
            Vector3f outRgb) {

    }

    /**
<p>and a function to draw a full screen quad in an offscreen framebuffer (with
blending separately enabled or disabled for each color attachment):
*/
    private final void DrawQuad(boolean[] enable_blend, int quad_vao) {
        for ( int i = 0; i < enable_blend.length; ++i) {
            if (enable_blend[i]) {
                gl.glEnablei(GLenum.GL_BLEND, i);
            }
        }

        gl.glBindVertexArray(quad_vao);
        gl.glDrawArrays(GLenum.GL_TRIANGLE_STRIP, 0, 4);
        gl.glBindVertexArray(0);

        for (int i = 0; i < enable_blend.length; ++i) {
            gl.glDisablei(GLenum.GL_BLEND, i);
        }
    }

    private void Precompute(
            RenderTargets fbo,
            Texture2D delta_irradiance_texture,
            Texture3D delta_rayleigh_scattering_texture,
            Texture3D delta_mie_scattering_texture,
            Texture3D delta_scattering_density_texture,
            Texture3D delta_multiple_scattering_texture,
            float[] lambdas,
            float[] luminance_from_radiance,
            boolean blend,
            int num_scattering_orders) {
        // The precomputations require specific GLSL programs, for each precomputation
        // step. We create and compile them here (they are automatically destroyed
        // when this method returns, via the Program destructor).
        /*std::string header = glsl_header_factory_(lambdas);
        Program compute_transmittance(
                kVertexShader, header + kComputeTransmittanceShader);
        Program compute_direct_irradiance(
                kVertexShader, header + kComputeDirectIrradianceShader);
        Program compute_single_scattering(kVertexShader, kGeometryShader,
                header + kComputeSingleScatteringShader);
        Program compute_scattering_density(kVertexShader, kGeometryShader,
                header + kComputeScatteringDensityShader);
        Program compute_indirect_irradiance(
                kVertexShader, header + kComputeIndirectIrradianceShader);
        Program compute_multiple_scattering(kVertexShader, kGeometryShader,
                header + kComputeMultipleScatteringShader);*/
        final String shaderPath = "labs/Atmosphere/shaders/";
        final String kVertexShader = "shader_libs/PostProcessingDefaultScreenSpaceVS.vert";
        GLSLProgram compute_transmittance = GLSLProgram.createProgram(kVertexShader, shaderPath+"ComputeTransmittancePS.frag", null);
        GLSLProgram compute_direct_irradiance = GLSLProgram.createProgram(kVertexShader, shaderPath+"ComputeDirectIrradiancePS.frag", null);
        GLSLProgram compute_single_scattering = GLSLProgram.createProgram(kVertexShader, shaderPath+"GemetryLayer.gemo",shaderPath+"ComputeSingleScatteringPS.frag", null);
        GLSLProgram compute_scattering_density = GLSLProgram.createProgram(kVertexShader, shaderPath+"GemetryLayer.gemo",shaderPath+"ComputeScatteringDensityPS.frag", null);
        GLSLProgram compute_indirect_irradiance = GLSLProgram.createProgram(kVertexShader, shaderPath+"ComputeIndirectIrradiancePS.frag", null);
        GLSLProgram compute_multiple_scattering = GLSLProgram.createProgram(kVertexShader, shaderPath+"GemetryLayer.gemo",shaderPath+"ComputeMultipleScattering.frag", null);

        gl.glBlendEquationSeparate(GLenum.GL_FUNC_ADD, GLenum.GL_FUNC_ADD);
        gl.glBlendFuncSeparate(GLenum.GL_ONE, GLenum.GL_ONE, GLenum.GL_ONE, GLenum.GL_ONE);

        // Compute the transmittance, and store it in transmittance_texture_.
        /*glFramebufferTexture(
                GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, transmittance_texture_, 0);
        glDrawBuffer(GL_COLOR_ATTACHMENT0);*/
        fbo.bind();
        fbo.setRenderTexture(transmittance_texture_, null);

        gl.glViewport(0, 0, TRANSMITTANCE_TEXTURE_WIDTH, TRANSMITTANCE_TEXTURE_HEIGHT);
        compute_transmittance.enable();
//        DrawQuad({}, full_screen_quad_vao_);
        gl.glBindVertexArray(full_screen_quad_vao_);
        gl.glDrawArrays(GLenum.GL_TRIANGLES, 0, 3);
        gl.glBindVertexArray(0);

        // Compute the direct irradiance, store it in delta_irradiance_texture and,
        // depending on 'blend', either initialize irradiance_texture_ with zeros or
        // leave it unchanged (we don't want the direct irradiance in
        // irradiance_texture_, but only the irradiance from the sky).
        /*glFramebufferTexture(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0,
                delta_irradiance_texture, 0);
        glFramebufferTexture(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT1,
                irradiance_texture_, 0);
        glDrawBuffers(2, kDrawBuffers);*/

        fbo.setRenderTextures(new TextureGL[]{delta_irradiance_texture, irradiance_texture_}, null);
        gl.glViewport(0, 0, IRRADIANCE_TEXTURE_WIDTH, IRRADIANCE_TEXTURE_HEIGHT);
        compute_direct_irradiance.enable();
//        compute_direct_irradiance.BindTexture2d(
//                "transmittance_texture", transmittance_texture_, 0);  todo texture
//        DrawQuad({false, blend}, full_screen_quad_vao_);
        gl.glBindVertexArray(full_screen_quad_vao_);
        gl.glDrawArrays(GLenum.GL_TRIANGLES, 0, 3);
        gl.glBindVertexArray(0);


        // Compute the rayleigh and mie single scattering, store them in
        // delta_rayleigh_scattering_texture and delta_mie_scattering_texture, and
        // either store them or accumulate them in scattering_texture_ and
        // optional_single_mie_scattering_texture_.
        /*glFramebufferTexture(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0,
                delta_rayleigh_scattering_texture, 0);
        glFramebufferTexture(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT1,
                delta_mie_scattering_texture, 0);
        glFramebufferTexture(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT2,
                scattering_texture_, 0);*/
        if (optional_single_mie_scattering_texture_ != null) {
            /*glFramebufferTexture(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT3,
                    optional_single_mie_scattering_texture_, 0);
            glDrawBuffers(4, kDrawBuffers);*/
            fbo.setRenderTextures(new TextureGL[]{delta_rayleigh_scattering_texture, delta_mie_scattering_texture,scattering_texture_,optional_single_mie_scattering_texture_}, null);
        } else {
//            glDrawBuffers(3, kDrawBuffers);
            fbo.setRenderTextures(new TextureGL[]{delta_rayleigh_scattering_texture, delta_mie_scattering_texture,scattering_texture_}, null);
        }
        gl.glViewport(0, 0, SCATTERING_TEXTURE_WIDTH, SCATTERING_TEXTURE_HEIGHT);
        compute_single_scattering.enable();
        /*compute_single_scattering.BindMat3(  todo uniform and textures
                "luminance_from_radiance", luminance_from_radiance);
        compute_single_scattering.BindTexture2d(
                "transmittance_texture", transmittance_texture_, 0);*/

        final boolean[] blendAdd= {false, false, blend, blend};
        for (int layer = 0; layer < SCATTERING_TEXTURE_DEPTH; ++layer) {
//            compute_single_scattering.BindInt("layer", layer);  todo uniform
            DrawQuad(blendAdd, full_screen_quad_vao_);
        }

        // Compute the 2nd, 3rd and 4th order of scattering, in sequence.
        for (int scattering_order = 2; scattering_order <= num_scattering_orders; ++scattering_order) {
            // Compute the scattering density, and store it in
            // delta_scattering_density_texture.
            /*glFramebufferTexture(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0,
                    delta_scattering_density_texture, 0);
            glFramebufferTexture(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT1, 0, 0);
            glFramebufferTexture(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT2, 0, 0);
            glFramebufferTexture(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT3, 0, 0);
            glDrawBuffer(GL_COLOR_ATTACHMENT0);*/
            fbo.setRenderTexture(delta_scattering_density_texture, null);
            gl.glViewport(0, 0, SCATTERING_TEXTURE_WIDTH, SCATTERING_TEXTURE_HEIGHT);
            compute_scattering_density.enable();
            /*compute_scattering_density.BindTexture2d(  todo uniform
                    "transmittance_texture", transmittance_texture_, 0);
            compute_scattering_density.BindTexture3d(
                    "single_rayleigh_scattering_texture",
                    delta_rayleigh_scattering_texture,
                    1);
            compute_scattering_density.BindTexture3d(
                    "single_mie_scattering_texture", delta_mie_scattering_texture, 2);
            compute_scattering_density.BindTexture3d(
                    "multiple_scattering_texture", delta_multiple_scattering_texture, 3);
            compute_scattering_density.BindTexture2d(
                    "irradiance_texture", delta_irradiance_texture, 4);
            compute_scattering_density.BindInt("scattering_order", scattering_order);*/
            for (int layer = 0; layer < SCATTERING_TEXTURE_DEPTH; ++layer) {
//                compute_scattering_density.BindInt("layer", layer); todo uniform
                DrawQuad(Numeric.EMPTY_BOOL, full_screen_quad_vao_);
            }

            // Compute the indirect irradiance, store it in delta_irradiance_texture and
            // accumulate it in irradiance_texture_.
            /*glFramebufferTexture(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0,
                    delta_irradiance_texture, 0);
            glFramebufferTexture(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT1,
                    irradiance_texture_, 0);
            glDrawBuffers(2, kDrawBuffers);*/
            fbo.setRenderTextures(new TextureGL[]{delta_irradiance_texture, irradiance_texture_}, null);
            gl.glViewport(0, 0, IRRADIANCE_TEXTURE_WIDTH, IRRADIANCE_TEXTURE_HEIGHT);
            compute_indirect_irradiance.enable();
            /*compute_indirect_irradiance.BindMat3(  todo
                    "luminance_from_radiance", luminance_from_radiance);
            compute_indirect_irradiance.BindTexture3d(
                    "single_rayleigh_scattering_texture",
                    delta_rayleigh_scattering_texture,
                    0);
            compute_indirect_irradiance.BindTexture3d(
                    "single_mie_scattering_texture", delta_mie_scattering_texture, 1);
            compute_indirect_irradiance.BindTexture3d(
                    "multiple_scattering_texture", delta_multiple_scattering_texture, 2);
            compute_indirect_irradiance.BindInt("scattering_order",
                    scattering_order - 1);*/
            DrawQuad(new boolean[]{false, true}, full_screen_quad_vao_);

            // Compute the multiple scattering, store it in
            // delta_multiple_scattering_texture, and accumulate it in
            // scattering_texture_.
            /*glFramebufferTexture(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0,
                    delta_multiple_scattering_texture, 0);
            glFramebufferTexture(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT1,
                    scattering_texture_, 0);
            glDrawBuffers(2, kDrawBuffers);*/
            fbo.setRenderTextures(new TextureGL[]{delta_multiple_scattering_texture, scattering_texture_}, null);
            gl.glViewport(0, 0, SCATTERING_TEXTURE_WIDTH, SCATTERING_TEXTURE_HEIGHT);
            compute_multiple_scattering.enable();
            /*compute_multiple_scattering.BindMat3(  todo
                    "luminance_from_radiance", luminance_from_radiance);
            compute_multiple_scattering.BindTexture2d(
                    "transmittance_texture", transmittance_texture_, 0);
            compute_multiple_scattering.BindTexture3d(
                    "scattering_density_texture", delta_scattering_density_texture, 1);*/
            for (int layer = 0; layer < SCATTERING_TEXTURE_DEPTH; ++layer) {
//                compute_multiple_scattering.BindInt("layer", layer);  todo
                DrawQuad(new boolean[]{false, true}, full_screen_quad_vao_);
            }
        }
        /*glFramebufferTexture(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT1, 0, 0);
        glFramebufferTexture(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT2, 0, 0);
        glFramebufferTexture(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT3, 0, 0);*/
    }

    private Texture2D NewTexture2d(int width, int height){
        int format = rgb_format_supported_ ? (half_precision_?GLenum.GL_RGB16F:GLenum.GL_RGB16F):(half_precision_?GLenum.GL_RGBA32F:GLenum.GL_RGBA16F);

        Texture2DDesc desc2D = new Texture2DDesc(width, height, format);
        return TextureUtils.createTexture2D(desc2D, null);
    }

    private Texture3D NewTexture3d(int width, int height, int depth){
        int format = rgb_format_supported_ ? (half_precision_?GLenum.GL_RGB16F:GLenum.GL_RGB16F):(half_precision_?GLenum.GL_RGBA32F:GLenum.GL_RGBA16F);

        Texture3DDesc desc3D = new Texture3DDesc(width, height, depth, 1, format);
        return TextureUtils.createTexture3D(desc3D, null);
    }
}
