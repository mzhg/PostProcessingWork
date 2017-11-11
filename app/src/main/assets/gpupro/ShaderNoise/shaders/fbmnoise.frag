#version 120
//
// Description : Array and textureless GLSL 3D simplex noise function.
//      Author : Ian McEwan, Ashima Arts.
//  Maintainer : ijm
//     Lastmod : 20110409 (stegu)
//     License : Copyright (C) 2011 Ashima Arts. All rights reserved.
//               Distributed under the MIT License. See LICENSE file.
//

// #include "snoise3.glsl"
#include "../../../shader_libs/NoiseLib.glsl"

varying vec3 vTexCoord3D;

void main( void )
{
  float n = abs(SimplexNoise(vTexCoord3D));  // snoise
  n += 0.5 * abs(SimplexNoise(vTexCoord3D * 2.0));
  n += 0.25 * abs(SimplexNoise(vTexCoord3D * 4.0));
  n += 0.125 * abs(SimplexNoise(vTexCoord3D * 8.0));
  
  gl_FragColor = vec4(vec3(1.5-n, 1.0-n, 0.5-n), 1.0);
}
