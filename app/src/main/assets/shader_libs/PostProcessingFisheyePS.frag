#include "PostProcessingCommonPS.frag"

uniform sampler2D iChannel0;
//uniform vec2 iResolution;
//uniform float factor;

uniform vec4 g_Uniforms;
#define iResolution g_Uniforms.xy
#define factor      g_Uniforms.z

#define EPSILON 0.000011

void main(void)//Drag mouse over rendering area
{
	vec2 p = gl_FragCoord.xy / iResolution.x;//normalized coords with some cheat
	                                                         //(assume 1:1 prop)
	float prop = iResolution.x / iResolution.y;//screen proroption
	vec2 m = vec2(0.5, 0.5 / prop);//center coords
	vec2 d = p - m;//vector from center to current fragment
	float r = sqrt(dot(d, d)); // distance of pixel from center

	float power = ( 2.0 * 3.141592 / (2.0 * sqrt(dot(m, m))) ) *
				(/*iMouse.x / iResolution.x*/ factor - 0.5);//amount of effect

	float bind;//radius of 1:1 effect
	if (power > 0.0) bind = sqrt(dot(m, m));//stick to corners
	else {if (prop < 1.0) bind = m.x; else bind = m.y;}//stick to borders

	//Weird formulas
	vec2 uv;
	if (power > 0.0)//fisheye
		uv = m + normalize(d) * tan(r * power) * bind / tan( bind * power);
	else if (power < 0.0)//antifisheye
		uv = m + normalize(d) * atan(r * -power * 10.0) * bind / atan(-power * bind * 10.0);
	else uv = p;//no effect for power = 1.0

	vec3 col = texture(iChannel0, vec2(uv.x, -uv.y * prop)).xyz;//Second part of cheat
	                                                  //for round effect, not elliptical
	Out_f4Color = vec4(col, 1.0);
}