#if GL_ES  // Mobile
    #if __VERSION__ >= 300
    in vec2 vInterpTexCoord;
    out vec4 Output;
    #else
    varying vec2 vInterpTexCoord;
    #define Output gl_FragColor
    #endif
#else  // Desktop
    #if __VERSION__ >= 130
    in vec2 vInterpTexCoord;
    out vec4 Output;
    #else
    varying vec2 vInterpTexCoord;
    #define Output gl_FragColor
    #endif
#endif

#ifdef GFSDK_WAVEWORKS_GL4
layout(binding=0) uniform sampler2D g_samplerDisplacementMap;  // unit 0
layout(binding=0) uniform globals
{
    vec4 g_Scales;         // was: float g_ChoppyScale, g_GradMap2TexelWSScale
    vec4 g_OneTexel_Left;
    vec4 g_OneTexel_Right;
    vec4 g_OneTexel_Back;
    vec4 g_OneTexel_Front;
};
#else  // GL2 profile
uniform sampler2D g_samplerDisplacementMap;  // unit 0
uniform vec4 g_Scales;
uniform vec4 g_OneTexel_Left;
uniform vec4 g_OneTexel_Right;
uniform vec4 g_OneTexel_Back;
uniform vec4 g_OneTexel_Front;
#endif

void main(){
    // Sample neighbour texels
    vec3 displace_left	    = textureLod(g_samplerDisplacementMap, vInterpTexCoord.xy + g_OneTexel_Left.xy, 0.0).rgb;
    vec3 displace_right	    = textureLod(g_samplerDisplacementMap, vInterpTexCoord.xy + g_OneTexel_Right.xy, 0.0).rgb;
    vec3 displace_back	    = textureLod(g_samplerDisplacementMap, vInterpTexCoord.xy + g_OneTexel_Back.xy, 0.0).rgb;
    vec3 displace_front	    = textureLod(g_samplerDisplacementMap, vInterpTexCoord.xy + g_OneTexel_Front.xy, 0.0).rgb;
    vec4 debug_value	    = textureLod(g_samplerDisplacementMap, vInterpTexCoord.xy, 0.0);

    // -------- Do not store the actual normal value, instead, it preserves two differential values.
    vec2 gradient = vec2(-(displace_right.z - displace_left.z) / max(0.01,1.0 + g_Scales.y*(displace_right.x - displace_left.x)), -(displace_front.z - displace_back.z) / max(0.01,1.0+g_Scales.y*(displace_front.y - displace_back.y)));
    //float2 gradient = {-(displace_right.z - displace_left.z), -(displace_front.z - displace_back.z) };

    // Calculate Jacobian corelation from the partial differential of displacement field
    vec2 Dx = (displace_right.xy - displace_left.xy) * g_Scales.x;
    vec2 Dy = (displace_front.xy - displace_back.xy) * g_Scales.x;
    float J = (1.0f + Dx.x) * (1.0f + Dy.y) - Dx.y * Dy.x;

    // Output
    Output = vec4(gradient, J, 0);
}