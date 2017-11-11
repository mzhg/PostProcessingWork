//
// Description : Array and textureless GLSL 3D simplex noise function.
//      Author : Ian McEwan, Ashima Arts.
//  Maintainer : ijm
//     Lastmod : 20110409 (stegu)
//     License : Copyright (C) 2011 Ashima Arts. All rights reserved.
//               Distributed under the MIT License. See LICENSE file.
//

#version 120
// #include "snoise3.glsl"
#include "../../../shader_libs/NoiseLib.glsl"
varying vec3 vTexCoord3D;

void main( void )
{
  float n = SimplexNoise(vTexCoord3D);

  gl_FragColor = vec4(0.5 + 0.6 * vec3(n, n, n), 1.0);
}
