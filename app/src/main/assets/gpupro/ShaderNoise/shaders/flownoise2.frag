//
// GLSL implementation of 2D "flow noise" as presented
// by Ken Perlin and Fabrice Neyret at Siggraph 2001.
// (2D simplex noise with analytic derivatives and
// in-plane rotation of generating gradients,
// in a fractal sum where higher frequencies are
// displaced (advected) by lower frequencies in the
// direction of their gradient. For details, please
// refer to the 2001 paper "Flow Noise" by Perlin and Neyret.)
//
// Author: Stefan Gustavson (stefan.gustavson@liu.se)
// Distributed under the terms of the MIT license.
// See LICENSE file for details.
//
#version 120
#include "../../../shader_libs/NoiseLib.glsl"
varying vec2 vTexCoord2D;
uniform float time;
// #include "srdnoise2.glsl"

void main(void) {
  vec2 g1, g2;
  vec2 p = vTexCoord2D;
  float n1 = FlowNoise(p*0.5, 0.2*time, g1);
  float n2 = FlowNoise(p*2.0 + g1*0.5, 0.51*time, g2);
  float n3 = FlowNoise(p*4.0 + g1*0.5 + g2*0.25, 0.77*time, g2);
  gl_FragColor = vec4(vec3(0.4, 0.5, 0.6) + vec3(n1+0.75*n2+0.5*n3), 1.0);
}
