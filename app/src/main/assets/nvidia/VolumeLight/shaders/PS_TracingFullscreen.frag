
#include "../../../shader_libs/PostProcessingHLSLCompatiable.glsl"

in vec4 m_f4UVAndScreenPos;
out float Out_Light;

uniform float g_BufferWidthInv;
uniform float g_BufferHeightInv;
uniform bool useZOptimizations;

uniform mat4 g_MWorldViewProjectionInv;
uniform mat4 g_ModelLightProj;
uniform float3 g_EyePosition;
uniform float g_ZNear;
uniform bool g_UseAngleOptimization;
uniform float g_rSamplingRate;

layout(binding = 0) uniform sampler2D s0;
layout(binding = 1) uniform sampler2D DepthBufferTexture;
layout(binding = 2) uniform sampler2D NoiseTexture;

#define int2 ivec2

void main()
{
    float sceneDepth = texture(DepthBufferTexture, m_f4UVAndScreenPos.xy ).x;   // samplerPoint
    	
    float4 clipPos;
    clipPos.x = 2.0 * m_f4UVAndScreenPos.x - 1.0;
    clipPos.y = 2.0 * m_f4UVAndScreenPos.y - 1.0;
    clipPos.z = 2.0 * sceneDepth - 1.0;
    clipPos.w = 1.0;
                        
    // World space position of the texel from the depth buffer
    float4 positionWS = mul( clipPos, g_MWorldViewProjectionInv );
    positionWS.w = 1.0 / positionWS.w;
    positionWS.xyz *= positionWS.w;
    
    float3 vecForward = normalize( positionWS.xyz - g_EyePosition.xyz );
    float traceDistance = dot( positionWS.xyz - ( g_EyePosition.xyz + vecForward * g_ZNear ), vecForward );
    traceDistance = clamp( traceDistance, 0.0, 2500.0 ); // Far trace distance
    
    positionWS.xyz = g_EyePosition.xyz + vecForward * g_ZNear;

    if( g_UseAngleOptimization )
    {
        float dotViewLight = dot( vecForward, g_LightForward );
        vecForward *= exp( dotViewLight * dotViewLight );
    }
        
    vecForward *= g_rSamplingRate * 2.0;
    int stepsNum = min( int(traceDistance / length( vecForward )), MAX_STEPS );

    // Add jittering
    float jitter = texture(NoiseTexture, m_f4UVAndScreenPos.xy ).x;   // samplerLinear

    float step = length( vecForward );
    float scale = step * 0.0005; // Set base brightness factor
    float4 shadowUV;
    float3 coordinates;
    
    // Calculate coordinate delta ( coordinate step in ligh space )
    float3 curPosition = positionWS.xyz + vecForward * jitter;
    shadowUV = mul( float4( curPosition, 1.0 ), g_ModelLightProj );
    coordinates = shadowUV.xyz / shadowUV.w;
    coordinates.x = ( coordinates.x + 1.0 ) * 0.5;
    coordinates.y = ( 1.0 - coordinates.y ) * 0.5;
    coordinates.z = dot( curPosition - g_LightPosition, g_LightForward );

    curPosition = positionWS.xyz + vecForward * ( 1.0 + jitter );
    shadowUV = mul( float4( curPosition, 1.0 ), g_ModelLightProj );
    float3 coordinateEnd = shadowUV.xyz / shadowUV.w;
    coordinateEnd.x = ( coordinateEnd.x + 1.0 ) * 0.5;
    coordinateEnd.y = ( 1.0 - coordinateEnd.y ) * 0.5;
    coordinateEnd.z = dot( curPosition - g_LightPosition, g_LightForward );

    float3 coordinateDelta = coordinateEnd - coordinates;

    float2 vecForwardProjection;
    vecForwardProjection.x = dot( g_LightRight, vecForward );
    vecForwardProjection.y = dot( g_LightUp, vecForward );

    // Calculate coarse step size
    float longStepScale = int( g_CoarseDepthTexelSize / length( vecForwardProjection ) );
    longStepScale = max( longStepScale, 1. );
    
    float sampleFine;
    float2 sampleMinMax;
    float light = 0.0;
    float coordinateZ_end;
    float isLongStep;
    float longStepScale_1 = longStepScale - 1;

    float longStepsNum = 0; 
    float realStepsNum = 0;
    
    for( uint i = 0; i < stepsNum; i++ )
    {
        sampleMinMax = s0.SampleLevel( samplerDepthMinMax, coordinates.xy, 0 ).xy;
        
        // Use point sampling. Linear sampling can cause the whole coarse step being incorrect
        sampleFine = DepthTexture.SampleCmpLevelZero( samplerPoint_Less, coordinates.xy, coordinates.z );

        float zStart = s1.SampleLevel( samplerPoint, coordinates.xy, 0 );
        
        const float transactionScale = 100.0f;
        
        // Add some attenuation for smooth light fading out
        float attenuation = ( coordinates.z - zStart ) / ( ( sampleMinMax.y + transactionScale ) - zStart );
        attenuation = saturate( attenuation );
        attenuation = 1.0 - attenuation;
        attenuation *= attenuation;

        float attenuation2 = ( ( zStart + transactionScale ) - coordinates.z ) * ( 1.0 / transactionScale );
        attenuation2 = 1.0 - saturate( attenuation2 );

        attenuation *= attenuation2;
        
        // Use this value to incerase light factor for "indoor" areas
        float density = s1.SampleCmpLevelZero( samplerPoint_Greater, coordinates.xy, coordinates.z );
        density *= 10.0 * attenuation;
        density += 0.25;
        sampleFine *= density;
        
        coordinateZ_end = coordinates.z + coordinateDelta.z * longStepScale;
        
        float comparisonValue = max( coordinates.z, coordinateZ_end );
        float isLight = float(comparisonValue < sampleMinMax.x); // .x stores min depth values
        
        comparisonValue = min( coordinates.z, coordinateZ_end );
        float isShadow = float(comparisonValue > sampleMinMax.y); // .y stores max depth values
        
        // We can perform coarse step if all samples are in light or shadow
        isLongStep = isLight + isShadow;

        longStepsNum += isLongStep;
        realStepsNum += 1.0;

        if( useZOptimizations )
        {
            light += scale * sampleFine * ( 1.0 + isLongStep * longStepScale_1 ); // longStepScale should be >= 1 if we use a coarse step

            coordinates += coordinateDelta * ( 1.0 + isLongStep * longStepScale_1 );
            i += isLongStep * longStepScale_1;
        }
        else
        {
            light += scale * sampleFine;
            coordinates += coordinateDelta;
        }
    }

    // Do correction for final coarse steps.
    if( useZOptimizations )
    {
        light -= scale * sampleFine * ( i - stepsNum );
    }

    //return longStepsNum / realStepsNum;
    //return light * cos( light );
    Out_Light = light;
}