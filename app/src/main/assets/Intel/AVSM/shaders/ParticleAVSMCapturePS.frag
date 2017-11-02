
//#include "IntelExtensions.hlsl"
#include "GBuffer.glsl"
#include "AVSM_Gen.glsl"
#include "AVSM_Resolve.glsl"
#include "Rendering.glsl"
#include "ConstantBuffers.glsl"
#include "ScreenSpace.glsl"

layout(early_fragment_tests) in;

in DynamicParticlePSIn Input;

//void ParticleAVSMCapture_PS(DynamicParticlePSIn Input)
void main()
{
    float3 entry, exit;
    float  segmentTransmittance;
	if (IntersectDynamicParticle(Input, entry, exit, segmentTransmittance)) {

        // Allocate a new node
        // (If we're running out of memory we simply drop this fragment
        uint newNodeAddress;

        if (LT_AllocSegmentNode(newNodeAddress)) {
            // Fill node
            ListTexSegmentNode node;
            node.depth[0] = entry.z;
            node.depth[1] = exit.z;
            node.trans    = segmentTransmittance;
            node.sortKey  = Input.ViewPos.z;

	        // Get fragment viewport coordinates
            int2 screenAddress = int2(gl_FragCoord.xy);

            // Insert node!
            LT_InsertFirstSegmentNode(screenAddress, newNodeAddress, node);
        }
	}
}