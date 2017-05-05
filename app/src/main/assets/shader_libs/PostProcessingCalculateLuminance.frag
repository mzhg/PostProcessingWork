#include "PostProcessingCommonPS.frag"

uniform sampler2D g_InputImage;
uniform sampler2D g_LastLumTex;
const vec3 LUMINANCE_VECTOR = vec3(0.2125f, 0.7154f, 0.0721f);

uniform float g_ElapsedTime;

void main()
{
	float logLumSum = 0.0f;
	int x, y;
//	ivec2 size = textureSize(inputImage, 0);
	for (y = 0; y<16; y++) {
		for (x = 0; x<16; x++) {
		    vec2 texcoord = vec2(x, y)/vec2(16);
			logLumSum += (dot(texture(g_InputImage, texcoord).rgb, LUMINANCE_VECTOR) + 0.00001);
		}
	}
	logLumSum /= 256.0;
	float currentLum = (logLumSum + 0.00001);

//	float currentLum = textureLod(currentImage, vec2(0), 0.0).r;
    float lastLum = texture(g_LastLumTex, vec2(0)).r;
    float newLum = lastLum + (currentLum - lastLum) * (1.0 - pow(0.98, 30.0 * g_ElapsedTime));
    //	imageStore(image1, ivec2(0, 0), vec4(newLum, newLum, newLum, newLum));

	Out_f4Color = vec4(newLum);
}