//--------------------------------------------------------------------------------------
// Copyright 2013 Intel Corporation
// All Rights Reserved
//
// Permission is granted to use, copy, distribute and prepare derivative works of this
// software for any purpose and without fee, provided, that the above copyright notice
// and this statement appear in all copies.  Intel makes no representations about the
// suitability of this software for any purpose.  THIS SOFTWARE IS PROVIDED "AS IS."
// INTEL SPECIFICALLY DISCLAIMS ALL WARRANTIES, EXPRESS OR IMPLIED, AND ALL LIABILITY,
// INCLUDING CONSEQUENTIAL AND OTHER INDIRECT DAMAGES, FOR THE USE OF THIS SOFTWARE,
// INCLUDING LIABILITY FOR INFRINGEMENT OF ANY PROPRIETARY RIGHTS, AND INCLUDING THE
// WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE.  Intel does not
// assume any responsibility for any errors which may appear in this software nor any
// responsibility to update it.
//--------------------------------------------------------------------------------------

//#ifndef _STRCUTURES_FXH_
//#define _STRCUTURES_FXH_

#include "../PostProcessingHLSLCompatiable.glsl"

#define PI 3.1415928

#define MAX_CASCADES 8

#define LIGHT_SCTR_TECHNIQUE_EPIPOLAR_SAMPLING 0
#define LIGHT_SCTR_TECHNIQUE_BRUTE_FORCE 1

#define CASCADE_PROCESSING_MODE_SINGLE_PASS 0
#define CASCADE_PROCESSING_MODE_MULTI_PASS 1
#define CASCADE_PROCESSING_MODE_MULTI_PASS_INST 2

#define REFINEMENT_CRITERION_DEPTH_DIFF 0
#define REFINEMENT_CRITERION_INSCTR_DIFF 1

// Extinction evaluation mode used when attenuating background
#define EXTINCTION_EVAL_MODE_PER_PIXEL 0// Evaluate extinction for each pixel using analytic formula 
                                        // by Eric Bruneton
#define EXTINCTION_EVAL_MODE_EPIPOLAR 1 // Render extinction in epipolar space and perform
                                        // bilateral filtering in the same manner as for
                                        // inscattering

#define SINGLE_SCTR_MODE_NONE 0
#define SINGLE_SCTR_MODE_INTEGRATION 1
#define SINGLE_SCTR_MODE_LUT 2

#define MULTIPLE_SCTR_MODE_NONE 0
#define MULTIPLE_SCTR_MODE_UNOCCLUDED 1
#define MULTIPLE_SCTR_MODE_OCCLUDED 2

#define TONE_MAPPING_MODE_EXP 0
#define TONE_MAPPING_MODE_REINHARD 1
#define TONE_MAPPING_MODE_REINHARD_MOD 2
#define TONE_MAPPING_MODE_UNCHARTED2 3
#define TONE_MAPPING_FILMIC_ALU 4
#define TONE_MAPPING_LOGARITHMIC 5
#define TONE_MAPPING_ADAPTIVE_LOG 6

//#endif //_STRCUTURES_FXH_

// Common uniform variable declared
uniform float g_fEarthRadius = 6360000.0;
uniform vec2 g_f2ParticleScaleHeight = float2(7994.0, 1200.0);
uniform float g_fAtmTopHeight = 80000.0;
uniform float g_fAtmTopRadius = 6360000.0 + 80000.0;
uniform vec4 g_f4ExtraterrestrialSunColor = vec4(5);

uniform vec4  g_f4DirOnLight;
uniform vec4  g_f4RayleighExtinctionCoeff;
uniform vec4  g_f4MieExtinctionCoeff;

void GetRaySphereIntersection(in float3 f3RayOrigin,
                              in float3 f3RayDirection,
                              in float3 f3SphereCenter,
                              in float fSphereRadius,
                              out float2 f2Intersections)
{
    // http://wiki.cgsociety.org/index.php/Ray_Sphere_Intersection
    f3RayOrigin -= f3SphereCenter;
    float A = dot(f3RayDirection, f3RayDirection);
    float B = 2.0 * dot(f3RayOrigin, f3RayDirection);
    float C = dot(f3RayOrigin,f3RayOrigin) - fSphereRadius*fSphereRadius;
    float D = B*B - 4.0*A*C;
    // If discriminant is negative, there are no real roots hence the ray misses the
    // sphere
    if( D<0 )
    {
        f2Intersections = float2(-1.0);
    }
    else
    {
        D = sqrt(D);
        f2Intersections = float2(-B - D, -B + D) / (2.0*A); // A must be positive here!!
    }
}