#include "GPUQuadDL.glsl"

void main()
{
    // Shade only sample 0
    Out_Color = GPUQuadDLResolve( 0);
}