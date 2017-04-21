#if GL_ES

#if __VERSION__  >= 300
	#define ENABLE_VERTEX_ID 1
	#define ENABLE_IN_OUT_FEATURE 1
#endif

#else

// The Desktop Platform, Almost all of the video drivers support the gl_VertexID, so just to enable it simply.
 #define ENABLE_VERTEX_ID 1

 #if __VERSION__ >= 130
 #define ENABLE_IN_OUT_FEATURE 1
 #endif

#endif