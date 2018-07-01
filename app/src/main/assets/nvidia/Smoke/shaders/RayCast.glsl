// cubic b-spline
float bsW0(float a)
{
    return (1.0/6.0 * ( -(a*a*a) + (3.0 * a*a) - (3.0 * a) + 1.0));
}

float bsW1(float a)
{
    return (1.0/6.0 * ( (3.0 * a*a*a) - (6.0 * a*a) + 4.0 ));
}

float bsW2(float a)
{
    return (1.0/6.0 * ( -(3.0 * a*a*a) + (3.0 * a*a) + (3.0*a) + 1.0));
}

float bsW3(float a)
{
    return (1.0/6.0 * a*a*a);
}

float g0(float a)
{
    return (bsW0(a) + bsW1(a));
}

float g1(float a)
{
    return (bsW2(a) + bsW3(a));
}

float h0texels(float a)
{
    return (1.0 + a - (bsW1(a)/(bsW0(a)+bsW1(a))));
}

float h1texels(float a)
{
    return (1.0 - a + (bsW3(a)/(bsW2(a)+bsW3(a))));
}
/// end cubic-bspline

// first derivative of cubic b-spline
float bsfdW0(float a)
{
    return (1.0/6.0 * ( -(3.0 * a*a) + (6.0 * a) - 3.0));
}

float bsfdW1(float a)
{
    return (1.0/6.0 * ( (9.0 * a*a) - (12.0 * a) ));
}

float bsfdW2(float a)
{
    return (1.0/6.0 * ( -(9.0 * a*a) + (6.0 * a) + 3.0));
}

float bsfdW3(float a)
{
    return (1.0/6.0 * 3.0 * a*a);
}

float gfd0(float a)
{
    return (bsfdW0(a) + bsfdW1(a));
}

float gfd1(float a)
{
    return (bsfdW2(a) + bsfdW3(a));
}

float hfd0texels(float a)
{
    return (1.0 + a - (bsfdW1(a)/(bsfdW0(a)+bsfdW1(a))));
}

float hfd1texels(float a)
{
    return (1.0 - a + (bsfdW3(a)/(bsfdW2(a)+bsfdW3(a))));
}
/// end first derivative of cubic b-spline

float4 getHHGG( float xTexels)
{
    float a = frac(xTexels);
    return float4( -h0texels(a), h1texels(a), 1.0-g0(a), g0(a) );
}

float4 getfdHHGG( float xTexels)
{
    float a = frac(xTexels);
    return float4( -hfd0texels(a), hfd1texels(a), gfd1(a), -gfd1(a) );
}


float4 SampleTricubicGeneric(sampler3D tex, float3 tc, float4 hg_x, float4 hg_y, float4 hg_z)
{
    float3  tc100, tc000, tc110, tc010,
            tc101, tc001, tc111, tc011;

    tc100 = tc;
    tc000 = tc;
    tc100.x += (hg_x.x * recGridDim.x);
    tc000.x += (hg_x.y * recGridDim.x);

    tc110 = tc100;
    tc010 = tc000;
    tc110.y += (hg_y.x * recGridDim.y);
    tc010.y += (hg_y.x * recGridDim.y);
    tc100.y += (hg_y.y * recGridDim.y);
    tc000.y += (hg_y.y * recGridDim.y);

    tc111 = tc110;
    tc011 = tc010;
    tc101 = tc100;
    tc001 = tc000;
    tc111.z += (hg_z.x * recGridDim.z);
    tc011.z += (hg_z.x * recGridDim.z);
    tc101.z += (hg_z.x * recGridDim.z);
    tc001.z += (hg_z.x * recGridDim.z);

    float4 v001 = textureLod(tex, tc001, 0);   //samLinearClamp
    float4 v011 = textureLod(tex, tc011, 0);
    float4 v101 = textureLod(tex, tc101, 0);
    float4 v111 = textureLod(tex, tc111, 0);

    float4 v0Y1 = (v001 * hg_y.z) + (v011 * hg_y.w);
    float4 v1Y1 = (v101 * hg_y.z) + (v111 * hg_y.w);

    float4 vXY1 = (v0Y1 * hg_x.z) + (v1Y1 * hg_x.w);

    tc110.z += (hg_z.y * recGridDim.z);
    tc010.z += (hg_z.y * recGridDim.z);
    tc100.z += (hg_z.y * recGridDim.z);
    tc000.z += (hg_z.y * recGridDim.z);


    float4 v000 = textureLod(tex, tc000, 0);
    float4 v010 = textureLod(tex, tc010, 0);
    float4 v100 = textureLod(tex, tc100, 0);
    float4 v110 = textureLod(tex, tc110, 0);

    float4 v0Y0 = (v000 * hg_y.z) + (v010 * hg_y.w);
    float4 v1Y0 = (v100 * hg_y.z) + (v110 * hg_y.w);

    float4 vXY0 = (v0Y0 * hg_x.z) + (v1Y0 * hg_x.w);


    float4 vXYZ = (vXY0 * hg_z.z) + (vXY1 * hg_z.w);

    return vXYZ;
}


float4 SampleTricubic(Texture3D tex, float3 tc)
{
    float3 tcTexels = (tc * gridDim) - 0.49;

    float4 hg_x = getHHGG(tcTexels.x);
    float4 hg_y = getHHGG(tcTexels.y);
    float4 hg_z = getHHGG(tcTexels.z);

    return SampleTricubicGeneric(tex, tc, hg_x, hg_y, hg_z);
}

float4 SampleGradientTricubic(Texture3D tex, float3 tc)
{
    float3 tcTexels = (tc * gridDim) - 0.49;

    float4 hg_x   = getHHGG(tcTexels.x);
    float4 hg_y   = getHHGG(tcTexels.y);
    float4 hg_z   = getHHGG(tcTexels.z);
    float4 hgfd_x = getfdHHGG(tcTexels.x);
    float4 hgfd_y = getfdHHGG(tcTexels.y);
    float4 hgfd_z = getfdHHGG(tcTexels.z);

    return float4(  SampleTricubicGeneric(tex, tc, hgfd_x, hg_y, hg_z).r,
                    SampleTricubicGeneric(tex, tc, hg_x, hgfd_y, hg_z).r,
                    SampleTricubicGeneric(tex, tc, hg_x, hg_y, hgfd_z).r, 1.0 );
}


float4 SampleTrilinear(Texture3D tex, float3 tc)
{
    return textureLod(tex, tc, 0);   // tex.SampleLevel( samLinearClamp
}

float4 SampleGradientTrilinear(Texture3D tex, float3 tc)
{
    #define LEFTCELL    float3 (tc.x-(1.0/gridDim.x), tc.y, tc.z)
    #define RIGHTCELL   float3 (tc.x+(1.0/gridDim.x), tc.y, tc.z)
    #define BOTTOMCELL  float3 (tc.x, (tc.y-(1.0/gridDim.y)), tc.z)
    #define TOPCELL     float3 (tc.x, (tc.y+(1.0/gridDim.y)), tc.z)
    #define DOWNCELL    float3 (tc.x, tc.y, tc.z - (1.0/gridDim.z))
    #define UPCELL      float3 (tc.x, tc.y, tc.z + (1.0/gridDim.z))

    // tex.SampleLevel( samLinearClamp
    float4 texL = textureLod(tex, LEFTCELL, 0 );
    float4 texR = textureLod(tex, RIGHTCELL, 0 );
    float4 texB = textureLod(tex, BOTTOMCELL, 0 );
    float4 texT = textureLod(tex, TOPCELL, 0 );
    float4 texU = textureLod(tex, UPCELL, 0 );
    float4 texD = textureLod(tex, DOWNCELL, 0 );
    return float4(  texR.r - texL.r, texT.r - texB.r, texU.r - texD.r, 1 );
}


float4 Sample(Texture3D tex, float3 tc)
{
    if( g_bRaycastFilterTricubic )
        return SampleTricubic(tex, tc);
    else
        return SampleTrilinear(tex, tc);
}

float4 SampleGradient(Texture3D tex, float3 tc)
{
    if( g_bRaycastFilterTricubic )
        return SampleGradientTricubic(tex, tc);
    else
        return SampleGradientTrilinear(tex, tc);
}
// END Custom Sampling Functions
/////////////////////////////////

#define OBSTACLE_MAX_HEIGHT 4
void SampleSmokeOrFire(float weight, float3 O, inout float4 color, bool renderFire )
{

    float3 texcoords = O;

    if(!renderFire)
    {
        //render smoke with front to back blending
        float t;
        float4 samples = weight * abs(textureLod(volumeTex, texcoords, 0));   //samLinearClamp
        //float4 sample = weight *abs(SampleTricubic(volumeTex, texcoords));
        samples.a = (samples.r) * 0.1;
        t = samples.a * (1.0-color.a);
        color.rgb += t * samples.r;
        color.a += t;
    }
    else
    {
        //render fire and smoke with back to front blending

        //dont render the area below where the fire originates
        if(O.z < OBSTACLE_MAX_HEIGHT/gridDim.z)
            return;

        //this is the threshold at which we decide whether to render fire or smoke
        float threshold = 1.4;
        float maxValue = 3;

        float s = textureLod(volumeTex, texcoords, 0).x;   //samLinearClamp
        s = clamp(s,0.,maxValue);

        if(s>threshold)
        {
            //render fire
            float lookUpVal = ( (s-threshold)/(maxValue-threshold) );
            lookUpVal = 1.0 - pow(lookUpVal, rednessFactor);
            lookUpVal = clamp(lookUpVal,0,1);
            float3 interpColor = textureLod(fireTransferFunction, float2(lookUpVal,0),0).rgb;   // samLinearClamp
            float mult = (s-threshold);
            color += float4(weight*interpColor.rgb, weight*mult*mult*fireAlphaMultiplier);
        }
        else
        {
             //render smoke
             float4 samples = weight*s;
             samples.a = samples.r*0.1*smokeAlphaMultiplier;
             float3 smokeColor = float3(0.9,0.35,0.055);
             color.rgb = (1 - samples.a) * color.rgb + samples.a * samples.rrr * smokeColor * smokeColorMultiplier * 5.0;
             color.a = (1 - samples.a) * color.a + samples.a;
        }
    }
}


float4 Raycast( /*PS_INPUT_RAYCAST input,*/  int raycastMode,  float sampleFactor )
{
    float4 color = float4(0);
    float2 normalizedInputPos = float2(gl_FragCoord.x/RTWidth,gl_FragCoord.y/RTHeight);
    float4 rayData = rayDataTex.Sample(samLinearClamp, normalizedInputPos);

    // Don't raycast if the starting position is negative
    //   (see use of OCCLUDED_PIXEL_RAYVALUE in PS_RAYDATA_FRONT)
    if(rayData.x < 0.)
    {
        return color;
    }

    // If the front face of the box was clipped here by the near plane of the camera
    //   (see use of NEARCLIPPED_PIXEL_RAYPOS in PS_RAYDATA_BACK)
    if(rayData.y < 0)
    {
       // Initialize the position of the fragment and adjust the depth
       rayData.xyz = m_PosInGrid.xyz/m_PosInGrid.w;  // TODO
       float2 inputPos = float2((normalizedInputPos.x*2.0)-1.0,(normalizedInputPos.y*2.0)-1.0);
       float distanceToNearPlane = length(float3( inputPos.x * ZNear * tan_FovXhalf, inputPos.y * ZNear * tan_FovYhalf, ZNear ));
       rayData.w = rayData.w - distanceToNearPlane;
    }

    float3 rayOrigin = rayData.xyz;
    float rayLength = rayData.w;

    // Sample twice per voxel
    float fSamples = ( rayLength / gridScaleFactor * maxGridDim ) * sampleFactor;
    int nSamples = floor(fSamples);
    float3 stepVec = normalize( (rayOrigin - eyeOnGrid) * gridDim ) * recGridDim * (1.0/sampleFactor);

    float3 O = rayOrigin;

    if(raycastMode == RM_LEVELSET)
    {
        float rho = 0;
        float levelSet;
        int i;

        // Use lsFactor to negate the sampled value if we start inside the levelSet (i.e. first sample < rho)
        //  this is used to find the intersection when we start raycasting inside the volume
        float lsFactor = 1.0;
        levelSet = SampleTrilinear( volumeTex, O-1*stepVec ).r;
        if( levelSet < rho )
            lsFactor = -1.0;

        O += stepVec;
        for( i=0; i<nSamples; i++, O += stepVec )
        {
        #if 1
            levelSet = Sample( volumeTex, O ).r * lsFactor;
            if(levelSet<rho)
                break;
        #else
            // adaptive sampling test

            levelSet = SampleTrilinear( volumeTex, O ).r * lsFactor;
            if(levelSet<(rho+0.05))
            {
                // go backwards a bit and increase sampling rate near detected intersection
                int backStepCount = 1;
                O -= (backStepCount*stepVec);

                float incSamplingFactor = 2.0;
                backStepCount *= incSamplingFactor;
                stepVec /= incSamplingFactor;
                for( int j=0; j<=backStepCount; j++, O += stepVec )
                {
                    levelSet = Sample( volumeTex, O ).r * lsFactor;
                    if(levelSet<rho)
                        break;
                }
                stepVec *= incSamplingFactor;
                if(levelSet<rho)
                    break;
            }
        #endif
        }

        if((i == nSamples) || (levelSet > rho) )
            return float4(0,0,0,0);


        if( g_bRaycastBisection )
        {
            // use bisection method to refine the intersection point
            float3 a = O - stepVec;
            float3 b = O;
            float3 m;

            // starting assumption is (ls_a > rho) && (ls_b < rho)
            for( int i=0; i<3; i++)
            {
                m = (a+b) * 0.5;

                float ls_m = Sample(volumeTex, m).r  * lsFactor;

                if( ls_m < rho )
                {
                    b = m;
                }
                else
                {
                    a = m;
                }

            }

            O =  (a+b) * 0.5;
        }

        float3 normal = SampleGradient(volumeTex, O).xyz;
        normal = normalize( normal);

        float3 shadedColor = 0;
        if( g_bRaycastShadeAsWater )
        {

            float waterIR = 1.3333;
            float airIR   = 1.0003;
            float etaRatio = waterIR / airIR;

            // if going from air to water
            if( lsFactor > 0 )
            {
                normal.y = -normal.y;
                etaRatio = 1.0 / etaRatio;
            }

            // Render using cubemap reflection and refraction
            float4 eyeVec = float4(normalize(O - eyeOnGrid), 0);
            float3 eyeReflected = mul(reflect(eyeVec, normal), Grid2World).xyz;
            float3 eyeRefracted = mul(refract(eyeVec, normal, etaRatio), Grid2World).xyz;

            float3 refractColor = textureLod( envMapTex, eyeRefracted, 0).rgb;   //samLinearWrap
            //float3 refractColor = 0;
            float3 reflectColor = textureLod( envMapTex, eyeReflected, 0).rgb;
            float fresnelReflectionCoeff = max(0, min(1, pow(1+dot(eyeVec,normal),2)) );

            shadedColor += refractColor * (1 - fresnelReflectionCoeff);
            shadedColor += reflectColor * fresnelReflectionCoeff;
        }
        else
        {
            shadedColor = (normal+1.0)/2.0;
        }

        color = float4(shadedColor, 1);
    }
    else
    {
        float Offset = texture( jitterTex, gl_FragCoord.xy / 256.0 ).r;   // samLinearWrap
        O += stepVec*Offset;

        if(raycastMode == RM_FIRE)
        {
            // we render fire with back to front ray marching
            // In back-to-front blending we start raycasting from the surface point and step towards the eye
            O += fSamples * stepVec;
            stepVec = -stepVec;
        }

        for( int i=0; i<nSamples ; i++ )
        {
            SampleSmokeOrFire(1, O, color, (raycastMode == RM_FIRE));
            O += stepVec;

            if(!(raycastMode == RM_FIRE))
            {
                // If doing front-to-back blending we can do early exit when opacity saturates
                if( color.a > 0.99 )
                    break;
            }
        }

        // The last sample is weighted by the fractional part of the ray length in voxel
        //  space (fSamples), thus avoiding banding artifacts when the smoke is blended against the scene
        if( i == nSamples )
        {
            SampleSmokeOrFire(frac(fSamples), O, color, (raycastMode == RM_FIRE));
        }
    }

    return color;
}