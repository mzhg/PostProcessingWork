#if GL_ES

#if __VERSION__ < 300
    #define texture(x, y) texture2D(x, y)
#endif

#if __VERSION__  >= 300
	#define ENABLE_VERTEX_ID 1
	#define ENABLE_IN_OUT_FEATURE 1
	#define LAYOUT_LOC(x)  layout(location = x)
#endif

#if __VERSION__ == 300
    vec4 textureGather(sampler2D tex, vec2 uv, int comp)
    {
        vec4 c0 = textureOffset(tex, uv, ivec2(0,1));
        vec4 c1 = textureOffset(tex, uv, ivec2(1,1));
        vec4 c2 = textureOffset(tex, uv, ivec2(1,0));
        vec4 c3 = textureOffset(tex, uv, ivec2(0,0));

        return vec4(c0[comp], c1[comp], c2[comp],c3[comp]);
    }
#elif __VERSION__ < 300

    vec4 textureGather(sampler2D tex, vec2 uv, int comp)
    {
        vec4 c3 = texture2D(tex, uv);
        return vec4(c3[comp], c3[comp], c3[comp],c3[comp]);
    }
#endif

    #define LAYOUT_LOC(x)
#else  // Desktop

// The Desktop Platform, Almost all of the video drivers support the gl_VertexID, so just to enable it simply.
 #define ENABLE_VERTEX_ID 1

 #if __VERSION__ >= 130
 #define ENABLE_IN_OUT_FEATURE 1
 #endif

 #if __VERSION__ >= 410
    #define LAYOUT_LOC(x)  layout(location = x)
 #else
    #define LAYOUT_LOC(x)
 #endif

 #if __VERSION__ < 400
     vec4 textureGather(sampler2D tex, vec2 uv, int comp)
     {
         vec4 c0 = textureOffset(tex, uv, ivec2(0,1));
         vec4 c1 = textureOffset(tex, uv, ivec2(1,1));
         vec4 c2 = textureOffset(tex, uv, ivec2(1,0));
         vec4 c3 = textureOffset(tex, uv, ivec2(0,0));

         return vec4(c0[comp], c1[comp], c2[comp],c3[comp]);
     }
 #endif

#endif


#define rcp(x) 1.0/x
#define saturate(x) clamp(x, 0.0, 1.0)
#define lerp(x,a,b) mix(x,a,b)