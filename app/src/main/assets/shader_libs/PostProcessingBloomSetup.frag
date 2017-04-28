#include "PostProcessingCommonPS.frag"

// x:BloomThreshold, y:ExposureScale (useful if eyeadaptation is locked)
uniform vec2 f2BloomThreshold;
uniform sampler2D g_Texture;

void main()
{
	vec2 f2UV = m_f4UVAndScreenPos.xy;

	vec4 f4SceneColor = texture(g_Texture, f2UV);

	// clamp to avoid artifacts from exceeding fp16 through framebuffer blending of multiple very bright lights
	f4SceneColor.rgb = min(vec3(256 * 256, 256 * 256, 256 * 256), f4SceneColor.rgb);

	vec3 f3LinearColor = f4SceneColor.rgb;

	float fExposureScale = f2BloomThreshold.y;

	// todo: make this adjustable (e.g. LUT)
	float fTotalLuminance = dot(f3LinearColor, vec3(0.3, 0.59, 0.11)) * fExposureScale;
	float fBloomLuminance = fTotalLuminance - f2BloomThreshold.x;
	// mask 0..1
	float fBloomAmount = clamp(fBloomLuminance / 2.0, 0.0, 1.0);

	Out_f4Color = vec4(fBloomAmount * f3LinearColor, 1.0/*f4SceneColor.a*/);
}