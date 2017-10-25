#include "Fire_Common.glsl"

layout(location=0) out vec4 Out_Color;

in VolumeVertex
{
//    float4   ClipPos      : SV_Position;
    float3   Pos         /*: TEXCOORD0*/;      // vertex position in model space
    float3   RayDir      /*: TEXCOORD1*/;   // ray direction in model space
}In;

// Perlin fire based on 4D noise
void main()
{
    float3 Dir = normalize(In.Pos - EyePos) * StepSize;
    float Offset = bJitter ? texture(JitterTexture, gl_FragCoord.xy / 256.0).r : 0;  // SamplerRepeat

    // Jitter initial position
    float3 Pos = In.Pos + Dir * Offset;

    float3 resultColor = float3(0);
    float SceneZ = texelFetch(ScreenDepth, int2( gl_FragCoord.xy ), 0 ).x;

    while ( true )
    {
        float4 ClipPos = mul( float4( Pos, 1 ), WorldViewProj );
        float CurrentDepth = 0.5 * ClipPos.z/ClipPos.w + 0.5;

        // Break out of the loop if there's a blocking occluder
        if ( CurrentDepth > SceneZ || any( greaterThan(abs( Pos ),  float3(0.5)) ) )
            break;

        float4 NoiseCoord;
        NoiseCoord.xyz = Pos;
        NoiseCoord.y -= Time;
        NoiseCoord.w = Time * 0.5;

        // Evaluate turbulence function
        float Turbulence = abs( Turbulence4D( NoiseCoord * NoiseScale ) );

        float2 tc;
        tc.x = length( Pos.xz ) * 2;
        tc.y = 0.5 - Pos.y - Roughness * Turbulence * pow( ( 0.5 + Pos.y ), 0.5 );

        resultColor += StepSize * 12 * textureLod( FireShape, tc, 0.0 ).rgb;  // SamplerClamp

        Pos += Dir;
    }

    Out_Color = float4(resultColor, 1);
}