#include "Rain_Common.glsl"

in PSSceneIn
{
//    float4 pos;// : SV_Position;
    float3 lightDir;//   : LIGHT;
    float3 pointLightDir;// : LIGHT2;
    float3 eyeVec;//     : EYE;
    float2 tex;// : TEXTURE0;
    uint type;//  : TYPE;
    float random;// : RAND;
}_input;

void rainResponse(/*PSSceneIn input,*/ float3 lightVector, float lightIntensity, float3 lightColor, float3 eyeVector, bool fallOffFactor, inout float4 rainResponseVal)
{

    float opacity = 0.0;

    float fallOff;
    if(fallOffFactor)
    {
        float distToLight = length(lightVector);
        fallOff = 1.0/( distToLight * distToLight);
        fallOff = saturate(fallOff);
    }
    else
    {  fallOff = 1;
    }

    if(fallOff > 0.01 && lightIntensity > 0.01 )
    {
        float3 dropDir = g_TotalVel;

        #define MAX_VIDX 4
        #define MAX_HIDX 8
        // Inputs: lightVector, eyeVector, dropDir
        float3 L = normalize(lightVector);
        float3 E = normalize(eyeVector);
        float3 N = normalize(dropDir);

        bool is_EpLp_angle_ccw = true;
        float hangle = 0;
        float vangle = abs( (acos(dot(L,N)) * 180/PI) - 90 ); // 0 to 90

        {
            float3 Lp = normalize( L - dot(L,N)*N );
            float3 Ep = normalize( E - dot(E,N)*N );
            hangle = acos( dot(Ep,Lp) ) * 180/PI;  // 0 to 180
            hangle = (hangle-10)/20.0;           // -0.5 to 8.5
            is_EpLp_angle_ccw = dot( N, cross(Ep,Lp)) > 0;
        }

        if(vangle>=88.0)
        {
            hangle = 0;
            is_EpLp_angle_ccw = true;
        }

        vangle = (vangle-10.0)/20.0; // -0.5 to 4.5

        // Outputs:
        // verticalLightIndex[1|2] - two indices in the vertical direction
        // t - fraction at which the vangle is between these two indices (for lerp)
        int verticalLightIndex1 = floor(vangle); // 0 to 5
        int verticalLightIndex2 = min(MAX_VIDX, (verticalLightIndex1 + 1) );
        verticalLightIndex1 = max(0, verticalLightIndex1);
        float t = frac(vangle);

        // textureCoordsH[1|2] used in case we need to flip the texture horizontally
        float textureCoordsH1 = _input.tex.x;
        float textureCoordsH2 = _input.tex.x;

        // horizontalLightIndex[1|2] - two indices in the horizontal direction
        // s - fraction at which the hangle is between these two indices (for lerp)
        int horizontalLightIndex1 = 0;
        int horizontalLightIndex2 = 0;
        float s = 0;

        s = frac(hangle);
        horizontalLightIndex1 = floor(hangle); // 0 to 8
        horizontalLightIndex2 = horizontalLightIndex1+1;
        if( horizontalLightIndex1 < 0 )
        {
            horizontalLightIndex1 = 0;
            horizontalLightIndex2 = 0;
        }

        if( is_EpLp_angle_ccw )
        {
            if( horizontalLightIndex2 > MAX_HIDX )
            {
                horizontalLightIndex2 = MAX_HIDX;
                textureCoordsH2 = 1.0 - textureCoordsH2;
            }
        }
        else
        {
            textureCoordsH1 = 1.0 - textureCoordsH1;
            if( horizontalLightIndex2 > MAX_HIDX )
            {
                horizontalLightIndex2 = MAX_HIDX;
            } else
            {
                textureCoordsH2 = 1.0 - textureCoordsH2;
            }
        }

        if( verticalLightIndex1 >= MAX_VIDX )
        {
            textureCoordsH2 = _input.tex.x;
            horizontalLightIndex1 = 0;
            horizontalLightIndex2 = 0;
            s = 0;
        }

        // Generate the final texture coordinates for each sample
        uint type = _input.type;
        uint2 texIndicesV1 = uint2(verticalLightIndex1*90 + horizontalLightIndex1*10 + type,
                                     verticalLightIndex1*90 + horizontalLightIndex2*10 + type);
        float3 tex1 = float3(textureCoordsH1, _input.tex.y, texIndicesV1.x);
        float3 tex2 = float3(textureCoordsH2, _input.tex.y, texIndicesV1.y);
        if( (verticalLightIndex1<4) && (verticalLightIndex2>=4) )
        {
            s = 0;
            horizontalLightIndex1 = 0;
            horizontalLightIndex2 = 0;
            textureCoordsH1 = _input.tex.x;
            textureCoordsH2 = _input.tex.x;
        }

        uint2 texIndicesV2 = uint2(verticalLightIndex2*90 + horizontalLightIndex1*10 + type,
                                     verticalLightIndex2*90 + horizontalLightIndex2*10 + type);
        float3 tex3 = float3(textureCoordsH1, _input.tex.y, texIndicesV2.x);
        float3 tex4 = float3(textureCoordsH2, _input.tex.y, texIndicesV2.y);

        // Sample opacity from the textures
        float col1 = rainTextureArray.Sample( samAniso, tex1) * g_rainfactors[texIndicesV1.x];
        float col2 = rainTextureArray.Sample( samAniso, tex2) * g_rainfactors[texIndicesV1.y];
        float col3 = rainTextureArray.Sample( samAniso, tex3) * g_rainfactors[texIndicesV2.x];
        float col4 = rainTextureArray.Sample( samAniso, tex4) * g_rainfactors[texIndicesV2.y];

        // Compute interpolated opacity using the s and t factors
        float hOpacity1 = lerp(col1,col2,s);
        float hOpacity2 = lerp(col3,col4,s);
        opacity = lerp(hOpacity1,hOpacity2,t);
        opacity = pow(opacity,0.7); // inverse gamma correction (expand dynamic range)
        opacity = 4*lightIntensity * opacity * fallOff;
    }

   rainResponseVal = float4(lightColor,opacity);

}

layout(location = 0) out float4 Out_f4Color;
//float4 PSRenderRain(PSSceneIn input) : SV_Target
void main()
{
      //return float4(1,0,0,0.1);

      //directional lighting---------------------------------------------------------------------------------
      float4 directionalLight;
      rainResponse(/*input,*/ _input.lightDir, 2.0*dirLightIntensity*g_ResponseDirLight*_input.random, float3(1.0,1.0,1.0), _input.eyeVec, false, directionalLight);

      //point lighting---------------------------------------------------------------------------------------
      float4 pointLight = float4(0,0,0,0);

      float3 L = normalize( _input.pointLightDir );
      float angleToSpotLight = dot(-L, g_SpotLightDir);

      if( !g_useSpotLight || g_useSpotLight && angleToSpotLight > g_cosSpotlightAngle )
          rainResponse(/*input,*/ _input.pointLightDir, 2*g_PointLightIntensity*g_ResponsePointLight*_input.random, pointLightColor.xyz, _input.eyeVec, true,pointLight);

      float totalOpacity = pointLight.a+directionalLight.a;
      Out_f4Color = float4( float3(pointLight.rgb*pointLight.a/totalOpacity + directionalLight.rgb*directionalLight.a/totalOpacity), totalOpacity);
}