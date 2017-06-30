#include "Rain_Common.glsl"

in VS_OUTPUT_SCENE
{
//    float4 Position            : SV_POSITION;
    float3 Normal;//              : NORMAL;
    float3 Tan;//                 : TANGENT;
    float4 worldPos;//            : WPOSITION;
    float2 Texture;//             : TEXTURE0;
}In;

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

    float f1= Ftable.SampleLevel(samLinearClamp, float2((v1-g_fXOffset)*g_fXScale, (u-g_fYOffset)*g_fYScale), 0);
    float f2= Ftable.SampleLevel(samLinearClamp, float2((v2-g_fXOffset)*g_fXScale, (u-g_fYOffset)*g_fYScale), 0);
    float airlight = (g_beta.x*lightIntensity*exp(-g_beta.x*Dsv*cosGamma))/(2*PI*Dsv*sinGamma)*(f1-f2);

    return airlight.xxx;
}

float3 calculateDiffusePointLight(float Kd,float Dvp,float Dsv,float3 pointLightDir,float3 N,float3 V)
{

    float Dsp = length(pointLightDir);
    float3 L = pointLightDir/Dsp;
    float thetas = acos(dot(N, L));
    float lightIntensity = g_PointLightIntensity * 100;

    //spotlight
    float angleToSpotLight = dot(-L, g_SpotLightDir);
    if(g_useSpotLight)
    {    if(angleToSpotLight > g_cosSpotlightAngle)
             lightIntensity *= abs((angleToSpotLight - g_cosSpotlightAngle)/(1-g_cosSpotlightAngle));
         else
             lightIntensity = 0;
    }

    //diffuse contribution
    float t1 = exp(-g_beta.x*Dsp)*max(cos(thetas),0.0)/Dsp;
    float4 t2 = g_beta.x*textureLod(Gtable, float2((g_beta.x*Dsp-g_diffXOffset)*g_diffXScale, (thetas-g_diffYOffset)*g_diffYScale),0.0)/(2*PI);  // samLinearClamp
    float rCol = (t1+t2.x)*exp(-g_beta.x*Dvp)*Kd*lightIntensity/Dsp;
    float diffusePointLight = float3(rCol,rCol,rCol);
    return diffusePointLight.xxx;
}

float3 Specular(float lightIntensity, float Ks, float Dsp, float Dvp, float specPow, float3 L, float3 VReflect)
{
    lightIntensity = lightIntensity * 100.0;
    float LDotVReflect = dot(L,VReflect);
    float thetas = acos(LDotVReflect);

    float t1 = exp(-g_beta*Dsp)*pow(max(LDotVReflect,0.0),specPow)/Dsp;
    float4 t2 = g_beta.x*textureLod(G_20table, float2((g_beta.x*Dsp-g_20XOffset)*g_20XScale, (thetas-g_20YOffset)*g_20YScale),0.0)/(2*PI);  // samLinearClamp
    float specular = (t1+t2.x)*exp(-g_beta.x*Dvp)*Ks*lightIntensity/Dsp;
    return specular.xxx;
}


float3 phaseFunctionSchlick(float cosTheta)
{
   float k = -0.2;
   float p = (1.0-k*k)/(pow(1+k*cosTheta,2.0) );
   return float3(p,p,p);
}

//pixel shader for the scene
//float4 PSScene(VS_OUTPUT_SCENE In) : SV_Target
layout(location = 0) out float4 Out_f4Color;
void main()
{
    float4 outputColor;

    float4 sceneColor = texture( SceneTextureDiffuse, In.Texture );   // samLinear
    float3 viewVec = In.worldPos - g_eyePos;
    float Dvp = length(viewVec);
    float3 V =  viewVec/Dvp;
    float3 exDir = float3( exp(-g_beta.x*Dvp),  exp(-g_beta.y*Dvp),  exp(-g_beta.z*Dvp)  );
    float4 sceneSpecular = texture(SceneTextureSpecular, In.Texture );   // samLinear

    //perturb the normal based on the surface and the rain-----------------------------

    float3 Tan = normalize(In.Tan);
    float3 InNormal = normalize(In.Normal);
    float wetSurf = saturate(g_KsDir/2.0*saturate(InNormal.y));

    float4 normalMap = texture( SceneTextureNormal, In.Texture );  // samAniso
    float3 norm = float3(normalMap.ga*2.0-1.0, 0);
    norm.z = sqrt( 1 - norm.x*norm.x + norm.y*norm.y );
    float3 binorm = normalize( cross( InNormal, Tan ) );
    if( dot( normalize(In.worldPos) ,binorm) < 0 )
        binorm = -binorm;
    float3x3 BTNMatrix = float3x3( binorm, Tan, InNormal );
    float3 N = normalize(mul( norm, BTNMatrix ));


    //add the normal map from the rain bumps
    //based on the direction of the surface and the amount of rainyness
    float4 BumpMapVal = textureLod(SplashBumpTexture,  // samAnisoMirror TODO Lod ???
                       float2(In.worldPos.x/2.0 + g_splashXDisplace, In.worldPos.z/2.0 + g_splashYDisplace), g_timeCycle) - 0.5;
    N += wetSurf * 2.0 * (BumpMapVal.x * Tan + BumpMapVal.y * binorm);
    N = normalize(N);
    float3 splashDiffuse = wetSurf * textureLod(SplashDiffuseTexture, In.worldPos.xz, g_timeCycle);  // samAnisoMirror

    //reflection of the scene-----------------------------------------------------------
    float3 reflVect = reflect(V, N);

    //directional light-----------------------------------------------------------------
    float3 lightDir = g_lightPos - In.worldPos;
    float3 lightDirNorm = normalize(lightDir);
    float3 SDir = normalize( g_lightPos - g_eyePos);
    float cosGammaDir = dot(SDir, V);
    float dirLighting = g_Kd*dirLightIntensity*saturate( dot( N,lightDirNorm ) );
    //diffuse
    float3 diffuseDirLight = dirLighting*exDir;
    //airlight
    float3 dirAirLight = phaseFunctionSchlick(cosGammaDir)* dirLightIntensity*float3(1-exDir.x,1-exDir.y,1-exDir.z);
    //specular
    float3 specularDirLight = saturate( pow(  dot(lightDirNorm,reflVect),g_specPower)) * dirLightIntensity * g_KsDir * exDir;

    //point light 1---------------------------------------------------------------------
    //diffuse surface radiance and airlight due to point light
    float3 pointLightDir = g_PointLightPos - In.worldPos;
    //diffuse
    float3 diffusePointLight1 = calculateDiffusePointLight(0.1,Dvp,g_DSVPointLight,pointLightDir,N,V);
    //airlight
    float3 airlight1 = calculateAirLightPointLight(Dvp,g_DSVPointLight,g_VecPointLightEye,V);
    //specular
    float3 specularPointLight = Specular(g_PointLightIntensity, g_KsPoint, length(pointLightDir), Dvp, g_specPower, normalize(pointLightDir), reflVect);

    //point light 2---------------------------------------------------------------------
    //diffuse surface radiance
    float3 diffusePointLight2 = float3(0,0,0);
    float3 pointLightDir2 = g_PointLightPos2 - In.worldPos;
    //diffuse
    diffusePointLight2 = calculateDiffusePointLight(0.1,Dvp,g_DSVPointLight2,pointLightDir2,N,V);
    //airlight
    float3 airlight2 = calculateAirLightPointLight(Dvp,g_DSVPointLight2,g_VecPointLightEye2,V);
    //specular
    float3 specularPointLight2 = Specular(g_PointLightIntensity, g_KsPoint, length(pointLightDir2), Dvp, g_specPower, normalize(pointLightDir2), reflVect);


    float3 airlightColor = airlight1 + airlight2 + dirAirLight;

    outputColor = float4( airlightColor.xyz +
                          sceneColor.xyz*(diffusePointLight1.xyz + diffusePointLight2.xyz + diffuseDirLight.xyz) +
                          (splashDiffuse + sceneSpecular.xyz)*(specularDirLight + specularPointLight + specularPointLight2) ,1);

     //if this is a lamp make it emissive
    if(sceneColor.x > 0.9 && sceneColor.y> 0.9)
        outputColor = float4(1,1,1,1)*g_PointLightIntensity*20;

    Out_f4Color = outputColor;
}