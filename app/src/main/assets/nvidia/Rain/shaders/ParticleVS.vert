//float3 pos              : POSITION;         //position of the particle
//    float3 seed             : SEED;
//    float3 speed            : SPEED;
//    float random            : RAND;
//    uint   Type             : TYPE;             //particle type

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
    vs.pos =In_Pos;
    vs.seed =In_Seed;
    vs.speed =In_Speed;
    vs.random =In_Random;
    vs.Type =In_Type;
}