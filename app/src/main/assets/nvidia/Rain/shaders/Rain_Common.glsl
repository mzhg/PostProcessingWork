//----------------------------------------------------------------------------------
// File:   Rain.fx
// Author: Sarah Tariq
// Email:  sdkfeedback@nvidia.com
//
// Copyright (c) 2007 NVIDIA Corporation. All rights reserved.
//
// TO  THE MAXIMUM  EXTENT PERMITTED  BY APPLICABLE  LAW, THIS SOFTWARE  IS PROVIDED
// *AS IS*  AND NVIDIA AND  ITS SUPPLIERS DISCLAIM  ALL WARRANTIES,  EITHER  EXPRESS
// OR IMPLIED, INCLUDING, BUT NOT LIMITED  TO, IMPLIED WARRANTIES OF MERCHANTABILITY
// AND FITNESS FOR A PARTICULAR PURPOSE.  IN NO EVENT SHALL  NVIDIA OR ITS SUPPLIERS
// BE  LIABLE  FOR  ANY  SPECIAL,  INCIDENTAL,  INDIRECT,  OR  CONSEQUENTIAL DAMAGES
// WHATSOEVER (INCLUDING, WITHOUT LIMITATION,  DAMAGES FOR LOSS OF BUSINESS PROFITS,
// BUSINESS INTERRUPTION, LOSS OF BUSINESS INFORMATION, OR ANY OTHER PECUNIARY LOSS)
// ARISING OUT OF THE  USE OF OR INABILITY  TO USE THIS SOFTWARE, EVEN IF NVIDIA HAS
// BEEN ADVISED OF THE POSSIBILITY OF SUCH DAMAGES.
//
//
//----------------------------------------------------------------------------------
#include "../../../shader_libs/PostProcessingHLSLCompatiable.glsl"
#ifndef PI
#define PI 3.14159265
#endif

layout(binding = 0) uniform sampler2DArray rainTextureArray;
layout(binding = 1) uniform sampler2D singleTexture;
layout(binding = 2) uniform sampler2D backgroundTexture;
layout(binding = 3) uniform sampler2D SceneTextureDiffuse;
layout(binding = 4) uniform sampler2D SceneTextureSpecular;
layout(binding = 5) uniform sampler2D SceneTextureNormal;
layout(binding = 6) uniform sampler2D Ftable;
layout(binding = 7) uniform sampler2D Gtable;
layout(binding = 8) uniform sampler2D G_20table;
layout(binding = 9) uniform sampler3D SplashBumpTexture;
layout(binding = 10) uniform sampler3D SplashDiffuseTexture;

//ChangesEveryFrame

uniform float4x4 g_mInverseProjection;

uniform float4x4 g_mWorld;
uniform float4x4 g_mWorldViewProj;
uniform float4x4 g_mWorldView;
uniform float4x4 g_mProjection;
uniform float4x4 g_mViewProjectionInverse;
uniform float4x4 g_mInvView;
uniform float3 g_eyePos;   //eye in world space
uniform float3 g_lightPos = float3(10,10,0); //the directional light in world space
uniform float g_de;
uniform float3 g_ViewSpaceLightVec;
uniform float3 g_ViewSpaceLightVec2;
uniform float g_DSVPointLight;
uniform float g_DSVPointLight2;
uniform float g_DSVPointLight3;
uniform float3 g_VecPointLightEye;
uniform float3 g_VecPointLightEye2;
uniform float3 g_VecPointLightEye3;
uniform bool g_useSpotLight = true;
uniform float g_cosSpotlightAngle = 0.8;
uniform float3 g_SpotLightDir = float3(0,-1,0);
uniform float g_FrameRate;
uniform float g_timeCycle;
uniform float g_splashXDisplace;
uniform float g_splashYDisplace;

//changesOften
uniform float g_ResponseDirLight = 1.0;
uniform float g_ResponsePointLight = 1.0;
uniform float dirLightIntensity = 1.0;
uniform bool renderBg = false;
uniform bool moveParticles = false;
uniform float3 g_TotalVel = float3(0,-0.25,0);
uniform float4 g_DiffuseColor;
uniform float g_PointLightIntensity = 2.0;
uniform float g_SpriteSize = 1.0;
uniform float3 g_beta = float3(0.04,0.04,0.04);
uniform float g_BgAirLight = 0.0;
uniform float g_Kd = 0.1;
uniform float g_KsPoint = 20;
uniform float g_KsDir = 10;
uniform float g_specPower = 20;


//changesRarely
uniform float g_ScreenWidth = 640.0;
uniform float g_ScreenHeight = 480.0;
uniform float g_ScreenWidthMultiplier =  0.0031299;
uniform float g_ScreenHeightMultiplier = 0.0041754;
uniform float g_heightMin = 0.0;
uniform float g_radiusMin = 1.0;
uniform float g_heightRange = 30.0;
uniform float g_radiusRange = 30.0;
uniform float maxHeight;
uniform float g_Near;
uniform float g_Far;

    const float3 g_positions[4] = float3[4]
    (
        float3( -1, 1, 0 ),
        float3( 1, 1, 0 ),
        float3( -1, -1, 0 ),
        float3( 1, -1, 0 )
    );

    const float2 g_texcoords[4] = float2[4]
    (
        float2(0,1),
        float2(1,1),
        float2(0,0),
        float2(1,0)
    );

    //normalization factors for the rain textures, one per texture
    const float g_rainfactors[370] = float[370]
    (

        0.004535 , 0.014777 , 0.012512 , 0.130630 , 0.013893 , 0.125165 , 0.011809 , 0.244907 , 0.010722 , 0.218252,
        0.011450 , 0.016406 , 0.015855 , 0.055476 , 0.015024 , 0.067772 , 0.021120 , 0.118653 , 0.018705 , 0.142495,
        0.004249 , 0.017267 , 0.042737 , 0.036384 , 0.043433 , 0.039413 , 0.058746 , 0.038396 , 0.065664 , 0.054761,
        0.002484 , 0.003707 , 0.004456 , 0.006006 , 0.004805 , 0.006021 , 0.004263 , 0.007299 , 0.004665 , 0.007037,
        0.002403 , 0.004809 , 0.004978 , 0.005211 , 0.004855 , 0.004936 , 0.006266 , 0.007787 , 0.006973 , 0.007911,
        0.004843 , 0.007565 , 0.007675 , 0.011109 , 0.007726 , 0.012165 , 0.013179 , 0.021546 , 0.013247 , 0.012964,
        0.105644 , 0.126661 , 0.128746 , 0.101296 , 0.123779 , 0.106198 , 0.123470 , 0.129170 , 0.116610 , 0.137528,
        0.302834 , 0.379777 , 0.392745 , 0.339152 , 0.395508 , 0.334227 , 0.374641 , 0.503066 , 0.387906 , 0.519618,
        0.414521 , 0.521799 , 0.521648 , 0.498219 , 0.511921 , 0.490866 , 0.523137 , 0.713744 , 0.516829 , 0.743649,
        0.009892 , 0.013868 , 0.034567 , 0.025788 , 0.034729 , 0.036399 , 0.030606 , 0.017303 , 0.051809 , 0.030852,
        0.018874 , 0.027152 , 0.031625 , 0.023033 , 0.038150 , 0.024483 , 0.029034 , 0.021801 , 0.037730 , 0.016639,
        0.002868 , 0.004127 , 0.133022 , 0.013847 , 0.123368 , 0.012993 , 0.122183 , 0.015031 , 0.126043 , 0.015916,
        0.002030 , 0.002807 , 0.065443 , 0.002752 , 0.069440 , 0.002810 , 0.081357 , 0.002721 , 0.076409 , 0.002990,
        0.002425 , 0.003250 , 0.003180 , 0.011331 , 0.002957 , 0.011551 , 0.003387 , 0.006086 , 0.002928 , 0.005548,
        0.003664 , 0.004258 , 0.004269 , 0.009404 , 0.003925 , 0.009233 , 0.004224 , 0.009405 , 0.004014 , 0.008435,
        0.038058 , 0.040362 , 0.035946 , 0.072104 , 0.038315 , 0.078789 , 0.037069 , 0.077795 , 0.042554 , 0.073945,
        0.124160 , 0.122589 , 0.121798 , 0.201886 , 0.122283 , 0.214549 , 0.118196 , 0.192104 , 0.122268 , 0.209397,
        0.185212 , 0.181729 , 0.194527 , 0.420721 , 0.191558 , 0.437096 , 0.199995 , 0.373842 , 0.192217 , 0.386263,
        0.003520 , 0.053502 , 0.060764 , 0.035197 , 0.055078 , 0.036764 , 0.048231 , 0.052671 , 0.050826 , 0.044863,
        0.002254 , 0.023290 , 0.082858 , 0.043008 , 0.073780 , 0.035838 , 0.080650 , 0.071433 , 0.073493 , 0.026725,
        0.002181 , 0.002203 , 0.112864 , 0.060140 , 0.115635 , 0.065531 , 0.093277 , 0.094123 , 0.093125 , 0.144290,
        0.002397 , 0.002369 , 0.043241 , 0.002518 , 0.040455 , 0.002656 , 0.002540 , 0.090915 , 0.002443 , 0.101604,
        0.002598 , 0.002547 , 0.002748 , 0.002939 , 0.002599 , 0.003395 , 0.002733 , 0.003774 , 0.002659 , 0.004583,
        0.003277 , 0.003176 , 0.003265 , 0.004301 , 0.003160 , 0.004517 , 0.003833 , 0.008354 , 0.003140 , 0.009214,
        0.008558 , 0.007646 , 0.007622 , 0.026437 , 0.007633 , 0.021560 , 0.007622 , 0.017570 , 0.007632 , 0.018037,
        0.031062 , 0.028428 , 0.028428 , 0.108300 , 0.028751 , 0.111013 , 0.028428 , 0.048661 , 0.028699 , 0.061490,
        0.051063 , 0.047597 , 0.048824 , 0.129541 , 0.045247 , 0.124975 , 0.047804 , 0.128904 , 0.045053 , 0.119087,
        0.002197 , 0.002552 , 0.002098 , 0.200688 , 0.002073 , 0.102060 , 0.002111 , 0.163116 , 0.002125 , 0.165419,
        0.002060 , 0.002504 , 0.002105 , 0.166820 , 0.002117 , 0.144274 , 0.005074 , 0.143881 , 0.004875 , 0.205333,
        0.001852 , 0.002184 , 0.002167 , 0.163804 , 0.002132 , 0.212644 , 0.003431 , 0.244546 , 0.004205 , 0.315848,
        0.002450 , 0.002360 , 0.002243 , 0.154635 , 0.002246 , 0.148259 , 0.002239 , 0.348694 , 0.002265 , 0.368426,
        0.002321 , 0.002393 , 0.002376 , 0.074124 , 0.002439 , 0.126918 , 0.002453 , 0.439270 , 0.002416 , 0.489812,
        0.002484 , 0.002629 , 0.002559 , 0.150246 , 0.002579 , 0.140103 , 0.002548 , 0.493103 , 0.002637 , 0.509481,
        0.002960 , 0.002952 , 0.002880 , 0.294884 , 0.002758 , 0.332805 , 0.002727 , 0.455842 , 0.002816 , 0.431807,
        0.003099 , 0.003028 , 0.002927 , 0.387154 , 0.002899 , 0.397946 , 0.002957 , 0.261333 , 0.002909 , 0.148548,
        0.004887 , 0.004884 , 0.006581 , 0.414647 , 0.003735 , 0.431317 , 0.006426 , 0.148997 , 0.003736 , 0.080715,
        0.001969 , 0.002159 , 0.002325 , 0.200211 , 0.002288 , 0.202137 , 0.002289 , 0.595331 , 0.002311 , 0.636097
     );

    const float4 pointLightColor = float4(1.0,1.0,1.0,1.0);
    const float3 g_PointLightPos = float3(  3.7,5.8,3.15);
    const float3 g_PointLightPos2 = float3(-3.7,5.8,3.15);

    const float g_fXOffset = 0;
    const float g_fXScale = 0.6366198; //1/(PI/2)
    const float g_fYOffset = 0;
    const float g_fYScale = 0.5;

    const float g_20XOffset = 0;
    const float g_20XScale = 0.6366198; //1/(PI/2)
    const float g_20YOffset = 0;
    const float g_20YScale = 0.5;



    const float g_diffXOffset = 0;
    const float g_diffXScale = 0.5;
    const float g_diffYOffset = 0;
    const float g_diffYScale = 0.3183099;  //1/PI

    void GenRainSpriteVertices(float3 worldPos, float3 velVec, float3 eyePos, out float3 outPos[4])
    {
        float height = g_SpriteSize/2.0;
        float width = height/10.0;

        velVec = normalize(velVec);
        float3 eyeVec = eyePos - worldPos;
        float3 eyeOnVelVecPlane = eyePos - ((dot(eyeVec, velVec)) * velVec);
        float3 projectedEyeVec = eyeOnVelVecPlane - worldPos;
        float3 sideVec = normalize(cross(projectedEyeVec, velVec));

        outPos[0] =  worldPos - (sideVec * 0.5*width);
        outPos[1] = outPos[0] + (sideVec * width);
        outPos[2] = outPos[0] + (velVec * height);
        outPos[3] = outPos[2] + (sideVec * width );
    }

    float3 phaseFunctionSchlick(float cosTheta)
    {
       float k = -0.2;
       float p = (1.0-k*k)/(pow(1.0+k*cosTheta,2.0) );
       return float3(p,p,p);
    }

    //---------------------------------------------------------------------------------------
    //auxiliary functions for calculating the Fog
    //---------------------------------------------------------------------------------------
    float3 calculateAirLightPointLight(float Dvp,float Dsv,float3 S,float3 V)
    {
        float gamma = acos(dot(S, V));
        gamma = clamp(gamma,0.01,PI-0.01);
        float sinGamma = sin(gamma);
        float cosGamma = cos(gamma);
        float u = g_beta.x * Dsv * sinGamma;
        float v1 = 0.25*PI+0.5*atan((Dvp-Dsv*cosGamma)/(Dsv*sinGamma));
        float v2 = 0.5*gamma;

        float lightIntensity = g_PointLightIntensity * 100;

        float f1= textureLod(Ftable, float2((v1-g_fXOffset)*g_fXScale, (u-g_fYOffset)*g_fYScale), 0.0).r;   // samLinearClamp
        float f2= textureLod(Ftable, float2((v2-g_fXOffset)*g_fXScale, (u-g_fYOffset)*g_fYScale), 0.0).r;   // samLinearClamp
        float airlight = (g_beta.x*lightIntensity*exp(-g_beta.x*Dsv*cosGamma))/(2*PI*Dsv*sinGamma)*(f1-f2);

        return airlight.xxx;
    }