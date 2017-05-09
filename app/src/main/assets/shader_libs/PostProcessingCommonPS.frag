#include "PostProcessingCommon.glsl"

#if GL_ES
precision highp float;
#endif

#if ENABLE_IN_OUT_FEATURE
    in vec4 m_f4UVAndScreenPos;
    LAYOUT_LOC(0) out vec4 Out_f4Color;
#else
    varying vec4 m_f4UVAndScreenPos;
   #define Out_f4Color gl_FragColor
#endif