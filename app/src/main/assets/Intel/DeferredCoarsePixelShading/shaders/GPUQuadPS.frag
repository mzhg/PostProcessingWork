#include "GPUQuad.glsl"

 layout(location = 0) out float4 Out_Color /*: SV_Target0*/;
  in flat int lightIndex /*: lightIndex*/;

 void main()
 {
     // Shade only sample 0
     Out_Color =  GPUQuad(gl_FragCoord.xy, lightIndex, 0);
 }