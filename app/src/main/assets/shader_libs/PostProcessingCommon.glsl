#if GL_ES

#if __VERSION__  >= 300
	#define ENABLE_VERTEX_ID 1
	#define ENABLE_IN_OUT_FEATURE 1
	#define LAYOUT_LOC(x)  layout(location = x)
#endif
    #define LAYOUT_LOC(x)
#else

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

#endif