#include "../UE4Common.glsl"

#ifndef NUM_CULLED_GRID_PRIMITIVE_TYPES
#define NUM_CULLED_GRID_PRIMITIVE_TYPES 2  // Alawyas 2
#endif

layout(binding = 0, r32ui) uniform uimageBuffer RWNumCulledLightsGrid;
layout(binding = 1, r32ui) uniform uimageBuffer RWCulledLightDataGrid;

#define LIGHT_LINK_STRIDE 2
#define NUM_CULLED_LIGHTS_GRID_STRIDE 2

void writeCullLightTo(uimageBuffer dest, uint destIndex, uint first, uint second)
{
    imageStore(dest, int(destIndex) * LIGHT_LINK_STRIDE + 0, uint4(first, 0, 0, 0));
    imageStore(dest, int(destIndex) * LIGHT_LINK_STRIDE + 1, uint4(second, 0, 0, 0));
}

uint atomicAdd(atomic_uint buf, uint value)
{
    uint result = atomicCounter(buf);

    while(value > 0)
    {
        result = atomicCounterIncrement(buf);
        value --;
    }

    return result;
}