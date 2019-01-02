#include "globals.glsl"

/*struct GS_OUTPUT
{
  float4 position: SV_POSITION;
  float2 texCoords: TEXCOORD;
};

struct FS_OUTPUT
{
  float4 fragColor: SV_TARGET;
};*/

out float4 fragColor;
#define SKY_COLOR float4(0.26f,0.49f,0.92f,1.0f)

//FS_OUTPUT main(GS_OUTPUT input)
void main()
{
	fragColor = SKY_COLOR;
}