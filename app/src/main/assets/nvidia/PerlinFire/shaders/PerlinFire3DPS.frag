#include "Fire_Common.glsl"

layout(location=0) out vec4 Out_Color;

in VolumeVertex
{
//    float4   ClipPos      : SV_Position;
    float3   Pos         /*: TEXCOORD0*/;      // vertex position in model space
    float3   RayDir      /*: TEXCOORD1*/;   // ray direction in model space
}In;

// Perlin fire based on 3D noise
void main()
{
    float3 Dir = normalize(In.RayDir) * StepSize;
    float Offset = bJitter ? texture( JitterTexture, gl_FragCoord.xy / 256.0 ).r : 0.0;  // SamplerRepeat

    // Jitter initial position
    float3 Pos = In.Pos + Dir * Offset;

    float3 resultColor = float3(0);
    float SceneZ =texelFetch(ScreenDepth, int2(gl_FragCoord.xy), 0).x;

    while ( true )
    {
        //float3 Pos = In.Pos + Dir * Offset + Dir * i;
        float4 ClipPos = mul( float4( Pos, 1 ), WorldViewProj );
        ClipPos.z /= ClipPos.w;
        ClipPos.z = 0.5 * ClipPos.z + 0.5;  // remap to [0, 1]

        // Break out of the loop if there's a blocking occluder or we're outside the fire volume
        if ( ClipPos.z > SceneZ || any( greaterThan(abs( Pos ),  float3(0.5)) ))
            break;

        float3 NoiseCoord = Pos;
        NoiseCoord.y -= Time;

        // Evaluate turbulence function
        float Turbulence = abs( Turbulence3D( NoiseCoord * NoiseScale ) );

        float2 tc;
        tc.x = length( Pos.xz ) * 2;
        tc.y = 0.5 - Pos.y - Roughness * Turbulence * pow( ( 0.5 + Pos.y ), 0.5 );

        resultColor += StepSize * 12 *
//                            FireShape.SampleLevel( SamplerClamp, tc, 0 );
                              textureLod(FireShape, tc,  0.0).rgb;

        Pos += Dir;
    }

    Out_Color =  float4(resultColor, 1);
}