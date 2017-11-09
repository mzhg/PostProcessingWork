uniform int g_useTexture;
uniform vec3 g_podiumCenterWorld;
uniform bool g_useDiffuse;
uniform vec3 gLightPos;

in vec4 worldPosition;
in vec4 lightPosition;
in vec3 normal;

in float fDepth;
in vec4 lightViewPos;
in vec3 wLight;

layout(binding = 0) uniform sampler2D g_rockDiffuse;
layout(binding = 1) uniform sampler2D g_groundDiffuse;
layout(binding = 2) uniform sampler2D g_groundNormal;
layout(binding = 3) uniform sampler2D g_ShadowDepth;   // VarianceShadowMap
layout(binding = 4) uniform sampler2DShadow g_ShadowMap;

bool isBlack(vec3 c)
{
    return (dot(c, c) == 0.0f);
}

vec2 cubeMapTexCoords(vec3 v)
{
    vec2 uv;
    if (abs(v.x) > abs(v.y) && abs(v.x) > abs(v.z))
        uv = vec2(v.y / abs(v.x), v.z / abs(v.x));
    if (abs(v.y) > abs(v.x) && abs(v.y) > abs(v.z))
        uv = vec2(v.x / abs(v.y), v.z / abs(v.y));
    else
        uv = vec2(v.x / abs(v.z), v.y / abs(v.z));
    return uv * 0.5f + 0.5f;
}

vec4 shade(vec3 worldPos, vec3 normal)
{
    vec3 lightDir = normalize(gLightPos - worldPos);
    if (g_useTexture == 1)
    {
        vec2 uv = (worldPos.xz * 0.5f + 0.5f) * 2.0f;
        vec3 diffuse = texture(g_groundDiffuse, uv).xyz;
        normal = texture(g_groundNormal, uv).xzy * 2.0f - 1.0f;
        diffuse *= max(dot(lightDir, normal), 0.0f);
        diffuse *= pow(dot(lightDir, normalize(gLightPos)), 40.0f);
        return vec4(diffuse, 1.0f);
    }
    else if (g_useTexture == 2)
    {
        vec2 uv = cubeMapTexCoords(normalize(worldPos.xyz - g_podiumCenterWorld));
        vec3 diffuse = texture2D(g_rockDiffuse, uv).xyz * 1.2f;
        diffuse *= max(dot(lightDir, normal), 0.0f);
        return vec4(diffuse, 1.0f);
    }
    else
    {
        float x = max(dot(lightDir, normal), 0.0f);
        vec4 diffuse = vec4(x, x, x, 1.0f);
        return g_useDiffuse ? diffuse : vec4(1.0f, 1.0f, 1.0f, 1.0f);
    }
}

layout(location = 0) out vec4 OutColor;
uniform bool bShowVariance = false;
uniform bool bShowMD = false;
uniform bool bShowCheb = false;
uniform bool bVSM = false;
uniform float fFilterWidth = 8.0;

vec4 VSM_DEBUG( vec2 tex, float fragDepth )
{
//    float2 moments = texShadowMap.Sample( FILT_LINEAR, tex );
    vec2 moments = texture(g_ShadowDepth, tex).rg;

    float E_x2 = moments.y;
    float Ex_2 = moments.x * moments.x;
    float variance = E_x2 - Ex_2;
    float mD = (moments.x - fragDepth );
    float mD_2 = mD * mD;
    float p = variance / (variance + mD_2 );

	return vec4( bShowVariance ? variance : 0.0f, bShowMD ? mD_2 : 0.0f, bShowCheb ? p : 0.0f, 1.0f );
}

float BoxFilterStart( float fWidth )  //Assumes filter is odd
{
    return ( ( fWidth - 1.0f ) / 2.0f );
}

float PCF_FILTER( vec2 tex, float fragDepth )
{
    ivec2 texSize = textureSize(g_ShadowMap, 0);
    float fTextureWidth = float(texSize.x);
    //PreShader - This should all be optimized away by the compiler
    //====================================
    float fStartOffset = BoxFilterStart( fFilterWidth );
    float texOffset = 1.0f / fTextureWidth;
    //====================================

    fragDepth -= 0.0001f;
    tex -= fStartOffset * texOffset;

    float lit = 0.0f;
    for( float i = 0.0; i < fFilterWidth; i+= 1.0 )
        for( float j = 0.0; j < fFilterWidth; j+=1.0 )
        {
//            lit += texShadowMap.SampleCmpLevelZero( FILT_PCF,    float2( tex.x + i * texOffset, tex.y + j * texOffset ), fragDepth );
            lit += texture(g_ShadowMap, vec3(tex.x + i * texOffset, tex.y + j * texOffset, fragDepth));
        }
	return lit / ( fFilterWidth * fFilterWidth );
}

float VSM_FILTER( vec2 tex, float fragDepth )
{
    float lit = 0.0f;
    vec2 moments = texture( g_ShadowDepth,    tex).rg;

    float E_x2 = moments.y;
    float Ex_2 = moments.x * moments.x;
    float variance = E_x2 - Ex_2;
    float mD = (moments.x - fragDepth );
    float mD_2 = mD * mD;
    float p = variance / (variance + mD_2 );
    lit = max( p, float(fragDepth <= moments.x) );

    return lit;
}

void main()
{
    /*input.lightViewPos.xy /= input.lightViewPos.w;
    float2 tex = input.lightViewPos.xy * float2( 0.5f, -0.5f ) + 0.5f;*/
    vec3 tex = lightViewPos.xyz/lightViewPos.w * 0.5 + 0.5;

    if( bShowVariance || bShowMD || bShowCheb )  //Debug code for visualizing VSM
    {
        OutColor = VSM_DEBUG( tex.xy, tex.z );
        return;
    }

    float lit = 0.0f;
    if( bVSM )
        lit = VSM_FILTER( tex.xy, fDepth );
    else
        lit = PCF_FILTER( tex.xy, /*input.lightViewPos.z / input.lightViewPos.w*/tex.z);

    OutColor = shade(worldPosition.xyz, normal) * max(0. , lit);

}