#include "CloudsCommon.glsl"

// This shader compute dispatch arguments for the DispatchIndirect() method
//[numthreads(1, 1, 1)]
layout (local_size_x = 1) in;

void main()
{
    uint s = g_GlobalCloudAttribs.uiDensityBufferScale;
    g_DispatchArgsRW[0] = (g_ValidCellsCounter.Load(0) * s*s*s * g_GlobalCloudAttribs.uiMaxLayers + THREAD_GROUP_SIZE-1) / THREAD_GROUP_SIZE;
}