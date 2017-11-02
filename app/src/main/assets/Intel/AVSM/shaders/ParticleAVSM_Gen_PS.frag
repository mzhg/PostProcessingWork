#include "GBuffer.glsl"
#include "AVSM_Gen.glsl"
#include "AVSM_Resolve.glsl"
#include "Rendering.glsl"
#include "ConstantBuffers.glsl"
#include "ScreenSpace.glsl"

layout(early_fragment_tests) in;
in DynamicParticlePSIn Input;

void main()
{
    // Initialize Intel Shader Extensions
//    	IntelExt_Init();

        float3 entry, exit;
        float  segmentTransmittance;
    	IntersectDynamicParticle(Input, entry, exit, segmentTransmittance); // does the ray tracing with sphere for transmittence amount + thickness (thickness doesn't get used yet)
    	if (segmentTransmittance > 0.0)
        {
    		// From now on serialize all UAV accesses (with respect to other fragments shaded in flight which map to the same pixel)
//    		IntelExt_BeginPixelShaderOrdering();

            const int2 pixelAddr = int2(gl_Position.xy);

    #ifndef AVSM_GEN_SOFT
            const float shadowZBias = 0.9; // shift sample a bit away from shadow source to avoid self-shadowing due to shadow sample cube size (less required for bigger shadowmap dimensions and/or more nodes, and much less in case linear shadow function is added)
            entry.z += shadowZBias;
    		exit.z  += shadowZBias;
    #endif

            float ctrlSurface = AVSMGenLoadControlSurfaceUAV(pixelAddr);
            if (0 == ctrlSurface) {
                AVSMGenData avsmData;

                // Clear and initialize avsm data with just one fragment
    #ifdef AVSM_GEN_SOFT
                AVSMGenInitDataSoft(avsmData, entry.z, exit.z, segmentTransmittance);
    #else
                AVSMGenInitData(avsmData, entry.z, segmentTransmittance);
    #endif

    			// Store AVSM data
                AVSMGenStoreRawDataUAV(pixelAddr, avsmData);

    			// Update control surface
    			ctrlSurface = 1;
                AVSMGenStoreControlSurface(pixelAddr, ctrlSurface);
            } else {
                AVSMGenNode nodeArray[AVSM_NODE_COUNT];

                AVSMGenLoadDataUAV(pixelAddr, nodeArray);
    #ifdef AVSM_GEN_SOFT
                AVSMGenInsertFragmentSoft(entry.z, exit.z, segmentTransmittance, nodeArray);
    #else
                AVSMGenInsertFragment(entry.z, segmentTransmittance, nodeArray);
    #endif
                AVSMGenStoreDataUAV(pixelAddr, nodeArray);
            }
    	}
}