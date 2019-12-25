
layout(binding = 0) uniform usampler2D Reprojection;

void main()
{
    uint depth = texelFetch(Reprojection, ivec2(gl_FragCoord.xy), 0).x;
    gl_FragDepth = 1.0 - uintBitsToFloat(depth);
//    gl_FragDepth = float(depth)/float(0xFFFFFFFFu);
}