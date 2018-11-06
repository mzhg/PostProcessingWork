/*
This source is published under the following 3-clause BSD license.

Copyright (c) 2012 - 2013, Lukas Hosek and Alexander Wilkie
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:

    * Redistributions of source code must retain the above copyright
      notice, this list of conditions and the following disclaimer.
    * Redistributions in binary form must reproduce the above copyright
      notice, this list of conditions and the following disclaimer in the
      documentation and/or other materials provided with the distribution.
    * None of the names of the contributors may be used to endorse or promote
      products derived from this software without specific prior written
      permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS BE LIABLE FOR ANY
DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
(INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/


/* ============================================================================

This file is part of a sample implementation of the analytical skylight and
solar radiance models presented in the SIGGRAPH 2012 paper


           "An Analytic Model for Full Spectral Sky-Dome Radiance"

and the 2013 IEEE CG&A paper

       "Adding a Solar Radiance Function to the Hosek Skylight Model"

                                   both by

                       Lukas Hosek and Alexander Wilkie
                Charles University in Prague, Czech Republic


                        Version: 1.4a, February 22nd, 2013

Version history:

1.4a  February 22nd, 2013
      Removed unnecessary and counter-intuitive solar radius parameters
      from the interface of the colourspace sky dome initialisation functions.

1.4   February 11th, 2013
      Fixed a bug which caused the relative brightness of the solar disc
      and the sky dome to be off by a factor of about 6. The sun was too
      bright: this affected both normal and alien sun scenarios. The
      coefficients of the solar radiance function were changed to fix this.

1.3   January 21st, 2013 (not released to the public)
      Added support for solar discs that are not exactly the same size as
      the terrestrial sun. Also added support for suns with a different
      emission spectrum ("Alien World" functionality).

1.2a  December 18th, 2012
      Fixed a mistake and some inaccuracies in the solar radiance function
      explanations found in ArHosekSkyModel.h. The actual source code is
      unchanged compared to version 1.2.

1.2   December 17th, 2012
      Native RGB data and a solar radiance function that matches the turbidity
      conditions were added.

1.1   September 2012
      The coefficients of the spectral model are now scaled so that the output
      is given in physical units: W / (m^-2 * sr * nm). Also, the output of the
      XYZ model is now no longer scaled to the range [0...1]. Instead, it is
      the result of a simple conversion from spectral data via the CIE 2 degree
      standard observer matching functions. Therefore, after multiplication
      with 683 lm / W, the Y channel now corresponds to luminance in lm.

1.0   May 11th, 2012
      Initial release.


Please visit http://cgg.mff.cuni.cz/projects/SkylightModelling/ to check if
an updated version of this code has been published!

============================================================================ */


/*

This code is taken from ART, a rendering research system written in a
mix of C99 / Objective C. Since ART is not a small system and is intended to
be inter-operable with other libraries, and since C does not have namespaces,
the structures and functions in ART all have to have somewhat wordy
canonical names that begin with Ar.../ar..., like those seen in this example.

Usage information:
==================


Model initialisation
--------------------

A separate ArHosekSkyModelState has to be maintained for each spectral
band you want to use the model for. So in a renderer with 'num_channels'
bands, you would need something like

    ArHosekSkyModelState  * skymodel_state[num_channels];

You then have to allocate and initialise these states. In the following code
snippet, we assume that 'albedo' is defined as

    double  albedo[num_channels];

with a ground albedo value between [0,1] for each channel. The solar elevation
is given in radians.

    for ( unsigned int i = 0; i < num_channels; i++ )
        skymodel_state[i] =
            arhosekskymodelstate_alloc_init(
                  turbidity,
                  albedo[i],
                  solarElevation
                );

Note that starting with version 1.3, there is also a second initialisation
function which generates skydome states for different solar emission spectra
and solar radii: 'arhosekskymodelstate_alienworld_alloc_init()'.

See the notes about the "Alien World" functionality provided further down for a
discussion of the usefulness and limits of that second initalisation function.
Sky model states that have been initialised with either function behave in a
completely identical fashion during use and cleanup.

Using the model to generate skydome samples
-------------------------------------------

Generating a skydome radiance spectrum "skydome_result" for a given location
on the skydome determined via the angles theta and gamma works as follows:

    double  skydome_result[num_channels];

    for ( unsigned int i = 0; i < num_channels; i++ )
        skydome_result[i] =
            arhosekskymodel_radiance(
                skymodel_state[i],
                theta,
                gamma,
                channel_center[i]
              );

The variable "channel_center" is assumed to hold the channel center wavelengths
for each of the num_channels samples of the spectrum we are building.


Cleanup after use
-----------------

After rendering is complete, the content of the sky model states should be
disposed of via

        for ( unsigned int i = 0; i < num_channels; i++ )
            arhosekskymodelstate_free( skymodel_state[i] );


CIE XYZ Version of the Model
----------------------------

Usage of the CIE XYZ version of the model is exactly the same, except that
num_channels is of course always 3, and that ArHosekTristimSkyModelState and
arhosek_tristim_skymodel_radiance() have to be used instead of their spectral
counterparts.

RGB Version of the Model
------------------------

The RGB version uses sRGB primaries with a linear gamma ramp. The same set of
functions as with the XYZ data is used, except the model is initialized
by calling arhosek_rgb_skymodelstate_alloc_init.

Solar Radiance Function
-----------------------

For each position on the solar disc, this function returns the entire radiance
one sees - direct emission, as well as in-scattered light in the area of the
solar disc. The latter is important for low solar elevations - nice images of
the setting sun would not be possible without this. This is also the reason why
this function, just like the regular sky dome model evaluation function, needs
access to the sky dome data structures, as these provide information on
in-scattered radiance.

CAVEAT #1: in this release, this function is only provided in spectral form!
           RGB/XYZ versions to follow at a later date.

CAVEAT #2: (fixed from release 1.3 onwards)

CAVEAT #3: limb darkening renders the brightness of the solar disc
           inhomogeneous even for high solar elevations - only taking a single
           sample at the centre of the sun will yield an incorrect power
           estimate for the solar disc! Always take multiple random samples
           across the entire solar disc to estimate its power!

CAVEAT #4: in this version, the limb darkening calculations still use a fairly
           computationally expensive 5th order polynomial that was directly
           taken from astronomical literature. For the purposes of Computer
           Graphics, this is needlessly accurate, though, and will be replaced
           by a cheaper approximation in a future release.

"Alien World" functionality
---------------------------

The Hosek sky model can be used to roughly (!) predict the appearance of
outdoor scenes on earth-like planets, i.e. planets of a similar size and
atmospheric make-up. Since the spectral version of our model predicts sky dome
luminance patterns and solar radiance independently for each waveband, and
since the intensity of each waveband is solely dependent on the input radiance
from the star that the world in question is orbiting, it is trivial to re-scale
the wavebands to match a different star radiance.

At least in theory, the spectral version of the model has always been capable
of this sort of thing, and the actual sky dome and solar radiance models were
actually not altered at all in this release. All we did was to add some support
functionality for doing this more easily with the existing data and functions,
and to add some explanations.

Just use 'arhosekskymodelstate_alienworld_alloc_init()' to initialise the sky
model states (you will have to provide values for star temperature and solar
intensity compared to the terrestrial sun), and do everything else as you
did before.

CAVEAT #1: we assume the emission of the star that illuminates the alien world
           to be a perfect blackbody emission spectrum. This is never entirely
           realistic - real star emission spectra are considerably more complex
           than this, mainly due to absorption effects in the outer layers of
           stars. However, blackbody spectra are a reasonable first assumption
           in a usage scenario like this, where 100% accuracy is simply not
           necessary: for rendering purposes, there are likely no visible
           differences between a highly accurate solution based on a more
           involved simulation, and this approximation.

CAVEAT #2: we always use limb darkening data from our own sun to provide this
           "appearance feature", even for suns of strongly different
           temperature. Which is presumably not very realistic, but (as with
           the unaltered blackbody spectrum from caveat #1) probably not a bad
           first guess, either. If you need more accuracy than we provide here,
           please make inquiries with a friendly astro-physicst of your choice.

CAVEAT #3: you have to provide a value for the solar intensity of the star
           which illuminates the alien world. For this, please bear in mind
           that there is very likely a comparatively tight range of absolute
           solar irradiance values for which an earth-like planet with an
           atmosphere like the one we assume in our model can exist in the
           first place!

           Too much irradiance, and the atmosphere probably boils off into
           space, too little, it freezes. Which means that stars of
           considerably different emission colour than our sun will have to be
           fairly different in size from it, to still provide a reasonable and
           inhabitable amount of irradiance. Red stars will need to be much
           larger than our sun, while white or blue stars will have to be
           comparatively tiny. The initialisation function handles this and
           computes a plausible solar radius for a given emission spectrum. In
           terms of absolute radiometric values, you should probably not stray
           all too far from a solar intensity value of 1.0.

CAVEAT #4: although we now support different solar radii for the actual solar
           disc, the sky dome luminance patterns are *not* parameterised by
           this value - i.e. the patterns stay exactly the same for different
           solar radii! Which is of course not correct. But in our experience,
           solar discs up to several degrees in diameter (! - our own sun is
           half a degree across) do not cause the luminance patterns on the sky
           to change perceptibly. The reason we know this is that we initially
           used unrealistically large suns in our brute force path tracer, in
           order to improve convergence speeds (which in the beginning were
           abysmal). Later, we managed to do the reference renderings much
           faster even with realistically small suns, and found that there was
           no real difference in skydome appearance anyway.
           Conclusion: changing the solar radius should not be over-done, so
           close orbits around red supergiants are a no-no. But for the
           purposes of getting a fairly credible first impression of what an
           alien world with a reasonably sized sun would look like, what we are
           doing here is probably still o.k.

HINT #1:   if you want to model the sky of an earth-like planet that orbits
           a binary star, just super-impose two of these models with solar
           intensity of ~0.5 each, and closely spaced solar positions. Light is
           additive, after all. Tattooine, here we come... :-)

           P.S. according to Star Wars canon, Tattooine orbits a binary
           that is made up of a G and K class star, respectively.
           So ~5500K and ~4200K should be good first guesses for their
           temperature. Just in case you were wondering, after reading the
           previous paragraph.
*/
package jet.opengl.demos.labs.skylight;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.StringTokenizer;

import jet.opengl.postprocessing.util.CommentFilter;
import jet.opengl.postprocessing.util.DebugTools;
import jet.opengl.postprocessing.util.StackDouble;

public final class ArHosekSkyModel implements ArHosekSkyModelData_CIEXYZ, ArHosekSkyModelData_RGB{

    static double[] dataset320;
    static double[] datasetRad320;
    static double[] dataset360;
    static double[] datasetRad360;
    static double[] dataset400;
    static double[] datasetRad400;
    static double[] dataset440;
    static double[] datasetRad440;
    static double[] dataset480;
    static double[] datasetRad480;
    static double[] dataset520;
    static double[] datasetRad520;
    static double[] dataset560;
    static double[] datasetRad560;
    static double[] dataset600;
    static double[] datasetRad600;
    static double[] dataset640;
    static double[] datasetRad640;
    static double[] dataset680;
    static double[] datasetRad680;
    static double[] dataset720;
    static double[] datasetRad720;
    static double[] solarDataset320;
    static double[] solarDataset360;
    static double[] solarDataset400;
    static double[] solarDataset440;
    static double[] solarDataset480;
    static double[] solarDataset520;
    static double[] solarDataset560;
    static double[] solarDataset600;
    static double[] solarDataset640;
    static double[] solarDataset680;
    static double[] solarDataset720;
    static double[] limbDarkeningDataset320;
    static double[] limbDarkeningDataset360;
    static double[] limbDarkeningDataset400;
    static double[] limbDarkeningDataset440;
    static double[] limbDarkeningDataset480;
    static double[] limbDarkeningDataset520;
    static double[] limbDarkeningDataset560;
    static double[] limbDarkeningDataset600;
    static double[] limbDarkeningDataset640;
    static double[] limbDarkeningDataset680;
    static double[] limbDarkeningDataset720;

    static double[][] datasets;

    static double[][] datasetsRad;

    static double[][] solarDatasets;

    static double[][] limbDarkeningDatasets;

    static {
        load_static_data();

        datasets = new double[][]
                {
                        dataset320,
                        dataset360,
                        dataset400,
                        dataset440,
                        dataset480,
                        dataset520,
                        dataset560,
                        dataset600,
                        dataset640,
                        dataset680,
                        dataset720
                };

        datasetsRad = new double[][]
                {
                        datasetRad320,
                        datasetRad360,
                        datasetRad400,
                        datasetRad440,
                        datasetRad480,
                        datasetRad520,
                        datasetRad560,
                        datasetRad600,
                        datasetRad640,
                        datasetRad680,
                        datasetRad720
                };

        solarDatasets =new double[][]
                {
                        solarDataset320,
                        solarDataset360,
                        solarDataset400,
                        solarDataset440,
                        solarDataset480,
                        solarDataset520,
                        solarDataset560,
                        solarDataset600,
                        solarDataset640,
                        solarDataset680,
                        solarDataset720
                };

        limbDarkeningDatasets =new double[][]
                {
                        limbDarkeningDataset320,
                        limbDarkeningDataset360,
                        limbDarkeningDataset400,
                        limbDarkeningDataset440,
                        limbDarkeningDataset480,
                        limbDarkeningDataset520,
                        limbDarkeningDataset560,
                        limbDarkeningDataset600,
                        limbDarkeningDataset640,
                        limbDarkeningDataset680,
                        limbDarkeningDataset720
                };

        System.out.println("Static initlize doen!");
    }

    //   'blackbody_scaling_factor'
    //
    //   Fudge factor, computed in Mathematica, to scale the results of the
    //   following function to match the solar radiance spectrum used in the
    //   original simulation. The scaling is done so their integrals over the
    //   range from 380.0 to 720.0 nanometers match for a blackbody temperature
    //   of 5800 K.
    //   Which leaves the original spectrum being less bright overall than the 5.8k
    //   blackbody radiation curve if the ultra-violet part of the spectrum is
    //   also considered. But the visible brightness should be very similar.

    private static final double blackbody_scaling_factor = 3.19992 * 10E-11;
    private static final double TERRESTRIAL_SOLAR_RADIUS = ( ( 0.51 * 180.0 / Math.PI ) / 2.0 );

    public static ArHosekSkyModelState  arhosekskymodelstate_alloc_init(
        final double  solar_elevation,
        final double  atmospheric_turbidity,
        final double  ground_albedo
    ){
        ArHosekSkyModelState  state = new ArHosekSkyModelState();

        state.solar_radius = Math.toRadians( 0.51 ) / 2.0;
        state.turbidity    = atmospheric_turbidity;
        state.albedo       = ground_albedo;
        state.elevation    = solar_elevation;

        int wl;
        for( wl = 0; wl < 11; ++wl )
        {
            ArHosekSkyModel_CookConfiguration(
                    datasets[wl],
                    state.configs[wl],
                    atmospheric_turbidity,
                    ground_albedo,
                    solar_elevation
            );

            state.radiances[wl] =
                    ArHosekSkyModel_CookRadianceConfiguration(
                            datasetsRad[wl],
                            atmospheric_turbidity,
                            ground_albedo,
                            solar_elevation
                    );

            state.emission_correction_factor_sun[wl] = 1.0;
            state.emission_correction_factor_sky[wl] = 1.0;
        }

        return state;
    }

    //   'originalSolarRadianceTable[]'
    //
    //   The solar spectrum incident at the top of the atmosphere, as it was used
    //   in the brute force path tracer that generated the reference results the
    //   model was fitted to. We need this as the yardstick to compare any altered
    //   Blackbody emission spectra for alien world stars to.

    //   This is just the data from the Preetham paper, extended into the UV range.

    static final double originalSolarRadianceTable[] =
            {
                    7500.0,
                    12500.0,
                    21127.5,
                    26760.5,
                    30663.7,
                    27825.0,
                    25503.8,
                    25134.2,
                    23212.1,
                    21526.7,
                    19870.8
            };

    /* ----------------------------------------------------------------------------

    arhosekskymodelstate_alienworld_alloc_init() function
    -----------------------------------------------------

    Initialises an ArHosekSkyModelState struct for an "alien world" setting
    with a sun of a surface temperature given in 'kelvin'. The parameter
    'solar_intensity' controls the overall brightness of the sky, relative
    to the solar irradiance on Earth. A value of 1.0 yields a sky dome that
    is, on average over the wavelenghts covered in the model (!), as bright
    as the terrestrial sky in radiometric terms.

    Which means that the solar radius has to be adjusted, since the
    emissivity of a solar surface with a given temperature is more or less
    fixed. So hotter suns have to be smaller to be equally bright as the
    terrestrial sun, while cooler suns have to be larger. Note that there are
    limits to the validity of the luminance patterns of the underlying model:
    see the discussion above for more on this. In particular, an alien sun with
    a surface temperature of only 2000 Kelvin has to be very large if it is
    to be as bright as the terrestrial sun - so large that the luminance
    patterns are no longer a really good fit in that case.

    If you need information about the solar radius that the model computes
    for a given temperature (say, for light source sampling purposes), you
    have to query the 'solar_radius' variable of the sky model state returned
    *after* running this function.

---------------------------------------------------------------------------- */

    public static ArHosekSkyModelState  arhosekskymodelstate_alienworld_alloc_init(
            double  solar_elevation,
            double  solar_intensity,
            double  solar_surface_temperature_kelvin,
            double  atmospheric_turbidity,
            double  ground_albedo
    ){
        ArHosekSkyModelState   state = new ArHosekSkyModelState();

        state.turbidity    = atmospheric_turbidity;
        state.albedo       = ground_albedo;
        state.elevation    = solar_elevation;

        int wl;
        for( wl = 0; wl < 11; ++wl )
        {
            //   Basic init as for the normal scenario

            ArHosekSkyModel_CookConfiguration(
                    datasets[wl],
                    state.configs[wl],
                    atmospheric_turbidity,
                    ground_albedo,
                    solar_elevation
            );

            state.radiances[wl] =
                    ArHosekSkyModel_CookRadianceConfiguration(
                            datasetsRad[wl],
                            atmospheric_turbidity,
                            ground_albedo,
                            solar_elevation
                    );

            //   The wavelength of this band in nanometers

            double  owl = ( 320.0 + 40.0 * wl ) * 10E-10;

            //   The original intensity we just computed

            double  osr = originalSolarRadianceTable[wl];

            //   The intensity of a blackbody with the desired temperature
            //   The fudge factor described above is used to make sure the BB
            //   function matches the used radiance data reasonably well
            //   in magnitude.

            double  nsr =
                    art_blackbody_dd_value(solar_surface_temperature_kelvin, owl)
                            * blackbody_scaling_factor;

            //   Correction factor for this waveband is simply the ratio of
            //   the two.

            state.emission_correction_factor_sun[wl] = nsr / osr;
        }

        //   We then compute the average correction factor of all wavebands.

        //   Theoretically, some weighting to favour wavelengths human vision is
        //   more sensitive to could be introduced here - think V(lambda). But
        //   given that the whole effort is not *that* accurate to begin with (we
        //   are talking about the appearance of alien worlds, after all), simple
        //   averaging over the visible wavelenghts (! - this is why we start at
        //   WL #2, and only use 2-11) seems like a sane first approximation.

        double  correctionFactor = 0.0;

        int i;
        for ( i = 2; i < 11; i++ )
        {
            correctionFactor +=state.emission_correction_factor_sun[i];
        }

        //   This is the average ratio in emitted energy between our sun, and an
        //   equally large sun with the blackbody spectrum we requested.

        //   Division by 9 because we only used 9 of the 11 wavelengths for this
        //   (see above).

        double  ratio = correctionFactor / 9.0;

        //   This ratio is then used to determine the radius of the alien sun
        //   on the sky dome. The additional factor 'solar_intensity' can be used
        //   to make the alien sun brighter or dimmer compared to our sun.

        state.solar_radius =
                ( Math.sqrt( solar_intensity ) * TERRESTRIAL_SOLAR_RADIUS )
                        / Math.sqrt( ratio );

        //   Finally, we have to reduce the scaling factor of the sky by the
        //   ratio used to scale the solar disc size. The rationale behind this is
        //   that the scaling factors apply to the new blackbody spectrum, which
        //   can be more or less bright than the one our sun emits. However, we
        //   just scaled the size of the alien solar disc so it is roughly as
        //   bright (in terms of energy emitted) as the terrestrial sun. So the sky
        //   dome has to be reduced in brightness appropriately - but not in an
        //   uniform fashion across wavebands. If we did that, the sky colour would
        //   be wrong.

        for ( i = 0; i < 11; i++ )
        {
            state.emission_correction_factor_sky[i] =
                    solar_intensity
                            * state.emission_correction_factor_sun[i] / ratio;
        }

        return state;
    }

    private static double fmod(double x, double y){
        return x % y;
    }

    public static final double arhosekskymodel_radiance(
            ArHosekSkyModelState    state,
            double                  theta,
            double                  gamma,
            double                  wavelength
    )
    {
        int low_wl = (int) ((wavelength - 320.0 ) / 40.0);

        if ( low_wl < 0 || low_wl >= 11 )
            return 0.0f;

        double interp = fmod((wavelength - 320.0 ) / 40.0, 1.0);

        double val_low =
                ArHosekSkyModel_GetRadianceInternal(
                        state.configs[low_wl],
                        theta,
                        gamma
                )
                        * state.radiances[low_wl]
                        * state.emission_correction_factor_sky[low_wl];

        if ( interp < 1e-6 )
            return val_low;

        double result = ( 1.0 - interp ) * val_low;

        if ( low_wl+1 < 11 )
        {
            result +=
                    interp
                            * ArHosekSkyModel_GetRadianceInternal(
                            state.configs[low_wl+1],
                            theta,
                            gamma
                    )
                            * state.radiances[low_wl+1]
                            * state.emission_correction_factor_sky[low_wl+1];
        }

        return result;
    }


    public ArHosekSkyModelState  arhosek_xyz_skymodelstate_alloc_init(
         double  turbidity,
         double  albedo,
         double  elevation
    ){
        ArHosekSkyModelState  state = new ArHosekSkyModelState();

        state.solar_radius = TERRESTRIAL_SOLAR_RADIUS;
        state.turbidity    = turbidity;
        state.albedo       = albedo;
        state.elevation    = elevation;

        int channel;
        for( channel = 0; channel < 3; ++channel )
        {
            ArHosekSkyModel_CookConfiguration(
                    datasetsXYZ[channel],
                    state.configs[channel],
                    turbidity,
                    albedo,
                    elevation
            );

            state.radiances[channel] =
                    ArHosekSkyModel_CookRadianceConfiguration(
                            datasetsXYZRad[channel],
                            turbidity,
                            albedo,
                            elevation
                    );
        }

        return state;
    }


    public static ArHosekSkyModelState arhosek_rgb_skymodelstate_alloc_init(
        final double  turbidity,
        final double  albedo,
        final double  elevation
    ){
        ArHosekSkyModelState state = new ArHosekSkyModelState();

        state.solar_radius = TERRESTRIAL_SOLAR_RADIUS;
        state.turbidity    = turbidity;
        state.albedo       = albedo;
        state.elevation    = elevation;

        for( int channel = 0; channel < 3; ++channel )
        {
            ArHosekSkyModel_CookConfiguration(
                    datasetsRGB[channel],
                    state.configs[channel],
                    turbidity,
                    albedo,
                    elevation
            );

            state.radiances[channel] =
                    ArHosekSkyModel_CookRadianceConfiguration(
                            datasetsRGBRad[channel],
                            turbidity,
                            albedo,
                            elevation
                    );
        }

        return state;
    }


    public static double arhosek_tristim_skymodel_radiance(
            ArHosekSkyModelState    state,
            double                  theta,
            double                  gamma,
            int                     channel){
        return
                ArHosekSkyModel_GetRadianceInternal(
                        state.configs[channel],
                        theta,
                        gamma
                )
                        * state.radiances[channel];
    }

//   Delivers the complete function: sky + sun, including limb darkening.
//   Please read the above description before using this - there are several
//   caveats!

    public static double arhosekskymodel_solar_radiance(
            ArHosekSkyModelState        state,
            double                      theta,
            double                      gamma,
            double                      wavelength
    ){
        double  direct_radiance =
                arhosekskymodel_solar_radiance_internal2(
                        state,
                        wavelength,
                        ((Math.PI/2.0)-theta),
                        gamma
                );

        double  inscattered_radiance =
                arhosekskymodel_radiance(
                        state,
                        theta,
                        gamma,
                        wavelength
                );

        return  direct_radiance + inscattered_radiance;
    }

    // internal functions

    private static void ArHosekSkyModel_CookConfiguration(
            double[]       dataset,
            double[/*9*/]  config,
            double                        turbidity,
            double                        albedo,
            double                        solar_elevation
    )
    {
        int  elev_matrix;

        int     int_turbidity = (int)turbidity;
        double  turbidity_rem = turbidity - (double)int_turbidity;

        solar_elevation = Math.pow(solar_elevation / (Math.PI / 2.0), (1.0 / 3.0));

        // alb 0 low turb

        elev_matrix = /*dataset +*/ ( 9 * 6 * (int_turbidity-1) );

        int i;
        for( i = 0; i < 9; ++i )
        {
            //(1-t).^3* A1 + 3*(1-t).^2.*t * A2 + 3*(1-t) .* t .^ 2 * A3 + t.^3 * A4;
            config[i] =
                    (1.0-albedo) * (1.0 - turbidity_rem)
                            * ( Math.pow(1.0-solar_elevation, 5.0) * dataset[elev_matrix+i]  +
                            5.0  * Math.pow(1.0-solar_elevation, 4.0) * solar_elevation * dataset[elev_matrix+i+9] +
                            10.0*Math.pow(1.0-solar_elevation, 3.0)*Math.pow(solar_elevation, 2.0) * dataset[elev_matrix+i+18] +
                            10.0*Math.pow(1.0-solar_elevation, 2.0)*Math.pow(solar_elevation, 3.0) * dataset[elev_matrix+i+27] +
                            5.0*(1.0-solar_elevation)*Math.pow(solar_elevation, 4.0) * dataset[elev_matrix+i+36] +
                            Math.pow(solar_elevation, 5.0)  * dataset[elev_matrix+i+45]);
        }

        // alb 1 low turb
        elev_matrix = /*dataset +*/ (9*6*10 + 9*6*(int_turbidity-1));
        for( i = 0; i < 9; ++i)
        {
            //(1-t).^3* A1 + 3*(1-t).^2.*t * A2 + 3*(1-t) .* t .^ 2 * A3 + t.^3 * A4;
            config[i] +=
                    (albedo) * (1.0 - turbidity_rem)
                            * ( Math.pow(1.0-solar_elevation, 5.0) * dataset[elev_matrix+i]  +
                            5.0  * Math.pow(1.0-solar_elevation, 4.0) * solar_elevation * dataset[elev_matrix+i+9] +
                            10.0*Math.pow(1.0-solar_elevation, 3.0)*Math.pow(solar_elevation, 2.0) * dataset[elev_matrix+i+18] +
                            10.0*Math.pow(1.0-solar_elevation, 2.0)*Math.pow(solar_elevation, 3.0) * dataset[elev_matrix+i+27] +
                            5.0*(1.0-solar_elevation)*Math.pow(solar_elevation, 4.0) * dataset[elev_matrix+i+36] +
                            Math.pow(solar_elevation, 5.0)  * dataset[elev_matrix+i+45]);
        }

        if(int_turbidity == 10)
            return;

        // alb 0 high turb
        elev_matrix = /*dataset +*/ (9*6*(int_turbidity));
        for( i = 0; i < 9; ++i)
        {
            //(1-t).^3* A1 + 3*(1-t).^2.*t * A2 + 3*(1-t) .* t .^ 2 * A3 + t.^3 * A4;
            config[i] +=
                    (1.0-albedo) * (turbidity_rem)
                            * ( Math.pow(1.0-solar_elevation, 5.0) * dataset[elev_matrix+i]  +
                            5.0  * Math.pow(1.0-solar_elevation, 4.0) * solar_elevation * dataset[elev_matrix+i+9] +
                            10.0*Math.pow(1.0-solar_elevation, 3.0)*Math.pow(solar_elevation, 2.0) * dataset[elev_matrix+i+18] +
                            10.0*Math.pow(1.0-solar_elevation, 2.0)*Math.pow(solar_elevation, 3.0) * dataset[elev_matrix+i+27] +
                            5.0*(1.0-solar_elevation)*Math.pow(solar_elevation, 4.0) * dataset[elev_matrix+i+36] +
                            Math.pow(solar_elevation, 5.0)  * dataset[elev_matrix+i+45]);
        }

        // alb 1 high turb
        elev_matrix = /*dataset +*/ (9*6*10 + 9*6*(int_turbidity));
        for( i = 0; i < 9; ++i)
        {
            //(1-t).^3* A1 + 3*(1-t).^2.*t * A2 + 3*(1-t) .* t .^ 2 * A3 + t.^3 * A4;
            config[i] +=
                    (albedo) * (turbidity_rem)
                            * ( Math.pow(1.0-solar_elevation, 5.0) * dataset[elev_matrix+i]  +
                            5.0  * Math.pow(1.0-solar_elevation, 4.0) * solar_elevation * dataset[elev_matrix+i+9] +
                            10.0*Math.pow(1.0-solar_elevation, 3.0)*Math.pow(solar_elevation, 2.0) * dataset[elev_matrix+i+18] +
                            10.0*Math.pow(1.0-solar_elevation, 2.0)*Math.pow(solar_elevation, 3.0) * dataset[elev_matrix+i+27] +
                            5.0*(1.0-solar_elevation)*Math.pow(solar_elevation, 4.0) * dataset[elev_matrix+i+36] +
                            Math.pow(solar_elevation, 5.0)  * dataset[elev_matrix+i+45]);
        }
    }

    private static double ArHosekSkyModel_CookRadianceConfiguration(
            double[]                          dataset,
            double                            turbidity,
            double                            albedo,
            double                            solar_elevation
    )
    {
        int elev_matrix;

        int int_turbidity = (int)turbidity;
        double turbidity_rem = turbidity - (double)int_turbidity;
        double res;
        solar_elevation = Math.pow(solar_elevation / (Math.PI / 2.0), (1.0 / 3.0));

        // alb 0 low turb
        elev_matrix = /*dataset +*/ (6*(int_turbidity-1));
        //(1-t).^3* A1 + 3*(1-t).^2.*t * A2 + 3*(1-t) .* t .^ 2 * A3 + t.^3 * A4;
        res = (1.0-albedo) * (1.0 - turbidity_rem) *
                ( Math.pow(1.0-solar_elevation, 5.0) * dataset[elev_matrix+0] +
                        5.0*Math.pow(1.0-solar_elevation, 4.0)*solar_elevation * dataset[elev_matrix+1] +
                        10.0*Math.pow(1.0-solar_elevation, 3.0)*Math.pow(solar_elevation, 2.0) * dataset[elev_matrix+2] +
                        10.0*Math.pow(1.0-solar_elevation, 2.0)*Math.pow(solar_elevation, 3.0) * dataset[elev_matrix+3] +
                        5.0*(1.0-solar_elevation)*Math.pow(solar_elevation, 4.0) * dataset[elev_matrix+4] +
                        Math.pow(solar_elevation, 5.0) * dataset[elev_matrix+5]);

        // alb 1 low turb
        elev_matrix = /*dataset +*/ (6*10 + 6*(int_turbidity-1));
        //(1-t).^3* A1 + 3*(1-t).^2.*t * A2 + 3*(1-t) .* t .^ 2 * A3 + t.^3 * A4;
        res += (albedo) * (1.0 - turbidity_rem) *
                ( Math.pow(1.0-solar_elevation, 5.0) * dataset[elev_matrix+0] +
                        5.0*Math.pow(1.0-solar_elevation, 4.0)*solar_elevation * dataset[elev_matrix+1] +
                        10.0*Math.pow(1.0-solar_elevation, 3.0)*Math.pow(solar_elevation, 2.0) * dataset[elev_matrix+2] +
                        10.0*Math.pow(1.0-solar_elevation, 2.0)*Math.pow(solar_elevation, 3.0) * dataset[elev_matrix+3] +
                        5.0*(1.0-solar_elevation)*Math.pow(solar_elevation, 4.0) * dataset[elev_matrix+4] +
                        Math.pow(solar_elevation, 5.0) * dataset[elev_matrix+5]);
        if(int_turbidity == 10)
            return res;

        // alb 0 high turb
        elev_matrix = /*dataset +*/ (6*(int_turbidity));
        //(1-t).^3* A1 + 3*(1-t).^2.*t * A2 + 3*(1-t) .* t .^ 2 * A3 + t.^3 * A4;
        res += (1.0-albedo) * (turbidity_rem) *
                ( Math.pow(1.0-solar_elevation, 5.0) * dataset[elev_matrix+0] +
                        5.0*Math.pow(1.0-solar_elevation, 4.0)*solar_elevation * dataset[elev_matrix+1] +
                        10.0*Math.pow(1.0-solar_elevation, 3.0)*Math.pow(solar_elevation, 2.0) * dataset[elev_matrix+2] +
                        10.0*Math.pow(1.0-solar_elevation, 2.0)*Math.pow(solar_elevation, 3.0) * dataset[elev_matrix+3] +
                        5.0*(1.0-solar_elevation)*Math.pow(solar_elevation, 4.0) * dataset[elev_matrix+4] +
                        Math.pow(solar_elevation, 5.0) * dataset[elev_matrix+5]);

        // alb 1 high turb
        elev_matrix = /*dataset +*/ (6*10 + 6*(int_turbidity));
        //(1-t).^3* A1 + 3*(1-t).^2.*t * A2 + 3*(1-t) .* t .^ 2 * A3 + t.^3 * A4;
        res += (albedo) * (turbidity_rem) *
                ( Math.pow(1.0-solar_elevation, 5.0) * dataset[elev_matrix+0] +
                        5.0*Math.pow(1.0-solar_elevation, 4.0)*solar_elevation * dataset[elev_matrix+1] +
                        10.0*Math.pow(1.0-solar_elevation, 3.0)*Math.pow(solar_elevation, 2.0) * dataset[elev_matrix+2] +
                        10.0*Math.pow(1.0-solar_elevation, 2.0)*Math.pow(solar_elevation, 3.0) * dataset[elev_matrix+3] +
                        5.0*(1.0-solar_elevation)*Math.pow(solar_elevation, 4.0) * dataset[elev_matrix+4] +
                        Math.pow(solar_elevation, 5.0) * dataset[elev_matrix+5]);
        return res;
    }

    private static final double ArHosekSkyModel_GetRadianceInternal(
            double[]  configuration,
            double                        theta,
            double                        gamma
    )
    {
        final double expM = Math.exp(configuration[4] * gamma);
        final double rayM = Math.cos(gamma)*Math.cos(gamma);
        final double mieM = (1.0 + Math.cos(gamma)*Math.cos(gamma)) / Math.pow((1.0 + configuration[8]*configuration[8] - 2.0*configuration[8]*Math.cos(gamma)), 1.5);
        final double zenith = Math.sqrt(Math.cos(theta));

        return (1.0 + configuration[0] * Math.exp(configuration[1] / (Math.cos(theta) + 0.01))) *
                (configuration[2] + configuration[3] * expM + configuration[5] * rayM + configuration[6] * mieM + configuration[7] * zenith);
    }

    //   'art_blackbody_dd_value()' function
//
//   Blackbody radiance, Planck's formula

    private static double art_blackbody_dd_value(double  temperature,double  lambda)
    {
        double  c1 = 3.74177 * 10E-17;
        double  c2 = 0.0143878;
        double  value;

        value =   ( c1 / ( Math.pow( lambda, 5.0 ) ) )
                * ( 1.0 / ( Math.exp( c2 / ( lambda * temperature ) ) - 1.0 ) );

        return value;
    }

    private static final double arhosekskymodel_sr_internal(
            ArHosekSkyModelState    state,
            int                     turbidity,
            int                     wl,
            double                  elevation
    )
    {
        final int pieces = 45;
        final int order = 4;
        int pos =
                (int) (Math.pow(2.0*elevation / Math.PI, 1.0/3.0) * pieces); // floor

        if ( pos > 44 ) pos = 44;

        final double break_x =
            Math.pow(((double) pos / (double) pieces), 3.0) * (Math.PI * 0.5);

        /*final double  * coefs =
            solarDatasets[wl] + (order * pieces * turbidity + order * (pos+1) - 1);*/
        final double[] cpefs_array = solarDatasets[wl];
        int coefs_idx = (order * pieces * turbidity + order * (pos+1) - 1);

        double res = 0.0;
        final double x = elevation - break_x;
        double x_exp = 1.0;

        for (int i = 0; i < order; ++i)
        {
            res += x_exp * /**coefs--*/  cpefs_array[coefs_idx--];
            x_exp *= x;
        }

        return res * state.emission_correction_factor_sun[wl];
    }

    private static final double arhosekskymodel_solar_radiance_internal2(
            ArHosekSkyModelState    state,
            double                  wavelength,
            double                  elevation,
            double                  gamma
    )
    {
        assert(
                wavelength >= 320.0
                        && wavelength <= 720.0
                        && state.turbidity >= 1.0
                        && state.turbidity <= 10.0
        );

        // sun distance to diameter ratio, squared

        final double sol_rad_sin = Math.sin(state.solar_radius);
        final double ar2 = 1 / ( sol_rad_sin * sol_rad_sin );
        final double singamma = Math.sin(gamma);
        double sc2 = 1.0 - ar2 * singamma * singamma;
        if (sc2 < 0.0 ) {
            sc2 = 0.0;
            return 0.0;
        }

        double sampleCosine = Math.sqrt (sc2);

        int     turb_low  = (int) state.turbidity - 1;
        double  turb_frac = state.turbidity - (double) (turb_low + 1);

        if ( turb_low == 9 )
        {
            turb_low  = 8;
            turb_frac = 1.0;
        }

        int    wl_low  = (int) ((wavelength - 320.0) / 40.0);
        double wl_frac = fmod(wavelength, 40.0) / 40.0;

        if ( wl_low == 10 )
        {
            wl_low = 9;
            wl_frac = 1.0;
        }

        double direct_radiance =
                ( 1.0 - turb_frac )
                        * (    (1.0 - wl_frac)
                        * arhosekskymodel_sr_internal(
                        state,
                        turb_low,
                        wl_low,
                        elevation
                )
                        +   wl_frac
                        * arhosekskymodel_sr_internal(
                        state,
                        turb_low,
                        wl_low+1,
                        elevation
                )
                )
                        +   turb_frac
                        * (    ( 1.0 - wl_frac )
                        * arhosekskymodel_sr_internal(
                        state,
                        turb_low+1,
                        wl_low,
                        elevation
                )
                        +   wl_frac
                        * arhosekskymodel_sr_internal(
                        state,
                        turb_low+1,
                        wl_low+1,
                        elevation
                )
                );

        final double[] ldCoefficient = new double[6];

        for ( int i = 0; i < 6; i++ )
            ldCoefficient[i] =
                    (1.0 - wl_frac) * limbDarkeningDatasets[wl_low  ][i]
                            +        wl_frac  * limbDarkeningDatasets[wl_low+1][i];

        //   The following will be improved in future versions of the model:
        //   here, we directly use fitted 5th order polynomials provided by the
        //   astronomical community for the limb darkening effect. Astronomers need
        //   such accurate fittings for their predictions. However, this sort of
        //   accuracy is not really needed for CG purposes, so an approximated
        //   dataset based on quadratic polynomials will be provided in a future
        //   release.

        double  darkeningFactor =
                ldCoefficient[0]
                        + ldCoefficient[1] * sampleCosine
                        + ldCoefficient[2] * Math.pow( sampleCosine, 2.0 )
                        + ldCoefficient[3] * Math.pow( sampleCosine, 3.0 )
                        + ldCoefficient[4] * Math.pow( sampleCosine, 4.0 )
                        + ldCoefficient[5] * Math.pow( sampleCosine, 5.0 );

        direct_radiance *= darkeningFactor;

        return direct_radiance;
    }



    private enum FileState{
        None,
        Begin,
        Process,
        End,
    }

    static void load_static_data(){
//        StringBuilder loadDataArray = new StringBuilder();
//        StringBuilder sourceArray = new StringBuilder();

        try(BufferedReader in = new BufferedReader(new FileReader(new File("E:\\SDK\\HosekWilkie_SkylightModel_C_Source.1.4a\\ArHosekSkyModelData_Spectral.h")))){
            CommentFilter filter = new CommentFilter(in);
            String line;

            FileState state = FileState.None;
            StackDouble parsedValues = new StackDouble();

            ArrayName name=null;
            while ((line = filter.nextLine()) != null){
                line = line.trim();
                if(line.length() == 0)
                    continue;

                switch (state){
                    case None:
                        name = getName(line);
//                        if(name.isArray){
//                            sourceArray.append(line).append('\n');
//                        }else{
//                            loadDataArray.append("static double[] ").append(name.name).append(";\n");
//                        }

                        state = FileState.Begin;
                        break;
                    case Begin:
                        int endIdx = line.indexOf('}');
                        if(endIdx >= 0){
                            if(name.isArray) {
//                                sourceArray.append(line).append('\n');
                            }else{
                                StringTokenizer tokenizer = new StringTokenizer(line, " \t{};,");
                                StackDouble floats = new StackDouble(128);
                                while (tokenizer.hasMoreElements()){
                                    floats.push(Double.parseDouble(tokenizer.nextToken()));
                                }

                                floats.trim();
                                assign(name.name, floats.getData());
                            }

                            state = FileState.None;  // goto the none state
                            parsedValues.clear();
                        }else{
                            if(!line.equals("{"))
                                throw new IllegalArgumentException("Inner error!!!");

//                            if(name.isArray)
//                                sourceArray.append(line).append('\n');

                            state = FileState.Process;
                        }
                        break;
                    case Process:
                        if(!line.contains("}")){
                            if(name.isArray){
//                                sourceArray.append(line).append('\n');
                            }else{
                                double v = Double.parseDouble(line.substring(0, line.length() - 1));
                                parsedValues.push(v);
                            }
                        }else{
                            if(name.isArray){
//                                sourceArray.append(line).append('\n');
                            }else {
                                // end
                                assign(name.name, parsedValues.copy().getData());
                                parsedValues.clear();
                            }

                            state = FileState.None;  // goto the none state
                        }
                        break;
                }
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

//        System.out.print(loadDataArray);
        System.out.print(Arrays.toString(limbDarkeningDataset720));
    }

    private static void assign(String name, double[] a){
        Field field = DebugTools.getField(ArHosekSkyModel.class, name);
        try {
            field.set(null, a);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    private static ArrayName getName(String line){
        StringTokenizer tokenizer = new StringTokenizer(line, " ");
        ArrayName name = new ArrayName();
        String typeName = tokenizer.nextToken();
        String arrayName = tokenizer.nextToken();
        name.isArray = typeName.indexOf('*') >= 0;
        name.name = arrayName.substring(0, arrayName.length() - 2);

        return name;
    }

    private static final class ArrayName{
        String name;
        boolean isArray;
    }
}
