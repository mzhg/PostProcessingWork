#include "Common.glsl"

/** Z index of the minimum slice in the range. */
uniform int MinZ;
uniform float4 ViewSpaceBoundingSphere;
uniform float4x4 ViewToVolumeClip;

layout(location = 1) in vec2 InUV;

out flat int LayerIndex;

void main()
{
    float SliceDepth = ComputeDepthFromZSlice(gl_InstanceID + MinZ);
    float SliceDepthOffset = abs(SliceDepth - ViewSpaceBoundingSphere.z);

    if (SliceDepthOffset < ViewSpaceBoundingSphere.w)
    {
        // Compute the radius of the circle formed by the intersection of the bounding sphere and the current depth slice
        float SliceRadius = sqrt(ViewSpaceBoundingSphere.w * ViewSpaceBoundingSphere.w - SliceDepthOffset * SliceDepthOffset);
        // Place the quad vertex to tightly bound the circle
        float3 ViewSpaceVertexPosition = float3(ViewSpaceBoundingSphere.xy + (InUV * 2 - 1) * SliceRadius, SliceDepth);
        gl_Position = mul(float4(ViewSpaceVertexPosition, 1), ViewToVolumeClip);
    }
    else
    {
        // Slice does not intersect bounding sphere, emit degenerate triangle
        gl_Position = vec4(0);
    }

    // Debug - draw to entire texture in xy
    //Output.Vertex.Position = float4(InUV * float2(2, -2) + float2(-1, 1), 0, 1);

//    Output.Vertex.UV = 0;
    LayerIndex = gl_InstanceID + MinZ;
}