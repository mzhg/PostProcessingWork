#include "Rain_Common.glsl"

layout(location = 0) in vec3 In_Pos;
layout(location = 1) in vec3 In_Seed;
layout(location = 2) in vec3 In_Speed;
layout(location = 3) in float In_Random;
layout(location = 4) in uint In_Type;

out Particle
{
  vec3 pos              ; // POSITION;         //position of the particle
  vec3 seed             ;// SEED;
  vec3 speed            ;// SPEED;
  float random            ;// RAND;
  uint   Type             ;// TYPE;
}vs;

void main()
{
    if(moveParticles)
    {
         //move forward
         vs.pos.xyz = In_Pos + In_Seed.xyz/g_FrameRate + g_TotalVel.xyz;

         //if the particle is outside the bounds, move it to random position near the eye
         if(vs.pos.y <=  g_eyePos.y-g_heightRange )
         {
            float x = In_Seed.x + g_eyePos.x;
            float z = In_Seed.z + g_eyePos.z;
            float y = In_Seed.y + g_eyePos.y;
            vs.pos = float3(x,y,z);
         }
    }

    vs.seed = In_Seed;
    vs.speed = In_Speed;
    vs.random = In_Random;
    vs.Type = In_Type;
}