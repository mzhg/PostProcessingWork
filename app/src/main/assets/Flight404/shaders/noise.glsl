
#include "../../shader_libs/NoiseLib.glsl"

uniform int counter;

float getRads(float val1, float val2, float mult, float div) {
	float rads = snoise(vec3(val1 / div, val2 / div, counter / div));
	float minNoise = 0.499f;
	float maxNoise = 0.501f;

	if (rads < minNoise)
		minNoise = rads;
	if (rads > maxNoise)
		maxNoise = rads;

	rads -= minNoise;
	rads *= 1.0 / (maxNoise - minNoise);

	return rads * mult;
}