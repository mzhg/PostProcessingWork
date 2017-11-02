#include "AVSM.glsl"

//layout(binding = 0) uniform sampler2DArray gDebugAVSMDataSRV;
layout(location = 0) out vec4 OutColor;
void main()
{
    int2 viewportPos = int2(gl_FragCoord.xy);

    //return NONCPUT_gAVSMTexture[int3(x,y,0)][0] == mEmptyNode ? float4(1, 1, 1, 1) : float4(0, 0, 0, 0); // Visualize where 1st node is a valid depth value
    float minTransmittance = 1.0f;
    for (int i = 0; i < AVSM_RT_COUNT; ++i) {
//        float4 depth = NONCPUT_gAVSMTexture[int3(viewportPos.xy,i)];
//        float4 trans = NONCPUT_gAVSMTexture[int3(viewportPos.xy,i + AVSM_RT_COUNT)];
        vec4 depth = texelFetch(NONCPUT_gAVSMTexture, int3(viewportPos.xy,i), 0);
        vec4 trans = texelFetch(NONCPUT_gAVSMTexture, int3(viewportPos.xy,i + AVSM_RT_COUNT), 0);

        for (int j = 0; j < 4; ++j) {
            if (depth[j] != mEmptyNode)
//            if (notEqual(depth[j], float4(mEmptyNode)))
            {
                minTransmittance = min(minTransmittance, trans[j]);
            }
        }
    }

    OutColor = float4(minTransmittance);
}