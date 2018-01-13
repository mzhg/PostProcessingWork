
//#include "IntelExtensions.hlsl"
#include "GBuffer.glsl"
#include "AVSM_Gen.glsl"
#include "ListTexture.glsl"

layout(early_fragment_tests) in;

//in DynamicParticlePSIn Input;

in _DynamicParticlePSIn
{
//    float4 Position  : SV_POSITION;
    float3 UVS		 /*: TEXCOORD0*/;
    float  Opacity	 /*: TEXCOORD1*/;
    float3 ViewPos	 /*: TEXCOORD2*/;
    float3 ObjPos    /*: TEXCOORD3*/;
    float3 ViewCenter/*: TEXCOORD4*/;
    float4 color      /*: COLOR*/;
    float2 ShadowInfo /*: TEXCOORD5*/;
}Input;

//void ParticleAVSMCapture_PS(DynamicParticlePSIn Input)
void main()
{
    float3 entry, exit;
    float  segmentTransmittance;

    DynamicParticlePSIn _Input;
    _Input.UVS = Input.UVS;
    _Input.Opacity = Input.Opacity;
    _Input.ViewPos = Input.ViewPos;
    _Input.ObjPos = Input.ObjPos;
    _Input.ViewCenter = Input.ViewCenter;
    _Input.color = Input.color;
    _Input.ShadowInfo = Input.ShadowInfo;

	if (IntersectDynamicParticle(_Input, entry, exit, segmentTransmittance)) {

        // Allocate a new node
        // (If we're running out of memory we simply drop this fragment
        uint newNodeAddress;

        if (LT_AllocSegmentNode(newNodeAddress)) {
            // Fill node
            ListTexSegmentNode node;
            node.depth[0] = entry.z;
            node.depth[1] = exit.z;
            node.trans    = segmentTransmittance;
            node.sortKey  = _Input.ViewPos.z;

	        // Get fragment viewport coordinates
            int2 screenAddress = int2(gl_FragCoord.xy);

            // Insert node!
            LT_InsertFirstSegmentNode(screenAddress, newNodeAddress, node);
        }
	}
}