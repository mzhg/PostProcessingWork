#include "Rain_Common.glsl"

in PSRainIn
{
//    float4 pos;// : SV_Position;
    float2 tex;// : TEXTURE0;
}_input;

layout(location = 0) out float4 Out_f4Color;

void main()
{
    Out_f4Color = float4(1,1,1,dirLightIntensity*g_ResponseDirLight*0.1);
}