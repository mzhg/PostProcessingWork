#include "../../../shader_libs/ShadowWork/ShadowWork.glsl"
#include "../../../shader_libs/ShadowWork/ShadowPercentageCloserFiltering.glsl"

uniform int g_useTexture;
uniform vec3 g_podiumCenterWorld;
uniform vec3 g_lightPos;
uniform bool g_useDiffuse;

// xy:  ; z: SoftTransitionScale
uniform vec4 PCSSParameters;

uniform mat4 ScreenToShadowMatrix;
// .x:DepthBias, .y:SlopeDepthBias, .z:ReceiverBias, .w: MaxSubjectZ - MinSubjectZ
uniform vec4 ProjectionDepthBiasParameters;
uniform vec2 InvViewpport = 1.0f/vec2(1280, 720);

in vec4 worldPosition;
in vec4 lightPosition;
in vec3 normal;

layout(binding = 0) uniform sampler2D g_rockDiffuse;
layout(binding = 1) uniform sampler2D g_groundDiffuse;
layout(binding = 2) uniform sampler2D g_groundNormal;
layout(binding = 3) uniform sampler2D g_ShadowDepth;
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
    vec3 lightDir = normalize(g_lightPos - worldPos);
    if (g_useTexture == 1)
    {
        vec2 uv = (worldPos.xz * 0.5f + 0.5f) * 2.0f;
        vec3 diffuse = texture(g_groundDiffuse, uv).xyz;
        normal = texture(g_groundNormal, uv).xzy * 2.0f - 1.0f;
        diffuse *= max(dot(lightDir, normal), 0.0f);
        diffuse *= pow(dot(lightDir, normalize(g_lightPos)), 40.0f);
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

#define USE_PCSS 1
#define SPOT_LIGHT_PCSS 0

void main()
{
//    vec2 uv = lightPosition.xy / lightPosition.w;
//    float z = lightPosition.z / lightPosition.w;

    // Compute gradient using ddx/ddy before any branching
//    vec2 dz_duv = depthGradient(uv, z);

    FPCSSSamplerSettings Settings;
    float shadow = 1;

#if USE_PCSS
    #if SPOT_LIGHT_PCSS
    {
        float CotanOuterCone = DeferredLightUniforms.SpotAngles.x * rsqrt(1. - DeferredLightUniforms.SpotAngles.x * DeferredLightUniforms.SpotAngles.x);
        float WorldLightDistance = dot(DeferredLightUniforms.Direction, DeferredLightUniforms.Position - WorldPosition);
        Settings.ProjectedSourceRadius = 0.5 * DeferredLightUniforms.SourceRadius * CotanOuterCone / WorldLightDistance;
        Settings.TanLightSourceAngle = 0;
    }
    #else
    {
        Settings.ProjectedSourceRadius = 0;
        Settings.TanLightSourceAngle = PCSSParameters.x;
    }
    #endif
//    Settings.ShadowDepthTexture = g_ShadowDepth;
//    Settings.ShadowDepthTextureSampler = ShadowDepthTextureSampler;
    ivec2 shadowMapSize = textureSize(g_ShadowDepth, 0);
    vec2 shadowTexelSize = 1.0f/vec2(shadowMapSize);

    vec4 SVPos = gl_FragCoord;
    vec2 ScreenUV = SVPos.xy * InvViewpport;
    vec4 ScreenPosition = vec4(ScreenUV * 2 - 1, gl_FragCoord.z * 2 - 1, 1);

    vec4 lightViewPos = g_lightView * worldPosition;
    vec4 lightProjPos = g_lightProj * lightViewPos;

    vec3 ShadowPosition = (lightProjPos.xyz / lightProjPos.w) * 0.5 + 0.5;

    float ShadowZ = ShadowPosition.z;
    // Clamp pixel depth in light space for shadowing opaque, because areas of the shadow depth buffer that weren't rendered to will have been cleared to 1
    // We want to force the shadow comparison to result in 'unshadowed' in that case, regardless of whether the pixel being shaded is in front or behind that plane
    float LightSpacePixelDepthForOpaque = min(ShadowZ, 0.99999f);

    float3 ScreenPositionDDX = ddx(ScreenPosition.xyz);
    float3 ScreenPositionDDY = ddy(ScreenPosition.xyz);
    float4 ShadowPositionDDX = mul(float4(ScreenPositionDDX, 0), ScreenToShadowMatrix);
    float4 ShadowPositionDDY = mul(float4(ScreenPositionDDY, 0), ScreenToShadowMatrix);
    #if SPOT_LIGHT_PCSS
    // perspective correction for derivatives, could be good enough and way cheaper to just use ddx(ScreenPosition)
    ShadowPositionDDX.xyz -= ShadowPosition.xyz * ShadowPositionDDX.w;
    ShadowPositionDDY.xyz -= ShadowPosition.xyz * ShadowPositionDDY.w;
    #endif

    Settings.ShadowBufferSize = vec4(shadowMapSize, shadowTexelSize);
    Settings.ShadowTileOffsetAndSize = vec4(0,0,1,1);
    Settings.SceneDepth = LightSpacePixelDepthForOpaque;
    Settings.TransitionScale = PCSSParameters.z;
    Settings.MaxKernelSize = PCSSParameters.y;
    Settings.SvPosition = SVPos.xy;
    Settings.PQMPContext = float2(0);
    Settings.DebugViewportUV = ScreenUV;
    Settings.StateFrameIndexMod8 = 0;

    shadow = DirectionalPCSS(g_ShadowDepth, Settings, ShadowPosition.xy, ShadowPositionDDX.xyz, ShadowPositionDDY.xyz);
#else
    shadow = CaculateShadows(worldPosition.xyz, g_ShadowMap, g_ShadowDepth);
#endif
    OutColor = shade(worldPosition.xyz, normal) * shadow;


}