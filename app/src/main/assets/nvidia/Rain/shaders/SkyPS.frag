#include "Rain_Common.glsl"

in float3 m_worldPos;

layout(location = 0) out vec4 Out_f4Color;

void main()
{
    float4 sceneColor =  float4(0,0,0,0);

    float3 viewVec = m_worldPos - g_eyePos ;
    float Dvp = 50;
    float3 V =  normalize(viewVec);
    float3 exDir = float3( exp(-g_beta.x*Dvp),  exp(-g_beta.y*Dvp),  exp(-g_beta.z*Dvp)  );

    //directional light
    float3 SDir = normalize( g_eyePos - g_lightPos);
    float cosGammaDir = dot(SDir, -V);
    float3 diffuseDirLight = dirLightIntensity*exDir;
    float3 dirAirLight = phaseFunctionSchlick(cosGammaDir)*
                         dirLightIntensity*float3(1-exDir.x,1-exDir.y,1-exDir.z);

    // air light
    float3 viewRay = normalize(float3(gl_FragCoord.x - g_ScreenWidth/2.0, gl_FragCoord.y - g_ScreenHeight/2.0,-g_de));
    float3 airlight = calculateAirLightPointLight(Dvp,g_DSVPointLight,g_ViewSpaceLightVec,viewRay);
    float3 airlight2 = calculateAirLightPointLight(Dvp,g_DSVPointLight2,g_ViewSpaceLightVec2,viewRay);

    float3 airlightColor = airlight + airlight2 + dirAirLight;
    Out_f4Color = float4( airlightColor.xyz + sceneColor.xyz*diffuseDirLight.xyz, 1);
}