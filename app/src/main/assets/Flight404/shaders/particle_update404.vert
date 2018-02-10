#version 330

layout (location = 0) in vec3 loc;        // position vector
layout (location = 1) in vec3 vel;        // velocity vector
layout (location = 2) in float radius;    // particles's size
layout (location = 3) in float age;       // current age of particle
layout (location = 4) in float lifeSpan;  // max allowed age of particle
layout (location = 5) in float gen;       // number of times particle has been involved in a SPLIT
layout (location = 6) in float bounceAge; // amount to age particle when it bounces off floor
layout (location = 7) in uint type;       // the type of particle
layout (location = 8) in vec3 tail0;
layout (location = 9) in vec3 tail1;
layout (location = 10) in vec3 tail2;
layout (location = 11) in vec3 tail3;

out Particle
{
  vec3 loc;
  vec3 vel;
  float radius;
  float age;
  float lifeSpan;
  float gen;
  float bounceAge;
  uint type;
  
  vec3 tail0;
  vec3 tail1;
  vec3 tail2;
  vec3 tail3;
}vs;

void main(void)
{
    vs.loc = loc;
    vs.vel = vel;
    vs.radius = radius;
    vs.age = age;
    vs.lifeSpan = lifeSpan;
    vs.gen = gen;
    vs.bounceAge = bounceAge;
    vs.type = type;
    
    vs.tail0 = tail0;
    vs.tail1 = tail1;
    vs.tail2 = tail2;
    vs.tail3 = tail3;
}