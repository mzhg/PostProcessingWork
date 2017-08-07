
layout(location = 0) out vec4 Out_f4Color;
layout(rgba32f, binding = 0) uniform uimageBuffer AnchorPoints;
uniform mat4 AnchorPointTransform;

void main()
{
    int index = int(gl_FragCoord.x);
    float4 particle = imageLoad(AnchorPoints, index); // AnchorPoints.Load(index);
    float4 newPosition = mul(float4(particle.yzw, 1), AnchorPointTransform);
    Out_f4Color = float4(0, newPosition.xyz);
}