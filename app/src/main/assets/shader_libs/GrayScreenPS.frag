#include "PostProcessingCommonPS.frag"

uniform sampler2D iChannel0;
uniform sampler2D iPaper;

// uniform float g_RectBorder = 0.08;
uniform vec4 g_Uniforms;
#define iResolution g_Uniforms.xy
#define g_RectBorder      g_Uniforms.z
#define g_Alpha           g_Uniforms.w   // for compistion

#define EPSILON 0.000011
#define SAMPLES  5.0

float DistToRect(vec2 uv)
{
    float prop = iResolution.y / iResolution.x;//screen proroption

    float Xmin = g_RectBorder;
    float Xmax = 1.0 - g_RectBorder;
    float Ymin = g_RectBorder;
    float Ymax = prop - Ymin;

    float X = clamp(uv.x, Xmin, Xmax);
    float Y = clamp(uv.y, Ymin, Ymax);

//    return max(abs(uv.x - X), abs(uv.y - Y));

    return distance(uv, vec2(X,Y));
}

// 2x1 hash. Used to jitter the samples.
float hash( vec2 p ){ return fract(sin(dot(p, vec2(41, 289)))*45758.5453); }

float ConstractLinear(float color, float middleGray, float c)
{
    // middleGray , 100/256
    return color + (color - middleGray) * c;
}

float ConstractLog(float color, float c)
{
    const float F = 1.0;
    return c * log(color * F + 1.0) / F;
}

float ConstractPower(float color, float c, float gamma)
{
    float diff = color - 100.0 / 256.0;
    return c * pow(abs(diff), gamma) * sign(diff) + 100.0 / 256.0;
}

void mainImage( out vec4 fragColor ){
    // Screen coordinates.
    vec2 uv = m_f4UVAndScreenPos.xy;

    // Radial blur factors.
    //
    // Falloff, as we radiate outwards.
    float decay = 0.97;
    // Controls the sample density, which in turn, controls the sample spread.
    float density = 0.02;
    // Sample weight. Decays as we radiate outwards.
    float weight = 1.0f/SAMPLES;

    float iGlobalTime = 10.0;
    // Light offset. Kind of fake. See above.
    vec3 l = vec3(0);

    // Offset texture position (uv - .5), offset again by the fake light movement.
    // It's used to set the blur direction (a direction vector of sorts), and is used
    // later to center the spotlight.
    //
    // The range is centered on zero, which allows the accumulation to spread out in
    // all directions. Ie; It's radial.
    //    vec2 tuv =  uv - .5 - l.xy*.45;
    vec2 tuv = uv - 0.5;

    // Dividing the direction vector above by the sample number and a density factor
    // which controls how far the blur spreads out. Higher density means a greater
    // blur radius.
    vec2 dTuv = tuv*density * weight;

    // Grabbing a portion of the initial texture sample. Higher numbers will make the
    // scene a little clearer, but I'm going for a bit of abstraction.
    vec4 col = texture(iChannel0, uv.xy) * weight;

    // Jittering, to get rid of banding. Vitally important when accumulating discontinuous
    // samples, especially when only a few layers are being used.
    uv += dTuv*(hash(uv.xy + fract(iGlobalTime))*2. - 1.);

    float totalWeight = 1.0 / SAMPLES;
    // The radial blur loop. Take a texture sample, move a little in the direction of
    // the radial direction vector (dTuv) then take another, slightly less weighted,
    // sample, add it to the total, then repeat the process until done.
    for(float i=0.; i < SAMPLES; i++){

        uv -= dTuv;
        col += texture(iChannel0, uv) * weight;
        totalWeight += weight;

        weight *= decay;
    }

    // Multiplying the final color with a spotlight centered on the focal point of the radial
    // blur. It's a nice finishing touch... that Passion came up with. If it's a good idea,
    // it didn't come from me. :)
    col *= (1. - dot(tuv, tuv)*.75);
    col /= totalWeight;

    // Smoothstepping the final color, just to bring it out a bit.
    fragColor = smoothstep(0., 1., col);
    fragColor  = col;
}

#define USE_PAPER 1

float overlaySingle(float s, float d)
{
    int di = int(d * 255.0);
    int si = int(s * 255.0);

    int t;
    int dor;
    if ( di < 128 )
    {
        t = di * si + 0x80;
        dor = 2 * (((t >> 8) + t) >> 8);
    }
    else
    {
        t = (255-di) * (255-si) + 0x80;
        dor = 2 * (255 - ( ((t >> 8) + t) >> 8 ));
    }

    return float(dor) / 255.0;
}

vec3 overlay(vec4 src, vec4 dst)
{
    vec3 result;
    result.r = overlaySingle(src.r, dst.r);
    result.g = overlaySingle(src.g, dst.g);
    result.b = overlaySingle(src.b, dst.b);

    return mix(dst.rgb, result.rgb, g_Alpha);
}

void main(void)//Drag mouse over rendering area
{
    if(USE_PAPER != 0)
    {
        vec4 src0 = texture(iChannel0, m_f4UVAndScreenPos.xy);
//        ivec2 texSize = textureSize();
        vec4 src1 = texture(iPaper, m_f4UVAndScreenPos.xy);

        Out_f4Color.rgb = overlay(src1, src0);
        Out_f4Color.a = 1;
    }
    else
    {
        vec2 p = gl_FragCoord.xy / iResolution.x;//normalized coords with some cheat
        float dist = DistToRect(p);

        //    float fade = dist / g_RectBorder;
        float fade = smoothstep(0.,1., dist / g_RectBorder);
        //    float fade = (dist > 0.0) ? dist / g_RectBorder : 1.0;

        //    vec3 backgroundColor = texelFetch(iChannel0, ivec2(gl_FragCoord.xy), 0).rgb;

        vec4 backgroundColor;
        mainImage(backgroundColor);

        //    backgroundColor = texelFetch(iChannel0, ivec2(gl_FragCoord.xy), 0);
        const vec3 ToGray = vec3(0.299, 0.587, 0.114);

        float grayCol = dot(backgroundColor.rgb, ToGray);
        //    grayCol += (grayCol - 100.0/255.0) * 0.3;

        grayCol = ConstractPower(grayCol, 1.0, 1);

        //for round effect, not elliptical
        Out_f4Color = vec4(mix(vec3(grayCol), vec3(0.0), fade), 1.0);
    }


}