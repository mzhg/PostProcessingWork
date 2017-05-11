#include "PostProcessingCommonPS.frag"

#ifndef AO_BLUR
#define AO_BLUR 1
#endif

uniform sampler2DArray texResultsArray;

void main() {
  ivec2 FullResPos = ivec2(gl_FragCoord.xy);
  ivec2 Offset = FullResPos & 3;
  int SliceId = Offset.y * 4 + Offset.x;
  ivec2 QuarterResPos = FullResPos >> 2;
  
#if AO_BLUR
  Out_f4Color = vec4(texelFetch( texResultsArray, ivec3(QuarterResPos, SliceId), 0).xy,0,0);
#else
  Out_f4Color = vec4(texelFetch( texResultsArray, ivec3(QuarterResPos, SliceId), 0).x);
#endif
//  out_Color.r = 1.0;
}