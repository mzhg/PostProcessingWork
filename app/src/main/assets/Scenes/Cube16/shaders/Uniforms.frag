
#if 0

layout(binding=0) uniform CameraCB  // egister(b0)
{
	float4x4 c_mViewProj   /*: packoffset(c0)*/;
    float3 c_vEyePos                    /*: packoffset(c4)*/;
    float c_fZNear                      /*: packoffset(c5)*/;
    float c_fZFar                       /*: packoffset(c5.y)*/;
};

layout(binding=1) uniform ObjectCB //: register( b1 )
{
    float4x4 c_mObject /*: packoffset(c0)*/;
    float3 c_vObjectColor /*: packoffset(c4)*/;
};

layout(binding=2) uniform LightCB // : register( b2 )
{
    float4x4 c_mLightViewProj  /*: packoffset(c0)*/;
    float3 c_vLightDirection                /*: packoffset(c4)*/;
    float c_fLightFalloffCosTheta           /*: packoffset(c4.w)*/;
    float3 c_vLightPos                      /*: packoffset(c5)*/;
    float c_fLightFalloffPower              /*: packoffset(c5.w)*/;
    float3 c_vLightColor                    /*: packoffset(c6)*/;
    float4 c_vLightAttenuationFactors       /*: packoffset(c7)*/;
    float c_fLightZNear                     /*: packoffset(c8)*/;
    float c_fLightZNFar                     /*: packoffset(c8.y)*/;
    float3 c_vSigmaExtinction               /*: packoffset(c9)*/;
};

#else
	uniform float4x4 c_mViewProj   				/*: packoffset(c0)*/;
	uniform float3 c_vEyePos                    /*: packoffset(c4)*/;
	uniform float c_fZNear                      /*: packoffset(c5)*/;
	uniform float c_fZFar                       /*: packoffset(c5.y)*/;
		
	uniform float4x4 c_mObject /*: packoffset(c0)*/;
	uniform float3 c_vObjectColor /*: packoffset(c4)*/;
	    
	uniform float4x4 c_mLightViewProj  /*: packoffset(c0)*/;
	uniform float3 c_vLightDirection                /*: packoffset(c4)*/;
	uniform float c_fLightFalloffCosTheta           /*: packoffset(c4.w)*/;
	uniform float3 c_vLightPos                      /*: packoffset(c5)*/;
	uniform float c_fLightFalloffPower              /*: packoffset(c5.w)*/;
	uniform float3 c_vLightColor                    /*: packoffset(c6)*/;
	uniform float4 c_vLightAttenuationFactors       /*: packoffset(c7)*/;
	uniform float c_fLightZNear                     /*: packoffset(c8)*/;
	uniform float c_fLightZNFar                     /*: packoffset(c8.y)*/;
	uniform float3 c_vSigmaExtinction               /*: packoffset(c9)*/;
#endif